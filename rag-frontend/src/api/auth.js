import request from '../utils/request'

export function login(username, password) {
  return request.post('/api/v1/auth/login', { username, password })
}

export function logout() {
  return request.post('/api/v1/auth/logout')
}

export function getUserInfo() {
  return request.get('/api/v1/auth/userinfo')
}

export function register(data) {
  return request.post('/api/v1/auth/register', data)
}
