export const maskPhone = (phone, emptyText = '未绑定') => {
  if (!phone) return emptyText
  const value = String(phone)
  if (value.includes('*')) return value
  if (/^1[3-9]\d{9}$/.test(value)) {
    return value.replace(/^(\d{3})\d{4}(\d{4})$/, '$1****$2')
  }
  if (value.length <= 4) return '*'.repeat(value.length)
  return `${value.slice(0, 2)}****${value.slice(-2)}`
}

export const maskEmail = (email, emptyText = '未绑定') => {
  if (!email) return emptyText
  const value = String(email)
  if (value.includes('*')) return value
  const [name, domain] = value.split('@')
  if (!name || !domain) return value.length <= 4 ? '****' : `${value.slice(0, 2)}***`
  const visible = name.length <= 2 ? name.slice(0, 1) : name.slice(0, 2)
  return `${visible}${'*'.repeat(Math.max(2, Math.min(5, name.length)))}@${domain}`
}
