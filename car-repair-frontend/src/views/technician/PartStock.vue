<template>
  <div class="part-stock-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <div class="title-wrapper">
            <span class="card-title">配件库存查询</span>
          </div>
          <div class="filter-wrapper">
            <el-input v-model="keyword" placeholder="搜索配件名称或编码" clearable style="width: 280px" @change="loadData" size="default">
              <template #prefix><el-icon><Search /></el-icon></template>
            </el-input>
          </div>
        </div>
      </template>
      
      <el-table :data="parts" v-loading="loading" border>
        <el-table-column prop="partCode" label="配件编码" align="center" width="120" />
        <el-table-column label="配件图片" align="center" width="100">
          <template #default="{ row }">
            <el-image v-if="row.imageUrl" :src="row.imageUrl" :preview-src-list="[row.imageUrl]" 
              fit="cover" style="width: 60px; height: 60px; border-radius: 4px; cursor: pointer" />
            <span v-else style="color: #909399; font-size: 12px">暂无图片</span>
          </template>
        </el-table-column>
        <el-table-column prop="partName" label="配件名称" align="center" />
        <el-table-column prop="category" label="分类" align="center" width="100" />
        <el-table-column prop="brand" label="品牌" align="center" width="100" />
        <el-table-column prop="unit" label="单位" align="center" width="80" />
        <el-table-column label="库存状态" align="center" width="120">
          <template #default="{ row }">
            <el-tag :type="getStockType(row)" size="small">
              {{ getStockStatus(row) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="库存数量" align="center" width="100">
          <template #default="{ row }">
            <span :style="{ color: row.stockQuantity - row.reservedQuantity <= row.minStock ? '#f56c6c' : '#67c23a' }">
              {{ row.stockQuantity - row.reservedQuantity }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="salePrice" label="销售价" align="center" width="100">
          <template #default="{ row }">¥{{ row.salePrice }}</template>
        </el-table-column>
      </el-table>
      
      <el-pagination v-model:current-page="page" :page-size="pageSize" :total="total" 
        layout="total, prev, pager, next" @current-change="loadData" 
        style="margin-top: 20px; justify-content: flex-end" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import request from '@/utils/request'

const parts = ref<any[]>([])
const loading = ref(false)
const page = ref(1)
const pageSize = ref(15)
const total = ref(0)
const keyword = ref('')

const getStockStatus = (row: any) => {
  const available = row.stockQuantity - row.reservedQuantity
  if (available <= 0) return '缺货'
  if (available <= row.minStock) return '库存不足'
  return '库存充足'
}

const getStockType = (row: any) => {
  const available = row.stockQuantity - row.reservedQuantity
  if (available <= 0) return 'danger'
  if (available <= row.minStock) return 'warning'
  return 'success'
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await request.get('/technician/part/list', { 
      params: { page: page.value, size: pageSize.value, keyword: keyword.value } 
    })
    parts.value = res.data.list || []
    total.value = res.data.total || 0
  } finally {
    loading.value = false
  }
}

onMounted(loadData)
</script>

<style scoped>
.part-stock-page {
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
