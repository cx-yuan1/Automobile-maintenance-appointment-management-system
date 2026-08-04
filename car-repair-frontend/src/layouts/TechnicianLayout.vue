<template>
  <div class="tech-layout">
    <el-container class="main-container">
      <el-aside width="240px" class="sidebar">
        <div class="sidebar-content">
          <div class="logo">
            <el-icon :size="24"><Tools /></el-icon>
            <span>维修工作台</span>
          </div>
          
          <el-menu :default-active="activeMenu" router class="menu">
            <el-menu-item index="/technician">
              <el-icon><Odometer /></el-icon>
              <span>工作台</span>
            </el-menu-item>
            <el-menu-item index="/technician/orders">
              <el-icon><Document /></el-icon>
              <span>我的工单</span>
            </el-menu-item>
            <el-menu-item index="/technician/parts">
              <el-icon><Box /></el-icon>
              <span>配件库存</span>
            </el-menu-item>
            <el-menu-item index="/technician/history">
              <el-icon><Clock /></el-icon>
              <span>历史工单</span>
            </el-menu-item>
            <el-menu-item index="/technician/profile">
              <el-icon><User /></el-icon>
              <span>个人中心</span>
            </el-menu-item>
          </el-menu>

        </div>
      </el-aside>

      <el-container class="content-wrapper">
        <el-header class="header">
          <div class="header-breadcrumb">
            <el-breadcrumb separator="/">
              <el-breadcrumb-item>管理后台</el-breadcrumb-item>
              <el-breadcrumb-item>{{ getPageTitle() }}</el-breadcrumb-item>
            </el-breadcrumb>
          </div>
          <div class="header-right">
            <el-dropdown @command="handleCommand">
              <span class="el-dropdown-link">
                <el-avatar :size="32" class="header-avatar">
                  {{ userStore.userInfo?.realName?.charAt(0) || '技' }}
                </el-avatar>
                <span class="header-username">{{ userStore.userInfo?.realName || '技师' }}</span>
                <el-icon class="el-icon--right"><arrow-down /></el-icon>
              </span>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="profile">个人中心</el-dropdown-item>
                  <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </el-header>
        <el-main class="main">
          <router-view />
        </el-main>
      </el-container>
    </el-container>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { Tools, Odometer, Document, Box, Clock, User, SwitchButton, ArrowDown } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const activeMenu = computed(() => {
  const path = route.path
  if (path.startsWith('/technician/order/')) return '/technician/orders'
  return path
})

const getPageTitle = () => {
  const titles: Record<string, string> = {
    '/technician': '工作台',
    '/technician/orders': '我的工单',
    '/technician/parts': '配件库存',
    '/technician/history': '历史工单',
    '/technician/profile': '个人中心'
  }
  return titles[route.path] || '工单详情'
}

const handleCommand = async (command: string) => {
  if (command === 'logout') {
    await userStore.logout()
    router.push('/login')
  } else if (command === 'profile') {
    router.push('/technician/profile')
  }
}
</script>

<style scoped>
.tech-layout {
  height: 100vh;
  display: flex;
}

.main-container {
  height: 100%;
}

.sidebar {
  background: #fff;
  border-right: 1px solid #f0f0f0;
  display: flex;
  flex-direction: column;
}

.sidebar-content {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.logo {
  height: 64px;
  display: flex;
  align-items: center;
  padding: 0 24px;
  font-size: 18px;
  font-weight: 600;
  color: #5a9bd5;
  gap: 10px;
}

.menu {
  flex: 1;
  border-right: none;
  padding: 10px 0;
}

.menu :deep(.el-menu-item) {
  height: 50px;
  line-height: 50px;
  margin: 4px 12px;
  border-radius: 8px;
  color: #606266;
}

.menu :deep(.el-menu-item:hover) {
  background-color: #f5f9fc;
  color: #5a9bd5;
}

.menu :deep(.el-menu-item.is-active) {
  background-color: #f0f7fc;
  color: #5a9bd5;
  font-weight: 600;
}

.menu :deep(.el-menu-item.is-active::before) {
  content: '';
  position: absolute;
  left: 0;
  top: 15%;
  height: 70%;
  width: 3px;
  background-color: #5a9bd5;
  border-radius: 0 4px 4px 0;
}



.content-wrapper {
  background-color: #f5f8fa;
}

.header {
  height: 64px;
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  border-bottom: 1px solid #f0f0f0;
}

.header-right {
  display: flex;
  align-items: center;
}

.el-dropdown-link {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  color: #606266;
  padding: 4px 8px;
  border-radius: 8px;
  transition: all 0.3s;
}

.el-dropdown-link:hover {
  background-color: #f5f7fa;
}

.header-avatar {
  background-color: #5a9bd5;
  color: #fff;
  font-weight: 600;
}

.header-username {
  font-size: 14px;
  font-weight: 500;
}

.main {
  padding: 24px;
}

:deep(.el-card) {
  border-radius: 12px;
  border: none;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.03);
}

:deep(.el-table) {
  border-radius: 8px;
}

:deep(.el-table th) {
  background-color: #f8fafc !important;
  color: #606266;
  font-weight: 600;
}
</style>
