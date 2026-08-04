<template>
  <div class="vehicle-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>我的车辆</span>
          <el-button type="primary" @click="showDialog()">添加车辆</el-button>
        </div>
      </template>
      
      <!-- 卡片式展示 -->
      <el-row :gutter="20" v-loading="loading">
        <el-col :span="8" v-for="item in vehicles" :key="item.id" style="margin-bottom: 20px">
          <el-card shadow="hover" class="vehicle-card">
            <div class="vehicle-image">
              <el-image :src="item.imageUrl || defaultCarImage" fit="cover" style="width: 100%; height: 150px">
                <template #error>
                  <div class="image-placeholder">
                    <el-icon :size="40"><Van /></el-icon>
                  </div>
                </template>
              </el-image>
            </div>
            <div class="vehicle-info">
              <h3>{{ item.plateNumber }}</h3>
              <p>{{ item.brand }} {{ item.model }}</p>
              <p class="vehicle-meta">
                <el-tag size="small" :type="vehicleTypeColor[item.vehicleType]">{{ vehicleTypeMap[item.vehicleType] }}</el-tag>
                <span>{{ item.mileage?.toLocaleString() }} km</span>
              </p>
            </div>
            <div class="vehicle-actions">
              <el-button type="primary" link @click="showDialog(item)">编辑</el-button>
              <el-button type="danger" link @click="handleDelete(item.id)">删除</el-button>
            </div>
          </el-card>
        </el-col>
      </el-row>
      <el-empty v-if="!loading && vehicles.length === 0" description="暂无车辆，点击上方按钮添加" />
    </el-card>

    <!-- 添加/编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑车辆' : '添加车辆'" width="500px">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="80px">
        <el-form-item label="车辆图片">
          <div class="image-uploader">
            <el-image v-if="form.imageUrl" :src="form.imageUrl" style="width: 120px; height: 80px; border-radius: 4px" fit="cover" />
            <div v-else class="upload-placeholder-car">
              <el-icon><Van /></el-icon>
            </div>
            <div class="upload-actions">
              <el-upload
                :action="uploadUrl"
                :show-file-list="false"
                :on-success="handleUploadSuccess"
                :before-upload="beforeUpload"
                accept="image/*"
              >
                <el-button size="small" type="primary">上传图片</el-button>
              </el-upload>
              <el-button v-if="form.imageUrl" size="small" type="danger" @click="form.imageUrl = ''">删除</el-button>
            </div>
          </div>
        </el-form-item>
        <el-form-item label="车牌号" prop="plateNumber">
          <el-input v-model="form.plateNumber" placeholder="请输入车牌号" />
        </el-form-item>
        <el-form-item label="品牌" prop="brand">
          <el-input v-model="form.brand" placeholder="请输入品牌" />
        </el-form-item>
        <el-form-item label="车型" prop="model">
          <el-input v-model="form.model" placeholder="请输入车型" />
        </el-form-item>
        <el-form-item label="车辆类型" prop="vehicleType">
          <el-select v-model="form.vehicleType" placeholder="请选择" style="width: 100%">
            <el-option label="普通车" :value="1" />
            <el-option label="豪华车" :value="2" />
            <el-option label="新能源" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="VIN码">
          <el-input v-model="form.vin" placeholder="请输入VIN码" />
        </el-form-item>
        <el-form-item label="颜色">
          <el-input v-model="form.color" placeholder="请输入颜色" />
        </el-form-item>
        <el-form-item label="里程(km)">
          <el-input-number v-model="form.mileage" :min="0" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance } from 'element-plus'
import request from '@/utils/request'

interface Vehicle {
  id?: number
  plateNumber: string
  brand: string
  model: string
  vehicleType: number
  vin: string
  color: string
  mileage: number
  imageUrl?: string
}

