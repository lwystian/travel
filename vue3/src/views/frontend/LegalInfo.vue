<template>
  <main class="legal-page">
    <section class="legal-hero" :style="legalHeroStyle">
      <div class="legal-hero__content">
        <p>Trust Center</p>
        <template v-if="editMode">
          <el-input v-model="currentPage.title" class="legal-edit-title" maxlength="60" placeholder="页面标题" />
          <el-input v-model="currentPage.summary" type="textarea" :rows="2" maxlength="220" show-word-limit placeholder="页面摘要" />
        </template>
        <template v-else>
          <h1>{{ currentPage.title }}</h1>
          <span>{{ currentPage.summary }}</span>
        </template>
      </div>
    </section>

    <section class="legal-shell">
      <div v-if="canEdit" class="legal-admin-bar">
        <div>
          <strong>页面内容编辑</strong>
          <span>管理员可在展示页直接维护正文与资质图片</span>
        </div>
        <div class="legal-admin-actions">
          <el-button v-if="!editMode" type="primary" @click="startEdit">编辑当前内容</el-button>
          <template v-else>
            <el-button type="primary" :loading="saving" @click="saveLegalPages">保存</el-button>
            <el-button :disabled="saving" @click="cancelEdit">取消</el-button>
          </template>
        </div>
      </div>

      <aside class="legal-nav" aria-label="合规说明导航">
        <router-link
          v-for="item in displayedLegalPages"
          :key="item.type"
          :to="`/legal/${item.type}`"
          class="legal-nav__item"
          :class="{ active: item.type === currentType }"
        >
          <strong>{{ item.title }}</strong>
          <span>{{ item.navText }}</span>
        </router-link>
      </aside>

      <article class="legal-content">
        <div class="legal-card">
          <template v-if="editMode">
            <el-input v-model="currentPage.kicker" maxlength="30" placeholder="页面小标题" />
            <el-input v-model="currentPage.heading" class="legal-edit-heading" maxlength="100" placeholder="正文标题" />
            <el-input v-model="currentPage.navText" maxlength="80" placeholder="左侧导航说明" />
            <el-input v-model="currentPage.lead" type="textarea" :rows="4" maxlength="800" show-word-limit placeholder="正文导语" />
          </template>
          <template v-else>
            <p class="legal-kicker">{{ currentPage.kicker }}</p>
            <h2>{{ currentPage.heading }}</h2>
            <p class="legal-lead">{{ currentPage.lead }}</p>
          </template>

          <section v-for="(section, sectionIndex) in currentPage.sections" :key="`section-${sectionIndex}`" class="legal-section">
            <template v-if="editMode">
              <div class="legal-edit-section-head">
                <el-input v-model="section.title" maxlength="60" placeholder="段落标题" />
                <el-button type="danger" plain @click="removeSection(currentPage, sectionIndex)">删除段落</el-button>
              </div>
              <el-input v-model="section.text" type="textarea" :rows="3" maxlength="800" show-word-limit placeholder="段落说明，可留空" />
              <div v-for="(item, itemIndex) in section.items" :key="`item-${sectionIndex}-${itemIndex}`" class="legal-edit-item">
                <el-input v-model="section.items[itemIndex]" maxlength="220" placeholder="条目内容" />
                <el-button type="danger" plain @click="removeSectionItem(section, itemIndex)">删除</el-button>
              </div>
              <el-button plain @click="addSectionItem(section)">新增条目</el-button>
            </template>
            <template v-else>
              <h3>{{ section.title }}</h3>
              <p v-if="section.text">{{ section.text }}</p>
              <ul v-if="section.items?.length">
                <li v-for="item in section.items" :key="item">{{ item }}</li>
              </ul>
            </template>
          </section>

          <div v-if="editMode" class="legal-edit-toolbar">
            <el-button type="primary" plain @click="addSection(currentPage)">新增内容段落</el-button>
          </div>

          <section v-if="editMode && canUploadImage(currentPage.type)" class="legal-section legal-image-editor">
            <h3>{{ legalImageTitle }}</h3>
            <el-input v-model="currentPage.qualificationTitle" maxlength="60" placeholder="图片区域标题" />
            <div class="legal-image-editor__row">
              <figure v-if="currentPage.imageUrl" class="qualification-image" :class="{ compact: currentPage.type === 'support-feedback' }">
                <img :src="getFooterAssetUrl(currentPage.imageUrl)" :alt="legalImageTitle" />
              </figure>
              <el-upload :show-file-list="false" :http-request="options => uploadLegalPageImage(options, currentPage)" :before-upload="beforeImageUpload">
                <el-button type="primary" plain :loading="uploading">上传图片</el-button>
              </el-upload>
            </div>
            <el-input v-model="currentPage.imageUrl" placeholder="图片地址，也可以点击上传自动生成" />
          </section>

          <section v-else-if="currentQualificationImage" class="legal-section">
            <h3>{{ legalImageTitle }}</h3>
            <figure class="qualification-image" :class="{ compact: currentPage.type === 'support-feedback' }">
              <img :src="getFooterAssetUrl(currentQualificationImage)" :alt="legalImageTitle" />
            </figure>
          </section>

          <div v-if="editMode" class="legal-section">
            <h3>温馨提示</h3>
            <el-input v-model="currentPage.notice" type="textarea" :rows="3" maxlength="500" show-word-limit placeholder="页面底部提示" />
          </div>

          <div v-else class="legal-notice">
            <strong>温馨提示</strong>
            <span>{{ currentPage.notice }}</span>
          </div>
        </div>
      </article>
    </section>
  </main>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getPublicFooterConfig, saveFooterConfig } from '@/api/siteFooter'
