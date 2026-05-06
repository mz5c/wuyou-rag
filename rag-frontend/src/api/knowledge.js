import request from '../utils/request'

export function listKnowledgeBases() {
  return request.get('/api/v1/knowledge/list')
}

export function getKnowledgeBaseById(id) {
  return request.get(`/api/v1/knowledge/${id}`)
}

export function createKnowledgeBase(data) {
  return request.post('/api/v1/knowledge', data)
}

export function updateKnowledgeBase(id, data) {
  return request.put(`/api/v1/knowledge/${id}`, data)
}

export function deleteKnowledgeBase(id) {
  return request.delete(`/api/v1/knowledge/${id}`)
}
