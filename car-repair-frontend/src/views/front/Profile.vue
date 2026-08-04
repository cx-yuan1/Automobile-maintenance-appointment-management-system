<template>
  <div class="profile-page">
    <el-card>
      <template #header><span>个人信息</span></template>
      <el-form :model="userForm" label-width="100px" style="max-width: 500px">
        <el-form-item label="用户名">
          <el-input v-model="userForm.username" disabled />
        </el-form-item>
        <el-form-item label="真实姓名">
          <el-input v-model="userForm.realName" placeholder="请输入真实姓名" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="userForm.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="客户等级">
          <el-tag :type="levelType[userForm.customerLevel]">{{ levelMap[userForm.customerLevel] }}</el-tag>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="updateProfile" :loading="saving">保存修改</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card style="margin-top: 20px">
      <template #header><span>修改密码</span></template>
      <el-form :model="pwdForm" label-width="100px" style="max-width: 500px">
        <el-form-item label="原密码">
          <el-input v-model="pwdForm.oldPassword" type="password" show-password placeholder="请输入原密码" />
        </el-form-item>
        <el-form-item label="新密码">
          <el-input v-model="pwdForm.newPassword" type="password" show-password placeholder="请输入新密码" />
        </el-form-item>
        <el-form-item label="确认密码">
          <el-input v-model="pwdForm.confirmPassword" type="password" show-password placeholder="请再次输入新密码" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="changePassword" :loading="changingPwd">修改密码</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card style="margin-top: 20px">
      <template #header><span>我的统计</span></template>
      <el-row :gutter="20">
        <el-col :span="6">
          <div class="stat-item">
            <div class="stat-value">{{ myStats.vehicleCount || 0 }}</div>
            <div class="stat-label">我的车辆</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat-item">
            <div class="stat-value">{{ myStats.orderCount || 0 }}</div>
            <div class="stat-label">维修次数</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat-item">
            <div class="stat-value">¥{{ myStats.totalSpent || 0 }}</div>
            <div class="stat-label">累计消费</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat-item">
            <div class="stat-value">{{ myStats.points || 0 }}</div>
            <div class="stat-label">积分</div>
          </div>
        </el-col>
      </el-row>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const saving = ref(false)
const changingPwd = ref(false)

const levelMap: Record<number, string> = { 1: '普通会员', 2: '银卡会员', 3: '金卡会员', 4: 'VIP会员' }
const levelType: Record<number, string> = { 1: 'info', 2: '', 3: 'warning', 4: 'danger' }

const userForm = reactive({
  username: '',
  realName: '',
  phone: '',
  customerLevel: 1
})

const pwdForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const myStats = reactive({
  vehicleCount: 0,
  orderCount: 0,
  totalSpent: 0,
  points: 0
})

const loadUserInfo = async () => {
  try {
    const res = await request.get('/common/currentUser')
    if (res.data) {
      userForm.username = res.data.username
      userForm.realName = res.data.realName
      userForm.phone = res.data.phone
      userForm.customerLevel = res.data.customerLevel || 1
    }
  } catch { /* 忽略错误 */ }
}

const loadMyStats = async () => {
  try {
    const res = await request.get('/front/profile/stats')
    Object.assign(myStats, res.data)
  } catch { /* 忽略错误 */ }
}

const updateProfile = async () => {
  if (!userForm.realName) {
    ElMessage.warning('请输入真实姓名')
    return
  }
  saving.value = true
  try {
    await request.put('/front/profile/update', {
      realName: userForm.realName,
      phone: userForm.phone
    })
    ElMessage.success('信息更新成功')
    userStore.getCurrentUser()
  } finally {
    saving.value = false
  }
}

const changePassword = async () => {
  if (!pwdForm.oldPassword || !pwdForm.newPassword) {
    ElMessage.warning('请填写完整密码信息')
    return
  }
  if (pwdForm.newPassword !== pwdForm.confirmPassword) {
    ElMessage.warning('两次输入的密码不一致')
    return
  }
  if (pwdForm.newPassword.length < 6) {
    ElMessage.warning('新密码长度不能少于6位')
    return
  }
  changingPwd.value = true
  try {
    await request.put('/front/profile/password', {
      oldPassword: pwdForm.oldPassword,
      newPassword: pwdForm.newPassword
    })
    ElMessage.success('密码修改成功')
    pwdForm.oldPassword = ''
    pwdForm.newPassword = ''
    pwdForm.confirmPassword = ''
  } finally {
    changingPwd.value = false
  }
}

onMounted(() => {
  loadUserInfo()
  loadMyStats()
})
</script>

<style scoped>
.profile-page { max-width: 800px; margin: 0 auto; }
.stat-item { text-align: center; padding: 20px; background: #f5f9fc; border-radius: 8px; }
.stat-value { font-size: 28px; font-weight: bold; color: #5a9bd5; }
.stat-label { font-size: 14px; color: #909399; margin-top: 8px; }
</style>
