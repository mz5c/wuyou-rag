<template>
  <nav class="w-menu">
    <div
      v-for="item in items"
      :key="item.key"
      :class="['w-menu__item', { 'w-menu__item--active': item.key === activeKey }]"
      @click="$emit('select', item.key)"
    >
      <WIcon v-if="item.icon" :name="item.icon" size="16" />
      <span>{{ item.label }}</span>
    </div>
  </nav>
</template>

<script setup>
defineProps({
  items: { type: Array, required: true }, // [{ key, label, icon }]
  activeKey: { type: String, default: '' }
})
defineEmits(['select'])
</script>

<style scoped>
.w-menu { display: flex; flex-direction: column; gap: 2px; }
.w-menu__item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border-radius: 8px;
  font-size: var(--font-size-sm);
  color: rgba(255,255,255,0.6);
  cursor: pointer;
  transition: background var(--duration-fast), color var(--duration-fast);
}
.w-menu__item:hover { color: rgba(255,255,255,0.9); background: rgba(255,255,255,0.06); }
.w-menu__item--active {
  color: #fff;
  background: rgba(13,148,136,0.2);
  font-weight: 500;
}
</style>
