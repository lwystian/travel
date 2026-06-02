import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/store/user'
import BackendLayout from '@/layouts/BackendLayout.vue'
import logger from '@/utils/logger'
import { updateSeo, routeDescription, isRouteIndexable } from '@/utils/seo'

export const backendRoutes = [
  {
    path: '/back',
    component: BackendLayout,
    redirect: '/back/dashboard',
    meta: { indexable: false },
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/backend/Dashboard.vue'),
        meta: { title: '首页', icon: 'HomeFilled' }
      },
      {
        path: 'user',
        name: 'UserManagement',
        component: () => import('@/views/backend/user/index.vue'),
        meta: { title: '用户管理', icon: 'User' }
      },
      {
        path: 'profile',
        name: 'BackendProfile',
        component: () => import('@/views/backend/user/PersonInfo.vue'),
        meta: { title: '个人信息', icon: 'UserFilled' }
      },
      {
        path: 'scenic',
        name: 'ScenicManagement',
        component: () => import('@/views/backend/scenic/index.vue'),
        meta: { title: '景点管理', icon: 'Location' }
      },
      {
        path: 'category',
        name: 'CategoryManagement',
        component: () => import('@/views/backend/category/CategoryList.vue'),
        meta: { title: '分类管理', icon: 'Menu' }
      },
      {
        path: 'comment',
        name: 'CommentManagement',
        component: () => import('@/views/backend/comment/index.vue'),
        meta: { title: '评论管理', icon: 'ChatDotRound' }
      },
      {
        path: 'guide',
        name: 'GuideManagement',
        component: () => import('@/views/backend/guide/GuideList.vue'),
        meta: { title: '攻略管理', icon: 'Document' }
      },
      {
        path: 'collection',
        name: 'CollectionManagement',
        component: () => import('@/views/backend/collection/CollectionList.vue'),
        meta: { title: '收藏管理', icon: 'Star' }
      },
      {
        path: 'ticket',
        name: 'TicketManagement',
        component: () => import('@/views/backend/ticket/index.vue'),
        meta: { title: '行程管理', icon: 'Ticket' }
      },
      {
        path: 'recommend',
        name: 'RecommendManagement',
        component: () => import('@/views/backend/recommend/RecommendManager.vue'),
        meta: { title: '首页推荐', icon: 'Star' }
      },
      {
        path: 'site-footer',
        name: 'SiteFooterConfig',
        component: () => import('@/views/backend/site/FooterConfig.vue'),
        meta: { title: '网站页脚', icon: 'OfficeBuilding' }
      },
      {
        path: 'site-assets',
        name: 'SiteAssetConfig',
        component: () => import('@/views/backend/site/AssetConfig.vue'),
        meta: { title: '站点素材', icon: 'Picture' }
      },
      {
        path: 'site-settings',
        redirect: '/back/site-settings/site-access'
      },
      {
        path: 'site-settings/site-access',
        name: 'SiteAccessSetting',
        component: () => import('@/views/backend/site/WebsiteSettings.vue'),
        meta: { title: '网站开关', icon: 'SwitchButton', activeMenu: '/back/site-settings/site-access' }
      },
      {
        path: 'site-settings/device-access',
        name: 'DeviceAccessSetting',
        component: () => import('@/views/backend/site/WebsiteSettings.vue'),
        meta: { title: '访问终端', icon: 'Monitor', activeMenu: '/back/site-settings/site-access' }
      },
      {
        path: 'log',
        name: 'LogManagement',
        component: () => import('@/views/backend/log/LogManager.vue'),
        meta: { title: '系统日志', icon: 'Document' }
      },
      {
        path: 'order',
        name: 'OrderManagement',
        component: () => import('@/views/backend/order/index.vue'),
        meta: { title: '订单管理', icon: 'List' }
      },
      {
        path: 'accommodation',
        name: 'AccommodationManagement',
        component: () => import('@/views/backend/accommodation/index.vue'),
        meta: { title: '住宿管理', icon: 'House' }
      },
      {
        path: 'carousel',
        name: 'CarouselManagement',
        component: () => import('@/views/backend/carousel/index.vue'),
        meta: { title: '轮播图管理', icon: 'Picture' }
      },
      {
        path: 'payment',
        name: 'PaymentManagement',
        component: () => import('@/views/backend/payment/PaymentConfig.vue'),
        meta: { title: '支付配置', icon: 'Wallet' }
      },
      {
        path: 'auth-config',
        name: 'AuthConfigManagement',
        component: () => import('@/views/backend/auth/AuthConfig.vue'),
        meta: { title: '认证配置', icon: 'Lock' }
      },
      {
        path: 'review',
        name: 'ReviewManagement',
        component: () => import('@/views/backend/review/ReviewManager.vue'),
        meta: { title: '内容审核', icon: 'Checked' }
      },
      {
        path: 'sensitive-word',
        name: 'SensitiveWordManagement',
        component: () => import('@/views/backend/sensitive/SensitiveWordManager.vue'),
        meta: { title: '敏感词过滤', icon: 'Warning' }
      },
      {
        path: 'notification',
        name: 'NotificationManagement',
        component: () => import('@/views/backend/notification/NotificationManager.vue'),
        meta: { title: '站内消息', icon: 'Bell' }
      }
    ]
  }
]

