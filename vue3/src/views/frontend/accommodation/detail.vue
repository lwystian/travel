<template>
  <div class="accommodation-detail-container">
    <!-- 英雄区域 - 参考景点详情页面设计 -->
    <div class="detail-hero-section" v-if="accommodation">
      <div class="hero-image-container">
        <div class="image-wrapper">
          <img :src="getImageUrl(accommodation.imageUrl)" :alt="accommodation.name" class="hero-image" />
          <div class="image-overlay">
            <div class="overlay-gradient"></div>
            <div class="hero-content">
              <div class="breadcrumb">
                <el-breadcrumb separator="/">
                  <el-breadcrumb-item @click="$router.push('/')">首页</el-breadcrumb-item>
                  <el-breadcrumb-item @click="$router.push('/accommodation')">住宿推荐</el-breadcrumb-item>
                  <el-breadcrumb-item>{{ accommodation.name }}</el-breadcrumb-item>
                </el-breadcrumb>
              </div>
              <h1 class="accommodation-title">{{ accommodation.name }}</h1>
              <div class="accommodation-meta">
                <div class="meta-item">
                  <el-icon><Location /></el-icon>
                  <span>{{ accommodation.address }}</span>
                </div>
                <div class="meta-item" v-if="accommodation.type">
                  <el-icon><House /></el-icon>
                  <span>{{ accommodation.type }}</span>
                </div>
                <div class="meta-item rating">
                  <el-icon><Star /></el-icon>
                  <span>{{ getDisplayRating(accommodation.starLevel) }}</span>
                  <span class="rating-text">({{ formatReviewCount(reviewTotal) }})</span>
                </div>
              </div>
              <div class="action-buttons">
                <el-button
                  type="primary"
                  size="large"
                  @click="openReviewDialog"
                  class="review-btn"
                >
                  <el-icon><EditPen /></el-icon>
                  发表评价
                </el-button>
                <el-button size="large" class="share-btn" @click="handleShare">
                  <el-icon><Share /></el-icon>
                  分享
                </el-button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 加载状态 -->
    <div v-if="loading" class="loading-section">
      <div class="section-container">
        <el-skeleton :rows="20" animated />
      </div>
    </div>
    <!-- 详细信息区域 - 参考景点详情页面设计 -->
    <div class="detail-content" v-else-if="accommodation">
      <div class="section-container">
        <div class="content-grid">
          <!-- 左侧主要内容 -->
          <div class="main-content">
            <!-- 住宿描述 -->
            <div class="info-card description-card">
              <h3 class="card-title">
                <el-icon><Document /></el-icon>
                住宿介绍
              </h3>
              <div class="description-content">{{ accommodation.description || '暂无详细描述' }}</div>
            </div>

            <!-- 特色服务 -->
            <div class="info-card features-card">
              <h3 class="card-title">
                <el-icon><Star /></el-icon>
                特色服务
              </h3>
              <div class="features-content">{{ accommodation.features || '暂无特色服务介绍' }}</div>
            </div>

            <!-- 评价列表 -->
            <div class="info-card reviews-card">
              <div class="reviews-header">
                <h3 class="card-title">
                  <el-icon><ChatDotRound /></el-icon>
                  住客评价 ({{ reviewTotal || 0 }})
                </h3>
              </div>

              <div v-if="loadingReviews" class="review-loading">
                <el-skeleton :rows="3" animated />
              </div>

              <div v-else-if="reviewList.length === 0" class="empty-reviews">
                <el-icon class="empty-icon"><ChatDotRound /></el-icon>
                <h4 class="empty-title">暂无评价</h4>
                <p class="empty-desc">成为第一个评价这家住宿的客人</p>
              </div>

              <div v-else class="review-list">
                <div v-for="(review, index) in reviewList" :key="index" class="review-item">
                  <div class="review-header">
                    <div class="review-user">
                      <el-avatar :size="40" :src="getImageUrl(review.userAvatar || review.avatar)">{{ (review.userNickname || review.nickname || '用户').charAt(0) }}</el-avatar>
                      <div class="user-info">
                        <span class="username">{{ review.userNickname || review.nickname || '匿名用户' }}</span>
                        <span v-if="officialBadge(review)" class="identity-badge" :class="officialBadge(review).className">
                          {{ officialBadge(review).label }}
                        </span>
                        <span class="review-date">{{ formatDate(review.createTime) }}</span>
                      </div>
                    </div>
                    <div class="review-rating">
                      <el-rate v-model="review.rating" disabled size="small" />
                      <span class="rating-score">{{ review.rating }}分</span>
                    </div>
                  </div>

                  <div class="review-content">
                    <div class="review-text">{{ review.content }}</div>
                  </div>

                  <div v-if="canDelete(review)" class="review-actions">
                    <el-button type="danger" size="small" link @click="handleDeleteReview(review.id)">
                      <el-icon><Delete /></el-icon> 删除
                    </el-button>
                  </div>
                </div>

                <!-- 评价分页 -->
                <div class="review-pagination" v-if="reviewTotal > 10">
                  <el-pagination
                    background
                    layout="prev, pager, next"
                    :current-page="reviewPage"
                    :page-size="reviewPageSize"
                    :total="reviewTotal"
                    @current-change="handleReviewPageChange"
                    class="modern-pagination"
                  />
                </div>
              </div>
            </div>
          </div>

          <!-- 右侧信息栏 -->
          <div class="sidebar-content">
            <!-- 基本信息卡片 -->
            <div class="info-card basic-info-card">
              <h3 class="card-title">
                <el-icon><InfoFilled /></el-icon>
                基本信息
              </h3>
              <div class="info-list">
                <div class="info-item">
                  <div class="info-label">
                    <el-icon><Money /></el-icon>
                    价格区间
                  </div>
                  <div class="info-value price-value">{{ accommodation.priceRange }}</div>
                </div>
                <div class="info-item">
                  <div class="info-label">
                    <el-icon><Phone /></el-icon>
                    联系电话
                  </div>
                  <div class="info-value">{{ accommodation.contactPhone || '暂无' }}</div>
                </div>
                <div class="info-item" v-if="accommodation.scenicName">
                  <div class="info-label">
                    <el-icon><Location /></el-icon>
                    附近景点
                  </div>
                  <div class="info-value scenic-link" @click="goToScenic(accommodation.scenicId)">
                    {{ accommodation.scenicName }}
                    <span v-if="accommodation.distance" class="distance">（{{ accommodation.distance }}）</span>
                  </div>
                </div>
              </div>
            </div>
            <!-- 周边景点推荐 -->
            <div class="info-card nearby-scenics-card" v-if="nearbyScenics.length > 0">
              <h3 class="card-title">
                <el-icon><MapLocation /></el-icon>
                周边景点
              </h3>

              <div v-if="loadingScenics" class="loading-recommendations">
                <el-skeleton :rows="3" animated />
              </div>

              <div v-else class="scenic-list">
                <div v-for="scenic in nearbyScenics" :key="scenic.id" class="scenic-item" @click="goToScenic(scenic.id)">
                  <div class="scenic-image">
                    <img :src="getImageUrl(scenic.imageUrl)" :alt="scenic.name" />
                  </div>
                  <div class="scenic-info">
                    <div class="scenic-name">{{ scenic.name }}</div>
                    <div class="scenic-price">{{ scenic.price ? `¥${scenic.price}` : '免费' }}</div>
                  </div>
                </div>
              </div>
            </div>

            <!-- 更多住宿推荐 -->
            <div class="info-card similar-accommodations-card" v-if="similarAccommodations.length > 0">
              <h3 class="card-title">
                <el-icon><House /></el-icon>
                更多住宿
              </h3>

              <div v-if="loadingSimilar" class="loading-recommendations">
                <el-skeleton :rows="3" animated />
              </div>

              <div v-else class="accommodation-list">
                <div
                  v-for="item in similarAccommodations"
                  :key="item.id"
                  class="accommodation-item"
                  @click="goToAccommodation(item.id)"
                >
                  <div class="accommodation-image">
                    <img :src="getImageUrl(item.imageUrl)" :alt="item.name" />
                  </div>
                  <div class="accommodation-info">
                    <div class="item-name">{{ item.name }}</div>
                    <div class="item-rating">
                      <el-rate v-model="item.starLevel" disabled :max="5" size="small" />
                    </div>
                    <div class="item-price">{{ item.priceRange }}</div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 空状态 -->
    <div v-else class="empty-state">
      <div class="section-container">
        <div class="empty-content">
          <el-icon class="empty-icon"><House /></el-icon>
          <h3 class="empty-title">住宿信息不存在</h3>
          <p class="empty-desc">该住宿可能已下线或不存在</p>
          <el-button type="primary" @click="$router.push('/accommodation')">
            返回住宿列表
          </el-button>
        </div>
      </div>
    </div>
    
    <!-- 评价对话框 -->
    <el-dialog
      v-model="showReviewDialog"
      title="发表评价"
      width="500px"
      :close-on-click-modal="false"
    >
      <el-form ref="reviewFormRef" :model="reviewForm" :rules="reviewRules" label-position="top">
        <el-form-item label="评分" prop="rating">
          <el-rate v-model="reviewForm.rating" :colors="colors" show-score />
        </el-form-item>
        
        <el-form-item label="评价内容" prop="content">
          <el-input
            v-model="reviewForm.content"
            type="textarea"
            :rows="4"
            placeholder="请分享您的住宿体验..."
          />
        </el-form-item>
      </el-form>
      
      <template #footer>
        <el-button @click="showReviewDialog = false">取消</el-button>
        <el-button type="primary" :loading="submittingReview" @click="submitReview">提交评价</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'
