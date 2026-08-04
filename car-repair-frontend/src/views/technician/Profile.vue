<template>
  <div class="tech-profile-page">
    <el-row :gutter="24">
      <el-col :span="16">
        <el-card>
          <template #header>
            <div class="card-header">
              <div class="title-wrapper"><span class="card-title">个人信息</span></div>
            </div>
          </template>
          <el-form :model="form" label-width="100px" style="padding: 10px 0">
            <el-form-item label="用户名">
              <el-input v-model="form.username" disabled />
            </el-form-item>
            <el-form-item label="真实姓名">
              <el-input v-model="form.realName" />
            </el-form-item>
            <el-form-item label="手机号">
              <el-input v-model="form.phone" />
            </el-form-item>
            <el-form-item style="margin-bottom: 0">
              <el-button type="primary" @click="updateInfo" :loading="updating">保存修改</el-button>
            </el-form-item>
          </el-form>
        </el-card>

        <el-card style="margin-top: 24px">
          <template #header>
            <div class="card-header">
              <div class="title-wrapper"><span class="card-title">修改密码</span></div>
            </div>
          </template>
          <el-form :model="pwdForm" label-width="100px" style="padding: 10px 0">
            <el-form-item label="原密码">
              <el-input v-model="pwdForm.oldPassword" type="password" show-password />
            </el-form-item>
            <el-form-item label="新密码">
              <el-input v-model="pwdForm.newPassword" type="password" show-password />
            </el-form-item>
            <el-form-item label="确认密码">
              <el-input v-model="pwdForm.confirmPassword" type="password" show-password />
            </el-form-item>
            <el-form-item style="margin-bottom: 0">
              <el-button type="primary" @click="updatePassword" :loading="changingPwd">修改密码</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>

      <el-col :span="8">
        <el-card>
          <template #header>
            <div class="card-header">
              <div class="title-wrapper"><span class="card-title">工作统计</span></div>
            </div>
          </template>
          <el-descriptions :column="1" border>
            <el-descriptions-item label="累计完成">{{ stats.totalOrders || 0 }} 单</el-descriptions-item>
            <el-descriptions-item label="平均评分">{{ stats.avgScore || 0 }} 分</el-descriptions-item>
            <el-descriptions-item label="累计收入">¥{{ stats.totalIncome || 0 }}</el-descriptions-item>
            <el-descriptions-item label="本月完成">{{ stats.monthOrders || 0 }} 单</el-descriptions-item>
            <el-descriptions-item label="本月收入">¥{{ stats.monthIncome || 0 }}</el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import request from '@/utils/request'

const userStore = useUserStore()
const updating = ref(false)
const changingPwd = ref(false)

const form = reactive({
  username: '',
  realName: '',
  phone: ''
})

const pwdForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const stats = reactive({
  totalOrders: 0,
  avgScore: 0,
  totalIncome: 0,
  monthOrders: 0,
  monthIncome: 0
})

const loadUserInfo = async () => {
  try {
    const res = await request.get('/technician/profile/info')
    Object.assign(form, res.data)
  } catch { /* 忽略错误 */ }
}

const loadStats = async () => {
  try {
    const res = await request.get('/technician/profile/stats')
    Object.assign(stats, res.data)
  } catch { /* 忽略错误 */ }
}

const updateInfo = async () => {
  updating.value = true
  try {
    await request.put('/technician/profile/update', form)
    ElMessage.success('信息更新成功')
    await userStore.getCurrentUser()
  } finally {
    updating.value = false
  }
}

const updatePassword = async () => {
  if (!pwdForm.oldPassword || !pwdForm.newPassword) {
    ElMessage.warning('请填写完整信息')
    return
  }
  if (pwdForm.newPassword !== pwdForm.confirmPassword) {
    ElMessage.warning('两次密码输入不一致')
    return
  }
  changingPwd.value = true
  try {
    await request.put('/technician/profile/password', {
      oldPassword: pwdForm.oldPassword,
      newPassword: pwdForm.newPassword
    })
    ElMessage.success('密码修改成功，请重新登录')
    pwdForm.oldPassword = ''
    pwdForm.newPassword = ''
    pwdForm.confirmPassword = ''
  } finally {
    changingPwd.value = false
  }
}

onMounted(() => {
  loadUserInfo()
  loadStats()
})
</script>

<style scoped>
.tech-profile-page {
  padding: 0;
}

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
</style>
