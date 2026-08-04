<template>
  <div class="booking-page">
    <el-card>
      <template #header>
        <span>预约维修</span>
      </template>
      
      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
        <el-form-item label="选择车辆" prop="vehicleId">
          <el-select v-model="form.vehicleId" placeholder="请选择车辆" style="width: 100%">
            <el-option v-for="v in vehicles" :key="v.id" :label="`${v.plateNumber} - ${v.brand} ${v.model}`" :value="v.id" />
          </el-select>
        </el-form-item>
        
        <el-form-item label="服务项目" prop="serviceIds">
          <div class="service-container">
            <div class="service-filter">
              <el-radio-group v-model="selectedCategory" size="small">
                <el-radio-button label="">全部</el-radio-button>
                <el-radio-button v-for="cat in categories" :key="cat" :label="cat">{{ cat }}</el-radio-button>
              </el-radio-group>
            </div>
            <el-checkbox-group v-model="form.serviceIds" class="service-list">
              <el-checkbox v-for="s in filteredServices" :key="s.id" :label="s.id">
                {{ s.serviceName }} (¥{{ s.basePrice }})
              </el-checkbox>
            </el-checkbox-group>
          </div>
        </el-form-item>
        
        <el-form-item label="预约日期" prop="bookingDate">
          <el-date-picker v-model="form.bookingDate" type="date" placeholder="选择日期" 
            :disabled-date="disabledDate" value-format="YYYY-MM-DD" style="width: 100%" @change="loadTimeSlots" />
        </el-form-item>
        
        <el-form-item label="预约时段" prop="timeSlot" v-if="timeSlots.length">
          <el-radio-group v-model="form.timeSlot">
            <el-radio-button v-for="slot in timeSlots" :key="slot.timeSlot" :label="slot.timeSlot" :disabled="!slot.available">
              {{ slot.timeSlot }} ({{ slot.available ? `剩余${slot.maxCount - slot.bookedCount}` : '已满' }})
            </el-radio-button>
          </el-radio-group>
        </el-form-item>
        
        <el-form-item label="选择维修师" prop="technicianId">
          <el-select v-model="form.technicianId" placeholder="请选择维修师" style="width: 100%">
            <el-option v-for="tech in technicians" :key="tech.id" :label="tech.realName || tech.username" :value="tech.id">
              <span>{{ tech.realName || tech.username }}</span>
              <span style="float: right; color: #8492a6; font-size: 13px">
                评分: {{ tech.avgScore || '暂无' }} | 完成: {{ tech.completedOrders || 0 }}单
              </span>
            </el-option>
          </el-select>
        </el-form-item>
        
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" placeholder="请输入备注信息" />
        </el-form-item>
        
        <el-form-item>
          <el-button type="primary" @click="getQuotation" :loading="quoting">获取报价</el-button>
        </el-form-item>
      </el-form>
      
      <!-- 报价结果 -->
      <el-card v-if="quotation" class="quotation-card">
        <template #header>
          <span>智能报价</span>
        </template>
        <el-descriptions :column="2" border>
          <el-descriptions-item label="工时费">¥{{ quotation.laborCost }}</el-descriptions-item>
          <el-descriptions-item label="车型系数">{{ quotation.vehicleFactor }}</el-descriptions-item>
          <el-descriptions-item label="季节系数">{{ quotation.seasonFactor }}</el-descriptions-item>
          <el-descriptions-item label="会员折扣">{{ quotation.vipDiscount }}</el-descriptions-item>
          <el-descriptions-item label="预估总价">
            <span class="price">¥{{ quotation.finalPrice }}</span>
          </el-descriptions-item>
        </el-descriptions>
        <div class="submit-btn">
          <el-button type="primary" size="large" @click="handleSubmit" :loading="submitting">确认预约</el-button>
        </div>
      </el-card>
    </el-card>

    <!-- 我的预约列表 -->
    <el-card style="margin-top: 20px">
      <template #header>
        <span>我的预约</span>
      </template>
      <el-table :data="bookings" v-loading="loadingBookings">
        <el-table-column prop="bookingNo" label="预约编号" align="center" />
        <el-table-column prop="bookingDate" label="预约日期" align="center" />
        <el-table-column prop="timeSlot" label="时段" align="center" />
        <el-table-column prop="estimatedPrice" label="预估价格" align="center">
          <template #default="{ row }">¥{{ row.estimatedPrice || '-' }}</template>
        </el-table-column>
        <el-table-column prop="technicianName" label="维修人员" align="center">
          <template #default="{ row }">{{ row.technicianName }}</template>
        </el-table-column>
        <el-table-column prop="status" label="状态" align="center">
          <template #default="{ row }">
            <el-tag :type="statusType[row.status]">{{ statusMap[row.status] }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" align="center">
          <template #default="{ row }">
            <el-button type="danger" link @click="cancelBooking(row.id)" v-if="row.status <= 2">取消</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox, type FormInstance } from 'element-plus'
