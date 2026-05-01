<!-- eslint-disable -->
<template>
  <div class="scenic-frontend-container">
    <!-- Hero 区域 - 背景图片适中 -->
    <div class="hero-section" ref="heroSection">
      <div class="hero-overlay"></div>
      <div class="hero-content">
        <div class="hero-text">
          <h1 class="hero-title">探索精彩景点</h1>
          <p class="hero-subtitle">发现世界各地的美丽风景和文化遗产</p>
        </div>
        <div class="hero-search">
          <div class="search-field main-field">
            <el-icon class="field-icon"><Search /></el-icon>
            <input
              v-model="searchForm.name"
              type="text"
              class="search-input"
              placeholder="搜索景点名称..."
              @keyup.enter="handleSearch"
            />
          </div>
          <button class="hero-search-btn" @click="handleSearch">
            <el-icon><Search /></el-icon>
            <span>搜索</span>
          </button>
        </div>
      </div>
    </div>

    <!-- 主内容区 -->
    <div class="main-section" ref="mainSection">
      <div class="section-container">
        <!-- 分类标签栏 -->
        <div class="category-bar">
          <div class="category-wrapper">
            <span class="category-label">热门分类</span>
            <div class="category-list">
              <span
                v-for="category in categoryList"
                :key="category.id"
                class="category-chip"
                :class="{ active: searchForm.categoryId === category.id }"
                @click="handleCategoryChange(category.id)"
              >
                {{ category.name }}
                <span class="chip-count" v-if="category.count">({{ category.count }})</span>
              </span>
            </div>
          </div>
          <div class="reset-btn" @click="resetSearch">
            <el-icon><Refresh /></el-icon>
            <span>重置筛选</span>
          </div>
        </div>

        <!-- 结果统计与排序 -->
        <div class="result-bar">
          <div class="result-count">
            发现 <strong>{{ total }}</strong> 个精彩景点
          </div>
          <div class="sort-tabs">
            <span
              v-for="s in sortOptions"
              :key="s.value"
              class="sort-tab"
              :class="{ active: currentSort === s.value }"
              @click="currentSort = s.value"
            >
              {{ s.label }}
            </span>
          </div>
        </div>

        <!-- 景点列表 - 横版大卡片 -->
        <div v-if="loading" class="loading-state">
          <div class="skeleton-list">
            <div v-for="i in 5" :key="i" class="skeleton-card-horizontal">
              <div class="skeleton-img-horizontal"></div>
              <div class="skeleton-content-horizontal">
                <div class="skeleton-title"></div>
                <div class="skeleton-line"></div>
                <div class="skeleton-line short"></div>
                <div class="skeleton-footer"></div>
              </div>
            </div>
          </div>
        </div>

        <div v-else-if="tableData && tableData.length > 0" class="scenic-list">
          <div
            v-for="(item, index) in tableData"
            :key="item.id"
            class="scenic-card-horizontal"
            @click="goDetail(item.id)"
          >
            <!-- 左侧图片区域 - 固定宽高 -->
            <div class="card-image-horizontal">
              <img :src="getImageUrl(item.imageUrl)" :alt="item.name" />
              <div class="rating-badge-horizontal">
                <el-icon><Star /></el-icon>
                <span>{{ getDisplayRating(item.rating) }}</span>
              </div>
            </div>
            
            <!-- 右侧内容区域 - 左对齐 -->
            <div class="card-info-horizontal">
              <div class="info-header">
                <h3 class="scenic-name-horizontal">{{ item.name }}</h3>
                <div class="category-tag">{{ item.categoryInfo?.name || '景点' }}</div>
              </div>
              
              <div class="location-info">
                <el-icon><Location /></el-icon>
                <span>{{ item.location || '未知地区' }}</span>
              </div>
              
              <p class="description">{{ truncateText(item.description, 100) }}</p>
              
              <div class="feature-tags">
                <span
                  v-for="tag in (item.tags || [])"
                  :key="tag.id"
                  class="feature-tag"
                  :style="getTagStyle(tag)"
                >
                  {{ tag.name }}
                </span>
                <span v-if="!item.tags || item.tags.length === 0" class="no-tags">暂无标签</span>
              </div>
              
              <div class="card-footer-horizontal">
                <div class="stats">
                  <div class="stat">
                    <el-icon><ChatDotRound /></el-icon>
                    <span>{{ formatReviewCount(item.reviewCount) }}</span>
                  </div>
                </div>
                <button class="book-btn">查看详情 →</button>
              </div>
            </div>
          </div>
        </div>

        <!-- 空状态 -->
        <div v-else class="empty-state">
          <div class="empty-icon">🏔️</div>
          <h3 class="empty-title">暂无景点信息</h3>
          <p class="empty-desc">试试调整搜索条件或浏览其他分类</p>
          <button class="empty-action" @click="resetSearch">重新探索</button>
        </div>

        <!-- 分页 -->
        <div class="pagination-wrapper" v-if="total > 0">
          <el-pagination
            background
            layout="prev, pager, next"
            :current-page="currentPage"
            :page-size="pageSize"
            :total="total"
            @current-change="handleCurrentChange"
            class="modern-pagination"
          />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, watch, nextTick } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import request from '@/utils/request'
