<template>
  <div class="about-page">
    <div v-if="userStore.isAdmin" class="about-admin-bar">
      <div>
        <strong>页面编辑模式</strong>
        <span>可直接维护当前页面的展示文案</span>
      </div>
      <div class="about-admin-actions">
        <el-button v-if="!editMode" type="primary" @click="startEdit">开启编辑</el-button>
        <template v-else>
          <el-button type="primary" :loading="savingContent" @click="saveAboutContent">保存</el-button>
          <el-button :disabled="savingContent" @click="cancelEdit">取消</el-button>
        </template>
      </div>
    </div>
    <section class="about-hero">
      <img class="hero-image" :src="aboutHeroUrl" alt="旅行目的地海岸与旅店" />
      <div class="hero-shade"></div>
      <div class="hero-content">
        <inline-editable-text v-model="pageContent.hero.kicker" :edit-mode="editMode" tag="p" class="hero-kicker" maxlength="60" />
        <inline-editable-text v-model="pageContent.hero.title" :edit-mode="editMode" tag="h1" maxlength="90" />
        <inline-editable-text v-model="pageContent.hero.lead" :edit-mode="editMode" tag="p" class="hero-lead" multiline :rows="3" maxlength="360" />
        <p v-if="false" class="hero-lead">
          立足山城重庆，面向家庭出游、周边短线、三峡邮轮、目的地度假与团队定制，为旅客提供更清晰、更安心的旅行服务。
        </p>
        <div class="hero-actions">
          <a :href="pageContent.hero.primaryLinkUrl" class="primary-action" @click="handleConfiguredLinkClick(pageContent.hero.primaryLinkUrl, $event)">
            <inline-editable-text v-model="pageContent.hero.primaryLinkText" :edit-mode="editMode" tag="span" maxlength="30" />
            <el-icon><ArrowRight /></el-icon>
          </a>
          <a :href="pageContent.hero.secondaryLinkUrl" class="secondary-action" @click="handleConfiguredLinkClick(pageContent.hero.secondaryLinkUrl, $event)">
            <inline-editable-text v-model="pageContent.hero.secondaryLinkText" :edit-mode="editMode" tag="span" maxlength="30" />
          </a>
        </div>
        <div v-if="editMode" class="link-edit-grid">
          <el-input v-model="pageContent.hero.primaryLinkUrl" size="small" placeholder="主按钮链接，如 /tickets" />
          <el-input v-model="pageContent.hero.secondaryLinkUrl" size="small" placeholder="次按钮链接，如 /guide" />
        </div>
      </div>
      <div class="license-panel">
        <inline-editable-text v-model="pageContent.license.label" :edit-mode="editMode" tag="span" class="panel-label" maxlength="60" />
        <inline-editable-text v-model="pageContent.license.number" :edit-mode="editMode" tag="strong" maxlength="50" />
        <inline-editable-text v-model="pageContent.license.desc" :edit-mode="editMode" tag="span" maxlength="120" />
        <template v-if="false">
        <span class="panel-label">旅行社业务经营许可证</span>
        <inline-editable-text v-model="pageContent.license.number" :edit-mode="editMode" tag="strong" maxlength="50" />
        <span>境内旅游业务、入境旅游业务</span>
        </template>
      </div>
    </section>

    <section class="fact-band">
      <div class="fact-item" v-for="item in facts" :key="item.label">
        <el-icon><component :is="item.icon" /></el-icon>
        <inline-editable-text v-model="item.label" :edit-mode="editMode" tag="span" maxlength="40" />
        <inline-editable-text v-model="item.value" :edit-mode="editMode" tag="strong" maxlength="60" />
      </div>
    </section>

    <section class="intro-section">
      <div class="intro-copy">
        <inline-editable-text v-model="pageContent.intro.kicker" :edit-mode="editMode" tag="p" class="section-kicker" maxlength="60" />
        <inline-editable-text v-model="pageContent.intro.title" :edit-mode="editMode" tag="h2" maxlength="80" />
        <inline-editable-text v-model="pageContent.intro.paragraph1" :edit-mode="editMode" tag="p" multiline :rows="4" maxlength="520" />
        <inline-editable-text v-model="pageContent.intro.paragraph2" :edit-mode="editMode" tag="p" multiline :rows="4" maxlength="520" />
        <template v-if="false">
        <p>
          侠客行国旅面向重庆本地及来渝游客，围绕热门景区、城市周边、长江三峡、亲子休闲、团队出行等场景，提供线路展示、行程预订、攻略参考与出行服务衔接。
        </p>
        <p>
          公开名录显示，公司登记在重庆市九龙坡区，许可经营范围包含境内旅游业务与入境旅游业务。页面展示的信息以政府公开名录为准，平台内容侧重帮助用户快速了解服务方向与出行体验。
        </p>
        </template>
      </div>
      <div class="identity-board">
        <div class="identity-row">
          <inline-editable-text v-model="pageContent.identity.companyLabel" :edit-mode="editMode" tag="span" maxlength="40" />
          <template v-if="false"><span>公司名称</span></template>
          <inline-editable-text v-model="pageContent.identity.companyName" :edit-mode="editMode" tag="strong" maxlength="90" />
        </div>
        <div class="identity-row">
          <inline-editable-text v-model="pageContent.identity.licenseLabel" :edit-mode="editMode" tag="span" maxlength="40" />
          <template v-if="false"><span>许可证号</span></template>
          <inline-editable-text v-model="pageContent.identity.licenseNumber" :edit-mode="editMode" tag="strong" maxlength="50" />
        </div>
        <div class="identity-row">
          <inline-editable-text v-model="pageContent.identity.regionLabel" :edit-mode="editMode" tag="span" maxlength="40" />
          <template v-if="false"><span>所在区域</span></template>
          <inline-editable-text v-model="pageContent.identity.region" :edit-mode="editMode" tag="strong" maxlength="80" />
        </div>
        <div class="identity-row">
          <inline-editable-text v-model="pageContent.identity.scopeLabel" :edit-mode="editMode" tag="span" maxlength="40" />
          <template v-if="false"><span>业务范围</span></template>
          <inline-editable-text v-model="pageContent.identity.scope" :edit-mode="editMode" tag="strong" maxlength="120" />
        </div>
      </div>
    </section>

    <section class="service-section">
      <div class="section-heading">
        <inline-editable-text v-model="pageContent.serviceHeading.kicker" :edit-mode="editMode" tag="p" class="section-kicker" maxlength="60" />
        <inline-editable-text v-model="pageContent.serviceHeading.title" :edit-mode="editMode" tag="h2" maxlength="90" />
      </div>
      <div class="service-grid">
        <article class="service-card" v-for="service in services" :key="service.title">
          <div class="service-icon">
            <el-icon><component :is="service.icon" /></el-icon>
          </div>
          <inline-editable-text v-model="service.title" :edit-mode="editMode" tag="h3" maxlength="50" />
          <inline-editable-text v-model="service.desc" :edit-mode="editMode" tag="p" multiline :rows="3" maxlength="220" />
        </article>
      </div>
    </section>

    <section class="route-section">
      <div class="route-copy">
        <inline-editable-text v-model="pageContent.route.kicker" :edit-mode="editMode" tag="p" class="section-kicker" maxlength="60" />
        <inline-editable-text v-model="pageContent.route.title" :edit-mode="editMode" tag="h2" maxlength="90" />
        <inline-editable-text v-model="pageContent.route.desc" :edit-mode="editMode" tag="p" multiline :rows="4" maxlength="520" />
        <p v-if="false">
          重庆兼具城市观光、山水峡江、温泉休闲、红色文化与西南交通枢纽优势。侠客行国旅的产品表达可以围绕“短线高频、长线精选、邮轮慢游、团队定制”展开，让游客在出发前就看清行程节奏与服务边界。
        </p>
      </div>
      <div class="route-list">
        <div class="route-item" v-for="route in routes" :key="route.name">
          <inline-editable-text v-model="route.tag" :edit-mode="editMode" tag="span" maxlength="30" />
          <inline-editable-text v-model="route.name" :edit-mode="editMode" tag="strong" maxlength="60" />
          <inline-editable-text v-model="route.text" :edit-mode="editMode" tag="p" multiline :rows="3" maxlength="220" />
        </div>
      </div>
    </section>

    <section class="process-section">
      <div class="section-heading">
        <inline-editable-text v-model="pageContent.processHeading.kicker" :edit-mode="editMode" tag="p" class="section-kicker" maxlength="60" />
        <inline-editable-text v-model="pageContent.processHeading.title" :edit-mode="editMode" tag="h2" maxlength="90" />
      </div>
      <div class="process-rail">
        <div class="process-step" v-for="(step, index) in process" :key="step.title">
          <span class="step-number">{{ String(index + 1).padStart(2, '0') }}</span>
          <inline-editable-text v-model="step.title" :edit-mode="editMode" tag="h3" maxlength="50" />
          <inline-editable-text v-model="step.text" :edit-mode="editMode" tag="p" multiline :rows="3" maxlength="220" />
        </div>
      </div>
    </section>

    <section class="source-section">
      <div>
        <inline-editable-text v-model="pageContent.source.kicker" :edit-mode="editMode" tag="p" class="section-kicker" maxlength="60" />
        <inline-editable-text v-model="pageContent.source.title" :edit-mode="editMode" tag="h2" maxlength="90" />
        <inline-editable-text v-model="pageContent.source.desc" :edit-mode="editMode" tag="p" multiline :rows="3" maxlength="320" />
        <p v-if="false">
          页面中的许可证号、地址区域与业务范围，依据重庆市九龙坡区人民政府公开的公共文旅机构名录整理。
        </p>
      </div>
      <a :href="pageContent.source.linkUrl" target="_blank" rel="noopener noreferrer" class="source-link" @click="handleConfiguredLinkClick(pageContent.source.linkUrl, $event)">
        <inline-editable-text v-model="pageContent.source.linkText" :edit-mode="editMode" tag="span" maxlength="40" />
        <el-icon><ArrowRight /></el-icon>
      </a>
      <el-input v-if="editMode" v-model="pageContent.source.linkUrl" class="source-link-input" size="small" placeholder="政府公开名录链接" />
    </section>
  </div>