const frontendRoutes = [
  {
    path: '/',
    component: () => import('@/layouts/FrontendLayout.vue'),
    children: [
      {
        path: '',
        name: 'Home',
        component: () => import('@/views/frontend/Home.vue'),
        meta: { title: '侠客行国旅首页', description: '侠客行国旅提供精品旅游线路、热门景点攻略、住宿推荐和在线行程预订服务。' }
      },
      {
        path: 'about',
        name: 'About',
        component: () => import('@/views/frontend/About.vue'),
        meta: { title: '关于侠客行', description: '了解侠客行国旅的旅行服务、企业资质、目的地资源与客户服务承诺。' }
      },
      {
        path: 'legal/:type',
        name: 'LegalInfo',
        component: () => import('@/views/frontend/LegalInfo.vue'),
        meta: { title: '服务与合规说明' }
      },
      {
        path: 'profile',
        name: 'Profile',
        component: () => import('@/views/frontend/profile/index.vue'),
        meta: { title: '个人中心', requiresAuth: true, indexable: false }
      },
      {
        path: 'accommodation',
        name: 'AccommodationList',
        component: () => import('@/views/frontend/accommodation/index.vue'),
        meta: { title: '景区住宿推荐', description: '查询景区周边酒店、民宿和特色住宿，了解价格区间、位置、评分和服务特色。' }
      },
      {
        path: 'accommodation/:id',
        name: 'AccommodationDetail',
        component: () => import('@/views/frontend/accommodation/detail.vue'),
        meta: { title: '住宿详情', description: '查看住宿详情、地址、价格区间、特色服务和周边景点信息。' }
      },
      {
        path: 'tickets',
        name: 'Tickets',
        component: () => import('@/views/frontend/ticket/index.vue'),
        meta: { title: '旅游线路预订', description: '浏览侠客行国旅精品旅游线路，按目的地、出发城市、行程天数和价格筛选。' }
      },
      {
        path: 'ticket/booking/:id',
        name: 'TicketBooking',
        component: () => import('@/views/frontend/ticket/booking.vue'),
        meta: { title: '预订行程', requiresAuth: true, indexable: false }
      },
      {
        path: 'orders',
        name: 'Orders',
        component: () => import('@/views/frontend/orders/index.vue'),
        meta: { title: '我的订单', requiresAuth: true, indexable: false }
      },
      {
        path: 'scenic',
        name: 'ScenicList',
        component: () => import('@/views/frontend/scenic/ScenicList.vue'),
        meta: { title: '热门旅游景点', description: '查看热门景点介绍、开放时间、地理位置、标签和周边旅游线路推荐。' }
      },
      {
        path: 'scenic/:id',
        name: 'ScenicDetail',
        component: () => import('@/views/frontend/scenic/ScenicDetail.vue'),
        meta: { title: '景点详情', description: '查看景点介绍、开放时间、地理位置、用户评价和周边行程住宿推荐。' }
      },
      {
        path: 'scenic/category/:categoryId',
        name: 'ScenicByCategory',
        component: () => import('@/views/frontend/scenic/ScenicList.vue'),
        props: true,
        meta: { title: '分类景点' }
      },
      {
        path: 'tour/:id',
        name: 'TourDetail',
        component: () => import('@/views/frontend/tour/TourDetail.vue'),
        meta: { title: '行程详情', description: '查看旅游线路详情、行程特色、价格、出发日期、套餐和预订说明。' }
      },
      {
        path: 'guide',
        name: 'GuideList',
        component: () => import('@/views/frontend/guide/GuideList.vue'),
        meta: { title: '旅游攻略', description: '阅读目的地旅游攻略、出行经验、玩法推荐和行程建议。' }
      },
      {
        path: 'guide/detail/:id',
        name: 'GuideDetail',
        component: () => import('@/views/frontend/guide/GuideDetail.vue'),
        meta: { title: '攻略详情', description: '查看旅游攻略详情，了解目的地玩法、路线安排和出行建议。' }
      },
      {
        path: 'guide/edit',
        name: 'GuideEdit',
        component: () => import('@/views/frontend/guide/GuideEdit.vue'),
        meta: { title: '发布攻略', requiresAuth: true, indexable: false }
      },
      {
        path: 'my-guide',
        name: 'MyGuideList',
        component: () => import('@/views/frontend/guide/MyGuideList.vue'),
        meta: { title: '我的攻略', requiresAuth: true, indexable: false }
      },
      {
        path: 'collection',
        name: 'Collection',
        component: () => import('@/views/frontend/collection/MyCollection.vue'),
        meta: { title: '我的收藏', requiresAuth: true, indexable: false }
      },
      {
        path: 'payment/alipay/:id',
        name: 'AlipayPayment',
        component: () => import('@/views/frontend/payment/alipay-form.vue'),
        meta: { title: '支付宝支付', requiresAuth: true, indexable: false }
      },
      {
        path: 'tour-order-confirm/:id',
        name: 'TourOrderConfirm',
        component: () => import('@/views/frontend/payment/tour-order-confirm.vue'),
        meta: { title: '订单确认', requiresAuth: true, indexable: false }
      },
      {
        path: 'tour-order-pay/:id',
        name: 'TourOrderPay',
        component: () => import('@/views/frontend/payment/tour-order-pay.vue'),
        meta: { title: '订单支付', requiresAuth: true, indexable: false }
      },
      {
        path: 'payment-failed',
        name: 'PaymentFailed',
        component: () => import('@/views/frontend/payment/payment-failed.vue'),
        meta: { title: '支付失败', indexable: false }
      },
      {
        path: 'payment/result',
        name: 'PaymentResult',
        component: () => import('@/views/frontend/payment/payment-result.vue'),
        meta: { title: '支付结果', indexable: false }
      }
    ]
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/auth/Login.vue'),
    meta: { title: '登录', indexable: false }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/auth/Register.vue'),
    meta: { title: '注册', indexable: false }
  }
]

