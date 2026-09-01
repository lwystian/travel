<template>
  <div class="website-settings">
    <section class="settings-hero">
      <div class="hero-copy">
        <h1>网站设置</h1>
        <span>集中管理官网访问、互动能力与行程商品来源。</span>
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
        <div class="site-status-pill source-pill" :class="{ remote: useMiniappProducts }">
          <span class="status-dot"></span>
          <strong>{{ useMiniappProducts ? '小程序商品' : '本地商品' }}</strong>
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

        <div v-else-if="activeTab === 'tour-source'" class="control-card source-module">
          <div class="card-head source-mode-head">
            <div>
              <h2>官网行程商品来源</h2>
              <span>切换后只影响官网前台展示；原有本地商品、后台上架功能和历史订单都会保留。</span>
            </div>
            <el-switch
              v-model="useMiniappProducts"
              size="large"
              inline-prompt
              active-text="小程序"
              inactive-text="本地"
              style="--el-switch-on-color: #0f766e; --el-switch-off-color: #64748b"
            />
          </div>

          <div class="source-flow" :class="{ active: useMiniappProducts }">
            <div>
              <strong>商品展示</strong>
              <span>{{ useMiniappProducts ? '小程序上架后，官网通过同源后端接口自动读取并转换字段。' : '官网继续读取 travel 原有商品表。' }}</span>
            </div>
            <div>
              <strong>预订与库存</strong>
              <span>{{ useMiniappProducts ? '商品、套餐、班期和库存读取小程序 API，下单与支付继续使用官网流程。' : '本地商品继续使用官网原有预订和支付流程。' }}</span>
            </div>
          </div>

          <el-form label-position="top" class="copy-form source-form">
            <el-form-item label="小程序 API 地址">
              <el-input v-model="sourceForm.miniappApiBaseUrl" placeholder="例如：https://mini.example.com/api" maxlength="500" />
              <span class="field-help">填写小程序后端的完整 API 根地址，travel 后端会从服务器侧访问该地址。</span>
            </el-form-item>
            <el-form-item label="接口异常保护">
              <el-switch
                v-model="sourceForm.fallbackToLocal"
                inline-prompt
                active-text="回退"
                inactive-text="报错"
              />
              <span class="field-help">开启后，小程序商品列表暂时不可用时自动显示原有本地商品；已打开的小程序商品详情不会串到本地商品。</span>
            </el-form-item>
          </el-form>

          <div class="connection-check">
            <div>
              <strong>连接检查</strong>
              <span>测试 API 地址是否可达，并读取当前已上架商品数量。</span>
            </div>
            <el-button :loading="checkingSource" @click="testTourSource">
              <el-icon><Connection /></el-icon>
              测试连接
            </el-button>
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

        <div v-if="activeTab === 'site-access' || activeTab === 'device-access'" class="support-config">
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
            <span>{{ saveHint }}</span>
          </div>
          <el-button type="primary" size="large" :loading="saving" @click="saveCurrentConfig">保存设置</el-button>
        </div>
      </main>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ChatDotRound, Connection, Goods, Monitor, SwitchButton } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { getSiteAccessConfig, saveSiteAccessConfig } from '@/api/siteAccess'
import { checkMiniappTourSource, getTourSourceConfig, saveTourSourceConfig } from '@/api/tourSource'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const saving = ref(false)
const checkingSource = ref(false)

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

const sourceForm = reactive({
  sourceMode: 'LOCAL',
  miniappApiBaseUrl: '',
  miniappBookingUrlTemplate: '',
  fallbackToLocal: true
})

const useMiniappProducts = computed({
  get: () => sourceForm.sourceMode === 'MINIAPP',
  set: enabled => {
    sourceForm.sourceMode = enabled ? 'MINIAPP' : 'LOCAL'
  }
})

const navItems = [
  { key: 'site-access', title: '网站开关', desc: '开启或关闭官网前台', icon: SwitchButton },
  { key: 'device-access', title: '访问终端', desc: '控制移动端访问策略', icon: Monitor },
  { key: 'public-interaction', title: '互动内容', desc: '关闭用户发帖评论', icon: ChatDotRound },
  { key: 'tour-source', title: '商品来源', desc: '切换本地或小程序商品', icon: Goods }
]

