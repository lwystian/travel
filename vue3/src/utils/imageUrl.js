import noImage from '@/assets/images/no-image.png'

const baseAPI = process.env.VUE_APP_BASE_API || '/api'

export const resolveImageUrl = (url, fallback = noImage) => {
  if (!url) return fallback
  const value = String(url).trim()
  if (!value) return fallback
  if (/^(https?:|data:|blob:)/i.test(value)) return value
  if (value.startsWith('//')) return `${window.location.protocol}${value}`
  if (value.startsWith(baseAPI)) return value
  if (value.startsWith('/api/')) return value
  if (value.startsWith('/')) return `${baseAPI}${value}`
  return `${baseAPI}/${value}`
}

export const resolveAbsoluteImageUrl = (url, fallback = noImage) => {
  const resolved = resolveImageUrl(url, fallback)
  if (/^(https?:|data:|blob:)/i.test(resolved)) return resolved
  return `${window.location.origin}${resolved}`
}
