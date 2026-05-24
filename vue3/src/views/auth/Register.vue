<template>
  <div class="auth-page" :style="authBackgroundStyle">
    <main class="auth-card">
      <section class="form-panel">
        <div class="form-head">
          <h2>创建账号</h2>
          <p>完成手机号验证后即可开始使用旅行服务</p>
        </div>

        <el-form ref="formRef" :model="form" :rules="rules" class="auth-form">
          <el-form-item prop="phone">
            <el-input v-model="form.phone" :prefix-icon="Phone" maxlength="11" placeholder="手机号" @input="resetCaptcha" />
          </el-form-item>

          <GeetestBox
            ref="captchaRef"
            @verified="handleCaptchaVerified"
            @error="handleCaptchaError"
            @unavailable="handleCaptchaUnavailable"
          />

          <div class="sms-row">
            <el-form-item prop="smsCode" class="sms-input">
              <el-input v-model="form.smsCode" :prefix-icon="Key" maxlength="6" placeholder="短信验证码" />
            </el-form-item>
            <el-button :loading="sending" :disabled="countdown > 0" @click="sendRegisterCode">
              {{ countdown > 0 ? `${countdown}s` : '发送验证码' }}
            </el-button>
          </div>

          <el-form-item prop="password">
            <el-input v-model="form.password" :prefix-icon="Lock" type="password" show-password placeholder="设置密码：8-32位，含大小写、数字和特殊符号" />
          </el-form-item>

          <div class="password-strength">
            <span :class="{ active: strength >= 1 }">长度</span>
            <span :class="{ active: strength >= 2 }">组合</span>
            <span :class="{ active: strength >= 3 }">安全</span>
          </div>

          <el-form-item prop="confirmPassword">
            <el-input v-model="form.confirmPassword" :prefix-icon="Lock" type="password" show-password placeholder="再次输入密码" />
          </el-form-item>
        </el-form>

        <div class="agreement-row">
          <el-checkbox v-model="form.agreementAccepted" />
          <span>我已阅读并同意</span>
          <button type="button" @click="agreementVisible = true">用户服务协议与隐私保护条款</button>
        </div>

        <el-button class="submit-button" type="primary" :loading="loading" @click="submitRegister">
          注册
        </el-button>

        <div class="switch-link">
          已有账号？
          <router-link to="/login">去登录</router-link>
        </div>
      </section>

      <section class="visual-panel" :style="authBackgroundStyle">
        <div class="brand">
          <div class="brand-logo">侠客行</div>
          <div>
            <h1>旅游服务平台</h1>
            <p>Travel Service Platform</p>
          </div>
        </div>
        <div class="visual-copy">
          <h2>一个手机号，连接你的攻略、收藏、订单与站内消息</h2>
          <p>注册阶段完成短信和滑动验证，账号创建后自动接收安全提醒与业务通知。</p>
        </div>
        <div class="security-row">
          <span>手机号唯一绑定</span>
          <span>密码强度校验</span>
          <span>通知可追踪</span>
        </div>
      </section>
    </main>

    <AgreementDialog v-model="agreementVisible" @accept="form.agreementAccepted = true" />
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Key, Lock, Phone } from '@element-plus/icons-vue'
import { registerByPhone, sendSmsCode } from '@/api/auth'
import { encryptPassword } from '@/utils/passwordCrypto'
import AgreementDialog from '@/components/auth/AgreementDialog.vue'
import GeetestBox from '@/components/auth/GeetestBox.vue'
import { useSiteAssets, getAssetUrl } from '@/utils/siteAssets'
import noImage from '@/assets/images/no-image.png'

const router = useRouter()
const { siteAssets, loadSiteAssets } = useSiteAssets()
const formRef = ref(null)
const captchaRef = ref(null)
const loading = ref(false)
const sending = ref(false)
const countdown = ref(0)
const agreementVisible = ref(false)
const geetestValidate = ref(null)
const geetestRequired = ref(true)
const authBackgroundStyle = computed(() => ({
  '--auth-bg-url': `url("${getAssetUrl(siteAssets.value.authBackgroundUrl, noImage)}")`
}))

