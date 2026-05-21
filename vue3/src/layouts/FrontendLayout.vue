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
              <span class="frontend-notice-bell">
                <NotificationBell trigger="hover" click-to-center />
              </span>
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
                <img src="@/assets/wechat.jpg" alt="微信公众号" @error="handleQRCodeError" />
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
              <span>首页</span>
            </el-menu-item>

            <el-menu-item index="/scenic">
              <span>景点</span>
            </el-menu-item>

            <el-menu-item index="/guide">
              <span>攻略</span>
            </el-menu-item>

            <el-menu-item index="/accommodation">
              <span>住宿</span>
            </el-menu-item>

            <el-sub-menu index="/around">
              <template #title>
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
                <span>邮轮出行</span>
              </template>
              <el-menu-item-group>
                <template #title>选择邮轮类型</template>
                <el-menu-item @click="goToCruise('sanxia')">三峡邮轮</el-menu-item>
                <el-menu-item @click="goToCruise('xisha')">西沙邮轮</el-menu-item>
              </el-menu-item-group>
            </el-sub-menu>

            <el-menu-item index="/tickets">
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

    <!-- 企业化官网页脚 -->
    <footer v-if="footerConfig.enabled !== false" class="footer">
      <div class="footer-content">
        <section v-if="visibleFeatureItems.length" class="footer-feature-grid" aria-label="企业服务能力">
          <article v-for="item in visibleFeatureItems" :key="item.title" class="footer-feature-item">
            <div class="footer-feature-icon" :class="getFooterFeatureIconClass(item.icon)">
              <svg viewBox="0 0 48 48" aria-hidden="true">
                <path
                  v-for="path in getFooterFeatureIconPaths(item.icon)"
                  :key="path"
                  :d="path"
                />
              </svg>
            </div>
            <h3>{{ item.title }}</h3>
            <p>{{ item.description }}</p>
          </article>
        </section>

        <div class="footer-lower">
          <section class="footer-text-info" aria-label="企业信息">
            <nav v-if="visibleLinks(footerConfig.topLinks).length" class="footer-nav" aria-label="页脚导航">
              <a
                v-for="link in visibleLinks(footerConfig.topLinks)"
                :key="`${link.label}-${link.url}`"
                :href="link.url || '#'"
                class="footer-nav-link"
              >
                {{ link.label }}
              </a>
            </nav>

            <div class="footer-line">
              <a
                v-for="link in visibleLinks(footerConfig.complianceLinks)"
                :key="`${link.label}-${link.url}`"
                :href="link.url || '#'"
              >
                {{ link.label }}
              </a>
              <span v-if="footerConfig.licenseNumber">许可编号：{{ footerConfig.licenseNumber }}</span>
            </div>

            <div class="footer-line">
              <span v-if="footerConfig.reportEmail">服务邮箱：{{ footerConfig.reportEmail }}</span>
              <span v-if="footerConfig.minorReportEmail">未成年人信息保护邮箱：{{ footerConfig.minorReportEmail }}</span>
            </div>

            <div class="footer-line">
              <span v-if="footerConfig.consultationPhone">旅行咨询电话：{{ footerConfig.consultationPhone }}</span>
              <span v-if="footerConfig.cruisePhone">团队与邮轮咨询：{{ footerConfig.cruisePhone }}</span>
              <span v-if="footerConfig.complaintPhone">投诉与服务监督：{{ footerConfig.complaintPhone }}</span>
            </div>

            <div class="footer-line">
              <span v-if="footerConfig.address">公司总部地址：{{ footerConfig.address }}</span>
              <span v-if="footerConfig.serviceTime">服务时间：{{ footerConfig.serviceTime }}</span>
            </div>

            <div v-if="visibleLinks(footerConfig.friendlyLinks).length" class="footer-line footer-friends">
              <span>合作链接</span>
              <a
                v-for="link in visibleLinks(footerConfig.friendlyLinks)"
                :key="`${link.label}-${link.url}`"
                :href="link.url || '#'"
                target="_blank"
                rel="noopener"
              >
                {{ link.label }}
              </a>
            </div>

            <div class="footer-line footer-compliance">
              <span>{{ footerConfig.copyright }}</span>
              <span v-if="footerConfig.technicalSupport">法律与技术支持：{{ footerConfig.technicalSupport }}</span>
            </div>

            <div v-if="footerConfig.legalNotes?.length" class="footer-line footer-legal">
              <span v-for="note in footerConfig.legalNotes" :key="note">{{ note }}</span>
            </div>
          </section>

          <section v-if="visibleQrCodes.length" class="footer-qrs" aria-label="官方二维码">
            <div v-for="item in visibleQrCodes" :key="item.label" class="footer-qr-card">
              <div class="footer-qr-image">
                <img v-if="item.imageUrl" :src="getFooterAssetUrl(item.imageUrl)" :alt="item.label" @error="hideBrokenImage" />
                <span v-else>{{ item.label }}</span>
              </div>
              <strong>{{ item.label }}</strong>
              <p>{{ item.description }}</p>
            </div>
          </section>
        </div>

        <div class="footer-records">
          <a v-if="footerConfig.icpNumber" :href="footerConfig.icpUrl || '#'" target="_blank" rel="noopener">
            {{ footerConfig.icpNumber }}
          </a>
          <a v-if="footerConfig.policeNumber" :href="footerConfig.policeUrl || '#'" target="_blank" rel="noopener">
            {{ footerConfig.policeNumber }}
          </a>
        </div>
      </div>
    </footer>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useUserStore } from '@/store/user'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import NotificationBell from '@/components/NotificationBell.vue'
