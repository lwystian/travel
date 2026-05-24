<template>
  <div ref="carouselRoot" class="home-carousel" :style="{ '--home-carousel-height': `${carouselHeight}px` }">
    <el-carousel :interval="4000" :height="`${carouselHeight}px`" indicator-position="outside" arrow="never" v-loading="loading">
      <el-carousel-item v-for="item in carouselList" :key="item.id">
        <div class="carousel-content">
          <img :src="getImageUrl(item.imageUrl)" :alt="item.title" class="carousel-image" />
          <div class="carousel-overlay">
            <div class="carousel-title" v-if="item.title">{{ item.title }}</div>
          </div>
        </div>
      </el-carousel-item>
    </el-carousel>
  </div>
</template>

<script setup>
import { nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import request from '@/utils/request'
import noImage from '@/assets/images/no-image.png'

const carouselList = ref([])
const loading = ref(false)
const carouselRoot = ref(null)
const carouselHeight = ref(482)
const baseAPI = process.env.VUE_APP_BASE_API || '/api'

const updateCarouselHeight = () => {
  if (!carouselRoot.value) return
  const rect = carouselRoot.value.getBoundingClientRect()
  const pageTop = rect.top + window.scrollY
  const availableHeight = window.innerHeight - pageTop
  carouselHeight.value = Math.max(260, Math.round(availableHeight))
}

const getImageUrl = (url) => {
  if (!url) return noImage
  return url.startsWith('http') ? url : baseAPI + url
}

const fetchCarousels = async () => {
  loading.value = true
  try {
    await request.get('/carousel/active', null, {
      showDefaultMsg: false,
      onSuccess: (data) => {
        carouselList.value = data || []
        nextTick(updateCarouselHeight)
      }
    })
  } catch (error) {
    console.error('获取轮播图失败:', error)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchCarousels()
  nextTick(updateCarouselHeight)
  setTimeout(updateCarouselHeight, 100)
  setTimeout(updateCarouselHeight, 400)
  window.addEventListener('resize', updateCarouselHeight)
  window.addEventListener('orientationchange', updateCarouselHeight)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', updateCarouselHeight)
  window.removeEventListener('orientationchange', updateCarouselHeight)
})
</script>

<style scoped>
.home-carousel {
  width: 100%;
  height: var(--home-carousel-height, 482px);
  position: relative;
}

.carousel-content {
  position: relative;
  width: 100%;
  height: 100%;
}

.carousel-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.carousel-overlay {
  position: absolute;
  bottom: 0;
  left: 0;
  width: 100%;
  background: linear-gradient(to top, rgba(0, 0, 0, 0.7), transparent);
  padding: 60px 20px 20px;
}

.carousel-title {
  color: #fff;
  font-size: 28px;
  font-weight: 600;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.5);
  max-width: 800px;
  margin: 0 auto;
  text-align: center;
}
</style>

<style>
.home-carousel .el-carousel {
  width: 100%;
  height: var(--home-carousel-height, 482px);
  position: relative;
}

.home-carousel .el-carousel__container {
  height: var(--home-carousel-height, 482px) !important;
}

.home-carousel .el-carousel__indicators--outside {
  position: absolute !important;
  bottom: 24px !important;
  left: 50% !important;
  transform: translateX(-50%) !important;
  z-index: 100 !important;
  display: flex !important;
  justify-content: center !important;
  background: transparent !important;
}

.home-carousel .el-carousel__indicators--outside .el-carousel__indicator {
  padding: 0 6px !important;
  margin: 0 4px !important;
}

.home-carousel .el-carousel__indicators--outside .el-carousel__button {
  width: 16px !important;
  height: 16px !important;
  border-radius: 50% !important;
  background: transparent !important;
  border: 2px solid rgba(255, 255, 255, 0.7) !important;
  opacity: 1 !important;
  display: block !important;
}

.home-carousel .el-carousel__indicators--outside li.is-active .el-carousel__button {
  background: rgba(255, 255, 255, 0.9) !important;
  border-color: #fff !important;
  transform: scale(1.4) !important;
}

.home-carousel .el-carousel__indicators--outside li:hover .el-carousel__button {
  background: rgba(255, 255, 255, 1) !important;
}
</style>
