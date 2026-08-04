<template>
  <div class="message-page">
    <el-card>
      <template #header>
        <div style="display: flex; justify-content: space-between; align-items: center">
          <span>消息中心</span>
          <el-button type="primary" size="small" @click="readAll" :disabled="!hasUnread">全部已读</el-button>
        </div>
      </template>
      
      <el-tabs v-model="activeTab" @tab-change="loadMessages">
        <el-tab-pane label="全部消息" name="all" />
        <el-tab-pane label="未读消息" name="unread" />
      </el-tabs>

      <div class="message-list" v-loading="loading">
        <div v-for="msg in messages" :key="msg.id" class="message-item" :class="{ unread: msg.isRead === 0 }">
          <div class="message-header">
            <span class="message-title">
              <el-badge is-dot :hidden="msg.isRead === 1">{{ msg.title }}</el-badge>
            </span>
            <span class="message-time">{{ msg.createTime }}</span>
          </div>
          <div class="message-content">{{ msg.content }}</div>
          <div class="message-actions">
            <el-button v-if="msg.isRead === 0" type="primary" link size="small" @click="markRead(msg)">标记已读</el-button>
            <el-button type="danger" link size="small" @click="deleteMsg(msg)">删除</el-button>
          </div>
        </div>
        <el-empty v-if="!loading && !messages.length" description="暂无消息" />
      </div>

      <el-pagination v-if="total > 0" background layout="prev, pager, next" :total="total" :page-size="pageSize"
        v-model:current-page="currentPage" @current-change="loadMessages" style="margin-top: 20px; justify-content: center" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'
import request from '@/utils/request'

const userStore = useUserStore()
const loading = ref(false)
const messages = ref<any[]>([])
const activeTab = ref('all')
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const unreadCount = ref(0)

let ws: WebSocket | null = null

const hasUnread = computed(() => unreadCount.value > 0)

const loadMessages = async () => {
  loading.value = true
  try {
    const params: any = { page: currentPage.value, size: pageSize.value }
    if (activeTab.value === 'unread') {
      params.isRead = 0
    }
    const res = await request.get('/front/message/list', { params })
    messages.value = res.data.list || []
    total.value = res.data.total || 0
  } finally {
    loading.value = false
  }
}

const loadUnreadCount = async () => {
  try {
    const res = await request.get('/front/message/unreadCount')
    unreadCount.value = res.data || 0
  } catch { /* 忽略错误 */ }
}

const markRead = async (msg: any) => {
  try {
    await request.put(`/front/message/read/${msg.id}`)
    msg.isRead = 1
    unreadCount.value = Math.max(0, unreadCount.value - 1)
    // 触发全局事件通知布局更新未读数
    window.dispatchEvent(new CustomEvent('update-unread-count'))
    ElMessage.success('已标记为已读')
  } catch { /* 忽略错误 */ }
}

const readAll = async () => {
  try {
    await request.put('/front/message/readAll')
    messages.value.forEach(m => m.isRead = 1)
    unreadCount.value = 0
    // 触发全局事件通知布局更新未读数
    window.dispatchEvent(new CustomEvent('update-unread-count'))
    ElMessage.success('全部标记为已读')
  } catch { /* 忽略错误 */ }
}

const deleteMsg = async (msg: any) => {
  try {
    await request.delete(`/front/message/delete/${msg.id}`)
    ElMessage.success('删除成功')
    loadMessages()
    if (msg.isRead === 0) {
      unreadCount.value = Math.max(0, unreadCount.value - 1)
      // 触发全局事件通知布局更新未读数
      window.dispatchEvent(new CustomEvent('update-unread-count'))
    }
  } catch { /* 忽略错误 */ }
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
        // 收到新消息，刷新列表和未读数
        loadMessages()
        loadUnreadCount()
        // 显示通知
        ElMessage.info({
          message: `新消息：${data.title}`,
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
  loadMessages()
  loadUnreadCount()
  initWebSocket()
})

onUnmounted(() => {
  if (ws) {
    ws.close()
  }
})
</script>

<style scoped>
.message-page { max-width: 800px; margin: 0 auto; }
.message-list { min-height: 300px; }
.message-item { padding: 15px; border-bottom: 1px solid #eee; transition: background 0.3s; }
.message-item:hover { background: #f5f9fc; }
.message-item.unread { background: #f0f7fc; }
.message-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }
.message-title { font-weight: 500; color: #303133; }
.message-time { font-size: 12px; color: #909399; }
.message-content { color: #606266; font-size: 14px; line-height: 1.6; }
.message-actions { margin-top: 10px; text-align: right; }
</style>
