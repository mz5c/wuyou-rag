<template>
  <div class="config-page">
    <h2 class="page-title">系统配置</h2>

    <el-card shadow="never">
      <el-table :data="configs" v-loading="loading" stripe style="width: 100%">
        <el-table-column prop="configKey" label="配置键" min-width="200">
          <template #default="{ row }">
            <code style="font-size: 13px; color: #409eff">{{ row.configKey || row.key }}</code>
          </template>
        </el-table-column>
        <el-table-column label="配置值" min-width="300">
          <template #default="{ row }">
            <el-input
              v-if="editingId === (row.id || row.configKey)"
              v-model="editValue"
              type="textarea"
              :rows="2"
              size="small"
            />
            <span v-else>{{ row.configValue || row.value }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="说明" min-width="200" show-overflow-tooltip />
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <template v-if="editingId === (row.id || row.configKey)">
              <el-button type="primary" size="small" :loading="saving" @click="saveConfig(row)">
                保存
              </el-button>
              <el-button size="small" @click="cancelEdit">取消</el-button>
            </template>
            <el-button
              v-else
              text
              type="primary"
              size="small"
              @click="startEdit(row)"
            >
              编辑
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getConfigs, updateConfig } from '../../api/admin'

const configs = ref([])
const loading = ref(false)
const editingId = ref(null)
const editValue = ref('')
const saving = ref(false)

onMounted(() => {
  loadConfigs()
})

async function loadConfigs() {
  loading.value = true
  try {
    const res = await getConfigs()
    const list = res.data?.data || res.data || []
    configs.value = Array.isArray(list) ? list : []
  } catch {
    configs.value = []
  } finally {
    loading.value = false
  }
}

function startEdit(row) {
  editingId.value = row.id || row.configKey
  editValue.value = row.configValue || row.value || ''
}

function cancelEdit() {
  editingId.value = null
  editValue.value = ''
}

async function saveConfig(row) {
  saving.value = true
  try {
    await updateConfig({
      id: row.id,
      configKey: row.configKey || row.key,
      configValue: editValue.value,
      description: row.description
    })
    ElMessage.success('配置已更新')
    editingId.value = null
    await loadConfigs()
  } catch {
    // error handled by interceptor
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.config-page {
  max-width: 1200px;
}

.page-title {
  font-size: 20px;
  color: #303133;
  margin: 0 0 24px 0;
}
</style>
