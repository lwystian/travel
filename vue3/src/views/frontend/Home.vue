<template>
  <div class="home-container">
    <!-- Hero区域 - 全宽轮播图 -->
    <div class="hero-section">
      <home-carousel />
    </div>

    <!-- 精选行程区域 -->
    <div class="featured-section">
      <div class="section-container">
        <div class="section-header">
          <h2 class="section-title">
            精选行程
          </h2>
          <router-link to="/tickets" class="view-more-btn">
            查看全部 <el-icon><ArrowRight /></el-icon>
          </router-link>
        </div>

        <!-- 行程展示区域 - 杂志风布局 -->
        <el-skeleton :loading="tourLoading" animated :throttle="500">
          <template #template>
            <div class="tour-magazine-layout">
              <div class="skeleton-hero-card"></div>
              <div class="skeleton-side-cards">
                <div class="skeleton-mini-card"></div>
                <div class="skeleton-mini-card"></div>
              </div>
            </div>
          </template>
          <template #default>
            <!-- 主打行程 - 大卡片 -->
            <div class="tour-showcase" v-if="featuredTour">
              <div class="hero-tour-card" @click="goToTourDetail(featuredTour.id)">
                <div class="hero-image-wrapper">
                  <img :src="getImageUrl(featuredTour.mainImage)" :alt="featuredTour.title" />
                  <div class="hero-overlay">
                    <div class="tour-badges">
                      <span class="badge recommend" v-if="featuredTour.tourType">
                        <el-icon><Flag /></el-icon>
                        {{ getTourTypeName(featuredTour.tourType) }}
                      </span>
                      <span class="badge days" v-if="featuredTour.days">
                        <el-icon><Calendar /></el-icon>
                        {{ featuredTour.days }}天{{ featuredTour.days - 1 }}晚
                      </span>
                    </div>
                  </div>
                </div>
                <div class="hero-content">
                  <div class="content-main">
                    <h3 class="tour-title">{{ featuredTour.title }}</h3>
                    <p class="tour-subtitle" v-if="featuredTour.subtitle">{{ featuredTour.subtitle }}</p>
                    <div class="tour-tags" v-if="featuredTour.tags">
                      <span class="tag" v-for="(tag, idx) in getTags(featuredTour.tags)" :key="idx">{{ tag }}</span>
                    </div>
                  </div>
                  <div class="content-aside">
                    <div class="tour-rating" v-if="featuredTour.starRating">
                      <el-icon class="star-icon"><Star /></el-icon>
                      <span class="rating-value">{{ featuredTour.starRating }}</span>
                    </div>
                    <div class="tour-enrolled" v-if="featuredTour.enrolledCount">
                      <el-icon><User /></el-icon>
                      <span>{{ featuredTour.enrolledCount }}人已出行</span>
                    </div>
                    <div class="tour-price">
                      <span class="price-label">起</span>
                      <span class="price-value">¥{{ featuredTour.minPrice !== undefined && featuredTour.minPrice !== null && featuredTour.minPrice !== '' ? featuredTour.minPrice : '--' }}</span>
                      <span class="price-unit">/人</span>
                    </div>
                    <el-button type="primary" class="book-btn">
                      查看详情 <el-icon><ArrowRight /></el-icon>
                    </el-button>
                  </div>
                </div>
              </div>
            </div>

            <!-- 精选行程列表 - 横向滑动卡片 -->
            <div class="tour-scroll-section" v-if="tourList.length > 1">
              <div class="section-subtitle">
                <span class="subtitle-line"></span>
                <span class="subtitle-text">更多精选推荐</span>
                <span class="subtitle-line"></span>
              </div>
              <div class="tour-scroll-container">
                <div class="tour-scroll-wrapper">
                  <div 
                    v-for="(item, index) in tourList.slice(1)" 
                    :key="item.id"
                    class="tour-scroll-card"
                    :class="`card-style-${index % 4}`"
                    @click="goToTourDetail(item.id)"
                  >
                    <div class="card-image-wrapper">
                      <img :src="getImageUrl(item.mainImage)" :alt="item.title" />
                      <div class="card-type-badge">
                        {{ getTourTypeName(item.tourType) }}
                      </div>
                      <div class="card-price" v-if="item.minPrice !== undefined && item.minPrice !== null && item.minPrice !== ''">
                        <span class="price">¥{{ item.minPrice }}</span>
                        <span class="unit">起</span>
                      </div>
                    </div>
                    <div class="card-info">
                      <h4 class="card-title">{{ item.title }}</h4>
                      <div class="card-meta">
                        <span class="meta-item" v-if="item.days">
                          <el-icon><Calendar /></el-icon>
                          {{ item.days }}天{{ item.days - 1 }}晚
                        </span>
                        <span class="meta-item" v-if="item.starRating">
                          <el-icon><Star /></el-icon>
                          {{ item.starRating }}
                        </span>
                      </div>
                      <div class="card-tags" v-if="item.tags">
                        <span class="mini-tag" v-for="(tag, idx) in getTags(item.tags)" :key="idx">{{ tag }}</span>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </template>
        </el-skeleton>
      </div>
    </div>

    <!-- 旅游攻略区域 -->
    <div class="inspiration-section">
      <div class="section-container">
        <div class="section-header">
          <h2 class="section-title">
            旅游攻略
          </h2>
          <router-link to="/guide" class="view-more-btn">
            查看全部 <el-icon><ArrowRight /></el-icon>
          </router-link>
        </div>

        <!-- 攻略横向滚动列表 -->
        <el-skeleton :loading="guideLoading" animated :throttle="500">
          <template #template>
            <div class="guide-scroll-wrapper">
              <div v-for="i in 4" :key="i" class="guide-scroll-skeleton"></div>
            </div>
          </template>
          <template #default>
            <div class="guide-scroll-section" v-if="guideList.length > 0">
              <div class="guide-scroll-container">
                <div class="guide-scroll-wrapper">
                  <div 
                    v-for="(item, index) in guideList" 
                    :key="item.id"
                    class="guide-scroll-card"
                    :class="`card-style-${index % 4}`"
                    @click="goToGuideDetail(item.id)"
                  >
                    <div class="guide-card-image">
                      <img :src="getImageUrl(item.coverImage)" :alt="item.title" />
                      <div class="guide-card-overlay">
                        <div class="guide-views">
                          <el-icon><View /></el-icon>
                          {{ item.views || 0 }}
                        </div>
                      </div>
                    </div>
                    <div class="guide-card-info">
                      <h4 class="guide-card-title">{{ item.title }}</h4>
                      <div class="guide-card-meta">
                        <div class="meta-author">
                          <el-icon><User /></el-icon>
                          <span>{{ item.userNickname || '旅游达人' }}</span>
                        </div>
                        <div class="meta-date">
                          {{ formatDate(item.createTime) }}
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </template>
        </el-skeleton>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick, computed } from 'vue'
