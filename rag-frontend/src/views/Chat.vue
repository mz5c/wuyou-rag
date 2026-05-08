<template>
  <div class="chat-page">
    <!-- Sidebar -->
    <div class="chat-sidebar">
      <div class="sidebar-header">
        <div class="sidebar-brand">
          <div class="sidebar-brand__icon">W</div>
          <span>Wuyou RAG</span>
        </div>
        <button class="sidebar-new-btn" @click="newConversation">
          <WIcon name="plus" size="14" /> 新对话
        </button>
      </div>
      <div class="sidebar-list">
        <template v-if="conversations.length === 0">
          <div class="sidebar-empty">暂无对话</div>
        </template>
        <template v-for="group in groupedConversations" :key="group.label">
          <div class="sidebar-group-label">{{ group.label }}</div>
          <div
            v-for="conv in group.items"
            :key="conv.id || conv.conversationId"
            :class="['sidebar-item', { 'sidebar-item--active': activeConversationId === String(conv.id || conv.conversationId) }]"
            @click="switchConversation(conv.id || conv.conversationId)"
          >
            <WIcon name="chat" size="16" class="sidebar-item__icon" />
            <div class="sidebar-item__content">
              <span
                v-if="editingConvId !== conv.id"
                class="sidebar-item__title"
                @dblclick.stop="startEditTitle(conv)"
              >{{ conv.title || conv.topic || '新对话' }}</span>
              <input
                v-else
                v-model="editTitleText"
                class="sidebar-item__edit"
                @blur="saveEditTitle(conv)"
                @keyup.enter="saveEditTitle(conv)"
                ref="titleInputRef"
                maxlength="100"
              />
            </div>
            <div class="sidebar-item__actions">
              <button class="sidebar-item__btn" @click.stop="startEditTitle(conv)" title="编辑">
                <WIcon name="edit" size="12" />
              </button>
              <button class="sidebar-item__btn" @click.stop="handleDeleteConversation(conv.id || conv.conversationId)" title="删除">
                <WIcon name="delete" size="12" />
              </button>
            </div>
          </div>
        </template>
      </div>
      <div class="sidebar-footer">
        <button v-if="isAdmin()" class="sidebar-footer__btn" @click="goToAdmin">
          <WIcon name="settings" size="16" /> 管理后台
        </button>
        <button class="sidebar-footer__btn" @click="handleLogout">
          <WIcon name="logout" size="16" /> 退出登录
        </button>
      </div>
    </div>

    <!-- Main Chat Area -->
    <div class="chat-main">
      <div class="messages-container" ref="messagesRef">
        <div v-if="messages.length === 0" class="messages-empty">
          <div class="messages-empty__icon">
            <WIcon name="chat" :size="28" />
          </div>
          <p class="messages-empty__title">开始新的对话</p>
          <p class="messages-empty__desc">在下方输入问题，我将基于知识库为您解答</p>
        </div>
        <template v-for="msg in messages" :key="msg.id || msg.timestamp">
          <ChatMessage :message="msg" />
          <SourceReference
            v-if="msg.role === 'assistant' && msg.sources && msg.sources.length > 0"
            :sources="msg.sources"
          />
        </template>
        <div v-if="streaming" class="streaming-indicator">
          <span class="streaming-dot"></span>
          <span class="streaming-dot"></span>
          <span class="streaming-dot"></span>
        </div>
      </div>

      <div class="input-area">
        <div class="input-area__inner">
          <textarea
            v-model="inputText"
            class="input-area__textarea"
            placeholder="输入您的问题，Enter 发送，Shift+Enter 换行"
            :disabled="streaming"
            rows="2"
            @keydown="handleKeydown"
          ></textarea>
          <button class="input-area__send" :disabled="streaming || !inputText.trim()" @click="sendMessage">
            <WIcon name="send" size="18" />
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import ChatMessage from '../components/ChatMessage.vue'
import SourceReference from '../components/SourceReference.vue'
import { WMessage } from '../components/ui/WMessage'
import { WMessageBox } from '../components/ui/WMessageBox'
import {
  chat, getMessages, listConversations,
  deleteConversation, createConversation, updateConversationTitle
} from '../api/chat'
import { useAuth } from '../store/auth'

