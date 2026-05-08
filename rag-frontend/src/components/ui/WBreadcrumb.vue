<template>
  <div class="w-breadcrumb">
    <span v-for="(item, idx) in items" :key="idx" class="w-breadcrumb__item">
      <span v-if="item.to" class="w-breadcrumb__link" @click="$emit('navigate', item.to)">{{ item.label }}</span>
      <span v-else class="w-breadcrumb__current">{{ item.label }}</span>
      <WIcon v-if="idx < items.length - 1" name="arrowRight" size="12" class="w-breadcrumb__sep" />
    </span>
  </div>
</template>

<script setup>
defineProps({
  items: { type: Array, required: true } // [{ label, to? }]
})
defineEmits(['navigate'])
</script>

<style scoped>
.w-breadcrumb { display: flex; align-items: center; gap: 4px; }
.w-breadcrumb__item { display: flex; align-items: center; gap: 4px; }
.w-breadcrumb__link { font-size: var(--font-size-sm); color: var(--color-text-muted); cursor: pointer; }
.w-breadcrumb__link:hover { color: var(--color-primary-600); }
.w-breadcrumb__current { font-size: var(--font-size-sm); font-weight: 500; color: var(--color-text); }
.w-breadcrumb__sep { color: var(--color-text-muted); }
</style>
