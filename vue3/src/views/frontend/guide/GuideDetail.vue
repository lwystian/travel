<template>
  <div class="guide-detail-page" v-loading="loading">
    <article class="guide-detail" v-if="guide">
      <section class="guide-hero">
        <img v-if="guide.coverImage" :src="getImageUrl(guide.coverImage)" :alt="guide.title" class="hero-media" />
        <div v-else class="default-hero"></div>
        <div class="hero-overlay"></div>

        <div class="hero-inner">
          <div class="breadcrumb">
            <el-breadcrumb separator="/">
              <el-breadcrumb-item @click="$router.push('/')">首页</el-breadcrumb-item>
              <el-breadcrumb-item @click="$router.push('/guide')">旅游攻略</el-breadcrumb-item>
              <el-breadcrumb-item>攻略详情</el-breadcrumb-item>
            </el-breadcrumb>
          </div>

          <div class="hero-content">
            <h1>{{ guide.title }}</h1>
            <p>{{ articleSummary }}</p>

            <div class="article-meta">
              <span class="author-meta">
                <el-avatar class="author-avatar" :src="getAvatarUrl(guide.userAvatar)" :size="36" fit="cover">
                  <el-icon><User /></el-icon>
                </el-avatar>
                {{ guide.userNickname || `旅行者${guide.userId || ''}` }}
                <span v-if="officialBadge(guide)" class="identity-badge" :class="officialBadge(guide).className">
                  {{ officialBadge(guide).label }}
                </span>
              </span>
              <span>
                <el-icon><Calendar /></el-icon>
                {{ formatDate(guide.createTime) }}
              </span>
              <span>
                <el-icon><View /></el-icon>
                {{ formatNumber(guide.views) }} 阅读
              </span>
              <span v-if="guide.destination">
                <el-icon><Location /></el-icon>
                {{ guide.destination }}
              </span>
            </div>

            <div class="hero-actions">
              <el-button
                :type="isCollected ? 'danger' : 'primary'"
                size="large"
                @click="handleCollectionToggle"
              >
                <el-icon>
                  <StarFilled v-if="isCollected" />
                  <Star v-else />
                </el-icon>
                {{ isCollected ? '已收藏' : '收藏攻略' }}
              </el-button>
              <el-button size="large" class="ghost-button" @click="handleShare">
                <el-icon><Share /></el-icon>
                分享
              </el-button>
            </div>
          </div>
        </div>
      </section>

      <section class="reading-section">
        <div class="reading-container">
          <main class="article-shell">
            <div class="article-toolbar">
              <span>攻略正文</span>
              <span>{{ readingMeta }}</span>
            </div>
            <div class="article-content content-display w-e-text-container">
              <div class="w-e-scroll">
                <div v-html="safeGuideContent" class="article-editor-view" data-slate-editor="true"></div>
              </div>
            </div>
          </main>

          <aside class="article-sidebar">
            <div class="sticky-sidebar-stack">
              <div class="sidebar-panel">
                <div class="sidebar-title">
                  <strong>攻略信息</strong>
                </div>
                <div class="sidebar-list">
                  <div>
                    <span>目的地</span>
                    <strong>{{ guide.destination || '未设置' }}</strong>
                  </div>
                  <div>
                    <span>发布时间</span>
                    <strong>{{ formatDate(guide.createTime) }}</strong>
                  </div>
                  <div>
                    <span>阅读量</span>
                    <strong>{{ formatNumber(guide.views) }}</strong>
                  </div>
                </div>
              </div>

              <div class="sidebar-panel toc-panel">
                <div class="sidebar-title">
                  <strong>文章目录</strong>
                </div>
                <div v-if="tableOfContents.length > 0" class="toc-list">
                  <button
                    v-for="item in tableOfContents"
                    :key="item.id"
                    type="button"
                    class="toc-item"
                    :class="`level-${item.level}`"
                    @click="scrollToHeading(item.id)"
                  >
                    <span>{{ item.order }}</span>
                    <strong>{{ item.text }}</strong>
                  </button>
                </div>
                <div v-else class="toc-empty">正文暂无目录标题</div>
              </div>
            </div>

            <div class="sidebar-panel" v-if="relatedGuides.length > 0">
              <div class="sidebar-title">
                <strong>相关攻略</strong>
              </div>
              <div class="related-list">
                <button
                  v-for="item in relatedGuides"
                  :key="item.id"
                  type="button"
                  class="related-item"
                  @click="goToGuide(item.id)"
                >
                  <strong>{{ item.title }}</strong>
                  <span>
                    <el-icon><View /></el-icon>
                    {{ formatNumber(item.views) }} 阅读
                  </span>
                </button>
              </div>
            </div>
          </aside>
        </div>
      </section>
    </article>

    <div class="guide-not-found" v-else-if="!loading">
      <el-empty description="未找到该攻略" />
      <el-button type="primary" @click="goBack">返回</el-button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import request from '@/utils/request'
