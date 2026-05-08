import request from '../utils/request'

export function getAdminStats() {
  return request.get('/api/v1/admin/dashboard/stats')
}

export function getAuditLogs(params) {
  return request.get('/api/v1/admin/audit-logs', { params })
}

export function getConfigs() {
  return request.get('/api/v1/admin/config')
}

export function updateConfig(data) {
  return request.put('/api/v1/admin/config', data)
}

export function getUsers(params) {
  return request.get('/api/v1/admin/users', { params })
}

export function updateUser(id, data) {
  return request.put(`/api/v1/admin/users/${id}`, data)
}

export function createUser(data) {
  return request.post('/api/v1/admin/users', data)
}
