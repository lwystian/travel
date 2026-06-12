<template>
  <div class="coupon-admin">
    <section class="coupon-hero">
      <div>
        <span>Coupon Center</span>
        <h2>优惠券管理</h2>
        <p>配置满减、折扣、适用行程与发放范围，发券后用户会收到站内信提醒。</p>
      </div>
      <el-button type="primary" size="large" @click="openEditor()">
        <el-icon><Plus /></el-icon>
        新建优惠券
      </el-button>
    </section>

    <section class="coupon-toolbar">
      <el-input v-model="query.keyword" clearable placeholder="搜索券名称/券码" class="keyword-input" @keyup.enter="fetchCoupons" />
      <el-select v-model="query.status" clearable placeholder="状态" style="width: 140px">
        <el-option label="启用" :value="1" />
        <el-option label="停用" :value="0" />
      </el-select>
      <el-button @click="fetchCoupons">
        <el-icon><Search /></el-icon>
        查询
      </el-button>
    </section>

    <div v-if="loading" class="coupon-loading">
      <el-skeleton :rows="6" animated />
    </div>
    <el-empty v-else-if="coupons.length === 0" description="暂无优惠券" />
    <section v-else class="coupon-grid">
      <article v-for="item in coupons" :key="item.id" class="coupon-card" :class="{ disabled: item.status !== 1 }">
        <div class="coupon-card-main">
          <div class="coupon-value">
            <span v-if="item.discountType === 'RATE' || item.discountType === 'AGE_GROUP_RATE'">{{ formatRate(item.discountRate) }}</span>
            <span v-else>¥{{ formatMoney(item.discountAmount) }}</span>
            <small>{{ couponTypeText(item) }}</small>
          </div>
          <div class="coupon-info">
            <div class="coupon-title-row">
              <h3>{{ item.name }}</h3>
              <code>{{ item.code || '-' }}</code>
            </div>
            <p class="coupon-scope-summary">{{ detailedScopeText(item) }}</p>
            <div class="coupon-tags">
              <el-tag size="small" effect="plain">{{ scopeText(item) }}</el-tag>
              <el-tag size="small" type="warning" effect="plain">满 ¥{{ formatMoney(item.minOrderAmount) }} 可用</el-tag>
              <el-tag v-if="ageLimitText(item)" size="small" type="danger" effect="plain">{{ ageLimitText(item) }}</el-tag>
              <el-tag size="small" type="success" effect="plain">已领 {{ item.issuedQuantity || 0 }}</el-tag>
              <el-tag size="small" type="info" effect="plain">已用 {{ item.usedQuantity || 0 }}</el-tag>
              <el-tag v-if="isSoldOut(item)" size="small" type="danger" effect="plain">已领完</el-tag>
              <el-tag size="small" :type="item.autoReceive ? 'success' : 'info'" effect="plain">
                {{ item.autoReceive ? '前台可领' : '仅后台发放' }}
              </el-tag>
            </div>
          </div>
        </div>
        <div class="coupon-meta">
          <span>有效：{{ rangeText(item.validStartTime, item.validEndTime) }}</span>
          <span>库存：{{ item.issuedQuantity || 0 }} / {{ item.totalQuantity || '不限' }}</span>
        </div>
        <div class="coupon-actions">
          <el-switch
            :model-value="item.status"
            :active-value="1"
            :inactive-value="0"
            :disabled="isSoldOut(item)"
            @change="status => changeStatus(item, status)"
          />
          <el-button plain :disabled="item.status !== 1 || isSoldOut(item)" @click="openIssue(item)">
            <el-icon><Promotion /></el-icon>
            发放
          </el-button>
          <el-button type="primary" plain @click="openEditor(item)">
            <el-icon><EditPen /></el-icon>
            编辑
          </el-button>
          <el-button type="danger" plain @click="removeCoupon(item)">
            <el-icon><Delete /></el-icon>
            删除
          </el-button>
        </div>
      </article>
    </section>

    <div class="pager">
      <el-pagination
        v-model:current-page="query.currentPage"
        v-model:page-size="query.pageSize"
        :total="total"
        :page-sizes="[8, 12, 20, 40]"
        layout="total, sizes, prev, pager, next"
        @size-change="fetchCoupons"
        @current-change="fetchCoupons"
      />
    </div>

    <el-drawer
      v-model="editorVisible"
      :title="form.id ? '编辑优惠券' : '新建优惠券'"
      size="680px"
      append-to-body
      :close-on-click-modal="false"
      :close-on-press-escape="false"
      :before-close="handleEditorBeforeClose"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" class="coupon-form">
        <el-form-item label="优惠券名称" prop="name">
          <el-input v-model="form.name" maxlength="60" placeholder="如：暑期出游立减券" />
        </el-form-item>
        <el-form-item label="券码">
          <el-input v-model="form.code" placeholder="留空自动生成" />
        </el-form-item>
        <div class="form-grid">
          <el-form-item label="优惠形式" prop="discountType">
            <el-segmented v-model="form.discountType" :options="discountTypeOptions" />
          </el-form-item>
          <el-form-item label="使用门槛" prop="minOrderAmount">
            <el-input-number v-model="form.minOrderAmount" :min="0" :precision="2" style="width:100%" />
          </el-form-item>
          <section class="discount-rule-card">
            <div class="rule-card-head">
              <strong>优惠规则</strong>
              <span>可设置整单优惠，也可限定年龄后按符合条件的出行人逐人计算。</span>
            </div>
            <div class="rule-card-grid">
              <el-form-item v-if="form.discountType !== 'RATE'" label="减免金额" prop="discountAmount">
                <el-input-number v-model="form.discountAmount" :min="1" :precision="2" style="width:100%" />
              </el-form-item>
              <el-form-item v-else label="折扣比例" prop="discountRate">
                <el-input-number v-model="form.discountRate" :min="0.01" :max="0.99" :step="0.01" :precision="2" style="width:100%" />
              </el-form-item>
              <el-form-item v-if="form.discountType === 'RATE'" label="折扣封顶">
                <el-input-number v-model="form.maxDiscountAmount" :min="0" :precision="2" style="width:100%" />
              </el-form-item>
              <el-form-item label="年龄范围" class="age-range-field">
                <div class="age-range-input">
                  <el-input-number v-model="form.minAge" :min="0" :max="150" :precision="0" placeholder="不限" />
                  <span>至</span>
                  <el-input-number v-model="form.maxAge" :min="0" :max="150" :precision="0" placeholder="不限" />
                </div>
              </el-form-item>
            </div>
            <p class="form-tip">
              年龄不填时按订单总价优惠；填写年龄后，系统会按出发日期计算出行人年龄，并在对应成人/儿童单价上分别判断门槛与优惠。
            </p>
          </section>
          <el-form-item label="总发行量">
            <el-input-number v-model="form.totalQuantity" :min="0" :precision="0" style="width:100%" />
          </el-form-item>
          <el-form-item label="每人限领">
            <el-input-number v-model="form.perUserLimit" :min="1" :precision="0" style="width:100%" />
          </el-form-item>
        </div>

        <el-form-item label="适用范围">
          <el-radio-group v-model="form.scopeType" @change="handleScopeTypeChange">
            <el-radio-button value="ALL_TOUR">全部行程</el-radio-button>
            <el-radio-button value="TOUR">指定行程</el-radio-button>
            <el-radio-button value="TOUR_PACKAGE">指定套餐</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <section v-if="form.scopeType !== 'ALL_TOUR'" class="scope-panel">
          <div class="scope-panel-head">
            <el-input v-model="scopeKeyword" clearable placeholder="搜索行程名称/编号" />
            <span>已选 {{ selectedScopeValues.length }} 项</span>
          </div>
          <div class="scope-tour-list">
            <article v-for="tour in filteredScopeTours" :key="tour.id" class="scope-tour">
              <div class="scope-tour-main" @click="toggleTourScope(tour)">
                <el-checkbox :model-value="isTourSelected(tour)" @click.stop="toggleTourScope(tour)" />
                <div>
                  <strong>{{ tour.title || tour.code || `行程 ${tour.id}` }}</strong>
                  <span>{{ tour.code || '-' }}</span>
                </div>
              </div>
              <div v-if="form.scopeType === 'TOUR_PACKAGE'" class="scope-package-list">
                <button
                  v-for="pkg in tour.packages || []"
                  :key="pkg.id"
                  type="button"
                  :class="{ active: isPackageSelected(tour, pkg), disabled: pkg.status === 0 }"
                  :disabled="pkg.status === 0"
                  @click="togglePackageScope(tour, pkg)"
                >
                  {{ pkg.name || `套餐 ${pkg.id}` }}
                </button>
                <small v-if="!tour.packages || tour.packages.length === 0">暂无套餐</small>
              </div>
              <div class="scope-batch-list">
                <small>出发班期</small>
                <button
                  v-for="batch in tour.batches || []"
                  :key="batch.id"
                  type="button"
                  :class="{ active: isBatchSelected(tour, batch), disabled: batch.status !== '可报名' }"
                  :disabled="batch.status !== '可报名'"
                  @click="toggleBatchScope(tour, batch)"
                >
                  {{ batch.departureDate }}<span>{{ batch.status }}</span>
                </button>
                <em v-if="!tour.batches || tour.batches.length === 0">暂无班期，选择行程则默认全部班期</em>
              </div>
            </article>
          </div>
        </section>

        <el-form-item label="有效期">
          <el-date-picker
            v-model="validRange"
            type="datetimerange"
            value-format="YYYY-MM-DD HH:mm:ss"
            start-placeholder="留空为永久"
            end-placeholder="留空为永久"
            clearable
            style="width:100%"
          />
        </el-form-item>
        <el-form-item label="前台领取">
          <el-switch v-model="form.autoReceive" :active-value="1" :inactive-value="0" active-text="允许用户领取" inactive-text="仅后台发放" />
        </el-form-item>
        <el-form-item label="使用说明">
          <el-input v-model="form.description" type="textarea" :rows="3" maxlength="240" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="requestCloseEditor">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitCoupon">保存</el-button>
      </template>
    </el-drawer>

    <el-dialog
      v-model="issueVisible"
      title="发放优惠券"
      width="680px"
      :close-on-click-modal="false"
      :close-on-press-escape="false"
    >
      <el-radio-group v-model="issueForm.allUsers">
        <el-radio-button :value="true">全部启用用户</el-radio-button>
        <el-radio-button :value="false">指定用户</el-radio-button>
      </el-radio-group>
      <section v-if="!issueForm.allUsers" class="issue-user-panel">
        <div class="issue-search">
          <el-input v-model="issueKeyword" clearable placeholder="搜索手机号/用户名/昵称" @keyup.enter="fetchIssueUsers" />
          <el-button @click="fetchIssueUsers">搜索</el-button>
        </div>
        <div class="issue-user-list">
          <article
            v-for="user in issueUsers"
            :key="user.id"
            :class="{ active: issueForm.userIds.includes(user.id) }"
            @click="toggleIssueUser(user)"
          >
            <el-checkbox :model-value="issueForm.userIds.includes(user.id)" @click.stop="toggleIssueUser(user)" />
            <div class="issue-user-info">
              <strong>{{ user.nickname || user.username }}</strong>
              <span>{{ user.phone || '未绑定手机' }} · {{ user.username }}</span>
            </div>
          </article>
        </div>
        <el-pagination
          small
          layout="prev, pager, next"
          :total="issueUserTotal"
          :page-size="issueUserQuery.pageSize"
          :current-page="issueUserQuery.currentPage"
          @current-change="page => { issueUserQuery.currentPage = page; fetchIssueUsers() }"
        />
      </section>
      <template #footer>
        <el-button @click="issueVisible = false">取消</el-button>
        <el-button type="primary" :loading="issuing" @click="submitIssue">确认发放</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete, EditPen, Plus, Promotion, Search } from '@element-plus/icons-vue'