const activeTab = computed(() => {
  if (route.path.includes('device-access')) return 'device-access'
  if (route.path.includes('public-interaction')) return 'public-interaction'
  if (route.path.includes('tour-source')) return 'tour-source'
  return 'site-access'
})

const saveHint = computed(() => activeTab.value === 'tour-source'
  ? '切换为小程序商品前请先测试连接；保存后商品与库存立即切换，官网订单和支付流程保持不变。'
  : '保存后配置会立即对前台生效，请确认提示文案准确、客服渠道可用。')

const switchTab = (key) => {
  const pathMap = {
    'site-access': '/back/site-settings/site-access',
    'device-access': '/back/site-settings/device-access',
    'public-interaction': '/back/site-settings/public-interaction',
    'tour-source': '/back/site-settings/tour-source'
  }
  router.push(pathMap[key] || pathMap['site-access'])
}

const loadConfig = async () => {
  loading.value = true
  try {
    const [siteData, sourceData] = await Promise.all([
      getSiteAccessConfig(),
      getTourSourceConfig()
    ])
    Object.assign(form, siteData || {})
    Object.assign(sourceForm, sourceData || {})
  } finally {
    loading.value = false
  }
}

const saveCurrentConfig = async () => {
  saving.value = true
  try {
    if (activeTab.value === 'tour-source') {
      const data = await saveTourSourceConfig({ ...sourceForm }, {
        successMsg: '商品来源设置已保存'
      })
      Object.assign(sourceForm, data || {})
    } else {
      await saveSiteAccessConfig({ ...form }, {
        successMsg: '网站设置已保存'
      })
    }
  } finally {
    saving.value = false
  }
}

const testTourSource = async () => {
  checkingSource.value = true
  try {
    const result = await checkMiniappTourSource({ ...sourceForm })
    const count = Number(result?.productCount || 0)
    const sample = result?.sampleTitle ? `，示例：${result.sampleTitle}` : ''
    ElMessage.success(`连接成功，读取到 ${count} 个已上架商品${sample}`)
  } catch {
    ElMessage.error('连接失败，请检查 API 地址、小程序后端状态和网络访问策略')
  } finally {
    checkingSource.value = false
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

.site-status-pill.source-pill {
  border-color: rgba(71, 85, 105, 0.22);
  background: #f8fafc;
  box-shadow: none;

  .status-dot {
    background: #64748b;
  }
}

.site-status-pill.source-pill.remote {
  border-color: rgba(15, 118, 110, 0.24);
  background: #f0fdfa;

  .status-dot {
    background: #0f766e;
  }
}

.source-module {
  display: grid;
  gap: 22px;
}

.source-flow {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  border: 1px solid #e2e8f0;
  background: #f8fafc;

  div {
    display: grid;
    gap: 7px;
    padding: 18px 20px;
  }

  div + div {
    border-left: 1px solid #e2e8f0;
  }

  strong {
    color: #334155;
  }

  span {
    color: #64748b;
    line-height: 1.65;
  }
}

.source-flow.active {
  border-color: #99f6e4;
  background: #f0fdfa;

  strong {
    color: #115e59;
  }
}

.source-form {
  padding-top: 4px;
}

.field-help {
  display: block;
  width: 100%;
  margin-top: 7px;
  color: #64748b;
  font-size: 12px;
  line-height: 1.6;
}

.connection-check {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 20px;
  padding: 16px 18px;
  border: 1px solid #dbeafe;
  background: #eff6ff;

  div {
    display: grid;
    gap: 5px;
  }

  strong {
    color: #1e3a8a;
  }

  span {
    color: #475569;
    line-height: 1.5;
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

  .source-flow {
    grid-template-columns: 1fr;

    div + div {
      border-top: 1px solid #e2e8f0;
      border-left: 0;
    }
  }

  .connection-check {
    align-items: stretch;
    flex-direction: column;
  }
}
</style>