import { useUserStore } from '@/store/user'
import { Search, Location, Refresh, Star, ChatDotRound } from '@element-plus/icons-vue'

const baseAPI = process.env.VUE_APP_BASE_API || '/api'
const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

// DOM 引用
const heroSection = ref(null)
const mainSection = ref(null)

// 数据状态
const tableData = ref([])
const categoryList = ref([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(8)
const total = ref(0)
const hasUserSearched = ref(false) // 追踪用户是否主动搜索

// 搜索表单
const searchForm = reactive({
  name: '',
  categoryId: null
})

// 排序
const currentSort = ref('recommend')
const sortOptions = [
  { label: '推荐排序', value: 'recommend' },
  { label: '评分最高', value: 'rating' }
]

// 滚动到内容区域
const scrollToContent = () => {
  nextTick(() => {
    if (hasUserSearched.value && mainSection.value) {
      window.scrollTo({
        top: mainSection.value.offsetTop,
        behavior: 'smooth'
      })
    }
  })
}

// 获取分类列表
const fetchCategories = async () => {
  try {
    await request.get('/scenic-category/tree', {}, {
      onSuccess: (res) => {
        categoryList.value = res || []
      }
    })
  } catch (error) {
    console.error('获取分类列表失败:', error)
    categoryList.value = []
  }
}

// 获取景点列表
const fetchScenicSpots = async () => {
  loading.value = true
  try {
    const params = {
      name: searchForm.name,
      categoryId: searchForm.categoryId,
      currentPage: currentPage.value,
      size: pageSize.value
    }

    // 排序处理
    if (currentSort.value === 'rating') {
      params.orderBy = 'rating'
      params.order = 'desc'
    }

    await request.get('/scenic/page', params, {
      onSuccess: (res) => {
        tableData.value = res.records || []
        total.value = res.total || 0
        scrollToContent()
      }
    })
  } catch (error) {
    console.error('获取景点列表失败:', error)
    tableData.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

// 处理URL参数
const handleUrlParams = () => {
  const searchParam = route.query.search
  const categoryParam = route.params.categoryId

  if (searchParam) {
    searchForm.name = decodeURIComponent(searchParam)
    hasUserSearched.value = true
  }
  if (categoryParam) {
    searchForm.categoryId = parseInt(categoryParam)
    hasUserSearched.value = true
  }
  currentPage.value = 1
  fetchScenicSpots()
}

// 监听路由变化
watch(() => route.query.search, () => handleUrlParams())
watch(() => route.params.categoryId, () => handleUrlParams())
watch(currentSort, () => {
  currentPage.value = 1
  fetchScenicSpots()
})

// 方法
const handleSearch = () => {
  hasUserSearched.value = true
  currentPage.value = 1
  fetchScenicSpots()
}

const resetSearch = () => {
  searchForm.name = ''
  searchForm.categoryId = null
  hasUserSearched.value = true
  currentPage.value = 1
  fetchScenicSpots()
}

const handleCurrentChange = (page) => {
  currentPage.value = page
  fetchScenicSpots()
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

const handleCategoryChange = (categoryId) => {
  if (searchForm.categoryId === categoryId) {
    searchForm.categoryId = null
  } else {
    searchForm.categoryId = categoryId
  }
  hasUserSearched.value = true
  currentPage.value = 1
  fetchScenicSpots()
}

const goDetail = (id) => {
  router.push(`/scenic/${id}`)
}

const getImageUrl = (url) => {
  if (!url) return 'https://picsum.photos/seed/scape/400/260'
  return url.startsWith('http') ? url : baseAPI + url
}

const truncateText = (text, length) => {
  if (!text) return ''
  return text.length > length ? text.substring(0, length) + '...' : text
}

const formatReviewCount = (count) => {
  if (!count || count === 0) return '暂无评价'
  if (count >= 10000) return (count / 10000).toFixed(1) + 'w'
  return count + '条评价'
}

const getDisplayRating = (rating) => {
  if (!rating) return '4.5'
  return parseFloat(rating).toFixed(1)
}

// 获取标签样式
const getTagStyle = (tag) => {
  const color = tag.color || '#FF6B35'
  const bgAlpha = 0.1
  return {
    backgroundColor: hexToRgba(color, bgAlpha),
    color: color,
    borderColor: hexToRgba(color, 0.3)
  }
}

// HEX转RGBA
const hexToRgba = (hex, alpha) => {
  if (!hex) return `rgba(255, 107, 53, ${alpha})`
  const r = parseInt(hex.slice(1, 3), 16)
  const g = parseInt(hex.slice(3, 5), 16)
  const b = parseInt(hex.slice(5, 7), 16)
  return `rgba(${r}, ${g}, ${b}, ${alpha})`
}

// 生命周期
onMounted(() => {
  fetchCategories()
  handleUrlParams()
})
</script>

<style lang="scss" scoped>
$primary: #FF6B35;
$primary-dark: #e55a2a;
$text-main: #1a1a2e;
$text-sub: #6c757d;
$text-light: #adb5bd;
$bg-light: #f8f9fa;
$border: #e9ecef;

.scenic-frontend-container {
  min-height: 100vh;
  background: $bg-light;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'PingFang SC', 'Microsoft YaHei', sans-serif;
}

/* ============== Hero 区域 - 背景图片占满当前页 ============== */
.hero-section {
  position: relative;
  width: 100%;
  height: calc(100vh - 215px);
  background: url('https://images.unsplash.com/photo-1464822759023-fed622ff2c3b?w=1920&q=80') center bottom / cover no-repeat;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

.hero-overlay {
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, rgba(0, 0, 0, 0.45) 0%, rgba(0, 0, 0, 0.55) 100%);
}

.hero-content {
  position: relative;
  z-index: 2;
  width: 100%;
  max-width: 700px;
  padding: 0 20px;
  text-align: center;
}

.hero-text {
  margin-bottom: 32px;
}

.hero-title {
  font-size: 42px;
  font-weight: 700;
  color: #fff;
  margin: 0 0 12px;
  text-shadow: 0 2px 12px rgba(0, 0, 0, 0.3);
}

.hero-subtitle {
  font-size: 16px;
  color: rgba(255, 255, 255, 0.9);
  margin: 0;
}

/* 搜索栏 - 更细长 */
.hero-search {
  display: flex;
  align-items: center;
  background: #fff;
  border-radius: 48px;
  padding: 4px 4px 4px 20px;
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.15);
  max-width: 520px;
  margin: 0 auto;
}

.search-field {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
  padding: 0 8px;

  .field-icon {
    color: $primary;
    font-size: 18px;
  }
}

.search-input {
  flex: 1;
  border: none;
  outline: none;
  font-size: 15px;
  padding: 12px 0;
  background: transparent;

  &::placeholder {
    color: $text-light;
    font-size: 14px;
  }
}

.hero-search-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 0 24px;
  height: 44px;
  background: $primary;
  color: #fff;
  border: none;
  border-radius: 44px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s;

  &:hover {
    background: $primary-dark;
    transform: scale(1.02);
  }
}

/* ============== 主内容区 ============== */
.main-section {
  padding: 40px 0 60px;
}

.section-container {
  max-width: 1300px;
  margin: 0 auto;
  padding: 0 20px;
}

/* 分类栏 */
.category-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 16px;
  margin-bottom: 28px;
  padding-bottom: 16px;
  border-bottom: 1px solid $border;
}

.category-wrapper {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 16px;
}

.category-label {
  font-size: 14px;
  font-weight: 600;
  color: $text-sub;
}

.category-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.category-chip {
  padding: 8px 18px;
  font-size: 14px;
  font-weight: 500;
  color: $text-sub;
  background: #fff;
  border-radius: 30px;
  cursor: pointer;
  transition: all 0.2s;
  border: 1px solid $border;

  &:hover {
    border-color: $primary;
    color: $primary;
  }

  &.active {
    background: $primary;
    border-color: $primary;
    color: #fff;
  }

  .chip-count {
    margin-left: 4px;
    opacity: 0.7;
  }
}

.reset-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 18px;
  font-size: 13px;
  color: $text-sub;
  background: #fff;
  border: 1px solid $border;
  border-radius: 30px;
  cursor: pointer;
  transition: all 0.2s;

  &:hover {
    border-color: $primary;
    color: $primary;
  }
}

/* 结果栏 */
.result-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 16px;
  margin-bottom: 28px;
}

