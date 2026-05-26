<template>
  <main class="orders-page">
    <section class="orders-shell">
      <header class="orders-header">
        <div>
          <span class="kicker">Order Center</span>
          <h1>我的订单</h1>
          <p>查看行程订单、支付状态、联系人与出行信息。</p>
        </div>
        <div class="header-actions">
          <el-button class="light-btn" @click="resetOrders">
            <el-icon><Refresh /></el-icon>
            刷新
          </el-button>
          <el-button type="primary" class="primary-btn" @click="goToTicketList">
            <el-icon><Ticket /></el-icon>
            预订行程
          </el-button>
        </div>
      </header>

      <section class="stats-row">
        <div class="stat-card dark">
          <span>全部订单</span>
          <strong>{{ orderStats.total || 0 }}</strong>
        </div>
        <div class="stat-card">
          <span>待支付</span>
          <strong>{{ orderStats.pending || 0 }}</strong>
        </div>
        <div class="stat-card">
          <span>已支付</span>
          <strong>{{ orderStats.paid || 0 }}</strong>
        </div>
        <div class="stat-card">
          <span>已完成</span>
          <strong>{{ orderStats.completed || 0 }}</strong>
        </div>
      </section>

      <section class="orders-layout">
        <aside class="status-nav">
          <button
            v-for="item in statusFilters"
            :key="item.name"
            type="button"
            class="status-item"
            :class="{ active: activeTab === item.name }"
            @click="selectStatus(item.name)"
          >
            <el-icon><component :is="item.icon" /></el-icon>
            <span>{{ item.label }}</span>
            <strong>{{ item.count }}</strong>
          </button>
        </aside>

        <section class="orders-panel">
          <div class="panel-title">
            <div>
              <span>当前视图</span>
              <strong>{{ currentFilterLabel }}</strong>
            </div>
            <p>每次显示约 4 条订单高度，继续下滑自动加载更多。</p>
          </div>

          <div v-if="loading" class="loading-box">
            <el-skeleton :rows="8" animated />
          </div>

          <div v-else-if="orderList.length === 0" class="empty-box">
            <el-icon><Document /></el-icon>
            <h2>暂无匹配订单</h2>
            <p>当前筛选条件下还没有订单，可以切换状态或去预订新的行程。</p>
            <el-button type="primary" @click="goToTicketList">
              <el-icon><Ticket /></el-icon>
              去预订行程
            </el-button>
          </div>

          <div v-else ref="orderScrollRef" class="orders-scroll">
            <article
              v-for="order in orderList"
              :key="order.id"
              class="order-card"
              :class="getStatusClass(order.status)"
            >
              <header class="card-head">
                <div class="order-title">
                  <span class="order-no">{{ order.orderNo || '-' }}</span>
                  <h2>{{ order.tourName || '未命名行程' }}</h2>
                </div>
                <span class="status-pill" :class="getStatusClass(order.status)">
                  {{ getOrderStatusText(order.status) }}
                </span>
              </header>

              <div class="info-grid">
                <div class="info-cell">
                  <span>出发日期</span>
                  <strong>{{ formatDate(order.departureDate) }}</strong>
                </div>
                <div class="info-cell">
                  <span>套餐</span>
                  <strong>{{ order.packageName || '标准套餐' }}</strong>
                </div>
                <div class="info-cell">
                  <span>出行人数</span>
                  <strong>成人 {{ order.adultCount || 0 }} 人<span v-if="order.childCount">，儿童 {{ order.childCount }} 人</span></strong>
                </div>
                <div class="info-cell">
                  <span>联系人</span>
                  <strong>{{ order.contactName || '-' }}</strong>
                </div>
                <div class="info-cell">
                  <span>联系电话</span>
                  <strong>{{ maskPhone(order.contactPhone, '-') }}</strong>
                </div>
                <div class="info-cell">
                  <span>{{ order.paymentTime ? '支付时间' : '下单时间' }}</span>
                  <strong>{{ formatTime(order.paymentTime || order.createTime) }}</strong>
                </div>
              </div>

              <footer class="card-foot">
                <div class="amount-box">
                  <span>订单金额</span>
                  <strong>¥{{ formatPrice(order.totalAmount) }}</strong>
                </div>
                <div class="pay-state" :class="{ waiting: !order.paymentTime }">
                  <el-icon><CreditCard /></el-icon>
                  <span>{{ order.paymentTime ? '付款已确认' : '等待付款确认' }}</span>
                </div>
                <div class="action-box">
                  <el-button
                    v-if="order.status === 0"
                    type="primary"
                    @click="goToConfirm(order)"
                  >
                    <el-icon><Edit /></el-icon>
                    完善订单
                  </el-button>
                  <el-button
                    v-if="order.status === 0"
                    plain
                    type="danger"
                    @click="cancelOrder(order.id)"
                  >
                    <el-icon><Close /></el-icon>
                    取消订单
                  </el-button>
                  <el-button plain @click="viewOrderDetail(order)">
                    <el-icon><View /></el-icon>
                    查看详情
                  </el-button>
                </div>
              </footer>
            </article>

            <div ref="loadMoreTrigger" class="load-more">
              <template v-if="loadingMore">
                <el-icon class="is-loading"><Loading /></el-icon>
                <span>正在加载更多订单</span>
              </template>
              <template v-else-if="hasMoreOrders">
                <span>下滑加载更多订单</span>
              </template>
              <template v-else>
                <span>已展示全部 {{ total }} 条订单</span>
              </template>
            </div>
          </div>
        </section>
      </section>
    </section>

    <el-dialog
      v-model="detailDialogVisible"
      title="订单详情"
      width="780px"
      class="order-detail-dialog"
    >
      <div v-if="currentOrder" class="detail-view">
        <section class="detail-head">
          <div>
            <span>{{ currentOrder.orderNo || '-' }}</span>
            <h2>{{ currentOrder.tourName || '未命名行程' }}</h2>
            <p>{{ currentOrder.packageName || '标准套餐' }} · {{ formatDate(currentOrder.departureDate) }}</p>
          </div>
          <div class="detail-price">
            <span>订单金额</span>
            <strong>¥{{ formatPrice(currentOrder.totalAmount) }}</strong>
          </div>
        </section>

        <section class="detail-grid">
          <div><span>订单状态</span><strong>{{ getOrderStatusText(currentOrder.status) }}</strong></div>
          <div><span>出行人数</span><strong>成人 {{ currentOrder.adultCount || 0 }} 人，儿童 {{ currentOrder.childCount || 0 }} 人</strong></div>
          <div><span>联系人</span><strong>{{ currentOrder.contactName || '-' }}</strong></div>
          <div><span>联系电话</span><strong>{{ maskPhone(currentOrder.contactPhone, '-') }}</strong></div>
          <div><span>下单时间</span><strong>{{ formatTime(currentOrder.createTime) }}</strong></div>
          <div><span>支付时间</span><strong>{{ currentOrder.paymentTime ? formatTime(currentOrder.paymentTime) : '-' }}</strong></div>
          <div><span>支付方式</span><strong>{{ currentOrder.paymentMethod || '-' }}</strong></div>
          <div class="wide"><span>备注</span><strong>{{ currentOrder.remark || '无' }}</strong></div>
        </section>
      </div>
    </el-dialog>
  </main>
