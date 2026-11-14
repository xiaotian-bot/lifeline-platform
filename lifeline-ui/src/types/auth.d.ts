/** 登录请求 - 匹配 LoginRequest.java */
export interface LoginRequest {
  username: string
  password: string
}

/** 登录响应 - 匹配 LoginResponse.java */
export interface LoginResponse {
  userId: number
  username: string
  realName: string
  roles: string[]
  accessToken: string
  refreshToken: string
  tokenType: string
  expiresIn: number
}

/** 用户信息响应 - 匹配 UserInfoResponse.java */
export interface UserInfoResponse {
  userId: number
  username: string
  realName: string
  phone: string
  email: string
  deptId: number
  status: number  // 0=禁用, 1=启用
  roles: string[]
  permissions: string[]
}

/** 菜单树节点 - 匹配 MenuResponse.java */
export interface MenuResponse {
  menuId: number
  parentId: number
  menuName: string
  menuType: string  // M=目录, C=菜单, F=按钮
  path: string
  permission: string
  icon: string
  sortOrder: number
  children: MenuResponse[]
}

/** 创建用户请求 - 匹配 UserCreateRequest.java */
export interface UserCreateRequest {
  username: string
  password: string
  realName?: string
  phone?: string
  email?: string
  roleId: number
  status?: number
}

/** 权限分配请求 - 匹配 PermissionAssignRequest.java */
export interface PermissionAssignRequest {
  roleId: number
  permissionIds: number[]
}

/** 角色 */
export interface SysRole {
  roleId: number
  roleName: string
  roleCode: string
  description: string
  status: number
  sortOrder?: number
  createTime?: string
}

/** 用户列表项 */
export interface SysUser {
  userId: number
  username: string
  realName: string
  phone: string
  email: string
  status: number
  roleId: number
  roleName: string
  createTime: string
}

/** 用户更新请求 */
export interface UserUpdateRequest {
  realName?: string
  phone?: string
  email?: string
  status?: number
  roleId?: number
}

/** 权限树节点 */
export interface PermissionTreeNode {
  menuId: number
  parentId: number
  menuName: string
  menuType: string
  permission: string
  icon: string
  sortOrder: number
  children: PermissionTreeNode[]
}

/** 操作日志 */
export interface OperationLog {
  logId: number
  userId: number
  username: string
  module: string
  action: string
  method: string
  params: string
  ip: string
  result: string
  errorMsg: string
  operateTime: string
}
