<template>
  <span :class="['w-tag', `w-tag--${variant}`, `w-tag--${size}`]">
    <slot />
  </span>
</template>

<script setup>
defineProps({
  variant: { type: String, default: 'default', validator: v => ['default', 'success', 'warning', 'danger', 'info', 'primary'].includes(v) },
  size: { type: String, default: 'small', validator: v => ['small', 'medium'].includes(v) }
})
</script>

<style scoped>
.w-tag {
  display: inline-flex;
  align-items: center;
  font-weight: 500;
  white-space: nowrap;
  line-height: 1;
}
.w-tag--small { font-size: 11px; padding: 3px 8px; border-radius: 4px; }
.w-tag--medium { font-size: var(--font-size-sm); padding: 4px 10px; border-radius: var(--radius-sm); }
.w-tag--default { background: var(--color-bg); color: var(--color-text-secondary); }
.w-tag--success { background: var(--color-success-bg); color: var(--color-success); }
.w-tag--warning { background: var(--color-warning-bg); color: var(--color-warning); }
.w-tag--danger { background: var(--color-danger-bg); color: var(--color-danger); }
.w-tag--info { background: var(--color-info-bg); color: var(--color-info); }
.w-tag--primary { background: var(--color-primary-50); color: var(--color-primary-600); }
</style>
