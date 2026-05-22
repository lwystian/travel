<template>
  <div class="person-info">
    <section class="account-hero">
      <div class="hero-profile">
        <el-avatar :size="88" :src="avatarUrl" class="profile-avatar" @error="() => false">
          {{ avatarInitial }}
        </el-avatar>
        <div class="profile-main">
          <div class="profile-title-row">
            <h1>{{ displayName }}</h1>
            <span class="role-chip" :class="{ 'role-chip--super': isSuperAdmin }">{{ roleLabel }}</span>
          </div>
          <div class="profile-meta">
            <span>用户名 {{ form.username || '-' }}</span>
            <span>ID {{ form.id || '-' }}</span>
            <span>{{ maskedPhone }}</span>
          </div>
        </div>
      </div>
      <div class="hero-actions">
        <el-upload
          action="#"
          :auto-upload="true"
          :show-file-list="false"
          :http-request="customUploadAvatar"
          :before-upload="beforeAvatarUpload"
          :disabled="!isEditing"
        >
          <el-button :disabled="!isEditing" class="soft-btn">更换头像</el-button>
        </el-upload>
        <template v-if="!isEditing">
          <el-button type="primary" @click="handleEdit">编辑资料</el-button>
        </template>
        <template v-else>
          <el-button type="primary" :loading="saving" @click="handleSave">保存资料</el-button>
          <el-button @click="handleCancel">取消</el-button>
        </template>
      </div>
    </section>

    <section class="content-grid">
      <div class="profile-panel">
        <div class="section-head">
          <div>
            <h2>基本资料</h2>
            <p>用户名作为系统登录标识保留，业务展示优先使用昵称。</p>
          </div>
          <span class="state-pill">{{ isEditing ? '编辑中' : '只读' }}</span>
        </div>

        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          label-position="top"
          class="modern-form"
        >
          <div class="form-grid">
            <el-form-item label="用户名" prop="username">
              <el-input v-model="form.username" disabled />
            </el-form-item>
            <el-form-item label="昵称" prop="nickname">
              <el-input v-model="form.nickname" :disabled="!isEditing" maxlength="50" show-word-limit placeholder="请输入用于展示的昵称" />
            </el-form-item>
          </div>

          <el-form-item label="性别" prop="sex">
            <el-radio-group v-model="form.sex" :disabled="!isEditing" class="gender-segment">
              <el-radio-button label="男">男</el-radio-button>
              <el-radio-button label="女">女</el-radio-button>
            </el-radio-group>
          </el-form-item>

          <div class="bind-list">
            <div class="bind-item">
              <div>
                <span class="bind-label">电子邮箱</span>
                <strong>{{ maskedEmail }}</strong>
                <p>邮箱绑定需要完成验证，防止误填和账号归属争议。</p>
              </div>
              <el-button disabled class="soft-btn">前台验证绑定</el-button>
            </div>
            <div class="bind-item">
              <div>
                <span class="bind-label">手机号码</span>
                <strong>{{ maskedPhone }}</strong>
                <p>更换手机号需要极验和短信验证码；超级管理员还需验证当前手机号。</p>
              </div>
              <el-button type="primary" plain @click="openPhoneDialog">变更手机号</el-button>
            </div>
          </div>
        </el-form>
      </div>

      <aside class="security-panel">
        <div class="section-head">
          <div>
            <h2>账号安全</h2>
            <p>敏感操作完成后会刷新登录态，请重新登录确认身份。</p>
          </div>
        </div>

        <div class="security-summary">
          <div class="summary-item strong">
            <span>权限级别</span>
            <strong>{{ roleLabel }}</strong>
          </div>
          <div class="summary-item">
            <span>手机号</span>
            <strong>{{ form.phone ? '已绑定' : '未绑定' }}</strong>
          </div>
          <div class="summary-item">
            <span>邮箱</span>
            <strong>{{ form.email ? '已绑定' : '未绑定' }}</strong>
          </div>
        </div>

        <el-form
          ref="passwordFormRef"
          :model="passwordForm"
          :rules="passwordRules"
          label-position="top"
          class="modern-form password-form"
        >
          <el-form-item label="原密码" prop="oldPassword">
            <el-input v-model="passwordForm.oldPassword" type="password" placeholder="请输入原密码" show-password />
          </el-form-item>
          <el-form-item label="新密码" prop="newPassword">
            <el-input v-model="passwordForm.newPassword" type="password" placeholder="请输入新密码" show-password />
          </el-form-item>
          <el-form-item label="确认密码" prop="confirmPassword">
            <el-input v-model="passwordForm.confirmPassword" type="password" placeholder="请再次输入新密码" show-password />
          </el-form-item>

          <template v-if="isSuperAdmin">
            <div class="verify-box">
              <div class="verify-title">超级管理员二次验证</div>
              <p>修改密码前需先完成极验，并验证当前绑定手机号。</p>
              <GeetestBox
                ref="passwordCaptchaRef"
                success-text="验证已通过，可以发送当前手机号验证码"
                @verified="passwordGeetestValidate = $event"
                @error="message => ElMessage.error(message)"
                @unavailable="passwordGeetestRequired = false"
              />
              <div class="code-row">
                <el-form-item label="当前手机验证码" prop="currentPhoneCode">
                  <el-input v-model="passwordForm.currentPhoneCode" maxlength="6" placeholder="6位验证码" />
                </el-form-item>
                <el-button :loading="passwordCodeSending" :disabled="passwordCountdown > 0" @click="sendPasswordCurrentCode">
                  {{ passwordCountdown > 0 ? `${passwordCountdown}s` : '发送验证码' }}
                </el-button>
              </div>
            </div>
          </template>

          <el-button type="primary" class="submit-btn" :loading="changingPassword" @click="handleChangePassword">
            修改密码
          </el-button>
        </el-form>
      </aside>
    </section>

    <el-dialog
      v-model="phoneDialogVisible"
      title="安全变更手机号"
      width="560px"
      class="secure-dialog"
      @close="resetPhoneDialog"
    >
      <el-form ref="phoneFormRef" :model="phoneForm" :rules="phoneRules" label-position="top" class="modern-form dialog-form">
        <template v-if="isSuperAdmin">
          <div class="verify-box">
            <div class="verify-title">验证当前手机号</div>
            <p>超级管理员变更手机号前，需要先确认当前绑定手机号仍由本人持有。</p>
            <GeetestBox
              ref="currentPhoneCaptchaRef"
              success-text="验证已通过，可以发送当前手机号验证码"
              @verified="currentPhoneGeetestValidate = $event"
              @error="message => ElMessage.error(message)"
              @unavailable="currentPhoneGeetestRequired = false"
            />
            <div class="code-row">
              <el-form-item label="当前手机验证码" prop="currentSmsCode">
                <el-input v-model="phoneForm.currentSmsCode" maxlength="6" placeholder="6位验证码" />
              </el-form-item>
              <el-button :loading="currentPhoneCodeSending" :disabled="currentPhoneCountdown > 0" @click="sendCurrentPhoneCode">
                {{ currentPhoneCountdown > 0 ? `${currentPhoneCountdown}s` : '发送验证码' }}
              </el-button>
            </div>
          </div>
        </template>

        <div class="verify-box verify-box--plain">
          <div class="verify-title">验证新手机号</div>
          <el-form-item label="新手机号" prop="phone">
            <el-input v-model="phoneForm.phone" maxlength="11" placeholder="请输入新手机号" />
          </el-form-item>
          <GeetestBox
            ref="newPhoneCaptchaRef"
            success-text="验证已通过，可以发送新手机号验证码"
            @verified="newPhoneGeetestValidate = $event"
            @error="message => ElMessage.error(message)"
            @unavailable="newPhoneGeetestRequired = false"
          />
          <div class="code-row">
            <el-form-item label="新手机验证码" prop="smsCode">
              <el-input v-model="phoneForm.smsCode" maxlength="6" placeholder="6位验证码" />
            </el-form-item>
            <el-button :loading="newPhoneCodeSending" :disabled="newPhoneCountdown > 0" @click="sendNewPhoneCode">
              {{ newPhoneCountdown > 0 ? `${newPhoneCountdown}s` : '发送验证码' }}
            </el-button>
          </div>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="phoneDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="phoneSubmitting" @click="confirmPhoneChange">确认变更</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/store/user'
