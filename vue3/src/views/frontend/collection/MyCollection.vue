<template>
  <main class="collection-page">
    <section class="page-shell">
      <header class="page-hero">
        <div>
          <span class="eyebrow">Collection Center</span>
          <h1>我的收藏</h1>
          <p>集中管理已收藏的景点、攻略和行程，快速回到感兴趣的内容。</p>
        </div>
        <div class="hero-stats">
          <div>
            <span>全部收藏</span>
            <strong>{{ totalCollections }}</strong>
          </div>
          <div>
            <span>景点</span>
            <strong>{{ scenicTotal }}</strong>
          </div>
          <div>
            <span>攻略</span>
            <strong>{{ guideTotal }}</strong>
          </div>
          <div>
            <span>行程</span>
            <strong>{{ tourTotal }}</strong>
          </div>
        </div>
      </header>

      <section class="content-panel">
        <div class="segment-bar">
          <button
            v-for="tab in tabs"
            :key="tab.name"
            type="button"
            class="segment-item"
            :class="{ active: activeTab === tab.name }"
            @click="handleTabChange(tab.name)"
          >
            <el-icon><component :is="tab.icon" /></el-icon>
            <span>{{ tab.label }}</span>
            <strong>{{ tab.count }}</strong>
          </button>
        </div>

        <div class="panel-toolbar">
          <div>
            <span>当前分类</span>
            <strong>{{ activeTabInfo.label }}</strong>
          </div>
          <el-button class="ghost-btn" @click="activeTabInfo.refresh">
            <el-icon><Refresh /></el-icon>
            刷新
          </el-button>
        </div>

        <div v-if="activeTabInfo.loading" class="state-box">
          <el-skeleton :rows="8" animated />
        </div>

        <div v-else-if="activeTabInfo.items.length === 0" class="empty-state">
          <div class="empty-icon">
            <el-icon><Star /></el-icon>
          </div>
          <h2>{{ activeTabInfo.emptyTitle }}</h2>
          <p>{{ activeTabInfo.emptyDesc }}</p>
          <el-button type="primary" @click="activeTabInfo.browse">
            <el-icon><Search /></el-icon>
            {{ activeTabInfo.browseText }}
          </el-button>
        </div>

        <div v-else class="collection-grid">
          <article
            v-for="item in activeTabInfo.items"
            :key="item.id"
            class="collection-card"
            @click="activeTabInfo.open(item)"
          >
            <div class="cover-wrap">
              <img :src="activeTabInfo.image(item)" :alt="activeTabInfo.title(item)" />
              <span class="type-chip">{{ activeTabInfo.cardType }}</span>
              <span v-if="activeTabInfo.price(item)" class="price-chip">{{ activeTabInfo.price(item) }}</span>
            </div>

            <div class="card-body">
              <h2>{{ activeTabInfo.title(item) }}</h2>
              <p>{{ activeTabInfo.subtitle(item) }}</p>

              <div class="meta-row">
                <span>
                  <el-icon><Clock /></el-icon>
                  收藏于 {{ formatDate(item.createTime) || '-' }}
                </span>
                <span v-if="activeTabInfo.metric(item)">
                  <el-icon><View /></el-icon>
                  {{ activeTabInfo.metric(item) }}
                </span>
              </div>

              <div class="card-actions">
                <el-button type="primary" @click.stop="activeTabInfo.open(item)">
                  {{ activeTabInfo.primaryText }}
                </el-button>
                <el-button plain type="danger" @click.stop="activeTabInfo.remove(item)">
                  <el-icon><Delete /></el-icon>
                  取消收藏
                </el-button>
              </div>
            </div>
          </article>
        </div>

        <div class="pagination-wrap" v-if="activeTabInfo.total > 0">
          <el-pagination
            background
            layout="total, prev, pager, next"
            :total="activeTabInfo.total"
            :page-size="activeTabInfo.pageSize"
            :current-page="activeTabInfo.currentPage"
            @current-change="activeTabInfo.pageChange"
          />
        </div>
      </section>
    </section>
  </main>
</template>

