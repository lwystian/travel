<template>
  <el-dialog
    :title="`出发班期管理 - ${tourTitle}`"
    v-model="dialogVisible"
    width="900px"
    :close-on-click-modal="false"
    :close-on-press-escape="false"
  >
    <!-- 工具栏 -->
    <div class="toolbar">
      <div class="toolbar-left">
        <el-button type="primary" @click="showAddBatchDialog">
          <el-icon><Plus /></el-icon> 添加班期
        </el-button>
        <el-button type="success" @click="showBatchAddDialog">
          <el-icon><Plus /></el-icon> 批量添加
        </el-button>
      </div>
      <div class="toolbar-right">
        <span class="tip">提示：余位为0或过去日期的班期将不显示在前台</span>
      </div>
    </div>

    <!-- 班期列表 -->
    <div class="batch-section">
      <div v-loading="loading" class="batch-table-wrap">
        <table class="batch-table">
          <colgroup>
            <col style="width: 8%;">
            <col style="width: 17%;">
            <col style="width: 15%;">
            <col style="width: 15%;">
            <col style="width: 17%;">
            <col style="width: 12%;">
            <col style="width: 16%;">
          </colgroup>
          <thead>
            <tr>
              <th>ID</th>
              <th>出发日期</th>
              <th>成人附加费</th>
              <th>儿童附加费</th>
              <th>余位/锁定/容量</th>
              <th>状态</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in batches" :key="row.id">
              <td>{{ row.id }}</td>
              <td><span :class="{ 'expired': isExpired(row.departureDate) }">{{ row.departureDate }}</span></td>
              <td><span class="price">{{ row.adultDateExtraFee > 0 ? '+¥' + row.adultDateExtraFee : '-' }}</span></td>
              <td><span class="price">{{ row.childDateExtraFee > 0 ? '+¥' + row.childDateExtraFee : '-' }}</span></td>
              <td>
                <span :class="{ 'warning': getAvailableSeats(row) <= 5 }">
                  {{ getAvailableSeats(row) }}/{{ row.occupied || 0 }}/{{ row.maxCapacity || 0 }}
                </span>
              </td>
              <td><el-tag :type="getStatusType(row.status)" size="small">{{ row.status }}</el-tag></td>
              <td>
                <div class="table-actions">
                  <el-button type="primary" size="small" @click="handleEditBatch(row)">编辑</el-button>
                  <el-button type="warning" size="small" @click="handleUpdateRemaining(row)">余位</el-button>
                  <el-button type="danger" size="small" @click="handleDeleteBatch(row)">删除</el-button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <el-empty v-if="batches.length === 0 && !loading" description="暂无班期，请添加" :image-size="80" />
    </div>

    <!-- 添加/编辑班期对话框 -->
    <el-dialog
      :title="isBatchEdit ? '编辑班期' : '添加班期'"
      v-model="batchDialogVisible"
      width="450px"
      append-to-body
      :close-on-click-modal="false"
      :close-on-press-escape="false"
      :before-close="batchDialogGuard.beforeClose"
    >
      <el-form ref="batchFormRef" :model="batchForm" :rules="batchRules" label-width="100px">
        <el-form-item label="出发日期" prop="departureDate">
          <el-date-picker
            v-model="batchForm.departureDate"
            type="date"
            placeholder="选择日期"
            format="YYYY-MM-DD"
            value-format="YYYY-MM-DD"
            :disabled-date="disabledDate"
            style="width: 100%;"
          />
        </el-form-item>
        <el-form-item label="成人附加费" prop="adultDateExtraFee">
          <el-input-number v-model="batchForm.adultDateExtraFee" :precision="2" :min="0" :step="10" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="儿童附加费" prop="childDateExtraFee">
          <el-input-number v-model="batchForm.childDateExtraFee" :precision="2" :min="0" :step="10" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select v-model="batchForm.status" placeholder="请选择状态" style="width: 100%;">
            <el-option label="可报名" value="可报名" />
            <el-option label="已满员" value="已满员" />
            <el-option label="已结束" value="已结束" />
          </el-select>
        </el-form-item>
        <el-form-item label="余位" prop="remaining">
          <el-input-number v-model="batchForm.remaining" :min="batchForm.occupied || 0" :max="batchForm.maxCapacity || 999" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="最大容量" prop="maxCapacity">
          <el-input-number v-model="batchForm.maxCapacity" :min="Math.max(1, batchForm.remaining || 0)" :max="999" style="width: 100%;" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="batchDialogGuard.requestClose">取消</el-button>
        <el-button type="primary" @click="submitBatch" :loading="batchLoading">确定</el-button>
      </template>
    </el-dialog>

    <!-- 批量添加班期对话框 -->
    <el-dialog
      title="批量添加班期"
      v-model="batchAddDialogVisible"
      width="500px"
      append-to-body
      :close-on-click-modal="false"
      :close-on-press-escape="false"
      :before-close="batchAddDialogGuard.beforeClose"
    >
      <el-form ref="batchAddFormRef" :model="batchAddForm" label-width="100px">
        <el-form-item label="出发日期">
          <el-date-picker
            v-model="batchAddForm.dates"
            type="dates"
            placeholder="可一次选择多个出发日期"
            format="YYYY-MM-DD"
            value-format="YYYY-MM-DD"
            :disabled-date="disabledDate"
            style="width: 100%;"
          />
        </el-form-item>
        <el-form-item label="成人附加费">
          <el-input-number v-model="batchAddForm.adultDateExtraFee" :precision="2" :min="0" :step="10" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="儿童附加费">
          <el-input-number v-model="batchAddForm.childDateExtraFee" :precision="2" :min="0" :step="10" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="余位">
          <el-input-number v-model="batchAddForm.remaining" :min="0" :max="batchAddForm.maxCapacity || 999" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="最大容量">
          <el-input-number v-model="batchAddForm.maxCapacity" :min="Math.max(1, batchAddForm.remaining || 0)" :max="999" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="batchAddForm.status" placeholder="请选择状态" style="width: 100%;">
            <el-option label="可报名" value="可报名" />
            <el-option label="已满员" value="已满员" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="batchAddDialogGuard.requestClose">取消</el-button>
        <el-button type="primary" @click="submitBatchAdd" :loading="batchAddLoading">确定</el-button>
      </template>
    </el-dialog>

    <!-- 修改余位对话框 -->
    <el-dialog
      title="修改余位"
      v-model="remainingDialogVisible"
      width="350px"
      append-to-body
      :close-on-click-modal="false"
      :close-on-press-escape="false"
      :before-close="remainingDialogGuard.beforeClose"
    >
      <el-form label-width="80px">
        <el-form-item label="班期">
          <span>{{ currentBatch?.departureDate }}</span>
        </el-form-item>
        <el-form-item label="当前余位">
          <span>{{ currentBatch?.remaining }}</span>
        </el-form-item>
        <el-form-item label="锁定名额">
          <span>{{ currentBatch?.occupied || 0 }}</span>
        </el-form-item>
        <el-form-item label="最大容量">
          <span>{{ currentBatch?.maxCapacity || 0 }}</span>
        </el-form-item>
        <el-form-item label="新余位">
          <el-input-number v-model="newRemaining" :min="currentBatch?.occupied || 0" :max="currentBatch?.maxCapacity || 999" style="width: 100%;" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="remainingDialogGuard.requestClose">取消</el-button>
        <el-button type="primary" @click="submitRemaining">确定</el-button>
      </template>
    </el-dialog>
  </el-dialog>
