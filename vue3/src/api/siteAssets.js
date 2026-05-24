import request from '@/utils/request'

export function getPublicSiteAssets() {
  return request.get('/site/assets/public')
}

export function getSiteAssets() {
  return request.get('/site/assets')
}

export function saveSiteAssets(data, config = {}) {
  return request.post('/site/assets', data, config)
}
