<template>
  <el-popover placement="bottom-end" width="360" :trigger="trigger" @show="loadMessages">
    <template #reference>
      <el-badge :value="unreadCount" :hidden="unreadCount === 0" :max="99" class="notice-badge">
        <button class="notice-trigger" type="button" aria-label="站内消息" @click="handleBellClick">
          <el-icon><Bell /></el-icon>
        </button>
      </el-badge>
    </template>

    <div class="notice-panel">
      <div class="notice-header">
        <strong>站内消息</strong>
        <el-button text type="primary" @click="markAllRead" :disabled="unreadCount === 0">全部已读</el-button>
      </div>
      <div class="notice-body" @scroll="handleScroll">
        <el-empty v-if="messages.length === 0 && !loading" description="暂无消息" :image-size="72" />
        <div v-else class="notice-list">
          <div
            v-for="item in messages"
            :key="item.id"
            class="notice-item"
            :class="{ unread: item.readStatus === 0 }"
            @click="openMessage(item)"
          >
            <div class="notice-title">
              <span>{{ item.title }}</span>
              <el-tag v-if="item.readStatus === 0" size="small" type="danger">未读</el-tag>
            </div>
            <p>{{ item.content }}</p>
            <time>{{ formatDate(item.createTime) }}</time>
          </div>
        </div>
        <div v-if="loading" class="notice-loading">加载中...</div>
        <div v-else-if="messages.length > 0 && !hasMore" class="notice-end">已显示全部消息</div>
      </div>
    </div>
  </el-popover>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { Bell } from '@element-plus/icons-vue'
import request from '@/utils/request'
import { formatDate } from '@/utils/dateUtils'

const router = useRouter()
const props = defineProps({
  trigger: {
    type: String,
    default: 'click'
  },
  clickToCenter: {
    type: Boolean,
    default: false
  }
})
const unreadCount = ref(0)
const messages = ref([])
const currentPage = ref(1)
const pageSize = 10
const total = ref(0)
const loading = ref(false)
const hasMore = ref(true)
let unreadTimer = null
const UNREAD_POLL_INTERVAL = 15000

const loadUnread = async () => {
  try {
    unreadCount.value = await request.get('/notification/unread-count', {}, { showDefaultMsg: false })
  } catch (error) {
    unreadCount.value = 0
  }
}

const startUnreadPolling = () => {
  stopUnreadPolling()
  unreadTimer = window.setInterval(loadUnread, UNREAD_POLL_INTERVAL)
}

const stopUnreadPolling = () => {
  if (unreadTimer) {
    window.clearInterval(unreadTimer)
    unreadTimer = null
  }
}

const handleVisibilityChange = () => {
  if (!document.hidden) {
    loadUnread()
  }
}

const loadMessages = async () => {
  currentPage.value = 1
  messages.value = []
  hasMore.value = true
  await loadNextPage()
}

const loadNextPage = async () => {
  if (loading.value || !hasMore.value) return
  loading.value = true
  try {
    const res = await request.get('/notification/page', { currentPage: currentPage.value, size: pageSize }, { showDefaultMsg: false })
    const records = res.records || []
    total.value = res.total || 0
    messages.value = currentPage.value === 1 ? records : [...messages.value, ...records]
    hasMore.value = messages.value.length < total.value
    currentPage.value += 1
    loadUnread()
  } catch (error) {
    if (currentPage.value === 1) {
      messages.value = []
    }
    hasMore.value = false
  } finally {
    loading.value = false
  }
}

const handleScroll = (event) => {
  const target = event.target
  if (target.scrollTop + target.clientHeight >= target.scrollHeight - 24) {
    loadNextPage()
  }
}

const markAllRead = async () => {
  await request.put('/notification/read-all', {}, { showDefaultMsg: false })
  await loadMessages()
}

const openMessage = async (item) => {
  if (item.readStatus === 0) {
    await request.put(`/notification/${item.id}/read`, {}, { showDefaultMsg: false })
    item.readStatus = 1
    loadUnread()
  }
  if (item.linkUrl) {
    router.push(item.linkUrl)
  }
}

const handleBellClick = () => {
  if (props.clickToCenter) {
    router.push('/profile?tab=notifications')
  }
}

onMounted(() => {
  loadUnread()
  startUnreadPolling()
  document.addEventListener('visibilitychange', handleVisibilityChange)
})

onUnmounted(() => {
  stopUnreadPolling()
  document.removeEventListener('visibilitychange', handleVisibilityChange)
})
</script>

<style scoped>
.notice-badge {
  margin-right: 8px;
}

.notice-trigger {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  padding: 0;
  color: #dc2626;
  background: transparent;
  border: 0;
  cursor: pointer;
}

.notice-trigger :deep(.el-icon) {
  font-size: 22px;
  stroke-width: 2.8;
}

.notice-trigger :deep(svg path) {
  stroke-width: 2.8;
}

.notice-trigger:hover {
  color: #b91c1c;
}

.notice-panel {
  max-height: 480px;
}

.notice-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-bottom: 8px;
  border-bottom: 1px solid #edf0f5;
}

.notice-list {
  display: grid;
  gap: 8px;
  margin-top: 10px;
}

.notice-body {
  max-height: 420px;
  overflow-y: auto;
  padding-right: 4px;
}

.notice-body::-webkit-scrollbar {
  width: 6px;
}

.notice-body::-webkit-scrollbar-thumb {
  background: #cbd5e1;
  border-radius: 999px;
}

.notice-item {
  padding: 10px 12px;
  border: 1px solid #edf0f5;
  border-radius: 8px;
  cursor: pointer;
  background: #fff;
}

.notice-item.unread {
  background: #fff7f7;
  border-color: #fecaca;
}

.notice-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  font-weight: 600;
  color: #1f2937;
}

.notice-item p {
  margin: 6px 0;
  color: #64748b;
  line-height: 1.5;
}

.notice-item time {
  color: #94a3b8;
  font-size: 12px;
}

.notice-loading,
.notice-end {
  padding: 10px 0 2px;
  text-align: center;
  color: #94a3b8;
  font-size: 12px;
}
</style>
