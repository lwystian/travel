<template>
  <div class="tour-order-confirm-container">
    <!-- 加载状态 -->
    <div v-if="loading" class="loading-container">
      <div class="loading-spinner"></div>
      <p>正在加载订单信息...</p>
    </div>

    <!-- 错误状态 -->
    <div v-else-if="error" class="error-container">
      <el-result icon="error" title="加载失败" :sub-title="error">
        <template #extra>
          <el-button type="primary" @click="goBack">返回</el-button>
        </template>
      </el-result>
    </div>

    <!-- 订单确认表单 -->
    <div v-else-if="order" class="confirm-container">
      <!-- 步骤条 -->
      <div class="steps-container">
        <el-steps :active="currentStep" align-center finish-status="success">
          <el-step title="填写信息" description="填写出行人和联系人信息" />
          <el-step title="选择支付" description="选择支付方式完成付款" />
          <el-step title="完成预订" description="支付成功完成预订" />
        </el-steps>
      </div>

      <div class="content-wrapper">
        <!-- 左侧：表单区域 -->
        <div class="form-section">
          <!-- 产品信息 -->
          <div class="info-card">
            <div class="card-header">
              <span class="card-title">产品信息</span>
            </div>
            <div class="card-body">
              <div class="info-row">
                <span class="label">行程名称</span>
                <span class="value">{{ order.tourName }}</span>
              </div>
              <div class="info-row" v-if="order.packageName">
                <span class="label">批次套餐</span>
                <span class="value">{{ order.batchPackageName || order.packageName }}</span>
              </div>
              <div class="info-row" v-if="order.departureDate">
                <span class="label">出行日期</span>
                <span class="value">{{ formatDate(order.departureDate) }}</span>
              </div>
              <div class="info-row">
                <span class="label">行程套餐</span>
                <span class="value">
                  <template v-if="order.hotelName">
                    {{ order.tourName }} + {{ order.hotelName }} ({{ order.hotelDays }}晚)
                  </template>
                  <template v-else>
                    {{ order.tourName }}
                  </template>
                </span>
              </div>
            </div>
          </div>

          <!-- 联系人信息 -->
          <div class="info-card">
            <div class="card-header">
              <span class="card-title">联系人信息</span>
              <span class="card-subtitle">（合同签署及出团通知接收）</span>
            </div>
            <div class="card-body">
              <el-form ref="contactFormRef" :model="contactForm" :rules="contactRules" label-width="100px">
                <el-row :gutter="20">
                  <el-col :span="12">
                    <el-form-item label="姓名" prop="name">
                      <el-input v-model="contactForm.name" placeholder="请输入联系人姓名" clearable />
                    </el-form-item>
                  </el-col>
                  <el-col :span="12">
                    <el-form-item label="手机号" prop="phone">
                      <el-input v-model="contactForm.phone" placeholder="请输入手机号" clearable maxlength="11" />
                    </el-form-item>
                  </el-col>
                </el-row>
              </el-form>
            </div>
          </div>

          <!-- 出行人信息 -->
          <div class="info-card">
            <div class="card-header">
              <span class="card-title">出行人信息</span>
              <span class="card-subtitle">（需 {{ order.adultCount }} 成人{{ order.childCount > 0 ? `, ${order.childCount} 儿童` : '' }}）</span>
            </div>
            <div class="card-body">
              <!-- 常用出行人选择区域 -->
              <div class="frequent-travelers-section">
                <div class="section-title">
                  <span>从常用出行人中选择</span>
                  <el-button type="primary" size="small" @click="showAddTravelerDialog">
                    <el-icon><Plus /></el-icon>
                    添加常用出行人
                  </el-button>
                </div>
                
                <!-- 已保存的常用出行人列表 -->
                <div v-if="frequentTravelers.length > 0" class="frequent-traveler-list">
                  <div
                    v-for="traveler in frequentTravelers"
                    :key="traveler.id"
                    :class="['frequent-traveler-item', { selected: selectedFrequentTravelerIds.includes(traveler.id) }]"
                    @click="toggleFrequentTraveler(traveler)"
                  >
                    <el-checkbox
                      :model-value="selectedFrequentTravelerIds.includes(traveler.id)"
                      @click.stop
                      @change="toggleFrequentTraveler(traveler)"
                    />
                    <div class="traveler-info" @click.stop="toggleFrequentTraveler(traveler)">
                      <div class="traveler-main">
                        <span class="traveler-name">{{ traveler.name }}</span>
                        <el-tag :type="traveler.travelerType === 'ADULT' ? 'success' : 'warning'" size="small">
                          {{ traveler.travelerType === 'ADULT' ? '成人' : '儿童' }}
                        </el-tag>
                        <span class="traveler-phone">{{ traveler.phone || '未填写电话' }}</span>
                      </div>
                      <div class="traveler-detail">
                        <span>证件类型：{{ getIdTypeLabel(traveler.idType) }}</span>
                        <span>证件号码：{{ maskIdNumber(traveler.idNumber) }}</span>
                        <span v-if="traveler.birthDate">出生日期：{{ formatDate(traveler.birthDate) }}</span>
                      </div>
                    </div>
                    <div class="traveler-actions" @click.stop>
                      <el-button type="primary" link size="small" @click="editFrequentTraveler(traveler)">编辑</el-button>
                      <el-button type="danger" link size="small" @click="handleDeleteFrequentTraveler(traveler.id)">删除</el-button>
                    </div>
                  </div>
                </div>
                <div v-else class="no-frequent-travelers">
                  <span>暂无常用出行人，点击上方按钮添加</span>
                </div>
              </div>

              <!-- 已选择的出行人列表 -->
              <div v-if="selectedTravelers.length > 0" class="selected-travelers-section">
                <div class="section-title selected-title">
                  <span>已选择的出行人（{{ selectedTravelers.length }}/{{ totalRequired }}）</span>
                  <span v-if="selectedTravelers.length < totalRequired" class="require-hint">还需选择 {{ totalRequired - selectedTravelers.length }} 人</span>
                </div>
                <div class="selected-traveler-list">
                  <div v-for="(traveler, index) in selectedTravelers" :key="index" class="selected-traveler-item">
                    <div class="selected-main">
                      <el-tag :type="traveler.travelerType === 'ADULT' ? 'success' : 'warning'" size="small">
                        {{ traveler.travelerType === 'ADULT' ? '成人' : '儿童' }}
                      </el-tag>
                      <span class="selected-name">{{ traveler.name }}</span>
                      <span class="selected-phone">{{ traveler.phone }}</span>
                    </div>
                    <el-button type="danger" link size="small" @click="removeSelectedTraveler(index)">移除</el-button>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 右侧：结算信息（固定定位） -->
        <div class="summary-section" :class="{ 'is-fixed': isSummaryFixed }" ref="summarySection">
          <div class="summary-card">
            <div class="summary-header">
              <span>结算信息</span>
            </div>
            <div class="summary-body">
              <div class="fee-item" v-if="order.adultCount > 0">
                <span class="fee-label">成人</span>
                <span class="fee-value">
                  ¥{{ formatPrice(order.adultUnitPrice) }} × {{ order.adultCount }}
                </span>
              </div>
              <div class="fee-item" v-if="order.childCount > 0">
                <span class="fee-label">儿童</span>
                <span class="fee-value">
                  ¥{{ formatPrice(order.childUnitPrice) }} × {{ order.childCount }}
                </span>
              </div>
              <div class="fee-item" v-if="order.hotelAmount">
                <span class="fee-label">酒店住宿</span>
                <span class="fee-value">¥{{ formatPrice(order.hotelAmount) }}</span>
              </div>
              <div class="fee-total">
                <span class="total-label">应付总额</span>
                <span class="total-value">¥{{ formatPrice(order.totalAmount) }}</span>
              </div>
            </div>
            <div class="summary-footer">
              <!-- 操作按钮 -->
              <div class="action-buttons">
                <el-button @click="goBack" size="large">返回</el-button>
                <el-button type="primary" @click="handlePay" :loading="submitting" size="large">
                  去支付
                </el-button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 添加/编辑常用出行人弹窗 -->
    <el-dialog
      v-model="travelerDialogVisible"
      :title="editingTraveler ? '编辑出行人' : '添加出行人'"
      width="600px"
      :close-on-click-modal="false"
    >
      <el-form ref="travelerFormRef" :model="travelerForm" :rules="travelerRules" label-width="100px">
        <el-form-item label="姓名" prop="name">
          <el-input v-model="travelerForm.name" placeholder="请输入出行人姓名" clearable />
        </el-form-item>
        <el-form-item label="性别" prop="gender">
          <el-radio-group v-model="travelerForm.gender">
            <el-radio value="MALE">男</el-radio>
            <el-radio value="FEMALE">女</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="类型" prop="travelerType">
          <el-radio-group v-model="travelerForm.travelerType">
            <el-radio value="ADULT">成人</el-radio>
            <el-radio value="CHILD">儿童</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="手机号码" prop="phone">
          <el-input v-model="travelerForm.phone" placeholder="请输入手机号码" clearable maxlength="11" />
        </el-form-item>
        <el-form-item label="证件类型" prop="idType">
          <el-select v-model="travelerForm.idType" placeholder="请选择证件类型" style="width: 100%">
            <el-option label="身份证" value="ID_CARD" />
            <el-option label="护照" value="PASSPORT" />
          </el-select>
        </el-form-item>
        <el-form-item label="证件号码" prop="idNumber">
          <el-input v-model="travelerForm.idNumber" :placeholder="idNumberPlaceholder" clearable maxlength="18" />
        </el-form-item>
        <el-form-item label="出生日期" prop="birthDate">
          <el-date-picker
            v-model="travelerForm.birthDate"
            type="date"
            placeholder="选择出生日期"
            style="width: 100%"
            format="YYYY-MM-DD"
            value-format="YYYY-MM-DD"
            :disabled-date="disableFutureDate"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="travelerDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveTraveler">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, reactive } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { getTourOrderDetail, updateOrderContact } from '@/api/tourOrder'
