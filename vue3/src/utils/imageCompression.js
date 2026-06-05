import { ElMessage, ElMessageBox } from 'element-plus'

export const SUPPORTED_IMAGE_TYPES = [
  'image/jpeg',
  'image/jpg',
  'image/png',
  'image/webp',
  'image/gif',
  'image/x-icon',
  'image/vnd.microsoft.icon',
  'image/bmp',
  'image/x-ms-bmp',
  'image/avif',
  'image/tiff',
  'image/heic',
  'image/heif'
]
export const SUPPORTED_IMAGE_FORMAT_LABEL = 'JPG、PNG、WebP、GIF、ICO、BMP、AVIF、TIFF、HEIC、HEIF'

const SUPPORTED_IMAGE_EXTENSIONS = [
  'jpg',
  'jpeg',
  'png',
  'webp',
  'gif',
  'ico',
  'bmp',
  'avif',
  'tif',
  'tiff',
  'heic',
  'heif'
]
const COMPRESSIBLE_TYPES = ['image/jpeg', 'image/jpg', 'image/png', 'image/webp', 'image/bmp', 'image/x-ms-bmp', 'image/avif']
const MAX_DIMENSION = 1920
const MIN_DIMENSION = 960
const TARGET_SIZE = 700 * 1024
const DEFAULT_MAX_UPLOAD_SIZE = 2 * 1024 * 1024
const BASE_API = process.env.VUE_APP_BASE_API || '/api'
const IMAGE_CONFIG_URL = `${BASE_API}/file/upload/img/config`
let cachedMaxUploadSize = null

const formatSize = size => {
  if (!size) return '0KB'
  if (size >= 1024 * 1024) return `${(size / 1024 / 1024).toFixed(2)}MB`
  return `${Math.max(1, Math.round(size / 1024))}KB`
}

const getUploadFile = formData => {
  if (!(formData instanceof FormData)) return null
  const file = formData.get('file')
  return file instanceof File ? file : null
}

const getFileExtension = file => String(file?.name || '')
  .split('.')
  .pop()
  .toLowerCase()

export const isSupportedImageFile = file => {
  const type = String(file?.type || '').toLowerCase()
  const ext = getFileExtension(file)
  return SUPPORTED_IMAGE_TYPES.includes(type) || SUPPORTED_IMAGE_EXTENSIONS.includes(ext)
}

export const getSupportedImageMessage = () => `仅支持 ${SUPPORTED_IMAGE_FORMAT_LABEL} 图片`

const shouldHandleImageUpload = (url, data) => {
  const file = getUploadFile(data)
  return Boolean(url && String(url).includes('/file/upload/img') && file)
}

const loadImage = file => new Promise((resolve, reject) => {
  const url = URL.createObjectURL(file)
  const image = new Image()
  image.onload = () => {
    URL.revokeObjectURL(url)
    resolve(image)
  }
  image.onerror = () => {
    URL.revokeObjectURL(url)
    reject(new Error('图片读取失败'))
  }
  image.src = url
})

const canvasToBlob = (canvas, type, quality) => new Promise((resolve, reject) => {
  canvas.toBlob(blob => {
    if (blob) {
      resolve(blob)
    } else {
      reject(new Error('图片压缩失败'))
    }
  }, type, quality)
})

const getCompressedTypes = file => {
  const types = ['image/webp']
  if (file.type !== 'image/webp') types.push('image/jpeg')
  return types
}

const getCanvas = (image, maxDimension) => {
  const ratio = Math.min(1, maxDimension / Math.max(image.naturalWidth, image.naturalHeight))
  const width = Math.max(1, Math.round(image.naturalWidth * ratio))
  const height = Math.max(1, Math.round(image.naturalHeight * ratio))
  const canvas = document.createElement('canvas')
  canvas.width = width
  canvas.height = height
  const ctx = canvas.getContext('2d')
  ctx.drawImage(image, 0, 0, width, height)
  return canvas
}