import { useRouter } from 'vue-router'
import request from '@/utils/request'
import HomeCarousel from '@/components/frontend/HomeCarousel.vue'
import {
  ArrowRight,
  View,
  Calendar,
  Star,
  User,
  Flag
} from '@element-plus/icons-vue'

const router = useRouter()
const baseAPI = process.env.VUE_APP_BASE_API || '/api'

// 行程数据
const tourList = ref([])
const tourLoading = ref(true)

// 攻略数据
const guideList = ref([])
const guideLoading = ref(true)

// 获取精选行程
const fetchTours = async () => {
  tourLoading.value = true
  try {
    // 获取精选行程和更多推荐
    const [featuredRes, moreRes] = await Promise.all([
      request.get('/tour/featured', {}, { showDefaultMsg: false }),
      request.get('/tour/more', {}, { showDefaultMsg: false })
    ])
    
    console.log('精选行程数据:', featuredRes)
    console.log('更多推荐数据:', moreRes)
    
    // 合并数据：精选在前，更多在后
    const featuredTours = featuredRes || []
    const moreTours = moreRes || []
    tourList.value = [...featuredTours, ...moreTours]
    
    console.log('合并后的行程列表:', tourList.value)
  } catch (error) {
    console.error('获取精选行程失败:', error)
    // 如果API调用失败，回退到原来的获取所有行程方式
    try {
      await request.get('/tour/all', {}, {
        showDefaultMsg: false,
        onSuccess: (data) => {
          tourList.value = data || []
        }
      })
    } catch (e) {
      console.error('获取所有行程也失败:', e)
    }
  } finally {
    tourLoading.value = false
  }
}

