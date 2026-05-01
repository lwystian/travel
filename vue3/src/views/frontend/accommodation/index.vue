<template>
  <div class="accommodation-frontend-container">
    <!-- 顶部 Hero 区：背景图 + 标题 + 搜索框 -->
    <div class="hero-section">
      <div class="hero-overlay"></div>
      <!-- 装饰性波浪分隔线 -->
      <div class="hero-wave-divider"></div>
      <div class="hero-content">
        <div class="hero-text">
          <h1 class="hero-title">寻一处舒心住所，让旅途更安然</h1>
          <p class="hero-subtitle">精选优质酒店、民宿、度假村，为你的每一段旅程保驾护航</p>
        </div>

        <!-- 集成搜索栏 -->
        <div class="hero-search">
          <div class="search-field main-field">
            <el-icon class="field-icon"><Search /></el-icon>
            <el-input
              v-model="searchForm.name"
              placeholder="搜索住宿名称、地址或目的地"
              :prefix-icon="null"
              size="large"
              class="borderless-input"
              @keyup.enter="handleSearch"
            />
          </div>
          <div class="field-divider"></div>
          <div class="search-field">
            <el-icon class="field-icon"><MapLocation /></el-icon>
            <el-select
              v-model="filters.scenicId"
              placeholder="选择目的地景点"
              clearable
              size="large"
              class="borderless-select"
            >
              <el-option
                v-for="item in scenicOptions"
                :key="item.id"
                :label="item.name"
                :value="item.id"
              />
            </el-select>
          </div>
          <button class="hero-search-btn" @click="handleSearch">
            <el-icon><Search /></el-icon>
            <span>搜索</span>
          </button>
        </div>
      </div>
    </div>

    <!-- 主内容区 -->
    <div class="main-section" id="main-content">
      <div class="section-container">
        <!-- 筛选标签栏 -->
        <div class="filter-bar">
          <!-- 住宿类型 -->
          <div class="filter-line">
            <span class="filter-label">住宿类型</span>
            <div class="chip-list">
              <span
                class="chip"
                :class="{ active: !filters.type }"
                @click="setType('')"
              >
                全部
              </span>
              <span
                v-for="item in typeOptions"
                :key="item"
                class="chip"
                :class="{ active: filters.type === item }"
                @click="setType(item)"
              >
                {{ item }}
              </span>
            </div>
          </div>

          <!-- 价格区间 -->
          <div class="filter-line">
            <span class="filter-label">价格区间</span>
            <div class="chip-list">
              <span
                v-for="(p, idx) in priceRanges"
                :key="idx"
                class="chip"
                :class="{ active: activePriceIdx === idx }"
                @click="setPriceRange(idx)"
              >
                {{ p.label }}
              </span>
              <div class="price-custom">
                <input
                  v-model="filters.minPrice"
                  type="number"
                  class="mini-input"
                  placeholder="最低"
                />
                <span class="mini-sep">—</span>
                <input
                  v-model="filters.maxPrice"
                  type="number"
                  class="mini-input"
                  placeholder="最高"
                />
                <span class="mini-btn" @click="handleSearch">确定</span>
              </div>
            </div>
          </div>

          <!-- 评分 -->
          <div class="filter-line">
            <span class="filter-label">最低评分</span>
            <div class="chip-list">
              <span
                v-for="r in [0, 3, 4, 4.5, 5]"
                :key="r"
                class="chip"
                :class="{ active: filters.minRating === r }"
                @click="setRating(r)"
              >
                <template v-if="r === 0">不限</template>
                <template v-else>
                  <el-icon class="chip-star"><Star /></el-icon>
                  {{ r }}+
                </template>
              </span>
              <span class="reset-link" @click="resetSearch">
                <el-icon><Refresh /></el-icon>
                清空筛选
              </span>
            </div>
          </div>
        </div>

        <!-- 结果统计与排序 -->
        <div class="result-bar">
          <div class="result-count">
            为你找到
            <strong>{{ total }}</strong>
            处优质住宿
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

        <!-- 列表 -->
        <div v-if="loading" class="loading-state">
          <el-skeleton :rows="8" animated />
        </div>

        <div
          v-else-if="accommodationList && accommodationList.length > 0"
          class="accommodation-grid"
        >
          <div
            v-for="item in accommodationList"
            :key="item.id"
            class="accommodation-card"
            @click="goToDetail(item.id)"
          >
            <div class="card-image">
              <img :src="getImageUrl(item.imageUrl)" :alt="item.name" />
              <div class="card-badges">
                <span v-if="item.type" class="badge">{{ item.type }}</span>
              </div>
              <div class="card-rating">
                <el-icon><Star /></el-icon>
                <span>{{ getDisplayRating(item.starLevel) }}</span>
              </div>
            </div>
            <div class="card-content">
              <h3 class="accommodation-name">{{ item.name }}</h3>
              <div class="accommodation-location">
                <el-icon><Location /></el-icon>
                <span>{{ item.address || '地址待更新' }}</span>
              </div>
              <div v-if="item.scenicName" class="accommodation-scenic">
                <el-icon><MapLocation /></el-icon>
                <span>靠近 {{ item.scenicName }}</span>
                <span v-if="item.distance" class="distance">{{ item.distance }}</span>
              </div>
              <p class="accommodation-features">
                {{ truncateText(item.features || '舒适便捷的住宿环境，为您提供优质的服务体验', 60) }}
              </p>
              <div class="card-footer">
                <div class="price-block">
                  <span class="price-label">参考价</span>
                  <span class="price-value">{{ item.priceRange || '价格面议' }}</span>
                </div>
                <button class="detail-btn" @click.stop="goToDetail(item.id)">
                  查看详情
                </button>
              </div>
            </div>
          </div>
        </div>

        <div v-else class="empty-state">
          <div class="empty-illustration">
            <el-icon :size="48"><HomeFilled /></el-icon>
          </div>
          <h3 class="empty-title">暂无符合条件的住宿</h3>
          <p class="empty-desc">试试调整筛选条件，或浏览其他热门目的地</p>
          <button class="empty-action" @click="resetSearch">重新搜索</button>
        </div>

        <!-- 分页 -->
        <div class="pagination-wrapper" v-if="total > 0">
          <el-pagination
            background
            layout="total, prev, pager, next, jumper"
            :current-page="currentPage"
            :page-size="pageSize"
            :total="total"
            @current-change="handleCurrentChange"
            @size-change="handleSizeChange"
            class="modern-pagination"
          />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import request from '@/utils/request'
