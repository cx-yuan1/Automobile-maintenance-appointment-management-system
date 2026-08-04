<template>
  <div class="order-detail-page" v-loading="loading">
    <el-page-header @back="router.back()" content="订单详情" />
    
    <el-card v-if="detail" style="margin-top: 20px">
      <el-descriptions title="工单信息" :column="2" border>
        <el-descriptions-item label="工单编号">{{ detail.order.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="statusType[detail.order.status]">{{ statusMap[detail.order.status] }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="车辆">{{ detail.vehicle?.plateNumber }} {{ detail.vehicle?.brand }}</el-descriptions-item>
        <el-descriptions-item label="维修人员">{{ detail.technician?.realName || '待分配' }}</el-descriptions-item>
        <el-descriptions-item label="工时费">¥{{ detail.order.laborCost }}</el-descriptions-item>
        <el-descriptions-item label="配件费">¥{{ detail.order.partsCost }}</el-descriptions-item>
        <el-descriptions-item label="总金额">
          <span class="price">¥{{ detail.order.totalAmount }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="支付状态">
          <el-tag :type="detail.order.paymentStatus === 1 ? 'success' : 'warning'">
            {{ detail.order.paymentStatus === 1 ? '已支付' : '未支付' }}
          </el-tag>
        </el-descriptions-item>
      </el-descriptions>
    </el-card>

    <el-card v-if="detail?.progress?.length" style="margin-top: 20px">
      <template #header>
        <span>维修进度</span>
      </template>
      <el-timeline>
        <el-timeline-item v-for="p in detail.progress" :key="p.id" :timestamp="p.createTime" placement="top">
          <el-card>
            <h4>{{ p.progressStatus }}</h4>
            <p>{{ p.progressDesc }}</p>
            <p class="operator">操作人：{{ p.operatorName }}</p>
          </el-card>
        </el-timeline-item>
      </el-timeline>
    </el-card>

    <!-- 评价 -->
    <el-card v-if="detail?.order?.status === 5 && detail?.order?.paymentStatus === 1" style="margin-top: 20px">
      <template #header>
        <span>服务评价</span>
      </template>
      <el-form v-if="!evaluated" :model="evalForm" label-width="80px">
        <el-form-item label="评分">
          <el-rate v-model="evalForm.score" />
        </el-form-item>
        <el-form-item label="评价内容">
          <el-input v-model="evalForm.content" type="textarea" placeholder="请输入评价内容" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="submitEvaluation" :loading="submitting">提交评价</el-button>
        </el-form-item>
      </el-form>
      <div v-else>
        <p>您已评价，感谢您的反馈！</p>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElNotification } from 'element-plus'
import request from '@/utils/request'
import { wsClient, type WsMessage } from '@/utils/websocket'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)
const detail = ref<any>(null)
const evaluated = ref(false)
const submitting = ref(false)

const statusMap: Record<number, string> = { 1: '待接待', 2: '维修中', 3: '待质检', 4: '待结算', 5: '已完成', 6: '已取消' }
const statusType: Record<number, string> = { 1: 'info', 2: 'warning', 3: 'primary', 4: 'danger', 5: 'success', 6: '' }

const evalForm = reactive({
  score: 5,
  content: ''
})

const loadData = async () => {
  loading.value = true
  try {
    const res = await request.get(`/front/order/detail/${route.params.id}`)
    detail.value = res.data
    // 检查是否已评价
    if (res.data.evaluation) {
      evaluated.value = true
    }
  } finally {
    loading.value = false
  }
}

/**
 * 处理 WebSocket 推送的进度更新消息
 */
const handleWsMessage = (message: WsMessage) => {
  // 只处理当前订单的进度更新
  if (message.type === 'PROGRESS_UPDATE' && message.orderId === Number(route.params.id)) {
    // 显示通知
    ElNotification({
      title: '维修进度更新',
      message: message.description || '您的订单有新的进度更新',
      type: 'success',
      duration: 5000
    })
    // 重新加载数据
    loadData()
  }
}

const submitEvaluation = async () => {
  submitting.value = true
  try {
    await request.post('/front/evaluation/submit', {
      orderId: Number(route.params.id),
      score: evalForm.score,
      content: evalForm.content
    })
    ElMessage.success('评价成功')
    evaluated.value = true
  } catch {
    // 错误已在拦截器处理
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  loadData()
  // 连接 WebSocket 并监听消息
  if (userStore.userInfo?.id) {
    wsClient.connect(userStore.userInfo.id)
    wsClient.onMessage(handleWsMessage)
  }
})

onUnmounted(() => {
  // 移除消息监听
  wsClient.offMessage(handleWsMessage)
})
</script>

<style scoped>
.price {
  font-size: 20px;
  color: #f56c6c;
  font-weight: bold;
}

.operator {
  color: #909399;
  font-size: 12px;
  margin-top: 5px;
}
</style>