</template>

<script setup>
import { ref, watch, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import {
  getTourBatches,
  addTourBatch,
  addTourBatchesBatch,
  updateTourBatch,
  deleteTourBatch
} from '@/api/tourDetail'
import { createUnsavedDialogGuard } from '@/utils/unsavedDialogGuard'

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false
  },
  tourId: {
    type: [Number, String],
    default: null
  },
  tourTitle: {
    type: String,
    default: ''
  }
})

const emit = defineEmits(['update:modelValue'])

const dialogVisible = ref(false)
const loading = ref(false)
const batches = ref([])

// 班期表单
const batchDialogVisible = ref(false)
const isBatchEdit = ref(false)
const batchLoading = ref(false)
const batchFormRef = ref(null)
const batchForm = ref({
  id: null,
  departureDate: '',
  adultDateExtraFee: 0,
  childDateExtraFee: 0,
  status: '可报名',
  remaining: 30,
  maxCapacity: 50
})
const batchDialogGuard = createUnsavedDialogGuard(() => batchForm.value, batchDialogVisible)

const batchRules = {
  departureDate: [{ required: true, message: '请选择出发日期', trigger: 'change' }]
}

// 批量添加表单
const batchAddDialogVisible = ref(false)
const batchAddLoading = ref(false)
const batchAddFormRef = ref(null)
const batchAddForm = ref({
  dates: [],
  adultDateExtraFee: 0,
  childDateExtraFee: 0,
  remaining: 30,
  maxCapacity: 50,
  status: '可报名'
})
const batchAddDialogGuard = createUnsavedDialogGuard(() => batchAddForm.value, batchAddDialogVisible)

