<template>
  <div class="frontend-layout">
    <!-- 顶部导航栏 - 三层结构 -->
    <header class="header">
      <!-- 第一层：顶部信息栏 -->
      <div class="header-top">
        <div class="header-top-container">
          <!-- 左侧：欢迎语 -->
          <div class="welcome-message">
            <span>您好，欢迎来到重庆侠客行国际旅行社官网!</span>
          </div>

          <!-- 右侧：用户链接 -->
          <div class="top-links">
            <template v-if="isLoggedIn">
              <el-dropdown trigger="click" @command="handleCommand">
                <span class="welcome-text user-dropdown">
                  <el-icon><User /></el-icon>
                  您好，{{ userName || '用户' }}
                  <el-icon class="arrow-icon"><ArrowDown /></el-icon>
                </span>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item command="profile">
                      <el-icon><User /></el-icon>
                      个人中心
                    </el-dropdown-item>
                    <el-dropdown-item command="orders">
                      <el-icon><Tickets /></el-icon>
                      我的订单
                    </el-dropdown-item>
                    <el-dropdown-item command="collection">
                      <el-icon><Star /></el-icon>
                      我的收藏
                    </el-dropdown-item>
                    <el-dropdown-item command="guide">
                      <el-icon><Reading /></el-icon>
                      我的攻略
                    </el-dropdown-item>
                    <el-dropdown-divider />
                    <el-dropdown-item command="logout" divided>
                      <el-icon><SwitchButton /></el-icon>
                      退出登录
                    </el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </template>
            <template v-else>
              <router-link to="/login" class="top-link">会员登录</router-link>
              <el-divider direction="vertical" />
              <router-link to="/register" class="top-link">会员注册</router-link>
              <el-divider direction="vertical" />
            </template>
            <span class="top-link favorite-link" @click="handleAddFavorite">
              <el-icon><Star /></el-icon>
              加入收藏
            </span>
            <el-divider direction="vertical" />
            <router-link to="/about" class="top-link">关于侠客行</router-link>
            <el-divider direction="vertical" />
            <span class="wechat-link" @mouseenter="showWechatQR = true" @mouseleave="showWechatQR = false">
              <el-icon><ChatDotRound /></el-icon>
              微信
              <div v-if="showWechatQR" class="wechat-qrcode">
                <img src="@/assets/logo.png" alt="微信公众号" @error="handleQRCodeError" />
                <p>扫码关注微信公众号</p>
              </div>
            </span>
          </div>
        </div>
      </div>

      <!-- 第二层：Logo和搜索框 -->
      <div class="header-middle">
        <div class="header-middle-container">
          <router-link to="/" class="logo-section">
            <img src="@/assets/logo.png" class="logo-icon" />
            <div class="logo-text-group">
              <h1 class="logo-title">侠客行国旅</h1>
              <p class="logo-subtitle">XIAKEXING TRAVEL</p>
            </div>
          </router-link>

          <!-- 位置选择器移到搜索框左边 -->
          <div class="search-bar-wrapper">
            <div class="location-selector">
              <el-icon><Location /></el-icon>
              <el-dropdown trigger="click" @command="handleLocationChange">
                <span class="current-location">
                  {{ currentCity }}
                  <el-icon class="arrow-icon"><ArrowDown /></el-icon>
                </span>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item
                      v-for="city in cityList"
                      :key="city.code"
                      :command="city.code"
                      :class="{ 'is-active': currentCity === city.name }"
                    >
                      {{ city.name }}
                    </el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </div>

            <!-- 智能搜索框 -->
            <div class="smart-search-wrapper" ref="searchContainer">
              <!-- 搜索类别下拉 -->
              <el-dropdown trigger="click" @command="handleCategoryChange" class="category-dropdown">
                <div class="category-selector">
                  <span>{{ currentCategory.label }}</span>
                  <el-icon class="arrow-icon"><ArrowDown /></el-icon>
                </div>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item
                      v-for="cat in searchCategories"
                      :key="cat.value"
                      :command="cat.value"
                      :class="{ 'is-active': currentCategory.value === cat.value }"
                    >
                      <el-icon><component :is="cat.icon" /></el-icon>
                      {{ cat.label }}
                    </el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>

              <el-input
                v-model="searchKeyword"
                :placeholder="getPlaceholder()"
                class="search-input"
                @input="handleSearchInput"
                @keydown="handleSearchKeydown"
                @focus="handleSearchFocus"
                @blur="handleSearchBlur"
              >
                <template #prefix>
                  <el-icon><Search /></el-icon>
                </template>
              </el-input>
              <button class="search-btn" @click="handleSearchSubmit">
                <el-icon><Search /></el-icon>
                <span>搜索</span>
              </button>

              <!-- 搜索建议下拉框 -->
              <div
                v-show="showSearchSuggestions && hasSuggestions"
                class="suggestions-dropdown"
                @mousedown.prevent
              >
                <!-- 线路推荐（综合模式或线路模式显示） -->
                <div v-if="searchSuggestions.lines.length > 0 && (currentCategory.value === 'all' || currentCategory.value === 'line')" class="suggestion-section">
                  <div class="section-title">
                    <el-icon><Tickets /></el-icon>
                    <span>线路推荐</span>
                  </div>
                  <div
                    v-for="(item, index) in searchSuggestions.lines"
                    :key="`line-${item.id}`"
                    class="suggestion-item"
                    :class="{ active: selectedSuggestionIndex === getLineIndex(index) }"
                    @click="goToTicketDetail(item.id)"
                    @mouseenter="selectedSuggestionIndex = getLineIndex(index)"
                  >
                    <div class="item-image">
                      <img
                        :src="getImageUrl(item.ticketImage)"
                        :alt="item.ticketName"
                        @error="$event.target.src = 'https://via.placeholder.com/48x48?text=No+Image'"
                      />
                    </div>
                    <div class="item-content">
                      <div class="item-title">{{ item.ticketName }}</div>
                      <div class="item-subtitle">
                        <el-icon><Ticket /></el-icon>
                        {{ item.description || '精彩线路' }}
                        <span v-if="item.discountPrice && item.discountPrice > 0" class="price">¥{{ item.discountPrice }}</span>
                        <span v-else-if="item.price && item.price > 0" class="price">¥{{ item.price }}</span>
                      </div>
                    </div>
                  </div>
                </div>

                <!-- 景点推荐（综合模式或景点模式显示） -->
                <div v-if="searchSuggestions.scenics.length > 0 && (currentCategory.value === 'all' || currentCategory.value === 'scenic')" class="suggestion-section">
                  <div class="section-title">
                    <el-icon><Location /></el-icon>
                    <span>景点推荐</span>
                  </div>
                  <div
                    v-for="(item, index) in searchSuggestions.scenics"
                    :key="`scenic-${item.id}`"
                    class="suggestion-item"
                    :class="{ active: selectedSuggestionIndex === index }"
                    @click="goToScenicDetail(item.id)"
                    @mouseenter="selectedSuggestionIndex = index"
                  >
                    <div class="item-image">
                      <img
                        :src="getImageUrl(item.imageUrl)"
                        :alt="item.name"
                        @error="$event.target.src = 'https://via.placeholder.com/48x48?text=No+Image'"
                      />
                    </div>
                    <div class="item-content">
                      <div class="item-title">{{ item.name }}</div>
                      <div class="item-subtitle">
                        <el-icon><Location /></el-icon>
                        {{ item.location || '未知地区' }}
                        <span v-if="item.price && item.price > 0" class="price">¥{{ item.price }}</span>
                        <span v-else class="price free">免费</span>
                      </div>
                    </div>
                  </div>
                </div>

                <!-- 攻略推荐（综合模式或攻略模式显示） -->
                <div v-if="searchSuggestions.guides.length > 0 && (currentCategory.value === 'all' || currentCategory.value === 'guide')" class="suggestion-section">
                  <div class="section-title">
                    <el-icon><Document /></el-icon>
                    <span>攻略推荐</span>
                  </div>
                  <div
                    v-for="(item, index) in searchSuggestions.guides"
                    :key="`guide-${item.id}`"
                    class="suggestion-item"
                    :class="{ active: selectedSuggestionIndex === getGuideIndex(index) }"
                    @click="goToGuideDetail(item.id)"
                    @mouseenter="selectedSuggestionIndex = getGuideIndex(index)"
                  >
                    <div class="item-image">
                      <img
                        :src="getImageUrl(item.coverImage)"
                        :alt="item.title"
                        @error="$event.target.src = 'https://via.placeholder.com/48x48?text=No+Image'"
                      />
                    </div>
                    <div class="item-content">
                      <div class="item-title">{{ item.title }}</div>
                      <div class="item-subtitle">
                        <el-icon><User /></el-icon>
                        {{ item.userNickname || '匿名用户' }}
                        <span class="views">{{ item.views || 0 }}阅读</span>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 第三层：主导航菜单 -->
      <div class="header-bottom">
        <div class="header-bottom-container">
          <el-menu
            mode="horizontal"
            :ellipsis="false"
            class="main-menu"
            :router="true"
            :default-active="activeMenuIndex"
          >
            <el-menu-item index="/">
              <el-icon><HomeFilled /></el-icon>
              <span>首页</span>
            </el-menu-item>

            <el-menu-item index="/scenic">
              <el-icon><Bicycle /></el-icon>
              <span>景点</span>
            </el-menu-item>

            <el-menu-item index="/guide">
              <el-icon><Reading /></el-icon>
              <span>攻略</span>
            </el-menu-item>

            <el-menu-item index="/accommodation">
              <el-icon><House /></el-icon>
              <span>住宿</span>
            </el-menu-item>

            <el-sub-menu index="/around">
              <template #title>
                <el-icon><Place /></el-icon>
                <span>周边游</span>
              </template>
              <el-menu-item-group>
                <template #title>选择出游天数</template>
                <el-menu-item @click="goToAroundTour('1')">一日游</el-menu-item>
                <el-menu-item @click="goToAroundTour('2')">二日游</el-menu-item>
                <el-menu-item @click="goToAroundTour('3')">三日游</el-menu-item>
                <el-menu-item @click="goToAroundTour('4')">四日游</el-menu-item>
                <el-menu-item @click="goToAroundTour('5')">五日游</el-menu-item>
              </el-menu-item-group>
            </el-sub-menu>

            <el-sub-menu index="/cruise">
              <template #title>
                <el-icon><Ship /></el-icon>
                <span>邮轮出行</span>
              </template>
              <el-menu-item-group>
                <template #title>选择邮轮类型</template>
                <el-menu-item @click="goToCruise('sanyan')">三峡邮轮</el-menu-item>
                <el-menu-item @click="goToCruise('xisha')">西沙邮轮</el-menu-item>
              </el-menu-item-group>
            </el-sub-menu>

            <el-menu-item index="/tickets">
              <el-icon><Ticket /></el-icon>
              <span>行程预订</span>
            </el-menu-item>
          </el-menu>
        </div>
      </div>
    </header>

    <!-- 主要内容区域 -->
    <el-main class="main-content">
      <router-view />
    </el-main>

    <!-- 页脚 -->
    <footer class="footer">
      <div class="footer-content">
        <p class="copyright">&copy; 2021-2026 侠客行国旅 | 版权所有</p>
      </div>
    </footer>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useUserStore } from '@/store/user'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'
