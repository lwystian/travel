<template>
  <div class="dashboard">
    <!-- 背景装饰 -->
    <div class="bg-decoration">
      <div class="cloud cloud-1"></div>
      <div class="cloud cloud-2"></div>
      <div class="cloud cloud-3"></div>
      <div class="mountain mountain-1"></div>
      <div class="mountain mountain-2"></div>
    </div>

    <!-- 欢迎卡片 -->
    <el-card class="welcome-card">
      <template #header>
        <div class="welcome-header">
          <div class="avatar-container">
            <el-avatar :size="64" :src="avatarUrl">
              {{ userInfo?.name?.charAt(0) }}
            </el-avatar>
            <div class="avatar-ring"></div>
          </div>
          <div class="welcome-info">
            <h2>
              <el-icon class="welcome-icon"><Sunny /></el-icon>
              欢迎回来, {{ userInfo?.name || userInfo?.username }}
            </h2>
            <p>
              <el-icon><Clock /></el-icon>
              {{ currentTime }}
            </p>
            <p class="welcome-subtitle">开启美好的旅游管理之旅</p>
          </div>
        </div>
      </template>
      <div class="role-info">
        <el-tag type="success" effect="light" size="large">
          <el-icon><User /></el-icon>
          {{ roleLabel }}
        </el-tag>
      </div>
    </el-card>

    <!-- 快捷功能卡片 -->
    <div class="quick-actions">
      <el-row :gutter="20">
        <el-col v-for="item in quickActions" :key="item.path" :xs="24" :sm="12" :md="8" :lg="6">
          <el-card class="action-card" :class="item.cardClass" @click="navigateTo(item.path)">
            <div class="action-content">
              <el-icon class="action-icon"><component :is="item.icon" /></el-icon>
              <h3>{{ item.title }}</h3>
              <p>{{ item.desc }}</p>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>
  </div>
</template>

