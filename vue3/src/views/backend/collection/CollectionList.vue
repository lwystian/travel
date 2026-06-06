<template>
  <div class="collection-management">
    <section class="hero-panel">
      <div>
        <p class="eyebrow">Collection Console</p>
        <h1>收藏管理</h1>
        <span>统一管理景点、攻略、行程收藏，兼容当前前台收藏业务。</span>
      </div>
      <div class="hero-stats">
        <div>
          <strong>{{ total }}</strong>
          <span>当前结果</span>
        </div>
        <div>
          <strong>{{ activeTypeLabel }}</strong>
          <span>收藏类型</span>
        </div>
      </div>
    </section>

    <section class="filter-panel">
      <el-radio-group v-model="searchForm.type" class="type-tabs" @change="handleSearch">
        <el-radio-button label="all">全部收藏</el-radio-button>
        <el-radio-button label="scenic">景点</el-radio-button>
        <el-radio-button label="guide">攻略</el-radio-button>
        <el-radio-button label="tour">行程</el-radio-button>
      </el-radio-group>

      <el-form :model="searchForm" class="search-form">
        <el-form-item label="用户">
          <el-input v-model="searchForm.username" placeholder="用户名 / 昵称" clearable />
        </el-form-item>
        <el-form-item label="收藏内容">
          <el-input v-model="searchForm.keyword" placeholder="景点、攻略或行程名称" clearable />
        </el-form-item>
        <el-form-item class="action-item">
          <el-button type="primary" :icon="Search" @click="handleSearch">搜索</el-button>
          <el-button :icon="Refresh" @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>
    </section>

    <section class="table-panel">
      <el-table :data="tableData" v-loading="loading" class="collection-table">
        <el-table-column label="收藏内容" min-width="280">
          <template #default="{ row }">
            <div class="target-cell">
              <div class="target-cover">
                <el-image v-if="row.coverImage" :src="resolveAsset(row.coverImage)" fit="cover" />
                <component v-else :is="typeIcon(row.type)" />
              </div>
              <div class="target-main">
                <div class="target-title">{{ row.targetTitle || '内容已删除' }}</div>
                <div class="target-meta">
                  <el-tag size="small" :type="typeTag(row.type)" effect="light">{{ row.typeLabel }}</el-tag>
                  <span>ID {{ row.targetId || '-' }}</span>
                  <span v-if="row.views !== null && row.views !== undefined">{{ row.views }} 浏览</span>
                </div>
              </div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="用户" min-width="210">
          <template #default="{ row }">
            <div class="user-cell">
              <el-avatar :src="resolveAsset(row.userAvatar)" :size="36">{{ avatarText(row) }}</el-avatar>
              <div>
                <div class="user-name">{{ row.userNickname || row.username || '未知用户' }}</div>
                <div class="user-sub">@{{ row.username || row.userId || '-' }}</div>
              </div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="收藏时间" width="190">
          <template #default="{ row }">
            <span class="time-text">{{ formatDate(row.createTime) }}</span>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="130" align="right">
          <template #default="{ row }">
            <el-button type="danger" plain :icon="Delete" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>

        <template #empty>
          <el-empty description="暂无匹配的收藏记录" />
        </template>
      </el-table>

      <div class="pagination-container">
        <el-pagination
          background
          layout="total, prev, pager, next"
          :current-page="currentPage"
          :page-size="pageSize"
          :total="total"
          @current-change="handleCurrentChange"
        />
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { Delete, Location, Notebook, Picture, Refresh, Search, Suitcase } from '@element-plus/icons-vue'
import { ElMessageBox } from 'element-plus'
import request from '@/utils/request'
import { formatDate } from '@/utils/dateUtils'
import noImage from '@/assets/images/no-image.png'
import { resolveImageUrl } from '@/utils/imageUrl'

