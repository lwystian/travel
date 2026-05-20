<template>
  <main class="checkout-page">
    <section v-if="loading" class="state-panel">
      <el-skeleton :rows="10" animated />
      <p>正在加载订单信息...</p>
    </section>

    <section v-else-if="error" class="state-panel">
      <el-result icon="error" title="加载失败" :sub-title="error">
        <template #extra>
          <el-button type="primary" @click="goBack">返回</el-button>
        </template>
      </el-result>
    </section>

    <section v-else-if="order" class="checkout-shell">
      <header class="checkout-hero">
        <div>
          <span class="eyebrow">Checkout</span>
          <h1>确认订单信息</h1>
          <p>请核对行程、联系人和出行人信息。提交后将进入支付方式选择。</p>
        </div>
        <div class="step-strip">
          <span class="active">1 确认信息</span>
          <span>2 选择支付</span>
          <span>3 完成预订</span>
        </div>
      </header>

      <div class="checkout-layout">
        <section class="main-flow">
          <article class="panel-card">
            <header class="panel-head">
              <div>
                <span>Product</span>
                <strong>行程信息</strong>
              </div>
            </header>
            <div class="product-grid">
              <div>
                <span>行程名称</span>
                <strong>{{ order.tourName || '-' }}</strong>
              </div>
              <div>
                <span>套餐</span>
                <strong>{{ order.batchPackageName || order.packageName || '标准套餐' }}</strong>
              </div>
              <div>
                <span>出发日期</span>
                <strong>{{ formatDate(order.departureDate) || '-' }}</strong>
              </div>
              <div>
                <span>出行人数</span>
                <strong>成人 {{ order.adultCount || 0 }} 人<span v-if="order.childCount">，儿童 {{ order.childCount }} 人</span></strong>
              </div>
              <div v-if="order.hotelName" class="wide">
                <span>住宿酒店</span>
                <strong>{{ order.hotelName }}（{{ order.hotelDays || 0 }} 晚）</strong>
              </div>
            </div>
          </article>

          <article class="panel-card">
            <header class="panel-head">
              <div>
                <span>Contact</span>
                <strong>联系人信息</strong>
              </div>
              <p>用于接收出团通知、合同签署和订单服务提醒。</p>
            </header>
            <el-form ref="contactFormRef" :model="contactForm" :rules="contactRules" label-position="top" class="contact-form">
              <el-form-item label="联系人姓名" prop="name">
                <el-input v-model="contactForm.name" placeholder="请输入联系人姓名" clearable />
              </el-form-item>
              <el-form-item label="联系人手机号" prop="phone">
                <el-input v-model="contactForm.phone" placeholder="请输入 11 位手机号" clearable maxlength="11" />
              </el-form-item>
            </el-form>
          </article>

          <article class="panel-card">
            <header class="panel-head">
              <div>
                <span>Travelers</span>
                <strong>出行人信息</strong>
              </div>
              <el-button type="primary" @click="showAddTravelerDialog">
                <el-icon><Plus /></el-icon>
                添加出行人
              </el-button>
            </header>

            <div class="quota-row">
              <span>需要成人 {{ adultRequired }} 人</span>
              <span v-if="childRequired">需要儿童 {{ childRequired }} 人</span>
              <strong>已选择 {{ selectedTravelers.length }}/{{ totalRequired }} 人</strong>
            </div>

            <div v-if="frequentTravelers.length > 0" class="traveler-list">
              <div
                v-for="traveler in frequentTravelers"
                :key="traveler.id"
                class="traveler-card"
                :class="{ selected: selectedFrequentTravelerIds.includes(traveler.id) }"
                @click="toggleFrequentTraveler(traveler)"
              >
                <el-checkbox :model-value="selectedFrequentTravelerIds.includes(traveler.id)" @click.stop @change="toggleFrequentTraveler(traveler)" />
                <div class="traveler-info">
                  <div class="traveler-title">
                    <strong>{{ traveler.name }}</strong>
                    <el-tag :type="traveler.travelerType === 'ADULT' ? 'success' : 'warning'" size="small">
                      {{ traveler.travelerType === 'ADULT' ? '成人' : '儿童' }}
                    </el-tag>
                  </div>
                  <p>{{ maskPhone(traveler.phone, '未填写电话') }} · {{ getIdTypeLabel(traveler.idType) }} {{ maskIdNumber(traveler.idNumber) }}</p>
                </div>
                <div class="traveler-actions" @click.stop>
                  <el-button link type="primary" @click="editFrequentTraveler(traveler)">编辑</el-button>
                  <el-button link type="danger" @click="handleDeleteFrequentTraveler(traveler.id)">删除</el-button>
                </div>
              </div>
            </div>

            <div v-else class="empty-inline">
              暂无常用出行人，请先添加后再选择。
            </div>

            <div v-if="selectedTravelers.length > 0" class="selected-box">
              <header>
                <strong>已选出行人</strong>
                <span v-if="selectedTravelers.length < totalRequired">还需选择 {{ totalRequired - selectedTravelers.length }} 人</span>
              </header>
              <div class="selected-list">
                <div v-for="(traveler, index) in selectedTravelers" :key="index" class="selected-item">
                  <span>{{ index + 1 }}</span>
                  <strong>{{ traveler.name }}</strong>
                  <em>{{ traveler.travelerType === 'ADULT' ? '成人' : '儿童' }}</em>
                  <small>{{ maskPhone(traveler.phone, '-') }}</small>
                  <el-button link type="danger" @click="removeSelectedTraveler(index)">移除</el-button>
                </div>
              </div>
            </div>
          </article>
        </section>

        <aside class="summary-panel">
          <div class="summary-card">
            <header>
              <span>Settlement</span>
              <strong>结算信息</strong>
            </header>
            <div class="fee-list">
              <div v-if="order.adultCount > 0">
                <span>成人</span>
                <strong>¥{{ formatPrice(order.adultUnitPrice) }} x {{ order.adultCount }}</strong>
              </div>
              <div v-if="order.childCount > 0">
                <span>儿童</span>
                <strong>¥{{ formatPrice(order.childUnitPrice) }} x {{ order.childCount }}</strong>
              </div>
              <div v-if="order.hotelAmount">
                <span>酒店住宿</span>
                <strong>¥{{ formatPrice(order.hotelAmount) }}</strong>
              </div>
            </div>
            <div class="total-row">
              <span>应付总额</span>
              <strong>¥{{ formatPrice(order.totalAmount) }}</strong>
            </div>
            <div class="security-note">订单信息提交前会进行联系人、出行人数量和证件信息校验。</div>
            <div class="summary-actions">
              <el-button @click="goBack" :disabled="submitting">返回</el-button>
              <el-button type="primary" @click="handlePay" :loading="submitting">
                去选择支付方式
              </el-button>
            </div>
          </div>
        </aside>
      </div>
    </section>

    <el-dialog
      v-model="travelerDialogVisible"
      :title="editingTraveler ? '编辑出行人' : '添加出行人'"
      width="620px"
      :close-on-click-modal="false"
      class="traveler-dialog"
    >
      <el-form ref="travelerFormRef" :model="travelerForm" :rules="travelerRules" label-position="top" class="traveler-form">
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
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="travelerForm.phone" placeholder="请输入手机号" clearable maxlength="11" />
        </el-form-item>
        <el-form-item label="证件类型" prop="idType">
          <el-select v-model="travelerForm.idType" placeholder="请选择证件类型" style="width: 100%">
            <el-option label="身份证" value="ID_CARD" />
          </el-select>
        </el-form-item>
        <el-form-item label="证件号码" prop="idNumber">
          <el-input v-model="travelerForm.idNumber" :placeholder="idNumberPlaceholder" clearable maxlength="18" />
        </el-form-item>
        <el-form-item label="出生日期" prop="birthDate">
          <el-date-picker
            v-model="travelerForm.birthDate"
            type="date"
            placeholder="请选择出生日期"
            style="width: 100%"
            format="YYYY-MM-DD"
            value-format="YYYY-MM-DD"
            :disabled-date="disableFutureDate"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="travelerDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveTraveler">保存</el-button>
      </template>
    </el-dialog>
  </main>