import {
  HomeFilled,
  User,
  Bicycle,
  Reading,
  Notebook,
  Star,
  SwitchButton,
  Key,
  Ticket,
  Tickets,
  House,
  Place,
  Ship,
  ArrowDown,
  Location,
  Search,
  ChatDotRound,
  Document,
  Grid
} from '@element-plus/icons-vue'

const userStore = useUserStore()
const router = useRouter()
const route = useRoute()
const baseAPI = process.env.VUE_APP_BASE_API || '/api'

// 位置相关
const currentCity = ref('重庆')
const showWechatQR = ref(false)

const cityList = ref([
  { code: '500000', name: '重庆' },
  { code: '420500', name: '宜昌' },
  { code: '460100', name: '海口' },
  { code: '460200', name: '三亚' },
  { code: '510100', name: '成都' },
  { code: '500100', name: '万州' },
  { code: '500200', name: '涪陵' },
  { code: '421200', name: '恩施' },
  { code: '430100', name: '长沙' },
  { code: '520100', name: '贵阳' },
  { code: '530100', name: '昆明' },
  { code: '310100', name: '上海' },
  { code: '110000', name: '北京' },
  { code: '440100', name: '广州' },
  { code: '440300', name: '深圳' },
  { code: '320100', name: '南京' }
])

