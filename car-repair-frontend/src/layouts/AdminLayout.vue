<template>
  <el-container class="admin-layout">
    <el-aside width="200px" class="aside">
      <div class="logo">管理后台</div>
      <el-menu :default-active="route.path" router background-color="#f0f7fc" text-color="#5a6a7a" active-text-color="#5a9bd5">
        <el-menu-item index="/admin">
          <el-icon><DataAnalysis /></el-icon>
          <span>数据统计</span>
        </el-menu-item>
        <el-menu-item index="/admin/booking">
          <el-icon><Calendar /></el-icon>
          <span>预约管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/order">
          <el-icon><Document /></el-icon>
          <span>工单管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/service">
          <el-icon><Tools /></el-icon>
          <span>服务项目</span>
        </el-menu-item>
        <el-menu-item index="/admin/part">
          <el-icon><Box /></el-icon>
          <span>配件库存</span>
        </el-menu-item>
        <el-menu-item index="/admin/employee">
          <el-icon><User /></el-icon>
          <span>员工管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/pricing">
          <el-icon><PriceTag /></el-icon>
          <span>定价配置</span>
        </el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="header">
        <div class="header-right">
          <el-dropdown @command="handleCommand">
            <span class="el-dropdown-link">
              {{ userStore.userInfo?.realName || '管理员' }}
              <el-icon class="el-icon--right"><arrow-down /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="logout">退出登录</el-dropdown-item>
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
</template>

<script setup lang="ts">
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const handleCommand = async (command: string) => {
  if (command === 'logout') {
    await userStore.logout()
    router.push('/login')
  }
}
</script>

<style scoped>
.admin-layout {
  min-height: 100vh;
}

.aside {
  background-color: #f0f7fc;
}

.logo {
  height: 60px;
  line-height: 60px;
  text-align: center;
  font-size: 18px;
  font-weight: bold;
  color: #5a9bd5;
  background-color: #e0eef8;
}

.header {
  background: #fff;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.1);
  display: flex;
  align-items: center;
  justify-content: flex-end;
  padding: 0 20px;
}

.el-dropdown-link {
  cursor: pointer;
  color: #5a9bd5;
  display: flex;
  align-items: center;
}

.main {
  background: #f5f9fc;
  padding: 20px;
}
</style>
