<template>
  <div class="auth-config-page">
    <el-card class="header-card">
      <div class="header-content">
        <div>
          <h2>认证配置</h2>
          <p>集中管理阿里云短信、SMTP邮箱、极验验证和登录注册安全策略。</p>
        </div>
        <el-tag type="success" effect="dark">生产安全配置</el-tag>
      </div>
    </el-card>

    <el-card class="config-card">
      <el-tabs v-model="activeTab">
        <el-tab-pane label="阿里云短信" name="sms">
          <div class="config-workbench">
          <el-form ref="smsFormRef" :model="smsForm" :rules="smsRules" label-width="150px" class="config-form">
            <el-form-item label="启用短信服务">
              <el-switch v-model="smsForm.enabled" />
            </el-form-item>
            <el-form-item label="AccessKey ID" prop="accessKeyId">
              <el-input v-model="smsForm.accessKeyId" placeholder="请输入阿里云 AccessKey ID" clearable />
            </el-form-item>
            <el-form-item label="AccessKey Secret" prop="accessKeySecret">
              <el-input v-model="smsForm.accessKeySecret" type="password" show-password placeholder="留空表示不修改已保存密钥" clearable />
              <div v-if="smsForm.configured" class="field-hint">当前已保存密钥，出于安全原因不会明文回显。</div>
            </el-form-item>
            <el-form-item label="短信签名" prop="signName">
              <el-input v-model="smsForm.signName" placeholder="例如：侠客行国旅" clearable />
            </el-form-item>
            <el-form-item label="验证码模板Code" prop="templateCode">
              <el-input v-model="smsForm.templateCode" placeholder="例如：SMS_123456789" clearable />
            </el-form-item>
            <el-form-item label="用户订单通知模板">
              <el-input v-model="smsForm.orderUserTemplateCode" placeholder="支付成功后发给用户的短信模板 Code" clearable />
              <div class="field-hint">用于行程订单支付成功通知用户，推荐模板变量：${orderNo}、${tourName}、${departureDate}。</div>
            </el-form-item>
            <el-form-item label="管理员订单通知模板">
              <el-input v-model="smsForm.orderAdminTemplateCode" placeholder="支付成功后发给管理员的短信模板 Code" clearable />
              <div class="field-hint">用于提醒管理员跟进已付款订单，推荐模板变量：${orderNo}、${tourName}、${contactName}。</div>
            </el-form-item>
            <el-form-item label="Region ID">
              <el-input v-model="smsForm.regionId" placeholder="cn-hangzhou" clearable />
            </el-form-item>
            <el-form-item label="Endpoint">
              <el-input v-model="smsForm.endpoint" placeholder="dysmsapi.aliyuncs.com" clearable />
            </el-form-item>
            <el-form-item label="验证码有效期">
              <el-input-number v-model="smsForm.codeExpireMinutes" :min="1" :max="15" />
              <span class="unit">分钟</span>
            </el-form-item>
            <el-form-item label="发送间隔">
              <el-input-number v-model="smsForm.sendIntervalSeconds" :min="30" :max="300" />
              <span class="unit">秒</span>
            </el-form-item>
            <el-form-item label="单日上限">
              <el-input-number v-model="smsForm.dailyLimit" :min="1" :max="50" />
              <span class="unit">次/手机号</span>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="savingSms" @click="saveSms">保存短信配置</el-button>
            </el-form-item>
          </el-form>
          <aside class="test-panel">
            <div class="test-panel-head">
              <span>Configuration Check</span>
              <strong>短信完整测试</strong>
              <p>向指定手机号发送一次真实测试验证码，校验 AccessKey、签名、模板和阿里云接口调用链路。</p>
            </div>
            <el-form label-position="top" class="test-form">
              <el-form-item label="测试模板">
                <el-select
                  v-model="smsTest.templateCode"
                  class="template-select"
                  placeholder="请先保存短信配置后选择模板Code"
                  filterable
                  :disabled="smsTemplateOptions.length === 0"
                  @change="handleSmsTemplateChange"
                >
                  <el-option
                    v-for="item in smsTemplateOptions"
                    :key="item.value"
                    :label="`${item.name}：${item.value}`"
                    :value="item.value"
                  >
                    <div class="template-option-row">
                      <strong>{{ item.name }}</strong>
                      <span>{{ item.value }}</span>
                    </div>
                  </el-option>
                </el-select>
                <div v-else class="field-hint">模板列表来自已保存配置。修改左侧模板 Code 后，请先保存短信配置。</div>
              </el-form-item>
              <el-form-item label="测试手机号">
                <el-input v-model="smsTest.phone" maxlength="11" placeholder="请输入接收测试短信的手机号" clearable />
              </el-form-item>
              <el-button class="test-button" type="primary" :loading="testingSms" @click="runSmsTest">
                立即测试短信配置
              </el-button>
            </el-form>
            <div class="test-result" :class="smsTest.result?.success ? 'success' : smsTest.result ? 'danger' : 'idle'">
              <strong>{{ smsTest.result?.title || '等待测试' }}</strong>
              <p>{{ smsTest.result?.message || '保存配置后输入测试手机号，系统会发起一次真实短信发送测试。' }}</p>
              <ul v-if="smsTest.result?.checks?.length">
                <li v-for="item in smsTest.result.checks" :key="item">{{ item }}</li>
              </ul>
            </div>
          </aside>
          </div>
        </el-tab-pane>

        <el-tab-pane label="SMTP邮箱" name="email">
          <div class="config-workbench">
          <el-form ref="emailFormRef" :model="emailForm" :rules="emailRules" label-width="150px" class="config-form">
            <el-form-item label="启用邮件服务">
              <el-switch v-model="emailForm.enabled" />
            </el-form-item>
            <el-form-item label="SMTP Host" prop="host">
              <el-input v-model="emailForm.host" placeholder="例如：smtp.qq.com" clearable />
            </el-form-item>
            <el-form-item label="端口" prop="port">
              <el-input-number v-model="emailForm.port" :min="1" :max="65535" />
            </el-form-item>
            <el-form-item label="账号" prop="username">
              <el-input v-model="emailForm.username" placeholder="请输入SMTP账号" clearable />
            </el-form-item>
            <el-form-item label="授权码/密码" prop="password">
              <el-input v-model="emailForm.password" type="password" show-password placeholder="留空表示不修改已保存密钥" clearable />
              <div v-if="emailForm.configured" class="field-hint">当前已保存授权码，出于安全原因不会明文回显。</div>
            </el-form-item>
            <el-form-item label="发件邮箱" prop="fromEmail">
              <el-input v-model="emailForm.fromEmail" placeholder="例如：service@example.com" clearable />
            </el-form-item>
            <el-form-item label="协议">
              <el-select v-model="emailForm.protocol" placeholder="请选择协议">
                <el-option label="SMTPS" value="smtps" />
                <el-option label="SMTP" value="smtp" />
              </el-select>
            </el-form-item>
            <el-form-item label="启用SSL">
              <el-switch v-model="emailForm.sslEnabled" />
            </el-form-item>
            <el-form-item label="验证码有效期">
              <el-input-number v-model="emailForm.codeExpireMinutes" :min="1" :max="30" />
              <span class="unit">分钟</span>
            </el-form-item>
            <el-form-item label="发送间隔">
              <el-input-number v-model="emailForm.sendIntervalSeconds" :min="30" :max="300" />
              <span class="unit">秒</span>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="savingEmail" @click="saveEmail">保存邮箱配置</el-button>
            </el-form-item>
          </el-form>
          <aside class="test-panel">
            <div class="test-panel-head">
              <span>Delivery Check</span>
              <strong>SMTP完整测试</strong>
              <p>向指定邮箱发送测试邮件，校验 SMTP 主机、端口、账号授权、发件地址和投递能力。</p>
            </div>
            <el-form label-position="top" class="test-form">
              <el-form-item label="测试邮箱">
                <el-input v-model="emailTest.email" placeholder="请输入接收测试邮件的邮箱" clearable />
              </el-form-item>
              <el-button class="test-button" type="primary" :loading="testingEmail" @click="runEmailTest">
                立即测试SMTP配置
              </el-button>
            </el-form>
            <div class="test-result" :class="emailTest.result?.success ? 'success' : emailTest.result ? 'danger' : 'idle'">
              <strong>{{ emailTest.result?.title || '等待测试' }}</strong>
              <p>{{ emailTest.result?.message || '测试会发送一封带有侠客行国旅身份说明的邮件。' }}</p>
              <ul v-if="emailTest.result?.checks?.length">
                <li v-for="item in emailTest.result.checks" :key="item">{{ item }}</li>
              </ul>
            </div>
          </aside>
          </div>
        </el-tab-pane>

        <el-tab-pane label="极验验证" name="geetest">
          <div class="config-workbench">
          <el-form ref="geetestFormRef" :model="geetestForm" :rules="geetestRules" label-width="150px" class="config-form">
            <el-form-item label="启用极验验证">
              <el-switch v-model="geetestForm.enabled" />
            </el-form-item>
            <el-form-item label="Captcha ID" prop="captchaId">
              <el-input v-model="geetestForm.captchaId" placeholder="请输入极验 Captcha ID" clearable />
            </el-form-item>
            <el-form-item label="Captcha Key" prop="captchaKey">
              <el-input v-model="geetestForm.captchaKey" type="password" show-password placeholder="留空表示不修改已保存密钥" clearable />
              <div v-if="geetestForm.configured" class="field-hint">当前已保存密钥，出于安全原因不会明文回显。</div>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="savingGeetest" @click="saveGeetest">保存极验配置</el-button>
            </el-form-item>
          </el-form>
          <aside class="test-panel">
            <div class="test-panel-head">
              <span>Captcha Check</span>
              <strong>极验完整测试</strong>
              <p>调用极验注册接口获取挑战参数，校验 Captcha ID、Captcha Key 配置完整性和服务连通性。</p>
            </div>
            <el-button class="test-button" type="primary" :loading="testingGeetest" @click="runGeetestTest">
              立即测试极验配置
            </el-button>
            <div class="test-result" :class="geetestTest.result?.success ? 'success' : geetestTest.result ? 'danger' : 'idle'">
              <strong>{{ geetestTest.result?.title || '等待测试' }}</strong>
              <p>{{ geetestTest.result?.message || '保存配置后可检查极验服务是否可正常签发挑战参数。' }}</p>
              <ul v-if="geetestTest.result?.checks?.length">
                <li v-for="item in geetestTest.result.checks" :key="item">{{ item }}</li>
              </ul>
            </div>
          </aside>
          </div>
        </el-tab-pane>

        <el-tab-pane label="安全策略" name="policy">
          <div class="policy-grid">
            <div class="policy-item">
              <strong>短信验证码</strong>
              <span>按手机号和场景隔离，验证码哈希存储，过期后自动失效。</span>
            </div>
            <div class="policy-item">
              <strong>邮箱绑定</strong>
              <span>邮箱必须先通过SMTP验证码验证，验证通过后才会写入用户账户。</span>
            </div>
            <div class="policy-item">
              <strong>手机号变更</strong>
              <span>用户侧变更登录手机号必须验证新手机号，后台管理操作保留审计日志。</span>
            </div>
            <div class="policy-item">
              <strong>发送限频</strong>
              <span>默认60秒内不可重复发送，并限制单手机号每日获取次数。</span>
            </div>
            <div class="policy-item">
              <strong>密码复杂度</strong>
              <span>8-32位，必须同时包含大小写字母、数字和特殊符号。</span>
            </div>
            <div class="policy-item">
              <strong>用户协议</strong>
              <span>注册和登录均需勾选协议，避免无确认授权的账号行为。</span>
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  getEmailConfig,
  getGeetestConfig,
  getSmsConfig,
  saveEmailConfig,
  saveGeetestConfig,
  saveSmsConfig,
  testEmailConfig,
  testGeetestConfig,
  testSmsConfig
} from '@/api/authConfig'

