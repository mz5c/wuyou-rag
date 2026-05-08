<template>
  <div class="audit-page">
    <h2 class="page-title">审计日志</h2>

    <!-- Filter Bar -->
    <div class="filter-card">
      <div class="filter-bar">
        <div class="filter-item">
          <WSelect v-model="filter.operation" :options="operationOptions" placeholder="操作类型" />
        </div>
        <div class="filter-item date-range">
          <input type="date" v-model="filter.startDate" class="date-input" placeholder="开始日期" />
          <span class="date-sep">至</span>
          <input type="date" v-model="filter.endDate" class="date-input" placeholder="结束日期" />
        </div>
        <div class="filter-actions">
          <WButton variant="primary" @click="loadLogs">查询</WButton>
          <WButton @click="resetFilter">重置</WButton>
        </div>
      </div>
    </div>

    <!-- Table -->
    <div class="table-card">
      <div v-if="loading" class="loading-state">
        <WLoading type="dots" />
      </div>
      <WTable v-else :columns="columns" :data="logs" empty-text="暂无日志">
        <template #operation="{ row }">
          <WTag variant="default" size="small">{{ operationLabel(row.operation) }}</WTag>
        </template>
      </WTable>

      <WPagination v-if="total > 0" v-model="page" :total="total" :page-size="size" @change="loadLogs" />
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getAuditLogs } from '../../api/admin'

const logs = ref([])
const loading = ref(false)
const page = ref(1)
const size = ref(20)
const total = ref(0)

const filter = reactive({
  operation: '',
  startDate: '',
  endDate: ''
})

const columns = [
  { key: 'username', label: '用户', width: '140px' },
  { key: 'operation', label: '操作', width: '160px' },
  { key: 'detail', label: '详情' },
  { key: 'ip', label: 'IP', width: '140px' },
  { key: 'createTime', label: '时间', width: '180px' }
]

const operationOptions = [
  { value: 'LOGIN', label: '登录' },
  { value: 'LOGOUT', label: '登出' },
  { value: 'UPLOAD_DOCUMENT', label: '上传文档' },
  { value: 'DELETE_DOCUMENT', label: '删除文档' },
  { value: 'CREATE_KNOWLEDGE', label: '创建知识库' },
  { value: 'DELETE_KNOWLEDGE', label: '删除知识库' },
  { value: 'UPDATE_CONFIG', label: '配置修改' },
  { value: 'USER_MANAGE', label: '用户管理' }
]

onMounted(() => {
  loadLogs()
})

function resetFilter() {
  filter.operation = ''
  filter.startDate = ''
  filter.endDate = ''
  page.value = 1
  loadLogs()
}

async function loadLogs() {
  loading.value = true
  try {
    const params = { page: page.value, size: size.value }
    if (filter.operation) params.operation = filter.operation
    if (filter.startDate) params.startDate = filter.startDate
    if (filter.endDate) params.endDate = filter.endDate
    const res = await getAuditLogs(params)
    const data = res.data?.data || res.data || {}
    logs.value = Array.isArray(data.records || data.list || data) ? (data.records || data.list || data) : []
    total.value = data.total || logs.value.length
  } catch {
    logs.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

const operationLabels = {
  LOGIN: '登录',
  LOGOUT: '登出',
  UPLOAD_DOCUMENT: '上传文档',
  DELETE_DOCUMENT: '删除文档',
  CREATE_KNOWLEDGE: '创建知识库',
  DELETE_KNOWLEDGE: '删除知识库',
  UPDATE_CONFIG: '配置修改',
  USER_MANAGE: '用户管理'
}

function operationLabel(op) {
  return operationLabels[op] || op || '未知'
}
</script>

<style scoped>
.audit-page { max-width: 1200px; }
.page-title {
  font-size: var(--font-size-xl); color: var(--color-text); margin: 0 0 var(--space-lg) 0; font-weight: 600;
}
.filter-card {
  margin-bottom: 16px; background: var(--color-surface); border: 1px solid var(--color-border);
  border-radius: var(--radius-md); padding: 16px 20px; box-shadow: var(--shadow-sm);
}
.filter-bar { display: flex; align-items: center; flex-wrap: wrap; gap: 12px; }
.filter-item { min-width: 180px; flex: 1; }
.date-range { display: flex; align-items: center; gap: 8px; min-width: 240px; }
.date-input {
  flex: 1;
  padding: 9px 12px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  font-size: var(--font-size-sm);
  font-family: var(--font-body);
  color: var(--color-text);
  background: var(--color-surface);
  outline: none;
  transition: border-color var(--duration-fast) var(--ease-out);
}
.date-input:focus { border-color: var(--color-primary-500); box-shadow: 0 0 0 3px rgba(13,148,136,0.1); }
.date-sep { color: var(--color-text-muted); font-size: var(--font-size-sm); flex-shrink: 0; }
.filter-actions { display: flex; gap: 8px; flex-shrink: 0; }

.table-card {
  background: var(--color-surface); border: 1px solid var(--color-border);
  border-radius: var(--radius-md); padding: var(--space-lg); box-shadow: var(--shadow-sm);
}
.loading-state { display: flex; justify-content: center; padding: 48px 0; }
</style>
