<template>
  <div class="travel-page">
    <!-- 顶部 Banner -->
    <div class="banner">
      <img
        class="banner-img"
        :src="guideHeroUrl"
        alt="banner"
      />
      <div class="banner-mask">
        <div class="banner-content">
          <h1 class="banner-title">探索旅行者分享的精彩旅游体验和实用建议</h1>
          <div class="search-bar">
            <input
              v-model="searchForm.title"
              type="text"
              class="search-input"
              placeholder="游记/旅行攻略/目的地"
              @keyup.enter="handleSearch"
            />
            <button class="search-btn" @click="handleSearch">搜索</button>
          </div>
        </div>
      </div>
    </div>

    <!-- 内容区域 -->
    <div class="content">
      <!-- Tab 切换 -->
      <div class="tab-bar">
        <div class="tabs">
          <div
            v-for="tab in tabs"
            :key="tab"
            class="tab"
            :class="{ active: currentTab === tab }"
            @click="handleTabChange(tab)"
          >
            {{ tab }}
          </div>
        </div>
        <div class="filter-btn" :class="{ active: filterVisible || activeFilterCount > 0 }" @click="openFilter">
          <span class="filter-icon">≡</span>
          <span>筛选{{ activeFilterCount ? `(${activeFilterCount})` : '' }}</span>
        </div>
      </div>

      <div v-if="filterVisible" class="filter-panel">
        <label class="filter-field">
          <span>目的地</span>
          <el-cascader
            v-model="filterForm.destinationPath"
            :options="regionOptions"
            :props="leafRegionCascaderProps"
            placeholder="留空不限，可搜索省 / 市 / 区县"
            clearable
            filterable
            class="region-cascader"
            popper-class="guide-region-popper"
            @change="applyFilters"
          />
        </label>
        <label class="filter-field">
          <span>作者身份</span>
          <select v-model="filterForm.authorRole">
            <option value="all">全部作者</option>
            <option value="official">官方发布</option>
            <option value="USER">用户发布</option>
          </select>
        </label>
        <label class="filter-field">
          <span>发布时间</span>
          <select v-model="filterForm.timeRange">
            <option value="">不限时间</option>
            <option value="week">最近一周</option>
            <option value="month">最近一月</option>
            <option value="quarter">最近三月</option>
            <option value="year">最近一年</option>
          </select>
        </label>
        <label class="filter-field">
          <span>阅读量</span>
          <select v-model="filterForm.minViews">
            <option value="">不限阅读</option>
            <option value="100">100+ 阅读</option>
            <option value="500">500+ 阅读</option>
            <option value="1000">1000+ 阅读</option>
            <option value="5000">5000+ 阅读</option>
          </select>
        </label>
        <div class="filter-actions">
          <button type="button" class="filter-reset" @click="resetFilters">重置</button>
          <button type="button" class="filter-apply" @click="applyFilters">应用筛选</button>
        </div>
      </div>

      <!-- 攻略列表 -->
      <div class="article-list" v-loading="loading">
        <!-- 空状态 -->
        <div class="empty-state" v-if="tableData.length === 0 && !loading">
          <div class="empty-icon">📝</div>
          <h3 class="empty-title">暂无攻略内容</h3>
          <p class="empty-desc">成为第一个分享旅游攻略的人吧！</p>
          <el-button type="primary" @click="goEdit" class="empty-action">
            <el-icon><Edit /></el-icon>
            发布攻略
          </el-button>
        </div>

        <!-- 攻略列表 -->
        <div v-for="item in tableData" :key="item.id" class="article-item" @click="goDetail(item.id)">
          <div class="article-cover">
            <img :src="getImageUrl(item.coverImage)" :alt="item.title" loading="lazy" decoding="async" />
            <span v-if="item.isEssence" class="essence-tag">精华游记</span>
          </div>
          <div class="article-info">
            <div class="article-header">
              <h3 class="article-title">{{ item.title }}</h3>
              <span v-if="item.isApp" class="app-tag">APP</span>
            </div>
            <p class="article-desc">{{ truncateDesc(item.content) }}</p>
            <div class="article-meta">
              <div class="meta-item">
                <span class="icon-location">⊙</span>
                <span class="meta-text">{{ getDestinationLabel(item.destination) }}</span>
              </div>
              <div class="meta-item">
                <img :src="getImageUrl(item.userAvatar)" class="avatar" :alt="item.userNickname" loading="lazy" decoding="async" />
                <span class="meta-text author">{{ item.userNickname || '旅行者' + item.userId }}</span>
                <span v-if="officialBadge(item)" class="identity-badge" :class="officialBadge(item).className">
                  {{ officialBadge(item).label }}
                </span>
              </div>
              <div class="meta-item">
                <span class="icon-view">◉</span>
                <span class="meta-text">{{ formatNumber(item.views) }}阅读</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 分页 -->
      <div class="pagination-section" v-if="total > 0">
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

    <!-- 发布攻略悬浮按钮 -->
    <div class="float-publish-btn" @click="goEdit">
      <el-icon><Edit /></el-icon>
      <span>发布攻略</span>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch, nextTick } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import request from '@/utils/request'
