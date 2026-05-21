import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/store/user'
import BackendLayout from '@/layouts/BackendLayout.vue'
import logger from '@/utils/logger'

export const backendRoutes = [
  {
    path: '/back',
    component: BackendLayout,
    redirect: '/back/dashboard',
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
        path: 'tag',
        name: 'TagManagement',
        component: () => import('@/views/backend/tag/TagList.vue'),
        meta: { title: '标签管理', icon: 'PriceTag' }
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
        meta: { title: '首页' }
      },
      {
        path: 'about',
        name: 'About',
        component: () => import('@/views/frontend/About.vue'),
        meta: { title: '关于侠客行' }
      },
      {
        path: 'profile',
        name: 'Profile',
        component: () => import('@/views/frontend/profile/index.vue'),
        meta: { title: '个人中心', requiresAuth: true }
      },
      {
        path: 'accommodation',
        name: 'AccommodationList',
        component: () => import('@/views/frontend/accommodation/index.vue'),
        meta: { title: '住宿' }
      },
      {
        path: 'accommodation/:id',
        name: 'AccommodationDetail',
        component: () => import('@/views/frontend/accommodation/detail.vue'),
        meta: { title: '住宿详情' }
      },
      {
        path: 'tickets',
        name: 'Tickets',
        component: () => import('@/views/frontend/ticket/index.vue'),
        meta: { title: '行程预订' }
      },
      {
        path: 'ticket/booking/:id',
        name: 'TicketBooking',
        component: () => import('@/views/frontend/ticket/booking.vue'),
        meta: { title: '预订行程', requiresAuth: true }
      },
      {
        path: 'orders',
        name: 'Orders',
        component: () => import('@/views/frontend/orders/index.vue'),
        meta: { title: '我的订单', requiresAuth: true }
      },
      {
        path: 'scenic',
        name: 'ScenicList',
        component: () => import('@/views/frontend/scenic/ScenicList.vue'),
        meta: { title: '景点列表' }
      },
      {
        path: 'scenic/:id',
        name: 'ScenicDetail',
        component: () => import('@/views/frontend/scenic/ScenicDetail.vue'),
        meta: { title: '景点详情' }
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
        meta: { title: '行程详情' }
      },
      {
        path: 'guide',
        name: 'GuideList',
        component: () => import('@/views/frontend/guide/GuideList.vue'),
        meta: { title: '攻略列表' }
      },
      {
        path: 'guide/detail/:id',
        name: 'GuideDetail',
        component: () => import('@/views/frontend/guide/GuideDetail.vue'),
        meta: { title: '攻略详情' }
      },
      {
        path: 'guide/edit',
        name: 'GuideEdit',
        component: () => import('@/views/frontend/guide/GuideEdit.vue'),
        meta: { title: '发布攻略', requiresAuth: true }
      },
      {
        path: 'my-guide',
        name: 'MyGuideList',
        component: () => import('@/views/frontend/guide/MyGuideList.vue'),
        meta: { title: '我的攻略', requiresAuth: true }
      },
      {
        path: 'collection',
        name: 'Collection',
        component: () => import('@/views/frontend/collection/MyCollection.vue'),
        meta: { title: '我的收藏', requiresAuth: true }
      },
      {
        path: 'payment/alipay/:id',
        name: 'AlipayPayment',
        component: () => import('@/views/frontend/payment/alipay-form.vue'),
        meta: { title: '支付宝支付', requiresAuth: true }
      },
      {
        path: 'tour-order-confirm/:id',
        name: 'TourOrderConfirm',
        component: () => import('@/views/frontend/payment/tour-order-confirm.vue'),
        meta: { title: '订单确认', requiresAuth: true }
      },
      {
        path: 'tour-order-pay/:id',
        name: 'TourOrderPay',
        component: () => import('@/views/frontend/payment/tour-order-pay.vue'),
        meta: { title: '订单支付', requiresAuth: true }
      },
      {
        path: 'payment-failed',
        name: 'PaymentFailed',
        component: () => import('@/views/frontend/payment/payment-failed.vue'),
        meta: { title: '支付失败' }
      },
      {
        path: 'payment/result',
        name: 'PaymentResult',
        component: () => import('@/views/frontend/payment/payment-result.vue'),
        meta: { title: '支付结果' }
      }
    ]
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/auth/Login.vue'),
    meta: { title: '登录' }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/auth/Register.vue'),
    meta: { title: '注册' }
  }
]

const errorRoutes = [
  {
    path: '/404',
    name: '404',
    component: () => import('@/views/error/404.vue'),
    meta: { title: '404' }
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
  scrollBehavior() {
    return { top: 0 }
  }
})

router.beforeEach((to, from, next) => {
  document.title = to.meta.title ? `${to.meta.title} - 侠客行国旅` : '侠客行国旅'

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