import { useUserStore } from '@/store/user'
import request from '@/utils/request'
import { useSiteAssets, getAssetUrl } from '@/utils/siteAssets'
import noImage from '@/assets/images/no-image.png'
import { getSupportedImageMessage, isSupportedImageFile } from '@/utils/imageCompression'

const route = useRoute()
const userStore = useUserStore()
const baseAPI = process.env.VUE_APP_BASE_API || '/api'
const footerConfig = ref({})
const editableLegalPages = ref([])
const editMode = ref(false)
const saving = ref(false)
const uploading = ref(false)
const { siteAssets, loadSiteAssets } = useSiteAssets()
const legalHeroStyle = computed(() => ({
  '--legal-bg-url': `url("${getAssetUrl(siteAssets.value.legalHeroUrl, noImage)}")`
}))

const legalPages = [
  {
    type: 'business-license',
    title: '营业执照',
    qualificationTitle: '营业执照资质图片',
    navText: '企业主体与经营信息',
    kicker: '企业资质',
    heading: '重庆侠客行国际旅行社有限公司主体说明',
    summary: '展示企业主体、经营范围与资质核验方式，方便用户识别平台服务主体。',
    lead: '侠客行国旅依法开展旅行咨询、线路组织、订单服务与售后协助等相关业务。平台展示的企业信息用于帮助用户了解服务主体，具体登记信息以市场监督管理部门公示为准。',
    sections: [
      {
        title: '主体信息',
        items: [
          '企业名称：重庆侠客行国际旅行社有限公司',
          '品牌简称：侠客行国旅',
          '服务定位：重庆及西南目的地品质旅行服务、团队出行与定制咨询',
          '总部地址：重庆市渝中区解放碑商圈时代旅行中心 18F'
        ]
      },
      {
        title: '核验方式',
        text: '用户可通过国家企业信用信息公示系统、地方市场监督管理部门公开渠道，核验企业登记状态、统一社会信用代码、经营范围和行政许可信息。'
      },
      {
        title: '信息更新',
        text: '如企业主体信息、办公地址、联系方式或经营范围发生调整，平台将在后台页脚配置及相关说明页中同步更新。'
      }
    ],
    notice: '如需查看营业执照扫描件或进一步核验主体信息，可联系平台客服或通过官方公开渠道查询。'
  },
  {
    type: 'travel-license',
    title: '旅行社业务经营许可',
    qualificationTitle: '旅行社业务经营许可资质图片',
    navText: '旅行社许可与业务边界',
    kicker: '经营许可',
    heading: '旅行社业务经营许可说明',
    summary: '说明旅行社业务经营许可、服务范围、订单确认与履约边界。',
    lead: '平台围绕旅行社业务许可范围提供线路咨询、行程组织、产品预订与出行服务。涉及交通、住宿、景区、邮轮、保险等第三方服务时，以订单确认信息、供应商规则和目的地实时政策为准。',
    sections: [
      {
        title: '许可信息',
        items: [
          '许可编号：L-CQ-XXK-2026',
          '业务范围：境内旅游、入境旅游相关咨询与组织服务',
          '服务区域：以重庆、三峡及西南目的地为核心，覆盖平台实际发布线路'
        ]
      },
      {
        title: '订单确认',
        text: '用户提交订单后，平台将根据库存、成团情况、供应商确认、实名信息和支付状态进行确认。未完成确认前，页面展示内容不视为最终履约承诺。'
      },
      {
        title: '出行资料',
        text: '出行前请仔细核对订单、集合时间、行程通知、证件要求、目的地天气和当地实时管理政策。'
      }
    ],
    notice: '线路页面、攻略和图片仅作服务说明，最终安排以合同、订单确认单和出行通知为准。'
  },
  {
    type: 'service-standard',
    title: '服务规范',
    navText: '预订、履约与售后标准',
    kicker: '服务标准',
    heading: '侠客行国旅服务规范',
    summary: '明确咨询、预订、出行、变更、退款和售后处理的基本流程。',
    lead: '我们希望每一次旅行服务都清楚、可追踪、可沟通。平台通过订单记录、客服跟进和出行通知，帮助用户理解服务进度与权责边界。',
    sections: [
      {
        title: '咨询与预订',
        items: [
          '展示线路亮点、价格说明、费用包含与不包含项目',
          '重要限制条件、年龄要求、证件要求和取消规则应在下单前提示',
          '客服或旅行顾问应基于用户需求提供真实、完整、不过度承诺的建议'
        ]
      },
      {
        title: '出行履约',
        items: [
          '出行前发送集合信息、联系人、注意事项和必要材料提醒',
          '行程中如遇天气、交通、景区管控等不可控因素，将优先保障人身安全并协调替代方案',
          '团队服务应尊重当地风俗、公共秩序和生态环境'
        ]
      },
      {
        title: '售后与争议',
        text: '用户可通过在线服务与投诉反馈专区、客服电话或订单售后入口提交问题。平台会根据订单证据、供应商规则和法律法规进行处理。'
      }
    ],
    notice: '涉及退改、赔付或补偿事项，以订单规则、合同约定及实际责任认定为准。'
  },
  {
    type: 'community-guidelines',
    title: '社区公约',
    navText: '评价、攻略与互动规则',
    kicker: '社区秩序',
    heading: '侠客行社区公约',
    summary: '维护真实、友好、有帮助的旅行内容环境。',
    lead: '平台鼓励用户分享真实体验、实用攻略和建设性建议，也会保护旅行者、商家、导游和目的地社区的合法权益。',
    sections: [
      {
        title: '鼓励发布',
        items: [
          '真实旅行经历、实用路线建议、避坑提醒和消费体验',
          '清晰的图片、时间、交通、住宿、餐饮和目的地信息',
          '对服务改进有帮助的理性反馈'
        ]
      },
      {
        title: '禁止内容',
        items: [
          '虚假评价、刷单引流、恶意诋毁或冒充他人',
          '泄露他人隐私、证件、联系方式、订单信息或未成年人个人信息',
          '违法违规、低俗辱骂、歧视攻击、危险行为引导或破坏公共秩序的内容'
        ]
      },
      {
        title: '处理方式',
        text: '平台可对违规内容采取提醒修改、限制展示、删除、账号限制、提交主管部门或配合司法机关处理等措施。'
      }
    ],
    notice: '如你认为内容被误判，可通过意见反馈或客服渠道提交申诉材料。'
  },
  {
    type: 'privacy-safety',
    title: '安全与隐私保护',
    navText: '数据、账号与未成年人保护',
    kicker: '隐私安全',
    heading: '安全与隐私保护说明',
    summary: '说明平台如何保护账号安全、订单信息、联系方式与未成年人信息。',
    lead: '平台仅在实现注册登录、订单履约、客户服务、风险控制和法律合规所必需的范围内处理用户信息，并采取合理措施保护数据安全。',
    sections: [
      {
        title: '信息使用范围',
        items: [
          '账号注册、短信验证、身份识别和登录安全',
          '订单预订、出行通知、客服沟通、发票或合同处理',
          '投诉处理、风险识别、内容审核和法律法规要求的保存'
        ]
      },
      {
        title: '安全措施',
        items: [
          '重要操作采用验证码、权限控制或登录态校验',
          '后台管理按角色控制访问范围，减少非必要信息暴露',
          '敏感信息处理遵循最小必要原则，避免无关使用和扩散'
        ]
      },
      {
        title: '未成年人保护',
        text: '未成年人使用平台服务应取得监护人同意。涉及未成年人订单、证件、联系方式、照片或出行安全事项时，平台将以更审慎方式处理。'
      }
    ],
    notice: '隐私或未成年人信息相关问题可联系 safe@xkxtrip.com；账号和订单服务可联系 service@xkxtrip.com。'
  },
  {
    type: 'support-feedback',
    title: '在线服务与投诉反馈专区',
    navText: '客服、投诉与监督渠道',
    kicker: '服务反馈',
    heading: '在线服务与投诉反馈专区',
    summary: '提供咨询、售后、投诉、监督和紧急问题处理渠道。',
    qualificationTitle: '联系图片',
    imageUrl: '',
    lead: '如果你在咨询、预订、支付、出行或售后过程中遇到问题，可以通过平台客服、电话、邮箱或订单记录提交反馈。我们会根据问题类型分级处理。',
    sections: [
      {
        title: '联系方式',
        items: [
          '服务邮箱：service@xkxtrip.com',
          '未成年人信息保护邮箱：safe@xkxtrip.com',
          '旅行咨询电话：400-800-5178',
          '团队与邮轮咨询：023-6789-5178',
          '投诉与服务监督：12345 / 023-6789-5178'
        ]
      },
      {
        title: '处理流程',
        items: [
          '普通咨询：按服务时间尽快响应',
          '订单售后：结合订单号、支付记录、出行凭证和沟通记录核查',
          '投诉监督：由专人复核并给出处理意见或进一步沟通安排'
        ]
      },
      {
        title: '提交建议',
        text: '为了提高处理效率，请尽量提供订单号、手机号后四位、出行日期、问题描述、截图或相关凭证。'
      }
    ],
    notice: '服务时间为 09:00 - 20:00，节假日专人在线；紧急出行问题将优先处理。'
  }
]

