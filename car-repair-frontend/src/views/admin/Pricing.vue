<template>
  <div class="admin-pricing">
    <el-card>
      <template #header>
        <span>定价配置</span>
      </template>
      
      <el-tabs v-model="activeTab" @tab-change="loadData">
        <el-tab-pane label="车型系数" name="VEHICLE_TYPE" />
        <el-tab-pane label="季节系数" name="SEASON" />
        <el-tab-pane label="库存系数" name="INVENTORY" />
        <el-tab-pane label="VIP折扣" name="VIP_LEVEL" />
      </el-tabs>
      
      <el-table :data="configs" v-loading="loading">
        <el-table-column prop="configName" label="名称" align="center" />
        <el-table-column prop="configKey" label="键值" align="center" />
        <el-table-column prop="factorValue" label="系数值" align="center" />
        <el-table-column prop="description" label="描述" align="center" />
        <el-table-column prop="status" label="状态" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '禁用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" align="center" width="100">
          <template #default="{ row }">
            <el-button type="primary" link @click="showDialog(row)">编辑</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" title="编辑配置" width="400px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="名称">
          <el-input v-model="form.configName" disabled />
        </el-form-item>
        <el-form-item label="系数值">
          <el-input-number v-model="form.factorValue" :min="0" :max="5" :step="0.05" :precision="2" style="width: 100%" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" />
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
import { ElMessage } from 'element-plus'
import request from '@/utils/request'

const configs = ref<any[]>([])
const loading = ref(false)
const activeTab = ref('VEHICLE_TYPE')
const dialogVisible = ref(false)
const submitting = ref(false)

const form = reactive({
  id: 0,
  configName: '',
  factorValue: 1,
  description: '',
  status: 1
})

const loadData = async () => {
  loading.value = true
  try {
    const res = await request.get('/admin/pricing/list', { params: { configType: activeTab.value } })
    configs.value = res.data
  } finally {
    loading.value = false
  }
}

const showDialog = (row: any) => {
  Object.assign(form, row)
  dialogVisible.value = true
}

const handleSubmit = async () => {
  submitting.value = true
  try {
    await request.put('/admin/pricing/update', form)
    ElMessage.success('更新成功')
    dialogVisible.value = false
    loadData()
  } finally {
    submitting.value = false
  }
}

onMounted(loadData)
</script>