import request from '@/utils/request'
import GeetestBox from '@/components/auth/GeetestBox.vue'
import { maskEmail, maskPhone } from '@/utils/mask'

const baseAPI = process.env.VUE_APP_BASE_API || '/api'
const userStore = useUserStore()
const formRef = ref(null)
const passwordFormRef = ref(null)
const isEditing = ref(false)
const saving = ref(false)
const changingPassword = ref(false)
const phoneDialogVisible = ref(false)
const phoneSubmitting = ref(false)
const phoneFormRef = ref(null)
const passwordCaptchaRef = ref(null)
const currentPhoneCaptchaRef = ref(null)
const newPhoneCaptchaRef = ref(null)
const passwordGeetestValidate = ref(null)
const currentPhoneGeetestValidate = ref(null)
const newPhoneGeetestValidate = ref(null)
const passwordGeetestRequired = ref(true)
const currentPhoneGeetestRequired = ref(true)
const newPhoneGeetestRequired = ref(true)
const passwordCodeSending = ref(false)
const currentPhoneCodeSending = ref(false)
const newPhoneCodeSending = ref(false)
const passwordCountdown = ref(0)
const currentPhoneCountdown = ref(0)
const newPhoneCountdown = ref(0)
const isSuperAdmin = computed(() => userStore.isSuperAdmin)
const roleLabel = computed(() => {
  const roleCode = userStore.userInfo?.roleCode
  if (roleCode === 'SUPER_ADMIN') return '超级管理员'
  if (roleCode === 'ADMIN') return '管理员'
  return '普通用户'
})
const displayName = computed(() => form.nickname || form.username || '账户资料')
const avatarInitial = computed(() => (displayName.value || '用').slice(0, 1))
const maskedPhone = computed(() => {
  return maskPhone(form.phone, '未绑定手机号')
})
const maskedEmail = computed(() => {
  return maskEmail(form.email, '未绑定邮箱')
})

