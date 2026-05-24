<template>
  <div class="home-container">
    <section class="hero-section">
      <home-carousel />
    </section>

    <div v-if="userStore.isAdmin" class="home-admin-bar">
      <div>
        <strong>首页编辑模式</strong>
        <span>可直接修改首页个性化文案，保存后写入数据库</span>
      </div>
      <div class="home-admin-actions">
        <el-button v-if="!editMode" type="primary" @click="startEdit">开启编辑</el-button>
        <template v-else>
          <el-button type="primary" :loading="savingContent" @click="saveHomeContent">保存</el-button>
          <el-button :disabled="savingContent" @click="cancelEdit">取消</el-button>
        </template>
      </div>
    </div>

    <main class="home-main">
      <section class="trust-strip">
        <div class="section-container trust-grid">
          <div v-for="item in trustMetrics" :key="item.label" class="trust-item">
            <inline-editable-text v-model="item.value" :edit-mode="editMode" tag="div" class="trust-value" maxlength="20" />
            <div class="trust-copy">
              <inline-editable-text v-model="item.label" :edit-mode="editMode" tag="span" maxlength="40" />
              <inline-editable-text v-model="item.desc" :edit-mode="editMode" tag="small" multiline :rows="2" maxlength="120" />
            </div>
          </div>
        </div>
      </section>

      <section class="enterprise-section featured-programs">
        <div class="section-container">
          <div class="section-heading">
            <div>
              <inline-editable-text v-model="sectionContent.featured.eyebrow" :edit-mode="editMode" tag="span" class="eyebrow" maxlength="40" />
              <inline-editable-text v-model="sectionContent.featured.title" :edit-mode="editMode" tag="h2" maxlength="60" />
              <inline-editable-text v-model="sectionContent.featured.desc" :edit-mode="editMode" tag="p" multiline :rows="2" maxlength="260" />
              <template v-if="false">
              <h2>精选行程</h2>
              <p>以目的地资源、履约能力和服务标准为核心，甄选适合家庭、企业与高端定制需求的旅行产品。</p>
              </template>
            </div>
            <div class="editable-action-wrap">
              <a :href="sectionContent.featured.linkUrl" class="section-link" @click="handleConfiguredLinkClick(sectionContent.featured.linkUrl, $event)">
                <inline-editable-text v-model="sectionContent.featured.linkText" :edit-mode="editMode" tag="span" maxlength="40" />
                <el-icon><ArrowRight /></el-icon>
              </a>
              <el-input v-if="editMode" v-model="sectionContent.featured.linkUrl" class="link-url-input" size="small" placeholder="跳转链接，如 /tickets" />
            </div>
          </div>

          <el-skeleton :loading="tourLoading" animated :throttle="500">
            <template #template>
              <div class="program-skeleton">
                <div class="skeleton-block skeleton-large"></div>
                <div class="skeleton-list">
                  <div v-for="i in 3" :key="i" class="skeleton-block"></div>
                </div>
              </div>
            </template>
            <template #default>
              <div v-if="featuredTour" class="program-layout">
                <article class="program-hero" @click="goToTourDetail(featuredTour.id)">
                  <div class="program-image">
                    <img :src="getImageUrl(featuredTour.mainImage)" :alt="featuredTour.title" />
                    <div class="program-badge">
                      <el-icon><Flag /></el-icon>
                      {{ getTourTypeName(featuredTour.tourType) }}
                    </div>
                  </div>
                  <div class="program-content">
                    <div>
                      <span class="content-label">年度推荐</span>
                      <h3>{{ featuredTour.title }}</h3>
                      <p v-if="featuredTour.subtitle">{{ featuredTour.subtitle }}</p>
                      <div class="tag-row" v-if="featuredTour.tags">
                        <span v-for="(tag, idx) in getTags(featuredTour.tags).slice(0, 4)" :key="idx">{{ tag }}</span>
                      </div>
                    </div>
                    <div class="program-footer">
                      <div class="meta-row">
                        <span v-if="featuredTour.days"><el-icon><Calendar /></el-icon>{{ featuredTour.days }}天{{ Math.max((featuredTour.days || 1) - 1, 0) }}晚</span>
                        <span v-if="featuredTour.starRating"><el-icon><Star /></el-icon>{{ featuredTour.starRating }}</span>
                        <span v-if="featuredTour.enrolledCount"><el-icon><User /></el-icon>{{ featuredTour.enrolledCount }}人已出行</span>
                      </div>
                      <div class="price-action">
                        <div class="price">
                          <small>参考价</small>
                          <strong>¥{{ featuredTour.minPrice !== undefined && featuredTour.minPrice !== null && featuredTour.minPrice !== '' ? featuredTour.minPrice : '--' }}</strong>
                          <span>/人起</span>
                        </div>
                        <el-button type="primary" class="primary-action">
                          查看详情 <el-icon><ArrowRight /></el-icon>
                        </el-button>
                      </div>
                    </div>
                  </div>
                </article>

                <div class="program-list">
                  <article
                    v-for="item in secondaryTours"
                    :key="item.id"
                    class="program-card"
                    @click="goToTourDetail(item.id)"
                  >
                    <img :src="getImageUrl(item.mainImage)" :alt="item.title" />
                    <div class="program-card-body">
                      <div class="program-card-top">
                        <span>{{ getTourTypeName(item.tourType) }}</span>
                        <strong v-if="item.minPrice !== undefined && item.minPrice !== null && item.minPrice !== ''">¥{{ item.minPrice }}起</strong>
                      </div>
                      <h4>{{ item.title }}</h4>
                      <div class="meta-row compact">
                        <span v-if="item.days"><el-icon><Calendar /></el-icon>{{ item.days }}天{{ Math.max((item.days || 1) - 1, 0) }}晚</span>
                        <span v-if="item.starRating"><el-icon><Star /></el-icon>{{ item.starRating }}</span>
                      </div>
                    </div>
                  </article>
                </div>
              </div>
              <el-empty v-else description="暂无精选行程" />
            </template>
          </el-skeleton>
        </div>
      </section>

      <section class="enterprise-section capability-section">
        <div class="section-container">
          <div class="section-heading compact-heading">
            <div>
              <inline-editable-text v-model="sectionContent.capability.eyebrow" :edit-mode="editMode" tag="span" class="eyebrow" maxlength="40" />
              <inline-editable-text v-model="sectionContent.capability.title" :edit-mode="editMode" tag="h2" maxlength="80" />
              <inline-editable-text v-model="sectionContent.capability.desc" :edit-mode="editMode" tag="p" multiline :rows="2" maxlength="260" />
              <template v-if="false">
              <h2>企业级旅行服务能力</h2>
              <p>围绕出行前咨询、资源确认、订单履约和售后响应建立标准化流程，让每一次出行都有清晰保障。</p>
              </template>
            </div>
          </div>

          <div class="capability-grid">
            <article v-for="item in capabilityList" :key="item.title" class="capability-card">
              <div class="capability-icon">
                <el-icon><component :is="item.icon" /></el-icon>
              </div>
              <inline-editable-text v-model="item.title" :edit-mode="editMode" tag="h3" maxlength="50" />
              <inline-editable-text v-model="item.desc" :edit-mode="editMode" tag="p" multiline :rows="3" maxlength="220" />
            </article>
          </div>
        </div>
      </section>

      <section class="enterprise-section scenario-section">
        <div class="section-container scenario-layout">
          <div class="scenario-copy">
            <inline-editable-text v-model="sectionContent.scenario.eyebrow" :edit-mode="editMode" tag="span" class="eyebrow" maxlength="40" />
            <inline-editable-text v-model="sectionContent.scenario.title" :edit-mode="editMode" tag="h2" maxlength="100" />
            <inline-editable-text v-model="sectionContent.scenario.desc" :edit-mode="editMode" tag="p" multiline :rows="3" maxlength="320" />
            <template v-if="false">
            <h2>面向不同客户场景，提供更稳妥的出行方案</h2>
            <p>不是简单陈列线路，而是围绕预算、人数、出行节奏、目的地资源和履约风险进行方案组合，帮助客户更快做出可靠选择。</p>
            </template>
            <div class="editable-action-wrap">
              <a :href="sectionContent.scenario.linkUrl" class="section-link scenario-link" @click="handleConfiguredLinkClick(sectionContent.scenario.linkUrl, $event)">
                <inline-editable-text v-model="sectionContent.scenario.linkText" :edit-mode="editMode" tag="span" maxlength="40" />
                <el-icon><ArrowRight /></el-icon>
              </a>
              <el-input v-if="editMode" v-model="sectionContent.scenario.linkUrl" class="link-url-input" size="small" placeholder="跳转链接，如 /tickets" />
            </div>
          </div>
          <div class="scenario-grid">
            <article v-for="item in scenarioList" :key="item.title" class="scenario-card">
              <inline-editable-text v-model="item.index" :edit-mode="editMode" tag="span" maxlength="8" />
              <inline-editable-text v-model="item.title" :edit-mode="editMode" tag="h3" maxlength="50" />
              <inline-editable-text v-model="item.desc" :edit-mode="editMode" tag="p" multiline :rows="3" maxlength="220" />
            </article>
          </div>
        </div>
      </section>

      <section class="enterprise-section insight-section">
        <div class="section-container">
          <div class="section-heading">
            <div>
              <inline-editable-text v-model="sectionContent.insight.eyebrow" :edit-mode="editMode" tag="span" class="eyebrow" maxlength="40" />
              <inline-editable-text v-model="sectionContent.insight.title" :edit-mode="editMode" tag="h2" maxlength="60" />
              <inline-editable-text v-model="sectionContent.insight.desc" :edit-mode="editMode" tag="p" multiline :rows="2" maxlength="260" />
              <template v-if="false">
              <h2>旅行攻略</h2>
              <p>从目的地经验、路线规划到出行提示，为客户提供更可靠的决策参考。</p>
              </template>
            </div>
            <div class="editable-action-wrap">
              <a :href="sectionContent.insight.linkUrl" class="section-link" @click="handleConfiguredLinkClick(sectionContent.insight.linkUrl, $event)">
                <inline-editable-text v-model="sectionContent.insight.linkText" :edit-mode="editMode" tag="span" maxlength="40" />
                <el-icon><ArrowRight /></el-icon>
              </a>
              <el-input v-if="editMode" v-model="sectionContent.insight.linkUrl" class="link-url-input" size="small" placeholder="跳转链接，如 /guide" />
            </div>
          </div>

          <el-skeleton :loading="guideLoading" animated :throttle="500">
            <template #template>
              <div class="insight-grid">
                <div v-for="i in 3" :key="i" class="skeleton-block guide-skeleton"></div>
              </div>
            </template>
            <template #default>
              <div v-if="guideList.length > 0" class="insight-grid">
                <article
                  v-for="item in guideList"
                  :key="item.id"
                  class="insight-card"
                  @click="goToGuideDetail(item.id)"
                >
                  <div class="insight-image">
                    <img :src="getImageUrl(item.coverImage)" :alt="item.title" />
                    <span><el-icon><View /></el-icon>{{ item.views || 0 }}</span>
                  </div>
                  <div class="insight-body">
                    <h3>{{ item.title }}</h3>
                    <div class="insight-meta">
                      <span><el-icon><User /></el-icon>{{ item.userNickname || '旅行顾问' }}</span>
                      <span>{{ formatDate(item.createTime) }}</span>
                    </div>
                  </div>
                </article>
              </div>
              <el-empty v-else description="暂无旅行攻略" />
            </template>
          </el-skeleton>
        </div>
      </section>

      <section class="assurance-section">
        <div class="section-container assurance-layout">
          <div class="assurance-title">
            <inline-editable-text v-model="sectionContent.assurance.eyebrow" :edit-mode="editMode" tag="span" class="eyebrow" maxlength="40" />
            <inline-editable-text v-model="sectionContent.assurance.title" :edit-mode="editMode" tag="h2" maxlength="110" />
            <inline-editable-text v-model="sectionContent.assurance.desc" :edit-mode="editMode" tag="p" multiline :rows="4" maxlength="420" />
            <template v-if="false">
            <h2>把旅行服务拆成可确认、可通知、可追踪的业务节点</h2>
            <p>从信息核验、支付安全到行前通知和售后跟进，每个关键环节都有明确反馈，帮助客户减少等待、降低信息遗漏风险，让出行安排更安心、更可控。</p>
            </template>
          </div>
          <div class="assurance-list">
            <div v-for="item in assuranceList" :key="item.title" class="assurance-item">
              <inline-editable-text v-model="item.title" :edit-mode="editMode" tag="strong" maxlength="50" />
              <inline-editable-text v-model="item.desc" :edit-mode="editMode" tag="span" multiline :rows="2" maxlength="220" />
            </div>
          </div>
        </div>
      </section>

      <section class="process-section">
        <div class="section-container process-layout">
          <div class="process-copy">
            <inline-editable-text v-model="sectionContent.process.eyebrow" :edit-mode="editMode" tag="span" class="eyebrow" maxlength="40" />
            <inline-editable-text v-model="sectionContent.process.title" :edit-mode="editMode" tag="h2" maxlength="100" />
            <inline-editable-text v-model="sectionContent.process.desc" :edit-mode="editMode" tag="p" multiline :rows="3" maxlength="320" />
            <template v-if="false">
            <h2>从咨询到出行，全流程清晰可追踪</h2>
            <p>官网提交需求后，由客服顾问进行订单确认、合同与支付引导，并在出行前完成必要信息核验和提醒。</p>
            </template>
          </div>
          <div class="process-steps">
            <div v-for="(step, index) in serviceProcess" :key="step.title" class="process-step">
              <span>{{ String(index + 1).padStart(2, '0') }}</span>
              <div>
                <inline-editable-text v-model="step.title" :edit-mode="editMode" tag="h3" maxlength="50" />
                <inline-editable-text v-model="step.desc" :edit-mode="editMode" tag="p" multiline :rows="2" maxlength="220" />
              </div>
            </div>
          </div>
        </div>
      </section>
    </main>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'
