import { reactive } from 'vue'

const STORAGE_KEY = 'user_info'

function loadUserInfo() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (raw) return JSON.parse(raw)
  } catch {
    // corrupted data
  }
  return null
}

function saveUserInfo(u) {
  localStorage.setItem('token', u.token)
  localStorage.setItem(STORAGE_KEY, JSON.stringify({
    token: u.token,
    userId: u.userId,
    username: u.username,
    nickname: u.nickname,
    role: u.role
  }))
}

const saved = loadUserInfo()

const user = reactive({
  token: (saved && saved.token) || '',
  userId: saved ? saved.userId : null,
  username: saved ? saved.username : '',
  nickname: saved ? saved.nickname : '',
  role: saved ? saved.role : ''
})

export function useAuth() {
  function setUser(u) {
    user.token = u.token
    user.userId = u.userId
    user.username = u.username
    user.nickname = u.nickname
    user.role = u.role
    saveUserInfo(user)
  }

  function logout() {
    user.token = ''
    user.userId = null
    user.username = ''
    user.nickname = ''
    user.role = ''
    localStorage.removeItem('token')
    localStorage.removeItem(STORAGE_KEY)
  }

  function isLoggedIn() {
    return !!user.token
  }

  function isAdmin() {
    return user.role === 'ADMIN'
  }

  return { user, setUser, logout, isLoggedIn, isAdmin }
}