const router = useRouter()
const { logout, isAdmin } = useAuth()

const messagesRef = ref(null)
const inputText = ref('')
const messages = ref([])
const conversations = ref([])
const activeConversationId = ref('')
const streaming = ref(false)
const editingConvId = ref(null)
const editTitleText = ref('')
const titleInputRef = ref(null)

const groupedConversations = computed(() => {
  const now = new Date()
  const today = now.toDateString()
  const yesterday = new Date(now)
  yesterday.setDate(yesterday.getDate() - 1)
  const yDay = yesterday.toDateString()

  const groups = { today: [], yesterday: [], earlier: [] }
  conversations.value.forEach(conv => {
    const t = conv.createTime || conv.updatedTime
    if (!t) { groups.earlier.push(conv); return }
    const d = new Date(t)
    const ds = d.toDateString()
    if (ds === today) groups.today.push(conv)
    else if (ds === yDay) groups.yesterday.push(conv)
    else groups.earlier.push(conv)
  })

  const result = []
  if (groups.today.length) result.push({ label: '今天', items: groups.today })
  if (groups.yesterday.length) result.push({ label: '昨天', items: groups.yesterday })
  if (groups.earlier.length) result.push({ label: '更早', items: groups.earlier })
  return result
})

onMounted(() => { loadConversations() })

function handleKeydown(e) {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    sendMessage()
  }
}

async function loadConversations() {
  try {
    const res = await listConversations()
    const list = res.data?.data || res.data || []
    conversations.value = Array.isArray(list) ? list : []
  } catch {
    conversations.value = []
  }
}

async function switchConversation(id) {
  activeConversationId.value = String(id)
  messages.value = []
  try {
    const res = await getMessages(id, { page: 1, size: 100 })
    const pageData = res.data?.data
    const records = pageData?.records || []
    const list = Array.isArray(records) ? records : []
    if (list.length > 0 && list[0].question !== undefined) {
      messages.value = list.flatMap(r => {
        const msgs = [{ role: 'user', content: r.question, timestamp: r.createTime }]
        if (r.answer) {
          let sources = []
          if (r.sources) {
            if (typeof r.sources === 'string') {
              try { sources = JSON.parse(r.sources) } catch { sources = [] }
            } else if (Array.isArray(r.sources)) {
              sources = r.sources
            }
          }
          msgs.push({ role: 'assistant', content: r.answer, reasoningContent: r.reasoningContent || null, sources, timestamp: r.createTime })
        }
        return msgs
      })
    } else {
      messages.value = []
    }
    await nextTick()
    scrollToBottom()
  } catch {
    messages.value = []
  }
}

async function newConversation() {
  activeConversationId.value = ''
  messages.value = []
  inputText.value = ''
}

function startEditTitle(conv) {
  editingConvId.value = conv.id
  editTitleText.value = conv.title || ''
  nextTick(() => {
    const el = document.querySelector('.sidebar-item__edit')
    if (el) el.focus()
  })
}

async function saveEditTitle(conv) {
  if (editTitleText.value.trim() && editTitleText.value !== conv.title) {
    const oldTitle = conv.title
    conv.title = editTitleText.value
    try {
      await updateConversationTitle(conv.id, { title: editTitleText.value })
    } catch {
      conv.title = oldTitle
    }
  }
  editingConvId.value = null
}

