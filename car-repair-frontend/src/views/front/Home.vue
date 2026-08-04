<template>
  <div class="home">
    <!-- 轮播图 -->
    <el-carousel height="320px" class="banner-carousel">
      <el-carousel-item v-for="(banner, index) in banners" :key="index">
        <div class="banner-item" :style="{ backgroundImage: `url(${banner.bgImage})` }">
          <div class="banner-overlay"></div>
          <div class="banner-content">
            <h1>{{ banner.title }}</h1>
            <p>{{ banner.subtitle }}</p>
            <el-button type="primary" round @click="router.push(banner.link)">
              {{ banner.btnText }}
            </el-button>
          </div>
        </div>
      </el-carousel-item>
    </el-carousel>
    
    <el-row :gutter="20" class="quick-actions">
      <el-col :span="8">
        <el-card shadow="hover" @click="router.push('/booking')">
          <el-icon :size="40" color="#5a9bd5"><Calendar /></el-icon>
          <h3>在线预约</h3>
          <p>选择服务项目，预约维修时间</p>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover" @click="router.push('/vehicle')">
          <el-icon :size="40" color="#7ab8d9"><Van /></el-icon>
          <h3>车辆管理</h3>
          <p>管理您的车辆信息</p>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover" @click="router.push('/order')">
          <el-icon :size="40" color="#8cc5e0"><Document /></el-icon>
          <h3>订单跟踪</h3>
          <p>查看维修进度和历史记录</p>
        </el-card>
      </el-col>
    </el-row>

    <el-card class="service-intro">
      <template #header>
        <span>服务项目</span>
      </template>
      <el-row :gutter="20">
        <el-col :span="6" v-for="item in services" :key="item.name">
          <div class="service-item" @click="goToBooking(item.category)">
            <el-icon :size="30" :color="item.color"><component :is="item.icon" /></el-icon>
            <h4>{{ item.name }}</h4>
          </div>
        </el-col>
      </el-row>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()

// 轮播图数据
const banners = [
  {
    title: '专业汽车维修服务',
    subtitle: '便捷预约，专业维修，贴心服务',
    btnText: '立即预约',
    link: '/booking',
    bgImage: 'https://images.unsplash.com/photo-1486262715619-67b85e0b08d3?w=1200&h=400&fit=crop',
    icon: 'Van'
  },
  {
    title: '常规保养套餐',
    subtitle: '机油更换、滤芯更换、全车检测',
    btnText: '查看详情',
    link: '/booking?category=保养',
    bgImage: 'https://images.unsplash.com/photo-1619642751034-765dfdf7c58e?w=1200&h=400&fit=crop',
    icon: 'Tools'
  },
  {
    title: '实时进度追踪',
    subtitle: '随时掌握爱车维修状态',
    btnText: '查看订单',
    link: '/order',
    bgImage: 'https://images.unsplash.com/photo-1492144534655-ae79c964c9d7?w=1200&h=400&fit=crop',
    icon: 'Document'
  }
]

const services = [
  { name: '常规保养', icon: 'Tools', color: '#5a9bd5', category: '保养' },
  { name: '故障维修', icon: 'Setting', color: '#7ab8d9', category: '维修' },
  { name: '钣金喷漆', icon: 'Brush', color: '#8cc5e0', category: '钣喷' },
  { name: '电子检测', icon: 'Monitor', color: '#9dd1e8', category: '维修' }
]

// 跳转到预约页面
const goToBooking = (category: string) => {
  router.push({ path: '/booking', query: { category } })
}
</script>

<style scoped>
.home {
  padding: 20px 0;
}

/* 轮播图样式 */
.banner-carousel {
  border-radius: 8px;
  overflow: hidden;
  margin-bottom: 20px;
}

.banner-item {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 60px;
  color: #fff;
  background-size: cover;
  background-position: center;
  background-repeat: no-repeat;
  position: relative;
}

.banner-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.35);
}

.banner-content {
  position: relative;
  z-index: 1;
  text-align: center;
}

.banner-content h1 {
  font-size: 36px;
  margin-bottom: 15px;
  font-weight: 600;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
}

.banner-content p {
  font-size: 18px;
  margin-bottom: 25px;
  opacity: 0.95;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.2);
}

.banner-content .el-button {
  background: rgba(255, 255, 255, 0.95);
  color: #5a9bd5;
  border: none;
  padding: 12px 30px;
  font-size: 16px;
}

.banner-content .el-button:hover {
  background: #fff;
  transform: scale(1.05);
}

.quick-actions {
  margin-bottom: 20px;
}

.quick-actions .el-card {
  text-align: center;
  padding: 20px;
  cursor: pointer;
  transition: transform 0.3s;
}

.quick-actions .el-card:hover {
  transform: translateY(-5px);
}

.quick-actions h3 {
  margin: 15px 0 10px;
}

.quick-actions p {
  color: #909399;
  font-size: 14px;
}

.service-intro .service-item {
  text-align: center;
  padding: 20px;
  cursor: pointer;
  border-radius: 8px;
  transition: all 0.3s;
}

.service-intro .service-item:hover {
  background: #f5f9fc;
  transform: translateY(-3px);
}

.service-intro h4 {
  margin-top: 10px;
  color: #606266;
}
</style>
