<template>
  <el-dialog
    :title="`出发班期管理 - ${tourTitle}`"
    v-model="dialogVisible"
    width="900px"
    :close-on-click-modal="false"
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
      <el-table :data="batches" border style="width: 100%" v-loading="loading">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column label="出发日期" width="120">
          <template #default="scope">
            <span :class="{ 'expired': isExpired(scope.row.departureDate) }">
              {{ scope.row.departureDate }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="成人附加费" width="110">
          <template #default="scope">
            <span class="price">
              {{ scope.row.adultDateExtraFee > 0 ? '+¥' + scope.row.adultDateExtraFee : '-' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="儿童附加费" width="110">
          <template #default="scope">
            <span class="price">
              {{ scope.row.childDateExtraFee > 0 ? '+¥' + scope.row.childDateExtraFee : '-' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="余位/容量" width="100">
          <template #default="scope">
            <span :class="{ 'warning': scope.row.remaining <= 5 }">
              {{ scope.row.remaining }}/{{ scope.row.maxCapacity }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="scope">
            <el-tag :type="getStatusType(scope.row.status)" size="small">
              {{ scope.row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="scope">
            <el-button type="primary" size="small" @click="handleEditBatch(scope.row)">编辑</el-button>
            <el-button type="warning" size="small" @click="handleUpdateRemaining(scope.row)">余位</el-button>
            <el-button type="danger" size="small" @click="handleDeleteBatch(scope.row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="batches.length === 0 && !loading" description="暂无班期，请添加" :image-size="80" />
    </div>

    <!-- 添加/编辑班期对话框 -->
    <el-dialog
      :title="isBatchEdit ? '编辑班期' : '添加班期'"
      v-model="batchDialogVisible"
      width="450px"
      append-to-body
      :close-on-click-modal="false"
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
          <el-input-number v-model="batchForm.remaining" :min="0" :max="999" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="最大容量" prop="maxCapacity">
          <el-input-number v-model="batchForm.maxCapacity" :min="1" :max="999" style="width: 100%;" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="batchDialogVisible = false">取消</el-button>
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
    >
      <el-form ref="batchAddFormRef" :model="batchAddForm" label-width="100px">
        <el-form-item label="开始日期">
          <el-date-picker
            v-model="batchAddForm.startDate"
            type="date"
            placeholder="选择开始日期"
            format="YYYY-MM-DD"
            value-format="YYYY-MM-DD"
            :disabled-date="disabledDate"
            style="width: 100%;"
          />
        </el-form-item>
        <el-form-item label="结束日期">
          <el-date-picker
            v-model="batchAddForm.endDate"
            type="date"
            placeholder="选择结束日期"
            format="YYYY-MM-DD"
            value-format="YYYY-MM-DD"
            :disabled-date="disabledDate"
            style="width: 100%;"
          />
        </el-form-item>
        <el-form-item label="重复周期">
          <el-select v-model="batchAddForm.frequency" placeholder="选择重复周期" style="width: 100%;">
            <el-option label="每天" :value="1" />
            <el-option label="每2天" :value="2" />
            <el-option label="每3天" :value="3" />
            <el-option label="每5天" :value="5" />
            <el-option label="每周" :value="7" />
            <el-option label="每2周" :value="14" />
          </el-select>
        </el-form-item>
        <el-form-item label="成人附加费">
          <el-input-number v-model="batchAddForm.adultDateExtraFee" :precision="2" :min="0" :step="10" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="儿童附加费">
          <el-input-number v-model="batchAddForm.childDateExtraFee" :precision="2" :min="0" :step="10" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="余位">
          <el-input-number v-model="batchAddForm.remaining" :min="0" :max="999" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="最大容量">
          <el-input-number v-model="batchAddForm.maxCapacity" :min="1" :max="999" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="batchAddForm.status" placeholder="请选择状态" style="width: 100%;">
            <el-option label="可报名" value="可报名" />
            <el-option label="已满员" value="已满员" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="batchAddDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitBatchAdd" :loading="batchAddLoading">确定</el-button>
      </template>
    </el-dialog>

    <!-- 修改余位对话框 -->
    <el-dialog
      title="修改余位"
      v-model="remainingDialogVisible"
      width="350px"
      append-to-body
    >
      <el-form label-width="80px">
        <el-form-item label="班期">
          <span>{{ currentBatch?.departureDate }}</span>
        </el-form-item>
        <el-form-item label="当前余位">
          <span>{{ currentBatch?.remaining }}</span>
        </el-form-item>
        <el-form-item label="新余位">
          <el-input-number v-model="newRemaining" :min="0" :max="999" style="width: 100%;" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="remainingDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitRemaining">确定</el-button>
      </template>
    </el-dialog>
  </el-dialog>
</template>

<script setup>
import { ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import {
  getTourBatches,
  addTourBatch,
  addTourBatchesBatch,
  updateTourBatch,
  deleteTourBatch
} from '@/api/tourDetail'

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

const batchRules = {
  departureDate: [{ required: true, message: '请选择出发日期', trigger: 'change' }]
}

// 批量添加表单
const batchAddDialogVisible = ref(false)
const batchAddLoading = ref(false)
const batchAddFormRef = ref(null)
const batchAddForm = ref({
  startDate: '',
  endDate: '',
  frequency: 7,
  adultDateExtraFee: 0,
  childDateExtraFee: 0,
  remaining: 30,
  maxCapacity: 50,
  status: '可报名'
})

// 余位修改
const remainingDialogVisible = ref(false)
const currentBatch = ref(null)
const newRemaining = ref(0)

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
}

const handleEditBatch = (row) => {
  isBatchEdit.value = true
  batchForm.value = { ...row }
  batchDialogVisible.value = true
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
    batchLoading.value = true
    try {
      const data = { ...batchForm.value, tourId: props.tourId }
      if (isBatchEdit.value) {
        await updateTourBatch(batchForm.value.id, data)
        ElMessage.success('更新成功')
      } else {
        await addTourBatch(data)
        ElMessage.success('添加成功')
      }
      batchDialogVisible.value = false
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
    startDate: '',
    endDate: '',
    frequency: 7,
    adultDateExtraFee: 0,
    childDateExtraFee: 0,
    remaining: 30,
    maxCapacity: 50,
    status: '可报名'
  }
  batchAddDialogVisible.value = true
}

const submitBatchAdd = async () => {
  if (!batchAddForm.value.startDate || !batchAddForm.value.endDate) {
    ElMessage.warning('请选择开始和结束日期')
    return
  }
  if (new Date(batchAddForm.value.startDate) > new Date(batchAddForm.value.endDate)) {
    ElMessage.warning('开始日期不能晚于结束日期')
    return
  }

  batchAddLoading.value = true
  try {
    // 生成日期列表
    const dates = []
    let current = new Date(batchAddForm.value.startDate)
    const end = new Date(batchAddForm.value.endDate)

    while (current <= end) {
      dates.push(current.toISOString().split('T')[0])
      current.setDate(current.getDate() + batchAddForm.value.frequency)
    }

    // 构建班期列表
    const batchList = dates.map(date => ({
      tourId: props.tourId,
      departureDate: date,
      adultDateExtraFee: batchAddForm.value.adultDateExtraFee,
      childDateExtraFee: batchAddForm.value.childDateExtraFee,
      status: batchAddForm.value.status,
      remaining: batchAddForm.value.remaining,
      maxCapacity: batchAddForm.value.maxCapacity
    }))

    await addTourBatchesBatch(batchList)
    ElMessage.success(`成功添加 ${batchList.length} 个班期`)
    batchAddDialogVisible.value = false
    fetchBatches()
  } catch (error) {
    console.error('批量添加失败:', error)
  } finally {
    batchAddLoading.value = false
  }
}

const handleUpdateRemaining = (row) => {
  currentBatch.value = row
  newRemaining.value = row.remaining
  remainingDialogVisible.value = true
}

const submitRemaining = async () => {
  if (!currentBatch.value) return
  try {
    await updateTourBatch(currentBatch.value.id, {
      ...currentBatch.value,
      remaining: newRemaining.value,
      tourId: props.tourId
    })
    ElMessage.success('余位更新成功')
    remainingDialogVisible.value = false
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
