<template>
  <button
    :class="[
      'w-button',
      `w-button--${variant}`,
      `w-button--${size}`,
      { 'w-button--loading': loading, 'w-button--block': block }
    ]"
    :disabled="disabled || loading"
    @click="$emit('click', $event)"
  >
    <span v-if="loading" class="w-button__spinner">
      <WIcon name="loading" :size="size === 'small' ? 14 : 16" class="w-spin" />
    </span>
    <slot v-else />
  </button>
</template>

<script setup>
defineProps({
  variant: { type: String, default: 'primary', validator: v => ['primary', 'default', 'text', 'danger'].includes(v) },
  size: { type: String, default: 'medium', validator: v => ['small', 'medium', 'large'].includes(v) },
  loading: { type: Boolean, default: false },
  block: { type: Boolean, default: false },
  disabled: { type: Boolean, default: false }
})

defineEmits(['click'])
</script>

<style scoped>
.w-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  border: 1px solid transparent;
  border-radius: var(--radius-md);
  font-family: var(--font-body);
  font-weight: 500;
  cursor: pointer;
  transition: all var(--duration-fast) var(--ease-out);
  white-space: nowrap;
  user-select: none;
  outline: none;
  line-height: 1;
}
.w-button:focus-visible { box-shadow: 0 0 0 3px rgba(13,148,136,0.3); }

/* Sizes */
.w-button--small { padding: 6px 12px; font-size: var(--font-size-sm); height: 30px; border-radius: var(--radius-sm); }
.w-button--medium { padding: 10px 20px; font-size: var(--font-size-base); height: 40px; }
.w-button--large { padding: 12px 24px; font-size: var(--font-size-lg); height: 48px; }

/* Variants */
.w-button--primary {
  background: var(--gradient-primary);
  color: #fff;
  border: none;
}
.w-button--primary:hover:not(:disabled) { opacity: 0.9; box-shadow: 0 4px 12px rgba(13,148,136,0.3); }
.w-button--primary:active:not(:disabled) { opacity: 0.85; transform: translateY(1px); }

.w-button--default {
  background: var(--color-surface);
  color: var(--color-text);
  border-color: var(--color-border);
}
.w-button--default:hover:not(:disabled) { border-color: var(--color-primary-300); color: var(--color-primary-600); }
.w-button--default:active:not(:disabled) { background: var(--color-primary-50); }

.w-button--text {
  background: transparent;
  color: var(--color-text-secondary);
  border: none;
  padding: 4px 8px;
}
.w-button--text:hover:not(:disabled) { color: var(--color-primary-600); background: var(--color-primary-50); }

.w-button--danger {
  background: var(--color-danger);
  color: #fff;
  border: none;
}
.w-button--danger:hover:not(:disabled) { opacity: 0.9; }
.w-button--danger:active:not(:disabled) { opacity: 0.85; }

.w-button--loading { cursor: not-allowed; opacity: 0.8; }
.w-button--block { width: 100%; }
.w-button:disabled { opacity: 0.5; cursor: not-allowed; }

.w-button__spinner { display: flex; align-items: center; }
.w-spin { animation: w-spin 0.8s linear infinite; }
@keyframes w-spin { to { transform: rotate(360deg); } }
</style>
