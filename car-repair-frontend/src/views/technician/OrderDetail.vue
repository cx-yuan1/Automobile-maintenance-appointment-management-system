<template>
  <div class="tech-order-detail" v-loading="loading">
    <el-page-header @back="router.back()" content="工单处理" />
    
    <!-- 状态提示 -->
    <el-alert v-if="detail && detail.order.status === 1" 
      title="工单尚未分配" 
      type="warning" 
      description="此工单还在待接待状态，需要管理员分配维修师后才能开始处理" 
      :closable="false" 
      style="margin-top: 20px" />
    
    <el-alert v-if="detail && detail.order.status === 2 && (!detail.services || detail.services.length === 0)" 
      title="暂无服务项目" 
      type="info" 
      description="此工单还没有添加服务项目，请联系管理员添加" 
      :closable="false" 
      style="margin-top: 20px" />
    
    <el-row :gutter="20" style="margin-top: 20px" v-if="detail">
      <el-col :span="16">
        <el-card>
          <template #header><span>工单信息</span></template>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="工单编号">{{ detail.order.orderNo }}</el-descriptions-item>
            <el-descriptions-item label="状态">
              <el-tag :type="statusType[detail.order.status]">{{ statusMap[detail.order.status] }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="客户">{{ detail.customer?.realName }}</el-descriptions-item>
            <el-descriptions-item label="联系电话">{{ detail.customer?.phone }}</el-descriptions-item>
            <el-descriptions-item label="车辆">{{ detail.vehicle?.plateNumber }} {{ detail.vehicle?.brand }}</el-descriptions-item>
            <el-descriptions-item label="里程">{{ detail.vehicle?.mileage }} km</el-descriptions-item>
            <el-descriptions-item label="故障描述" :span="2">{{ detail.order.faultDesc || '无' }}</el-descriptions-item>
          </el-descriptions>
        </el-card>

        <!-- 服务项目 -->
        <el-card style="margin-top: 20px">
          <template #header><span>服务项目</span></template>
          <el-table :data="detail.services || []" border>
            <el-table-column prop="serviceName" label="服务名称" align="center" />
            <el-table-column prop="standardHours" label="标准工时" align="center" />
            <el-table-column prop="unitPrice" label="单价" align="center">
              <template #default="{ row }">¥{{ row.unitPrice }}</template>
            </el-table-column>
            <el-table-column prop="status" label="状态" align="center">
              <template #default="{ row }">
                <el-tag :type="row.status === 3 ? 'success' : row.status === 2 ? 'warning' : 'info'" size="small">
                  {{ row.status === 3 ? '已完成' : row.status === 2 ? '进行中' : '待处理' }}
                </el-tag>
              </template>
            </el-table-column>
          </el-table>
        </el-card>

        <!-- 配件申领 -->
        <el-card style="margin-top: 20px">
          <template #header>
            <div style="display: flex; justify-content: space-between; align-items: center">
              <span>配件申领</span>
              <el-button type="primary" size="small" @click="showApplyDialog" 
                v-if="detail.order.status === 2" 
                :disabled="!detail.services || detail.services.length === 0">
                申领配件
              </el-button>
              <el-tag v-else-if="detail.order.status === 1" type="info" size="small">工单未开始</el-tag>
              <el-tag v-else-if="detail.order.status >= 3" type="success" size="small">维修已完成</el-tag>
            </div>
          </template>
          <el-table :data="detail.parts || []" border v-if="detail.parts?.length">
            <el-table-column prop="partName" label="配件名称" align="center" />
            <el-table-column prop="quantity" label="数量" align="center" />
            <el-table-column prop="unitPrice" label="单价" align="center">
              <template #default="{ row }">¥{{ row.unitPrice }}</template>
            </el-table-column>
            <el-table-column prop="totalPrice" label="总价" align="center">
              <template #default="{ row }">¥{{ row.totalPrice }}</template>
            </el-table-column>
          </el-table>
          <el-empty v-else description="暂无配件申领记录" />
        </el-card>

        <!-- 进度更新 -->
        <el-card style="margin-top: 20px" v-if="detail.order.status === 2">
          <template #header><span>更新进度</span></template>
          <el-form :model="progressForm" label-width="80px">
            <el-form-item label="进度状态">
              <el-input v-model="progressForm.progressStatus" placeholder="如：更换机油" 
                :disabled="!detail.services || detail.services.length === 0" />
            </el-form-item>
            <el-form-item label="进度描述">
              <el-input v-model="progressForm.progressDesc" type="textarea" placeholder="详细描述当前进度" 
                :disabled="!detail.services || detail.services.length === 0" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="updateProgress" :loading="updating" 
                :disabled="!detail.services || detail.services.length === 0">
                提交进度
              </el-button>
              <el-button type="success" @click="completeOrder" 
                :disabled="!detail.services || detail.services.length === 0">
                确认完工
              </el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>
      
      <el-col :span="8">
        <el-card>
          <template #header><span>维修进度</span></template>
          <el-timeline v-if="detail.progress?.length">
            <el-timeline-item v-for="p in detail.progress" :key="p.id" :timestamp="p.createTime" placement="top">
              <h4>{{ p.progressStatus }}</h4>
              <p>{{ p.progressDesc }}</p>
            </el-timeline-item>
          </el-timeline>
          <el-empty v-else description="暂无进度记录" />
        </el-card>
      </el-col>
    </el-row>

    <!-- 配件申领对话框 -->
    <el-dialog v-model="applyDialogVisible" title="配件申领" width="600px">
      <el-form label-width="80px">
        <el-form-item label="选择配件">
          <el-select v-model="applyForm.partId" placeholder="请选择配件" filterable style="width: 100%">
            <el-option v-for="p in availableParts" :key="p.id" :label="`${p.partName} (库存: ${p.stockQuantity - p.reservedQuantity})`" 
              :value="p.id" :disabled="p.stockQuantity - p.reservedQuantity <= 0" />
          </el-select>
        </el-form-item>
        <el-form-item label="申领数量">
          <el-input-number v-model="applyForm.quantity" :min="1" :max="maxQuantity" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="applyDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitApply" :loading="applying">确认申领</el-button>
      </template>
    </el-dialog>

    <!-- 完工对话框 -->
    <el-dialog v-model="completeDialogVisible" title="确认完工" width="400px">
      <el-form :model="completeForm" label-width="80px">
        <el-form-item label="实际工时">
          <el-input-number v-model="completeForm.actualHours" :min="0.5" :step="0.5" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="completeForm.remark" type="textarea" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="completeDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitComplete" :loading="completing">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const detail = ref<any>(null)
const updating = ref(false)
const completing = ref(false)
const applying = ref(false)
const completeDialogVisible = ref(false)
const applyDialogVisible = ref(false)
const availableParts = ref<any[]>([])

const statusMap: Record<number, string> = { 1: '待接待', 2: '维修中', 3: '待质检', 4: '待结算', 5: '已完成', 6: '已取消' }
const statusType: Record<number, string> = { 1: 'info', 2: 'warning', 3: 'primary', 4: 'danger', 5: 'success', 6: '' }

const progressForm = reactive({ progressStatus: '', progressDesc: '' })
const completeForm = reactive({ actualHours: 1, remark: '' })
const applyForm = reactive({ partId: null as number | null, quantity: 1 })

// 计算最大可申领数量
const maxQuantity = computed(() => {
  if (!applyForm.partId) return 1
  const part = availableParts.value.find(p => p.id === applyForm.partId)
  return part ? part.stockQuantity - part.reservedQuantity : 1
})

const loadData = async () => {
  loading.value = true
  try {
    const res = await request.get(`/technician/order/detail/${route.params.id}`)
    detail.value = res.data
  } finally {
    loading.value = false
  }
}

const loadParts = async () => {
  try {
    const res = await request.get('/technician/part/available')
    availableParts.value = res.data || []
  } catch { /* 忽略错误 */ }
}

const showApplyDialog = () => {
  applyForm.partId = null
  applyForm.quantity = 1
  loadParts()
  applyDialogVisible.value = true
}

const submitApply = async () => {
  if (!applyForm.partId) {
    ElMessage.warning('请选择配件')
    return
  }
  applying.value = true
  try {
    await request.post('/technician/part/apply', {
      orderId: route.params.id,
      partId: applyForm.partId,
      quantity: applyForm.quantity
    })
    ElMessage.success('配件申领成功')
    applyDialogVisible.value = false
    loadData()
  } finally {
    applying.value = false
  }
}

const updateProgress = async () => {
  if (!progressForm.progressStatus) {
    ElMessage.warning('请输入进度状态')
    return
  }
  updating.value = true
  try {
    await request.post('/technician/progress/update', { orderId: route.params.id, ...progressForm })
    ElMessage.success('进度更新成功')
    progressForm.progressStatus = ''
    progressForm.progressDesc = ''
    loadData()
  } finally {
    updating.value = false
  }
}

const completeOrder = () => { completeDialogVisible.value = true }

const submitComplete = async () => {
  completing.value = true
  try {
    await request.post('/technician/order/complete', { orderId: route.params.id, ...completeForm })
    ElMessage.success('完工确认成功')
    completeDialogVisible.value = false
    router.back()
  } finally {
    completing.value = false
  }
}

onMounted(loadData)
</script>

<style scoped>
.tech-order-detail { padding: 20px; }
</style>