import {
  Location,
  Star,
  Search,
  Refresh,
  MapLocation,
  HomeFilled
} from '@element-plus/icons-vue'

const router = useRouter()
const baseAPI = process.env.VUE_APP_BASE_API || '/api'

// 数据状态
const loading = ref(false)
const accommodationList = ref([])
const currentPage = ref(1)
const pageSize = ref(12)
const total = ref(0)
const scenicOptions = ref([])
const typeOptions = ref([])
const currentSort = ref('recommend')

// 搜索表单
const searchForm = reactive({
  name: ''
})

// 筛选条件
const filters = reactive({
  scenicId: '',
  type: '',
  minPrice: '',
  maxPrice: '',
  minRating: 0
})

// 价格预设
const priceRanges = [
  { label: '不限', min: '', max: '' },
  { label: '¥200以下', min: '', max: 200 },
  { label: '¥200-500', min: 200, max: 500 },
  { label: '¥500-1000', min: 500, max: 1000 },
  { label: '¥1000以上', min: 1000, max: '' }
]

// 排序选项
const sortOptions = [
  { label: '推荐排序', value: 'recommend' },
  { label: '评分最高', value: 'rating' },
  { label: '价格从低到高', value: 'price_asc' },
  { label: '价格从高到低', value: 'price_desc' }
]

// 当前激活的价格档
const activePriceIdx = computed(() => {
  return priceRanges.findIndex(
    p =>
      String(p.min) === String(filters.minPrice) &&
      String(p.max) === String(filters.maxPrice)
  )
})

// 设置筛选项
const setType = type => {
  filters.type = type
  handleSearch()
}
const setPriceRange = idx => {
  const p = priceRanges[idx]
  filters.minPrice = p.min
  filters.maxPrice = p.max
  handleSearch()
}
const setRating = r => {
  filters.minRating = r
  handleSearch()
}

