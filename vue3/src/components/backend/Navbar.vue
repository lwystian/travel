<template>
  <header class="navbar">
    <div class="left-menu">
      <button class="hamburger" type="button" @click="toggleSidebar" aria-label="切换菜单">
        <el-icon :size="20">
          <component :is="appStore.sidebarCollapsed ? Expand : Fold" />
        </el-icon>
      </button>

      <div class="route-meta">
        <div class="route-title">{{ route.meta.title || '控制台' }}</div>
        <el-breadcrumb separator="/">
          <el-breadcrumb-item :to="{ path: '/back/dashboard' }">首页</el-breadcrumb-item>
          <el-breadcrumb-item v-if="route.meta.title">{{ route.meta.title }}</el-breadcrumb-item>
        </el-breadcrumb>
      </div>
    </div>

    <div class="right-menu">
      <button class="nav-action" type="button" @click="toggleFullScreen" aria-label="切换全屏">
        <el-icon :size="19">
          <component :is="isFullscreen ? Aim : FullScreen" />
        </el-icon>
      </button>

      <div class="nav-action notification-wrap">
        <NotificationBell />
      </div>

      <el-dropdown trigger="click" popper-class="backend-user-dropdown" @command="handleUserCommand">
        <div class="avatar-wrapper">
          <el-avatar :size="34" :src="avatarUrl">
            {{ userInitial }}
          </el-avatar>
          <div class="user-copy">
            <span class="user-name">{{ displayName }}</span>
            <span class="user-role">{{ roleText }}</span>
          </div>
          <el-icon class="arrow-icon">
            <ArrowDown />
          </el-icon>
        </div>
        <template #dropdown>
          <el-dropdown-menu class="user-dropdown-menu">
            <el-dropdown-item command="dashboard">
              <el-icon><DataBoard /></el-icon>
              工作台首页
            </el-dropdown-item>
            <el-dropdown-item command="profile">
              <el-icon><User /></el-icon>
              个人信息
            </el-dropdown-item>
            <el-dropdown-item command="frontend">
              <el-icon><House /></el-icon>
              返回前台
            </el-dropdown-item>
            <el-dropdown-item v-if="userStore.isAdmin" command="site-settings">
              <el-icon><Setting /></el-icon>
              网站设置
            </el-dropdown-item>
            <el-dropdown-item divided command="logout" class="logout-item">
              <el-icon><SwitchButton /></el-icon>
              退出登录
            </el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </header>
</template>

<script setup>
import { computed, ref, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/store/user'
import { useAppStore } from '@/store/app'
import { ElMessageBox } from 'element-plus'
import { Expand, Fold, ArrowDown, SwitchButton, FullScreen, Aim, DataBoard, User, House, Setting } from '@element-plus/icons-vue'
import NotificationBell from '@/components/NotificationBell.vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const appStore = useAppStore()
const baseAPI = process.env.VUE_APP_BASE_API || '/api'
const userInfo = computed(() => userStore.userInfo)
const isFullscreen = ref(false)

const displayName = computed(() => userInfo.value?.name || userInfo.value?.nickname || userInfo.value?.username || '用户')
const userInitial = computed(() => displayName.value?.charAt(0)?.toUpperCase() || 'U')
const avatarUrl = computed(() => userInfo.value?.avatar ? baseAPI + userInfo.value.avatar : '')
const roleText = computed(() => userStore.isSuperAdmin ? '超级管理员' : userStore.isAdmin ? '管理员' : '用户')

const toggleSidebar = () => {
  appStore.toggleSidebar()
}

const toggleFullScreen = () => {
  if (!document.fullscreenElement) {
    document.documentElement.requestFullscreen()
    isFullscreen.value = true
    return
  }
  if (document.exitFullscreen) {
    document.exitFullscreen()
    isFullscreen.value = false
  }
}

const fullscreenChangeHandler = () => {
  isFullscreen.value = !!document.fullscreenElement
}

document.addEventListener('fullscreenchange', fullscreenChangeHandler)

onUnmounted(() => {
  document.removeEventListener('fullscreenchange', fullscreenChangeHandler)
})

const handleLogout = () => {
  ElMessageBox.confirm('确定要退出登录吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  }).then(async () => {
    await userStore.logout()
    router.push('/login')
  }).catch(() => {})
}

