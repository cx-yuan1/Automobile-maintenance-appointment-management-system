<template>
  <div class="tech-dashboard">
    <!-- 统计卡片 -->
    <el-row :gutter="24">
      <el-col :span="6">
        <el-card shadow="never" class="stat-card blue">
          <div class="stat-content">
            <div class="stat-icon-wrapper">
              <el-icon><Document /></el-icon>
            </div>
            <div class="stat-details">
              <div class="stat-value">{{ stats.todayOrders || 0 }}</div>
              <div class="stat-label">今日工单</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="never" class="stat-card purple">
          <div class="stat-content">
            <div class="stat-icon-wrapper">
              <el-icon><Tools /></el-icon>
            </div>
            <div class="stat-details">
              <div class="stat-value">{{ stats.inProgressOrders || 0 }}</div>
              <div class="stat-label">正在维修</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="never" class="stat-card green">
          <div class="stat-content">
            <div class="stat-icon-wrapper">
              <el-icon><CircleCheck /></el-icon>
            </div>
            <div class="stat-details">
              <div class="stat-value">{{ stats.completedOrders || 0 }}</div>
              <div class="stat-label">本月完成</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="never" class="stat-card pink">
          <div class="stat-content">
            <div class="stat-icon-wrapper">
              <el-icon><Money /></el-icon>
            </div>
            <div class="stat-details">
              <div class="stat-value">¥{{ stats.monthIncome || 0 }}</div>
              <div class="stat-label">本月收入</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 待处理工单 -->
    <el-card style="margin-top: 24px">
      <template #header>
        <div class="card-header">
          <div class="title-wrapper">
            <span class="card-title">最近收到的待处理工单</span>
          </div>
          <el-button type="primary" link @click="router.push('/technician/orders')">
            查看全部 <el-icon class="el-icon--right"><ArrowRight /></el-icon>
          </el-button>
        </div>
      </template>
      <el-table :data="pendingOrders" v-loading="loading" border>
        <el-table-column prop="orderNo" label="工单编号" align="center" min-width="180" />
        <el-table-column prop="createTime" label="创建时间" align="center" width="180">
          <template #default="{ row }">
            {{ row.createTime ? row.createTime.replace('T', ' ') : '-' }}
          </template>
        </el-table-column>
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
        <el-table-column prop="status" label="状态" align="center" width="120">
          <template #default="{ row }">
            <el-tag :type="statusType[row.status]" size="small" effect="light">{{ statusMap[row.status] }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" align="center" width="100">
          <template #default="{ row }">
            <el-button type="primary" link @click="router.push(`/technician/order/${row.id}`)">处理</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loading && !pendingOrders.length" description="暂无待处理工单" />
    </el-card>

    <!-- 快捷操作与统计 -->
    <el-row :gutter="24" style="margin-top: 24px">
      <el-col :span="12">
        <el-card style="height: 100%">
          <template #header>
            <div class="card-header">
              <div class="title-wrapper"><span class="card-title">快捷操作</span></div>
            </div>
          </template>
          <div class="quick-actions">
            <el-button class="action-btn blue" @click="router.push('/technician/orders')">
              <el-icon><Document /></el-icon><span>进入我的工单</span>
            </el-button>
            <el-button class="action-btn green" @click="router.push('/technician/parts')">
              <el-icon><Box /></el-icon><span>查询配件库存</span>
            </el-button>
            <el-button class="action-btn gray" @click="router.push('/technician/history')">
              <el-icon><Clock /></el-icon><span>查看历史记录</span>
            </el-button>
          </div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card style="height: 100%">
          <template #header>
            <div class="card-header">
              <div class="title-wrapper"><span class="card-title">本月工作统计</span></div>
            </div>
          </template>
          <el-descriptions :column="2" border direction="vertical">
            <el-descriptions-item label="完成工单">
              <span class="stat-item-val">{{ stats.completedOrders || 0 }}</span> 单
            </el-descriptions-item>
            <el-descriptions-item label="平均工时">
              <span class="stat-item-val">{{ stats.avgHours || 0 }}</span> 小时
            </el-descriptions-item>
            <el-descriptions-item label="客户评分">
              <span class="stat-item-val">{{ stats.avgScore || 0 }}</span> 分
            </el-descriptions-item>
            <el-descriptions-item label="本月收入">
              <span class="stat-item-val money">¥{{ stats.monthIncome || 0 }}</span>
            </el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import request from '@/utils/request'
import { Document, Tools, CircleCheck, Money, Clock, Box, ArrowRight } from '@element-plus/icons-vue'

const router = useRouter()
const loading = ref(false)
const pendingOrders = ref<any[]>([])
const stats = reactive({
  todayOrders: 0,
  inProgressOrders: 0,
  completedOrders: 0,
  monthIncome: 0,
  avgHours: 0,
  avgScore: 0
})

const statusMap: Record<number, string> = { 1: '待接待', 2: '维修中', 3: '待质检', 4: '待结算' }
const statusType: Record<number, string> = { 1: 'info', 2: 'warning', 3: 'primary', 4: 'danger' }

const loadStats = async () => {
  try {
    const res = await request.get('/technician/dashboard/stats')
    Object.assign(stats, res.data)
  } catch { /* 忽略错误 */ }
}

const loadPendingOrders = async () => {
  loading.value = true
  try {
    const res = await request.get('/technician/order/list', { params: { page: 1, size: 5, status: 2 } })
    pendingOrders.value = res.data.list || []
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadStats()
  loadPendingOrders()
})
</script>

<style scoped>
.tech-dashboard { padding: 0; }

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

/* 统计卡片样式 */
.stat-card {
  height: 110px;
  border-radius: 16px;
  transition: transform 0.3s;
}

.stat-card:hover {
  transform: translateY(-4px);
}

.stat-content {
  display: flex;
  align-items: center;
  gap: 16px;
}

.stat-icon-wrapper {
  width: 56px;
  height: 56px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 26px;
}

.stat-details {
  display: flex;
  flex-direction: column;
}

.stat-value {
  font-size: 24px;
  font-weight: 700;
  color: #303133;
  line-height: 1.2;
}

.stat-label {
  font-size: 13px;
  color: #909399;
  margin-top: 4px;
}

/* 颜色变体 */
.stat-card.blue { background: linear-gradient(135deg, #e0f2fe 0%, #bae6fd 100%); }
.stat-card.blue .stat-icon-wrapper { background: #5a9bd5; color: #fff; }

.stat-card.purple { background: linear-gradient(135deg, #f5f3ff 0%, #ddd6fe 100%); }
.stat-card.purple .stat-icon-wrapper { background: #8b5cf6; color: #fff; }

.stat-card.green { background: linear-gradient(135deg, #f0fdf4 0%, #dcfce7 100%); }
.stat-card.green .stat-icon-wrapper { background: #10b981; color: #fff; }

.stat-card.pink { background: linear-gradient(135deg, #fff1f2 0%, #ffe4e6 100%); }
.stat-card.pink .stat-icon-wrapper { background: #f43f5e; color: #fff; }

/* 快捷操作样式 */
.quick-actions {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.action-btn {
  height: 48px;
  margin: 0 !important;
  font-size: 14px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  transition: all 0.3s;
}

.action-btn.blue { background: #eff6ff; border: 1px solid #dbeafe; color: #3b82f6; }
.action-btn.green { background: #f0fdf4; border: 1px solid #dcfce7; color: #10b981; }
.action-btn.gray { background: #f8fafc; border: 1px solid #f1f5f9; color: #64748b; }

.action-btn:hover {
  filter: brightness(0.95);
  transform: translateX(4px);
}

.stat-item-val {
  font-size: 20px;
  font-weight: 700;
  color: #3b82f6;
}

.stat-item-val.money {
  color: #f43f5e;
}
</style>