import {
  deleteCoupon,
  getCouponIssueUsers,
  getCouponScopeOptions,
  issueCoupon,
  pageCoupons,
  saveCoupon,
  updateCouponStatus
} from '@/api/coupon'

const loading = ref(false)
const saving = ref(false)
const issuing = ref(false)
const coupons = ref([])
const total = ref(0)
const editorVisible = ref(false)
const issueVisible = ref(false)
const currentCoupon = ref(null)
const formRef = ref(null)
const query = reactive({ currentPage: 1, pageSize: 8, keyword: '', status: '' })
const validRange = ref([])
const formSnapshot = ref('')
const scopeTours = ref([])
const scopeKeyword = ref('')
const selectedScopeValues = ref([])
const issueKeyword = ref('')
const issueUsers = ref([])
const issueUserTotal = ref(0)
const issueUserQuery = reactive({ currentPage: 1, pageSize: 8 })
const issueForm = reactive({ allUsers: true, userIds: [] })

const defaultForm = () => ({
  id: null,
  name: '',
  code: '',
  description: '',
  discountType: 'AMOUNT',
  discountAmount: 50,
  discountRate: 0.9,
  maxDiscountAmount: 0,
  minOrderAmount: 0,
  minAge: null,
  maxAge: null,
  scopeType: 'ALL_TOUR',
  scopeIds: '',
  totalQuantity: 0,
  perUserLimit: 1,
  receiveStartTime: '',
  receiveEndTime: '',
  validStartTime: '',
  validEndTime: '',
  autoReceive: 1,
  status: 1
})
const form = reactive(defaultForm())