</template>

<script setup>
import {
  ArrowRight,
  Location,
  Tickets,
  Ship,
  Service,
  Guide,
  Flag,
  Van,
  Medal
} from '@element-plus/icons-vue'
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useSiteAssets, getAssetUrl } from '@/utils/siteAssets'
import noImage from '@/assets/images/no-image.png'
import InlineEditableText from '@/components/frontend/InlineEditableText.vue'
import { useUserStore } from '@/store/user'
import { getPublicPageContent, savePageContent } from '@/api/pageContent'

const { siteAssets, loadSiteAssets } = useSiteAssets()
const userStore = useUserStore()
const router = useRouter()
const aboutHeroUrl = computed(() => getAssetUrl(siteAssets.value.aboutHeroUrl, noImage))
const editMode = ref(false)
const savingContent = ref(false)
const editingSnapshot = ref(null)

const defaultPageContent = {
  hero: {
    kicker: 'ABOUT XIAKEXING TRAVEL',
    title: '重庆侠客行国际旅行社有限公司',
    lead: '立足山城重庆，面向家庭出游、周边短线、三峡邮轮、目的地度假与团队定制，为旅客提供更清晰、更安心的旅行服务。',
    primaryLinkText: '查看行程',
    primaryLinkUrl: '/tickets',
    secondaryLinkText: '阅读攻略',
    secondaryLinkUrl: '/guide'
  },
  license: {
    label: '旅行社业务经营许可证',
    number: 'L-CQ-100181',
    desc: '境内旅游业务、入境旅游业务'
  },
  intro: {
    kicker: '山城出发，走向更远',
    title: '把复杂的行程，变成可靠的安排',
    paragraph1: '侠客行国旅面向重庆本地及来渝游客，围绕热门景区、城市周边、长江三峡、亲子休闲、团队出行等场景，提供线路展示、行程预订、攻略参考与出行服务衔接。',
    paragraph2: '公开名录显示，公司登记在重庆市九龙坡区，许可经营范围包含境内旅游业务与入境旅游业务。页面展示的信息以政府公开名录为准，平台内容侧重帮助用户快速了解服务方向与出行体验。'
  },
  identity: {
    companyLabel: '公司名称',
    companyName: '重庆侠客行国际旅行社有限公司',
    licenseLabel: '许可证号',
    licenseNumber: 'L-CQ-100181',
    regionLabel: '所在区域',
    region: '重庆市九龙坡区',
    scopeLabel: '业务范围',
    scope: '境内旅游业务、入境旅游业务'
  },
  serviceHeading: {
    kicker: 'SERVICE MATRIX',
    title: '围绕真实出行场景组织服务'
  },
  route: {
    kicker: 'TRAVEL THEMES',
    title: '从重庆出发的多元旅行主题',
    desc: '重庆兼具城市观光、山水峡江、温泉休闲、红色文化与西南交通枢纽优势。侠客行国旅的产品表达可以围绕“短线高频、长线精选、邮轮慢游、团队定制”展开，让游客在出发前就看清行程节奏与服务边界。'
  },
  processHeading: {
    kicker: 'HOW WE WORK',
    title: '把出行服务拆成清楚的四步'
  },
  source: {
    kicker: 'PUBLIC RECORD',
    title: '公开信息来源',
    desc: '页面中的许可证号、地址区域与业务范围，依据重庆市九龙坡区人民政府公开的公共文旅机构名录整理。',
    linkText: '查看政府公开名录',
    linkUrl: 'https://cqjlp.gov.cn/bmjz/qzfbm_97119/qwhlyw_97727/zwgk_97124/gkml/jczwgk/ggwhfwly/ggfw/ggwljgml/202601/t20260116_15326517.html'
  }
}

