<template>
  <div class="audit-page">
    <h2 class="page-title">审计日志</h2>

    <!-- Filter Bar -->
    <el-card class="filter-card" shadow="never">
      <el-row :gutter="16" align="middle">
        <el-col :span="6">
          <el-select v-model="filter.operation" placeholder="操作类型" clearable style="width: 100%">
            <el-option label="登录" value="LOGIN" />
            <el-option label="登出" value="LOGOUT" />
            <el-option label="上传文档" value="UPLOAD_DOCUMENT" />
            <el-option label="删除文档" value="DELETE_DOCUMENT" />
            <el-option label="创建知识库" value="CREATE_KNOWLEDGE" />
            <el-option label="删除知识库" value="DELETE_KNOWLEDGE" />
            <el-option label="配置修改" value="UPDATE_CONFIG" />
            <el-option label="用户管理" value="USER_MANAGE" />
          </el-select>
        </el-col>
        <el-col :span="8">
          <el-date-picker
            v-model="filter.dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            style="width: 100%"
            value-format="YYYY-MM-DD"
          />
        </el-col>
        <el-col :span="4">
          <el-button type="primary" @click="loadLogs">查询</el-button>
          <el-button @click="resetFilter">重置</el-button>
        </el-col>
      </el-row>
    </el-card>

    <!-- Table -->
    <el-card shadow="never">
      <el-table :data="logs" v-loading="loading" stripe style="width: 100%">
        <el-table-column prop="username" label="用户" width="140" />
        <el-table-column prop="operation" label="操作" width="160">
          <template #default="{ row }">
            <el-tag size="small" effect="plain">{{ operationLabel(row.operation) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="detail" label="详情" min-width="300" show-overflow-tooltip />
        <el-table-column prop="ip" label="IP" width="140" />
        <el-table-column prop="createTime" label="时间" width="180" />
      </el-table>

      <div class="pagination-wrapper" v-if="total > 0">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="size"
          :total="total"
          layout="prev, pager, next, total"
          @change="loadLogs"
        />
      </div>
    </el-card>
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
  dateRange: null
})

onMounted(() => {
  loadLogs()
})

function resetFilter() {
  filter.operation = ''
  filter.dateRange = null
  page.value = 1
  loadLogs()
}

async function loadLogs() {
  loading.value = true
  try {
    const params = { page: page.value, size: size.value }
    if (filter.operation) params.operation = filter.operation
    if (filter.dateRange) {
      params.startDate = filter.dateRange[0]
      params.endDate = filter.dateRange[1]
    }
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
  border-radius: var(--radius-md); box-shadow: var(--shadow-sm);
}
.pagination-wrapper { margin-top: 16px; display: flex; justify-content: flex-end; }
</style>