// 搜索相关变量
const searchKeyword = ref('')
const searchContainer = ref(null)
const showSearchSuggestions = ref(false)
const selectedSuggestionIndex = ref(-1)
const searchSuggestions = reactive({
  scenics: [],
  guides: [],
  lines: []
})

// 搜索类别 - 添加综合（默认）
const searchCategories = [
  { value: 'all', label: '综合', icon: 'Grid' },
  { value: 'line', label: '线路', icon: 'Tickets' },
  { value: 'scenic', label: '景点', icon: 'Bicycle' },
  { value: 'guide', label: '攻略', icon: 'Reading' }
]
const currentCategory = ref(searchCategories[0])

const hasSuggestions = computed(() => {
  return searchSuggestions.scenics.length > 0 || searchSuggestions.guides.length > 0 || searchSuggestions.lines.length > 0
})

// 防抖定时器
let searchDebounceTimer = null

const isLoggedIn = computed(() => !!userStore.token)
const userName = computed(() => userStore.userInfo?.name || userStore.userInfo?.username || '')

// 计算当前激活的菜单索引
const activeMenuIndex = computed(() => {
  const path = route.path
  if (path === '/') return '/'
  if (path.startsWith('/scenic')) return '/scenic'
  if (path.startsWith('/guide')) return '/guide'
  if (path.startsWith('/accommodation')) return '/accommodation'
  if (path.startsWith('/around')) return '/around'
  if (path.startsWith('/tickets') || path.startsWith('/ticket/booking')) return '/tickets'
  return '/'
})

// 获取图片完整URL
const getImageUrl = (url) => {
  if (!url) return 'https://via.placeholder.com/48x48?text=No+Image'
  return url.startsWith('http') ? url : baseAPI + url
}

// 获取占位符文本
const getPlaceholder = () => {
  switch (currentCategory.value.value) {
    case 'all': return '搜索旅行线路、景点、攻略...'
    case 'line': return '搜索线路...'
    case 'scenic': return '搜索景点...'
    case 'guide': return '搜索攻略...'
    default: return '搜索景点、酒店、攻略...'
  }
}

