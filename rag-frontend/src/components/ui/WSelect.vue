<template>
  <div class="w-select" ref="selectRef">
    <div
      :class="['w-select__trigger', { 'w-select__trigger--active': open, 'w-select__trigger--disabled': disabled }]"
      @click="toggle"
    >
      <span v-if="selectedLabel" class="w-select__value">{{ selectedLabel }}</span>
      <span v-else class="w-select__placeholder">{{ placeholder }}</span>
      <WIcon name="arrowDown" :size="14" :class="['w-select__arrow', { 'w-select__arrow--open': open }]" />
    </div>
    <Transition name="w-select-drop">
      <div v-if="open" class="w-select__dropdown">
        <div
          v-for="opt in options"
          :key="opt.value"
          :class="['w-select__option', { 'w-select__option--selected': opt.value === modelValue, 'w-select__option--hover': hoveredValue === opt.value }]"
          @click="select(opt)"
          @mouseenter="hoveredValue = opt.value"
        >
          {{ opt.label }}
        </div>
        <div v-if="options.length === 0" class="w-select__empty">无选项</div>
      </div>
    </Transition>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'

const props = defineProps({
  modelValue: [String, Number],
  options: { type: Array, default: () => [] },
  placeholder: { type: String, default: '请选择' },
  disabled: Boolean
})

const emit = defineEmits(['update:modelValue', 'change'])
const open = ref(false)
const selectRef = ref(null)
const hoveredValue = ref(null)

const selectedLabel = computed(() => {
  const opt = props.options.find(o => o.value === props.modelValue)
  return opt ? opt.label : null
})

function toggle() {
  if (props.disabled) return
  open.value = !open.value
}

function select(opt) {
  emit('update:modelValue', opt.value)
  emit('change', opt.value)
  open.value = false
}

function handleClickOutside(e) {
  if (selectRef.value && !selectRef.value.contains(e.target)) {
    open.value = false
  }
}

onMounted(() => document.addEventListener('click', handleClickOutside))
onBeforeUnmount(() => document.removeEventListener('click', handleClickOutside))
</script>

<style scoped>
.w-select { position: relative; width: 100%; }
.w-select__trigger {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 14px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-surface);
  cursor: pointer;
  transition: border-color var(--duration-fast) var(--ease-out),
              box-shadow var(--duration-fast) var(--ease-out);
  gap: 8px;
}
.w-select__trigger:hover { border-color: var(--color-primary-300); }
.w-select__trigger--active { border-color: var(--color-primary-500); box-shadow: 0 0 0 3px rgba(13,148,136,0.1); }
.w-select__trigger--disabled { opacity: 0.5; cursor: not-allowed; }
.w-select__value { font-size: var(--font-size-base); color: var(--color-text); }
.w-select__placeholder { font-size: var(--font-size-base); color: var(--color-text-muted); }
.w-select__arrow { color: var(--color-text-muted); transition: transform var(--duration-fast); }
.w-select__arrow--open { transform: rotate(180deg); }
.w-select__dropdown {
  position: absolute;
  top: calc(100% + 4px);
  left: 0;
  right: 0;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-lg);
  z-index: 100;
  max-height: 240px;
  overflow-y: auto;
}
.w-select__option {
  padding: 10px 14px;
  font-size: var(--font-size-base);
  color: var(--color-text);
  cursor: pointer;
  transition: background var(--duration-fast);
}
.w-select__option:hover, .w-select__option--hover { background: var(--color-primary-50); }
.w-select__option--selected { color: var(--color-primary-600); font-weight: 500; }
.w-select__empty { padding: 16px; text-align: center; color: var(--color-text-muted); font-size: var(--font-size-sm); }
.w-select-drop-enter-active, .w-select-drop-leave-active { transition: all 0.15s ease-out; }
.w-select-drop-enter-from, .w-select-drop-leave-to { opacity: 0; transform: translateY(-4px); }
</style>
