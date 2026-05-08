<template>
  <div class="users-page">
    <h2 class="page-title">用户管理</h2>

    <div class="section-header">
      <h3 class="section-title">用户列表</h3>
      <WButton variant="primary" size="small" @click="openCreateDialog">新增用户</WButton>
    </div>

    <div class="table-card">
      <div v-if="loading" class="loading-state">
        <WLoading type="dots" />
      </div>
      <WTable v-else :columns="columns" :data="users" empty-text="暂无用户">
        <template #role="{ row }">
          <WTag :variant="row.role === 'ADMIN' ? 'danger' : 'info'" size="small">
            {{ row.role === 'ADMIN' ? '管理员' : '普通用户' }}
          </WTag>
        </template>
        <template #status="{ row }">
          <WTag :variant="row.status === 1 ? 'success' : 'info'" size="small">
            {{ row.status === 1 ? '启用' : '禁用' }}
          </WTag>
        </template>
        <template #actions="{ row }">
          <WButton variant="text" size="small" @click="openEditDialog(row)">编辑</WButton>
        </template>
      </WTable>

      <WPagination v-if="total > 0" v-model="page" :total="total" :page-size="size" @change="loadUsers" />
    </div>

    <!-- Edit Dialog -->
    <WDialog v-model="editDialogVisible" title="编辑用户" width="500px">
      <div class="form-group">
        <label class="form-label">用户名</label>
        <WInput v-model="editForm.username" disabled />
      </div>
      <div class="form-group">
        <label class="form-label">昵称</label>
        <WInput v-model="editForm.nickname" />
      </div>
      <div class="form-group">
        <label class="form-label">角色</label>
        <WSelect v-model="editForm.role" :options="roleOptions" />
      </div>
      <div class="form-group">
        <label class="form-label">状态</label>
        <div class="switch-row">
          <WSwitch v-model="editForm.statusActive" />
          <span class="switch-label">{{ editForm.statusActive ? '启用' : '禁用' }}</span>
        </div>
      </div>
      <div class="form-group">
        <label class="form-label">密码</label>
        <WInput v-model="editForm.password" type="password" placeholder="留空不修改" />
      </div>
      <template #footer>
        <WButton @click="editDialogVisible = false">取消</WButton>
        <WButton variant="primary" :loading="saving" @click="handleSaveUser">保存</WButton>
      </template>
    </WDialog>

    <!-- Create User Dialog -->
    <WDialog v-model="createDialogVisible" title="新增用户" width="460px">
      <div class="form-group">
        <label class="form-label">用户名</label>
        <WInput v-model="createForm.username" placeholder="2-32位字母、数字或下划线" />
      </div>
      <div class="form-group">
        <label class="form-label">昵称</label>
        <WInput v-model="createForm.nickname" placeholder="请输入昵称" />
      </div>
      <div class="form-group">
        <label class="form-label">密码</label>
        <WInput v-model="createForm.password" type="password" placeholder="6-32位密码" />
      </div>
      <div class="form-group">
        <label class="form-label">角色</label>
        <WSelect v-model="createForm.role" :options="roleOptions" />
      </div>
      <template #footer>
        <WButton @click="createDialogVisible = false">取消</WButton>
        <WButton variant="primary" :loading="creating" @click="handleCreateUser">创建</WButton>
      </template>
    </WDialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { WMessage } from '../../components/ui/WMessage'
import { getUsers, updateUser, createUser } from '../../api/admin'

const users = ref([])
const loading = ref(false)
const page = ref(1)
const size = ref(20)
const total = ref(0)

const editDialogVisible = ref(false)
const saving = ref(false)
const editForm = reactive({
  id: null,
  username: '',
  nickname: '',
  role: 'USER',
  statusActive: true,
  password: ''
})

const createDialogVisible = ref(false)
const creating = ref(false)
const createForm = reactive({
  username: '',
  nickname: '',
  password: '',
  role: 'USER'
})

const columns = [
  { key: 'username', label: '用户名', width: '150px' },
  { key: 'nickname', label: '昵称', width: '150px' },
  { key: 'role', label: '角色', width: '120px' },
  { key: 'status', label: '状态', width: '100px' },
  { key: 'createTime', label: '注册时间', width: '180px' },
  { key: 'actions', label: '操作', width: '200px' }
]

const roleOptions = [
  { value: 'USER', label: '普通用户' },
  { value: 'ADMIN', label: '管理员' }
]

onMounted(() => {
  loadUsers()
})

async function loadUsers() {
  loading.value = true
  try {
    const res = await getUsers({ page: page.value, size: size.value })
    const data = res.data?.data || res.data || {}
    users.value = Array.isArray(data.records || data.list || data) ? (data.records || data.list || data) : []
    total.value = data.total || users.value.length
  } catch {
    users.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function openEditDialog(row) {
  editForm.id = row.id
  editForm.username = row.username
  editForm.nickname = row.nickname || ''
  editForm.role = row.role || 'USER'
  editForm.statusActive = row.status === 1
  editForm.password = ''
  editDialogVisible.value = true
}

async function handleSaveUser() {
  saving.value = true
  try {
    const data = {
      nickname: editForm.nickname,
      role: editForm.role,
      status: editForm.statusActive ? 1 : 0
    }
    if (editForm.password) {
      data.password = editForm.password
    }
    await updateUser(editForm.id, data)
    WMessage.success('用户信息已更新')
    editDialogVisible.value = false
    await loadUsers()
  } catch {
    // error handled by interceptor
  } finally {
    saving.value = false
  }
}

function openCreateDialog() {
  createForm.username = ''
  createForm.nickname = ''
  createForm.password = ''
  createForm.role = 'USER'
  createDialogVisible.value = true
}

function validateCreateForm() {
  if (!createForm.username.trim()) { WMessage.warning('请输入用户名'); return false }
  if (!/^[a-zA-Z0-9_]{2,32}$/.test(createForm.username)) { WMessage.warning('用户名长度为2-32位，只能包含字母、数字和下划线'); return false }
  if (!createForm.password) { WMessage.warning('请输入密码'); return false }
  if (createForm.password.length < 6 || createForm.password.length > 32) { WMessage.warning('密码长度为6-32位'); return false }
  if (!createForm.nickname.trim()) { WMessage.warning('请输入昵称'); return false }
  return true
}

async function handleCreateUser() {
  if (!validateCreateForm()) return
  creating.value = true
  try {
    await createUser({ ...createForm })
    WMessage.success('用户已创建')
    createDialogVisible.value = false
    await loadUsers()
  } catch {
    // error handled by interceptor
  } finally {
    creating.value = false
  }
}
</script>

<style scoped>
.users-page { max-width: 1200px; }
.page-title {
  font-size: var(--font-size-xl); color: var(--color-text); margin: 0 0 var(--space-lg) 0; font-weight: 600;
}
.section-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.section-title { font-size: var(--font-size-lg); color: var(--color-text); margin: 0; font-weight: 600; }

.table-card {
  background: var(--color-surface); border: 1px solid var(--color-border);
  border-radius: var(--radius-md); padding: var(--space-lg); box-shadow: var(--shadow-sm);
}
.loading-state { display: flex; justify-content: center; padding: 48px 0; }

.form-group { margin-bottom: 16px; }
.form-label { display: block; font-size: var(--font-size-sm); font-weight: 500; color: var(--color-text); margin-bottom: 6px; }

.switch-row { display: flex; align-items: center; gap: 10px; }
.switch-label { font-size: var(--font-size-sm); color: var(--color-text-secondary); }
</style>
