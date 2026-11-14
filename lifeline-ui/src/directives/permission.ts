import type { Directive, DirectiveBinding } from 'vue'
import { usePermissionStore } from '@/stores/permission'

export const vPermission: Directive = {
  mounted(el: HTMLElement, binding: DirectiveBinding<string | string[]>) {
    checkPermission(el, binding)
  },
  updated(el: HTMLElement, binding: DirectiveBinding<string | string[]>) {
    checkPermission(el, binding)
  },
}

function checkPermission(el: HTMLElement, binding: DirectiveBinding<string | string[]>) {
  const { value } = binding
  const permissionStore = usePermissionStore()
  const permissions = permissionStore.buttonPermissions

  if (value && typeof value === 'string') {
    if (!permissions.includes(value)) {
      el.parentNode?.removeChild(el)
    }
  }

  if (value && Array.isArray(value)) {
    const hasPermission = value.some((perm) => permissions.includes(perm))
    if (!hasPermission) {
      el.parentNode?.removeChild(el)
    }
  }
}

export default vPermission