<script setup>
import { computed, ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'
import {
  Clock,
  Delete,
  Document,
  Location,
  MapLocation,
  Refresh,
  Search,
  Star,
  Tickets,
  View
} from '@element-plus/icons-vue'

const router = useRouter()
const baseAPI = process.env.VUE_APP_BASE_API || '/api'

const activeTab = ref('scenic')

const scenicLoading = ref(false)
const scenicCollections = ref([])
const scenicCurrentPage = ref(1)
const scenicPageSize = ref(9)
const scenicTotal = ref(0)

const guideLoading = ref(false)
const guideCollections = ref([])
const guideCurrentPage = ref(1)
const guidePageSize = ref(9)
const guideTotal = ref(0)

const tourLoading = ref(false)
const tourCollections = ref([])
const tourCurrentPage = ref(1)
const tourPageSize = ref(9)
const tourTotal = ref(0)

const totalCollections = computed(() => scenicTotal.value + guideTotal.value + tourTotal.value)

const tabs = computed(() => [
  { name: 'scenic', label: '景点收藏', count: scenicTotal.value, icon: MapLocation },
  { name: 'guide', label: '攻略收藏', count: guideTotal.value, icon: Document },
  { name: 'tour', label: '行程收藏', count: tourTotal.value, icon: Tickets }
])

const activeTabInfo = computed(() => {
  const configs = {
    scenic: {
      label: '景点收藏',
      loading: scenicLoading.value,
      items: scenicCollections.value,
      total: scenicTotal.value,
      pageSize: scenicPageSize.value,
      currentPage: scenicCurrentPage.value,
      cardType: '景点',
      primaryText: '查看景点',
      emptyTitle: '暂无收藏景点',
      emptyDesc: '发现喜欢的目的地后，可以收藏到这里集中管理。',
      browseText: '浏览景点',
      refresh: fetchScenicCollections,
      browse: goToScenicList,
      pageChange: handleScenicPageChange,
      open: (item) => goToScenicDetail(item.scenicInfo?.id),
      remove: (item) => handleCancelScenicCollection(item.scenicInfo?.id),
      image: (item) => getImageUrl(item.scenicInfo?.imageUrl),
      title: (item) => item.scenicInfo?.name || '未命名景点',
      subtitle: (item) => item.scenicInfo?.location || '暂无位置信息',
      metric: (item) => item.scenicInfo?.categoryInfo?.name || '',
      price: (item) => item.scenicInfo?.price === 0 ? '免费' : (item.scenicInfo?.price > 0 ? `¥${item.scenicInfo.price}` : '')
    },
    guide: {
      label: '攻略收藏',
      loading: guideLoading.value,
      items: guideCollections.value,
      total: guideTotal.value,
      pageSize: guidePageSize.value,
      currentPage: guideCurrentPage.value,
      cardType: '攻略',
      primaryText: '查看攻略',
      emptyTitle: '暂无收藏攻略',
      emptyDesc: '收藏优质攻略后，可以从这里快速回看。',
      browseText: '浏览攻略',
      refresh: fetchGuideCollections,
      browse: goToGuideList,
      pageChange: handleGuidePageChange,
      open: (item) => goToGuideDetail(item.guideId),
      remove: (item) => handleCancelGuideCollection(item.guideId),
      image: (item) => getImageUrl(item.guideCoverImage),
      title: (item) => item.guideTitle || '未命名攻略',
      subtitle: (item) => item.username || item.userNickname || '作者未展示',
      metric: (item) => `${item.guideViews || 0} 浏览`,
      price: () => ''
    },
    tour: {
      label: '行程收藏',
      loading: tourLoading.value,
      items: tourCollections.value,
      total: tourTotal.value,
      pageSize: tourPageSize.value,
      currentPage: tourCurrentPage.value,
      cardType: '行程',
      primaryText: '去预订',
      emptyTitle: '暂无收藏行程',
      emptyDesc: '把感兴趣的行程收藏起来，之后可以快速预订。',
      browseText: '浏览行程',
      refresh: fetchTourCollections,
      browse: goToTourList,
      pageChange: handleTourPageChange,
      open: (item) => goToTourBooking(item.tourId),
      remove: (item) => handleCancelTourCollection(item.tourId),
      image: (item) => getImageUrl(getTourImage(item.tourInfo)),
      title: (item) => item.tourInfo?.title || '行程预订',
      subtitle: (item) => item.tourInfo?.destination || item.tourInfo?.city || '目的地待确认',
      metric: (item) => `${item.tourInfo?.days || 1} 天`,
      price: (item) => item.tourInfo?.minPrice ? `¥${item.tourInfo.minPrice}` : ''
    }
  }
  return configs[activeTab.value]
})

const fetchScenicCollections = async () => {
  scenicLoading.value = true
  try {
    const params = new URLSearchParams()
    params.append('currentPage', scenicCurrentPage.value)
    params.append('size', scenicPageSize.value)
    const res = await request.get('/scenic-collection/user?' + params.toString(), null, { showDefaultMsg: false })
    scenicCollections.value = res?.records || []
    scenicTotal.value = res?.total || 0
  } catch (error) {
    console.error('获取收藏景点失败:', error)
  } finally {
    scenicLoading.value = false
  }
}

const fetchGuideCollections = async () => {
  guideLoading.value = true
  try {
    const params = new URLSearchParams()
    params.append('currentPage', guideCurrentPage.value)
    params.append('size', guidePageSize.value)
    const res = await request.get('/collection/page?' + params.toString(), null, { showDefaultMsg: false })
    guideCollections.value = res?.records || []
    guideTotal.value = res?.total || 0
  } catch (error) {
    console.error('获取收藏攻略失败:', error)
  } finally {
    guideLoading.value = false
  }
}

const fetchTourCollections = async () => {
  tourLoading.value = true
  try {
    const params = new URLSearchParams()
    params.append('currentPage', tourCurrentPage.value)
    params.append('size', tourPageSize.value)
    const res = await request.get('/tour-collection/user?' + params.toString(), null, { showDefaultMsg: false })
    tourCollections.value = res?.records || []
    tourTotal.value = res?.total || 0
  } catch (error) {
    console.error('获取收藏行程失败:', error)
  } finally {
    tourLoading.value = false
  }
}

const fetchAllCollectionCounts = async () => {
  await Promise.all([fetchScenicCollections(), fetchGuideCollections(), fetchTourCollections()])
}

const handleTabChange = (tabName) => {
  activeTab.value = tabName
}

const handleScenicPageChange = (page) => {
  scenicCurrentPage.value = page
  fetchScenicCollections()
}

const handleGuidePageChange = (page) => {
  guideCurrentPage.value = page
  fetchGuideCollections()
}

const handleTourPageChange = (page) => {
  tourCurrentPage.value = page
  fetchTourCollections()
}

const goToScenicDetail = (scenicId) => {
  if (scenicId) router.push(`/scenic/${scenicId}`)
}

const goToGuideDetail = (guideId) => {
  if (guideId) router.push(`/guide/detail/${guideId}`)
}

const goToTourBooking = (tourId) => {
  if (tourId) router.push(`/ticket/booking/${tourId}`)
}

const goToScenicList = () => router.push('/scenic')
const goToGuideList = () => router.push('/guide')
const goToTourList = () => router.push('/tickets')

const handleCancelScenicCollection = (scenicId) => {
  if (!scenicId) return
  ElMessageBox.confirm('确认取消收藏该景点吗？', '取消收藏', {
    confirmButtonText: '确认取消',
    cancelButtonText: '再想想',
    type: 'warning'
  }).then(async () => {
    try {
      await request.delete(`/scenic-collection/${scenicId}`)
      ElMessage.success('已取消收藏')
      scenicCollections.value = scenicCollections.value.filter(item => item.scenicInfo?.id !== scenicId)
      scenicTotal.value = Math.max(0, scenicTotal.value - 1)
      if (scenicCollections.value.length === 0 && scenicCurrentPage.value > 1) {
        scenicCurrentPage.value--
        fetchScenicCollections()
      }
    } catch (error) {
      console.error('取消景点收藏失败:', error)
    }
  }).catch(() => {})
}

const handleCancelGuideCollection = (guideId) => {
  if (!guideId) return
  ElMessageBox.confirm('确认取消收藏该攻略吗？', '取消收藏', {
    confirmButtonText: '确认取消',
    cancelButtonText: '再想想',
    type: 'warning'
  }).then(async () => {
    try {
      const params = new URLSearchParams()
      params.append('guideId', guideId)
      await request.delete(`/collection/cancel?${params.toString()}`)
      ElMessage.success('已取消收藏')
      guideCollections.value = guideCollections.value.filter(item => item.guideId !== guideId)
      guideTotal.value = Math.max(0, guideTotal.value - 1)
      if (guideCollections.value.length === 0 && guideCurrentPage.value > 1) {
        guideCurrentPage.value--
        fetchGuideCollections()
      }
    } catch (error) {
      console.error('取消攻略收藏失败:', error)
    }
  }).catch(() => {})
}

const handleCancelTourCollection = (tourId) => {
  if (!tourId) return
  ElMessageBox.confirm('确认取消收藏该行程吗？', '取消收藏', {
    confirmButtonText: '确认取消',
    cancelButtonText: '再想想',
    type: 'warning'
  }).then(async () => {
    try {
      await request.delete(`/tour-collection/${tourId}`)
      ElMessage.success('已取消收藏')
      tourCollections.value = tourCollections.value.filter(item => item.tourId !== tourId)
      tourTotal.value = Math.max(0, tourTotal.value - 1)
      if (tourCollections.value.length === 0 && tourCurrentPage.value > 1) {
        tourCurrentPage.value--
        fetchTourCollections()
      }
    } catch (error) {
      console.error('取消行程收藏失败:', error)
    }
  }).catch(() => {})
}

const getTourImage = (tour) => {
  if (!tour?.mainImage) return ''
  try {
    const images = JSON.parse(tour.mainImage)
    if (Array.isArray(images) && images.length > 0) return images[0]
  } catch (error) {
    // mainImage may already be a plain URL.
  }
  return tour.mainImage
}

const getImageUrl = (url) => {
  if (!url) return '/default-image.jpg'
  return url.startsWith('http') ? url : baseAPI + url
}

const formatDate = (dateStr) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
}

