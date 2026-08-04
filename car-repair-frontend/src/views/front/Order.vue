<template>
  <div class="order-page">
    <el-card>
      <template #header>
        <span>我的订单</span>
      </template>
      
      <el-table :data="orders" v-loading="loading">
        <el-table-column prop="orderNo" label="工单编号" align="center" />
        <el-table-column prop="createTime" label="创建时间" align="center">
          <template #default="{ row }">
            {{ row.createTime ? row.createTime.replace('T', ' ') : '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="totalAmount" label="总金额" align="center">
          <template #default="{ row }">¥{{ row.totalAmount }}</template>
        </el-table-column>
        <el-table-column prop="status" label="状态" align="center">
          <template #default="{ row }">
            <el-tag :type="statusType[row.status]">{{ statusMap[row.status] }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="paymentStatus" label="支付状态" align="center">
          <template #default="{ row }">
            <el-tag :type="row.paymentStatus === 1 ? 'success' : 'warning'">
              {{ row.paymentStatus === 1 ? '已支付' : '未支付' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" align="center" width="200">
          <template #default="{ row }">
            <el-button type="primary" link @click="router.push(`/order/${row.id}`)">详情</el-button>
            <el-button type="success" link @click="handlePay(row.id)" v-if="row.status === 4 && row.paymentStatus === 0">支付</el-button>
          </template>
        </el-table-column>
      </el-table>
      
      <el-pagination v-model:current-page="page" :page-size="10" :total="total" 
        layout="total, prev, pager, next" @current-change="loadData" style="margin-top: 20px; justify-content: flex-end" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'

const router = useRouter()
const orders = ref<any[]>([])
const loading = ref(false)
const page = ref(1)
const total = ref(0)

const statusMap: Record<number, string> = { 1: '待接待', 2: '维修中', 3: '待质检', 4: '待结算', 5: '已完成', 6: '已取消' }
const statusType: Record<number, string> = { 1: 'info', 2: 'warning', 3: 'primary', 4: 'danger', 5: 'success', 6: '' }

const loadData = async () => {
  loading.value = true
  try {
    const res = await request.get('/front/order/list', { params: { page: page.value, size: 10 } })
    orders.value = res.data.list
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

const handlePay = async (id: number) => {
  await ElMessageBox.confirm('确定要支付该订单吗？', '提示', { type: 'warning' })
  await request.post(`/front/order/pay/${id}`)
  ElMessage.success('支付成功')
  loadData()
}

onMounted(loadData)
</script>
