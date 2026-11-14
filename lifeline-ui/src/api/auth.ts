import { post, get, put } from '@/utils/request'
import type { PageResult, PageQuery } from '@/types/api.d'
import type {
  LoginRequest, LoginResponse, UserInfoResponse, MenuResponse,
  UserCreateRequest, SysRole, PermissionAssignRequest,
  SysUser, UserUpdateRequest, PermissionTreeNode,
} from '@/types/auth.d'

/** 登录 */
export function login(data: LoginRequest): Promise<LoginResponse> {
  return post<LoginResponse>('/api/v1/auth/login', data)
}

/** 刷新令牌 */
export function refresh(data: { refreshToken: string }): Promise<LoginResponse> {
  return post<LoginResponse>('/api/v1/auth/refresh', data)
}

/** 获取当前用户信息 */
export function getUserInfo(): Promise<UserInfoResponse> {
  return get<UserInfoResponse>('/api/v1/auth/user-info')
}

/** 获取当前用户菜单树 */
export function getMenus(): Promise<MenuResponse[]> {
  return get<MenuResponse[]>('/api/v1/auth/menus')
}

/** 创建用户 */
export function createUser(data: UserCreateRequest): Promise<number> {
  return post<number>('/api/v1/auth/users', data)
}

/** 获取角色列表 */
export function getRoles(): Promise<SysRole[]> {
  return get<SysRole[]>('/api/v1/auth/roles')
}

/** 为角色分配权限 */
export function assignPermissions(data: PermissionAssignRequest): Promise<void> {
  return post<void>('/api/v1/auth/permissions', data)
}

/** 用户列表（分页） */
export function getUsers(params: PageQuery & Record<string, unknown>): Promise<PageResult<SysUser>> {
  return get('/api/v1/auth/users', params as Record<string, unknown>)
}

/** 更新用户 */
export function updateUser(userId: number, data: UserUpdateRequest): Promise<void> {
  return put(`/api/v1/auth/users/${userId}`, data)
}

/** 重置用户密码 */
export function resetPassword(userId: number, password: string): Promise<void> {
  return put(`/api/v1/auth/users/${userId}/reset-password`, { password })
}

/** 获取权限树 */
export function getPermissionTree(): Promise<PermissionTreeNode[]> {
  return get('/api/v1/auth/permissions/tree')
}

/** 获取角色权限ID列表 */
export function getRolePermissions(roleId: number): Promise<number[]> {
  return get(`/api/v1/auth/roles/${roleId}/permissions`)
}