onMounted(fetchAllCollectionCounts)
</script>

<style lang="scss" scoped>
.collection-page {
  min-height: 100vh;
  background: #f3f6fb;
  color: #101828;
  font-family: "Source Han Sans", "Noto Sans CJK SC", "Microsoft YaHei", sans-serif;
}

.page-shell {
  width: min(1240px, calc(100% - 40px));
  margin: 0 auto;
  padding: 36px 0 64px;
}

.page-hero {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(360px, 520px);
  gap: 28px;
  align-items: end;
  padding: 26px 28px;
  border: 1px solid #dde5f0;
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 18px 44px rgba(16, 24, 40, 0.06);
}

.eyebrow {
  display: inline-flex;
  height: 24px;
  align-items: center;
  padding: 0 10px;
  border-radius: 999px;
  background: #e8f2ff;
  color: #155eef;
  font-size: 12px;
  font-weight: 800;
}

.page-hero h1 {
  margin: 12px 0 8px;
  font-size: 34px;
  font-weight: 900;
  letter-spacing: 0;
}

.page-hero p {
  margin: 0;
  color: #667085;
  font-size: 15px;
}

.hero-stats {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
}

.hero-stats div {
  min-height: 82px;
  padding: 14px;
  border: 1px solid #e4eaf3;
  border-radius: 8px;
  background: #fbfcfe;
}