import request from '@/utils/request'

const route = useRoute()
const formRef = ref<FormInstance>()
const vehicles = ref<any[]>([])
const services = ref<any[]>([])
const technicians = ref<any[]>([])
const timeSlots = ref<any[]>([])
const bookings = ref<any[]>([])
const quotation = ref<any>(null)
const quoting = ref(false)
const submitting = ref(false)
const loadingBookings = ref(false)
const selectedCategory = ref('')

const statusMap: Record<number, string> = { 1: '待确认', 2: '已确认', 3: '已到店', 4: '已取消', 5: '已完成' }
const statusType: Record<number, string> = { 1: 'warning', 2: 'success', 3: 'primary', 4: 'info', 5: '' }

// 根据分类筛选服务
const filteredServices = computed(() => {
  if (!selectedCategory.value) return services.value
  return services.value.filter(s => s.category === selectedCategory.value)
})

// 服务分类列表
const categories = computed(() => {
  const cats = [...new Set(services.value.map(s => s.category))]
  return cats
})

const form = reactive({
  vehicleId: null as number | null,
  serviceIds: [] as number[],
  bookingDate: '',
  timeSlot: '',
  technicianId: null as number | null,
  remark: ''
})

const rules = {
  vehicleId: [{ required: true, message: '请选择车辆', trigger: 'change' }],
  serviceIds: [{ required: true, message: '请选择服务项目', trigger: 'change', type: 'array', min: 1 }],
  bookingDate: [{ required: true, message: '请选择预约日期', trigger: 'change' }],
  timeSlot: [{ required: true, message: '请选择预约时段', trigger: 'change' }],
  technicianId: [{ required: true, message: '请选择维修师', trigger: 'change' }]
}

const disabledDate = (date: Date) => {
  return date.getTime() < Date.now() - 86400000
}

const loadTimeSlots = async () => {
  if (!form.bookingDate) return
  const res = await request.get('/front/booking/timeSlots', { params: { date: form.bookingDate } })
  timeSlots.value = res.data
}

const getQuotation = async () => {
  await formRef.value?.validate()
  quoting.value = true
  try {
    const res = await request.post('/front/booking/quotation', {
      vehicleId: form.vehicleId,
      serviceIds: form.serviceIds,
      bookingDate: form.bookingDate
    })
    quotation.value = res.data
  } finally {
    quoting.value = false
  }
}

const handleSubmit = async () => {
  submitting.value = true
  try {
    await request.post('/front/booking/create', {
      ...form,
      estimatedPrice: quotation.value?.finalPrice
    })
    ElMessage.success('预约成功')
    quotation.value = null
    loadBookings()
  } finally {
    submitting.value = false
  }
}

const loadBookings = async () => {
  loadingBookings.value = true
  try {
    const res = await request.get('/front/booking/list', { params: { page: 1, size: 10 } })
    bookings.value = res.data.list
  } finally {
    loadingBookings.value = false
  }
}

const cancelBooking = async (id: number) => {
  await ElMessageBox.confirm('确定要取消该预约吗？', '提示', { type: 'warning' })
  await request.put(`/front/booking/cancel/${id}`)
  ElMessage.success('取消成功')
  loadBookings()
}

onMounted(async () => {
  const [vRes, sRes, tRes] = await Promise.all([
    request.get('/front/vehicle/list'),
    request.get('/front/service/list'),
    request.get('/front/booking/technicians')
  ])
  vehicles.value = vRes.data
  services.value = sRes.data
  technicians.value = tRes.data
  
  // 从URL参数获取分类
  if (route.query.category) {
    selectedCategory.value = route.query.category as string
  }
  
  loadBookings()
})
</script>

<style scoped>
.quotation-card {
  margin-top: 20px;
}

.price {
  font-size: 24px;
  color: #f56c6c;
  font-weight: bold;
}

.submit-btn {
  margin-top: 20px;
  text-align: center;
}

.service-container {
  width: 100%;
}

.service-filter {
  margin-bottom: 15px;
}

.service-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.service-list .el-checkbox {
  margin-right: 20px;
  margin-bottom: 10px;
}
</style>
