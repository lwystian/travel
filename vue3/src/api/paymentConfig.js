import request from '@/utils/request'

/**
 * 获取已启用的支付方式列表
 */
export function getEnabledPayments() {
  return request.get('/payment-config/enabled')
}

/**
 * 获取所有支付配置
 */
export function getAllPayments() {
  return request.get('/payment-config/list')
}

/**
 * 获取支付配置详情
 */
export function getPaymentConfig(id) {
  return request.get(`/payment-config/${id}`)
}

/**
 * 获取支付配置详情（根据支付类型）
 */
export function getPaymentDetail(paymentType) {
  return request.get('/payment-config/detail', { paymentType })
}

/**
 * 获取支付宝配置详情
 */
export function getAlipayDetail() {
  return request.get('/payment-config/alipay/detail')
}

/**
 * 保存支付宝配置
 */
export function saveAlipayConfig(data, config) {
  return request.post('/payment-config/alipay/save', data, config)
}

/**
 * 保存支付配置
 */
export function savePayment(data, config) {
  return request.post('/payment-config/save', data, config)
}

/**
 * 更新支付配置
 */
export function updatePayment(id, data) {
  return request.put(`/payment-config/${id}`, data)
}

/**
 * 切换启用状态
 */
export function togglePaymentEnabled(id, enabled, config = {}) {
  return request.put(`/payment-config/${id}/toggle`, null, {
    params: { enabled },
    ...config
  })
}

/**
 * 删除支付配置
 */
export function deletePaymentApi(id, config = {}) {
  return request.delete(`/payment-config/${id}`, config)
}

/**
 * 获取可用的支付方式（用于支付页面）
 */
export function getAvailablePaymentMethods() {
  return request.get('/tour-order-pay/methods')
}
