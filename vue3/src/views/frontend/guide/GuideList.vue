<template>
  <div class="travel-page">
    <!-- 顶部 Banner -->
    <div class="banner">
      <img
        class="banner-img"
        src="https://images.unsplash.com/photo-1469854523086-cc02fe5d8800?w=1920&h=600&fit=crop"
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
        <div class="filter-btn" @click="openFilter">
          <span class="filter-icon">≡</span>
          <span>筛选</span>
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
            <img :src="getImageUrl(item.coverImage)" :alt="item.title" />
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
                <span class="meta-text">{{ item.destination || '未知目的地' }}</span>
              </div>
              <div class="meta-item">
                <img :src="getImageUrl(item.userAvatar)" class="avatar" :alt="item.userNickname" />
                <span class="meta-text author">{{ item.userNickname || '旅行者' + item.userId }}</span>
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
import { ref, reactive, onMounted, watch, nextTick } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import request from '@/utils/request'
import { formatDate } from '@/utils/dateUtils'
import { Edit } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

const baseAPI = process.env.VUE_APP_BASE_API || '/api'
const router = useRouter()
const route = useRoute()

// 数据
const tableData = ref([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

// 搜索表单
const searchForm = reactive({
  title: ''
})

// Tab 切换
const tabs = ['热门游记', '推荐游记', '最新游记']
const currentTab = ref('热门游记')

// 获取图片完整URL
const getImageUrl = (url) => {
  if (!url) return ''
  return url.startsWith('http') ? url : baseAPI + url
}

// 格式化数字
const formatNumber = (num) => {
  if (!num) return '0'
  if (num >= 10000) {
    return (num / 10000).toFixed(1) + 'w'
  }
  return num.toString()
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
    let orderBy = 'createTime'
    let order = 'desc'
    
    // 根据Tab确定排序规则
    if (currentTab.value === '热门游记') {
      orderBy = 'views'
      order = 'desc'
    } else if (currentTab.value === '最新游记') {
      orderBy = 'createTime'
      order = 'desc'
    } else if (currentTab.value === '推荐游记') {
      orderBy = 'likes'
      order = 'desc'
    }
    
    await request.get('/guide/page', {
      title: searchForm.title || undefined,
      currentPage: currentPage.value,
      size: pageSize.value,
      orderBy: orderBy,
      order: order
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

// 打开筛选（预留功能）
const openFilter = () => {
  ElMessage.info('筛选功能开发中...')
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
const clearSearchTitle = () => {
  searchForm.title = ''
  handleSearch()
}

// 重置搜索
const resetSearch = () => {
  searchForm.title = ''
  currentPage.value = 1
  fetchGuides()
}

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
  width: 100%;
  height: 480px;
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
  max-width: 1100px;
  margin: 0 auto;
  padding: 30px 20px;
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

.filter-btn:hover {
  color: #f90;
  border-color: #f90;
}

.filter-icon {
  font-size: 16px;
  font-weight: bold;
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
  align-items: center;
  gap: 28px;
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
  
  .float-publish-btn {
    bottom: 20px;
    right: 20px;
    padding: 10px 20px;
    font-size: 14px;
  }
}
</style>