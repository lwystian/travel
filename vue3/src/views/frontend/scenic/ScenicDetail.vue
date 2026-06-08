<!-- eslint-disable -->
<template>
  <div class="scenic-detail-page" v-loading="loading">
    <section class="scenic-hero">
      <img :src="getImageUrl(scenic.imageUrl)" :alt="scenic.name || '景点图片'" class="hero-media" />
      <div class="hero-overlay"></div>

      <div class="hero-inner">
        <div class="breadcrumb">
          <el-breadcrumb separator="/">
            <el-breadcrumb-item @click="$router.push('/')">首页</el-breadcrumb-item>
            <el-breadcrumb-item @click="$router.push('/scenic')">景点列表</el-breadcrumb-item>
            <el-breadcrumb-item>景点详情</el-breadcrumb-item>
          </el-breadcrumb>
        </div>

        <div class="hero-panel">
          <div class="hero-copy">
            <h1>{{ scenic.name || '景点详情' }}</h1>
            <p>{{ heroDescription }}</p>

            <div class="hero-tags">
              <span v-if="scenic.location">
                <el-icon><Location /></el-icon>
                {{ scenic.location }}
              </span>
              <span v-if="scenic.categoryInfo?.name">
                <el-icon><CollectionTag /></el-icon>
                {{ scenic.categoryInfo.name }}
              </span>
              <span>
                <el-icon><Star /></el-icon>
                {{ getDisplayRating(scenic.rating) }} / 5.0
              </span>
            </div>

            <div class="hero-actions">
              <el-button
                :type="isCollected ? 'danger' : 'primary'"
                size="large"
                :loading="collectionLoading"
                @click="handleCollection"
              >
                <el-icon>
                  <StarFilled v-if="isCollected" />
                  <Star v-else />
                </el-icon>
                {{ isCollected ? '已收藏' : '收藏景点' }}
              </el-button>
              <el-button size="large" class="ghost-button" @click="handleShare">
                <el-icon><Share /></el-icon>
                分享
              </el-button>
            </div>
          </div>
        </div>
      </div>
    </section>

    <main class="scenic-main">
      <section class="content-band">
        <div class="section-container detail-layout">
          <article class="profile-panel">
            <h2>景点概览</h2>
            <div
              v-if="scenicDescriptionContent"
              class="description-prose content-display w-e-text-container"
            >
              <div class="w-e-scroll">
                <div
                  v-html="scenicDescriptionContent"
                  class="scenic-editor-view"
                  data-slate-editor="true"
                ></div>
              </div>
            </div>
            <div v-else class="description-prose empty-description">
              精选目的地信息正在完善中，您可以查看开放时间、地理位置、相关行程和游客评价。
            </div>
          </article>

          <aside class="planning-panel">
            <h2>出行信息</h2>
            <div class="info-list">
              <div class="info-row">
                <span>
                  <el-icon><Timer /></el-icon>
                  开放时间
                </span>
                <strong>{{ scenic.openingHours || '全天开放' }}</strong>
              </div>
              <div class="info-row">
                <span>
                  <el-icon><Location /></el-icon>
                  位置
                </span>
                <strong>{{ scenic.location || '待完善' }}</strong>
              </div>
              <div class="info-row" v-if="scenic.categoryInfo?.name">
                <span>
                  <el-icon><CollectionTag /></el-icon>
                  类型
                </span>
                <strong>{{ scenic.categoryInfo.name }}</strong>
              </div>
              <div class="info-row" v-if="hasCoordinates">
                <span>
                  <el-icon><InfoFilled /></el-icon>
                  坐标
                </span>
                <button type="button" class="coordinate-link" @click="copyCoordinates">
                  {{ scenic.longitude }}, {{ scenic.latitude }}
                  <el-icon><CopyDocument /></el-icon>
                </button>
              </div>
            </div>
          </aside>
        </div>
      </section>

      <section class="weather-section" v-if="weather.now || weatherLoading || weatherForecast.length > 0">
        <div class="section-container">
          <div class="section-heading">
            <div>
              <h2>
                <el-icon><Sunny /></el-icon>
                出行天气
              </h2>
            </div>
            <span v-if="weather.updateTime">更新时间 {{ formatWeatherTime(weather.updateTime) }}</span>
          </div>

          <div v-if="weather.now" class="weather-panel">
            <div class="weather-current">
              <span>当前天气</span>
              <strong>{{ weather.now.temp }}°C</strong>
              <em>{{ weather.now.text }}</em>
            </div>
            <div class="weather-metrics">
              <div>
                <span>体感温度</span>
                <strong>{{ weather.now.feelsLike }}°C</strong>
              </div>
              <div>
                <span>湿度</span>
                <strong>{{ weather.now.humidity }}%</strong>
              </div>
              <div>
                <span>风向</span>
                <strong>{{ weather.now.windDir || '-' }}</strong>
              </div>
              <div>
                <span>风速</span>
                <strong>{{ weather.now.windSpeed }} km/h</strong>
              </div>
            </div>
          </div>

          <div v-else-if="weatherLoading" class="loading-line">
            <el-icon class="loading-icon"><Loading /></el-icon>
            <span>获取天气信息中...</span>
          </div>

          <div class="forecast-list" v-if="weatherForecast.length > 0">
            <div v-for="(day, index) in weatherForecast.slice(0, 4)" :key="index" class="forecast-item">
              <span>{{ formatForecastDate(day.fxDate) }}</span>
              <strong>{{ day.textDay }}</strong>
              <em>{{ day.tempMin }}° / {{ day.tempMax }}°</em>
            </div>
          </div>
        </div>
      </section>

      <section class="map-section">
        <div class="section-container">
          <div class="section-heading">
            <div>
              <h2>
                <el-icon><Location /></el-icon>
                地理位置
              </h2>
            </div>
            <span>{{ scenic.location || '景区位置' }}</span>
          </div>
          <div class="map-shell">
            <div id="scenic-map-container" class="map-container"></div>
          </div>
        </div>
      </section>

      <section class="recommended-section" v-if="recommendedTours.length > 0">
        <div class="section-container">
          <div class="section-heading">
            <div>
              <h2>
                <el-icon><Tickets /></el-icon>
                相关行程推荐
              </h2>
            </div>
            <span>根据 {{ scenic.name || '当前景点' }} 匹配精选行程</span>
          </div>

          <div class="tour-grid">
            <article
              v-for="tour in recommendedTours"
              :key="tour.id"
              class="tour-card"
              @click.stop="goToTourDetail(tour.id)"
            >
              <div class="tour-image">
                <img :src="getTourImage(tour)" :alt="tour.title" />
                <span>{{ getTourTypeName(tour.tourType) }}</span>
              </div>
              <div class="tour-body">
                <h3>{{ tour.title }}</h3>
                <div class="tour-meta">
                  <span v-if="tour.destination">
                    <el-icon><Location /></el-icon>
                    {{ formatCity(tour.destination) }}
                  </span>
                  <span v-if="tour.days">{{ tour.days }}天</span>
                  <span v-if="tour.starRating">
                    <el-icon><Star /></el-icon>
                    {{ tour.starRating }}
                  </span>
                </div>
                <div class="tour-footer">
                  <strong v-if="tour.minPrice !== undefined && tour.minPrice !== null && tour.minPrice !== ''">
                    ¥{{ formatPrice(tour.minPrice) }} 起
                  </strong>
                  <em v-if="tour.enrolledCount">{{ tour.enrolledCount }}人已报名</em>
                </div>
              </div>
            </article>
          </div>
        </div>
      </section>

      <section class="comment-section" v-if="publicInteractionEnabled">
        <div class="section-container">
          <div class="section-heading">
            <div>
              <h2>
                <el-icon><ChatDotRound /></el-icon>
                游客评价
              </h2>
            </div>
            <span>{{ formatReviewCount(scenic.reviewCount) }}</span>
          </div>
          <div class="comment-shell">
            <CommentList />
          </div>
        </div>
      </section>
    </main>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, computed, nextTick, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import request from '@/utils/request'
