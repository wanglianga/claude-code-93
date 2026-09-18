import axios from 'axios'
import { ElMessage } from 'element-plus'

const api = axios.create({ baseURL: '/api', timeout: 15000 })

api.interceptors.request.use(cfg => {
  const token = localStorage.getItem('token')
  if (token) cfg.headers['X-Auth-Token'] = token
  return cfg
})

api.interceptors.response.use(
  resp => {
    const body = resp.data
    if (body && typeof body === 'object' && 'code' in body) {
      if (body.code !== 0) {
        ElMessage.error(body.message || '操作失败')
        return Promise.reject(new Error(body.message))
      }
      return body.data
    }
    return body
  },
  err => {
    const status = err.response?.status
    const msg = err.response?.data?.message || err.message
    if (status === 401) {
      localStorage.clear()
      if (!location.hash.startsWith('#/board')) location.hash = '#/login'
    }
    ElMessage.error(msg || '网络异常')
    return Promise.reject(err)
  }
)

export function currentUser() {
  const raw = localStorage.getItem('user')
  return raw ? JSON.parse(raw) : null
}

export function logout() {
  localStorage.clear()
  location.hash = '#/login'
}

export default api
