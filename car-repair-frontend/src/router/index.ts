import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'Login',
      component: () => import('@/views/Login.vue')
    },
    {
      path: '/register',
      name: 'Register',
      component: () => import('@/views/Register.vue')
    },
    // 前台页面（车主用户）
    {
      path: '/',
      component: () => import('@/layouts/FrontLayout.vue'),
      meta: { requiresAuth: true, userType: 1 },
      children: [
        { path: '', name: 'Home', component: () => import('@/views/front/Home.vue') },
        { path: 'vehicle', name: 'Vehicle', component: () => import('@/views/front/Vehicle.vue') },
        { path: 'booking', name: 'Booking', component: () => import('@/views/front/Booking.vue') },
        { path: 'order', name: 'Order', component: () => import('@/views/front/Order.vue') },
        { path: 'order/:id', name: 'OrderDetail', component: () => import('@/views/front/OrderDetail.vue') },
        { path: 'profile', name: 'Profile', component: () => import('@/views/front/Profile.vue') },
        { path: 'message', name: 'Message', component: () => import('@/views/front/Message.vue') }
      ]
    },
    // 维修端（工作台）
    {
      path: '/technician',
      component: () => import('@/layouts/TechnicianLayout.vue'),
      meta: { requiresAuth: true, userType: 2 },
      children: [
        { path: '', name: 'TechHome', component: () => import('@/views/technician/Dashboard.vue') },
        { path: 'orders', name: 'TechOrders', component: () => import('@/views/technician/WorkOrder.vue') },
        { path: 'order/:id', name: 'TechOrderDetail', component: () => import('@/views/technician/OrderDetail.vue') },
        { path: 'parts', name: 'TechParts', component: () => import('@/views/technician/PartStock.vue') },
        { path: 'history', name: 'TechHistory', component: () => import('@/views/technician/History.vue') },
        { path: 'profile', name: 'TechProfile', component: () => import('@/views/technician/Profile.vue') }
      ]
    },
    // 管理端（后台管理）
    {
      path: '/admin',
      component: () => import('@/layouts/AdminLayout.vue'),
      meta: { requiresAuth: true, userType: 3 },
      children: [
        { path: '', name: 'Dashboard', component: () => import('@/views/admin/Dashboard.vue') },
        { path: 'booking', name: 'AdminBooking', component: () => import('@/views/admin/Booking.vue') },
        { path: 'order', name: 'AdminOrder', component: () => import('@/views/admin/Order.vue') },
        { path: 'service', name: 'AdminService', component: () => import('@/views/admin/Service.vue') },
        { path: 'part', name: 'AdminPart', component: () => import('@/views/admin/Part.vue') },
        { path: 'employee', name: 'AdminEmployee', component: () => import('@/views/admin/Employee.vue') },
        { path: 'pricing', name: 'AdminPricing', component: () => import('@/views/admin/Pricing.vue') }
      ]
    }
  ]
})

// 路由守卫
router.beforeEach(async (to, _from, next) => {
  const userStore = useUserStore()
  
  // 不需要认证的页面
  if (!to.meta.requiresAuth) {
    next()
    return
  }
  
  // 检查登录状态
  if (!userStore.isLoggedIn) {
    await userStore.getCurrentUser()
  }
  
  if (!userStore.isLoggedIn) {
    next('/login')
    return
  }
  
  // 检查用户类型权限
  const requiredType = to.meta.userType as number
  const userType = userStore.userInfo?.userType
  
  // 管理员可以访问所有页面
  if (userType === 3) {
    next()
    return
  }
  
  // 检查是否有权限访问
  if (requiredType && userType !== requiredType) {
    // 重定向到对应的首页
    if (userType === 1) next('/')
    else if (userType === 2) next('/technician')
    else next('/login')
    return
  }
  
  next()
})

export default router
