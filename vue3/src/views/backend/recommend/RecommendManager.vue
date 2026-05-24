<template>
  <div class="recommend-management">
    <div class="page-header">
      <h1 class="page-title">首页推荐管理</h1>
      <p class="page-subtitle">Homepage Recommendation Management</p>
    </div>

    <!-- 操作说明 -->
    <el-card class="info-card" shadow="never">
      <div class="info-content">
        <div class="info-icon">💡</div>
        <div class="info-text">
          <p><strong>使用说明：</strong></p>
          <p>1. 精选行程用于首页“精选行程”主推大图，只保留1个上架行程</p>
          <p>2. 更多推荐用于主推右侧的副推卡片和后续推荐，建议至少配置3个，支持拖拽排序</p>
          <p>3. 更多推荐会自动排除当前精选行程，避免首页重复展示同一产品</p>
          <p>4. 如果清空设置，将按默认规则自动补齐首页展示内容</p>
        </div>
      </div>
    </el-card>

    <!-- 推荐类型切换 -->
    <div class="type-tabs">
      <el-radio-group v-model="currentType" @change="handleTypeChange">
        <el-radio-button label="featured">
          <i class="el-icon-star-on"></i> 精选行程
        </el-radio-button>
        <el-radio-button label="more">
          <i class="el-icon-more"></i> 更多推荐
        </el-radio-button>
      </el-radio-group>
      <div class="tab-actions">
        <el-button type="primary" @click="handleAddRecommend" class="add-btn">
          <i class="el-icon-plus"></i> {{ currentType === 'featured' ? '设置精选' : '添加推荐' }}
        </el-button>
        <el-button type="danger" plain @click="handleClearAll" :loading="clearing">
          <i class="el-icon-delete"></i> 清空设置
        </el-button>
      </div>
    </div>

    <!-- 当前推荐列表 -->
    <el-card class="table-card" shadow="never">
      <template #header>
        <div class="card-header">
          <span class="card-title">
            {{ currentType === 'featured' ? '精选行程' : '更多推荐' }}
            <el-tag type="info" size="small">{{ recommendList.length }} 个</el-tag>
          </span>
          <span class="card-tip" v-if="currentType === 'more'">拖拽可调整顺序</span>
        </div>
      </template>

      <!-- 更多推荐：拖拽排序列表 -->
      <div 
        v-if="currentType === 'more' && recommendList.length > 0" 
        class="sortable-list"
        ref="sortableListRef"
      >
        <div
          v-for="(item, index) in recommendList"
          :key="item.id"
          class="sortable-item"
          :data-id="item.id"
          draggable="true"
          @dragstart="handleDragStart($event, index)"
          @dragover.prevent="handleDragOver($event, index)"
          @drop="handleDrop($event, index)"
          @dragend="handleDragEnd($event)"
        >
          <div class="item-index">{{ index + 1 }}</div>
          <div class="item-drag">
            <i class="el-icon-rank"></i>
          </div>
          <div class="item-image">
            <el-image
              v-if="item.mainImage"
              :src="getImageUrl(item.mainImage)"
              fit="cover"
              class="tour-image"
            >
              <template #error>
                <div class="image-placeholder">
                  <i class="el-icon-picture-outline"></i>
                </div>
              </template>
            </el-image>
            <div v-else class="image-placeholder">
              <i class="el-icon-picture-outline"></i>
            </div>
          </div>
          <div class="item-info">
            <div class="item-title">{{ item.title || '行程已删除' }}</div>
            <div class="item-meta">
              <span v-if="item.tourType">
                <el-tag size="small" type="info">{{ getTourTypeName(item.tourType) }}</el-tag>
              </span>
              <span v-if="item.days">{{ item.days }}天行程</span>
              <span class="price">¥{{ formatPrice(item.minPrice) }}起</span>
            </div>
          </div>
          <div class="item-actions">
            <el-button type="danger" size="small" plain @click="handleDeleteRecommend(item)" class="delete-btn">
              <i class="el-icon-delete"></i> 移除
            </el-button>
          </div>
        </div>
      </div>

      <!-- 精选行程：单卡片展示 -->
      <div v-else-if="currentType === 'featured' && recommendList.length > 0" class="sortable-list">
        <div
          v-for="item in recommendList"
          :key="item.id"
          class="sortable-item"
        >
          <div class="item-image">
            <el-image
              v-if="item.mainImage"
              :src="getImageUrl(item.mainImage)"
              fit="cover"
              class="tour-image"
            >
              <template #error>
                <div class="image-placeholder">
                  <i class="el-icon-picture-outline"></i>
                </div>
              </template>
            </el-image>
            <div v-else class="image-placeholder">
              <i class="el-icon-picture-outline"></i>
            </div>
          </div>
          <div class="item-info">
            <div class="item-title">{{ item.title || '行程已删除' }}</div>
            <div class="item-meta">
              <span v-if="item.tourType">
                <el-tag size="small" type="info">{{ getTourTypeName(item.tourType) }}</el-tag>
              </span>
              <span v-if="item.days">{{ item.days }}天行程</span>
              <span class="price">¥{{ formatPrice(item.minPrice) }}起</span>
            </div>
          </div>
          <div class="item-actions">
            <el-button type="danger" size="small" plain @click="handleDeleteRecommend(item)" class="delete-btn">
              <i class="el-icon-delete"></i> 移除
            </el-button>
          </div>
        </div>
      </div>

      <!-- 空状态 -->
      <div v-else class="empty-state">
        <i class="el-icon-folder-opened"></i>
        <p>暂无设置</p>
        <p class="empty-tip">点击上方"{{ currentType === 'featured' ? '设置精选' : '添加推荐' }}"开始配置</p>
      </div>
    </el-card>

    <!-- 添加推荐对话框 -->
    <el-dialog
      :title="currentType === 'featured' ? '设置精选行程' : '选择推荐行程'"
      v-model="dialogVisible"
      width="800px"
      class="add-recommend-dialog"
    >
      <div class="dialog-search">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索行程名称"
          clearable
          @input="handleSearch"
        >
          <template #prefix>
            <i class="el-icon-search"></i>
          </template>
        </el-input>
      </div>

      <div class="dialog-tour-list">
        <!-- 精选行程使用 checkbox-group -->
        <el-checkbox-group
          v-if="currentType === 'featured'"
          v-model="selectedTourId"
          style="display: flex; flex-direction: column;"
        >
          <div
            v-for="tour in filteredTourList"
            :key="tour.id"
            :class="['tour-option', { selected: selectedTourId.includes(tour.id) }]"
            @click="selectedTourId = [tour.id]"
          >
            <el-checkbox :value="tour.id" @click.stop @change="selectedTourId = [tour.id]">
              <div class="tour-option-content">
                <div class="tour-option-image">
                  <el-image
                    v-if="tour.mainImage"
                    :src="getImageUrl(tour.mainImage)"
                    fit="cover"
                  >
                    <template #error>
                      <div class="image-placeholder-sm">
                        <i class="el-icon-picture-outline"></i>
                      </div>
                    </template>
                  </el-image>
                  <div v-else class="image-placeholder-sm">
                    <i class="el-icon-picture-outline"></i>
                  </div>
                </div>
                <div class="tour-option-info">
                  <div class="tour-option-title">{{ tour.title }}</div>
                  <div class="tour-option-meta">
                    <el-tag size="small" type="info">{{ getTourTypeName(tour.tourType) }}</el-tag>
                    <span>¥{{ formatPrice(tour.minPrice) }}起</span>
                  </div>
                </div>
              </div>
            </el-checkbox>
          </div>
        </el-checkbox-group>

        <!-- 更多推荐使用 checkbox-group -->
        <el-checkbox-group
          v-else
          v-model="selectedTourIds"
          style="display: flex; flex-direction: column;"
        >
          <div
            v-for="tour in filteredTourList"
            :key="tour.id"
            :class="['tour-option', { selected: selectedTourIds.includes(tour.id) }]"
            @click="toggleTourSelection(tour.id)"
          >
            <el-checkbox :value="tour.id" @click.stop>
              <div class="tour-option-content">
                <div class="tour-option-image">
                  <el-image
                    v-if="tour.mainImage"
                    :src="getImageUrl(tour.mainImage)"
                    fit="cover"
                  >
                    <template #error>
                      <div class="image-placeholder-sm">
                        <i class="el-icon-picture-outline"></i>
                      </div>
                    </template>
                  </el-image>
                  <div v-else class="image-placeholder-sm">
                    <i class="el-icon-picture-outline"></i>
                  </div>
                </div>
                <div class="tour-option-info">
                  <div class="tour-option-title">{{ tour.title }}</div>
                  <div class="tour-option-meta">
                    <el-tag size="small" type="info">{{ getTourTypeName(tour.tourType) }}</el-tag>
                    <span>¥{{ formatPrice(tour.minPrice) }}起</span>
                  </div>
                </div>
              </div>
            </el-checkbox>
          </div>
        </el-checkbox-group>

        <div v-if="filteredTourList.length === 0" class="no-data">
          <p>没有可选择的行程</p>
        </div>
      </div>

      <template #footer>
        <span class="dialog-footer">
          <span v-if="currentType === 'more'" class="selected-count">已选择 {{ selectedTourIds.length }} 个行程</span>
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleConfirmAdd" :loading="submitting">
            确认
          </el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import request from '@/utils/request'