// 表单数据
const form = reactive({
  id: '',
  username: '',
  nickname: '',
  email: '',
  phone: '',
  sex: '男',
  avatar: ''
})

// 头像地址
const avatarUrl = computed(() => {
  return form.avatar ? baseAPI + form.avatar : '';
})

// 密码表单
const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: '',
  currentPhoneCode: ''
})

const phoneForm = reactive({
  phone: '',
  smsCode: '',
  currentSmsCode: ''
})

// 表单验证规则
const rules = {
  nickname: [
    { required: true, message: '请输入昵称', trigger: 'blur' },
    { min: 2, max: 50, message: '长度在 2 到 50 个字符', trigger: 'blur' }
  ],
  email: [
    { type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' }
  ],
  phone: [
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }
  ],
  sex: [
    { required: true, message: '请选择性别', trigger: 'change' }
  ]
}

// 密码验证规则
const passwordRules = {
  oldPassword: [
    { required: true, message: '请输入原密码', trigger: 'blur' },
    { min: 6, max: 20, message: '长度在 6 到 20 个字符', trigger: 'blur' }
  ],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 20, message: '长度在 6 到 20 个字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== passwordForm.newPassword) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ],
  currentPhoneCode: [
    {
      validator: (rule, value, callback) => {
        if (isSuperAdmin.value && !/^\d{6}$/.test(value || '')) {
          callback(new Error('请输入6位当前手机号验证码'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

const phoneRules = {
  phone: [
    { required: true, message: '请输入新手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }
  ],
  smsCode: [
    { required: true, message: '请输入新手机号验证码', trigger: 'blur' },
    { pattern: /^\d{6}$/, message: '验证码为6位数字', trigger: 'blur' }
  ],
  currentSmsCode: [
    {
      validator: (rule, value, callback) => {
        if (isSuperAdmin.value && !/^\d{6}$/.test(value || '')) {
          callback(new Error('请输入6位当前手机号验证码'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

const startCountdown = (target) => {
  target.value = 60
  const timer = window.setInterval(() => {
    target.value -= 1
    if (target.value <= 0) {
      window.clearInterval(timer)
    }
  }, 1000)
}

// 获取用户信息
const fetchUserInfo = async () => {
  try {
    // 获取当前用户的最新信息
    const userId = userStore.userInfo.id
    const res = await request.get(`/user/${userId}`, null, {
      showDefaultMsg: false
    })
    
    // 直接更新表单数据
    form.id = res.id || userStore.userInfo.id
    form.username = res.username || ''
    form.nickname = res.nickname || ''
    form.email = res.email || ''
    form.phone = res.phone || ''
    form.sex = res.sex || '男'
    form.avatar = res.avatar || ''
    
    console.log('用户信息加载成功:', form)
  } catch (error) {
    console.error('获取用户信息失败:', error)
    ElMessage.error('获取用户信息失败')
  }
}

// 上传头像前的校验
const beforeAvatarUpload = (file) => {
  const isJPG = file.type === 'image/jpeg'
  const isPNG = file.type === 'image/png'
  const isLt2M = file.size / 1024 / 1024 < 2

  if (!isJPG && !isPNG) {
    ElMessage.error('头像只能是 JPG 或 PNG 格式!')
    return false
  }
  if (!isLt2M) {
    ElMessage.error('头像大小不能超过 2MB!')
    return false
  }
  return true
}

// 自定义头像上传方法
const customUploadAvatar = async (options) => {
  try {
    const { file } = options

    // 创建 FormData 对象
    const formData = new FormData()
    formData.append('file', file)

    // 设置自定义上传选项
    const uploadOptions = {
      headers: {
        token: localStorage.getItem('token') || '',
      },
      // 不进行JSON处理
      transformRequest: [(data) => data],
      // 自定义成功消息
      successMsg: '头像上传成功',
      // 自定义错误消息
      errorMsg: '头像上传失败',
      // 成功回调
      onSuccess: async (data) => {
        // 更新用户头像
        form.avatar = data

        // 保存到后端
        await updateUserAvatar(data)

        // 通知上传成功
        options.onSuccess({ data })
      },
      // 错误回调
      onError: (error) => {
        console.error('头像上传错误:', error)
        options.onError(new Error(error.message || '上传失败'))
      },
    }

    // 发送上传请求
    await request.post('/file/upload/img', formData, uploadOptions)
  } catch (error) {
    options.onError(error)
    console.error('头像上传过程发生错误:', error)
  }
}

// 更新用户头像信息
const updateUserAvatar = async (avatarPath) => {
  try {
    await request.put(
      `/user/${form.id}`,
      { avatar: avatarPath },
      {
        showDefaultMsg: false,
        successMsg: '头像更新成功',
        onSuccess: () => {
          // 更新本地用户信息
          const updatedUserInfo = { ...userStore.userInfo, avatar: avatarPath }
          userStore.updateUserInfo(updatedUserInfo)
        },
      }
    )
  } catch (error) {
    console.error('头像信息保存失败', error)
    ElMessage.error(error.message || '头像信息保存失败')
    throw error
  }
}

// 编辑信息
const handleEdit = () => {
  isEditing.value = true
}

// 取消编辑
const handleCancel = () => {
  isEditing.value = false
  fetchUserInfo() // 重新获取数据，恢复原值
}

// 保存信息
const handleSave = async () => {
  if (!formRef.value) return
  
  try {
    await formRef.value.validate()
    saving.value = true
    
    await request.put(
      `/user/${form.id}`,
      {
        nickname: form.nickname,
        sex: form.sex
      },
      {
        showDefaultMsg: false,
        successMsg: '个人信息更新成功',
        onSuccess: () => {
          isEditing.value = false
          // 更新store中的用户信息
          userStore.updateUserInfo({
            ...userStore.userInfo,
            nickname: form.nickname,
            sex: form.sex
          })
        }
      }
    )
  } catch (error) {
    console.error('更新用户信息失败:', error)
    ElMessage.error(error.message || '更新用户信息失败')
  } finally {
    saving.value = false
  }
}

const openPhoneDialog = () => {
  phoneForm.phone = ''
  phoneForm.smsCode = ''
  phoneForm.currentSmsCode = ''
  phoneDialogVisible.value = true
}

const resetPhoneDialog = () => {
  phoneForm.phone = ''
  phoneForm.smsCode = ''
  phoneForm.currentSmsCode = ''
  currentPhoneGeetestValidate.value = null
  newPhoneGeetestValidate.value = null
  currentPhoneCaptchaRef.value?.resetCaptcha()
  newPhoneCaptchaRef.value?.resetCaptcha()
}

const sendPasswordCurrentCode = async () => {
  if (passwordGeetestRequired.value && !passwordGeetestValidate.value) {
    ElMessage.warning('请先完成极验验证')
    return
  }
  passwordCodeSending.value = true
  try {
    await request.post('/user/phone/current/code', { geetest: passwordGeetestValidate.value }, { showDefaultMsg: false })
    ElMessage.success('验证码已发送至当前绑定手机号')
    startCountdown(passwordCountdown)
    passwordGeetestValidate.value = null
    passwordCaptchaRef.value?.resetCaptcha()
  } catch (error) {
    ElMessage.error(error.message || '验证码发送失败')
  } finally {
    passwordCodeSending.value = false
  }
}

const sendCurrentPhoneCode = async () => {
  if (currentPhoneGeetestRequired.value && !currentPhoneGeetestValidate.value) {
    ElMessage.warning('请先完成当前手机号极验验证')
    return
  }
  currentPhoneCodeSending.value = true
  try {
    await request.post('/user/phone/current/code', { geetest: currentPhoneGeetestValidate.value }, { showDefaultMsg: false })
    ElMessage.success('当前手机号验证码已发送')
    startCountdown(currentPhoneCountdown)
    currentPhoneGeetestValidate.value = null
    currentPhoneCaptchaRef.value?.resetCaptcha()
  } catch (error) {
    ElMessage.error(error.message || '验证码发送失败')
  } finally {
    currentPhoneCodeSending.value = false
  }
}

const sendNewPhoneCode = async () => {
  const validPhone = await phoneFormRef.value?.validateField('phone').catch(() => false)
  if (validPhone === false) return
  if (newPhoneGeetestRequired.value && !newPhoneGeetestValidate.value) {
    ElMessage.warning('请先完成新手机号极验验证')
    return
  }
  newPhoneCodeSending.value = true
  try {
    await request.post('/user/phone/change/code', {
      phone: phoneForm.phone,
      geetest: newPhoneGeetestValidate.value
    }, { showDefaultMsg: false })
    ElMessage.success('新手机号验证码已发送')
    startCountdown(newPhoneCountdown)
    newPhoneGeetestValidate.value = null
    newPhoneCaptchaRef.value?.resetCaptcha()
  } catch (error) {
    ElMessage.error(error.message || '验证码发送失败')
  } finally {
    newPhoneCodeSending.value = false
  }
}

const confirmPhoneChange = async () => {
  if (!phoneFormRef.value) return
  try {
    await phoneFormRef.value.validate()
    phoneSubmitting.value = true
    await request.post('/user/phone/change/confirm', {
      phone: phoneForm.phone,
      smsCode: phoneForm.smsCode,
      currentSmsCode: phoneForm.currentSmsCode
    }, {
      showDefaultMsg: false,
      successMsg: '手机号变更成功'
    })
    form.phone = phoneForm.phone
    userStore.updateUserInfo({ ...userStore.userInfo, phone: phoneForm.phone })
    phoneDialogVisible.value = false
  } catch (error) {
    ElMessage.error(error.message || '手机号变更失败')
  } finally {
    phoneSubmitting.value = false
  }
}

// 修改密码
const handleChangePassword = async () => {
  if (!passwordFormRef.value) return

  try {
    await passwordFormRef.value.validate()
    changingPassword.value = true

    await request.put(
      `/user/password/${form.id}`,
      {
        oldPassword: passwordForm.oldPassword,
        newPassword: passwordForm.newPassword,
        currentPhoneCode: passwordForm.currentPhoneCode
      },
      {
        showDefaultMsg: false,
        successMsg: '密码修改成功',
        onSuccess: () => {
          // 清空密码表单
          passwordFormRef.value.resetFields()
          
          // 提示用户重新登录
          ElMessageBox.confirm('密码已修改，需要重新登录', '提示', {
            confirmButtonText: '重新登录',
            cancelButtonText: '取消',
            type: 'warning',
          }).then(() => {
            // 清除用户信息并跳转到登录页
            userStore.clearUserInfo()
            window.location.href = '/login'
          })
        }
      }
    )
  } catch (error) {
    console.error('修改密码失败:', error)
    ElMessage.error(error.message || '密码修改失败')
  } finally {
    changingPassword.value = false
  }
}

onMounted(() => {
  fetchUserInfo()
})
</script>

<style lang="scss" scoped>
.person-info {
  min-height: calc(100vh - 120px);
  padding: 24px;
  background:
    radial-gradient(circle at 8% 0%, rgba(59, 130, 246, 0.08), transparent 26%),
    linear-gradient(180deg, #f8fafc 0%, #eef2f7 100%);
  color: #111827;
}

.account-hero,
.profile-panel,
.security-panel {
  border: 1px solid rgba(148, 163, 184, 0.22);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.94);
  box-shadow: 0 16px 40px rgba(15, 23, 42, 0.08);
}

.account-hero {
  display: flex;
  justify-content: space-between;
  gap: 24px;
  align-items: center;
  padding: 28px;
  margin-bottom: 20px;
}

.hero-profile {
  display: flex;
  align-items: center;
  min-width: 0;
  gap: 18px;
}

.profile-avatar {
  flex: 0 0 auto;
  border: 4px solid #ffffff;
  background: linear-gradient(135deg, #1d4ed8, #0f766e);
  color: #ffffff;
  font-size: 28px;
  font-weight: 700;
  box-shadow: 0 12px 24px rgba(15, 23, 42, 0.14);
}

.profile-main {
  min-width: 0;
}

.profile-title-row {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;

  h1 {
    max-width: 420px;
    margin: 0;
    overflow: hidden;
    color: #0f172a;
    font-size: 28px;
    font-weight: 700;
    line-height: 1.25;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}

.profile-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 10px;

  span {
    padding: 6px 10px;
    border-radius: 999px;
    background: #f1f5f9;
    color: #475569;
    font-size: 13px;
  }
}

.role-chip,
.state-pill {
  display: inline-flex;
  align-items: center;
  height: 26px;
  padding: 0 10px;
  border-radius: 999px;
  background: #ecfeff;
  color: #0e7490;
  font-size: 12px;
  font-weight: 700;
  white-space: nowrap;
}

.role-chip--super {
  background: #fff1f2;
  color: #be123c;
}

.hero-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 10px;
}

.soft-btn {
  border-color: #dbe3ef;
  background: #ffffff;
  color: #334155;
}

.content-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(360px, 0.58fr);
  gap: 20px;
  align-items: start;
}

.profile-panel,
.security-panel {
  padding: 24px;
}

.section-head {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
  margin-bottom: 22px;

  h2 {
    margin: 0;
    color: #0f172a;
    font-size: 20px;
    font-weight: 700;
  }

  p {
    margin: 6px 0 0;
    color: #64748b;
    font-size: 13px;
    line-height: 1.6;
  }
}

.modern-form {
  :deep(.el-form-item) {
    margin-bottom: 18px;
  }

  :deep(.el-form-item__label) {
    margin-bottom: 8px;
    padding: 0;
    color: #334155;
    font-size: 13px;
    font-weight: 700;
    line-height: 1.2;
  }

  :deep(.el-form-item.is-required:not(.is-no-asterisk).asterisk-left > .el-form-item__label::before),
  :deep(.el-form-item.is-required:not(.is-no-asterisk).asterisk-right > .el-form-item__label::after) {
    display: none;
  }

  :deep(.el-input__wrapper) {
    min-height: 42px;
    border-radius: 8px;
    box-shadow: 0 0 0 1px #dbe3ef inset;
  }

  :deep(.el-input__wrapper.is-focus) {
    box-shadow: 0 0 0 1px #2563eb inset, 0 0 0 3px rgba(37, 99, 235, 0.12);
  }
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.gender-segment {
  :deep(.el-radio-button__inner) {
    min-width: 92px;
    border-color: #dbe3ef;
    color: #475569;
  }

  :deep(.el-radio-button__original-radio:checked + .el-radio-button__inner) {
    border-color: #2563eb;
    background: #2563eb;
    box-shadow: none;
  }
}

.bind-list {
  display: grid;
  gap: 12px;
  margin-top: 8px;
}

.bind-item {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: center;
  padding: 16px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  background: #f8fafc;

  div {
    min-width: 0;
  }

  strong {
    display: block;
    margin-top: 4px;
    overflow: hidden;
    color: #0f172a;
    font-size: 15px;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  p {
    margin: 6px 0 0;
    color: #64748b;
    font-size: 12px;
    line-height: 1.5;
  }
}

.bind-label {
  color: #64748b;
  font-size: 12px;
  font-weight: 700;
}

.security-summary {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
  margin-bottom: 22px;
}

.summary-item {
  padding: 14px;
  border-radius: 8px;
  background: #f8fafc;

  span {
    display: block;
    color: #64748b;
    font-size: 12px;
  }

  strong {
    display: block;
    margin-top: 6px;
    color: #0f172a;
    font-size: 15px;
  }
}

.summary-item.strong {
  background: #eff6ff;

  strong {
    color: #1d4ed8;
  }
}

.verify-box {
  margin: 4px 0 18px;
  padding: 16px;
  border: 1px solid #bfdbfe;
  border-radius: 8px;
  background: #f8fbff;

  p {
    margin: 4px 0 12px;
    color: #64748b;
    font-size: 12px;
    line-height: 1.6;
  }
}

.verify-box--plain {
  border-color: #e2e8f0;
  background: #ffffff;
}

.verify-title {
  color: #0f172a;
  font-size: 14px;
  font-weight: 700;
}

.code-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 118px;
  gap: 10px;
  align-items: end;

  :deep(.el-form-item) {
    margin-bottom: 0;
  }

  .el-button {
    height: 42px;
  }
}

.submit-btn {
  width: 100%;
  height: 42px;
  border-radius: 8px;
  font-weight: 700;
}

.secure-dialog {
  :deep(.el-dialog) {
    border-radius: 8px;
    overflow: hidden;
  }

  :deep(.el-dialog__header) {
    margin: 0;
    padding: 20px 22px 12px;
    border-bottom: 1px solid #edf2f7;
  }

  :deep(.el-dialog__title) {
    color: #0f172a;
    font-weight: 700;
  }

  :deep(.el-dialog__body) {
    padding: 20px 22px;
  }

  :deep(.el-dialog__footer) {
    padding: 14px 22px 20px;
    border-top: 1px solid #edf2f7;
  }
}

@media (max-width: 1180px) {
  .content-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 760px) {
  .person-info {
    padding: 16px;
  }

  .account-hero,
  .bind-item {
    align-items: stretch;
    flex-direction: column;
  }

  .hero-profile {
    align-items: flex-start;
  }

  .profile-title-row {
    align-items: flex-start;
    flex-direction: column;

    h1 {
      max-width: 100%;
      font-size: 24px;
    }
  }

  .hero-actions {
    justify-content: flex-start;
  }

  .form-grid,
  .security-summary,
  .code-row {
    grid-template-columns: 1fr;
  }
}
</style>