const cloneData = (data) => JSON.parse(JSON.stringify(data))
const pageContent = ref(cloneData(defaultPageContent))

const facts = reactive([
  { label: '许可资质', value: 'L-CQ-100181', icon: Medal },
  { label: '服务范围', value: '境内 / 入境', icon: Flag },
  { label: '城市据点', value: '重庆九龙坡', icon: Location },
  { label: '平台服务', value: '线路 / 攻略 / 预订', icon: Tickets }
])

const services = reactive([
  {
    title: '周边短线',
    icon: Van,
    desc: '面向重庆及周边高频出游需求，适合一日游、二日游和家庭轻度假。'
  },
  {
    title: '三峡与邮轮',
    icon: Ship,
    desc: '围绕长江三峡、水上观光和慢节奏休闲，提供更清楚的行程节奏说明。'
  },
  {
    title: '团队定制',
    icon: Service,
    desc: '承接企业团建、亲友小团、研学和主题出行，强调预算、时间和体验匹配。'
  },
  {
    title: '攻略内容',
    icon: Guide,
    desc: '通过目的地攻略、用户游记和路线建议，帮助游客做出更稳妥的选择。'
  }
])

const routes = reactive([
  {
    tag: 'CHONGQING',
    name: '山城城市游',
    text: '串联两江夜景、城市地标、美食街区与历史文化空间。'
  },
  {
    tag: 'YANGTZE',
    name: '长江三峡游',
    text: '适合慢旅行、家庭休闲和跨城联动行程，强调船期与岸上体验。'
  },
  {
    tag: 'WEEKEND',
    name: '周末微度假',
    text: '以温泉、古镇、山地避暑和亲子体验为核心，降低决策成本。'
  }
])