const currentType = computed(() => route.params.type || 'business-license')
const canEdit = computed(() => userStore.hasPermission('site-footer:manage'))
const persistedLegalPages = computed(() => footerConfig.value.legalPages?.length ? footerConfig.value.legalPages : legalPages)
const displayedLegalPages = computed(() => editMode.value ? editableLegalPages.value : persistedLegalPages.value)
const currentPage = computed(() => displayedLegalPages.value.find(item => item.type === currentType.value) || displayedLegalPages.value[0])
const currentQualificationImage = computed(() => currentPage.value?.imageUrl || '')
const legalImageTitle = computed(() => currentPage.value?.qualificationTitle || (currentPage.value?.type === 'support-feedback' ? '联系图片' : '资质图片'))

const cloneLegalPages = (pages = []) => JSON.parse(JSON.stringify(pages))
const canUploadImage = (type) => ['business-license', 'travel-license', 'support-feedback'].includes(type)

const normalizeLegalPages = (pages = []) => pages.map(page => ({
  ...page,
  qualificationTitle: page.qualificationTitle || (page.type === 'support-feedback' ? '联系图片' : ''),
  sections: (page.sections || [])
    .filter(section => section.title || section.text || section.items?.some(Boolean))
    .map(section => ({
      ...section,
      items: (section.items || []).filter(Boolean)
    }))
}))

