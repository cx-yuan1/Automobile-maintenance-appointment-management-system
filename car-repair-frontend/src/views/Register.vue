<template>
  <div class="register-container">
    <!-- 左侧介绍区域 -->
    <div class="register-left">
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
    
    <!-- 右侧注册区域 -->
    <div class="register-right">
      <div class="register-box">
        <h2>用户注册</h2>
        <p class="welcome-text">创建账号，开始您的便捷维修之旅</p>
        <el-form :model="form" :rules="rules" ref="formRef" label-width="0">
          <el-form-item prop="username">
            <el-input v-model="form.username" placeholder="请输入用户名" prefix-icon="User" size="large" />
          </el-form-item>
          <el-form-item prop="password">
            <el-input v-model="form.password" type="password" placeholder="请输入密码" prefix-icon="Lock" size="large" show-password />
          </el-form-item>
          <el-form-item prop="realName">
            <el-input v-model="form.realName" placeholder="请输入真实姓名" prefix-icon="UserFilled" size="large" />
          </el-form-item>
          <el-form-item prop="phone">
            <el-input v-model="form.phone" placeholder="请输入手机号" prefix-icon="Phone" size="large" />
          </el-form-item>
          <el-form-item prop="userType">
            <el-select v-model="form.userType" placeholder="选择注册身份" style="width: 100%" size="large">
              <el-option label="车主用户" :value="1" />
              <el-option label="维修人员" :value="2" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handleRegister" :loading="loading" size="large" style="width: 100%">注 册</el-button>
          </el-form-item>
          <el-form-item>
            <div class="register-footer">
              <span>已有账号？</span>
              <router-link to="/login" class="login-link">立即登录</router-link>
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
import request from '@/utils/request'

const router = useRouter()
const formRef = ref<FormInstance>()
const loading = ref(false)

const form = reactive({
  username: '',
  password: '',
  realName: '',
  phone: '',
  userType: 1
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  realName: [{ required: true, message: '请输入真实姓名', trigger: 'blur' }],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }
  ],
  userType: [{ required: true, message: '请选择注册身份', trigger: 'change' }]
}

const handleRegister = async () => {
  await formRef.value?.validate()
  loading.value = true
  
  try {
    await request.post('/common/register', form)
    ElMessage.success('注册成功，请登录')
    router.push('/login')
  } catch {
    // 错误已在拦截器处理
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.register-container {
  min-height: 100vh;
  display: flex;
}

/* 左侧区域 */
.register-left {
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

.register-left h1 {
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
.register-right {
  width: 480px;
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px;
}

.register-box {
  width: 100%;
  max-width: 360px;
}

.register-box h2 {
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

.register-footer {
  width: 100%;
  text-align: center;
  color: #909399;
}

.login-link {
  color: #5a9bd5;
  text-decoration: none;
  margin-left: 5px;
}

.login-link:hover {
  text-decoration: underline;
}

/* 响应式 */
@media (max-width: 900px) {
  .register-left {
    display: none;
  }
  .register-right {
    width: 100%;
  }
}
</style>
