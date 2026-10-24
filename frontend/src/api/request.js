import axios from 'axios'
import { ElMessage } from 'element-plus'

// 与 Vite base 保持一致：独立部署为 /api，war 部署在 /archive 下则为 /archive/api。
const base = import.meta.env.BASE_URL || '/'
const loginPath = `${base}login`

const service = axios.create({
  baseURL: import.meta.env.VITE_API_BASE || `${base}api`,
  timeout: 60000
})

service.interceptors.request.use((config) => {
  const token = localStorage.getItem('archive_token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

function handleUnauthorized() {
  localStorage.removeItem('archive_token')
  localStorage.removeItem('archive_profile')
  if (!window.location.pathname.startsWith(loginPath)) {
    // redirect 使用应用内路径（去掉上下文前缀），登录后由 vue-router 自动补回 base。
    const fullPath = window.location.pathname + window.location.search
    const appPath = base !== '/' && fullPath.startsWith(base)
      ? `/${fullPath.slice(base.length)}`
      : fullPath
    window.location.href = `${loginPath}?redirect=${encodeURIComponent(appPath)}`
  }
}

service.interceptors.response.use(
  (response) => {
    if (response.config.responseType === 'blob') {
      return response
    }
    const body = response.data
    if (body && body.code === 200) {
      return body.data
    }
    const message = body?.message || '请求处理失败'
    ElMessage.error(message)
    return Promise.reject(new Error(message))
  },
  (error) => {
    const status = error.response?.status
    let message = error.response?.data?.message
    if (error.response?.data instanceof Blob) {
      message = '文件处理失败，请稍后重试'
    }
    if (status === 401) {
      ElMessage.error(message || '登录已过期，请重新登录')
      handleUnauthorized()
    } else if (status === 403) {
      ElMessage.error(message || '您没有权限执行该操作')
    } else if (status === 400) {
      ElMessage.error(message || '请求参数不正确')
    } else if (error.code === 'ECONNABORTED') {
      ElMessage.error('请求超时，请稍后重试')
    } else {
      ElMessage.error(message || (status ? '服务处理失败' : '无法连接服务器，请检查后端服务'))
    }
    return Promise.reject(error)
  }
)

export default service