import { getFrequentTravelers, saveFrequentTraveler, updateFrequentTraveler, deleteFrequentTraveler as deleteFrequentTravelerApi } from '@/api/frequentTraveler'
import { saveTravelers } from '@/api/traveler'
import { ElMessageBox } from 'element-plus'

const route = useRoute()
const router = useRouter()

// 状态
const loading = ref(true)
const submitting = ref(false)
const order = ref(null)
const error = ref(null)
const currentStep = ref(0)
const contactFormRef = ref(null)
const travelerFormRef = ref(null)
const summarySection = ref(null)
const isSummaryFixed = ref(false)
let summaryTop = 0
let summaryWidth = 320

// 联系人表单
const contactForm = reactive({
  name: '',
  phone: ''
})

// 联系人验证规则
const contactRules = {
  name: [
    { required: true, message: '请输入联系人姓名', trigger: 'blur' },
    { min: 2, max: 20, message: '姓名长度为2-20个字符', trigger: 'blur' }
  ],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的11位手机号', trigger: 'blur' }
  ]
}

// 常用出行人相关
const frequentTravelers = ref([])
const selectedFrequentTravelerIds = ref([])
const selectedTravelers = ref([])
const travelerDialogVisible = ref(false)
const editingTraveler = ref(null)

// 出行人表单
const travelerForm = reactive({
  name: '',
  gender: 'MALE',
  travelerType: 'ADULT',
  phone: '',
  idType: 'ID_CARD',
  idNumber: '',
  birthDate: ''
})

