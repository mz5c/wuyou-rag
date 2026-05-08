<template>
  <svg
    class="w-icon"
    :width="size"
    :height="size"
    viewBox="0 0 24 24"
    fill="none"
    stroke="currentColor"
    stroke-width="2"
    stroke-linecap="round"
    stroke-linejoin="round"
    :style="{ color: color }"
  >
    <path :d="path" />
  </svg>
</template>

<script setup>
import { computed } from 'vue'
import { Icons } from './icons'

const props = defineProps({
  name: { type: String, required: true },
  size: { type: [Number, String], default: 18 },
  color: { type: String, default: undefined }
})

const path = computed(() => Icons[props.name] || '')
</script>

<style scoped>
.w-icon {
  flex-shrink: 0;
  display: inline-flex;
  vertical-align: middle;
}
</style>
