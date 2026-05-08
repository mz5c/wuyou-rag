<template>
  <div :class="['chat-message', `message-${message.role}`]">
    <div class="message-avatar">
      <el-avatar v-if="message.role === 'user'" :size="36" icon="UserFilled" />
      <el-avatar v-else :size="36" icon="MagicStick" style="background-color: var(--color-accent, #2563eb)" />
    </div>
    <div class="message-content-wrapper">
      <details v-if="message.reasoningContent" class="reasoning-toggle">
        <summary>思考过程</summary>
        <div class="reasoning-content">{{ message.reasoningContent }}</div>
      </details>

      <div v-if="message.role === 'assistant'" class="message-content markdown-body" ref="contentRef" v-html="renderedContent"></div>
      <div v-else class="message-content" v-html="renderedContent"></div>

      <div v-if="message.timestamp" class="message-time">{{ message.timestamp }}</div>
    </div>
  </div>
</template>

<script setup>
import { computed, ref, onMounted, nextTick, watch } from 'vue'
import { marked } from 'marked'
import renderMathInElement from 'katex/contrib/auto-render'

const props = defineProps({
  message: { type: Object, required: true }
})

const contentRef = ref(null)

marked.setOptions({
  breaks: true,
  gfm: true
})

const renderedContent = computed(() => {
  const text = props.message.content || ''
  if (props.message.role === 'user') {
    return text.replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/\n/g, '<br>')
  }
  return marked.parse(text)
})

function renderMath() {
  if (contentRef.value) {
    renderMathInElement(contentRef.value, {
      delimiters: [
        { left: '$$', right: '$$', display: true },
        { left: '$', right: '$', display: false }
      ]
    })
  }
}

onMounted(() => { nextTick(renderMath) })
watch(() => props.message.content, () => { nextTick(renderMath) })
</script>

<style scoped>
.chat-message { display: flex; gap: 12px; margin-bottom: 20px; max-width: 85%; }
.message-user { flex-direction: row-reverse; margin-left: auto; }
.message-assistant { flex-direction: row; margin-right: auto; }
.message-system { justify-content: center; max-width: 100%; }
.message-system .message-content-wrapper { background: #f1f5f9; border-radius: 8px; padding: 8px 16px; text-align: center; color: #64748b; font-size: 13px; }
.message-system .message-avatar { display: none; }
.message-avatar { flex-shrink: 0; margin-top: 4px; }
.message-content-wrapper { display: flex; flex-direction: column; gap: 4px; min-width: 0; }

.message-user .message-content {
  background: #2563eb; color: #fff; border-radius: 12px 4px 12px 12px;
  padding: 10px 14px; font-size: 14px; line-height: 1.6;
}
.message-assistant .message-content {
  background: #fff; color: #1e293b; border-radius: 4px 12px 12px 12px;
  padding: 10px 14px; font-size: 14px; line-height: 1.6; border: 1px solid #e2e8f0;
}

.reasoning-toggle { margin-bottom: 8px; font-size: 13px; }
.reasoning-toggle summary {
  cursor: pointer; color: #64748b; font-size: 12px; padding: 4px 0;
  user-select: none; list-style: none; display: flex; align-items: center; gap: 4px;
}
.reasoning-toggle summary::before { content: '▶'; font-size: 10px; transition: transform 0.15s; }
.reasoning-toggle[open] summary::before { transform: rotate(90deg); }
.reasoning-toggle summary:hover { color: #2563eb; }
.reasoning-content {
  margin-top: 6px; padding: 10px 12px; background: #f8fafc; border-left: 3px solid #e2e8f0;
  border-radius: 0 6px 6px 0; color: #64748b; font-size: 13px; line-height: 1.6;
  white-space: pre-wrap; word-break: break-word;
}

.markdown-body :deep(h1), .markdown-body :deep(h2), .markdown-body :deep(h3) { margin: 16px 0 8px; color: #0f172a; }
.markdown-body :deep(h1) { font-size: 1.25em; }
.markdown-body :deep(h2) { font-size: 1.15em; }
.markdown-body :deep(h3) { font-size: 1.05em; }
.markdown-body :deep(p) { margin: 4px 0 10px; }
.markdown-body :deep(ul), .markdown-body :deep(ol) { padding-left: 20px; margin: 8px 0; }
.markdown-body :deep(li) { margin: 4px 0; }
.markdown-body :deep(code) {
  background: #f1f5f9; padding: 2px 6px; border-radius: 4px; font-size: 0.9em;
  font-family: 'SF Mono', 'Fira Code', monospace; color: #1e293b;
}
.markdown-body :deep(pre) {
  background: #0f172a; color: #e2e8f0; padding: 14px 16px; border-radius: 8px;
  overflow-x: auto; margin: 10px 0; font-size: 13px; line-height: 1.5;
}
.markdown-body :deep(pre code) { background: none; padding: 0; color: inherit; font-size: inherit; }
.markdown-body :deep(table) { border-collapse: collapse; margin: 10px 0; width: 100%; }
.markdown-body :deep(th), .markdown-body :deep(td) { border: 1px solid #e2e8f0; padding: 8px 12px; text-align: left; font-size: 13px; }
.markdown-body :deep(th) { background: #f8fafc; font-weight: 600; }
.markdown-body :deep(blockquote) { border-left: 3px solid #2563eb; padding: 4px 12px; margin: 10px 0; color: #64748b; background: #f8fafc; }
.markdown-body :deep(a) { color: #2563eb; text-decoration: underline; }
.markdown-body :deep(strong) { font-weight: 600; color: #0f172a; }
.markdown-body :deep(.katex) { font-size: 1.05em; }

.message-time { font-size: 12px; color: #94a3b8; padding: 0 4px; }
.message-user .message-time { text-align: right; }
</style>