const handleLocationChange = (cityCode) => {
  const city = cityList.value.find(c => c.code === cityCode)
  if (city) {
    currentCity.value = city.name
    ElMessage.success(`已切换到${city.name}`)
  }
}

// 城市名到代码的映射
const cityNameToCode = {
  '重庆': 'chongqing', '成都': 'chengdu', '昆明': 'kunming', '贵阳': 'guiyang',
  '三亚': 'sanya', '北京': 'beijing', '上海': 'shanghai', '长沙': 'changsha',
  '宜昌': 'yichang', '杭州': 'hangzhou', '广州': 'guangzhou', '深圳': 'shenzhen',
  '宁波': 'ningbo', '嘉兴': 'jiaxing', '绍兴': 'shaoxing', '金华': 'jinhua',
  '衢州': 'quzhou', '舟山': 'zhoushan', '台州': 'taizhou', '丽水': 'lishui',
  '温州': 'wenzhou', '苏州': 'suzhou', '南京': 'nanjing', '湖州': 'huzhou',
  '海口': 'haikou', '万州': 'wanzhou', '涪陵': 'fuling', '恩施': 'enshi'
}

// 周边游跳转 - 使用精确天数
const goToAroundTour = (days) => {
  const cityCode = cityNameToCode[currentCity.value] || 'chongqing'
  // 周边游直接传精确天数
  router.push(`/tickets?tourType=around&city=${cityCode}&days=${days}`)
}

// 邮轮出行跳转 - 三峡/西沙作为目的地筛选
const goToCruise = (cruiseType) => {
  // cruiseType 直接作为目的地值
  router.push(`/tickets?tourType=cruise&destination=${cruiseType}`)
}

// 搜索类别变更
const handleCategoryChange = (categoryValue) => {
  const cat = searchCategories.find(c => c.value === categoryValue)
  if (cat) {
    currentCategory.value = cat
    // 清空之前的搜索建议
    searchSuggestions.scenics = []
    searchSuggestions.guides = []
    searchSuggestions.lines = []
    showSearchSuggestions.value = false
  }
}

// 搜索输入处理
const handleSearchInput = (value) => {
  if (searchDebounceTimer) {
    clearTimeout(searchDebounceTimer)
  }

  if (!value.trim()) {
    showSearchSuggestions.value = false
    return
  }

  searchDebounceTimer = setTimeout(() => {
    fetchSearchSuggestions(value.trim())
  }, 300)
}

// 获取搜索建议
const fetchSearchSuggestions = async (keyword) => {
  if (!keyword) return

  try {
    // 清空之前的建议
    searchSuggestions.scenics = []
    searchSuggestions.guides = []
    searchSuggestions.lines = []

    const category = currentCategory.value.value

    if (category === 'all') {
      // 综合搜索：同时获取景点、攻略和线路建议
      const [scenicResponse, guideResponse] = await Promise.all([
        request.get('/scenic/suggestions', {
          keyword,
          limit: 3
        }, {
          showDefaultMsg: false
        }).catch(() => []),

        request.get('/guide/suggestions', {
          keyword,
          limit: 3
        }, {
          showDefaultMsg: false
        }).catch(() => [])
      ])

      searchSuggestions.scenics = Array.isArray(scenicResponse) ? scenicResponse : (scenicResponse?.data || [])
      searchSuggestions.guides = Array.isArray(guideResponse) ? guideResponse : (guideResponse?.data || [])

      // 单独获取线路建议（使用行程接口）
      await request.get('/ticket/page', {
        ticketName: keyword,
        currentPage: 1,
        size: 3
      }, {
        showDefaultMsg: false,
        onSuccess: (res) => {
          searchSuggestions.lines = res.records || []
        }
      })
    } else if (category === 'scenic') {
      // 只获取景点建议
      const response = await request.get('/scenic/suggestions', {
        keyword,
        limit: 5
      }, {
        showDefaultMsg: false
      }).catch(() => [])

      searchSuggestions.scenics = Array.isArray(response) ? response : (response?.data || [])
    } else if (category === 'guide') {
      // 只获取攻略建议
      const response = await request.get('/guide/suggestions', {
        keyword,
        limit: 5
      }, {
        showDefaultMsg: false
      }).catch(() => [])

      searchSuggestions.guides = Array.isArray(response) ? response : (response?.data || [])
    } else if (category === 'line') {
      // 只获取线路建议（使用行程接口）
      await request.get('/ticket/page', {
        ticketName: keyword,
        currentPage: 1,
        size: 5
      }, {
        showDefaultMsg: false,
        onSuccess: (res) => {
          searchSuggestions.lines = res.records || []
        }
      })
    }

    showSearchSuggestions.value = (searchSuggestions.scenics.length > 0 || searchSuggestions.guides.length > 0 || searchSuggestions.lines.length > 0)
    selectedSuggestionIndex.value = -1
  } catch (error) {
    console.error('获取搜索建议失败:', error)
    searchSuggestions.scenics = []
    searchSuggestions.guides = []
    searchSuggestions.lines = []
    showSearchSuggestions.value = false
  }
}

