<template>
  <div class="dashboard-workbench">
    <section class="hero-panel">
      <div class="hero-copy">
        <p class="eyebrow">Management Console</p>
        <h1>{{ greeting }}，{{ displayName }}</h1>
        <p class="hero-desc">围绕订单履约、内容审核、产品维护和系统配置开展日常运营。</p>
        <div class="hero-meta">
          <span>{{ currentTime }}</span>
          <span>{{ roleLabel }}</span>
        </div>
      </div>

      <div class="hero-actions">
        <button
          v-for="item in primaryActions"
          :key="item.path"
          type="button"
          class="primary-action"
          @click="navigateTo(item.path)"
        >
          <span class="action-icon">
            <el-icon><component :is="item.icon" /></el-icon>
          </span>
          <span>
            <strong>{{ item.title }}</strong>
            <small>{{ item.desc }}</small>
          </span>
        </button>
      </div>
    </section>

    <section class="metric-grid">
      <article v-for="item in metrics" :key="item.title" class="metric-card">
        <div class="metric-icon">
          <el-icon><component :is="item.icon" /></el-icon>
        </div>
        <div>
          <span>{{ item.title }}</span>
          <strong>{{ item.value }}</strong>
          <p>{{ item.desc }}</p>
        </div>
      </article>
    </section>

    <section class="workbench-grid">
      <article class="panel module-panel">
        <div class="panel-header">
          <div>
            <p class="eyebrow">Workflow</p>
            <h2>日常运营入口</h2>
          </div>
        </div>

        <div class="module-list">
          <button
            v-for="item in dailyModules"
            :key="item.path"
            type="button"
            class="module-item"
            @click="navigateTo(item.path)"
          >
            <span class="module-icon">
              <el-icon><component :is="item.icon" /></el-icon>
            </span>
            <span class="module-copy">
              <strong>{{ item.title }}</strong>
              <small>{{ item.desc }}</small>
            </span>
          </button>
        </div>
      </article>

      <article class="panel standard-panel">
        <div class="panel-header">
          <div>
            <p class="eyebrow">Standard</p>
            <h2>推荐处理顺序</h2>
          </div>
        </div>

        <div class="timeline">
          <div v-for="(item, index) in standards" :key="item.title" class="timeline-item">
            <span>{{ index + 1 }}</span>
            <div>
              <strong>{{ item.title }}</strong>
              <p>{{ item.desc }}</p>
            </div>
          </div>
        </div>
      </article>
    </section>

    <section class="panel system-panel">
      <div class="panel-header">
        <div>
          <p class="eyebrow">Configuration</p>
          <h2>系统与官网配置</h2>
        </div>
      </div>

      <div class="config-grid">
        <button
          v-for="item in configModules"
          :key="item.path"
          type="button"
          class="config-item"
          @click="navigateTo(item.path)"
        >
          <el-icon><component :is="item.icon" /></el-icon>
          <span>{{ item.title }}</span>
        </button>
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
import {
  Bell,
  Checked,
  ChatDotRound,
  CollectionTag,
  Document,
  Files,
  House,
  List,
  Location,
  Lock,
  Menu,
  OfficeBuilding,
  Picture,
  PriceTag,
  Setting,
  Star,
  Ticket,
  User,
  UserFilled,
  Wallet,
  Warning
} from '@element-plus/icons-vue'

const router = useRouter()
const userStore = useUserStore()
const userInfo = computed(() => userStore.userInfo)
const currentTime = ref('')
let timeInterval = null

const displayName = computed(() => userInfo.value?.nickname || userInfo.value?.name || userInfo.value?.username || '管理员')

const roleLabel = computed(() => {
  const roleMap = {
    SUPER_ADMIN: '超级管理员',
    ADMIN: '管理员',
    USER: '普通用户'
  }
  return roleMap[userInfo.value?.roleCode] || '后台用户'
})

const greeting = computed(() => {
  const hour = new Date().getHours()
  if (hour < 6) return '夜深了'
  if (hour < 12) return '上午好'
  if (hour < 18) return '下午好'
  return '晚上好'
})

const allPrimaryActions = [
  { title: '处理订单', desc: '查看支付、退款与履约状态', path: '/back/order', icon: List, adminOnly: true },
  { title: '内容审核', desc: '审核评论、攻略与展示内容', path: '/back/review', icon: Checked, adminOnly: true },
  { title: '维护行程', desc: '更新线路、套餐与班期', path: '/back/ticket', icon: Ticket, adminOnly: true }
]

