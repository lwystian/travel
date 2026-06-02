const ALLOWED_TAGS = new Set([
  'A', 'B', 'BLOCKQUOTE', 'BR', 'CODE', 'DEL', 'DIV', 'EM', 'H1', 'H2', 'H3',
  'H4', 'H5', 'H6', 'HR', 'I', 'IMG', 'LI', 'OL', 'P', 'PRE', 'SPAN',
  'STRONG', 'TABLE', 'TBODY', 'TD', 'TH', 'THEAD', 'TR', 'U', 'UL'
])

const ALLOWED_ATTRS = {
  A: new Set(['href', 'target', 'rel', 'title']),
  IMG: new Set(['src', 'alt', 'title']),
  '*': new Set(['class'])
}

const isSafeUrl = (value) => {
  const url = String(value || '').trim()
  return url.startsWith('/') || url.startsWith('http://') || url.startsWith('https://') || url.startsWith('data:image/')
}

export const sanitizeHtml = (html) => {
  if (typeof window === 'undefined' || !window.DOMParser) {
    return String(html || '')
      .replace(/<script[\s\S]*?>[\s\S]*?<\/script>/gi, '')
      .replace(/\son\w+="[^"]*"/gi, '')
      .replace(/\son\w+='[^']*'/gi, '')
      .replace(/javascript:/gi, '')
  }

  const parser = new window.DOMParser()
  const doc = parser.parseFromString(`<div>${html || ''}</div>`, 'text/html')

  const cleanNode = (node) => {
    Array.from(node.childNodes).forEach(child => {
      if (child.nodeType === Node.ELEMENT_NODE) {
        if (!ALLOWED_TAGS.has(child.tagName)) {
          child.replaceWith(...Array.from(child.childNodes))
          return
        }

        Array.from(child.attributes).forEach(attr => {
          const allowed = ALLOWED_ATTRS[child.tagName]?.has(attr.name) || ALLOWED_ATTRS['*'].has(attr.name)
          const isUrlAttr = attr.name === 'href' || attr.name === 'src'
          if (!allowed || attr.name.startsWith('on') || (isUrlAttr && !isSafeUrl(attr.value))) {
            child.removeAttribute(attr.name)
          }
        })

        if (child.tagName === 'A') {
          child.setAttribute('rel', 'noopener noreferrer')
          if (!child.getAttribute('target')) child.setAttribute('target', '_blank')
        }
        cleanNode(child)
      } else if (child.nodeType !== Node.TEXT_NODE) {
        child.remove()
      }
    })
  }

  cleanNode(doc.body)
  return doc.body.firstChild?.innerHTML || ''
}
