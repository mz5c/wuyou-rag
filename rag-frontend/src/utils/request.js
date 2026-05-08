import axios from 'axios'
import { WMessage } from '../components/ui/WMessage'

const request = axios.create({
  baseURL: '',
  timeout: 60000
})

request.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

request.interceptors.response.use(
  (response) => {
    const res = response.data
    if (res.code !== undefined && res.code !== 200 && res.code !== 0) {
      WMessage.error(res.message || '请求失败')
      return Promise.reject(new Error(res.message || '请求失败'))
    }
    return response
  },
  (error) => {
    if (error.response) {
      const { status } = error.response
      if (status === 401) {
        localStorage.removeItem('token')
        window.location.href = '/login'
      } else if (status === 403) {
        WMessage.error('无权限访问')
      } else if (status === 500) {
        WMessage.error('服务器错误')
      } else {
        WMessage.error(error.response.data?.message || `请求失败 (${status})`)
      }
    } else if (error.code === 'ECONNABORTED') {
      WMessage.error('请求超时')
    } else {
      WMessage.error('网络错误')
    }
    return Promise.reject(error)
  }
)

export default request
