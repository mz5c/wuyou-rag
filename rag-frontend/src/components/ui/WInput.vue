<template>
  <div :class="['w-input-wrapper', { 'w-input-wrapper--error': error }]">
    <div class="w-input__inner">
      <WIcon v-if="prefixIcon" :name="prefixIcon" size="16" class="w-input__prefix" />
      <input
        class="w-input"
        :type="type"
        :value="modelValue"
        :placeholder="placeholder"
        :disabled="disabled"
        v-bind="$attrs"
        @input="$emit('update:modelValue', $event.target.value)"
        @blur="$emit('blur', $event)"
      />
      <button v-if="clearable && modelValue" type="button" class="w-input__clear" @click="$emit('update:modelValue', '')">
        <WIcon name="close" size="14" />
      </button>
    </div>
    <p v-if="error" class="w-input__error">{{ error }}</p>
  </div>
</template>

<script setup>
defineProps({
  modelValue: [String, Number],
  type: { type: String, default: 'text' },
  placeholder: String,
  disabled: Boolean,
  prefixIcon: String,
  clearable: Boolean,
  error: String
})
defineEmits(['update:modelValue', 'blur'])
</script>

<style scoped>
.w-input-wrapper { width: 100%; }
.w-input__inner {
  display: flex;
  align-items: center;
  gap: 8px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: 0 14px;
  background: var(--color-surface);
  transition: border-color var(--duration-fast) var(--ease-out),
              box-shadow var(--duration-fast) var(--ease-out);
}
.w-input__inner:focus-within {
  border-color: var(--color-primary-500);
  box-shadow: 0 0 0 3px rgba(13,148,136,0.1);
}
.w-input-wrapper--error .w-input__inner {
  border-color: var(--color-danger);
}
.w-input__inner:hover { border-color: var(--color-primary-300); }
.w-input-wrapper--error .w-input__inner:hover { border-color: var(--color-danger); }

.w-input {
  flex: 1;
  border: none;
  outline: none;
  padding: 10px 0;
  font-size: var(--font-size-base);
  font-family: var(--font-body);
  color: var(--color-text);
  background: transparent;
  line-height: 1.4;
  min-width: 0;
}
.w-input::placeholder { color: var(--color-text-muted); }
.w-input:disabled { color: var(--color-text-muted); cursor: not-allowed; }
.w-input__prefix { color: var(--color-text-muted); flex-shrink: 0; }
.w-input__clear {
  border: none; background: none; cursor: pointer; padding: 0;
  color: var(--color-text-muted); display: flex; align-items: center;
}
.w-input__clear:hover { color: var(--color-text-secondary); }
.w-input__error {
  font-size: var(--font-size-xs);
  color: var(--color-danger);
  margin-top: 4px;
  padding-left: 2px;
}
</style>
