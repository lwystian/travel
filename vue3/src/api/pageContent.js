import request from '@/utils/request'

export function getPublicPageContent(pageKey) {
  return request.get(`/site/page-content/public/${pageKey}`)
}

export function getPageContent(pageKey) {
  return request.get(`/site/page-content/${pageKey}`)
}

export function savePageContent(pageKey, data, config = {}) {
  return request.post(`/site/page-content/${pageKey}`, data, config)
}
