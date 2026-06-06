const ALLOWED_TAGS = new Set([
  'A', 'B', 'BLOCKQUOTE', 'BR', 'CODE', 'DEL', 'DIV', 'EM', 'H1', 'H2', 'H3',
  'H4', 'H5', 'H6', 'HR', 'I', 'IMG', 'LI', 'OL', 'P', 'PRE', 'SPAN',
  'STRONG', 'TABLE', 'TBODY', 'TD', 'TH', 'THEAD', 'TR', 'U', 'UL'
])

const ALLOWED_ATTRS = {
  A: new Set(['href', 'target', 'rel', 'title']),
  IMG: new Set(['src', 'alt', 'title']),
  '*': new Set(['class', 'id', 'style'])
}

const isSafeId = (value) => /^[A-Za-z][\w:-]{0,80}$/.test(String(value || ''))

const ALLOWED_STYLE_PROPS = new Set([
  'background-color',
  'color',
  'font-size',
  'font-style',
  'font-weight',
  'line-height',
  'text-align',
  'text-decoration',
  'text-indent'
])

const isSafeStyleValue = (value) => {
  const text = String(value || '').trim()
  return text &&
    text.length <= 80 &&
    !/[<>{}]/.test(text) &&
    !/url\s*\(|expression\s*\(|javascript:/i.test(text)
}

const sanitizeStyle = (value) => {
  const declarations = String(value || '').split(';')
  const safeDeclarations = []

  declarations.forEach(declaration => {
    const separatorIndex = declaration.indexOf(':')
    if (separatorIndex <= 0) return

    const property = declaration.slice(0, separatorIndex).trim().toLowerCase()
    const styleValue = declaration.slice(separatorIndex + 1).trim()
    if (!ALLOWED_STYLE_PROPS.has(property) || !isSafeStyleValue(styleValue)) return

    safeDeclarations.push(`${property}: ${styleValue}`)
  })

  return safeDeclarations.join('; ')
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
          const invalidId = attr.name === 'id' && !isSafeId(attr.value)
          if (!allowed || attr.name.startsWith('on') || invalidId || (isUrlAttr && !isSafeUrl(attr.value))) {
            child.removeAttribute(attr.name)
          } else if (attr.name === 'style') {
            const safeStyle = sanitizeStyle(attr.value)
            if (safeStyle) child.setAttribute('style', safeStyle)
            else child.removeAttribute('style')
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