import request from '@/utils/request'
import { getPublicFooterConfig } from '@/api/siteFooter'
import wechatQrImage from '@/assets/wechat.jpg'
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
const DEFAULT_CITY = '重庆'
const LOCATION_STORAGE_KEY = 'frontend_selected_city'
const currentCity = ref(DEFAULT_CITY)
const showWechatQR = ref(false)

const createDefaultFooterConfig = () => ({
  enabled: true,
  companyName: '侠客行国际旅行社有限公司',
  brandName: '侠客行国旅',
  slogan: '扎根重庆，连接山城、三峡与西南山河的品质旅行服务',
  consultationPhone: '400-800-5178',
  cruisePhone: '023-6789-5178',
  serviceTime: '09:00 - 20:00，节假日专人在线',
  address: '重庆市渝中区解放碑商圈时代旅行中心 18F',
  copyright: '© 2021-2026 侠客行国旅 版权所有',
  icpNumber: '',
  icpUrl: 'https://beian.miit.gov.cn/',
  policeNumber: '',
  policeUrl: 'https://beian.mps.gov.cn/',
  licenseNumber: 'L-CQ-XXK-2026',
  complaintPhone: '12345 / 023-6789-5178',
  technicalSupport: '侠客行数字旅行中心',
  reportEmail: 'service@xkxtrip.com',
  minorReportEmail: 'safe@xkxtrip.com',
  featureItems: [
    { title: '守信', description: '行程说明、费用明细与服务标准清楚呈现，重要节点可追踪。', icon: 'shield' },
    { title: '甄选', description: '围绕重庆、三峡与西南目的地，打磨小而美的品质线路。', icon: 'route' },
    { title: '陪伴', description: '顾问、导游与客服协同响应，从预订到返程持续跟进。', icon: 'service' },
    { title: '自在', description: '控制车程、住宿与游览节奏，让旅途保持轻松和松弛感。', icon: 'experience' },
    { title: '友好', description: '尊重当地风俗与自然环境，倡导低打扰、负责任的旅行。', icon: 'leaf' },
    { title: '共创', description: '收集真实评价与旅途故事，把反馈转化为下一次优化。', icon: 'share' }
  ],
  topLinks: [
    { label: '关于侠客行', url: '/about' },
    { label: '山城线路', url: '/tickets' },
    { label: '目的地灵感', url: '/scenic' },
    { label: '旅行攻略', url: '/guide' },
    { label: '服务承诺', url: '/about' },
    { label: '联系我们', url: '/about' }
  ],
  complianceLinks: [
    { label: '营业执照', url: '/about' },
    { label: '旅行社业务经营许可', url: '/about' },
    { label: '服务规范', url: '/about' },
    { label: '社区公约', url: '/about' },
    { label: '安全与隐私保护', url: '/about' },
    { label: '在线服务与投诉反馈专区', url: '/about' }
  ],
  friendlyLinks: [],
  qrCodes: [
    { label: '侠客行服务号', imageUrl: wechatQrImage, description: '线路上新与出行提醒' },
    { label: '旅行顾问', imageUrl: wechatQrImage, description: '定制咨询与售后协助' }
  ],
  certificates: [],
  legalNotes: ['平台展示的图片、攻略与目的地资料仅用于旅行服务说明，出行前请以订单确认信息和当地实时政策为准。']
})

