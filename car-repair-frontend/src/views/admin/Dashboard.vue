<template>
  <div class="dashboard">
    <!-- 统计卡片 -->
    <el-row :gutter="20">
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-icon" style="background: #a8d8ea">
            <el-icon :size="30"><Document /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.orderCount || 0 }}</div>
            <div class="stat-label">总工单数</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-icon" style="background: #aa96da">
            <el-icon :size="30"><Money /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">¥{{ formatMoney(stats.totalRevenue) }}</div>
            <div class="stat-label">总收入</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-icon" style="background: #b8e0d2">
            <el-icon :size="30"><User /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.totalCustomers || 0 }}</div>
            <div class="stat-label">客户数</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-icon" style="background: #fcbad3">
            <el-icon :size="30"><Star /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.avgScore || 0 }}</div>
            <div class="stat-label">平均评分</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 第一行图表：收入趋势（折线图） + 工单趋势（柱状图+折线图） -->
    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :span="12">
        <el-card>
          <template #header><span>收入趋势分析</span></template>
          <div ref="revenueChartRef" class="chart-container"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template #header><span>工单趋势统计</span></template>
          <div ref="orderTrendChartRef" class="chart-container"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 第二行图表：维修类型分布（饼图） + 技师工作量（柱状图） -->
    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :span="12">
        <el-card>
          <template #header><span>维修类型分布</span></template>
          <div ref="serviceTypeChartRef" class="chart-container"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template #header><span>技师工作量排行</span></template>
          <div ref="technicianChartRef" class="chart-container"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 第三行图表：配件周转率（柱状图） + 定价效果（柱状图） -->
    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :span="12">
        <el-card>
          <template #header><span>配件周转率 TOP10</span></template>
          <div ref="turnoverChartRef" class="chart-container"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template #header><span>定价效果评估</span></template>
          <div ref="pricingChartRef" class="chart-container"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 第四行：库存预警 + 客户留存分析 -->
    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :span="12">
        <el-card>
          <template #header><span>库存预警</span></template>
          <el-table :data="warningParts" max-height="300">
            <el-table-column prop="partName" label="配件名称" align="center" />
            <el-table-column prop="stockQuantity" label="当前库存" align="center" />
            <el-table-column prop="minStock" label="最低库存" align="center" />
            <el-table-column label="状态" align="center">
              <template #default><el-tag type="danger">库存不足</el-tag></template>
            </el-table-column>
          </el-table>
          <el-empty v-if="!warningParts.length" description="暂无库存预警" />
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template #header><span>客户留存分析</span></template>
          <div ref="retentionChartRef" class="chart-container"></div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted, nextTick } from 'vue'
import request from '@/utils/request'
import * as echarts from 'echarts'

const stats = reactive({ orderCount: 0, totalRevenue: 0, totalCustomers: 0, avgScore: 0 })
const warningParts = ref<any[]>([])
const retention = reactive({ totalCustomers: 0, repeatCustomers: 0, retentionRate: 0 })

// 图表引用
const revenueChartRef = ref<HTMLElement>()
const orderTrendChartRef = ref<HTMLElement>()
const serviceTypeChartRef = ref<HTMLElement>()
const technicianChartRef = ref<HTMLElement>()
const turnoverChartRef = ref<HTMLElement>()
const pricingChartRef = ref<HTMLElement>()
const retentionChartRef = ref<HTMLElement>()

// 图表实例
let revenueChart: echarts.ECharts | null = null
let orderTrendChart: echarts.ECharts | null = null
let serviceTypeChart: echarts.ECharts | null = null
let technicianChart: echarts.ECharts | null = null
let turnoverChart: echarts.ECharts | null = null
let pricingChart: echarts.ECharts | null = null
let retentionChart: echarts.ECharts | null = null

// 统一淡色系配色（根据设计文档）
const themeColors = ['#a8d8ea', '#aa96da', '#fcbad3', '#b8e0d2', '#ffffd2', '#ffd3b6']

// 格式化金额
const formatMoney = (value: number) => {
  if (!value) return '0'
  return value >= 10000 ? (value / 10000).toFixed(1) + '万' : value.toFixed(0)
}

