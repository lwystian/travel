<template>
  <main class="not-found-page">
    <section class="not-found-shell" aria-labelledby="not-found-title">
      <div class="not-found-copy">
        <div class="status-pill">
          <el-icon><Compass /></el-icon>
          <span>路线暂未抵达</span>
        </div>

        <p class="eyebrow">404 / PAGE NOT FOUND</p>
        <h1 id="not-found-title">这段旅程还没有开放</h1>
        <p class="description">
          你访问的页面可能已迁移、下架，或者地址里有一小段走偏了。
          可以回到首页重新出发，也可以继续探索热门目的地。
        </p>

        <div class="actions" aria-label="页面导航">
          <el-button type="primary" size="large" class="primary-action" @click="goHome">
            <el-icon><House /></el-icon>
            返回首页
          </el-button>
          <el-button size="large" class="secondary-action" @click="goBack">
            <el-icon><Back /></el-icon>
            返回上一页
          </el-button>
        </div>

        <nav class="quick-links" aria-label="快捷入口">
          <router-link v-for="item in quickLinks" :key="item.path" :to="item.path">
            <el-icon><component :is="item.icon" /></el-icon>
            <span>{{ item.label }}</span>
          </router-link>
        </nav>
      </div>

      <div class="visual-panel" aria-hidden="true">
        <div class="map-card">
          <div class="map-toolbar">
            <span></span>
            <span></span>
            <span></span>
          </div>
          <div class="route-field">
            <div class="route-line"></div>
            <div class="pin pin-start">
              <el-icon><Location /></el-icon>
            </div>
            <div class="pin pin-end">
              <span>?</span>
            </div>
            <div class="landmark landmark-a"></div>
            <div class="landmark landmark-b"></div>
            <div class="landmark landmark-c"></div>
          </div>
        </div>
        <div class="code-mark">404</div>
      </div>
    </section>
  </main>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { Back, Compass, House, Location, OfficeBuilding, Picture, Reading, Van } from '@element-plus/icons-vue'

const router = useRouter()

const quickLinks = [
  { label: '行程预订', path: '/tickets', icon: Van },
  { label: '景点推荐', path: '/scenic', icon: Picture },
  { label: '旅游攻略', path: '/guide', icon: Reading },
  { label: '景区住宿', path: '/accommodation', icon: OfficeBuilding }
]

const goHome = () => {
  router.push('/')
}

const goBack = () => {
  if (window.history.length > 1) {
    router.back()
    return
  }
  router.push('/')
}
</script>

<style scoped>
.not-found-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
  color: #1f2937;
  background:
    radial-gradient(circle at 18% 20%, rgba(74, 144, 226, 0.18), transparent 28%),
    radial-gradient(circle at 86% 78%, rgba(46, 204, 113, 0.16), transparent 30%),
    linear-gradient(135deg, #f6f9fc 0%, #edf4f8 50%, #f8fafc 100%);
  box-sizing: border-box;
}

.not-found-shell {
  width: min(1120px, 100%);
  min-height: 620px;
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(360px, 0.86fr);
  align-items: center;
  gap: 56px;
}

.not-found-copy {
  max-width: 620px;
}

.status-pill {
  width: fit-content;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  height: 36px;
  padding: 0 14px;
  border: 1px solid rgba(31, 41, 55, 0.1);
  border-radius: 999px;
  color: #0f766e;
  background: rgba(255, 255, 255, 0.76);
  box-shadow: 0 12px 36px rgba(15, 23, 42, 0.08);
  font-size: 14px;
  font-weight: 600;
}

.eyebrow {
  margin: 34px 0 14px;
  color: #64748b;
  font-size: 13px;
  font-weight: 700;
  letter-spacing: 0.08em;
}

h1 {
  margin: 0;
  color: #102033;
  font-size: 54px;
  line-height: 1.12;
  font-weight: 800;
  letter-spacing: 0;
}

.description {
  max-width: 560px;
  margin: 22px 0 0;
  color: #5f6f82;
  font-size: 17px;
  line-height: 1.9;
}

.actions {
  display: flex;
  flex-wrap: wrap;
  gap: 14px;
  margin-top: 34px;
}

.actions :deep(.el-button) {
  height: 46px;
  padding: 0 22px;
  border-radius: 8px;
  font-weight: 700;
}

.primary-action {
  border-color: #0f766e;
  background: #0f766e;
  box-shadow: 0 16px 30px rgba(15, 118, 110, 0.22);
}

.primary-action:hover,
.primary-action:focus {
  border-color: #115e59;
  background: #115e59;
}