import { useUserStore } from '@/store/user'
import { shareCurrentPage } from '@/utils/share'
import { updateSeo, seoDescription } from '@/utils/seo'
import { useSiteAssets, getAssetUrl } from '@/utils/siteAssets'
import { resolveImageUrl, resolveAbsoluteImageUrl } from '@/utils/imageUrl'
import defaultPlaceholder from '@/assets/images/no-image.png'
import {
  Location, Star, Phone, Delete, House, MapLocation,
  Document, InfoFilled, Money, ChatDotRound, EditPen, Share
} from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const { siteAssets, loadSiteAssets } = useSiteAssets()

// 数据状态
const accommodation = ref(null)
const loading = ref(false)
const reviewList = ref([])
const reviewTotal = ref(0)
const reviewPage = ref(1)
const reviewPageSize = ref(10)
const loadingReviews = ref(false)
const nearbyScenics = ref([])
const loadingScenics = ref(false)
const similarAccommodations = ref([])
const loadingSimilar = ref(false)
// 评分颜色
const colors = ['#99A9BF', '#F7BA2A', '#FF9900']

// 评价表单
const showReviewDialog = ref(false)
const submittingReview = ref(false)
const reviewForm = reactive({
  accommodationId: parseInt(route.params.id),
  rating: 5,
  content: ''
})

