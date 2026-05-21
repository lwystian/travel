<template>
  <main class="guide-page">
    <section class="page-shell">
      <header class="page-hero">
        <div>
          <span class="eyebrow">Guide Studio</span>
          <h1>我的攻略</h1>
          <p>管理已发布的旅行攻略，跟踪审核状态、浏览数据和内容维护动作。</p>
        </div>
        <el-button type="primary" class="publish-btn" @click="goEdit()">
          <el-icon><Edit /></el-icon>
          发布新攻略
        </el-button>
      </header>

      <section class="stats-row">
        <div class="stat-card dark">
          <span>攻略总数</span>
          <strong>{{ total }}</strong>
        </div>
        <div class="stat-card">
          <span>本页浏览量</span>
          <strong>{{ totalViews }}</strong>
        </div>
        <div class="stat-card">
          <span>本月新增</span>
          <strong>{{ newGuideCount }}</strong>
        </div>
        <div class="stat-card">
          <span>待审核</span>
          <strong>{{ pendingCount }}</strong>
        </div>
      </section>

      <section class="guide-panel">
        <div class="panel-head">
          <div>
            <span>内容管理</span>
            <strong>攻略列表</strong>
          </div>
          <el-button class="ghost-btn" @click="fetchGuides">
            <el-icon><Refresh /></el-icon>
            刷新
          </el-button>
        </div>

        <div v-if="loading" class="state-box">
          <el-skeleton :rows="8" animated />
        </div>

        <div v-else-if="tableData.length === 0" class="empty-state">
          <div class="empty-icon">
            <el-icon><Document /></el-icon>
          </div>
          <h2>还没有发布攻略</h2>
          <p>分享你的旅行经验，让更多用户发现值得去的地方。</p>
          <el-button type="primary" @click="goEdit()">
            <el-icon><Edit /></el-icon>
            立即发布
          </el-button>
        </div>

        <div v-else class="guide-list">
          <article
            v-for="guide in tableData"
            :key="guide.id"
            class="guide-card"
            @click="viewGuide(guide)"
          >
            <div class="cover-wrap">
              <img :src="getImageUrl(guide.coverImage)" :alt="guide.title" />
              <el-tag :type="getReviewStatusType(guide.reviewStatus)" size="small" class="status-tag">
                {{ getReviewStatusText(guide.reviewStatus) }}
              </el-tag>
            </div>

            <div class="guide-main">
              <div class="guide-title-row">
                <div>
                  <span class="guide-id">攻略 ID {{ guide.id }}</span>
                  <h2>{{ guide.title || '未命名攻略' }}</h2>
                </div>
                <span class="fresh-chip" v-if="isNew(guide.createTime)">新发布</span>
              </div>

              <div class="meta-grid">
                <div>
                  <span>浏览量</span>
                  <strong>{{ guide.views || 0 }}</strong>
                </div>
                <div>
                  <span>发布时间</span>
                  <strong>{{ formatDate(guide.createTime) || '-' }}</strong>
                </div>
                <div>
                  <span>审核状态</span>
                  <strong>{{ getReviewStatusText(guide.reviewStatus) }}</strong>
                </div>
              </div>
            </div>

            <div class="guide-actions">
              <el-button type="primary" @click.stop="viewGuide(guide)">
                <el-icon><View /></el-icon>
                查看
              </el-button>
              <el-button
                v-if="guide.reviewStatus !== 1"
                plain
                @click.stop="goEdit(guide)"
              >
                <el-icon><Edit /></el-icon>
                编辑
              </el-button>
              <el-button plain type="danger" @click.stop="deleteGuide(guide)">
                <el-icon><Delete /></el-icon>
                删除
              </el-button>
            </div>
          </article>
        </div>

        <div class="pagination-wrap" v-if="total > 0">
          <el-pagination
            background
            layout="total, prev, pager, next"
            :total="total"
            :page-size="pageSize"
            :current-page="currentPage"
            @current-change="handleCurrentChange"
          />
        </div>
      </section>
    </section>
  </main>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import request from '@/utils/request'
import { formatDate } from '@/utils/dateUtils'
import {
  Delete,
  Document,
  Edit,
  Refresh,
  View
} from '@element-plus/icons-vue'

