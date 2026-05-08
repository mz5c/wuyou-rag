<template>
  <div class="w-table-wrapper">
    <table class="w-table">
      <thead>
        <tr>
          <th v-for="col in columns" :key="col.key" :style="col.width ? { width: col.width } : {}">
            {{ col.label }}
          </th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="(row, idx) in data" :key="row.id || idx">
          <td v-for="col in columns" :key="col.key">
            <slot :name="col.key" :row="row" :value="getValue(row, col.key)">
              {{ getValue(row, col.key) }}
            </slot>
          </td>
        </tr>
        <tr v-if="data.length === 0">
          <td :colspan="columns.length" class="w-table__empty">
            <WEmpty :title="emptyText" />
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<script setup>
defineProps({
  columns: { type: Array, required: true }, // [{ key, label, width }]
  data: { type: Array, default: () => [] },
  emptyText: { type: String, default: '暂无数据' }
})

function getValue(row, key) {
  return key.split('.').reduce((o, k) => (o != null ? o[k] : ''), row)
}
</script>

<style scoped>
.w-table-wrapper {
  overflow-x: auto;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-surface);
}
.w-table { width: 100%; border-collapse: collapse; }
.w-table th {
  text-align: left;
  padding: 12px 16px;
  font-size: 12px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.3px;
  color: var(--color-text-muted);
  background: var(--color-bg);
  border-bottom: 1px solid var(--color-border);
  white-space: nowrap;
}
.w-table td {
  padding: 12px 16px;
  font-size: var(--font-size-sm);
  color: var(--color-text);
  border-bottom: 1px solid var(--color-border-light);
}
.w-table tbody tr:last-child td { border-bottom: none; }
.w-table tbody tr:hover { background: var(--color-primary-50); }
.w-table__empty { text-align: center; padding: 40px 16px !important; }
</style>
