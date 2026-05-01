import request from '@/utils/request'

export function getTravelersByOrderId(orderId) {
  return request.get('/traveler/order/' + orderId)
}

export function getTravelersByOrderNo(orderNo) {
  return request.get('/traveler/order-no/' + orderNo)
}

export function saveTravelers(orderId, orderNo, travelers) {
  return request.post('/traveler/save/' + orderId, travelers, {
    params: { orderNo }
  })
}