const form = reactive({
  phone: '',
  smsCode: '',
  password: '',
  confirmPassword: '',
  agreementAccepted: false
})

const passwordPattern = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^A-Za-z0-9]).{8,32}$/

const strength = computed(() => {
  if (!form.password) return 0
  if (passwordPattern.test(form.password)) return 3
  if (form.password.length >= 8 && /[A-Za-z]/.test(form.password) && /\d/.test(form.password)) return 2
  if (form.password.length >= 8) return 1
  return 0
})

const validatePassword = (rule, value, callback) => {
  if (!value) {
    callback(new Error('请设置密码'))
    return
  }
  if (!passwordPattern.test(value)) {
    callback(new Error('密码需8-32位，并包含大小写字母、数字和特殊符号'))
    return
  }
  callback()
}

const validateConfirmPassword = (rule, value, callback) => {
  if (!value) {
    callback(new Error('请再次输入密码'))
    return
  }
  if (value !== form.password) {
    callback(new Error('两次输入的密码不一致'))
    return
  }
  callback()
}

const rules = {
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }
  ],
  smsCode: [
    { required: true, message: '请输入短信验证码', trigger: 'blur' },
    { pattern: /^\d{6}$/, message: '验证码为6位数字', trigger: 'blur' }
  ],
  password: [{ validator: validatePassword, trigger: 'blur' }],
  confirmPassword: [{ validator: validateConfirmPassword, trigger: 'blur' }]
}

const resetCaptcha = () => {
  geetestValidate.value = null
  captchaRef.value?.resetCaptcha()
}

const handleCaptchaVerified = validate => {
  geetestValidate.value = validate
  geetestRequired.value = Boolean(validate) || geetestRequired.value
}

const handleCaptchaError = message => {
  geetestValidate.value = null
  ElMessage.error(message)
}

const handleCaptchaUnavailable = () => {
  geetestValidate.value = null
  geetestRequired.value = false
}

const startCountdown = () => {
  countdown.value = 60
  const timer = window.setInterval(() => {
    countdown.value -= 1
    if (countdown.value <= 0) {
      window.clearInterval(timer)
    }
  }, 1000)
}

const sendRegisterCode = async () => {
  const validPhone = await formRef.value?.validateField('phone').catch(() => false)
  if (validPhone === false) return
  if (geetestRequired.value && !geetestValidate.value) {
    ElMessage.warning('请先完成上方滑动验证')
    return
  }
  sending.value = true
  try {
    await sendSmsCode({ phone: form.phone, scene: 'REGISTER', geetest: geetestValidate.value }, { showDefaultMsg: false })
    ElMessage.success('验证码已发送，请注意查收')
    startCountdown()
    resetCaptcha()
  } catch (err) {
    ElMessage.error(err.message || '验证码发送失败')
    resetCaptcha()
  } finally {
    sending.value = false
  }
}

const submitRegister = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  if (!form.agreementAccepted) {
    ElMessage.warning('请先阅读并同意用户协议')
    return
  }

  loading.value = true
  try {
    const encryptedPassword = await encryptPassword(form.password)
    const encryptedConfirmPassword = await encryptPassword(form.confirmPassword)
    await registerByPhone({
      phone: form.phone,
      smsCode: form.smsCode,
      encryptedPassword,
      encryptedConfirmPassword,
      agreementAccepted: form.agreementAccepted
    }, { showDefaultMsg: false })
    ElMessage.success('注册成功，请登录')
    router.push('/login')
  } catch (err) {
    ElMessage.error(err.message || '注册失败')
  } finally {
    loading.value = false
  }
}

onMounted(loadSiteAssets)
</script>

<style scoped>
.auth-page {
  min-height: 100vh;
  padding: 22px 32px 32px;
  display: flex;
  align-items: flex-start;
  justify-content: center;
  background:
    linear-gradient(135deg, rgba(13, 36, 55, 0.6), rgba(8, 78, 93, 0.42)),
    var(--auth-bg-url) center/cover no-repeat;
}

