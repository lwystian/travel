<template>
  <div class="payment-config-container">
    <el-card class="header-card">
      <div class="header-content">
        <div class="title">
          <el-icon><Wallet /></el-icon>
          <span>支付配置管理</span>
        </div>
        <p class="subtitle">配置和管理支付方式，包括支付宝、微信支付等</p>
      </div>
    </el-card>

    <el-card class="config-card">
      <template #header>
        <div class="card-header">
          <span>支付方式列表</span>
          <el-button
            type="primary"
            size="small"
            @click="handleAdd"
            :disabled="availablePaymentTypes.length === 0"
          >
            添加支付方式
          </el-button>
        </div>
        <div v-if="availablePaymentTypes.length === 0 && paymentList.length > 0" class="add-hint">
          已添加全部支付方式，如需重新添加请先删除现有配置
        </div>
      </template>

      <el-table :data="paymentList" v-loading="loading" stripe class="payment-table" :row-class-name="() => ''">
        <el-table-column prop="paymentName" label="支付方式" min-width="120" align="left">
          <template #default="{ row }">
            <span>{{ row.paymentName || row.paymentType }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="描述" min-width="150" align="left" show-overflow-tooltip />
        <el-table-column prop="isSandbox" label="环境" width="100" align="left">
          <template #default="{ row }">
            <el-tag :type="row.isSandbox ? 'warning' : 'success'" size="small">
              {{ row.isSandbox ? '沙箱' : '生产' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="enabled" label="状态" width="100" align="left">
          <template #default="{ row }">
            <el-switch
              v-model="row.enabled"
              :loading="row.toggling"
              @change="handleToggle(row)"
            />
          </template>
        </el-table-column>
        <el-table-column prop="configured" label="配置状态" width="100" align="left">
          <template #default="{ row }">
            <el-tag :type="row.configured ? 'success' : 'danger'" size="small">
              {{ row.configured ? '已配置' : '未配置' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right" align="left">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleEdit(row)">
              {{ row.configured ? '编辑' : '配置' }}
            </el-button>
            <el-button type="danger" link @click="handleDelete(row)">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && paymentList.length === 0" description="暂无支付配置" />
    </el-card>

    <!-- 支付配置对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑支付配置' : '添加支付方式'"
      width="700px"
      :close-on-click-modal="false"
    >
      <el-form
        ref="formRef"
        :model="form"
        :rules="isAlipay ? rules : {}"
        label-width="120px"
      >
        <el-form-item label="支付类型" prop="paymentType">
          <el-select
            v-model="form.paymentType"
            :disabled="isEdit"
            placeholder="请选择支付类型"
            style="width: 200px"
            @change="handlePaymentTypeChange"
          >
            <el-option value="alipay" label="支付宝" />
            <el-option value="wechat" label="微信支付" />
          </el-select>
        </el-form-item>

        <el-form-item label="支付名称" prop="paymentName">
          <el-input
            v-model="form.paymentName"
            :placeholder="'请输入' + getPaymentTypeName(form.paymentType) + '名称'"
          />
        </el-form-item>

        <el-form-item label="支付描述">
          <el-input v-model="form.description" placeholder="支付方式描述" />
        </el-form-item>

        <el-form-item label="环境配置">
          <el-select v-model="form.isSandbox" style="width: 150px">
            <el-option :value="false" label="生产环境" />
            <el-option :value="true" label="沙箱环境" />
          </el-select>
          <span style="margin-left: 12px; color: #909399; font-size: 12px;">
            {{ getGatewayUrlHint(form.paymentType) }}
          </span>
        </el-form-item>

        <el-form-item label="支付超时时间" prop="timeoutExpress">
          <el-input
            v-model="form.timeoutExpress"
            placeholder="如: 30m, 1h, 2h"
            clearable
            style="width: 200px"
          />
          <span style="margin-left: 12px; color: #909399; font-size: 12px;">
            订单支付超时时间，默认为 2h
          </span>
        </el-form-item>

        <!-- 支付宝配置区域 -->
        <template v-if="isAlipay">
          <el-divider content-position="left">密钥配置</el-divider>

          <el-form-item label="应用ID" prop="appId">
            <el-input
              v-model="form.appId"
              placeholder="请输入应用ID(APPID)"
              clearable
            />
          </el-form-item>

          <el-form-item label="商户私钥" prop="privateKey">
            <el-input
              v-model="form.privateKey"
              type="textarea"
              :rows="4"
              placeholder="请输入商户私钥（PKCS8格式）"
              clearable
            />
          </el-form-item>

          <el-form-item label="支付宝公钥" prop="alipayPublicKey">
            <el-input
              v-model="form.alipayPublicKey"
              type="textarea"
              :rows="4"
              placeholder="请输入支付宝公钥"
              clearable
            />
          </el-form-item>
        </template>

        <!-- 微信支付待开发提示 -->
        <template v-else>
          <el-divider content-position="left">密钥配置</el-divider>
          <div class="pending-config">
            <el-icon :size="32" color="#909399"><Warning /></el-icon>
            <p>微信支付配置功能正在开发中...</p>
            <p class="pending-hint">请稍后添加密钥配置</p>
          </div>
        </template>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave" :loading="saving">
          保存配置
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Wallet, Warning } from '@element-plus/icons-vue'
import {
  getAllPayments,
  getPaymentDetail,
  saveAlipayConfig,
  savePayment,
  togglePaymentEnabled,
  deletePaymentApi
} from '@/api/paymentConfig'

const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref(null)

const paymentList = ref([])
const currentRow = ref(null)

// 可用的支付类型
const allPaymentTypes = [
  { value: 'alipay', label: '支付宝' },
  { value: 'wechat', label: '微信支付' }
]

// 还未添加的支付类型
const availablePaymentTypes = computed(() => {
  const existingTypes = paymentList.value.map(p => p.paymentType)
  return allPaymentTypes.filter(t => !existingTypes.includes(t.value))
})

// 当前是否为支付宝
const isAlipay = computed(() => form.paymentType === 'alipay')

// 支付类型名称映射
const paymentTypeNames = {
  alipay: '支付宝',
  wechat: '微信支付'
}

// 网关URL提示映射
const gatewayUrlHints = {
  alipay: {
    sandbox: '网关：openapi-sandbox.dl.alipaydev.com',
    production: '网关：openapi.alipay.com'
  }
}

const form = reactive({
  paymentType: 'alipay',
  paymentName: '',
  description: '',
  appId: '',
  privateKey: '',
  alipayPublicKey: '',
  isSandbox: true,
  timeoutExpress: '2h'
})

const rules = {
  appId: [{ required: true, message: '请输入应用ID', trigger: 'blur' }],
  privateKey: [{ required: true, message: '请输入商户私钥', trigger: 'blur' }],
  alipayPublicKey: [{ required: true, message: '请输入支付宝公钥', trigger: 'blur' }]
}

const getPaymentTypeName = (type) => {
  return paymentTypeNames[type] || type
}

const getGatewayUrlHint = (type) => {
  if (type === 'alipay') {
    return form.isSandbox ? gatewayUrlHints.alipay.sandbox : gatewayUrlHints.alipay.production
  }
  return ''
}

// 支付类型变化时处理
const handlePaymentTypeChange = (type) => {
  // 清空密钥相关字段
  form.appId = ''
  form.privateKey = ''
  form.alipayPublicKey = ''
  // 自动填充支付名称
  form.paymentName = paymentTypeNames[type] || ''
}

const getPaymentIcon = (type) => {
  const icons = {
    alipay: 'Aliwangwang',
    wechat: 'Wechat'
  }
  return icons[type] || 'Wallet'
}

const getPaymentIconColor = (type) => {
  const colors = {
    alipay: '#1677FF',
    wechat: '#07C160'
  }
  return colors[type] || '#666'
}

const loadPayments = async () => {
  loading.value = true
  try {
    const res = await getAllPayments()
    paymentList.value = Array.isArray(res) ? res : []
    if (paymentList.value.length === 0) {
      ElMessage.info('暂无支付配置，请添加')
    }
  } catch (err) {
    console.error('加载支付配置失败:', err)
    ElMessage.error('加载支付配置失败: ' + (err.message || '未知错误'))
  } finally {
    loading.value = false
  }
}

// 添加支付方式
const handleAdd = () => {
  if (availablePaymentTypes.value.length === 0) {
    ElMessage.warning('已添加全部支付方式，请先删除现有配置后再添加')
    return
  }
  currentRow.value = null
  isEdit.value = false
  dialogVisible.value = true
  // 使用第一个可用支付类型
  const firstAvailable = availablePaymentTypes.value[0]
  Object.assign(form, {
    paymentType: firstAvailable.value,
    paymentName: firstAvailable.label,
    description: '',
    appId: '',
    privateKey: '',
    alipayPublicKey: '',
    isSandbox: true,
    timeoutExpress: '2h'
  })
}

// 编辑支付方式
const handleEdit = async (row) => {
  currentRow.value = row
  isEdit.value = true
  dialogVisible.value = true

  // 先填充基本信息
  form.paymentType = row.paymentType
  form.paymentName = row.paymentName
  form.description = row.description || ''
  form.isSandbox = row.isSandbox || false

  // 加载密钥配置
  try {
    const data = await getPaymentDetail(row.paymentType)
    form.appId = data.appId || ''
    form.privateKey = data.privateKey || ''
    form.alipayPublicKey = data.alipayPublicKey || ''
    form.timeoutExpress = data.timeoutExpress || '2h'
  } catch (err) {
    console.error('加载配置详情失败:', err)
    form.appId = ''
    form.privateKey = ''
    form.alipayPublicKey = ''
    form.timeoutExpress = '2h'
  }
}

// 删除支付方式
const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除支付方式「${row.paymentName || row.paymentType}」吗？`,
      '删除确认',
      {
        confirmButtonText: '删除',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    await deletePaymentApi(row.id, { showDefaultMsg: false })
    ElMessage.success('删除成功')
    loadPayments()
  } catch (err) {
    if (err !== 'cancel') {
      ElMessage.error(err.message || '删除失败')
    }
  }
}

const handleSave = async () => {
  // 非支付宝需要跳过表单验证
  if (isAlipay.value) {
    await formRef.value.validate(async (valid) => {
      if (!valid) return
      await doSave()
    })
  } else {
    await doSave()
  }
}

const doSave = async () => {
  saving.value = true
  try {
    // 保存支付方式基本信息
    const paymentData = {
      paymentType: form.paymentType,
      paymentName: form.paymentName,
      description: form.description,
      isSandbox: form.isSandbox
    }

    if (isEdit.value && currentRow.value?.id) {
      // 编辑模式 - 更新支付配置
      await savePayment({
        id: currentRow.value.id,
        ...paymentData
      }, { showDefaultMsg: false })
    } else {
      // 新增模式 - 保存支付配置
      await savePayment(paymentData, { showDefaultMsg: false })
    }

    // 仅支付宝保存密钥配置
    if (isAlipay.value) {
      await saveAlipayConfig({
        paymentType: form.paymentType,
        appId: form.appId,
        privateKey: form.privateKey,
        alipayPublicKey: form.alipayPublicKey,
        isSandbox: form.isSandbox,
        timeoutExpress: form.timeoutExpress || '2h'
      }, { showDefaultMsg: false })
    }

    ElMessage.success('保存成功')
    dialogVisible.value = false
    loadPayments()
  } catch (err) {
    console.error('保存失败:', err)
    ElMessage.error(err.message || '保存失败')
  } finally {
    saving.value = false
  }
}

const handleToggle = async (row) => {
  if (!row.configured) {
    ElMessage.warning('请先配置支付方式后再启用')
    row.enabled = false
    return
  }

  try {
    await togglePaymentEnabled(row.id, row.enabled, { showDefaultMsg: false })
    ElMessage.success(row.enabled ? '已启用' : '已禁用')
  } catch (err) {
    ElMessage.error(err.message || '操作失败')
    row.enabled = !row.enabled
  }
}

onMounted(() => {
  loadPayments()
})
</script>

<style scoped>
.payment-config-container {
  padding: 20px;
}

.header-card {
  margin-bottom: 20px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
}

.header-card :deep(.el-card__body) {
  color: white;
}

.header-content .title {
  font-size: 24px;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 10px;
}

.header-content .subtitle {
  margin-top: 8px;
  opacity: 0.9;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 16px;
  font-weight: 600;
}

.add-hint {
  font-size: 12px;
  color: #909399;
  margin-top: 8px;
}

.payment-name-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.pending-config {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
  color: #909399;
  text-align: center;
}

.pending-config p {
  margin: 10px 0 0 0;
  font-size: 14px;
}

.pending-config .pending-hint {
  color: #c0c4cc;
  font-size: 12px;
}

/* 表格样式：确保表头和内容左对齐 */
.payment-table :deep(th.el-table__cell) {
  text-align: left !important;
  background-color: #f5f7fa !important;
}

.payment-table :deep(.cell) {
  text-align: left !important;
  padding-left: 0px !important;
  padding-right: 0px !important;
}

.payment-table .el-table__body .el-table__row .el-table__cell .cell {
  padding-left: 0px !important;
}

.payment-table :deep(td.el-table__cell) {
  text-align: left !important;
}

.payment-table :deep(.el-table__header th) {
  text-align: left !important;
}

/* 移除空的 el-icon 前缀占位 */
.payment-table :deep(.cell .el-icon:empty) {
  display: none;
}

.payment-table :deep(.cell .el-icon + *) {
  margin-left: 0 !important;
}
</style>
