<template>
  <div class="w-empty">
    <div class="w-empty__icon">
      <slot name="icon">
        <WIcon name="info" :size="48" color="#cbd5e1" />
      </slot>
    </div>
    <p class="w-empty__title">{{ title }}</p>
    <p v-if="description" class="w-empty__description">{{ description }}</p>
    <div v-if="$slots.action" class="w-empty__action">
      <slot name="action" />
    </div>
  </div>
</template>

<script setup>
defineProps({
  title: { type: String, default: '暂无数据' },
  description: String
})
</script>

<style scoped>
.w-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 48px 24px;
  text-align: center;
}
.w-empty__icon { margin-bottom: 16px; }
.w-empty__title { font-size: var(--font-size-base); color: var(--color-text-secondary); font-weight: 500; margin: 0; }
.w-empty__description { font-size: var(--font-size-sm); color: var(--color-text-muted); margin: 4px 0 0 0; }
.w-empty__action { margin-top: 20px; }
</style>
