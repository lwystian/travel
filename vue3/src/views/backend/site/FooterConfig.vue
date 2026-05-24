<template>
  <div class="footer-config-page">
    <section class="page-hero">
      <div>
        <p>Site Footer</p>
        <h1>网站页脚配置</h1>
        <span>维护页脚展示状态、企业信息、备案链接、导航链接、二维码、证书和法律说明。</span>
      </div>
      <el-button type="primary" size="large" :loading="saving" @click="handleSave">
        保存配置
      </el-button>
    </section>

    <el-form ref="formRef" :model="form" label-position="top" class="config-form" v-loading="loading">
      <el-card class="config-card" shadow="never">
        <template #header>
          <div class="card-title">
            <el-icon><OfficeBuilding /></el-icon>
            <span>基础信息</span>
          </div>
        </template>

        <el-row :gutter="18">
          <el-col :xs="24">
            <el-form-item label="前台展示状态">
              <el-switch
                v-model="form.enabled"
                active-text="启用网站页脚"
                inactive-text="不展示"
                inline-prompt
              />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :md="12">
            <el-form-item label="企业名称">
              <el-input v-model="form.companyName" maxlength="80" show-word-limit />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :md="12">
            <el-form-item label="品牌名称">
              <el-input v-model="form.brandName" maxlength="40" show-word-limit />
            </el-form-item>
          </el-col>
          <el-col :xs="24">
            <el-form-item label="页脚标语">
              <el-input v-model="form.slogan" maxlength="120" show-word-limit />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :md="12">
            <el-form-item label="旅行咨询电话">
              <el-input v-model="form.consultationPhone" maxlength="40" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :md="12">
            <el-form-item label="团队与邮轮咨询">
              <el-input v-model="form.cruisePhone" maxlength="40" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :md="12">
            <el-form-item label="服务时间">
              <el-input v-model="form.serviceTime" maxlength="80" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :md="12">
            <el-form-item label="公司地址">
              <el-input v-model="form.address" maxlength="180" show-word-limit />
            </el-form-item>
          </el-col>
        </el-row>
      </el-card>

      <el-card class="config-card" shadow="never">
        <template #header>
          <div class="card-title">
            <el-icon><Checked /></el-icon>
            <span>备案与合规</span>
          </div>
        </template>

        <el-row :gutter="18">
          <el-col :xs="24" :md="12">
            <el-form-item label="ICP备案号">
              <el-input v-model="form.icpNumber" maxlength="80" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :md="12">
            <el-form-item label="ICP备案链接">
              <el-input v-model="form.icpUrl" maxlength="300" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :md="12">
            <el-form-item label="公安备案号">
              <el-input v-model="form.policeNumber" maxlength="80" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :md="12">
            <el-form-item label="公安备案链接">
              <el-input v-model="form.policeUrl" maxlength="300" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :md="12">
            <el-form-item label="许可证号">
              <el-input v-model="form.licenseNumber" maxlength="80" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :md="12">
            <el-form-item label="投诉与服务监督">
              <el-input v-model="form.complaintPhone" maxlength="40" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :md="12">
            <el-form-item label="法律与技术支持">
              <el-input v-model="form.technicalSupport" maxlength="80" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :md="12">
            <el-form-item label="服务邮箱">
              <el-input v-model="form.reportEmail" maxlength="80" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :md="12">
            <el-form-item label="未成年人保护邮箱">
              <el-input v-model="form.minorReportEmail" maxlength="80" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :md="12">
            <el-form-item label="版权信息">
              <el-input v-model="form.copyright" maxlength="120" show-word-limit />
            </el-form-item>
          </el-col>
        </el-row>

        <div class="dynamic-header">
          <span>版权与免责声明</span>
          <el-button text type="primary" @click="addLegalNote">新增说明</el-button>
        </div>
        <div v-for="(note, index) in form.legalNotes" :key="`note-${index}`" class="line-editor">
          <el-input v-model="form.legalNotes[index]" maxlength="180" show-word-limit />
          <el-button :icon="Delete" circle plain type="danger" @click="removeItem(form.legalNotes, index)" />
        </div>
      </el-card>

      <el-card class="config-card" shadow="never">
        <template #header>
          <div class="card-title">
            <el-icon><Medal /></el-icon>
            <span>服务能力展示</span>
          </div>
        </template>

        <div class="dynamic-header">
          <span>页脚服务能力</span>
          <el-button text type="primary" @click="addFeature">新增能力</el-button>
        </div>
        <div v-for="(item, index) in form.featureItems" :key="`feature-${index}`" class="feature-editor">
          <el-input v-model="item.title" placeholder="标题" maxlength="20" />
          <el-select v-model="item.icon" placeholder="图标" class="icon-select">
            <el-option label="守护" value="shield" />
            <el-option label="路线" value="route" />
            <el-option label="服务" value="service" />
            <el-option label="体验" value="experience" />
            <el-option label="自然" value="leaf" />
            <el-option label="分享" value="share" />
          </el-select>
          <el-input v-model="item.description" placeholder="说明" maxlength="80" show-word-limit />
          <el-button :icon="Delete" circle plain type="danger" @click="removeItem(form.featureItems, index)" />
        </div>
      </el-card>

      <el-card class="config-card" shadow="never">
        <template #header>
          <div class="card-title">
            <el-icon><Link /></el-icon>
            <span>导航与链接</span>
          </div>
        </template>

        <div class="dynamic-header">
          <span>页脚导航</span>
          <el-button text type="primary" @click="addLink('topLinks')">新增链接</el-button>
        </div>
        <div v-for="(link, index) in form.topLinks" :key="`top-${index}`" class="link-editor">
          <el-input v-model="link.label" placeholder="名称" maxlength="30" />
          <el-input v-model="link.url" placeholder="链接" maxlength="300" />
          <el-button :icon="Delete" circle plain type="danger" @click="removeItem(form.topLinks, index)" />
        </div>

        <div class="dynamic-header">
          <span>合规说明链接</span>
          <el-button text type="primary" @click="addLink('complianceLinks')">新增说明页</el-button>
        </div>
        <div v-for="(link, index) in form.complianceLinks" :key="`compliance-${index}`" class="link-editor">
          <el-input v-model="link.label" placeholder="名称" maxlength="30" />
          <el-input v-model="link.url" placeholder="链接" maxlength="300" />
          <el-button :icon="Delete" circle plain type="danger" @click="removeItem(form.complianceLinks, index)" />
        </div>

        <div class="dynamic-header">
          <span>友情链接</span>
          <el-button text type="primary" @click="addLink('friendlyLinks')">新增友情链接</el-button>
        </div>
        <div v-for="(link, index) in form.friendlyLinks" :key="`friend-${index}`" class="link-editor">
          <el-input v-model="link.label" placeholder="名称" maxlength="30" />
          <el-input v-model="link.url" placeholder="链接" maxlength="300" />
          <el-button :icon="Delete" circle plain type="danger" @click="removeItem(form.friendlyLinks, index)" />
        </div>
      </el-card>

      <el-card class="config-card" shadow="never">
        <template #header>
          <div class="card-title">
            <el-icon><Picture /></el-icon>
            <span>二维码与证书</span>
          </div>
        </template>

        <div class="dynamic-header">
          <span>官方二维码</span>
          <el-button text type="primary" @click="addQrCode">新增二维码</el-button>
        </div>
        <div v-for="(item, index) in form.qrCodes" :key="`qr-${index}`" class="media-editor">
          <div class="media-preview">
            <img v-if="item.imageUrl" :src="getImageUrl(item.imageUrl)" alt="" />
            <span v-else>QR</span>
          </div>
          <div class="media-fields">
            <el-input v-model="item.label" placeholder="名称" maxlength="30" />
            <el-input v-model="item.description" placeholder="说明" maxlength="60" />
            <el-input v-model="item.imageUrl" placeholder="图片路径" maxlength="300" />
          </div>
          <el-upload :show-file-list="false" :http-request="(options) => uploadImage(options, item)" :before-upload="beforeImageUpload">
            <el-button :icon="UploadFilled" circle />
          </el-upload>
          <el-button :icon="Delete" circle plain type="danger" @click="removeItem(form.qrCodes, index)" />
        </div>

        <div class="dynamic-header">
          <span>荣誉证书</span>
          <el-button text type="primary" @click="addCertificate">新增证书</el-button>
        </div>
        <div v-for="(item, index) in form.certificates" :key="`cert-${index}`" class="media-editor">
          <div class="media-preview">
            <img v-if="item.imageUrl" :src="getImageUrl(item.imageUrl)" alt="" />
            <span v-else>CERT</span>
          </div>
          <div class="media-fields">
            <el-input v-model="item.title" placeholder="标题" maxlength="40" />
            <el-input v-model="item.description" placeholder="说明" maxlength="80" />
            <el-input v-model="item.imageUrl" placeholder="图片路径" maxlength="300" />
          </div>
          <el-upload :show-file-list="false" :http-request="(options) => uploadImage(options, item)" :before-upload="beforeImageUpload">
            <el-button :icon="UploadFilled" circle />
          </el-upload>
          <el-button :icon="Delete" circle plain type="danger" @click="removeItem(form.certificates, index)" />
        </div>
      </el-card>
    </el-form>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Checked, Delete, Link, Medal, OfficeBuilding, Picture, UploadFilled } from '@element-plus/icons-vue'
