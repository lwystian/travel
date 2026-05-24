<template>
  <div class="comment-management">
    <section class="hero-panel">
      <div>
        <p class="eyebrow">Comment Console</p>
        <h1>评论管理</h1>
        <span>统一管理景点评论与住宿评价，只有审核通过的内容才会在前台展示。</span>
      </div>
      <div class="hero-stats">
        <div>
          <strong>{{ total }}</strong>
          <span>当前结果</span>
        </div>
        <div>
          <strong>{{ statusSummary }}</strong>
          <span>审核状态</span>
        </div>
      </div>
    </section>

    <section class="filter-panel">
      <div class="filter-row">
        <el-radio-group v-model="searchForm.type" class="type-tabs" @change="handleSearch">
          <el-radio-button label="all">全部</el-radio-button>
          <el-radio-button label="scenic">景点评论</el-radio-button>
          <el-radio-button label="accommodation">住宿评价</el-radio-button>
        </el-radio-group>
        <el-select v-model="searchForm.reviewStatus" placeholder="审核状态" clearable class="status-select" @change="handleSearch">
          <el-option label="待审核" :value="0" />
          <el-option label="已通过" :value="1" />
          <el-option label="已拒绝" :value="2" />
        </el-select>
      </div>

      <el-form :model="searchForm" class="search-form">
        <el-form-item label="对象">
          <el-input v-model="searchForm.targetName" placeholder="景点 / 住宿名称" clearable />
        </el-form-item>
        <el-form-item label="用户">
          <el-input v-model="searchForm.userName" placeholder="用户名 / 昵称" clearable />
        </el-form-item>
        <el-form-item label="内容">
          <el-input v-model="searchForm.content" placeholder="评论关键词" clearable />
        </el-form-item>
        <el-form-item class="action-item">
          <el-button type="primary" :icon="Search" @click="handleSearch">搜索</el-button>
          <el-button :icon="Refresh" @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>
    </section>

    <section class="table-panel">
      <el-table :data="tableData" v-loading="loading" class="comment-table">
        <el-table-column label="评论内容" min-width="340">
          <template #default="{ row }">
            <div class="content-cell">
              <div class="content-head">
                <el-tag size="small" :type="row.type === 'scenic' ? 'success' : 'warning'" effect="light">
                  {{ row.typeLabel }}
                </el-tag>
                <span>{{ row.targetName || '内容已删除' }}</span>
              </div>
              <p>{{ row.content || '-' }}</p>
              <div class="content-meta">
                <el-rate :model-value="Number(row.rating || 0)" disabled :max="5" size="small" />
                <span v-if="row.likes !== null && row.likes !== undefined">{{ row.likes }} 点赞</span>
              </div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="用户" min-width="200">
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

        <el-table-column label="状态" width="140" align="center">
          <template #default="{ row }">
            <el-tag :type="statusMeta(row.reviewStatus).type" effect="light" round>
              {{ statusMeta(row.reviewStatus).label }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="审核信息" min-width="190">
          <template #default="{ row }">
            <div class="review-cell">
              <span>{{ row.reviewerName || '尚未审核' }}</span>
              <small v-if="row.reviewTime">{{ formatDate(row.reviewTime) }}</small>
              <small v-if="row.reviewComment">意见：{{ row.reviewComment }}</small>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="发布时间" width="170">
          <template #default="{ row }">
            <span class="time-text">{{ formatDate(row.createTime) }}</span>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="220" align="right" fixed="right">
          <template #default="{ row }">
            <div class="action-buttons">
              <el-button v-if="row.reviewStatus === 0" type="success" plain :icon="Check" @click="reviewItem(row, 1)">通过</el-button>
              <el-button v-if="row.reviewStatus === 0" type="warning" plain :icon="Close" @click="openReject(row)">拒绝</el-button>
              <el-button type="danger" plain :icon="Delete" @click="deleteItem(row)">删除</el-button>
            </div>
          </template>
        </el-table-column>

        <template #empty>
          <el-empty description="暂无匹配的评论或评价" />
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

    <el-dialog v-model="rejectDialogVisible" title="拒绝原因" width="520px" align-center>
      <el-input
        v-model="rejectComment"
        type="textarea"
        :rows="4"
        maxlength="200"
        show-word-limit
        placeholder="请填写明确、可追溯的审核意见"
      />
      <template #footer>
        <el-button @click="rejectDialogVisible = false">取消</el-button>
        <el-button type="warning" @click="confirmReject">确认拒绝</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { Check, Close, Delete, Refresh, Search } from '@element-plus/icons-vue'
import { ElMessageBox } from 'element-plus'
import request from '@/utils/request'
import { formatDate } from '@/utils/dateUtils'
import noImage from '@/assets/images/no-image.png'

const baseAPI = process.env.VUE_APP_BASE_API || '/api'
const tableData = ref([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const rejectDialogVisible = ref(false)
const rejectComment = ref('')
const rejectTarget = ref(null)

const searchForm = reactive({
  type: 'all',
  targetName: '',
  userName: '',
  content: '',
  reviewStatus: null
})

const statusSummary = computed(() => statusMeta(searchForm.reviewStatus).label)

const fetchComments = async () => {
  loading.value = true
  try {
    await request.get('/review/comment/unified', {
      type: searchForm.type,
      targetName: searchForm.targetName,
      userName: searchForm.userName,
      content: searchForm.content,
      reviewStatus: searchForm.reviewStatus,
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
  fetchComments()
}

const resetSearch = () => {
  searchForm.type = 'all'
  searchForm.targetName = ''
  searchForm.userName = ''
  searchForm.content = ''
  searchForm.reviewStatus = null
  currentPage.value = 1
  fetchComments()
}

const handleCurrentChange = (page) => {
  currentPage.value = page
  fetchComments()
}

const reviewItem = async (row, status, reviewComment = '') => {
  const endpointType = row.type === 'accommodation' ? 'accommodation' : 'comment'
  await request.post(`/review/${endpointType}/${row.id}`, {
    status,
    reviewComment
  }, {
    successMsg: status === 1 ? '审核通过' : '已拒绝',
    onSuccess: fetchComments
  })
}

const openReject = (row) => {
  rejectTarget.value = row
  rejectComment.value = ''
  rejectDialogVisible.value = true
}

const confirmReject = async () => {
  if (!rejectTarget.value) return
  await reviewItem(rejectTarget.value, 2, rejectComment.value || '内容不符合平台展示规范')
  rejectDialogVisible.value = false
}

const deleteItem = (row) => {
  ElMessageBox.confirm(`确定删除这条${row.typeLabel || '评论'}吗？删除后不可恢复。`, '删除确认', {
    confirmButtonText: '删除',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    request.delete(`/review/comment/unified/${row.type}/${row.id}`, {
      successMsg: '删除成功',
      onSuccess: fetchComments
    })
  }).catch(() => {})
}

const statusMeta = (status) => {
  if (status === 0) return { label: '待审核', type: 'warning' }
  if (status === 1) return { label: '已通过', type: 'success' }
  if (status === 2) return { label: '已拒绝', type: 'danger' }
  return { label: '全部状态', type: 'info' }
}

const resolveAsset = (url) => {
  if (!url) return noImage
  if (/^(https?:)?\/\//.test(url) || url.startsWith('data:')) return url
  return `${baseAPI}${url.startsWith('/') ? url : `/${url}`}`
}

const avatarText = (row) => (row.userNickname || row.username || '用').slice(0, 1)

onMounted(fetchComments)
</script>

<style lang="scss" scoped>
.comment-management {
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
    color: #7c3aed;
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

.filter-row {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
}

.status-select {
  width: 160px;
}

.search-form {
  display: grid;
  grid-template-columns: minmax(180px, 240px) minmax(180px, 240px) minmax(220px, 1fr) auto;
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

.comment-table {
  :deep(.el-table__header th) {
    background: #f8fafc;
    color: #475569;
    font-weight: 700;
  }

  :deep(.el-table__row) {
    min-height: 92px;
  }
}

.content-cell p {
  max-width: 560px;
  margin: 8px 0;
  color: #334155;
  line-height: 1.6;
}

.content-head,
.content-meta,
.user-cell {
  display: flex;
  align-items: center;
}

.content-head {
  gap: 10px;
  font-weight: 700;
  color: #111827;
}

.content-meta {
  gap: 12px;
  color: #64748b;
  font-size: 13px;
}

.user-cell {
  gap: 10px;
}

.user-name {
  max-width: 140px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-weight: 700;
}

.user-sub,
.time-text,
.review-cell small {
  color: #64748b;
  font-size: 13px;
}

.review-cell {
  display: grid;
  gap: 4px;
}

.review-cell span {
  font-weight: 600;
}

.action-buttons {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  flex-wrap: wrap;
}

.pagination-container {
  display: flex;
  justify-content: flex-end;
  padding: 18px 16px 0;
}

@media (max-width: 1100px) {
  .hero-panel,
  .filter-row {
    flex-direction: column;
  }

  .search-form {
    grid-template-columns: 1fr;
  }
}
</style>
