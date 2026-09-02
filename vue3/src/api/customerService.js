import request from '@/utils/request'

export function getPublicCustomerServiceConfig() {
  return request.get('/site/customer-service/public', null, { showDefaultMsg: false })
}

export function getCustomerServiceConfig() {
  return request.get('/site/customer-service')
}

export function saveCustomerServiceConfig(data, config = {}) {
  return request.post('/site/customer-service', data, config)
}
