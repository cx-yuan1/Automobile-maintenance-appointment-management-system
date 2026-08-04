<template>
  <div class="history-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <div class="title-wrapper">
            <span class="card-title">历史工单</span>
          </div>
          <div class="filter-wrapper">
            <el-date-picker v-model="dateRange" type="daterange" range-separator="至" 
              start-placeholder="开始日期" end-placeholder="结束日期" @change="loadData" size="default" />
          </div>
        </div>
      </template>
      
      <el-table :data="orders" v-loading="loading" border>
        <el-table-column prop="orderNo" label="工单编号" align="center" width="150" />
        <el-table-column prop="customerName" label="车主" align="center" width="120" />
        <el-table-column prop="createTime" label="创建时间" align="center" width="180">
          <template #default="{ row }">
            {{ row.createTime ? row.createTime.replace('T', ' ') : '-' }}
          </template>
        </el-table-column>
        <el-table-column label="车辆信息" align="center" min-width="200">
          <template #default="{ row }">
            <div v-if="row.plateNumber">
              <el-tag size="small" effect="plain" style="margin-right: 5px">{{ row.plateNumber }}</el-tag>
              <span>{{ row.vehicleBrand }} {{ row.vehicleModel }}</span>
            </div>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="actualHours" label="实际工时" align="center" width="100">
          <template #default="{ row }">{{ row.actualHours || '-' }} 小时</template>
        </el-table-column>
        <el-table-column prop="totalAmount" label="总金额" align="center" width="100">
          <template #default="{ row }">¥{{ row.totalAmount }}</template>
        </el-table-column>
        <el-table-column prop="status" label="状态" align="center" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 5 ? 'success' : 'info'" size="small">
              {{ row.status === 5 ? '已完成' : '已取消' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" align="center" width="120">
          <template #default="{ row }">
            <el-button type="primary" link @click="viewDetail(row)">查看详情</el-button>
          </template>
        </el-table-column>
      </el-table>
      
      <el-pagination v-model:current-page="page" :page-size="10" :total="total" 
        layout="total, prev, pager, next" @current-change="loadData" 
        style="margin-top: 20px; justify-content: flex-end" />
    </el-card>

    <!-- 详情对话框 -->
    <el-dialog v-model="detailVisible" title="工单详情" width="800px">
      <el-descriptions :column="2" border v-if="currentOrder">
        <el-descriptions-item label="工单编号">{{ currentOrder.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="currentOrder.status === 5 ? 'success' : 'info'">
            {{ currentOrder.status === 5 ? '已完成' : '已取消' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ currentOrder.createTime }}</el-descriptions-item>
        <el-descriptions-item label="完成时间">{{ currentOrder.endTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="实际工时">{{ currentOrder.actualHours || '-' }} 小时</el-descriptions-item>
        <el-descriptions-item label="工时费">¥{{ currentOrder.laborCost }}</el-descriptions-item>
        <el-descriptions-item label="配件费">¥{{ currentOrder.partsCost }}</el-descriptions-item>
        <el-descriptions-item label="总金额">¥{{ currentOrder.totalAmount }}</el-descriptions-item>
        <el-descriptions-item label="故障描述" :span="2">{{ currentOrder.faultDesc || '无' }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import request from '@/utils/request'

const orders = ref<any[]>([])
const loading = ref(false)
const page = ref(1)
const total = ref(0)
const dateRange = ref<[Date, Date]>()
const detailVisible = ref(false)
const currentOrder = ref<any>(null)

const loadData = async () => {
  loading.value = true
  try {
    const params: any = { page: page.value, size: 10, status: 5 }
    if (dateRange.value) {
      params.startDate = dateRange.value[0].toISOString().split('T')[0]
      params.endDate = dateRange.value[1].toISOString().split('T')[0]
    }
    const res = await request.get('/technician/order/list', { params })
    orders.value = res.data.list || []
    total.value = res.data.total || 0
  } finally {
    loading.value = false
  }
}

const viewDetail = async (row: any) => {
  try {
    const res = await request.get(`/technician/order/detail/${row.id}`)
    currentOrder.value = res.data.order
    detailVisible.value = true
  } catch { /* 忽略错误 */ }
}

onMounted(loadData)
</script>

<style scoped>
.history-page {
  padding: 0;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  position: relative;
  padding-left: 12px;
}

.card-title::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 4px;
  height: 16px;
  background: #5a9bd5;
  border-radius: 2px;
}
</style>
