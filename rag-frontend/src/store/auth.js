import { reactive } from 'vue'

const user = reactive({
  token: localStorage.getItem('token') || '',
  userId: null,
  username: '',
  nickname: '',
  role: ''
})

export function useAuth() {
  function setUser(u) {
    user.token = u.token
    user.userId = u.userId
    user.username = u.username
    user.nickname = u.nickname
    user.role = u.role
    localStorage.setItem('token', u.token)
  }

  function logout() {
    user.token = ''
    user.userId = null
    user.username = ''
    user.nickname = ''
    user.role = ''
    localStorage.removeItem('token')
  }

  function isLoggedIn() {
    return !!user.token
  }

  function isAdmin() {
    return user.role === 'ADMIN'
  }

  return { user, setUser, logout, isLoggedIn, isAdmin }
}
