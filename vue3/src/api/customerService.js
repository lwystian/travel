import request from '@/utils/request'

export function getPublicCustomerServiceConfig() {
  return request.get('/site/customer-service/public', null, { showDefaultMsg: false })
}
