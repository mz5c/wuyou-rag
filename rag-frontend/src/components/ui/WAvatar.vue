<template>
  <div
    :class="['w-avatar', `w-avatar--${size}`]"
    :style="{ background: bgColor }"
  >
    <img v-if="src" :src="src" :alt="alt" class="w-avatar__img" />
    <span v-else-if="label" class="w-avatar__label">{{ label.charAt(0).toUpperCase() }}</span>
    <WIcon v-else name="user" />
  </div>
</template>

<script setup>
defineProps({
  src: String,
  alt: { type: String, default: '' },
  label: String,
  size: { type: String, default: 'medium', validator: v => ['small', 'medium', 'large'].includes(v) },
  bgColor: { type: String, default: 'linear-gradient(135deg, #0d9488, #14b8a6)' }
})
</script>

<style scoped>
.w-avatar {
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--radius-sm);
  flex-shrink: 0;
  overflow: hidden;
  color: #fff;
}
.w-avatar--small { width: 28px; height: 28px; border-radius: 6px; }
.w-avatar--medium { width: 36px; height: 36px; border-radius: var(--radius-sm); }
.w-avatar--large { width: 48px; height: 48px; border-radius: var(--radius-md); }
.w-avatar__img { width: 100%; height: 100%; object-fit: cover; }
.w-avatar__label { font-weight: 600; font-size: 14px; line-height: 1; }
.w-avatar--small .w-avatar__label { font-size: 11px; }
.w-avatar--large .w-avatar__label { font-size: 18px; }
</style>
