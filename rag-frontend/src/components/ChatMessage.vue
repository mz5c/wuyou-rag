<template>
  <div :class="['chat-message', `message-${message.role}`]">
    <div class="message-avatar">
      <el-avatar v-if="message.role === 'user'" :size="36" icon="UserFilled" />
      <el-avatar v-else :size="36" icon="MagicStick" style="background-color: #409eff" />
    </div>
    <div class="message-content-wrapper">
      <div class="message-content" v-html="formattedContent"></div>
      <div v-if="message.timestamp" class="message-time">{{ message.timestamp }}</div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  message: {
    type: Object,
    required: true
  }
})

const formattedContent = computed(() => {
  const text = props.message.content || ''
  return text
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/\n/g, '<br>')
})
</script>

<style scoped>
.chat-message {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
  max-width: 85%;
}

.message-user {
  flex-direction: row-reverse;
  margin-left: auto;
}

.message-assistant {
  flex-direction: row;
  margin-right: auto;
}

.message-system {
  justify-content: center;
  max-width: 100%;
}

.message-system .message-content-wrapper {
  background-color: #f0f2f5;
  border-radius: 8px;
  padding: 8px 16px;
  text-align: center;
  color: #909399;
  font-size: 13px;
}

.message-system .message-avatar {
  display: none;
}

.message-avatar {
  flex-shrink: 0;
  margin-top: 4px;
}

.message-content-wrapper {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.message-user .message-content {
  background-color: #409eff;
  color: #fff;
  border-radius: 12px 4px 12px 12px;
  padding: 10px 14px;
  font-size: 14px;
  line-height: 1.6;
}

.message-assistant .message-content {
  background-color: #fff;
  color: #303133;
  border-radius: 4px 12px 12px 12px;
  padding: 10px 14px;
  font-size: 14px;
  line-height: 1.6;
  border: 1px solid #ebeef5;
}

.message-time {
  font-size: 12px;
  color: #c0c4cc;
  padding: 0 4px;
}

.message-user .message-time {
  text-align: right;
}
</style>