const discountTypeOptions = [
  { label: '满减', value: 'AMOUNT' },
  { label: '折扣', value: 'RATE' }
]

const rules = {
  name: [{ required: true, message: '请输入优惠券名称', trigger: 'blur' }],
  discountType: [{ required: true, message: '请选择优惠形式', trigger: 'change' }],
  minOrderAmount: [{ required: true, message: '请输入门槛', trigger: 'blur' }]
}

const filteredScopeTours = computed(() => {
  const keyword = scopeKeyword.value.trim().toLowerCase()
  if (!keyword) return scopeTours.value
  return scopeTours.value.filter(item => {
    return String(item.title || '').toLowerCase().includes(keyword) ||
      String(item.code || '').toLowerCase().includes(keyword)
  })
})

const formatMoney = value => {
  const number = Number(value || 0)
  return Number.isInteger(number) ? String(number) : number.toFixed(2).replace(/\.?0+$/, '')
}
const formatRate = value => `${Number((Number(value || 1) * 10).toFixed(1)).toString()}折`
const formatDateTime = value => value ? String(value).replace('T', ' ').slice(0, 16) : '不限'
const rangeText = (start, end) => `${formatDateTime(start)} - ${formatDateTime(end)}`
const couponTypeText = item => {
  if (item.discountType === 'RATE' || item.discountType === 'AGE_GROUP_RATE') {
    return ageLimitText(item) ? '年龄折扣券' : '折扣券'
  }
  return ageLimitText(item) ? '年龄满减券' : '满减券'
}
const isSoldOut = item => Number(item.totalQuantity || 0) > 0 && Number(item.issuedQuantity || 0) >= Number(item.totalQuantity || 0)
const ageLimitText = item => {
  if (item?.minAge == null && item?.maxAge == null) return ''
  if (item.minAge != null && item.maxAge != null) return `${item.minAge}-${item.maxAge}岁可用`
  if (item.minAge != null) return `${item.minAge}岁以上可用`
  return `${item.maxAge}岁以下可用`
}
const scopeText = item => {
  if (item.scopeType === 'TOUR') return '指定行程'
  if (item.scopeType === 'TOUR_PACKAGE') return '指定套餐'
  if (item.scopeType === 'TOUR_TYPE') return '指定类型'
  return '全部行程'
}
const findScopeTour = (idOrCode) => {
  const value = String(idOrCode || '')
  return scopeTours.value.find(tour => String(tour.id) === value || String(tour.code) === value)
}
const detailedScopeText = item => {
  if (!item || item.scopeType === 'ALL_TOUR') return '全部行程可用'
  const values = String(item.scopeIds || '').split(/[,\s，、]+/).map(v => v.trim()).filter(Boolean)
  if (!values.length) return scopeText(item)
  if (item.scopeType === 'TOUR') {
    return values.map(value => {
      const { scope, date } = parseScopeValue(value)
      const tour = findScopeTour(scope)
      return `${tour?.title || tour?.code || scope}${date ? `（${date}）` : ''}`
    }).join('、')
  }
  if (item.scopeType === 'TOUR_PACKAGE') {
    return values.map(value => {
      const { scope, date } = parseScopeValue(value)
      const [tourValue, packageValue] = scope.split(':')
      const tour = findScopeTour(tourValue)
      const pkg = tour?.packages?.find(item => String(item.id) === String(packageValue))
      const tourName = tour?.title || tour?.code || tourValue
      const pkgName = pkg?.name || (packageValue ? `套餐 ${packageValue}` : '指定套餐')
      return `${tourName} / ${pkgName}${date ? `（${date}）` : ''}`
    }).join('、')
  }
  return values.join('、')
}

