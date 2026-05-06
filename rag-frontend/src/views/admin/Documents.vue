<template>
  <div class="documents-page">
    <h2 class="page-title">文档管理</h2>

    <!-- Toolbar -->
    <el-card class="toolbar-card" shadow="never">
      <el-row :gutter="16" align="middle">
        <el-col :span="6">
          <el-select
            v-model="selectedKbId"
            placeholder="选择知识库"
            style="width: 100%"
            @change="loadDocuments"
          >
            <el-option
              v-for="kb in knowledgeBases"
              :key="kb.id"
              :label="kb.name"
              :value="kb.id"
            />
          </el-select>
        </el-col>
        <el-col :span="18" style="text-align: right">
          <el-button type="primary" :icon="Upload" @click="showUploadDialog">
            上传文档
          </el-button>
        </el-col>
      </el-row>
    </el-card>

    <!-- Table -->
    <el-card shadow="never">
      <el-table
        :data="documents"
        v-loading="loading"
        stripe
        style="width: 100%"
      >
        <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip />
        <el-table-column prop="fileType" label="类型" width="100">
          <template #default="{ row }">
            <el-tag size="small" effect="plain">{{ row.fileType || row.type || '未知' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="fileSize" label="大小" width="120">
          <template #default="{ row }">
            {{ formatSize(row.fileSize || row.size) }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="120">
          <template #default="{ row }">
            <el-tag
              :type="statusType(row.status)"
              size="small"
            >
              {{ statusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="creatorName" label="创建人" width="120" />
        <el-table-column prop="createTime" label="时间" width="180" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button
              text
              type="primary"
              size="small"
              @click="handleReprocess(row)"
              :disabled="row.status === 'PROCESSING'"
            >
              重新处理
            </el-button>
            <el-button
              text
              type="danger"
              size="small"
              @click="handleDelete(row)"
            >
              删除
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
          @change="loadDocuments"
        />
      </div>
    </el-card>

    <!-- Upload Dialog -->
    <el-dialog v-model="uploadDialogVisible" title="上传文档" width="500px">
      <el-form label-width="80px">
        <el-form-item label="知识库">
          <el-select v-model="uploadKbId" placeholder="选择知识库" style="width: 100%">
            <el-option
              v-for="kb in knowledgeBases"
              :key="kb.id"
              :label="kb.name"
              :value="kb.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="文件">
          <el-upload
            ref="uploadRef"
            :auto-upload="false"
            :limit="1"
            :on-change="handleFileChange"
            :on-remove="() => { uploadFile = null }"
          >
            <el-button type="primary" plain>选择文件</el-button>
            <template #tip>
              <div style="font-size: 12px; color: #909399; margin-top: 4px">
                支持 PDF、DOCX、TXT、MD 等格式
              </div>
            </template>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="uploadDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="uploading" @click="handleUpload">
          上传
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { Upload } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listKnowledgeBases } from '../../api/knowledge'
import { uploadDocument, listDocuments, deleteDocument, reprocessDocument } from '../../api/document'

const knowledgeBases = ref([])
const documents = ref([])
const loading = ref(false)
const selectedKbId = ref('')
const page = ref(1)
const size = ref(20)
const total = ref(0)

// Upload dialog
const uploadDialogVisible = ref(false)
const uploadKbId = ref('')
const uploadFile = ref(null)
const uploadRef = ref(null)
const uploading = ref(false)

onMounted(async () => {
  try {
    const res = await listKnowledgeBases()
    const list = res.data?.data || res.data || []
    knowledgeBases.value = Array.isArray(list) ? list : []
    if (knowledgeBases.value.length > 0) {
      selectedKbId.value = knowledgeBases.value[0].id
      await loadDocuments()
    }
  } catch {
    knowledgeBases.value = []
  }
})

async function loadDocuments() {
  if (!selectedKbId.value) {
    documents.value = []
    return
  }
  loading.value = true
  try {
    const res = await listDocuments(selectedKbId.value, { page: page.value, size: size.value })
    const data = res.data?.data || res.data || {}
    documents.value = Array.isArray(data.records || data.list || data) ? (data.records || data.list || data) : []
    total.value = data.total || documents.value.length
  } catch {
    documents.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function showUploadDialog() {
  uploadKbId.value = selectedKbId.value
  uploadFile.value = null
  uploadDialogVisible.value = true
}

function handleFileChange(uploadFileObj) {
  uploadFile.value = uploadFileObj.raw
}

async function handleUpload() {
  if (!uploadKbId.value) {
    ElMessage.warning('请选择知识库')
    return
  }
  if (!uploadFile.value) {
    ElMessage.warning('请选择文件')
    return
  }

  uploading.value = true
  try {
    await uploadDocument(uploadKbId.value, uploadFile.value)
    ElMessage.success('上传成功')
    uploadDialogVisible.value = false
    await loadDocuments()
  } catch {
    // error handled by interceptor
  } finally {
    uploading.value = false
  }
}

async function handleReprocess(row) {
  try {
    await reprocessDocument(row.id)
    ElMessage.success('已重新加入处理队列')
    await loadDocuments()
  } catch {
    // error handled by interceptor
  }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`确定删除文档「${row.title}」？`, '提示', { type: 'warning' })
    await deleteDocument(row.id)
    ElMessage.success('已删除')
    await loadDocuments()
  } catch {
    // cancelled or error
  }
}

function formatSize(bytes) {
  if (!bytes) return '-'
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
}

function statusType(status) {
  const map = {
    COMPLETED: 'success',
    PROCESSING: 'warning',
    FAILED: 'danger',
    PENDING: 'info'
  }
  return map[status] || 'info'
}

function statusLabel(status) {
  const map = {
    COMPLETED: '已完成',
    PROCESSING: '处理中',
    FAILED: '失败',
    PENDING: '待处理'
  }
  return map[status] || status || '未知'
}
</script>

<style scoped>
.documents-page {
  max-width: 1200px;
}

.page-title {
  font-size: 20px;
  color: #303133;
  margin: 0 0 24px 0;
}

.toolbar-card {
  margin-bottom: 16px;
}

.pagination-wrapper {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
