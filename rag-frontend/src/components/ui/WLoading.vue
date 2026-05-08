<template>
  <div :class="['w-loading', `w-loading--${type}`, { 'w-loading--inline': inline }]">
    <WIcon v-if="type === 'spinner'" name="loading" class="w-loading__spin" />
    <div v-else class="w-loading__dots">
      <span class="w-loading__dot" />
      <span class="w-loading__dot" />
      <span class="w-loading__dot" />
    </div>
    <span v-if="text" class="w-loading__text">{{ text }}</span>
  </div>
</template>

<script setup>
defineProps({
  type: { type: String, default: 'spinner', validator: v => ['spinner', 'dots'].includes(v) },
  text: String,
  inline: Boolean
})
</script>

<style scoped>
.w-loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  color: var(--color-primary-600);
  padding: 32px;
}
.w-loading--inline {
  flex-direction: row;
  padding: 0;
}
.w-loading__spin { animation: w-loading-spin 0.8s linear infinite; }
@keyframes w-loading-spin { to { transform: rotate(360deg); } }
.w-loading__dots { display: flex; gap: 6px; }
.w-loading__dot {
  width: 8px; height: 8px;
  border-radius: 50%;
  background: var(--color-primary-400);
  animation: w-loading-dot 1s ease-in-out infinite;
}
.w-loading__dot:nth-child(2) { animation-delay: 0.15s; }
.w-loading__dot:nth-child(3) { animation-delay: 0.3s; }
@keyframes w-loading-dot {
  0%, 80%, 100% { transform: scale(0.6); opacity: 0.4; }
  40% { transform: scale(1); opacity: 1; }
}
.w-loading__text { font-size: var(--font-size-sm); color: var(--color-text-secondary); }
</style>