const baseAPI = process.env.VUE_APP_BASE_API || '/api'
const router = useRouter()
const tableData = ref([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const totalViews = computed(() => {
  return tableData.value.reduce((sum, guide) => sum + (guide.views || 0), 0)
})

const newGuideCount = computed(() => {
  const now = new Date()
  const currentMonth = now.getMonth()
  const currentYear = now.getFullYear()
  return tableData.value.filter(guide => {
    if (!guide.createTime) return false
    const createDate = new Date(guide.createTime)
    return createDate.getMonth() === currentMonth && createDate.getFullYear() === currentYear
  }).length
})

const pendingCount = computed(() => {
  return tableData.value.filter(guide => guide.reviewStatus === 0).length
})

const getImageUrl = (url) => {
  if (!url) return '/default-guide-cover.jpg'
  return url.startsWith('http') ? url : baseAPI + url
}

const isNew = (dateString) => {
  if (!dateString) return false
  const publishDate = new Date(dateString)
  const now = new Date()
  const diffTime = now - publishDate
  const diffDays = diffTime / (1000 * 60 * 60 * 24)
  return diffDays < 7
}

const getReviewStatusText = (status) => {
  switch (status) {
    case 0: return '待审核'
    case 1: return '已通过'
    case 2: return '已拒绝'
    default: return '未知'
  }
}

const getReviewStatusType = (status) => {
  switch (status) {
    case 0: return 'warning'
    case 1: return 'success'
    case 2: return 'danger'
    default: return 'info'
  }
}

const fetchGuides = async () => {
  loading.value = true
  try {
    await request.get('/guide/my', {
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
    console.error('获取我的攻略列表失败', error)
  } finally {
    loading.value = false
  }
}

const handleCurrentChange = (page) => {
  currentPage.value = page
  fetchGuides()
}

const viewGuide = (row) => {
  router.push(`/guide/detail/${row.id}`)
}

const goEdit = (row) => {
  if (row) {
    router.push({ name: 'GuideEdit', query: { id: row.id } })
  } else {
    router.push({ name: 'GuideEdit' })
  }
}

const deleteGuide = (row) => {
  ElMessageBox.confirm('确定要删除该攻略吗？删除后无法恢复。', '删除确认', {
    confirmButtonText: '确认删除',
    cancelButtonText: '取消',
    type: 'warning',
    closeOnClickModal: false
  }).then(async () => {
    try {
      await request.delete(`/guide/delete/${row.id}`, {
        successMsg: '删除成功',
        onSuccess: () => fetchGuides()
      })
    } catch (error) {
      console.error('删除攻略失败', error)
    }
  }).catch(() => {})
}

onMounted(fetchGuides)
</script>

<style lang="scss" scoped>
.guide-page {
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
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 24px;
  padding: 26px 28px;
  border: 1px solid #dde5f0;
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 18px 44px rgba(16, 24, 40, 0.06);
  text-align: left;
}

.page-hero > div:first-child {
  min-width: 0;
  text-align: left;
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

.publish-btn,
.ghost-btn {
  height: 42px;
  border-radius: 8px;
  font-weight: 800;
}

.publish-btn {
  background: #155eef;
  border-color: #155eef;
}

.stats-row {
  display: grid;
  grid-template-columns: 1.25fr repeat(3, 1fr);
  gap: 12px;
  margin: 16px 0;
}

.stat-card {
  min-height: 102px;
  padding: 18px;
  border: 1px solid #dde5f0;
  border-radius: 8px;
  background: #fff;
}

.stat-card span {
  display: block;
  color: #667085;
  font-size: 13px;
  font-weight: 800;
}

.stat-card strong {
  display: block;
  margin-top: 10px;
  font-size: 32px;
  line-height: 1;
  font-weight: 900;
}

.stat-card.dark {
  background: #101828;
  border-color: #101828;
  color: #fff;
}

.stat-card.dark span {
  color: #fff;
}

.guide-panel {
  border: 1px solid #dde5f0;
  border-radius: 8px;
  background: #fff;
  overflow: hidden;
}

.panel-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding: 18px 20px;
  border-bottom: 1px solid #e4eaf3;
  background: #fbfcfe;
  text-align: left;
}

.panel-head > div {
  text-align: left;
}

.panel-head span {
  display: block;
  color: #98a2b3;
  font-size: 12px;
  font-weight: 800;
}

.panel-head strong {
  display: block;
  margin-top: 4px;
  font-size: 18px;
  font-weight: 900;
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

.guide-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 14px;
}

.guide-card {
  display: grid;
  grid-template-columns: 220px minmax(0, 1fr) 180px;
  gap: 18px;
  padding: 14px;
  border: 1px solid #e4eaf3;
  border-radius: 8px;
  background: #fff;
  cursor: pointer;
  transition: border-color 0.18s ease, box-shadow 0.18s ease, transform 0.18s ease;
}

.guide-card:hover {
  border-color: #b9d6ff;
  box-shadow: 0 16px 34px rgba(16, 24, 40, 0.08);
  transform: translateY(-2px);
}

.cover-wrap {
  position: relative;
  height: 142px;
  border-radius: 8px;
  background: #eef2f7;
  overflow: hidden;
}

.cover-wrap img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.status-tag {
  position: absolute;
  top: 10px;
  left: 10px;
}

.guide-main {
  min-width: 0;
}

.guide-title-row {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.guide-id {
  display: block;
  margin-bottom: 8px;
  color: #667085;
  font-size: 12px;
  font-weight: 800;
}

.guide-title-row h2 {
  display: -webkit-box;
  margin: 0;
  overflow: hidden;
  font-size: 19px;
  font-weight: 900;
  line-height: 1.38;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.fresh-chip {
  flex-shrink: 0;
  height: 26px;
  padding: 0 9px;
  border-radius: 999px;
  background: #ecfdf3;
  color: #027a48;
  font-size: 12px;
  font-weight: 900;
  line-height: 26px;
}

.meta-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
  margin-top: 18px;
}

.meta-grid div {
  min-width: 0;
  padding: 11px 12px;
  border: 1px solid #eef2f7;
  border-radius: 8px;
  background: #fbfcfe;
}

.meta-grid span {
  display: block;
  margin-bottom: 6px;
  color: #98a2b3;
  font-size: 12px;
  font-weight: 800;
}

.meta-grid strong {
  display: block;
  overflow: hidden;
  color: #344054;
  font-size: 13px;
  font-weight: 900;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.guide-actions {
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 8px;
}

.guide-actions :deep(.el-button) {
  width: 100%;
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
    align-items: flex-start;
    flex-direction: column;
  }

  .stats-row {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .guide-card {
    grid-template-columns: 180px minmax(0, 1fr);
  }

  .guide-actions {
    grid-column: span 2;
    flex-direction: row;
    justify-content: flex-start;
  }

  .guide-actions :deep(.el-button) {
    width: auto;
  }
}

@media (max-width: 640px) {
  .page-hero h1 {
    font-size: 28px;
  }

  .stats-row,
  .guide-card,
  .meta-grid {
    grid-template-columns: 1fr;
  }

  .guide-actions {
    grid-column: auto;
    flex-direction: column;
  }

  .guide-actions :deep(.el-button) {
    width: 100%;
  }
}
</style>
