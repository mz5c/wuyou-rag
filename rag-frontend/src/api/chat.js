import request from '../utils/request'

export function createConversation(data) {
  return request.post('/api/v1/chat/conversation', data)
}

export function listConversations() {
  return request.get('/api/v1/chat/conversations')
}

export function chat(data) {
  return request.post('/api/v1/chat', data)
}

export function getMessages(conversationId, params) {
  return request.get(`/api/v1/chat/conversation/${conversationId}/messages`, { params })
}

export function deleteConversation(id) {
  return request.delete(`/api/v1/chat/conversation/${id}`)
}

export function feedback(data) {
  return request.post('/api/v1/chat/feedback', data)
}

export function updateConversationTitle(id, data) {
  return request.put(`/api/v1/chat/conversation/${id}/title`, data)
}

export function getStreamUrl(params) {
  const query = new URLSearchParams(params).toString()
  return `/api/v1/chat/stream?${query}`
}
