import request from '@/utils/request'

// 创建行程订单
export function createTourOrder(data, config = {}) {
  return request.post('/tour-order', data, config)
}

// 支付订单
export function payTourOrder(id, paymentMethod) {
  return request.put(`/tour-order/${id}/pay`, {}, { params: { paymentMethod } })
}

// 取消订单
export function cancelTourOrder(id, config = {}) {
  return request.put(`/tour-order/${id}/cancel`, null, config)
}

// 获取用户订单列表
export function getUserTourOrders(params) {
  return request.get('/tour-order/user', params)
}

// 获取订单详情
export function getTourOrderDetail(id) {
  return request.get(`/tour-order/${id}`)
}

// 更新订单联系人信息
export function updateOrderContact(id, contactName, contactPhone) {
  return request.put(`/tour-order/${id}/contact`, null, {
    params: { contactName, contactPhone }
  })
}
