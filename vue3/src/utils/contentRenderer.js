import { sanitizeHtml } from '@/utils/sanitizeHtml'

const MARKDOWN_PREFIX = '<!--markdown-->'

export const encodeMarkdownContent = (value) => `${MARKDOWN_PREFIX}\n${value || ''}`

export const isMarkdownContent = (value) => String(value || '').startsWith(MARKDOWN_PREFIX)

export const decodeMarkdownContent = (value) => {
  const text = String(value || '')
  return isMarkdownContent(text) ? text.slice(MARKDOWN_PREFIX.length).replace(/^\r?\n/, '') : text
}

const escapeHtml = (value) => String(value || '')
  .replace(/&/g, '&amp;')
  .replace(/</g, '&lt;')
  .replace(/>/g, '&gt;')
  .replace(/"/g, '&quot;')

const renderInlineMarkdown = (value) => value
  .replace(/!\[(.*?)\]\((https?:\/\/.*?|\/.*?)\)/g, '<img src="$2" alt="$1">')
  .replace(/~~(.*?)~~/g, '<del>$1</del>')
  .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
  .replace(/\*(.*?)\*/g, '<em>$1</em>')
  .replace(/`([^`]+)`/g, '<code>$1</code>')
  .replace(/\[(.*?)\]\((https?:\/\/.*?)\)/g, '<a href="$2" target="_blank" rel="noopener noreferrer">$1</a>')

const splitTableRow = (line) => line
  .trim()
  .replace(/^\|/, '')
  .replace(/\|$/, '')
  .split('|')
  .map(cell => renderInlineMarkdown(cell.trim()))

const isTableDivider = (line) => /^\s*\|?\s*:?-{3,}:?\s*(\|\s*:?-{3,}:?\s*)+\|?\s*$/.test(line)

const isTableHeader = (lines, index) => {
  if (index + 1 >= lines.length) return false
  return lines[index].includes('|') && isTableDivider(lines[index + 1])
}

const renderTable = (lines, startIndex, html) => {
  const headers = splitTableRow(lines[startIndex])
  let index = startIndex + 2
  const rows = []

  while (index < lines.length && lines[index].includes('|') && lines[index].trim()) {
    rows.push(splitTableRow(lines[index]))
    index++
  }

  html.push('<div class="content-table-wrap"><table><thead><tr>')
  headers.forEach(cell => html.push(`<th>${cell}</th>`))
  html.push('</tr></thead><tbody>')
  rows.forEach(row => {
    html.push('<tr>')
    headers.forEach((_, cellIndex) => html.push(`<td>${row[cellIndex] || ''}</td>`))
    html.push('</tr>')
  })
  html.push('</tbody></table></div>')

  return index
}

export const renderMarkdown = (value) => {
  const lines = escapeHtml(value).replace(/\r\n/g, '\n').split('\n')
  const html = []
  let inList = false
  let listType = ''
  let inCode = false
  let codeLines = []

  const closeList = () => {
    if (inList) {
      html.push(`</${listType}>`)
      inList = false
      listType = ''
    }
  }

  for (let i = 0; i < lines.length; i++) {
    const line = lines[i]
    const text = line.trim()

    if (/^```/.test(text)) {
      closeList()
      if (inCode) {
        html.push(`<pre><code>${codeLines.join('\n')}</code></pre>`)
        codeLines = []
        inCode = false
      } else {
        inCode = true
      }
      continue
    }

    if (inCode) {
      codeLines.push(line)
      continue
    }

    if (isTableHeader(lines, i)) {
      closeList()
      i = renderTable(lines, i, html) - 1
      continue
    }

    if (/^[-*]\s+/.test(text)) {
      if (!inList || listType !== 'ul') {
        closeList()
        html.push('<ul>')
        inList = true
        listType = 'ul'
      }
      html.push(`<li>${renderInlineMarkdown(text.replace(/^[-*]\s+/, ''))}</li>`)
      continue
    }

    if (/^\d+\.\s+/.test(text)) {
      if (!inList || listType !== 'ol') {
        closeList()
        html.push('<ol>')
        inList = true
        listType = 'ol'
      }
      html.push(`<li>${renderInlineMarkdown(text.replace(/^\d+\.\s+/, ''))}</li>`)
      continue
    }

    closeList()
    if (!text) continue
    if (/^---+$/.test(text)) html.push('<hr>')
    else if (/^###\s+/.test(text)) html.push(`<h3>${renderInlineMarkdown(text.replace(/^###\s+/, ''))}</h3>`)
    else if (/^##\s+/.test(text)) html.push(`<h2>${renderInlineMarkdown(text.replace(/^##\s+/, ''))}</h2>`)
    else if (/^#\s+/.test(text)) html.push(`<h1>${renderInlineMarkdown(text.replace(/^#\s+/, ''))}</h1>`)
    else if (/^>\s+/.test(text)) html.push(`<blockquote>${renderInlineMarkdown(text.replace(/^>\s+/, ''))}</blockquote>`)
    else html.push(`<p>${renderInlineMarkdown(text)}</p>`)
  }

  closeList()
  if (inCode) html.push(`<pre><code>${codeLines.join('\n')}</code></pre>`)
  return html.join('')
}

const looksLikeHtml = (value) => /<\/?[a-z][\s\S]*>/i.test(String(value || ''))

export const renderContent = (value) => {
  const text = String(value || '')
  if (!text.trim()) return ''
  if (isMarkdownContent(text)) return sanitizeHtml(renderMarkdown(decodeMarkdownContent(text)))
  if (looksLikeHtml(text)) return sanitizeHtml(text)
  return sanitizeHtml(renderMarkdown(text))
}