import noImage from '@/assets/images/no-image.png'
import HomeCarousel from '@/components/frontend/HomeCarousel.vue'
import InlineEditableText from '@/components/frontend/InlineEditableText.vue'
import { useUserStore } from '@/store/user'
import { getPublicPageContent, savePageContent } from '@/api/pageContent'
import {
  ArrowRight,
  View,
  Calendar,
  Star,
  User,
  Flag,
  OfficeBuilding,
  Service,
  Medal,
  Location
} from '@element-plus/icons-vue'

const router = useRouter()
const userStore = useUserStore()
const baseAPI = process.env.VUE_APP_BASE_API || '/api'

const tourList = ref([])
const tourLoading = ref(true)
const guideList = ref([])
const guideLoading = ref(true)
const editMode = ref(false)
const savingContent = ref(false)
const editingSnapshot = ref(null)

const trustMetrics = [
  { value: '24h', label: '服务响应', desc: '订单咨询与出行问题快速跟进' },
  { value: '1v1', label: '顾问对接', desc: '重要节点由专人确认' },
  { value: '全流程', label: '履约跟踪', desc: '从预订到出行持续管理' },
  { value: '合规', label: '安全保障', desc: '资料、支付与通知规范处理' }
]

const capabilityList = [
  { icon: OfficeBuilding, title: '企业与团队出行', desc: '支持企业团建、会议会奖、研学与定制路线，兼顾预算、体验与执行效率。' },
  { icon: Service, title: '专属客服跟进', desc: '下单后由客服主动联系客户，核对联系人、出行人和关键行程信息。' },
  { icon: Medal, title: '严选产品资源', desc: '围绕目的地、服务商与行程节奏进行筛选，减少低质产品带来的履约风险。' },
  { icon: Location, title: '目的地方案能力', desc: '结合攻略内容和真实出行反馈，提供更贴近客户场景的旅行建议。' }
]

