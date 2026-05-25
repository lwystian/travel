<template>
  <div class="tour-detail-container">
    <!-- Hero区域 -->
    <div class="detail-hero-section">
      <div class="hero-image-container">
        <div class="image-wrapper">
          <img :src="currentImage" :alt="tour.title" class="hero-image" />
          <div class="image-overlay">
            <div class="overlay-gradient"></div>
            <div class="hero-content">
              <div class="breadcrumb">
                <el-breadcrumb separator="/">
                  <el-breadcrumb-item @click="$router.push('/')">首页</el-breadcrumb-item>
                  <el-breadcrumb-item @click="$router.push('/tickets')">行程列表</el-breadcrumb-item>
                  <el-breadcrumb-item>{{ tour.title }}</el-breadcrumb-item>
                </el-breadcrumb>
              </div>
              <div class="tour-tags" v-if="tour.tags">
                <span
                  v-for="tag in parsedTags"
                  :key="tag"
                  class="tour-tag"
                >
                  {{ tag }}
                </span>
              </div>
              <h1 class="tour-title">{{ tour.title }}</h1>
              <p class="tour-subtitle" v-if="tour.subtitle">{{ tour.subtitle }}</p>
              <div class="tour-meta">
                <div class="meta-item" v-if="tour.days">
                  <el-icon><Timer /></el-icon>
                  <span>{{ tour.days }}天{{ tour.days - 1 }}晚</span>
                </div>
                <div class="meta-item" v-if="tour.city">
                  <el-icon><Location /></el-icon>
                  <span>出发地: {{ formatCity(tour.city) }}</span>
                </div>
                <div class="meta-item" v-if="tour.destination">
                  <el-icon><MapLocation /></el-icon>
                  <span>目的地: {{ formatCity(tour.destination) }}</span>
                </div>
              </div>
              <div class="tour-stats">
                <div class="stat-item" v-if="tour.starRating">
                  <el-icon><Star /></el-icon>
                  <span class="stat-value">{{ tour.starRating }}</span>
                  <span class="stat-label">分</span>
                </div>
                <div class="stat-item" v-if="tour.enrolledCount">
                  <el-icon><User /></el-icon>
                  <span class="stat-value">{{ tour.enrolledCount }}</span>
                  <span class="stat-label">人已报名</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 主内容区域 -->
    <div class="detail-content">
      <div class="section-container">
        <div class="content-grid">
          <!-- 左侧主要内容 -->
          <div class="main-content">
            <!-- 行程特色 -->
            <div class="info-card" v-if="features.length > 0">
              <h3 class="card-title">
                <el-icon><MagicStick /></el-icon>
                行程特色
              </h3>
              <div class="features-list">
                <div
                  v-for="(feature, index) in features"
                  :key="index"
                  class="feature-item"
                >
                  <div class="feature-icon">
                    <el-icon><CircleCheck /></el-icon>
                  </div>
                  <span>{{ feature }}</span>
                </div>
              </div>
            </div>

            <!-- 产品详情 -->
            <div class="info-card">
              <h3 class="card-title">
                <el-icon><Document /></el-icon>
                产品详情
              </h3>
              <div class="detail-info">
                <div class="detail-row">
                  <span class="detail-label">行程编号</span>
                  <span class="detail-value">{{ tour.code || '暂无' }}</span>
                </div>
                <div class="detail-row">
                  <span class="detail-label">行程天数</span>
                  <span class="detail-value">{{ tour.days || 0 }}天{{ (tour.days || 0) - 1 }}晚</span>
                </div>
                <div class="detail-row">
                  <span class="detail-label">出发月份</span>
                  <span class="detail-value">{{ tour.month || '全年' }}</span>
                </div>
                <div class="detail-row">
                  <span class="detail-label">行程类型</span>
                  <span class="detail-value">{{ getTourTypeName(tour.tourType) }}</span>
                </div>
                <div class="detail-row" v-if="tour.theme">
                  <span class="detail-label">产品主题</span>
                  <span class="detail-value">{{ getThemeName(tour.theme) }}</span>
                </div>
                <div class="detail-row" v-if="tour.recommendDate">
                  <span class="detail-label">推荐日期</span>
                  <span class="detail-value">{{ tour.recommendDate }}</span>
                </div>
              </div>
            </div>

            <!-- 供应商信息 -->
            <div class="info-card">
              <h3 class="card-title">
                <el-icon><Shop /></el-icon>
                供应商信息
              </h3>
              <div class="supplier-info">
                <div class="supplier-logo">
                  <el-icon><OfficeBuilding /></el-icon>
                </div>
                <div class="supplier-detail">
                  <div class="supplier-name">重庆侠客行国际旅行社有限公司</div>
                  <div class="supplier-desc">正规资质 · 品质保障</div>
                </div>
              </div>
            </div>

            <!-- 退订政策 -->
            <div class="info-card">
              <h3 class="card-title">
                <el-icon><Refresh /></el-icon>
                退订政策
              </h3>
              <div class="refund-policy">
                <div class="policy-item">
                  <el-icon><Check /></el-icon>
                  <span>支持退订</span>
                </div>
                <div class="policy-item">
                  <el-icon><Clock /></el-icon>
                  <span>特殊原因退订保障</span>
                </div>
              </div>
            </div>
          </div>

          <!-- 右侧预订区域 -->
          <div class="booking-sidebar">
            <div class="booking-card">
              <div class="price-section">
                <div class="price-label">起售价</div>
                <div class="price-value">
                  <span class="currency">¥</span>
                  <span class="amount">{{ minPrice }}</span>
                  <span class="unit">/人</span>
                </div>
              </div>

              <div class="booking-form">
                <div class="form-item">
                  <label>选择套餐</label>
                  <el-select v-model="selectedPackageId" placeholder="请选择套餐" class="package-select">
                    <el-option
                      v-for="pkg in packages"
                      :key="pkg.id"
                      :label="pkg.name"
                      :value="pkg.id"
                    >
                      <span>{{ pkg.name }}</span>
                      <span class="package-price">¥{{ pkg.adultPrice }}</span>
                    </el-option>
                  </el-select>
                </div>

                <div class="form-item">
                  <label>出发日期</label>
                  <el-select v-model="selectedBatchId" placeholder="请选择出发日期" class="package-select">
                    <el-option
                      v-for="batch in batches"
                      :key="batch.id"
                      :label="`${batch.departureDate} (${batch.status})`"
                      :value="batch.id"
                      :disabled="batch.remaining <= 0"
                    >
                      <span>{{ batch.departureDate }}</span>
                      <span class="batch-status" :class="batch.status === '可报名' ? 'available' : 'full'">
                        {{ batch.status }} {{ batch.remaining > 0 ? `剩${batch.remaining}` : '' }}
                      </span>
                    </el-option>
                  </el-select>
                </div>

                <div class="form-item">
                  <label>出游人数</label>
                  <el-input-number
                    v-model="adultCount"
                    :min="1"
                    :max="10"
                    @change="calculateTotal"
                  />
                  <span class="count-label">成人</span>
                </div>

                <div class="total-section">
                  <div class="total-row">
                    <span>成人单价</span>
                    <span>¥{{ selectedPackage?.adultPrice || 0 }}</span>
                  </div>
                  <div class="total-row">
                    <span>日期加价</span>
                    <span>¥{{ dateExtraFee }}</span>
                  </div>
                  <div class="total-row total">
                    <span>总价</span>
                    <span class="total-price">¥{{ totalPrice }}</span>
                  </div>
                </div>

                <el-button type="primary" class="booking-btn" @click="handleBooking">
                  立即预订
                </el-button>

                <div class="booking-tips">
                  <p>周边游提前1天，国内游提前3天</p>
                  <p>出境游提前3-5天，APP和短信群发出团通知</p>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 图片展示区域 -->
    <div class="images-section" v-if="images.length > 0">
      <div class="section-container">
        <h2 class="section-title">
          <el-icon><Picture /></el-icon>
          景点图片
        </h2>
        <div class="images-grid">
          <div
            v-for="(img, index) in images"
            :key="index"
            class="image-item"
            @click="currentImageIndex = index"
          >
            <img :src="getImageUrl(img)" :alt="`图片${index + 1}`" />
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'
import {
  Timer, Location, MapLocation, Star, User, Document, MagicStick,
  CircleCheck, Shop, OfficeBuilding, Refresh, Check, Clock, Picture
} from '@element-plus/icons-vue'
import noImage from '@/assets/images/no-image.png'

