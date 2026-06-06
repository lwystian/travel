import chinaRegions from '@/assets/中国地区数据.json'

export const regionCascaderProps = {
  checkStrictly: true,
  emitPath: true,
  expandTrigger: 'hover'
}

export const leafRegionCascaderProps = {
  checkStrictly: false,
  emitPath: true,
  expandTrigger: 'hover'
}

export const chinaRegionOptions = chinaRegions.map(province => ({
  value: province.province,
  label: province.province,
  children: (province.citys || []).map(city => ({
    value: city.city,
    label: city.city,
    children: (city.areas || []).map(area => ({
      value: area.area,
      label: area.area
    }))
  }))
}))

const normalizeRegionName = (name) => {
  if (!name) return ''
  return String(name)
    .replace(/特别行政区$/, '')
    .replace(/壮族自治区$/, '')
    .replace(/回族自治区$/, '')
    .replace(/维吾尔自治区$/, '')
    .replace(/自治区$/, '')
    .replace(/省$/, '')
    .replace(/市$/, '')
    .replace(/地区$/, '')
    .replace(/盟$/, '')
    .replace(/区$/, '')
    .trim()
}

export const getRegionKeyword = (path) => {
  if (!Array.isArray(path) || path.length === 0) return ''
  const selectedName = path[path.length - 1] || ''
  return normalizeRegionName(selectedName) || selectedName
}

export const getRegionLabel = (path, separator = ' / ') => {
  if (!Array.isArray(path) || path.length === 0) return ''
  return path.filter(Boolean).join(separator)
}

export const selectRegionOnExpand = (path, setter) => {
  if (!Array.isArray(path) || path.length === 0 || typeof setter !== 'function') return
  setter([...path])
}

export const findRegionPath = (value) => {
  if (!value) return []
  const text = String(value).trim()
  const parts = text.split(/\s*[-/·]\s*|\s+-\s+/).filter(Boolean)
  for (const province of chinaRegionOptions) {
    if (matchesRegion(province.value, text, parts[0])) {
      if (parts.length === 1) return [province.value]
      const cityPath = findCityPath(province, text, parts)
      return cityPath || [province.value]
    }
    const cityPath = findCityPath(province, text, parts)
    if (cityPath) return cityPath
  }
  return []
}

const findCityPath = (province, text, parts) => {
  for (const city of province.children || []) {
    if (matchesRegion(city.value, text, parts[1] || parts[0])) {
      const areaPath = findAreaPath(province, city, text, parts)
      return areaPath || [province.value, city.value]
    }
    const areaPath = findAreaPath(province, city, text, parts)
    if (areaPath) return areaPath
  }
  return null
}

const findAreaPath = (province, city, text, parts) => {
  for (const area of city.children || []) {
    if (matchesRegion(area.value, text, parts[2] || parts[1] || parts[0])) {
      return [province.value, city.value, area.value]
    }
  }
  return null
}

const matchesRegion = (regionName, fullText, partText) => {
  const normalizedRegion = normalizeRegionName(regionName)
  const normalizedFull = normalizeRegionName(fullText)
  const normalizedPart = normalizeRegionName(partText)
  return regionName === fullText ||
    regionName === partText ||
    normalizedRegion === normalizedFull ||
    normalizedRegion === normalizedPart
}