import { ElMessage, ElMessageBox } from 'element-plus'
import noImage from '@/assets/images/no-image.png'

const currentType = ref('featured')
const recommendList = ref([])
const allTourList = ref([])
const loading = ref(false)
const clearing = ref(false)
const dialogVisible = ref(false)
const searchKeyword = ref('')
const selectedTourIds = ref([])
const selectedTourId = ref([])
const submitting = ref(false)
const featuredRecommendIds = ref([])

// 拖拽相关
const sortableListRef = ref(null)
const dragIndex = ref(-1)

const baseAPI = process.env.VUE_APP_BASE_API || '/api'

// 获取图片完整URL
const getImageUrl = (url) => {
  if (!url) return noImage
  return url.startsWith('http') ? url : baseAPI + url
}

// 获取行程类型名称
const getTourTypeName = (type) => {
  const typeMap = {
    'around': '周边游',
    'long': '长线游',
    'team': '跟团游',
    'cruise': '邮轮出行'
  }
  return typeMap[type] || '精选游'
}

// 格式化价格显示
const formatPrice = (price) => {
  if (price === undefined || price === null || price === '') return '--'
  const num = Number(price)
  if (isNaN(num)) return '--'
  return num.toFixed(2)
}

// 过滤后的行程列表（排除已推荐的）
const filteredTourList = computed(() => {
  const recommendedIds = recommendList.value
    .filter(item => item.tourId)
    .map(item => item.tourId)
  
  let excludedIds = recommendedIds
  if (currentType.value === 'more') {
    excludedIds = [...new Set([...recommendedIds, ...featuredRecommendIds.value])]
  }

  let list = allTourList.value.filter(tour => !excludedIds.includes(tour.id))
  
  if (searchKeyword.value) {
    list = list.filter(tour => 
      tour.title && tour.title.toLowerCase().includes(searchKeyword.value.toLowerCase())
    )
  }
  
  return list
})

