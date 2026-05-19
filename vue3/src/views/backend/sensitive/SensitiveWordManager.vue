<template>
  <div class="sensitive-page">
    <div class="page-header">
      <div class="header-text">
        <h1>敏感词过滤</h1>
        <p>发布内容先经过敏感词过滤，再进入人工审核；管理员可维护规则、批量导入并即时测试。</p>
      </div>
      <div class="header-actions">
        <el-button :icon="Upload" @click="openImport">批量导入</el-button>
        <el-button type="primary" :icon="Plus" @click="openCreate">新增规则</el-button>
      </div>
    </div>

    <section class="test-panel">
      <div class="panel-title">
        <h2>规则测试</h2>
        <el-button type="primary" plain :icon="CircleCheck" @click="checkContent" :loading="checking">检测</el-button>
      </div>
      <div class="test-layout">
        <el-input
          v-model="testContent"
          type="textarea"
          :rows="4"
          maxlength="500"
          show-word-limit
          placeholder="输入一段评论、攻略或住宿评价内容"
        />
        <div class="check-result" v-if="checkResult">
          <el-alert
            :title="checkResult.hit ? (checkResult.rejected ? '命中禁止发布内容' : '命中敏感词，已生成脱敏结果') : '未命中敏感词'"
            :type="checkResult.rejected ? 'error' : (checkResult.hit ? 'warning' : 'success')"
            show-icon
            :closable="false"
          />
          <div class="hit-list" v-if="checkResult.hit">
            <el-tag
              v-for="item in checkResult.hits"
              :key="`${item.word}-${item.category}`"
              :type="getLevelType(item.level)"
            >
              {{ item.word }} / {{ getCategoryLabel(item.category) }}
            </el-tag>
          </div>
          <div class="filtered-content" v-if="checkResult.hit">{{ checkResult.filteredContent }}</div>
        </div>
      </div>
    </section>

    <div class="filter-bar">
      <el-input class="keyword-input" v-model="query.keyword" placeholder="搜索敏感词" clearable @keyup.enter="loadData" />
      <el-select v-model="query.category" placeholder="分类" clearable>
        <el-option v-for="item in categoryOptions" :key="item.value" :label="item.label" :value="item.value" />
      </el-select>
      <el-select v-model="query.level" placeholder="处置级别" clearable>
        <el-option v-for="item in levelOptions" :key="item.value" :label="item.label" :value="item.value" />
      </el-select>
      <el-select class="status-select" v-model="query.status" placeholder="状态" clearable>
        <el-option label="启用" :value="1" />
        <el-option label="停用" :value="0" />
      </el-select>
      <div class="filter-actions">
        <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
        <el-button :icon="Refresh" @click="resetSearch">重置</el-button>
      </div>
    </div>

    <section class="table-panel">
      <div class="table-toolbar">
        <span>已选 {{ selectedRows.length }} 项</span>
        <el-button type="danger" :icon="Delete" :disabled="selectedRows.length === 0" @click="removeSelected">批量删除</el-button>
      </div>

      <el-table
        :data="tableData"
        v-loading="loading"
        border
        row-key="id"
        @selection-change="selectedRows = $event"
      >
        <el-table-column type="selection" width="48" align="center" />
        <el-table-column
          prop="word"
          label="敏感词"
          min-width="170"
          align="left"
          header-align="left"
          class-name="word-column"
          label-class-name="word-header"
          show-overflow-tooltip
        >
          <template #default="{ row }">
            <span class="word-text">{{ row.word }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="category" label="分类" width="130" align="left" header-align="left">
          <template #default="{ row }">
            <el-tag effect="plain">{{ getCategoryLabel(row.category) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="level" label="处置级别" width="130" align="left" header-align="left">
          <template #default="{ row }">
            <el-tag :type="getLevelType(row.level)">{{ getLevelLabel(row.level) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="matchType" label="匹配方式" width="110" align="left" header-align="left">
          <template #default="{ row }">{{ row.matchType === 'EXACT' ? '完全匹配' : '包含匹配' }}</template>
        </el-table-column>
        <el-table-column prop="replacement" label="替换为" width="100" align="left" header-align="left" />
        <el-table-column prop="status" label="状态" width="90" align="left" header-align="left">
          <template #default="{ row }">
            <el-switch :model-value="row.status === 1" @change="value => toggleStatus(row, value)" />
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="180" align="left" header-align="left" show-overflow-tooltip />
        <el-table-column prop="updateTime" label="更新时间" width="170" align="left" header-align="left">
          <template #default="{ row }">{{ formatDate(row.updateTime || row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right" align="left" header-align="left">
          <template #default="{ row }">
            <div class="row-actions">
              <el-button type="primary" text :icon="Edit" @click="openEdit(row)">编辑</el-button>
              <el-button type="danger" text :icon="Delete" @click="removeRule(row)">删除</el-button>
            </div>
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
          @size-change="loadData"
          @current-change="loadData"
        />
      </div>
    </section>

    <el-dialog v-model="dialogVisible" :title="dialogMode === 'create' ? '新增敏感词' : '编辑敏感词'" width="560px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="96px">
        <el-form-item label="敏感词" prop="word">
          <el-input v-model.trim="form.word" maxlength="120" placeholder="请输入敏感词或短语" />
        </el-form-item>
        <el-form-item label="分类" prop="category">
          <el-select v-model="form.category" placeholder="请选择分类" class="full-width">
            <el-option v-for="item in categoryOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="处置级别" prop="level">
          <el-radio-group v-model="form.level">
            <el-radio-button label="BLOCK">拦截</el-radio-button>
            <el-radio-button label="REVIEW">脱敏审核</el-radio-button>
            <el-radio-button label="REPLACE">仅脱敏</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="匹配方式" prop="matchType">
          <el-radio-group v-model="form.matchType">
            <el-radio-button label="CONTAINS">包含匹配</el-radio-button>
            <el-radio-button label="EXACT">完全匹配</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="替换文本" prop="replacement">
          <el-input v-model.trim="form.replacement" maxlength="40" placeholder="默认 ***" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" active-text="启用" inactive-text="停用" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model.trim="form.remark" type="textarea" :rows="3" maxlength="255" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitForm">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="importVisible" title="批量导入敏感词" width="620px">
      <el-form label-width="96px">
        <el-form-item label="默认分类">
          <el-select v-model="importForm.category" class="full-width">
            <el-option v-for="item in categoryOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="处置级别">
          <el-radio-group v-model="importForm.level">
            <el-radio-button label="BLOCK">拦截</el-radio-button>
            <el-radio-button label="REVIEW">脱敏审核</el-radio-button>
            <el-radio-button label="REPLACE">仅脱敏</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="敏感词">
          <el-upload class="text-upload" accept=".txt,text/plain" :show-file-list="false" :before-upload="beforeTextImport">
            <el-button :icon="Upload">选择 TXT 文本</el-button>
          </el-upload>
          <el-input
            v-model="importForm.words"
            type="textarea"
            :rows="10"
            placeholder="可选择 TXT 文本导入，也可以直接粘贴；一行一个敏感词，重复项会自动跳过"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="importVisible = false">取消</el-button>
        <el-button type="primary" :loading="importing" @click="submitImport">导入</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { CircleCheck, Delete, Edit, Plus, Refresh, Search, Upload } from '@element-plus/icons-vue'
import request from '@/utils/request'
import { formatDate } from '@/utils/dateUtils'

const categoryOptions = [
  { label: '涉黄', value: 'PORN' },
  { label: '赌博', value: 'GAMBLING' },
  { label: '涉毒', value: 'DRUG' },
  { label: '武器危险品', value: 'WEAPON' },
  { label: '欺诈黑产', value: 'FRAUD' },
  { label: '站外引流', value: 'CONTACT' },
  { label: '政治谣言', value: 'POLITICAL' },
  { label: '非法组织', value: 'ILLEGAL' },
  { label: '辱骂攻击', value: 'ABUSE' },
  { label: '暴力威胁', value: 'VIOLENCE' },
  { label: '旅游风险', value: 'TRAVEL_RISK' },
  { label: '其他', value: 'OTHER' }
]

const levelOptions = [
  { label: '拦截', value: 'BLOCK' },
  { label: '脱敏审核', value: 'REVIEW' },
  { label: '仅脱敏', value: 'REPLACE' }
]

const query = reactive({
  keyword: '',
  category: '',
  level: '',
  status: '',
  currentPage: 1,
  size: 10
})

const tableData = ref([])
const selectedRows = ref([])
const total = ref(0)
const loading = ref(false)
const dialogVisible = ref(false)
const importVisible = ref(false)
const dialogMode = ref('create')
const submitting = ref(false)
const importing = ref(false)
const formRef = ref(null)
const testContent = ref('')
const checking = ref(false)
const checkResult = ref(null)

const form = reactive({
  id: null,
  word: '',
  category: 'TRAVEL_RISK',
  level: 'REVIEW',
  matchType: 'CONTAINS',
  replacement: '***',
  status: 1,
  remark: ''
})

const importForm = reactive({
  words: '',
  category: 'TRAVEL_RISK',
  level: 'REVIEW'
})

const rules = {
  word: [
    { required: true, message: '请输入敏感词', trigger: 'blur' },
    { min: 1, max: 120, message: '长度为 1 到 120 个字符', trigger: 'blur' }
  ],
  category: [{ required: true, message: '请选择分类', trigger: 'change' }],
  level: [{ required: true, message: '请选择处置级别', trigger: 'change' }],
  matchType: [{ required: true, message: '请选择匹配方式', trigger: 'change' }]
}

const loadData = async () => {
  loading.value = true
  try {
    const params = { ...query }
    if (params.status === '') delete params.status
    const res = await request.get('/sensitive-word/page', params, { showDefaultMsg: false })
    tableData.value = res.records || []
    total.value = res.total || 0
    selectedRows.value = []
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  query.currentPage = 1
  loadData()
}

const resetSearch = () => {
  Object.assign(query, {
    keyword: '',
    category: '',
    level: '',
    status: '',
    currentPage: 1,
    size: 10
  })
  loadData()
}

const resetForm = () => {
  Object.assign(form, {
    id: null,
    word: '',
    category: 'TRAVEL_RISK',
    level: 'REVIEW',
    matchType: 'CONTAINS',
    replacement: '***',
    status: 1,
    remark: ''
  })
  formRef.value?.clearValidate()
}

const openCreate = () => {
  dialogMode.value = 'create'
  resetForm()
  dialogVisible.value = true
}

const openEdit = (row) => {
  dialogMode.value = 'edit'
  Object.assign(form, {
    ...row,
    matchType: row.matchType || 'CONTAINS',
    replacement: row.replacement || '***',
    status: row.status ?? 1
  })
  dialogVisible.value = true
}

const openImport = () => {
  importForm.words = ''
  importForm.category = 'TRAVEL_RISK'
  importForm.level = 'REVIEW'
  importVisible.value = true
}

const submitForm = async () => {
  await formRef.value?.validate()
  submitting.value = true
  try {
    if (dialogMode.value === 'create') {
      await request.post('/sensitive-word', form, { successMsg: '敏感词已新增' })
    } else {
      await request.put(`/sensitive-word/${form.id}`, form, { successMsg: '敏感词已更新' })
    }
    dialogVisible.value = false
    loadData()
  } finally {
    submitting.value = false
  }
}

const submitImport = async () => {
  const words = importForm.words
    .split(/\r?\n/)
    .map(item => item.trim())
    .filter(Boolean)
  const uniqueWords = Array.from(new Set(words))
  if (!uniqueWords.length) {
    ElMessage.warning('请先粘贴需要导入的敏感词')
    return
  }
  importing.value = true
  try {
    const payload = uniqueWords.map(word => ({
      word,
      category: importForm.category,
      level: importForm.level,
      matchType: 'CONTAINS',
      replacement: '***',
      status: 1
    }))
    await request.post('/sensitive-word/batch', payload, { successMsg: '敏感词已导入' })
    importVisible.value = false
    loadData()
  } finally {
    importing.value = false
  }
}

const beforeTextImport = (file) => {
  const reader = new FileReader()
  reader.onload = () => {
    const text = String(reader.result || '')
    importForm.words = importForm.words ? `${importForm.words.trim()}\n${text.trim()}` : text.trim()
  }
  reader.readAsText(file, 'UTF-8')
  return false
}

const toggleStatus = async (row, value) => {
  const oldStatus = row.status
  row.status = value ? 1 : 0
  try {
    await request.put(`/sensitive-word/${row.id}/status`, { status: row.status }, { successMsg: row.status === 1 ? '已启用' : '已停用' })
  } catch (error) {
    row.status = oldStatus
  }
}

const removeRule = async (row) => {
  try {
    await ElMessageBox.confirm(`确定删除敏感词「${row.word}」吗？`, '删除确认', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
    await request.delete(`/sensitive-word/${row.id}`, { successMsg: '敏感词已删除' })
    loadData()
  } catch (error) {
    // 取消删除时保持列表状态
  }
}

const removeSelected = async () => {
  if (!selectedRows.value.length) return
  try {
    await ElMessageBox.confirm(`确定删除已选的 ${selectedRows.value.length} 条敏感词吗？`, '批量删除确认', {
      type: 'warning',
      confirmButtonText: '批量删除',
      cancelButtonText: '取消'
    })
    const ids = selectedRows.value.map(item => item.id)
    await request.delete('/sensitive-word/batch', {
      data: ids,
      successMsg: '已批量删除敏感词'
    })
    loadData()
  } catch (error) {
    // 取消删除时保持列表状态
  }
}

const checkContent = async () => {
  if (!testContent.value.trim()) {
    ElMessage.warning('请先输入需要检测的内容')
    return
  }
  checking.value = true
  try {
    checkResult.value = await request.post('/sensitive-word/check', { content: testContent.value }, { showDefaultMsg: false })
  } finally {
    checking.value = false
  }
}

const getCategoryLabel = (value) => categoryOptions.find(item => item.value === value)?.label || value || '-'
const getLevelLabel = (value) => levelOptions.find(item => item.value === value)?.label || value || '-'
const getLevelType = (value) => {
  if (value === 'BLOCK') return 'danger'
  if (value === 'REVIEW') return 'warning'
  return 'success'
}

onMounted(loadData)
</script>

<style lang="scss" scoped>
.sensitive-page {
  padding: 20px;
  min-height: calc(100vh - 120px);
  background: #f6f8fb;
}

.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
  text-align: left;

  .header-text {
    min-width: 0;
  }

  h1 {
    margin: 0 0 6px;
    font-size: 24px;
    font-weight: 600;
    color: #1f2937;
  }

  p {
    margin: 0;
    color: #64748b;
    font-size: 14px;
  }
}

.header-actions {
  display: flex;
  gap: 10px;
  flex-shrink: 0;
}

.test-panel,
.table-panel,
.filter-bar {
  background: #ffffff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
}

.test-panel {
  padding: 16px;
  margin-bottom: 16px;
}

.panel-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;

  h2 {
    margin: 0;
    font-size: 16px;
    color: #1f2937;
  }
}

.test-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(320px, 0.75fr);
  gap: 16px;
  align-items: start;
}

.check-result {
  display: grid;
  gap: 12px;
}

.hit-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.filtered-content {
  min-height: 72px;
  padding: 12px;
  color: #334155;
  line-height: 1.7;
  white-space: pre-wrap;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
}

.filter-bar {
  display: grid;
  grid-template-columns: 200px 150px 150px 130px auto;
  gap: 12px;
  align-items: center;
  padding: 16px;
  margin-bottom: 16px;
}

.keyword-input {
  max-width: 200px;
}

.filter-actions {
  display: flex;
  gap: 12px;
  justify-content: flex-start;
  margin-left: 0;
}

.table-panel {
  padding: 16px;
}

.table-toolbar {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;
  margin-bottom: 12px;
  color: #64748b;
  font-size: 14px;
}

.word-text {
  display: block;
  max-width: 100%;
  padding-left: 12px;
  font-weight: 600;
  color: #111827;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

:deep(.word-header .cell) {
  padding-left: 24px;
}

:deep(.word-column .cell) {
  padding-left: 12px;
  padding-right: 14px;
}

.row-actions {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  white-space: nowrap;

  :deep(.el-button + .el-button) {
    margin-left: 0;
  }
}

.pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.full-width {
  width: 100%;
}

.text-upload {
  margin-bottom: 10px;
}

@media (max-width: 1180px) {
  .test-layout {
    grid-template-columns: 1fr;
  }

  .filter-bar {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .keyword-input {
    max-width: none;
  }

  .filter-actions {
    margin-left: 0;
  }
}
</style>