const route = useRoute()
const router = useRouter()

const baseAPI = import.meta.env.VITE_BASE_API || '/api'

// 行程数据
const tour = ref({})
const tourDetail = ref(null)
const images = ref([])
const currentImageIndex = ref(0)

// 套餐和批次
const packages = ref([])
const batches = ref([])
const selectedPackageId = ref(null)
const selectedBatchId = ref(null)
const adultCount = ref(1)
const dateExtraFee = ref(0)

const currentImage = computed(() => {
  if (images.value.length > 0 && currentImageIndex.value < images.value.length) {
    return getImageUrl(images.value[currentImageIndex.value])
  }
  return noImage
})

const parsedTags = computed(() => {
  if (!tour.value.tags) return []
  if (typeof tour.value.tags === 'string') {
    return tour.value.tags.split(/[,，]/).filter(t => t.trim())
  }
  return Array.isArray(tour.value.tags) ? tour.value.tags : []
})

const features = computed(() => {
  if (!tour.value.feature) return []
  return tour.value.feature.split(/[,，]/).filter(f => f.trim())
})

const minPrice = computed(() => {
  if (packages.value.length > 0) {
    const prices = packages.value.map(p => p.adultPrice).filter(p => p)
    return prices.length > 0 ? Math.min(...prices).toLocaleString() : '暂无'
  }
  return tour.value.minPrice ? tour.value.minPrice.toLocaleString() : '暂无'
})