const scenarioList = [
  { index: '01', title: '家庭度假', desc: '关注酒店舒适度、亲子体验、交通衔接和每日节奏，减少长途奔波。' },
  { index: '02', title: '企业团建', desc: '兼顾团队规模、预算审批、统一通知和安全保障，便于组织与落地。' },
  { index: '03', title: '长线深度游', desc: '强化目的地停留质量、景点组合和服务商资源，提升完整体验。' },
  { index: '04', title: '高端定制', desc: '按客户偏好定制行程结构，由顾问对接关键节点和特殊需求。' }
]

const assuranceList = [
  { title: '信息核验', desc: '联系人、出行人、订单金额和出发日期在提交前后都有明确确认。' },
  { title: '支付安全', desc: '支付渠道由后台启用，前端不接触密钥和渠道敏感配置。' },
  { title: '消息通知', desc: '订单关键状态通过站内信、短信和邮件触达用户与管理人员。' },
  { title: '售后跟进', desc: '付款成功后安排客服电话对接，减少客户等待和信息遗漏。' }
]

const serviceProcess = [
  { title: '选择行程', desc: '浏览精选路线与攻略建议，确认适合的出行方案。' },
  { title: '提交订单', desc: '填写联系人和出行人信息，系统完成订单创建与通知。' },
  { title: '安全支付', desc: '仅展示后台启用渠道，由后端生成支付表单并跳转。' },
  { title: '客服对接', desc: '付款后客服进行电话确认，协助完成后续出行安排。' }
]