const getEditorSnapshot = () => JSON.stringify({
  form: { ...form },
  validRange: validRange.value || [],
  selectedScopeValues: selectedScopeValues.value
})
const hasUnsavedChanges = () => editorVisible.value && formSnapshot.value && formSnapshot.value !== getEditorSnapshot()

const fetchCoupons = async () => {
  loading.value = true
  try {
    const res = await pageCoupons({
      ...query,
      status: query.status === '' ? undefined : query.status
    })
    coupons.value = res?.records || []
    total.value = res?.total || 0
  } finally {
    loading.value = false
  }
}

const fetchScopeOptions = async () => {
  scopeTours.value = await getCouponScopeOptions() || []
}

const openEditor = async (item) => {
  await fetchScopeOptions()
  Object.assign(form, defaultForm(), item || {})
  if (form.discountType === 'AGE_PER_PERSON') {
    form.discountType = 'AMOUNT'
  } else if (form.discountType === 'AGE_GROUP_AMOUNT') {
    form.discountType = 'AMOUNT'
  } else if (form.discountType === 'AGE_GROUP_RATE') {
    form.discountType = 'RATE'
  }
  validRange.value = form.validStartTime && form.validEndTime ? [form.validStartTime, form.validEndTime] : []
  selectedScopeValues.value = form.scopeType === 'ALL_TOUR'
    ? []
    : String(form.scopeIds || '').split(/[,\s，、]+/).map(v => v.trim()).filter(Boolean)
  editorVisible.value = true
  requestAnimationFrame(() => {
    formSnapshot.value = getEditorSnapshot()
  })
}