</template>

<script setup>
import { computed, ref, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Check,
  CircleCheck,
  Clock,
  Close,
  CreditCard,
  Document,
  Edit,
  List,
  Loading,
  Refresh,
  RefreshLeft,
  Ticket,
  View
} from '@element-plus/icons-vue'
import { getUserTourOrders, cancelTourOrder as cancelTourOrderApi } from '@/api/tourOrder'
import { maskPhone } from '@/utils/mask'

const router = useRouter()

const currentPage = ref(1)
const pageSize = ref(4)
const total = ref(0)
const orderList = ref([])
const loading = ref(false)
const loadingMore = ref(false)
const activeTab = ref('all')
const detailDialogVisible = ref(false)
const currentOrder = ref(null)
const orderScrollRef = ref(null)
const loadMoreTrigger = ref(null)
let loadMoreObserver = null

const orderStats = ref({
  total: 0,
  pending: 0,
  paid: 0,
  cancelled: 0,
  refunded: 0,
  completed: 0
})

const statusFilters = computed(() => [
  { name: 'all', label: '全部订单', count: orderStats.value.total || 0, icon: List },
  { name: '0', label: '待支付', count: orderStats.value.pending || 0, icon: Clock },
  { name: '1', label: '已支付', count: orderStats.value.paid || 0, icon: Check },
  { name: '2', label: '已取消', count: orderStats.value.cancelled || 0, icon: Close },
  { name: '3', label: '已退款', count: orderStats.value.refunded || 0, icon: RefreshLeft },
  { name: '4', label: '已完成', count: orderStats.value.completed || 0, icon: CircleCheck }
])

