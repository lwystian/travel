<template>
  <div class="order-management">
    <div class="page-header">
      <h1 class="page-title">订单管理</h1>
      <p class="page-subtitle">Order Management</p>
    </div>

    <!-- 搜索表单 -->
    <el-card class="search-card" shadow="never">
      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="订单号">
          <el-input v-model="searchForm.orderNo" placeholder="请输入订单号" clearable></el-input>
        </el-form-item>
        <el-form-item label="联系人">
          <el-input v-model="searchForm.contactName" placeholder="请输入联系人姓名" clearable></el-input>
        </el-form-item>
        <el-form-item label="联系电话">
          <el-input v-model="searchForm.contactPhone" placeholder="请输入联系电话" clearable></el-input>
        </el-form-item>
        <el-form-item label="订单状态">
          <el-select v-model="searchForm.status" placeholder="请选择订单状态" clearable>
            <el-option label="待支付" :value="0"></el-option>
            <el-option label="已支付" :value="1"></el-option>
            <el-option label="已取消" :value="2"></el-option>
            <el-option label="已退款" :value="3"></el-option>
            <el-option label="已完成" :value="4"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch" class="search-btn">
            <i class="el-icon-search"></i> 查询
          </el-button>
          <el-button @click="resetSearch" class="reset-btn">
            <i class="el-icon-refresh"></i> 重置
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 订单列表表格 -->
    <el-card class="table-card" shadow="never">
      <el-table
        v-loading="loading"
        :data="orderList"
        border
        style="width: 100%"
        class="order-table"
      >
        <el-table-column prop="orderNo" label="订单号" width="180" />
        <el-table-column prop="tourName" label="行程名称" min-width="150" />
        <el-table-column prop="packageName" label="套餐" width="100">
          <template #default="scope">
            {{ scope.row.packageName || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="出发日期" width="100">
          <template #default="scope">
            {{ formatDate(scope.row.departureDate) }}
          </template>
        </el-table-column>
        <el-table-column label="人数" width="100" align="center">
          <template #default="scope">
            成人 {{ scope.row.adultCount }}<span v-if="scope.row.childCount"> / 儿童 {{ scope.row.childCount }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="contactName" label="联系人" width="100" />
        <el-table-column prop="contactPhone" label="联系电话" width="120" />
        <el-table-column prop="totalAmount" label="总金额" width="100">
          <template #default="scope">
            <span class="amount">¥{{ scope.row.totalAmount }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="订单状态" width="100">
          <template #default="scope">
            <el-tag :type="getOrderStatusType(scope.row.status)">
              {{ getOrderStatusText(scope.row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="下单时间" width="150">
          <template #default="scope">
            <span class="date-text">{{ formatTime(scope.row.createTime) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="scope">
            <el-button type="primary" size="small" plain @click="handleViewDetail(scope.row)" class="action-btn">
              <i class="el-icon-view"></i> 详情
            </el-button>
            <el-button
              v-if="scope.row.status === 1"
              type="warning"
              size="small"
              plain
              @click="handleRefund(scope.row)"
              class="action-btn"
            >
              <i class="el-icon-money"></i> 退款
            </el-button>
            <el-button
              v-if="[2, 3, 4].includes(scope.row.status)"
              type="danger"
              size="small"
              plain
              @click="handleDelete(scope.row)"
              class="action-btn"
            >
              <i class="el-icon-delete"></i> 删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-container">
        <el-pagination
          background
          layout="total, sizes, prev, pager, next, jumper"
          :total="total"
          :page-size="pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :current-page="currentPage"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>

    <!-- 订单详情对话框 -->
    <el-dialog
      title="订单详情"
      v-model="detailDialogVisible"
      width="700px"
      class="order-dialog"
    >
      <div class="order-detail" v-if="currentOrder">
        <el-descriptions title="行程订单信息" :column="2" border>
          <el-descriptions-item label="订单号" :span="2">{{ currentOrder.orderNo }}</el-descriptions-item>
          <el-descriptions-item label="行程名称" :span="2">{{ currentOrder.tourName }}</el-descriptions-item>
          <el-descriptions-item label="套餐类型">{{ currentOrder.packageName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="批次套餐">{{ currentOrder.batchPackageName || '标准' }}</el-descriptions-item>
          <el-descriptions-item label="出发日期">{{ formatDate(currentOrder.departureDate) }}</el-descriptions-item>
          <el-descriptions-item label="成人人数">{{ currentOrder.adultCount }} 人</el-descriptions-item>
          <el-descriptions-item label="儿童人数">{{ currentOrder.childCount || 0 }} 人</el-descriptions-item>
          <el-descriptions-item label="成人单价">¥{{ currentOrder.adultUnitPrice || 0 }}</el-descriptions-item>
          <el-descriptions-item label="儿童单价">¥{{ currentOrder.childUnitPrice || 0 }}</el-descriptions-item>
          <el-descriptions-item label="行程费用">¥{{ currentOrder.tourAmount || 0 }}</el-descriptions-item>
          <el-descriptions-item label="酒店费用">
            <template v-if="currentOrder.hotelAmount && currentOrder.hotelAmount > 0">
              ¥{{ currentOrder.hotelAmount }}（{{ currentOrder.hotelName }} {{ currentOrder.hotelDays }}晚）
            </template>
            <template v-else>-</template>
          </el-descriptions-item>
          <el-descriptions-item label="总金额" :span="2">
            <span class="amount">¥{{ currentOrder.totalAmount }}</span>
          </el-descriptions-item>
        </el-descriptions>

        <el-descriptions title="联系人信息" :column="1" border style="margin-top: 20px">
          <el-descriptions-item label="联系人">{{ currentOrder.contactName }}</el-descriptions-item>
          <el-descriptions-item label="联系电话">{{ currentOrder.contactPhone }}</el-descriptions-item>
          <el-descriptions-item label="备注">{{ currentOrder.remark || '无' }}</el-descriptions-item>
        </el-descriptions>

        <el-descriptions title="订单状态" :column="1" border style="margin-top: 20px">
          <el-descriptions-item label="订单状态">
            <el-tag :type="getOrderStatusType(currentOrder.status)">
              {{ getOrderStatusText(currentOrder.status) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="下单时间">{{ formatTime(currentOrder.createTime) }}</el-descriptions-item>
          <el-descriptions-item label="支付时间" v-if="currentOrder.paymentTime">
            {{ formatTime(currentOrder.paymentTime) }}
          </el-descriptions-item>
          <el-descriptions-item label="支付方式" v-if="currentOrder.paymentMethod">
            {{ currentOrder.paymentMethod }}
          </el-descriptions-item>
        </el-descriptions>

        <el-descriptions title="用户信息" :column="1" border style="margin-top: 20px">
          <el-descriptions-item label="用户ID">{{ currentOrder.userId }}</el-descriptions-item>
        </el-descriptions>
      </div>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="detailDialogVisible = false">关闭</el-button>
          <template v-if="currentOrder && currentOrder.status === 1">
            <el-button type="warning" plain @click="handleRefund(currentOrder)">
              <i class="el-icon-money"></i> 退款
            </el-button>
          </template>
          <template v-if="currentOrder && [2, 3, 4].includes(currentOrder.status)">
            <el-button type="danger" plain @click="handleDelete(currentOrder)">
              <i class="el-icon-delete"></i> 删除
            </el-button>
          </template>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'

// 分页参数
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

// 订单列表数据
const orderList = ref([])
const loading = ref(false)

// 搜索表单
const searchForm = reactive({
  orderNo: '',
  contactName: '',
  contactPhone: '',
  status: null
})

// 当前查看的订单
const currentOrder = ref(null)
const detailDialogVisible = ref(false)

// 获取订单列表
const fetchOrders = async () => {
  loading.value = true
  try {
    const res = await request.get('/tour-order/all', {
      orderNo: searchForm.orderNo,
      contactName: searchForm.contactName,
      contactPhone: searchForm.contactPhone,
      status: searchForm.status,
      currentPage: currentPage.value,
      size: pageSize.value
    }, {
      showDefaultMsg: false
    })
    orderList.value = res?.records || []
    total.value = res?.total || 0
  } catch (error) {
    console.error('获取订单列表失败:', error)
  } finally {
    loading.value = false
  }
}

// 搜索按钮点击事件
const handleSearch = () => {
  currentPage.value = 1
  fetchOrders()
}

// 重置搜索条件
const resetSearch = () => {
  searchForm.orderNo = ''
  searchForm.contactName = ''
  searchForm.contactPhone = ''
  searchForm.status = null
  currentPage.value = 1
  fetchOrders()
}

// 分页变化事件
const handleSizeChange = (size) => {
  pageSize.value = size
  fetchOrders()
}

const handleCurrentChange = (page) => {
  currentPage.value = page
  fetchOrders()
}

// 查看订单详情
const handleViewDetail = async (row) => {
  try {
    const res = await request.get(`/tour-order/${row.id}`, {}, {
      showDefaultMsg: false
    })
    currentOrder.value = res
    detailDialogVisible.value = true
  } catch (error) {
    console.error('获取订单详情失败:', error)
    ElMessage.error('获取订单详情失败')
  }
}

// 退款操作
const handleRefund = (row) => {
  ElMessageBox.confirm(`确定要对订单"${row.orderNo}"进行退款操作吗？`, '退款确认', {
    confirmButtonText: '确定退款',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await request.put(`/tour-order/${row.id}/refund`, {}, {
        showDefaultMsg: false
      })
      ElMessage.success('订单退款成功')
      fetchOrders()
      if (detailDialogVisible.value) {
        detailDialogVisible.value = false
      }
    } catch (error) {
      console.error('订单退款失败:', error)
    }
  }).catch(() => {})
}

// 删除订单操作
const handleDelete = (row) => {
  ElMessageBox.confirm(`确定要删除订单"${row.orderNo}"吗？此操作不可恢复！`, '删除确认', {
    confirmButtonText: '确定删除',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await request.delete(`/tour-order/${row.id}`, {
        showDefaultMsg: false
      })
      ElMessage.success('订单删除成功')
      fetchOrders()
      if (detailDialogVisible.value) {
        detailDialogVisible.value = false
      }
    } catch (error) {
      console.error('删除订单失败:', error)
    }
  }).catch(() => {})
}

// 获取订单状态文本
const getOrderStatusText = (status) => {
  const statusMap = {
    0: '待支付',
    1: '已支付',
    2: '已取消',
    3: '已退款',
    4: '已完成'
  }
  return statusMap[status] || '未知状态'
}

// 获取订单状态类型
const getOrderStatusType = (status) => {
  const typeMap = {
    0: 'warning',
    1: 'success',
    2: 'info',
    3: 'danger',
    4: 'primary'
  }
  return typeMap[status] || 'info'
}

// 格式化日期
const formatDate = (dateStr) => {
  if (!dateStr) return '-'
  const date = new Date(dateStr)
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
}

// 格式化时间
const formatTime = (timeStr) => {
  if (!timeStr) return '-'
  const date = new Date(timeStr)
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')} ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}:${String(date.getSeconds()).padStart(2, '0')}`
}

// 页面加载时获取数据
onMounted(() => {
  fetchOrders()
})
</script>

<style lang="scss" scoped>
.order-management {
  padding: 20px;
  background-color: #f9fafc;
  min-height: calc(100vh - 120px);

  .page-header {
    margin-bottom: 24px;
    text-align: left;

    .page-title {
      font-size: 24px;
      color: #34495e;
      margin: 0 0 8px 0;
      font-weight: 500;
    }

    .page-subtitle {
      font-size: 14px;
      color: #7f8c8d;
      margin: 0;
      font-style: italic;
    }
  }

  .search-card {
    margin-bottom: 20px;
    border-radius: 8px;
    background-color: #fff;
    box-shadow: none;

    .search-form {
      padding: 10px 0;
      display: flex;
      flex-wrap: wrap;
      align-items: center;

      .el-form-item {
        margin-bottom: 0;
        margin-right: 16px;
      }

      .search-btn {
        background-color: #3498db;
        border-color: #3498db;

        &:hover, &:focus {
          background-color: #2980b9;
          border-color: #2980b9;
        }
      }

      .reset-btn {
        color: #7f8c8d;
        border-color: #bdc3c7;

        &:hover, &:focus {
          color: #34495e;
          border-color: #95a5a6;
          background-color: #FFFFFF;
        }
      }
    }
  }

  .table-card {
    border-radius: 8px;
    overflow: hidden;
    box-shadow: none;

    .order-table {
      border-radius: 4px;
      overflow: hidden;

      :deep(thead) {
        background-color: #ecf0f1;

        th {
          background-color: #ecf0f1;
          color: #34495e;
          font-weight: 500;
          padding: 12px 0;
        }
      }

      :deep(tbody tr) {
        transition: all 0.3s;

        &:hover {
          background-color: #f8f9fa;
        }
      }

      .amount {
        font-weight: 600;
        color: #e74c3c;
      }

      .date-text {
        color: #7f8c8d;
        font-size: 12px;
      }

      .action-btn {
        margin-right: 5px;
      }
    }
  }

  .pagination-container {
    margin-top: 20px;
    display: flex;
    justify-content: flex-end;
    padding: 0 20px;
  }

  .order-dialog {
    :deep(.el-dialog__header) {
      border-bottom: 1px solid #ecf0f1;
      padding: 20px;

      .el-dialog__title {
        font-size: 18px;
        color: #34495e;
        font-weight: 500;
      }
    }

    :deep(.el-dialog__body) {
      padding: 30px 20px;
    }

    :deep(.el-dialog__footer) {
      border-top: 1px solid #ecf0f1;
      padding: 15px 20px;
    }

    .dialog-footer {
      display: flex;
      justify-content: flex-end;
    }
  }
}
</style>
