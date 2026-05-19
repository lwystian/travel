<template>
  <div class="review-management">
    <!-- 页面标题 -->
    <div class="page-header">
      <h1 class="page-title">内容审核管理</h1>
      <p class="page-subtitle">Content Review Management - 公安备案合规审核</p>
    </div>

    <!-- 统计卡片 -->
    <el-row :gutter="20" class="stats-row">
      <el-col :span="6">
        <el-card class="stat-card pending-card" shadow="never">
          <div class="stat-content">
            <div class="stat-icon">
              <el-icon><Clock /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.pendingComment + stats.pendingGuide + stats.pendingAccommodation }}</div>
              <div class="stat-label">待审核</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card comment-card" shadow="never">
          <div class="stat-content">
            <div class="stat-icon">
              <el-icon><ChatDotRound /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.pendingComment }}</div>
              <div class="stat-label">待审评论</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card guide-card" shadow="never">
          <div class="stat-content">
            <div class="stat-icon">
              <el-icon><Document /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.pendingGuide }}</div>
              <div class="stat-label">待审攻略</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card total-card" shadow="never">
          <div class="stat-content">
            <div class="stat-icon">
              <el-icon><Finished /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.approvedComment + stats.approvedGuide + stats.approvedAccommodation }}</div>
              <div class="stat-label">已通过</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 标签页 -->
    <el-card class="main-card" shadow="never">
      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <!-- 评论审核 -->
        <el-tab-pane label="评论审核" name="comment">
          <div class="tab-header">
            <el-tag type="warning" size="large">待审核评论: {{ stats.pendingComment }}</el-tag>
            <div class="tab-actions">
              <el-button type="success" @click="batchApprove('comment')" :disabled="!multipleSelectionComment.length">
                <el-icon><Check /></el-icon> 批量通过
              </el-button>
              <el-button type="danger" @click="batchReject('comment')" :disabled="!multipleSelectionComment.length">
                <el-icon><Close /></el-icon> 批量拒绝
              </el-button>
            </div>
          </div>
          
          <el-table
            :data="commentTableData"
            v-loading="commentLoading"
            border
            stripe
            @selection-change="handleCommentSelectionChange"
            class="review-table"
          >
            <el-table-column type="selection" width="60" align="center" />
            <el-table-column prop="id" label="评论ID" width="100" />
            <el-table-column label="用户" width="140">
              <template #default="scope">
                <div class="user-cell">
                  <el-avatar :src="scope.row.userAvatar" :size="32">
                    {{ (scope.row.userNickname || '用户').charAt(0) }}
                  </el-avatar>
                  <span class="user-name">{{ scope.row.userNickname || '用户' }}</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="content" label="评论内容" show-overflow-tooltip />
            <el-table-column prop="rating" label="评分" width="140" align="center">
              <template #default="scope">
                <div class="rating-cell">
                  <el-rate v-model="scope.row.rating" disabled text-color="#ff9900" size="large" />
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="createTime" label="发布时间" width="160">
              <template #default="scope">
                {{ formatDate(scope.row.createTime) }}
              </template>
            </el-table-column>
            <el-table-column label="操作" width="180" fixed="right">
              <template #default="scope">
                <el-button type="success" size="small" @click="approveItem('comment', scope.row)">
                  <el-icon><Check /></el-icon> 通过
                </el-button>
                <el-button type="danger" size="small" @click="rejectItem('comment', scope.row)">
                  <el-icon><Close /></el-icon> 拒绝
                </el-button>
              </template>
            </el-table-column>
          </el-table>
          
          <div class="pagination-container">
            <el-pagination
              background
              layout="total, prev, pager, next"
              :current-page="commentPage"
              :page-size="commentPageSize"
              :total="commentTotal"
              @current-change="handleCommentPageChange"
            />
          </div>
        </el-tab-pane>

        <!-- 住宿评价审核 -->
        <el-tab-pane label="住宿评价审核" name="accommodation">
          <div class="tab-header">
            <el-tag type="warning" size="large">待审核住宿评价 {{ stats.pendingAccommodation }}</el-tag>
            <div class="tab-actions">
              <el-button type="success" @click="batchApprove('accommodation')" :disabled="!multipleSelectionAccommodation.length">
                <el-icon><Check /></el-icon> 批量通过
              </el-button>
              <el-button type="danger" @click="batchReject('accommodation')" :disabled="!multipleSelectionAccommodation.length">
                <el-icon><Close /></el-icon> 批量拒绝
              </el-button>
            </div>
          </div>

          <el-table
            :data="accommodationTableData"
            v-loading="accommodationLoading"
            border
            stripe
            @selection-change="handleAccommodationSelectionChange"
            class="review-table"
          >
            <el-table-column type="selection" width="60" align="center" />
            <el-table-column prop="id" label="评价ID" width="100" />
            <el-table-column label="用户" width="140">
              <template #default="scope">
                <div class="user-cell">
                  <el-avatar :src="scope.row.avatar" :size="32">
                    {{ (scope.row.nickname || '用户').charAt(0) }}
                  </el-avatar>
                  <span class="user-name">{{ scope.row.nickname || '用户' }}</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="content" label="评价内容" show-overflow-tooltip />
            <el-table-column prop="rating" label="评分" width="140" align="center">
              <template #default="scope">
                <el-rate v-model="scope.row.rating" disabled text-color="#ff9900" size="large" />
              </template>
            </el-table-column>
            <el-table-column prop="createTime" label="发布时间" width="160">
              <template #default="scope">{{ formatDate(scope.row.createTime) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="180" fixed="right">
              <template #default="scope">
                <el-button type="success" size="small" @click="approveItem('accommodation', scope.row)">
                  <el-icon><Check /></el-icon> 通过
                </el-button>
                <el-button type="danger" size="small" @click="rejectItem('accommodation', scope.row)">
                  <el-icon><Close /></el-icon> 拒绝
                </el-button>
              </template>
            </el-table-column>
          </el-table>

          <div class="pagination-container">
            <el-pagination
              background
              layout="total, prev, pager, next"
              :current-page="accommodationPage"
              :page-size="accommodationPageSize"
              :total="accommodationTotal"
              @current-change="handleAccommodationPageChange"
            />
          </div>
        </el-tab-pane>

        <!-- 攻略审核 -->
        <el-tab-pane label="攻略审核" name="guide">
          <div class="tab-header">
            <el-tag type="warning" size="large">待审核攻略: {{ stats.pendingGuide }}</el-tag>
            <div class="tab-actions">
              <el-button type="success" @click="batchApprove('guide')" :disabled="!multipleSelectionGuide.length">
                <el-icon><Check /></el-icon> 批量通过
              </el-button>
              <el-button type="danger" @click="batchReject('guide')" :disabled="!multipleSelectionGuide.length">
                <el-icon><Close /></el-icon> 批量拒绝
              </el-button>
            </div>
          </div>
          
          <el-table
            :data="guideTableData"
            v-loading="guideLoading"
            border
            stripe
            @selection-change="handleGuideSelectionChange"
            class="review-table"
          >
            <el-table-column type="selection" width="60" align="center" />
            <el-table-column prop="id" label="攻略ID" width="100" />
            <el-table-column label="作者" width="140">
              <template #default="scope">
                <div class="user-cell">
                  <el-avatar :src="scope.row.userAvatar" :size="32">
                    {{ (scope.row.userNickname || '用户').charAt(0) }}
                  </el-avatar>
                  <span class="user-name">{{ scope.row.userNickname || '用户' }}</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="title" label="标题" show-overflow-tooltip />
            <el-table-column prop="destination" label="目的地" width="120" />
            <el-table-column prop="views" label="浏览量" width="100" align="center" />
            <el-table-column prop="createTime" label="发布时间" width="160">
              <template #default="scope">
                {{ formatDate(scope.row.createTime) }}
              </template>
            </el-table-column>
            <el-table-column label="操作" width="180" fixed="right">
              <template #default="scope">
                <el-button type="success" size="small" @click="approveItem('guide', scope.row)">
                  <el-icon><Check /></el-icon> 通过
                </el-button>
                <el-button type="danger" size="small" @click="rejectItem('guide', scope.row)">
                  <el-icon><Close /></el-icon> 拒绝
                </el-button>
              </template>
            </el-table-column>
          </el-table>
          
          <div class="pagination-container">
            <el-pagination
              background
              layout="total, prev, pager, next"
              :current-page="guidePage"
              :page-size="guidePageSize"
              :total="guideTotal"
              @current-change="handleGuidePageChange"
            />
          </div>
        </el-tab-pane>

        <!-- 审核记录 -->
        <el-tab-pane label="审核记录" name="history">
          <div class="history-info">
            <el-alert type="info" :closable="false">
              <template #title>
                <span>审核记录显示所有已审核的内容，包括通过和拒绝的记录。</span>
              </template>
            </el-alert>
          </div>
          
          <el-tabs v-model="historySubTab">
            <el-tab-pane label="评论记录" name="commentHistory">
              <el-table :data="commentHistoryData" border stripe v-loading="historyLoading" class="review-table">
                <el-table-column prop="id" label="评论ID" width="100" />
                <el-table-column label="用户" width="140">
                  <template #default="scope">
                    <div class="user-cell">
                      <el-avatar :src="scope.row.userAvatar" :size="32">
                        {{ (scope.row.userNickname || '用户').charAt(0) }}
                      </el-avatar>
                      <span class="user-name">{{ scope.row.userNickname || '用户' }}</span>
                    </div>
                  </template>
                </el-table-column>
                <el-table-column prop="content" label="评论内容" show-overflow-tooltip />
                <el-table-column label="审核结果" width="100" align="center">
                  <template #default="scope">
                    <el-tag :type="scope.row.reviewStatus === 1 ? 'success' : 'danger'" size="small">
                      {{ scope.row.reviewStatus === 1 ? '通过' : '拒绝' }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="审核人" width="120">
                  <template #default="scope">
                    {{ scope.row.reviewerName || '-' }}
                  </template>
                </el-table-column>
                <el-table-column prop="reviewTime" label="审核时间" width="160">
                  <template #default="scope">
                    {{ formatDate(scope.row.reviewTime) }}
                  </template>
                </el-table-column>
                <el-table-column prop="reviewComment" label="审核意见" show-overflow-tooltip />
              </el-table>
              <div class="pagination-container">
                <el-pagination
                  background
                  layout="total, prev, pager, next"
                  :current-page="historyCommentPage"
                  :page-size="historyPageSize"
                  :total="historyCommentTotal"
                  @current-change="handleHistoryCommentPageChange"
                />
              </div>
            </el-tab-pane>
            
            <el-tab-pane label="攻略记录" name="guideHistory">
              <el-table :data="guideHistoryData" border stripe v-loading="historyLoading" class="review-table">
                <el-table-column prop="id" label="攻略ID" width="100" />
                <el-table-column label="作者" width="140">
                  <template #default="scope">
                    <div class="user-cell">
                      <el-avatar :src="scope.row.userAvatar" :size="32">
                        {{ (scope.row.userNickname || '用户').charAt(0) }}
                      </el-avatar>
                      <span class="user-name">{{ scope.row.userNickname || '用户' }}</span>
                    </div>
                  </template>
                </el-table-column>
                <el-table-column prop="title" label="标题" show-overflow-tooltip />
                <el-table-column label="审核结果" width="100" align="center">
                  <template #default="scope">
                    <el-tag :type="scope.row.reviewStatus === 1 ? 'success' : 'danger'" size="small">
                      {{ scope.row.reviewStatus === 1 ? '通过' : '拒绝' }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="审核人" width="120">
                  <template #default="scope">
                    {{ scope.row.reviewerName || '-' }}
                  </template>
                </el-table-column>
                <el-table-column prop="reviewTime" label="审核时间" width="160">
                  <template #default="scope">
                    {{ formatDate(scope.row.reviewTime) }}
                  </template>
                </el-table-column>
                <el-table-column prop="reviewComment" label="审核意见" show-overflow-tooltip />
              </el-table>
              <div class="pagination-container">
                <el-pagination
                  background
                  layout="total, prev, pager, next"
                  :current-page="historyGuidePage"
                  :page-size="historyPageSize"
                  :total="historyGuideTotal"
                  @current-change="handleHistoryGuidePageChange"
                />
              </div>
            </el-tab-pane>

            <el-tab-pane label="住宿评价记录" name="accommodationHistory">
              <el-table :data="accommodationHistoryData" border stripe v-loading="historyLoading" class="review-table">
                <el-table-column prop="id" label="评价ID" width="100" />
                <el-table-column label="用户" width="140">
                  <template #default="scope">
                    <div class="user-cell">
                      <el-avatar :src="scope.row.avatar" :size="32">
                        {{ (scope.row.nickname || '用户').charAt(0) }}
                      </el-avatar>
                      <span class="user-name">{{ scope.row.nickname || '用户' }}</span>
                    </div>
                  </template>
                </el-table-column>
                <el-table-column prop="content" label="评价内容" show-overflow-tooltip />
                <el-table-column label="审核结果" width="100" align="center">
                  <template #default="scope">
                    <el-tag :type="scope.row.reviewStatus === 1 ? 'success' : 'danger'" size="small">
                      {{ scope.row.reviewStatus === 1 ? '通过' : '拒绝' }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="审核人" width="120">
                  <template #default="scope">{{ scope.row.reviewerName || '-' }}</template>
                </el-table-column>
                <el-table-column prop="reviewTime" label="审核时间" width="160">
                  <template #default="scope">{{ formatDate(scope.row.reviewTime) }}</template>
                </el-table-column>
                <el-table-column prop="reviewComment" label="审核意见" show-overflow-tooltip />
              </el-table>
              <div class="pagination-container">
                <el-pagination
                  background
                  layout="total, prev, pager, next"
                  :current-page="historyAccommodationPage"
                  :page-size="historyPageSize"
                  :total="historyAccommodationTotal"
                  @current-change="handleHistoryAccommodationPageChange"
                />
              </div>
            </el-tab-pane>
          </el-tabs>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <!-- 拒绝原因对话框 -->
    <el-dialog v-model="rejectDialogVisible" title="拒绝原因" width="500px">
      <el-form>
        <el-form-item label="拒绝原因">
          <el-input v-model="rejectComment" type="textarea" :rows="3" placeholder="请输入拒绝原因（可选）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rejectDialogVisible = false">取消</el-button>
        <el-button type="danger" @click="confirmReject">确认拒绝</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Clock, ChatDotRound, Document, Finished, Check, Close } from '@element-plus/icons-vue'
import request from '@/utils/request'

const baseAPI = process.env.VUE_APP_BASE_API || '/api'

// 统计数据
const stats = ref({
  pendingComment: 0,
  pendingGuide: 0,
  pendingAccommodation: 0,
  approvedComment: 0,
  approvedGuide: 0,
  approvedAccommodation: 0,
  rejectedComment: 0,
  rejectedGuide: 0,
  rejectedAccommodation: 0
})

// 标签页状态
const activeTab = ref('comment')
const historySubTab = ref('commentHistory')

// 评论表格
const commentTableData = ref([])
const commentLoading = ref(false)
const commentPage = ref(1)
const commentPageSize = ref(10)
const commentTotal = ref(0)
const multipleSelectionComment = ref([])

// 住宿评价表格
const accommodationTableData = ref([])
const accommodationLoading = ref(false)
const accommodationPage = ref(1)
const accommodationPageSize = ref(10)
const accommodationTotal = ref(0)
const multipleSelectionAccommodation = ref([])

// 攻略表格
const guideTableData = ref([])
const guideLoading = ref(false)
const guidePage = ref(1)
const guidePageSize = ref(10)
const guideTotal = ref(0)
const multipleSelectionGuide = ref([])

// 历史记录表格
const commentHistoryData = ref([])
const guideHistoryData = ref([])
const accommodationHistoryData = ref([])
const historyLoading = ref(false)
const historyCommentPage = ref(1)
const historyGuidePage = ref(1)
const historyAccommodationPage = ref(1)
const historyPageSize = ref(10)
const historyCommentTotal = ref(0)
const historyGuideTotal = ref(0)
const historyAccommodationTotal = ref(0)

// 拒绝对话框
const rejectDialogVisible = ref(false)
const rejectComment = ref('')
const currentRejectItem = ref(null)
const currentRejectType = ref('')

// 获取统计数据
const fetchStats = async () => {
  try {
    await request.get('/review/stats', null, {
      showDefaultMsg: false,
      onSuccess: (res) => {
        stats.value = {
          pendingComment: res.pendingCommentCount || 0,
          pendingGuide: res.pendingGuideCount || 0,
          pendingAccommodation: res.pendingAccommodationReviewCount || 0,
          approvedComment: res.approvedCommentCount || 0,
          approvedGuide: res.approvedGuideCount || 0,
          approvedAccommodation: res.approvedAccommodationReviewCount || 0,
          rejectedComment: res.rejectedCommentCount || 0,
          rejectedGuide: res.rejectedGuideCount || 0,
          rejectedAccommodation: res.rejectedAccommodationReviewCount || 0
        }
      }
    })
  } catch (error) {
    console.error('获取统计数据失败:', error)
  }
}

// 获取待审核住宿评价
const fetchPendingAccommodationReviews = async () => {
  accommodationLoading.value = true
  try {
    await request.get('/review/accommodation/pending', {
      currentPage: accommodationPage.value,
      size: accommodationPageSize.value
    }, {
      showDefaultMsg: false,
      onSuccess: (res) => {
        accommodationTableData.value = res.records || []
        accommodationTotal.value = res.total || 0
      }
    })
  } catch (error) {
    console.error('获取待审核住宿评价失败:', error)
  } finally {
    accommodationLoading.value = false
  }
}

// 获取待审核评论
const fetchPendingComments = async () => {
  commentLoading.value = true
  try {
    await request.get('/review/comment/pending', {
      currentPage: commentPage.value,
      size: commentPageSize.value
    }, {
      showDefaultMsg: false,
      onSuccess: (res) => {
        commentTableData.value = res.records || []
        commentTotal.value = res.total || 0
      }
    })
  } catch (error) {
    console.error('获取待审核评论失败:', error)
  } finally {
    commentLoading.value = false
  }
}

// 获取住宿评价历史
const fetchAccommodationHistory = async () => {
  historyLoading.value = true
  try {
    await request.get('/review/accommodation/all', {
      reviewStatus: '',
      currentPage: historyAccommodationPage.value,
      size: historyPageSize.value
    }, {
      showDefaultMsg: false,
      onSuccess: (res) => {
        accommodationHistoryData.value = (res.records || []).filter(item => item.reviewStatus !== 0)
        historyAccommodationTotal.value = res.total || 0
      }
    })
  } catch (error) {
    console.error('获取住宿评价历史失败:', error)
  } finally {
    historyLoading.value = false
  }
}

// 获取待审核攻略
const fetchPendingGuides = async () => {
  guideLoading.value = true
  try {
    await request.get('/review/guide/pending', {
      currentPage: guidePage.value,
      size: guidePageSize.value
    }, {
      showDefaultMsg: false,
      onSuccess: (res) => {
        guideTableData.value = res.records || []
        guideTotal.value = res.total || 0
      }
    })
  } catch (error) {
    console.error('获取待审核攻略失败:', error)
  } finally {
    guideLoading.value = false
  }
}

// 获取评论历史
const fetchCommentHistory = async () => {
  historyLoading.value = true
  try {
    await request.get('/review/comment/all', {
      reviewStatus: '', // 查询所有状态
      currentPage: historyCommentPage.value,
      size: historyPageSize.value
    }, {
      showDefaultMsg: false,
      onSuccess: (res) => {
        commentHistoryData.value = (res.records || []).filter(item => item.reviewStatus !== 0)
        historyCommentTotal.value = res.total || 0
      }
    })
  } catch (error) {
    console.error('获取评论历史失败:', error)
  } finally {
    historyLoading.value = false
  }
}

// 获取攻略历史
const fetchGuideHistory = async () => {
  historyLoading.value = true
  try {
    await request.get('/review/guide/all', {
      reviewStatus: '',
      currentPage: historyGuidePage.value,
      size: historyPageSize.value
    }, {
      showDefaultMsg: false,
      onSuccess: (res) => {
        guideHistoryData.value = (res.records || []).filter(item => item.reviewStatus !== 0)
        historyGuideTotal.value = res.total || 0
      }
    })
  } catch (error) {
    console.error('获取攻略历史失败:', error)
  } finally {
    historyLoading.value = false
  }
}

// 审核操作
const approveItem = async (type, item) => {
  try {
    await request.post(`/review/${type}/${item.id}`, {
      status: 1
    }, {
      successMsg: '审核通过'
    })
    // 刷新列表
    if (type === 'comment') {
      fetchPendingComments()
    } else if (type === 'accommodation') {
      fetchPendingAccommodationReviews()
    } else {
      fetchPendingGuides()
    }
    fetchStats()
  } catch (error) {
    console.error('审核操作失败:', error)
  }
}

const rejectItem = (type, item) => {
  currentRejectItem.value = item
  currentRejectType.value = type
  rejectComment.value = ''
  rejectDialogVisible.value = true
}

const confirmReject = async () => {
  try {
    await request.post(`/review/${currentRejectType.value}/${currentRejectItem.value.id}`, {
      status: 2,
      reviewComment: rejectComment.value
    }, {
      successMsg: '已拒绝'
    })
    rejectDialogVisible.value = false
    // 刷新列表
    if (currentRejectType.value === 'comment') {
      fetchPendingComments()
    } else if (currentRejectType.value === 'accommodation') {
      fetchPendingAccommodationReviews()
    } else {
      fetchPendingGuides()
    }
    fetchStats()
  } catch (error) {
    console.error('拒绝操作失败:', error)
  }
}

// 批量审核
const batchApprove = async (type) => {
  const items = type === 'comment' ? multipleSelectionComment.value : (type === 'accommodation' ? multipleSelectionAccommodation.value : multipleSelectionGuide.value)
  const ids = items.map(item => item.id)
  
  try {
    await ElMessageBox.confirm(`确定要通过选中的 ${ids.length} 条内容吗？`, '批量通过', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'success'
    })
    
    await request.post(`/review/${type}/batch`, {
      ids: ids,
      status: 1
    }, {
      successMsg: '批量通过成功'
    })
    
    // 刷新列表
    if (type === 'comment') {
      fetchPendingComments()
    } else if (type === 'accommodation') {
      fetchPendingAccommodationReviews()
    } else {
      fetchPendingGuides()
    }
    fetchStats()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('批量通过失败:', error)
    }
  }
}