<script setup>
import { computed, ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
import { 
  Sunny, 
  Clock, 
  User, 
  MapLocation, 
  House, 
  Document, 
  UserFilled,
  ChatDotRound,
  Ticket,
  Picture,
  Star,
  PriceTag,
  Menu,
  Checked,
  Warning,
  Bell,
  Wallet,
  List,
  Setting
} from '@element-plus/icons-vue'

const router = useRouter()
const userStore = useUserStore()
const userInfo = computed(() => userStore.userInfo)
const baseAPI = process.env.VUE_APP_BASE_API || '/api'

// 角色标签
const roleLabel = computed(() => {
  const roleMap = {
    'SUPER_ADMIN': '超级管理员',
    'ADMIN': '系统管理员',
    'USER': '普通用户'
  }
  return roleMap[userInfo.value?.roleCode] || '未知角色'
})

const avatarUrl = computed(() => {
  return userInfo.value?.avatar ? baseAPI + userInfo.value.avatar : '';
})

const quickActions = [
  { title: '用户管理', desc: '管理系统用户信息', path: '/back/user', icon: UserFilled, cardClass: 'user-card' },
  { title: '个人信息', desc: '维护当前账号资料', path: '/back/profile', icon: User, cardClass: 'profile-card' },
  { title: '景点管理', desc: '管理旅游景点信息', path: '/back/scenic', icon: MapLocation, cardClass: 'scenic-card' },
  { title: '景点标签', desc: '维护景点标签体系', path: '/back/tag', icon: PriceTag, cardClass: 'tag-card' },
  { title: '分类管理', desc: '维护内容分类目录', path: '/back/category', icon: Menu, cardClass: 'category-card' },
  { title: '内容审核', desc: '审核评论与攻略内容', path: '/back/review', icon: Checked, cardClass: 'review-card' },
  { title: '敏感词过滤', desc: '维护发布内容风控规则', path: '/back/sensitive-word', icon: Warning, cardClass: 'sensitive-card' },
  { title: '站内消息', desc: '发送和管理系统通知', path: '/back/notification', icon: Bell, cardClass: 'notification-card' },
  { title: '评论管理', desc: '管理用户评论信息', path: '/back/comment', icon: ChatDotRound, cardClass: 'comment-card' },
  { title: '攻略管理', desc: '管理旅游攻略内容', path: '/back/guide', icon: Document, cardClass: 'guide-card' },
  { title: '收藏管理', desc: '管理用户收藏信息', path: '/back/collection', icon: Star, cardClass: 'collection-card' },
  { title: '行程管理', desc: '管理景点行程信息', path: '/back/ticket', icon: Ticket, cardClass: 'ticket-card' },
  { title: '首页推荐', desc: '维护首页推荐内容', path: '/back/recommend', icon: Star, cardClass: 'recommend-card' },
  { title: '网站设置', desc: '控制官网开放与访问终端策略', path: '/back/site-settings/site-access', icon: Setting, cardClass: 'setting-card' },
  { title: '订单管理', desc: '处理订单与退款流程', path: '/back/order', icon: List, cardClass: 'order-card' },
  { title: '住宿管理', desc: '管理酒店住宿信息', path: '/back/accommodation', icon: House, cardClass: 'accommodation-card' },
  { title: '轮播图管理', desc: '管理首页轮播图片', path: '/back/carousel', icon: Picture, cardClass: 'carousel-card' },
  { title: '支付配置', desc: '管理支付方式和状态', path: '/back/payment', icon: Wallet, cardClass: 'payment-card' },
  { title: '系统日志', desc: '系统操作日志记录', path: '/back/log', icon: Document, cardClass: 'log-card' }
]

// 当前时间
const currentTime = ref('')
let timeInterval = null

const updateTime = () => {
  const now = new Date()
  const options = { 
    year: 'numeric', 
    month: 'long', 
    day: 'numeric', 
    weekday: 'long',
    hour: '2-digit',
    minute: '2-digit'
  }
  currentTime.value = now.toLocaleDateString('zh-CN', options)
}

const navigateTo = (path) => {
  router.push(path)
}

onMounted(() => {
  updateTime()
  timeInterval = setInterval(updateTime, 60000)
})

onUnmounted(() => {
  if (timeInterval) {
    clearInterval(timeInterval)
    timeInterval = null
  }
})
</script>

<style lang="scss" scoped>
.dashboard {
  position: relative;
  min-height: 100vh;
  padding: 20px;
  overflow: hidden;

  // 背景装饰
  .bg-decoration {
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    pointer-events: none;
    z-index: 0;

    .cloud {
      position: absolute;
      background: rgba(255, 255, 255, 0.1);
      border-radius: 50px;
      animation: float 6s ease-in-out infinite;

      &::before, &::after {
        content: '';
        position: absolute;
        background: rgba(255, 255, 255, 0.1);
        border-radius: 50px;
      }

      &.cloud-1 {
        width: 100px;
        height: 40px;
        top: 10%;
        left: 10%;
        animation-delay: 0s;

        &::before {
          width: 50px;
          height: 50px;
          top: -25px;
          left: 10px;
        }

        &::after {
          width: 60px;
          height: 40px;
          top: -15px;
          right: 10px;
        }
      }

      &.cloud-2 {
        width: 80px;
        height: 30px;
        top: 20%;
        right: 15%;
        animation-delay: 2s;

        &::before {
          width: 40px;
          height: 40px;
          top: -20px;
          left: 15px;
        }

        &::after {
          width: 50px;
          height: 30px;
          top: -10px;
          right: 15px;
        }
      }

      &.cloud-3 {
        width: 120px;
        height: 50px;
        top: 5%;
        right: 40%;
        animation-delay: 4s;

        &::before {
          width: 60px;
          height: 60px;
          top: -30px;
          left: 20px;
        }

        &::after {
          width: 70px;
          height: 50px;
          top: -20px;
          right: 20px;
        }
      }
    }

    .mountain {
      position: absolute;
      bottom: 0;
      background: rgba(255, 255, 255, 0.05);
      
      &.mountain-1 {
        left: 0;
        width: 0;
        height: 0;
        border-left: 200px solid transparent;
        border-right: 200px solid transparent;
        border-bottom: 150px solid rgba(255, 255, 255, 0.05);
        background: none;
      }

      &.mountain-2 {
        right: 0;
        width: 0;
        height: 0;
        border-left: 150px solid transparent;
        border-right: 150px solid transparent;
        border-bottom: 100px solid rgba(255, 255, 255, 0.03);
        background: none;
      }
    }
  }

  .welcome-card {
    position: relative;
    z-index: 1;
    margin-bottom: 30px;
    border-radius: 20px;
    background: rgba(255, 255, 255, 0.95);
    backdrop-filter: blur(10px);
    border: 1px solid rgba(255, 255, 255, 0.2);
    box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
    transition: all 0.3s ease;
    
    &:hover {
      transform: translateY(-5px);
      box-shadow: 0 12px 40px rgba(0, 0, 0, 0.15);
    }
    
    .welcome-header {
      display: flex;
      align-items: center;
      gap: 25px;
      
      .avatar-container {
        position: relative;
        
        .el-avatar {
          background: linear-gradient(135deg, #667eea, #764ba2);
          transition: transform 0.3s ease;
          z-index: 2;
          position: relative;
        }

        .avatar-ring {
          position: absolute;
          top: -5px;
          left: -5px;
          right: -5px;
          bottom: -5px;
          border: 2px solid rgba(102, 126, 234, 0.3);
          border-radius: 50%;
          animation: pulse 2s infinite;
        }
      }
      
      .welcome-info {
        h2 {
          margin: 0 0 8px 0;
          font-size: 28px;
          background: linear-gradient(135deg, #667eea, #764ba2);
          -webkit-background-clip: text;
          -webkit-text-fill-color: transparent;
          display: flex;
          align-items: center;
          gap: 10px;

          .welcome-icon {
            color: #f39c12;
            animation: rotate 3s linear infinite;
          }
        }

        p {
          margin: 5px 0;
          color: #666;
          display: flex;
          align-items: center;
          gap: 8px;
        }

        .welcome-subtitle {
          color: #999;
          font-style: italic;
          font-size: 14px;
        }
      }
    }
    
    .role-info {
      margin-top: 20px;
      
      .el-tag {
        padding: 8px 16px;
        border-radius: 20px;
        font-weight: 500;
      }
    }
  }

  .quick-actions {
    position: relative;
    z-index: 1;

    :deep(.el-col) {
      margin-bottom: 20px;
    }

    .action-card {
      border-radius: 16px;
      border: none;
      cursor: pointer;
      transition: all 0.3s ease;
      background: rgba(255, 255, 255, 0.9);
      backdrop-filter: blur(10px);
      
      &:hover {
        transform: translateY(-8px) scale(1.02);
        box-shadow: 0 15px 35px rgba(0, 0, 0, 0.15);
      }

      .action-content {
        text-align: center;
        padding: 20px;

        .action-icon {
          font-size: 48px;
          margin-bottom: 15px;
          display: block;
        }

        h3 {
          margin: 0 0 8px 0;
          font-size: 18px;
          font-weight: 600;
        }

        p {
          margin: 0;
          color: #666;
          font-size: 14px;
        }
      }

      &.scenic-card .action-icon {
        color: #27ae60;
      }

      &.accommodation-card .action-icon {
        color: #3498db;
      }

      &.guide-card .action-icon {
        color: #e74c3c;
      }

      &.user-card .action-icon {
        color: #9b59b6;
      }

      &.comment-card .action-icon {
        color: #f39c12;
      }

      &.ticket-card .action-icon {
        color: #e67e22;
      }

      &.carousel-card .action-icon {
        color: #1abc9c;
      }

      &.collection-card .action-icon {
        color: #f1c40f;
      }

      &.log-card .action-icon {
        color: #95a5a6;
      }

      &.profile-card .action-icon {
        color: #16a085;
      }

      &.tag-card .action-icon {
        color: #d35400;
      }

      &.category-card .action-icon {
        color: #2c7be5;
      }

      &.review-card .action-icon {
        color: #2ecc71;
      }

      &.sensitive-card .action-icon {
        color: #e11d48;
      }

      &.notification-card .action-icon {
        color: #dc2626;
      }

      &.recommend-card .action-icon {
        color: #f97316;
      }

      &.order-card .action-icon {
        color: #6366f1;
      }

      &.payment-card .action-icon {
        color: #0ea5e9;
      }
    }
  }
}

@keyframes float {
  0%, 100% { transform: translateY(0px); }
  50% { transform: translateY(-20px); }
}

@keyframes pulse {
  0% { transform: scale(1); opacity: 1; }
  50% { transform: scale(1.1); opacity: 0.7; }
  100% { transform: scale(1); opacity: 1; }
}

@keyframes rotate {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}
</style>