const currentFilterLabel = computed(() => {
  return statusFilters.value.find(item => item.name === activeTab.value)?.label || '全部订单'
})

const hasMoreOrders = computed(() => orderList.value.length < total.value)

const fetchOrderStats = async () => {
  try {
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

const getStatusClass = (status) => {
  const statusMap = {
    0: 'pending',
    1: 'paid',
    2: 'cancelled',
    3: 'refunded',
    4: 'completed'
  }
  return statusMap[status] || 'unknown'
}

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

const goToTicketList = () => {
  router.push('/tickets')
}

const fetchOrders = async ({ append = false } = {}) => {
  if (append) {
    if (loading.value || loadingMore.value || !hasMoreOrders.value) return
    loadingMore.value = true
  } else {
    loading.value = true
  }

  try {
    const status = activeTab.value === 'all' ? null : parseInt(activeTab.value)
    const res = await getUserTourOrders({
      status,
      currentPage: currentPage.value,
      size: pageSize.value
    })
    const records = res?.records || []
    orderList.value = append ? [...orderList.value, ...records] : records
    total.value = res?.total || 0
  } catch (error) {
    console.error('获取订单列表失败:', error)
    if (!append) {
      orderList.value = []
      total.value = 0
    }
  } finally {
    loading.value = false
    loadingMore.value = false
  }
}

const setupLoadMoreObserver = () => {
  if (loadMoreObserver) {
    loadMoreObserver.disconnect()
    loadMoreObserver = null
  }
  if (!orderScrollRef.value || !loadMoreTrigger.value) return

  loadMoreObserver = new IntersectionObserver((entries) => {
    if (entries[0]?.isIntersecting) {
      loadMoreOrders()
    }
  }, {
    root: orderScrollRef.value,
    rootMargin: '32px 0px',
    threshold: 0.1
  })

  loadMoreObserver.observe(loadMoreTrigger.value)
}

const loadMoreOrders = () => {
  if (!hasMoreOrders.value || loading.value || loadingMore.value) return
  currentPage.value += 1
  fetchOrders({ append: true })
}

const resetOrders = () => {
  currentPage.value = 1
  fetchOrders().then(() => {
    if (orderScrollRef.value) orderScrollRef.value.scrollTop = 0
    nextTick(setupLoadMoreObserver)
  })
}

const selectStatus = (name) => {
  if (activeTab.value === name) return
  activeTab.value = name
  resetOrders()
}

const formatDate = (dateStr) => {
  if (!dateStr) return '-'
  const date = new Date(dateStr)
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
}

const formatTime = (timeStr) => {
  if (!timeStr) return '-'
  const date = new Date(timeStr)
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')} ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
}

const formatPrice = (price) => {
  return Number(price || 0).toFixed(2)
}

const goToConfirm = (order) => {
  router.push('/tour-order-confirm/' + order.id)
}

const cancelOrder = async (orderId) => {
  ElMessageBox.confirm('确定要取消该订单吗？取消后将无法继续支付。', '取消订单', {
    confirmButtonText: '确认取消',
    cancelButtonText: '再想想',
    type: 'warning'
  }).then(async () => {
    loading.value = true
    try {
      await cancelTourOrderApi(orderId, { showDefaultMsg: false })
      ElMessage.success('订单已取消')
      resetOrders()
      fetchOrderStats()
    } catch (error) {
      console.error('取消订单失败:', error)
    } finally {
      loading.value = false
    }
  }).catch(() => {})
}

const viewOrderDetail = (order) => {
  currentOrder.value = order
  detailDialogVisible.value = true
}

onMounted(() => {
  fetchOrderStats()
  fetchOrders().then(() => nextTick(setupLoadMoreObserver))
})

onUnmounted(() => {
  if (loadMoreObserver) {
    loadMoreObserver.disconnect()
    loadMoreObserver = null
  }
})
</script>

<style lang="scss" scoped>
.orders-page {
  min-height: 100vh;
  background: #f3f6fb;
  color: #101828;
  font-family: "Source Han Sans", "Noto Sans CJK SC", "Microsoft YaHei", sans-serif;
}

.orders-shell {
  width: min(var(--frontend-container-safe-width), var(--frontend-container-page));
  margin: 0 auto;
  padding: 36px 0 64px;
}

.orders-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 24px;
  padding: 26px 28px;
  border: 1px solid #dde5f0;
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 18px 44px rgba(16, 24, 40, 0.06);
  text-align: left;
}

.orders-header > div:first-child {
  min-width: 0;
  text-align: left;
}

.kicker {
  display: inline-flex;
  height: 24px;
  align-items: center;
  padding: 0 10px;
  border-radius: 999px;
  background: #e8f2ff;
  color: #155eef;
  font-size: 12px;
  font-weight: 800;
}

.orders-header h1 {
  margin: 12px 0 8px;
  font-size: 34px;
  font-weight: 900;
  letter-spacing: 0;
}

.orders-header p {
  margin: 0;
  color: #667085;
  font-size: 15px;
}

.header-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 10px;
}

