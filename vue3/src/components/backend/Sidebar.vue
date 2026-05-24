<template>
  <aside class="sidebar-container" :class="{ 'is-collapsed': isCollapsed }">
    <div class="logo">
      <img class="logo-mark" :src="logoUrl" alt="侠客行国旅" />
      <span class="logo-text" v-show="!isCollapsed">侠客行国旅</span>
    </div>

    <div class="menu-wrapper">
      <el-menu
        :default-active="activeMenu"
        :collapse="isCollapsed"
        :collapse-transition="false"
        mode="vertical"
        class="sidebar-menu"
        router
      >
        <el-menu-item index="/back/dashboard">
          <el-icon><HomeFilled /></el-icon>
          <template #title>首页</template>
        </el-menu-item>

        <el-menu-item index="/back/profile">
          <el-icon><UserFilled /></el-icon>
          <template #title>个人信息</template>
        </el-menu-item>

        <el-menu-item index="/back/order" v-if="userStore.isAdmin">
          <el-icon><List /></el-icon>
          <template #title>订单管理</template>
        </el-menu-item>

        <el-menu-item index="/back/user" v-if="userStore.isAdmin">
          <el-icon><User /></el-icon>
          <template #title>用户管理</template>
        </el-menu-item>

        <el-menu-item index="/back/review" v-if="userStore.isAdmin">
          <el-icon><Checked /></el-icon>
          <template #title>内容审核</template>
        </el-menu-item>

        <el-menu-item index="/back/notification" v-if="userStore.isAdmin">
          <el-icon><Bell /></el-icon>
          <template #title>站内消息</template>
        </el-menu-item>

        <el-menu-item index="/back/ticket" v-if="userStore.isAdmin">
          <el-icon><Ticket /></el-icon>
          <template #title>行程管理</template>
        </el-menu-item>

        <el-menu-item index="/back/recommend" v-if="userStore.isAdmin">
          <el-icon><Star /></el-icon>
          <template #title>首页推荐</template>
        </el-menu-item>

        <el-menu-item index="/back/scenic" v-if="userStore.isAdmin">
          <el-icon><Location /></el-icon>
          <template #title>景点管理</template>
        </el-menu-item>

        <el-menu-item index="/back/accommodation">
          <el-icon><House /></el-icon>
          <template #title>住宿管理</template>
        </el-menu-item>

        <el-menu-item index="/back/guide" v-if="userStore.isAdmin">
          <el-icon><Document /></el-icon>
          <template #title>攻略管理</template>
        </el-menu-item>

        <el-menu-item index="/back/comment" v-if="userStore.isAdmin">
          <el-icon><ChatDotRound /></el-icon>
          <template #title>评论管理</template>
        </el-menu-item>

        <el-menu-item index="/back/collection" v-if="userStore.isAdmin">
          <el-icon><CollectionTag /></el-icon>
          <template #title>收藏管理</template>
        </el-menu-item>

        <el-menu-item index="/back/category">
          <el-icon><Menu /></el-icon>
          <template #title>分类管理</template>
        </el-menu-item>

        <el-menu-item index="/back/carousel" v-if="userStore.isAdmin">
          <el-icon><Picture /></el-icon>
          <template #title>轮播图管理</template>
        </el-menu-item>

        <el-menu-item index="/back/site-footer" v-if="userStore.isAdmin">
          <el-icon><OfficeBuilding /></el-icon>
          <template #title>网站页脚</template>
        </el-menu-item>

        <el-menu-item index="/back/site-assets" v-if="userStore.isAdmin">
          <el-icon><Picture /></el-icon>
          <template #title>站点素材</template>
        </el-menu-item>

        <el-menu-item index="/back/site-settings/site-access" v-if="userStore.isAdmin">
          <el-icon><Setting /></el-icon>
          <template #title>网站设置</template>
        </el-menu-item>

        <el-menu-item index="/back/payment" v-if="userStore.isAdmin">
          <el-icon><Wallet /></el-icon>
          <template #title>支付配置</template>
        </el-menu-item>

        <el-menu-item index="/back/auth-config" v-if="userStore.isAdmin">
          <el-icon><Lock /></el-icon>
          <template #title>认证配置</template>
        </el-menu-item>

        <el-menu-item index="/back/sensitive-word" v-if="userStore.isAdmin">
          <el-icon><Warning /></el-icon>
          <template #title>敏感词过滤</template>
        </el-menu-item>

        <el-menu-item index="/back/log" v-if="userStore.isAdmin">
          <el-icon><Files /></el-icon>
          <template #title>系统日志</template>
        </el-menu-item>
      </el-menu>
    </div>
  </aside>
