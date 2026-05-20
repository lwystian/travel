import request from '@/utils/request'

export function getSmsConfig() {
  return request.get('/auth/config/sms')
}

export function saveSmsConfig(data, config = {}) {
  return request.post('/auth/config/sms', data, config)
}

export function getGeetestConfig() {
  return request.get('/auth/config/geetest')
}

export function saveGeetestConfig(data, config = {}) {
  return request.post('/auth/config/geetest', data, config)
}

export function getEmailConfig() {
  return request.get('/auth/config/email')
}

export function saveEmailConfig(data, config = {}) {
  return request.post('/auth/config/email', data, config)
}
