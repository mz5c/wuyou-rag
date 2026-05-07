import request from '../utils/request'

export function listKnowledgeBases() {
  return request.get('/api/v1/knowledge-bases')
}

export function getKnowledgeBaseById(id) {
  return request.get(`/api/v1/knowledge-bases/${id}`)
}

export function createKnowledgeBase(data) {
  return request.post('/api/v1/knowledge-bases', data)
}

export function updateKnowledgeBase(id, data) {
  return request.put(`/api/v1/knowledge-bases/${id}`, data)
}

export function deleteKnowledgeBase(id) {
  return request.delete(`/api/v1/knowledge-bases/${id}`)
}