const startEdit = () => {
  if (!canEdit.value) return
  editableLegalPages.value = cloneLegalPages(persistedLegalPages.value)
  editMode.value = true
}

const cancelEdit = () => {
  editableLegalPages.value = []
  editMode.value = false
}

const addSection = (page) => {
  page.sections = page.sections || []
  page.sections.push({ title: '', text: '', items: [] })
}

const removeSection = (page, index) => {
  page.sections.splice(index, 1)
}

const addSectionItem = (section) => {
  section.items = section.items || []
  section.items.push('')
}

const removeSectionItem = (section, index) => {
  section.items.splice(index, 1)
}

const beforeImageUpload = (file) => {
  if (!isSupportedImageFile(file)) {
    ElMessage.error(getSupportedImageMessage())
    return false
  }
  return true
}

const uploadLegalPageImage = async (options, page) => {
  const formData = new FormData()
  formData.append('file', options.file)
  uploading.value = true
  try {
    page.imageUrl = await request.upload('/file/upload/img', formData, { showDefaultMsg: false })
    ElMessage.success('图片上传成功')
  } catch (error) {
    ElMessage.error('图片上传失败')
  } finally {
    uploading.value = false
  }
}

const saveLegalPages = async () => {
  if (!canEdit.value) return
  const legalPagesPayload = normalizeLegalPages(editableLegalPages.value)
  const businessPage = legalPagesPayload.find(item => item.type === 'business-license')
  const travelPage = legalPagesPayload.find(item => item.type === 'travel-license')
  saving.value = true
  try {
    await saveFooterConfig({
      ...footerConfig.value,
      legalPages: legalPagesPayload,
      businessLicenseImageUrl: businessPage?.imageUrl || '',
      travelLicenseImageUrl: travelPage?.imageUrl || ''
    }, { showDefaultMsg: false })
    footerConfig.value = await getPublicFooterConfig() || {}
    editableLegalPages.value = []
    editMode.value = false
    ElMessage.success('页面内容已保存')
  } finally {
    saving.value = false
  }
}

