// src/utils/request.js
import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'

// 创建 axios 实例
const request = axios.create({
  baseURL: 'http://localhost:8080', // 对应我们 SpringBoot 后端的端口
  timeout: 10000
})

// 请求拦截器
request.interceptors.request.use(
  config => {
    // 尝试从 localStorage 获取前端用户 token 或后台管理员 token
    // 我们约定：观众端 token 存在 'userToken'，后台 token 存在 'adminToken'
    const token = localStorage.getItem('userToken') || localStorage.getItem('adminToken')
    
    if (token) {
      config.headers['Authorization'] = token
    }
    return config
  },
  error => {
    return Promise.reject(error)
  }
)

// 响应拦截器
request.interceptors.response.use(
  response => {
    // 解析我们后端的 Result 统一返回格式
    const res = response.data
    
    // 如果 code 是 200，说明业务成功
    if (res.code === 200) {
      return res.data
    } else {
      // 业务错误，弹出提示
      ElMessage.error(res.msg || '系统错误')
      return Promise.reject(new Error(res.msg || 'Error'))
    }
  },
  error => {
    // HTTP 状态码错误处理 (应对后端拦截器拦截的情况)
    if (error.response) {
      const status = error.response.status
      if (status === 401) {
        ElMessage.error('登录状态已过期或未登录，请重新登录')
        // 清除失效的 token 并跳转登录页
        localStorage.removeItem('userToken')
        localStorage.removeItem('adminToken')
        router.push('/login')
      } else if (status === 403) {
        ElMessage.error('您没有权限访问该资源')
      } else {
        ElMessage.error('网络请求异常，请稍后重试')
      }
    }
    return Promise.reject(error)
  }
)

export default request