import CommentList from '@/views/frontend/comment/CommentList.vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/store/user'
import axios from 'axios'
import { shareCurrentPage } from '@/utils/share'
import { updateSeo, seoDescription } from '@/utils/seo'
import { getTourTypeLabel } from '@/utils/tourTypes'
import { resolveImageUrl, resolveAbsoluteImageUrl } from '@/utils/imageUrl'
import { renderContent } from '@/utils/contentRenderer'
import { loadAmap } from '@/utils/amap'
import { usePublicInteraction } from '@/utils/publicInteraction'
import {
  Location, CollectionTag, Timer, Sunny, Loading, Star, StarFilled,
  InfoFilled, CopyDocument, Share, ChatDotRound, Tickets
} from '@element-plus/icons-vue'
import noImage from '@/assets/images/no-image.png'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const scenic = ref({})
const isCollected = ref(false)
const collectionLoading = ref(false)
const recommendedTours = ref([])
const weather = ref({})
const weatherForecast = ref([])
const weatherLoading = ref(false)
const { publicInteractionEnabled, loadPublicInteractionConfig } = usePublicInteraction()

const isLoggedIn = computed(() => userStore.isLoggedIn)
const hasCoordinates = computed(() => Boolean(scenic.value.longitude && scenic.value.latitude))