// 获取精选攻略
const fetchHotGuides = async () => {
  guideLoading.value = true
  try {
    await request.get('/guide/hot', {
      limit: 3
    }, {
      showDefaultMsg: false,
      onSuccess: (data) => {
        guideList.value = data || []
      }
    })
  } catch (error) {
    console.error('获取精选攻略失败:', error)
  } finally {
    guideLoading.value = false
  }
}

// 精选行程（第一个）
const featuredTour = computed(() => tourList.value[0] || null)

// 获取图片完整URL
const getImageUrl = (url) => {
  if (!url) return ''
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

// 解析标签（兼容多种格式：逗号分隔字符串、JSON数组字符串、数组）
const getTags = (tagsStr) => {
  if (!tagsStr) return []
  if (Array.isArray(tagsStr)) return tagsStr
  if (typeof tagsStr === 'string') {
    // 尝试解析JSON数组格式
    if (tagsStr.startsWith('[')) {
      try {
        return JSON.parse(tagsStr)
      } catch {
        // 如果JSON解析失败，使用逗号分隔
        return tagsStr.split(',').map(t => t.trim()).filter(t => t)
      }
    }
    // 普通逗号分隔
    return tagsStr.split(',').map(t => t.trim()).filter(t => t)
  }
  return []
}

// 格式化日期
const formatDate = (dateString) => {
  if (!dateString) return ''
  const date = new Date(dateString)
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
}

// 跳转到行程预订页
const goToTourDetail = (tourId) => {
  router.push(`/ticket/booking/${tourId}`)
}

// 跳转到攻略详情
const goToGuideDetail = (guideId) => {
  router.push(`/guide/detail/${guideId}`)
}

// 滚动动画观察器
const observeElements = () => {
  const observer = new IntersectionObserver((entries) => {
    entries.forEach(entry => {
      if (entry.isIntersecting) {
        entry.target.classList.add('animate-fade-in-up')
      }
    })
  }, {
    threshold: 0.1,
    rootMargin: '0px 0px -50px 0px'
  })

  // 观察需要动画的元素
  const animateElements = document.querySelectorAll('.animate-on-scroll')
  animateElements.forEach(el => observer.observe(el))
}

// 添加粒子效果
const createParticles = () => {
  const particlesContainer = document.querySelector('.hero-particles')
  if (!particlesContainer) return

  for (let i = 0; i < 20; i++) {
    const particle = document.createElement('div')
    particle.className = 'particle'
    particle.style.cssText = `
      position: absolute;
      width: ${Math.random() * 4 + 2}px;
      height: ${Math.random() * 4 + 2}px;
      background: rgba(255, 255, 255, ${Math.random() * 0.5 + 0.2});
      border-radius: 50%;
      left: ${Math.random() * 100}%;
      top: ${Math.random() * 100}%;
      animation: particleFloat ${Math.random() * 3 + 4}s ease-in-out infinite;
      animation-delay: ${Math.random() * 2}s;
    `
    particlesContainer.appendChild(particle)
  }
}

onMounted(() => {
  fetchTours()
  fetchHotGuides()

  // 延迟执行动画相关功能，确保DOM已渲染
  nextTick(() => {
    observeElements()
    createParticles()
  })
})
</script>

<style lang="scss" scoped>
.home-container {
  width: 100%;
  font-family: "思源黑体", "Source Han Sans", "Noto Sans CJK SC", sans-serif;
  color: #333;
  min-height: 100vh;
}

// Hero区域样式 - 全宽轮播图
.hero-section {
  width: 100%;
  margin: 0;
  padding: 0;
  position: relative;
}

// 通用容器样式
.section-container {
  max-width: 1300px;
  margin: 0 auto;
  padding: 60px 20px;
}

// 区域样式
.quick-nav-section,
.featured-section,
.inspiration-section {
  background: #F9F9F9;
  margin: 0;
  position: relative;

  &:nth-child(even) {
    background: #F9F9F9;
  }
}

// 标题样式
.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 40px;
  flex-wrap: wrap;
  gap: 20px;
}

.section-title {
  font-size: 32px;
  font-weight: 700;
  margin: 0;
  color: #2d3748;
  display: flex;
  align-items: center;
  gap: 12px;

  .title-icon {
    font-size: 28px;
  }
}



.view-more-btn,
.more-link {
  color: #667eea;
  font-size: 14px;
  display: flex;
  align-items: center;
  text-decoration: none;
  font-weight: 600;
  transition: all 0.3s ease;

  .el-icon {
    margin-left: 4px;
    transition: transform 0.3s ease;
  }

  &:hover {
    color: #764ba2;

    .el-icon {
      transform: translateX(3px);
    }
  }
}