</template>

<script setup>
import { ref, computed, onMounted, reactive } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { getTourOrderDetail, updateOrderContact } from '@/api/tourOrder'
import { getFrequentTravelers, saveFrequentTraveler, updateFrequentTraveler, deleteFrequentTraveler as deleteFrequentTravelerApi } from '@/api/frequentTraveler'
import { saveTravelers } from '@/api/traveler'
import { maskPhone } from '@/utils/mask'

const route = useRoute()
const router = useRouter()

const loading = ref(true)
const submitting = ref(false)
const order = ref(null)
const error = ref(null)
const contactFormRef = ref(null)
const travelerFormRef = ref(null)

const contactForm = reactive({
  name: '',
  phone: ''
})

const contactRules = {
  name: [
    { required: true, message: '请输入联系人姓名', trigger: 'blur' },
    { min: 2, max: 20, message: '姓名长度为 2-20 个字符', trigger: 'blur' }
  ],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的 11 位手机号', trigger: 'blur' }
  ]
}

const frequentTravelers = ref([])
const selectedFrequentTravelerIds = ref([])
const selectedTravelers = ref([])
const travelerDialogVisible = ref(false)
const editingTraveler = ref(null)

const travelerForm = reactive({
  name: '',
  gender: 'MALE',
  travelerType: 'ADULT',
  phone: '',
  idType: 'ID_CARD',
  idNumber: '',
  birthDate: ''
})