import { getFooterConfig, saveFooterConfig } from '@/api/siteFooter'
import request from '@/utils/request'
import noImage from '@/assets/images/no-image.png'

const baseAPI = process.env.VUE_APP_BASE_API || '/api'
const loading = ref(false)
const saving = ref(false)
const formRef = ref(null)

const defaultForm = () => ({
  enabled: false,
  companyName: '',
  brandName: '',
  slogan: '',
  consultationPhone: '',
  cruisePhone: '',
  serviceTime: '',
  address: '',
  copyright: '',
  icpNumber: '',
  icpUrl: '',
  policeNumber: '',
  policeUrl: '',
  licenseNumber: '',
  complaintPhone: '',
  technicalSupport: '',
  reportEmail: '',
  minorReportEmail: '',
  businessLicenseImageUrl: '',
  travelLicenseImageUrl: '',
  fieldLabels: {},
  featureItems: [],
  topLinks: [],
  complianceLinks: [],
  friendlyLinks: [],
  qrCodes: [],
  certificates: [],
  legalPages: [],
  legalNotes: []
})

const form = reactive(defaultForm())

const asArray = value => Array.isArray(value) ? value : []

const assignForm = (data = {}) => {
  Object.assign(form, defaultForm(), data)
  form.fieldLabels = data.fieldLabels || {}
  form.featureItems = asArray(data.featureItems)
  form.topLinks = asArray(data.topLinks)
  form.complianceLinks = asArray(data.complianceLinks)
  form.friendlyLinks = asArray(data.friendlyLinks)
  form.qrCodes = asArray(data.qrCodes)
  form.certificates = asArray(data.certificates)
  form.legalPages = asArray(data.legalPages)
  form.legalNotes = asArray(data.legalNotes)
}