const selectedPackage = computed(() => {
  return packages.value.find(p => p.id === selectedPackageId.value)
})

const totalPrice = computed(() => {
  const packagePrice = selectedPackage.value?.adultPrice || 0
  return (packagePrice + dateExtraFee.value) * adultCount.value
})

// 格式化城市名称
const formatCity = (city) => {
  const cityMap = {
    'chongqing': '重庆',
    'chengdu': '成都',
    'kunming': '昆明',
    'guiyang': '贵阳',
    'sanya': '三亚',
    'xisha': '西沙群岛',
    'sanxia': '三峡',
    'sanyan': '三峡',
    'beijing': '北京',
    'shanghai': '上海'
  }
  return cityMap[city] || city || ''
}

// 获取行程类型名称
const getTourTypeName = (type) => {
  const typeMap = {
    'around': '周边游',
    'long': '长线游',
    'team': '跟团游',
    'cruise': '邮轮出行'
  }
  return typeMap[type] || '旅行'
}

// 获取主题名称
const getThemeName = (theme) => {
  const themeMap = {
    'scenic': '风景游',
    'cultural': '文化游',
    'adventure': '探险游',
    'hiking': '徒步游',
    'family': '亲子游',
    'romantic': '浪漫游',
    'food': '美食游',
    'photography': '摄影游'
  }
  return themeMap[theme] || theme || ''
}

// 获取图片URL
const getImageUrl = (url) => {
  if (!url) return noImage
  if (url.startsWith('http')) return url
  return baseAPI + url
}

// 计算总价
const calculateTotal = () => {
  if (selectedBatchId.value) {
    const batch = batches.value.find(b => b.id === selectedBatchId.value)
    dateExtraFee.value = batch?.adultDateExtraFee || 0
  }
}

// 获取行程详情
const fetchTourDetail = async () => {
  const id = route.params.id
  try {
    const res = await request.get(`/tour/${id}/detail`)
    tourDetail.value = res
    tour.value = res.tour || {}
    packages.value = res.tripPackages || []
    batches.value = res.batchDates || []

    // 解析图片
    if (res.images) {
      images.value = res.images.thumbnails || []
    }

    // 默认选中第一个套餐和日期
    if (packages.value.length > 0) {
      selectedPackageId.value = packages.value[0].id
    }
    if (batches.value.length > 0) {
      const firstAvailable = batches.value.find(b => b.remaining > 0)
      if (firstAvailable) {
        selectedBatchId.value = firstAvailable.id
        dateExtraFee.value = firstAvailable.adultDateExtraFee || 0
      }
    }
  } catch (error) {
    console.error('获取行程详情失败:', error)
    ElMessage.error('获取行程详情失败')
  }
}