// 余位修改
const remainingDialogVisible = ref(false)
const currentBatch = ref(null)
const newRemaining = ref(0)
const remainingDialogGuard = createUnsavedDialogGuard(() => ({
  batchId: currentBatch.value?.id || null,
  newRemaining: newRemaining.value
}), remainingDialogVisible)

watch(() => props.modelValue, (val) => {
  dialogVisible.value = val
  if (val && props.tourId) {
    fetchBatches()
  }
})

watch(dialogVisible, (val) => {
  emit('update:modelValue', val)
})

const fetchBatches = async () => {
  if (!props.tourId) return
  loading.value = true
  try {
    const res = await getTourBatches(props.tourId)
    batches.value = res || []
  } catch (error) {
    console.error('获取班期列表失败:', error)
  } finally {
    loading.value = false
  }
}

const isExpired = (dateStr) => {
  if (!dateStr) return false
  return new Date(dateStr) < new Date(new Date().toDateString())
}

const getAvailableSeats = (batch) => {
  return Math.max(0, (batch?.remaining || 0) - (batch?.occupied || 0))
}

const normalizeBatchCapacity = (batch) => {
  const occupied = Number(batch.occupied || 0)
  const remaining = Number(batch.remaining || 0)
  const maxCapacity = Number(batch.maxCapacity || 0)
  return {
    ...batch,
    occupied,
    remaining: Math.max(occupied, remaining),
    maxCapacity: Math.max(1, remaining, maxCapacity)
  }
}

const validateBatchCapacity = (batch) => {
  const occupied = Number(batch.occupied || 0)
  const remaining = Number(batch.remaining || 0)
  const maxCapacity = Number(batch.maxCapacity || 0)
  if (remaining < occupied) {
    ElMessage.warning(`余位不能小于已锁定名额（当前锁定 ${occupied}）`)
    return false
  }
  if (remaining > maxCapacity) {
    ElMessage.warning('余位不能大于最大容量')
    return false
  }
  return true
}

const getStatusType = (status) => {
  switch (status) {
    case '可报名': return 'success'
    case '已满员': return 'warning'
    case '已结束': return 'info'
    default: return 'info'
  }
}

const disabledDate = (date) => {
  return date < new Date(new Date().toDateString())
}

const showAddBatchDialog = () => {
  isBatchEdit.value = false
  batchForm.value = {
    id: null,
    departureDate: '',
    adultDateExtraFee: 0,
    childDateExtraFee: 0,
    status: '可报名',
    remaining: 30,
    maxCapacity: 50
  }
  batchDialogVisible.value = true
  nextTick(batchDialogGuard.markPristine)
}

const handleEditBatch = (row) => {
  isBatchEdit.value = true
  batchForm.value = normalizeBatchCapacity(row)
  batchDialogVisible.value = true
  nextTick(batchDialogGuard.markPristine)
}