.auth-card {
  width: min(980px, 100%);
  height: min(660px, calc(100vh - 54px));
  display: grid;
  grid-template-columns: 0.95fr 1.05fr;
  border-radius: 18px;
  overflow: hidden;
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 28px 80px rgba(15, 23, 42, 0.32);
}

.form-panel {
  padding: 28px 44px 56px;
  display: flex;
  flex-direction: column;
  justify-content: flex-start;
}

.form-head h2 {
  margin: 0;
  color: #0f172a;
  font-size: 30px;
  letter-spacing: 0;
}

.form-head p {
  margin: 8px 0 24px;
  color: #64748b;
  line-height: 1.7;
}

.auth-form :deep(.el-form-item) {
  margin-bottom: 16px;
}

.auth-form :deep(.el-input__wrapper) {
  min-height: 46px;
  border-radius: 12px;
  box-shadow: 0 0 0 1px #e2e8f0 inset;
}

.auth-form :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px #0ea5e9 inset, 0 0 0 3px rgba(14, 165, 233, 0.12);
}

.sms-row {
  display: grid;
  grid-template-columns: 1fr 118px;
  gap: 10px;
  align-items: start;
}

.sms-row .el-button {
  height: 46px;
  border-radius: 12px;
}

.sms-input {
  margin-bottom: 0;
}

.password-strength {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
  margin: -6px 0 16px;
}

.password-strength span {
  height: 6px;
  overflow: hidden;
  color: transparent;
  border-radius: 999px;
  background: #e2e8f0;
}

.password-strength span.active {
  background: #10b981;
}

.agreement-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 2px 0 22px;
  color: #64748b;
  font-size: 13px;
  flex-wrap: wrap;
}

.agreement-row button {
  padding: 0;
  border: 0;
  background: transparent;
  color: #0284c7;
  cursor: pointer;
}

.submit-button {
  width: 100%;
  height: 46px;
  border-radius: 12px;
  font-size: 16px;
  font-weight: 700;
}

.switch-link {
  margin-top: 14px;
  margin-bottom: 4px;
  text-align: center;
  color: #64748b;
}

.switch-link a {
  color: #0284c7;
  text-decoration: none;
  font-weight: 700;
}

.visual-panel {
  padding: 46px;
  color: #fff;
  background:
    linear-gradient(160deg, rgba(37, 99, 235, 0.86), rgba(20, 184, 166, 0.78)),
    var(--auth-bg-url) center/cover no-repeat;
  display: flex;
  flex-direction: column;
}

.brand {
  display: flex;
  align-items: center;
  gap: 14px;
}

.brand-logo {
  min-width: 86px;
  height: 44px;
  padding: 0 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.2);
  font-size: 20px;
  font-weight: 700;
  white-space: nowrap;
}

.brand h1,
.brand p,
.visual-copy h2,
.visual-copy p {
  margin: 0;
}

.brand h1 {
  font-size: 22px;
}

.brand p {
  margin-top: 3px;
  opacity: 0.78;
  font-size: 13px;
}

.visual-copy {
  margin-top: 150px;
  max-width: 460px;
}

.visual-copy h2 {
  font-size: 34px;
  line-height: 1.25;
  letter-spacing: 0;
}

.visual-copy p {
  margin-top: 16px;
  line-height: 1.8;
  opacity: 0.9;
}

.security-row {
  display: flex;
  gap: 10px;
  margin-top: 28px;
  flex-wrap: wrap;
}

.security-row span {
  padding: 8px 12px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.18);
  font-size: 13px;
}

@media (max-width: 820px) {
  .auth-page {
    padding: 18px;
    align-items: flex-start;
  }

  .auth-card {
    grid-template-columns: 1fr;
    height: auto;
    min-height: 0;
  }

  .visual-panel {
    min-height: 260px;
    padding: 30px;
    order: -1;
  }

  .visual-copy h2 {
    font-size: 26px;
  }

  .form-panel {
    padding: 32px 24px;
  }
}
</style>