.result-count {
  font-size: 14px;
  color: $text-sub;

  strong {
    color: $primary;
    font-size: 20px;
    font-weight: 700;
    margin: 0 4px;
  }
}

.sort-tabs {
  display: flex;
  gap: 8px;
}

.sort-tab {
  padding: 6px 18px;
  font-size: 13px;
  color: $text-sub;
  background: #fff;
  border-radius: 30px;
  cursor: pointer;
  transition: all 0.2s;
  border: 1px solid $border;

  &:hover {
    border-color: $primary;
    color: $primary;
  }

  &.active {
    background: $primary;
    border-color: $primary;
    color: #fff;
  }
}

/* 骨架屏 - 横版 */
.loading-state {
  min-height: 400px;
}

.skeleton-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.skeleton-card-horizontal {
  display: flex;
  background: #fff;
  border-radius: 20px;
  overflow: hidden;
}

.skeleton-img-horizontal {
  width: 320px;
  height: 220px;
  background: linear-gradient(90deg, #e9ecef 25%, #f8f9fa 50%, #e9ecef 75%);
  background-size: 200% 100%;
  animation: skeleton 1.5s infinite;
}

.skeleton-content-horizontal {
  flex: 1;
  padding: 20px;
}

.skeleton-title {
  height: 22px;
  width: 60%;
  background: #e9ecef;
  border-radius: 4px;
  margin-bottom: 12px;
}

.skeleton-line {
  height: 14px;
  background: #e9ecef;
  border-radius: 4px;
  margin-bottom: 8px;
}

.skeleton-line.short {
  width: 40%;
}

.skeleton-footer {
  margin-top: 20px;
  height: 32px;
  width: 30%;
  background: #e9ecef;
  border-radius: 20px;
}

@keyframes skeleton {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}

/* ============== 横版大卡片样式 ============== */
.scenic-list {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.scenic-card-horizontal {
  display: flex;
  background: #fff;
  border-radius: 20px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.06);

  &:hover {
    transform: translateY(-4px);
    box-shadow: 0 16px 32px rgba(0, 0, 0, 0.12);
    
    .card-image-horizontal img {
      transform: scale(1.05);
    }
  }
}

/* 左侧图片区域 - 固定宽高 */
.card-image-horizontal {
  position: relative;
  width: 320px;
  height: 220px;
  flex-shrink: 0;
  overflow: hidden;

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
    transition: transform 0.4s ease;
  }
}