const reviewRules = {
  rating: [
    { required: true, message: '请选择评分', trigger: 'change' }
  ],
  content: [
    { required: true, message: '请输入评价内容', trigger: 'blur' },
    { min: 5, max: 500, message: '评价内容长度在5到500个字符之间', trigger: 'blur' }
  ]
}

// 检查是否为当前用户评价或管理员
const canDelete = (review) => {
  if (!userStore.userInfo) return false
  return userStore.userInfo.id === review.userId || userStore.isAdmin
}

const officialBadge = (item) => {
  if (item?.userRoleCode === 'SUPER_ADMIN') {
    return { label: '官方 · 超级管理员', className: 'super-admin' }
  }
  if (item?.userRoleCode === 'ADMIN') {
    return { label: '官方 · 管理员', className: 'admin' }
  }
  return null
}

const openReviewDialog = () => {
  if (!userStore.isLoggedIn) {
    ElMessageBox.confirm('发表评价需要登录，是否前往登录页面？', '提示', {
      confirmButtonText: '去登录',
      cancelButtonText: '取消',
      type: 'info'
    }).then(() => {
      router.push({
        path: '/login',
        query: { redirect: route.fullPath }
      })
    }).catch(() => {})
    return
  }
  showReviewDialog.value = true
}

// 格式化评价数量
const formatReviewCount = (count) => {
  if (!count || count === 0) return '暂无评价'
  if (count === 1) return '1条评价'
  return `${count}条评价`
}

// 获取评分显示
const getDisplayRating = (rating) => {
  if (!rating) return '4.5'
  return parseFloat(rating).toFixed(1)
}

// 获取住宿详情
const fetchAccommodationDetail = async () => {
  loading.value = true
  try {
    await request.get(`/accommodation/${route.params.id}`, {}, {
      onSuccess: (res) => {
        accommodation.value = res
        updateSeo({
          title: `${res.name}住宿推荐`,
          description: res.description || res.features || `${res.name}住宿详情、价格区间、地址和周边景点推荐。`,
          path: `/accommodation/${route.params.id}`,
          image: res.imageUrl,
          schema: {
            '@context': 'https://schema.org',
            '@type': 'LodgingBusiness',
            name: res.name,
            description: seoDescription(res.description || res.features || res.address || `${res.name}住宿推荐`),
            url: `${window.location.origin}/accommodation/${route.params.id}`,
            image: res.imageUrl ? getAbsoluteImageUrl(res.imageUrl) : undefined,
            address: res.address || undefined,
            priceRange: res.priceRange || undefined
          }
        })
      }
    })
  } catch (error) {
    console.error('获取住宿详情失败:', error)
  } finally {
    loading.value = false
  }
}

// 获取住宿评价列表
const fetchAccommodationReviews = async () => {
  loadingReviews.value = true
  try {
    await request.get('/accommodation/review/page', {
      accommodationId: route.params.id,
      currentPage: reviewPage.value,
      size: reviewPageSize.value
    }, {
      onSuccess: (res) => {
        reviewList.value = res.records||[]
        reviewTotal.value = res.total||0
      }
    })
  } catch (error) {
    console.error('获取住宿评价失败:', error)
  } finally {
    loadingReviews.value = false
  }
}

// 获取周边景点
const fetchNearbyScenics = async () => {
  if (!accommodation.value?.scenicId) return
  
  loadingScenics.value = true
  try {
    await request.get('/scenic/page', {
      size: 5,
      currentPage: 1
    }, {
      onSuccess: (res) => {
        // 过滤掉当前关联的景点，只显示其他景点
        nearbyScenics.value = res.records.filter(s => s.id !== accommodation.value?.scenicId).slice(0, 3)
      }
    })
  } catch (error) {
    console.error('获取周边景点失败:', error)
  } finally {
    loadingScenics.value = false
  }
}

// 获取相似住宿
const fetchSimilarAccommodations = async () => {
  if (!accommodation.value) return
  
  loadingSimilar.value = true
  try {
    await request.get('/accommodation/page', {
      type: accommodation.value.type,
      currentPage: 1,
      size: 3
    }, {
      onSuccess: (res) => {
        // 过滤掉当前住宿，只显示其他住宿
        similarAccommodations.value = res.records.filter(a => a.id !== parseInt(route.params.id)).slice(0, 3)
      }
    })
  } catch (error) {
    console.error('获取相似住宿失败:', error)
  } finally {
    loadingSimilar.value = false
  }
}

// 处理图片URL
const getImageUrl = (url) => {
  if (!url) return getAssetUrl(siteAssets.value.placeholderImageUrl, defaultPlaceholder)
  return resolveImageUrl(url, getAssetUrl(siteAssets.value.placeholderImageUrl, defaultPlaceholder))
}

const getAbsoluteImageUrl = (url) => {
  return resolveAbsoluteImageUrl(url, getAssetUrl(siteAssets.value.placeholderImageUrl, defaultPlaceholder))
}

// 处理评价分页
const handleReviewPageChange = (page) => {
  reviewPage.value = page
  fetchAccommodationReviews()
}

// 格式化日期
const formatDate = (dateStr) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
}

