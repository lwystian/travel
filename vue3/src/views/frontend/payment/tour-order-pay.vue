<template>
  <div class="tour-order-pay-container">
    <!-- 加载状态 -->
    <div v-if="loading" class="loading-container">
      <div class="loading-spinner"></div>
      <p>正在准备支付，请稍候...</p>
    </div>

    <!-- 错误状态 -->
    <div v-else-if="error" class="error-container">
      <el-result
        icon="error"
        title="加载失败"
        :sub-title="error"
      >
        <template #extra>
          <el-button type="primary" @click="goToOrders">返回订单列表</el-button>
        </template>
      </el-result>
    </div>

    <!-- 支付信息确认 -->
    <div v-else-if="order" class="payment-container">
      <div class="payment-header">
        <div class="payment-logo">
          <el-icon :size="40" color="#1677FF"><Wallet /></el-icon>
        </div>
        <p class="payment-tip">请选择支付方式并确认支付</p>
      </div>

      <!-- 订单信息 -->
      <div class="order-info-card">
        <div class="info-header">
          <span class="info-label">订单信息</span>
          <span class="order-no">{{ order.orderNo }}</span>
        </div>

        <div class="info-item">
          <span class="label">行程名称</span>
          <span class="value">{{ order.tourName }}</span>
        </div>

        <div class="info-item" v-if="order.packageName">
          <span class="label">套餐类型</span>
          <span class="value">{{ order.packageName }}</span>
        </div>

        <div class="info-item" v-if="order.departureDate">
          <span class="label">出发日期</span>
          <span class="value">{{ formatDate(order.departureDate) }}</span>
        </div>

        <div class="info-item">
          <span class="label">出行人数</span>
          <span class="value">
            成人 {{ order.adultCount }} 人
            <span v-if="order.childCount > 0">，儿童 {{ order.childCount }} 人</span>
          </span>
        </div>

        <div class="info-item" v-if="order.hotelName">
          <span class="label">住宿酒店</span>
          <span class="value">{{ order.hotelName }} ({{ order.hotelDays }}晚)</span>
        </div>

        <div class="info-item">
          <span class="label">联系人</span>
          <span class="value">{{ order.contactName }} {{ order.contactPhone }}</span>
        </div>
      </div>

      <!-- 支付方式选择 -->
      <div class="payment-methods-section">
        <div class="section-title">
          <span>选择支付方式</span>
        </div>

        <div class="payment-methods-grid">
          <div
            v-for="method in availableMethods"
            :key="method.id"
            class="payment-method-card"
            :class="{ active: selectedMethod === method.paymentType, disabled: !method.configured }"
            @click="selectMethod(method)"
          >
            <el-icon :size="32" :color="getPaymentColor(method.paymentType)">
              <component :is="getPaymentIcon(method.paymentType)" />
            </el-icon>
            <span class="method-name">{{ method.paymentName }}</span>
            <span class="method-badge" v-if="method.isSandbox">沙箱</span>
            <el-icon v-if="!method.configured" class="unconfigured-icon" color="#909399"><Lock /></el-icon>
            <div v-if="selectedMethod === method.paymentType" class="selected-check">
              <el-icon color="#fff"><Check /></el-icon>
            </div>
          </div>
        </div>

        <div v-if="availableMethods.length === 0" class="no-methods">
          <el-empty description="暂无可用的支付方式，请联系管理员配置" />
        </div>
      </div>

      <!-- 支付金额 -->
      <div class="amount-section">
        <span class="amount-label">应付金额</span>
        <span class="amount-value">¥ {{ formatPrice(order.totalAmount) }}</span>
      </div>

      <!-- 按钮区域 -->
      <div class="button-section">
        <el-button @click="cancelPayment" :disabled="paying">取消支付</el-button>
        <el-button
          type="primary"
          @click="handlePay"
          :loading="paying"
          :disabled="!selectedMethod || !selectedMethodConfig.configured"
          size="large"
        >
          <span v-if="!paying">
            {{ selectedMethod ? `使用${selectedMethodConfig.paymentName}支付` : '请选择支付方式' }}
          </span>
          <span v-else>正在跳转...</span>
        </el-button>
      </div>

      <!-- 调试模式提示 -->
      <div class="debug-mode-tip" v-if="showDebugTip">
        <el-alert
          title="提示"
          type="info"
          :closable="false"
          show-icon
        >
          <template #default>
            <p>如果无法跳转到支付页面，可点击下方按钮进行模拟支付测试</p>
            <el-button type="warning" size="small" @click="handleMockPay" :loading="paying" plain>
              模拟支付（调试用）
            </el-button>
          </template>
        </el-alert>
      </div>
    </div>

    <!-- 隐藏的支付表单 -->
    <form
      ref="payFormRef"
      name="paysubmit"
      method="post"
      style="display: none;"
    >
      <input type="hidden" name="biz_content" />
      <input type="hidden" name="charset" value="UTF-8" />
      <input type="hidden" name="sign_type" value="RSA2" />
    </form>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Wallet, Check, Lock, Aliwangwang, Wechat } from '@element-plus/icons-vue'