// 加载基础统计数据
const loadBasicStats = async () => {
  try {
    const today = new Date()
    const startDate = `${today.getFullYear()}-01-01`
    const endDate = `${today.getFullYear()}-12-31`
    
    const revenueRes = await request.get('/admin/statistics/revenue', { params: { startDate, endDate } })
    stats.totalRevenue = revenueRes.data.totalRevenue
    stats.orderCount = revenueRes.data.orderCount

    const retentionRes = await request.get('/admin/statistics/customerRetention')
    Object.assign(retention, retentionRes.data)
    stats.totalCustomers = retentionRes.data.totalCustomers

    const scoreRes = await request.get('/admin/statistics/avgScore')
    stats.avgScore = scoreRes.data.avgScore

    const warningRes = await request.get('/admin/part/warning')
    warningParts.value = warningRes.data
  } catch { /* 忽略错误 */ }
}

// 初始化收入趋势图表（多线折线图：总收入、工时收入、配件收入）
const initRevenueChart = async () => {
  if (!revenueChartRef.value) return
  revenueChart = echarts.init(revenueChartRef.value)
  try {
    // 计算最近7天的日期范围（从6天前到今天，共7天）
    const today = new Date()
    const sixDaysAgo = new Date(today)
    sixDaysAgo.setDate(today.getDate() - 6)
    
    const formatDate = (date: Date) => {
      const year = date.getFullYear()
      const month = String(date.getMonth() + 1).padStart(2, '0')
      const day = String(date.getDate()).padStart(2, '0')
      return `${year}-${month}-${day}`
    }
    
    const res = await request.get('/admin/statistics/revenueTrend', { 
      params: { 
        startDate: formatDate(sixDaysAgo), 
        endDate: formatDate(today) 
      } 
    })
    const data = res.data || []
    
    // 只取最近7天的数据
    const last7Days = data.slice(-7)
    
    revenueChart.setOption({
      tooltip: { trigger: 'axis', axisPointer: { type: 'cross' } },
      legend: { data: ['总收入', '工时收入', '配件收入'], top: 5 },
      grid: { left: '3%', right: '4%', bottom: '3%', top: '15%', containLabel: true },
      xAxis: { 
        type: 'category', 
        boundaryGap: false,
        data: last7Days.map((i: any) => i.month), 
        axisLine: { lineStyle: { color: '#ddd' } },
        axisLabel: { rotate: 0, interval: 0 }
      },
      yAxis: { 
        type: 'value', 
        name: '金额(元)',
        axisLine: { show: false }, 
        splitLine: { lineStyle: { color: '#eee' } },
        axisLabel: { formatter: (v: number) => v >= 10000 ? (v / 10000).toFixed(1) + '万' : v }
      },
      series: [
        { 
          name: '总收入', type: 'line', smooth: true, 
          data: last7Days.map((i: any) => i.revenue), 
          itemStyle: { color: themeColors[0] },
          areaStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(168, 216, 234, 0.4)' }, { offset: 1, color: 'rgba(168, 216, 234, 0.05)' }
          ])}
        },
        { 
          name: '工时收入', type: 'line', smooth: true, 
          data: last7Days.map((i: any) => i.laborRevenue || 0), 
          itemStyle: { color: themeColors[1] }
        },
        { 
          name: '配件收入', type: 'line', smooth: true, 
          data: last7Days.map((i: any) => i.partsRevenue || 0), 
          itemStyle: { color: themeColors[3] }
        }
      ]
    })
  } catch { /* 忽略错误 */ }
}

// 初始化工单趋势图表（柱状图+折线图组合）
const initOrderTrendChart = async () => {
  if (!orderTrendChartRef.value) return
  orderTrendChart = echarts.init(orderTrendChartRef.value)
  try {
    const today = new Date()
    const res = await request.get('/admin/statistics/orderTrend', { 
      params: { startDate: `${today.getFullYear()}-01-01`, endDate: `${today.getFullYear()}-12-31` } 
    })
    const data = res.data || []
    orderTrendChart.setOption({
      tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
      legend: { data: ['工单数', '完成率'], top: 5 },
      grid: { left: '3%', right: '4%', bottom: '3%', top: '15%', containLabel: true },
      xAxis: { 
        type: 'category', 
        data: data.map((i: any) => i.month.substring(5) + '月'),
        axisLine: { lineStyle: { color: '#ddd' } }
      },
      yAxis: [
        { type: 'value', name: '工单数', axisLine: { show: false }, splitLine: { lineStyle: { color: '#eee' } } },
        { type: 'value', name: '完成率(%)', min: 0, max: 100, position: 'right', axisLine: { show: false }, splitLine: { show: false } }
      ],
      series: [
        { 
          name: '工单数', type: 'bar', 
          data: data.map((i: any) => i.orderCount),
          itemStyle: { 
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: themeColors[0] }, { offset: 1, color: themeColors[3] }
            ]), 
            borderRadius: [4, 4, 0, 0] 
          },
          barWidth: '50%'
        },
        { 
          name: '完成率', type: 'line', yAxisIndex: 1, smooth: true,
          data: data.map((i: any) => i.completionRate || 0),
          itemStyle: { color: themeColors[2] },
          symbol: 'circle', symbolSize: 8
        }
      ]
    })
  } catch { /* 忽略错误 */ }
}