const travelerRules = computed(() => ({
  name: [
    { required: true, message: '请输入出行人姓名', trigger: 'blur' },
    { min: 2, max: 20, message: '姓名长度为 2-20 个字符', trigger: 'blur' }
  ],
  gender: [{ required: true, message: '请选择性别', trigger: 'change' }],
  travelerType: [{ required: true, message: '请选择类型', trigger: 'change' }],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的 11 位手机号', trigger: 'blur' }
  ],
  idType: [{ required: true, message: '请选择证件类型', trigger: 'change' }],
  idNumber: [
    { required: true, message: '请输入证件号码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (!/^\d{17}[\dXx]$/.test(value)) {
          callback(new Error('身份证号码格式不正确'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ],
  birthDate: [{ required: true, message: '请选择出生日期', trigger: 'change' }]
}))

const totalRequired = computed(() => (order.value?.adultCount || 0) + (order.value?.childCount || 0))
const adultRequired = computed(() => order.value?.adultCount || 0)
const childRequired = computed(() => order.value?.childCount || 0)
const selectedAdultCount = computed(() => selectedTravelers.value.filter(t => t.travelerType === 'ADULT').length)
const selectedChildCount = computed(() => selectedTravelers.value.filter(t => t.travelerType === 'CHILD').length)
const idNumberPlaceholder = computed(() => '请输入 18 位身份证号码')

const getIdTypeLabel = () => '身份证'

const maskIdNumber = (idNumber) => {
  if (!idNumber) return ''
  if (idNumber.length > 8) return `${idNumber.substring(0, 4)}****${idNumber.substring(idNumber.length - 4)}`
  return idNumber
}

const formatDate = (dateStr) => {
  if (!dateStr) return ''
  return String(dateStr).split('T')[0]
}

const formatPrice = (price) => Number(price || 0).toFixed(2)
const disableFutureDate = (date) => date.getTime() > Date.now()

const loadFrequentTravelers = async () => {
  try {
    const data = await getFrequentTravelers()
    frequentTravelers.value = data || []
  } catch (err) {
    console.error('获取常用出行人失败', err)
  }
}

const toggleFrequentTraveler = (traveler) => {
  const index = selectedFrequentTravelerIds.value.indexOf(traveler.id)
  if (index > -1) {
    selectedFrequentTravelerIds.value.splice(index, 1)
    selectedTravelers.value = selectedTravelers.value.filter(t => t.frequentTravelerId !== traveler.id)
    return
  }

  if (traveler.travelerType === 'ADULT' && selectedAdultCount.value >= adultRequired.value) {
    ElMessage.warning(`成人数量已满足要求（需要 ${adultRequired.value} 人）`)
    return
  }
  if (traveler.travelerType === 'CHILD' && childRequired.value === 0) {
    ElMessage.warning('该订单不需要儿童出行人')
    return
  }
  if (traveler.travelerType === 'CHILD' && selectedChildCount.value >= childRequired.value) {
    ElMessage.warning(`儿童数量已满足要求（需要 ${childRequired.value} 人）`)
    return
  }

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

const removeSelectedTraveler = (index) => {
  const traveler = selectedTravelers.value[index]
  if (traveler.frequentTravelerId) {
    const idIndex = selectedFrequentTravelerIds.value.indexOf(traveler.frequentTravelerId)
    if (idIndex > -1) selectedFrequentTravelerIds.value.splice(idIndex, 1)
  }
  selectedTravelers.value.splice(index, 1)
}

const resetTravelerForm = () => {
  Object.assign(travelerForm, {
    name: '',
    gender: 'MALE',
    travelerType: 'ADULT',
    phone: '',
    idType: 'ID_CARD',
    idNumber: '',
    birthDate: ''
  })
}

const showAddTravelerDialog = () => {
  editingTraveler.value = null
  resetTravelerForm()
  travelerDialogVisible.value = true
}

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
      const index = frequentTravelers.value.findIndex(t => t.id === editingTraveler.value.id)
      if (index > -1) frequentTravelers.value[index] = { ...frequentTravelers.value[index], ...data }
      const selectedIndex = selectedTravelers.value.findIndex(t => t.frequentTravelerId === editingTraveler.value.id)
      if (selectedIndex > -1) selectedTravelers.value[selectedIndex] = { ...selectedTravelers.value[selectedIndex], ...data }
    } else {
      const result = await saveFrequentTraveler(data)
      ElMessage.success('出行人已添加')
      if (result?.id) {
        const newTraveler = { id: result.id, ...data }
        frequentTravelers.value.unshift(newTraveler)
        toggleFrequentTraveler(newTraveler)
      }
    }
    travelerDialogVisible.value = false
  } catch (err) {
    if (err !== false) console.error('保存出行人失败', err)
  }
}

