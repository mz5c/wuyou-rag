<template>
  <div class="dashboard">
    <h2 class="page-title">仪表盘</h2>
    <el-row :gutter="20">
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon" style="background: rgba(64,158,255,0.1); color: #409eff">
              <el-icon :size="28"><Collection /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.knowledgeBaseCount || 0 }}</div>
              <div class="stat-label">知识库数</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon" style="background: rgba(103,194,58,0.1); color: #67c23a">
              <el-icon :size="28"><Document /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.documentCount || 0 }}</div>
              <div class="stat-label">文档数</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon" style="background: rgba(230,162,60,0.1); color: #e6a23c">
              <el-icon :size="28"><ChatDotSquare /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.conversationCount || 0 }}</div>
              <div class="stat-label">问答数</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon" style="background: rgba(245,108,108,0.1); color: #f56c6c">
              <el-icon :size="28"><User /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.userCount || 0 }}</div>
              <div class="stat-label">用户数</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { Collection, Document, ChatDotSquare, User } from '@element-plus/icons-vue'
import { getAdminStats } from '../../api/admin'

const stats = ref({})

onMounted(async () => {
  try {
    const res = await getAdminStats()
    stats.value = res.data?.data || res.data || {}
  } catch {
    stats.value = {}
  }
})
</script>

<style scoped>
.dashboard { max-width: 1200px; }
.page-title {
  font-size: var(--font-size-xl); color: var(--color-text); margin: 0 0 var(--space-lg) 0; font-weight: 600;
}
.stat-card {
  margin-bottom: 20px; background: var(--color-surface); border: 1px solid var(--color-border);
  border-radius: var(--radius-md); box-shadow: var(--shadow-sm);
}
.stat-card :deep(.el-card__body) { padding: 20px; }
.stat-content { display: flex; align-items: center; gap: 16px; }
.stat-icon {
  width: 56px; height: 56px; border-radius: var(--radius-sm); display: flex;
  align-items: center; justify-content: center; flex-shrink: 0;
}
.stat-info { flex: 1; }
.stat-value { font-size: 28px; font-weight: 700; color: var(--color-text); line-height: 1.2; }
.stat-label { font-size: var(--font-size-sm); color: var(--color-text-secondary); margin-top: 4px; }
</style>
