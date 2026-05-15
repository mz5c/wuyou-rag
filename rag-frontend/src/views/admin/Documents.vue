<template>
  <div class="documents-page">
    <h2 class="page-title">文档管理</h2>

    <!-- KB Management -->
    <div class="kb-section">
      <div class="section-header">
        <h3 class="section-title">知识库管理</h3>
        <WButton variant="primary" size="small" @click="openKbDialog()">新建知识库</WButton>
      </div>
      <div v-if="kbLoading" class="loading-state">
        <WLoading type="dots" />
      </div>
      <WTable v-else :columns="kbColumns" :data="kbList" empty-text="暂无知识库">
        <template #actions="{ row }">
          <WButton variant="text" size="small" @click="openKbDialog(row)">编辑</WButton>
          <WButton variant="text" size="small" @click="handleDeleteKb(row)">删除</WButton>
        </template>
      </WTable>
    </div>

    <!-- KB Dialog -->
    <WDialog v-model="kbDialogVisible" :title="kbEditingId ? '编辑知识库' : '新建知识库'" width="480px">
      <div class="form-group">
        <label class="form-label">名称</label>
        <WInput v-model="kbForm.name" placeholder="请输入知识库名称" />
      </div>
      <div class="form-group">
        <label class="form-label">描述</label>
        <WTextarea v-model="kbForm.description" :rows="3" placeholder="请输入描述（选填）" />
      </div>
      <template #footer>
        <WButton @click="kbDialogVisible = false">取消</WButton>
        <WButton variant="primary" :loading="kbSaving" @click="handleSaveKb">保存</WButton>
      </template>
    </WDialog>

    <!-- Toolbar -->
    <div class="toolbar-card">
      <div class="toolbar-inner">
        <div class="toolbar-left">
          <WSelect v-model="selectedKbId" :options="kbOptions" placeholder="选择知识库" @change="loadDocuments" />
        </div>
        <div class="toolbar-right">
          <WButton variant="primary" @click="showUploadDialog">
            <WIcon name="plus" size="14" /> 上传文档
          </WButton>
        </div>
      </div>
    </div>

    <!-- Document Table -->
    <div class="table-card">
      <div v-if="loading" class="loading-state">
        <WLoading type="dots" />
      </div>
      <WTable v-else :columns="docColumns" :data="documents" empty-text="暂无文档">
        <template #fileType="{ row }">
          <WTag variant="default" size="small">{{ row.fileType || row.type || '未知' }}</WTag>
        </template>
        <template #fileSize="{ row }">
          {{ formatSize(row.fileSize || row.size) }}
        </template>
        <template #status="{ row }">
          <WTag :variant="statusType(row.status)" size="small">{{ statusLabel(row.status) }}</WTag>
        </template>
        <template #actions="{ row }">
          <WButton variant="text" size="small" :disabled="row.status === 1" @click="handleReprocess(row)">重新处理</WButton>
          <WButton variant="text" size="small" @click="handleDelete(row)">删除</WButton>
        </template>
      </WTable>

      <WPagination v-if="total > 0" v-model="page" :total="total" :page-size="size" @change="loadDocuments" />
    </div>

    <!-- Upload Dialog -->
    <WDialog v-model="uploadDialogVisible" title="上传文档" width="500px">
      <div class="form-group">
        <label class="form-label">知识库</label>
        <WSelect v-model="uploadKbId" :options="kbOptions" placeholder="选择知识库" />
      </div>
      <div class="form-group">
        <label class="form-label">文件</label>
        <div class="upload-area">
          <WButton variant="default" @click="triggerFileInput">
            <WIcon name="upload" size="14" /> 选择文件
          </WButton>
          <input type="file" ref="fileInputRef" class="file-input-hidden" accept=".pdf,.docx,.txt,.md" @change="handleFileChange" />
          <span v-if="uploadFile" class="upload-file-name">{{ uploadFile.name }}</span>
          <button v-if="uploadFile" type="button" class="upload-clear" @click="clearUploadFile">×</button>
          <div class="upload-tip">支持 PDF、DOCX、TXT、MD 等格式</div>
        </div>
      </div>
      <template #footer>
        <WButton @click="uploadDialogVisible = false">取消</WButton>
        <WButton variant="primary" :loading="uploading" @click="handleUpload">上传</WButton>
      </template>
    </WDialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { WMessage } from '../../components/ui/WMessage'
import { WMessageBox } from '../../components/ui/WMessageBox'
import { listKnowledgeBases, createKnowledgeBase, updateKnowledgeBase, deleteKnowledgeBase } from '../../api/knowledge'
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
const fileInputRef = ref(null)
const uploading = ref(false)

// KB management
const kbList = ref([])
const kbLoading = ref(false)
const kbDialogVisible = ref(false)
const kbEditingId = ref(null)
const kbSaving = ref(false)
const kbForm = reactive({ name: '', description: '' })

const kbColumns = [
  { key: 'name', label: '名称' },
  { key: 'description', label: '描述' },
  { key: 'createTime', label: '创建时间', width: '170px' },
  { key: 'actions', label: '操作', width: '140px' }
]