// 预订
const handleBooking = () => {
  if (!selectedPackageId.value) {
    ElMessage.warning('请选择套餐')
    return
  }
  if (!selectedBatchId.value) {
    ElMessage.warning('请选择出发日期')
    return
  }

  // 跳转到预订确认页
  router.push({
    path: `/tour-order-confirm/${route.params.id}`,
    query: {
      packageId: selectedPackageId.value,
      batchId: selectedBatchId.value,
      adultCount: adultCount.value
    }
  })
}

onMounted(() => {
  fetchTourDetail()
})
</script>

<style lang="scss" scoped>
.tour-detail-container {
  min-height: 100vh;
  background: #f5f7fa;
}

.detail-hero-section {
  position: relative;
  height: 400px;
}

.hero-image-container {
  position: absolute;
  inset: 0;
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
}

.image-overlay {
  position: absolute;
  inset: 0;
  background: linear-gradient(to bottom, rgba(0,0,0,0.3) 0%, rgba(0,0,0,0.7) 100%);
}

.overlay-gradient {
  position: absolute;
  inset: 0;
}

.hero-content {
  position: relative;
  z-index: 1;
  max-width: 1680px;
  margin: 0 auto;
  padding: 40px 24px;
  height: 100%;
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
  color: #fff;
}

.breadcrumb {
  margin-bottom: 16px;

  :deep(.el-breadcrumb__item) {
    color: rgba(255,255,255,0.8);
    cursor: pointer;

    &:hover {
      color: #fff;
    }
  }

  :deep(.el-breadcrumb__separator) {
    color: rgba(255,255,255,0.6);
  }
}

.tour-tags {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
}

.tour-tag {
  padding: 4px 12px;
  background: rgba(255,255,255,0.2);
  border-radius: 20px;
  font-size: 12px;
  backdrop-filter: blur(4px);
}

.tour-title {
  font-size: 32px;
  font-weight: 700;
  margin: 0 0 8px;
  text-shadow: 0 2px 4px rgba(0,0,0,0.3);
}

.tour-subtitle {
  font-size: 16px;
  opacity: 0.9;
  margin: 0 0 16px;
}

.tour-meta {
  display: flex;
  gap: 24px;
  margin-bottom: 12px;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
}

.tour-stats {
  display: flex;
  gap: 24px;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 6px;

  .stat-value {
    font-size: 20px;
    font-weight: 700;
    color: #ffd700;
  }

  .stat-label {
    font-size: 14px;
    opacity: 0.8;
  }
}

.detail-content {
  padding: 40px 0;
}

.section-container {
  max-width: 1680px;
  margin: 0 auto;
  padding: 0 24px;
}

.content-grid {
  display: grid;
  grid-template-columns: 1fr 380px;
  gap: 32px;

  @media (max-width: 1024px) {
    grid-template-columns: 1fr;
  }
}

.main-content {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.info-card {
  background: #fff;
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.06);
}

.card-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 18px;
  font-weight: 600;
  margin: 0 0 20px;
  color: #2d3748;

  .el-icon {
    color: #667eea;
  }
}

.features-list {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;

  @media (max-width: 768px) {
    grid-template-columns: 1fr;
  }
}

.feature-item {
  display: flex;
  align-items: flex-start;
  gap: 10px;

  .feature-icon {
    width: 24px;
    height: 24px;
    border-radius: 50%;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;

    .el-icon {
      color: #fff;
      font-size: 14px;
    }
  }

  span {
    font-size: 14px;
    color: #4a5568;
    line-height: 1.5;
  }
}

