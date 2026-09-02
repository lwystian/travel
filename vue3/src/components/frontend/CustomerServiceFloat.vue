<template>
  <a
    v-if="visible"
    class="customer-service-float"
    :href="customerServiceUrl"
    target="_blank"
    rel="noopener noreferrer"
    :aria-label="displayName"
    :title="displayName"
  >
    <img :src="customerServiceIcon" alt="" aria-hidden="true" />
  </a>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { getPublicCustomerServiceConfig } from '@/api/customerService'
import { getPublicSiteAccessConfig } from '@/api/siteAccess'
import customerServiceIcon from '@/assets/images/customer-service.png'

const route = useRoute()
const config = ref({
  enabled: false,
  configured: false,
  displayName: '在线客服',
  serviceUrl: '',
  channelUrls: {}
})

const currentChannel = computed(() => {
  const path = route.path.toLowerCase()
  const intent = String(route.query.intentDestination || route.query.destination || '').toLowerCase()
  const keyword = String(route.query.search || '').trim()

  if (intent.includes('xisha') || keyword.includes('西沙')) return 'xisha'
  if (intent.includes('sanxia') || keyword.includes('三峡')) return 'sanxia'
  if (path.startsWith('/ticket') || path.startsWith('/tickets')) return 'tour'
  if (path.startsWith('/order') || path.startsWith('/payment')) return 'order'
  if (path.startsWith('/profile') || path.startsWith('/collection') || path.startsWith('/my-')) return 'user'
  if (path.startsWith('/around')) return 'chongqing'
  return 'home'
})

const validWecomUrl = value => {
  if (!value) return ''
  try {
    const url = new URL(String(value))
    return url.protocol === 'https:'
      && url.hostname === 'work.weixin.qq.com'
      && url.pathname.startsWith('/kfid/')
      ? url.toString()
      : ''
  } catch {
    return ''
  }
}

const customerServiceUrl = computed(() => {
  const channelUrl = config.value.channelUrls?.[currentChannel.value]
  return validWecomUrl(channelUrl) || validWecomUrl(config.value.serviceUrl)
})

const displayName = computed(() => String(config.value.displayName || '在线客服').trim() || '在线客服')
const visible = computed(() => config.value.enabled && config.value.configured && Boolean(customerServiceUrl.value))

const loadCustomerServiceConfig = async () => {
  try {
    const data = await getPublicCustomerServiceConfig()
    config.value = {
      ...config.value,
      ...(data || {}),
      channelUrls: data?.channelUrls || {}
    }
  } catch {
    try {
      const fallback = await getPublicSiteAccessConfig()
      const serviceUrl = validWecomUrl(fallback?.supportUrl)
      config.value = {
        enabled: Boolean(serviceUrl),
        configured: Boolean(serviceUrl),
        displayName: fallback?.supportButtonText || '在线客服',
        serviceUrl,
        channelUrls: {}
      }
    } catch {
      config.value.enabled = false
    }
  }
}

onMounted(loadCustomerServiceConfig)
</script>

<style scoped>
.customer-service-float {
  position: fixed;
  right: clamp(18px, 2vw, 32px);
  bottom: 32px;
  z-index: 900;
  width: 68px;
  height: 68px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: transparent;
  transition: transform 0.16s ease, opacity 0.16s ease;
}

.customer-service-float:hover {
  transform: translateY(-2px) scale(1.03);
}

.customer-service-float:active {
  transform: scale(0.95);
  opacity: 0.86;
}

.customer-service-float:focus-visible {
  outline: 3px solid rgba(255, 102, 0, 0.38);
  outline-offset: 4px;
}

.customer-service-float img {
  width: 64px;
  height: 65px;
  display: block;
  object-fit: contain;
  filter: drop-shadow(0 0 1px rgba(255, 255, 255, 0.96)) drop-shadow(0 8px 12px rgba(15, 23, 42, 0.22));
}

@media (max-width: 768px) {
  .customer-service-float {
    right: 16px;
    bottom: calc(22px + env(safe-area-inset-bottom));
    width: 60px;
    height: 60px;
  }

  .customer-service-float img {
    width: 58px;
    height: 59px;
  }
}
</style>
