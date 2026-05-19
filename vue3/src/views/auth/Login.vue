<template>
  <div class="auth-page">
    <main class="auth-card">
      <section class="visual-panel">
        <div class="brand">
          <div class="brand-logo">旅</div>
          <div>
            <h1>旅游服务平台</h1>
            <p>Travel Service Platform</p>
          </div>
        </div>
        <div class="visual-copy">
          <h2>安全进入你的旅行账户</h2>
          <p>支持手机号密码登录和短信验证码登录，登录、短信、验证行为都会进入安全审计链路。</p>
        </div>
        <div class="security-row">
          <span>手机认证</span>
          <span>极验验证</span>
          <span>安全审计</span>
        </div>
      </section>

      <section class="form-panel">
        <div class="form-head">
          <h2>登录</h2>
          <p>使用手机号进入你的旅行账户</p>
        </div>

        <div class="mode-switch">
          <button :class="{ active: activeTab === 'password' }" @click="switchTab('password')">密码登录</button>
          <button :class="{ active: activeTab === 'code' }" @click="switchTab('code')">验证码登录</button>
        </div>

        <div class="form-body">
          <transition name="auth-mode" mode="out-in">
            <el-form
              v-if="activeTab === 'password'"
              ref="passwordFormRef"
              key="password"
              :model="passwordForm"
              :rules="passwordRules"
              class="auth-form password-form"
            >
              <el-form-item prop="phone">
                <el-input v-model="passwordForm.phone" :prefix-icon="Phone" maxlength="11" placeholder="手机号" />
              </el-form-item>
              <el-form-item prop="password">
                <el-input v-model="passwordForm.password" :prefix-icon="Lock" type="password" show-password placeholder="密码" />
              </el-form-item>
            </el-form>

            <el-form
              v-else
              ref="codeFormRef"
              key="code"
              :model="codeForm"
              :rules="codeRules"
              class="auth-form"
            >
              <el-form-item prop="phone">
                <el-input v-model="codeForm.phone" :prefix-icon="Phone" maxlength="11" placeholder="手机号" @input="resetCaptcha" />
              </el-form-item>

              <GeetestBox
                ref="captchaRef"
                @verified="handleCaptchaVerified"
                @error="handleCaptchaError"
                @unavailable="handleCaptchaUnavailable"
              />

              <div class="sms-row">
                <el-form-item prop="smsCode" class="sms-input">
                  <el-input v-model="codeForm.smsCode" :prefix-icon="Key" maxlength="6" placeholder="短信验证码" />
                </el-form-item>
                <el-button :loading="sending" :disabled="countdown > 0" @click="sendLoginCode">
                  {{ countdown > 0 ? `${countdown}s` : '发送验证码' }}
                </el-button>
              </div>
            </el-form>
          </transition>
        </div>

        <div class="agreement-row">
          <el-checkbox v-model="agreementAccepted" />
          <span>我已阅读并同意</span>
          <button type="button" @click="agreementVisible = true">用户服务协议与隐私保护条款</button>
        </div>

        <el-button class="submit-button" type="primary" :loading="loading" @click="submitLogin">
          登录
        </el-button>

        <div class="switch-link">
          还没有账号？
          <router-link to="/register">立即注册</router-link>
        </div>
      </section>
    </main>

    <AgreementDialog v-model="agreementVisible" @accept="agreementAccepted = true" />
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Key, Lock, Phone } from '@element-plus/icons-vue'
import { loginByPhoneCode, loginByPhonePassword, sendSmsCode } from '@/api/auth'
import { encryptPassword } from '@/utils/passwordCrypto'
import { useUserStore } from '@/store/user'
import AgreementDialog from '@/components/auth/AgreementDialog.vue'
import GeetestBox from '@/components/auth/GeetestBox.vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const activeTab = ref('password')
const loading = ref(false)
const sending = ref(false)
const countdown = ref(0)
const agreementAccepted = ref(false)
const agreementVisible = ref(false)
const geetestValidate = ref(null)
const geetestRequired = ref(true)
const passwordFormRef = ref(null)
const codeFormRef = ref(null)
const captchaRef = ref(null)

const passwordForm = reactive({
  phone: '',
  password: ''
})

const codeForm = reactive({
  phone: '',
  smsCode: ''
})

const phoneRule = [
  { required: true, message: '请输入手机号', trigger: 'blur' },
  { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }
]