// 提交评价
const submitReview = async () => {
  // 检查是否登录
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录再发表评价')
    router.push('/login')
    return
  }
  
  submittingReview.value = true
  try {
    await request.post('/accommodation/review', reviewForm, {
      successMsg: '评价已提交，审核通过后展示',
      onSuccess: () => {
        showReviewDialog.value = false
        reviewForm.rating = 5
        reviewForm.content = ''
        // 刷新评价列表
        reviewPage.value = 1
        fetchAccommodationReviews()
        // 刷新住宿详情以更新评分
        fetchAccommodationDetail()
      }
    })
  } catch (error) {
    console.error('提交评价失败:', error)
  } finally {
    submittingReview.value = false
  }
}

// 删除评价
const handleDeleteReview = (id) => {
  ElMessageBox.confirm('确认删除该评价?', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await request.delete(`/accommodation/review/${id}`,  {
        successMsg: '评价已删除',
        onSuccess: () => {
          // 刷新评价列表
          fetchAccommodationReviews()
          // 刷新住宿详情以更新评分
          fetchAccommodationDetail()
        }
      })
    } catch (error) {
      console.error('删除评价失败:', error)
    }
  }).catch(() => {})
}

// 跳转到景点详情
const goToScenic = (id) => {
  router.push(`/scenic/${id}`)
}

// 跳转到住宿详情
const goToAccommodation = (id) => {
  router.push(`/accommodation/${id}`)
  // 如果是同一页面不同ID的情况，需要刷新数据
  if (parseInt(route.params.id) !== id) {
    // 延迟执行以确保路由已经完成
    setTimeout(() => {
      fetchAccommodationDetail()
      fetchAccommodationReviews()
      window.scrollTo(0, 0)
    }, 100)
  }
}

// 分享功能
const handleShare = () => {
  shareCurrentPage({
    title: accommodation.value?.name || '酒店详情',
    text: accommodation.value?.description || accommodation.value?.address || ''
  })
}

// 初始加载
onMounted(() => {
  loadSiteAssets()
  fetchAccommodationDetail()
  fetchAccommodationReviews()
})

// 监听住宿数据加载完成，加载相关推荐
watch(() => accommodation.value, (newVal) => {
  if (newVal) {
    fetchNearbyScenics()
    fetchSimilarAccommodations()
  }
}, { immediate: false })
</script>