const fetchConfig = async () => {
  loading.value = true
  try {
    const data = await getFooterConfig()
    assignForm(data || {})
  } finally {
    loading.value = false
  }
}

const addLink = field => {
  form[field].push({ label: '', url: '' })
}

const addFeature = () => {
  form.featureItems.push({ title: '', description: '', icon: '' })
}

const addQrCode = () => {
  form.qrCodes.push({ label: '', imageUrl: '', description: '' })
}

const addCertificate = () => {
  form.certificates.push({ title: '', imageUrl: '', description: '' })
}

const addLegalNote = () => {
  form.legalNotes.push('')
}

const removeItem = (list, index) => {
  list.splice(index, 1)
}

const cleanLinks = links => asArray(links).filter(item => item && item.label)

const normalizePayload = () => ({
  ...form,
  featureItems: asArray(form.featureItems).filter(item => item && item.title),
  topLinks: cleanLinks(form.topLinks),
  complianceLinks: cleanLinks(form.complianceLinks),
  friendlyLinks: cleanLinks(form.friendlyLinks),
  qrCodes: asArray(form.qrCodes).filter(item => item && item.label),
  certificates: asArray(form.certificates).filter(item => item && item.title),
  legalPages: asArray(form.legalPages),
  legalNotes: asArray(form.legalNotes).filter(Boolean)
})

