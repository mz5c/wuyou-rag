<template>
  <div class="chat-container">
    <!-- Sidebar -->
    <div class="chat-sidebar">
      <div class="sidebar-header">
        <h2 class="sidebar-title">RAG 知识库</h2>
        <el-button type="primary" :icon="Plus" style="width: 100%" @click="newConversation">
          新对话
        </el-button>
      </div>
      <div class="sidebar-conversations">
        <el-menu
          :default-active="activeConversationId"
          @select="switchConversation"
          style="border-right: none"
        >
          <el-menu-item
            v-for="conv in conversations"
            :key="conv.id || conv.conversationId"
            :index="String(conv.id || conv.conversationId)"
          >
            <el-icon><ChatDotSquare /></el-icon>
            <div class="conv-title-wrapper">
              <span v-if="editingConvId !== conv.id" @dblclick="startEditTitle(conv)" class="conv-title">{{ conv.title || conv.topic || '新对话' }}</span>
              <el-input
                v-else
                v-model="editTitleText"
                size="small"
                @blur="saveEditTitle(conv)"
                @keyup.enter="saveEditTitle(conv)"
                ref="titleInputRef"
                maxlength="100"
              />
              <el-icon class="edit-icon" @click="startEditTitle(conv)"><Edit /></el-icon>
            </div>
            <el-icon
              class="delete-btn"
              @click.stop="handleDeleteConversation(conv.id || conv.conversationId)"
            >
              <Delete />
            </el-icon>
          </el-menu-item>
        </el-menu>
        <div v-if="conversations.length === 0" class="sidebar-empty">
          暂无对话
        </div>
      </div>
      <div class="sidebar-footer">
        <el-button text @click="goToAdmin" v-if="isAdmin">
          <el-icon><Setting /></el-icon>
          管理后台
        </el-button>
        <el-button text @click="handleLogout">
          <el-icon><SwitchButton /></el-icon>
          退出登录
        </el-button>
      </div>
    </div>

    <!-- Main Chat Area -->
    <div class="chat-main">
      <div class="messages-container" ref="messagesRef">
        <div v-if="messages.length === 0" class="messages-empty">
          <el-icon :size="48" color="#c0c4cc"><ChatLineSquare /></el-icon>
          <p>开始一个新的对话</p>
          <p class="messages-hint">在下方输入您的问题，我将基于知识库为您解答</p>
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
        <el-input
          v-model="inputText"
          type="textarea"
          :rows="2"
          placeholder="输入您的问题，按 Enter 发送"
          :disabled="streaming"
          @keydown="handleKeydown"
        />
        <el-button
          type="primary"
          :icon="Promotion"
          :loading="streaming"
          style="margin-left: 12px; height: 56px"
          @click="sendMessage"
        >
          发送
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import {
  Plus, ChatDotSquare, Delete, Setting, SwitchButton,
  Promotion, ChatLineSquare, Edit
} from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import ChatMessage from '../components/ChatMessage.vue'
import SourceReference from '../components/SourceReference.vue'
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

onMounted(() => {
  loadConversations()
})

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
  activeConversationId.value = id
  messages.value = []
  try {
    const res = await getMessages(id, { page: 1, size: 100 })
    const records = res.data?.records || res.data?.data || res.data || []
    const list = Array.isArray(records) ? records : []
    if (list.length > 0 && list[0].question !== undefined) {
      messages.value = list.flatMap(r => {
        const msgs = [{ role: 'user', content: r.question, timestamp: r.createTime }]
        if (r.answer) {
          msgs.push({ role: 'assistant', content: r.answer, reasoningContent: r.reasoningContent || null, sources: r.sources || [], timestamp: r.createTime })
        }
        return msgs
      })
    } else {
      messages.value = list
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
  editTitleText.value = conv.title
  nextTick(() => {
    if (titleInputRef.value) titleInputRef.value.focus()
  })
}

async function saveEditTitle(conv) {
  if (editTitleText.value.trim() && editTitleText.value !== conv.title) {
    conv.title = editTitleText.value
    try {
      await updateConversationTitle(conv.id, { title: editTitleText.value })
    } catch {
      conv.title = conv.title
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
    await ElMessageBox.confirm('确定删除该对话？', '提示', { type: 'warning' })
    await deleteConversation(id)
    conversations.value = conversations.value.filter(c => (c.id || c.conversationId) !== id)
    if (activeConversationId.value === String(id)) {
      newConversation()
    }
    ElMessage.success('已删除')
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
.chat-container { display: flex; height: 100vh; overflow: hidden; }

.chat-sidebar {
  width: 280px; background: var(--color-sidebar-bg); border-right: 1px solid var(--color-border);
  display: flex; flex-direction: column; flex-shrink: 0;
}
.sidebar-header { padding: var(--space-md); border-bottom: 1px solid var(--color-border); }
.sidebar-title { font-size: var(--font-size-lg); margin: 0 0 12px 0; color: var(--color-text); font-weight: 600; }
.sidebar-conversations { flex: 1; overflow-y: auto; }
.sidebar-conversations .el-menu { border-right: none; background: transparent; }
.sidebar-conversations .el-menu-item {
  font-size: var(--font-size-sm); color: var(--color-text-secondary); border-radius: var(--radius-sm);
  margin: 2px 6px; height: 40px; line-height: 40px;
}
.sidebar-conversations .el-menu-item:hover { background: var(--color-sidebar-hover); color: var(--color-text); }
.sidebar-conversations .el-menu-item.is-active {
  background: var(--color-sidebar-active-bg); color: var(--color-sidebar-active-text); font-weight: 600;
}
.sidebar-empty { text-align: center; color: var(--color-text-muted); padding: 24px; font-size: var(--font-size-sm); }
.sidebar-footer {
  padding: 12px var(--space-md); border-top: 1px solid var(--color-border);
  display: flex; flex-direction: column; gap: 4px;
}

.conv-title-wrapper { display: flex; align-items: center; flex: 1; min-width: 0; }
.conv-title { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.edit-icon { display: none; margin-left: 4px; font-size: 14px; color: var(--color-text-muted); cursor: pointer; flex-shrink: 0; }
.conv-title-wrapper:hover .edit-icon { display: inline-flex; }
.delete-btn { display: none; position: absolute; right: 8px; }
.el-menu-item:hover .delete-btn { display: inline-flex; }

.chat-main { flex: 1; display: flex; flex-direction: column; background: var(--color-bg); }
.messages-container { flex: 1; overflow-y: auto; padding: var(--space-lg); }
.messages-empty {
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  height: 100%; color: var(--color-text-muted);
}
.messages-empty p { margin: 8px 0 0; font-size: var(--font-size-lg); }
.messages-hint { font-size: var(--font-size-sm) !important; margin-top: 4px !important; }

.streaming-indicator { display: flex; gap: 4px; padding: 12px 0; align-items: center; }
.streaming-dot { width: 8px; height: 8px; border-radius: 50%; background: var(--color-accent); animation: bounce 1.4s infinite ease-in-out both; }
.streaming-dot:nth-child(1) { animation-delay: -0.32s; }
.streaming-dot:nth-child(2) { animation-delay: -0.16s; }
.streaming-dot:nth-child(3) { animation-delay: 0s; }
@keyframes bounce { 0%, 80%, 100% { transform: scale(0); } 40% { transform: scale(1); } }

.input-area {
  padding: var(--space-md) var(--space-lg); background: var(--color-surface);
  border-top: 1px solid var(--color-border); display: flex; align-items: flex-start;
}
</style>