const confirmDiscard = async () => {
  if (!hasUnsavedChanges()) return true
  try {
    await ElMessageBox.confirm('当前优惠券有未保存修改，确定放弃吗？', '未保存提醒', {
      confirmButtonText: '放弃修改',
      cancelButtonText: '继续编辑',
      type: 'warning'
    })
    return true
  } catch {
    return false
  }
}

const handleEditorBeforeClose = async (done) => {
  if (await confirmDiscard()) done()
}

const requestCloseEditor = async () => {
  if (await confirmDiscard()) editorVisible.value = false
}

const handleScopeTypeChange = () => {
  selectedScopeValues.value = []
  form.scopeIds = ''
}

const isTourSelected = tour => selectedScopeValues.value.includes(String(tour.id))
const isPackageSelected = (tour, pkg) => selectedScopeValues.value.includes(`${tour.id}:${pkg.id}`)
const parseScopeValue = value => {
  const text = String(value || '')
  const index = text.indexOf('@')
  return index > -1
    ? { scope: text.slice(0, index), date: text.slice(index + 1) }
    : { scope: text, date: '' }
}
const isBatchSelected = (tour, batch) => {
  const date = batch.departureDate
  if (form.scopeType === 'TOUR') {
    return selectedScopeValues.value.includes(`${tour.id}@${date}`)
  }
  if (form.scopeType === 'TOUR_PACKAGE') {
    return selectedScopeValues.value.some(value => value.startsWith(`${tour.id}:`) && value.endsWith(`@${date}`))
  }
  return false
}