const stripContentText = (value = '') => String(value || '')
  .replace(/<style[\s\S]*?<\/style>/gi, '')
  .replace(/<script[\s\S]*?<\/script>/gi, '')
  .replace(/<[^>]+>/g, ' ')
  .replace(/&nbsp;/gi, ' ')
  .replace(/&amp;/gi, '&')
  .replace(/&lt;/gi, '<')
  .replace(/&gt;/gi, '>')
  .replace(/&quot;/gi, '"')
  .replace(/&#39;/gi, "'")
  .replace(/\s+/g, ' ')
  .trim()

const limitText = (value = '', maxLength = 150) => {
  const text = stripContentText(value)
  return text.length > maxLength ? `${text.slice(0, maxLength)}...` : text
}

const heroDescription = computed(() => {
  return limitText(scenic.value.description) || '精选目的地信息正在完善中，您可以查看开放时间、地理位置、相关行程和游客评价。'
})
const scenicDescriptionContent = computed(() => renderContent(scenic.value.description || ''))

let map = null
let marker = null

const getImageUrl = (url) => resolveImageUrl(url, noImage)
const getAbsoluteImageUrl = (url) => resolveAbsoluteImageUrl(url, noImage)

const formatReviewCount = (count) => {
  if (!count || count === 0) return '暂无评价'
  if (count === 1) return '1条评价'
  return `${count}条评价`
}

const getDisplayRating = (rating) => {
  if (!rating) return '4.5'
  return parseFloat(rating).toFixed(1)
}

const initMap = () => {
  if (!window.AMap || !document.getElementById('scenic-map-container')) return
  const AMap = window.AMap
  if (map) {
    map.destroy()
    map = null
  }

  map = new AMap.Map('scenic-map-container', {
    zoom: 15,
    resizeEnable: true
  })

  map.on('complete', () => {
    if (hasCoordinates.value) {
      const lnglat = new AMap.LngLat(scenic.value.longitude, scenic.value.latitude)
      map.setCenter(lnglat)
      map.setZoom(16)

      marker = new AMap.Marker({
        position: lnglat,
        title: scenic.value.name,
        animation: 'AMAP_ANIMATION_DROP'
      })
      map.add(marker)

      const infoWindow = new AMap.InfoWindow({
        content: `<div class="info-window"><h4>${scenic.value.name || ''}</h4><p>${scenic.value.location || ''}</p><p>开放时间: ${scenic.value.openingHours || '全天开放'}</p></div>`,
        offset: new AMap.Pixel(0, -30)
      })

      marker.on('click', () => {
        infoWindow.open(map, marker.getPosition())
      })
      infoWindow.open(map, marker.getPosition())
    } else {
      map.setCenter(new AMap.LngLat(116.397428, 39.90923))
      map.setZoom(10)
    }

    map.plugin(['AMap.ToolBar', 'AMap.Scale'], () => {
      map.addControl(new AMap.ToolBar())
      map.addControl(new AMap.Scale())
    })
  })
}

const loadMapScript = () => {
  return loadAmap(['AMap.ToolBar', 'AMap.Scale'])
}

const formatWeatherTime = (timestamp) => {
  if (!timestamp) return ''
  const date = new Date(timestamp)
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')} ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
}

const formatForecastDate = (dateString) => {
  if (!dateString) return ''
  const date = new Date(dateString)
  const today = new Date()
  const tomorrow = new Date()
  tomorrow.setDate(today.getDate() + 1)

  if (date.toDateString() === today.toDateString()) return '今天'
  if (date.toDateString() === tomorrow.toDateString()) return '明天'

  const weekdays = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
  return weekdays[date.getDay()]
}

const fetchWeatherInfo = async (location) => {
  if (!location) return

  weatherLoading.value = true
  try {
    const key = '308d7421f676413aab8b4064aba532d7'
    const locationResponse = await axios.get('https://geoapi.qweather.com/v2/city/lookup', {
      params: { location, key, number: 1 }
    })

    if (locationResponse.data.code === '200' && locationResponse.data.location?.length > 0) {
      const { lon, lat } = locationResponse.data.location[0]
      const weatherResponse = await axios.get('https://devapi.qweather.com/v7/weather/now', {
        params: { key, location: `${lon},${lat}` }
      })
      if (weatherResponse.data.code === '200') {
        weather.value = weatherResponse.data
      }

      const forecastResponse = await axios.get('https://devapi.qweather.com/v7/weather/3d', {
        params: { key, location: `${lon},${lat}` }
      })
      if (forecastResponse.data.code === '200') {
        weatherForecast.value = forecastResponse.data.daily
      }
    }
  } catch {
    weather.value = {}
    weatherForecast.value = []
  } finally {
    weatherLoading.value = false
  }
}

const fetchDetail = async () => {
  const id = route.params.id
  if (!id) return

  loading.value = true
  weather.value = {}
  weatherForecast.value = []
  recommendedTours.value = []
  isCollected.value = false

  await request.get(`/scenic/${id}`, null, {
    showDefaultMsg: false,
    onSuccess: (res) => {
      scenic.value = res || {}
      const descriptionText = stripContentText(res.description || '')
      updateSeo({
        title: `${res.name}旅游攻略、门票与路线`,
        description: descriptionText || `${res.name}景点介绍、开放时间、周边住宿和推荐行程。`,
        path: `/scenic/${id}`,
        image: res.imageUrl,
        schema: {
          '@context': 'https://schema.org',
          '@type': 'TouristAttraction',
          name: res.name,
          description: seoDescription(descriptionText || res.location || `${res.name}景点介绍`),
          url: `${window.location.origin}/scenic/${id}`,
          image: res.imageUrl ? getAbsoluteImageUrl(res.imageUrl) : undefined,
          address: res.location || undefined
        }
      })

      if (publicInteractionEnabled.value) {
        fetchCommentStats(id)
      } else {
        scenic.value.reviewCount = 0
      }
      fetchWeatherInfo(res.location)
      fetchRecommendedTours(res.name, res.location)

      nextTick(() => {
        loadMapScript()
          .then(() => setTimeout(initMap, 300))
          .catch(() => {
            ElMessage.warning('地图配置未完成，暂时无法展示位置地图')
          })
      })

      if (isLoggedIn.value) {
        checkCollectionStatus(id)
      }
    },
    onError: () => {
      scenic.value = {}
    }
  }).finally(() => {
    loading.value = false
  })
}

const fetchCommentStats = async (scenicId) => {
  try {
    await request.get('/comment/page', {
      scenicId,
      currentPage: 1,
      size: 1
    }, {
      showDefaultMsg: false,
      onSuccess: (res) => {
        scenic.value.reviewCount = res?.total || 0
        if (res?.records?.length > 0) {
          fetchAllCommentsForRating(scenicId)
        }
      }
    })
  } catch {
    scenic.value.reviewCount = 0
  }
}

const fetchAllCommentsForRating = async (scenicId) => {
  try {
    await request.get(`/comment/scenic/${scenicId}`, null, {
      showDefaultMsg: false,
      onSuccess: (res) => {
        if (res?.length > 0) {
          const totalRating = res.reduce((sum, comment) => sum + (comment.rating || 0), 0)
          scenic.value.rating = (totalRating / res.length).toFixed(1)
        }
      }
    })
  } catch {
    scenic.value.rating = scenic.value.rating || ''
  }
}

const checkCollectionStatus = async (scenicId) => {
  if (!isLoggedIn.value) return

  try {
    await request.get(`/scenic-collection/is-collected/${scenicId}`, null, {
      showDefaultMsg: false,
      onSuccess: (res) => {
        isCollected.value = res
      }
    })
  } catch {
    isCollected.value = false
  }
}

const fetchRecommendedTours = async (scenicName, location) => {
  try {
    await request.get('/tour/recommended', {
      scenicName,
      location,
      limit: 6
    }, {
      showDefaultMsg: false,
      onSuccess: (res) => {
        recommendedTours.value = res || []
      },
      onError: () => {
        recommendedTours.value = []
      }
    })
  } catch {
    recommendedTours.value = []
  }
}

const getTourImage = (tour) => {
  if (!tour) return noImage
  if (tour.mainImage) return resolveImageUrl(tour.mainImage, noImage)
  if (tour.images) {
    try {
      const images = JSON.parse(tour.images)
      if (Array.isArray(images) && images.length > 0) {
        return resolveImageUrl(images[0], noImage)
      }
    } catch {
      return noImage
    }
  }
  return noImage
}

const formatPrice = (price) => {
  if (!price) return '0'
  const num = typeof price === 'string' ? parseFloat(price) : price
  return num.toLocaleString('zh-CN', { minimumFractionDigits: 0, maximumFractionDigits: 0 })
}

const getTourTypeName = (type) => getTourTypeLabel(type, '旅行')

const formatCity = (city) => {
  const cityMap = {
    chongqing: '重庆',
    chengdu: '成都',
    kunming: '昆明',
    guiyang: '贵阳',
    sanya: '三亚',
    xisha: '西沙群岛',
    sanxia: '三峡',
    sanyan: '三峡',
    beijing: '北京',
    shanghai: '上海',
    guangzhou: '广州',
    shenzhen: '深圳',
    xian: '西安',
    hangzhou: '杭州',
    suzhou: '苏州',
    nanjing: '南京',
    wuhan: '武汉',
    changsha: '长沙',
    haikou: '海口',
    dali: '大理',
    lijiang: '丽江',
    xiamen: '厦门',
    qingdao: '青岛'
  }
  return cityMap[city] || city || ''
}

const goToTourDetail = (tourId) => {
  router.push(`/ticket/booking/${tourId}`)
}

const handleCollection = async () => {
  if (!isLoggedIn.value) {
    ElMessageBox.confirm('收藏功能需要登录，是否前往登录页面？', '提示', {
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

  const scenicId = scenic.value.id
  if (!scenicId) return

  collectionLoading.value = true
  try {
    if (isCollected.value) {
      await request.delete(`/scenic-collection/${scenicId}`, { successMsg: '取消收藏成功' })
      isCollected.value = false
    } else {
      await request.post(`/scenic-collection/${scenicId}`, null, { successMsg: '收藏成功' })
      isCollected.value = true
    }
  } catch {
    ElMessage.error('操作失败，请稍后重试')
  } finally {
    collectionLoading.value = false
  }
}

const copyCoordinates = () => {
  if (!hasCoordinates.value) return

  const text = `${scenic.value.longitude}, ${scenic.value.latitude}`
  navigator.clipboard.writeText(text)
    .then(() => {
      ElMessage.success('坐标已复制到剪贴板')
    })
    .catch(() => {
      ElMessage.error('复制失败，请手动复制')
    })
}

const handleShare = () => {
  shareCurrentPage({
    title: scenic.value.name || '景点详情',
    text: heroDescription.value || ''
  })
}

watch(() => route.params.id, () => {
  fetchDetail()
})

onMounted(async () => {
  await loadPublicInteractionConfig()
  fetchDetail()
})

onUnmounted(() => {
  if (map) {
    map.destroy()
    map = null
  }
})
</script>

<style lang="scss" scoped>
.scenic-detail-page {
  min-height: 100vh;
  background:
    linear-gradient(180deg, #efe6d4 0%, #f5efe3 42%, #eef3ef 100%);
  color: #17211d;
}

.scenic-hero {
  position: relative;
  height: 480px;
  min-height: 360px;
  overflow: hidden;
}

.hero-media {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.hero-overlay {
  position: absolute;
  inset: 0;
  background:
    linear-gradient(90deg, rgba(54, 40, 24, 0.5) 0%, rgba(54, 40, 24, 0.22) 48%, rgba(10, 20, 18, 0.42) 100%),
    linear-gradient(180deg, rgba(255, 215, 142, 0.08) 0%, rgba(8, 18, 17, 0.38) 100%);
}

.hero-inner,
.section-container {
  width: min(calc(100% - 64px), 1320px);
  margin: 0 auto;
}

.section-container {
  width: min(calc(100% - 64px), 1520px);
}

.hero-inner {
  position: relative;
  z-index: 1;
  display: flex;
  height: 100%;
  flex-direction: column;
  justify-content: center;
  padding: 42px 0;
  text-align: center;
}

.breadcrumb {
  margin-bottom: 26px;

  :deep(.el-breadcrumb__inner) {
    color: rgba(255, 255, 255, 0.9);
    font-weight: 700;
    text-shadow: 0 2px 10px rgba(0, 0, 0, 0.24);
    cursor: pointer;
  }

  :deep(.el-breadcrumb__item:last-child .el-breadcrumb__inner),
  :deep(.el-breadcrumb__inner:hover),
  :deep(.el-breadcrumb__separator) {
    color: #fff;
  }
}

.hero-panel {
  max-width: 1120px;
  margin: 0 auto;
  text-align: center;
}

.hero-copy {
  max-width: 1120px;
  margin: 0 auto;
  text-align: center;
}

.hero-copy h1 {
  max-width: 1120px;
  margin: 0 auto 20px;
  color: #fff;
  font-size: clamp(32px, 4.4vw, 56px);
  font-weight: 800;
  line-height: 1.16;
  letter-spacing: 0;
  text-align: center;
  text-shadow: 0 5px 18px rgba(0, 0, 0, 0.36);
}

.hero-copy p {
  max-width: 760px;
  margin: 0 auto;
  color: rgba(255, 255, 255, 0.92);
  font-size: 18px;
  font-weight: 600;
  line-height: 1.8;
  text-align: center;
  text-shadow: 0 3px 12px rgba(0, 0, 0, 0.32);
}

.hero-tags {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 14px;
  margin-top: 28px;
}

.hero-tags span,
.meta-item {
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
  font-weight: 700;
  box-shadow: 0 10px 28px rgba(0, 0, 0, 0.16);
}

.hero-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 12px;
  margin-top: 30px;

  :deep(.el-button) {
    height: 48px;
    min-width: 128px;
    border-radius: 999px;
    font-weight: 700;
  }
}

.ghost-button {
  border-color: rgba(255, 255, 255, 0.38);
  background: rgba(24, 24, 24, 0.2);
  color: #fff;
}

.scenic-main {
  position: relative;
  background:
    linear-gradient(180deg, #efe6d4 0%, #f7f1e7 38%, #edf3ef 100%);
}

.scenic-main::before {
  content: "";
  position: absolute;
  inset: 0;
  background:
    linear-gradient(90deg, rgba(22, 69, 58, 0.055) 1px, transparent 1px),
    linear-gradient(180deg, rgba(22, 69, 58, 0.045) 1px, transparent 1px);
  background-size: 48px 48px;
  pointer-events: none;
}

.scenic-main > section {
  position: relative;
}

.content-band,
.weather-section,
.map-section,
.recommended-section,
.comment-section {
  padding: 34px 0 42px;
}

.detail-layout {
  display: grid;
  grid-template-columns: minmax(0, 1040px) 400px;
  gap: 40px;
  align-items: start;
  justify-content: center;
}

.profile-panel,
.planning-panel,
.weather-panel,
.map-shell,
.comment-shell {
  border: 1px solid #dce5e1;
  border-radius: 10px;
  background:
    linear-gradient(180deg, rgba(255, 252, 246, 0.97), rgba(250, 252, 250, 0.98));
  box-shadow: 0 18px 42px rgba(23, 33, 29, 0.08);
}

.profile-panel,
.planning-panel {
  padding: 48px 70px 56px;
}

.profile-panel h2,
.planning-panel h2,
.section-heading h2 {
  margin: 8px 0 0;
  color: #17211d;
  font-size: 24px;
  font-weight: 800;
  letter-spacing: 0;
}

.description-prose {
  max-width: 900px;
  margin-right: auto;
  margin-left: auto;
  margin-top: 22px;
  color: #33423d;
  font-size: 16px;
  line-height: 2;
  background: transparent;
  --w-e-textarea-bg-color: transparent;

  .w-e-scroll {
    height: auto;
    overflow: visible;
    background: transparent;
  }

  .scenic-editor-view {
    min-height: auto;
    padding: 0;
    background: transparent;
    white-space: normal;
  }

  :deep(.w-e-scroll),
  :deep([data-slate-editor]),
  :deep(p),
  :deep(div),
  :deep(section),
  :deep(article) {
    background: transparent !important;
  }

  :deep(img) {
    box-shadow: 0 16px 34px rgba(23, 33, 29, 0.12);
  }
}

.empty-description {
  color: #66736e;
}

.planning-panel {
  position: sticky;
  top: 0;
}

.info-list {
  display: grid;
  gap: 0;
  margin-top: 22px;
}

.info-row {
  display: grid;
  grid-template-columns: 110px minmax(0, 1fr);
  gap: 16px;
  align-items: center;
  padding: 16px 0;
  border-top: 1px solid #e6eeea;

  span {
    display: inline-flex;
    align-items: center;
    gap: 7px;
    color: #66736e;
    font-size: 14px;
  }

  strong {
    color: #17211d;
    font-size: 14px;
    font-weight: 700;
    text-align: right;
  }
}

.coordinate-link {
  display: inline-flex;
  justify-content: flex-end;
  align-items: center;
  gap: 6px;
  padding: 0;
  border: 0;
  background: transparent;
  color: #0f766e;
  font: inherit;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
}

.section-heading {
  display: flex;
  justify-content: space-between;
  align-items: end;
  gap: 20px;
  margin-bottom: 20px;

  h2 {
    display: flex;
    align-items: center;
    gap: 9px;
  }

  > span {
    color: #66736e;
    font-size: 14px;
  }
}

.weather-panel {
  display: grid;
  grid-template-columns: 260px minmax(0, 1fr);
  overflow: hidden;
  background: #fff;
}

.weather-current {
  padding: 28px;
  background: #173b34;
  color: #fff;

  span,
  em {
    display: block;
    color: rgba(255, 255, 255, 0.72);
    font-style: normal;
  }

  strong {
    display: block;
    margin: 10px 0 6px;
    font-size: 44px;
    font-weight: 800;
  }
}

.weather-metrics {
  display: grid;
  grid-template-columns: repeat(4, 1fr);

  div {
    padding: 28px 22px;
    border-left: 1px solid #e6eeea;
  }

  span {
    display: block;
    color: #66736e;
    font-size: 13px;
  }

  strong {
    display: block;
    margin-top: 8px;
    color: #17211d;
    font-size: 17px;
  }
}

.loading-line {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: #66736e;
}

.loading-icon {
  animation: spin 1s linear infinite;
}

.forecast-list {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 14px;
  margin-top: 16px;
}

.forecast-item {
  padding: 18px;
  border: 1px solid #dce5e1;
  border-radius: 8px;
  background: #fff;

  span,
  em {
    display: block;
    color: #66736e;
    font-size: 13px;
    font-style: normal;
  }

  strong {
    display: block;
    margin: 9px 0;
    color: #17211d;
    font-size: 17px;
  }
}

.map-shell {
  overflow: hidden;
}

.map-container {
  width: 100%;
  height: 430px;
}

.tour-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 18px;
}

.tour-card {
  overflow: hidden;
  border: 1px solid #dce5e1;
  border-radius: 10px;
  background:
    linear-gradient(180deg, #ffffff, #fbfaf6);
  box-shadow: 0 14px 30px rgba(23, 33, 29, 0.06);
  cursor: pointer;
  transition: transform 0.18s ease, box-shadow 0.18s ease, border-color 0.18s ease;

  &:hover {
    border-color: #9ccfc6;
    box-shadow: 0 20px 42px rgba(23, 33, 29, 0.1);
    transform: translateY(-3px);

    img {
      transform: scale(1.04);
    }
  }
}

.tour-image {
  position: relative;
  height: 188px;
  overflow: hidden;

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
    transition: transform 0.3s ease;
  }

  span {
    position: absolute;
    left: 14px;
    top: 14px;
    padding: 5px 10px;
    border-radius: 6px;
    background: rgba(23, 59, 52, 0.88);
    color: #fff;
    font-size: 12px;
    font-weight: 700;
  }
}

.tour-body {
  padding: 18px;

  h3 {
    display: -webkit-box;
    min-height: 44px;
    margin: 0;
    overflow: hidden;
    color: #17211d;
    font-size: 17px;
    font-weight: 800;
    line-height: 1.45;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
  }
}

.tour-meta,
.tour-footer {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
  margin-top: 14px;
}

.tour-meta span {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  color: #66736e;
  font-size: 13px;
}

.tour-footer {
  justify-content: space-between;
  padding-top: 14px;
  border-top: 1px solid #e6eeea;

  strong {
    color: #b45309;
    font-size: 18px;
  }

  em {
    color: #66736e;
    font-size: 13px;
    font-style: normal;
  }
}

.comment-shell {
  padding: 24px;
}

:deep(.amap-info-window) {
  .info-window {
    padding: 10px;

    h4 {
      margin: 0 0 8px;
      color: #17211d;
      font-size: 16px;
    }

    p {
      margin: 4px 0;
      color: #66736e;
      font-size: 13px;
    }
  }
}

@keyframes spin {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

@media (max-width: 1180px) {
  .section-container {
    width: min(calc(100% - 48px), 900px);
  }

  .detail-layout {
    grid-template-columns: 1fr;
  }

  .planning-panel {
    position: static;
  }

  .profile-panel,
  .planning-panel {
    padding: 34px;
  }

  .weather-panel {
    grid-template-columns: 1fr;
  }

  .weather-metrics {
    grid-template-columns: repeat(2, 1fr);
  }

  .tour-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .scenic-hero,
  .hero-inner {
    min-height: 320px;
  }

  .hero-inner {
    padding: 24px 0 36px;
    justify-content: center;
  }

  .hero-copy h1 {
    font-size: 34px;
  }

  .hero-copy p,
  .description-prose {
    font-size: 15px;
  }

  .content-band,
  .weather-section,
  .map-section,
  .recommended-section,
  .comment-section {
    padding: 28px 0;
  }

  .profile-panel,
  .planning-panel,
  .comment-shell {
    padding: 20px;
  }

  .section-heading {
    align-items: flex-start;
    flex-direction: column;
  }

  .info-row {
    grid-template-columns: 1fr;
    gap: 8px;

    strong,
    .coordinate-link {
      justify-content: flex-start;
      text-align: left;
    }
  }

  .weather-metrics,
  .forecast-list,
  .tour-grid {
    grid-template-columns: 1fr;
  }

  .map-container {
    height: 340px;
  }
}
</style>