const errorRoutes = [
  {
    path: '/404',
    name: '404',
    component: () => import('@/views/error/404.vue'),
    meta: { title: '404', indexable: false }
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/404'
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes: [
    ...frontendRoutes,
    ...backendRoutes,
    ...errorRoutes
  ],
  scrollBehavior(to, from, savedPosition) {
    if (savedPosition) {
      return savedPosition
    }
    if (to.name === 'LegalInfo' && from.name === 'LegalInfo') {
      return false
    }
    return { top: 0 }
  }
})

router.beforeEach((to, from, next) => {
  updateSeo({
    title: to.meta.title,
    description: routeDescription(to),
    path: to.path,
    indexable: isRouteIndexable(to)
  })

  const userStore = useUserStore()
  userStore.syncFromStorage()

  if (userStore.token && userStore.isTokenExpired) {
    userStore.clearUserInfo()
    if (to.matched.some(record => record.meta.requiresAuth)) {
      next({
        path: '/login',
        query: { redirect: to.fullPath }
      })
      return
    }
  }

  logger.debug('Current route:', to.path)
  logger.debug('User status:', {
    isLoggedIn: userStore.isLoggedIn,
    isUser: userStore.isUser,
    isTokenExpired: userStore.isTokenExpired
  })

  if (to.matched.some(record => record.meta.requiresAuth) && !userStore.isLoggedIn) {
    next({
      path: '/login',
      query: { redirect: to.fullPath }
    })
    return
  }

  if (userStore.isLoggedIn) {
    if (to.path === '/login') {
      next(userStore.isUser ? '/' : '/back/dashboard')
      return
    }

    if (userStore.isUser && to.path.startsWith('/back')) {
      next('/')
      return
    }

    next()
    return
  }

  if (to.path.startsWith('/back')) {
    next('/login')
    return
  }

  next()
})

export default router
