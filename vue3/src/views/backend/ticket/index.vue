<template>
  <div class="tour-management">
    <div class="page-header">
      <h1 class="page-title">行程管理</h1>
      <p class="page-subtitle">Tour Product Management</p>
    </div>

    <div class="action-bar">
      <div class="action-right">
        <el-button type="primary" @click="showAddDialog" class="add-btn">
          <el-icon><Plus /></el-icon> 添加行程
        </el-button>
      </div>
    </div>

    <!-- 搜索表单 -->
    <el-card shadow="never" class="search-card">
      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="行程名称" class="search-item">
          <el-input v-model="searchForm.title" placeholder="请输入" clearable style="width: 140px;"></el-input>
        </el-form-item>
        <el-form-item label="行程类型" class="search-item">
          <el-select v-model="searchForm.tourType" placeholder="请选择" clearable style="width: 110px;">
            <el-option label="周边游" value="around"></el-option>
            <el-option label="长线游" value="long"></el-option>
            <el-option label="跟团游" value="team"></el-option>
            <el-option label="邮轮出行" value="cruise"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="出发城市" class="search-item">
          <el-select v-model="searchForm.city" placeholder="请选择" clearable filterable style="width: 110px;">
            <el-option v-for="(name, code) in cityMap" :key="code" :label="name" :value="code"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="目的地" class="search-item">
          <el-select v-model="searchForm.destination" placeholder="请选择" clearable filterable style="width: 110px;">
            <el-option v-for="(name, code) in destinationMap" :key="code" :label="name" :value="code"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="主题" class="search-item">
          <el-select v-model="searchForm.theme" placeholder="请选择" clearable style="width: 110px;">
            <el-option label="风景游" value="scenic"></el-option>
            <el-option label="文化游" value="cultural"></el-option>
            <el-option label="探险游" value="adventure"></el-option>
            <el-option label="徒步游" value="hiking"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item class="search-buttons">
          <el-button type="primary" @click="handleSearch">
            <el-icon><Search /></el-icon> 查询
          </el-button>
          <el-button @click="resetSearch">
            <el-icon><Refresh /></el-icon> 重置
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 行程列表表格 -->
    <el-card shadow="never" class="table-card">
      <el-table :data="tourList" border style="width: 100%" v-loading="loading">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column label="行程封面" width="120">
          <template #default="scope">
            <el-image
              v-if="scope.row.mainImage"
              :src="scope.row.mainImage"
              fit="cover"
              style="width: 80px; height: 60px; border-radius: 4px;"
              :preview-src-list="[scope.row.mainImage]"
            />
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="行程名称" min-width="200">
          <template #default="scope">
            <div class="tour-title">
              <span class="title-text">{{ scope.row.title }}</span>
              <el-tag v-if="scope.row.tourType" size="small" type="primary">{{ getTourTypeLabel(scope.row.tourType) }}</el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="subtitle" label="副标题" min-width="120" show-overflow-tooltip />
        <el-table-column label="出发地-目的地" width="140">
          <template #default="scope">
            <span>{{ getCityLabel(scope.row.city) }}</span>
            <span v-if="scope.row.destination"> → {{ getDestinationLabel(scope.row.destination) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="行程天数" width="90">
          <template #default="scope">
            {{ scope.row.days }}天{{ scope.row.days > 1 ? scope.row.days - 1 + '晚' : '' }}
          </template>
        </el-table-column>
        <el-table-column label="最低价" width="100">
          <template #default="scope">
            <span class="price">¥{{ scope.row.minPrice }}</span>
          </template>
        </el-table-column>
        <el-table-column label="评分" width="80">
          <template #default="scope">
            <el-rate v-model="scope.row.starRating" disabled text-color="#ff9900" />
          </template>
        </el-table-column>
        <el-table-column label="标签" width="150">
          <template #default="scope">
            <el-tag v-for="tag in parseTags(scope.row.tags)" :key="tag" size="small" style="margin-right: 4px; margin-bottom: 2px;">{{ tag }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="80">
          <template #default="scope">
            <el-tag :type="scope.row.status === 1 ? 'success' : 'info'">
              {{ scope.row.status === 1 ? '上架' : '下架' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="scope">
            <el-dropdown trigger="click" @command="(cmd) => handleAction(cmd, scope.row)">
              <el-button type="primary" size="small">
                操作<el-icon class="el-icon--right"><ArrowDown /></el-icon>
              </el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="edit">
                    <el-icon><Edit /></el-icon> 编辑
                  </el-dropdown-item>
                  <el-dropdown-item command="detail">
                    <el-icon><List /></el-icon> 预订详情
                  </el-dropdown-item>
                  <el-dropdown-item :command="scope.row.status === 1 ? 'off' : 'on'">
                    <el-icon><Switch /></el-icon> {{ scope.row.status === 1 ? '下架' : '上架' }}
                  </el-dropdown-item>
                  <el-dropdown-item command="delete" divided style="color: #F56C6C;">
                    <el-icon><Delete /></el-icon> 删除
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-container">
        <el-pagination
          background
          layout="total, sizes, prev, pager, next, jumper"
          :total="total"
          :page-size="pageSize"
          :page-sizes="[10, 20, 50]"
          :current-page="currentPage"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>

    <!-- 添加/编辑行程对话框 -->
    <el-dialog :title="isEdit ? '编辑行程' : '添加行程'" v-model="dialogVisible" width="75%" class="tour-dialog">
      <el-form ref="tourFormRef" :model="tourForm" :rules="tourRules" label-width="100px">
        <el-row :gutter="20">
          <el-col :span="24">
            <el-form-item label="行程名称" prop="title">
              <el-input v-model="tourForm.title" placeholder="请输入行程名称" maxlength="100" show-word-limit />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="副标题" prop="subtitle">
              <el-input v-model="tourForm.subtitle" placeholder="请输入副标题" maxlength="50" show-word-limit />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="行程封面" prop="mainImage">
              <el-upload
                class="image-uploader"
                :show-file-list="false"
                :http-request="handleImageUpload"
                :before-upload="beforeImageUpload"
                accept="image/*"
              >
                <img v-if="tourForm.mainImage" :src="tourForm.mainImage" class="uploaded-image" />
                <el-icon v-else class="image-uploader-icon"><Plus /></el-icon>
              </el-upload>
              <div class="upload-tip">点击上传封面图片，建议尺寸 400x300</div>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="标签" prop="tag">
              <el-input v-model="tourForm.tag" placeholder="如：周边游·一日游" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="行程类型" prop="tourType">
              <el-select v-model="tourForm.tourType" placeholder="请选择行程类型" style="width: 100%;">
                <el-option label="周边游" value="around"></el-option>
                <el-option label="长线游" value="long"></el-option>
                <el-option label="跟团游" value="team"></el-option>
                <el-option label="邮轮出行" value="cruise"></el-option>
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="出发城市" prop="city">
              <el-select v-model="tourForm.city" placeholder="请选择出发城市" filterable style="width: 100%;">
                <el-option v-for="(name, code) in cityMap" :key="code" :label="name" :value="code"></el-option>
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="目的地" prop="destination">
              <el-select v-model="tourForm.destination" placeholder="请选择目的地" filterable style="width: 100%;">
                <el-option v-for="(name, code) in destinationMap" :key="code" :label="name" :value="code"></el-option>
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="行程天数" prop="days">
              <el-input-number v-model="tourForm.days" :min="1" :max="30" style="width: 100%;" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="出发月份" prop="month">
              <el-select v-model="tourForm.month" placeholder="请选择出发月份" style="width: 100%;">
                <el-option v-for="m in 12" :key="m" :label="m + '月'" :value="m"></el-option>
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="评分" prop="starRating">
              <el-rate v-model="tourForm.starRating" allow-half />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="推荐日期" prop="recommendDate">
              <el-input v-model="tourForm.recommendDate" placeholder="如：2026-05-01 周五" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="更多日期" prop="moreDates">
              <el-input v-model="tourForm.moreDates" placeholder="如：05-10、05-15、05-20" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="已报名人数" prop="enrolledCount">
              <el-input-number v-model="tourForm.enrolledCount" :min="0" style="width: 100%;" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="主题" prop="theme">
              <el-select v-model="tourForm.theme" placeholder="请选择主题" style="width: 100%;">
                <el-option label="风景游" value="scenic"></el-option>
                <el-option label="文化游" value="cultural"></el-option>
                <el-option label="探险游" value="adventure"></el-option>
                <el-option label="徒步游" value="hiking"></el-option>
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="行程特色" prop="feature">
              <el-input v-model="tourForm.feature" type="textarea" :rows="2" placeholder="请输入行程特色" maxlength="200" show-word-limit />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="标签" prop="tags">
              <el-input v-model="tagsInput" placeholder="请输入标签，多个标签用英文逗号分隔，如：纯玩,无购物,摄影" />
              <div class="form-tip">多个标签用英文逗号分隔，输入即生效</div>
              <div v-if="parsedTags.length > 0" class="tags-preview">
                <el-tag v-for="tag in parsedTags" :key="tag" size="small" style="margin-right: 4px; margin-top: 4px;">{{ tag }}</el-tag>
              </div>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="行程状态" prop="status">
              <el-switch
                v-model="tourForm.status"
                :active-value="1"
                :inactive-value="0"
                active-text="上架"
                inactive-text="下架"
              />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitForm" :loading="formLoading">确定</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 预订详情管理对话框 -->
    <TourDetailManager
      v-model="detailDialogVisible"
      :tour-id="detailManagerTourId"
      :tour-title="detailManagerTourTitle"
    />
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search, Refresh, Edit, Delete, Switch, ArrowDown, List } from '@element-plus/icons-vue'
import request from '@/utils/request'
import TourDetailManager from './TourDetailManager.vue'

// 出发城市映射
const cityMap = {
  chongqing: '重庆', chengdu: '成都', guiyang: '贵阳', kunming: '昆明',
  yichang: '宜昌', wuhan: '武汉', changsha: '长沙', guangzhou: '广州',
  shenzhen: '深圳', shanghai: '上海', hangzhou: '杭州', nanjing: '南京',
  xian: '西安', beijing: '北京', sanya: '三亚', haikou: '海口'
}

// 目的地映射
const destinationMap = {
  chongqing: '重庆', sichuan: '四川', yunnan: '云南', guizhou: '贵州',
  hunan: '湖南', hubei: '湖北', hainan: '海南', xisha: '西沙', sanyan: '三峡'
}

// 行程类型映射
const tourTypeMap = {
  around: '周边游', long: '长线游', team: '跟团游', cruise: '邮轮出行'
}

// 主题映射
const themeMap = {
  scenic: '风景游', cultural: '文化游', adventure: '探险游', hiking: '徒步游'
}

// 分页参数
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

// 行程列表
const tourList = ref([])
const loading = ref(false)

// 搜索表单
const searchForm = reactive({
  title: '',
  tourType: '',
  city: '',
  destination: '',
  theme: ''
})

// 对话框
const dialogVisible = ref(false)
const isEdit = ref(false)
const formLoading = ref(false)
const tourFormRef = ref(null)

// 预订详情管理
const detailDialogVisible = ref(false)
const detailManagerTourId = ref(null)
const detailManagerTourTitle = ref('')

// 标签输入
const tagsInput = ref('')
const parsedTags = computed(() => {
  if (!tagsInput.value) return []
  return tagsInput.value.split(',').map(t => t.trim()).filter(t => t)
})

// 解析标签（处理字符串或数组格式）
const parseTags = (tags) => {
  if (!tags) return []
  if (Array.isArray(tags)) return tags
  if (typeof tags === 'string') {
    // 尝试解析JSON数组
    try {
      return JSON.parse(tags)
    } catch {
      // 如果不是JSON，按逗号分隔
      return tags.split(',').map(t => t.trim()).filter(t => t)
    }
  }
  return []
}

// 图片上传
const handleImageUpload = async (options) => {
  const { file, onSuccess, onError } = options
  const formData = new FormData()
  formData.append('file', file)

  try {
    const imageUrl = await request.upload('/file/upload/img', formData, {
      showDefaultMsg: false
    })
    if (imageUrl) {
      tourForm.mainImage = imageUrl
      ElMessage.success('图片上传成功')
      onSuccess()
    } else {
      onError(new Error('上传失败'))
    }
  } catch (error) {
    ElMessage.error('图片上传失败')
    onError(error)
  }
}

// 图片上传前校验
const beforeImageUpload = (file) => {
  const isImage = file.type.startsWith('image/')
  const isLt5M = file.size / 1024 / 1024 < 5
  if (!isImage) {
    ElMessage.error('只能上传图片文件!')
    return false
  }
  if (!isLt5M) {
    ElMessage.error('图片大小不能超过 5MB!')
    return false
  }
  return true
}

// 表单数据
const tourForm = reactive({
  id: null,
  title: '',
  subtitle: '',
  mainImage: '',
  tag: '',
  tourType: '',
  city: '',
  destination: '',
  days: 1,
  month: new Date().getMonth() + 1,
  starRating: 5,
  recommendDate: '',
  moreDates: '',
  feature: '',
  tags: [],
  enrolledCount: 0,
  theme: '',
  status: 1
})

// 表单验证规则
const tourRules = {
  title: [{ required: true, message: '请输入行程名称', trigger: 'blur' }],
  tourType: [{ required: true, message: '请选择行程类型', trigger: 'change' }],
  city: [{ required: true, message: '请选择出发城市', trigger: 'change' }],
  destination: [{ required: true, message: '请选择目的地', trigger: 'change' }],
  days: [{ required: true, message: '请输入行程天数', trigger: 'blur' }]
}

// 获取行程列表
const fetchTours = async () => {
  loading.value = true
  try {
    await request.get('/tour/page', {
      title: searchForm.title,
      tourType: searchForm.tourType,
      city: searchForm.city,
      destination: searchForm.destination,
      theme: searchForm.theme,
      currentPage: currentPage.value,
      size: pageSize.value
    }, {
      showDefaultMsg: false,
      onSuccess: (res) => {
        // 确保每条记录的 tags 是数组格式
        tourList.value = (res.records || []).map(item => ({
          ...item,
          tags: parseTags(item.tags)
        }))
        total.value = res.total || 0
      }
    })
  } catch (error) {
    console.error('获取行程列表失败:', error)
  } finally {
    loading.value = false
  }
}

// 获取城市标签
const getCityLabel = (city) => cityMap[city] || city || '-'

// 获取目的地标签
const getDestinationLabel = (dest) => destinationMap[dest] || dest || '-'

// 获取行程类型标签
const getTourTypeLabel = (type) => tourTypeMap[type] || type || '-'

// 搜索
const handleSearch = () => {
  currentPage.value = 1
  fetchTours()
}

// 重置搜索
const resetSearch = () => {
  searchForm.title = ''
  searchForm.tourType = ''
  searchForm.city = ''
  searchForm.destination = ''
  searchForm.theme = ''
  currentPage.value = 1
  fetchTours()
}

// 分页
const handleSizeChange = (size) => {
  pageSize.value = size
  fetchTours()
}

const handleCurrentChange = (page) => {
  currentPage.value = page
  fetchTours()
}

// 显示添加对话框
const showAddDialog = () => {
  isEdit.value = false
  resetForm()
  dialogVisible.value = true
}

// 编辑
const handleEdit = (row) => {
  isEdit.value = true
  resetForm()
  Object.keys(tourForm).forEach(key => {
    if (row[key] !== undefined) {
      tourForm[key] = row[key]
    }
  })
  // 解析并设置标签输入
  tagsInput.value = parseTags(row.tags).join(', ')
  dialogVisible.value = true
}

// 下拉菜单操作处理
const handleAction = (command, row) => {
  switch (command) {
    case 'edit':
      handleEdit(row)
      break
    case 'detail':
      handleManageDetail(row)
      break
    case 'on':
    case 'off':
      handleToggleStatus(row)
      break
    case 'delete':
      handleDelete(row)
      break
  }
}

// 切换状态
const handleToggleStatus = async (row) => {
  const newStatus = row.status === 1 ? 0 : 1
  const statusText = newStatus === 1 ? '上架' : '下架'
  try {
    await request.put(`/tour/${row.id}`, { status: newStatus }, {
      successMsg: `行程${statusText}成功`,
      onSuccess: () => { row.status = newStatus }
    })
  } catch (error) {
    console.error('操作失败:', error)
  }
}

// 删除
const handleDelete = (row) => {
  ElMessageBox.confirm(`确定要删除行程"${row.title}"吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await request.delete(`/tour/${row.id}`, {
        successMsg: '删除成功',
        onSuccess: () => fetchTours()
      })
    } catch (error) {
      console.error('删除失败:', error)
    }
  }).catch(() => {})
}

// 重置表单
const resetForm = () => {
  if (tourFormRef.value) tourFormRef.value.resetFields()
  tagsInput.value = ''
  Object.assign(tourForm, {
    id: null, title: '', subtitle: '', mainImage: '', tag: '',
    tourType: '', city: '', destination: '', days: 1,
    month: new Date().getMonth() + 1, starRating: 5,
    recommendDate: '', moreDates: '', feature: '', tags: '',
    enrolledCount: 0, theme: '', status: 1
  })
}

// 管理预订详情
const handleManageDetail = (row) => {
  detailManagerTourId.value = row.id
  detailManagerTourTitle.value = row.title
  detailDialogVisible.value = true
}

// 预览图片
const previewImage = () => {
  if (tourForm.mainImage) {
    window.open(tourForm.mainImage, '_blank')
  }
}

// 提交表单
const submitForm = async () => {
  tourFormRef.value.validate(async (valid) => {
    if (!valid) return
    formLoading.value = true
    try {
      // 将标签转换为逗号分隔的字符串
      const submitData = { ...tourForm, tags: parsedTags.value.join(',') }

      if (isEdit.value) {
        await request.put(`/tour/${tourForm.id}`, submitData, {
          successMsg: '更新成功',
          onSuccess: () => {
            dialogVisible.value = false
            fetchTours()
          }
        })
      } else {
        await request.post('/tour', submitData, {
          successMsg: '添加成功',
          onSuccess: () => {
            dialogVisible.value = false
            fetchTours()
          }
        })
      }
    } catch (error) {
      console.error('操作失败:', error)
    } finally {
      formLoading.value = false
    }
  })
}

onMounted(() => {
  fetchTours()
})
</script>

<style lang="scss" scoped>
.tour-management {
  padding: 20px;
  background-color: #f5f7fa;
  min-height: calc(100vh - 120px);

  .page-header {
    margin-bottom: 20px;
    text-align: left;

    .page-title {
      font-size: 24px;
      color: #303133;
      margin: 0 0 4px 0;
      font-weight: 600;
    }

    .page-subtitle {
      font-size: 14px;
      color: #909399;
      margin: 0;
    }
  }

  .action-bar {
    margin-bottom: 16px;
    display: flex;
    justify-content: flex-end;

    .add-btn {
      background-color: #409eff;
      border-color: #409eff;
    }
  }

  .search-card {
    margin-bottom: 16px;
    border-radius: 8px;

    .search-form {
      display: flex;
      flex-wrap: nowrap;
      align-items: center;

      :deep(.el-form-item) {
        margin-bottom: 0;
        margin-right: 12px;
      }

      :deep(.el-form-item__label) {
        padding-right: 4px;
      }

      .search-item {
        flex-shrink: 0;
      }

      .search-buttons {
        flex-shrink: 0;
        margin-left: auto;
      }
    }
  }

  .table-card {
    border-radius: 8px;

    .tour-title {
      display: flex;
      align-items: center;
      gap: 8px;

      .title-text {
        flex: 1;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }
    }

    .price {
      color: #f56c6c;
      font-weight: 600;
    }

    .action-buttons {
      display: flex;
      flex-wrap: wrap;
      gap: 4px;
      align-items: center;

      .el-button {
        flex-shrink: 0;
        padding: 4px 8px;
      }
    }

    .el-dropdown {
      display: inline-block;
    }
  }

  .pagination-container {
    margin-top: 16px;
    display: flex;
    justify-content: flex-end;
  }

  .tour-dialog {
    .image-uploader {
      :deep(.el-upload) {
        border: 1px dashed #d9d9d9;
        border-radius: 6px;
        cursor: pointer;
        position: relative;
        overflow: hidden;
        transition: border-color 0.2s;
        &:hover {
          border-color: #409eff;
        }
      }
      .uploaded-image {
        width: 148px;
        height: 100px;
        display: block;
        object-fit: cover;
      }
      .image-uploader-icon {
        font-size: 28px;
        color: #8c939d;
        width: 148px;
        height: 100px;
        line-height: 100px;
        text-align: center;
      }
    }
    .upload-tip {
      font-size: 12px;
      color: #909399;
      margin-top: 4px;
    }

    .form-tip {
      font-size: 12px;
      color: #909399;
      margin-top: 4px;
    }

    .tags-preview {
      margin-top: 8px;
    }

    :deep(.el-rate) {
      line-height: 32px;
    }
  }
}
</style>