const toggleTourScope = (tour) => {
  if (form.scopeType !== 'TOUR') return
  const value = String(tour.id)
  selectedScopeValues.value = isTourSelected(tour)
    ? selectedScopeValues.value.filter(item => item !== value)
    : [...selectedScopeValues.value, value]
}

const togglePackageScope = (tour, pkg) => {
  if (form.scopeType !== 'TOUR_PACKAGE' || pkg.status === 0) return
  const value = `${tour.id}:${pkg.id}`
  selectedScopeValues.value = isPackageSelected(tour, pkg)
    ? selectedScopeValues.value.filter(item => item !== value)
    : [...selectedScopeValues.value, value]
}

const toggleBatchScope = (tour, batch) => {
  if (batch.status !== '可报名') return
  const date = batch.departureDate
  if (form.scopeType === 'TOUR') {
    const plain = String(tour.id)
    const value = `${tour.id}@${date}`
    selectedScopeValues.value = selectedScopeValues.value.filter(item => item !== plain)
    selectedScopeValues.value = selectedScopeValues.value.includes(value)
      ? selectedScopeValues.value.filter(item => item !== value)
      : [...selectedScopeValues.value, value]
    return
  }
  if (form.scopeType === 'TOUR_PACKAGE') {
    const availablePackageIds = parseBatchPackageIds(batch)
    const packageIds = availablePackageIds.length
      ? availablePackageIds
      : (tour.packages || []).filter(pkg => pkg.status !== 0).map(pkg => Number(pkg.id))
    const values = packageIds.map(id => `${tour.id}:${id}@${date}`)
    const allSelected = values.length > 0 && values.every(value => selectedScopeValues.value.includes(value))
    selectedScopeValues.value = allSelected
      ? selectedScopeValues.value.filter(item => !values.includes(item))
      : Array.from(new Set([...selectedScopeValues.value, ...values]))
  }
}

const parseBatchPackageIds = (batch) => {
  const raw = batch?.packageIds
  if (Array.isArray(raw)) return raw.map(Number).filter(Boolean)
  const text = String(raw || '').trim()
  if (!text) return []
  try {
    if (text.startsWith('[')) {
      return JSON.parse(text).map(Number).filter(Boolean)
    }
  } catch (error) {
    return text.split(/[,\s，、]+/).map(Number).filter(Boolean)
  }
  return text.split(/[,\s，、]+/).map(Number).filter(Boolean)
}

const submitCoupon = async () => {
  await formRef.value?.validate()
  if (form.scopeType !== 'ALL_TOUR' && selectedScopeValues.value.length === 0) {
    ElMessage.warning('请选择适用范围')
    return
  }
  saving.value = true
  try {
    const data = {
      ...form,
      minAge: form.minAge === '' ? null : form.minAge,
      maxAge: form.maxAge === '' ? null : form.maxAge,
      scopeIds: form.scopeType === 'ALL_TOUR' ? '' : selectedScopeValues.value.join(','),
      receiveStartTime: null,
      receiveEndTime: null,
      validStartTime: validRange.value?.[0] || null,
      validEndTime: validRange.value?.[1] || null
    }
    await saveCoupon(data)
    ElMessage.success('保存成功')
    editorVisible.value = false
    await fetchCoupons()
  } finally {
    saving.value = false
  }
}

const changeStatus = async (item, status) => {
  await updateCouponStatus(item.id, status)
  item.status = status
  ElMessage.success(status === 1 ? '已启用' : '已停用，未使用券已作废')
  await fetchCoupons()
}

