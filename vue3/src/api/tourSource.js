import request from '@/utils/request'

export function getPublicTourSourceConfig() {
  return request.get('/site/tour-source/public', {}, { showDefaultMsg: false })
}

export function getTourSourceConfig() {
  return request.get('/site/tour-source', {}, { showDefaultMsg: false })
}

export function saveTourSourceConfig(data, config = {}) {
  return request.post('/site/tour-source', data, config)
}

export function checkMiniappTourSource(data) {
  return request.post('/site/tour-source/check', data, { showDefaultMsg: false })
}