const defaultSectionContent = {
  featured: {
    eyebrow: 'Featured Programs',
    title: '精选行程',
    desc: '以目的地资源、履约能力和服务标准为核心，甄选适合家庭、企业与高端定制需求的旅行产品。',
    linkText: '查看全部行程',
    linkUrl: '/tickets'
  },
  capability: {
    eyebrow: 'Service Capability',
    title: '企业级旅行服务能力',
    desc: '围绕出行前咨询、资源确认、订单履约和售后响应建立标准化流程，让每一次出行都有清晰保障。'
  },
  scenario: {
    eyebrow: 'Travel Solutions',
    title: '面向不同客户场景，提供更稳定的出行方案',
    desc: '不是简单陈列线路，而是围绕预算、人数、出行节奏、目的地资源和履约风险进行方案组合，帮助客户更快做出可靠选择。',
    linkText: '浏览全部产品',
    linkUrl: '/tickets'
  },
  insight: {
    eyebrow: 'Travel Insight',
    title: '旅行攻略',
    desc: '从目的地经验、路线规划到出行提示，为客户提供更可靠的决策参考。',
    linkText: '查看全部攻略',
    linkUrl: '/guide'
  },
  assurance: {
    eyebrow: 'Operational Standard',
    title: '把旅行服务拆成可确认、可通知、可追踪的业务节点',
    desc: '从信息核验、支付安全到行前通知和售后跟进，每个关键环节都有明确反馈，帮助客户减少等待、降低信息遗漏风险。'
  },
  process: {
    eyebrow: 'Booking Process',
    title: '从咨询到出行，全流程清晰可追踪',
    desc: '官网提交需求后，由客服顾问进行订单确认、合同与支付引导，并在出行前完成必要信息核验和提醒。'
  }
}

const cloneData = (data) => JSON.parse(JSON.stringify(data))
const sectionContent = ref(cloneData(defaultSectionContent))
const capabilityIcons = [OfficeBuilding, Service, Medal, Location]

const replaceList = (target, source, mapper = item => item) => {
  if (!Array.isArray(source)) return
  target.splice(0, target.length, ...source.map((item, index) => mapper(item || {}, index)))
}

const applyHomeContent = (content = {}) => {
  sectionContent.value = {
    ...cloneData(defaultSectionContent),
    ...(content.sections || {})
  }
  replaceList(trustMetrics, content.trustMetrics)
  replaceList(capabilityList, content.capabilityList, (item, index) => ({
    title: item.title || '',
    desc: item.desc || '',
    icon: capabilityIcons[index] || Service
  }))
  replaceList(scenarioList, content.scenarioList)
  replaceList(assuranceList, content.assuranceList)
  replaceList(serviceProcess, content.serviceProcess)
}