import { formatDate } from '@/utils/dateUtils'
import { useUserStore } from '@/store/user'
import { ElMessage } from 'element-plus'
import { View, Calendar, Star, StarFilled, Share, User, Location } from '@element-plus/icons-vue'
import { shareCurrentPage } from '@/utils/share'
import { updateSeo, seoDescription } from '@/utils/seo'
import { renderContent } from '@/utils/contentRenderer'
import { resolveImageUrl, resolveAbsoluteImageUrl } from '@/utils/imageUrl'
import noImage from '@/assets/images/no-image.png'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const guide = ref(null)
const isCollected = ref(false)
const loading = ref(true)
const relatedGuides = ref([])
const renderedGuideContent = computed(() => renderContent(guide.value?.content))
const articleRenderResult = computed(() => buildArticleRender(renderedGuideContent.value))
const safeGuideContent = computed(() => articleRenderResult.value.html)
const tableOfContents = computed(() => articleRenderResult.value.toc)

const articleSummary = computed(() => {
  const raw = guide.value?.summary || guide.value?.destination || stripHtml(guide.value?.content || '')
  return raw ? raw.slice(0, 110) : '精选目的地玩法、路线建议和出行经验，为企业客户与高品质旅行用户提供清晰可靠的参考。'
})

const readingMeta = computed(() => {
  const text = stripHtml(guide.value?.content || '')
  const minutes = Math.max(1, Math.ceil(text.length / 500))
  return `约 ${minutes} 分钟阅读`
})

const getImageUrl = (url) => resolveImageUrl(url, noImage)
const getAvatarUrl = (url) => resolveImageUrl(url, '')
const getAbsoluteImageUrl = (url) => resolveAbsoluteImageUrl(url, noImage)

const stripHtml = (value = '') => String(value).replace(/<[^>]+>/g, '').replace(/\s+/g, ' ').trim()

const buildArticleRender = (html = '') => {
  if (!html) return { html: '', toc: [] }
  if (typeof window === 'undefined' || !window.DOMParser) {
    return { html, toc: [] }
  }

  const parser = new window.DOMParser()
  const doc = parser.parseFromString(`<div>${html}</div>`, 'text/html')
  const wrapper = doc.body.firstElementChild
  if (!wrapper) return { html, toc: [] }

  const headings = Array.from(wrapper.querySelectorAll('h1, h2, h3, h4'))
    .filter(heading => heading.textContent?.trim())

  const toc = headings.map((heading, index) => {
    const id = `guide-section-${index + 1}`
    const tagLevel = Number(heading.tagName.replace('H', '')) || 2
    const level = tagLevel <= 2 ? 2 : 3
    heading.setAttribute('id', id)
    heading.classList.add('article-anchor-heading')

    return {
      id,
      level,
      order: String(index + 1).padStart(2, '0'),
      text: heading.textContent.trim()
    }
  })

  return { html: wrapper.innerHTML, toc }
}