import { Edit } from '@element-plus/icons-vue'
import noImage from '@/assets/images/no-image.png'
import { useSiteAssets, getAssetUrl } from '@/utils/siteAssets'
import { resolveImageUrl } from '@/utils/imageUrl'
import { chinaRegionOptions, leafRegionCascaderProps, getRegionKeyword } from '@/utils/chinaRegion'

const regionOptions = chinaRegionOptions

// 获取目的地标签
const getDestinationLabel = (dest) => {
  if (!dest) return '未知目的地'
  return dest || '未知目的地'
}

const router = useRouter()
const route = useRoute()
const { siteAssets, loadSiteAssets } = useSiteAssets()
const guideHeroUrl = computed(() => getAssetUrl(siteAssets.value.guideHeroUrl, noImage))

// 数据
const tableData = ref([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const filterVisible = ref(false)

// 搜索表单
const searchForm = reactive({
  title: ''
})

const filterForm = reactive({
  destinationPath: [],
  authorRole: 'all',
  timeRange: '',
  minViews: ''
})

// Tab 切换
const tabs = ['热门游记', '推荐游记', '最新游记']
const currentTab = ref('热门游记')

const sortModeMap = {
  热门游记: 'hot',
  推荐游记: 'recommended',
  最新游记: 'latest'
}

const activeFilterCount = computed(() => {
  let count = 0
  if (filterForm.destinationPath?.length) count++
  if (filterForm.authorRole && filterForm.authorRole !== 'all') count++
  if (filterForm.timeRange) count++
  if (filterForm.minViews) count++
  return count
})

// 获取图片完整URL
const getImageUrl = (url) => {
  return resolveImageUrl(url, noImage)
}

// 格式化数字
const formatNumber = (num) => {
  if (!num) return '0'
  if (num >= 10000) {
    return (num / 10000).toFixed(1) + 'w'
  }
  return num.toString()
}

const officialBadge = (item) => {
  if (item?.userRoleCode === 'SUPER_ADMIN') {
    return { label: '官方', className: 'super-admin' }
  }
  if (item?.userRoleCode === 'ADMIN') {
    return { label: '管理员', className: 'admin' }
  }
  return null
}

// 截取描述内容
const truncateDesc = (content) => {
  if (!content) return '暂无描述'
  // 去除HTML标签
  const text = content.replace(/<[^>]*>/g, '')
  if (text.length <= 120) return text
  return text.substring(0, 120) + '...'
}

// 获取攻略列表
const fetchGuides = async () => {
  loading.value = true
  try {
    await request.get('/guide/page', {
      title: searchForm.title || undefined,
      sortMode: sortModeMap[currentTab.value] || 'hot',
      destination: getRegionKeyword(filterForm.destinationPath) || undefined,
      authorRole: filterForm.authorRole !== 'all' ? filterForm.authorRole : undefined,
      timeRange: filterForm.timeRange || undefined,
      minViews: filterForm.minViews ? Number(filterForm.minViews) : undefined,
      currentPage: currentPage.value,
      size: pageSize.value
    }, {
      showDefaultMsg: false,
      onSuccess: (res) => {
        tableData.value = res.records || []
        total.value = res.total || 0
      }
    })
  } catch (error) {
    console.error('获取攻略列表失败', error)
  } finally {
    loading.value = false
  }
}

// 搜索
const handleSearch = () => {
  currentPage.value = 1
  fetchGuides()
  nextTick(() => {
    document.querySelector('.content')?.scrollIntoView({ behavior: 'smooth' })
  })
}

// Tab切换
const handleTabChange = (tab) => {
  currentTab.value = tab
  currentPage.value = 1
  fetchGuides()
}

// 打开筛选
const openFilter = () => {
  filterVisible.value = !filterVisible.value
}

const applyFilters = () => {
  currentPage.value = 1
  fetchGuides()
}

const resetFilters = () => {
  filterForm.destinationPath = []
  filterForm.authorRole = 'all'
  filterForm.timeRange = ''
  filterForm.minViews = ''
  currentPage.value = 1
  fetchGuides()
}

// 分页变化
const handleCurrentChange = (page) => {
  currentPage.value = page
  fetchGuides()
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

// 跳转详情
const goDetail = (id) => {
  router.push(`/guide/detail/${id}`)
}

// 跳转编辑
const goEdit = () => {
  router.push('/guide/edit')
}

// 清除搜索
// 重置搜索
// 处理URL参数
const handleUrlParams = () => {
  const searchParam = route.query.search
  if (searchParam) {
    searchForm.title = decodeURIComponent(String(searchParam))
  }
  fetchGuides()
}

// 监听路由参数变化
watch(() => route.query.search, (newSearch, oldSearch) => {
  if (newSearch !== oldSearch) {
    handleUrlParams()
  }
})

// 生命周期
onMounted(() => {
  loadSiteAssets()
  handleUrlParams()
})
</script>

<style scoped>
.travel-page {
  width: 100%;
  min-height: 100vh;
  background: #F9F9F9;
  font-family: "Microsoft YaHei", "PingFang SC", Arial, sans-serif;
  color: #333;
  position: relative;
}

/* Banner */
.banner {
  position: relative;
  width: min(var(--frontend-container-safe-width), var(--frontend-container-wide));
  height: 420px;
  margin: 20px auto 0;
  border-radius: 12px;
  overflow: hidden;
}

.banner-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.banner-mask {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.35);
  display: flex;
  align-items: center;
  justify-content: center;
}

.banner-content {
  text-align: center;
  padding: 0 20px;
  max-width: 800px;
  width: 100%;
}

.banner-title {
  font-size: 36px;
  color: #fff;
  font-weight: 700;
  text-shadow: 0 2px 12px rgba(0, 0, 0, 0.5);
  margin: 0 0 30px;
  line-height: 1.3;
}

.search-bar {
  display: flex;
  width: 100%;
  max-width: 600px;
  margin: 0 auto;
  height: 56px;
  background: #fff;
  border-radius: 28px;
  overflow: hidden;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.2);
}

.search-input {
  flex: 1;
  border: none;
  outline: none;
  padding: 0 24px;
  font-size: 16px;
  color: #333;
}

.search-input::placeholder {
  color: #bbb;
  font-size: 14px;
}

.search-btn {
  width: 100px;
  border: none;
  background: #f90;
  color: #fff;
  font-size: 16px;
  font-weight: 500;
  cursor: pointer;
  transition: background 0.2s;
}

.search-btn:hover {
  background: #f80;
}

/* 内容区 */
.content {
  width: min(var(--frontend-container-safe-width), var(--frontend-container-wide));
  margin: 0 auto;
  padding: 30px 0;
}

/* Tab */
.tab-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  padding: 0 30px;
  border-bottom: 1px solid #eee;
  border-radius: 8px 8px 0 0;
}

