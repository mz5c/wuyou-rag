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
            <span>{{ conv.title || conv.topic || '新对话' }}</span>
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
  Promotion, ChatLineSquare
} from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import ChatMessage from '../components/ChatMessage.vue'
import SourceReference from '../components/SourceReference.vue'
import {
  chat, getMessages, listConversations,
  deleteConversation, createConversation
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
    const res = await getMessages(id)
    const list = res.data?.data || res.data || []
    messages.value = Array.isArray(list) ? list : []
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
.chat-container {
  display: flex;
  height: 100vh;
  overflow: hidden;
}

.chat-sidebar {
  width: 280px;
  background: #fff;
  border-right: 1px solid #e4e7ed;
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
}

.sidebar-header {
  padding: 16px;
  border-bottom: 1px solid #e4e7ed;
}

.sidebar-title {
  font-size: 18px;
  margin: 0 0 12px 0;
  color: #303133;
}

.sidebar-conversations {
  flex: 1;
  overflow-y: auto;
}

.sidebar-empty {
  text-align: center;
  color: #c0c4cc;
  padding: 24px;
  font-size: 14px;
}

.sidebar-footer {
  padding: 12px 16px;
  border-top: 1px solid #e4e7ed;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.delete-btn {
  display: none;
  position: absolute;
  right: 8px;
}

.el-menu-item:hover .delete-btn {
  display: inline-flex;
}

.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: #f5f7fa;
}

.messages-container {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
}

.messages-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: #c0c4cc;
}

.messages-empty p {
  margin: 8px 0 0;
  font-size: 16px;
}

.messages-hint {
  font-size: 13px !important;
  margin-top: 4px !important;
}

.streaming-indicator {
  display: flex;
  gap: 4px;
  padding: 12px 0;
  align-items: center;
}

.streaming-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #409eff;
  animation: bounce 1.4s infinite ease-in-out both;
}

.streaming-dot:nth-child(1) { animation-delay: -0.32s; }
.streaming-dot:nth-child(2) { animation-delay: -0.16s; }
.streaming-dot:nth-child(3) { animation-delay: 0s; }

@keyframes bounce {
  0%, 80%, 100% { transform: scale(0); }
  40% { transform: scale(1); }
}

.input-area {
  padding: 16px 24px;
  background: #fff;
  border-top: 1px solid #e4e7ed;
  display: flex;
  align-items: flex-start;
}
</style>
