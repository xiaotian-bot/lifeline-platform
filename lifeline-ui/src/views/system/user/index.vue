<template>
  <div class="user-page">
    <!-- 搜索栏 -->
    <div class="search-bar">
      <el-form :model="query" inline>
        <el-form-item label="用户名">
          <el-input v-model="query.username" placeholder="请输入用户名" clearable style="width: 150px" @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 100px">
            <el-option label="启用" :value="1" />
            <el-option label="禁用" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="query.roleId" placeholder="全部" clearable style="width: 130px">
            <el-option v-for="r in roleOptions" :key="r.roleId" :label="r.roleName" :value="r.roleId" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">搜索</el-button>
          <el-button :icon="Refresh" @click="reset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 表格 -->
    <div class="table-card">
      <div class="table-toolbar">
        <el-button type="primary" :icon="Plus" @click="openAddDialog">新增用户</el-button>
        <el-button :icon="Refresh" circle @click="fetch" />
      </div>

      <PageTable
        :columns="columns"
        :data="dataList"
        :total="total"
        :loading="loading"
        :page="query.page!"
        :size="query.size!"
        @page-change="handlePageChange"
        @size-change="handleSizeChange"
      >
        <template #status="{ row }">
          <el-switch
            :model-value="row.status === 1"
            @change="(val: boolean) => handleToggleStatus(row, val)"
            :disabled="row.username === 'admin'"
          />
        </template>
        <template #roleName="{ row }">
          <el-tag size="small" type="info">{{ row.roleName || '-' }}</el-tag>
        </template>
        <template #action="{ row }">
          <el-button link type="primary" size="small" @click="openEditDialog(row)">编辑</el-button>
          <el-button link type="warning" size="small" @click="handleResetPwd(row)">重置密码</el-button>
        </template>
      </PageTable>
    </div>

    <!-- 新增/编辑用户对话框 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑用户' : '新增用户'" width="520px" destroy-on-close>
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="80px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="formData.username" placeholder="请输入用户名" :disabled="isEdit" />
        </el-form-item>
        <el-form-item v-if="!isEdit" label="密码" prop="password">
          <el-input v-model="formData.password" type="password" placeholder="请输入密码" show-password />
        </el-form-item>
        <el-form-item label="真实姓名" prop="realName">
          <el-input v-model="formData.realName" placeholder="请输入真实姓名" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="formData.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="formData.email" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item label="角色" prop="roleId">
          <el-select v-model="formData.roleId" placeholder="请选择角色" style="width: 100%">
            <el-option v-for="r in roleOptions" :key="r.roleId" :label="r.roleName" :value="r.roleId" />
          </el-select>
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

    <!-- 重置密码对话框 -->
    <el-dialog v-model="resetPwdVisible" title="重置密码" width="400px" destroy-on-close>
      <el-form ref="resetFormRef" :model="resetPwdForm" :rules="resetPwdRules" label-width="80px">
        <el-form-item label="新密码" prop="password">
          <el-input v-model="resetPwdForm.password" type="password" placeholder="请输入新密码" show-password />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPwd">
          <el-input v-model="resetPwdForm.confirmPwd" type="password" placeholder="请确认新密码" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="resetPwdVisible = false">取消</el-button>
        <el-button type="primary" :loading="resetPwdLoading" @click="handleResetPwdSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { Search, Refresh, Plus } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox, type FormInstance } from 'element-plus'
import { useTable } from '@/composables/useTable'
import { getUsers, createUser, updateUser, resetPassword, getRoles } from '@/api/auth'
import type { SysUser, SysRole } from '@/types/auth.d'
import PageTable from '@/components/common/PageTable.vue'

// ==================== 角色选项 ====================
const roleOptions = ref<SysRole[]>([])

async function loadRoles() {
  try {
    roleOptions.value = await getRoles()
  } catch {
    // handled
  }
}

