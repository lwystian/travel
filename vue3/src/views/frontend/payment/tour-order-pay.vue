<template>
  <main class="pay-page">
    <section v-if="loading" class="state-panel">
      <el-skeleton :rows="9" animated />
      <p>正在准备支付，请稍候...</p>
    </section>

    <section v-else-if="error" class="state-panel">
      <el-result icon="error" title="加载失败" :sub-title="error">
        <template #extra>
          <el-button type="primary" @click="goToOrders">返回订单列表</el-button>
        </template>
      </el-result>
    </section>

    <section v-else-if="order" class="pay-shell">
      <header class="pay-hero">
        <div>
          <span class="eyebrow">Secure Payment</span>
          <h1>选择支付方式</h1>
          <p>请确认订单金额与支付方式，系统将通过当前窗口跳转至支付渠道。</p>
        </div>
        <div class="step-strip">
          <span>1 确认信息</span>
          <span class="active">2 选择支付</span>
          <span>3 完成预订</span>
        </div>
      </header>

      <div class="pay-layout">
        <section class="pay-main">
          <article class="panel-card">
            <header class="panel-head">
              <div>
                <span>Order</span>
                <strong>订单信息</strong>
              </div>
              <code>{{ order.orderNo }}</code>
            </header>

            <div class="order-grid">
              <div>
                <span>行程名称</span>
                <strong>{{ order.tourName || '-' }}</strong>
              </div>
              <div>
                <span>套餐类型</span>
                <strong>{{ order.packageName || '标准套餐' }}</strong>
              </div>
              <div>
                <span>出发日期</span>
                <strong>{{ formatDate(order.departureDate) || '-' }}</strong>
              </div>
              <div>
                <span>出行人数</span>
                <strong>成人 {{ order.adultCount || 0 }} 人<span v-if="order.childCount">，儿童 {{ order.childCount }} 人</span></strong>
              </div>
              <div v-if="order.hotelName">
                <span>住宿酒店</span>
                <strong>{{ order.hotelName }}（{{ order.hotelDays || 0 }} 晚）</strong>
              </div>
              <div>
                <span>联系人</span>
                <strong>{{ order.contactName || '-' }} {{ maskPhone(order.contactPhone, '-') }}</strong>
              </div>
            </div>
          </article>

          <article class="panel-card">
            <header class="panel-head">
              <div>
                <span>Payment Method</span>
                <strong>支付方式</strong>
              </div>
            </header>

            <div v-if="availableMethods.length > 0" class="method-grid">
              <button
                v-for="method in availableMethods"
                :key="method.id || method.paymentType"
                type="button"
                class="method-card"
                :class="{ active: selectedMethod === method.paymentType, disabled: !method.configured }"
                @click="selectMethod(method)"
              >
                <span class="method-icon" :style="{ color: getPaymentColor(method.paymentType) }">
                  <el-icon><component :is="getPaymentIcon(method.paymentType)" /></el-icon>
                </span>
                <span class="method-copy">
                  <strong>{{ method.paymentName }}</strong>
                  <small>{{ method.configured ? '可用于当前订单支付' : '后台未完成配置' }}</small>
                </span>
                <span v-if="method.isSandbox" class="sandbox-chip">沙箱</span>
                <el-icon v-if="selectedMethod === method.paymentType" class="check-icon"><Check /></el-icon>
                <el-icon v-else-if="!method.configured" class="lock-icon"><Lock /></el-icon>
              </button>
            </div>

            <el-empty v-else description="暂无可用的支付方式，请联系管理员配置" />
          </article>

        </section>

        <aside class="amount-panel">
          <div class="amount-card">
            <header>
              <span>Payable</span>
              <strong>支付确认</strong>
            </header>
            <div class="amount-value">
              <span>应付金额</span>
              <strong>¥{{ formatPrice(order.totalAmount) }}</strong>
            </div>
            <div class="selected-method">
              <span>当前支付方式</span>
              <strong>{{ selectedMethod ? selectedMethodConfig.paymentName : '未选择' }}</strong>
            </div>
            <div class="pay-actions">
              <el-button @click="cancelPayment" :disabled="paying">取消支付</el-button>
              <el-button
                type="primary"
                @click="handlePay"
                :loading="paying"
                :disabled="!selectedMethod || !selectedMethodConfig.configured"
              >
                {{ selectedMethod ? `使用${selectedMethodConfig.paymentName}支付` : '请选择支付方式' }}
              </el-button>
            </div>
          </div>
        </aside>
      </div>

      <form ref="payFormRef" name="paysubmit" method="post" style="display: none;">
        <input type="hidden" name="biz_content" />
        <input type="hidden" name="charset" value="UTF-8" />
        <input type="hidden" name="sign_type" value="RSA2" />
      </form>
    </section>
  </main>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Wallet, Check, Lock, CreditCard, ChatDotRound } from '@element-plus/icons-vue'
