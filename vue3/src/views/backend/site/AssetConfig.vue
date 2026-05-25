<template>
  <div class="asset-config-page">
    <section class="page-hero">
      <div>
        <p>Site Assets</p>
        <h1>站点素材配置</h1>
        <span>前台品牌、二维码、背景图和默认占位图</span>
      </div>
      <el-button type="primary" size="large" :loading="saving" @click="handleSave">保存配置</el-button>
    </section>

    <section class="asset-grid" v-loading="loading">
      <article v-for="item in assetItems" :key="item.key" class="asset-card">
        <div class="asset-preview">
          <el-image
            v-if="form[item.key]"
            :src="getImageUrl(form[item.key])"
            :alt="item.title"
            fit="contain"
            :preview-src-list="[getImageUrl(form[item.key])]"
            preview-teleported
          />
          <span v-else>未配置</span>
        </div>
        <div class="asset-body">
          <strong>{{ item.title }}</strong>
          <p>{{ item.desc }}</p>
          <el-input v-model="form[item.key]" placeholder="上传后自动填充，也可填写 /img/ 或 HTTPS 图片地址" />
          <div class="asset-actions">
            <el-upload :show-file-list="false" :http-request="options => uploadImage(options, item.key)" :before-upload="beforeImageUpload">
              <el-button type="primary" plain :loading="uploadingKey === item.key">上传图片</el-button>
            </el-upload>
            <el-button plain @click="form[item.key] = ''">清空</el-button>
          </div>
        </div>
      </article>
    </section>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'
import { getSiteAssets, saveSiteAssets } from '@/api/siteAssets'
import { getAssetUrl } from '@/utils/siteAssets'

const loading = ref(false)
const saving = ref(false)
const uploadingKey = ref('')

const form = reactive({
  faviconUrl: '',
  logoUrl: '',
  wechatQrUrl: '',
  authBackgroundUrl: '',
  aboutHeroUrl: '',
  legalHeroUrl: '',
  accommodationHeroUrl: '',
  scenicHeroUrl: '',
  guideHeroUrl: '',
  placeholderImageUrl: ''
})

const assetItems = [
  { key: 'faviconUrl', title: '网站 Favicon', desc: '浏览器标签页和收藏夹图标。' },
  { key: 'logoUrl', title: '品牌 Logo', desc: '前台顶部导航和后台侧边栏使用。' },
  { key: 'wechatQrUrl', title: '微信二维码', desc: '前台顶部微信浮层和页脚二维码的默认素材。' },
  { key: 'authBackgroundUrl', title: '登录注册背景', desc: '登录、注册等认证页面背景图。' },
  { key: 'aboutHeroUrl', title: '关于我们头图', desc: '关于页面首屏展示图片。' },
  { key: 'legalHeroUrl', title: '合规页面背景', desc: '营业执照、隐私安全等合规说明页面头图。' },
  { key: 'accommodationHeroUrl', title: '住宿列表头图', desc: '住宿列表页搜索首屏背景。' },
  { key: 'scenicHeroUrl', title: '景点列表头图', desc: '景点列表页搜索首屏背景。' },
  { key: 'guideHeroUrl', title: '攻略列表头图', desc: '攻略列表页搜索首屏背景。' },
  { key: 'placeholderImageUrl', title: '默认占位图', desc: '住宿等业务数据缺少图片时显示。' }
]

const loadConfig = async () => {
  loading.value = true
  try {
    Object.assign(form, await getSiteAssets() || {})
  } finally {
    loading.value = false
  }
}

const beforeImageUpload = (file) => {
  const validType = ['image/jpeg', 'image/png', 'image/webp', 'image/gif', 'image/x-icon', 'image/vnd.microsoft.icon'].includes(file.type)
  const validSize = file.size / 1024 / 1024 < 10
  if (!validType) {
    ElMessage.error('仅支持 JPG、PNG、WebP、GIF、ICO 图片')
    return false
  }
  if (!validSize) {
    ElMessage.error('图片大小不能超过 10MB')
    return false
  }
  return true
}

const uploadImage = async (options, key) => {
  const formData = new FormData()
  formData.append('file', options.file)
  uploadingKey.value = key
  try {
    form[key] = await request.upload('/file/upload/img', formData, { showDefaultMsg: false })
    ElMessage.success('图片上传成功')
  } finally {
    uploadingKey.value = ''
  }
}

const handleSave = async () => {
  saving.value = true
  try {
    await saveSiteAssets({ ...form }, { showDefaultMsg: false })
    ElMessage.success('站点素材配置已保存')
  } finally {
    saving.value = false
  }
}

const getImageUrl = url => getAssetUrl(url)

onMounted(loadConfig)
</script>

<style lang="scss" scoped>
.asset-config-page {
  min-height: 100%;
  padding: 24px;
  background: #f6f8fb;
  color: #172033;
}

.page-hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  padding: 24px 28px;
  margin-bottom: 20px;
  border-radius: 12px;
  background: #ffffff;
  box-shadow: 0 10px 24px rgba(31, 45, 61, 0.07);

  p {
    margin: 0 0 8px;
    color: #1f7ae0;
    font-size: 13px;
    font-weight: 800;
  }

  h1 {
    margin: 0;
    font-size: 26px;
    font-weight: 900;
  }

  span {
    display: block;
    margin-top: 10px;
    color: #64748b;
  }
}

.asset-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(420px, 1fr));
  gap: 16px;
}

.asset-card {
  display: grid;
  grid-template-columns: 220px minmax(0, 1fr);
  gap: 16px;
  padding: 16px;
  border-radius: 12px;
  background: #fff;
  box-shadow: 0 10px 24px rgba(31, 45, 61, 0.07);
}

.asset-preview {
  width: 220px;
  height: 150px;
  display: grid;
  place-items: center;
  border: 1px dashed #b7c6d9;
  border-radius: 8px;
  background:
    linear-gradient(45deg, #f1f5f9 25%, transparent 25%),
    linear-gradient(-45deg, #f1f5f9 25%, transparent 25%),
    linear-gradient(45deg, transparent 75%, #f1f5f9 75%),
    linear-gradient(-45deg, transparent 75%, #f1f5f9 75%);
  background-color: #ffffff;
  background-position: 0 0, 0 8px, 8px -8px, -8px 0;
  background-size: 16px 16px;
  color: #64748b;
  font-size: 13px;

  :deep(.el-image) {
    width: 100%;
    height: 100%;
    border-radius: 8px;
  }

  :deep(img) {
    width: 100%;
    height: 100%;
    object-fit: contain;
  }
}

.asset-body {
  min-width: 0;
  display: grid;
  gap: 10px;

  strong {
    font-size: 16px;
  }

  p {
    margin: 0;
    color: #64748b;
    line-height: 1.6;
  }
}

.asset-actions {
  display: flex;
  gap: 10px;
}

@media (max-width: 720px) {
  .page-hero,
  .asset-card {
    grid-template-columns: 1fr;
  }

  .page-hero {
    flex-direction: column;
    align-items: flex-start;
  }

  .asset-preview {
    width: 100%;
    height: 180px;
  }
}
</style>