.tabs {
  display: flex;
  gap: 50px;
}

.tab {
  position: relative;
  padding: 20px 0;
  font-size: 16px;
  color: #666;
  cursor: pointer;
  transition: color 0.2s;
}

.tab:hover {
  color: #f90;
}

.tab.active {
  color: #f90;
  font-weight: 600;
}

.tab.active::after {
  content: '';
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  height: 3px;
  background: #f90;
  border-radius: 2px;
}

.filter-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 14px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
  color: #666;
  cursor: pointer;
  transition: all 0.2s;
}

.filter-btn:hover,
.filter-btn.active {
  color: #f90;
  border-color: #f90;
  background: #fff8ec;
}

.filter-icon {
  font-size: 16px;
  font-weight: bold;
}

.filter-panel {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr)) auto;
  gap: 14px;
  align-items: end;
  padding: 14px 30px 18px;
  border-bottom: 1px solid #eef1f5;
  background: #fff;
}

.filter-field {
  display: grid;
  gap: 6px;
  min-width: 0;

  span {
    color: #667085;
    font-size: 13px;
    font-weight: 700;
  }

  select,
  input {
    width: 100%;
    height: 32px;
    padding: 0 12px;
    border: 1px solid #d8dee8;
    border-radius: 4px;
    outline: none;
    color: #344054;
    font-size: 14px;
    background: #fff;
    transition: border-color 0.2s, box-shadow 0.2s;

    &:focus {
      border-color: #f90;
      box-shadow: 0 0 0 3px rgba(255, 153, 0, 0.14);
    }
  }

  small {
    color: #98a2b3;
    font-size: 12px;
    line-height: 1.4;
  }
}

