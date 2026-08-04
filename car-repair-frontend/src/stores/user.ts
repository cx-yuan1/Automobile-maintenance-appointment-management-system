import { defineStore } from 'pinia'
import { ref } from 'vue'
import request from '@/utils/request'
import { wsClient } from '@/utils/websocket'

interface UserInfo {
  userId: number
  username: string
  realName: string
  userType: number
  phone: string
  customerLevel?: number
}

export const useUserStore = defineStore('user', () => {
  const userInfo = ref<UserInfo | null>(null)
  const isLoggedIn = ref(false)

  // 获取当前用户信息
  async function getCurrentUser() {
    try {
      const res = await request.get('/common/currentUser')
      userInfo.value = res.data
      isLoggedIn.value = true
      
      // 登录成功后连接WebSocket（仅客户端用户）
      if (res.data && res.data.userType === 1) {
        wsClient.connect(res.data.userId)
      }
      
      return res.data
    } catch {
      userInfo.value = null
      isLoggedIn.value = false
      return null
    }
  }

  // 登出
  async function logout() {
    try {
      await request.post('/common/logout')
    } finally {
      // 断开WebSocket连接
      wsClient.disconnect()
      userInfo.value = null
      isLoggedIn.value = false
    }
  }

  // 设置用户信息（登录成功后调用）
  function setUserInfo(info: UserInfo) {
    userInfo.value = info
    isLoggedIn.value = true
    
    // 登录成功后连接WebSocket（仅客户端用户）
    if (info && info.userType === 1) {
      wsClient.connect(info.userId)
    }
  }

  return {
    userInfo,
    isLoggedIn,
    getCurrentUser,
    logout,
    setUserInfo
  }
})