const formatNumber = (num) => {
  if (!num) return '0'
  if (num >= 10000) return `${(num / 10000).toFixed(1)}w`
  if (num >= 1000) return `${(num / 1000).toFixed(1)}k`
  return num.toString()
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

const fetchGuide = async () => {
  loading.value = true
  guide.value = null
  relatedGuides.value = []
  isCollected.value = false
  const id = route.params.id

  try {
    await request.get(`/guide/${id}`, {}, {
      showDefaultMsg: false,
      onSuccess: (res) => {
        guide.value = res
        updateSeo({
          title: `${res.title}旅游攻略`,
          description: res.content || res.destination || '侠客行国旅旅游攻略，分享目的地玩法、路线和出行建议。',
          path: `/guide/detail/${id}`,
          image: res.coverImage,
          type: 'article',
          schema: {
            '@context': 'https://schema.org',
            '@type': 'Article',
            headline: res.title,
            description: seoDescription(res.content || res.destination || '侠客行国旅旅游攻略'),
            url: `${window.location.origin}/guide/detail/${id}`,
            image: res.coverImage ? getAbsoluteImageUrl(res.coverImage) : undefined,
            datePublished: res.createTime || undefined,
            dateModified: res.updateTime || res.createTime || undefined,
            author: {
              '@type': 'Person',
              name: res.userNickname || `旅行者${res.userId || ''}`
            }
          }
        })

        if (userStore.isLoggedIn) {
          checkIsCollected(id)
        }
        fetchRelatedGuides()
      }
    })
  } catch (error) {
    console.error('获取攻略详情失败:', error)
  } finally {
    loading.value = false
  }
}

const fetchRelatedGuides = async () => {
  if (!guide.value) return

  try {
    await request.get('/guide/related', {
      guideId: guide.value.id,
      limit: 4
    }, {
      showDefaultMsg: false,
      onSuccess: (data) => {
        relatedGuides.value = data || []
      }
    })
  } catch (error) {
    console.error('获取相关攻略失败:', error)
  }
}

const checkIsCollected = async (guideId) => {
  if (!userStore.isLoggedIn) return

  try {
    await request.get('/collection/isCollected', { guideId }, {
      showDefaultMsg: false,
      onSuccess: (data) => {
        isCollected.value = data
      }
    })
  } catch (error) {
    console.error('查询收藏状态出错:', error)
  }
}

const handleCollectionToggle = async () => {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    router.push('/login?redirect=' + route.fullPath)
    return
  }

  try {
    if (isCollected.value) {
      await request.delete(`/collection/cancel?guideId=${guide.value.id}`, {
        successMsg: '取消收藏成功',
        onSuccess: () => {
          isCollected.value = false
        }
      })
    } else {
      await request.post('/collection/add', { guideId: guide.value.id }, {
        successMsg: '收藏成功',
        onSuccess: () => {
          isCollected.value = true
        }
      })
    }
  } catch (error) {
    console.error('收藏操作失败:', error)
  }
}

const handleShare = () => {
  shareCurrentPage({
    title: guide.value?.title || '旅游攻略',
    text: guide.value?.summary || guide.value?.destination || ''
  })
}