// 键盘事件处理
const handleSearchKeydown = (event) => {
  const totalItems = searchSuggestions.scenics.length + searchSuggestions.guides.length + searchSuggestions.lines.length

  switch (event.key) {
    case 'ArrowDown':
      event.preventDefault()
      selectedSuggestionIndex.value = Math.min(selectedSuggestionIndex.value + 1, totalItems - 1)
      break
    case 'ArrowUp':
      event.preventDefault()
      selectedSuggestionIndex.value = Math.max(selectedSuggestionIndex.value - 1, -1)
      break
    case 'Enter':
      event.preventDefault()
      if (selectedSuggestionIndex.value >= 0) {
        selectSuggestion(selectedSuggestionIndex.value)
      } else {
        handleSearchSubmit()
      }
      break
    case 'Escape':
      showSearchSuggestions.value = false
      selectedSuggestionIndex.value = -1
      break
  }
}

// 获得焦点
const handleSearchFocus = () => {
  if (searchKeyword.value.trim() && hasSuggestions.value) {
    showSearchSuggestions.value = true
  }
}

// 失去焦点
const handleSearchBlur = () => {
  setTimeout(() => {
    hideSearchSuggestions()
  }, 200)
}

// 隐藏建议
const hideSearchSuggestions = () => {
  showSearchSuggestions.value = false
  selectedSuggestionIndex.value = -1
}

// 获取攻略建议的索引（线路在最上，然后景点，然后攻略）
const getGuideIndex = (index) => {
  if (currentCategory.value === 'all') {
    return searchSuggestions.lines.length + searchSuggestions.scenics.length + index
  }
  return index
}

// 获取线路建议的索引（线路在最上面）
const getLineIndex = (index) => {
  return index
}

// 选择建议项
const selectSuggestion = (index) => {
  const lineCount = searchSuggestions.lines.length
  const scenicCount = searchSuggestions.scenics.length
  const guideCount = searchSuggestions.guides.length

  if (index < lineCount) {
    // 线路
    const line = searchSuggestions.lines[index]
    goToTicketDetail(line.id)
  } else if (index < lineCount + scenicCount) {
    // 景点
    const scenic = searchSuggestions.scenics[index - lineCount]
    goToScenicDetail(scenic.id)
  } else {
    // 攻略
    const guide = searchSuggestions.guides[index - lineCount - scenicCount]
    goToGuideDetail(guide.id)
  }
}

// 跳转到景点详情
const goToScenicDetail = (id) => {
  router.push(`/scenic/${id}`)
  hideSearchSuggestions()
}

// 跳转到攻略详情
const goToGuideDetail = (id) => {
  router.push(`/guide/detail/${id}`)
  hideSearchSuggestions()
}

// 跳转到行程详情
const goToTicketDetail = (id) => {
  router.push(`/ticket/booking/${id}`)
  hideSearchSuggestions()
}

// 跳转到线路详情（兼容旧名称）
const goToLineDetail = (id) => {
  goToTicketDetail(id)
}

// 执行搜索提交
const handleSearchSubmit = () => {
  if (!searchKeyword.value.trim()) return

  const keyword = encodeURIComponent(searchKeyword.value.trim())

  switch (currentCategory.value.value) {
    case 'line':
      // 线路搜索跳转到行程预订页面
      router.push(`/tickets?search=${keyword}`)
      break
    case 'scenic':
      // 景点搜索跳转到景点列表页
      router.push(`/scenic?search=${keyword}`)
      break
    case 'guide':
      // 攻略搜索跳转到攻略列表页
      router.push(`/guide?search=${keyword}`)
      break
    case 'all':
    default:
      // 综合搜索默认跳转到行程预订页面（线路）
      router.push(`/tickets?search=${keyword}`)
  }

  hideSearchSuggestions()
}

const handleAddFavorite = () => {
  try {
    window.external.addToFavorites(window.location.href, '侠客行国旅')
  } catch (e) {
    // 通用浏览器收藏方法
    var url = window.location.href
    var title = document.title
    if (window.sidebar && window.sidebar.addPanel) {
      // Firefox
      window.sidebar.addPanel(title, url, '')
    } else if (window.external && window.external.AddToFavoritesCb) {
      // IE
      window.external.AddToFavoritesCb(url, title)
    } else {
      // Chrome等现代浏览器（无法自动收藏，需提示用户）
      ElMessage.info('请按 Ctrl+D 键收藏本站')
    }
  }
}

const handleQRCodeError = (e) => {
  // 图片加载失败时隐藏
  e.target.parentElement.style.display = 'none'
}