// 获取推荐列表
const fetchRecommends = async () => {
  loading.value = true
  try {
    await request.get('/tour/recommends', {
      type: currentType.value
    }, {
      showDefaultMsg: false,
      onSuccess: (data) => {
        const tourMap = new Map(allTourList.value.map(t => [t.id, t]))
        recommendList.value = (data || []).map(item => {
          const tourData = tourMap.get(item.tourId)
          if (tourData && !item.minPrice && tourData.minPrice) {
            return { ...item, ...tourData }
          }
          return item
        })
      }
    })
  } catch (error) {
    console.error('获取推荐列表失败:', error)
  } finally {
    loading.value = false
  }
}

// 获取所有行程列表
const fetchAllTours = async () => {
  try {
    await request.get('/tour/all', {}, {
      showDefaultMsg: false,
      onSuccess: (data) => {
        allTourList.value = data || []
      }
    })
  } catch (error) {
    console.error('获取行程列表失败:', error)
  }
}

// 切换类型
const handleTypeChange = () => {
  searchKeyword.value = ''
  selectedTourIds.value = []
  selectedTourId.value = []
  if (currentType.value === 'more') {
    loadFeaturedRecommendIds()
  } else {
    featuredRecommendIds.value = []
  }
  fetchRecommends()
}

// 搜索
const handleSearch = () => {
  // 搜索时重新计算过滤列表
}

