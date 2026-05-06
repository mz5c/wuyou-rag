<template>
  <div class="source-references">
    <el-divider content-position="left" style="margin: 8px 0">
      <span style="font-size: 12px; color: #909399">来源文档</span>
    </el-divider>
    <div v-for="(source, index) in sources" :key="index" class="source-item">
      <div class="source-header" @click="toggleExpand(index)">
        <el-icon style="margin-right: 4px">
          <Document />
        </el-icon>
        <span class="source-title">{{ source.docTitle || source.documentTitle || '未知文档' }}</span>
        <el-icon style="margin-left: auto; transition: transform 0.2s" :class="{ expanded: expandedIndex === index }">
          <ArrowDown />
        </el-icon>
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

.source-item {
  border-radius: 6px;
  overflow: hidden;
  margin-bottom: 4px;
  border: 1px solid #ebeef5;
  background-color: #fafafa;
}

.source-header {
  display: flex;
  align-items: center;
  padding: 6px 10px;
  cursor: pointer;
  font-size: 13px;
  color: #606266;
  transition: background-color 0.2s;
}

.source-header:hover {
  background-color: #f0f2f5;
}

.source-title {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.source-body {
  padding: 8px 10px;
  border-top: 1px solid #ebeef5;
  background-color: #fff;
}

.source-content {
  font-size: 13px;
  color: #606266;
  line-height: 1.6;
  margin: 0;
}

.source-score {
  font-size: 12px;
  color: #909399;
  margin-top: 6px;
}

.expanded {
  transform: rotate(180deg);
}
</style>