.image-tags {
  position: absolute;
  top: 12px;
  left: 12px;
  display: flex;
  gap: 8px;
}

.tag {
  padding: 4px 10px;
  border-radius: 20px;
  font-size: 11px;
  font-weight: 600;
  backdrop-filter: blur(4px);
  
  &.free-tag {
    background: #10b981;
    color: #fff;
  }
  
  &.price-tag {
    background: rgba(0, 0, 0, 0.7);
    color: #fff;
  }
}

.rating-badge-horizontal {
  position: absolute;
  bottom: 12px;
  right: 12px;
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 4px 10px;
  background: rgba(0, 0, 0, 0.7);
  backdrop-filter: blur(4px);
  border-radius: 20px;
  color: #FFD700;
  font-size: 12px;
  font-weight: 600;

  .el-icon {
    font-size: 11px;
  }

  span {
    color: #fff;
  }
}

/* 右侧内容区域 - 左对齐 */
.card-info-horizontal {
  flex: 1;
  padding: 18px 24px;
  display: flex;
  flex-direction: column;
  text-align: left;
}

.info-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.scenic-name-horizontal {
  font-size: 18px;
  font-weight: 700;
  color: $text-main;
  margin: 0;
  text-align: left;
}

.category-tag {
  padding: 4px 12px;
  background: #f0f0f0;
  border-radius: 20px;
  font-size: 11px;
  color: #666;
}

