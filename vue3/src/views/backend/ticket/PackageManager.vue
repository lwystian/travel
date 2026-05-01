<template>
  <el-dialog
    :title="`行程套餐管理 - ${tourTitle}`"
    v-model="dialogVisible"
    width="800px"
    :close-on-click-modal="false"
  >
    <!-- 套餐列表 -->
    <div class="package-section">
      <div class="section-header">
        <span class="section-title">行程套餐</span>
        <el-button type="primary" size="small" @click="showAddPackageDialog">
          <el-icon><Plus /></el-icon> 添加套餐
        </el-button>
      </div>

      <el-table :data="packages" border style="width: 100%" v-loading="loading">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="name" label="套餐名称" min-width="150" />
        <el-table-column label="成人价" width="100">
          <template #default="scope">
            <span class="price">¥{{ scope.row.adultPrice }}</span>
          </template>
        </el-table-column>
        <el-table-column label="儿童价" width="100">
          <template #default="scope">
            <span class="price">{{ scope.row.childPrice > 0 ? '¥' + scope.row.childPrice : '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="描述" min-width="150" show-overflow-tooltip />
        <el-table-column label="状态" width="80">
          <template #default="scope">
            <el-tag :type="scope.row.status === 1 ? 'success' : 'info'" size="small">
              {{ scope.row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="scope">
            <el-button type="primary" size="small" @click="handleEditPackage(scope.row)">编辑</el-button>
            <el-button type="danger" size="small" @click="handleDeletePackage(scope.row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="packages.length === 0 && !loading" description="暂无套餐，请添加" :image-size="80" />
    </div>

    <!-- 添加/编辑套餐对话框 -->
    <el-dialog
      :title="isPackageEdit ? '编辑套餐' : '添加套餐'"
      v-model="packageDialogVisible"
      width="500px"
      append-to-body
      :close-on-click-modal="false"
    >
      <el-form ref="packageFormRef" :model="packageForm" :rules="packageRules" label-width="90px">
        <el-form-item label="套餐名称" prop="name">
          <el-input v-model="packageForm.name" placeholder="如：标准套餐、VIP套餐" />
        </el-form-item>
        <el-form-item label="成人价格" prop="adultPrice">
          <el-input-number v-model="packageForm.adultPrice" :precision="2" :min="0" :step="10" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="儿童价格" prop="childPrice">
          <el-input-number v-model="packageForm.childPrice" :precision="2" :min="0" :step="10" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="packageForm.description" type="textarea" :rows="2" placeholder="套餐描述" />
        </el-form-item>
        <el-form-item label="排序" prop="sortOrder">
          <el-input-number v-model="packageForm.sortOrder" :min="0" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-switch
            v-model="packageForm.status"
            :active-value="1"
            :inactive-value="0"
            active-text="启用"
            inactive-text="禁用"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="packageDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitPackage" :loading="packageLoading">确定</el-button>
      </template>
    </el-dialog>
  </el-dialog>
</template>

<script setup>
import { ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import {
  getTourPackages,
  addTourPackage,
  updateTourPackage,
  deleteTourPackage
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
const packages = ref([])

// 套餐表单
const packageDialogVisible = ref(false)
const isPackageEdit = ref(false)
const packageLoading = ref(false)
const packageFormRef = ref(null)
const packageForm = ref({
  id: null,
  name: '',
  adultPrice: 0,
  childPrice: 0,
  description: '',
  sortOrder: 0,
  status: 1
})

const packageRules = {
  name: [{ required: true, message: '请输入套餐名称', trigger: 'blur' }],
  adultPrice: [{ required: true, message: '请输入成人价格', trigger: 'blur' }]
}

watch(() => props.modelValue, (val) => {
  dialogVisible.value = val
  if (val && props.tourId) {
    fetchPackages()
  }
})

watch(dialogVisible, (val) => {
  emit('update:modelValue', val)
})

const fetchPackages = async () => {
  if (!props.tourId) return
  loading.value = true
  try {
    const res = await getTourPackages(props.tourId)
    packages.value = res || []
  } catch (error) {
    console.error('获取套餐列表失败:', error)
  } finally {
    loading.value = false
  }
}

const showAddPackageDialog = () => {
  isPackageEdit.value = false
  packageForm.value = {
    id: null,
    name: '',
    adultPrice: 0,
    childPrice: 0,
    description: '',
    sortOrder: packages.value.length,
    status: 1
  }
  packageDialogVisible.value = true
}

const handleEditPackage = (row) => {
  isPackageEdit.value = true
  packageForm.value = { ...row }
  packageDialogVisible.value = true
}

const handleDeletePackage = (row) => {
  ElMessageBox.confirm(`确定要删除套餐"${row.name}"吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await deleteTourPackage(row.id)
      ElMessage.success('删除成功')
      fetchPackages()
    } catch (error) {
      console.error('删除失败:', error)
    }
  }).catch(() => {})
}

const submitPackage = async () => {
  packageFormRef.value.validate(async (valid) => {
    if (!valid) return
    packageLoading.value = true
    try {
      const data = { ...packageForm.value, tourId: props.tourId }
      if (isPackageEdit.value) {
        await updateTourPackage(packageForm.value.id, data)
        ElMessage.success('更新成功')
      } else {
        await addTourPackage(data)
        ElMessage.success('添加成功')
      }
      packageDialogVisible.value = false
      fetchPackages()
    } catch (error) {
      console.error('操作失败:', error)
    } finally {
      packageLoading.value = false
    }
  })
}
</script>

<style scoped>
.package-section {
  margin-bottom: 20px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.price {
  color: #f56c6c;
  font-weight: 600;
}
</style>
