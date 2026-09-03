import request from '@/utils/request'

export function getPublicCustomerServiceConfig() {
  return request.get('/site/customer-service/public', null, { showDefaultMsg: false })
}

export function resolveCustomerServiceUrl(config, channel = 'home') {
  if (!config?.enabled) return ''
  const channelUrl = config?.channelUrls?.[channel]
  return validWecomUrl(channelUrl) || validWecomUrl(config?.serviceUrl)
}

function validWecomUrl(value) {
  if (!value) return ''
  try {
    const url = new URL(String(value))
    return url.protocol === 'https:'
      && url.hostname === 'work.weixin.qq.com'
      && url.pathname.startsWith('/kfid/')
      ? url.toString()
      : ''
  } catch {
    return ''
  }
}

export function getCustomerServiceConfig() {
  return request.get('/site/customer-service')
}

export function saveCustomerServiceConfig(data, config = {}) {
  return request.post('/site/customer-service', data, config)
}
