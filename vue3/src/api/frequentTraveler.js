import request from '@/utils/request'

// 获取常用出行人列表
export function getFrequentTravelers() {
  return request.get('/frequent-traveler')
}

// 保存常用出行人
export function saveFrequentTraveler(data) {
  return request.post('/frequent-traveler', data)
}

// 更新常用出行人
export function updateFrequentTraveler(id, data) {
  return request.put('/frequent-traveler/' + id, data)
}

// 删除常用出行人
export function deleteFrequentTraveler(id) {
  return request.delete('/frequent-traveler/' + id)
}

// 设置默认出行人
export function setDefaultTraveler(id) {
  return request.put('/frequent-traveler/' + id + '/default')
}
