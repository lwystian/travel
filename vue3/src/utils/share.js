import { ElMessage } from 'element-plus'

const copyText = async (text) => {
  if (navigator.clipboard && window.isSecureContext) {
    await navigator.clipboard.writeText(text)
    return
  }

  const textarea = document.createElement('textarea')
  textarea.value = text
  textarea.setAttribute('readonly', 'readonly')
  textarea.style.position = 'fixed'
  textarea.style.left = '-9999px'
  textarea.style.top = '0'
  document.body.appendChild(textarea)
  textarea.focus()
  textarea.select()
  const copied = document.execCommand('copy')
  document.body.removeChild(textarea)
  if (!copied) {
    throw new Error('copy failed')
  }
}

const normalizeShareText = (value = '') => String(value || '')
  .replace(/<[^>]+>/g, '')
  .replace(/\s+/g, ' ')
  .trim()

const getCurrentUrl = () => {
  const url = new URL(window.location.href)
  return url.toString()
}

const shouldUseNativeShare = (payload) => {
  if (!navigator.share) return false

  const userAgent = navigator.userAgent || ''
  const isMobile = /Android|iPhone|iPad|iPod|Mobile/i.test(userAgent)
  const isCoarsePointer = window.matchMedia?.('(pointer: coarse)')?.matches
  if (!isMobile && !isCoarsePointer) return false

  if (navigator.canShare) {
    try {
      return navigator.canShare(payload)
    } catch {
      return false
    }
  }

  return true
}

export const shareCurrentPage = async ({ title = document.title, text = '', url = getCurrentUrl() } = {}) => {
  const shareTitle = normalizeShareText(title) || document.title || '页面分享'
  const shareText = normalizeShareText(text)
  const shareUrl = String(url || getCurrentUrl())
  const sharePayload = { title: shareTitle, text: shareText, url: shareUrl }
  const fallbackText = [shareTitle, shareText, shareUrl].filter(Boolean).join('\n')

  try {
    if (shouldUseNativeShare(sharePayload)) {
      await navigator.share(sharePayload)
      return
    }
  } catch (error) {
    if (error?.name === 'AbortError') {
      return
    }
  }

  try {
    await copyText(fallbackText)
    ElMessage.success('链接已复制，可以分享给朋友了')
  } catch (error) {
    ElMessage.error('分享失败，请手动复制浏览器地址')
  }
}
