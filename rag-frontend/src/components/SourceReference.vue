<template>
  <div class="source-references">
    <div class="source-divider">
      <span>来源文档</span>
    </div>
    <div v-for="(source, index) in sources" :key="index" class="source-item">
      <div class="source-header" @click="toggleExpand(index)">
        <WIcon name="document" size="14" />
        <span class="source-title">{{ source.docTitle || source.documentTitle || '未知文档' }}</span>
        <WIcon :name="expandedIndex === index ? 'arrowDown' : 'arrowRight'" size="12" class="source-arrow" />
      </div>
      <div v-show="expandedIndex === index" class="source-body">
        <p class="source-content">{{ source.content || source.chunkContent || source.text || '' }}</p>
        <div v-if="source.score !== undefined" class="source-score">
          相关度: {{ (source.score * 100).toFixed(1) }}%
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'

defineProps({
  sources: {
    type: Array,
    default: () => []
  }
})

const expandedIndex = ref(-1)

function toggleExpand(index) {
  expandedIndex.value = expandedIndex.value === index ? -1 : index
}
</script>

<style scoped>
.source-references {
  margin-top: 4px;
}
.source-divider {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 8px 0;
  font-size: 12px;
  color: var(--color-text-muted);
}
.source-divider::before,
.source-divider::after {
  content: '';
  flex: 1;
  height: 1px;
  background: var(--color-border-light);
}

.source-item {
  border-radius: var(--radius-sm);
  overflow: hidden;
  margin-bottom: 4px;
  border: 1px solid var(--color-border-light);
  background: var(--color-bg);
}
.source-header {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 10px;
  cursor: pointer;
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
  transition: background var(--duration-fast);
}
.source-header:hover {
  background: var(--color-primary-50);
}
.source-title {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.source-arrow {
  color: var(--color-text-muted);
  flex-shrink: 0;
}
.source-body {
  padding: 8px 10px;
  border-top: 1px solid var(--color-border-light);
  background: var(--color-surface);
}
.source-content {
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
  line-height: 1.6;
  margin: 0;
}
.source-score {
  font-size: var(--font-size-xs);
  color: var(--color-text-muted);
  margin-top: 6px;
}
</style>