const vehicles = ref<Vehicle[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const submitting = ref(false)
const formRef = ref<FormInstance>()

// 默认车辆图片
const defaultCarImage = 'https://via.placeholder.com/300x150/e8f4fc/5a9bd5?text=Car'
const uploadUrl = '/api/common/upload'

const vehicleTypeMap: Record<number, string> = {
  1: '普通车',
  2: '豪华车',
  3: '新能源'
}

const vehicleTypeColor: Record<number, string> = {
  1: 'info',
  2: 'warning',
  3: 'success'
}

const form = reactive<Vehicle>({
  plateNumber: '',
  brand: '',
  model: '',
  vehicleType: 1,
  vin: '',
  color: '',
  mileage: 0,
  imageUrl: ''
})

const rules = {
  plateNumber: [{ required: true, message: '请输入车牌号', trigger: 'blur' }],
  brand: [{ required: true, message: '请输入品牌', trigger: 'blur' }],
  vehicleType: [{ required: true, message: '请选择车辆类型', trigger: 'change' }]
}

// 上传前校验
const beforeUpload = (file: File) => {
  const isImage = file.type.startsWith('image/')
  const isLt2M = file.size / 1024 / 1024 < 2
  if (!isImage) {
    ElMessage.error('只能上传图片文件')
    return false
  }
  if (!isLt2M) {
    ElMessage.error('图片大小不能超过 2MB')
    return false
  }
  return true
}

// 上传成功回调
const handleUploadSuccess = (response: any) => {
  if (response.code === 200) {
    form.imageUrl = response.data
    ElMessage.success('上传成功')
  } else {
    ElMessage.error(response.message || '上传失败')
  }
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await request.get('/front/vehicle/list')
    vehicles.value = res.data
  } finally {
    loading.value = false
  }
}

const showDialog = (row?: Vehicle) => {
  isEdit.value = !!row
  if (row) {
    Object.assign(form, row)
  } else {
    Object.assign(form, {
      id: undefined,
      plateNumber: '',
      brand: '',
      model: '',
      vehicleType: 1,
      vin: '',
      color: '',
      mileage: 0,
      imageUrl: ''
    })
  }
  dialogVisible.value = true
}

const handleSubmit = async () => {
  await formRef.value?.validate()
  submitting.value = true
  try {
    if (isEdit.value) {
      await request.put('/front/vehicle/update', form)
    } else {
      await request.post('/front/vehicle/add', form)
    }
    ElMessage.success(isEdit.value ? '更新成功' : '添加成功')
    dialogVisible.value = false
    loadData()
  } finally {
    submitting.value = false
  }
}

const handleDelete = async (id: number) => {
  await ElMessageBox.confirm('确定要删除该车辆吗？', '提示', { type: 'warning' })
  await request.delete(`/front/vehicle/delete/${id}`)
  ElMessage.success('删除成功')
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

.vehicle-card {
  cursor: pointer;
  transition: transform 0.3s;
}

.vehicle-card:hover {
  transform: translateY(-5px);
}

.vehicle-image {
  margin: -20px -20px 15px -20px;
  border-radius: 4px 4px 0 0;
  overflow: hidden;
}

.image-placeholder {
  width: 100%;
  height: 150px;
  background: #f5f7fa;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #c0c4cc;
}

.vehicle-info h3 {
  margin: 0 0 8px 0;
  font-size: 18px;
  color: #303133;
}

.vehicle-info p {
  margin: 0 0 8px 0;
  color: #606266;
  font-size: 14px;
}

.vehicle-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  color: #909399;
  font-size: 13px;
}

.vehicle-actions {
  margin-top: 15px;
  padding-top: 15px;
  border-top: 1px solid #ebeef5;
  text-align: right;
}

.image-uploader {
  display: flex;
  align-items: flex-start;
  gap: 15px;
}

.upload-placeholder-car {
  width: 120px;
  height: 80px;
  background: #f5f7fa;
  border: 1px dashed #d9d9d9;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #c0c4cc;
  font-size: 24px;
}

.upload-actions {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
</style>