const allDailyModules = [
  { title: '订单管理', desc: '订单、支付、退款与履约处理', path: '/back/order', icon: List, adminOnly: true },
  { title: '用户管理', desc: '账号状态、角色与安全信息维护', path: '/back/user', icon: UserFilled, adminOnly: true },
  { title: '站内消息', desc: '查看通知、运营提醒和系统消息', path: '/back/notification', icon: Bell, adminOnly: true },
  { title: '行程管理', desc: '线路产品、套餐、班期和上下架', path: '/back/ticket', icon: Ticket, adminOnly: true },
  { title: '首页推荐', desc: '精选行程与官网推荐位配置', path: '/back/recommend', icon: Star, adminOnly: true },
  { title: '景点管理', desc: '目的地基础信息与展示内容', path: '/back/scenic', icon: Location, adminOnly: true },
  { title: '住宿管理', desc: '酒店住宿信息与展示维护', path: '/back/accommodation', icon: House },
  { title: '攻略管理', desc: '攻略内容管理与发布状态跟进', path: '/back/guide', icon: Document, adminOnly: true },
  { title: '评论管理', desc: '评论内容、状态和关联对象管理', path: '/back/comment', icon: ChatDotRound, adminOnly: true },
  { title: '收藏管理', desc: '景点、行程和攻略收藏数据管理', path: '/back/collection', icon: CollectionTag, adminOnly: true },
  { title: '分类管理', desc: '栏目分类与内容归档结构维护', path: '/back/category', icon: Menu },
  { title: '个人信息', desc: '当前后台账号资料与安全信息', path: '/back/profile', icon: User }
]

const allConfigModules = [
  { title: '景点标签', path: '/back/tag', icon: PriceTag, adminOnly: true },
  { title: '轮播图管理', path: '/back/carousel', icon: Picture, adminOnly: true },
  { title: '网站页脚', path: '/back/site-footer', icon: OfficeBuilding, adminOnly: true },
  { title: '网站设置', path: '/back/site-settings/site-access', icon: Setting, adminOnly: true },
  { title: '支付配置', path: '/back/payment', icon: Wallet, adminOnly: true },
  { title: '认证配置', path: '/back/auth-config', icon: Lock, adminOnly: true },
  { title: '敏感词过滤', path: '/back/sensitive-word', icon: Warning, adminOnly: true },
  { title: '系统日志', path: '/back/log', icon: Files, adminOnly: true }
]

const allowModule = (item) => !item.adminOnly || userStore.isAdmin

const primaryActions = computed(() => allPrimaryActions.filter(allowModule))
const dailyModules = computed(() => allDailyModules.filter(allowModule))
const configModules = computed(() => allConfigModules.filter(allowModule))

const metrics = computed(() => [
  { title: '今日重点', value: userStore.isAdmin ? '订单履约' : '资料维护', desc: userStore.isAdmin ? '优先处理支付、退款与客户通知' : '维护当前账号资料与住宿内容', icon: List },
  { title: '内容质量', value: '审核优先', desc: '先审核，再展示，降低前台风险', icon: Checked },
  { title: '系统状态', value: '配置可控', desc: '网站开关、支付、认证集中管理', icon: Setting },
  { title: '安全记录', value: '可追踪', desc: '关键操作进入日志与消息链路', icon: Files }
])

const standards = [
  { title: '先处理订单', desc: '优先查看待支付、已支付、退款和需要人工跟进的订单。' },
  { title: '再审核内容', desc: '攻略、评论和前台展示内容通过审核后再进入公开展示。' },
  { title: '维护产品资料', desc: '行程、景点、住宿、分类与推荐位按业务变化及时更新。' },
  { title: '最后检查配置', desc: '支付、认证、网站开关和系统日志作为每日收尾检查项。' }
]

const updateTime = () => {
  const now = new Date()
  currentTime.value = now.toLocaleString('zh-CN', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
    weekday: 'long',
    hour: '2-digit',
    minute: '2-digit'
  })
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
  }
})
</script>

<style lang="scss" scoped>
.dashboard-workbench {
  min-height: 100%;
  color: #172033;
  background: #ffffff;
}

.hero-panel,
.panel,
.metric-card {
  border: 1px solid #edf1f6;
  border-radius: 22px;
  background: #ffffff;
  box-shadow: 0 14px 36px rgba(15, 23, 42, 0.05);
}

