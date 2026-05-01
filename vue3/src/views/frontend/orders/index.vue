<template>
  <div class="orders-container">
    <!-- 页面头部容器 -->
    <div class="page-header-wrapper">
    <div class="page-header">
      <div class="header-content">
        <h1 class="page-title">
          <span class="title-icon">🎫</span>
          我的订单
        </h1>
        <p class="page-subtitle">
          查看和管理您的行程订单
        </p>
      </div>
    </div>
    </div>

    <!-- 标签页区域 -->
    <div class="orders-section">
      <div class="section-container">
        <div class="orders-tabs">
          <el-tabs
            v-model="activeTab"
            @tab-click="handleTabClick"
            class="modern-tabs"
          >
            <el-tab-pane label="全部" name="all">
              <template #label>
                <div class="tab-label">
                  <el-icon><List /></el-icon>
                  <span>全部</span>
                  <span class="tab-count">{{ orderStats.total || 0 }}</span>
                </div>
              </template>
            </el-tab-pane>
            <el-tab-pane label="待支付" name="0">
              <template #label>
                <div class="tab-label">
                  <el-icon><Clock /></el-icon>
                  <span>待支付</span>
                  <span class="tab-count">{{ orderStats.pending || 0 }}</span>
                </div>
              </template>
            </el-tab-pane>
            <el-tab-pane label="已支付" name="1">
              <template #label>
                <div class="tab-label">
                  <el-icon><Check /></el-icon>
                  <span>已支付</span>
                  <span class="tab-count">{{ orderStats.paid || 0 }}</span>
                </div>
              </template>
            </el-tab-pane>
            <el-tab-pane label="已取消" name="2">
              <template #label>
                <div class="tab-label">
                  <el-icon><Close /></el-icon>
                  <span>已取消</span>
                  <span class="tab-count">{{ orderStats.cancelled || 0 }}</span>
                </div>
              </template>
            </el-tab-pane>
            <el-tab-pane label="已退款" name="3">
              <template #label>
                <div class="tab-label">
                  <el-icon><RefreshLeft /></el-icon>
                  <span>已退款</span>
                  <span class="tab-count">{{ orderStats.refunded || 0 }}</span>
                </div>
              </template>
            </el-tab-pane>
            <el-tab-pane label="已完成" name="4">
              <template #label>
                <div class="tab-label">
                  <el-icon><CircleCheck /></el-icon>
                  <span>已完成</span>
                  <span class="tab-count">{{ orderStats.completed || 0 }}</span>
                </div>
              </template>
            </el-tab-pane>
          </el-tabs>

          <!-- 加载状态 -->
          <div v-if="loading" class="loading-state">
            <el-skeleton :rows="6" animated />
          </div>

          <!-- 空状态 -->
          <div v-else-if="orderList.length === 0" class="empty-state">
            <div class="empty-icon">📋</div>
            <h3 class="empty-title">暂无订单</h3>
            <p class="empty-desc">您还没有任何订单，快去预订行程吧</p>
            <el-button type="primary" @click="goToTicketList" class="empty-action">
              <el-icon><Ticket /></el-icon>
              浏览行程
            </el-button>
          </div>

          <!-- 订单列表 -->
          <div v-else class="orders-list">
            <div
              v-for="(order, index) in orderList"
              :key="order.id"
              class="order-card"
              :class="`delay-${(index % 4 + 1) * 100}`"
            >
              <div class="order-header">
                <div class="order-info">
                  <div class="order-no">
                    <el-icon><Document /></el-icon>
                    <span>{{ order.orderNo }}</span>
                  </div>
                  <div class="order-time">{{ formatTime(order.createTime) }}</div>
                </div>
                <div class="order-status-badge" :class="getStatusClass(order.status)">
                  {{ getOrderStatusText(order.status) }}
                </div>
              </div>

              <div class="order-content">
                <div class="ticket-section">
                  <div class="ticket-main">
                    <h3 class="ticket-name">{{ order.tourName }}</h3>
                    <div class="scenic-info" v-if="order.packageName">
                      <el-icon><Collection /></el-icon>
                      <span>{{ order.packageName }}</span>
                    </div>
                  </div>

                  <div class="order-details">
                    <div class="detail-row">
                      <div class="detail-item">
                        <el-icon><Calendar /></el-icon>
                        <span>出发日期</span>
                        <strong>{{ formatDate(order.departureDate) }}</strong>
                      </div>
                      <div class="detail-item">
                        <el-icon><User /></el-icon>
                        <span>出行人数</span>
                        <strong>成人 {{ order.adultCount }} 人<span v-if="order.childCount">，儿童 {{ order.childCount }} 人</span></strong>
                      </div>
                    </div>
                    <div class="detail-row">
                      <div class="detail-item">
                        <el-icon><UserFilled /></el-icon>
                        <span>联系人</span>
                        <strong>{{ order.contactName }}</strong>
                      </div>
                      <div class="detail-item">
                        <el-icon><Phone /></el-icon>
                        <span>联系电话</span>
                        <strong>{{ order.contactPhone }}</strong>
                      </div>
                    </div>
                  </div>
                </div>

                <div class="price-section">
                  <div class="price-label">订单总额</div>
                  <div class="price-amount">¥{{ order.totalAmount }}</div>
                </div>
              </div>

              <div class="order-footer">
                <div class="payment-info" v-if="order.paymentTime">
                  <el-icon><CreditCard /></el-icon>
                  <span>支付时间: {{ formatTime(order.paymentTime) }}</span>
                </div>
                <div class="order-actions">
                  <el-button
                    v-if="order.status === 0"
                    type="primary"
                    @click="goToConfirm(order)"
                    class="action-btn pay-btn"
                  >
                    <el-icon><Edit /></el-icon>
                    填写订单信息
                  </el-button>
                  <el-button
                    v-if="order.status === 0"
                    type="danger"
                    @click="cancelOrder(order.id)"
                    class="action-btn cancel-btn"
                  >
                    <el-icon><Close /></el-icon>
                    取消订单
                  </el-button>
                  <el-button
                    @click="viewOrderDetail(order)"
                    class="action-btn detail-btn"
                  >
                    <el-icon><View /></el-icon>
                    查看详情
                  </el-button>
                </div>
              </div>
            </div>

            <!-- 分页 -->
            <div class="pagination-wrapper" v-if="total > 0">
              <el-pagination
                background
                layout="total, prev, pager, next"
                :total="total"
                :page-size="pageSize"
                :current-page="currentPage"
                @current-change="handleCurrentChange"
                class="modern-pagination"
              />
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 详情对话框 -->
    <el-dialog
      title="订单详情"
      v-model="detailDialogVisible"
      width="600px"
    >
      <div class="order-detail" v-if="currentOrder">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="订单号">{{ currentOrder.orderNo }}</el-descriptions-item>
          <el-descriptions-item label="行程名称">{{ currentOrder.tourName }}</el-descriptions-item>
          <el-descriptions-item label="套餐类型">{{ currentOrder.packageName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="批次套餐">{{ currentOrder.batchPackageName || '标准' }}</el-descriptions-item>
          <el-descriptions-item label="出发日期">{{ formatDate(currentOrder.departureDate) }}</el-descriptions-item>
          <el-descriptions-item label="成人人数">{{ currentOrder.adultCount }} 人</el-descriptions-item>
          <el-descriptions-item label="儿童人数">{{ currentOrder.childCount || 0 }} 人</el-descriptions-item>
          <el-descriptions-item label="成人单价">¥{{ currentOrder.adultUnitPrice }}</el-descriptions-item>
          <el-descriptions-item label="儿童单价">¥{{ currentOrder.childUnitPrice || 0 }}</el-descriptions-item>
          <el-descriptions-item label="行程费用">¥{{ currentOrder.tourAmount }}</el-descriptions-item>
          <el-descriptions-item label="酒店费用">
            <template v-if="currentOrder.hotelAmount && currentOrder.hotelAmount > 0">
              ¥{{ currentOrder.hotelAmount }}（{{ currentOrder.hotelName }} {{ currentOrder.hotelDays }}晚）
            </template>
            <template v-else>-</template>
          </el-descriptions-item>
          <el-descriptions-item label="订单金额">
            <span style="color: #e53e3e; font-weight: bold; font-size: 18px;">¥{{ currentOrder.totalAmount }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="联系人">{{ currentOrder.contactName }}</el-descriptions-item>
          <el-descriptions-item label="联系电话">{{ currentOrder.contactPhone }}</el-descriptions-item>
          <el-descriptions-item label="备注">{{ currentOrder.remark || '无' }}</el-descriptions-item>
          <el-descriptions-item label="订单状态">
            <el-tag :type="getStatusTagType(currentOrder.status)">
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
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getUserTourOrders, cancelTourOrder as cancelTourOrderApi } from '@/api/tourOrder'
import { Edit, Close, View, CreditCard } from '@element-plus/icons-vue'

const router = useRouter()

// 分页参数
const currentPage = ref(1)
const pageSize = ref(12)
const total = ref(0)

// 订单列表数据
const orderList = ref([])
const loading = ref(false)
const activeTab = ref('all')

// 对话框控制
const detailDialogVisible = ref(false)
const currentOrder = ref(null)

// 订单统计数据
const orderStats = ref({
  total: 0,
  pending: 0,
  paid: 0,
  cancelled: 0,
  refunded: 0,
  completed: 0
})

// 获取用户订单统计信息
const fetchOrderStats = async () => {
  try {
    // 获取全部订单用于统计
    const allRes = await getUserTourOrders({ currentPage: 1, size: 1000 })
    const allOrders = allRes?.records || []

    orderStats.value = {
      total: allRes?.total || 0,
      pending: allOrders.filter(o => o.status === 0).length,
      paid: allOrders.filter(o => o.status === 1).length,
      cancelled: allOrders.filter(o => o.status === 2).length,
      refunded: allOrders.filter(o => o.status === 3).length,
      completed: allOrders.filter(o => o.status === 4).length
    }
  } catch (error) {
    console.error('获取订单统计失败:', error)
  }
}

// 获取状态样式类
const getStatusClass = (status) => {
  const statusMap = {
    0: 'pending',
    1: 'paid',
    2: 'cancelled',
    3: 'refunded',
    4: 'completed'
  }
  return statusMap[status] || 'default'
}

// 获取状态标签类型
const getStatusTagType = (status) => {
  const typeMap = {
    0: 'warning',
    1: 'success',
    2: 'info',
    3: 'danger',
    4: 'primary'
  }
  return typeMap[status] || 'info'
}

// 跳转到行程列表
const goToTicketList = () => {
  router.push('/ticket')
}

// 获取订单列表
const fetchOrders = async () => {
  loading.value = true
  try {
    // 根据标签页筛选状态
    const status = activeTab.value === 'all' ? null : parseInt(activeTab.value)

    const res = await getUserTourOrders({
      status: status,
      currentPage: currentPage.value,
      size: pageSize.value
    })
    orderList.value = res?.records || []
    total.value = res?.total || 0
  } catch (error) {
    console.error('获取订单列表失败:', error)
    orderList.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

// 分页变化事件
const handleCurrentChange = (page) => {
  currentPage.value = page
  fetchOrders()
}

// 标签页切换事件
const handleTabClick = () => {
  currentPage.value = 1
  fetchOrders()
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

// 跳转到订单确认页面
const goToConfirm = (order) => {
  router.push('/tour-order-confirm/' + order.id)
}

// 跳转到支付页面
const goToPay = (order) => {
  router.push('/tour-order-pay/' + order.id)
}

// 取消订单
const cancelOrder = async (orderId) => {
  ElMessageBox.confirm('确定要取消该订单吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    loading.value = true
    try {
      await cancelTourOrderApi(orderId, { showDefaultMsg: false })
      ElMessage.success('订单已取消')
      fetchOrders()
      fetchOrderStats()
    } catch (error) {
      console.error('取消订单失败:', error)
    } finally {
      loading.value = false
    }
  }).catch(() => {})
}

// 查看订单详情
const viewOrderDetail = async (order) => {
  currentOrder.value = order
  detailDialogVisible.value = true
}

// 页面加载时获取订单列表
onMounted(() => {
  fetchOrderStats()
  fetchOrders()
})
</script>

<style lang="scss" scoped>
.orders-container {
  min-height: 100vh;
  background: #FFFFFF;
  font-family: "思源黑体", "Source Han Sans", "Noto Sans CJK SC", sans-serif;
  color: #333;

  .section-container {
    max-width: 1300px;
    margin: 0 auto;
    padding: 40px 20px;
  }

  .page-header-wrapper {
    max-width: 1300px;
    margin: 0 auto;
    padding: 40px 20px 0;
  }

  .page-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 0;
    padding: 0;
    border-bottom: none;
  }

  .header-content {
    flex: 1;
  }

  .page-title {
    font-size: 36px;
    font-weight: 700;
    margin: 0 0 8px;
    color: #2d3748;
    display: flex;
    align-items: center;
    gap: 12px;

    .title-icon {
      font-size: 32px;
    }
  }

  .page-subtitle {
    font-size: 16px;
    color: #64748b;
    text-align: left;
    margin: 0;
  }

  .orders-section {
    background: white;
    margin: 0;
    padding-top: 20px;
  }

  .orders-tabs {
    background: white;
    border-radius: 16px;
    box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
    overflow: hidden;
    border: 1px solid #e2e8f0;
  }

  .modern-tabs {
    :deep(.el-tabs__header) {
      margin: 0;
      background: #FFFFFF;
      border-bottom: 1px solid #e2e8f0;
    }

    :deep(.el-tabs__nav-wrap) {
      padding: 0 24px;
    }

    :deep(.el-tabs__item) {
      padding: 20px 0;
      font-size: 16px;
      font-weight: 600;
      color: #64748b;
      border: none;
      margin-right: 40px;

      &.is-active {
        color: #667eea;
      }

      &:hover {
        color: #667eea;
      }
    }

    :deep(.el-tabs__active-bar) {
      background: linear-gradient(45deg, #667eea, #764ba2);
      height: 3px;
    }

    :deep(.el-tabs__content) {
      padding: 40px 24px;
    }

    .tab-label {
      display: flex;
      align-items: center;
      gap: 8px;

      .tab-count {
        background: #667eea;
        color: white;
        padding: 2px 8px;
        border-radius: 12px;
        font-size: 12px;
        font-weight: 600;
      }
    }
  }

  .loading-state {
    padding: 40px 20px;
  }

  .empty-state {
    text-align: center;
    padding: 80px 20px;

    .empty-icon {
      font-size: 64px;
      margin-bottom: 20px;
    }

    .empty-title {
      font-size: 24px;
      font-weight: 600;
      color: #2d3748;
      margin: 0 0 8px;
    }

    .empty-desc {
      font-size: 16px;
      color: #64748b;
      margin: 0 0 24px;
    }

    .empty-action {
      background: linear-gradient(45deg, #667eea, #764ba2);
      border: none;
      border-radius: 20px;
      padding: 12px 24px;
      font-weight: 600;
    }
  }

  .orders-list {
    display: flex;
    flex-direction: column;
    gap: 20px;
  }

  .order-card {
    background: white;
    border-radius: 16px;
    box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
    border: 1px solid #e2e8f0;
    overflow: hidden;
    transition: all 0.3s ease;

    &:hover {
      transform: translateY(-4px);
      box-shadow: 0 8px 30px rgba(0, 0, 0, 0.12);
    }
  }

  .order-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 20px 24px;
    background: #FFFFFF;
    border-bottom: 1px solid #e2e8f0;

    .order-info {
      .order-no {
        display: flex;
        align-items: center;
        gap: 8px;
        font-size: 16px;
        font-weight: 600;
        color: #2d3748;
        margin-bottom: 4px;

        .el-icon {
          color: #667eea;
        }
      }

      .order-time {
        font-size: 12px;
        color: #64748b;
      }
    }

    .order-status-badge {
      padding: 6px 12px;
      border-radius: 12px;
      font-size: 12px;
      font-weight: 600;

      &.pending {
        background: linear-gradient(45deg, #f59e0b, #d97706);
        color: white;
      }

      &.paid {
        background: linear-gradient(45deg, #10b981, #059669);
        color: white;
      }

      &.cancelled {
        background: linear-gradient(45deg, #ef4444, #dc2626);
        color: white;
      }

      &.refunded {
        background: linear-gradient(45deg, #8b5cf6, #7c3aed);
        color: white;
      }

      &.completed {
        background: linear-gradient(45deg, #06b6d4, #0891b2);
        color: white;
      }
    }
  }

  .order-content {
    padding: 24px;
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    gap: 24px;
  }

  .ticket-section {
    flex: 1;

    .ticket-main {
      margin-bottom: 16px;

      .ticket-name {
        font-size: 20px;
        font-weight: 700;
        color: #2d3748;
        margin: 0 0 8px;
        line-height: 1.3;
      }

      .scenic-info {
        display: flex;
        align-items: center;
        gap: 6px;
        font-size: 14px;
        color: #64748b;

        .el-icon {
          color: #667eea;
        }
      }
    }

    .order-details {
      .detail-row {
        display: grid;
        grid-template-columns: 1fr 1fr;
        gap: 16px;
        margin-bottom: 12px;

        &:last-child {
          margin-bottom: 0;
        }
      }

      .detail-item {
        display: flex;
        align-items: center;
        gap: 8px;
        font-size: 14px;
        color: #64748b;

        .el-icon {
          color: #667eea;
          flex-shrink: 0;
        }

        strong {
          color: #2d3748;
          font-weight: 600;
          margin-left: auto;
        }
      }
    }
  }

  .price-section {
    text-align: right;
    flex-shrink: 0;

    .price-label {
      font-size: 14px;
      color: #64748b;
      margin-bottom: 4px;
    }

    .price-amount {
      font-size: 28px;
      font-weight: 700;
      color: #e53e3e;
    }
  }

  .order-footer {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 16px 24px;
    background: #FFFFFF;
    border-top: 1px solid #e2e8f0;

    .payment-info {
      display: flex;
      align-items: center;
      gap: 6px;
      font-size: 12px;
      color: #64748b;

      .el-icon {
        color: #667eea;
      }
    }

    .order-actions {
      display: flex;
      gap: 8px;

      .action-btn {
        border-radius: 8px;
        font-weight: 600;
        transition: all 0.3s ease;

        &.pay-btn {
          background: linear-gradient(45deg, #667eea, #764ba2);
          border: none;
          color: white;

          &:hover {
            transform: translateY(-1px);
            box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
          }
        }

        &.cancel-btn {
          background: #f56565;
          border: none;
          color: white;

          &:hover {
            background: #e53e3e;
            transform: translateY(-1px);
          }
        }

        &.detail-btn {
          background: white;
          border: 2px solid #e2e8f0;
          color: #64748b;

          &:hover {
            border-color: #667eea;
            color: #667eea;
            background: #FFFFFF;
          }
        }
      }
    }
  }

  .pagination-wrapper {
    display: flex;
    justify-content: center;
    margin-top: 40px;
  }

  .modern-pagination {
    :deep(.el-pagination) {
      .el-pager li {
        border-radius: 8px;
        margin: 0 4px;
        transition: all 0.3s ease;

        &:hover {
          background: #667eea;
          color: white;
        }

        &.is-active {
          background: linear-gradient(45deg, #667eea, #764ba2);
          color: white;
        }
      }

      .btn-prev,
      .btn-next {
        border-radius: 8px;
        transition: all 0.3s ease;

        &:hover {
          background: #667eea;
          color: white;
        }
      }
    }
  }
}
</style>