.region-cascader {
  width: 100%;
  height: 32px;

  :deep(.el-input__wrapper) {
    height: 32px;
    min-height: 32px;
    padding: 0 11px;
    border-radius: 4px;
    box-shadow: 0 0 0 1px #d8dee8 inset;
    transition: box-shadow 0.2s;
  }

  :deep(.el-input__wrapper.is-focus),
  :deep(.el-input__wrapper.is-focused),
  :deep(.el-input__wrapper:hover) {
    box-shadow: 0 0 0 1px #f90 inset, 0 0 0 3px rgba(255, 153, 0, 0.14);
  }

  :deep(.el-input__inner) {
    height: 32px;
    line-height: 32px;
    color: #344054;
    font-size: 14px;
  }
}

.filter-actions {
  display: flex;
  gap: 10px;
}

.filter-reset,
.filter-apply {
  height: 32px;
  padding: 0 16px;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 700;
  white-space: nowrap;
  transition: all 0.2s;
}

.filter-reset {
  border: 1px solid #d8dee8;
  color: #667085;
  background: #fff;

  &:hover {
    border-color: #f90;
    color: #f90;
  }
}

.filter-apply {
  border: 1px solid #f90;
  color: #fff;
  background: #f90;

  &:hover {
    border-color: #f80;
    background: #f80;
  }
}

/* 攻略列表 */
.article-list {
  background: #fff;
  padding: 0 30px;
  min-height: 400px;
  border-radius: 0 0 8px 8px;
}

.article-item {
  display: flex;
  gap: 24px;
  padding: 24px 0;
  border-bottom: 1px solid #f0f0f0;
  cursor: pointer;
  transition: background 0.2s;
}

.article-item:hover {
  background: #fafafa;
}

.article-item:last-child {
  border-bottom: none;
}

.article-cover {
  position: relative;
  width: 240px;
  height: 160px;
  flex-shrink: 0;
  border-radius: 8px;
  overflow: hidden;
}

.article-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.essence-tag {
  position: absolute;
  top: 8px;
  left: 8px;
  background: #fff;
  color: #e63;
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 4px;
  font-weight: 600;
}

.essence-tag::before {
  content: '★ ';
  color: #e63;
}

.article-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.article-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.article-title {
  font-size: 18px;
  font-weight: 600;
  color: #222;
  margin: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex: 1;
}

.app-tag {
  flex-shrink: 0;
  background: #ffeac2;
  color: #f90;
  font-size: 12px;
  font-weight: 700;
  padding: 4px 14px;
  border-radius: 20px;
  border: 1px solid #ffd989;
}

.article-desc {
  font-size: 13px;
  color: #888;
  line-height: 1.7;
  margin: 0 0 16px;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.article-meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px 24px;
  margin-top: auto;
  font-size: 13px;
  color: #999;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 6px;
}

