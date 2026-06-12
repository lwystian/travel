import request from '@/utils/request'

export function pageCoupons(params) {
  return request.get('/coupon/admin/page', params)
}

export function saveCoupon(data) {
  return request.post('/coupon/admin', data)
}

export function updateCouponStatus(id, status) {
  return request.put(`/coupon/admin/${id}/status`, null, { params: { status } })
}

export function deleteCoupon(id) {
  return request.delete(`/coupon/admin/${id}`)
}

export function issueCoupon(id, data) {
  return request.post(`/coupon/admin/${id}/issue`, data)
}

export function getCouponScopeOptions() {
  return request.get('/coupon/admin/scope-options', {}, { showDefaultMsg: false })
}

export function getCouponIssueUsers(params = {}) {
  return request.get('/coupon/admin/users', params, { showDefaultMsg: false })
}

export function getReceivableCoupons() {
  return request.get('/coupon/receivable')
}

export function receiveCoupon(id) {
  return request.post(`/coupon/${id}/receive`)
}

export function getMyCoupons(params = {}) {
  return request.get('/coupon/my', params)
}

export function getAvailableCoupons(params, config = {}) {
  return request.get('/coupon/available', params, config)
}

export function getOrderAvailableCoupons(orderId, travelers = [], config = {}) {
  return request.post(`/coupon/order/${orderId}/available`, travelers, config)
}

export function applyOrderCoupon(orderId, couponUserId = null, config = {}) {
  return request.put(`/coupon/order/${orderId}/apply`, null, {
    ...config,
    params: couponUserId ? { couponUserId } : {}
  })
}