// 添加推荐
const handleAddRecommend = async () => {
  selectedTourIds.value = []
  selectedTourId.value = []
  await loadFeaturedRecommendIds()
  dialogVisible.value = true
}

const loadFeaturedRecommendIds = async () => {
  if (currentType.value !== 'more') {
    featuredRecommendIds.value = []
    return
  }
  await request.get('/tour/recommends', {
    type: 'featured'
  }, {
    showDefaultMsg: false,
    onSuccess: (data) => {
      featuredRecommendIds.value = (data || []).map(item => item.tourId).filter(Boolean)
    }
  })
}

// 切换行程选择（多选）
const toggleTourSelection = (tourId) => {
  const index = selectedTourIds.value.indexOf(tourId)
  if (index === -1) {
    selectedTourIds.value.push(tourId)
  } else {
    selectedTourIds.value.splice(index, 1)
  }
}

// 确认添加
const handleConfirmAdd = async () => {
  if (currentType.value === 'featured') {
    // 精选行程单选
    if (selectedTourId.value.length === 0) {
      ElMessage.warning('请选择一个行程作为精选')
      return
    }
    
    submitting.value = true
    try {
      await request.post('/tour/recommends', {
        type: currentType.value,
        tourIds: selectedTourId.value
      }, {
        successMsg: '设置成功',
        onSuccess: () => {
          dialogVisible.value = false
          selectedTourId.value = []
          fetchRecommends()
        }
      })
    } catch (error) {
      console.error('设置精选失败:', error)
    } finally {
      submitting.value = false
    }
  } else {
    // 更多推荐多选
    if (selectedTourIds.value.length === 0) {
      ElMessage.warning('请选择要添加的行程')
      return
    }
    
    submitting.value = true
    try {
      await request.post('/tour/recommends', {
        type: currentType.value,
        tourIds: selectedTourIds.value
      }, {
        successMsg: '添加成功',
        onSuccess: () => {
          dialogVisible.value = false
          selectedTourIds.value = []
          fetchRecommends()
        }
      })
    } catch (error) {
      console.error('添加推荐失败:', error)
    } finally {
      submitting.value = false
    }
  }
}

// 删除单个推荐
const handleDeleteRecommend = (item) => {
  ElMessageBox.confirm('确定要移除该推荐吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await request.delete(`/tour/recommend/${item.id}`, {
        successMsg: '移除成功',
        onSuccess: () => {
          fetchRecommends()
        }
      })
    } catch (error) {
      console.error('移除推荐失败:', error)
    }
  }).catch(() => {})
}