const getFooterAssetUrl = (url) => {
  if (!url) return noImage
  if (/^(https?:|data:|blob:)/.test(url)) return url
  if (url.startsWith('/img/') || url.startsWith('/assets/')) return url
  return url.startsWith('/') ? `${baseAPI}${url}` : url
}

const loadFooterConfig = async () => {
  try {
    footerConfig.value = await getPublicFooterConfig() || {}
  } catch (error) {
    footerConfig.value = {}
  }
}

onMounted(loadFooterConfig)
onMounted(loadSiteAssets)
</script>

<style scoped>
.legal-page {
  min-height: 100vh;
  background: #f5f7fb;
  color: #17233d;
}

.legal-hero {
  background:
    linear-gradient(135deg, rgba(15, 34, 63, 0.92), rgba(34, 77, 112, 0.82)),
    var(--legal-bg-url) center/cover;
  color: #fff;
  padding: 72px 24px 64px;
}

.legal-hero__content {
  width: min(1120px, 100%);
  margin: 0 auto;
  text-align: center;
}

.legal-hero p,
.legal-kicker {
  margin: 0 0 10px;
  color: #d3a751;
  font-size: 13px;
  font-weight: 800;
  letter-spacing: 0;
  text-transform: uppercase;
}

.legal-hero h1 {
  margin: 0;
  font-size: 38px;
  line-height: 1.2;
}

.legal-hero span {
  display: block;
  max-width: 720px;
  margin: 14px auto 0;
  color: rgba(255, 255, 255, 0.82);
  font-size: 16px;
  line-height: 1.8;
}

.legal-shell {
  display: grid;
  grid-template-columns: 280px minmax(0, 1fr);
  gap: 24px;
  width: min(var(--frontend-container-safe-width), var(--frontend-container-readable));
  margin: -34px auto 72px;
  align-items: start;
}

.legal-admin-bar {
  grid-column: 1 / -1;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 14px 16px;
  border: 1px solid #d9e7f8;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 14px 34px rgba(22, 38, 62, 0.08);
}