.hero-stats span {
  display: block;
  color: #667085;
  font-size: 12px;
  font-weight: 800;
}

.hero-stats strong {
  display: block;
  margin-top: 10px;
  font-size: 26px;
  line-height: 1;
  font-weight: 900;
}

.content-panel {
  margin-top: 16px;
  border: 1px solid #dde5f0;
  border-radius: 8px;
  background: #fff;
  overflow: hidden;
}

.segment-bar {
  display: flex;
  gap: 8px;
  padding: 14px;
  border-bottom: 1px solid #e4eaf3;
  background: #fbfcfe;
  overflow-x: auto;
}

.segment-item {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  min-width: max-content;
  height: 40px;
  padding: 0 12px;
  border: 1px solid #d6deeb;
  border-radius: 8px;
  background: #fff;
  color: #344054;
  font-weight: 900;
  cursor: pointer;
}

.segment-item strong {
  min-width: 24px;
  height: 22px;
  padding: 0 7px;
  border-radius: 999px;
  background: #eef2f7;
  font-size: 12px;
  line-height: 22px;
  text-align: center;
}

.segment-item.active {
  border-color: #155eef;
  background: #155eef;
  color: #fff;
}

.segment-item.active strong {
  background: rgba(255, 255, 255, 0.2);
}

.panel-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 18px 20px;
}

