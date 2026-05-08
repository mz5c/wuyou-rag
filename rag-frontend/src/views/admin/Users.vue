<template>
  <div class="users-page">
    <h2 class="page-title">用户管理</h2>

    <div class="section-header">
      <h3 class="section-title">用户列表</h3>
      <el-button type="primary" size="small" @click="openCreateDialog">新增用户</el-button>
    </div>

    <el-card shadow="never">
      <el-table :data="users" v-loading="loading" stripe style="width: 100%">
        <el-table-column prop="username" label="用户名" width="150" />
        <el-table-column prop="nickname" label="昵称" width="150" />
        <el-table-column prop="role" label="角色" width="120">
          <template #default="{ row }">
            <el-tag :type="row.role === 'ADMIN' ? 'danger' : 'info'" size="small">
              {{ row.role === 'ADMIN' ? '管理员' : '普通用户' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="注册时间" width="180" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" size="small" @click="openEditDialog(row)">
              编辑
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper" v-if="total > 0">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="size"
          :total="total"
          layout="prev, pager, next, total"
          @change="loadUsers"
        />
      </div>
    </el-card>

    <!-- Edit Dialog -->
    <el-dialog v-model="editDialogVisible" title="编辑用户" width="500px">
      <el-form :model="editForm" label-width="80px">
        <el-form-item label="用户名">
          <el-input v-model="editForm.username" disabled />
        </el-form-item>
        <el-form-item label="昵称">
          <el-input v-model="editForm.nickname" />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="editForm.role" style="width: 100%">
            <el-option label="管理员" value="ADMIN" />
            <el-option label="普通用户" value="USER" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-switch
            v-model="editForm.statusActive"
            active-text="启用"
            inactive-text="禁用"
          />
        </el-form-item>
        <el-form-item label="密码">
          <el-input
            v-model="editForm.password"
            type="password"
            placeholder="留空不修改"
            show-password
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSaveUser">
          保存
        </el-button>
      </template>
    </el-dialog>

    <!-- Create User Dialog -->
    <el-dialog v-model="createDialogVisible" title="新增用户" width="460px">
      <el-form :model="createForm" :rules="createRules" ref="createFormRef" label-position="top">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="createForm.username" placeholder="2-32位字母、数字或下划线" />
        </el-form-item>
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="createForm.nickname" placeholder="请输入昵称" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="createForm.password" type="password" show-password placeholder="6-32位密码" />
        </el-form-item>
        <el-form-item label="角色" prop="role">
          <el-select v-model="createForm.role" style="width:100%">
            <el-option label="普通用户" value="USER" />
            <el-option label="管理员" value="ADMIN" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="creating" @click="handleCreateUser">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
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
const createFormRef = ref(null)
const createForm = reactive({
  username: '',
  nickname: '',
  password: '',
  role: 'USER'
})
const createRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 2, max: 32, message: '用户名长度为2-32位', trigger: 'blur' },
    { pattern: /^[a-zA-Z0-9_]+$/, message: '用户名只能包含字母、数字和下划线', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 32, message: '密码长度为6-32位', trigger: 'blur' }
  ]
}

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
    ElMessage.success('用户信息已更新')
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

async function handleCreateUser() {
  const valid = await createFormRef.value.validate().catch(() => false)
  if (!valid) return
  creating.value = true
  try {
    await createUser({ ...createForm })
    ElMessage.success('用户已创建')
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
.pagination-wrapper { margin-top: 16px; display: flex; justify-content: flex-end; }
</style>
