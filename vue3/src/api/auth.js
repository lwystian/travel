import request from '@/utils/request'

export function getGeetestPublicConfig() {
  return request.get('/auth/geetest/public')
}

export function getPasswordPublicKey() {
  return request.get('/auth/crypto/public-key')
}

export function sendSmsCode(data, config = {}) {
  return request.post('/auth/sms/send', data, config)
}

export function registerByPhone(data, config = {}) {
  return request.post('/auth/register/phone', data, config)
}

export function loginByPhonePassword(data, config = {}) {
  return request.post('/auth/login/password', data, config)
}

export function loginByPhoneCode(data, config = {}) {
  return request.post('/auth/login/code', data, config)
}