const process = reactive([
  { title: '需求确认', text: '明确人数、预算、出发日期、偏好和特殊需求。' },
  { title: '线路匹配', text: '根据天数和目的地，匹配平台线路或制定团队方案。' },
  { title: '行前说明', text: '确认费用包含、集合信息、证件要求和注意事项。' },
  { title: '服务跟进', text: '围绕订单、通知、攻略和售后反馈形成闭环。' }
])

const factIcons = [Medal, Flag, Location, Tickets]
const serviceIcons = [Van, Ship, Service, Guide]

const replaceList = (target, source, mapper = item => item) => {
  if (!Array.isArray(source)) return
  target.splice(0, target.length, ...source.map((item, index) => mapper(item || {}, index)))
}

const applyAboutContent = (content = {}) => {
  pageContent.value = {
    ...cloneData(defaultPageContent),
    ...(content.pageContent || {})
  }
  replaceList(facts, content.facts, (item, index) => ({
    label: item.label || '',
    value: item.value || '',
    icon: factIcons[index] || Medal
  }))
  replaceList(services, content.services, (item, index) => ({
    title: item.title || '',
    desc: item.desc || '',
    icon: serviceIcons[index] || Service
  }))
  replaceList(routes, content.routes)
  replaceList(process, content.process)
}