async function sendMessage() {
  const text = inputText.value.trim()
  if (!text || streaming.value) return

  const userMsg = {
    id: Date.now().toString(),
    role: 'user',
    content: text,
    timestamp: new Date().toLocaleTimeString()
  }
  messages.value.push(userMsg)
  inputText.value = ''
  streaming.value = true

  try {
    const res = await chat({ question: text, conversationId: activeConversationId.value || undefined })
    const data = res.data?.data || res.data

    const assistantMsg = {
      id: (Date.now() + 1).toString(),
      role: 'assistant',
      content: data.answer || data.content || data.response || '',
      reasoningContent: data.reasoningContent || null,
      sources: data.sources || data.references || [],
      timestamp: new Date().toLocaleTimeString()
    }
    messages.value.push(assistantMsg)

    if (data.conversationId && !activeConversationId.value) {
      activeConversationId.value = data.conversationId
      await loadConversations()
    }
  } catch {
    messages.value.push({
      id: (Date.now() + 2).toString(),
      role: 'assistant',
      content: '抱歉，我遇到了问题，请稍后重试。',
      timestamp: new Date().toLocaleTimeString()
    })
  } finally {
    streaming.value = false
    await nextTick()
    scrollToBottom()
  }
}

async function handleDeleteConversation(id) {
  try {
    await WMessageBox({ title: '提示', message: '确定删除该对话？', type: 'warning' })
    await deleteConversation(id)
    conversations.value = conversations.value.filter(c => (c.id || c.conversationId) !== id)
    if (activeConversationId.value === String(id)) {
      newConversation()
    }
    WMessage.success('已删除')
  } catch {
    // cancelled
  }
}

async function handleLogout() {
  logout()
  await router.push('/login')
}

function goToAdmin() {
  router.push('/admin')
}

function scrollToBottom() {
  if (messagesRef.value) {
    messagesRef.value.scrollTop = messagesRef.value.scrollHeight
  }
}
</script>

<style scoped>
.chat-page {
  display: flex;
  height: 100vh;
  overflow: hidden;
}

/* Sidebar */
.chat-sidebar {
  width: 280px;
  background: var(--color-sidebar-bg);
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
}

.sidebar-header {
  padding: 20px 16px 16px;
  border-bottom: 1px solid rgba(255,255,255,0.06);
}

.sidebar-brand {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 16px;
}
.sidebar-brand__icon {
  width: 28px;
  height: 28px;
  background: var(--gradient-primary);
  border-radius: var(--radius-sm);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-weight: 700;
  font-size: 14px;
}
.sidebar-brand span {
  color: #fff;
  font-weight: 600;
  font-size: 15px;
}

.sidebar-new-btn {
  width: 100%;
  padding: 10px 0;
  background: rgba(255,255,255,0.06);
  color: rgba(255,255,255,0.9);
  border: 1px dashed rgba(255,255,255,0.12);
  border-radius: var(--radius-md);
  font-size: var(--font-size-sm);
  font-weight: 500;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  transition: all var(--duration-fast);
  font-family: var(--font-body);
}
.sidebar-new-btn:hover {
  background: rgba(255,255,255,0.1);
  border-color: rgba(255,255,255,0.2);
}

.sidebar-list {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
}

.sidebar-group-label {
  font-size: 11px;
  font-weight: 600;
  color: rgba(255,255,255,0.35);
  text-transform: uppercase;
  letter-spacing: 0.5px;
  padding: 12px 12px 6px;
}