// 通过IP获取位置（可选功能）
const fetchLocationByIP = async () => {
  // 尝试多个IP定位API
  const apis = [
    'https://ipapi.co/json/',
    'https://ip-api.com/json/',
    'https://whois.pconline.com.cn/ipJson.jsp?json=true'
  ]
  
  for (const api of apis) {
    try {
      const controller = new AbortController()
      const timeoutId = setTimeout(() => controller.abort(), 3000)
      
      let url = api
      let data
      
      if (api.includes('pconline')) {
        // 太平洋网络IP查询（国内API，返回gbk编码）
        url = 'https://whois.pconline.com.cn/ipJson.jsp?json=true'
        const response = await fetch(url, { 
          signal: controller.signal,
          headers: { 'Accept': 'application/json' }
        })
        clearTimeout(timeoutId)
        if (response.ok) {
          const text = await response.text()
          // 处理gbk编码的响应
          const decoder = new TextDecoder('gbk')
          const uint8Array = new Uint8Array([...text].map(c => c.charCodeAt(0)))
          data = JSON.parse(decoder.decode(uint8Array))
          const cityName = data.city || ''
          const matchedCity = cityList.value.find(c =>
            c.name.includes(cityName) || cityName.includes(c.name)
          )
          if (matchedCity) {
            currentCity.value = matchedCity.name
            return
          }
        }
      } else {
        const response = await fetch(url, { signal: controller.signal })
        clearTimeout(timeoutId)
        if (response.ok) {
          data = await response.json()
          const cityName = data.city || data.cityName || ''
          const matchedCity = cityList.value.find(c =>
            c.name.includes(cityName) || cityName.includes(c.name)
          )
          if (matchedCity) {
            currentCity.value = matchedCity.name
            return
          }
        }
      }
    } catch (error) {
      console.log(`IP定位API ${api} 请求失败:`, error.message)
      continue
    }
  }
  
  // 所有API都失败时使用默认城市
  console.log('IP定位失败，使用默认城市杭州')
}

const handleCommand = (command) => {
  switch (command) {
    case 'profile':
      router.push('/profile')
      break
    case 'guide':
      router.push('/my-guide')
      break
    case 'collection':
      router.push('/collection')
      break
    case 'orders':
      router.push('/orders')
      break
    case 'logout':
      handleLogout()
      break
  }
}

const goToLogin = () => {
  router.push('/login')
}

const handleLogout = () => {
  userStore.clearUserInfo()
  router.push('/login')
}

onMounted(() => {
  // 页面加载时尝试通过IP获取位置
  fetchLocationByIP()
})
</script>

<style lang="scss" scoped>
.frontend-layout {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  background-color: #FFFFFF;
}

// ==================== 三层页眉样式 ====================

.header {
  background: #ffffff;
  position: relative;
  z-index: 1000;
  margin-bottom: 0;
}

