<template>
  <div class="admin-employee">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>员工管理</span>
          <el-button type="primary" @click="showDialog()">添加员工</el-button>
        </div>
      </template>
      
      <!-- 查询条件 -->
      <el-form :inline="true" class="search-form">
        <el-form-item label="姓名">
          <el-input v-model="searchForm.realName" placeholder="请输入姓名" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="searchForm.phone" placeholder="请输入手机号" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="searchForm.userType" placeholder="全部" clearable>
            <el-option label="维修人员" :value="2" />
            <el-option label="管理员" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
      
      <el-table :data="employees" v-loading="loading">
        <el-table-column prop="username" label="用户名" align="center" />
        <el-table-column prop="realName" label="姓名" align="center" />
        <el-table-column prop="phone" label="手机号" align="center" />
        <el-table-column prop="userType" label="角色" align="center">
          <template #default="{ row }">{{ userTypeMap[row.userType] }}</template>
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

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑员工' : '添加员工'" width="500px">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="80px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" :disabled="isEdit" />
        </el-form-item>
        <el-form-item label="密码" :prop="isEdit ? '' : 'password'">
          <el-input v-model="form.password" type="password" :placeholder="isEdit ? '留空则不修改' : '请输入密码'" />
        </el-form-item>
        <el-form-item label="姓名" prop="realName">
          <el-input v-model="form.realName" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" />
        </el-form-item>
        <el-form-item label="角色" prop="userType">
          <el-select v-model="form.userType" style="width: 100%">
            <el-option label="维修人员" :value="2" />
            <el-option label="管理员" :value="3" />
          </el-select>
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

const employees = ref<any[]>([])
const loading = ref(false)
const page = ref(1)
const total = ref(0)
const dialogVisible = ref(false)
const isEdit = ref(false)
const submitting = ref(false)
const formRef = ref<FormInstance>()

const userTypeMap: Record<number, string> = { 2: '维修人员', 3: '管理员' }

// 查询表单
const searchForm = reactive({
  realName: '',
  phone: '',
  userType: null as number | null
})

const form = reactive({
  id: undefined as number | undefined,
  username: '',
  password: '',
  realName: '',
  phone: '',
  userType: 2,
  status: 1
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  phone: [{ required: true, message: '请输入手机号', trigger: 'blur' }],
  userType: [{ required: true, message: '请选择角色', trigger: 'change' }]
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await request.get('/admin/employee/list', { 
      params: { 
        page: page.value, 
        size: 10,
        realName: searchForm.realName || undefined,
        phone: searchForm.phone || undefined,
        userType: searchForm.userType
      } 
    })
    employees.value = res.data.list
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
  searchForm.realName = ''
  searchForm.phone = ''
  searchForm.userType = null
  page.value = 1
  loadData()
}

const showDialog = (row?: any) => {
  isEdit.value = !!row
  if (row) {
    Object.assign(form, { ...row, password: '' })
  } else {
    Object.assign(form, { id: undefined, username: '', password: '', realName: '', phone: '', userType: 2, status: 1 })
  }
  dialogVisible.value = true
}

const handleSubmit = async () => {
  await formRef.value?.validate()
  submitting.value = true
  try {
    if (isEdit.value) {
      await request.put('/admin/employee/update', form)
    } else {
      await request.post('/admin/employee/add', form)
    }
    ElMessage.success(isEdit.value ? '更新成功' : '添加成功')
    dialogVisible.value = false
    loadData()
  } finally {
    submitting.value = false
  }
}

const handleDelete = async (id: number) => {
  await ElMessageBox.confirm('确定要删除该员工吗？', '提示', { type: 'warning' })
  try {
    await request.delete(`/admin/employee/delete/${id}`)
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
</style>
