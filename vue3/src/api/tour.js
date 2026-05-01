import request from '@/utils/request'

// 获取行程列表（分页）
export function getTourPage(params) {
  return request.get('/tour/page', params)
}

// 获取所有行程
export function getAllTours() {
  return request.get('/tour/all', {}, { showDefaultMsg: false })
}

// 获取所有上架行程
export function getTourList() {
  return request.get('/tour/list', {}, { showDefaultMsg: false })
}

// 获取行程详情（简单信息）
export function getTourDetail(id) {
  return request.get(`/tour/${id}`, {}, { showDefaultMsg: false })
}

// 获取行程完整详情（包含套餐、批次等）
export function getTourDetailFull(id) {
  return request.get(`/tour/${id}/detail`, {}, { showDefaultMsg: false })
}

// 更新行程图片
export function updateTourImages(id, images) {
  return request.put(`/tour/${id}/images`, { images })
}

// 更新行程视频
export function updateTourVideo(id, video) {
  return request.put(`/tour/${id}/video`, video)
}
