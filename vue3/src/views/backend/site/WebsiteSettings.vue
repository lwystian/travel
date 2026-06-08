<template>
  <div class="website-settings">
    <section class="settings-hero">
      <div class="hero-copy">
        <h1>网站设置</h1>
        <span>集中管理官网访问、访问终端与互动内容开关。</span>
      </div>
      <div class="status-group">
        <div class="site-status-pill" :class="{ closed: !form.siteEnabled }">
          <span class="status-dot"></span>
          <strong>{{ form.siteEnabled ? '网站已开启' : '网站已关闭' }}</strong>
        </div>
        <div class="site-status-pill" :class="{ closed: !form.publicInteractionEnabled }">
          <span class="status-dot"></span>
          <strong>{{ form.publicInteractionEnabled ? '互动已开启' : '互动已关闭' }}</strong>
        </div>
      </div>
    </section>

    <section class="settings-layout">
      <aside class="settings-nav">
        <button
          v-for="item in navItems"
          :key="item.key"
          type="button"
          :class="{ active: activeTab === item.key }"
          @click="switchTab(item.key)"
        >
          <el-icon><component :is="item.icon" /></el-icon>
          <span>{{ item.title }}</span>
          <small>{{ item.desc }}</small>
        </button>
      </aside>

      <main class="settings-panel" v-loading="loading">
        <div v-if="activeTab === 'site-access'" class="control-card">
          <div class="card-head">
            <div>
              <h2>一键开启 / 关闭网站</h2>
              <span>关闭后普通前台页面会展示维护提示，后台仍可登录并恢复网站。</span>
            </div>
            <el-switch
              v-model="form.siteEnabled"
              size="large"
              inline-prompt
              active-text="开启"
              inactive-text="关闭"
            />
          </div>

          <div class="preview-box" :class="{ danger: !form.siteEnabled }">
            <strong>{{ form.siteEnabled ? '当前用户可正常访问官网' : form.closedTitle }}</strong>
            <span>{{ form.siteEnabled ? '前台页面、登录注册、下单流程保持正常开放。' : form.closedMessage }}</span>
          </div>

          <el-form label-position="top" class="copy-form">
            <el-form-item label="关闭提示标题">
              <el-input v-model="form.closedTitle" maxlength="40" show-word-limit />
            </el-form-item>
            <el-form-item label="关闭提示说明">
              <el-input v-model="form.closedMessage" type="textarea" :rows="4" maxlength="160" show-word-limit />
            </el-form-item>
            <el-form-item label="关闭提示联系文案">
              <el-input v-model="form.closedContact" maxlength="120" show-word-limit />
            </el-form-item>
          </el-form>
        </div>

        <div v-else-if="activeTab === 'public-interaction'" class="control-card interaction-module" :class="{ disabled: !form.publicInteractionEnabled }">
          <div class="interaction-main">
            <div class="interaction-icon">
              <el-icon><ChatDotRound /></el-icon>
            </div>
            <div>
              <h2>互动内容开关</h2>
              <span>控制前台发帖、评论、评价入口，关闭后接口同步拒绝提交。</span>
            </div>
          </div>
          <div class="interaction-side">
            <el-switch
              v-model="form.publicInteractionEnabled"
              size="large"
              inline-prompt
              active-text="开放"
              inactive-text="关闭"
            />
            <small>{{ form.publicInteractionEnabled ? '当前开放' : '当前关闭' }}</small>
          </div>
        </div>

        <div v-else class="control-card">
          <div class="card-head">
            <div>
              <h2>一键拒绝移动端访问</h2>
              <span>开启后手机、平板等移动端访问前台时会看到桌面访问提示，电脑端不受影响。</span>
            </div>
            <el-switch
              v-model="form.rejectMobile"
              size="large"
              inline-prompt
              active-text="拒绝"
              inactive-text="允许"
            />
          </div>

          <div class="preview-box" :class="{ warning: form.rejectMobile }">
            <strong>{{ form.rejectMobile ? form.mobileTitle : '当前移动端允许访问' }}</strong>
            <span>{{ form.rejectMobile ? form.mobileMessage : '用户可通过电脑、手机和平板访问前台页面。' }}</span>
          </div>

          <el-form label-position="top" class="copy-form">
            <el-form-item label="移动端提示标题">
              <el-input v-model="form.mobileTitle" maxlength="40" show-word-limit />
            </el-form-item>
            <el-form-item label="移动端提示说明">
              <el-input v-model="form.mobileMessage" type="textarea" :rows="4" maxlength="180" show-word-limit />
            </el-form-item>
            <el-form-item label="移动端提示联系文案">
              <el-input v-model="form.mobileContact" maxlength="120" show-word-limit />
            </el-form-item>
          </el-form>
        </div>

        <div v-if="activeTab !== 'public-interaction'" class="support-config">
          <div class="support-head">
            <h2>客服入口设置</h2>
            <span>用于网站关闭和移动端拒绝页面。可填写企业微信、微信客服、企微活码、二维码图片或客服凭证。</span>
          </div>
          <el-form label-position="top" class="copy-form">
            <el-form-item label="按钮文字">
              <el-input v-model="form.supportButtonText" maxlength="30" show-word-limit />
            </el-form-item>
            <el-form-item label="客服跳转链接">
              <el-input v-model="form.supportUrl" maxlength="300" show-word-limit />
            </el-form-item>
            <el-form-item label="客服凭证">
              <el-input v-model="form.supportCredential" maxlength="120" show-word-limit />
            </el-form-item>
            <el-form-item label="客服二维码图片地址">
              <el-input v-model="form.supportQrImageUrl" maxlength="300" show-word-limit />
            </el-form-item>
          </el-form>
        </div>

        <div class="save-bar">
          <div>
            <strong>生产提示</strong>
            <span>保存后配置会立即对前台生效，请确认提示文案准确、客服渠道可用。</span>
          </div>
          <el-button type="primary" size="large" :loading="saving" @click="saveConfig">保存设置</el-button>
        </div>
      </main>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ChatDotRound, Monitor, SwitchButton } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { getSiteAccessConfig, saveSiteAccessConfig } from '@/api/siteAccess'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const saving = ref(false)