import { getTourOrderDetail } from '@/api/tourOrder'
import { generatePayForm as generatePayFormApi, mockPay as mockPayApi, getOrderByOrderNo, getAvailablePaymentMethods } from '@/api/tourOrderPay'

const route = useRoute()
const router = useRouter()

// 状态
const loading = ref(true)
const paying = ref(false)
const order = ref(null)
const error = ref(null)
const payFormRef = ref(null)
const availableMethods = ref([])
const selectedMethod = ref('alipay')

// 是否显示调试提示
const showDebugTip = computed(() => {
  return route.query.debug === 'true'
})

// 选中的支付方式配置
const selectedMethodConfig = computed(() => {
  return availableMethods.value.find(m => m.paymentType === selectedMethod.value) || {
    paymentName: '未知',
    configured: false
  }
})

// 获取支付方式图标
const getPaymentIcon = (type) => {
  const icons = {
    alipay: Aliwangwang,
    wechat: Wechat
  }
  return icons[type] || Wallet
}

// 获取支付方式颜色
const getPaymentColor = (type) => {
  const colors = {
    alipay: '#1677FF',
    wechat: '#07C160'
  }
  return colors[type] || '#666'
}

// 选择支付方式
const selectMethod = (method) => {
  if (!method.configured) {
    ElMessage.warning(`${method.paymentName} 未配置，请选择其他支付方式`)
    return
  }
  selectedMethod.value = method.paymentType
}

// 格式化日期
const formatDate = (dateStr) => {
  if (!dateStr) return ''
  return dateStr
}

// 格式化价格
const formatPrice = (price) => {
  if (!price) return '0.00'
  return Number(price).toFixed(2)
}

// 加载可用的支付方式
const loadAvailableMethods = async () => {
  try {
    const res = await getAvailablePaymentMethods()
    // 响应拦截器已返回 res.data，直接使用
    availableMethods.value = Array.isArray(res) ? res : (res.data || [])

    // 默认选择第一个已配置的支付方式
    const configuredMethod = availableMethods.value.find(m => m.configured)
    if (configuredMethod) {
      selectedMethod.value = configuredMethod.paymentType
    } else if (availableMethods.value.length > 0) {
      selectedMethod.value = availableMethods.value[0].paymentType
    }
  } catch (err) {
    console.error('加载支付方式失败:', err)
  }
}

// 获取订单信息
const fetchOrderInfo = async () => {
  try {
    loading.value = true
    error.value = null

    const orderId = route.params.id
    const orderNo = route.query.orderNo

    if (orderId) {
      const data = await getTourOrderDetail(orderId)
      order.value = data
    } else if (orderNo) {
      const data = await getOrderByOrderNo(orderNo)
      order.value = data
    } else {
      error.value = '订单信息不存在'
      return
    }

    // 检查订单状态
    if (order.value && order.value.status !== 0) {
      if (order.value.status === 1) {
        ElMessage.warning('该订单已支付')
        router.replace(`/payment/result?out_trade_no=${order.value.orderNo}&status=success`)
      } else {
        ElMessage.warning('该订单无法支付')
        router.replace('/orders')
      }
    }
  } catch (err) {
    console.error('获取订单信息失败:', err)
    error.value = err.message || '获取订单信息失败'
  } finally {
    loading.value = false
  }
}

// 处理支付
const handlePay = async () => {
  if (!order.value) {
    ElMessage.error('订单信息不存在')
    return
  }

  if (!selectedMethod.value) {
    ElMessage.error('请选择支付方式')
    return
  }

  try {
    paying.value = true
    ElMessage.info('正在准备支付...')

    // 调用后端生成支付表单
    const formHtml = await generatePayFormApi(order.value.id, selectedMethod.value)

    // 创建临时容器
    const container = document.createElement('div')
    container.innerHTML = formHtml
    container.style.display = 'none'
    document.body.appendChild(container)

    // 获取表单并自动提交
    const form = container.querySelector('form')
    if (form) {
      form.target = '_blank'
      form.method = 'post'
      form.acceptCharset = 'UTF-8'
      form.submit()
    } else {
      throw new Error('支付表单生成失败')
    }

    // 清理临时容器
    setTimeout(() => {
      document.body.removeChild(container)
    }, 100)

  } catch (err) {
    console.error('准备支付失败:', err)
    ElMessage.error(err.message || '准备支付失败，请重试')
    paying.value = false
  }
}