.icon-location,
.icon-view {
  color: #f90;
  font-size: 14px;
}

.avatar {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  object-fit: cover;
}

.author {
  color: #f90;
}

.identity-badge {
  display: inline-flex;
  align-items: center;
  height: 20px;
  padding: 0 8px;
  border: 1px solid transparent;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 800;
  line-height: 1;

  &.super-admin {
    color: #6f4a08;
    border-color: rgba(180, 130, 22, 0.28);
    background: linear-gradient(135deg, #fff8df 0%, #f7e5a8 100%);
  }

  &.admin {
    color: #075985;
    border-color: rgba(14, 116, 144, 0.22);
    background: linear-gradient(135deg, #e7faff 0%, #d8eef7 100%);
  }
}

.meta-text {
  color: #999;
}

/* 空状态样式 */
.empty-state {
  text-align: center;
  padding: 80px 20px;
  color: #64748b;
}

.empty-icon {
  font-size: 64px;
  margin-bottom: 24px;
  opacity: 0.6;
}

.empty-title {
  font-size: 24px;
  font-weight: 600;
  margin: 0 0 12px;
  color: #2d3748;
}

.empty-desc {
  font-size: 16px;
  margin: 0 0 32px;
  opacity: 0.8;
}

.empty-action {
  border-radius: 8px;
  padding: 12px 32px;
  font-weight: 600;
  background: #f90;
  border: none;
}

.empty-action:hover {
  background: #f80;
}

/* 分页样式 */
.pagination-section {
  background: #fff;
  padding: 20px 30px;
  display: flex;
  justify-content: center;
  border-radius: 0 0 8px 8px;
}

.modern-pagination {
  :deep(.el-pagination) {
    .el-pager li {
      border-radius: 4px;
      margin: 0 4px;
      transition: all 0.3s ease;

      &:hover {
        background: #f90;
        color: white;
      }

      &.is-active {
        background: #f90;
        color: white;
      }
    }

    .btn-prev, .btn-next {
      border-radius: 4px;
      transition: all 0.3s ease;

      &:hover {
        background: #f90;
        color: white;
      }
    }
  }
}

/* 发布攻略悬浮按钮 */
.float-publish-btn {
  position: fixed;
  bottom: 40px;
  right: 40px;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 24px;
  background: linear-gradient(135deg, #f90 0%, #f80 100%);
  color: #fff;
  border-radius: 40px;
  cursor: pointer;
  box-shadow: 0 4px 15px rgba(255, 153, 0, 0.3);
  transition: all 0.3s ease;
  z-index: 100;
  font-weight: 600;
  border: none;
}

.float-publish-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(255, 153, 0, 0.4);
}

.float-publish-btn .el-icon {
  font-size: 18px;
}

/* 响应式 */
@media (max-width: 768px) {
  .banner {
    height: 320px;
  }
  
  .banner-title {
    font-size: 22px;
    margin-bottom: 20px;
  }
  
  .search-bar {
    height: 48px;
    border-radius: 24px;
  }
  
  .search-input {
    padding: 0 18px;
    font-size: 14px;
  }
  
  .search-btn {
    width: 80px;
    font-size: 14px;
  }
  
  .tab-bar {
    padding: 0 16px;
  }
  
  .tabs {
    gap: 20px;
  }
  
  .tab {
    padding: 14px 0;
    font-size: 14px;
  }
  
  .article-list {
    padding: 0 16px;
  }
  
  .article-item {
    flex-direction: column;
    gap: 12px;
  }
  
  .article-cover {
    width: 100%;
    height: 180px;
  }
  
  .article-title {
    font-size: 16px;
    white-space: normal;
    -webkit-line-clamp: 2;
    display: -webkit-box;
    -webkit-box-orient: vertical;
  }
  
  .article-meta {
    flex-wrap: wrap;
    gap: 16px;
  }

  .filter-panel {
    grid-template-columns: 1fr;
    padding: 16px;
  }

  .filter-actions {
    justify-content: flex-end;
  }
  
  .float-publish-btn {
    bottom: 20px;
    right: 20px;
    padding: 10px 20px;
    font-size: 14px;
  }
}
</style>