const serializeAboutContent = () => ({
  pageContent: pageContent.value,
  facts: facts.map(({ label, value }) => ({ label, value })),
  services: services.map(({ title, desc }) => ({ title, desc })),
  routes,
  process
})

const handleConfiguredLinkClick = (url, event) => {
  event?.preventDefault?.()
  if (editMode.value) return
  if (!url) return
  if (/^(https?:)?\/\//.test(url)) {
    window.open(url, '_blank', 'noopener,noreferrer')
    return
  }
  router.push(url)
}

const loadAboutContent = async () => {
  try {
    const data = await getPublicPageContent('about')
    applyAboutContent(data || {})
  } catch {
    applyAboutContent({})
  }
}

const startEdit = () => {
  editingSnapshot.value = cloneData(serializeAboutContent())
  editMode.value = true
}

const cancelEdit = () => {
  if (editingSnapshot.value) {
    applyAboutContent(editingSnapshot.value)
  }
  editMode.value = false
}

const saveAboutContent = async () => {
  savingContent.value = true
  try {
    await savePageContent('about', serializeAboutContent(), { successMsg: '关于页内容已保存' })
    editMode.value = false
    editingSnapshot.value = null
  } catch {
    ElMessage.error('保存关于页内容失败')
  } finally {
    savingContent.value = false
  }
}

onMounted(() => {
  loadSiteAssets()
  loadAboutContent()
})
</script>

<style lang="scss" scoped>
.about-page {
  min-height: 100vh;
  background: #f6f8fb;
  color: #172033;
}

.about-admin-bar {
  position: sticky;
  top: 0;
  z-index: 30;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  width: min(var(--frontend-container-safe-width), var(--frontend-container-fluid));
  margin: 0 auto;
  padding: 12px 16px;
  border: 1px solid rgba(31, 122, 224, 0.18);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 14px 30px rgba(22, 46, 84, 0.10);
  text-align: left;
}

.about-admin-bar strong {
  display: block;
  color: #14233f;
  font-size: 15px;
}

.about-admin-bar span {
  color: #6d7890;
  font-size: 13px;
}

.about-admin-actions {
  display: flex;
  flex-shrink: 0;
  gap: 8px;
}

.about-hero {
  position: relative;
  min-height: 560px;
  overflow: hidden;
  display: flex;
  align-items: center;
}

.hero-image {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  object-position: center;
}