// 清空所有推荐
const handleClearAll = () => {
  ElMessageBox.confirm(
    `确定要清空所有"${currentType.value === 'featured' ? '精选行程' : '更多推荐'}"设置吗？清空后将按默认规则自动推荐。`,
    '警告',
    {
      confirmButtonText: '确定清空',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(async () => {
    clearing.value = true
    try {
      await request.delete('/tour/recommends/clear', {
        params: { type: currentType.value },
        successMsg: '清空成功',
        onSuccess: () => {
          fetchRecommends()
        }
      })
    } catch (error) {
      console.error('清空推荐失败:', error)
    } finally {
      clearing.value = false
    }
  }).catch(() => {})
}

// 拖拽排序方法
const handleDragStart = (e, index) => {
  dragIndex.value = index
  e.dataTransfer.effectAllowed = 'move'
  e.dataTransfer.setData('text/plain', index)
}

const handleDragOver = (e, index) => {
  e.preventDefault()
  if (dragIndex.value !== index) {
    // 添加视觉提示
    const items = sortableListRef.value?.querySelectorAll('.sortable-item')
    if (items) {
      items.forEach((item, i) => {
        item.style.opacity = i === index ? '0.5' : '1'
      })
    }
  }
}

const handleDrop = async (e, index) => {
  e.preventDefault()
  if (dragIndex.value === -1 || dragIndex.value === index) {
    resetDragStyles()
    return
  }

  // 移动数组元素
  const item = recommendList.value.splice(dragIndex.value, 1)[0]
  recommendList.value.splice(index, 0, item)
  resetDragStyles()

  // 保存排序
  try {
    const ids = recommendList.value.map(item => item.id)
    await request.put('/tour/recommend/sort', { ids }, {
      showDefaultMsg: false
    })
    ElMessage.success('排序已保存')
  } catch (error) {
    console.error('保存排序失败:', error)
    // 恢复原顺序
    fetchRecommends()
  }

  dragIndex.value = -1
}

const handleDragEnd = () => {
  resetDragStyles()
  dragIndex.value = -1
}

const resetDragStyles = () => {
  const items = sortableListRef.value?.querySelectorAll('.sortable-item')
  if (items) {
    items.forEach(item => {
      item.style.opacity = '1'
    })
  }
}

onMounted(() => {
  fetchRecommends()
  fetchAllTours()
})
</script>

<style lang="scss" scoped>
.recommend-management {
  padding: 20px;
  background-color: #f9fafc;
  min-height: calc(100vh - 120px);

  .page-header {
    margin-bottom: 24px;
    text-align: left;

    .page-title {
      font-size: 24px;
      color: #34495e;
      margin: 0 0 8px 0;
      font-weight: 500;
    }

    .page-subtitle {
      font-size: 14px;
      color: #7f8c8d;
      margin: 0;
      font-style: italic;
    }
  }

  .info-card {
    margin-bottom: 20px;
    border-radius: 8px;
    background: linear-gradient(135deg, #f0f9ff 0%, #e0f2fe 100%);
    border: 1px solid #bae6fd;

    .info-content {
      display: flex;
      align-items: flex-start;
      gap: 12px;

      .info-icon {
        font-size: 24px;
      }

      .info-text {
        p {
          margin: 0 0 4px 0;
          font-size: 14px;
          color: #0369a1;

          &:first-child {
            margin-bottom: 8px;
          }

          strong {
            color: #0c4a6e;
          }
        }
      }
    }
  }

  .type-tabs {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;
    padding: 16px 20px;
    background: #fff;
    border-radius: 8px;

    .el-radio-button {
      :deep(.el-radio-button__inner) {
        padding: 10px 20px;
        font-size: 14px;
      }
    }

    .tab-actions {
      display: flex;
      gap: 12px;

      .add-btn {
        background-color: #8B5CF6;
        border-color: #8B5CF6;

        &:hover, &:focus {
          background-color: #7C3AED;
          border-color: #7C3AED;
        }
      }
    }
  }

  .table-card {
    border-radius: 8px;
    overflow: hidden;
    box-shadow: none;

    :deep(.el-card__header) {
      background-color: #f8fafc;
      border-bottom: 1px solid #e2e8f0;
      padding: 16px 20px;
    }

    .card-header {
      display: flex;
      justify-content: space-between;
      align-items: center;

      .card-title {
        font-size: 16px;
        font-weight: 500;
        color: #34495e;
        display: flex;
        align-items: center;
        gap: 8px;
      }

      .card-tip {
        font-size: 12px;
        color: #94a3b8;
      }
    }
  }

  // 拖拽排序列表
  .sortable-list {
    padding: 12px 0;
  }

  .sortable-item {
    display: flex;
    align-items: center;
    padding: 12px 16px;
    border-bottom: 1px solid #f1f5f9;
    transition: all 0.3s;
    cursor: move;
    background: #fff;

    &:hover {
      background-color: #f8fafc;
    }

    &.dragging {
      opacity: 0.5;
      background-color: #f0f9ff;
    }

    &:last-child {
      border-bottom: none;
    }

    .item-index {
      width: 28px;
      height: 28px;
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      color: #fff;
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 12px;
      font-weight: 600;
      margin-right: 12px;
    }

    .item-drag {
      color: #94a3b8;
      margin-right: 12px;
      font-size: 18px;

      &:hover {
        color: #64748b;
      }
    }

    .item-image {
      width: 80px;
      height: 56px;
      border-radius: 8px;
      overflow: hidden;
      margin-right: 16px;
      flex-shrink: 0;

      .tour-image {
        width: 100%;
        height: 100%;
      }

      .image-placeholder {
        width: 100%;
        height: 100%;
        background: #f1f5f9;
        display: flex;
        align-items: center;
        justify-content: center;
        color: #94a3b8;
        font-size: 24px;
      }
    }

    .item-info {
      flex: 1;
      min-width: 0;

      .item-title {
        font-size: 14px;
        font-weight: 500;
        color: #1e293b;
        margin-bottom: 6px;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }

      .item-meta {
        display: flex;
        align-items: center;
        gap: 12px;
        font-size: 12px;
        color: #64748b;

        .price {
          color: #ef4444;
          font-weight: 500;
        }
      }
    }

    .item-actions {
      margin-left: 16px;

      .delete-btn {
        color: #ef4444;
        border-color: #fecaca;

        &:hover {
          background-color: #fef2f2;
        }
      }
    }
  }

  .empty-state {
    text-align: center;
    padding: 60px 20px;
    color: #94a3b8;

    i {
      font-size: 48px;
      margin-bottom: 16px;
    }

    p {
      margin: 0;
      font-size: 14px;
    }

    .empty-tip {
      margin-top: 8px !important;
      font-size: 12px !important;
      color: #cbd5e1;
    }
  }

  // 添加推荐对话框
  .add-recommend-dialog {
    :deep(.el-dialog__body) {
      padding: 20px;
    }

    .dialog-search {
      margin-bottom: 16px;
    }

    .dialog-tour-list,
    .single-select-list {
      max-height: 400px;
      overflow-y: auto;
      border: 1px solid #e2e8f0;
      border-radius: 8px;
      padding: 8px;
    }

    .tour-option {
      padding: 12px;
      border-radius: 8px;
      margin-bottom: 8px;
      cursor: pointer;
      border: 1px solid transparent;
      transition: all 0.2s;

      &:hover {
        background-color: #f8fafc;
      }

      &.selected {
        background-color: #f0f9ff;
        border-color: #93c5fd;
      }

      :deep(.el-checkbox),
      :deep(.el-radio) {
        width: 100%;
        height: auto;
        line-height: normal;
        margin-right: 0;
        display: flex;
        align-items: center;

        .el-checkbox__label,
        .el-radio__label {
          width: 100%;
          padding-left: 8px;
          vertical-align: top;
          display: flex;
          align-items: center;
        }
      }

      :deep(.el-radio-group),
      :deep(.el-checkbox-group) {
        display: flex;
        flex-direction: column;
      }

      .tour-option-content {
        display: flex;
        align-items: center;
        gap: 12px;
        position: relative;
      }

      .tour-option-image {
        width: 60px;
        height: 42px;
        border-radius: 6px;
        overflow: hidden;
        flex-shrink: 0;

        .el-image {
          width: 100%;
          height: 100%;
        }

        .image-placeholder-sm {
          width: 100%;
          height: 100%;
          background: #f1f5f9;
          display: flex;
          align-items: center;
          justify-content: center;
          color: #94a3b8;
          font-size: 18px;
        }
      }

      .tour-option-info {
        flex: 1;
        min-width: 0;

        .tour-option-title {
          font-size: 13px;
          font-weight: 500;
          color: #1e293b;
          margin-bottom: 4px;
          overflow: hidden;
          text-overflow: ellipsis;
          white-space: nowrap;
        }

        .tour-option-meta {
          display: flex;
          align-items: center;
          gap: 8px;
          font-size: 12px;
          color: #64748b;

          span:last-child {
            color: #ef4444;
          }
        }
      }

      .tour-option-check {
        position: absolute;
        right: 8px;
        top: 50%;
        transform: translateY(-50%);
        width: 24px;
        height: 24px;
        background: #3b82f6;
        color: #fff;
        border-radius: 50%;
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 14px;
      }
    }

    .no-data {
      text-align: center;
      padding: 40px;
      color: #94a3b8;
    }

    :deep(.dialog-footer) {
      display: flex;
      align-items: center;
      justify-content: flex-end;
      gap: 12px;

      .selected-count {
        margin-right: auto;
        color: #64748b;
        font-size: 14px;
      }
    }
  }
}
</style>