import { getTourOrderDetail } from '@/api/tourOrder'
import { generatePayForm as generatePayFormApi, getOrderByOrderNo, getAvailablePaymentMethods } from '@/api/tourOrderPay'
import { maskPhone } from '@/utils/mask'

const route = useRoute()
const router = useRouter()

const loading = ref(true)
const paying = ref(false)
const order = ref(null)
const error = ref(null)
const payFormRef = ref(null)
const availableMethods = ref([])
const selectedMethod = ref('alipay')

const selectedMethodConfig = computed(() => {
  return availableMethods.value.find(m => m.paymentType === selectedMethod.value) || {
    paymentName: '未知',
    configured: false
  }
})

const getPaymentIcon = (type) => {
  const icons = {
    alipay: CreditCard,
    wechat: ChatDotRound
  }
  return icons[type] || Wallet
}

const getPaymentColor = (type) => {
  const colors = {
    alipay: '#1677ff',
    wechat: '#07c160'
  }
  return colors[type] || '#344054'
}

const selectMethod = (method) => {
  if (!method.configured) {
    ElMessage.warning(`${method.paymentName} 未配置，请选择其他支付方式`)
    return
  }
  selectedMethod.value = method.paymentType
}

const formatDate = (dateStr) => {
  if (!dateStr) return ''
  return String(dateStr).split('T')[0]
}

const formatPrice = (price) => Number(price || 0).toFixed(2)

