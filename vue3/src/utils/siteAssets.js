import { ref } from 'vue'
import { getPublicSiteAssets } from '@/api/siteAssets'

const baseAPI = process.env.VUE_APP_BASE_API || '/api'
const siteAssets = ref({})
let loadingPromise = null
const defaultFavicon = `${process.env.BASE_URL || '/'}favicon.ico`

export const getAssetUrl = (url, fallback = '') => {
  if (!url) return fallback
  const value = url
  if (!value) return ''
  if (/^(https?:|data:|blob:)/.test(value)) return value
  if (value.startsWith('/img/')) return `${baseAPI}${value}`
  return value.startsWith('/') ? value : fallback
}

const setFavicon = (url) => {
  const href = getAssetUrl(url, defaultFavicon) || defaultFavicon
  const selectors = [
    'link[rel="icon"]',
    'link[rel="shortcut icon"]',
    'link[rel="apple-touch-icon"]'
  ]
  selectors.forEach(selector => {
    document.querySelectorAll(selector).forEach(node => node.parentNode?.removeChild(node))
  })
  const link = document.createElement('link')
  link.rel = 'icon'
  link.href = href
  document.head.appendChild(link)
}

export const useSiteAssets = () => {
  const loadSiteAssets = async () => {
    if (loadingPromise) return loadingPromise
    loadingPromise = getPublicSiteAssets()
      .then(data => {
        siteAssets.value = data || {}
        setFavicon(siteAssets.value.faviconUrl)
        return siteAssets.value
      })
      .catch(() => {
        siteAssets.value = {}
        setFavicon('')
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