// 初始化维修类型分布图表（环形饼图）
const initServiceTypeChart = async () => {
  if (!serviceTypeChartRef.value) return
  serviceTypeChart = echarts.init(serviceTypeChartRef.value)
  try {
    const res = await request.get('/admin/statistics/serviceTypeDistribution')
    const data = res.data || []
    serviceTypeChart.setOption({
      tooltip: { trigger: 'item', formatter: '{b}: {c}单 ({d}%)' },
      legend: { orient: 'vertical', right: '5%', top: 'center' },
      series: [{ 
        name: '维修类型', type: 'pie', radius: ['40%', '70%'], center: ['40%', '50%'],
        itemStyle: { borderRadius: 8, borderColor: '#fff', borderWidth: 2 },
        label: { show: true, formatter: '{b}\n{d}%' },
        emphasis: { label: { show: true, fontSize: 14, fontWeight: 'bold' } },
        data: data.map((i: any, idx: number) => ({ ...i, itemStyle: { color: themeColors[idx % themeColors.length] } }))
      }]
    })
  } catch { /* 忽略错误 */ }
}

// 初始化技师工作量排行图表（横向柱状图）
const initTechnicianChart = async () => {
  if (!technicianChartRef.value) return
  technicianChart = echarts.init(technicianChartRef.value)
  try {
    const res = await request.get('/admin/statistics/technicianWorkload')
    const data = res.data || []
    technicianChart.setOption({
      tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
      legend: { data: ['完成工单数', '平均评分'], top: 5 },
      grid: { left: '3%', right: '10%', bottom: '3%', top: '15%', containLabel: true },
      xAxis: { type: 'value' },
      yAxis: { 
        type: 'category', 
        data: data.map((i: any) => i.technicianName),
        axisLine: { lineStyle: { color: '#ddd' } }
      },
      series: [
        { 
          name: '完成工单数', type: 'bar',
          data: data.map((i: any) => i.orderCount),
          itemStyle: { 
            color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
              { offset: 0, color: themeColors[0] }, { offset: 1, color: themeColors[1] }
            ]),
            borderRadius: [0, 4, 4, 0]
          },
          barWidth: '50%',
          label: { show: true, position: 'right', formatter: '{c}单' }
        }
      ]
    })
  } catch { /* 忽略错误 */ }
}

// 初始化配件周转率图表（柱状图+折线图组合）
const initTurnoverChart = async () => {
  if (!turnoverChartRef.value) return
  turnoverChart = echarts.init(turnoverChartRef.value)
  try {
    const res = await request.get('/admin/statistics/partTurnover')
    const data = res.data || []
    turnoverChart.setOption({
      tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
      legend: { data: ['周转率(%)', '周转天数'], top: 5 },
      grid: { left: '3%', right: '8%', bottom: '15%', top: '15%', containLabel: true },
      xAxis: { 
        type: 'category', 
        data: data.map((i: any) => i.partName), 
        axisLabel: { rotate: 30, fontSize: 10 },
        axisLine: { lineStyle: { color: '#ddd' } }
      },
      yAxis: [
        { type: 'value', name: '周转率(%)', axisLine: { show: false }, splitLine: { lineStyle: { color: '#eee' } } },
        { type: 'value', name: '周转天数', position: 'right', axisLine: { show: false }, splitLine: { show: false } }
      ],
      series: [
        { 
          name: '周转率(%)', type: 'bar', 
          data: data.map((i: any) => i.turnoverRate),
          itemStyle: { 
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: themeColors[0] }, { offset: 1, color: themeColors[3] }
            ]), 
            borderRadius: [4, 4, 0, 0] 
          }, 
          barWidth: '50%'
        },
        { 
          name: '周转天数', type: 'line', yAxisIndex: 1, smooth: true,
          data: data.map((i: any) => i.turnoverDays || 0),
          itemStyle: { color: themeColors[2] },
          symbol: 'circle', symbolSize: 6
        }
      ]
    })
  } catch { /* 忽略错误 */ }
}