const handleDeleteBatch = (row) => {
  ElMessageBox.confirm(`确定要删除 ${row.departureDate} 的班期吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await deleteTourBatch(row.id)
      ElMessage.success('删除成功')
      fetchBatches()
    } catch (error) {
      console.error('删除失败:', error)
    }
  }).catch(() => {})
}

const submitBatch = async () => {
  batchFormRef.value.validate(async (valid) => {
    if (!valid) return
    const data = normalizeBatchCapacity({
      ...batchForm.value,
      tourId: props.tourId
    })
    if (!validateBatchCapacity(data)) return
    batchLoading.value = true
    try {
      if (isBatchEdit.value) {
        await updateTourBatch(batchForm.value.id, data)
        ElMessage.success('更新成功')
      } else {
        await addTourBatch(data)
        ElMessage.success('添加成功')
      }
      batchDialogGuard.closeAfterSave()
      fetchBatches()
    } catch (error) {
      console.error('操作失败:', error)
    } finally {
      batchLoading.value = false
    }
  })
}

const showBatchAddDialog = () => {
  batchAddForm.value = {
    dates: [],
    adultDateExtraFee: 0,
    childDateExtraFee: 0,
    remaining: 30,
    maxCapacity: 50,
    status: '可报名'
  }
  batchAddDialogVisible.value = true
  nextTick(batchAddDialogGuard.markPristine)
}

const submitBatchAdd = async () => {
  const dates = [...new Set(batchAddForm.value.dates || [])].sort()
  if (!dates.length) {
    ElMessage.warning('请选择出发日期')
    return
  }
  const normalizedForm = normalizeBatchCapacity(batchAddForm.value)
  if (!validateBatchCapacity(normalizedForm)) return

  batchAddLoading.value = true
  try {
    // 构建班期列表
    const batchList = dates.map(date => ({
      tourId: props.tourId,
      departureDate: date,
      adultDateExtraFee: normalizedForm.adultDateExtraFee,
      childDateExtraFee: normalizedForm.childDateExtraFee,
      status: normalizedForm.status,
      remaining: normalizedForm.remaining,
      maxCapacity: normalizedForm.maxCapacity
    }))

    await addTourBatchesBatch(batchList)
    ElMessage.success(`成功添加 ${batchList.length} 个班期`)
    batchAddDialogGuard.closeAfterSave()
    fetchBatches()
  } catch (error) {
    console.error('批量添加失败:', error)
  } finally {
    batchAddLoading.value = false
  }
}

const handleUpdateRemaining = (row) => {
  currentBatch.value = row
  newRemaining.value = Math.max(row.remaining || 0, row.occupied || 0)
  remainingDialogVisible.value = true
  nextTick(remainingDialogGuard.markPristine)
}

const submitRemaining = async () => {
  if (!currentBatch.value) return
  const data = normalizeBatchCapacity({
    ...currentBatch.value,
    remaining: newRemaining.value,
    tourId: props.tourId
  })
  if (!validateBatchCapacity(data)) return
  try {
    await updateTourBatch(currentBatch.value.id, {
      ...data
    })
    ElMessage.success('余位更新成功')
    remainingDialogGuard.closeAfterSave()
    fetchBatches()
  } catch (error) {
    console.error('更新余位失败:', error)
  }
}
</script>

<style scoped>
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
  padding-bottom: 15px;
  border-bottom: 1px solid #eee;
}

.toolbar-left {
  display: flex;
  gap: 10px;
}

.toolbar-right .tip {
  font-size: 12px;
  color: #909399;
}

.batch-section {
  max-height: 500px;
  overflow-y: auto;
}

.batch-table-wrap {
  width: 100%;
}

.batch-table {
  width: 100%;
  table-layout: fixed;
  border-collapse: collapse;
  border: 1px solid #ebeef5;
  font-size: 14px;
}

.batch-table th,
.batch-table td {
  height: 40px;
  padding: 6px 8px;
  text-align: center;
  vertical-align: middle;
  border-right: 1px solid #ebeef5;
  border-bottom: 1px solid #ebeef5;
  box-sizing: border-box;
}

.batch-table th {
  color: #909399;
  font-weight: 600;
  background: #f8fafc;
}

.batch-table td {
  color: #606266;
  background: #fff;
  word-break: break-word;
}

.table-actions {
  display: flex;
  justify-content: center;
  gap: 6px;
}

.table-actions :deep(.el-button + .el-button) {
  margin-left: 0;
}

.capacity-tip {
  margin: -4px 0 8px 100px;
  color: #909399;
  font-size: 12px;
  line-height: 1.5;
}

.price {
  color: #f56c6c;
  font-weight: 600;
}

.warning {
  color: #e6a23c;
  font-weight: 600;
}

.expired {
  color: #c0c4cc;
}
</style>