const activeTab = ref('sms')
const smsFormRef = ref(null)
const emailFormRef = ref(null)
const geetestFormRef = ref(null)
const savingSms = ref(false)
const savingEmail = ref(false)
const savingGeetest = ref(false)
const testingSms = ref(false)
const testingEmail = ref(false)
const testingGeetest = ref(false)

const smsForm = reactive({
  enabled: false,
  accessKeyId: '',
  accessKeySecret: '',
  signName: '',
  templateCode: '',
  orderUserTemplateCode: '',
  orderAdminTemplateCode: '',
  regionId: 'cn-hangzhou',
  endpoint: 'dysmsapi.aliyuncs.com',
  codeExpireMinutes: 5,
  sendIntervalSeconds: 60,
  dailyLimit: 10,
  configured: false
})

const savedSmsTemplates = reactive({
  templateCode: '',
  orderUserTemplateCode: '',
  orderAdminTemplateCode: ''
})

const smsTest = reactive({
  phone: '',
  templateCode: '',
  result: null
})

const geetestForm = reactive({
  enabled: false,
  captchaId: '',
  captchaKey: '',
  configured: false
})

const geetestTest = reactive({
  result: null
})

const emailForm = reactive({
  enabled: false,
  host: 'smtp.qq.com',
  port: 465,
  username: '',
  password: '',
  fromEmail: '',
  protocol: 'smtps',
  sslEnabled: true,
  codeExpireMinutes: 10,
  sendIntervalSeconds: 60,
  configured: false
})