const footerConfig = ref(createDefaultFooterConfig())

const cityList = ref([
  { code: 'chongqing', name: '重庆', aliases: ['重庆市', 'Chongqing'] },
  { code: 'yichang', name: '宜昌', aliases: ['宜昌市', 'Yichang'] },
  { code: 'hainan', name: '海南', aliases: ['海南省', '海口', '海口市', '三亚', '三亚市', 'Hainan', 'Haikou', 'Sanya'] },
  { code: 'chengdu', name: '成都', aliases: ['成都市', 'Chengdu'] },
  { code: 'guiyang', name: '贵阳', aliases: ['贵阳市', 'Guiyang'] },
  { code: 'kunming', name: '昆明', aliases: ['昆明市', 'Kunming'] },
  { code: 'changsha', name: '长沙', aliases: ['长沙市', 'Changsha'] },
  { code: 'beijing', name: '北京', aliases: ['北京市', 'Beijing'] },
  { code: 'shanghai', name: '上海', aliases: ['上海市', 'Shanghai'] },
  { code: 'guangzhou', name: '广州', aliases: ['广州市', 'Guangzhou'] },
  { code: 'shenzhen', name: '深圳', aliases: ['深圳市', 'Shenzhen'] },
  { code: 'hangzhou', name: '杭州', aliases: ['杭州市', 'Hangzhou'] },
  { code: 'nanjing', name: '南京', aliases: ['南京市', 'Nanjing'] },
  { code: 'xian', name: '西安', aliases: ['西安市', 'Xi’an', "Xi'an", 'Xian'] }
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
const userName = computed(() => userStore.userInfo?.nickname || userStore.userInfo?.name || userStore.userInfo?.username || '')

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

const visibleLinks = (links = []) => {
  return Array.isArray(links) ? links.filter(item => item?.label) : []
}

const visibleQrCodes = computed(() => {
  return Array.isArray(footerConfig.value.qrCodes)
    ? footerConfig.value.qrCodes.filter(item => item?.label)
    : []
})

const withFallbackQrImages = (qrCodes = [], fallbackQrCodes = []) => {
  return qrCodes.map((item, index) => ({
    ...item,
    imageUrl: item.imageUrl || fallbackQrCodes[index]?.imageUrl || ''
  }))
}

const visibleFeatureItems = computed(() => {
  return Array.isArray(footerConfig.value.featureItems)
    ? footerConfig.value.featureItems.filter(item => item?.title)
    : []
})

const getFooterAssetUrl = (url) => {
  if (!url) return ''
  if (/^(https?:|data:|blob:)/.test(url)) return url
  if (url.startsWith('/img/') || url.startsWith('/assets/')) return url
  return url.startsWith('/') ? baseAPI + url : url
}

const getFooterFeatureIconClass = (icon) => {
  const allowed = ['shield', 'route', 'service', 'experience', 'leaf', 'share']
  return `is-${allowed.includes(icon) ? icon : 'service'}`
}

const footerFeatureIconPaths = {
  shield: [
    'M24 6l14 5v10c0 9.3-5.7 16.7-14 21-8.3-4.3-14-11.7-14-21V11l14-5z',
    'M18 24l4 4 8-9'
  ],
  route: [
    'M10 36c8-14 20 2 28-12',
    'M13 13c-3 0-5 2.3-5 5 0 4.2 5 9 5 9s5-4.8 5-9c0-2.7-2-5-5-5z',
    'M35 8c-3 0-5 2.3-5 5 0 4.2 5 9 5 9s5-4.8 5-9c0-2.7-2-5-5-5z',
    'M13 18h.1M35 13h.1'
  ],
  service: [
    'M12 27a12 12 0 0124 0',
    'M10 26h5v9h-5a4 4 0 01-4-4v-1a4 4 0 014-4z',
    'M38 26h-5v9h5a4 4 0 004-4v-1a4 4 0 00-4-4z',
    'M32 37c-2 3-5 4-8 4h-4'
  ],
  experience: [
    'M8 27c6-12 26-12 32 0',
    'M13 27l5 10h12l5-10',
    'M18 18c2-5 10-5 12 0',
    'M24 13v-5',
    'M15 39h18'
  ],
  leaf: [
    'M39 9c-15 1-27 8-28 23 8 5 22 2 27-10 2-5 1-9 1-13z',
    'M12 34c6-8 13-13 24-21',
    'M17 31c2 6 7 9 15 8'
  ],
  share: [
    'M13 33h22a4 4 0 004-4V14H9v15a4 4 0 004 4z',
    'M15 14V9h18v5',
    'M17 24h14',
    'M24 19v10',
    'M20 23l4-4 4 4'
  ]
}

const getFooterFeatureIconPaths = (icon) => {
  return footerFeatureIconPaths[icon] || footerFeatureIconPaths.service
}

const hideBrokenImage = (event) => {
  if (event?.target) {
    event.target.style.display = 'none'
  }
}

const loadFooterConfig = async () => {
  try {
    const data = await getPublicFooterConfig()
    const fallback = createDefaultFooterConfig()
    footerConfig.value = {
      ...fallback,
      ...(data || {}),
      featureItems: data?.featureItems?.length ? data.featureItems : fallback.featureItems,
      topLinks: data?.topLinks?.length ? data.topLinks : fallback.topLinks,
      complianceLinks: data?.complianceLinks?.length ? data.complianceLinks : fallback.complianceLinks,
      qrCodes: data?.qrCodes?.length ? withFallbackQrImages(data.qrCodes, fallback.qrCodes) : fallback.qrCodes,
      legalNotes: data?.legalNotes?.length ? data.legalNotes : fallback.legalNotes
    }
  } catch (error) {
    footerConfig.value = createDefaultFooterConfig()
  }
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
    localStorage.setItem(LOCATION_STORAGE_KEY, city.code)
    ElMessage.success(`已切换到${city.name}`)
  }
}

// 城市名到代码的映射
const cityNameToCode = {
  '重庆': 'chongqing', '宜昌': 'yichang', '海南': 'hainan', '成都': 'chengdu',
  '贵阳': 'guiyang', '昆明': 'kunming', '长沙': 'changsha', '北京': 'beijing',
  '上海': 'shanghai', '广州': 'guangzhou', '深圳': 'shenzhen', '杭州': 'hangzhou',
  '南京': 'nanjing', '西安': 'xian'
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
      await request.get('/tour/page', {
        title: keyword,
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
      await request.get('/tour/page', {
        title: keyword,
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

const normalizeLocationName = (value = '') => {
  return String(value)
    .trim()
    .replace(/(省|市|地区|自治州|特别行政区)$/g, '')
    .toLowerCase()
}

const matchCityFromLocation = (...values) => {
  const tokens = values
    .filter(Boolean)
    .flatMap(value => String(value).split(/[,\s/|-]+/))
    .map(normalizeLocationName)
    .filter(Boolean)

  return cityList.value.find(city => {
    const names = [city.name, ...(city.aliases || [])].map(normalizeLocationName)
    return tokens.some(token => names.some(name => token.includes(name) || name.includes(token)))
  })
}

const applyDefaultCity = () => {
  currentCity.value = DEFAULT_CITY
}

// 通过公网 IP 自动定位。失败或未命中展示城市时，默认回到重庆。
const fetchLocationByIP = async () => {
  const savedCityCode = localStorage.getItem(LOCATION_STORAGE_KEY)
  const savedCity = cityList.value.find(city => city.code === savedCityCode)
  if (savedCity) {
    currentCity.value = savedCity.name
    return
  }

  const apis = [
    {
      url: 'https://ipapi.co/json/',
      parse: data => [data.city, data.region, data.country_name]
    },
    {
      url: 'https://ipwho.is/',
      parse: data => [data.city, data.region, data.country]
    },
    {
      url: 'https://api.vore.top/api/IPdata',
      parse: data => [
        data.ipdata?.info1,
        data.ipdata?.info2,
        data.ipdata?.info3,
        data.adcode?.o
      ]
    }
  ]

  for (const api of apis) {
    const controller = new AbortController()
    const timeoutId = setTimeout(() => controller.abort(), 3000)
    try {
      const response = await fetch(api.url, {
        signal: controller.signal,
        cache: 'no-store',
        headers: { Accept: 'application/json' }
      })
      clearTimeout(timeoutId)
      if (!response.ok) {
        continue
      }
      const data = await response.json()
      const matchedCity = matchCityFromLocation(...api.parse(data))
      if (matchedCity) {
        currentCity.value = matchedCity.name
        return
      }
    } catch (error) {
      clearTimeout(timeoutId)
    }
  }

  applyDefaultCity()
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
  loadFooterConfig()
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

  .frontend-notice-bell {
    display: inline-flex;
    align-items: center;
    margin-right: 2px;
    transform: translateY(2px);

    :deep(.notice-badge) {
      margin-right: 2px;
    }

    :deep(.notice-trigger) {
      width: 20px;
      height: 20px;
      color: #e94560;
      transition: color 0.2s ease, transform 0.2s ease;
    }

    :deep(.notice-trigger:hover),
    :deep(.notice-trigger:focus-visible) {
      color: #ff5a72;
      transform: translateY(-1px);
    }

    :deep(.notice-trigger:active) {
      color: #c73e54;
      transform: translateY(0);
    }

    :deep(.notice-trigger .el-icon) {
      font-size: 15px;
    }

    :deep(.notice-trigger svg path) {
      stroke: currentColor;
      stroke-width: 2.6;
    }
  }

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

// 企业化页脚样式
.footer {
  background:
    linear-gradient(180deg, rgba(7, 19, 42, 0.96), rgba(5, 15, 34, 0.98)),
    radial-gradient(circle at 20% 0%, rgba(50, 119, 255, 0.18), transparent 34%);
  color: #d7e3f4;
  padding: 46px 0 28px;
}

.footer-content {
  max-width: 1400px;
  margin: 0 auto;
  padding: 0 28px;
}

.footer-nav {
  display: flex;
  flex-wrap: wrap;
  gap: 14px 0;
  padding-bottom: 24px;
  margin-bottom: 28px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.footer-nav-link {
  color: #f8fbff;
  font-size: 15px;
  font-weight: 700;
  text-decoration: none;
  padding: 0 18px;
  border-right: 1px solid rgba(255, 255, 255, 0.35);

  &:first-child {
    padding-left: 0;
  }

  &:last-child {
    border-right: 0;
  }

  &:hover {
    color: #8fd5ff;
  }
}

.footer-main {
  display: grid;
  grid-template-columns: minmax(360px, 1fr) auto minmax(260px, 360px);
  gap: 34px;
  align-items: stretch;
}

.footer-company {
  min-width: 0;
}

.footer-brand-row {
  display: flex;
  gap: 16px;
  align-items: center;
  margin-bottom: 22px;

  h2 {
    margin: 0 0 8px;
    color: #ffffff;
    font-size: 24px;
    line-height: 1.25;
    font-weight: 800;
  }

  p {
    margin: 0;
    color: #9fb1ce;
    font-size: 14px;
  }
}

.footer-brand-mark {
  width: 52px;
  height: 52px;
  border-radius: 14px;
  display: grid;
  place-items: center;
  flex-shrink: 0;
  background: linear-gradient(135deg, #2a8cff, #30d5c8);
  color: #fff;
  font-size: 24px;
  font-weight: 900;
  box-shadow: 0 14px 34px rgba(42, 140, 255, 0.28);
}

.footer-contact-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(180px, 1fr));
  gap: 12px;
}

.footer-info-line {
  min-height: 70px;
  padding: 14px 16px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.045);

  span {
    display: block;
    color: #8ea2c2;
    font-size: 12px;
    margin-bottom: 8px;
  }

  strong {
    display: block;
    color: #edf5ff;
    font-size: 15px;
    line-height: 1.5;
    word-break: break-word;
  }
}

.footer-info-wide {
  grid-column: 1 / -1;
}

.footer-qrs {
  display: flex;
  gap: 16px;
  justify-content: center;
}

.footer-qr-card {
  width: 132px;
  text-align: center;

  strong {
    display: block;
    margin-top: 10px;
    color: #ffffff;
    font-size: 14px;
  }

  p {
    margin: 5px 0 0;
    color: #91a6c5;
    font-size: 12px;
    line-height: 1.45;
  }
}

.footer-qr-image {
  width: 116px;
  height: 116px;
  margin: 0 auto;
  border-radius: 14px;
  padding: 8px;
  background: #ffffff;
  display: grid;
  place-items: center;
  color: #1d3557;
  font-size: 13px;
  font-weight: 700;
  box-shadow: 0 16px 34px rgba(0, 0, 0, 0.22);

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
    border-radius: 8px;
  }
}

.footer-section-title {
  color: #ffffff;
  font-size: 16px;
  font-weight: 800;
  margin-bottom: 14px;
}

.footer-cert-list {
  display: grid;
  gap: 12px;
}

.footer-cert-item {
  display: flex;
  gap: 12px;
  padding: 12px;
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.045);
  border: 1px solid rgba(255, 255, 255, 0.1);

  img {
    width: 54px;
    height: 54px;
    border-radius: 8px;
    object-fit: cover;
    background: #fff;
    flex-shrink: 0;
  }

  strong {
    color: #f8fbff;
    font-size: 14px;
  }

  p {
    margin: 5px 0 0;
    color: #91a6c5;
    font-size: 12px;
    line-height: 1.5;
  }
}

.footer-compliance,
.footer-friends,
.footer-legal,
.footer-bottom {
  margin-top: 22px;
  padding-top: 18px;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
}

.footer-compliance,
.footer-friends {
  display: flex;
  flex-wrap: wrap;
  gap: 10px 18px;
  color: #b8c8df;
  font-size: 13px;

  a {
    color: #d8e8ff;
    text-decoration: none;

    &:hover {
      color: #8fd5ff;
    }
  }
}

.footer-friends span {
  color: #ffffff;
  font-weight: 800;
}

.footer-legal {
  p {
    margin: 0 0 8px;
    color: #8fa3c1;
    font-size: 12px;
    line-height: 1.7;

    &:last-child {
      margin-bottom: 0;
    }
  }
}

.footer-bottom {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  color: #879ab8;
  font-size: 13px;
}

// 现代旅行品牌信息矩阵页脚
.footer {
  background: #363636;
  color: #c8d0d7;
  padding: 30px 0 22px;
}

.footer-content {
  width: 100%;
  max-width: 1570px;
  margin: 0 auto;
  padding: 0 34px;
}

.footer-feature-grid {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 34px;
  padding: 0 26px 24px;
  margin-bottom: 22px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.14);
}

.footer-feature-item {
  text-align: center;
  min-width: 0;

  h3 {
    margin: 8px 0 8px;
    color: #ffbd00;
    font-size: 15px;
    line-height: 1.2;
    font-weight: 900;
  }

  p {
    margin: 0;
    color: #a9b1ba;
    font-size: 12px;
    line-height: 1.65;
    text-align: left;
  }
}

.footer-feature-icon {
  position: relative;
  width: 62px;
  height: 62px;
  margin: 0 auto;
  border: 1px solid rgba(255, 189, 0, 0.5);
  border-radius: 50%;
  display: grid;
  place-items: center;
  color: #ffbd00;
  background: rgba(255, 189, 0, 0.035);
  box-shadow: none;

  svg {
    position: relative;
    z-index: 1;
    width: 34px;
    height: 34px;
    fill: none;
    stroke: currentColor;
    stroke-width: 2.2;
    stroke-linecap: round;
    stroke-linejoin: round;
  }

  &::after {
    content: "";
    position: absolute;
    inset: 6px;
    border-radius: 50%;
    border: 1px solid rgba(255, 189, 0, 0.18);
  }
}

.footer-lower {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 34px;
  align-items: start;
  padding: 0 26px;
}

.footer-records {
  display: flex;
  align-items: center;
  justify-content: flex-start;
  flex-wrap: wrap;
  gap: 6px 18px;
  margin: 15px 26px 0;
  padding-top: 10px;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
  color: #b8c0c8;
  font-size: 12px;
  line-height: 1.5;

  a,
  span {
    color: inherit;
  }

  a:hover {
    color: #ffbd00;
  }
}

.footer-text-info {
  grid-column: 1 / span 4;
  min-width: 0;
}

.footer-nav {
  display: flex;
  flex-wrap: wrap;
  gap: 6px 15px;
  padding-bottom: 0;
  margin-bottom: 10px;
  border-bottom: 0;
}

.footer-nav-link {
  color: #e6ecf2;
  font-size: 12px;
  font-weight: 500;
  transition: color 0.2s ease;

  &:hover {
    color: #ffbd00;
  }
}

.footer-line {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 5px 14px;
  margin-top: 7px;
  color: #aeb6bf;
  font-size: 12px;
  line-height: 1.5;

  span,
  a {
    color: inherit;
  }

  a:hover {
    color: #ffbd00;
  }
}

.footer-main {
  grid-template-columns: minmax(0, 1fr) 286px;
  gap: 34px;
  align-items: start;
}

.footer-brand-row {
  margin-bottom: 16px;

  h2 {
    color: #f8fbfd;
    font-size: 22px;
    letter-spacing: 0;
  }

  p {
    color: #aeb8c2;
  }
}

.footer-brand-mark {
  width: 46px;
  height: 46px;
  border-radius: 50%;
  background: linear-gradient(135deg, #ffd061 0%, #52c2b1 100%);
  color: #202427;
  box-shadow: 0 14px 34px rgba(82, 194, 177, 0.16);
}

.footer-contact-grid {
  display: flex;
  flex-direction: column;
  gap: 9px;
}

.footer-info-line {
  min-height: auto;
  padding: 0;
  border: 0;
  border-radius: 0;
  background: transparent;
  display: flex;
  gap: 10px;
  align-items: baseline;

  span {
    display: inline;
    color: #eef2f5;
    font-size: 14px;
    margin-bottom: 0;
    flex: 0 0 auto;
  }

  strong {
    display: inline;
    color: #aeb8c2;
    font-size: 14px;
    line-height: 1.65;
    font-weight: 500;
  }
}

.footer-info-wide {
  grid-column: auto;
}

.footer-qrs {
  grid-column: 5 / span 2;
  display: flex;
  gap: 34px;
  justify-content: flex-end;
  align-items: flex-start;
  padding-top: 2px;
}

.footer-qr-card {
  width: 96px;
  text-align: center;

  strong {
    color: #ffbd00;
    display: block;
    margin-top: 7px;
    font-size: 12px;
    font-weight: 900;
  }

  p {
    margin: 3px 0 0;
    color: #9faab4;
    font-size: 11px;
    line-height: 1.35;
  }
}

.footer-qr-image {
  width: 78px;
  height: 78px;
  margin: 0 auto;
  border-radius: 0;
  padding: 4px;
  background: #fff;
  box-shadow: none;

  img {
    border-radius: 0;
  }

  span {
    color: #2b3034;
    font-size: 11px;
    font-weight: 800;
    text-align: center;
  }
}

.footer-certs {
  grid-column: 1 / -1;
  margin-top: 10px;
}

.footer-section-title {
  color: #f8fbfd;
  font-size: 14px;
  margin-bottom: 10px;
}

.footer-cert-list {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.footer-cert-item {
  width: min(100%, 320px);
  padding: 0;
  border-radius: 0;
  background: transparent;
  border: 0;

  img {
    width: 42px;
    height: 42px;
    border-radius: 2px;
  }
}

.footer-compliance,
.footer-friends,
.footer-legal,
.footer-bottom {
  margin-top: 7px;
  padding-top: 0;
  border-top: 0;
}

.footer-compliance,
.footer-friends {
  gap: 5px 14px;
  color: #aeb8c2;
  font-size: 12px;

  a:hover {
    color: #ffbd00;
  }
}

.footer-legal {
  font-size: 12px;
  line-height: 1.5;
  color: #9faab4;
}

.footer-bottom {
  justify-content: flex-start;
  flex-wrap: wrap;
  gap: 8px 14px;
  color: #c8d0d7;
  font-size: 12px;
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

  .footer {
    padding: 34px 0 24px;
  }

  .footer-content {
    padding: 0 16px;
  }

  .footer-nav {
    justify-content: flex-start;
  }

  .footer-feature-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 24px;
  }

  .footer-feature-icon {
    width: 72px;
    height: 72px;
  }

  .footer-main {
    grid-template-columns: 1fr;
    gap: 26px;
  }

  .footer-contact-grid {
    grid-template-columns: 1fr;
  }

  .footer-qrs {
    justify-content: flex-start;
    flex-wrap: wrap;
    grid-column: auto;
    grid-row: auto;
  }

  .footer-bottom {
    flex-direction: column;
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
