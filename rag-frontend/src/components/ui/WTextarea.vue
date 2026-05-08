<template>
  <div :class="['w-textarea-wrapper', { 'w-textarea-wrapper--error': error }]">
    <textarea
      class="w-textarea"
      :value="modelValue"
      :placeholder="placeholder"
      :rows="rows"
      :disabled="disabled"
      @input="$emit('update:modelValue', $event.target.value)"
      v-bind="$attrs"
    ></textarea>
    <p v-if="error" class="w-textarea__error">{{ error }}</p>
  </div>
</template>

<script setup>
defineProps({
  modelValue: [String, Number],
  placeholder: String,
  rows: { type: [Number, String], default: 3 },
  disabled: Boolean,
  error: String
})
defineEmits(['update:modelValue'])
</script>

<style scoped>
.w-textarea-wrapper { width: 100%; }
.w-textarea {
  width: 100%;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: 10px 14px;
  font-size: var(--font-size-base);
  font-family: var(--font-body);
  color: var(--color-text);
  background: var(--color-surface);
  line-height: 1.6;
  resize: vertical;
  outline: none;
  transition: border-color var(--duration-fast) var(--ease-out),
              box-shadow var(--duration-fast) var(--ease-out);
}
.w-textarea::placeholder { color: var(--color-text-muted); }
.w-textarea:hover { border-color: var(--color-primary-300); }
.w-textarea:focus { border-color: var(--color-primary-500); box-shadow: 0 0 0 3px rgba(13,148,136,0.1); }
.w-textarea-wrapper--error .w-textarea { border-color: var(--color-danger); }
.w-textarea:disabled { color: var(--color-text-muted); cursor: not-allowed; background: var(--color-bg); }
.w-textarea__error { font-size: var(--font-size-xs); color: var(--color-danger); margin-top: 4px; }
</style>