.light-btn,
.primary-btn {
  height: 42px;
  border-radius: 8px;
  font-weight: 800;
}

.light-btn {
  background: #fff;
  border-color: #d6deeb;
  color: #344054;
}

.primary-btn {
  background: #155eef;
  border-color: #155eef;
}

.stats-row {
  display: grid;
  grid-template-columns: 1.25fr repeat(3, 1fr);
  gap: 12px;
  margin: 16px 0;
}

.stat-card {
  min-height: 102px;
  padding: 18px;
  border: 1px solid #dde5f0;
  border-radius: 8px;
  background: #fff;
}

.stat-card span {
  display: block;
  color: #667085;
  font-size: 13px;
  font-weight: 800;
}

.stat-card strong {
  display: block;
  margin-top: 10px;
  font-size: 32px;
  line-height: 1;
  font-weight: 900;
}

.stat-card.dark {
  background: #101828;
  border-color: #101828;
  color: #fff;
}

.stat-card.dark span {
  color: #fff;
}

.orders-layout {
  display: grid;
  grid-template-columns: 248px minmax(0, 1fr);
  gap: 16px;
}

.status-nav {
  position: sticky;
  top: 18px;
  align-self: start;
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 10px;
  border: 1px solid #dde5f0;
  border-radius: 8px;
  background: #fff;
}

.status-item {
  display: grid;
  grid-template-columns: 28px minmax(0, 1fr) auto;
  align-items: center;
  gap: 10px;
  min-height: 48px;
  padding: 0 12px;
  border: 1px solid transparent;
  border-radius: 8px;
  background: transparent;
  color: #344054;
  text-align: left;
  cursor: pointer;
}

.status-item.active {
  background: #edf5ff;
  border-color: #b9d6ff;
  color: #155eef;
}

