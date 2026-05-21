import request from '@/utils/request'

export function getPublicFooterConfig() {
  return request.get('/site/footer/public')
}

export function getFooterConfig() {
  return request.get('/site/footer')
}

export function saveFooterConfig(data, config = {}) {
  return request.post('/site/footer', data, config)
}