.location-info {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: $text-sub;
  margin-bottom: 10px;

  .el-icon {
    color: $primary;
    font-size: 13px;
  }
}

.description {
  font-size: 13px;
  color: $text-sub;
  line-height: 1.5;
  margin: 0 0 14px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  text-align: left;
}

.feature-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 16px;
}

.feature-tag {
  display: inline-flex;
  align-items: center;
  padding: 2px 10px;
  font-size: 11px;
  color: #f60;
  background: #fff5f0;
  border-radius: 12px;
  border: 1px solid #ffe0c0;
  transition: all 0.25s ease;

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  }
}

.no-tags {
  padding: 5px 14px;
  background: #f5f5f5;
  border-radius: 20px;
  font-size: 12px;
  color: #999;
}

.card-footer-horizontal {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: auto;
}

.stats {
  display: flex;
  gap: 20px;
}

.stat {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 12px;
  color: $text-light;

  .el-icon {
    font-size: 13px;
  }
}

.book-btn {
  padding: 7px 24px;
  background: transparent;
  border: 1.5px solid $primary;
  border-radius: 30px;
  font-size: 12px;
  font-weight: 600;
  color: $primary;
  cursor: pointer;
  transition: all 0.3s;

  &:hover {
    background: $primary;
    color: #fff;
  }
}

/* 空状态 */
.empty-state {
  text-align: center;
  padding: 80px 20px;
  background: #fff;
  border-radius: 24px;
}

.empty-icon {
  font-size: 64px;
  margin-bottom: 20px;
  opacity: 0.6;
}

.empty-title {
  font-size: 20px;
  font-weight: 700;
  color: $text-main;
  margin: 0 0 8px;
}

.empty-desc {
  font-size: 14px;
  color: $text-sub;
  margin: 0 0 28px;
}

.empty-action {
  padding: 10px 32px;
  background: $primary;
  color: #fff;
  border: none;
  border-radius: 40px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s;

  &:hover {
    background: $primary-dark;
  }
}

/* 分页 */
.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 48px;
}

.modern-pagination {
  :deep(.el-pagination) {
    .el-pager li {
      border-radius: 40px;
      margin: 0 4px;
      transition: all 0.2s;

      &:hover {
        background: $primary;
        color: #fff;
      }

      &.is-active {
        background: $primary;
        color: #fff;
      }
    }

    .btn-prev, .btn-next {
      border-radius: 40px;

      &:hover {
        background: $primary;
        color: #fff;
      }
    }
  }
}

/* 响应式 */
@media (max-width: 992px) {
  .card-image-horizontal {
    width: 280px;
    height: 200px;
  }
  
  .scenic-name-horizontal {
    font-size: 17px;
  }
}

@media (max-width: 768px) {
  .hero-section {
    height: calc(100vh - 140px);
  }

  .hero-title {
    font-size: 28px;
  }

  .hero-subtitle {
    font-size: 13px;
  }

  .hero-search {
    flex-direction: column;
    border-radius: 28px;
    padding: 12px;
    gap: 10px;
    max-width: 90%;
  }

  .search-field {
    width: 100%;
    border: 1px solid $border;
    border-radius: 40px;
    padding: 6px 16px;
  }

  .hero-search-btn {
    width: 100%;
    justify-content: center;
  }

  .category-bar {
    flex-direction: column;
    align-items: flex-start;
  }

  .result-bar {
    flex-direction: column;
    align-items: flex-start;
  }

  .scenic-card-horizontal {
    flex-direction: column;
  }
  
  .card-image-horizontal {
    width: 100%;
    height: 200px;
  }
  
  .scenic-name-horizontal {
    font-size: 17px;
  }
  
  .card-footer-horizontal {
    flex-direction: column;
    gap: 12px;
    align-items: flex-start;
  }
  
  .book-btn {
    width: 100%;
    text-align: center;
  }
  
  .stats {
    gap: 16px;
  }
  
  .info-header {
    flex-wrap: wrap;
    gap: 8px;
  }
}
</style>