// 获取住宿列表
const fetchAccommodations = async () => {
  loading.value = true
  try {
    const params = {
      currentPage: currentPage.value,
      size: pageSize.value
    }
    if (searchForm.name) params.name = searchForm.name
    if (filters.scenicId) params.scenicId = filters.scenicId
    if (filters.type) params.type = filters.type
    if (filters.minPrice) params.minPrice = filters.minPrice
    if (filters.maxPrice) params.maxPrice = filters.maxPrice
    if (filters.minRating > 0) params.minRating = filters.minRating

    await request.get('/accommodation/page', params, {
      onSuccess: res => {
        accommodationList.value = res.records || []
        total.value = res.total || 0
      }
    })
  } catch (error) {
    console.error('获取住宿列表失败:', error)
  } finally {
    loading.value = false
  }
}

const fetchScenicOptions = async () => {
  try {
    await request.get('/scenic/all', {}, {
      onSuccess: res => {
        scenicOptions.value = res || []
      }
    })
  } catch (error) {
    console.error('获取景点列表失败:', error)
  }
}

const fetchAccommodationTypes = async () => {
  try {
    await request.get('/accommodation/types', {}, {
      onSuccess: res => {
        typeOptions.value = res || []
      }
    })
  } catch (error) {
    console.error('获取住宿类型列表失败:', error)
  }
}

const getImageUrl = url => {
  if (!url) return require('@/assets/images/no-image.png')
  if (url.startsWith('http')) return url
  return baseAPI + url
}

const handleSearch = () => {
  currentPage.value = 1
  fetchAccommodations()
  nextTick(() => {
    document.querySelector('#main-content')?.scrollIntoView({ behavior: 'smooth' })
  })
}

const resetSearch = () => {
  searchForm.name = ''
  filters.scenicId = ''
  filters.type = ''
  filters.minPrice = ''
  filters.maxPrice = ''
  filters.minRating = 0
  currentPage.value = 1
  fetchAccommodations()
}

const handleSizeChange = size => {
  pageSize.value = size
  fetchAccommodations()
}

const handleCurrentChange = page => {
  currentPage.value = page
  fetchAccommodations()
}

const goToDetail = id => {
  router.push(`/accommodation/${id}`)
}

const truncateText = (text, length) => {
  if (!text) return ''
  return text.length > length ? text.substring(0, length) + '...' : text
}

const getDisplayRating = rating => {
  if (!rating) return '4.5'
  return parseFloat(rating).toFixed(1)
}

onMounted(() => {
  fetchScenicOptions()
  fetchAccommodationTypes()
  fetchAccommodations()
})
</script>

<style lang="scss" scoped>
$primary: #ff8a00;       // 暖橙主色
$primary-dark: #f57600;
$text-main: #1f2937;
$text-sub: #6b7280;
$text-light: #9ca3af;
$bg: #f7f8fa;
$border: #ececec;

.accommodation-frontend-container {
  min-height: 100vh;
  background: $bg;
  font-family: "PingFang SC", "Microsoft YaHei", "Source Han Sans", sans-serif;
  color: $text-main;
}

/* ============== Hero 区 ============== */
.hero-section {
  position: relative;
  height: calc(100vh - 180px);
  min-height: 380px;
  max-height: 600px;
  background: url('https://images.unsplash.com/photo-1542314831-068cd1dbfeeb?w=1920&q=80')
    center / cover no-repeat;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

.hero-overlay {
  position: absolute;
  inset: 0;
  background: linear-gradient(
    180deg,
    rgba(0, 0, 0, 0.25) 0%,
    rgba(0, 0, 0, 0.55) 100%
  );
}

// 新增：装饰性波浪分隔线
.hero-wave-divider {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 35px;
  z-index: 2;
  overflow: hidden;
  
  &::before {
    content: '';
    position: absolute;
    bottom: 0;
    left: 50%;
    transform: translateX(-50%);
    width: 200%;
    height: 50px;
    background: $bg;
    border-radius: 50% 50% 0 0 / 25px 25px 0 0;
  }
}

.hero-content {
  position: relative;
  z-index: 2;
  width: 100%;
  max-width: 1200px;
  height: 100%;
  padding: 0 20px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  margin-top: -60px;
}

.hero-text {
  text-align: center;
  color: #fff;
  margin-bottom: 36px;
}

.hero-title {
  font-size: 40px;
  font-weight: 700;
  margin: 0 0 12px;
  letter-spacing: 1px;
  text-shadow: 0 2px 12px rgba(0, 0, 0, 0.3);
}

.hero-subtitle {
  font-size: 16px;
  color: rgba(255, 255, 255, 0.9);
  margin: 0;
}

/* 集成式搜索栏 */
.hero-search {
  display: flex;
  align-items: center;
  background: #fff;
  border-radius: 999px;
  padding: 8px 8px 8px 24px;
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.18);
  max-width: 1000px;
  margin: 0 auto;
}