const tableData = ref([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const searchForm = reactive({
  type: 'all',
  username: '',
  keyword: ''
})

const typeLabels = {
  all: '全部',
  scenic: '景点',
  guide: '攻略',
  tour: '行程'
}

const activeTypeLabel = computed(() => typeLabels[searchForm.type] || '全部')

const fetchCollections = async () => {
  loading.value = true
  try {
    await request.get('/collection/admin/unified/page', {
      type: searchForm.type,
      username: searchForm.username,
      keyword: searchForm.keyword,
      currentPage: currentPage.value,
      size: pageSize.value
    }, {
      showDefaultMsg: false,
      onSuccess: (res) => {
        tableData.value = res.records || []
        total.value = res.total || 0
      }
    })
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  currentPage.value = 1
  fetchCollections()
}

const resetSearch = () => {
  searchForm.type = 'all'
  searchForm.username = ''
  searchForm.keyword = ''
  currentPage.value = 1
  fetchCollections()
}

const handleCurrentChange = (val) => {
  currentPage.value = val
  fetchCollections()
}

const handleDelete = (row) => {
  ElMessageBox.confirm(`确定删除这条${row.typeLabel || '收藏'}记录吗？`, '删除确认', {
    confirmButtonText: '删除',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    request.delete(`/collection/admin/unified/${row.type}/${row.id}`, {
      successMsg: '删除成功',
      onSuccess: fetchCollections
    })
  }).catch(() => {})
}

const resolveAsset = (url) => {
  return resolveImageUrl(url, noImage)
}

const avatarText = (row) => (row.userNickname || row.username || '用').slice(0, 1)
const typeTag = (type) => ({ scenic: 'success', guide: 'warning', tour: 'primary' }[type] || 'info')
const typeIcon = (type) => ({ scenic: Location, guide: Notebook, tour: Suitcase }[type] || Picture)

onMounted(fetchCollections)
</script>

<style lang="scss" scoped>
.collection-management {
  min-height: calc(100vh - 120px);
  padding: 24px;
  background: #f5f7fb;
  color: #1f2937;
}

.hero-panel,
.filter-panel,
.table-panel {
  border: 1px solid #e5e7eb;
  border-radius: 18px;
  background: #fff;
  box-shadow: 0 18px 45px rgba(15, 23, 42, 0.06);
}

.hero-panel {
  display: flex;
  justify-content: space-between;
  gap: 24px;
  padding: 28px 30px;
  margin-bottom: 18px;

  .eyebrow {
    margin: 0 0 8px;
    color: #2563eb;
    font-size: 13px;
    font-weight: 700;
  }

  h1 {
    margin: 0;
    font-size: 28px;
    font-weight: 800;
  }

  span {
    display: inline-block;
    margin-top: 10px;
    color: #64748b;
  }
}

.hero-stats {
  display: grid;
  grid-template-columns: repeat(2, minmax(118px, 1fr));
  gap: 12px;

  div {
    padding: 16px;
    border-radius: 14px;
    background: #f8fafc;
  }

  strong {
    display: block;
    font-size: 22px;
    color: #0f172a;
  }

  span {
    margin-top: 4px;
    color: #64748b;
    font-size: 13px;
  }
}

.filter-panel {
  padding: 18px 20px 6px;
  margin-bottom: 18px;
}

.type-tabs {
  margin-bottom: 16px;
}

.search-form {
  display: grid;
  grid-template-columns: minmax(180px, 260px) minmax(220px, 360px) auto;
  gap: 14px;
  align-items: flex-start;

  :deep(.el-form-item__label) {
    font-weight: 600;
    color: #475569;
  }

  .action-item {
    align-self: end;
  }
}

.table-panel {
  padding: 8px 8px 18px;
}

.collection-table {
  :deep(.el-table__header th) {
    background: #f8fafc;
    color: #475569;
    font-weight: 700;
  }

  :deep(.el-table__row) {
    height: 76px;
  }
}

.target-cell,
.user-cell,
.target-meta {
  display: flex;
  align-items: center;
}

.target-cell {
  gap: 14px;
}

.target-cover {
  width: 54px;
  height: 54px;
  display: grid;
  place-items: center;
  flex: 0 0 54px;
  border-radius: 14px;
  overflow: hidden;
  background: #eef2ff;
  color: #4f46e5;

  .el-image,
  :deep(img) {
    width: 100%;
    height: 100%;
  }

  :deep(svg) {
    width: 24px;
    height: 24px;
  }
}

.target-title,
.user-name {
  max-width: 420px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-weight: 700;
  color: #111827;
}

.target-meta {
  gap: 8px;
  margin-top: 8px;
  color: #64748b;
  font-size: 12px;
}

.user-cell {
  gap: 10px;
}

.user-sub,
.time-text {
  color: #64748b;
  font-size: 13px;
}

.pagination-container {
  display: flex;
  justify-content: flex-end;
  padding: 18px 16px 0;
}

@media (max-width: 980px) {
  .hero-panel {
    flex-direction: column;
  }

  .search-form {
    grid-template-columns: 1fr;
  }
}
</style>