.detail-info {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.detail-row {
  display: flex;
  padding: 12px 0;
  border-bottom: 1px solid #f1f5f9;

  &:last-child {
    border-bottom: none;
  }

  .detail-label {
    width: 100px;
    color: #64748b;
    font-size: 14px;
  }

  .detail-value {
    flex: 1;
    color: #2d3748;
    font-size: 14px;
    font-weight: 500;
  }
}

.supplier-info {
  display: flex;
  align-items: center;
  gap: 16px;

  .supplier-logo {
    width: 56px;
    height: 56px;
    border-radius: 12px;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    display: flex;
    align-items: center;
    justify-content: center;

    .el-icon {
      font-size: 28px;
      color: #fff;
    }
  }

  .supplier-name {
    font-size: 16px;
    font-weight: 600;
    color: #2d3748;
  }

  .supplier-desc {
    font-size: 12px;
    color: #a0aec0;
    margin-top: 4px;
  }
}

.refund-policy {
  display: flex;
  gap: 24px;

  .policy-item {
    display: flex;
    align-items: center;
    gap: 8px;
    color: #48bb78;
    font-size: 14px;

    .el-icon {
      font-size: 18px;
    }
  }
}

.booking-sidebar {
  position: sticky;
  top: 24px;
  height: fit-content;
}

.booking-card {
  background: #fff;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 4px 20px rgba(0,0,0,0.1);
}

.price-section {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 24px;
  text-align: center;
  color: #fff;
}

.price-label {
  font-size: 14px;
  opacity: 0.9;
}

.price-value {
  display: flex;
  align-items: baseline;
  justify-content: center;
  margin-top: 8px;

  .currency {
    font-size: 18px;
  }

  .amount {
    font-size: 36px;
    font-weight: 700;
  }

  .unit {
    font-size: 14px;
    opacity: 0.8;
    margin-left: 4px;
  }
}

.booking-form {
  padding: 24px;
}

.form-item {
  margin-bottom: 20px;

  label {
    display: block;
    font-size: 14px;
    color: #4a5568;
    margin-bottom: 8px;
  }

  .count-label {
    margin-left: 12px;
    font-size: 14px;
    color: #64748b;
  }

  :deep(.el-input-number) {
    width: 120px;
  }
}

.package-select {
  width: 100%;
}

:deep(.el-select-dropdown__item) {
  display: flex;
  justify-content: space-between;

  .package-price, .batch-status {
    color: #667eea;
    font-weight: 600;
  }

  .batch-status {
    &.available {
      color: #48bb78;
    }
    &.full {
      color: #e53e3e;
    }
  }
}

.total-section {
  background: #f7fafc;
  border-radius: 12px;
  padding: 16px;
  margin-bottom: 20px;
}

.total-row {
  display: flex;
  justify-content: space-between;
  font-size: 14px;
  color: #64748b;
  margin-bottom: 8px;

  &:last-child {
    margin-bottom: 0;
  }

  &.total {
    padding-top: 12px;
    border-top: 1px dashed #e2e8f0;
    margin-top: 8px;
    font-weight: 600;
    color: #2d3748;

    .total-price {
      font-size: 24px;
      color: #e53e3e;
      font-weight: 700;
    }
  }
}

.booking-btn {
  width: 100%;
  height: 48px;
  font-size: 16px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;

  &:hover {
    opacity: 0.9;
    transform: translateY(-1px);
  }
}

.booking-tips {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #f1f5f9;

  p {
    font-size: 12px;
    color: #a0aec0;
    margin: 0 0 4px;
    text-align: center;
  }
}

.images-section {
  padding: 40px 0;
  background: #fff;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 20px;
  font-weight: 600;
  margin: 0 0 24px;
  color: #2d3748;

  .el-icon {
    color: #667eea;
  }
}

.images-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;

  @media (max-width: 768px) {
    grid-template-columns: repeat(2, 1fr);
  }
}

.image-item {
  border-radius: 12px;
  overflow: hidden;
  cursor: pointer;
  transition: transform 0.3s;

  &:hover {
    transform: scale(1.02);
  }

  img {
    width: 100%;
    height: 160px;
    object-fit: cover;
  }
}
</style>
