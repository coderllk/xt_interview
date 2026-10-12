<template>
  <div class="user-management">
    <div class="page-header">
      <el-button type="primary" :icon="Plus" @click="openCreateDialog">
        添加用户
      </el-button>
      <el-button :icon="Refresh" :disabled="loading" @click="loadUsers">刷新</el-button>
    </div>

    <el-form :inline="true" :model="searchForm" class="search-form" @submit.prevent="handleSearch">
      <el-form-item label="用户名">
        <el-input
          v-model="searchForm.username"
          placeholder="请输入用户名"
          clearable
          maxlength="50"
        />
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="searchForm.status" placeholder="全部状态" clearable class="search-select">
          <el-option label="启用" :value="1" />
          <el-option label="禁用" :value="0" />
        </el-select>
      </el-form-item>
      <el-form-item label="权限级别">
        <el-select v-model="searchForm.roleLevel" placeholder="全部权限" clearable class="search-select">
          <el-option label="用户权限" :value="1" />
          <el-option label="管理员权限" :value="2" />
          <el-option label="超级管理员权限" :value="3" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :icon="Search" @click="handleSearch">搜索</el-button>
        <el-button @click="resetSearch">重置</el-button>
      </el-form-item>
    </el-form>

    <el-table
      v-loading="loading"
      :data="users"
      border
      stripe
      style="width: 100%"
    >
      <el-table-column prop="username" label="用户名" width="120" />
      <el-table-column prop="name" label="姓名" width="100" />
      <el-table-column prop="gender" label="性别" width="80" />
      <el-table-column prop="phone" label="联系方式" width="140" />
      <el-table-column label="所属部门" min-width="140">
        <template #default="{ row }">{{ row.departmentName || '未分配' }}</template>
      </el-table-column>
      <el-table-column label="权限级别" width="120">
        <template #default="{ row }">
          <el-tag :type="roleTagType(row.roleLevel)">
            {{ roleText(row.roleLevel) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'danger'">
            {{ row.status === 1 ? '启用' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" width="180" />
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
              :aria-label="`用户 ${row.username} 启用状态`"
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

    <el-pagination
      class="pagination"
      :current-page="currentPage"
      :page-size="pageSize"
      :page-sizes="[10, 20, 50, 100]"
      :total="totalElements"
      layout="total, sizes, prev, pager, next, jumper"
      @current-change="handlePageChange"
      @size-change="handleSizeChange"
    />

    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑用户' : '添加用户'"
      width="500px"
      :close-on-click-modal="!submitting"
      :close-on-press-escape="!submitting"
      :show-close="!submitting"
      @close="resetForm"
    >
      <el-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        :disabled="submitting"
        label-width="90px"
      >
        <el-form-item label="用户名" prop="username">
          <el-input v-model="formData.username" placeholder="请输入用户名" maxlength="50" />
        </el-form-item>
        <el-form-item label="姓名" prop="name">
          <el-input v-model="formData.name" placeholder="请输入姓名" maxlength="100" />
        </el-form-item>
        <el-form-item label="性别" prop="gender">
          <el-select v-model="formData.gender" placeholder="请选择性别" style="width: 100%">
            <el-option label="男" value="男" />
            <el-option label="女" value="女" />
          </el-select>
        </el-form-item>
        <el-form-item label="联系方式" prop="phone">
          <el-input v-model="formData.phone" placeholder="请输入联系方式" maxlength="20" />
        </el-form-item>
        <el-form-item label="所属部门" prop="departmentId">
          <el-select
            v-model="formData.departmentId"
            placeholder="请选择所属部门（可选）"
            clearable
            filterable
            :loading="departmentsLoading"
            :disabled="departmentsLoading"
            style="width: 100%"
            @clear="formData.departmentId = null"
          >
            <el-option
              v-for="department in departmentOptions"
              :key="department.id"
              :label="department.name"
              :value="department.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input
            v-model="formData.password"
            type="password"
            show-password
            maxlength="255"
            :placeholder="isEdit ? '留空则不修改密码' : '留空则默认123456'"
          />
        </el-form-item>
        <el-form-item label="权限级别" prop="roleLevel">
          <el-select v-model="formData.roleLevel" placeholder="请选择权限级别" style="width: 100%">
            <el-option label="用户权限" :value="1" />
            <el-option label="管理员权限" :value="2" />
            <el-option label="超级管理员权限" :value="3" />
          </el-select>
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
        <el-button type="primary" :loading="submitting" :disabled="departmentsLoading" @click="handleSubmit">
          确定
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { accountApi, departmentApi } from '@/api'
import type { Account, AccountInput, AccountPageQuery, Department } from '@/api'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Edit, Delete, Refresh, Search } from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'

const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const editId = ref<number>(0)
const deletingId = ref<number | null>(null)
const statusUpdatingIds = reactive(new Set<number>())
const departmentsLoading = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const totalElements = ref(0)
let usersRequestId = 0
let departmentsRequestId = 0

const users = ref<Account[]>([])
const departments = ref<Department[]>([])
const currentDepartment = ref<{ id: number; name: string } | null>(null)
const searchForm = reactive({
  username: '',
  status: undefined as number | undefined,
  roleLevel: undefined as number | undefined
})
const searchParams = reactive<Pick<AccountPageQuery, 'username' | 'status' | 'roleLevel'>>({})

const departmentOptions = computed(() => {
  const options = departments.value.map(({ id, name }) => ({ id, name }))
  const current = currentDepartment.value
  if (current && !options.some((department) => department.id === current.id)) {
    options.push({ id: current.id, name: `${current.name}（已删除）` })
  }
  return options
})

const formRef = ref<FormInstance>()

const formData = reactive({
  username: '',
  name: '',
  gender: '男',
  phone: '',
  password: '',
  roleLevel: 1,
  status: 1,
  isSuper: 0,
  departmentId: null as number | null
})

const formRules: FormRules = {
  username: [
    { required: true, whitespace: true, message: '请输入用户名', trigger: 'blur' },
    { max: 50, message: '用户名不能超过50个字符', trigger: 'blur' }
  ],
  name: [
    { required: true, whitespace: true, message: '请输入姓名', trigger: 'blur' },
    { max: 100, message: '姓名不能超过100个字符', trigger: 'blur' }
  ],
  phone: [{ max: 20, message: '联系方式不能超过20个字符', trigger: 'blur' }],
  password: [{ max: 255, message: '密码不能超过255个字符', trigger: 'blur' }],
  roleLevel: [{ required: true, type: 'enum', enum: [1, 2, 3], message: '请选择权限级别', trigger: 'change' }],
  status: [{ required: true, type: 'enum', enum: [0, 1], message: '请选择状态', trigger: 'change' }]
}

function roleText(level: number): string {
  const map: Record<number, string> = { 1: '用户权限', 2: '管理员权限', 3: '超级管理员' }
  return map[level] || '未知'
}

function roleTagType(level: number): 'info' | 'warning' | 'danger' {
  const map: Record<number, 'info' | 'warning' | 'danger'> = {
    1: 'info',
    2: 'warning',
    3: 'danger'
  }
  return map[level] || 'info'
}

async function loadUsers() {
  const requestId = ++usersRequestId
  loading.value = true
  try {
    const result = await accountApi.getPage({
      ...searchParams,
      page: currentPage.value - 1,
      size: pageSize.value
    })
    if (requestId !== usersRequestId) return

    totalElements.value = result.totalElements
    const lastPage = Math.max(1, result.totalPages)
    if (currentPage.value > lastPage) {
      currentPage.value = lastPage
      await loadUsers()
      return
    }
    users.value = result.list
  } catch (error: any) {
    if (requestId === usersRequestId) {
      ElMessage.error(error.message || '加载用户列表失败')
    }
  } finally {
    if (requestId === usersRequestId) loading.value = false
  }
}

function isRowBusy(id: number) {
  return statusUpdatingIds.has(id) || deletingId.value === id
    || (dialogVisible.value && isEdit.value && editId.value === id)
}

async function handleStatusChange(row: Account, status: string | number | boolean) {
  if ((status !== 0 && status !== 1) || status === row.status || loading.value || isRowBusy(row.id)) return
  statusUpdatingIds.add(row.id)
  try {
    const result = await accountApi.updateStatus(row.id, status)
    Object.assign(row, result)
    ElMessage.success(status === 1 ? '用户已启用' : '用户已禁用')
    await loadUsers()
  } catch (error: any) {
    ElMessage.error(error.message || '更新用户状态失败')
  } finally {
    statusUpdatingIds.delete(row.id)
  }
}

function handleSearch() {
  searchParams.username = searchForm.username.trim() || undefined
  searchParams.status = typeof searchForm.status === 'number' ? searchForm.status : undefined
  searchParams.roleLevel = typeof searchForm.roleLevel === 'number' ? searchForm.roleLevel : undefined
  currentPage.value = 1
  loadUsers()
}

function resetSearch() {
  searchForm.username = ''
  searchForm.status = undefined
  searchForm.roleLevel = undefined
  handleSearch()
}

function handlePageChange(page: number) {
  currentPage.value = page
  loadUsers()
}

function handleSizeChange(size: number) {
  pageSize.value = size
  currentPage.value = 1
  loadUsers()
}

async function loadDepartments() {
  const requestId = ++departmentsRequestId
  departmentsLoading.value = true
  try {
    const result = await departmentApi.getAll()
    if (requestId === departmentsRequestId) departments.value = result
  } catch (error: any) {
    if (requestId === departmentsRequestId) {
      departments.value = []
      ElMessage.error(error.message || '加载部门列表失败')
    }
  } finally {
    if (requestId === departmentsRequestId) departmentsLoading.value = false
  }
}

function resetForm() {
  formData.username = ''
  formData.name = ''
  formData.gender = '男'
  formData.phone = ''
  formData.password = ''
  formData.roleLevel = 1
  formData.status = 1
  formData.isSuper = 0
  formData.departmentId = null
  currentDepartment.value = null
  isEdit.value = false
  editId.value = 0
  formRef.value?.clearValidate()
}

function openCreateDialog() {
  resetForm()
  dialogVisible.value = true
  loadDepartments()
}

function openEditDialog(row: Account) {
  if (isRowBusy(row.id)) return
  resetForm()
  isEdit.value = true
  editId.value = row.id
  formData.username = row.username
  formData.name = row.name
  formData.gender = row.gender || '男'
  formData.phone = row.phone || ''
  formData.roleLevel = row.roleLevel
  formData.status = row.status
  formData.isSuper = row.isSuper
  formData.departmentId = row.departmentId ?? null
  if (row.departmentId != null) {
    currentDepartment.value = {
      id: row.departmentId,
      name: row.departmentName || `部门 ${row.departmentId}`
    }
  }
  dialogVisible.value = true
  loadDepartments()
}

async function handleSubmit() {
  if (!formRef.value || submitting.value || departmentsLoading.value) return
  submitting.value = true
  try {
    const valid = await formRef.value.validate().catch(() => false)
    if (!valid) return

    const { password, ...fields } = formData
    const data: AccountInput = {
      ...fields,
      username: fields.username.trim(),
      name: fields.name.trim(),
      departmentId: fields.departmentId ?? null,
      ...(password ? { password } : {})
    }

    if (isEdit.value) {
      await accountApi.update(editId.value, data)
      ElMessage.success('用户更新成功')
    } else {
      await accountApi.create(data)
      ElMessage.success('用户创建成功')
    }
    dialogVisible.value = false
    await loadUsers()
  } catch (error: any) {
    ElMessage.error(error.message || '操作失败')
  } finally {
    submitting.value = false
  }
}

async function handleDelete(row: Account) {
  if (deletingId.value !== null || isRowBusy(row.id)) return
  deletingId.value = row.id
  try {
    await ElMessageBox.confirm(
      `确定要删除用户 "${row.username}" 吗？`,
      '删除确认',
      {
        confirmButtonText: '确定删除',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    await accountApi.delete(row.id)
    ElMessage.success('用户删除成功')
    await loadUsers()
  } catch (error: any) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error(error.message || '删除失败')
    }
  } finally {
    deletingId.value = null
  }
}

onMounted(() => {
  loadUsers()
})
</script>

<style scoped>
.user-management {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
}

.page-header {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
}

.search-select {
  width: 170px;
}

.pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
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
