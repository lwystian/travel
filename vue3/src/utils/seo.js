const SITE_NAME = '侠客行国旅'
const DEFAULT_DESCRIPTION = '侠客行国旅提供精品旅游线路、景点攻略、住宿推荐和在线行程预订服务。'
const DEFAULT_ROBOTS = 'index,follow,max-image-preview:large'
const NOINDEX_ROBOTS = 'noindex,nofollow'

const stripHtml = (value = '') => String(value)
  .replace(/<[^>]*>/g, ' ')
  .replace(/&nbsp;/gi, ' ')
  .replace(/&amp;/gi, '&')
  .replace(/\s+/g, ' ')
  .trim()

const limitText = (value = '', maxLength = 155) => {
  const text = stripHtml(value)
  return text.length > maxLength ? text.slice(0, maxLength) : text
}

const ensureMeta = (selector, attrs) => {
  let element = document.head.querySelector(selector)
  if (!element) {
    element = document.createElement('meta')
    Object.entries(attrs).forEach(([key, value]) => element.setAttribute(key, value))
    document.head.appendChild(element)
  }
  return element
}

const setMetaContent = (selector, attrs, content) => {
  const element = ensureMeta(selector, attrs)
  element.setAttribute('content', content)
}

const removeElement = (selector) => {
  const element = document.head.querySelector(selector)
  if (element) {
    element.remove()
  }
}

const setCanonical = (url) => {
  let link = document.head.querySelector('link[rel="canonical"]')
  if (!link) {
    link = document.createElement('link')
    link.setAttribute('rel', 'canonical')
    document.head.appendChild(link)
  }
  link.setAttribute('href', url)
}

export const updateSeo = ({ title, description, path, image, indexable = true, type = 'website', schema } = {}) => {
  const finalTitle = title ? `${title} - ${SITE_NAME}` : SITE_NAME
  const finalDescription = limitText(description || DEFAULT_DESCRIPTION)
  const finalUrl = `${window.location.origin}${path || window.location.pathname}`
  const robots = indexable ? DEFAULT_ROBOTS : NOINDEX_ROBOTS

  document.title = finalTitle
  setCanonical(finalUrl)
  setMetaContent('meta[name="robots"]', { name: 'robots' }, robots)
  setMetaContent('meta[name="description"]', { name: 'description' }, finalDescription)
  setMetaContent('meta[name="twitter:card"]', { name: 'twitter:card' }, image ? 'summary_large_image' : 'summary')
  setMetaContent('meta[name="twitter:title"]', { name: 'twitter:title' }, finalTitle)
  setMetaContent('meta[name="twitter:description"]', { name: 'twitter:description' }, finalDescription)
  setMetaContent('meta[property="og:type"]', { property: 'og:type' }, type)
  setMetaContent('meta[property="og:title"]', { property: 'og:title' }, finalTitle)
  setMetaContent('meta[property="og:description"]', { property: 'og:description' }, finalDescription)
  setMetaContent('meta[property="og:url"]', { property: 'og:url' }, finalUrl)
  if (image) {
    const imageUrl = image.startsWith('http') ? image : `${window.location.origin}${image.startsWith('/') ? image : `/${image}`}`
    setMetaContent('meta[property="og:image"]', { property: 'og:image' }, imageUrl)
    setMetaContent('meta[name="twitter:image"]', { name: 'twitter:image' }, imageUrl)
  } else {
    removeElement('meta[property="og:image"]')
    removeElement('meta[name="twitter:image"]')
  }

  const schemaElementId = 'page-structured-data'
  removeElement(`#${schemaElementId}`)
  if (schema) {
    const script = document.createElement('script')
    script.type = 'application/ld+json'
    script.id = schemaElementId
    script.textContent = JSON.stringify(schema)
    document.head.appendChild(script)
  }
}

export const routeDescription = (route) => route?.meta?.description || DEFAULT_DESCRIPTION
export const isRouteIndexable = (route) => route?.matched?.every(record => record.meta?.indexable !== false && !record.meta?.requiresAuth) !== false
export const seoDescription = limitText
