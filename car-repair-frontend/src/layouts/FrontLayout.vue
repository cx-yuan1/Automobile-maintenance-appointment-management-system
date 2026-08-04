<template>
  <div class="front-layout">
    <el-header class="header">
      <div class="logo">汽车维修预约平台</div>
      <el-menu mode="horizontal" :default-active="route.path" router :ellipsis="false">
        <el-menu-item index="/">首页</el-menu-item>
        <el-menu-item index="/vehicle">我的车辆</el-menu-item>
        <el-menu-item index="/booking">预约维修</el-menu-item>
        <el-menu-item index="/order">我的订单</el-menu-item>
        <el-menu-item index="/message" class="message-menu-item">
          <el-badge :value="unreadCount" :hidden="unreadCount === 0" :max="99" class="message-badge">
            <span>消息中心</span>
          </el-badge>
        </el-menu-item>
        <el-menu-item index="/profile">个人中心</el-menu-item>
      </el-menu>
      <div class="user-info">
        <el-dropdown @command="handleCommand">
          <span class="el-dropdown-link">
            {{ userStore.userInfo?.realName || '用户' }}
            <el-icon class="el-icon--right"><arrow-down /></el-icon>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="profile">个人中心</el-dropdown-item>
              <el-dropdown-item command="logout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </el-header>
    <el-main class="main">
      <router-view />
    </el-main>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const unreadCount = ref(0)

let ws: WebSocket | null = null

const loadUnreadCount = async () => {
  try {
    const res = await request.get('/front/message/unreadCount')
    unreadCount.value = res.data || 0
  } catch { /* 忽略错误 */ }
}

const handleCommand = async (command: string) => {
  if (command === 'logout') {
    await userStore.logout()
    router.push('/login')
  } else if (command === 'profile') {
    router.push('/profile')
  }
}

// 初始化WebSocket连接
const initWebSocket = () => {
  if (!userStore.userInfo?.id) return
  
  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
  const wsUrl = `${protocol}//${window.location.hostname}:8080/ws/${userStore.userInfo.id}`
  
  ws = new WebSocket(wsUrl)
  
  ws.onopen = () => {
    console.log('WebSocket连接已建立')
  }
  
  ws.onmessage = (event) => {
    try {
      const data = JSON.parse(event.data)
      if (data.type === 'NEW_MESSAGE') {
        // 收到新消息,刷新未读数
        loadUnreadCount()
        // 显示通知
        ElMessage.info({
          message: `新消息: ${data.title}`,
          duration: 3000
        })
      }
    } catch (error) {
      console.error('WebSocket消息解析失败:', error)
    }
  }
  
  ws.onerror = (error) => {
    console.error('WebSocket错误:', error)
  }
  
  ws.onclose = () => {
    console.log('WebSocket连接已关闭')
  }
  
  // 心跳保持连接
  setInterval(() => {
    if (ws && ws.readyState === WebSocket.OPEN) {
      ws.send(JSON.stringify({ type: 'PING' }))
    }
  }, 30000)
}

onMounted(() => {
  loadUnreadCount()
  initWebSocket()
  // 监听来自Message页面的通知
  window.addEventListener('update-unread-count', loadUnreadCount)
})

onUnmounted(() => {
  if (ws) {
    ws.close()
  }
  window.removeEventListener('update-unread-count', loadUnreadCount)
})
</script>

<style scoped>
.front-layout { min-height: 100vh; }
.header { display: flex; align-items: center; background: #fff; box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1); padding: 0 20px; }
.logo { font-size: 20px; font-weight: bold; color: #5a9bd5; margin-right: 40px; }
.user-info { margin-left: auto; }
.el-dropdown-link { cursor: pointer; color: #5a9bd5; display: flex; align-items: center; }
.main { padding: 20px; max-width: 1200px; margin: 0 auto; }

/* 消息中心徽章样式 */
.message-menu-item {
  display: flex !important;
  align-items: center !important;
}

.message-badge {
  display: inline-flex;
  align-items: center;
  line-height: normal;
}

.message-badge :deep(.el-badge__content) {
  top: 0;
  right: -5px;
  transform: translateY(-50%) translateX(100%);
  border: none;
}
</style>
