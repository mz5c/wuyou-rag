<template>
  <div class="config-page">
    <h2 class="page-title">系统配置</h2>

    <div class="table-card">
      <div v-if="loading" class="loading-state">
        <WLoading type="dots" />
      </div>
      <WTable v-else :columns="columns" :data="configs" empty-text="暂无配置">
        <template #configKey="{ row }">
          <code class="config-key">{{ row.configKey || row.key }}</code>
        </template>
        <template #configValue="{ row }">
          <WTextarea
            v-if="editingId === (row.id || row.configKey)"
            v-model="editValue"
            :rows="2"
          />
          <span v-else>{{ row.configValue || row.value }}</span>
        </template>
        <template #description="{ row }">
          {{ row.description }}
        </template>
        <template #actions="{ row }">
          <template v-if="editingId === (row.id || row.configKey)">
            <WButton variant="primary" size="small" :loading="saving" @click="saveConfig(row)">保存</WButton>
            <WButton size="small" @click="cancelEdit">取消</WButton>
          </template>
          <WButton v-else variant="text" size="small" @click="startEdit(row)">编辑</WButton>
        </template>
      </WTable>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { WMessage } from '../../components/ui/WMessage'
import { getConfigs, updateConfig } from '../../api/admin'

const configs = ref([])
const loading = ref(false)
const editingId = ref(null)
const editValue = ref('')
const saving = ref(false)

const columns = [
  { key: 'configKey', label: '配置键' },
  { key: 'configValue', label: '配置值' },
  { key: 'description', label: '说明' },
  { key: 'actions', label: '操作', width: '160px' }
]

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
    WMessage.success('配置已更新')
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
.config-page { max-width: 1200px; }
.page-title {
  font-size: var(--font-size-xl); color: var(--color-text); margin: 0 0 var(--space-lg) 0; font-weight: 600;
}
.table-card {
  background: var(--color-surface); border: 1px solid var(--color-border);
  border-radius: var(--radius-md); padding: var(--space-lg); box-shadow: var(--shadow-sm);
}
.loading-state { display: flex; justify-content: center; padding: 48px 0; }

.config-key {
  font-size: var(--font-size-sm); color: var(--color-primary-700); background: var(--color-primary-50);
  padding: 2px 6px; border-radius: var(--radius-sm); font-family: 'SF Mono', 'Fira Code', monospace;
}
</style>
