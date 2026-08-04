<template>
  <div class="work-order-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <div class="title-wrapper">
            <span class="card-title">我的工单</span>
          </div>
          <div class="filter-wrapper">
            <el-radio-group v-model="filterStatus" @change="loadData" size="default">
              <el-radio-button :label="null">全部</el-radio-button>
              <el-radio-button :label="2">维修中</el-radio-button>
              <el-radio-button :label="3">待质检</el-radio-button>
              <el-radio-button :label="4">待结算</el-radio-button>
              <el-radio-button :label="5">已完成</el-radio-button>
            </el-radio-group>
          </div>
        </div>
      </template>
      
      <el-table :data="orders" v-loading="loading" border>
        <el-table-column prop="orderNo" label="工单编号" align="center" min-width="180" />
        <el-table-column prop="customerName" label="车主" align="center" width="120" />
        <el-table-column label="车辆信息" align="center" min-width="200">
          <template #default="{ row }">
            <div v-if="row.plateNumber || row.vehicleBrand">
              <el-tag v-if="row.plateNumber" size="small" effect="plain" style="margin-right: 5px">
                {{ row.plateNumber }}
              </el-tag>
              <span v-if="row.vehicleBrand || row.vehicleModel">
                {{ row.vehicleBrand || '' }} {{ row.vehicleModel || '' }}
              </span>
            </div>
            <span v-else style="color: #909399">暂无车辆信息</span>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" align="center" width="180">
          <template #default="{ row }">
            {{ row.createTime ? row.createTime.replace('T', ' ') : '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="totalAmount" label="总金额" align="center" width="120">
          <template #default="{ row }">
            <span style="color: #f43f5e; font-weight: 600">¥{{ (row.totalAmount || 0).toFixed(2) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" align="center" width="120">
          <template #default="{ row }">
            <el-tag :type="statusType[row.status]" size="small" effect="light">
              {{ statusMap[row.status] || '未知状态' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" align="center" width="100">
          <template #default="{ row }">
            <el-button type="primary" link @click="router.push(`/technician/order/${row.id}`)">处理</el-button>
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
import request from '@/utils/request'

const router = useRouter()
const orders = ref<any[]>([])
const loading = ref(false)
const page = ref(1)
const total = ref(0)
const filterStatus = ref<number | null>(null)

const statusMap: Record<number, string> = { 1: '待接待', 2: '维修中', 3: '待质检', 4: '待结算', 5: '已完成', 6: '已取消' }
const statusType: Record<number, string> = { 1: 'info', 2: 'warning', 3: 'primary', 4: 'danger', 5: 'success', 6: '' }

const loadData = async () => {
  loading.value = true
  try {
    const res = await request.get('/technician/order/list', { 
      params: { page: page.value, size: 10, status: filterStatus.value } 
    })
    orders.value = res.data.list
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

onMounted(loadData)
</script>

<style scoped>
.work-order-page {
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
