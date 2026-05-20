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
            <el-form-item label="模板Code" prop="templateCode">
              <el-input v-model="smsForm.templateCode" placeholder="例如：SMS_123456789" clearable />
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
        </el-tab-pane>

        <el-tab-pane label="SMTP邮箱" name="email">
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
        </el-tab-pane>

        <el-tab-pane label="极验验证" name="geetest">
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
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getEmailConfig, getGeetestConfig, getSmsConfig, saveEmailConfig, saveGeetestConfig, saveSmsConfig } from '@/api/authConfig'

const activeTab = ref('sms')
const smsFormRef = ref(null)
const emailFormRef = ref(null)
const geetestFormRef = ref(null)
const savingSms = ref(false)
const savingEmail = ref(false)
const savingGeetest = ref(false)

const smsForm = reactive({
  enabled: false,
  accessKeyId: '',
  accessKeySecret: '',
  signName: '',
  templateCode: '',
  regionId: 'cn-hangzhou',
  endpoint: 'dysmsapi.aliyuncs.com',
  codeExpireMinutes: 5,
  sendIntervalSeconds: 60,
  dailyLimit: 10,
  configured: false
})

const geetestForm = reactive({
  enabled: false,
  captchaId: '',
  captchaKey: '',
  configured: false
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

const loadConfigs = async () => {
  const [sms, email, geetest] = await Promise.all([getSmsConfig(), getEmailConfig(), getGeetestConfig()])
  Object.assign(smsForm, sms || {}, { accessKeySecret: '' })
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

.config-form {
  max-width: 760px;
  padding-top: 16px;
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
</style>