// 第一层：顶部信息栏
.header-top {
  background: linear-gradient(135deg, #1a1a2e 0%, #16213e 100%);
  color: #fff;
  font-size: 13px;
}

.header-top-container {
  max-width: 1400px;
  margin: 0 auto;
  padding: 0 20px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.welcome-message {
  color: #ffd700;
  font-size: 13px;
  font-weight: 500;
}

.location-selector {
  display: flex;
  align-items: center;
  cursor: pointer;
  padding: 6px 12px;
  border-radius: 16px;
  background-color: #FFFFFF;
  transition: all 0.3s;
  margin-right: 15px;
  white-space: nowrap;

  &:hover {
    background-color: #eee;
  }

  .el-icon {
    color: #e94560;
    margin-right: 4px;
    font-size: 14px;
  }

  .current-location {
    color: #333;
    display: flex;
    align-items: center;
    font-size: 14px;
    font-weight: 500;

    .arrow-icon {
      margin-left: 4px;
      font-size: 12px;
    }
  }

  :deep(.el-dropdown-menu) {
    max-height: 300px;
    overflow-y: auto;
  }

  :deep(.el-dropdown-menu__item) {
    &.is-active {
      color: #e94560;
      font-weight: 600;
    }
  }
}

.top-links {
  display: flex;
  align-items: center;

  .welcome-text {
    color: #ffd700;
    font-weight: 500;
  }

  .user-dropdown {
    color: #ffd700;
    cursor: pointer;
    display: flex;
    align-items: center;
    transition: color 0.3s;

    &:hover {
      color: #e94560;
    }

    .el-icon {
      margin-right: 4px;
    }

    .arrow-icon {
      margin-left: 4px;
      font-size: 12px;
    }
  }

  .el-divider {
    background-color: rgba(255, 255, 255, 0.2);
    margin: 0 10px;
  }

  .top-link {
    color: #c9d1d9;
    text-decoration: none;
    transition: color 0.3s;
    cursor: pointer;
    display: flex;
    align-items: center;

    &:hover {
      color: #e94560;
    }

    .el-icon {
      margin-right: 4px;
    }
  }

  .favorite-link {
    .el-icon {
      color: #ffd700;
    }
  }

  .wechat-link {
    color: #07c160;
    cursor: pointer;
    position: relative;
    display: flex;
    align-items: center;

    .el-icon {
      margin-right: 4px;
    }

    &:hover {
      color: #06ae56;
    }

    .wechat-qrcode {
      position: absolute;
      top: 100%;
      right: 0;
      margin-top: 10px;
      background: #fff;
      padding: 10px;
      border-radius: 8px;
      box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
      z-index: 1000;

      img {
        width: 120px;
        height: 120px;
        display: block;
      }

      p {
        margin: 8px 0 0 0;
        color: #606266;
        font-size: 12px;
        text-align: center;
        white-space: nowrap;
      }

      &::before {
        content: '';
        position: absolute;
        top: -6px;
        right: 20px;
        width: 12px;
        height: 12px;
        background: #fff;
        transform: rotate(45deg);
      }
    }
  }
}

// 第二层：Logo和搜索框
.header-middle {
  background: #fff;
  border-bottom: 1px solid #eee;
}

.header-middle-container {
  max-width: 1400px;
  margin: 0 auto;
  padding: 20px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.logo-section {
  display: flex;
  align-items: center;
  cursor: pointer;
  text-decoration: none;

  .logo-icon {
    width: 60px;
    height: 60px;
    margin-right: 15px;
    border-radius: 8px;
  }

  .logo-text-group {
    .logo-title {
      font-size: 28px;
      font-weight: 700;
      color: #1a1a2e;
      margin: 0;
      letter-spacing: 2px;
    }

    .logo-subtitle {
      font-size: 12px;
      color: #909399;
      margin: 2px 0 0 0;
      letter-spacing: 3px;
    }
  }
}

// 搜索栏包装器 - 位置选择器 + 搜索框组合
.search-bar-wrapper {
  flex: 1;
  max-width: 700px;
  display: flex;
  align-items: center;
  position: relative;
}

// 智能搜索框样式
.smart-search-wrapper {
  flex: 1;
  display: flex;
  align-items: center;
  position: relative;
}

// 搜索类别选择器
.category-dropdown {
  .category-selector {
    display: flex;
    align-items: center;
    padding: 0 12px;
    height: 48px;
    background: #FFFFFF;
    border: 2px solid #e94560;
    border-right: none;
    border-radius: 24px 0 0 24px;
    cursor: pointer;
    font-size: 14px;
    color: #333;
    white-space: nowrap;
    transition: background-color 0.3s;

    &:hover {
      background-color: #eee;
    }

    .arrow-icon {
      margin-left: 6px;
      font-size: 12px;
      color: #999;
    }
  }

  :deep(.el-dropdown-menu__item) {
    &.is-active {
      color: #e94560;
      font-weight: 600;
      position: relative;

      &::after {
        content: '';
        position: absolute;
        bottom: 4px;
        left: 50%;
        transform: translateX(-50%);
        width: 20px;
        height: 2px;
        background-color: #e94560;
        border-radius: 1px;
      }
    }

    .el-icon {
      margin-right: 6px;
    }
  }
}

.smart-search-wrapper {
  .search-input {
    flex: 1;

    :deep(.el-input__wrapper) {
      padding-left: 15px;
      border-radius: 0;
      border: 2px solid #e94560;
      border-left: none;
      box-shadow: none;
      height: 48px;

      &:hover, &:focus {
        box-shadow: none;
        border-color: #e94560;
      }
    }

    :deep(.el-input__inner) {
      height: 44px;
      line-height: 44px;
    }
  }

  .search-btn {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 6px;
    height: 48px;
    padding: 0 24px;
    background: linear-gradient(135deg, #e94560 0%, #c73e54 100%);
    border: none;
    border-radius: 0 24px 24px 0;
    color: #fff;
    font-size: 16px;
    font-weight: 500;
    cursor: pointer;
    transition: all 0.3s;

    &:hover {
      background: linear-gradient(135deg, #d63c55 0%, #b03548 100%);
    }

    .el-icon {
      font-size: 18px;
    }
  }
}

// 搜索建议下拉框
.suggestions-dropdown {
  position: absolute;
  top: 100%;
  left: 0;
  right: 0;
  background: white;
  border-radius: 12px;
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.15);
  z-index: 9999;
  margin-top: 8px;
  max-height: 400px;
  overflow-y: auto;
  border: 1px solid #e2e8f0;
}

.suggestion-section {
  &:not(:last-child) {
    border-bottom: 1px solid #f1f5f9;
  }
}

.section-title {
  display: flex;
  align-items: center;
  padding: 12px 16px 8px;
  font-size: 14px;
  font-weight: 600;
  color: #64748b;
  background: #FFFFFF;

  .el-icon {
    margin-right: 6px;
    color: #e94560;
  }
}

.suggestion-item {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  cursor: pointer;
  transition: all 0.2s ease;

  &:hover,
  &.active {
    background: #FFFFFF;
  }

  .item-image {
    width: 48px;
    height: 48px;
    border-radius: 8px;
    overflow: hidden;
    margin-right: 12px;
    flex-shrink: 0;

    img {
      width: 100%;
      height: 100%;
      object-fit: cover;
    }
  }

  .item-content {
    flex: 1;
    min-width: 0;

    .item-title {
      font-size: 15px;
      font-weight: 600;
      color: #2d3748;
      margin-bottom: 4px;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    }

    .item-subtitle {
      font-size: 13px;
      color: #64748b;
      display: flex;
      align-items: center;

      .el-icon {
        margin-right: 4px;
        font-size: 12px;
      }

      .price {
        margin-left: auto;
        font-weight: 600;
        color: #e94560;

        &.free {
          color: #10b981;
        }
      }

      .views {
        margin-left: auto;
        color: #94a3b8;
      }
    }
  }
}

// 滚动条样式
.suggestions-dropdown::-webkit-scrollbar {
  width: 6px;
}

.suggestions-dropdown::-webkit-scrollbar-track {
  background: #f1f5f9;
  border-radius: 3px;
}

.suggestions-dropdown::-webkit-scrollbar-thumb {
  background: #cbd5e0;
  border-radius: 3px;

  &:hover {
    background: #a0aec0;
  }
}

// 第三层：主导航菜单
.header-bottom {
  background: #fff;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}

.header-bottom-container {
  max-width: 1400px;
  margin: 0 auto;
  padding: 0 20px;
}

.main-menu {
  border-bottom: none;
  background-color: transparent;

  :deep(.el-menu-item),
  :deep(.el-sub-menu__title) {
    color: #333;
    font-size: 16px;
    height: 50px;
    line-height: 50px;
    padding: 0 20px;
    margin-right: 0;

    &:hover, &.is-active {
      color: #e94560;
      background-color: transparent;
      border-bottom: 3px solid #e94560;
    }

    .el-icon {
      color: #666;
      margin-right: 6px;
    }

    &.is-active .el-icon,
    &:hover .el-icon {
      color: #e94560;
    }
  }

  :deep(.el-menu-item) {
    margin-right: 20px;
  }

  :deep(.el-sub-menu) {
    margin-right: 20px;
  }

  // 隐藏sub-menu的箭头
  :deep(.el-sub-menu__icon-arrow) {
    display: none;
  }
}

// 主内容区域
.main-content {
  flex: 1;
  padding: 0;
  margin: 0;
  width: 100%;
  max-width: 100%;
}

// 页脚样式
.footer {
  background: #1a1a2e;
  color: #c9d1d9;
  padding: 30px 0;
}

.footer-content {
  max-width: 1400px;
  margin: 0 auto;
  text-align: center;
}

.copyright {
  margin: 0;
  font-size: 14px;
  color: #8b949e;
}

// 响应式样式
@media (max-width: 1024px) {
  .search-bar-wrapper {
    margin-left: 30px;
  }
}

@media (max-width: 768px) {
  .header-top-container {
    height: auto;
    padding: 10px;
  }

  .welcome-message {
    font-size: 12px;
    line-height: 1.4;
  }

  .top-links {
    order: 2;
    width: 100%;
    justify-content: center;
    flex-wrap: wrap;
    gap: 5px;

    .el-divider {
      display: none;
    }
  }

  .header-middle-container {
    flex-direction: column;
    padding: 15px;
  }

  .logo-section {
    margin-bottom: 15px;

    .logo-icon {
      width: 50px;
      height: 50px;
    }

    .logo-title {
      font-size: 22px;
    }
  }

  .search-bar-wrapper {
    width: 100%;
    max-width: none;
    margin-left: 0;
  }

  .location-selector {
    width: 100%;
    justify-content: center;
    margin-right: 0;
  }

  .smart-search-wrapper {
    width: 100%;
    flex-direction: column;

    .category-dropdown {
      width: 100%;
      margin-bottom: 8px;

      .category-selector {
        width: 100%;
        border-radius: 24px;
        justify-content: center;
      }
    }

    .search-input {
      width: 100%;

      :deep(.el-input__wrapper) {
        border-radius: 24px;
        border-left: 2px solid #e94560;
      }
    }

    .search-btn {
      position: absolute;
      right: 0;
      top: 0;
      height: 48px;
      border-radius: 0 24px 24px 0;
    }
  }

  .header-bottom-container {
    overflow-x: auto;
  }

  .main-menu {
    :deep(.el-menu-item),
    :deep(.el-sub-menu__title) {
      padding: 0 12px;
      font-size: 14px;
      margin-right: 12px;

      span {
        display: none;
      }
    }

    :deep(.el-sub-menu) {
      margin-right: 12px;
    }
  }
}
</style>