const handleSave = async () => {
  await formRef.value?.validate()
  saving.value = true
  try {
    await saveFooterConfig(normalizePayload(), { showDefaultMsg: false })
    ElMessage.success('网站页脚配置已保存')
    fetchConfig()
  } finally {
    saving.value = false
  }
}

const beforeImageUpload = file => {
  const validType = ['image/jpeg', 'image/png', 'image/webp'].includes(file.type)
  const validSize = file.size / 1024 / 1024 < 3
  if (!validType) {
    ElMessage.error('仅支持 JPG、PNG、WebP 图片')
    return false
  }
  if (!validSize) {
    ElMessage.error('图片大小不能超过 3MB')
    return false
  }
  return true
}

const uploadImage = async (options, target) => {
  const formData = new FormData()
  formData.append('file', options.file)
  try {
    const url = await request.upload('/file/upload/img', formData, { showDefaultMsg: false })
    target.imageUrl = url
    ElMessage.success('图片上传成功')
  } catch (error) {
    ElMessage.error('图片上传失败')
  }
}

const getImageUrl = url => {
  if (!url) return noImage
  return url.startsWith('http') ? url : baseAPI + url
}

onMounted(fetchConfig)
</script>

<style lang="scss" scoped>
.footer-config-page {
  padding: 24px;
  background: #f7f9fc;
  min-height: 100%;
}

.page-hero,
.config-card {
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  background: #fff;
}

.page-hero {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 20px;
  padding: 24px 28px;
  margin-bottom: 22px;
}

.page-hero p {
  margin: 0 0 8px;
  color: #64748b;
  font-weight: 700;
}

.page-hero h1 {
  margin: 0 0 8px;
  font-size: 26px;
}

.page-hero span {
  color: #64748b;
}

.config-form {
  display: grid;
  gap: 18px;
}

.card-title,
.dynamic-header,
.line-editor,
.feature-editor,
.link-editor,
.media-editor {
  display: flex;
  align-items: center;
  gap: 10px;
}

.card-title {
  font-weight: 700;
}

.dynamic-header {
  justify-content: space-between;
  margin: 16px 0 10px;
  font-weight: 700;
}

.line-editor,
.feature-editor,
.link-editor,
.media-editor {
  margin-bottom: 10px;
}

.feature-editor {
  display: grid;
  grid-template-columns: minmax(120px, 180px) 140px 1fr auto;
}

.link-editor {
  display: grid;
  grid-template-columns: minmax(160px, 240px) 1fr auto;
}

.media-editor {
  display: grid;
  grid-template-columns: 74px 1fr auto auto;
}

.media-preview {
  width: 64px;
  height: 64px;
  border: 1px dashed #cbd5e1;
  border-radius: 8px;
  display: grid;
  place-items: center;
  color: #94a3b8;
  overflow: hidden;
}

.media-preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.media-fields {
  display: grid;
  gap: 8px;
}

.icon-select {
  width: 140px;
}

@media (max-width: 768px) {
  .page-hero,
  .line-editor,
  .feature-editor,
  .link-editor,
  .media-editor {
    display: block;
  }

  .line-editor > *,
  .feature-editor > *,
  .link-editor > *,
  .media-editor > * {
    margin-bottom: 8px;
  }
}
</style>