// ============ 精选行程样式 ============

// 主打行程展示区
.tour-showcase {
  margin-bottom: 40px;
}

.hero-tour-card {
  display: flex;
  border-radius: 20px;
  overflow: hidden;
  background: white;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.1);
  cursor: pointer;
  transition: all 0.4s ease;

  &:hover {
    transform: translateY(-4px);
    box-shadow: 0 20px 60px rgba(0, 0, 0, 0.15);

    .hero-image-wrapper img {
      transform: scale(1.05);
    }
  }

  @media (max-width: 768px) {
    flex-direction: column;
  }
}

.hero-image-wrapper {
  width: 55%;
  height: 320px;
  position: relative;
  overflow: hidden;

  @media (max-width: 768px) {
    width: 100%;
    height: 200px;
  }

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
    transition: transform 0.6s ease;
  }
}

.hero-overlay {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: linear-gradient(135deg, rgba(0, 0, 0, 0.1) 0%, transparent 50%);
  padding: 20px;
  display: flex;
  align-items: flex-start;
}

.tour-badges {
  display: flex;
  flex-direction: column;
  gap: 8px;

  .badge {
    display: inline-flex;
    align-items: center;
    gap: 4px;
    padding: 6px 12px;
    border-radius: 20px;
    font-size: 13px;
    font-weight: 600;
    backdrop-filter: blur(10px);

    .el-icon {
      font-size: 14px;
    }

    &.recommend {
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      color: white;
    }

    &.days {
      background: rgba(255, 255, 255, 0.9);
      color: #333;
    }
  }
}

.hero-content {
  flex: 1;
  padding: 30px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;

  @media (max-width: 768px) {
    padding: 20px;
  }
}

.content-main {
  flex: 1;
}

.tour-title {
  font-size: 26px;
  font-weight: 700;
  color: #1a1a2e;
  margin: 0 0 10px;
  line-height: 1.3;

  @media (max-width: 768px) {
    font-size: 20px;
  }
}

.tour-subtitle {
  font-size: 15px;
  color: #666;
  margin: 0 0 16px;
  line-height: 1.5;
}

.tour-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;

  .tag {
    padding: 4px 12px;
    background: #f0f4ff;
    color: #667eea;
    border-radius: 15px;
    font-size: 12px;
    font-weight: 500;
  }
}

.content-aside {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 12px;
  margin-top: 20px;
}

.tour-rating {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 16px;
  font-weight: 600;
  color: #f59e0b;

  .star-icon {
    font-size: 18px;
  }
}

.tour-enrolled {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: #888;

  .el-icon {
    font-size: 14px;
  }
}

.tour-price {
  display: flex;
  align-items: baseline;
  gap: 4px;

  .price-label {
    font-size: 14px;
    color: #888;
  }

  .price-value {
    font-size: 32px;
    font-weight: 700;
    color: #ff6b6b;
    line-height: 1;

    @media (max-width: 768px) {
      font-size: 24px;
    }
  }

  .price-unit {
    font-size: 13px;
    color: #888;
  }
}

.hero-tour-card .book-btn {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  border-radius: 25px;
  padding: 10px 28px;
  font-weight: 600;
  transition: all 0.3s ease;

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 8px 20px rgba(102, 126, 234, 0.4);
  }

  .el-icon {
    margin-left: 4px;
  }
}

// 横向滚动行程列表
.tour-scroll-section {
  margin-top: 20px;
}

