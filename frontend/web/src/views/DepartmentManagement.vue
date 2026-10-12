<template>
  <div class="department-management">
    <div class="page-header">
      <el-button type="primary" :icon="Plus" @click="openCreateDialog">
        添加部门
      </el-button>
      <el-button :icon="Refresh" :disabled="loading" @click="loadDepartments">刷新</el-button>
    </div>

    <el-table v-loading="loading" :data="departments" border stripe style="width: 100%">
      <el-table-column prop="name" label="部门名称" width="160" />
      <el-table-column prop="code" label="部门编码" width="160" />
      <el-table-column prop="description" label="部门描述" min-width="200" show-overflow-tooltip />
      <el-table-column label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'danger'">
            {{ row.status === 1 ? '启用' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" width="180" />
      <el-table-column prop="updateTime" label="更新时间" width="180" />
      <el-table-column label="操作" width="240" fixed="right">
        <template #default="{ row }">
          <div class="row-actions">
            <el-switch
              :model-value="row.status"
              :active-value="1"
              :inactive-value="0"
              inline-prompt
              active-text="启用"
              inactive-text="禁用"
              :width="60"
              :loading="statusUpdatingIds.has(row.id)"
              :disabled="loading || isRowBusy(row.id)"
              :aria-label="`部门 ${row.name} 启用状态`"
              @change="handleStatusChange(row, $event)"
            />
            <el-button type="primary" link :icon="Edit" :disabled="isRowBusy(row.id)" @click="openEditDialog(row)">
              编辑
            </el-button>
            <el-button
              type="danger"
              link
              :icon="Delete"
              :loading="deletingId === row.id"
              :disabled="statusUpdatingIds.has(row.id)"
              @click="handleDelete(row)"
            >
              删除
            </el-button>
          </div>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑部门' : '添加部门'"
      width="500px"
      :close-on-click-modal="!submitting"
      :close-on-press-escape="!submitting"
      :show-close="!submitting"
      @close="resetForm"
    >
      <el-form ref="formRef" :model="formData" :rules="formRules" :disabled="submitting" label-width="90px">
        <el-form-item label="部门名称" prop="name">
          <el-input v-model="formData.name" placeholder="请输入部门名称" maxlength="100" />
        </el-form-item>
        <el-form-item label="部门编码" prop="code">
          <el-input v-model="formData.code" placeholder="请输入部门编码" maxlength="50" />
        </el-form-item>
        <el-form-item label="部门描述" prop="description">
          <el-input
            v-model="formData.description"
            type="textarea"
            :rows="3"
            placeholder="请输入部门描述（可选）"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select v-model="formData.status" placeholder="请选择状态" style="width: 100%">
            <el-option label="启用" :value="1" />
            <el-option label="禁用" :value="0" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button :disabled="submitting" @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { departmentApi } from '@/api'
import type { Department, DepartmentInput } from '@/api'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Edit, Delete, Refresh } from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'

const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const editId = ref(0)
const deletingId = ref<number | null>(null)
const statusUpdatingIds = reactive(new Set<number>())
const departments = ref<Department[]>([])
const formRef = ref<FormInstance>()
let departmentsRequestId = 0
const formData = reactive<DepartmentInput>({
  name: '',
  code: '',
  description: '',
  status: 1
})

const formRules: FormRules = {
  name: [
    { required: true, whitespace: true, message: '请输入部门名称', trigger: 'blur' },
    { max: 100, message: '部门名称不能超过100个字符', trigger: 'blur' }
  ],
  code: [
    { required: true, whitespace: true, message: '请输入部门编码', trigger: 'blur' },
    { max: 50, message: '部门编码不能超过50个字符', trigger: 'blur' }
  ],
  description: [{ max: 500, message: '部门描述不能超过500个字符', trigger: 'blur' }],
  status: [{ required: true, type: 'enum', enum: [0, 1], message: '请选择状态', trigger: 'change' }]
}

async function loadDepartments() {
  const requestId = ++departmentsRequestId
  loading.value = true
  try {
    const result = await departmentApi.getAll()
    if (requestId === departmentsRequestId) departments.value = result
  } catch (error: any) {
    if (requestId === departmentsRequestId) {
      ElMessage.error(error.message || '加载部门列表失败')
    }
  } finally {
    if (requestId === departmentsRequestId) loading.value = false
  }
}

function isRowBusy(id: number) {
  return statusUpdatingIds.has(id) || deletingId.value === id
    || (dialogVisible.value && isEdit.value && editId.value === id)
}

async function handleStatusChange(row: Department, status: string | number | boolean) {
  if ((status !== 0 && status !== 1) || status === row.status || loading.value || isRowBusy(row.id)) return
  statusUpdatingIds.add(row.id)
  try {
    const result = await departmentApi.updateStatus(row.id, status)
    Object.assign(row, result)
    ElMessage.success(status === 1 ? '部门已启用' : '部门已禁用')
    await loadDepartments()
  } catch (error: any) {
    ElMessage.error(error.message || '更新部门状态失败')
  } finally {
    statusUpdatingIds.delete(row.id)
  }
}

function resetForm() {
  formData.name = ''
  formData.code = ''
  formData.description = ''
  formData.status = 1
  isEdit.value = false
  editId.value = 0
  formRef.value?.clearValidate()
}

function openCreateDialog() {
  resetForm()
  dialogVisible.value = true
}

function openEditDialog(row: Department) {
  if (isRowBusy(row.id)) return
  resetForm()
  isEdit.value = true
  editId.value = row.id
  formData.name = row.name
  formData.code = row.code
  formData.description = row.description || ''
  formData.status = row.status
  dialogVisible.value = true
}

async function handleSubmit() {
  if (!formRef.value || submitting.value) return
  submitting.value = true
  try {
    const valid = await formRef.value.validate().catch(() => false)
    if (!valid) return

    const data: DepartmentInput = {
      ...formData,
      name: formData.name.trim(),
      code: formData.code.trim()
    }
    if (isEdit.value) {
      await departmentApi.update(editId.value, data)
      ElMessage.success('部门更新成功')
    } else {
      await departmentApi.create(data)
      ElMessage.success('部门创建成功')
    }
    dialogVisible.value = false
    await loadDepartments()
  } catch (error: any) {
    ElMessage.error(error.message || '操作失败')
  } finally {
    submitting.value = false
  }
}

async function handleDelete(row: Department) {
  if (deletingId.value !== null || isRowBusy(row.id)) return
  deletingId.value = row.id
  try {
    await ElMessageBox.confirm(`确定要删除部门 "${row.name}" 吗？`, '删除确认', {
      confirmButtonText: '确定删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await departmentApi.delete(row.id)
    ElMessage.success('部门删除成功')
    await loadDepartments()
  } catch (error: any) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error(error.message || '删除失败')
    }
  } finally {
    deletingId.value = null
  }
}

onMounted(() => {
  loadDepartments()
})
</script>

<style scoped>
.department-management {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
}

.page-header {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
}

.row-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.row-actions .el-button + .el-button {
  margin-left: 0;
}
</style>