.search-field {
  display: flex;
  align-items: center;
  gap: 10px;
  flex: 1;
  min-width: 0;
  padding: 6px 12px;

  &.main-field {
    flex: 1.4;
  }

  .field-icon {
    color: $primary;
    font-size: 18px;
    flex-shrink: 0;
  }
}

.field-divider {
  width: 1px;
  height: 28px;
  background: $border;
  flex-shrink: 0;
}

:deep(.borderless-input .el-input__wrapper),
:deep(.borderless-select .el-select__wrapper) {
  box-shadow: none !important;
  background: transparent !important;
  padding: 0 !important;
}

:deep(.borderless-input .el-input__inner),
:deep(.borderless-select .el-select__placeholder) {
  font-size: 15px;
  color: $text-main;
}

.hero-search-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  height: 48px;
  padding: 0 32px;
  background: $primary;
  color: #fff;
  border: none;
  border-radius: 999px;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.25s ease;
  flex-shrink: 0;

  &:hover {
    background: $primary-dark;
  }

  .el-icon {
    font-size: 16px;
  }
}

/* ============== 主内容区 ============== */
.main-section {
  position: relative;
  z-index: 3;
  padding-top: 12px;
}

.section-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 20px 60px;
}

/* 筛选条 */
.filter-bar {
  background: #fff;
  border-radius: 16px;
  padding: 8px 24px;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.06);
  margin-bottom: 24px;
  border: 1px solid rgba(0, 0, 0, 0.04); // 微妙的边框
}

.filter-line {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 14px 0;
  border-bottom: 1px dashed #f0f0f0;

  &:last-child {
    border-bottom: none;
  }
}

.filter-label {
  flex-shrink: 0;
  width: 70px;
  color: $text-sub;
  font-size: 14px;
}

.chip-list {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px 8px;
  flex: 1;
}

.chip {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 5px 14px;
  border-radius: 4px;
  font-size: 13px;
  color: $text-main;
  cursor: pointer;
  user-select: none;
  transition: all 0.2s ease;

  &:hover {
    color: $primary;
  }

  &.active {
    background: $primary;
    color: #fff;
  }

  .chip-star {
    color: #fbbf24;
    font-size: 13px;
  }

  &.active .chip-star {
    color: #fff;
  }
}

.price-custom {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  margin-left: 12px;
}

.mini-input {
  width: 80px;
  height: 28px;
  padding: 0 8px;
  border: 1px solid $border;
  border-radius: 4px;
  font-size: 13px;
  outline: none;
  transition: border-color 0.2s ease;

  &:focus {
    border-color: $primary;
  }
}

.mini-sep {
  color: $text-light;
}

.mini-btn {
  padding: 4px 12px;
  font-size: 13px;
  color: $primary;
  cursor: pointer;
  border: 1px solid $primary;
  border-radius: 4px;
  transition: all 0.2s ease;

  &:hover {
    background: $primary;
    color: #fff;
  }
}

.reset-link {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  margin-left: auto;
  font-size: 13px;
  color: $text-sub;
  cursor: pointer;
  transition: color 0.2s ease;

  &:hover {
    color: $primary;
  }
}

/* 结果条 */
.result-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 4px;
  margin-bottom: 20px;
}

.result-count {
  font-size: 14px;
  color: $text-sub;

  strong {
    color: $primary;
    font-size: 18px;
    font-weight: 700;
    margin: 0 4px;
  }
}

.sort-tabs {
  display: flex;
  align-items: center;
  gap: 4px;
  background: #fff;
  border-radius: 8px;
  padding: 4px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.sort-tab {
  padding: 6px 14px;
  font-size: 13px;
  color: $text-sub;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s ease;

  &:hover {
    color: $primary;
  }

  &.active {
    background: rgba(255, 138, 0, 0.1);
    color: $primary;
    font-weight: 600;
  }
}

/* ============== 卡片网格 ============== */
.loading-state {
  background: #fff;
  border-radius: 12px;
  padding: 40px;
}

.accommodation-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(290px, 1fr));
  gap: 22px;
  margin-bottom: 40px;
}

