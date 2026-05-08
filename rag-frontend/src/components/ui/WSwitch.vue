<template>
  <button
    type="button"
    :class="['w-switch', { 'w-switch--on': modelValue, 'w-switch--disabled': disabled }]"
    @click="toggle"
    role="switch"
    :aria-checked="modelValue"
  >
    <span class="w-switch__dot" />
  </button>
</template>

<script setup>
defineProps({ modelValue: Boolean, disabled: Boolean })
const emit = defineEmits(['update:modelValue'])
function toggle() { emit('update:modelValue', !modelValue) }
</script>

<style scoped>
.w-switch {
  position: relative;
  width: 44px; height: 24px;
  border-radius: 12px;
  border: none;
  background: var(--color-border);
  cursor: pointer;
  transition: background var(--duration-fast);
  padding: 0;
}
.w-switch--on { background: var(--color-primary-600); }
.w-switch--disabled { opacity: 0.5; cursor: not-allowed; }
.w-switch__dot {
  position: absolute;
  top: 2px; left: 2px;
  width: 20px; height: 20px;
  border-radius: 50%;
  background: #fff;
  transition: transform var(--duration-fast) var(--ease-out);
  box-shadow: 0 1px 3px rgba(0,0,0,0.15);
}
.w-switch--on .w-switch__dot { transform: translateX(20px); }
</style>
