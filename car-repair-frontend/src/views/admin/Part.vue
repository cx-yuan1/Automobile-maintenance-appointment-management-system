<template>
  <div class="admin-part">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>配件库存管理</span>
          <el-button type="primary" @click="showDialog()">添加配件</el-button>
        </div>
      </template>
      
      <!-- 查询条件 -->
      <el-form :inline="true" class="search-form">
        <el-form-item label="配件名称">
          <el-input v-model="searchForm.partName" placeholder="请输入配件名称" clearable @keyup.enter="handleSearch" style="width: 180px" />
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="searchForm.category" placeholder="全部" clearable style="width: 120px">
            <el-option label="油液" value="油液" />
            <el-option label="滤芯" value="滤芯" />
            <el-option label="制动" value="制动" />
            <el-option label="轮胎" value="轮胎" />
            <el-option label="电器" value="电器" />
            <el-option label="其他" value="其他" />
          </el-select>
        </el-form-item>
        <el-form-item label="库存状态">
          <el-select v-model="searchForm.stockStatus" placeholder="全部" clearable style="width: 120px">
            <el-option label="正常" value="normal" />
            <el-option label="库存不足" value="low" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
      
      <el-table :data="parts" v-loading="loading">
        <el-table-column prop="partCode" label="编码" align="center" width="100" />
        <el-table-column prop="partName" label="名称" align="center" />
        <el-table-column prop="category" label="分类" align="center" width="80" />
        <el-table-column prop="salePrice" label="售价" align="center">
          <template #default="{ row }">¥{{ row.salePrice }}</template>
        </el-table-column>
        <el-table-column prop="stockQuantity" label="库存" align="center" />
        <el-table-column prop="minStock" label="最低库存" align="center" />
        <el-table-column label="图片" width="100" align="center">
          <template #default="{ row }">
            <el-image :src="row.imageUrl || defaultImage" style="width: 60px; height: 60px; border-radius: 4px" fit="cover">
              <template #error>
                <div class="image-error"><el-icon><Box /></el-icon></div>
              </template>
            </el-image>
          </template>
        </el-table-column>
        <el-table-column label="状态" align="center">
          <template #default="{ row }">
            <el-tag :type="row.stockQuantity <= row.minStock ? 'danger' : 'success'">
              {{ row.stockQuantity <= row.minStock ? '库存不足' : '正常' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" align="center" width="200">
          <template #default="{ row }">
            <el-button type="success" link @click="showStockDialog(row, 'in')">入库</el-button>
            <el-button type="warning" link @click="showStockDialog(row, 'out')">出库</el-button>
            <el-button type="primary" link @click="showDialog(row)">编辑</el-button>
          </template>
        </el-table-column>
      </el-table>
      
      <el-pagination v-model:current-page="page" :page-size="10" :total="total" 
        layout="total, prev, pager, next" @current-change="loadData" style="margin-top: 20px; justify-content: flex-end" />
    </el-card>

    <!-- 添加/编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑配件' : '添加配件'" width="500px">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="80px">
        <el-form-item label="配件图片">
          <div class="image-uploader">
            <el-image v-if="form.imageUrl" :src="form.imageUrl" style="width: 100px; height: 100px; border-radius: 4px" fit="cover" />
            <div v-else class="upload-placeholder">
              <el-icon><Plus /></el-icon>
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
        <el-form-item label="编码" prop="partCode">
          <el-input v-model="form.partCode" :disabled="isEdit" />
        </el-form-item>
        <el-form-item label="名称" prop="partName">
          <el-input v-model="form.partName" />
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="form.category" style="width: 100%">
            <el-option label="油液" value="油液" />
            <el-option label="滤芯" value="滤芯" />
            <el-option label="制动" value="制动" />
            <el-option label="轮胎" value="轮胎" />
            <el-option label="电器" value="电器" />
            <el-option label="点火" value="点火" />
            <el-option label="空调" value="空调" />
            <el-option label="其他" value="其他" />
          </el-select>
        </el-form-item>
        <el-form-item label="品牌">
          <el-input v-model="form.brand" />
        </el-form-item>
        <el-form-item label="单位" prop="unit">
          <el-input v-model="form.unit" />
        </el-form-item>
        <el-form-item label="采购价" prop="purchasePrice">
          <el-input-number v-model="form.purchasePrice" :min="0" style="width: 100%" />
        </el-form-item>
        <el-form-item label="售价" prop="salePrice">
          <el-input-number v-model="form.salePrice" :min="0" style="width: 100%" />
        </el-form-item>
        <el-form-item label="最低库存">
          <el-input-number v-model="form.minStock" :min="0" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">确定</el-button>
      </template>
    </el-dialog>

    <!-- 入库/出库对话框 -->
    <el-dialog v-model="stockDialogVisible" :title="stockType === 'in' ? '配件入库' : '配件出库'" width="400px">
      <el-form label-width="80px">
        <el-form-item label="配件">{{ currentPart?.partName }}</el-form-item>
        <el-form-item label="当前库存">{{ currentPart?.stockQuantity }}</el-form-item>
        <el-form-item label="数量">
          <el-input-number v-model="stockForm.quantity" :min="1" style="width: 100%" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="stockForm.remark" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="stockDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleStock" :loading="stocking">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, type FormInstance } from 'element-plus'
import request from '@/utils/request'

const parts = ref<any[]>([])
const loading = ref(false)
const page = ref(1)
const total = ref(0)
const dialogVisible = ref(false)
const stockDialogVisible = ref(false)
const isEdit = ref(false)
const submitting = ref(false)
const stocking = ref(false)
const formRef = ref<FormInstance>()
const currentPart = ref<any>(null)
const stockType = ref<'in' | 'out'>('in')

const defaultImage = 'https://via.placeholder.com/60x60/e8f4fc/5a9bd5?text=Part'
const uploadUrl = '/api/common/upload'

// 查询表单
const searchForm = reactive({
  partName: '',
  category: '',
  stockStatus: ''
})

const form = reactive({
  id: undefined as number | undefined,
  partCode: '',
  partName: '',
  category: '',
  brand: '',
  unit: '个',
  purchasePrice: 0,
  salePrice: 0,
  minStock: 10,
  imageUrl: ''
})

const stockForm = reactive({
  quantity: 1,
  remark: ''
})

const rules = {
  partCode: [{ required: true, message: '请输入编码', trigger: 'blur' }],
  partName: [{ required: true, message: '请输入名称', trigger: 'blur' }],
  unit: [{ required: true, message: '请输入单位', trigger: 'blur' }],
  purchasePrice: [{ required: true, message: '请输入采购价', trigger: 'blur' }],
  salePrice: [{ required: true, message: '请输入售价', trigger: 'blur' }]
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
    const res = await request.get('/admin/part/list', { 
      params: { 
        page: page.value, 
        size: 10,
        keyword: searchForm.partName || undefined,
        category: searchForm.category || undefined,
        stockStatus: searchForm.stockStatus || undefined
      } 
    })
    parts.value = res.data.list
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
  searchForm.partName = ''
  searchForm.category = ''
  searchForm.stockStatus = ''
  page.value = 1
  loadData()
}

const showDialog = (row?: any) => {
  isEdit.value = !!row
  if (row) {
    Object.assign(form, row)
  } else {
    Object.assign(form, { id: undefined, partCode: '', partName: '', category: '', brand: '', unit: '个', purchasePrice: 0, salePrice: 0, minStock: 10, imageUrl: '' })
  }
  dialogVisible.value = true
}

const handleSubmit = async () => {
  await formRef.value?.validate()
  submitting.value = true
  try {
    if (isEdit.value) {
      await request.put('/admin/part/update', form)
    } else {
      await request.post('/admin/part/add', form)
    }
    ElMessage.success(isEdit.value ? '更新成功' : '添加成功')
    dialogVisible.value = false
    loadData()
  } finally {
    submitting.value = false
  }
}

const showStockDialog = (row: any, type: 'in' | 'out') => {
  currentPart.value = row
  stockType.value = type
  stockForm.quantity = 1
  stockForm.remark = ''
  stockDialogVisible.value = true
}

const handleStock = async () => {
  stocking.value = true
  try {
    const url = stockType.value === 'in' ? '/admin/inventory/in' : '/admin/inventory/out'
    await request.post(url, {
      partId: currentPart.value.id,
      ...stockForm
    })
    ElMessage.success(stockType.value === 'in' ? '入库成功' : '出库成功')
    stockDialogVisible.value = false
    loadData()
  } finally {
    stocking.value = false
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
  display: flex;
  align-items: flex-start;
  gap: 15px;
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
}

.upload-actions {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
</style>
