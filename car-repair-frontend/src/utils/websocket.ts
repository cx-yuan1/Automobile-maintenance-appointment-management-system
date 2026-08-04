/**
 * WebSocket 封装工具
 * 用于维修进度实时推送
 */
import { ElNotification } from 'element-plus'

// WebSocket 消息类型
export interface WsMessage {
  type: string
  orderId?: number
  orderNo?: string
  status?: string
  description?: string
  operatorName?: string
  imageUrl?: string
  createTime?: string
}

// 消息回调函数类型
type MessageCallback = (message: WsMessage) => void

class WebSocketClient {
  private ws: WebSocket | null = null
  private url: string = ''
  private reconnectTimer: number | null = null
  private heartbeatTimer: number | null = null
  private callbacks: MessageCallback[] = []
  private isManualClose: boolean = false
  private reconnectAttempts: number = 0
  private maxReconnectAttempts: number = 5

  /**
   * 连接 WebSocket
   * @param userId 用户ID
   */
  connect(userId: string | number): void {
    if (this.ws && this.ws.readyState === WebSocket.OPEN) {
      return
    }

    this.isManualClose = false
    this.reconnectAttempts = 0
    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
    this.url = `${protocol}//localhost:8080/ws/repair/${userId}`

    this.createConnection()
  }

  /**
   * 创建WebSocket连接
   */
  private createConnection(): void {
    try {
      this.ws = new WebSocket(this.url)

      this.ws.onopen = () => {
        console.log('WebSocket 连接成功')
        this.reconnectAttempts = 0
        this.startHeartbeat()
      }

      this.ws.onmessage = (event) => {
        try {
          const message: WsMessage = JSON.parse(event.data)
          // 忽略心跳响应
          if (message.type === 'PONG') return
          
          // 处理不同类型的消息
          this.handleMessage(message)
          
          // 触发所有回调
          this.callbacks.forEach(cb => cb(message))
        } catch (e) {
          console.error('WebSocket 消息解析失败:', e)
        }
      }

      this.ws.onclose = () => {
        console.log('WebSocket 连接关闭')
        this.stopHeartbeat()
        // 非手动关闭时自动重连
        if (!this.isManualClose) {
          this.reconnect()
        }
      }

      this.ws.onerror = (error) => {
        console.error('WebSocket 错误:', error)
      }
    } catch (e) {
      console.error('WebSocket 创建失败:', e)
    }
  }

  /**
   * 处理接收到的消息
   */
  private handleMessage(message: WsMessage): void {
    switch (message.type) {
      case 'PROGRESS_UPDATE':
        // 维修进度更新通知
        ElNotification({
          title: '维修进度更新',
          message: `工单 ${message.orderNo} 有新进度：${message.status}`,
          type: 'info',
          duration: 5000,
          position: 'top-right'
        })
        break
      case 'BOOKING_CONFIRM':
        // 预约确认通知
        ElNotification({
          title: '预约确认',
          message: `您的预约已确认，请按时到店`,
          type: 'success',
          duration: 5000,
          position: 'top-right'
        })
        break
      case 'ORDER_COMPLETE':
        // 维修完成通知
        ElNotification({
          title: '维修完成',
          message: `工单 ${message.orderNo} 已完成，请前往结算`,
          type: 'success',
          duration: 5000,
          position: 'top-right'
        })
        break
      default:
        // 其他消息类型
        if (message.status) {
          ElNotification({
            title: '系统通知',
            message: message.status,
            type: 'info',
            duration: 5000,
            position: 'top-right'
          })
        }
    }
  }

  /**
   * 断开连接
   */
  disconnect(): void {
    this.isManualClose = true
    this.stopHeartbeat()
    this.stopReconnect()
    if (this.ws) {
      this.ws.close()
      this.ws = null
    }
  }

  /**
   * 注册消息回调
   * @param callback 回调函数
   */
  onMessage(callback: MessageCallback): void {
    this.callbacks.push(callback)
  }

  /**
   * 移除消息回调
   * @param callback 回调函数
   */
  offMessage(callback: MessageCallback): void {
    const index = this.callbacks.indexOf(callback)
    if (index > -1) {
      this.callbacks.splice(index, 1)
    }
  }

  /**
   * 启动心跳
   */
  private startHeartbeat(): void {
    this.heartbeatTimer = window.setInterval(() => {
      if (this.ws && this.ws.readyState === WebSocket.OPEN) {
        this.ws.send(JSON.stringify({ type: 'PING' }))
      }
    }, 30000) // 30秒发送一次心跳
  }

  /**
   * 停止心跳
   */
  private stopHeartbeat(): void {
    if (this.heartbeatTimer) {
      clearInterval(this.heartbeatTimer)
      this.heartbeatTimer = null
    }
  }

  /**
   * 重连
   */
  private reconnect(): void {
    if (this.reconnectAttempts >= this.maxReconnectAttempts) {
      console.log('WebSocket 重连次数已达上限，停止重连')
      return
    }
    
    this.stopReconnect()
    this.reconnectAttempts++
    
    // 使用指数退避策略，每次重连间隔递增
    const delay = Math.min(1000 * Math.pow(2, this.reconnectAttempts), 30000)
    
    this.reconnectTimer = window.setTimeout(() => {
      console.log(`WebSocket 尝试第 ${this.reconnectAttempts} 次重连...`)
      this.createConnection()
    }, delay)
  }

  /**
   * 停止重连
   */
  private stopReconnect(): void {
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer)
      this.reconnectTimer = null
    }
  }

  /**
   * 获取连接状态
   */
  isConnected(): boolean {
    return this.ws !== null && this.ws.readyState === WebSocket.OPEN
  }
}

// 导出单例
export const wsClient = new WebSocketClient()
