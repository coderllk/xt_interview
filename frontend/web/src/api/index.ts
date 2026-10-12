import request from '@/utils/request'

export interface Department {
  id: number
  name: string
  code: string
  description: string | null
  status: number
  createTime: string
  updateTime: string
}

export interface DepartmentInput {
  name: string
  code: string
  description: string
  status: number
}

export interface Account {
  id: number
  username: string
  name: string
  gender: string | null
  phone: string | null
  roleLevel: number
  status: number
  isSuper: number
  departmentId: number | null
  departmentName: string | null
  createTime: string
  updateTime: string
}

export interface AccountInput {
  username: string
  name: string
  gender: string
  phone: string
  password?: string
  roleLevel: number
  status: number
  isSuper: number
  departmentId: number | null
}

export interface AccountPageQuery {
  username?: string
  status?: number
  roleLevel?: number
  page: number
  size: number
}

export interface AccountPage {
  list: Account[]
  totalElements: number
  totalPages: number
  currentPage: number
  pageSize: number
}

// 认证相关 API
export const authApi = {
  login(data: { username: string; password: string }) {
    return request.post('/auth/login', data)
  },
  logout() {
    return request.post('/auth/logout')
  },
  validate() {
    return request.get('/auth/validate')
  }
}

// 账号管理 API
export const accountApi = {
  getAll() {
    return request.get<Account[], Account[]>('/accounts')
  },
  getById(id: number) {
    return request.get<Account, Account>(`/accounts/${id}`)
  },
  getPage(params: AccountPageQuery) {
    return request.get<AccountPage, AccountPage>('/accounts/page', { params })
  },
  create(data: AccountInput) {
    return request.post<Account, Account>('/accounts', data)
  },
  update(id: number, data: AccountInput) {
    return request.put<Account, Account>(`/accounts/${id}`, data)
  },
  updateStatus(id: number, status: 0 | 1) {
    return request.put<Account, Account>(`/accounts/${id}/status`, { status })
  },
  delete(id: number) {
    return request.delete<void, void>(`/accounts/${id}`)
  }
}

// 部门管理 API
export const departmentApi = {
  getAll() {
    return request.get<Department[], Department[]>('/departments')
  },
  getById(id: number) {
    return request.get<Department, Department>(`/departments/${id}`)
  },
  create(data: DepartmentInput) {
    return request.post<Department, Department>('/departments', data)
  },
  update(id: number, data: DepartmentInput) {
    return request.put<Department, Department>(`/departments/${id}`, data)
  },
  updateStatus(id: number, status: 0 | 1) {
    return request.put<Department, Department>(`/departments/${id}/status`, { status })
  },
  delete(id: number) {
    return request.delete<void, void>(`/departments/${id}`)
  }
}
