import type { Permission } from '../services/role.service'

let permissionList: Permission[] = []

export function setPermissionStore(permissions: Permission[]) {
  permissionList = permissions
}

export function clearPermissionStore() {
  permissionList = []
}

export function checkPermission(code: string) {
  return permissionList.some((permission) => permission.code === code)
}