// 出行人验证规则
const travelerRules = computed(() => ({
  name: [
    { required: true, message: '请输入出行人姓名', trigger: 'blur' },
    { min: 2, max: 20, message: '姓名长度为2-20个字符', trigger: 'blur' }
  ],
  gender: [
    { required: true, message: '请选择性别', trigger: 'change' }
  ],
  travelerType: [
    { required: true, message: '请选择类型', trigger: 'change' }
  ],
  phone: [
    { required: true, message: '请输入手机号码', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的11位手机号', trigger: 'blur' }
  ],
  idType: [
    { required: true, message: '请选择证件类型', trigger: 'change' }
  ],
  idNumber: [
    { required: true, message: '请输入证件号码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (travelerForm.idType === 'ID_CARD') {
          if (!/^\d{17}[\dXx]$/.test(value)) {
            callback(new Error('身份证号码格式不正确'))
          } else {
            callback()
          }
        } else if (travelerForm.idType === 'PASSPORT') {
          if (!/^[A-Za-z0-9]{6,9}$/.test(value)) {
            callback(new Error('护照号码格式不正确'))
          } else {
            callback()
          }
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ],
  birthDate: [
    { required: true, message: '请选择出生日期', trigger: 'change' }
  ]
}))

// 计算属性
const totalRequired = computed(() => {
  return (order.value?.adultCount || 0) + (order.value?.childCount || 0)
})

const adultRequired = computed(() => order.value?.adultCount || 0)
const childRequired = computed(() => order.value?.childCount || 0)

const selectedAdultCount = computed(() => {
  return selectedTravelers.value.filter(t => t.travelerType === 'ADULT').length
})

const selectedChildCount = computed(() => {
  return selectedTravelers.value.filter(t => t.travelerType === 'CHILD').length
})

const canSubmit = computed(() => {
  // 检查联系人信息
  if (!contactForm.name || !contactForm.phone) return false
  if (!/^1[3-9]\d{9}$/.test(contactForm.phone)) return false
  
  // 检查出行人数量
  if (selectedTravelers.value.length < totalRequired.value) return false
  
  // 检查成人数量
  if (selectedAdultCount.value < adultRequired.value) return false
  
  // 检查儿童数量
  if (childRequired.value > 0 && selectedChildCount.value < childRequired.value) return false
  
  // 检查每个出行人的信息完整性
  for (const traveler of selectedTravelers.value) {
    if (!traveler.name) return false
    if (!traveler.phone || !/^1[3-9]\d{9}$/.test(traveler.phone)) return false
    if (!traveler.idType) return false
    if (!traveler.idNumber) return false
    if (!traveler.birthDate) return false
    if (!traveler.gender) return false
    if (!traveler.travelerType) return false
  }
  
  return true
})

// 证件号码提示
const idNumberPlaceholder = computed(() => {
  return travelerForm.idType === 'ID_CARD' ? '请输入18位身份证号码' : '请输入6-9位护照号码'
})

// 方法
const getIdTypeLabel = (type) => {
  const map = { 'ID_CARD': '身份证', 'PASSPORT': '护照' }
  return map[type] || type
}

const maskIdNumber = (idNumber) => {
  if (!idNumber) return ''
  if (idNumber.length > 8) {
    return idNumber.substring(0, 4) + '****' + idNumber.substring(idNumber.length - 4)
  }
  return idNumber
}

const formatDate = (dateStr) => {
  if (!dateStr) return ''
  return String(dateStr).split('T')[0]
}

const formatPrice = (price) => {
  if (!price) return '0.00'
  return Number(price).toFixed(2)
}

const disableFutureDate = (date) => {
  return date.getTime() > Date.now()
}

// 加载常用出行人列表
const loadFrequentTravelers = async () => {
  try {
    const data = await getFrequentTravelers()
    frequentTravelers.value = data || []
  } catch (err) {
    console.error('获取常用出行人失败:', err)
  }
}

// 切换常用出行人选择
const toggleFrequentTraveler = (traveler) => {
  const index = selectedFrequentTravelerIds.value.indexOf(traveler.id)
  if (index > -1) {
    // 取消选择
    selectedFrequentTravelerIds.value.splice(index, 1)
    selectedTravelers.value = selectedTravelers.value.filter(t => t.frequentTravelerId !== traveler.id)
  } else {
    // 检查类型和数量限制
    if (traveler.travelerType === 'ADULT' && selectedAdultCount.value >= adultRequired.value) {
      ElMessage.warning(`成人数量已满足要求（需要${adultRequired.value}人）`)
      return
    }
    if (traveler.travelerType === 'CHILD' && childRequired.value > 0 && selectedChildCount.value >= childRequired.value) {
      ElMessage.warning(`儿童数量已满足要求（需要${childRequired.value}人）`)
      return
    }
    if (traveler.travelerType === 'CHILD' && childRequired.value === 0) {
      ElMessage.warning('该订单不需要儿童')
      return
    }
    
    // 添加选择
    selectedFrequentTravelerIds.value.push(traveler.id)
    selectedTravelers.value.push({
      frequentTravelerId: traveler.id,
      name: traveler.name,
      phone: traveler.phone,
      idType: traveler.idType,
      idNumber: traveler.idNumber,
      birthDate: traveler.birthDate ? String(traveler.birthDate).split('T')[0] : '',
      gender: traveler.gender,
      travelerType: traveler.travelerType
    })
  }
}

// 移除已选出行人
const removeSelectedTraveler = (index) => {
  const traveler = selectedTravelers.value[index]
  if (traveler.frequentTravelerId) {
    const idIndex = selectedFrequentTravelerIds.value.indexOf(traveler.frequentTravelerId)
    if (idIndex > -1) {
      selectedFrequentTravelerIds.value.splice(idIndex, 1)
    }
  }
  selectedTravelers.value.splice(index, 1)
}

// 显示添加出行人弹窗
const showAddTravelerDialog = () => {
  editingTraveler.value = null
  Object.assign(travelerForm, {
    name: '',
    gender: 'MALE',
    travelerType: 'ADULT',
    phone: '',
    idType: 'ID_CARD',
    idNumber: '',
    birthDate: ''
  })
  travelerDialogVisible.value = true
}

// 编辑常用出行人
const editFrequentTraveler = (traveler) => {
  editingTraveler.value = traveler
  Object.assign(travelerForm, {
    id: traveler.id,
    name: traveler.name,
    gender: traveler.gender || 'MALE',
    travelerType: traveler.travelerType || 'ADULT',
    phone: traveler.phone || '',
    idType: traveler.idType || 'ID_CARD',
    idNumber: traveler.idNumber || '',
    birthDate: traveler.birthDate ? String(traveler.birthDate).split('T')[0] : ''
  })
  travelerDialogVisible.value = true
}

// 保存出行人
const saveTraveler = async () => {
  try {
    await travelerFormRef.value?.validate()
    
    const data = {
      name: travelerForm.name,
      gender: travelerForm.gender,
      travelerType: travelerForm.travelerType,
      phone: travelerForm.phone,
      idType: travelerForm.idType,
      idNumber: travelerForm.idNumber,
      birthDate: travelerForm.birthDate
    }
    
    if (editingTraveler.value) {
      await updateFrequentTraveler(editingTraveler.value.id, data)
      ElMessage.success('出行人信息已更新')

      // 更新本地常用出行人列表中的数据
      const index = frequentTravelers.value.findIndex(t => t.id === editingTraveler.value.id)
      if (index > -1) {
        frequentTravelers.value[index] = {
          ...frequentTravelers.value[index],
          ...data
        }
      }

      // 如果该出行人已被选中，同时更新已选择列表中的数据
      const selectedIndex = selectedTravelers.value.findIndex(t => t.frequentTravelerId === editingTraveler.value.id)
      if (selectedIndex > -1) {
        selectedTravelers.value[selectedIndex] = {
          ...selectedTravelers.value[selectedIndex],
          ...data
        }
      }
    } else {
      const result = await saveFrequentTraveler(data)
      ElMessage.success('出行人已添加到常用列表')
      
      // 自动选中新增的出行人
      if (result && result.id) {
        const newTraveler = {
          id: result.id,
          ...data
        }
        frequentTravelers.value.unshift(newTraveler)
        
        // 自动勾选
        toggleFrequentTraveler(newTraveler)
      }
    }
    
    travelerDialogVisible.value = false
  } catch (err) {
    if (err !== false) {
      console.error('保存出行人失败:', err)
    }
  }
}

// 删除常用出行人
const handleDeleteFrequentTraveler = async (id) => {
  try {
    await ElMessageBox.confirm('确定要删除该常用出行人吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    await deleteFrequentTravelerApi(id)
    
    // 从列表中移除
    const index = frequentTravelers.value.findIndex(t => t.id === id)
    if (index > -1) {
      frequentTravelers.value.splice(index, 1)
    }
    
    // 从已选中列表中移除
    const selectedIndex = selectedFrequentTravelerIds.value.indexOf(id)
    if (selectedIndex > -1) {
      selectedFrequentTravelerIds.value.splice(selectedIndex, 1)
      selectedTravelers.value = selectedTravelers.value.filter(t => t.frequentTravelerId !== id)
    }
    
    ElMessage.success('已删除')
  } catch (err) {
    if (err !== 'cancel') {
      console.error('删除出行人失败:', err)
    }
  }
}

// 加载订单信息
const loadOrderInfo = async () => {
  try {
    loading.value = true
    error.value = null

    const orderId = route.params.id
    if (!orderId) {
      error.value = '订单信息不存在'
      return
    }

    const data = await getTourOrderDetail(orderId)
    order.value = data

    // 设置联系人信息
    contactForm.name = data.contactName || ''
    contactForm.phone = data.contactPhone || ''
  } catch (err) {
    console.error('加载订单信息失败:', err)
    error.value = err.message || '加载订单信息失败'
  } finally {
    loading.value = false
  }
}

// 去支付
const handlePay = async () => {
  // 验证联系人表单
  try {
    await contactFormRef.value?.validate()
  } catch {
    ElMessage.warning('请完善联系人信息')
    return
  }
  
  // 检查出行人数量
  if (selectedTravelers.value.length < totalRequired.value) {
    ElMessage.warning(`请选择至少 ${totalRequired.value} 位出行人`)
    return
  }
  
  // 检查成人数量
  if (selectedAdultCount.value < adultRequired.value) {
    ElMessage.warning(`请选择至少 ${adultRequired.value} 位成人`)
    return
  }
  
  // 检查儿童数量
  if (childRequired.value > 0 && selectedChildCount.value < childRequired.value) {
    ElMessage.warning(`请选择至少 ${childRequired.value} 位儿童`)
    return
  }
  
  // 检查每个出行人的信息
  for (let i = 0; i < selectedTravelers.value.length; i++) {
    const t = selectedTravelers.value[i]
    if (!t.name) {
      ElMessage.warning(`第 ${i + 1} 位出行人姓名不能为空`)
      return
    }
    if (!t.phone || !/^1[3-9]\d{9}$/.test(t.phone)) {
      ElMessage.warning(`第 ${i + 1} 位出行人手机号格式不正确`)
      return
    }
    if (!t.idType) {
      ElMessage.warning(`第 ${i + 1} 位出行人证件类型不能为空`)
      return
    }
    if (!t.idNumber) {
      ElMessage.warning(`第 ${i + 1} 位出行人证件号码不能为空`)
      return
    }
    if (!t.birthDate) {
      ElMessage.warning(`第 ${i + 1} 位出行人出生日期不能为空`)
      return
    }
  }

  submitting.value = true

  try {
    // 先更新联系人信息到订单
    await updateOrderContact(order.value.id, contactForm.name, contactForm.phone)

    // 保存出行人信息（移除前端用的 frequentTravelerId 字段）
    const travelersData = selectedTravelers.value.map((t, index) => {
      const { frequentTravelerId, ...traveler } = t
      return {
        ...traveler,
        travelerIndex: index + 1
      }
    })
    
    await saveTravelers(order.value.id, order.value.orderNo, travelersData)

    // 跳转到支付页面
    router.push(`/tour-order-pay/${order.value.id}`)
  } catch (err) {
    console.error('保存信息失败:', err)
    ElMessage.error(err.message || '保存信息失败，请重试')
  } finally {
    submitting.value = false
  }
}

// 返回
const goBack = () => {
  router.back()
}

onMounted(async () => {
  await loadOrderInfo()
  await loadFrequentTravelers()

  // 等待 DOM 渲染完成后获取结算栏位置并添加滚动监听
  setTimeout(() => {
    updateSummaryPosition()
    window.addEventListener('scroll', handleScroll)
    window.addEventListener('resize', updateSummaryPosition)
  }, 100)
})

onUnmounted(() => {
  window.removeEventListener('scroll', handleScroll)
  window.removeEventListener('resize', updateSummaryPosition)
})

// 获取结算栏的初始位置
const updateSummaryPosition = () => {
  if (summarySection.value) {
    const rect = summarySection.value.getBoundingClientRect()
    summaryTop = rect.top + window.scrollY
    // 获取初始的 left 和 width 位置
    const leftPos = rect.left
    const width = rect.width
    summarySection.value.style.left = leftPos + 'px'
    // 存储宽度
    summaryWidth = width
  }
}

// 处理滚动事件
const handleScroll = () => {
  const scrollTop = window.scrollY
  const headerHeight = 160 // 页面头部高度（顶部信息栏 + Logo搜索栏 + 导航菜单）

  if (scrollTop + headerHeight >= summaryTop) {
    if (!isSummaryFixed.value) {
      isSummaryFixed.value = true
      // 创建占位元素
      createPlaceholder()
      // 固定时设置位置
      if (summarySection.value) {
        summarySection.value.style.width = summaryWidth + 'px'
      }
    }
  } else {
    if (isSummaryFixed.value) {
      isSummaryFixed.value = false
      // 移除占位元素
      removePlaceholder()
      // 恢复样式
      if (summarySection.value) {
        summarySection.value.style.left = ''
        summarySection.value.style.width = ''
      }
    }
  }
}

// 创建占位元素
let placeholderEl = null
const createPlaceholder = () => {
  if (!placeholderEl && summarySection.value) {
    placeholderEl = document.createElement('div')
    placeholderEl.className = 'summary-placeholder'
    placeholderEl.style.width = '320px'
    placeholderEl.style.flexShrink = '0'
    summarySection.value.parentNode.insertBefore(placeholderEl, summarySection.value)
  }
}

// 移除占位元素
const removePlaceholder = () => {
  if (placeholderEl) {
    placeholderEl.parentNode.removeChild(placeholderEl)
    placeholderEl = null
  }
}
</script>

<style scoped>
.tour-order-confirm-container {
  width: 100%;
  min-height: calc(100vh - 200px);
  background: linear-gradient(135deg, #f5f7fa 0%, #e4e8ed 100%);
  padding: 40px 20px;
}

.loading-container,
.error-container {
  max-width: 800px;
  margin: 0 auto;
  background: white;
  border-radius: 16px;
  padding: 60px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
  text-align: center;
}

.loading-spinner {
  width: 50px;
  height: 50px;
  border: 4px solid #f3f3f3;
  border-top: 4px solid #409EFF;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin: 0 auto 20px;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.confirm-container {
  max-width: 1200px;
  margin: 0 auto;
}

.steps-container {
  background: white;
  border-radius: 16px;
  padding: 30px;
  margin-bottom: 20px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
}

.content-wrapper {
  display: flex;
  gap: 20px;
  align-items: flex-start;
}

.form-section {
  flex: 1;
  min-width: 0;
}

.summary-section {
  width: 320px;
  flex-shrink: 0;
  transition: opacity 0.3s ease;
}

.summary-section.is-fixed {
  position: fixed;
  top: 160px;
  z-index: 100;
}

@media (max-width: 1200px) {
  .summary-section.is-fixed {
    right: 20px;
  }
}

.info-card {
  background: white;
  border-radius: 12px;
  margin-bottom: 20px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
  overflow: hidden;
}

.card-header {
  display: flex;
  align-items: center;
  padding: 16px 20px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.card-title {
  font-weight: 600;
  font-size: 16px;
}

.card-subtitle {
  margin-left: 10px;
  font-size: 13px;
  opacity: 0.8;
}

.card-body {
  padding: 20px;
}

.info-row {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  padding: 10px 0;
  border-bottom: 1px dashed #eee;
}

.info-row:last-child {
  border-bottom: none;
}

.info-row .label {
  color: #666;
  font-size: 14px;
  flex-shrink: 0;
}

.info-row .value {
  color: #333;
  font-size: 14px;
  text-align: right;
  max-width: 60%;
}

.frequent-travelers-section {
  margin-bottom: 20px;
}

.section-title {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px solid #eee;
  margin-bottom: 12px;
  font-weight: 600;
  color: #333;
}

.frequent-traveler-list {
  max-height: 300px;
  overflow-y: auto;
}

.frequent-traveler-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  border: 1px solid #e8e8e8;
  border-radius: 8px;
  margin-bottom: 8px;
  cursor: pointer;
  transition: all 0.2s;
}

.frequent-traveler-item:hover {
  border-color: #409EFF;
  background: #f6faff;
}

.frequent-traveler-item.selected {
  border-color: #409EFF;
  background: #ecf5ff;
}

.traveler-info {
  flex: 1;
  min-width: 0;
}

.traveler-main {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: nowrap;
}

.traveler-name {
  font-weight: 600;
  color: #333;
  font-size: 15px;
}

.traveler-phone {
  color: #999;
  font-size: 13px;
  margin-left: 8px;
}

.traveler-detail {
  display: flex;
  gap: 16px;
  font-size: 12px;
  color: #666;
}

.traveler-actions {
  display: flex;
  gap: 8px;
}

.no-frequent-travelers {
  text-align: center;
  padding: 30px;
  color: #999;
  background: #fafafa;
  border-radius: 8px;
}

.selected-travelers-section {
  background: #fffbf0;
  border-radius: 8px;
  padding: 12px;
  margin-bottom: 20px;
}

.selected-title {
  border-bottom-color: #ffe0b2;
}

.require-hint {
  color: #ff4d4f;
  font-size: 13px;
  font-weight: normal;
}

.selected-traveler-list {
  max-height: 200px;
  overflow-y: auto;
}

.selected-traveler-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 12px;
  background: #f5f5f5;
  border-radius: 6px;
  margin-top: 8px;
}

.selected-main {
  display: flex;
  align-items: center;
  gap: 10px;
}

.selected-name {
  font-weight: 500;
  color: #333;
  font-size: 13px;
}

.selected-phone {
  color: #666;
  font-size: 13px;
}

.summary-card {
  background: white;
  border-radius: 12px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
}

.summary-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  background: linear-gradient(135deg, #1677ff 0%, #0958d9 100%);
  color: white;
  border-radius: 12px 12px 0 0;
}

.summary-header span:first-child {
  font-weight: 600;
}

.summary-body {
  padding: 20px;
}

.fee-item {
  display: flex;
  justify-content: space-between;
  padding: 8px 0;
  font-size: 14px;
}

.fee-label {
  color: #666;
}

.fee-value {
  color: #333;
}

.fee-total {
  display: flex;
  justify-content: space-between;
  padding-top: 15px;
  margin-top: 10px;
  border-top: 2px dashed #e0d5b0;
}

.total-label {
  font-weight: 600;
  color: #333;
  font-size: 15px;
}

.total-value {
  font-weight: bold;
  color: #ff4d4f;
  font-size: 20px;
}

.summary-footer {
  padding: 20px;
  border-top: 1px solid #eee;
}

.action-buttons {
  display: flex;
  flex-direction: row;
  gap: 12px;
}

.action-buttons .el-button {
  flex: 1;
  height: 48px;
  font-size: 15px;
}

.action-buttons .el-button--primary {
  background: linear-gradient(135deg, #1677ff 0%, #0958d9 100%);
  border: none;
}

@media (max-width: 768px) {
  .content-wrapper {
    flex-direction: column;
  }

  .summary-section {
    width: 100%;
    position: static;
  }

  .traveler-detail {
    flex-wrap: wrap;
    gap: 8px;
  }

  .traveler-phone {
    display: none;
  }
}
</style>