.hero-panel {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(360px, 520px);
  gap: 32px;
  align-items: center;
  padding: 30px;
  background:
    linear-gradient(135deg, rgba(248, 251, 255, 0.96), #ffffff 58%),
    radial-gradient(circle at 88% 12%, rgba(37, 99, 235, 0.1), transparent 30%);
}

.eyebrow {
  margin: 0 0 10px;
  color: #2563eb;
  font-size: 12px;
  font-weight: 850;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.hero-copy h1,
.panel-header h2 {
  margin: 0;
  color: #172033;
  font-weight: 900;
  letter-spacing: 0;
}

.hero-copy h1 {
  font-size: 32px;
  line-height: 1.25;
}

.hero-desc {
  max-width: 620px;
  margin: 14px 0 0;
  color: #64748b;
  font-size: 15px;
  line-height: 1.8;
}

.hero-meta {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  max-width: 620px;
  gap: 10px;
  margin: 18px 0 0;

  span {
    padding: 8px 12px;
    border-radius: 999px;
    color: #475569;
    font-size: 13px;
    font-weight: 700;
    background: #f6f8fb;
  }
}

.hero-actions {
  display: grid;
  gap: 12px;
}

.primary-action,
.module-item,
.config-item {
  border: 0;
  cursor: pointer;
  text-align: left;
  background: transparent;
  font-family: inherit;
}

.primary-action {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 16px;
  border: 1px solid #e7edf5;
  border-radius: 18px;
  background: #ffffff;
  transition: transform 0.18s ease, border-color 0.18s ease, box-shadow 0.18s ease;

  &:hover {
    border-color: #bfdbfe;
    box-shadow: 0 16px 30px rgba(37, 99, 235, 0.1);
    transform: translateY(-2px);
  }

  strong,
  small {
    display: block;
  }

  strong {
    color: #172033;
    font-size: 15px;
    font-weight: 850;
  }

  small {
    margin-top: 5px;
    color: #64748b;
    font-size: 12px;
  }
}

.action-icon,
.module-icon,
.metric-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 auto;
  color: #1d4ed8;
  background: #eef6ff;
}

.action-icon {
  width: 44px;
  height: 44px;
  border-radius: 15px;
  font-size: 22px;
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
  margin-top: 18px;
}

.metric-card {
  display: flex;
  gap: 14px;
  padding: 18px;
}

.metric-icon {
  width: 42px;
  height: 42px;
  border-radius: 14px;
  font-size: 20px;
}

.metric-card span,
.metric-card p {
  color: #64748b;
  font-size: 12px;
}

.metric-card strong {
  display: block;
  margin: 6px 0;
  color: #172033;
  font-size: 20px;
  font-weight: 900;
}

.metric-card p {
  margin: 0;
  line-height: 1.55;
}

.workbench-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.35fr) minmax(320px, 0.65fr);
  gap: 18px;
  margin-top: 18px;
}

.panel {
  padding: 22px;
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 18px;
}

.panel-header h2 {
  font-size: 20px;
}

.module-list {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.module-item {
  display: flex;
  gap: 13px;
  align-items: center;
  min-height: 78px;
  padding: 14px;
  border: 1px solid #edf1f6;
  border-radius: 16px;
  background: #fbfcfe;
  transition: transform 0.18s ease, border-color 0.18s ease, background-color 0.18s ease;

  &:hover {
    border-color: #bfdbfe;
    background: #f8fbff;
    transform: translateY(-2px);
  }
}

.module-icon {
  width: 40px;
  height: 40px;
  border-radius: 14px;
  font-size: 20px;
}

.module-copy {
  min-width: 0;

  strong,
  small {
    display: block;
  }

  strong {
    color: #172033;
    font-size: 15px;
    font-weight: 850;
  }

  small {
    margin-top: 6px;
    overflow: hidden;
    color: #64748b;
    font-size: 12px;
    line-height: 1.45;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}

.timeline {
  display: grid;
  gap: 14px;
}

.timeline-item {
  display: grid;
  grid-template-columns: 34px minmax(0, 1fr);
  gap: 12px;

  > span {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 30px;
    height: 30px;
    border-radius: 999px;
    color: #1d4ed8;
    font-size: 13px;
    font-weight: 900;
    background: #eef6ff;
  }

  strong {
    display: block;
    color: #172033;
    font-size: 14px;
    font-weight: 850;
  }

  p {
    margin: 6px 0 0;
    color: #64748b;
    font-size: 12px;
    line-height: 1.65;
  }
}

.system-panel {
  margin-top: 18px;
}

.config-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.config-item {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 9px;
  min-height: 54px;
  border: 1px solid #edf1f6;
  border-radius: 15px;
  color: #475569;
  font-size: 14px;
  font-weight: 800;
  background: #fbfcfe;
  transition: transform 0.18s ease, color 0.18s ease, border-color 0.18s ease, background-color 0.18s ease;

  &:hover {
    color: #1d4ed8;
    border-color: #bfdbfe;
    background: #f8fbff;
    transform: translateY(-2px);
  }
}

@media (max-width: 1280px) {
  .hero-panel,
  .workbench-grid {
    grid-template-columns: 1fr;
  }

  .metric-grid,
  .config-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