const loadAvailableMethods = async () => {
  try {
    const res = await getAvailablePaymentMethods()
    availableMethods.value = Array.isArray(res) ? res : (res?.data || [])
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

const fetchOrderInfo = async () => {
  try {
    loading.value = true
    error.value = null

    const orderId = route.params.id
    const orderNo = route.query.orderNo
    if (orderId) {
      order.value = await getTourOrderDetail(orderId)
    } else if (orderNo) {
      order.value = await getOrderByOrderNo(orderNo)
    } else {
      error.value = '订单信息不存在'
      return
    }

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
    const formHtml = await generatePayFormApi(order.value.id, selectedMethod.value)
    const container = document.createElement('div')
    container.innerHTML = formHtml
    container.style.display = 'none'
    document.body.appendChild(container)

    const form = container.querySelector('form')
    if (!form) throw new Error('支付表单生成失败')
    form.target = '_self'
    form.method = 'post'
    form.acceptCharset = 'UTF-8'
    form.submit()

    setTimeout(() => {
      if (document.body.contains(container)) document.body.removeChild(container)
    }, 100)
  } catch (err) {
    console.error('准备支付失败:', err)
    ElMessage.error(err.message || '准备支付失败，请重试')
    paying.value = false
  }
}

const cancelPayment = () => {
  ElMessageBox.confirm('确定要取消支付吗？订单将保留但不会自动完成支付。', '取消支付', {
    confirmButtonText: '确认取消',
    cancelButtonText: '继续支付',
    type: 'warning'
  }).then(() => {
    router.push('/orders')
  }).catch(() => {})
}

const goToOrders = () => router.push('/orders')

onMounted(async () => {
  await Promise.all([fetchOrderInfo(), loadAvailableMethods()])
})
</script>

<style scoped>
.pay-page {
  min-height: 100vh;
  background: #f3f6fb;
  color: #101828;
  font-family: "Source Han Sans", "Noto Sans CJK SC", "Microsoft YaHei", sans-serif;
  padding: 36px 20px 64px;
}

.state-panel {
  max-width: 900px;
  margin: 0 auto;
  padding: 34px;
  border: 1px solid #dde5f0;
  border-radius: 8px;
  background: #fff;
}

.state-panel p {
  color: #667085;
  text-align: center;
}

.pay-shell {
  width: min(1120px, 100%);
  margin: 0 auto;
}

.pay-hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  padding: 26px 28px;
  border: 1px solid #dde5f0;
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 18px 44px rgba(16, 24, 40, 0.06);
}

.pay-hero > div:first-child {
  flex: 1;
  min-width: 0;
  text-align: left;
}

.eyebrow {
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

.pay-hero h1 {
  margin: 12px 0 8px;
  font-size: 34px;
  font-weight: 900;
  letter-spacing: 0;
}

.pay-hero p {
  margin: 0;
  color: #667085;
  max-width: 650px;
}

.step-strip {
  display: flex;
  flex-wrap: wrap;
  flex-shrink: 0;
  justify-content: flex-end;
  gap: 10px;
  padding: 10px;
  border: 1px solid #e4eaf3;
  border-radius: 8px;
  background: #f8fbff;
}

.step-strip span {
  min-height: 42px;
  padding: 0 16px;
  border-radius: 8px;
  background: #eef2f7;
  color: #667085;
  font-size: 13px;
  font-weight: 900;
  line-height: 42px;
  white-space: nowrap;
}

.step-strip .active {
  background: #155eef;
  color: #fff;
}

.pay-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 340px;
  gap: 16px;
  margin-top: 16px;
  align-items: start;
}

.pay-main {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.panel-card,
.amount-card {
  border: 1px solid #dde5f0;
  border-radius: 8px;
  background: #fff;
}

.panel-card {
  padding: 20px;
}

.panel-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 16px;
  text-align: left;
}

.panel-head > div {
  text-align: left;
}

.panel-head span,
.amount-card header span,
.order-grid span,
.amount-value span,
.selected-method span {
  display: block;
  color: #98a2b3;
  font-size: 12px;
  font-weight: 800;
}

.panel-head strong,
.amount-card header strong {
  display: block;
  margin-top: 4px;
  font-size: 18px;
  font-weight: 900;
}

.panel-head p {
  max-width: 360px;
  margin: 0;
  color: #667085;
  font-size: 13px;
  text-align: left;
}

.panel-head code {
  align-self: start;
  padding: 7px 10px;
  border-radius: 8px;
  background: #f2f4f7;
  color: #344054;
  font-size: 12px;
  font-weight: 800;
}

.order-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.order-grid div {
  min-width: 0;
  padding: 13px 14px;
  border: 1px solid #eef2f7;
  border-radius: 8px;
  background: #fbfcfe;
}

.order-grid strong {
  display: block;
  margin-top: 8px;
  color: #344054;
  line-height: 1.5;
  word-break: break-word;
}

.method-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.method-card {
  position: relative;
  display: grid;
  grid-template-columns: 44px minmax(0, 1fr) auto;
  align-items: center;
  gap: 12px;
  min-height: 82px;
  padding: 14px;
  border: 1px solid #e4eaf3;
  border-radius: 8px;
  background: #fff;
  color: #101828;
  text-align: left;
  cursor: pointer;
}

.method-card.active {
  border-color: #155eef;
  background: #edf5ff;
}

.method-card.disabled {
  opacity: 0.58;
  cursor: not-allowed;
}

.method-icon {
  display: grid;
  width: 44px;
  height: 44px;
  place-items: center;
  border-radius: 8px;
  background: #f2f4f7;
  font-size: 24px;
}

.method-copy strong,
.method-copy small {
  display: block;
}

.method-copy strong {
  font-size: 15px;
  font-weight: 900;
}

.method-copy small {
  margin-top: 5px;
  color: #667085;
  font-size: 12px;
  font-weight: 700;
}

.sandbox-chip {
  height: 24px;
  padding: 0 8px;
  border-radius: 999px;
  background: #fff7ed;
  color: #b54708;
  font-size: 12px;
  font-weight: 900;
  line-height: 24px;
}

.check-icon {
  color: #155eef;
}

.lock-icon {
  color: #98a2b3;
}

.debug-alert {
  border-radius: 8px;
}

.debug-alert p {
  margin: 0 0 10px;
}

.amount-panel {
  position: sticky;
  top: 16px;
}

.amount-card {
  padding: 20px;
  text-align: left;
}

.amount-value {
  margin-top: 18px;
  padding: 18px;
  border-radius: 8px;
  background: #fff8f6;
}

.amount-value strong {
  display: block;
  margin-top: 8px;
  color: #d92d20;
  font-size: 34px;
  font-weight: 900;
}

.selected-method {
  margin-top: 14px;
  padding: 14px;
  border: 1px solid #eef2f7;
  border-radius: 8px;
  background: #fbfcfe;
}

.selected-method strong {
  display: block;
  margin-top: 8px;
  color: #344054;
}

.pay-actions {
  display: grid;
  grid-template-columns: 1fr 1.5fr;
  gap: 10px;
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #eef2f7;
}

.pay-actions .el-button {
  height: 44px;
  border-radius: 8px;
  font-weight: 800;
}

@media (max-width: 900px) {
  .pay-hero,
  .panel-head {
    align-items: flex-start;
    flex-direction: column;
  }

  .panel-head p {
    text-align: left;
  }

  .pay-layout {
    grid-template-columns: 1fr;
  }

  .amount-panel {
    position: static;
  }
}

@media (max-width: 640px) {
  .pay-page {
    padding: 24px 12px 48px;
  }

  .pay-hero h1 {
    font-size: 28px;
  }

  .order-grid,
  .method-grid {
    grid-template-columns: 1fr;
  }

  .pay-actions {
    grid-template-columns: 1fr;
  }
}
</style>