.panel-toolbar span {
  display: block;
  color: #98a2b3;
  font-size: 12px;
  font-weight: 800;
}

.panel-toolbar strong {
  display: block;
  margin-top: 4px;
  font-size: 18px;
  font-weight: 900;
}

.ghost-btn {
  border-radius: 8px;
  font-weight: 800;
}

.state-box {
  padding: 24px;
}

.empty-state {
  display: flex;
  min-height: 420px;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 48px 24px;
  text-align: center;
}

.empty-icon {
  display: grid;
  width: 76px;
  height: 76px;
  place-items: center;
  margin-bottom: 18px;
  border: 1px solid #dbeafe;
  border-radius: 8px;
  background: #eff6ff;
  color: #155eef;
  font-size: 34px;
}

.empty-state h2 {
  margin: 0 0 8px;
  font-size: 22px;
  font-weight: 900;
}

.empty-state p {
  max-width: 460px;
  margin: 0 0 22px;
  color: #667085;
  line-height: 1.7;
}

.collection-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
  padding: 0 20px 20px;
}

.collection-card {
  min-width: 0;
  border: 1px solid #e4eaf3;
  border-radius: 8px;
  background: #fff;
  overflow: hidden;
  cursor: pointer;
  transition: border-color 0.18s ease, box-shadow 0.18s ease, transform 0.18s ease;
}

.collection-card:hover {
  border-color: #b9d6ff;
  box-shadow: 0 16px 34px rgba(16, 24, 40, 0.08);
  transform: translateY(-2px);
}

.cover-wrap {
  position: relative;
  height: 178px;
  overflow: hidden;
  background: #eef2f7;
}

.cover-wrap img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.type-chip,
.price-chip {
  position: absolute;
  top: 12px;
  height: 26px;
  padding: 0 9px;
  border-radius: 999px;
  background: rgba(16, 24, 40, 0.76);
  color: #fff;
  font-size: 12px;
  font-weight: 900;
  line-height: 26px;
}

.type-chip {
  left: 12px;
}

.price-chip {
  right: 12px;
  background: rgba(255, 255, 255, 0.92);
  color: #101828;
}

.card-body {
  padding: 16px;
}

.card-body h2 {
  display: -webkit-box;
  min-height: 48px;
  margin: 0 0 10px;
  overflow: hidden;
  font-size: 17px;
  font-weight: 900;
  line-height: 1.42;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.card-body p {
  overflow: hidden;
  margin: 0 0 14px;
  color: #667085;
  font-size: 13px;
  font-weight: 700;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.meta-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 12px;
  min-height: 24px;
  color: #667085;
  font-size: 12px;
  font-weight: 800;
}

.meta-row span {
  display: inline-flex;
  align-items: center;
  gap: 5px;
}

.card-actions {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 8px;
  margin-top: 16px;
}

.card-actions :deep(.el-button) {
  margin-left: 0;
  border-radius: 8px;
  font-weight: 800;
}

.pagination-wrap {
  display: flex;
  justify-content: center;
  padding: 10px 20px 24px;
}

.pagination-wrap :deep(.el-pagination.is-background .el-pager li),
.pagination-wrap :deep(.el-pagination.is-background .btn-prev),
.pagination-wrap :deep(.el-pagination.is-background .btn-next) {
  border-radius: 8px;
}

@media (max-width: 980px) {
  .page-shell {
    width: min(100% - 24px, 1240px);
    padding-top: 24px;
  }

  .page-hero {
    grid-template-columns: 1fr;
  }

  .collection-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .page-hero h1 {
    font-size: 28px;
  }

  .hero-stats,
  .collection-grid {
    grid-template-columns: 1fr;
  }

  .panel-toolbar {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
