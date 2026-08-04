<template>
  <div class="admin-service">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>服务项目管理</span>
          <el-button type="primary" @click="showDialog()">添加服务</el-button>
        </div>
      </template>
      
      <!-- 查询条件 -->
      <el-form :inline="true" class="search-form">
        <el-form-item label="服务名称">
          <el-input v-model="searchForm.serviceName" placeholder="请输入服务名称" clearable @keyup.enter="handleSearch" style="width: 180px" />
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="searchForm.category" placeholder="全部" clearable style="width: 120px">
            <el-option label="保养" value="保养" />
            <el-option label="维修" value="维修" />
            <el-option label="钣喷" value="钣喷" />
            <el-option label="美容" value="美容" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="全部" clearable style="width: 120px">
            <el-option label="启用" :value="1" />
            <el-option label="禁用" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
      
      <el-table :data="services" v-loading="loading">
        <el-table-column prop="serviceName" label="服务名称" align="center" />
        <el-table-column prop="category" label="分类" align="center" />
        <el-table-column prop="standardHours" label="标准工时(h)" align="center" />
        <el-table-column prop="basePrice" label="基础价格" align="center">
          <template #default="{ row }">¥{{ row.basePrice }}</template>
        </el-table-column>
        <el-table-column prop="difficultyFactor" label="难度系数" align="center" />
        <el-table-column label="图片" width="100" align="center">
          <template #default="{ row }">
            <el-image :src="row.imageUrl || defaultImage" style="width: 60px; height: 60px; border-radius: 4px" fit="cover">
              <template #error>
                <div class="image-error"><el-icon><Tools /></el-icon></div>
              </template>
            </el-image>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '禁用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" align="center" width="150">
          <template #default="{ row }">
            <el-button type="primary" link @click="showDialog(row)">编辑</el-button>
            <el-button type="danger" link @click="handleDelete(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      
      <el-pagination v-model:current-page="page" :page-size="10" :total="total" 
        layout="total, prev, pager, next" @current-change="loadData" style="margin-top: 20px; justify-content: flex-end" />
    </el-card>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑服务' : '添加服务'" width="500px">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
        <el-form-item label="服务图片">
          <el-upload
            class="image-uploader"
            :action="uploadUrl"
            :show-file-list="false"
            :on-success="handleUploadSuccess"
            :on-error="handleUploadError"
            :before-upload="beforeUpload"
            :with-credentials="true"
            accept="image/*"
          >
            <el-image v-if="form.imageUrl" :src="form.imageUrl" class="uploaded-image" fit="cover" />
            <div v-else class="upload-placeholder">
              <el-icon><Plus /></el-icon>
            </div>
          </el-upload>
          <el-button v-if="form.imageUrl" size="small" type="danger" @click.stop="form.imageUrl = ''" style="margin-left: 10px">删除</el-button>
        </el-form-item>
        <el-form-item label="服务名称" prop="serviceName">
          <el-input v-model="form.serviceName" />
        </el-form-item>
        <el-form-item label="分类" prop="category">
          <el-select v-model="form.category" style="width: 100%">
            <el-option label="保养" value="保养" />
            <el-option label="维修" value="维修" />
            <el-option label="钣喷" value="钣喷" />
            <el-option label="美容" value="美容" />
          </el-select>
        </el-form-item>
        <el-form-item label="标准工时" prop="standardHours">
          <el-input-number v-model="form.standardHours" :min="0.5" :step="0.5" style="width: 100%" />
        </el-form-item>
        <el-form-item label="基础价格" prop="basePrice">
          <el-input-number v-model="form.basePrice" :min="0" style="width: 100%" />
        </el-form-item>
        <el-form-item label="难度系数">
          <el-input-number v-model="form.difficultyFactor" :min="0.5" :max="3" :step="0.1" style="width: 100%" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" />
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

const services = ref<any[]>([])
const loading = ref(false)
const page = ref(1)
const total = ref(0)
const dialogVisible = ref(false)
const isEdit = ref(false)
const submitting = ref(false)
const formRef = ref<FormInstance>()

const defaultImage = 'https://via.placeholder.com/60x60/e8f4fc/5a9bd5?text=Service'
const uploadUrl = '/api/common/upload'

// 查询表单
const searchForm = reactive({
  serviceName: '',
  category: '',
  status: null as number | null
})

const form = reactive({
  id: undefined as number | undefined,
  serviceName: '',
  category: '',
  standardHours: 1,
  basePrice: 100,
  difficultyFactor: 1,
  description: '',
  imageUrl: '',
  status: 1
})

const rules = {
  serviceName: [{ required: true, message: '请输入服务名称', trigger: 'blur' }],
  category: [{ required: true, message: '请选择分类', trigger: 'change' }],
  standardHours: [{ required: true, message: '请输入标准工时', trigger: 'blur' }],
  basePrice: [{ required: true, message: '请输入基础价格', trigger: 'blur' }]
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
  console.log('上传响应:', response)
  if (response.code === 200) {
    form.imageUrl = response.data
    ElMessage.success('上传成功')
  } else {
    ElMessage.error(response.message || '上传失败')
  }
}

// 上传失败回调
const handleUploadError = (error: any) => {
  console.error('上传失败:', error)
  ElMessage.error('上传失败，请重试')
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await request.get('/admin/service/list', { 
      params: { 
        page: page.value, 
        size: 10,
        serviceName: searchForm.serviceName || undefined,
        category: searchForm.category || undefined,
        status: searchForm.status
      } 
    })
    services.value = res.data.list
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
  searchForm.serviceName = ''
  searchForm.category = ''
  searchForm.status = null
  page.value = 1
  loadData()
}

const showDialog = (row?: any) => {
  isEdit.value = !!row
  if (row) {
    Object.assign(form, row)
  } else {
    Object.assign(form, { id: undefined, serviceName: '', category: '', standardHours: 1, basePrice: 100, difficultyFactor: 1, description: '', imageUrl: '', status: 1 })
  }
  dialogVisible.value = true
}

const handleSubmit = async () => {
  await formRef.value?.validate()
  submitting.value = true
  try {
    if (isEdit.value) {
      await request.put('/admin/service/update', form)
    } else {
      await request.post('/admin/service/add', form)
    }
    ElMessage.success(isEdit.value ? '更新成功' : '添加成功')
    dialogVisible.value = false
    loadData()
  } finally {
    submitting.value = false
  }
}

const handleDelete = async (id: number) => {
  await ElMessageBox.confirm('确定要删除该服务项目吗？', '提示', { type: 'warning' })
  try {
    await request.delete(`/admin/service/delete/${id}`)
    ElMessage.success('删除成功')
    loadData()
  } catch {
    // 错误已在拦截器处理
  }
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

.image-error {
  width: 60px;
  height: 60px;
  background: #f5f7fa;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #c0c4cc;
  border-radius: 4px;
}

.image-uploader {
  display: inline-block;
  cursor: pointer;
}

.image-uploader:hover .upload-placeholder {
  border-color: #409eff;
  color: #409eff;
}

.uploaded-image {
  width: 100px;
  height: 100px;
  border-radius: 4px;
  display: block;
}

.upload-placeholder {
  width: 100px;
  height: 100px;
  background: #f5f7fa;
  border: 1px dashed #d9d9d9;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #c0c4cc;
  font-size: 24px;
  transition: all 0.3s;
}
</style>