.secondary-action {
  color: #334155;
  background: rgba(255, 255, 255, 0.72);
  border-color: rgba(148, 163, 184, 0.42);
}

.quick-links {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 36px;
}

.quick-links a {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  min-height: 40px;
  padding: 0 14px;
  border: 1px solid rgba(148, 163, 184, 0.28);
  border-radius: 8px;
  color: #334155;
  background: rgba(255, 255, 255, 0.58);
  text-decoration: none;
  font-size: 14px;
  font-weight: 600;
  transition: transform 0.2s ease, border-color 0.2s ease, background 0.2s ease;
}

.quick-links a:hover {
  transform: translateY(-2px);
  border-color: rgba(15, 118, 110, 0.42);
  background: rgba(255, 255, 255, 0.9);
}

.visual-panel {
  position: relative;
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 520px;
}

.map-card {
  position: relative;
  z-index: 2;
  width: min(430px, 100%);
  aspect-ratio: 0.82;
  overflow: hidden;
  border: 1px solid rgba(15, 23, 42, 0.1);
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 28px 70px rgba(15, 23, 42, 0.18);
}

.map-toolbar {
  height: 46px;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 0 18px;
  border-bottom: 1px solid rgba(148, 163, 184, 0.2);
  background: #f8fafc;
}

.map-toolbar span {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: #cbd5e1;
}

.map-toolbar span:nth-child(1) {
  background: #fb7185;
}

.map-toolbar span:nth-child(2) {
  background: #facc15;
}

.map-toolbar span:nth-child(3) {
  background: #34d399;
}

.route-field {
  position: relative;
  height: calc(100% - 46px);
  background:
    linear-gradient(90deg, rgba(148, 163, 184, 0.12) 1px, transparent 1px),
    linear-gradient(rgba(148, 163, 184, 0.12) 1px, transparent 1px),
    linear-gradient(145deg, #f8fafc, #ecfeff 52%, #f0fdf4);
  background-size: 44px 44px, 44px 44px, 100% 100%;
}

.route-line {
  position: absolute;
  left: 64px;
  right: 72px;
  top: 58%;
  height: 4px;
  border-radius: 999px;
  background: repeating-linear-gradient(
    90deg,
    #0f766e 0 14px,
    transparent 14px 24px
  );
  transform: rotate(-22deg);
}

.pin {
  position: absolute;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  box-shadow: 0 14px 28px rgba(15, 23, 42, 0.18);
}

.pin-start {
  left: 62px;
  bottom: 98px;
  width: 54px;
  height: 54px;
  color: #fff;
  background: #0f766e;
  font-size: 25px;
}

.pin-end {
  right: 58px;
  top: 92px;
  width: 76px;
  height: 76px;
  color: #0f172a;
  background: #fde68a;
  font-size: 34px;
  font-weight: 800;
}

.landmark {
  position: absolute;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.76);
  border: 1px solid rgba(148, 163, 184, 0.2);
  box-shadow: 0 16px 32px rgba(15, 23, 42, 0.08);
}

.landmark-a {
  left: 46px;
  top: 70px;
  width: 84px;
  height: 116px;
}

.landmark-b {
  right: 44px;
  bottom: 68px;
  width: 118px;
  height: 82px;
}

.landmark-c {
  left: 154px;
  bottom: 142px;
  width: 72px;
  height: 72px;
}

.code-mark {
  position: absolute;
  right: 0;
  bottom: 22px;
  z-index: 1;
  color: rgba(15, 23, 42, 0.08);
  font-size: 170px;
  line-height: 1;
  font-weight: 900;
}

@media (max-width: 900px) {
  .not-found-page {
    align-items: flex-start;
    padding: 32px 18px;
  }

  .not-found-shell {
    min-height: auto;
    grid-template-columns: 1fr;
    gap: 34px;
  }

  .not-found-copy {
    max-width: none;
  }

  h1 {
    font-size: 38px;
  }

  .description {
    font-size: 16px;
  }

  .visual-panel {
    min-height: auto;
  }

  .map-card {
    width: min(420px, 100%);
    aspect-ratio: 1.08;
  }

  .code-mark {
    display: none;
  }
}

@media (max-width: 520px) {
  .not-found-page {
    padding: 24px 16px;
  }

  .eyebrow {
    margin-top: 26px;
  }

  h1 {
    font-size: 32px;
  }

  .actions {
    flex-direction: column;
  }

  .actions :deep(.el-button) {
    width: 100%;
    margin-left: 0;
  }

  .quick-links {
    display: grid;
    grid-template-columns: 1fr;
  }

  .quick-links a {
    justify-content: center;
  }
}
</style>
