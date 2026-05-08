<template>
  <div class="dashboard">
    <h2 class="page-title">仪表盘</h2>

    <div class="stat-grid">
      <div class="stat-card">
        <div class="stat-icon" style="background: linear-gradient(135deg, #eef2ff, #e0e7ff)">
          <WIcon name="dashboard" :size="22" color="#4f46e5" />
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.totalKnowledgeBases || 0 }}</div>
          <div class="stat-label">知识库数</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon" style="background: linear-gradient(135deg, #f0fdf4, #dcfce7)">
          <WIcon name="document" :size="22" color="#059669" />
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.totalDocuments || 0 }}</div>
          <div class="stat-label">文档数</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon" style="background: linear-gradient(135deg, #fff7ed, #ffedd5)">
          <WIcon name="chat" :size="22" color="#d97706" />
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.totalChats || 0 }}</div>
          <div class="stat-label">问答数</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon" style="background: linear-gradient(135deg, #fef2f2, #fee2e2)">
          <WIcon name="users" :size="22" color="#dc2626" />
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.totalUsers || 0 }}</div>
          <div class="stat-label">用户数</div>
        </div>
      </div>
    </div>

    <div class="recent-section">
      <h3 class="section-title">最近文档</h3>
      <WTable :columns="columns" :data="recentDocs" empty-text="暂无文档">
        <template #fileType="{ row }">
          <WTag :variant="tagVariant(row.fileType || row.type)">{{ row.fileType || row.type || '未知' }}</WTag>
        </template>
        <template #status="{ row }">
          <WTag :variant="statusVariant(row.status)">{{ statusLabel(row.status) }}</WTag>
        </template>
        <template #fileSize="{ row }">
          {{ formatSize(row.fileSize || row.size) }}
        </template>
      </WTable>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getAdminStats } from '../../api/admin'

const stats = ref({})
const recentDocs = ref([])

const columns = [
  { key: 'title', label: '标题' },
  { key: 'fileType', label: '类型', width: '100px' },
  { key: 'fileSize', label: '大小', width: '100px' },
  { key: 'status', label: '状态', width: '100px' },
  { key: 'createTime', label: '时间', width: '170px' }
]

onMounted(async () => {
  try {
    const res = await getAdminStats()
    const data = res.data?.data || res.data || {}
    stats.value = data
    recentDocs.value = Array.isArray(data.recentDocuments) ? data.recentDocuments : []
  } catch {
    stats.value = {}
  }
})

function formatSize(bytes) {
  if (!bytes) return '-'
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
}

function tagVariant(type) {
  return 'default'
}

function statusVariant(status) {
  const map = { 2: 'success', 1: 'warning', 3: 'danger', 0: 'info' }
  return map[status] || 'info'
}

function statusLabel(status) {
  const map = { 2: '已完成', 1: '处理中', 3: '失败', 0: '待处理' }
  return map[status] !== undefined ? map[status] : String(status || '未知')
}
</script>

<style scoped>
.dashboard { max-width: 1200px; }
.page-title {
  font-size: 20px; font-weight: 600; color: var(--color-text); margin: 0 0 24px 0;
}

.stat-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 28px;
}
.stat-card {
  background: var(--color-surface);
  border-radius: var(--radius-lg);
  padding: 20px;
  border: 1px solid var(--color-border-light);
  box-shadow: var(--shadow-sm);
  display: flex;
  align-items: center;
  gap: 14px;
}
.stat-icon {
  width: 48px; height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.stat-info { flex: 1; }
.stat-value { font-size: 26px; font-weight: 700; color: var(--color-text); margin: 0; line-height: 1.2; }
.stat-label { font-size: var(--font-size-sm); color: var(--color-text-muted); margin-top: 2px; }

.recent-section {
  background: var(--color-surface);
  border-radius: var(--radius-lg);
  border: 1px solid var(--color-border-light);
  box-shadow: var(--shadow-sm);
  overflow: hidden;
}
.section-title {
  font-size: var(--font-size-base);
  font-weight: 600;
  color: var(--color-text);
  margin: 0;
  padding: 16px 20px;
  border-bottom: 1px solid var(--color-border-light);
}
</style>
