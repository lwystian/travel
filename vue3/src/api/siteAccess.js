import request from '@/utils/request'

export function getPublicSiteAccessConfig() {
  return request.get('/site/access/public')
}

export function getSiteAccessConfig() {
  return request.get('/site/access')
}

export function saveSiteAccessConfig(data, config = {}) {
  return request.post('/site/access', data, config)
}