.sidebar-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 12px;
  border-radius: var(--radius-sm);
  cursor: pointer;
  color: rgba(255,255,255,0.65);
  margin-bottom: 2px;
  transition: all var(--duration-fast);
  position: relative;
}
.sidebar-item:hover {
  background: var(--color-sidebar-hover);
  color: rgba(255,255,255,0.9);
}
.sidebar-item--active {
  background: var(--color-sidebar-active);
  color: #fff;
}
.sidebar-item__icon {
  flex-shrink: 0;
  color: rgba(255,255,255,0.4);
}
.sidebar-item--active .sidebar-item__icon {
  color: var(--color-primary-300);
}
.sidebar-item__content {
  flex: 1;
  min-width: 0;
}
.sidebar-item__title {
  font-size: var(--font-size-sm);
  display: block;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.sidebar-item__edit {
  width: 100%;
  background: rgba(255,255,255,0.1);
  border: 1px solid var(--color-primary-500);
  border-radius: 4px;
  padding: 2px 6px;
  font-size: var(--font-size-sm);
  color: #fff;
  outline: none;
  font-family: var(--font-body);
}
.sidebar-item__actions {
  display: none;
  gap: 2px;
  flex-shrink: 0;
}
.sidebar-item:hover .sidebar-item__actions {
  display: flex;
}
.sidebar-item__btn {
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  background: transparent;
  color: rgba(255,255,255,0.4);
  cursor: pointer;
  border-radius: 4px;
  padding: 0;
  transition: all var(--duration-fast);
}
.sidebar-item__btn:hover {
  background: rgba(255,255,255,0.1);
  color: rgba(255,255,255,0.8);
}

.sidebar-empty {
  text-align: center;
  color: rgba(255,255,255,0.3);
  padding: 24px;
  font-size: var(--font-size-sm);
}

.sidebar-footer {
  padding: 12px;
  border-top: 1px solid rgba(255,255,255,0.06);
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.sidebar-footer__btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  border: none;
  border-radius: var(--radius-sm);
  background: transparent;
  color: rgba(255,255,255,0.5);
  font-size: var(--font-size-sm);
  cursor: pointer;
  transition: all var(--duration-fast);
  font-family: var(--font-body);
  width: 100%;
  text-align: left;
}
.sidebar-footer__btn:hover {
  background: var(--color-sidebar-hover);
  color: rgba(255,255,255,0.8);
}

/* Main Chat Area */
.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: var(--color-bg);
}

.messages-container {
  flex: 1;
  overflow-y: auto;
  padding: 32px 40px;
}

.messages-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
}
.messages-empty__icon {
  width: 64px;
  height: 64px;
  border-radius: 20px;
  background: var(--color-primary-50);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-primary-500);
  margin-bottom: 16px;
}
.messages-empty__title {
  font-size: var(--font-size-lg);
  font-weight: 500;
  color: var(--color-text-secondary);
  margin: 0 0 4px;
}
.messages-empty__desc {
  font-size: var(--font-size-base);
  color: var(--color-text-muted);
  margin: 0;
}

.streaming-indicator {
  display: flex;
  gap: 5px;
  padding: 12px 0;
  align-items: center;
  margin-left: 48px;
}
.streaming-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--color-primary-400);
  animation: bounce 1.4s infinite ease-in-out both;
}
.streaming-dot:nth-child(1) { animation-delay: -0.32s; }
.streaming-dot:nth-child(2) { animation-delay: -0.16s; }
.streaming-dot:nth-child(3) { animation-delay: 0s; }
@keyframes bounce { 0%, 80%, 100% { transform: scale(0); } 40% { transform: scale(1); } }

/* Input Area */
.input-area {
  padding: 16px 24px 24px;
}
.input-area__inner {
  max-width: 800px;
  margin: 0 auto;
  display: flex;
  gap: 12px;
  align-items: flex-end;
}
.input-area__textarea {
  flex: 1;
  border: 1px solid var(--color-border);
  border-radius: 16px;
  padding: 14px 18px;
  font-size: var(--font-size-base);
  font-family: var(--font-body);
  line-height: 1.5;
  color: var(--color-text);
  background: var(--color-surface);
  resize: none;
  outline: none;
  transition: border-color var(--duration-fast) var(--ease-out),
              box-shadow var(--duration-fast) var(--ease-out);
  min-height: 24px;
  max-height: 120px;
}
.input-area__textarea:focus {
  border-color: var(--color-primary-500);
  box-shadow: 0 0 0 3px rgba(13,148,136,0.1);
}
.input-area__textarea::placeholder {
  color: var(--color-text-muted);
}
.input-area__textarea:disabled {
  color: var(--color-text-muted);
  cursor: not-allowed;
  background: var(--color-bg);
}
.input-area__send {
  width: 48px;
  height: 48px;
  border-radius: 14px;
  background: var(--gradient-primary);
  border: none;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  flex-shrink: 0;
  transition: all var(--duration-fast) var(--ease-out);
}
.input-area__send:hover:not(:disabled) {
  opacity: 0.9;
  box-shadow: 0 4px 12px rgba(13,148,136,0.3);
}
.input-area__send:active:not(:disabled) {
  transform: translateY(1px);
}
.input-area__send:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
</style>