.accommodation-card {
  background: #fff;
  border-radius: 12px;
  overflow: hidden;
  cursor: pointer;
  transition: transform 0.3s ease, box-shadow 0.3s ease;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);

  &:hover {
    transform: translateY(-6px);
    box-shadow: 0 14px 40px rgba(0, 0, 0, 0.12);

    .card-image img {
      transform: scale(1.06);
    }
  }
}

.card-image {
  position: relative;
  height: 200px;
  overflow: hidden;
  background: #f0f0f0;

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
    transition: transform 0.5s ease;
    display: block;
  }
}

.card-badges {
  position: absolute;
  top: 12px;
  left: 12px;
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.badge {
  padding: 4px 10px;
  background: rgba(255, 255, 255, 0.95);
  color: $primary;
  font-size: 12px;
  font-weight: 600;
  border-radius: 4px;
  backdrop-filter: blur(6px);
}

.card-rating {
  position: absolute;
  bottom: 12px;
  right: 12px;
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 4px 10px;
  background: rgba(0, 0, 0, 0.55);
  color: #fff;
  font-size: 13px;
  font-weight: 600;
  border-radius: 4px;
  backdrop-filter: blur(6px);

  .el-icon {
    color: #fbbf24;
    font-size: 14px;
  }
}

.card-content {
  padding: 18px 20px 20px;
}

.accommodation-name {
  font-size: 17px;
  font-weight: 700;
  color: $text-main;
  margin: 0 0 12px;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 1;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.accommodation-location,
.accommodation-scenic {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 13px;
  color: $text-sub;
  margin-bottom: 6px;
  line-height: 1.5;

  .el-icon {
    color: $primary;
    font-size: 14px;
    flex-shrink: 0;
  }

  span {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .distance {
    color: #10b981;
    font-weight: 600;
    margin-left: 4px;
    white-space: nowrap;
  }
}

.accommodation-features {
  font-size: 13px;
  color: $text-light;
  line-height: 1.6;
  margin: 10px 0 14px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.card-footer {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  padding-top: 14px;
  border-top: 1px solid #f5f5f5;
}

.price-block {
  display: flex;
  flex-direction: column;
  gap: 2px;

  .price-label {
    font-size: 12px;
    color: $text-light;
  }

  .price-value {
    font-size: 18px;
    font-weight: 700;
    color: $primary;
  }
}

.detail-btn {
  padding: 8px 18px;
  background: #fff;
  color: $primary;
  border: 1px solid $primary;
  border-radius: 6px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.25s ease;

  &:hover {
    background: $primary;
    color: #fff;
  }
}

/* ============== 空状态 ============== */
.empty-state {
  text-align: center;
  padding: 80px 20px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
}

.empty-illustration {
  width: 96px;
  height: 96px;
  margin: 0 auto 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 138, 0, 0.08);
  border-radius: 50%;
  color: $primary;
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
  margin: 0 0 24px;
}

.empty-action {
  padding: 10px 28px;
  background: $primary;
  color: #fff;
  border: none;
  border-radius: 999px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.25s ease;

  &:hover {
    background: $primary-dark;
  }
}

/* ============== 分页 ============== */
.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 40px;
}

:deep(.modern-pagination) {
  .el-pager li {
    border-radius: 6px;
    margin: 0 4px;

    &.is-active {
      background: $primary !important;
      color: #fff;
    }
  }

  .btn-prev,
  .btn-next {
    border-radius: 6px;
  }
}

/* ============== 响应式 ============== */
@media (max-width: 992px) {
  .hero-section {
    height: 420px;
  }
  .hero-title {
    font-size: 30px;
  }
  .hero-wave-divider {
    height: 30px;
  }
  .hero-search {
    flex-direction: column;
    border-radius: 16px;
    padding: 16px;
    gap: 8px;
  }
  .field-divider {
    display: none;
  }
  .search-field {
    width: 100%;
    border: 1px solid $border;
    border-radius: 10px;
    padding: 8px 12px;
  }
  .hero-search-btn {
    width: 100%;
    border-radius: 10px;
  }
}

@media (max-width: 640px) {
  .hero-section {
    height: 380px;
  }
  .hero-title {
    font-size: 24px;
  }
  .hero-subtitle {
    font-size: 13px;
  }
  .hero-wave-divider {
    height: 25px;
  }
  .filter-line {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }
  .filter-label {
    width: auto;
  }
  .result-bar {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }
  .accommodation-grid {
    grid-template-columns: 1fr;
  }
}
</style>