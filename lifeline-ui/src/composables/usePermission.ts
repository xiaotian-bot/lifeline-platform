import { computed } from 'vue'
import { usePermissionStore } from '@/stores/permission'

export function usePermission() {
  const permissionStore = usePermissionStore()

  /** 检查是否有某个按钮权限 */
  function hasPermission(code: string): boolean {
    return permissionStore.buttonPermissions.includes(code)
  }

  /** 检查是否有任一权限 */
  function hasAnyPermission(codes: string[]): boolean {
    return codes.some((code) => permissionStore.buttonPermissions.includes(code))
  }

  /** 检查是否有所有权限 */
  function hasAllPermissions(codes: string[]): boolean {
    return codes.every((code) => permissionStore.buttonPermissions.includes(code))
  }

  /** 角色判断 */
  const isAdmin = computed(() => permissionStore.menuTree.length > 0)

  return {
    hasPermission,
    hasAnyPermission,
    hasAllPermissions,
    isAdmin,
  }
}
