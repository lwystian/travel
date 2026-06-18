import request from '@/utils/request'

// ==================== 行程套餐管理 ====================

// 获取行程套餐列表
export function getTourPackages(tourId) {
  return request.get(`/tour-detail/packages/${tourId}`, {}, { showDefaultMsg: false })
}

// 新增行程套餐
export function addTourPackage(data) {
  return request.post('/tour-detail/packages', data)
}

// 更新行程套餐
export function updateTourPackage(id, data) {
  return request.put(`/tour-detail/packages/${id}`, data)
}

// 删除行程套餐
export function deleteTourPackage(id) {
  return request.delete(`/tour-detail/packages/${id}`)
}

// 获取行程套餐价格项
export function getTourPackagePriceItems(packageId) {
  return request.get(`/tour-detail/packages/${packageId}/price-items`, {}, { showDefaultMsg: false })
}

// 保存行程套餐价格项
export function saveTourPackagePriceItem(packageId, data) {
  return request.post(`/tour-detail/packages/${packageId}/price-items`, data)
}

// 删除行程套餐价格项
export function deleteTourPackagePriceItem(packageId, itemId) {
  return request.delete(`/tour-detail/packages/${packageId}/price-items/${itemId}`)
}

// ==================== 批次套餐管理 ====================

// 获取批次套餐列表
export function getBatchPackages(tourId) {
  return request.get(`/tour-detail/batch-packages/${tourId}`, {}, { showDefaultMsg: false })
}

// 新增批次套餐
export function addBatchPackage(data) {
  return request.post('/tour-detail/batch-packages', data)
}

// 更新批次套餐
export function updateBatchPackage(id, data) {
  return request.put(`/tour-detail/batch-packages/${id}`, data)
}

// 删除批次套餐
export function deleteBatchPackage(id) {
  return request.delete(`/tour-detail/batch-packages/${id}`)
}

// 获取附加费用价格项
export function getBatchPackagePriceItems(addonId) {
  return request.get(`/tour-detail/batch-packages/${addonId}/price-items`, {}, { showDefaultMsg: false })
}

// 保存附加费用价格项
export function saveBatchPackagePriceItem(addonId, data) {
  return request.post(`/tour-detail/batch-packages/${addonId}/price-items`, data)
}

// 删除附加费用价格项
export function deleteBatchPackagePriceItem(addonId, itemId) {
  return request.delete(`/tour-detail/batch-packages/${addonId}/price-items/${itemId}`)
}

// ==================== 出发班期管理 ====================

// 获取出发班期列表
export function getTourBatches(tourId) {
  return request.get(`/tour-detail/batches/${tourId}`, {}, { showDefaultMsg: false })
}

// 新增出发班期
export function addTourBatch(data) {
  return request.post('/tour-detail/batches', data)
}

// 批量新增出发班期
export function addTourBatchesBatch(data) {
  return request.post('/tour-detail/batches/batch', data)
}

// 更新出发班期
export function updateTourBatch(id, data) {
  return request.put(`/tour-detail/batches/${id}`, data)
}

// 删除出发班期
export function deleteTourBatch(id) {
  return request.delete(`/tour-detail/batches/${id}`)
}