const batchReject = async (type) => {
  const items = type === 'comment' ? multipleSelectionComment.value : (type === 'accommodation' ? multipleSelectionAccommodation.value : multipleSelectionGuide.value)
  const ids = items.map(item => item.id)
  
  try {
    await ElMessageBox.confirm(`确定要拒绝选中的 ${ids.length} 条内容吗？`, '批量拒绝', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    await request.post(`/review/${type}/batch`, {
      ids: ids,
      status: 2,
      reviewComment: '批量拒绝'
    }, {
      successMsg: '批量拒绝成功'
    })
    
    // 刷新列表
    if (type === 'comment') {
      fetchPendingComments()
    } else if (type === 'accommodation') {
      fetchPendingAccommodationReviews()
    } else {
      fetchPendingGuides()
    }
    fetchStats()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('批量拒绝失败:', error)
    }
  }
}

// 选择变化
const handleCommentSelectionChange = (selection) => {
  multipleSelectionComment.value = selection
}

const handleAccommodationSelectionChange = (selection) => {
  multipleSelectionAccommodation.value = selection
}

const handleGuideSelectionChange = (selection) => {
  multipleSelectionGuide.value = selection
}

// 标签页切换
const handleTabChange = (tab) => {
  if (tab === 'comment') {
    fetchPendingComments()
  } else if (tab === 'accommodation') {
    fetchPendingAccommodationReviews()
  } else if (tab === 'guide') {
    fetchPendingGuides()
  } else if (tab === 'history') {
    fetchCommentHistory()
    fetchGuideHistory()
    fetchAccommodationHistory()
  }
}