const removeCoupon = async (item) => {
  try {
    await ElMessageBox.confirm(`确定删除优惠券“${item.name}”吗？删除后未使用券会作废。`, '删除确认', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await deleteCoupon(item.id)
    ElMessage.success('已删除，未使用券已作废')
    await fetchCoupons()
  } catch (error) {
    if (error !== 'cancel') ElMessage.error(error.message || '删除失败')
  }
}

const fetchIssueUsers = async () => {
  const res = await getCouponIssueUsers({
    keyword: issueKeyword.value,
    currentPage: issueUserQuery.currentPage,
    pageSize: issueUserQuery.pageSize
  })
  issueUsers.value = res?.records || []
  issueUserTotal.value = res?.total || 0
}

const openIssue = async (item) => {
  currentCoupon.value = item
  issueForm.allUsers = true
  issueForm.userIds = []
  issueKeyword.value = ''
  issueUserQuery.currentPage = 1
  await fetchIssueUsers()
  issueVisible.value = true
}

const toggleIssueUser = (user) => {
  issueForm.userIds = issueForm.userIds.includes(user.id)
    ? issueForm.userIds.filter(id => id !== user.id)
    : [...issueForm.userIds, user.id]
}

const submitIssue = async () => {
  if (!issueForm.allUsers && issueForm.userIds.length === 0) {
    ElMessage.warning('请选择发放用户')
    return
  }
  issuing.value = true
  try {
    const res = await issueCoupon(currentCoupon.value.id, {
      allUsers: issueForm.allUsers,
      userIds: issueForm.userIds
    })
    ElMessage.success(`已发放 ${res?.issued || 0} 张优惠券`)
    issueVisible.value = false
    await fetchCoupons()
  } finally {
    issuing.value = false
  }
}

onMounted(async () => {
  await fetchScopeOptions()
  await fetchCoupons()
})
</script>

<style scoped>
.coupon-admin {
  padding: 22px;
  color: #243042;
}
.coupon-hero {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 24px 28px;
  background: linear-gradient(135deg, #fff7ed 0%, #eef6ff 100%);
  border: 1px solid #e5e7eb;
  border-radius: 8px;
}
.coupon-hero span {
  color: #f97316;
  font-weight: 700;
  font-size: 12px;
}
.coupon-hero h2 {
  margin: 8px 0;
  font-size: 26px;
}
.coupon-hero p {
  margin: 0;
  color: #64748b;
}
.coupon-toolbar {
  display: flex;
  gap: 12px;
  align-items: center;
  margin: 18px 0;
}
.keyword-input {
  width: 260px;
}
.coupon-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(390px, 1fr));
  gap: 16px;
}
.coupon-card {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 18px;
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.06);
}
.coupon-card.disabled {
  opacity: 0.62;
}
.coupon-card-main {
  display: flex;
  gap: 16px;
}
.coupon-value {
  width: 104px;
  min-height: 86px;
  border-radius: 8px;
  background: #f97316;
  color: #fff;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}
.coupon-value span {
  font-size: 28px;
  font-weight: 800;
}
.coupon-value small {
  margin-top: 3px;
}
.coupon-info {
  flex: 1;
  min-width: 0;
}
.coupon-title-row {
  display: flex;
  align-items: center;
  gap: 10px;
}
.coupon-title-row h3 {
  margin: 0;
  line-height: 24px;
  font-size: 18px;
}
.coupon-title-row code {
  display: inline-flex;
  align-items: center;
  min-height: 24px;
  padding: 2px 6px;
  border-radius: 4px;
  background: #f1f5f9;
  color: #475569;
  font-size: 12px;
}
.coupon-info p {
  min-height: 22px;
  margin: 8px 0 10px;
  color: #64748b;
}
.coupon-scope-summary {
  text-align: left;
  line-height: 1.5;
  word-break: break-word;
}
.coupon-tags {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}
.coupon-meta {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
  margin-top: 14px;
  padding-top: 12px;
  border-top: 1px dashed #e5e7eb;
  color: #64748b;
  font-size: 12px;
}
.coupon-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  align-items: center;
  margin-top: 14px;
}
.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 18px;
}
.coupon-form .form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}
.discount-rule-card {
  grid-column: 1 / -1;
  padding: 14px 16px;
  border: 1px solid #dbe7f3;
  border-radius: 8px;
  background: linear-gradient(180deg, #f8fbff 0%, #ffffff 100%);
}
.rule-card-head {
  display: flex;
  align-items: baseline;
  gap: 10px;
  margin-bottom: 12px;
}
.rule-card-head strong {
  flex: 0 0 auto;
  color: #0f172a;
  font-size: 14px;
}
.rule-card-head span {
  color: #8a99ad;
  font-size: 12px;
  line-height: 1.4;
}
.rule-card-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  column-gap: 14px;
  row-gap: 10px;
}
.rule-card-grid .el-form-item {
  margin-bottom: 0;
}
.rule-card-grid .age-range-field {
  grid-column: 1 / -1;
}
.age-range-input {
  display: grid;
  grid-template-columns: minmax(140px, 1fr) 32px minmax(140px, 1fr);
  align-items: center;
  gap: 10px;
  width: min(100%, 420px);
}
.age-range-input span {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 32px;
  color: #94a3b8;
  font-size: 12px;
  font-weight: 700;
  line-height: 1;
}
.form-tip {
  margin: 10px 0 0;
  color: #94a3b8;
  font-size: 11px;
  line-height: 1.6;
}
@media (max-width: 720px) {
  .coupon-form .form-grid,
  .rule-card-grid {
    grid-template-columns: 1fr;
  }
  .rule-card-head {
    display: block;
  }
  .rule-card-head span {
    display: block;
    margin-top: 4px;
  }
  .age-range-input {
    grid-template-columns: minmax(0, 1fr) 28px minmax(0, 1fr);
    width: 100%;
  }
}
.scope-panel {
  margin: 0 0 18px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  overflow: hidden;
}
.scope-panel-head,
.issue-search {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  background: #f8fafc;
}
.scope-panel-head span {
  white-space: nowrap;
  color: #64748b;
  font-size: 12px;
}
.scope-tour-list,
.issue-user-list {
  max-height: 320px;
  overflow: auto;
}
.scope-tour {
  padding: 12px;
  border-top: 1px solid #eef2f7;
}
.scope-tour-main,
.issue-user-list article {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
}
.scope-tour-main strong,
.issue-user-list strong {
  display: block;
  color: #1f2937;
}
.scope-tour-main span,
.issue-user-list span {
  display: block;
  margin-top: 2px;
  color: #64748b;
  font-size: 12px;
}
.scope-package-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin: 10px 0 0 32px;
}
.scope-package-list button {
  border: 1px solid #dbe3ef;
  background: #fff;
  border-radius: 6px;
  padding: 6px 10px;
  color: #475569;
  cursor: pointer;
}
.scope-package-list button.active {
  border-color: #f97316;
  background: #fff7ed;
  color: #c2410c;
  font-weight: 700;
}
.scope-package-list button.disabled {
  opacity: 0.45;
  cursor: not-allowed;
}
.scope-batch-list {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  margin: 10px 0 0 32px;
  padding-top: 10px;
  border-top: 1px dashed #e5e7eb;
}
.scope-batch-list small {
  color: #64748b;
  font-weight: 800;
}
.scope-batch-list button {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  border: 1px solid #dbe3ef;
  background: #fff;
  border-radius: 6px;
  padding: 6px 9px;
  color: #475569;
  cursor: pointer;
  font-weight: 700;
}
.scope-batch-list button span {
  color: #94a3b8;
  font-size: 11px;
  font-weight: 700;
}
.scope-batch-list button.active {
  border-color: #0d9488;
  background: #ecfdf5;
  color: #0f766e;
}
.scope-batch-list button.disabled {
  opacity: 0.45;
  cursor: not-allowed;
}
.scope-batch-list em {
  color: #94a3b8;
  font-size: 12px;
  font-style: normal;
}
.issue-user-panel {
  margin-top: 16px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  overflow: hidden;
}
.issue-user-list article {
  padding: 12px;
  border-top: 1px solid #eef2f7;
}
.issue-user-info {
  flex: 1;
  min-width: 0;
  text-align: left;
}
.issue-user-list strong,
.issue-user-list span {
  text-align: left;
}
.issue-user-list article.active {
  background: #fff7ed;
}
@media (max-width: 780px) {
  .coupon-hero,
  .coupon-toolbar,
  .coupon-meta {
    grid-template-columns: 1fr;
    flex-direction: column;
    align-items: stretch;
  }
  .keyword-input {
    width: 100%;
  }
  .coupon-grid,
  .coupon-form .form-grid {
    grid-template-columns: 1fr;
  }
}
</style>
