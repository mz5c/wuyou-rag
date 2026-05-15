<template>
  <Teleport to="body">
    <Transition name="w-dialog-fade">
      <div v-if="modelValue" class="w-dialog-overlay" @click.self="close">
        <Transition name="w-dialog-zoom" appear>
          <div class="w-dialog" :style="{ maxWidth: width }">
            <div class="w-dialog__header">
              <h3 class="w-dialog__title">{{ title }}</h3>
              <button class="w-dialog__close" type="button" @click="close">
                <WIcon name="close" size="16" />
              </button>
            </div>
            <div class="w-dialog__body">
              <slot />
            </div>
            <div v-if="$slots.footer" class="w-dialog__footer">
              <slot name="footer" />
            </div>
          </div>
        </Transition>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup>
defineProps({
  modelValue: Boolean,
  title: { type: String, default: '' },
  width: { type: String, default: '480px' }
})
const emit = defineEmits(['update:modelValue'])
function close() { emit('update:modelValue', false) }
</script>

<style scoped>
.w-dialog-overlay {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}
.w-dialog {
  width: 90%;
  background: var(--color-surface);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-xl);
  max-height: 85vh;
  display: flex;
  flex-direction: column;
}
.w-dialog__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 24px 0;
}
.w-dialog__title { font-size: var(--font-size-lg); font-weight: 600; color: var(--color-text); margin: 0; }
.w-dialog__close {
  border: none; background: none; cursor: pointer; padding: 4px;
  color: var(--color-text-muted); border-radius: var(--radius-sm); display: flex;
}
.w-dialog__close:hover { background: var(--color-bg); color: var(--color-text); }
.w-dialog__body { padding: 20px 24px; overflow-y: auto; }
.w-dialog__footer { padding: 0 24px 20px; display: flex; justify-content: flex-end; gap: 8px; }
.w-dialog-fade-enter-active, .w-dialog-fade-leave-active { transition: opacity 0.2s; }
.w-dialog-fade-enter-from, .w-dialog-fade-leave-to { opacity: 0; }
.w-dialog-zoom-enter-active { transition: all 0.2s ease-out; }
.w-dialog-zoom-leave-active { transition: all 0.15s ease-in; }
.w-dialog-zoom-enter-from { opacity: 0; transform: scale(0.95) translateY(8px); }
.w-dialog-zoom-leave-to { opacity: 0; transform: scale(0.95); }
</style>
