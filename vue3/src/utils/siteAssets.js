import { ref } from 'vue'
import { getPublicSiteAssets } from '@/api/siteAssets'

const baseAPI = process.env.VUE_APP_BASE_API || '/api'
const siteAssets = ref({})
let loadingPromise = null

export const getAssetUrl = (url, fallback = '') => {
  if (!url) return fallback
  const value = url
  if (!value) return ''
  if (/^(https?:|data:|blob:)/.test(value)) return value
  if (value.startsWith('/img/')) return `${baseAPI}${value}`
  return value.startsWith('/') ? value : fallback
}

export const useSiteAssets = () => {
  const loadSiteAssets = async () => {
    if (loadingPromise) return loadingPromise
    loadingPromise = getPublicSiteAssets()
      .then(data => {
        siteAssets.value = data || {}
        return siteAssets.value
      })
      .catch(() => {
        siteAssets.value = {}
        return siteAssets.value
      })
      .finally(() => {
        loadingPromise = null
      })
    return loadingPromise
  }

  return {
    siteAssets,
    loadSiteAssets,
    getAssetUrl
  }
}
