<template>
  <div class="notification-page">
    <div class="page-header">
      <div>
        <h1>站内消息</h1>
        <p>统一发送管理员通知，查看消息触达、已读状态和系统通知记录。</p>
      </div>
    </div>

    <section class="send-panel">
      <div class="section-title">
        <h2>发送消息</h2>
        <el-tag type="info" effect="plain">发送后会进入站内消息记录，并触发接收人的铃铛提醒</el-tag>
      </div>
      <el-form :model="form" label-width="88px">
        <el-form-item label="接收对象">
          <el-radio-group v-model="form.target">
            <el-radio-button label="ALL">全部用户</el-radio-button>
            <el-radio-button label="USER">普通用户</el-radio-button>
            <el-radio-button label="ADMIN">管理员</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="标题">
          <el-input v-model.trim="form.title" maxlength="120" show-word-limit placeholder="请输入消息标题" />
        </el-form-item>
        <el-form-item label="内容">
          <el-input v-model.trim="form.content" type="textarea" :rows="5" maxlength="1000" show-word-limit placeholder="请输入通知内容" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="submitting" @click="sendMessage">发送消息</el-button>
          <el-button @click="resetForm">清空</el-button>
        </el-form-item>
      </el-form>
    </section>

    <section class="record-panel">
      <div class="section-title">
        <h2>消息记录</h2>
        <div class="record-filters">
          <el-input v-model="query.keyword" placeholder="搜索标题或内容" clearable @keyup.enter="loadRecords" />
          <el-select v-model="query.readStatus" placeholder="阅读状态" clearable>
            <el-option label="未读" :value="0" />
            <el-option label="已读" :value="1" />
          </el-select>
          <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
          <el-button :icon="Refresh" @click="resetSearch">重置</el-button>
        </div>
      </div>

      <div class="record-toolbar">
        <span>已选 {{ selectedRows.length }} 条</span>
        <el-button type="danger" :icon="Delete" :disabled="selectedRows.length === 0" @click="deleteSelected">
          批量删除
        </el-button>
      </div>

      <el-table :data="records" v-loading="loading" border row-key="id" @selection-change="selectedRows = $event">
        <el-table-column type="selection" width="48" align="center" />
        <el-table-column prop="title" label="标题" min-width="150" show-overflow-tooltip />
        <el-table-column prop="content" label="内容" min-width="220" show-overflow-tooltip />
        <el-table-column prop="senderName" label="发送人" width="110" show-overflow-tooltip>
          <template #default="{ row }">{{ row.senderName || senderText(row.senderType) }}</template>
        </el-table-column>
        <el-table-column prop="receiverName" label="接收人" width="150" show-overflow-tooltip>
          <template #default="{ row }">{{ row.receiverName || `用户#${row.userId}` }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.readStatus === 1 ? 'success' : 'warning'" effect="plain">
              {{ row.readStatus === 1 ? '已读' : '未读' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="type" label="类型" width="110">
          <template #default="{ row }">{{ typeText(row.type) }}</template>
        </el-table-column>
        <el-table-column prop="createTime" label="发送时间" width="170">
          <template #default="{ row }">{{ formatDate(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="90" fixed="right">
          <template #default="{ row }">
            <el-button link type="danger" :icon="Delete" @click="deleteRecord(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination">
        <el-pagination
          background
          layout="total, sizes, prev, pager, next"
          v-model:current-page="query.currentPage"
          v-model:page-size="query.size"
          :page-sizes="[10, 20, 50]"
          :total="total"
          @size-change="loadRecords"
          @current-change="loadRecords"
        />
      </div>
    </section>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete, Refresh, Search } from '@element-plus/icons-vue'
import request from '@/utils/request'
import { formatDate } from '@/utils/dateUtils'

const submitting = ref(false)
const loading = ref(false)
const records = ref([])
const total = ref(0)
const selectedRows = ref([])

const form = reactive({
  target: 'ALL',
  title: '',
  content: ''
})

const query = reactive({
  keyword: '',
  readStatus: '',
  currentPage: 1,
  size: 10
})

const resetForm = () => {
  form.target = 'ALL'
  form.title = ''
  form.content = ''
}

const sendMessage = async () => {
  if (!form.title || !form.content) {
    ElMessage.warning('请填写标题和内容')
    return
  }
  submitting.value = true
  try {
    await request.post('/notification/admin/send', form, { successMsg: '站内消息已发送' })
    resetForm()
    loadRecords()
  } finally {
    submitting.value = false
  }
}

const loadRecords = async () => {
  loading.value = true
  try {
    const params = { ...query }
    if (params.readStatus === '') delete params.readStatus
    const res = await request.get('/notification/admin/page', params, { showDefaultMsg: false })
    records.value = res.records || []
    total.value = res.total || 0
    selectedRows.value = []
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  query.currentPage = 1
  loadRecords()
}

const resetSearch = () => {
  query.keyword = ''
  query.readStatus = ''
  query.currentPage = 1
  loadRecords()
}

const deleteRecord = async (row) => {
  try {
    await ElMessageBox.confirm(`确认删除消息「${row.title}」？`, '删除站内消息', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
    await request.delete(`/notification/admin/${row.id}`, { successMsg: '站内消息记录已删除' })
    loadRecords()
  } catch (error) {
    // 取消删除时保持当前状态
  }
}

const deleteSelected = async () => {
  if (!selectedRows.value.length) return
  try {
    await ElMessageBox.confirm(`确认删除选中的 ${selectedRows.value.length} 条站内消息？`, '批量删除站内消息', {
      type: 'warning',
      confirmButtonText: '批量删除',
      cancelButtonText: '取消'
    })
    await request.delete('/notification/admin/batch', {
      data: selectedRows.value.map(item => item.id),
      successMsg: '站内消息记录已批量删除'
    })
    loadRecords()
  } catch (error) {
    // 取消删除时保持当前状态
  }
}

const typeText = (type) => {
  const map = {
    ADMIN: '管理员通知',
    SYSTEM: '系统通知',
    SECURITY: '安全提醒',
    ORDER: '订单提醒',
    REVIEW: '审核通知',
    ACCOUNT: '账号通知'
  }
  return map[type] || type || '系统通知'
}

const senderText = (senderType) => {
  if (senderType === 'ADMIN') return '管理员'
  if (senderType === 'USER') return '用户'
  return '系统'
}

onMounted(loadRecords)
</script>

<style scoped>
.notification-page {
  padding: 20px;
  min-height: calc(100vh - 120px);
  background: #f6f8fb;
}

.page-header {
  margin-bottom: 18px;
  text-align: left;
}

.page-header h1 {
  margin: 0 0 6px;
  font-size: 24px;
  color: #1f2937;
}

.page-header p {
  margin: 0;
  color: #64748b;
}

.send-panel,
.record-panel {
  padding: 20px;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
}

.send-panel {
  margin-bottom: 16px;
}

.section-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding-left: 12px;
  border-left: 4px solid #409eff;
  margin-bottom: 16px;
}

.section-title h2 {
  margin: 0;
  font-size: 17px;
  color: #1f2937;
}

.record-filters {
  display: flex;
  align-items: center;
  gap: 10px;
}

.record-filters .el-input {
  width: 220px;
}

.record-filters .el-select {
  width: 130px;
}

.record-toolbar {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;
  margin-bottom: 12px;
  color: #64748b;
  font-size: 14px;
}

.pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
