import { ElMessage } from 'element-plus'

const IMAGE_EXTENSIONS = ['jpg', 'jpeg', 'png', 'webp', 'gif', 'bmp', 'svg']
const DROP_ZONE_SELECTOR = [
  '.avatar-frame',
  '.hero-profile',
  '.account-hero',
  '.cover-uploader-container',
  '.cover-upload',
  '.cover-uploader',
  '.cover-preview',
  '.cover-preview-container',
  '.image-item',
  '.asset-card',
  '.media-editor',
  '.legal-image-editor__row',
  '.qualification-image',
  '.upload-container',
  '.avatar-uploader',
  '.image-uploader',
  '.carousel-uploader',
  '.scenic-uploader',
  '.el-form-item'
].join(',')

let installed = false
let activeUpload = null
let activeZone = null
let dragDepth = 0
let lastOutsideDropWarnAt = 0

const isImageFile = (file) => {
  if (!file) return false
  if (file.type && file.type.startsWith('image/')) return true
  const extension = file.name?.split('.').pop()?.toLowerCase()
  return IMAGE_EXTENSIONS.includes(extension)
}

const isFileDrag = (dataTransfer) => {
  return Array.from(dataTransfer?.types || []).includes('Files')
}

const acceptsImage = (input) => {
  const accept = (input?.getAttribute('accept') || '').toLowerCase()
  if (!accept) return true
  if (accept.includes('image/')) return true
  return IMAGE_EXTENSIONS.some((ext) => accept.includes(`.${ext}`))
}

const getUploadInput = (upload) => {
  const input = upload?.querySelector('input[type="file"]')
  if (!input || input.disabled || !acceptsImage(input)) return null
  return input
}

const hasImagePreview = (element) => {
  return Boolean(element?.querySelector('img, .el-image, .el-avatar, video, canvas, svg'))
}

const getDropZone = (upload) => {
  const candidates = []
  let current = upload

  while (current && current !== document.body) {
    if (current.matches?.(DROP_ZONE_SELECTOR)) {
      candidates.push(current)
    }
    current = current.parentElement
  }

  const previewCandidate = candidates.find((item) => hasImagePreview(item) && item.getBoundingClientRect().height >= 40)
  return previewCandidate || candidates[0] || upload
}

const getImageUploadTargets = () => {
  return Array.from(document.querySelectorAll('.el-upload'))
    .map((upload) => {
      const input = getUploadInput(upload)
      if (!input) return null
      return {
        upload,
        input,
        zone: getDropZone(upload)
      }
    })
    .filter(Boolean)
}

const findUploadTarget = (eventTarget) => {
  if (!(eventTarget instanceof Element)) return null

  const upload = eventTarget.closest('.el-upload')
  const input = getUploadInput(upload)
  if (input) {
    return { upload, input, zone: getDropZone(upload) }
  }

  const containingTargets = getImageUploadTargets()
    .filter(({ zone }) => zone.contains(eventTarget))
    .sort((a, b) => {
      const aRect = a.zone.getBoundingClientRect()
      const bRect = b.zone.getBoundingClientRect()
      return (aRect.width * aRect.height) - (bRect.width * bRect.height)
    })

  return containingTargets[0] || null
}

const showDropZones = () => {
  document.body.classList.add('is-image-file-dragging')
  getImageUploadTargets().forEach(({ zone }) => {
    zone.classList.add('is-image-drop-ready')
  })
}

const hideDropZones = () => {
  document.body.classList.remove('is-image-file-dragging')
  document.querySelectorAll('.is-image-drop-ready').forEach((el) => {
    el.classList.remove('is-image-drop-ready')
  })
}

const setActiveUpload = (target) => {
  const upload = target?.upload
  const zone = target?.zone || upload
  if (!upload) return

  if (activeUpload && activeUpload !== upload) {
    activeUpload.classList.remove('is-image-dragover')
  }
  if (activeZone && activeZone !== zone) {
    activeZone.classList.remove('is-image-drop-active')
  }

  activeUpload = upload
  activeZone = zone
  activeUpload.classList.add('is-image-dragover')
  activeZone.classList.add('is-image-drop-active')
}

const clearActiveUpload = () => {
  if (activeUpload) {
    activeUpload.classList.remove('is-image-dragover')
    activeUpload = null
  }
  if (activeZone) {
    activeZone.classList.remove('is-image-drop-active')
    activeZone = null
  }
}

const endDragSession = () => {
  dragDepth = 0
  clearActiveUpload()
  hideDropZones()
}

const transferFileToUploadInput = (input, file) => {
  const dataTransfer = new DataTransfer()
  dataTransfer.items.add(file)
  input.files = dataTransfer.files
  input.dispatchEvent(new Event('change', { bubbles: true }))
}

export const installGlobalImageDropUpload = () => {
  if (installed || typeof window === 'undefined') return
  installed = true

  document.addEventListener('dragenter', (event) => {
    if (!isFileDrag(event.dataTransfer)) return

    dragDepth += 1
    event.preventDefault()
    showDropZones()

    const target = findUploadTarget(event.target)
    if (!target) return
    event.stopPropagation()
    setActiveUpload(target)
  }, true)

  document.addEventListener('dragover', (event) => {
    if (!isFileDrag(event.dataTransfer)) return

    event.preventDefault()
    event.dataTransfer.dropEffect = 'copy'
    showDropZones()

    const target = findUploadTarget(event.target)
    if (!target) {
      clearActiveUpload()
      return
    }

    event.stopPropagation()
    setActiveUpload(target)
  }, true)

  document.addEventListener('dragleave', (event) => {
    if (!isFileDrag(event.dataTransfer)) return

    dragDepth = Math.max(0, dragDepth - 1)
    const nextTarget = event.relatedTarget
    if (activeZone && (!nextTarget || !activeZone.contains(nextTarget))) {
      clearActiveUpload()
    }
    if (dragDepth === 0 || !nextTarget) {
      hideDropZones()
    }
  }, true)

  document.addEventListener('drop', (event) => {
    if (!isFileDrag(event.dataTransfer)) return

    const target = findUploadTarget(event.target)
    const files = Array.from(event.dataTransfer?.files || [])
    if (!target) {
      if (files.some(isImageFile)) {
        event.preventDefault()
        event.stopPropagation()
        endDragSession()
        const now = Date.now()
        if (now - lastOutsideDropWarnAt > 1800) {
          lastOutsideDropWarnAt = now
          ElMessage.warning('请将图片拖拽到高亮的上传区域')
        }
      }
      return
    }

    event.preventDefault()
    event.stopPropagation()
    endDragSession()

    if (!files.length) return

    const imageFiles = files.filter(isImageFile)
    if (!imageFiles.length) {
      ElMessage.error('请拖拽图片文件上传')
      return
    }
    if (files.length > 1) {
      ElMessage.warning('一次只能拖拽上传一张图片，已使用第一张图片')
    }

    transferFileToUploadInput(target.input, imageFiles[0])
  }, true)

  window.addEventListener('dragend', endDragSession)
  window.addEventListener('blur', endDragSession)
}