const handleUserCommand = (command) => {
  if (command === 'dashboard') {
    router.push('/back/dashboard')
    return
  }
  if (command === 'profile') {
    router.push('/back/profile')
    return
  }
  if (command === 'frontend') {
    window.open('/', '_blank', 'noopener,noreferrer')
    return
  }
  if (command === 'site-settings') {
    router.push('/back/site-settings/site-access')
    return
  }
  if (command === 'logout') {
    handleLogout()
  }
}
</script>

<style lang="scss" scoped>
.navbar {
  position: relative;
  z-index: 10;
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 72px;
  padding: 0 24px;
  border-bottom: 1px solid rgba(226, 232, 240, 0.9);
  background: rgba(255, 255, 255, 0.88);
  backdrop-filter: blur(16px);
}

.left-menu {
  display: flex;
  align-items: center;
  min-width: 0;
  gap: 16px;
}

.hamburger,
.nav-action {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 38px;
  height: 38px;
  padding: 0;
  border: 1px solid #dce5f1;
  border-radius: 13px;
  color: #475569;
  cursor: pointer;
  background: #ffffff;
  transition: color 0.16s ease, border-color 0.16s ease, box-shadow 0.16s ease, transform 0.16s ease;

  &:hover {
    color: #1d4ed8;
    border-color: #bad2f4;
    box-shadow: 0 10px 22px rgba(37, 99, 235, 0.1);
    transform: translateY(-1px);
  }
}

.route-meta {
  display: flex;
  flex-direction: column;
  min-width: 0;
  gap: 5px;
}

.route-title {
  max-width: 360px;
  overflow: hidden;
  color: #172033;
  font-size: 18px;
  font-weight: 850;
  line-height: 1.2;
  text-overflow: ellipsis;
  white-space: nowrap;
}

:deep(.el-breadcrumb) {
  line-height: 1;
}

:deep(.el-breadcrumb__inner) {
  color: #64748b;
  font-size: 12px;
  font-weight: 650;

  &.is-link {
    color: #94a3b8;

    &:hover {
      color: #2563eb;
    }
  }
}

.right-menu {
  display: flex;
  align-items: center;
  flex: 0 0 auto;
  gap: 10px;
}

.notification-wrap {
  border: 0;
  background: transparent;
  pointer-events: auto;

  :deep(.notification-bell) {
    width: 38px;
    height: 38px;
    border: 1px solid #dce5f1;
    border-radius: 13px;
    background: #fff;
  }
}

.avatar-wrapper {
  display: flex;
  align-items: center;
  gap: 10px;
  height: 46px;
  padding: 5px 8px 5px 6px;
  border: 1px solid #dce5f1;
  border-radius: 999px;
  cursor: pointer;
  background: #ffffff;
  transition: border-color 0.16s ease, box-shadow 0.16s ease, transform 0.16s ease;

  &:hover {
    border-color: #bad2f4;
    box-shadow: 0 10px 22px rgba(37, 99, 235, 0.1);
    transform: translateY(-1px);
  }

  :deep(.el-avatar) {
    color: #ffffff;
    font-weight: 800;
    background: linear-gradient(135deg, #0f766e, #2563eb);
  }
}

.user-copy {
  display: flex;
  flex-direction: column;
  min-width: 74px;
  max-width: 148px;
  line-height: 1.15;
}

.user-name {
  overflow: hidden;
  color: #172033;
  font-size: 13px;
  font-weight: 760;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.user-role {
  margin-top: 3px;
  color: #64748b;
  font-size: 11px;
  font-weight: 650;
}

.arrow-icon {
  color: #94a3b8;
  font-size: 14px;
}

:global(.backend-user-dropdown.el-popper) {
  margin-top: -2px !important;
  overflow: hidden;
  border: 1px solid #dce5f1 !important;
  border-radius: 16px !important;
  box-shadow: 0 18px 42px rgba(15, 23, 42, 0.12) !important;
}

:global(.backend-user-dropdown .el-popper__arrow) {
  display: none;
}

:deep(.user-dropdown-menu) {
  min-width: 184px;
  padding: 7px;
}

:deep(.el-dropdown-menu__item) {
  display: flex;
  align-items: center;
  gap: 9px;
  min-width: 180px;
  height: 40px;
  padding: 8px 12px;
  border-radius: 10px;
  color: #475569;
  font-weight: 700;

  &:hover {
    color: #1d4ed8;
    background: #f3f8ff;
  }

  .el-icon {
    margin-right: 0;
    color: inherit;
  }
}

:deep(.logout-item) {
  color: #dc2626;

  &:hover {
    color: #b91c1c;
    background: #fff1f2;
  }
}
</style>