// 初始化定价效果图表（分组柱状图）
const initPricingChart = async () => {
  if (!pricingChartRef.value) return
  pricingChart = echarts.init(pricingChartRef.value)
  try {
    const res = await request.get('/admin/statistics/pricingEffect')
    const data = res.data || {}
    pricingChart.setOption({
      tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
      legend: { data: ['动态定价', '固定定价估算'], top: 5 },
      grid: { left: '3%', right: '4%', bottom: '15%', top: '15%', containLabel: true },
      xAxis: { 
        type: 'category', 
        data: ['总收入', '平均客单价', '毛利率(%)'],
        axisLine: { lineStyle: { color: '#ddd' } }
      },
      yAxis: { type: 'value', axisLine: { show: false }, splitLine: { lineStyle: { color: '#eee' } } },
      series: [
        { 
          name: '动态定价', type: 'bar', 
          data: [data.dynamicRevenue || 0, data.avgDynamicPrice || 0, data.dynamicProfitRate || 32], 
          itemStyle: { color: themeColors[0], borderRadius: [4, 4, 0, 0] }, 
          barWidth: '25%',
          label: { show: true, position: 'top', fontSize: 10 }
        },
        { 
          name: '固定定价估算', type: 'bar', 
          data: [data.estimatedFixedRevenue || 0, data.avgFixedPrice || 0, data.fixedProfitRate || 28], 
          itemStyle: { color: '#d4d4d4', borderRadius: [4, 4, 0, 0] }, 
          barWidth: '25%',
          label: { show: true, position: 'top', fontSize: 10 }
        }
      ],
      graphic: [{ 
        type: 'text', left: 'center', bottom: 5, 
        style: { text: `动态定价收益提升: ${data.increaseRate || 0}%`, fill: themeColors[1], fontSize: 13, fontWeight: 'bold' } 
      }]
    })
  } catch { /* 忽略错误 */ }
}

// 初始化客户留存图表（漏斗图）
const initRetentionChart = () => {
  if (!retentionChartRef.value) return
  retentionChart = echarts.init(retentionChartRef.value)
  retentionChart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: {c}人' },
    series: [{ 
      name: '客户留存', type: 'funnel', left: '10%', width: '80%', sort: 'descending', gap: 2,
      label: { show: true, position: 'inside', formatter: '{b}\n{c}人', fontSize: 12 },
      labelLine: { show: false },
      itemStyle: { borderWidth: 0 },
      data: [
        { value: retention.totalCustomers, name: '总客户', itemStyle: { color: themeColors[0] } },
        { value: retention.repeatCustomers, name: '回头客', itemStyle: { color: themeColors[1] } }
      ]
    }],
    graphic: [{ 
      type: 'text', left: 'center', bottom: 10, 
      style: { text: `客户留存率: ${retention.retentionRate}%`, fill: themeColors[1], fontSize: 14, fontWeight: 'bold' } 
    }]
  })
}

// 窗口大小变化时重绘图表
const handleResize = () => {
  revenueChart?.resize()
  orderTrendChart?.resize()
  serviceTypeChart?.resize()
  technicianChart?.resize()
  turnoverChart?.resize()
  pricingChart?.resize()
  retentionChart?.resize()
}

onMounted(async () => {
  await loadBasicStats()
  await nextTick()
  initRevenueChart()
  initOrderTrendChart()
  initServiceTypeChart()
  initTechnicianChart()
  initTurnoverChart()
  initPricingChart()
  initRetentionChart()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  revenueChart?.dispose()
  orderTrendChart?.dispose()
  serviceTypeChart?.dispose()
  technicianChart?.dispose()
  turnoverChart?.dispose()
  pricingChart?.dispose()
  retentionChart?.dispose()
})
</script>

<style scoped>
.stat-card { display: flex; align-items: center; justify-content: center; padding: 20px; }
.stat-card :deep(.el-card__body) { display: flex; align-items: center; justify-content: center; width: 100%; }
.stat-icon { width: 60px; height: 60px; border-radius: 8px; display: flex; align-items: center; justify-content: center; color: #fff; margin-right: 15px; flex-shrink: 0; }
.stat-info { text-align: center; }
.stat-value { font-size: 24px; font-weight: bold; color: #303133; }
.stat-label { font-size: 14px; color: #909399; margin-top: 5px; }
.chart-container { height: 280px; width: 100%; }
</style>