const withFileExtension = (filename, type) => {
  const ext = type === 'image/webp' ? 'webp' : 'jpg'
  return String(filename || `image.${ext}`).replace(/\.[^.]+$/, '') + `.${ext}`
}

const getImageUploadMaxSize = async () => {
  if (cachedMaxUploadSize) return cachedMaxUploadSize
  try {
    const response = await fetch(IMAGE_CONFIG_URL, { credentials: 'include' })
    const res = await response.json()
    const maxSize = Number(res?.data?.maxSizeBytes)
    cachedMaxUploadSize = maxSize > 0 ? maxSize : DEFAULT_MAX_UPLOAD_SIZE
  } catch {
    cachedMaxUploadSize = DEFAULT_MAX_UPLOAD_SIZE
  }
  return cachedMaxUploadSize
}

const assertUploadSize = (file, maxSize) => {
  if (file.size > maxSize) {
    const message = `图片大小不能超过 ${formatSize(maxSize)}，请压缩后再上传`
    ElMessage.error(message)
    throw new Error(message)
  }
}

const assertUploadType = file => {
  if (!isSupportedImageFile(file)) {
    const message = getSupportedImageMessage()
    ElMessage.error(message)
    throw new Error(message)
  }
}

export const compressImageFile = async file => {
  if (!COMPRESSIBLE_TYPES.includes(file.type)) {
    return file
  }

  const image = await loadImage(file)
  let best = null
  const dimensions = [1920, 1600, 1400, 1200, 960]
    .filter(size => size <= MAX_DIMENSION && size >= MIN_DIMENSION)
  const qualities = [0.86, 0.82, 0.78, 0.74, 0.7]

  for (const dimension of dimensions) {
    const canvas = getCanvas(image, dimension)
    for (const type of getCompressedTypes(file)) {
      for (const quality of qualities) {
        const blob = await canvasToBlob(canvas, type, quality)
        const candidate = { blob, type, dimension, quality }
        if (!best || candidate.blob.size < best.blob.size) {
          best = candidate
        }
        if (blob.size <= TARGET_SIZE) {
          break
        }
      }
      if (best?.blob.size <= TARGET_SIZE) break
    }
    if (best?.blob.size <= TARGET_SIZE) break
  }

  if (!best || best.blob.size >= file.size) {
    ElMessage.info('压缩后体积未变小，将上传原图')
    return file
  }

  const compressedFile = new File([best.blob], withFileExtension(file.name, best.type), {
    type: best.type,
    lastModified: Date.now()
  })
  ElMessage.success(`图片已压缩：${formatSize(file.size)} -> ${formatSize(compressedFile.size)}`)
  return compressedFile
}

export const prepareImageUploadFormData = async (url, data) => {
  if (!shouldHandleImageUpload(url, data)) return data

  const file = getUploadFile(data)
  assertUploadType(file)
  const maxSize = await getImageUploadMaxSize()
  if (!COMPRESSIBLE_TYPES.includes(file.type)) {
    assertUploadSize(file, maxSize)
    return data
  }

  let shouldCompress = false
  try {
    await ElMessageBox.confirm(
      `当前图片大小为 ${formatSize(file.size)}，是否压缩后上传？`,
      '图片上传',
      {
        confirmButtonText: '压缩上传',
        cancelButtonText: '原图上传',
        distinguishCancelAndClose: true,
        type: 'warning'
      }
    )
    shouldCompress = true
  } catch (action) {
    if (action === 'cancel') {
      assertUploadSize(file, maxSize)
      return data
    }
    throw new Error('已取消上传')
  }

  const compressedFile = shouldCompress ? await compressImageFile(file) : file
  assertUploadSize(compressedFile, maxSize)
  if (compressedFile === file) return data

  const nextData = new FormData()
  data.forEach((value, key) => {
    nextData.append(key, key === 'file' ? compressedFile : value)
  })
  return nextData
}