// 模拟支付（测试用）
const handleMockPay = async () => {
  if (!order.value) {
    ElMessage.error('订单信息不存在')
    return
  }

  try {
    await ElMessageBox.confirm(
      `确定要模拟支付订单 ${order.value.orderNo} 吗？\n金额：¥${formatPrice(order.value.totalAmount)}`,
      '模拟支付',
      {
        confirmButtonText: '确定模拟支付',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    paying.value = true
    await mockPayApi(order.value.id, { showDefaultMsg: false })

    ElMessage.success('模拟支付成功')
    router.replace(`/payment/result?out_trade_no=${order.value.orderNo}&status=success`)

  } catch (err) {
    if (err !== 'cancel') {
      console.error('模拟支付失败:', err)
      ElMessage.error(err.message || '模拟支付失败')
    }
  } finally {
    paying.value = false
  }
}

// 取消支付
const cancelPayment = () => {
  ElMessageBox.confirm('确定要取消支付吗？订单将保留但不会自动完成支付。', '提示', {
    confirmButtonText: '确定取消',
    cancelButtonText: '继续支付',
    type: 'warning'
  }).then(() => {
    router.push('/orders')
  }).catch(() => {
    // 用户选择继续支付
  })
}

// 返回订单列表
const goToOrders = () => {
  router.push('/orders')
}

onMounted(async () => {
  await Promise.all([
    fetchOrderInfo(),
    loadAvailableMethods()
  ])
})
</script>

<style scoped>
.tour-order-pay-container {
  width: 100%;
  min-height: calc(100vh - 200px);
  background: linear-gradient(135deg, #f5f7fa 0%, #e4e8ed 100%);
  padding: 40px 20px;
}

.loading-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 400px;
}

.loading-spinner {
  width: 50px;
  height: 50px;
  border: 4px solid #f3f3f3;
  border-top: 4px solid #409EFF;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-bottom: 20px;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.loading-container p {
  color: #666;
  font-size: 16px;
}

.error-container {
  max-width: 600px;
  margin: 0 auto;
  background: white;
  border-radius: 12px;
  padding: 40px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
}

.payment-container {
  max-width: 700px;
  margin: 0 auto;
  background: white;
  border-radius: 16px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
  overflow: hidden;
}

.payment-header {
  background: linear-gradient(135deg, #1677ff 0%, #0958d9 100%);
  color: white;
  padding: 30px;
  text-align: center;
}

.payment-logo {
  margin-bottom: 15px;
}

.payment-header .payment-tip {
  font-size: 14px;
  opacity: 0.9;
  margin: 0;
}

.order-info-card {
  padding: 20px 30px;
  border-bottom: 1px solid #f0f0f0;
}

.info-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
  padding-bottom: 10px;
  border-bottom: 1px dashed #e8e8e8;
}

.info-label {
  font-weight: 600;
  color: #333;
  font-size: 16px;
}

.order-no {
  color: #999;
  font-size: 13px;
  font-family: monospace;
}

.info-item {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  padding: 10px 0;
}

.info-item .label {
  color: #666;
  font-size: 14px;
  flex-shrink: 0;
}

.info-item .value {
  color: #333;
  font-size: 14px;
  text-align: right;
  max-width: 60%;
}

.payment-methods-section {
  padding: 20px 30px;
  border-bottom: 1px solid #f0f0f0;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: #333;
  margin-bottom: 15px;
}

.payment-methods-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(150px, 1fr));
  gap: 15px;
}

.payment-method-card {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 20px 15px;
  border: 2px solid #e8e8e8;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.3s ease;
  background: #fff;
}

.payment-method-card:hover:not(.disabled) {
  border-color: #409EFF;
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.15);
}

.payment-method-card.active {
  border-color: #1677FF;
  background: linear-gradient(135deg, rgba(22, 119, 255, 0.05) 0%, rgba(9, 88, 217, 0.08) 100%);
}

.payment-method-card.disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.method-name {
  margin-top: 10px;
  font-size: 14px;
  font-weight: 500;
  color: #333;
}

.method-badge {
  position: absolute;
  top: 8px;
  right: 8px;
  font-size: 10px;
  padding: 2px 6px;
  background: #ff9800;
  color: white;
  border-radius: 4px;
}

.unconfigured-icon {
  position: absolute;
  top: 8px;
  left: 8px;
}

.selected-check {
  position: absolute;
  bottom: -1px;
  right: -1px;
  width: 24px;
  height: 24px;
  background: #1677FF;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.no-methods {
  padding: 20px;
}

.amount-section {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 25px 30px;
  background: #fffbf0;
  border-bottom: 1px solid #f0f0f0;
}

.amount-label {
  font-size: 16px;
  color: #666;
}

.amount-value {
  font-size: 32px;
  font-weight: bold;
  color: #ff4d4f;
}

.button-section {
  display: flex;
  gap: 15px;
  padding: 25px 30px;
}

.button-section .el-button {
  flex: 1;
  height: 48px;
  font-size: 16px;
}

.button-section .el-button--primary {
  background: linear-gradient(135deg, #1677ff 0%, #0958d9 100%);
  border: none;
}

.button-section .el-button--primary:hover:not(:disabled) {
  background: linear-gradient(135deg, #409eff 0%, #1677ff 100%);
}

.button-section .el-button:disabled {
  background: #f5f5f5;
  border-color: #d9d9d9;
  color: #bfbfbf;
}

.debug-mode-tip {
  padding: 0 30px 30px;
}

.debug-mode-tip .el-alert {
  border-radius: 8px;
}

.debug-mode-tip p {
  margin: 0 0 10px 0;
  color: #666;
}
</style>