const handleDeleteFrequentTraveler = async (id) => {
  try {
    await ElMessageBox.confirm('确定要删除该常用出行人吗？', '删除确认', {
      confirmButtonText: '确认删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await deleteFrequentTravelerApi(id)
    frequentTravelers.value = frequentTravelers.value.filter(t => t.id !== id)
    selectedFrequentTravelerIds.value = selectedFrequentTravelerIds.value.filter(item => item !== id)
    selectedTravelers.value = selectedTravelers.value.filter(t => t.frequentTravelerId !== id)
    ElMessage.success('已删除')
  } catch (err) {
    if (err !== 'cancel') console.error('删除出行人失败', err)
  }
}

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
    contactForm.name = data.contactName || ''
    contactForm.phone = data.contactPhone || ''
  } catch (err) {
    console.error('加载订单信息失败:', err)
    error.value = err.message || '加载订单信息失败'
  } finally {
    loading.value = false
  }
}

const validateTravelers = () => {
  if (selectedTravelers.value.length < totalRequired.value) {
    ElMessage.warning(`请选择至少 ${totalRequired.value} 位出行人`)
    return false
  }
  if (selectedAdultCount.value < adultRequired.value) {
    ElMessage.warning(`请选择至少 ${adultRequired.value} 位成人`)
    return false
  }
  if (childRequired.value > 0 && selectedChildCount.value < childRequired.value) {
    ElMessage.warning(`请选择至少 ${childRequired.value} 位儿童`)
    return false
  }
  for (let i = 0; i < selectedTravelers.value.length; i++) {
    const t = selectedTravelers.value[i]
    if (!t.name || !t.phone || !/^1[3-9]\d{9}$/.test(t.phone) || !t.idType || !t.idNumber || !t.birthDate) {
      ElMessage.warning(`第 ${i + 1} 位出行人信息不完整`)
      return false
    }
  }
  return true
}

const handlePay = async () => {
  try {
    await contactFormRef.value?.validate()
  } catch {
    ElMessage.warning('请完善联系人信息')
    return
  }
  if (!validateTravelers()) return

  submitting.value = true
  try {
    await updateOrderContact(order.value.id, contactForm.name, contactForm.phone)
    const travelersData = selectedTravelers.value.map((t, index) => {
      const traveler = { ...t }
      delete traveler.frequentTravelerId
      return { ...traveler, travelerIndex: index + 1 }
    })
    await saveTravelers(order.value.id, order.value.orderNo, travelersData)
    router.push(`/tour-order-pay/${order.value.id}`)
  } catch (err) {
    console.error('保存信息失败:', err)
    ElMessage.error(err.message || '保存信息失败，请重试')
  } finally {
    submitting.value = false
  }
}

const goBack = () => router.back()

onMounted(async () => {
  await loadOrderInfo()
  await loadFrequentTravelers()
})
</script>

<style scoped>
.checkout-page {
  min-height: 100vh;
  background: #f3f6fb;
  color: #101828;
  font-family: "Source Han Sans", "Noto Sans CJK SC", "Microsoft YaHei", sans-serif;
  padding: 36px 20px 64px;
}

