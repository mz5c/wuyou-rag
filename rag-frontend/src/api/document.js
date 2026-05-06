import request from '../utils/request'

export function uploadDocument(kbId, file) {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('kbId', kbId)
  return request.post('/api/v1/document/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export function listDocuments(kbId, params) {
  return request.get(`/api/v1/document/list/${kbId}`, { params })
}

export function getDocumentById(id) {
  return request.get(`/api/v1/document/${id}`)
}

export function deleteDocument(id) {
  return request.delete(`/api/v1/document/${id}`)
}

export function getDocumentProcessStatus(id) {
  return request.get(`/api/v1/document/${id}/status`)
}

export function reprocessDocument(id) {
  return request.post(`/api/v1/document/${id}/reprocess`)
}
