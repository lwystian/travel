import request from '@/utils/request'
import { encryptPassword } from '@/utils/passwordCrypto'

export async function login(data) {
  return request.post('/auth/login/password', {
    phone: data.phone,
    encryptedPassword: await encryptPassword(data.password),
    agreementAccepted: data.agreementAccepted === true
  })
}

export async function register(data) {
  return request.post('/auth/register/phone', {
    phone: data.phone,
    smsCode: data.smsCode,
    encryptedPassword: await encryptPassword(data.password),
    encryptedConfirmPassword: await encryptPassword(data.confirmPassword),
    agreementAccepted: data.agreementAccepted === true
  })
}