.state-panel {
  max-width: 900px;
  margin: 0 auto;
  padding: 34px;
  border: 1px solid #dde5f0;
  border-radius: 8px;
  background: #fff;
}

.state-panel p {
  color: #667085;
  text-align: center;
}

.checkout-shell {
  width: min(1220px, 100%);
  margin: 0 auto;
}

.checkout-hero {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 24px;
  padding: 26px 28px;
  border: 1px solid #dde5f0;
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 18px 44px rgba(16, 24, 40, 0.06);
}

.checkout-hero > div:first-child {
  flex: 1;
  min-width: 0;
  text-align: left;
}

.eyebrow {
  display: inline-flex;
  height: 24px;
  align-items: center;
  padding: 0 10px;
  border-radius: 999px;
  background: #e8f2ff;
  color: #155eef;
  font-size: 12px;
  font-weight: 800;
}

.checkout-hero h1 {
  margin: 12px 0 8px;
  font-size: 34px;
  font-weight: 900;
  letter-spacing: 0;
}

.checkout-hero p {
  margin: 0;
  color: #667085;
  max-width: 650px;
}

.step-strip {
  display: flex;
  flex-shrink: 0;
  gap: 10px;
  flex-wrap: wrap;
  justify-content: flex-end;
  padding: 10px;
  border: 1px solid #e4eaf3;
  border-radius: 8px;
  background: #f8fbff;
}

.step-strip span {
  min-height: 42px;
  padding: 0 16px;
  border-radius: 8px;
  background: #eef2f7;
  color: #667085;
  font-size: 13px;
  font-weight: 900;
  line-height: 42px;
  white-space: nowrap;
}

.step-strip .active {
  background: #155eef;
  color: #fff;
}

.checkout-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 340px;
  gap: 16px;
  margin-top: 16px;
  align-items: start;
}

.main-flow {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.panel-card,
.summary-card {
  border: 1px solid #dde5f0;
  border-radius: 8px;
  background: #fff;
}

.panel-card {
  padding: 20px;
}

.panel-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 16px;
  text-align: left;
}

.panel-head > div {
  text-align: left;
}

.panel-head span,
.summary-card header span,
.product-grid span,
.fee-list span,
.total-row span {
  display: block;
  color: #98a2b3;
  font-size: 12px;
  font-weight: 800;
}

.panel-head strong,
.summary-card header strong {
  display: block;
  margin-top: 4px;
  font-size: 18px;
  font-weight: 900;
}

.panel-head p {
  max-width: 360px;
  margin: 0;
  color: #667085;
  font-size: 13px;
  text-align: left;
}

.product-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.product-grid div,
.fee-list div {
  padding: 13px 14px;
  border: 1px solid #eef2f7;
  border-radius: 8px;
  background: #fbfcfe;
}

.product-grid .wide {
  grid-column: span 2;
}

.product-grid strong {
  display: block;
  margin-top: 8px;
  color: #344054;
  line-height: 1.5;
  word-break: break-word;
}

.contact-form,
.traveler-form {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px 16px;
}

.traveler-dialog :deep(.el-dialog) {
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 28px 70px rgba(16, 24, 40, 0.18);
}

.traveler-dialog :deep(.el-dialog__header) {
  padding: 22px 24px 14px;
  border-bottom: 1px solid #eef2f7;
  margin-right: 0;
}

.traveler-dialog :deep(.el-dialog__title) {
  color: #101828;
  font-size: 20px;
  font-weight: 900;
}

.traveler-dialog :deep(.el-dialog__body) {
  padding: 22px 24px 8px;
  background: linear-gradient(180deg, #fff 0%, #fbfcfe 100%);
}

.traveler-dialog :deep(.el-dialog__footer) {
  padding: 16px 24px 22px;
  border-top: 1px solid #eef2f7;
  background: #fff;
}

.traveler-dialog :deep(.el-dialog__footer .el-button) {
  min-width: 104px;
  height: 42px;
  border-radius: 8px;
  font-weight: 800;
}

.traveler-form :deep(.el-form-item) {
  margin-bottom: 0;
}

.traveler-form :deep(.el-form-item__label) {
  margin-bottom: 8px;
  color: #344054;
  font-size: 13px;
  font-weight: 900;
  line-height: 1.2;
}

.traveler-form :deep(.el-input__wrapper),
.traveler-form :deep(.el-select__wrapper) {
  min-height: 46px;
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 0 0 1px #d6deeb inset;
}

.traveler-form :deep(.el-input__wrapper.is-focus),
.traveler-form :deep(.el-select__wrapper.is-focused) {
  box-shadow: 0 0 0 1px #155eef inset, 0 0 0 3px rgba(21, 94, 239, 0.12);
}

.traveler-form :deep(.el-radio-group) {
  width: 100%;
  min-height: 46px;
  padding: 0 12px;
  border: 1px solid #d6deeb;
  border-radius: 8px;
  background: #fff;
  gap: 18px;
}

.quota-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 12px;
}

