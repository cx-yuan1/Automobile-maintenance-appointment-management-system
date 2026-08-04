<template>
  <div class="admin-order">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>工单管理</span>
        </div>
      </template>
      
      <!-- 查询条件 -->
      <el-form :inline="true" class="search-form">
        <el-form-item label="工单编号">
          <el-input v-model="searchForm.orderNo" placeholder="请输入工单编号" clearable @keyup.enter="handleSearch" style="width: 180px" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="全部" clearable style="width: 120px">
            <el-option label="待接待" :value="1" />
            <el-option label="维修中" :value="2" />
            <el-option label="待质检" :value="3" />
            <el-option label="待结算" :value="4" />
            <el-option label="已完成" :value="5" />
            <el-option label="已取消" :value="6" />
          </el-select>
        </el-form-item>
        <el-form-item label="维修人员">
          <el-select v-model="searchForm.technicianId" placeholder="全部" clearable style="width: 120px">
            <el-option v-for="t in technicians" :key="t.id" :label="t.realName" :value="t.id" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
      
      <el-table :data="orders" v-loading="loading" border>
        <el-table-column prop="orderNo" label="工单编号" align="center" />
        <el-table-column prop="createTime" label="创建时间" align="center">
          <template #default="{ row }">
            {{ row.createTime ? row.createTime.replace('T', ' ') : '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="customerName" label="车主" align="center" />
        <el-table-column prop="technicianName" label="维修师" align="center">
          <template #default="{ row }">
            {{ row.technicianName || '未分配' }}
          </template>
        </el-table-column>
        <el-table-column prop="totalAmount" label="总金额" align="center">
          <template #default="{ row }">¥{{ row.totalAmount || 0 }}</template>
        </el-table-column>
        <el-table-column prop="status" label="状态" align="center">
          <template #default="{ row }">
            <el-tag :type="statusType[row.status]">{{ statusMap[row.status] }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" align="center" width="180">
          <template #default="{ row }">
            <el-button type="primary" link @click="viewDetail(row.id)">详情</el-button>
            <el-button v-if="row.status === 3" type="success" link @click="handleQualityCheck(row.id)">质检通过</el-button>
          </template>
        </el-table-column>
      </el-table>
      
      <el-pagination v-model:current-page="page" :page-size="10" :total="total" 
        layout="total, prev, pager, next" @current-change="loadData" style="margin-top: 20px; justify-content: flex-end" />
    </el-card>

    <!-- 详情对话框 -->
    <el-dialog v-model="detailDialogVisible" title="工单详情" width="800px">
      <el-descriptions :column="2" border v-if="currentDetail">
        <el-descriptions-item label="工单编号">{{ currentDetail.order?.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="statusType[currentDetail.order?.status]">{{ statusMap[currentDetail.order?.status] }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="车主">{{ currentDetail.customer?.realName }}</el-descriptions-item>
        <el-descriptions-item label="联系电话">{{ currentDetail.customer?.phone }}</el-descriptions-item>
        <el-descriptions-item label="车辆">{{ currentDetail.vehicle?.plateNumber }} {{ currentDetail.vehicle?.brand }}</el-descriptions-item>
        <el-descriptions-item label="维修师">{{ currentDetail.technician?.realName || '未分配' }}</el-descriptions-item>
        <el-descriptions-item label="工时费">¥{{ currentDetail.order?.laborCost || 0 }}</el-descriptions-item>
        <el-descriptions-item label="配件费">¥{{ currentDetail.order?.partsCost || 0 }}</el-descriptions-item>
        <el-descriptions-item label="总金额" :span="2">
          <span style="font-size: 18px; color: #f56c6c; font-weight: bold">¥{{ currentDetail.order?.totalAmount || 0 }}</span>
        </el-descriptions-item>
      </el-descriptions>
      
      <!-- 服务项目 -->
      <el-divider content-position="left">服务项目</el-divider>
      <el-table :data="currentDetail?.services || []" border size="small">
        <el-table-column prop="serviceName" label="服务名称" align="center" />
        <el-table-column prop="standardHours" label="标准工时" align="center" />
        <el-table-column prop="unitPrice" label="单价" align="center">
          <template #default="{ row }">¥{{ row.unitPrice }}</template>
        </el-table-column>
        <el-table-column prop="totalPrice" label="总价" align="center">
          <template #default="{ row }">¥{{ row.totalPrice }}</template>
        </el-table-column>
      </el-table>
      
      <!-- 配件明细 -->
      <el-divider content-position="left">配件明细</el-divider>
      <el-table :data="currentDetail?.parts || []" border size="small" v-if="currentDetail?.parts?.length">
        <el-table-column prop="partName" label="配件名称" align="center" />
        <el-table-column prop="quantity" label="数量" align="center" />
        <el-table-column prop="unitPrice" label="单价" align="center">
          <template #default="{ row }">¥{{ row.unitPrice }}</template>
        </el-table-column>
        <el-table-column prop="totalPrice" label="总价" align="center">
          <template #default="{ row }">¥{{ row.totalPrice }}</template>
        </el-table-column>
      </el-table>
      <el-empty v-else description="暂无配件" :image-size="80" />
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'

const orders = ref<any[]>([])
const technicians = ref<any[]>([])
const loading = ref(false)
const page = ref(1)
const total = ref(0)
const detailDialogVisible = ref(false)
const currentDetail = ref<any>(null)

const statusMap: Record<number, string> = { 1: '待接待', 2: '维修中', 3: '待质检', 4: '待结算', 5: '已完成', 6: '已取消' }
const statusType: Record<number, string> = { 1: 'info', 2: 'warning', 3: 'primary', 4: 'danger', 5: 'success', 6: '' }

// 查询表单
const searchForm = reactive({
  orderNo: '',
  status: null as number | null,
  technicianId: null as number | null
})

const loadData = async () => {
  loading.value = true
  try {
    const res = await request.get('/admin/order/list', { 
      params: { 
        page: page.value, 
        size: 10,
        orderNo: searchForm.orderNo || undefined,
        status: searchForm.status,
        technicianId: searchForm.technicianId
      } 
    })
    orders.value = res.data.list
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

// 查询
const handleSearch = () => {
  page.value = 1
  loadData()
}

// 重置
const handleReset = () => {
  searchForm.orderNo = ''
  searchForm.status = null
  searchForm.technicianId = null
  page.value = 1
  loadData()
}

const loadTechnicians = async () => {
  try {
    const res = await request.get('/admin/employee/technicians')
    technicians.value = res.data
  } catch {
    // 忽略错误
  }
}

const viewDetail = async (id: number) => {
  try {
    const res = await request.get(`/admin/order/detail/${id}`)
    currentDetail.value = res.data
    detailDialogVisible.value = true
  } catch (error) {
    ElMessage.error('获取工单详情失败')
  }
}

// 质检通过
const handleQualityCheck = async (id: number) => {
  try {
    await request.post(`/admin/order/qualityCheckPass/${id}`)
    ElMessage.success('质检通过，工单已进入待结算状态')
    loadData()
  } catch (error: any) {
    ElMessage.error(error.response?.data?.message || '质检失败')
  }
}

onMounted(() => {
  loadData()
  loadTechnicians()
})
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.search-form {
  margin-bottom: 15px;
  padding-bottom: 15px;
  border-bottom: 1px solid #ebeef5;
}
</style>