const scrollToHeading = (id) => {
  const target = document.getElementById(id)
  if (!target) return
  target.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

const goToGuide = (id) => {
  router.push(`/guide/detail/${id}`)
}

const goBack = () => {
  router.back()
}

watch(() => route.params.id, () => {
  fetchGuide()
})

onMounted(() => {
  fetchGuide()
})
</script>

<style lang="scss" scoped>
.guide-detail-page {
  min-height: 100vh;
  background:
    linear-gradient(180deg, #efe6d4 0%, #f5efe3 42%, #eef3ef 100%);
  color: #17211d;
}

.guide-hero {
  position: relative;
  height: 480px;
  min-height: 360px;
  overflow: hidden;
}

.hero-media,
.default-hero {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.default-hero {
  background:
    linear-gradient(135deg, rgba(23, 59, 52, 0.92), rgba(41, 87, 74, 0.86)),
    linear-gradient(90deg, rgba(182, 138, 67, 0.28), transparent);
}

.hero-overlay {
  position: absolute;
  inset: 0;
  background:
    linear-gradient(90deg, rgba(54, 40, 24, 0.5) 0%, rgba(54, 40, 24, 0.22) 48%, rgba(10, 20, 18, 0.42) 100%),
    linear-gradient(180deg, rgba(255, 215, 142, 0.08) 0%, rgba(8, 18, 17, 0.38) 100%);
}

.hero-inner,
.reading-container {
  width: min(calc(100% - 64px), 1320px);
  margin: 0 auto;
}

.reading-container {
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

.hero-content {
  max-width: 1120px;
  margin: 0 auto;
  text-align: center;
}

.hero-content h1 {
  margin: 0 auto 20px;
  color: #fff;
  font-size: clamp(32px, 4.4vw, 56px);
  font-weight: 800;
  line-height: 1.16;
  letter-spacing: 0;
  text-align: center;
  text-shadow: 0 5px 18px rgba(0, 0, 0, 0.36);
}

.hero-content p {
  max-width: 760px;
  margin: 0 auto;
  color: rgba(255, 255, 255, 0.92);
  font-size: 18px;
  font-weight: 600;
  line-height: 1.8;
  text-align: center;
  text-shadow: 0 3px 12px rgba(0, 0, 0, 0.32);
}

.article-meta {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 14px;
  margin-top: 28px;
}

.article-meta > span {
  display: inline-flex;
  min-height: 38px;
  align-items: center;
  gap: 8px;
  padding: 0 12px;
  border: 1px solid rgba(255, 255, 255, 0.22);
  border-radius: 10px;
  background: rgba(24, 24, 24, 0.24);
  color: #fff;
  backdrop-filter: blur(10px);
  font-weight: 700;
  box-shadow: 0 10px 28px rgba(0, 0, 0, 0.16);
}

.author-meta {
  padding-left: 4px !important;
}

.author-avatar {
  flex: 0 0 36px;
  overflow: hidden;
  background: rgba(255, 255, 255, 0.18);

  :deep(img) {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }
}

.identity-badge {
  display: inline-flex;
  align-items: center;
  height: 24px;
  padding: 0 10px;
  border: 1px solid transparent;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 850;

  &.super-admin {
    color: #5c3d04;
    border-color: rgba(255, 219, 112, 0.42);
    background: linear-gradient(135deg, #fff3c4 0%, #e9c65d 100%);
  }

  &.admin {
    color: #074f68;
    border-color: rgba(125, 211, 252, 0.42);
    background: linear-gradient(135deg, #e0f7ff 0%, #a7d8ea 100%);
  }
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

.reading-section {
  position: relative;
  padding: 46px 0 78px;
  background:
    linear-gradient(180deg, #efe6d4 0%, #f7f1e7 42%, #eef3ef 100%);
}

.reading-section::before {
  content: "";
  position: absolute;
  inset: 0;
  background:
    linear-gradient(90deg, rgba(22, 69, 58, 0.055) 1px, transparent 1px),
    linear-gradient(180deg, rgba(22, 69, 58, 0.045) 1px, transparent 1px);
  background-size: 48px 48px;
  pointer-events: none;
}

.reading-section > * {
  position: relative;
}

.reading-container {
  display: grid;
  grid-template-columns: minmax(0, 1040px) 400px;
  gap: 40px;
  align-items: stretch;
  justify-content: center;
}

.article-shell,
.sidebar-panel {
  border: 1px solid #dce5e1;
  border-radius: 10px;
  box-shadow: 0 18px 42px rgba(23, 33, 29, 0.08);
}

.article-shell {
  min-width: 0;
  padding: 48px 70px 64px;
  background:
    linear-gradient(180deg, rgba(255, 252, 246, 0.96), rgba(250, 252, 250, 0.98));
}

.sidebar-panel {
  background:
    linear-gradient(180deg, rgba(255, 252, 246, 0.86), rgba(250, 252, 250, 0.9));
}

.article-toolbar {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 30px;
  padding-bottom: 16px;
  border-bottom: 1px solid #e6eeea;
  color: #66736e;
  font-size: 13px;

  span:first-child {
    color: #8a5a16;
    font-weight: 800;
  }
}

.article-content {
  max-width: 900px;
  margin: 0 auto;
  color: #2d3d38;
  font-size: 16px;
  background: transparent;
  --w-e-textarea-bg-color: transparent;

  .w-e-scroll {
    height: auto;
    overflow: visible;
    background: transparent;
  }

  .article-editor-view {
    min-height: auto;
    padding: 0;
    background: transparent;
    white-space: normal;
  }

  :deep(.w-e-text-container),
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

  :deep(.article-anchor-heading) {
    scroll-margin-top: 96px;
  }
}

.article-sidebar {
  display: flex;
  flex-direction: column;
  gap: 18px;
  align-self: stretch;
  min-height: 100%;
  overflow: visible;
}

.sticky-sidebar-stack {
  display: flex;
  flex-direction: column;
  gap: 18px;
  align-self: start;
  position: sticky;
  top: 0;
  z-index: 5;
  max-height: none;
  height: max-content;
  overflow: visible;
}

.sidebar-panel {
  padding: 22px;
}

.sidebar-title {
  padding-bottom: 16px;
  border-bottom: 1px solid #e6eeea;

  strong {
    display: block;
    color: #17211d;
    font-size: 18px;
  }

  span {
    display: block;
    margin-top: 6px;
    color: #7a8782;
    font-size: 12px;
  }
}

.sidebar-list {
  display: grid;
  gap: 0;

  div {
    display: flex;
    justify-content: space-between;
    gap: 16px;
    padding: 16px 0;
    border-bottom: 1px solid #e6eeea;

    &:last-child {
      border-bottom: 0;
    }
  }

  span {
    color: #66736e;
    font-size: 13px;
  }

  strong {
    color: #17211d;
    font-size: 13px;
    text-align: right;
  }
}

.related-list {
  display: grid;
  gap: 12px;
  margin-top: 18px;
}

.toc-panel {
  overflow: visible;
  background:
    linear-gradient(180deg, rgba(255, 252, 246, 0.99), rgba(248, 252, 249, 0.99));
}

.toc-list {
  display: grid;
  gap: 8px;
  margin-top: 16px;
  max-height: min(420px, calc(100vh - 330px));
  overflow-y: auto;
  padding-right: 4px;
  overscroll-behavior: contain;
}

.toc-item {
  display: grid;
  grid-template-columns: 34px minmax(0, 1fr);
  gap: 10px;
  width: 100%;
  align-items: start;
  padding: 10px 10px;
  border: 1px solid transparent;
  border-radius: 8px;
  background: transparent;
  text-align: left;
  cursor: pointer;
  transition: background 0.18s ease, border-color 0.18s ease, transform 0.18s ease;

  &:hover {
    border-color: #d4e7df;
    background: #f6faf7;
    transform: translateX(2px);
  }

  span {
    display: grid;
    height: 26px;
    place-items: center;
    border-radius: 999px;
    background: #173b34;
    color: #fff;
    font-size: 11px;
    font-weight: 800;
  }

  strong {
    display: -webkit-box;
    overflow: hidden;
    color: #273832;
    font-size: 14px;
    font-weight: 800;
    line-height: 1.55;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
  }
}

.toc-item.level-3 {
  padding-left: 22px;

  span {
    background: #b68a43;
  }

  strong {
    color: #50615b;
    font-size: 13px;
  }
}

.toc-empty {
  margin-top: 16px;
  padding: 16px;
  border: 1px dashed #dce5e1;
  border-radius: 8px;
  background: #f8faf8;
  color: #7a8782;
  font-size: 13px;
  text-align: center;
}

.related-item {
  display: grid;
  gap: 8px;
  width: 100%;
  padding: 14px;
  border: 1px solid #dce5e1;
  border-radius: 8px;
  background: #f8faf8;
  text-align: left;
  cursor: pointer;
  transition: border-color 0.18s ease, background 0.18s ease, transform 0.18s ease;

  &:hover {
    border-color: #9ccfc6;
    background: #fff;
    transform: translateY(-2px);
  }

  strong {
    display: -webkit-box;
    overflow: hidden;
    color: #17211d;
    font-size: 14px;
    line-height: 1.5;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
  }

  span {
    display: inline-flex;
    align-items: center;
    gap: 5px;
    color: #66736e;
    font-size: 12px;
  }
}

.guide-not-found {
  width: min(var(--frontend-container-safe-width), 760px);
  margin: 48px auto;
  padding: 48px 24px;
  border: 1px solid #dce5e1;
  border-radius: 8px;
  background: #fff;
  text-align: center;

  .el-button {
    margin-top: 18px;
  }
}

@media (max-width: 1100px) {
  .reading-container {
    width: min(calc(100% - 48px), 900px);
    grid-template-columns: minmax(0, 1fr);
  }

  .article-sidebar {
    position: static;
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    max-height: none;
    overflow: visible;
  }

  .sticky-sidebar-stack {
    position: static;
    display: contents;
    max-height: none;
    overflow: visible;
  }
}

@media (max-width: 768px) {
  .guide-hero,
  .hero-inner {
    min-height: 320px;
  }

  .hero-inner {
    padding: 24px 0 36px;
    justify-content: center;
  }

  .hero-content h1 {
    font-size: 34px;
  }

  .hero-content p,
  .article-content {
    font-size: 15px;
  }

  .reading-section {
    padding: 30px 0 54px;
  }

  .article-shell {
    padding: 28px 20px 36px;
  }

  .article-toolbar {
    align-items: flex-start;
    flex-direction: column;
  }

  .article-sidebar {
    grid-template-columns: 1fr;
  }

  .sticky-sidebar-stack {
    display: contents;
  }
}
</style>