const serializeHomeContent = () => ({
  sections: sectionContent.value,
  trustMetrics,
  capabilityList: capabilityList.map(({ title, desc }) => ({ title, desc })),
  scenarioList,
  assuranceList,
  serviceProcess
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

const loadHomeContent = async () => {
  try {
    const data = await getPublicPageContent('home')
    applyHomeContent(data || {})
  } catch {
    applyHomeContent({})
  }
}

const startEdit = () => {
  editingSnapshot.value = cloneData(serializeHomeContent())
  editMode.value = true
}

const cancelEdit = () => {
  if (editingSnapshot.value) {
    applyHomeContent(editingSnapshot.value)
  }
  editMode.value = false
}

const saveHomeContent = async () => {
  savingContent.value = true
  try {
    await savePageContent('home', serializeHomeContent(), { successMsg: '首页内容已保存' })
    editMode.value = false
    editingSnapshot.value = null
  } catch {
    ElMessage.error('保存首页内容失败')
  } finally {
    savingContent.value = false
  }
}

const featuredTour = computed(() => tourList.value[0] || null)
const secondaryTours = computed(() => tourList.value.slice(1, 4))

const fetchTours = async () => {
  tourLoading.value = true
  try {
    const [featuredRes, moreRes] = await Promise.all([
      request.get('/tour/featured', {}, { showDefaultMsg: false }),
      request.get('/tour/more', {}, { showDefaultMsg: false })
    ])
    tourList.value = mergeHomeTours(featuredRes || [], moreRes || [])
  } catch {
    try {
      await request.get('/tour/all', {}, {
        showDefaultMsg: false,
        onSuccess: (data) => {
          tourList.value = data || []
        }
      })
    } catch {
      tourList.value = []
    }
  } finally {
    tourLoading.value = false
  }
}

const mergeHomeTours = (featuredTours, moreTours) => {
  const result = []
  const seen = new Set()
  const append = (tour) => {
    if (!tour || !tour.id || seen.has(tour.id)) return
    seen.add(tour.id)
    result.push(tour)
  }
  append(featuredTours[0])
  moreTours.forEach(append)
  featuredTours.slice(1).forEach(append)
  return result
}

const fetchHotGuides = async () => {
  guideLoading.value = true
  try {
    await request.get('/guide/hot', {
      limit: 3
    }, {
      showDefaultMsg: false,
      onSuccess: (data) => {
        guideList.value = data || []
      }
    })
  } catch {
    guideList.value = []
  } finally {
    guideLoading.value = false
  }
}

const getImageUrl = (url) => {
  if (!url) return noImage
  let imageUrl = url
  if (typeof imageUrl === 'string' && imageUrl.trim().startsWith('[')) {
    try {
      const images = JSON.parse(imageUrl)
      imageUrl = Array.isArray(images) ? images[0] : imageUrl
    } catch {
      imageUrl = ''
    }
  }
  if (!imageUrl) return noImage
  return imageUrl.startsWith('http') ? imageUrl : baseAPI + imageUrl
}

const getTourTypeName = (type) => {
  const typeMap = {
    around: '周边游',
    long: '长线游',
    team: '跟团游',
    cruise: '邮轮出行'
  }
  return typeMap[type] || '精选游'
}

const getTags = (tagsStr) => {
  if (!tagsStr) return []
  if (Array.isArray(tagsStr)) return tagsStr
  if (typeof tagsStr === 'string') {
    if (tagsStr.startsWith('[')) {
      try {
        return JSON.parse(tagsStr)
      } catch {
        return tagsStr.split(',').map(t => t.trim()).filter(Boolean)
      }
    }
    return tagsStr.split(',').map(t => t.trim()).filter(Boolean)
  }
  return []
}

const formatDate = (dateString) => {
  if (!dateString) return ''
  const date = new Date(dateString)
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
}

const goToTourDetail = (tourId) => {
  router.push(`/ticket/booking/${tourId}`)
}

const goToGuideDetail = (guideId) => {
  router.push(`/guide/detail/${guideId}`)
}

onMounted(() => {
  loadHomeContent()
  fetchTours()
  fetchHotGuides()
})
</script>

<style lang="scss" scoped>
.home-container {
  width: 100%;
  min-height: 100vh;
  color: #172033;
  background: #f6f8fb;
  font-family: "Source Han Sans", "Noto Sans CJK SC", "Microsoft YaHei", sans-serif;
}

.hero-section {
  width: 100%;
  margin: 0;
  padding: 0;
  position: relative;
}

.home-admin-bar {
  position: sticky;
  top: 0;
  z-index: 20;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  width: min(1180px, calc(100% - 40px));
  margin: 14px auto 0;
  padding: 12px 16px;
  border: 1px solid rgba(31, 122, 224, 0.18);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 14px 30px rgba(22, 46, 84, 0.10);
  text-align: left;
}

.home-admin-bar strong {
  display: block;
  color: #14233f;
  font-size: 15px;
}

.home-admin-bar span {
  color: #6d7890;
  font-size: 13px;
}

.home-admin-actions {
  display: flex;
  flex-shrink: 0;
  gap: 8px;
}

.home-main {
  position: relative;
  display: flex;
  flex-direction: column;
  background:
    linear-gradient(180deg, #ffffff 0%, #f7f9fc 18%, #ffffff 44%, #f5f8fb 100%);
}

.trust-strip {
  order: 1;
}

.featured-programs {
  order: 2;
}

.insight-section {
  order: 3;
}

.capability-section {
  order: 4;
}

.scenario-section {
  order: 5;
}

.assurance-section {
  order: 6;
}

.process-section {
  order: 7;
}

.section-container {
  width: min(1240px, calc(100% - 48px));
  margin: 0 auto;
}

.trust-strip {
  position: relative;
  z-index: 2;
  padding: 30px 0 0;
  background: #fff;
}

.trust-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  background: #fff;
  border: 1px solid #dfe7f1;
  box-shadow: 0 14px 34px rgba(21, 35, 56, 0.08);
}

.trust-item {
  display: flex;
  gap: 16px;
  align-items: center;
  min-height: 98px;
  padding: 22px 26px;
  border-right: 1px solid #e6ebf2;

  &:last-child {
    border-right: 0;
  }
}

.trust-value {
  min-width: 70px;
  color: #0f766e;
  font-size: 26px;
  font-weight: 800;
  line-height: 1;
  letter-spacing: 0;
}

.trust-copy {
  display: flex;
  flex-direction: column;
  gap: 6px;

  span {
    color: #111827;
    font-size: 15px;
    font-weight: 700;
  }

  small {
    color: #687589;
    font-size: 12px;
    line-height: 1.5;
  }
}

.enterprise-section {
  padding: 86px 0;
}

.featured-programs {
  padding-top: 104px;
  padding-bottom: 58px;
  background:
    linear-gradient(180deg, #f7f9fc 0%, #ffffff 58%, #ffffff 100%);
}

.capability-section {
  position: relative;
  overflow: hidden;
  background:
    linear-gradient(135deg, #eefdfa 0%, #f8fafc 48%, #eef5ff 100%);
  color: #172033;

  &::before {
    content: '';
    position: absolute;
    inset: 0;
    background:
      linear-gradient(90deg, rgba(15, 118, 110, 0.08) 1px, transparent 1px),
      linear-gradient(180deg, rgba(15, 118, 110, 0.06) 1px, transparent 1px);
    background-size: 42px 42px;
    mask-image: linear-gradient(180deg, rgba(0, 0, 0, 0.72), transparent 78%);
    pointer-events: none;
  }

  .section-container {
    position: relative;
    z-index: 1;
  }
}

.insight-section {
  position: relative;
  overflow: hidden;
  padding-top: 64px;
  padding-bottom: 92px;
  background:
    radial-gradient(circle at 18% 22%, rgba(15, 118, 110, 0.055) 0, transparent 30%),
    radial-gradient(circle at 88% 72%, rgba(59, 130, 246, 0.05) 0, transparent 26%),
    linear-gradient(180deg, #f6f8fb 0%, #fbfcfe 54%, #ffffff 100%);

  &::before {
    content: '';
    position: absolute;
    left: 0;
    top: 0;
    width: 100%;
    height: 68px;
    background:
      linear-gradient(180deg, #ffffff 0%, rgba(255, 255, 255, 0.78) 42%, rgba(246, 248, 251, 0) 100%);
  }

  &::after {
    content: '';
    position: absolute;
    left: 50%;
    top: 22px;
    width: min(1240px, calc(100% - 48px));
    height: 1px;
    transform: translateX(-50%);
    background: linear-gradient(90deg, transparent, rgba(15, 118, 110, 0.16), rgba(59, 130, 246, 0.12), transparent);
    pointer-events: none;
  }

  .section-container {
    position: relative;
    z-index: 1;
  }
}

.section-heading {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  gap: 32px;
  margin-bottom: 34px;

  h2 {
    margin: 8px 0 12px;
    color: #111827;
    font-size: 34px;
    font-weight: 800;
    line-height: 1.2;
    letter-spacing: 0;
  }

  p {
    max-width: 640px;
    margin: 0;
    color: #667085;
    font-size: 15px;
    line-height: 1.8;
  }
}

.capability-section .section-heading {
  h2 {
    color: #0f172a;
  }

  p {
    color: #5c6b7e;
  }
}

.compact-heading {
  max-width: 760px;
}

.eyebrow {
  display: inline-flex;
  align-items: center;
  color: #0f766e;
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0;
  text-transform: uppercase;
}

.capability-section .eyebrow {
  color: #0f766e;
}

.section-link {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  flex: 0 0 auto;
  color: #0f766e;
  font-size: 14px;
  font-weight: 700;
  text-decoration: none;
  transition: color 0.2s ease, transform 0.2s ease;

  &:hover {
    color: #115e59;
    transform: translateX(2px);
  }
}

.editable-action-wrap {
  display: inline-flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 6px;
}

.link-url-input {
  width: min(260px, 70vw);
}

.program-layout {
  display: grid;
  grid-template-columns: minmax(0, 1.35fr) minmax(360px, 0.65fr);
  gap: 24px;
}

.program-hero,
.program-card,
.insight-card,
.capability-card {
  background: #fff;
  border: 1px solid #e5ebf3;
  cursor: pointer;
  transition: transform 0.25s ease, box-shadow 0.25s ease, border-color 0.25s ease;

  &:hover {
    transform: translateY(-4px);
    border-color: rgba(15, 118, 110, 0.28);
    box-shadow: 0 22px 46px rgba(18, 32, 52, 0.12);
  }
}

.program-hero {
  display: grid;
  grid-template-columns: minmax(320px, 0.96fr) minmax(0, 1.04fr);
  min-height: 470px;
  overflow: hidden;
}

.program-image,
.insight-image {
  position: relative;
  overflow: hidden;
  background: #e8eef5;

  img {
    width: 100%;
    height: 100%;
    display: block;
    object-fit: cover;
    transition: transform 0.45s ease;
  }
}

.program-hero:hover .program-image img,
.program-card:hover img,
.insight-card:hover img {
  transform: scale(1.045);
}

.program-badge {
  position: absolute;
  left: 22px;
  top: 22px;
  display: inline-flex;
  align-items: center;
  gap: 7px;
  padding: 9px 13px;
  color: #0f2d2b;
  background: rgba(255, 255, 255, 0.92);
  border: 1px solid rgba(255, 255, 255, 0.72);
  font-size: 13px;
  font-weight: 800;
  backdrop-filter: blur(10px);
}

.program-content {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  padding: 38px;
}

.content-label {
  display: inline-flex;
  margin-bottom: 12px;
  color: #0f766e;
  font-size: 13px;
  font-weight: 800;
}

.program-content h3 {
  margin: 0 0 14px;
  color: #101828;
  font-size: 30px;
  font-weight: 800;
  line-height: 1.3;
  letter-spacing: 0;
}

.program-content p {
  margin: 0;
  color: #667085;
  font-size: 15px;
  line-height: 1.8;
}

.tag-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 22px;

  span {
    padding: 7px 11px;
    color: #0f766e;
    background: #ecfdf5;
    border: 1px solid #cceee4;
    font-size: 12px;
    font-weight: 700;
  }
}

.program-footer {
  display: flex;
  flex-direction: column;
  gap: 24px;
  margin-top: 32px;
}

.meta-row {
  display: flex;
  flex-wrap: wrap;
  gap: 12px 18px;
  color: #667085;
  font-size: 13px;

  span {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    white-space: nowrap;
  }

  .el-icon {
    color: #0f766e;
  }
}

.price-action {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  gap: 20px;
  padding-top: 24px;
  border-top: 1px solid #e6ebf2;
}

.price {
  display: flex;
  align-items: baseline;
  gap: 6px;

  small {
    color: #7b8794;
    font-size: 12px;
  }

  strong {
    color: #b45309;
    font-size: 32px;
    font-weight: 900;
    line-height: 1;
  }

  span {
    color: #7b8794;
    font-size: 13px;
  }
}

.primary-action {
  height: 42px;
  padding: 0 20px;
  border: 0;
  border-radius: 2px;
  background: #0f766e;
  font-weight: 800;

  &:hover {
    background: #115e59;
  }
}

.program-list {
  display: grid;
  grid-template-rows: repeat(3, minmax(0, 1fr));
  gap: 18px;
}

.program-card {
  display: grid;
  grid-template-columns: 138px minmax(0, 1fr);
  overflow: hidden;

  img {
    width: 138px;
    height: 100%;
    min-height: 142px;
    object-fit: cover;
    background: #e8eef5;
    transition: transform 0.45s ease;
  }
}

.program-card-body {
  min-width: 0;
  padding: 18px;
}

.program-card-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;

  span {
    color: #0f766e;
    font-size: 12px;
    font-weight: 800;
  }

  strong {
    color: #b45309;
    font-size: 13px;
  }
}

.program-card h4 {
  display: -webkit-box;
  min-height: 48px;
  margin: 0 0 16px;
  overflow: hidden;
  color: #101828;
  font-size: 17px;
  font-weight: 800;
  line-height: 1.45;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.compact {
  gap: 12px;
  font-size: 12px;
}

.capability-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 18px;
}

.capability-card {
  min-height: 238px;
  padding: 28px 24px;
  color: #172033;
  background: rgba(255, 255, 255, 0.82);
  border-color: rgba(203, 213, 225, 0.88);
  cursor: default;
  backdrop-filter: blur(12px);

  &:hover {
    border-color: rgba(15, 118, 110, 0.3);
    box-shadow: 0 20px 42px rgba(21, 35, 56, 0.11);
  }

  h3 {
    margin: 22px 0 12px;
    font-size: 18px;
    font-weight: 800;
  }

  p {
    margin: 0;
    color: #667085;
    font-size: 14px;
    line-height: 1.8;
  }
}

.capability-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 46px;
  height: 46px;
  color: #0f766e;
  background: #ccfbf1;
  font-size: 22px;
}

.scenario-section {
  position: relative;
  background:
    linear-gradient(180deg, #f8fafc 0%, #eef5f8 100%);
  border-top: 1px solid #e5edf5;
  border-bottom: 1px solid #dce7f0;

  &::after {
    content: '';
    position: absolute;
    left: 50%;
    bottom: 0;
    width: min(1240px, calc(100% - 48px));
    height: 1px;
    transform: translateX(-50%);
    background: linear-gradient(90deg, transparent, rgba(15, 118, 110, 0.28), transparent);
  }
}

.scenario-layout {
  display: grid;
  grid-template-columns: minmax(280px, 0.78fr) minmax(0, 1.22fr);
  gap: 46px;
  align-items: start;
}

.scenario-copy {
  position: sticky;
  top: 92px;

  h2 {
    margin: 10px 0 16px;
    color: #111827;
    font-size: 34px;
    font-weight: 850;
    line-height: 1.25;
    letter-spacing: 0;
  }

  p {
    margin: 0;
    color: #667085;
    font-size: 15px;
    line-height: 1.85;
  }
}

.scenario-link {
  margin-top: 24px;
}

.scenario-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px;
}

.scenario-card {
  min-height: 214px;
  padding: 28px;
  background: rgba(255, 255, 255, 0.86);
  border: 1px solid #e3eaf3;
  box-shadow: 0 10px 26px rgba(21, 35, 56, 0.055);
  transition: transform 0.25s ease, box-shadow 0.25s ease, border-color 0.25s ease;

  &:hover {
    transform: translateY(-4px);
    border-color: rgba(15, 118, 110, 0.28);
    box-shadow: 0 18px 38px rgba(21, 35, 56, 0.1);
  }

  span {
    color: #0f766e;
    font-size: 13px;
    font-weight: 900;
  }

  h3 {
    margin: 34px 0 12px;
    color: #101828;
    font-size: 20px;
    font-weight: 850;
  }

  p {
    margin: 0;
    color: #667085;
    font-size: 14px;
    line-height: 1.8;
  }
}

.insight-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 22px;
}

.insight-card {
  overflow: hidden;
  box-shadow: 0 16px 36px rgba(21, 35, 56, 0.08);
}

.insight-image {
  height: 238px;

  span {
    position: absolute;
    left: 18px;
    bottom: 18px;
    display: inline-flex;
    align-items: center;
    gap: 6px;
    padding: 8px 11px;
    color: #fff;
    background: rgba(15, 23, 42, 0.68);
    font-size: 12px;
    font-weight: 700;
    backdrop-filter: blur(10px);
  }
}

.insight-body {
  padding: 22px;
  background:
    linear-gradient(180deg, #ffffff 0%, #fbfcfe 100%);

  h3 {
    display: -webkit-box;
    min-height: 56px;
    margin: 0 0 18px;
    overflow: hidden;
    color: #101828;
    font-size: 18px;
    font-weight: 800;
    line-height: 1.55;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
  }
}

.insight-meta {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  color: #667085;
  font-size: 13px;

  span {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    min-width: 0;
  }

  span:first-child {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .el-icon {
    flex: 0 0 auto;
    color: #0f766e;
  }
}

.process-section {
  padding: 76px 0 90px;
  background: #f6f8fb;
}

.assurance-section {
  padding: 72px 0;
  background: #0f766e;
  color: #fff;
}

.assurance-layout {
  display: grid;
  grid-template-columns: minmax(280px, 0.72fr) minmax(0, 1.28fr);
  gap: 46px;
  align-items: center;
}

.assurance-title {
  grid-column: 2;
  align-self: center;
  justify-self: center;
  max-width: 500px;
  transform: translateX(22px);

  .eyebrow {
    color: #a7f3d0;
  }

  h2 {
    margin: 10px 0 0;
    font-size: 31px;
    font-weight: 850;
    line-height: 1.32;
    letter-spacing: 0;
  }

  p {
    max-width: 410px;
    margin: 18px 0 0 28px;
    color: rgba(255, 255, 255, 0.76);
    font-size: 15px;
    line-height: 1.9;
  }
}

.assurance-list {
  grid-column: 1;
  grid-row: 1;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 1px;
  background: rgba(255, 255, 255, 0.22);
  border: 1px solid rgba(255, 255, 255, 0.22);
}

.assurance-item {
  min-height: 130px;
  padding: 24px;
  background: rgba(255, 255, 255, 0.08);

  strong {
    display: block;
    margin-bottom: 10px;
    font-size: 17px;
    font-weight: 850;
  }

  span {
    color: rgba(255, 255, 255, 0.78);
    font-size: 13px;
    line-height: 1.8;
  }
}

.process-layout {
  display: grid;
  grid-template-columns: minmax(260px, 0.78fr) minmax(0, 1.22fr);
  gap: 42px;
  align-items: center;
}

.process-copy {
  h2 {
    margin: 10px 0 16px;
    color: #111827;
    font-size: 32px;
    font-weight: 850;
    line-height: 1.28;
    letter-spacing: 0;
  }

  p {
    margin: 0;
    color: #667085;
    font-size: 15px;
    line-height: 1.85;
  }
}

.process-steps {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.process-step {
  display: flex;
  gap: 18px;
  min-height: 142px;
  padding: 24px;
  background: #fff;
  border: 1px solid #e5ebf3;

  > span {
    flex: 0 0 auto;
    color: #0f766e;
    font-size: 24px;
    font-weight: 900;
    line-height: 1;
  }

  h3 {
    margin: 0 0 10px;
    color: #101828;
    font-size: 17px;
    font-weight: 800;
  }

  p {
    margin: 0;
    color: #667085;
    font-size: 13px;
    line-height: 1.75;
  }
}

.program-skeleton {
  display: grid;
  grid-template-columns: minmax(0, 1.35fr) minmax(360px, 0.65fr);
  gap: 24px;
}

.skeleton-list {
  display: grid;
  gap: 18px;
}

.skeleton-block {
  min-height: 142px;
  background: linear-gradient(90deg, #edf1f6 25%, #e3e9f1 50%, #edf1f6 75%);
  background-size: 200% 100%;
  animation: skeleton-loading 1.4s infinite;
}

.skeleton-large {
  min-height: 470px;
}

.guide-skeleton {
  min-height: 324px;
}

@keyframes skeleton-loading {
  0% {
    background-position: 200% 0;
  }
  100% {
    background-position: -200% 0;
  }
}

@media (max-width: 1180px) {
  .program-layout,
  .program-skeleton {
    grid-template-columns: 1fr;
  }

  .program-list {
    grid-template-columns: repeat(3, minmax(0, 1fr));
    grid-template-rows: none;
  }

  .program-card {
    grid-template-columns: 1fr;

    img {
      width: 100%;
      height: 178px;
    }
  }

  .capability-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 900px) {
  .section-container {
    width: min(100% - 32px, 760px);
  }

  .trust-grid,
  .insight-grid,
  .process-layout,
  .scenario-layout,
  .assurance-layout {
    grid-template-columns: 1fr;
  }

  .trust-item {
    border-right: 0;
    border-bottom: 1px solid #e6ebf2;

    &:last-child {
      border-bottom: 0;
    }
  }

  .section-heading {
    align-items: flex-start;
    flex-direction: column;
  }

  .program-hero,
  .program-list,
  .process-steps,
  .scenario-grid,
  .assurance-list {
    grid-template-columns: 1fr;
  }

  .program-image {
    min-height: 280px;
  }

  .scenario-copy {
    position: static;
  }

  .assurance-title,
  .assurance-list {
    grid-column: auto;
    grid-row: auto;
  }

  .assurance-title {
    justify-self: stretch;
    max-width: none;
    transform: none;

    p {
      max-width: none;
      margin-left: 0;
    }
  }
}

@media (max-width: 640px) {
  .enterprise-section,
  .featured-programs {
    padding: 58px 0;
  }

  .section-heading h2,
  .process-copy h2 {
    font-size: 26px;
  }

  .program-content {
    padding: 24px;
  }

  .program-content h3 {
    font-size: 24px;
  }

  .price-action {
    align-items: flex-start;
    flex-direction: column;
  }

  .primary-action {
    width: 100%;
  }

  .capability-grid {
    grid-template-columns: 1fr;
  }
}
</style>
