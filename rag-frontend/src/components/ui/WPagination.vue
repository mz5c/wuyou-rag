<template>
  <div v-if="total > 0" class="w-pagination">
    <span class="w-pagination__total">共 {{ total }} 条</span>
    <div class="w-pagination__buttons">
      <button class="w-pagination__btn" type="button" :disabled="modelValue <= 1" @click="go(modelValue - 1)">
        <WIcon name="arrowLeft" size="14" />
      </button>
      <template v-for="p in pages" :key="p">
        <span v-if="p === '...'" class="w-pagination__ellipsis">…</span>
        <button
          v-else
          type="button"
          :class="['w-pagination__btn', 'w-pagination__num', { 'w-pagination__num--active': p === modelValue }]"
          @click="go(p)"
        >
          {{ p }}
        </button>
      </template>
      <button class="w-pagination__btn" type="button" :disabled="modelValue >= totalPages" @click="go(modelValue + 1)">
        <WIcon name="arrowRight" size="14" />
      </button>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  modelValue: { type: Number, default: 1 },
  total: { type: Number, default: 0 },
  pageSize: { type: Number, default: 20 }
})
const emit = defineEmits(['update:modelValue', 'change'])

const totalPages = computed(() => Math.max(1, Math.ceil(props.total / props.pageSize)))

const pages = computed(() => {
  const total = totalPages.value
  const current = props.modelValue
  const range = []
  if (total <= 7) {
    for (let i = 1; i <= total; i++) range.push(i)
  } else {
    range.push(1)
    if (current > 3) range.push('...')
    for (let i = Math.max(2, current - 1); i <= Math.min(total - 1, current + 1); i++) range.push(i)
    if (current < total - 2) range.push('...')
    range.push(total)
  }
  return range
})

function go(page) {
  if (page < 1 || page > totalPages.value || page === props.modelValue) return
  emit('update:modelValue', page)
  emit('change', page)
}
</script>

<style scoped>
.w-pagination {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 16px;
}
.w-pagination__total { font-size: var(--font-size-sm); color: var(--color-text-muted); }
.w-pagination__buttons { display: flex; align-items: center; gap: 4px; }
.w-pagination__btn {
  display: flex; align-items: center; justify-content: center;
  width: 32px; height: 32px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  background: var(--color-surface);
  color: var(--color-text-secondary);
  cursor: pointer;
  font-size: var(--font-size-sm);
  font-family: var(--font-body);
  transition: border-color var(--duration-fast),
              color var(--duration-fast),
              background var(--duration-fast);
}
.w-pagination__btn:hover:not(:disabled) { border-color: var(--color-primary-300); color: var(--color-primary-600); }
.w-pagination__btn:disabled { opacity: 0.4; cursor: not-allowed; }
.w-pagination__num--active { background: var(--color-primary-600); color: #fff; border-color: var(--color-primary-600); }
.w-pagination__num--active:hover { background: var(--color-primary-700) !important; color: #fff !important; }
.w-pagination__ellipsis { color: var(--color-text-muted); padding: 0 4px; }
</style>
