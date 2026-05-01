<template>
  <div class="alipay-form-container">
    <div v-if="loading" class="loading">
      <p>加载中...</p>
    </div>
    
    <div v-else-if="orderInfo" class="payment-container">
      <div class="header">
        <div class="logo">支付宝 - 模拟支付</div>
        <div>请确认支付信息</div>
      </div>
      
      <div class="order-info">
        <div class="info-row">
          <span>商品名称：</span>
          <span>{{ orderInfo.tourName }}</span>
        </div>
        <div class="info-row">
          <span>订单号：</span>
          <span>{{ orderInfo.orderNo }}</span>
        </div>
        <div class="info-row">
          <span>商户：</span>
          <span>侠客行国旅</span>
        </div>
      </div>
      
      <div class="amount">￥ {{ orderInfo.totalAmount }}</div>
      
      <div class="buttons" v-if="!paying">
        <button class="btn btn-secondary" @click="cancelPayment">取消支付</button>
        <button class="btn btn-primary" @click="confirmPayment">确认支付</button>
      </div>
      
      <div class="loading" v-if="paying">
        <p>支付处理中，请稍候...</p>
      </div>
    </div>
    
    <div v-else class="error">
      <p>加载支付信息失败，请重试</p>
      <button class="btn btn-primary" @click="goBack">返回</button>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'

const route = useRoute()
const router = useRouter()

const loading = ref(true)
const paying = ref(false)
const orderInfo = ref(null)

// 获取订单信息
const fetchOrderInfo = async () => {
  try {
    loading.value = true
    const res = await request.get(`/tour-order/${route.params.id}`, {}, {
      showDefaultMsg: false
    })
    if (res) {
      orderInfo.value = res
    } else {
      ElMessage.error('获取订单信息失败')
    }
  } catch (error) {
    console.error('获取订单信息失败:', error)
    ElMessage.error('获取订单信息失败')
  } finally {
    loading.value = false
  }
}

// 确认支付
const confirmPayment = async () => {
  if (!orderInfo.value) {
    ElMessage.error('订单信息错误')
    return
  }

  paying.value = true

  try {
    await request.post(`/tour-order-pay/mock-pay/${route.params.id}`, {}, {
      showDefaultMsg: false
    })
    ElMessage.success('支付成功')
    // 跳转到支付结果页面
    router.push(`/payment/result?out_trade_no=${orderInfo.value.orderNo}&status=success`)
  } catch (error) {
    console.error('支付失败:', error)
    ElMessage.error('支付失败，请重试')
    // 跳转到支付失败页面
    router.push(`/payment/result?out_trade_no=${orderInfo.value.orderNo}&status=failed`)
  } finally {
    paying.value = false
  }
}

// 取消支付
const cancelPayment = () => {
  ElMessageBox.confirm('确定要取消支付吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    router.push('/orders')
  }).catch(() => {
    // 用户取消了取消操作
  })
}

// 返回
const goBack = () => {
  router.push('/orders')
}

onMounted(() => {
  fetchOrderInfo()
})
</script>

<style scoped>
.alipay-form-container {
  width: 100%;
  height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background: #FFFFFF;
  font-family: Arial, sans-serif;
}

.loading {
  text-align: center;
  color: #666;
  font-size: 16px;
}

.error {
  text-align: center;
  color: #ff4d4f;
}

.payment-container {
  max-width: 500px;
  margin: 50px auto;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 10px rgba(0,0,0,0.1);
  padding: 30px;
}

.header {
  text-align: center;
  margin-bottom: 30px;
}

.logo {
  color: #1677ff;
  font-size: 24px;
  font-weight: bold;
  margin-bottom: 10px;
}

.order-info {
  background: #f8f9fa;
  padding: 20px;
  border-radius: 6px;
  margin-bottom: 20px;
}

.info-row {
  display: flex;
  justify-content: space-between;
  margin-bottom: 10px;
}

.info-row:last-child {
  margin-bottom: 0;
}

.amount {
  color: #ff4d4f;
  font-size: 24px;
  font-weight: bold;
  text-align: center;
  margin: 20px 0;
}

.buttons {
  display: flex;
  gap: 10px;
  justify-content: center;
}

.btn {
  padding: 12px 24px;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 16px;
  transition: all 0.3s;
  min-width: 100px;
}

.btn-primary {
  background: #1677ff;
  color: white;
}

.btn-primary:hover {
  background: #0958d9;
}

.btn-secondary {
  background: #FFFFFF;
  color: #666;
}

.btn-secondary:hover {
  background: #e6e6e6;
}

.btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
</style>