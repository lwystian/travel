<template>
  <div v-if="shouldCheckAccess && !accessReady" class="site-access-preflight">
    <div>
      <span></span>
      <strong>正在检查访问状态</strong>
      <small>请稍候，系统正在确认官网当前开放策略。</small>
    </div>
  </div>
  <SiteAccessNotice
    v-else-if="accessBlock.blocked"
    :mode="accessBlock.mode"
    :title="accessBlock.title"
    :message="accessBlock.message"
    :contact="accessBlock.contact"
    :support-button-text="accessConfig.supportButtonText"
    :support-url="accessConfig.supportUrl"
    :support-credential="accessConfig.supportCredential"
    :support-qr-image-url="resolvedSupportQrImage"
  />
  <router-view v-else />
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import SiteAccessNotice from '@/components/frontend/SiteAccessNotice.vue'
import { getPublicSiteAccessConfig } from '@/api/siteAccess'
import noImage from '@/assets/images/no-image.png'

const route = useRoute()
const baseAPI = process.env.VUE_APP_BASE_API || '/api'
const accessReady = ref(false)
const accessConfig = reactive({
  siteEnabled: true,
  rejectMobile: false,
  closedTitle: '网站维护中',
  closedMessage: '我们正在进行系统维护与服务升级，完成后将第一时间恢复访问。',
  closedContact: '如有紧急订单或出行问题，请联系官方客服处理。',
  mobileTitle: '请使用电脑访问',
  mobileMessage: '当前官网桌面版正在服务中，移动端 H5 模板正在制作，为保证浏览和下单体验，请使用电脑访问。',
  mobileContact: '如需咨询行程，可通过官方客服渠道联系我们。',
  supportButtonText: '联系官方客服',
  supportUrl: '',
  supportCredential: '',
  supportQrImageUrl: ''
})

const isBackendOrAuthRoute = computed(() => {
  return route.path.startsWith('/back') || route.path === '/login' || route.path === '/register'
})

const shouldCheckAccess = computed(() => !isBackendOrAuthRoute.value)

const isMobileDevice = () => {
  const ua = navigator.userAgent || navigator.vendor || window.opera || ''
  const uaMatched = /Android|webOS|iPhone|iPad|iPod|BlackBerry|IEMobile|Opera Mini|Mobile|Tablet|HarmonyOS/i.test(ua)
  const coarsePointer = window.matchMedia?.('(pointer: coarse)').matches
  const noHover = window.matchMedia?.('(hover: none)').matches
  const smallViewport = Math.min(window.innerWidth, window.innerHeight) < 900
  const iPadDesktopMode = navigator.platform === 'MacIntel' && navigator.maxTouchPoints > 1
  return uaMatched || iPadDesktopMode || (coarsePointer && noHover && smallViewport)
}

const accessBlock = computed(() => {
  if (isBackendOrAuthRoute.value) {
    return { blocked: false }
  }
  if (accessConfig.siteEnabled === false) {
    return {
      blocked: true,
      mode: 'closed',
      title: accessConfig.closedTitle,
      message: accessConfig.closedMessage,
      contact: accessConfig.closedContact
    }
  }
  if (accessConfig.rejectMobile && isMobileDevice()) {
    return {
      blocked: true,
      mode: 'mobile',
      title: accessConfig.mobileTitle,
      message: accessConfig.mobileMessage,
      contact: accessConfig.mobileContact
    }
  }
  return { blocked: false }
})

const resolvedSupportQrImage = computed(() => {
  const url = accessConfig.supportQrImageUrl
  if (!url) return noImage
  if (/^(https?:)?\/\//.test(url) || url.startsWith('data:')) return url
  return `${baseAPI}${url.startsWith('/') ? url : `/${url}`}`
})

const loadSiteAccess = async () => {
  if (!shouldCheckAccess.value) {
    accessReady.value = true
    return
  }
  accessReady.value = false
  try {
    const data = await getPublicSiteAccessConfig()
    Object.assign(accessConfig, data || {})
  } catch {
    // 访问控制配置拉取失败时保持默认开放，避免网络抖动误伤用户访问。
  } finally {
    accessReady.value = true
  }
}

onMounted(loadSiteAccess)
watch(() => route.fullPath, loadSiteAccess)
watch(accessBlock, (block) => {
  if (block.blocked && block.title) {
    document.title = `${block.title} - 侠客行国旅`
  }
}, { immediate: true })
</script>

<style lang="scss">
#app {
  font-family: Avenir, Helvetica, Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  text-align: center;
  color: #2c3e50;
}

.site-access-preflight {
  min-height: 100vh;
  display: grid;
  place-items: center;
  background: linear-gradient(135deg, #f8fafc 0%, #eef4ff 100%);
}

.site-access-preflight > div {
  display: grid;
  justify-items: center;
  gap: 12px;
  padding: 34px;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.86);
  box-shadow: 0 24px 70px rgba(15, 23, 42, 0.12);
}

.site-access-preflight span {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  border: 3px solid #bfdbfe;
  border-top-color: #2563eb;
  animation: access-spin 0.8s linear infinite;
}

.site-access-preflight strong {
  color: #0f172a;
  font-size: 18px;
}

.site-access-preflight small {
  color: #64748b;
}

@keyframes access-spin {
  to {
    transform: rotate(360deg);
  }
}

.el-overlay-dialog {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
}

.el-overlay-dialog > .el-dialog {
  max-height: calc(100vh - 48px);
  margin: 0 !important;
  display: flex;
  flex-direction: column;
}

.el-overlay-dialog > .el-dialog .el-dialog__body {
  overflow: auto;
}

@media (max-width: 640px) {
  .el-overlay-dialog {
    padding: 12px;
  }

  .el-overlay-dialog > .el-dialog {
    width: min(100%, calc(100vw - 24px)) !important;
    max-height: calc(100vh - 24px);
  }
}

</style>