const form = reactive({
  siteEnabled: true,
  rejectMobile: false,
  publicInteractionEnabled: true,
  closedTitle: '',
  closedMessage: '',
  closedContact: '',
  mobileTitle: '',
  mobileMessage: '',
  mobileContact: '',
  supportButtonText: '',
  supportUrl: '',
  supportCredential: '',
  supportQrImageUrl: ''
})

const navItems = [
  { key: 'site-access', title: '网站开关', desc: '开启或关闭官网前台', icon: SwitchButton },
  { key: 'device-access', title: '访问终端', desc: '控制移动端访问策略', icon: Monitor },
  { key: 'public-interaction', title: '互动内容', desc: '关闭用户发帖评论', icon: ChatDotRound }
]

const activeTab = computed(() => {
  if (route.path.includes('device-access')) return 'device-access'
  if (route.path.includes('public-interaction')) return 'public-interaction'
  return 'site-access'
})

const switchTab = (key) => {
  const pathMap = {
    'site-access': '/back/site-settings/site-access',
    'device-access': '/back/site-settings/device-access',
    'public-interaction': '/back/site-settings/public-interaction'
  }
  router.push(pathMap[key] || pathMap['site-access'])
}

const loadConfig = async () => {
  loading.value = true
  try {
    const data = await getSiteAccessConfig()
    Object.assign(form, data || {})
  } finally {
    loading.value = false
  }
}

const saveConfig = async () => {
  saving.value = true
  try {
    await saveSiteAccessConfig({ ...form }, {
      successMsg: '网站设置已保存'
    })
  } finally {
    saving.value = false
  }
}

watch(() => form.siteEnabled, (enabled) => {
  if (!enabled) {
    ElMessage.warning('网站关闭后，前台用户会看到维护提示，后台不受影响')
  }
})

