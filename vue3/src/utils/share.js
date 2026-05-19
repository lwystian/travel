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
  document.body.appendChild(textarea)
  textarea.select()
  document.execCommand('copy')
  document.body.removeChild(textarea)
}

export const shareCurrentPage = async ({ title = document.title, text = '' } = {}) => {
  const url = window.location.href
  try {
    if (navigator.share) {
      await navigator.share({ title, text, url })
      return
    }
  } catch (error) {
    if (error?.name === 'AbortError') {
      return
    }
  }

  try {
    await copyText(url)
    ElMessage.success('链接已复制，可以分享给朋友了')
  } catch (error) {
    ElMessage.error('分享失败，请手动复制浏览器地址')
  }
}
