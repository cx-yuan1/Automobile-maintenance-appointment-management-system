<template>
  <div class="admin-booking">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>预约管理</span>
        </div>
      </template>
      
      <!-- 查询条件 -->
      <el-form :inline="true" class="search-form">
        <el-form-item label="预约编号">
          <el-input v-model="searchForm.bookingNo" placeholder="请输入预约编号" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="预约日期">
          <el-date-picker v-model="searchForm.bookingDate" type="date" placeholder="选择日期" value-format="YYYY-MM-DD" clearable />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="全部" clearable>
            <el-option label="待确认" :value="1" />
            <el-option label="已确认" :value="2" />
            <el-option label="已到店" :value="3" />
            <el-option label="已取消" :value="4" />
            <el-option label="已完成" :value="5" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
      
      <el-table :data="bookings" v-loading="loading">
        <el-table-column prop="bookingNo" label="预约编号" align="center" />
        <el-table-column prop="bookingDate" label="预约日期" align="center" />
        <el-table-column prop="customerName" label="车主" align="center" />
        <el-table-column prop="technicianName" label="维修人员" align="center">
          <template #default="{ row }">{{ row.technicianName }}</template>
        </el-table-column>
        <el-table-column prop="estimatedPrice" label="预估价格" align="center">
          <template #default="{ row }">¥{{ row.estimatedPrice || '-' }}</template>
        </el-table-column>
        <el-table-column prop="status" label="状态" align="center">
          <template #default="{ row }">
            <el-tag :type="statusType[row.status]">{{ statusMap[row.status] }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" align="center" width="200">
          <template #default="{ row }">
            <el-button type="success" link @click="confirmBooking(row.id)" v-if="row.status === 1">确认</el-button>
            <el-button type="danger" link @click="rejectBooking(row.id)" v-if="row.status === 1">拒绝</el-button>
          </template>
        </el-table-column>
      </el-table>
      
      <el-pagination v-model:current-page="page" :page-size="10" :total="total" 
        layout="total, prev, pager, next" @current-change="loadData" style="margin-top: 20px; justify-content: flex-end" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'

const bookings = ref<any[]>([])
const loading = ref(false)
const page = ref(1)
const total = ref(0)

// 查询表单
const searchForm = reactive({
  bookingNo: '',
  bookingDate: '',
  status: null as number | null
})

const statusMap: Record<number, string> = { 1: '待确认', 2: '已确认', 3: '已到店', 4: '已取消', 5: '已完成' }
const statusType: Record<number, string> = { 1: 'warning', 2: 'success', 3: 'primary', 4: 'info', 5: '' }

const loadData = async () => {
  loading.value = true
  try {
    const res = await request.get('/admin/booking/list', { 
      params: { 
        page: page.value, 
        size: 10, 
        bookingNo: searchForm.bookingNo || undefined,
        bookingDate: searchForm.bookingDate || undefined,
        status: searchForm.status 
      } 
    })
    bookings.value = res.data.list
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
  searchForm.bookingNo = ''
  searchForm.bookingDate = ''
  searchForm.status = null
  page.value = 1
  loadData()
}

const confirmBooking = async (id: number) => {
  await ElMessageBox.confirm('确定要确认该预约吗？', '提示')
  await request.put(`/admin/booking/confirm/${id}`)
  ElMessage.success('预约已确认')
  loadData()
}

const rejectBooking = async (id: number) => {
  await ElMessageBox.confirm('确定要拒绝该预约吗？', '提示', { type: 'warning' })
  await request.put(`/admin/booking/reject/${id}`)
  ElMessage.success('预约已拒绝')
  loadData()
}

onMounted(loadData)
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