// 分页变化
const handleCommentPageChange = (page) => {
  commentPage.value = page
  fetchPendingComments()
}

const handleGuidePageChange = (page) => {
  guidePage.value = page
  fetchPendingGuides()
}

const handleAccommodationPageChange = (page) => {
  accommodationPage.value = page
  fetchPendingAccommodationReviews()
}

const handleHistoryCommentPageChange = (page) => {
  historyCommentPage.value = page
  fetchCommentHistory()
}

const handleHistoryGuidePageChange = (page) => {
  historyGuidePage.value = page
  fetchGuideHistory()
}

const handleHistoryAccommodationPageChange = (page) => {
  historyAccommodationPage.value = page
  fetchAccommodationHistory()
}

// 格式化日期
const formatDate = (date) => {
  if (!date) return '-'
  const d = new Date(date)
  return d.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

// 初始化
onMounted(() => {
  fetchStats()
  fetchPendingComments()
})
</script>

<style lang="scss" scoped>
.review-management {
  padding: 20px;
  background-color: #f9fafc;
  min-height: calc(100vh - 120px);

  .page-header {
    margin-bottom: 24px;
    text-align: left;
    
    .page-title {
      font-size: 24px;
      color: #34495e;
      margin: 0 0 8px 0;
      font-weight: 500;
    }
    
    .page-subtitle {
      font-size: 14px;
      color: #7f8c8d;
      margin: 0;
      font-style: italic;
    }
  }

  .stats-row {
    margin-bottom: 20px;

    .stat-card {
      border-radius: 8px;
      overflow: hidden;

      .stat-content {
        display: flex;
        align-items: center;
        padding: 10px 0;

        .stat-icon {
          width: 60px;
          height: 60px;
          border-radius: 50%;
          display: flex;
          align-items: center;
          justify-content: center;
          font-size: 28px;
          color: white;
          margin-right: 16px;
        }

        .stat-info {
          .stat-value {
            font-size: 28px;
            font-weight: bold;
            color: #2c3e50;
          }
          .stat-label {
            font-size: 14px;
            color: #7f8c8d;
          }
        }
      }
    }

    .pending-card .stat-icon {
      background: linear-gradient(135deg, #f39c12, #e67e22);
    }
    .comment-card .stat-icon {
      background: linear-gradient(135deg, #3498db, #2980b9);
    }
    .guide-card .stat-icon {
      background: linear-gradient(135deg, #9b59b6, #8e44ad);
    }
    .total-card .stat-icon {
      background: linear-gradient(135deg, #27ae60, #2ecc71);
    }
  }

  .main-card {
    border-radius: 8px;

    .tab-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 16px;

      .tab-actions {
        display: flex;
        gap: 10px;
      }
    }

    .history-info {
      margin-bottom: 16px;
    }

    .review-table {
      margin-top: 16px;

      .user-info {
        display: flex;
        align-items: center;
        gap: 8px;
      }

      .username-text {
        color: #34495e;
        font-weight: 500;
      }

      .user-cell {
        display: flex;
        align-items: center;
        gap: 8px;
        padding: 4px 0;

        .user-name {
          color: #34495e;
          font-weight: 500;
          max-width: 80px;
          overflow: hidden;
          text-overflow: ellipsis;
          white-space: nowrap;
        }
      }

      .rating-cell {
        padding: 0 8px;
        :deep(.el-rate) {
          .el-rate__icon {
            font-size: 20px;
          }
        }
      }

      :deep(.el-table-column--selection) {
        .el-table__header-wrapper,
        .el-table__body-wrapper {
          .cell {
            padding-left: 16px;
            padding-right: 16px;
          }
        }
      }
    }
  }

  .pagination-container {
    margin-top: 20px;
    display: flex;
    justify-content: flex-end;
  }
}
</style>