</template>

<script setup>
import { computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { useAppStore } from '@/store/app'
import { useUserStore } from '@/store/user'
import { useSiteAssets, getAssetUrl } from '@/utils/siteAssets'
import noImage from '@/assets/images/no-image.png'
import {
  HomeFilled,
  User,
  UserFilled,
  Location,
  Menu,
  ChatDotRound,
  Document,
  Star,
  Ticket,
  List,
  House,
  Picture,
  Wallet,
  Lock,
  Checked,
  Warning,
  Bell,
  OfficeBuilding,
  Setting,
  Files,
  CollectionTag
} from '@element-plus/icons-vue'

const route = useRoute()
const appStore = useAppStore()
const userStore = useUserStore()
const { siteAssets, loadSiteAssets } = useSiteAssets()

const isCollapsed = computed(() => appStore.sidebarCollapsed)
const logoUrl = computed(() => getAssetUrl(siteAssets.value.logoUrl, noImage))

const activeMenu = computed(() => {
  const { meta, path } = route
  return meta.activeMenu || path
})

onMounted(loadSiteAssets)
</script>

<style lang="scss" scoped>
.sidebar-container {
  position: relative;
  z-index: 12;
  display: flex;
  flex-direction: column;
  width: 248px;
  min-width: 248px;
  height: 100vh;
  overflow: hidden;
  border-right: 1px solid #edf1f6;
  background: #ffffff;
  box-shadow: 12px 0 34px rgba(15, 23, 42, 0.04);
  transition: width 0.24s ease, min-width 0.24s ease;

  &.is-collapsed {
    width: 78px;
    min-width: 78px;

    .logo {
      justify-content: center;
      padding: 0 12px;
    }

    .logo-mark {
      margin-right: 0;
    }

    :deep(.el-menu-item) {
      justify-content: center;
      width: 48px;
      padding: 0 !important;
      margin-right: auto;
      margin-left: auto;

      .el-icon {
        margin-right: 0;
      }
    }
  }
}

.logo {
  display: flex;
  align-items: center;
  height: 72px;
  padding: 0 22px;
  border-bottom: 1px solid #f0f3f8;
}

.logo-mark {
  display: block;
  width: 30px;
  height: 30px;
  margin-right: 12px;
  flex: 0 0 auto;
  object-fit: contain;
}

.logo-text {
  min-width: 0;
  color: #172033;
  font-size: 18px;
  font-weight: 850;
  letter-spacing: 0;
  white-space: nowrap;
}

.menu-wrapper {
  flex: 1;
  padding: 14px 0 18px;
  overflow-y: auto;
  overflow-x: hidden;

  &::-webkit-scrollbar {
    width: 6px;
  }

  &::-webkit-scrollbar-thumb {
    border-radius: 999px;
    background: rgba(148, 163, 184, 0.32);
  }

  &::-webkit-scrollbar-track {
    background: transparent;
  }
}

:deep(.sidebar-menu) {
  border-right: 0;
  background: transparent;

  .el-menu-item {
    position: relative;
    height: 46px;
    margin: 4px 14px;
    padding: 0 15px !important;
    border-radius: 14px;
    color: #61708a;
    font-weight: 680;
    line-height: 46px;
    transition: background-color 0.16s ease, color 0.16s ease, box-shadow 0.16s ease;

    &::after {
      content: '';
      position: absolute;
      left: 8px;
      top: 50%;
      width: 4px;
      height: 18px;
      border-radius: 999px;
      background: transparent;
      transform: translateY(-50%);
      transition: background-color 0.16s ease;
    }

    .el-icon {
      width: 20px;
      height: 20px;
      margin-right: 12px;
      color: inherit;
      font-size: 18px;
    }

    span {
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    &:hover {
      color: #1d4ed8;
      background: #f3f8ff;
    }

    &.is-active {
      color: #1d4ed8;
      background: #eef6ff;
      box-shadow: inset 0 0 0 1px rgba(37, 99, 235, 0.08);

      &::after {
        background: #2563eb;
      }
    }
  }

  &.el-menu--collapse {
    width: 78px;

    .el-tooltip__trigger {
      justify-content: center;
    }

    .el-menu-item::after {
      left: 4px;
    }
  }
}
</style>