const passwordRules = {
  phone: phoneRule,
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const codeRules = {
  phone: phoneRule,
  smsCode: [
    { required: true, message: '请输入短信验证码', trigger: 'blur' },
    { pattern: /^\d{6}$/, message: '验证码为6位数字', trigger: 'blur' }
  ]
}

const switchTab = tab => {
  activeTab.value = tab
  geetestValidate.value = null
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

const sendLoginCode = async () => {
  const validPhone = await codeFormRef.value?.validateField('phone').catch(() => false)
  if (validPhone === false) return
  if (geetestRequired.value && !geetestValidate.value) {
    ElMessage.warning('请先完成上方滑动验证')
    return
  }
  sending.value = true
  try {
    await sendSmsCode({ phone: codeForm.phone, scene: 'LOGIN', geetest: geetestValidate.value }, { showDefaultMsg: false })
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

const submitLogin = async () => {
  const formRef = activeTab.value === 'password' ? passwordFormRef.value : codeFormRef.value
  const valid = await formRef.validate().catch(() => false)
  if (!valid) return
  if (!agreementAccepted.value) {
    ElMessage.warning('请先阅读并同意用户协议')
    return
  }

  loading.value = true
  try {
    const encryptedPassword = activeTab.value === 'password'
      ? await encryptPassword(passwordForm.password)
      : ''
    const data = activeTab.value === 'password'
      ? await loginByPhonePassword({
        phone: passwordForm.phone,
        encryptedPassword,
        agreementAccepted: true
      }, { showDefaultMsg: false })
      : await loginByPhoneCode({ ...codeForm, agreementAccepted: true }, { showDefaultMsg: false })
    userStore.setUserInfo(data)
    ElMessage.success('登录成功')
    const redirect = route.query.redirect || (data.roleCode === 'ADMIN' ? '/back/dashboard' : '/')
    router.push(redirect)
  } catch (err) {
    ElMessage.error(err.message || '登录失败')
  } finally {
    loading.value = false
  }
}
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
    url('@/assets/bg.jpg') center/cover no-repeat;
}

.auth-card {
  width: min(980px, 100%);
  height: min(660px, calc(100vh - 54px));
  display: grid;
  grid-template-columns: 1.05fr 0.95fr;
  border-radius: 18px;
  overflow: hidden;
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 28px 80px rgba(15, 23, 42, 0.32);
}

.visual-panel {
  padding: 46px;
  color: #fff;
  background:
    linear-gradient(160deg, rgba(14, 116, 144, 0.92), rgba(22, 163, 74, 0.78)),
    url('@/assets/bg.jpg') center/cover no-repeat;
  display: flex;
  flex-direction: column;
}

.brand {
  display: flex;
  align-items: center;
  gap: 14px;
}

.brand-logo {
  width: 48px;
  height: 48px;
  display: grid;
  place-items: center;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.2);
  font-size: 24px;
  font-weight: 700;
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
  max-width: 430px;
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

.form-panel {
  padding: 42px 44px;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.form-head h2 {
  margin: 0;
  font-size: 30px;
  color: #0f172a;
  letter-spacing: 0;
}

.form-head p {
  margin: 8px 0 24px;
  color: #64748b;
}

.mode-switch {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 6px;
  padding: 5px;
  margin-bottom: 18px;
  background: #f1f5f9;
  border-radius: 12px;
}

.mode-switch button {
  height: 40px;
  border: 0;
  border-radius: 9px;
  background: transparent;
  color: #64748b;
  cursor: pointer;
  font-weight: 600;
}

.mode-switch button.active {
  color: #0f172a;
  background: #fff;
  box-shadow: 0 8px 18px rgba(15, 23, 42, 0.08);
}

.form-body {
  height: 214px;
  display: flex;
  flex-direction: column;
  justify-content: flex-start;
}

.form-body .auth-form {
  width: 100%;
}

.auth-mode-enter-active,
.auth-mode-leave-active {
  transition: opacity 0.18s ease, transform 0.18s ease;
}

.auth-mode-enter-from,
.auth-mode-leave-to {
  opacity: 0;
  transform: translateY(6px);
}

.auth-form :deep(.el-form-item) {
  margin-bottom: 16px;
}

.password-form :deep(.el-form-item) {
  margin-bottom: 22px;
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

.agreement-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0 0 18px;
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
  text-align: center;
  color: #64748b;
}

.switch-link a {
  color: #0284c7;
  text-decoration: none;
  font-weight: 700;
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
  }

  .visual-copy h2 {
    font-size: 26px;
  }

  .form-panel {
    padding: 32px 24px;
  }

  .form-body {
    height: auto;
    min-height: 214px;
  }
}
</style>