.status-item span {
  overflow: hidden;
  font-size: 14px;
  font-weight: 900;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.status-item strong {
  min-width: 26px;
  height: 22px;
  border-radius: 999px;
  background: #eef2f7;
  color: #344054;
  font-size: 12px;
  line-height: 22px;
  text-align: center;
}

.orders-panel {
  min-width: 0;
  border: 1px solid #dde5f0;
  border-radius: 8px;
  background: #fff;
  overflow: hidden;
}

.panel-title {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18px;
  padding: 18px 20px;
  border-bottom: 1px solid #e4eaf3;
  background: #fbfcfe;
  text-align: left;
}

.panel-title > div {
  text-align: left;
}

.panel-title span {
  display: block;
  margin-bottom: 4px;
  color: #98a2b3;
  font-size: 12px;
  font-weight: 800;
}

.panel-title strong {
  font-size: 18px;
  font-weight: 900;
}

.panel-title p {
  margin: 0;
  color: #667085;
  font-size: 13px;
  font-weight: 700;
  text-align: left;
}

.loading-box {
  padding: 22px;
}

.empty-box {
  display: flex;
  min-height: 420px;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 48px 24px;
  text-align: center;
}

.empty-box > .el-icon {
  display: grid;
  width: 76px;
  height: 76px;
  place-items: center;
  margin-bottom: 18px;
  border: 1px solid #dbeafe;
  border-radius: 8px;
  background: #eff6ff;
  color: #155eef;
  font-size: 34px;
}

.empty-box h2 {
  margin: 0 0 8px;
  font-size: 22px;
  font-weight: 900;
}

.empty-box p {
  max-width: 460px;
  margin: 0 0 22px;
  color: #667085;
  line-height: 1.7;
}

.orders-scroll {
  display: flex;
  max-height: 928px;
  flex-direction: column;
  gap: 12px;
  padding: 14px;
  overflow-y: auto;
  overscroll-behavior: contain;
  scrollbar-gutter: stable;
  scrollbar-width: thin;
  scrollbar-color: #a8b4c5 transparent;
}

.orders-scroll::-webkit-scrollbar {
  width: 6px;
}

.orders-scroll::-webkit-scrollbar-track {
  background: transparent;
}

.orders-scroll::-webkit-scrollbar-thumb {
  border-radius: 999px;
  background: #a8b4c5;
}

.order-card {
  position: relative;
  display: grid;
  min-height: 218px;
  grid-template-rows: auto auto auto;
  gap: 12px;
  padding: 16px;
  border: 1px solid #e4eaf3;
  border-left-width: 4px;
  border-radius: 8px;
  background: #fff;
}

.order-card.pending {
  border-left-color: #f59e0b;
}

.order-card.paid {
  border-left-color: #12b76a;
}

.order-card.cancelled {
  border-left-color: #98a2b3;
}

.order-card.refunded {
  border-left-color: #f04438;
}

.order-card.completed {
  border-left-color: #155eef;
}

.card-head {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 14px;
  align-items: start;
}

.order-no {
  display: block;
  margin-bottom: 6px;
  color: #667085;
  font-size: 12px;
  font-weight: 800;
}

.order-title h2 {
  display: -webkit-box;
  margin: 0;
  overflow: hidden;
  color: #101828;
  font-size: 18px;
  font-weight: 900;
  line-height: 1.38;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.status-pill {
  height: 28px;
  padding: 0 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 900;
  line-height: 28px;
  white-space: nowrap;
}

.status-pill.pending {
  background: #fff7ed;
  color: #b54708;
}

.status-pill.paid {
  background: #ecfdf3;
  color: #027a48;
}

.status-pill.cancelled {
  background: #f2f4f7;
  color: #667085;
}

.status-pill.refunded {
  background: #fef3f2;
  color: #b42318;
}

.status-pill.completed {
  background: #eff4ff;
  color: #155eef;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px 10px;
}

.info-cell {
  min-width: 0;
  min-height: 58px;
  padding: 8px 10px;
  border: 1px solid #eef2f7;
  border-radius: 8px;
  background: #fbfcfe;
  align-content: start;
}

.info-cell > span {
  display: block;
  margin-bottom: 4px;
  color: #98a2b3;
  font-size: 11px;
  font-weight: 800;
  line-height: 1.2;
}

.info-cell strong {
  display: block;
  overflow: hidden;
  color: #344054;
  font-size: 12px;
  font-weight: 900;
  line-height: 1.35;
  text-overflow: ellipsis;
  white-space: nowrap;
  word-break: break-word;
}

.info-cell:nth-child(2) strong,
.info-cell:nth-child(6) strong {
  display: -webkit-box;
  white-space: normal;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.info-cell:nth-child(3) strong {
  white-space: nowrap;
}

.card-foot {
  display: grid;
  grid-template-columns: auto minmax(130px, 1fr) auto;
  align-items: center;
  gap: 10px;
  padding-top: 12px;
  border-top: 1px solid #eef2f7;
}

.amount-box span {
  display: block;
  margin-bottom: 4px;
  color: #667085;
  font-size: 12px;
  font-weight: 800;
}

.amount-box strong {
  color: #d92d20;
  font-size: 24px;
  font-weight: 900;
  white-space: nowrap;
}

.pay-state {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: #027a48;
  font-size: 12px;
  font-weight: 900;
}

.pay-state.waiting {
  color: #b54708;
}

.action-box {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;
}

.action-box :deep(.el-button) {
  margin-left: 0;
  border-radius: 8px;
  font-weight: 800;
}

.load-more {
  display: flex;
  min-height: 48px;
  align-items: center;
  justify-content: center;
  gap: 8px;
  border: 1px dashed #d6deeb;
  border-radius: 8px;
  background: #fbfcfe;
  color: #667085;
  font-size: 13px;
  font-weight: 800;
}

.detail-head {
  display: flex;
  justify-content: space-between;
  gap: 18px;
  padding: 20px;
  border: 1px solid #dde5f0;
  border-radius: 8px;
  background: #f8fbff;
}

.detail-head span,
.detail-price span,
.detail-grid span {
  display: block;
  color: #667085;
  font-size: 12px;
  font-weight: 900;
}

.detail-head h2 {
  margin: 8px 0;
  font-size: 20px;
  font-weight: 900;
}

.detail-head p {
  margin: 0;
  color: #667085;
}

.detail-price {
  flex-shrink: 0;
  text-align: right;
}

.detail-price strong {
  display: block;
  margin-top: 8px;
  color: #d92d20;
  font-size: 28px;
  font-weight: 900;
}

.detail-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin-top: 14px;
}

.detail-grid div {
  min-height: 72px;
  padding: 14px;
  border: 1px solid #eef2f7;
  border-radius: 8px;
}

.detail-grid .wide {
  grid-column: span 2;
}

.detail-grid strong {
  display: block;
  margin-top: 8px;
  color: #344054;
  font-size: 14px;
  font-weight: 900;
  word-break: break-word;
}

@media (max-width: 980px) {
  .orders-shell {
    width: min(var(--frontend-container-safe-width), var(--frontend-container-page));
    padding-top: 24px;
  }

  .orders-header,
  .panel-title {
    align-items: flex-start;
    flex-direction: column;
  }

  .panel-title p {
    text-align: left;
  }

  .stats-row {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .orders-layout {
    grid-template-columns: 1fr;
  }

  .status-nav {
    position: static;
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .info-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .info-cell:nth-child(n) {
    grid-column: auto;
  }

  .card-foot {
    grid-template-columns: 1fr;
    align-items: start;
  }

  .action-box {
    justify-content: flex-start;
  }
}

@media (max-width: 640px) {
  .orders-header h1 {
    font-size: 28px;
  }

  .stats-row,
  .status-nav,
  .info-grid,
  .detail-grid {
    grid-template-columns: 1fr;
  }

  .card-head,
  .detail-head {
    grid-template-columns: 1fr;
    flex-direction: column;
  }

  .status-pill {
    justify-self: start;
  }

  .orders-scroll {
    max-height: 900px;
  }

  .detail-price {
    text-align: left;
  }

  .detail-grid .wide {
    grid-column: auto;
  }
}
</style>