.hero-shade {
  position: absolute;
  inset: 0;
  background:
    linear-gradient(90deg, rgba(8, 20, 38, 0.86) 0%, rgba(8, 20, 38, 0.58) 44%, rgba(8, 20, 38, 0.16) 100%),
    linear-gradient(0deg, rgba(8, 20, 38, 0.54) 0%, rgba(8, 20, 38, 0) 42%);
}

.hero-content {
  position: relative;
  z-index: 1;
  width: min(var(--frontend-container-safe-width), var(--frontend-container-fluid));
  margin: 0 auto;
  padding: 76px 0 130px;
  color: #fff;

  h1 {
    max-width: 760px;
    margin: 14px 0 18px;
    font-size: 56px;
    line-height: 1.12;
    font-weight: 800;
    letter-spacing: 0;
  }
}

.hero-kicker,
.section-kicker {
  margin: 0;
  font-size: 13px;
  font-weight: 700;
  letter-spacing: 0;
  color: #e94560;
}

.hero-lead {
  max-width: 680px;
  margin: 0;
  font-size: 19px;
  line-height: 1.8;
  color: rgba(255, 255, 255, 0.9);
}

.hero-actions {
  display: flex;
  gap: 14px;
  margin-top: 34px;
  flex-wrap: wrap;
}

.link-edit-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(180px, 260px));
  gap: 10px;
  margin-top: 12px;
}

.primary-action,
.secondary-action,
.source-link {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  min-height: 46px;
  padding: 0 22px;
  border-radius: 4px;
  text-decoration: none;
  font-weight: 700;
  transition: transform 0.2s ease, background 0.2s ease, color 0.2s ease;
}

.primary-action {
  background: #e94560;
  color: #fff;

  &:hover {
    background: #d63d56;
    transform: translateY(-2px);
  }
}

.secondary-action {
  border: 1px solid rgba(255, 255, 255, 0.5);
  color: #fff;

  &:hover {
    background: rgba(255, 255, 255, 0.14);
    transform: translateY(-2px);
  }
}

.license-panel {
  position: absolute;
  z-index: 2;
  right: max(28px, calc((100vw - 1180px) / 2));
  bottom: 42px;
  width: 330px;
  padding: 24px;
  background: rgba(255, 255, 255, 0.94);
  border-left: 5px solid #f2a900;
  box-shadow: 0 18px 45px rgba(12, 28, 48, 0.24);

  span {
    display: block;
    color: #526078;
    line-height: 1.6;
  }

  strong {
    display: block;
    margin: 8px 0;
    color: #172033;
    font-size: 28px;
    letter-spacing: 0;
  }
}

.panel-label {
  font-size: 13px;
}

.fact-band,
.intro-section,
.service-section,
.route-section,
.process-section,
.source-section {
  width: min(var(--frontend-container-safe-width), var(--frontend-container-fluid));
  margin: 0 auto;
}

.fact-band {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 1px;
  transform: translateY(-34px);
  background: #dbe3ee;
  box-shadow: 0 18px 45px rgba(34, 51, 84, 0.12);
}

.fact-item {
  min-height: 118px;
  padding: 24px;
  background: #fff;
  display: grid;
  align-content: center;
  gap: 8px;

  .el-icon {
    color: #008b8b;
    font-size: 24px;
  }

  span {
    color: #69758a;
    font-size: 14px;
  }

  strong {
    color: #172033;
    font-size: 20px;
  }
}

.intro-section,
.route-section,
.source-section {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 420px;
  gap: 56px;
  align-items: center;
  padding: 56px 0 76px;
}

.intro-copy,
.route-copy,
.source-section {
  h2 {
    margin: 10px 0 18px;
    font-size: 36px;
    line-height: 1.25;
    letter-spacing: 0;
  }

  p:not(.section-kicker) {
    margin: 0 0 14px;
    color: #526078;
    line-height: 1.9;
    font-size: 16px;
  }
}

.identity-board {
  background: #16213e;
  color: #fff;
  padding: 28px;
  box-shadow: 0 18px 45px rgba(22, 33, 62, 0.18);
}