const emailTest = reactive({
  email: '',
  result: null
})

const smsRules = {
  accessKeyId: [{ required: true, message: '请输入 AccessKey ID', trigger: 'blur' }],
  signName: [{ required: true, message: '请输入短信签名', trigger: 'blur' }],
  templateCode: [{ required: true, message: '请输入模板Code', trigger: 'blur' }]
}

const geetestRules = {
  captchaId: [{ required: true, message: '请输入 Captcha ID', trigger: 'blur' }]
}

const emailRules = {
  host: [{ required: true, message: '请输入SMTP Host', trigger: 'blur' }],
  port: [{ required: true, message: '请输入端口', trigger: 'blur' }],
  username: [{ required: true, message: '请输入SMTP账号', trigger: 'blur' }],
  fromEmail: [
    { required: true, message: '请输入发件邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
  ]
}

const smsTemplateOptions = computed(() => {
  const options = [
    { name: '验证码模板', value: savedSmsTemplates.templateCode },
    { name: '用户订单通知模板', value: savedSmsTemplates.orderUserTemplateCode },
    { name: '管理员订单通知模板', value: savedSmsTemplates.orderAdminTemplateCode }
  ]
  return options.filter(item => item.value)
})

const handleSmsTemplateChange = (templateCode) => {
  smsTest.templateCode = templateCode
  smsTest.result = null
}

const loadConfigs = async () => {
  const [sms, email, geetest] = await Promise.all([getSmsConfig(), getEmailConfig(), getGeetestConfig()])
  Object.assign(smsForm, sms || {}, { accessKeySecret: '' })
  Object.assign(savedSmsTemplates, {
    templateCode: sms?.templateCode || '',
    orderUserTemplateCode: sms?.orderUserTemplateCode || '',
    orderAdminTemplateCode: sms?.orderAdminTemplateCode || ''
  })
  if (!smsTest.templateCode || !smsTemplateOptions.value.some(item => item.value === smsTest.templateCode)) {
    smsTest.templateCode = smsTemplateOptions.value[0]?.value || ''
  }
  Object.assign(emailForm, email || {}, { password: '' })
  Object.assign(geetestForm, geetest || {}, { captchaKey: '' })
}

const saveSms = async () => {
  const valid = await smsFormRef.value.validate().catch(() => false)
  if (!valid) return
  if (!smsForm.configured && !smsForm.accessKeySecret) {
    ElMessage.warning('首次配置请输入 AccessKey Secret')
    return
  }
  savingSms.value = true
  try {
    await saveSmsConfig({ ...smsForm }, { showDefaultMsg: false })
    ElMessage.success('短信配置已保存')
    await loadConfigs()
  } catch (err) {
    ElMessage.error(err.message || '保存失败')
  } finally {
    savingSms.value = false
  }
}

const saveEmail = async () => {
  const valid = await emailFormRef.value.validate().catch(() => false)
  if (!valid) return
  if (!emailForm.configured && !emailForm.password) {
    ElMessage.warning('首次配置请输入SMTP授权码或密码')
    return
  }
  savingEmail.value = true
  try {
    await saveEmailConfig({ ...emailForm }, { showDefaultMsg: false })
    ElMessage.success('邮箱配置已保存')
    await loadConfigs()
  } catch (err) {
    ElMessage.error(err.message || '保存失败')
  } finally {
    savingEmail.value = false
  }
}

const saveGeetest = async () => {
  const valid = await geetestFormRef.value.validate().catch(() => false)
  if (!valid) return
  if (!geetestForm.configured && !geetestForm.captchaKey) {
    ElMessage.warning('首次配置请输入 Captcha Key')
    return
  }
  savingGeetest.value = true
  try {
    await saveGeetestConfig({ ...geetestForm }, { showDefaultMsg: false })
    ElMessage.success('极验配置已保存')
    await loadConfigs()
  } catch (err) {
    ElMessage.error(err.message || '保存失败')
  } finally {
    savingGeetest.value = false
  }
}

const runSmsTest = async () => {
  if (!smsTest.templateCode) {
    ElMessage.warning('请选择要测试的短信模板Code')
    return
  }
  if (!/^1[3-9]\d{9}$/.test(smsTest.phone || '')) {
    ElMessage.warning('请输入正确的测试手机号')
    return
  }
  testingSms.value = true
  try {
    smsTest.result = await testSmsConfig({ phone: smsTest.phone, templateCode: smsTest.templateCode }, { showDefaultMsg: false })
    ElMessage.success('短信配置测试通过')
  } catch (err) {
    smsTest.result = {
      success: false,
      title: '短信测试失败',
      message: err.message || '请检查阿里云短信配置、签名、模板和账号权限。',
      checks: ['服务是否启用', 'AccessKey 是否有效', '签名和模板是否审核通过', '测试手机号是否可接收短信']
    }
    ElMessage.error(smsTest.result.message)
  } finally {
    testingSms.value = false
  }
}

const runEmailTest = async () => {
  const emailPattern = /^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/
  if (!emailPattern.test(emailTest.email || '')) {
    ElMessage.warning('请输入正确的测试邮箱')
    return
  }
  testingEmail.value = true
  try {
    emailTest.result = await testEmailConfig({ email: emailTest.email }, { showDefaultMsg: false })
    ElMessage.success('SMTP配置测试通过')
  } catch (err) {
    emailTest.result = {
      success: false,
      title: 'SMTP测试失败',
      message: err.message || '请检查SMTP主机、端口、授权码、SSL和发件邮箱。',
      checks: ['服务是否启用', 'SMTP主机和端口是否可达', '账号授权码是否正确', '发件邮箱是否与账号匹配']
    }
    ElMessage.error(emailTest.result.message)
  } finally {
    testingEmail.value = false
  }
}

const runGeetestTest = async () => {
  testingGeetest.value = true
  try {
    geetestTest.result = await testGeetestConfig({ showDefaultMsg: false })
    ElMessage.success('极验配置测试通过')
  } catch (err) {
    geetestTest.result = {
      success: false,
      title: '极验测试失败',
      message: err.message || '请检查Captcha ID、Captcha Key和服务器到极验接口的网络连通性。',
      checks: ['服务是否启用', 'Captcha ID 是否正确', 'Captcha Key 是否正确', '服务器是否能访问极验接口']
    }
    ElMessage.error(geetestTest.result.message)
  } finally {
    testingGeetest.value = false
  }
}

onMounted(loadConfigs)
</script>

<style scoped>
.auth-config-page {
  padding: 20px;
}

.header-card {
  margin-bottom: 20px;
  border: none;
  background: linear-gradient(135deg, #0ea5e9 0%, #10b981 100%);
}

.header-card :deep(.el-card__body) {
  color: #fff;
}

.header-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
}

.header-content h2 {
  margin: 0 0 8px;
  font-size: 24px;
}

.header-content p {
  margin: 0;
  opacity: 0.9;
}

.config-card {
  border-radius: 8px;
}

.config-workbench {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 360px;
  gap: 22px;
  align-items: start;
}

.config-form {
  padding-top: 16px;
}

.test-panel {
  position: sticky;
  top: 18px;
  margin-top: 16px;
  padding: 20px;
  border: 1px solid #dbeafe;
  border-radius: 8px;
  background:
    linear-gradient(180deg, rgba(239, 246, 255, 0.92), rgba(255, 255, 255, 0.98)),
    #fff;
  box-shadow: 0 18px 46px rgba(15, 23, 42, 0.08);
}

.test-panel-head {
  margin-bottom: 18px;
  text-align: left;
}

.test-panel-head span {
  display: inline-flex;
  height: 24px;
  align-items: center;
  padding: 0 9px;
  border-radius: 999px;
  background: #e0f2fe;
  color: #0369a1;
  font-size: 12px;
  font-weight: 800;
}

.test-panel-head strong {
  display: block;
  margin-top: 12px;
  color: #0f172a;
  font-size: 20px;
  font-weight: 900;
}

.test-panel-head p {
  margin: 8px 0 0;
  color: #64748b;
  font-size: 13px;
  line-height: 1.7;
}

.test-form {
  margin-bottom: 16px;
}

.test-form :deep(.el-form-item__label) {
  color: #334155;
  font-weight: 800;
}

.test-form :deep(.el-input__wrapper) {
  min-height: 42px;
  border-radius: 8px;
}

.template-select {
  width: 100%;
}

.template-select :deep(.el-select__wrapper) {
  min-height: 42px;
  border-radius: 8px;
}

.template-option-row {
  display: flex;
  min-width: 280px;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.template-option-row strong {
  flex: 0 0 auto;
  color: #0f172a;
  font-size: 13px;
  font-weight: 800;
}

.template-option-row span {
  min-width: 0;
  color: #0f766e;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 13px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.test-button {
  width: 100%;
  height: 42px;
  border-radius: 8px;
  font-weight: 900;
}

.test-result {
  padding: 16px;
  border-radius: 8px;
  border: 1px solid #e2e8f0;
  background: #f8fafc;
  text-align: left;
}

.test-result strong {
  display: block;
  color: #0f172a;
  font-size: 15px;
  font-weight: 900;
}

.test-result p {
  margin: 8px 0 0;
  color: #64748b;
  font-size: 13px;
  line-height: 1.7;
}

.test-result ul {
  margin: 12px 0 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.test-result li {
  position: relative;
  list-style: none;
  padding-left: 16px;
  color: #475569;
  font-size: 13px;
  line-height: 1.5;
}

.test-result li::before {
  position: absolute;
  left: 0;
  top: 8px;
  width: 6px;
  height: 6px;
  border-radius: 999px;
  background: #94a3b8;
  content: "";
}

.test-result.success {
  border-color: #99f6e4;
  background: #f0fdfa;
}

.test-result.success strong,
.test-result.success li {
  color: #0f766e;
}

.test-result.success li::before {
  background: #14b8a6;
}

.test-result.danger {
  border-color: #fecaca;
  background: #fff7f7;
}

.test-result.danger strong,
.test-result.danger li {
  color: #b91c1c;
}

.test-result.danger li::before {
  background: #ef4444;
}

.field-hint {
  margin-top: 6px;
  color: #909399;
  font-size: 12px;
}

.unit {
  margin-left: 10px;
  color: #606266;
}

.policy-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 16px;
  padding: 16px 0;
}

.policy-item {
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 18px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  background: #f8fafc;
}

.policy-item strong {
  color: #0f172a;
}

.policy-item span {
  color: #64748b;
  line-height: 1.7;
}

@media (max-width: 1180px) {
  .config-workbench {
    grid-template-columns: 1fr;
  }

  .test-panel {
    position: static;
  }
}
</style>
