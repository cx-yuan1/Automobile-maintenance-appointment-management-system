<template>
  <div class="login-container">
    <!-- 左侧介绍区域 -->
    <div class="login-left">
      <div class="left-content">
        <div class="logo-area">
          <el-icon :size="60" color="#fff"><Van /></el-icon>
        </div>
        <h1>汽车维修预约管理系统</h1>
        <p class="slogan">专业维修 · 便捷预约 · 贴心服务</p>
        <div class="features">
          <div class="feature-item">
            <el-icon :size="24"><Calendar /></el-icon>
            <span>在线预约，省时省心</span>
          </div>
          <div class="feature-item">
            <el-icon :size="24"><Tools /></el-icon>
            <span>专业技师，品质保障</span>
          </div>
          <div class="feature-item">
            <el-icon :size="24"><Document /></el-icon>
            <span>进度透明，实时跟踪</span>
          </div>
          <div class="feature-item">
            <el-icon :size="24"><PriceTag /></el-icon>
            <span>智能报价，价格透明</span>
          </div>
        </div>
        <!-- 装饰图案 -->
        <div class="decoration">
          <div class="car-icon">🚗</div>
          <div class="wrench-icon">🔧</div>
          <div class="gear-icon">⚙️</div>
        </div>
      </div>
    </div>
    
    <!-- 右侧登录区域 -->
    <div class="login-right">
      <div class="login-box">
        <h2>用户登录</h2>
        <p class="welcome-text">欢迎回来，请登录您的账号</p>
        <el-form :model="form" :rules="rules" ref="formRef" label-width="0">
          <el-form-item prop="username">
            <el-input v-model="form.username" placeholder="请输入用户名" prefix-icon="User" size="large" />
          </el-form-item>
          <el-form-item prop="password">
            <el-input v-model="form.password" type="password" placeholder="请输入密码" prefix-icon="Lock" size="large" show-password />
          </el-form-item>
          <el-form-item prop="userType">
            <el-select v-model="form.userType" placeholder="选择登录身份" style="width: 100%" size="large">
              <el-option label="车主用户" :value="1" />
              <el-option label="维修人员" :value="2" />
              <el-option label="管理员" :value="3" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handleLogin" :loading="loading" size="large" style="width: 100%">登 录</el-button>
          </el-form-item>
          <el-form-item>
            <div class="login-footer">
              <span>还没有账号？</span>
              <router-link to="/register" class="register-link">立即注册</router-link>
            </div>
          </el-form-item>
        </el-form>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance } from 'element-plus'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()
const formRef = ref<FormInstance>()
const loading = ref(false)

const form = reactive({
  username: '',
  password: '',
  userType: 1
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  userType: [{ required: true, message: '请选择登录身份', trigger: 'change' }]
}

const handleLogin = async () => {
  await formRef.value?.validate()
  loading.value = true
  
  try {
    const formData = new FormData()
    formData.append('username', form.username)
    formData.append('password', form.password)
    formData.append('userType', form.userType.toString())
    
    const response = await fetch('/api/common/login', {
      method: 'POST',
      body: formData,
      credentials: 'include'
    })
    
    const res = await response.json()
    
    if (res.code === 200) {
      userStore.setUserInfo(res.data)
      ElMessage.success('登录成功')
      
      if (res.data.userType === 1) router.push('/')
      else if (res.data.userType === 2) router.push('/technician')
      else router.push('/admin')
    } else {
      ElMessage.error(res.message || '登录失败')
    }
  } catch {
    ElMessage.error('登录失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-container {
  min-height: 100vh;
  display: flex;
}

/* 左侧区域 */
.login-left {
  flex: 1;
  background: linear-gradient(135deg, #5a9bd5 0%, #7ab8d9 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
}

.left-content {
  text-align: center;
  color: #fff;
  padding: 40px;
  z-index: 1;
}

.logo-area {
  margin-bottom: 20px;
}

.login-left h1 {
  font-size: 32px;
  margin-bottom: 15px;
  font-weight: 600;
}

.slogan {
  font-size: 16px;
  opacity: 0.9;
  margin-bottom: 40px;
}

.features {
  text-align: left;
  max-width: 280px;
  margin: 0 auto;
}

.feature-item {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;
  font-size: 15px;
}

/* 装饰图案 */
.decoration {
  position: absolute;
  width: 100%;
  height: 100%;
  top: 0;
  left: 0;
  pointer-events: none;
}

.car-icon {
  position: absolute;
  font-size: 80px;
  opacity: 0.1;
  bottom: 10%;
  left: 10%;
}

.wrench-icon {
  position: absolute;
  font-size: 60px;
  opacity: 0.1;
  top: 15%;
  right: 15%;
}

.gear-icon {
  position: absolute;
  font-size: 100px;
  opacity: 0.08;
  bottom: 20%;
  right: 10%;
}

/* 右侧区域 */
.login-right {
  width: 480px;
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px;
}

.login-box {
  width: 100%;
  max-width: 360px;
}

.login-box h2 {
  font-size: 28px;
  color: #303133;
  margin-bottom: 10px;
  text-align: center;
}

.welcome-text {
  color: #909399;
  text-align: center;
  margin-bottom: 30px;
  font-size: 14px;
}

.login-footer {
  width: 100%;
  text-align: center;
  color: #909399;
}

.register-link {
  color: #5a9bd5;
  text-decoration: none;
  margin-left: 5px;
}

.register-link:hover {
  text-decoration: underline;
}

/* 响应式 */
@media (max-width: 900px) {
  .login-left {
    display: none;
  }
  .login-right {
    width: 100%;
  }
}
</style>