// ==================== 表格 ====================
const columns = [
  { prop: 'userId', label: '用户ID', width: 80 },
  { prop: 'username', label: '用户名', width: 120 },
  { prop: 'realName', label: '真实姓名', width: 120 },
  { prop: 'phone', label: '手机号', width: 130 },
  { prop: 'status', label: '状态', width: 90, align: 'center' as const, slot: 'status' },
  { prop: 'roleName', label: '角色', width: 120, slot: 'roleName' },
  { prop: 'createTime', label: '创建时间', minWidth: 160 },
  { prop: 'action', label: '操作', width: 160, fixed: 'right' as const, align: 'center' as const, slot: 'action' },
]

const { loading, dataList, total, query, fetch, reset, handlePageChange, handleSizeChange } = useTable<SysUser>({
  fetchApi: getUsers,
})

function handleSearch() {
  query.page = 1
  fetch()
}

// ==================== 启用/禁用 ====================
async function handleToggleStatus(row: SysUser, val: boolean) {
  try {
    await updateUser(row.userId, { status: val ? 1 : 0 })
    ElMessage.success(val ? '已启用' : '已禁用')
    fetch()
  } catch {
    // handled
  }
}

// ==================== 新增/编辑 ====================
const dialogVisible = ref(false)
const isEdit = ref(false)
const editingUserId = ref<number | null>(null)
const submitLoading = ref(false)
const formRef = ref<FormInstance>()

const defaultForm = () => ({
  username: '',
  password: '',
  realName: '',
  phone: '',
  email: '',
  roleId: undefined as number | undefined,
  status: 1,
})

const formData = reactive(defaultForm())

const formRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  realName: [{ required: true, message: '请输入真实姓名', trigger: 'blur' }],
  roleId: [{ required: true, message: '请选择角色', trigger: 'change' }],
}

function openAddDialog() {
  isEdit.value = false
  editingUserId.value = null
  Object.assign(formData, defaultForm())
  dialogVisible.value = true
}

function openEditDialog(row: SysUser) {
  isEdit.value = true
  editingUserId.value = row.userId
  Object.assign(formData, {
    username: row.username,
    password: '',
    realName: row.realName,
    phone: row.phone,
    email: row.email,
    roleId: row.roleId,
    status: row.status,
  })
  dialogVisible.value = true
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  submitLoading.value = true
  try {
    if (isEdit.value && editingUserId.value) {
      await updateUser(editingUserId.value, {
        realName: formData.realName,
        phone: formData.phone,
        email: formData.email,
        roleId: formData.roleId,
        status: formData.status,
      })
      ElMessage.success('更新成功')
    } else {
      await createUser({
        username: formData.username,
        password: formData.password,
        realName: formData.realName,
        phone: formData.phone,
        email: formData.email,
        roleId: formData.roleId!,
        status: formData.status,
      })
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    fetch()
  } catch {
    // handled
  } finally {
    submitLoading.value = false
  }
}

// ==================== 重置密码 ====================
const resetPwdVisible = ref(false)
const resetPwdLoading = ref(false)
const resetPwdForm = reactive({ userId: 0, password: '', confirmPwd: '' })
const resetFormRef = ref<FormInstance>()

const resetPwdRules = {
  password: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码至少6位', trigger: 'blur' },
  ],
  confirmPwd: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    {
      validator: (_rule: unknown, value: string, callback: (err?: Error) => void) => {
        if (value !== resetPwdForm.password) {
          callback(new Error('两次密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur',
    },
  ],
}

function handleResetPwd(row: SysUser) {
  resetPwdForm.userId = row.userId
  resetPwdForm.password = ''
  resetPwdForm.confirmPwd = ''
  resetPwdVisible.value = true
}

async function handleResetPwdSubmit() {
  const valid = await resetFormRef.value?.validate().catch(() => false)
  if (!valid) return

  resetPwdLoading.value = true
  try {
    await resetPassword(resetPwdForm.userId, resetPwdForm.password)
    ElMessage.success('密码重置成功')
    resetPwdVisible.value = false
  } catch {
    // handled
  } finally {
    resetPwdLoading.value = false
  }
}

// ==================== 初始化 ====================
onMounted(() => {
  loadRoles()
})
</script>

<style scoped lang="scss">
.user-page {
  padding: $spacing-md;
}

.search-bar {
  background: #fff;
  border-radius: $border-radius-lg;
  padding: $spacing-md $spacing-md 0;
  margin-bottom: $spacing-md;
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
</style>