onMounted(loadConfig)
</script>

<style lang="scss" scoped>
.website-settings {
  min-height: calc(100vh - 120px);
  padding: 24px;
  background: #f5f7fb;
  color: #0f172a;
}

.settings-hero,
.settings-panel,
.settings-nav {
  border: 1px solid #e5e7eb;
  border-radius: 20px;
  background: #fff;
  box-shadow: 0 18px 45px rgba(15, 23, 42, 0.06);
}

.settings-hero {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 24px;
  padding: 28px 30px;
  margin-bottom: 18px;

  h1 {
    margin: 0;
    font-size: 30px;
    font-weight: 900;
  }

  span {
    display: inline-block;
    margin-top: 10px;
    color: #64748b;
  }
}

.hero-copy {
  flex: 1;
  min-width: 0;
}

.status-group {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 10px;
  flex: 0 0 auto;
  max-width: 420px;
}

.site-status-pill {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  width: max-content;
  padding: 7px 11px;
  line-height: 1;
  border: 1px solid rgba(22, 163, 74, 0.2);
  border-radius: 999px;
  background: linear-gradient(135deg, #ecfdf5 0%, #f7fee7 100%);
  box-shadow: 0 14px 34px rgba(22, 163, 74, 0.12);

  .status-dot {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 16px;
    height: 16px;
    flex: 0 0 16px;
    border-radius: 50%;
    background: transparent;
  }

  .status-dot::after {
    content: '';
    width: 8px;
    height: 8px;
    border-radius: 50%;
    background: #16a34a;
    box-shadow: 0 0 0 4px rgba(22, 163, 74, 0.12);
    transform: translateY(-5px);
  }

  strong {
    display: inline-flex;
    align-items: center;
    height: 16px;
    color: #14532d;
    font-size: 13px;
    font-weight: 900;
    line-height: 16px;
    white-space: nowrap;
  }
}

.site-status-pill.closed {
  border-color: rgba(225, 29, 72, 0.2);
  background: linear-gradient(135deg, #fff1f2 0%, #fff7ed 100%);
  box-shadow: 0 14px 34px rgba(225, 29, 72, 0.12);

  .status-dot {
    background: transparent;
  }

  .status-dot::after {
    background: #e11d48;
    box-shadow: 0 0 0 4px rgba(225, 29, 72, 0.12);
  }

  strong {
    color: #9f1239;
  }

  small {
    color: #b45309;
  }
}

.settings-layout {
  display: grid;
  grid-template-columns: 280px minmax(0, 1fr);
  gap: 18px;
}

.settings-nav {
  align-self: start;
  padding: 12px;
}

.settings-nav button {
  width: 100%;
  min-height: 86px;
  display: grid;
  grid-template-columns: 38px 1fr;
  grid-template-rows: auto auto;
  column-gap: 12px;
  align-items: center;
  padding: 16px;
  border: 0;
  border-radius: 16px;
  background: transparent;
  color: #334155;
  text-align: left;
  cursor: pointer;
}

.settings-nav button + button {
  margin-top: 8px;
}

.settings-nav button.active {
  background: #eef6ff;
  color: #1d4ed8;
}

.settings-nav .el-icon {
  grid-row: 1 / 3;
  width: 38px;
  height: 38px;
  border-radius: 12px;
  background: #f1f5f9;
  font-size: 20px;
}

.settings-nav button.active .el-icon {
  background: #dbeafe;
}

.settings-nav span {
  font-weight: 800;
}

.settings-nav small {
  color: #64748b;
}

.settings-panel {
  padding: 22px;
}

.control-card {
  border: 1px solid #e5e7eb;
  border-radius: 18px;
  padding: 24px;
  background: linear-gradient(180deg, #ffffff 0%, #f8fafc 100%);
}

.interaction-module {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 24px;
  min-height: 126px;
  padding: 22px 24px;
  border: 1px solid #bfdbfe;
  border-radius: 18px;
  background:
    linear-gradient(135deg, rgba(239, 246, 255, 0.95) 0%, rgba(255, 255, 255, 0.98) 55%, rgba(236, 253, 245, 0.85) 100%);
  box-shadow: 0 16px 36px rgba(37, 99, 235, 0.08);
}

.interaction-module.disabled {
  border-color: #fed7aa;
  background:
    linear-gradient(135deg, #fff7ed 0%, #ffffff 54%, #fff1f2 100%);
  box-shadow: 0 16px 36px rgba(234, 88, 12, 0.08);
}

.interaction-main {
  display: flex;
  align-items: center;
  gap: 16px;
  min-width: 0;

  h2 {
    margin: 0;
    font-size: 22px;
    font-weight: 900;
  }

  span {
    display: inline-block;
    max-width: 720px;
    margin-top: 8px;
    color: #64748b;
    line-height: 1.7;
  }
}

.interaction-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 46px;
  height: 46px;
  flex: 0 0 auto;
  border-radius: 15px;
  color: #1d4ed8;
  background: #dbeafe;
  font-size: 22px;
}

.interaction-module.disabled .interaction-icon {
  color: #c2410c;
  background: #ffedd5;
}

.interaction-side {
  display: grid;
  justify-items: end;
  gap: 10px;
  flex: 0 0 132px;

  small {
    color: #64748b;
    font-weight: 700;
    white-space: nowrap;
  }
}

.card-head {
  display: flex;
  justify-content: space-between;
  gap: 24px;
  align-items: flex-start;

  p {
    margin: 0 0 8px;
    color: #2563eb;
    font-size: 12px;
    font-weight: 800;
    text-transform: uppercase;
  }

  h2 {
    margin: 0;
    font-size: 24px;
    font-weight: 900;
  }

  span {
    display: inline-block;
    max-width: 680px;
    margin-top: 10px;
    color: #64748b;
    line-height: 1.7;
  }
}

.preview-box {
  display: grid;
  gap: 8px;
  margin: 24px 0;
  padding: 20px;
  border-radius: 16px;
  border: 1px solid #bfdbfe;
  background: #eff6ff;

  strong {
    color: #1e3a8a;
    font-size: 18px;
  }

  span {
    color: #475569;
    line-height: 1.7;
  }
}

.preview-box.danger {
  border-color: #fecaca;
  background: #fff1f2;

  strong {
    color: #9f1239;
  }
}

.preview-box.warning {
  border-color: #fde68a;
  background: #fffbeb;

  strong {
    color: #92400e;
  }
}

.copy-form {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;

  :deep(.el-form-item:last-child) {
    grid-column: 1 / -1;
  }
}

.save-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 18px;
  margin-top: 18px;
  padding: 18px 20px;
  border-radius: 18px;
  background: #0f172a;
  color: #fff;

  div {
    display: grid;
    gap: 4px;
  }

  span {
    color: #cbd5e1;
  }
}

.support-config {
  margin-top: 18px;
  padding: 24px;
  border: 1px solid #e5e7eb;
  border-radius: 18px;
  background: #ffffff;
}

.support-head {
  margin-bottom: 18px;

  p {
    margin: 0 0 8px;
    color: #0f766e;
    font-size: 12px;
    font-weight: 800;
    text-transform: uppercase;
  }

  h2 {
    margin: 0;
    font-size: 22px;
    font-weight: 900;
  }

  span {
    display: inline-block;
    margin-top: 8px;
    color: #64748b;
    line-height: 1.7;
  }
}

@media (max-width: 1080px) {
  .settings-hero {
    align-items: flex-start;
    flex-direction: column;
  }

  .status-group {
    justify-content: flex-start;
    max-width: 100%;
  }

  .settings-layout {
    grid-template-columns: 1fr;
  }

  .copy-form {
    grid-template-columns: 1fr;
  }

  .interaction-module {
    align-items: stretch;
    flex-direction: column;
  }

  .interaction-side {
    width: 100%;
    display: flex;
    justify-content: space-between;
    align-items: center;
  }
}
</style>