.section-subtitle {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  margin-bottom: 24px;

  .subtitle-line {
    width: 60px;
    height: 1px;
    background: linear-gradient(90deg, transparent, #ddd, transparent);
  }

  .subtitle-text {
    font-size: 16px;
    color: #888;
    font-weight: 500;
  }
}

.tour-scroll-container {
  overflow-x: auto;
  padding-bottom: 16px;
  margin: 0 -10px;

  &::-webkit-scrollbar {
    height: 6px;
  }

  &::-webkit-scrollbar-track {
    background: #f1f1f1;
    border-radius: 3px;
  }

  &::-webkit-scrollbar-thumb {
    background: #c1c1c1;
    border-radius: 3px;

    &:hover {
      background: #a8a8a8;
    }
  }
}

.tour-scroll-wrapper {
  display: flex;
  gap: 20px;
  padding: 0 10px;
}

.tour-scroll-card {
  flex-shrink: 0;
  width: 280px;
  border-radius: 16px;
  overflow: hidden;
  background: white;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
  cursor: pointer;
  transition: all 0.4s ease;

  &:hover {
    transform: translateY(-8px);
    box-shadow: 0 12px 40px rgba(0, 0, 0, 0.15);

    .card-image-wrapper img {
      transform: scale(1.1);
    }
  }

  // 不同卡片的渐变色顶部装饰
  &.card-style-0 {
    .card-image-wrapper::before {
      background: linear-gradient(135deg, rgba(102, 126, 234, 0.8) 0%, rgba(118, 75, 162, 0.8) 100%);
    }
  }

  &.card-style-1 {
    .card-image-wrapper::before {
      background: linear-gradient(135deg, rgba(52, 211, 153, 0.8) 0%, rgba(16, 185, 129, 0.8) 100%);
    }
  }

  &.card-style-2 {
    .card-image-wrapper::before {
      background: linear-gradient(135deg, rgba(251, 146, 60, 0.8) 0%, rgba(249, 115, 22, 0.8) 100%);
    }
  }

  &.card-style-3 {
    .card-image-wrapper::before {
      background: linear-gradient(135deg, rgba(236, 72, 153, 0.8) 0%, rgba(219, 39, 119, 0.8) 100%);
    }
  }
}

.card-image-wrapper {
  height: 160px;
  position: relative;
  overflow: hidden;

  &::before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    z-index: 1;
    transition: opacity 0.3s ease;
  }

  img {
    position: relative;
    z-index: 2;
    width: 100%;
    height: 100%;
    object-fit: cover;
    transition: transform 0.5s ease;
  }
}

.card-type-badge {
  position: absolute;
  top: 10px;
  left: 10px;
  z-index: 3;
  padding: 4px 10px;
  background: rgba(255, 255, 255, 0.9);
  border-radius: 12px;
  font-size: 11px;
  font-weight: 600;
  color: #333;
  backdrop-filter: blur(4px);
}

.card-price {
  position: absolute;
  bottom: 10px;
  right: 10px;
  z-index: 3;
  display: flex;
  align-items: baseline;
  background: rgba(255, 255, 255, 0.95);
  padding: 4px 10px;
  border-radius: 12px;
  backdrop-filter: blur(4px);

  .price {
    font-size: 18px;
    font-weight: 700;
    color: #ff6b6b;
  }

  .unit {
    font-size: 11px;
    color: #888;
    margin-left: 2px;
  }
}

.card-info {
  padding: 16px;
}

