// src/stores/user.js
import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useUserStore = defineStore('user', () => {
  // 尝试从本地存储初始化数据
  const token = ref(localStorage.getItem('userToken') || '')
  const userInfo = ref(JSON.parse(localStorage.getItem('userInfo') || '{}'))

  // 保存登录数据
  const setLoginInfo = (newToken, info) => {
    token.value = newToken
    userInfo.value = info
    localStorage.setItem('userToken', newToken)
    localStorage.setItem('userInfo', JSON.stringify(info))
  }

  // 退出登录清除数据
  const logout = () => {
    token.value = ''
    userInfo.value = {}
    localStorage.removeItem('userToken')
    localStorage.removeItem('userInfo')
  }

  return { token, userInfo, setLoginInfo, logout }
})