.identity-row {
  padding: 18px 0;
  border-bottom: 1px solid rgba(255, 255, 255, 0.12);

  &:last-child {
    border-bottom: none;
  }

  span {
    display: block;
    color: #9fb0cc;
    font-size: 13px;
    margin-bottom: 6px;
  }

  strong {
    font-size: 18px;
    line-height: 1.5;
  }
}

.service-section,
.process-section {
  padding: 74px 0;
}

.section-heading {
  margin-bottom: 32px;

  h2 {
    margin: 10px 0 0;
    font-size: 34px;
    line-height: 1.3;
    letter-spacing: 0;
  }
}

.service-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
}

.service-card {
  min-height: 240px;
  padding: 26px;
  background: #fff;
  border-top: 4px solid #008b8b;
  box-shadow: 0 14px 38px rgba(35, 52, 86, 0.08);

  &:nth-child(2) {
    border-top-color: #e94560;
  }

  &:nth-child(3) {
    border-top-color: #f2a900;
  }

  &:nth-child(4) {
    border-top-color: #5470c6;
  }

  h3 {
    margin: 18px 0 10px;
    font-size: 21px;
  }

  p {
    margin: 0;
    color: #5f6c80;
    line-height: 1.75;
  }
}

.service-icon {
  width: 46px;
  height: 46px;
  display: grid;
  place-items: center;
  background: #f0f6f7;
  color: #008b8b;
  border-radius: 4px;
  font-size: 24px;
}

.route-section {
  background: #fff;
  width: 100%;
  max-width: none;
  padding: 76px max(20px, calc((100vw - 1180px) / 2));
}

.route-list {
  display: grid;
  gap: 16px;
}

.route-item {
  padding: 22px;
  border-left: 4px solid #e94560;
  background: #f6f8fb;

  span {
    color: #008b8b;
    font-size: 12px;
    font-weight: 800;
  }

  strong {
    display: block;
    margin: 6px 0;
    font-size: 20px;
  }

  p {
    margin: 0;
    color: #607086;
    line-height: 1.7;
  }
}

.process-rail {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 1px;
  background: #dce4ef;
}

.process-step {
  background: #fff;
  padding: 28px;
  min-height: 210px;

  .step-number {
    color: #e94560;
    font-size: 13px;
    font-weight: 800;
  }

  h3 {
    margin: 16px 0 10px;
    font-size: 20px;
  }

  p {
    margin: 0;
    color: #607086;
    line-height: 1.75;
  }
}

.source-section {
  grid-template-columns: minmax(0, 1fr) auto;
  padding-top: 24px;
}

.source-link {
  color: #fff;
  background: #172033;
  white-space: nowrap;

  &:hover {
    background: #008b8b;
    transform: translateY(-2px);
  }
}

.source-link-input {
  width: min(360px, 100%);
}

@media (max-width: 1024px) {
  .hero-content h1 {
    font-size: 44px;
  }

  .license-panel {
    left: 20px;
    right: auto;
    width: min(330px, calc(100% - 40px));
  }

  .fact-band,
  .service-grid,
  .process-rail {
    grid-template-columns: repeat(2, 1fr);
  }

  .intro-section,
  .route-section,
  .source-section {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .about-hero {
    min-height: 640px;
  }

  .hero-content {
    width: calc(100% - 30px);
    padding-top: 52px;

    h1 {
      font-size: 34px;
    }
  }

  .hero-lead {
    font-size: 16px;
  }

  .fact-band,
  .service-grid,
  .process-rail {
    grid-template-columns: 1fr;
  }

  .intro-section,
  .service-section,
  .process-section,
  .source-section {
    width: calc(100% - 30px);
    padding: 42px 0;
  }

  .route-section {
    padding: 42px 15px;
  }

  .intro-copy h2,
  .route-copy h2,
  .source-section h2,
  .section-heading h2 {
    font-size: 28px;
  }
}
</style>