.card-title {
  font-size: 15px;
  font-weight: 600;
  color: #1a1a2e;
  margin: 0 0 10px;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.card-meta {
  display: flex;
  gap: 12px;
  margin-bottom: 10px;

  .meta-item {
    display: flex;
    align-items: center;
    gap: 4px;
    font-size: 12px;
    color: #888;

    .el-icon {
      font-size: 13px;
      color: #667eea;
    }
  }
}

.card-tags {
  display: flex;
  gap: 6px;

  .mini-tag {
    padding: 2px 8px;
    background: #f60;
    color: #fff;
    border-radius: 2px;
    font-size: 11px;
  }
}

// 骨架屏样式
.tour-magazine-layout {
  display: flex;
  gap: 24px;

  @media (max-width: 768px) {
    flex-direction: column;
  }
}

.skeleton-hero-card {
  flex: 1;
  height: 320px;
  background: linear-gradient(90deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%);
  background-size: 200% 100%;
  animation: skeleton-loading 1.5s infinite;
  border-radius: 16px;

  @media (max-width: 768px) {
    height: 200px;
  }
}

.skeleton-side-cards {
  display: flex;
  flex-direction: column;
  gap: 16px;
  width: 280px;

  @media (max-width: 768px) {
    flex-direction: row;
    width: 100%;
  }
}

.skeleton-mini-card {
  height: 148px;
  background: linear-gradient(90deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%);
  background-size: 200% 100%;
  animation: skeleton-loading 1.5s infinite;
  border-radius: 12px;

  @media (max-width: 768px) {
    flex: 1;
    height: 120px;
  }
}

@keyframes skeleton-loading {
  0% {
    background-position: 200% 0;
  }
  100% {
    background-position: -200% 0;
  }
}

// ============ 旅游攻略横向滑动样式 ============
.guide-scroll-section {
  margin-top: 10px;
}

.guide-scroll-container {
  overflow-x: auto;
  padding-bottom: 16px;
  margin: 0 -10px;

  &::-webkit-scrollbar {
    height: 6px;
  }

  &::-webkit-scrollbar-track {
    background: #f1f1f1;
    border-radius: 3px;
  }

  &::-webkit-scrollbar-thumb {
    background: #c1c1c1;
    border-radius: 3px;

    &:hover {
      background: #a8a8a8;
    }
  }
}

.guide-scroll-wrapper {
  display: flex;
  gap: 20px;
  padding: 0 10px;
}

.guide-scroll-card {
  flex-shrink: 0;
  width: 300px;
  border-radius: 16px;
  overflow: hidden;
  background: white;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
  cursor: pointer;
  transition: all 0.4s ease;

  &:hover {
    transform: translateY(-8px);
    box-shadow: 0 12px 40px rgba(0, 0, 0, 0.15);

    .guide-card-image img {
      transform: scale(1.1);
    }
  }

  // 不同卡片的渐变色顶部装饰
  &.card-style-0 {
    .guide-card-image::before {
      background: linear-gradient(135deg, rgba(102, 126, 234, 0.6) 0%, rgba(118, 75, 162, 0.6) 100%);
    }
  }

  &.card-style-1 {
    .guide-card-image::before {
      background: linear-gradient(135deg, rgba(52, 211, 153, 0.6) 0%, rgba(16, 185, 129, 0.6) 100%);
    }
  }

  &.card-style-2 {
    .guide-card-image::before {
      background: linear-gradient(135deg, rgba(251, 146, 60, 0.6) 0%, rgba(249, 115, 22, 0.6) 100%);
    }
  }

  &.card-style-3 {
    .guide-card-image::before {
      background: linear-gradient(135deg, rgba(236, 72, 153, 0.6) 0%, rgba(219, 39, 119, 0.6) 100%);
    }
  }
}

.guide-card-image {
  height: 180px;
  position: relative;
  overflow: hidden;

  &::before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    z-index: 1;
    transition: opacity 0.3s ease;
  }

  img {
    position: relative;
    z-index: 2;
    width: 100%;
    height: 100%;
    object-fit: cover;
    transition: transform 0.5s ease;
  }
}

.guide-card-overlay {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  z-index: 3;
  display: flex;
  align-items: flex-end;
  padding: 12px;
}

.guide-views {
  display: flex;
  align-items: center;
  gap: 4px;
  background: rgba(0, 0, 0, 0.6);
  color: white;
  padding: 4px 10px;
  border-radius: 12px;
  font-size: 12px;
  backdrop-filter: blur(4px);

  .el-icon {
    font-size: 13px;
  }
}

.guide-card-info {
  padding: 16px;
}

.guide-card-title {
  font-size: 16px;
  font-weight: 600;
  color: #1a1a2e;
  margin: 0 0 12px;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.guide-card-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;

  .meta-author {
    display: flex;
    align-items: center;
    gap: 6px;
    font-size: 13px;
    color: #666;

    .el-icon {
      font-size: 14px;
      color: #667eea;
    }
  }

  .meta-date {
    font-size: 12px;
    color: #999;
  }
}

// 攻略骨架屏
.guide-scroll-skeleton {
  flex-shrink: 0;
  width: 300px;
  height: 260px;
  background: linear-gradient(90deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%);
  background-size: 200% 100%;
  animation: skeleton-loading 1.5s infinite;
  border-radius: 16px;
}

// 响应式样式
@media (max-width: 992px) {
  .section-container {
    padding: 40px 15px;
  }
}

@media (max-width: 768px) {
  .section-title {
    font-size: 24px;
  }

  .section-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 15px;
  }

  .tour-scroll-wrapper,
  .guide-scroll-wrapper {
    gap: 16px;
  }

  .tour-scroll-card {
    width: 240px;
  }

  .guide-scroll-card {
    width: 260px;
  }

  .section-container {
    padding: 30px 15px;
  }
}

@media (max-width: 480px) {
  .hero-tour-card {
    border-radius: 12px;
  }

  .tour-scroll-card {
    width: 200px;
  }

  .guide-scroll-card {
    width: 220px;
  }

  .guide-card-image {
    height: 140px;
  }
}
</style>