.legal-admin-bar strong,
.legal-admin-bar span {
  display: block;
}

.legal-admin-bar strong {
  color: #17233d;
  font-size: 15px;
}

.legal-admin-bar span {
  margin-top: 3px;
  color: #6a7890;
  font-size: 13px;
}

.legal-admin-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.legal-nav,
.legal-card {
  border: 1px solid #e6ebf2;
  background: #fff;
  box-shadow: 0 18px 45px rgba(22, 38, 62, 0.08);
}

.legal-nav {
  position: sticky;
  top: 88px;
  display: grid;
  gap: 4px;
  padding: 10px;
  border-radius: 8px;
}

.legal-nav__item {
  display: grid;
  gap: 4px;
  padding: 13px 14px;
  border-radius: 7px;
  color: #4a5870;
  text-decoration: none;
  transition: background 0.2s ease, color 0.2s ease;
}

.legal-nav__item strong {
  font-size: 14px;
}

.legal-nav__item span {
  font-size: 12px;
  color: #8190a5;
}

.legal-nav__item:hover,
.legal-nav__item.active {
  background: #f0f6ff;
  color: #1f6fd1;
}

.legal-card {
  border-radius: 8px;
  padding: 34px;
}

.legal-card :deep(.el-input),
.legal-card :deep(.el-textarea) {
  margin-bottom: 12px;
}

.legal-edit-title :deep(.el-input__wrapper),
.legal-edit-heading :deep(.el-input__wrapper) {
  min-height: 48px;
}

.legal-card h2 {
  margin: 0;
  font-size: 28px;
  line-height: 1.3;
}

.legal-lead {
  margin: 14px 0 28px;
  color: #5d6b80;
  font-size: 15px;
  line-height: 1.9;
}

.legal-section {
  padding: 24px 0;
  border-top: 1px solid #edf1f6;
}

.legal-section h3 {
  margin: 0 0 12px;
  font-size: 18px;
}

.legal-section p,
.legal-section li {
  color: #536176;
  font-size: 15px;
  line-height: 1.9;
}

.legal-section ul {
  display: grid;
  gap: 8px;
  margin: 0;
  padding-left: 18px;
}

.legal-edit-section-head,
.legal-edit-item,
.legal-image-editor__row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 12px;
  align-items: start;
}

.legal-edit-item {
  margin-bottom: 10px;
}

.legal-edit-toolbar {
  padding-top: 16px;
  border-top: 1px solid #edf1f6;
}

.legal-image-editor {
  display: grid;
  gap: 12px;
}

.legal-image-editor__row {
  align-items: end;
}

.qualification-image {
  margin: 0;
  border: 1px solid #e6ebf2;
  border-radius: 8px;
  background: #f8fafc;
  overflow: hidden;
}

.qualification-image img {
  display: block;
  width: 100%;
  max-height: 720px;
  object-fit: contain;
  background: #fff;
}

.qualification-image.compact {
  width: min(360px, 100%);
  margin-left: auto;
  margin-right: auto;
}

.qualification-image.compact img {
  max-height: 260px;
}

.legal-notice {
  display: grid;
  gap: 6px;
  margin-top: 12px;
  padding: 16px 18px;
  border-radius: 8px;
  background: #fff8e8;
  color: #6f5521;
  line-height: 1.8;
}

.legal-notice strong {
  color: #9a6a00;
}

@media (max-width: 860px) {
  .legal-hero {
    padding: 56px 18px 54px;
  }

  .legal-hero h1 {
    font-size: 30px;
  }

  .legal-shell {
    grid-template-columns: 1fr;
    width: min(100% - 28px, 680px);
    margin-bottom: 48px;
  }

  .legal-nav {
    position: static;
  }

  .legal-admin-bar,
  .legal-admin-actions,
  .legal-edit-section-head,
  .legal-edit-item,
  .legal-image-editor__row {
    grid-template-columns: 1fr;
  }

  .legal-admin-bar {
    align-items: stretch;
  }

  .legal-card {
    padding: 24px 18px;
  }
}
</style>