.quota-row span,
.quota-row strong {
  height: 30px;
  padding: 0 10px;
  border-radius: 999px;
  background: #eef2f7;
  color: #344054;
  font-size: 12px;
  font-weight: 900;
  line-height: 30px;
}

.quota-row strong {
  background: #eff4ff;
  color: #155eef;
}

.traveler-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.traveler-card {
  display: grid;
  grid-template-columns: 28px minmax(0, 1fr) auto;
  gap: 12px;
  align-items: center;
  padding: 14px;
  border: 1px solid #e4eaf3;
  border-radius: 8px;
  cursor: pointer;
}

.traveler-card.selected {
  border-color: #155eef;
  background: #edf5ff;
}

.traveler-title {
  display: flex;
  gap: 8px;
  align-items: center;
}

.traveler-info p {
  margin: 6px 0 0;
  color: #667085;
  font-size: 13px;
}

.traveler-actions {
  display: flex;
  gap: 8px;
}

.empty-inline {
  padding: 28px;
  border: 1px dashed #d6deeb;
  border-radius: 8px;
  background: #fbfcfe;
  color: #667085;
  text-align: center;
}

.selected-box {
  margin-top: 14px;
  padding: 14px;
  border: 1px solid #dbeafe;
  border-radius: 8px;
  background: #f8fbff;
}

.selected-box header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 10px;
}

.selected-box header span {
  color: #b54708;
  font-size: 13px;
  font-weight: 800;
}

.selected-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.selected-item {
  display: grid;
  grid-template-columns: 28px minmax(0, 1fr) 54px 120px auto;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border-radius: 8px;
  background: #fff;
}

.selected-item span {
  width: 28px;
  height: 28px;
  border-radius: 999px;
  background: #eff4ff;
  color: #155eef;
  font-weight: 900;
  line-height: 28px;
  text-align: center;
}

.selected-item em,
.selected-item small {
  color: #667085;
  font-style: normal;
  font-size: 13px;
}

.summary-panel {
  position: sticky;
  top: 16px;
}

.summary-card {
  padding: 20px;
}

.fee-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin: 16px 0;
}

.fee-list div {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.fee-list strong {
  color: #344054;
}

.total-row {
  padding-top: 16px;
  border-top: 1px solid #eef2f7;
}

.total-row strong {
  display: block;
  margin-top: 8px;
  color: #d92d20;
  font-size: 30px;
  font-weight: 900;
}

.security-note {
  margin: 14px 0;
  padding: 12px;
  border-radius: 8px;
  background: #f8fbff;
  color: #667085;
  font-size: 13px;
  line-height: 1.6;
}

.summary-actions {
  display: grid;
  grid-template-columns: 1fr 1.4fr;
  gap: 10px;
}

.summary-actions .el-button {
  height: 44px;
  border-radius: 8px;
  font-weight: 800;
}

:deep(.el-input__wrapper),
:deep(.el-select__wrapper) {
  min-height: 42px;
  border-radius: 8px;
}

@media (max-width: 980px) {
  .checkout-hero,
  .panel-head {
    align-items: flex-start;
    flex-direction: column;
  }

  .panel-head p {
    text-align: left;
  }

  .checkout-layout {
    grid-template-columns: 1fr;
  }

  .summary-panel {
    position: static;
  }
}

@media (max-width: 640px) {
  .checkout-page {
    padding: 24px 12px 48px;
  }

  .checkout-hero h1 {
    font-size: 28px;
  }

  .product-grid,
  .contact-form,
  .traveler-form {
    grid-template-columns: 1fr;
  }

  .product-grid .wide {
    grid-column: auto;
  }

  .traveler-card,
  .selected-item {
    grid-template-columns: 1fr;
  }

  .traveler-actions {
    justify-content: flex-start;
  }
}
</style>
