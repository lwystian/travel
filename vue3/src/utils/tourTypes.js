export const tourTypeOptions = [
  { value: 'around', label: '周边游' },
  { value: 'long', label: '长线游' },
  { value: 'team', label: '跟团游' },
  { value: 'free', label: '自由行' },
  { value: 'private', label: '私家团' },
  { value: 'custom', label: '定制游' },
  { value: 'local', label: '当地参团' },
  { value: 'selfdrive', label: '自驾游' },
  { value: 'parent_child', label: '亲子游' },
  { value: 'study', label: '研学游' },
  { value: 'photography', label: '摄影游' },
  { value: 'outdoor', label: '户外徒步' },
  { value: 'cruise', label: '邮轮出行' },
  { value: 'wellness', label: '康养度假' },
  { value: 'other', label: '其它' }
]

export const tourTypeMap = tourTypeOptions.reduce((map, item) => {
  map[item.value] = item.label
  return map
}, {})

export const getTourTypeLabel = (value, fallback = '精选游') => tourTypeMap[value] || value || fallback