<style lang="scss" scoped>
.accommodation-detail-container {
  min-height: 100vh;
  background: #FFFFFF;
  font-family: "思源黑体", "Source Han Sans", "Noto Sans CJK SC", sans-serif;
  color: #333;

  // 通用容器样式
  .section-container {
    width: min(var(--frontend-container-safe-width), var(--frontend-container-wide));
    margin: 0 auto;
    padding: 0;
  }

  // 英雄区域样式 - 参考景点详情页面
  .detail-hero-section {
    position: relative;
    height: 480px;
    min-height: 360px;
    overflow: hidden;
  }

  .hero-image-container {
    position: relative;
    width: 100%;
    height: 100%;
  }

  .image-wrapper {
    position: relative;
    width: 100%;
    height: 100%;
  }

  .hero-image {
    width: 100%;
    height: 100%;
    object-fit: cover;
    transition: transform 0.8s ease;
  }

  .image-overlay {
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    display: flex;
    align-items: center;
    justify-content: center;
  }

  .overlay-gradient {
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background: linear-gradient(
      to bottom,
      rgba(0, 0, 0, 0.3) 0%,
      rgba(0, 0, 0, 0.1) 50%,
      rgba(0, 0, 0, 0.6) 100%
    );
  }

  .hero-content {
    position: relative;
    z-index: 10;
    color: white;
    text-align: center;
    width: min(var(--frontend-container-safe-width), var(--frontend-container-wide));
    padding: 0;
  }

  .breadcrumb {
    margin-bottom: 30px;
    text-align: left;

    :deep(.el-breadcrumb) {
      .el-breadcrumb__item {
        .el-breadcrumb__inner {
          color: rgba(255, 255, 255, 0.8);
          font-weight: 500;
          cursor: pointer;

          &:hover {
            color: white;
          }
        }

        &:last-child .el-breadcrumb__inner {
          color: white;
        }
      }

      .el-breadcrumb__separator {
        color: rgba(255, 255, 255, 0.6);
      }
    }
  }

  .accommodation-title {
    font-size: 48px;
    font-weight: 700;
    margin: 0 0 30px;
    text-shadow: 0 4px 8px rgba(0, 0, 0, 0.3);
    line-height: 1.2;
  }

  .accommodation-meta {
    display: flex;
    justify-content: center;
    align-items: center;
    gap: 30px;
    margin-bottom: 30px;
    flex-wrap: wrap;
  }

  .meta-item {
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 16px;
    font-weight: 500;
    text-shadow: 0 2px 4px rgba(0, 0, 0, 0.3);

    .el-icon {
      font-size: 18px;
    }

    &.rating {
      .rating-text {
        opacity: 0.9;
        margin-left: 4px;
      }
    }
  }

  .action-buttons {
    display: flex;
    justify-content: center;
    gap: 16px;
    flex-wrap: wrap;
  }

  .review-btn,
  .share-btn {
    border-radius: 50px;
    padding: 12px 24px;
    font-weight: 600;
    font-size: 16px;
    transition: all 0.3s ease;
    border: 2px solid rgba(255, 255, 255, 0.3);
    backdrop-filter: blur(10px);

    &:hover {
      transform: translateY(-2px);
      box-shadow: 0 8px 25px rgba(0, 0, 0, 0.2);
    }
  }

  .review-btn {
    background: rgba(255, 255, 255, 0.9);
    color: #667eea;

    &:hover {
      background: white;
      color: #5a67d8;
      border-color: rgba(255, 255, 255, 0.6);
    }
  }

  .share-btn {
    background: rgba(255, 255, 255, 0.1);
    color: white;

    &:hover {
      background: rgba(255, 255, 255, 0.2);
      border-color: rgba(255, 255, 255, 0.5);
    }
  }

  // 加载状态
  .loading-section {
    background: white;
    padding: 60px 0;
  }

  // 详细信息区域 - 参考景点详情页面
  .detail-content {
    background: #FFFFFF;
    padding: 60px 0;
  }

  .content-grid {
    display: grid;
    grid-template-columns: 1fr 320px;
    gap: 40px;
    align-items: start;
  }

  .main-content {
    .info-card {
      background: white;
      border-radius: 16px;
      box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
      border: 1px solid #e2e8f0;
      margin-bottom: 30px;
      overflow: hidden;
    }

    .card-title {
      font-size: 20px;
      font-weight: 700;
      color: #2d3748;
      margin: 0 0 20px;
      padding: 24px 24px 0;
      display: flex;
      align-items: center;
      gap: 8px;

      .el-icon {
        color: #667eea;
        font-size: 20px;
      }
    }

    .description-content,
    .features-content {
      padding: 0 24px 24px;
      font-size: 16px;
      line-height: 1.8;
      color: #4a5568;
    }

    // 评价区域样式
    .reviews-card {
      .reviews-header {
        padding: 24px 24px 0;
        margin-bottom: 20px;
      }

      .review-loading {
        padding: 20px 24px;
      }

      .empty-reviews {
        text-align: center;
        padding: 40px 24px;

        .empty-icon {
          font-size: 48px;
          margin-bottom: 16px;
        }

        .empty-title {
          font-size: 18px;
          font-weight: 600;
          color: #2d3748;
          margin: 0 0 8px;
        }

        .empty-desc {
          color: #64748b;
          margin: 0;
        }
      }

      .review-list {
        padding: 0 24px 24px;
      }

      .review-item {
        padding: 20px 0;
        border-bottom: 1px solid #f1f5f9;

        &:last-child {
          border-bottom: none;
        }
      }

      .review-header {
        display: flex;
        justify-content: space-between;
        align-items: flex-start;
        margin-bottom: 12px;
      }

      .review-user {
        display: flex;
        align-items: center;
        gap: 12px;
      }

      .user-info {
        display: grid;
        gap: 4px;

        .username {
          font-size: 14px;
          font-weight: 600;
          color: #2d3748;
          margin: 0;
        }

        .identity-badge {
          display: inline-flex;
          width: fit-content;
          align-items: center;
          height: 22px;
          padding: 0 9px;
          border: 1px solid transparent;
          border-radius: 999px;
          font-size: 12px;
          font-weight: 800;

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

        .review-date {
          font-size: 12px;
          color: #64748b;
        }
      }

      .review-rating {
        display: flex;
        align-items: center;
        gap: 8px;

        .rating-score {
          font-size: 14px;
          color: #64748b;
          font-weight: 500;
        }
      }

      .review-content {
        .review-text {
          font-size: 14px;
          line-height: 1.6;
          color: #4a5568;
        }
      }

      .review-actions {
        margin-top: 12px;
        text-align: right;
      }

      .review-pagination {
        padding: 20px 24px 0;
        text-align: center;

        .modern-pagination {
          :deep(.el-pagination) {
            .el-pager li {
              border-radius: 8px;
              margin: 0 4px;
              transition: all 0.3s ease;

              &:hover {
                background: #667eea;
                color: white;
              }

              &.is-active {
                background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                color: white;
              }
            }

            .btn-prev,
            .btn-next {
              border-radius: 8px;
              transition: all 0.3s ease;

              &:hover {
                background: #667eea;
                color: white;
              }
            }
          }
        }
      }
    }
  }

  .sidebar-content {
    .info-card {
      background: white;
      border-radius: 16px;
      box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
      border: 1px solid #e2e8f0;
      margin-bottom: 30px;
      overflow: hidden;
    }

    .card-title {
      font-size: 18px;
      font-weight: 700;
      color: #2d3748;
      margin: 0 0 20px;
      padding: 20px 20px 0;
      display: flex;
      align-items: center;
      gap: 8px;

      .el-icon {
        color: #667eea;
        font-size: 18px;
      }
    }

    .info-list {
      padding: 0 20px 20px;
    }

    .info-item {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 12px 0;
      border-bottom: 1px solid #f1f5f9;

      &:last-child {
        border-bottom: none;
      }
    }

    .info-label {
      display: flex;
      align-items: center;
      gap: 6px;
      font-size: 14px;
      color: #64748b;
      font-weight: 500;

      .el-icon {
        color: #667eea;
        font-size: 16px;
      }
    }

    .info-value {
      font-size: 14px;
      color: #2d3748;
      font-weight: 600;

      &.price-value {
        color: #ef4444;
        font-size: 16px;
      }

      &.scenic-link {
        color: #667eea;
        cursor: pointer;
        transition: color 0.3s ease;

        &:hover {
          color: #5a67d8;
        }

        .distance {
          color: #10b981;
          font-weight: 500;
        }
      }
    }

    // 推荐区域样式
    .nearby-scenics-card,
    .similar-accommodations-card {
      .loading-recommendations {
        padding: 20px;
      }

      .scenic-list,
      .accommodation-list {
        padding: 0 20px 20px;
        display: flex;
        flex-direction: column;
        gap: 16px;
      }

      .scenic-item,
      .accommodation-item {
        display: flex;
        align-items: center;
        gap: 12px;
        padding: 12px;
        border-radius: 12px;
        border: 1px solid #f1f5f9;
        cursor: pointer;
        transition: all 0.3s ease;

        &:hover {
          border-color: #667eea;
          box-shadow: 0 4px 12px rgba(102, 126, 234, 0.15);
          transform: translateY(-2px);
        }
      }

      .scenic-image,
      .accommodation-image {
        width: 60px;
        height: 60px;
        border-radius: 8px;
        overflow: hidden;
        flex-shrink: 0;

        img {
          width: 100%;
          height: 100%;
          object-fit: cover;
        }
      }

      .scenic-info,
      .accommodation-info {
        flex: 1;
        min-width: 0;
      }

      .scenic-name,
      .item-name {
        font-size: 14px;
        font-weight: 600;
        color: #2d3748;
        margin: 0 0 4px;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }

      .scenic-price,
      .item-price {
        font-size: 13px;
        color: #ef4444;
        font-weight: 600;
      }

      .item-rating {
        margin: 4px 0;
      }
    }
  }

  // 空状态
  .empty-state {
    background: white;
    padding: 80px 0;

    .empty-content {
      text-align: center;
      max-width: 400px;
      margin: 0 auto;

      .empty-icon {
        font-size: 64px;
        margin-bottom: 20px;
      }

      .empty-title {
        font-size: 24px;
        font-weight: 700;
        color: #2d3748;
        margin: 0 0 12px;
      }

      .empty-desc {
        font-size: 16px;
        color: #64748b;
        margin: 0 0 24px;
      }
    }
  }

  // 响应式设计
  @media (max-width: 1024px) {
    .content-grid {
      grid-template-columns: 1fr;
      gap: 30px;
    }

    .sidebar-content {
      order: -1;
    }
  }

  @media (max-width: 768px) {
    .detail-hero-section {
      min-height: 320px;
    }

    .hero-content {
      padding: 0 20px;
    }

    .accommodation-title {
      font-size: 32px;
    }

    .accommodation-meta {
      flex-direction: column;
      gap: 16px;
    }

    .action-buttons {
      flex-direction: column;
      align-items: center;

      .review-btn,
      .share-btn {
        width: 200px;
      }
    }

    .section-container {
      padding: 0 16px;
    }

    .detail-content {
      padding: 40px 0;
    }

    .content-grid {
      gap: 20px;
    }

    .main-content .card-title {
      font-size: 18px;
      padding: 20px 20px 0;
    }

    .main-content .description-content,
    .main-content .features-content {
      padding: 0 20px 20px;
      font-size: 14px;
    }
  }

  @media (max-width: 480px) {
    .accommodation-title {
      font-size: 24px;
    }

    .meta-item {
      font-size: 14px;
    }

    .review-btn,
    .share-btn {
      padding: 10px 20px;
      font-size: 14px;
    }

    .main-content .card-title {
      font-size: 16px;
    }

    .sidebar-content .card-title {
      font-size: 16px;
    }
  }
}

/* 企业级住宿详情新版覆盖 */
.accommodation-detail-container {
  background:
    linear-gradient(180deg, #efe6d4 0%, #f5efe3 42%, #eef3ef 100%);
  color: #17211d;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", "PingFang SC", "Microsoft YaHei", sans-serif;
}

.accommodation-detail-container .section-container {
  width: min(calc(100% - 64px), 1520px);
}

.accommodation-detail-container .detail-hero-section {
  height: 480px;
  min-height: 360px;
}

.accommodation-detail-container .overlay-gradient {
  background:
    linear-gradient(90deg, rgba(54, 40, 24, 0.5) 0%, rgba(54, 40, 24, 0.22) 48%, rgba(10, 20, 18, 0.42) 100%),
    linear-gradient(180deg, rgba(255, 215, 142, 0.08) 0%, rgba(8, 18, 17, 0.38) 100%);
}

.accommodation-detail-container .hero-content {
  width: min(calc(100% - 64px), 1320px);
  text-align: center;
}

.accommodation-detail-container .breadcrumb {
  margin-bottom: 26px;
  text-align: center;
}

.accommodation-detail-container .breadcrumb :deep(.el-breadcrumb__inner) {
  color: rgba(255, 255, 255, 0.9);
  font-weight: 700;
  text-shadow: 0 2px 10px rgba(0, 0, 0, 0.24);
}

.accommodation-detail-container .breadcrumb :deep(.el-breadcrumb__item:last-child .el-breadcrumb__inner),
.accommodation-detail-container .breadcrumb :deep(.el-breadcrumb__inner:hover),
.accommodation-detail-container .breadcrumb :deep(.el-breadcrumb__separator) {
  color: #fff;
}

.accommodation-detail-container .accommodation-title {
  max-width: 1120px;
  margin: 0 auto 22px;
  color: #fff;
  font-size: clamp(32px, 4.4vw, 56px);
  font-weight: 800;
  line-height: 1.16;
  letter-spacing: 0;
  text-align: center;
  text-shadow: 0 5px 18px rgba(0, 0, 0, 0.36);
}

.accommodation-detail-container .accommodation-meta {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 14px;
  margin: 28px 0 0;
}

.accommodation-detail-container .meta-item {
  display: inline-flex;
  min-height: 38px;
  align-items: center;
  gap: 7px;
  padding: 0 12px;
  border: 1px solid rgba(255, 255, 255, 0.22);
  border-radius: 10px;
  background: rgba(24, 24, 24, 0.24);
  color: #fff;
  backdrop-filter: blur(10px);
  box-shadow: 0 10px 28px rgba(0, 0, 0, 0.16);
  font-size: 14px;
  font-weight: 700;
  text-shadow: none;
}

.accommodation-detail-container .action-buttons {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 12px;
  margin-top: 30px;
}

.accommodation-detail-container .review-btn,
.accommodation-detail-container .share-btn {
  height: 48px;
  min-width: 128px;
  padding: 0 24px;
  border-radius: 999px;
  border-width: 1px;
  font-size: 15px;
  font-weight: 700;
  box-shadow: none;
}

.accommodation-detail-container .review-btn {
  border-color: transparent;
  background: #0f766e;
  color: #fff;
}

.accommodation-detail-container .share-btn {
  border-color: rgba(255, 255, 255, 0.38);
  background: rgba(24, 24, 24, 0.2);
  color: #fff;
}

.accommodation-detail-container .detail-content,
.accommodation-detail-container .loading-section,
.accommodation-detail-container .empty-state {
  position: relative;
  padding: 34px 0 78px;
  background:
    linear-gradient(180deg, #efe6d4 0%, #f7f1e7 38%, #edf3ef 100%);
}

.accommodation-detail-container .detail-content::before,
.accommodation-detail-container .loading-section::before,
.accommodation-detail-container .empty-state::before {
  content: "";
  position: absolute;
  inset: 0;
  background:
    linear-gradient(90deg, rgba(22, 69, 58, 0.055) 1px, transparent 1px),
    linear-gradient(180deg, rgba(22, 69, 58, 0.045) 1px, transparent 1px);
  background-size: 48px 48px;
  pointer-events: none;
}

.accommodation-detail-container .detail-content > *,
.accommodation-detail-container .loading-section > *,
.accommodation-detail-container .empty-state > * {
  position: relative;
}

.accommodation-detail-container .content-grid {
  display: grid;
  grid-template-columns: minmax(0, 1040px) 400px;
  gap: 40px;
  align-items: start;
  justify-content: center;
}

.accommodation-detail-container .main-content,
.accommodation-detail-container .sidebar-content {
  min-width: 0;
}

.accommodation-detail-container .info-card {
  border: 1px solid #dce5e1;
  border-radius: 10px;
  background:
    linear-gradient(180deg, rgba(255, 252, 246, 0.94), rgba(250, 252, 250, 0.96));
  box-shadow: 0 18px 42px rgba(23, 33, 29, 0.08);
  overflow: hidden;
}

.accommodation-detail-container .main-content .info-card {
  margin-bottom: 24px;
  padding: 44px 64px 48px;
}

.accommodation-detail-container .sidebar-content {
  position: sticky;
  top: 0;
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.accommodation-detail-container .sidebar-content .info-card {
  margin-bottom: 0;
  padding: 22px;
}

.accommodation-detail-container .card-title {
  display: flex;
  align-items: center;
  gap: 9px;
  margin: 0 0 20px;
  padding: 0 0 16px;
  border-bottom: 1px solid #e6eeea;
  color: #17211d;
  font-size: 22px;
  font-weight: 800;
  letter-spacing: 0;
}

.accommodation-detail-container .card-title .el-icon {
  color: #0f766e;
  font-size: 22px;
}

.accommodation-detail-container .description-content,
.accommodation-detail-container .features-content {
  max-width: 900px;
  margin: 0 auto;
  padding: 0;
  color: #33423d;
  font-size: 16px;
  line-height: 2;
  white-space: pre-line;
}

.accommodation-detail-container .sidebar-content .card-title {
  margin-bottom: 0;
  font-size: 18px;
}

.accommodation-detail-container .sidebar-content .card-title .el-icon {
  font-size: 18px;
}

.accommodation-detail-container .info-list {
  padding: 0;
}

.accommodation-detail-container .info-item {
  display: grid;
  grid-template-columns: 108px minmax(0, 1fr);
  gap: 14px;
  align-items: center;
  padding: 16px 0;
  border-bottom: 0;
  border-top: 1px solid #e6eeea;
}

.accommodation-detail-container .info-label {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  color: #66736e;
  font-size: 14px;
}

.accommodation-detail-container .info-label .el-icon {
  color: #0f766e;
}

.accommodation-detail-container .info-value {
  color: #17211d;
  font-size: 14px;
  font-weight: 700;
  text-align: right;
}

.accommodation-detail-container .info-value.price-value {
  color: #b45309;
  font-size: 18px;
}

.accommodation-detail-container .info-value.scenic-link {
  color: #0f766e;
  cursor: pointer;
}

.accommodation-detail-container .reviews-card .reviews-header,
.accommodation-detail-container .reviews-card .review-loading,
.accommodation-detail-container .reviews-card .review-list,
.accommodation-detail-container .reviews-card .review-pagination {
  padding: 0;
}

.accommodation-detail-container .reviews-card .reviews-header {
  margin-bottom: 8px;
}

.accommodation-detail-container .review-item {
  padding: 22px 0;
  border-bottom: 1px solid #e6eeea;
}

.accommodation-detail-container .review-item:last-child {
  border-bottom: 0;
  padding-bottom: 0;
}

.accommodation-detail-container .review-header {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: center;
  margin-bottom: 14px;
}

.accommodation-detail-container .review-user {
  display: flex;
  align-items: center;
  gap: 12px;
}

.accommodation-detail-container .review-user :deep(.el-avatar) {
  background: #e6eeea;
}

.accommodation-detail-container .user-info .username {
  display: block;
  color: #17211d;
  font-size: 15px;
  font-weight: 800;
}

.accommodation-detail-container .user-info .review-date,
.accommodation-detail-container .rating-score {
  color: #66736e;
  font-size: 13px;
}

.accommodation-detail-container .review-rating {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.accommodation-detail-container .review-text {
  color: #33423d;
  font-size: 15px;
  line-height: 1.9;
}

.accommodation-detail-container .review-actions {
  margin-top: 10px;
  text-align: right;
}

.accommodation-detail-container .empty-reviews {
  display: grid;
  place-items: center;
  padding: 42px 20px 18px;
  text-align: center;
}

.accommodation-detail-container .empty-reviews .empty-icon,
.accommodation-detail-container .empty-content .empty-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 58px;
  height: 58px;
  margin-bottom: 16px;
  border-radius: 14px;
  background: rgba(15, 118, 110, 0.1);
  color: #0f766e;
  font-size: 30px;
}

.accommodation-detail-container .empty-title {
  margin: 0 0 8px;
  color: #17211d;
  font-size: 18px;
  font-weight: 800;
}

.accommodation-detail-container .empty-desc {
  margin: 0;
  color: #66736e;
}

.accommodation-detail-container .scenic-list,
.accommodation-detail-container .accommodation-list {
  display: grid;
  gap: 12px;
  padding: 16px 0 0;
}

.accommodation-detail-container .scenic-item,
.accommodation-detail-container .accommodation-item {
  display: grid;
  grid-template-columns: 76px minmax(0, 1fr);
  gap: 12px;
  align-items: center;
  padding: 12px;
  border: 1px solid #e6eeea;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.52);
  cursor: pointer;
  transition: transform 0.18s ease, box-shadow 0.18s ease, border-color 0.18s ease;
}

.accommodation-detail-container .scenic-item:hover,
.accommodation-detail-container .accommodation-item:hover {
  border-color: #9ccfc6;
  box-shadow: 0 14px 28px rgba(23, 33, 29, 0.08);
  transform: translateY(-2px);
}

.accommodation-detail-container .scenic-image,
.accommodation-detail-container .accommodation-image {
  width: 76px;
  height: 64px;
  border-radius: 7px;
  overflow: hidden;
}

.accommodation-detail-container .scenic-image img,
.accommodation-detail-container .accommodation-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.accommodation-detail-container .scenic-name,
.accommodation-detail-container .item-name {
  overflow: hidden;
  color: #17211d;
  font-size: 14px;
  font-weight: 800;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.accommodation-detail-container .scenic-price,
.accommodation-detail-container .item-price {
  margin-top: 6px;
  color: #b45309;
  font-size: 13px;
  font-weight: 700;
}

.accommodation-detail-container .item-rating {
  margin-top: 5px;
}

.accommodation-detail-container .empty-state {
  min-height: 420px;
}

.accommodation-detail-container .empty-content {
  max-width: 460px;
  margin: 0 auto;
  padding: 56px 28px;
  border: 1px solid #dce5e1;
  border-radius: 10px;
  background:
    linear-gradient(180deg, rgba(255, 252, 246, 0.94), rgba(250, 252, 250, 0.96));
  text-align: center;
  box-shadow: 0 18px 42px rgba(23, 33, 29, 0.08);
}

@media (max-width: 1180px) {
  .accommodation-detail-container .section-container {
    width: min(calc(100% - 48px), 900px);
  }

  .accommodation-detail-container .content-grid {
    grid-template-columns: 1fr;
  }

  .accommodation-detail-container .sidebar-content {
    position: static;
    order: -1;
  }
}

@media (max-width: 768px) {
  .accommodation-detail-container .detail-hero-section {
    height: 360px;
    min-height: 320px;
  }

  .accommodation-detail-container .hero-content {
    width: min(calc(100% - 40px), 900px);
  }

  .accommodation-detail-container .accommodation-title {
    font-size: 32px;
  }

  .accommodation-detail-container .accommodation-meta {
    gap: 10px;
  }

  .accommodation-detail-container .detail-content {
    padding: 28px 0 54px;
  }

  .accommodation-detail-container .main-content .info-card,
  .accommodation-detail-container .sidebar-content .info-card {
    padding: 22px;
  }

  .accommodation-detail-container .review-header,
  .accommodation-detail-container .info-item {
    grid-template-columns: 1fr;
  }

  .accommodation-detail-container .review-header {
    align-items: flex-start;
    flex-direction: column;
  }

  .accommodation-detail-container .info-value {
    text-align: left;
  }
}
</style>
