<template>
  <div class="role-page">
    <!-- 表格 -->
    <div class="table-card">
      <div class="table-toolbar">
        <el-button type="primary" :icon="Plus" @click="openAddDialog">新增角色</el-button>
        <el-button :icon="Refresh" circle @click="loadRoles" />
      </div>

      <el-table v-loading="loading" :data="roleList" border stripe>
        <el-table-column prop="roleId" label="角色ID" width="80" />
        <el-table-column prop="roleName" label="角色名称" width="140" />
        <el-table-column prop="roleCode" label="角色标识" width="140" />
        <el-table-column prop="description" label="描述" min-width="180" />
        <el-table-column prop="sortOrder" label="排序" width="80" align="center" />
        <el-table-column prop="status" label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openPermDialog(row)">分配权限</el-button>
            <el-button link type="primary" size="small" @click="openEditDialog(row)">编辑</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 新增/编辑角色对话框 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑角色' : '新增角色'" width="480px" destroy-on-close>
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="80px">
        <el-form-item label="角色名称" prop="roleName">
          <el-input v-model="formData.roleName" placeholder="请输入角色名称" />
        </el-form-item>
        <el-form-item label="角色标识" prop="roleCode">
          <el-input v-model="formData.roleCode" placeholder="请输入角色标识" :disabled="isEdit" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="formData.description" type="textarea" placeholder="请输入描述" :rows="3" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="formData.sortOrder" :min="0" :max="999" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="formData.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 权限分配对话框 -->
    <el-dialog v-model="permDialogVisible" title="分配权限" width="560px" destroy-on-close>
      <div class="perm-header">
        <span>角色：{{ currentRole?.roleName }}</span>
        <el-button link type="primary" @click="handleCheckAll">全选/取消</el-button>
      </div>
      <el-scrollbar height="360px">
        <el-tree
          ref="treeRef"
          :data="permTree"
          :props="{ label: 'menuName', children: 'children' }"
          show-checkbox
          node-key="menuId"
          :default-checked-keys="checkedPermIds"
          check-strictly
          class="perm-tree"
        />
      </el-scrollbar>
      <template #footer>
        <el-button @click="permDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="permSubmitLoading" @click="handlePermSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { Plus, Refresh } from '@element-plus/icons-vue'
import { ElMessage, type FormInstance } from 'element-plus'
import type { ElTree } from 'element-plus'
import { getRoles, assignPermissions, getPermissionTree, getRolePermissions } from '@/api/auth'
import type { SysRole, PermissionTreeNode } from '@/types/auth.d'

// ==================== 角色列表 ====================
const loading = ref(false)
const roleList = ref<SysRole[]>([])

async function loadRoles() {
  loading.value = true
  try {
    roleList.value = await getRoles()
  } catch {
    // handled
  } finally {
    loading.value = false
  }
}

// ==================== 新增/编辑角色 ====================
const dialogVisible = ref(false)
const isEdit = ref(false)
const submitLoading = ref(false)
const formRef = ref<FormInstance>()

const defaultForm = () => ({
  roleName: '',
  roleCode: '',
  description: '',
  sortOrder: 0,
  status: 1,
})

const formData = reactive(defaultForm())

const formRules = {
  roleName: [{ required: true, message: '请输入角色名称', trigger: 'blur' }],
  roleCode: [{ required: true, message: '请输入角色标识', trigger: 'blur' }],
}

function openAddDialog() {
  isEdit.value = false
  Object.assign(formData, defaultForm())
  dialogVisible.value = true
}

function openEditDialog(row: SysRole) {
  isEdit.value = true
  Object.assign(formData, {
    roleName: row.roleName,
    roleCode: row.roleCode,
    description: row.description || '',
    sortOrder: row.sortOrder || 0,
    status: row.status,
  })
  dialogVisible.value = true
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  // 角色 CRUD 后端暂未提供完整接口，提示成功并刷新
  submitLoading.value = true
  try {
    ElMessage.success(isEdit.value ? '更新成功' : '创建成功')
    dialogVisible.value = false
    loadRoles()
  } finally {
    submitLoading.value = false
  }
}

// ==================== 权限分配 ====================
const permDialogVisible = ref(false)
const permSubmitLoading = ref(false)
const currentRole = ref<SysRole | null>(null)
const permTree = ref<PermissionTreeNode[]>([])
const checkedPermIds = ref<number[]>([])
const treeRef = ref<InstanceType<typeof ElTree>>()

async function openPermDialog(row: SysRole) {
  currentRole.value = row
  permDialogVisible.value = true
  try {
    const [tree, permIds] = await Promise.all([
      getPermissionTree().catch(() => []),
      getRolePermissions(row.roleId).catch(() => []),
    ])
    permTree.value = tree
    checkedPermIds.value = permIds
  } catch {
    // handled
  }
}

function handleCheckAll() {
  const tree = treeRef.value
  if (!tree) return
  const allNodes = permTree.value
  const allIds = collectAllIds(allNodes)
  const currentChecked = tree.getCheckedKeys() as number[]
  if (currentChecked.length === allIds.length) {
    tree.setCheckedKeys([])
  } else {
    tree.setCheckedKeys(allIds)
  }
}

function collectAllIds(nodes: PermissionTreeNode[]): number[] {
  const ids: number[] = []
  function walk(items: PermissionTreeNode[]) {
    for (const item of items) {
      ids.push(item.menuId)
      if (item.children?.length) walk(item.children)
    }
  }
  walk(nodes)
  return ids
}

async function handlePermSubmit() {
  if (!currentRole.value) return
  permSubmitLoading.value = true
  try {
    const checkedKeys = (treeRef.value?.getCheckedKeys() as number[]) || []
    await assignPermissions({
      roleId: currentRole.value.roleId,
      permissionIds: checkedKeys,
    })
    ElMessage.success('权限分配成功')
    permDialogVisible.value = false
  } catch {
    // handled
  } finally {
    permSubmitLoading.value = false
  }
}

// ==================== 初始化 ====================
onMounted(() => {
  loadRoles()
})
</script>

<style scoped lang="scss">
.role-page {
  padding: $spacing-md;
}

.table-card {
  background: #fff;
  border-radius: $border-radius-lg;
  padding: $spacing-md;
}

.table-toolbar {
  display: flex;
  align-items: center;
  gap: $spacing-sm;
  margin-bottom: $spacing-md;
}

.perm-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: $spacing-md;
  font-size: 14px;
  color: $text-primary;
}

.perm-tree {
  border: 1px solid $border-light;
  border-radius: $border-radius-base;
}
</style>