const docColumns = [
  { key: 'title', label: '标题' },
  { key: 'fileType', label: '类型', width: '100px' },
  { key: 'fileSize', label: '大小', width: '120px' },
  { key: 'status', label: '状态', width: '120px' },
  { key: 'creatorName', label: '创建人', width: '120px' },
  { key: 'createTime', label: '时间', width: '180px' },
  { key: 'actions', label: '操作', width: '200px' }
]

const kbOptions = computed(() =>
  knowledgeBases.value.map(kb => ({ value: kb.id, label: kb.name }))
)

onMounted(async () => {
  await loadKnowledgeBases()
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

function triggerFileInput() {
  fileInputRef.value?.click()
}

function handleFileChange(e) {
  const file = e.target.files[0]
  uploadFile.value = file || null
}

function clearUploadFile() {
  uploadFile.value = null
  if (fileInputRef.value) fileInputRef.value.value = ''
}

async function handleUpload() {
  if (!uploadKbId.value) {
    WMessage.warning('请选择知识库')
    return
  }
  if (!uploadFile.value) {
    WMessage.warning('请选择文件')
    return
  }

  uploading.value = true
  try {
    await uploadDocument(uploadKbId.value, uploadFile.value)
    WMessage.success('上传成功')
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
    WMessage.success('已重新加入处理队列')
    await loadDocuments()
  } catch {
    // error handled by interceptor
  }
}

async function handleDelete(row) {
  try {
    await WMessageBox({ title: '提示', message: `确定删除文档「${row.title}」？`, type: 'warning' })
    await deleteDocument(row.id)
    WMessage.success('已删除')
    await loadDocuments()
  } catch {
    // cancelled or error
  }
}

async function loadKnowledgeBases() {
  kbLoading.value = true
  try {
    const res = await listKnowledgeBases()
    kbList.value = res.data?.data || res.data || []
  } finally {
    kbLoading.value = false
  }
}

function openKbDialog(row) {
  if (row) {
    kbEditingId.value = row.id
    kbForm.name = row.name
    kbForm.description = row.description || ''
  } else {
    kbEditingId.value = null
    kbForm.name = ''
    kbForm.description = ''
  }
  kbDialogVisible.value = true
}

async function handleSaveKb() {
  if (!kbForm.name.trim()) {
    WMessage.warning('请输入知识库名称')
    return
  }
  kbSaving.value = true
  try {
    if (kbEditingId.value) {
      await updateKnowledgeBase(kbEditingId.value, { name: kbForm.name, description: kbForm.description })
      WMessage.success('知识库已更新')
    } else {
      await createKnowledgeBase({ name: kbForm.name, description: kbForm.description })
      WMessage.success('知识库已创建')
    }
    kbDialogVisible.value = false
    await loadKnowledgeBases()
  } finally {
    kbSaving.value = false
  }
}

async function handleDeleteKb(row) {
  try {
    await WMessageBox({ title: '确认删除', message: `确定删除知识库「${row.name}」吗？`, type: 'warning' })
    await deleteKnowledgeBase(row.id)
    WMessage.success('知识库已删除')
    await loadKnowledgeBases()
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
  const map = { 2: 'success', 1: 'warning', 3: 'danger', 0: 'info' }
  return map[status] || 'info'
}

function statusLabel(status) {
  const map = { 2: '已完成', 1: '处理中', 3: '失败', 0: '待处理' }
  return map[status] !== undefined ? map[status] : String(status || '未知')
}
</script>

<style scoped>
.documents-page { max-width: 1200px; }
.page-title {
  font-size: var(--font-size-xl); color: var(--color-text); margin: 0 0 var(--space-lg) 0; font-weight: 600;
}
.kb-section {
  margin-bottom: 24px; background: var(--color-surface); border: 1px solid var(--color-border);
  border-radius: var(--radius-md); padding: var(--space-lg); box-shadow: var(--shadow-sm);
}
.section-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.section-title { font-size: var(--font-size-lg); color: var(--color-text); margin: 0; font-weight: 600; }

.toolbar-card {
  margin-bottom: 16px; background: var(--color-surface); border: 1px solid var(--color-border);
  border-radius: var(--radius-md); padding: 16px 20px; box-shadow: var(--shadow-sm);
}
.toolbar-inner { display: flex; align-items: center; justify-content: space-between; gap: 16px; }
.toolbar-left { width: 240px; }
.toolbar-right { flex-shrink: 0; }

.table-card {
  background: var(--color-surface); border: 1px solid var(--color-border);
  border-radius: var(--radius-md); padding: var(--space-lg); box-shadow: var(--shadow-sm);
}

.loading-state { display: flex; justify-content: center; align-items: center; padding: 48px 0; gap: 8px; color: var(--color-text-muted); font-size: var(--font-size-sm); }

.form-group { margin-bottom: 16px; }
.form-label { display: block; font-size: var(--font-size-sm); font-weight: 500; color: var(--color-text); margin-bottom: 6px; }

.upload-area { display: flex; flex-wrap: wrap; align-items: center; gap: 8px; }
.file-input-hidden { display: none; }
.upload-file-name { font-size: var(--font-size-sm); color: var(--color-text-secondary); }
.upload-clear {
  border: none; background: none; cursor: pointer; padding: 0 4px;
  color: var(--color-text-muted); font-size: 16px; line-height: 1;
}
.upload-clear:hover { color: var(--color-danger); }
.upload-tip { width: 100%; font-size: 12px; color: var(--color-text-muted); }
</style>
