<template>
  <div class="geetest-box">
    <div v-if="available" class="geetest-captcha">
      <div ref="captchaRef" class="geetest-mount"></div>
      <div v-if="loading" class="captcha-placeholder">正在加载验证...</div>
      <div v-else-if="errorMessage" class="captcha-error">{{ errorMessage }}</div>
    </div>
    <div v-if="available" class="captcha-status" :class="status">
      {{ statusText }}
    </div>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import {
  getGeetestConfig,
  isGeetestReady,
  loadGeetestScript,
  normalizeGeetestValidate,
  translateGeetestError
} from '@/utils/geetest'

const emit = defineEmits(['verified', 'error', 'ready', 'unavailable'])

const captchaRef = ref(null)
const loading = ref(true)
const errorMessage = ref('')
const status = ref('pending')
const available = ref(true)
let captchaObj = null
let captchaId = ''
let initTimer = null

const statusText = computed(() => {
  if (status.value === 'success') return '验证已通过，可以发送短信验证码'
  if (status.value === 'error') return '验证不可用，请检查极验配置'
  if (status.value === 'ready') return '请先点击上方按钮完成验证'
  return '正在准备安全验证'
})

const clearInitTimer = () => {
  if (initTimer) {
    clearTimeout(initTimer)
    initTimer = null
  }
}

const showError = error => {
  clearInitTimer()
  loading.value = false
  status.value = 'error'
  errorMessage.value = translateGeetestError(error)
  emit('error', errorMessage.value)
}

const markReady = () => {
  clearInitTimer()
  loading.value = false
  status.value = 'ready'
  emit('ready')
}

const resetCaptcha = () => {
  status.value = 'ready'
  errorMessage.value = ''
  emit('verified', null)
  if (captchaObj?.reset) {
    captchaObj.reset()
  }
}

const initCaptcha = async () => {
  loading.value = true
  errorMessage.value = ''
  status.value = 'pending'
  available.value = true
  clearInitTimer()

  try {
    const config = await getGeetestConfig()
    if (!isGeetestReady(config)) {
      available.value = false
      loading.value = false
      status.value = 'disabled'
      emit('verified', null)
      emit('unavailable', config)
      return
    }

    captchaId = config.captchaId
    await loadGeetestScript()
    await nextTick()

    if (!captchaRef.value) {
      throw new Error('验证容器初始化失败，请刷新页面后重试')
    }

    initTimer = setTimeout(() => {
      showError(new Error('极验验证初始化超时，请检查网络或 Captcha ID 后重试'))
    }, 12000)

    window.initGeetest(
      {
        gt: captchaId,
        challenge: config.challenge,
        offline: !config.success,
        new_captcha: true,
        product: 'float',
        width: '100%',
        lang: 'zh-cn',
        https: true,
        timeout: 10000
      },
      instance => {
        captchaObj = instance
        captchaObj.appendTo(captchaRef.value)
        markReady()

        captchaObj.onReady(() => {
          markReady()
        })
        captchaObj.onSuccess(() => {
          const result = captchaObj.getValidate()
          clearInitTimer()
          if (!result) {
            showError(new Error('请先完成滑动拼图验证'))
            return
          }
          status.value = 'success'
          emit('verified', normalizeGeetestValidate(captchaId, result))
        })
        captchaObj.onError?.(error => {
          showError(error)
        })
      }
    )
  } catch (error) {
    showError(error)
  }
}

onMounted(initCaptcha)

onBeforeUnmount(() => {
  clearInitTimer()
  if (captchaObj?.destroy) {
    captchaObj.destroy()
  }
})

defineExpose({ resetCaptcha, initCaptcha })
</script>

<style scoped>
.geetest-box {
  margin-bottom: 16px;
}

.geetest-captcha {
  position: relative;
  min-height: 44px;
}

.geetest-mount {
  min-height: 44px;
}

.captcha-placeholder,
.captcha-error {
  position: absolute;
  inset: 0;
  z-index: 1;
  min-height: 44px;
  display: flex;
  align-items: center;
  padding: 0 14px;
  border: 1px solid #d8dee9;
  border-radius: 4px;
  background: #fff;
  color: #64748b;
  font-size: 14px;
}

.captcha-error {
  border-color: #fecaca;
  background: #fff7f7;
  color: #b91c1c;
}

.captcha-status {
  margin-top: 8px;
  color: #64748b;
  font-size: 12px;
}

.captcha-status.success {
  color: #059669;
}

.captcha-status.error {
  color: #dc2626;
}
</style>
