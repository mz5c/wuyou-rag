<template>
  <div :class="['chat-message', `message-${message.role}`]">
    <div v-if="message.role === 'assistant'" class="message-avatar">
      <div class="message-avatar__inner message-avatar--assistant">
        <WIcon name="magic" :size="16" />
      </div>
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
    <div v-if="message.role === 'user'" class="message-avatar">
      <div class="message-avatar__inner message-avatar--user">
        <WIcon name="user" :size="16" />
      </div>
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
.message-system .message-content-wrapper { background: var(--color-bg); border-radius: var(--radius-md); padding: 8px 16px; text-align: center; color: var(--color-text-secondary); font-size: var(--font-size-sm); }
.message-system .message-avatar { display: none; }

.message-avatar { flex-shrink: 0; margin-top: 4px; }
.message-avatar__inner {
  width: 32px; height: 32px;
  border-radius: var(--radius-sm);
  display: flex;
  align-items: center;
  justify-content: center;
}
.message-avatar--assistant {
  background: var(--gradient-primary);
  color: #fff;
}
.message-avatar--user {
  background: var(--color-bg);
  color: var(--color-text-secondary);
  border: 1px solid var(--color-border);
}

.message-content-wrapper { display: flex; flex-direction: column; gap: 4px; min-width: 0; }

.message-user .message-content {
  background: var(--gradient-primary);
  color: #fff;
  border-radius: 16px 16px 4px 16px;
  padding: 12px 16px;
  font-size: var(--font-size-base);
  line-height: 1.6;
}
.message-assistant .message-content {
  background: var(--color-surface);
  color: var(--color-text);
  border-radius: 4px 16px 16px 16px;
  padding: 12px 16px;
  font-size: var(--font-size-base);
  line-height: 1.6;
  border: 1px solid var(--color-border);
}

.reasoning-toggle { margin-bottom: 8px; font-size: var(--font-size-sm); }
.reasoning-toggle summary {
  cursor: pointer; color: var(--color-text-muted); font-size: 12px; padding: 4px 0;
  user-select: none; list-style: none; display: flex; align-items: center; gap: 4px;
  font-weight: 500;
}
.reasoning-toggle summary::before { content: '▶'; font-size: 10px; transition: transform var(--duration-fast); }
.reasoning-toggle[open] summary::before { transform: rotate(90deg); }
.reasoning-toggle summary:hover { color: var(--color-primary-600); }
.reasoning-content {
  margin-top: 6px; padding: 10px 12px; background: var(--color-bg); border-left: 3px solid var(--color-border);
  border-radius: 0 var(--radius-sm) var(--radius-sm) 0; color: var(--color-text-secondary); font-size: var(--font-size-sm); line-height: 1.6;
  white-space: pre-wrap; word-break: break-word;
}

.markdown-body :deep(h1), .markdown-body :deep(h2), .markdown-body :deep(h3) { margin: 16px 0 8px; color: var(--color-text); }
.markdown-body :deep(h1) { font-size: 1.25em; }
.markdown-body :deep(h2) { font-size: 1.15em; }
.markdown-body :deep(h3) { font-size: 1.05em; }
.markdown-body :deep(p) { margin: 4px 0 10px; }
.markdown-body :deep(ul), .markdown-body :deep(ol) { padding-left: 20px; margin: 8px 0; }
.markdown-body :deep(li) { margin: 4px 0; }
.markdown-body :deep(code) {
  background: var(--color-bg); padding: 2px 6px; border-radius: 4px; font-size: 0.9em;
  font-family: var(--font-mono); color: var(--color-text);
}
.markdown-body :deep(pre) {
  background: #0f172a; color: #e2e8f0; padding: 14px 16px; border-radius: var(--radius-md);
  overflow-x: auto; margin: 10px 0; font-size: 13px; line-height: 1.5;
}
.markdown-body :deep(pre code) { background: none; padding: 0; color: inherit; font-size: inherit; }
.markdown-body :deep(table) { border-collapse: collapse; margin: 10px 0; width: 100%; }
.markdown-body :deep(th), .markdown-body :deep(td) { border: 1px solid var(--color-border); padding: 8px 12px; text-align: left; font-size: 13px; }
.markdown-body :deep(th) { background: var(--color-bg); font-weight: 600; }
.markdown-body :deep(blockquote) { border-left: 3px solid var(--color-primary-300); padding: 4px 12px; margin: 10px 0; color: var(--color-text-secondary); background: var(--color-bg); }
.markdown-body :deep(a) { color: var(--color-primary-600); text-decoration: underline; }
.markdown-body :deep(strong) { font-weight: 600; color: var(--color-text); }
.markdown-body :deep(.katex) { font-size: 1.05em; }

.message-time { font-size: var(--font-size-xs); color: var(--color-text-muted); padding: 0 4px; }
.message-user .message-time { text-align: right; }
</style>
