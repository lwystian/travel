<template>
  <div class="system-log-page">
    <div class="page-header">
      <div>
        <h2>系统日志</h2>
        <p>记录关键业务操作、访问来源与执行结果，用于审计追踪和生产排障。</p>
      </div>
      <div class="header-actions">
        <el-button :loading="downloadLoading" @click="openExportDialog">
          <el-icon><Download /></el-icon>
          下载备份
        </el-button>
        <el-button type="danger" :disabled="selectedRows.length === 0" :loading="deleteLoading" @click="handleBatchDelete">
          <el-icon><Delete /></el-icon>
          批量删除
        </el-button>
      </div>
    </div>

    <div class="filter-panel">
      <el-form :inline="true" :model="queryForm">
        <el-form-item label="操作人">
          <el-input v-model="queryForm.username" placeholder="输入用户名" clearable @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item label="操作类型">
          <el-select v-model="queryForm.operationType" placeholder="全部类型" clearable>
            <el-option v-for="item in operationOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="日志等级">
          <el-select v-model="queryForm.logLevel" placeholder="全部等级" clearable>
            <el-option v-for="item in logLevelOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="时间范围">
          <el-date-picker
            v-model="dateRange"
            type="datetimerange"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            value-format="YYYY-MM-DD HH:mm:ss"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleQuery">
            <el-icon><Search /></el-icon>
            查询
          </el-button>
          <el-button @click="handleReset">
            <el-icon><Refresh /></el-icon>
            重置
          </el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="table-panel">
      <el-table :data="tableData" v-loading="loading" row-key="id" stripe @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="48" />
        <el-table-column prop="createTime" label="时间" width="170" />
        <el-table-column label="等级" width="90">
          <template #default="{ row }">
            <el-tag size="small" :type="getLogLevelTagType(row)">
              {{ formatLogLevel(row) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作人" width="130">
          <template #default="{ row }">{{ row.username || '系统' }}</template>
        </el-table-column>
        <el-table-column label="操作" min-width="220">
          <template #default="{ row }">
            <div class="operation-cell">
              <el-tag size="small" :type="getOperationTagType(row.operationType)">
                {{ formatOperationType(row.operationType) }}
              </el-tag>
              <span>{{ formatOperationDesc(row) }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="对象" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">{{ formatTarget(row) }}</template>
        </el-table-column>
        <el-table-column label="结果" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" effect="light">
              {{ row.status === 1 ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="耗时" width="90">
          <template #default="{ row }">
            <span :class="{ 'slow-text': Number(row.executionTime) >= 1000 }">{{ row.executionTime || 0 }}ms</span>
          </template>
        </el-table-column>
        <el-table-column prop="ipAddress" label="IP" width="140" />
        <el-table-column label="请求" min-width="220" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="method-text">{{ row.requestMethod || '-' }}</span>
            <span>{{ row.requestUrl || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleDetail(row)">详情</el-button>
            <el-button link type="danger" :loading="deleteLoadingId === row.id" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-container">
        <el-pagination
          v-model:current-page="pagination.currentPage"
          v-model:page-size="pagination.size"
          :total="pagination.total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="loadData"
          @current-change="loadData"
        />
      </div>
    </div>

    <el-dialog v-model="detailVisible" title="系统日志详情" width="820px">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="日志ID">{{ detailData.id }}</el-descriptions-item>
        <el-descriptions-item label="时间">{{ detailData.createTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="日志等级">{{ formatLogLevel(detailData) }}</el-descriptions-item>
        <el-descriptions-item label="操作人">{{ detailData.username || '系统' }}</el-descriptions-item>
        <el-descriptions-item label="操作类型">{{ formatOperationType(detailData.operationType) }}</el-descriptions-item>
        <el-descriptions-item label="操作说明" :span="2">{{ formatOperationDesc(detailData) }}</el-descriptions-item>
        <el-descriptions-item label="操作对象">{{ formatTarget(detailData) }}</el-descriptions-item>
        <el-descriptions-item label="执行结果">
          <el-tag :type="detailData.status === 1 ? 'success' : 'danger'">
            {{ detailData.status === 1 ? '成功' : '失败' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="请求方法">{{ detailData.requestMethod || '-' }}</el-descriptions-item>
        <el-descriptions-item label="耗时">{{ detailData.executionTime || 0 }}ms</el-descriptions-item>
        <el-descriptions-item label="请求地址" :span="2">{{ detailData.requestUrl || '-' }}</el-descriptions-item>
        <el-descriptions-item label="IP地址">{{ detailData.ipAddress || '-' }}</el-descriptions-item>
        <el-descriptions-item label="端口">{{ detailData.port || '-' }}</el-descriptions-item>
        <el-descriptions-item label="User-Agent" :span="2">{{ detailData.userAgent || '-' }}</el-descriptions-item>
        <el-descriptions-item label="请求参数" :span="2">
          <pre>{{ formatJson(detailData.requestParams) }}</pre>
        </el-descriptions-item>
        <el-descriptions-item v-if="detailData.errorMessage" label="错误信息" :span="2">
          <span class="error-text">{{ detailData.errorMessage }}</span>
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>

    <el-dialog v-model="exportVisible" title="导出系统日志" width="520px">
      <el-form :model="exportForm" label-width="110px">
        <el-form-item label="导出范围">
          <el-radio-group v-model="exportForm.scope">
            <el-radio-button label="filtered">当前筛选</el-radio-button>
            <el-radio-button label="selected" :disabled="selectedRows.length === 0">选中日志</el-radio-button>
            <el-radio-button label="all">全部日志</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="文件内容">
          <el-checkbox v-model="exportForm.includeUserAgent">包含 User-Agent</el-checkbox>
          <el-checkbox v-model="exportForm.includeParams">包含请求参数</el-checkbox>
        </el-form-item>
        <el-form-item label="说明">
          <span class="export-tip">{{ exportTip }}</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="exportVisible = false">取消</el-button>
        <el-button type="primary" :loading="downloadLoading" @click="handleDownload">导出 CSV</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete, Download, Refresh, Search } from '@element-plus/icons-vue'
import request from '@/utils/request'
import logger from '@/utils/logger'

const loading = ref(false)
const deleteLoading = ref(false)
const deleteLoadingId = ref(null)
const downloadLoading = ref(false)
const tableData = ref([])
const selectedRows = ref([])
const detailVisible = ref(false)
const exportVisible = ref(false)
const detailData = ref({})
const dateRange = ref([])

const queryForm = reactive({
  username: '',
  operationType: '',
  logLevel: ''
})

const exportForm = reactive({
  scope: 'filtered',
  includeUserAgent: true,
  includeParams: true
})

const pagination = reactive({
  currentPage: 1,
  size: 20,
  total: 0
})

const operationOptions = [
  { label: '查询', value: 'QUERY' },
  { label: '新增', value: 'CREATE' },
  { label: '更新', value: 'UPDATE' },
  { label: '删除', value: 'DELETE' },
  { label: '登录', value: 'LOGIN' },
  { label: '退出', value: 'LOGOUT' },
  { label: '注册', value: 'REGISTER' },
  { label: '导出', value: 'EXPORT' },
  { label: '支付', value: 'PAY' },
  { label: '取消', value: 'CANCEL' },
  { label: '审核', value: 'REVIEW' },
  { label: '审核通过', value: 'REVIEW_PASS' },
  { label: '审核拒绝', value: 'REVIEW_REJECT' }
]

const logLevelOptions = [
  { label: 'INFO', value: 'INFO' },
  { label: 'WARN', value: 'WARN' },
  { label: 'ERROR', value: 'ERROR' }
]

const exportTip = computed(() => {
  if (exportForm.scope === 'selected') {
    return `将导出选中的 ${selectedRows.value.length} 条日志。`
  }
  if (exportForm.scope === 'all') {
    return '将忽略当前筛选条件，导出全部系统日志。'
  }
  return '将按当前操作人、操作类型、日志等级和时间范围导出。'
})

const operationTextMap = {
  QUERY: '查询',
  CREATE: '新增',
  UPDATE: '更新',
  DELETE: '删除',
  LOGIN: '登录',
  LOGOUT: '退出',
  REGISTER: '注册',
  EXPORT: '导出',
  PAY: '支付',
  CANCEL: '取消',
  REVIEW: '审核',
  REVIEW_PASS: '审核通过',
  REVIEW_REJECT: '审核拒绝'
}

const targetTextMap = {
  SysLog: '系统日志',
  系统日志: '系统日志',
  User: '用户',
  ScenicSpot: '景点',
  ScenicCategory: '分类',
  ScenicTag: '标签',
  TravelGuide: '攻略',
  Comment: '评论',
  Tour: '行程',
  TourOrder: '订单',
  PaymentConfig: '支付配置',
  支付配置: '支付配置',
  Review: '内容审核',
  Carousel: '轮播图',
  Accommodation: '住宿',
  AccommodationReview: '住宿评价',
  FrequentTraveler: '常用出行人',
  File: '文件',
  Collection: '收藏',
  ScenicCollection: '景点收藏',
  TourDetail: '行程明细',
  TourHotel: '行程酒店',
  TourOrderPay: '订单支付',
  Traveler: '出行人',
  RedisTest: 'Redis测试'
}

const loadData = async () => {
  loading.value = true
  try {
    const params = {
      currentPage: pagination.currentPage,
      size: pagination.size,
      username: queryForm.username,
      operationType: queryForm.operationType,
      logLevel: queryForm.logLevel
    }

    if (dateRange.value && dateRange.value.length === 2) {
      params.startTime = dateRange.value[0]
      params.endTime = dateRange.value[1]
    }

    const res = await request.get('/sys/log/page', params, { showDefaultMsg: false })
    tableData.value = res.records || []
    pagination.total = Number(res.total || 0)
  } catch (error) {
    logger.error('Load system logs failed:', error)
    ElMessage.error('系统日志加载失败')
  } finally {
    loading.value = false
  }
}

const handleQuery = () => {
  pagination.currentPage = 1
  loadData()
}

const handleReset = () => {
  queryForm.username = ''
  queryForm.operationType = ''
  queryForm.logLevel = ''
  dateRange.value = []
  handleQuery()
}

const handleSelectionChange = (rows) => {
  selectedRows.value = rows
}

const handleDetail = (row) => {
  detailData.value = row
  detailVisible.value = true
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(`确认删除日志 #${row.id}？删除后不可恢复。`, '删除系统日志', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })

    deleteLoadingId.value = row.id
    await request.delete(`/sys/log/${row.id}`, { showDefaultMsg: false })
    ElMessage.success('日志已删除')
    loadData()
  } catch (error) {
    if (error !== 'cancel') {
      logger.error('Delete system log failed:', error)
      ElMessage.error('删除日志失败')
    }
  } finally {
    deleteLoadingId.value = null
  }
}

const handleBatchDelete = async () => {
  if (selectedRows.value.length === 0) return

  try {
    await ElMessageBox.confirm(`确认删除选中的 ${selectedRows.value.length} 条日志？删除后不可恢复。`, '批量删除系统日志', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })

    deleteLoading.value = true
    const params = new URLSearchParams()
    selectedRows.value.forEach(row => params.append('ids', row.id))
    await request.delete(`/sys/log/batch?${params.toString()}`, { showDefaultMsg: false })
    ElMessage.success('选中日志已删除')
    selectedRows.value = []
    loadData()
  } catch (error) {
    if (error !== 'cancel') {
      logger.error('Batch delete system logs failed:', error)
      ElMessage.error('批量删除失败')
    }
  } finally {
    deleteLoading.value = false
  }
}

const openExportDialog = () => {
  if (selectedRows.value.length === 0 && exportForm.scope === 'selected') {
    exportForm.scope = 'filtered'
  }
  exportVisible.value = true
}

const handleDownload = async () => {
  downloadLoading.value = true
  try {
    const params = buildQueryParams()
    const response = await request.download('/sys/log/export', params, { showDefaultMsg: false })
    const blob = await normalizeDownloadBlob(response)
    downloadBlob(blob, `系统日志备份_${formatFileTime(new Date())}.csv`)
    ElMessage.success('系统日志备份已下载')
    exportVisible.value = false
  } catch (error) {
    logger.error('Download system logs failed:', error)
    ElMessage.error('下载备份失败')
  } finally {
    downloadLoading.value = false
  }
}

const normalizeDownloadBlob = async (response) => {
  const contentType = String(response.headers?.['content-type'] || '').toLowerCase()
  if (!contentType.includes('json')) {
    const text = await response.data.text()
    const trimmed = text.trim()
    if (!trimmed.startsWith('{') && !trimmed.startsWith('[')) {
      return new Blob([response.data], {
        type: contentType || 'text/csv;charset=utf-8'
      })
    }

    try {
      const payload = JSON.parse(text)
      const logs = Array.isArray(payload.data) ? payload.data : []
      return buildCsvBlob(logs)
    } catch {
      return new Blob([response.data], {
        type: contentType || 'text/csv;charset=utf-8'
      })
    }
  }

  const text = await response.data.text()
  const payload = JSON.parse(text)
  const logs = Array.isArray(payload.data) ? payload.data : []
  return buildCsvBlob(logs)
}

const buildCsvBlob = (logs) => {
  const headers = ['日志ID', '时间', '等级', '操作人', '操作类型', '操作说明', '操作对象', '请求方法', '请求地址', 'IP', '端口', '结果', '耗时(ms)', '错误信息']
  if (exportForm.includeUserAgent) headers.push('User-Agent')
  if (exportForm.includeParams) headers.push('请求参数')
  const rows = logs.map(log => [
    log.id,
    log.createTime,
    formatLogLevel(log),
    log.username,
    formatOperationType(log.operationType),
    formatOperationDesc(log),
    formatTarget(log),
    log.requestMethod,
    log.requestUrl,
    log.ipAddress,
    log.port,
    log.status === 1 ? '成功' : '失败',
    log.executionTime,
    log.errorMessage
  ].concat(exportForm.includeUserAgent ? [log.userAgent] : [])
    .concat(exportForm.includeParams ? [log.requestParams] : []))

  const csv = `\ufeff${[headers, ...rows].map(row => row.map(csvCell).join(',')).join('\r\n')}\r\n`
  return new Blob([csv], { type: 'text/csv;charset=utf-8' })
}

const csvCell = (value) => {
  if (value === null || value === undefined) return ''
  return `"${String(value).replace(/"/g, '""').replace(/\r?\n/g, ' ')}"`
}

const downloadBlob = (blob, filename) => {
  const url = window.URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  window.URL.revokeObjectURL(url)
}

const buildQueryParams = () => {
  const params = {
    includeUserAgent: exportForm.includeUserAgent,
    includeParams: exportForm.includeParams
  }

  if (exportForm.scope === 'selected') {
    params.ids = selectedRows.value.map(row => row.id).join(',')
    return params
  }

  if (exportForm.scope === 'all') {
    return params
  }

  params.username = queryForm.username
  params.operationType = queryForm.operationType
  params.logLevel = queryForm.logLevel

  if (dateRange.value && dateRange.value.length === 2) {
    params.startTime = dateRange.value[0]
    params.endTime = dateRange.value[1]
  }

  return params
}

const formatFileTime = (date) => {
  const pad = value => String(value).padStart(2, '0')
  return `${date.getFullYear()}${pad(date.getMonth() + 1)}${pad(date.getDate())}${pad(date.getHours())}${pad(date.getMinutes())}${pad(date.getSeconds())}`
}

const formatOperationType = (type) => {
  return operationTextMap[type] || type || '-'
}

const formatLogLevel = (row = {}) => {
  if (row.logLevel) return String(row.logLevel).toUpperCase()
  if (row.errorMessage) return 'ERROR'
  if (row.status === 0) return 'WARN'
  return 'INFO'
}

const getLogLevelTagType = (row = {}) => {
  const level = formatLogLevel(row)
  if (level === 'ERROR') return 'danger'
  if (level === 'WARN') return 'warning'
  return 'info'
}

const formatOperationDesc = (row = {}) => {
  if (isPaymentConfigToggle(row)) {
    const enabled = getParamValue(row.requestParams, 'enabled')
    if (enabled === true || enabled === 'true') {
      return '启用支付配置'
    }
    if (enabled === false || enabled === 'false') {
      return '停用支付配置'
    }
    return '调整支付配置启用状态'
  }

  if (isRawRequestDesc(row.operationDesc)) {
    return buildReadableDesc(row)
  }

  return row.operationDesc || '-'
}

const getOperationTagType = (type) => {
  const map = {
    QUERY: 'info',
    CREATE: 'success',
    UPDATE: 'warning',
    DELETE: 'danger',
    LOGIN: 'primary',
    LOGOUT: 'info',
    REGISTER: 'success',
    EXPORT: 'info',
    PAY: 'success',
    CANCEL: 'warning',
    REVIEW: 'warning',
    REVIEW_PASS: 'success',
    REVIEW_REJECT: 'danger'
  }
  return map[type] || 'info'
}

const formatTarget = (row = {}) => {
  const type = targetTextMap[row.targetType] || row.targetType || '-'
  return type
}

const isPaymentConfigToggle = (row = {}) => {
  return /\/payment-config\/[^/]+\/toggle$/.test(String(row.requestUrl || ''))
}

const isRawRequestDesc = (desc = '') => {
  return /\/api\/|\/[a-z-]+/.test(String(desc))
}

const buildReadableDesc = (row = {}) => {
  const target = targetTextMap[row.targetType] || row.targetType || '数据'
  const action = getReadableAction(row)
  return `${action}${target}`
}

const getReadableAction = (row = {}) => {
  const url = String(row.requestUrl || '')
  const method = String(row.requestMethod || '').toUpperCase()

  if (url.includes('/payment-config')) {
    if (method === 'POST') return '新增'
    if (method === 'PUT' || method === 'PATCH') return '更新'
    if (method === 'DELETE') return '删除'
  }

  return formatOperationType(row.operationType)
}

const getParamValue = (params, key) => {
  if (!params) return undefined
  try {
    const parsed = JSON.parse(params)
    if (Array.isArray(parsed)) {
      const item = parsed.find(param => param && param.name === key)
      return item ? item.value : undefined
    }
    if (parsed && typeof parsed === 'object') {
      return parsed[key]
    }
  } catch {
    return undefined
  }
  return undefined
}

const formatJson = (value) => {
  if (!value) return '-'
  try {
    return JSON.stringify(JSON.parse(value), null, 2)
  } catch {
    return value
  }
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.system-log-page {
  padding: 20px;
  background: #f5f7fb;
  min-height: 100%;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 16px;
  text-align: left;
}

.page-header > div:first-child,
.page-header h2 {
  text-align: left;
}

.page-header h2 {
  margin: 0 0 6px;
  font-size: 22px;
  font-weight: 600;
  color: #1f2937;
}

.page-header p {
  margin: 0;
  color: #6b7280;
  font-size: 13px;
}

.header-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
  flex-shrink: 0;
}

.header-actions :deep(.el-button + .el-button) {
  margin-left: 0;
}

.filter-panel,
.table-panel {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  padding: 16px;
  margin-bottom: 16px;
}

.operation-cell {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.method-text {
  display: inline-block;
  min-width: 48px;
  margin-right: 8px;
  color: #2563eb;
  font-weight: 600;
}

.slow-text {
  color: #d97706;
  font-weight: 600;
}

.error-text {
  color: #dc2626;
}

.export-tip {
  color: #6b7280;
  font-size: 13px;
}

.pagination-container {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

pre {
  margin: 0;
  padding: 10px;
  max-height: 240px;
  overflow: auto;
  white-space: pre-wrap;
  word-break: break-word;
  background: #f9fafb;
  border-radius: 4px;
  color: #374151;
  font-family: Consolas, Monaco, monospace;
  font-size: 12px;
}
</style>
