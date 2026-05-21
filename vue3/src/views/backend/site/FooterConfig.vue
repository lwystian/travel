<template>
  <div class="footer-config-page">
    <section class="page-hero">
      <div>
        <p>Site Footer</p>
        <h1>网站页脚配置</h1>
        <span>统一维护网站底部导航、备案合规、二维码、资质链接和版权说明。</span>
      </div>
      <el-button type="primary" size="large" :loading="saving" @click="handleSave">
        保存配置
      </el-button>
    </section>

    <div class="config-layout">
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" class="config-form">
        <el-card class="config-card" shadow="never">
          <template #header>
            <div class="card-title">
              <el-icon><OfficeBuilding /></el-icon>
              <span>基础信息</span>
            </div>
          </template>
          <el-row :gutter="18">
            <el-col :xs="24">
              <el-form-item>
                <template #label>
                  <el-input v-if="editingFieldLabel === 'enabled'" v-model="form.fieldLabels.enabled" class="label-edit-input" maxlength="30" @blur="finishFieldLabelEdit('enabled', '前台展示状态')" @keyup.enter="finishFieldLabelEdit('enabled', '前台展示状态')" />
                  <button v-else class="editable-label" type="button" @click="startFieldLabelEdit('enabled')">{{ getFieldLabel('enabled', '前台展示状态') }}</button>
                </template>
                <el-switch
                  v-model="form.enabled"
                  active-text="启用网站页脚"
                  inactive-text="暂不展示"
                  inline-prompt
                  style="--el-switch-on-color: #1f7ae0; --el-switch-off-color: #9aa8bb"
                />
              </el-form-item>
            </el-col>
            <el-col :xs="24" :md="12">
              <el-form-item prop="companyName">
                <template #label>
                  <el-input v-if="editingFieldLabel === 'companyName'" v-model="form.fieldLabels.companyName" class="label-edit-input" maxlength="30" @blur="finishFieldLabelEdit('companyName', '企业名称')" @keyup.enter="finishFieldLabelEdit('companyName', '企业名称')" />
                  <button v-else class="editable-label" type="button" @click="startFieldLabelEdit('companyName')">{{ getFieldLabel('companyName', '企业名称') }}</button>
                </template>
                <el-input v-model="form.companyName" maxlength="80" show-word-limit placeholder="请输入企业全称" />
              </el-form-item>
            </el-col>
            <el-col :xs="24" :md="12">
              <el-form-item prop="brandName">
                <template #label>
                  <el-input v-if="editingFieldLabel === 'brandName'" v-model="form.fieldLabels.brandName" class="label-edit-input" maxlength="30" @blur="finishFieldLabelEdit('brandName', '品牌名称')" @keyup.enter="finishFieldLabelEdit('brandName', '品牌名称')" />
                  <button v-else class="editable-label" type="button" @click="startFieldLabelEdit('brandName')">{{ getFieldLabel('brandName', '品牌名称') }}</button>
                </template>
                <el-input v-model="form.brandName" maxlength="40" show-word-limit placeholder="例如：侠客行国旅" />
              </el-form-item>
            </el-col>
            <el-col :xs="24">
              <el-form-item>
                <template #label>
                  <el-input v-if="editingFieldLabel === 'slogan'" v-model="form.fieldLabels.slogan" class="label-edit-input" maxlength="30" @blur="finishFieldLabelEdit('slogan', '页脚标语')" @keyup.enter="finishFieldLabelEdit('slogan', '页脚标语')" />
                  <button v-else class="editable-label" type="button" @click="startFieldLabelEdit('slogan')">{{ getFieldLabel('slogan', '页脚标语') }}</button>
                </template>
                <el-input v-model="form.slogan" maxlength="120" show-word-limit placeholder="请输入网站页脚展示标语" />
              </el-form-item>
            </el-col>
            <el-col :xs="24" :md="12">
              <el-form-item>
                <template #label>
                  <el-input v-if="editingFieldLabel === 'consultationPhone'" v-model="form.fieldLabels.consultationPhone" class="label-edit-input" maxlength="30" @blur="finishFieldLabelEdit('consultationPhone', '旅行咨询电话')" @keyup.enter="finishFieldLabelEdit('consultationPhone', '旅行咨询电话')" />
                  <button v-else class="editable-label" type="button" @click="startFieldLabelEdit('consultationPhone')">{{ getFieldLabel('consultationPhone', '旅行咨询电话') }}</button>
                </template>
                <el-input v-model="form.consultationPhone" maxlength="40" placeholder="例如：023-8691 5016" />
              </el-form-item>
            </el-col>
            <el-col :xs="24" :md="12">
              <el-form-item>
                <template #label>
                  <el-input v-if="editingFieldLabel === 'cruisePhone'" v-model="form.fieldLabels.cruisePhone" class="label-edit-input" maxlength="30" @blur="finishFieldLabelEdit('cruisePhone', '团队与邮轮咨询')" @keyup.enter="finishFieldLabelEdit('cruisePhone', '团队与邮轮咨询')" />
                  <button v-else class="editable-label" type="button" @click="startFieldLabelEdit('cruisePhone')">{{ getFieldLabel('cruisePhone', '团队与邮轮咨询') }}</button>
                </template>
                <el-input v-model="form.cruisePhone" maxlength="40" placeholder="例如：023-8691 5123" />
              </el-form-item>
            </el-col>
            <el-col :xs="24">
              <el-form-item>
                <template #label>
                  <el-input v-if="editingFieldLabel === 'serviceTime'" v-model="form.fieldLabels.serviceTime" class="label-edit-input" maxlength="30" @blur="finishFieldLabelEdit('serviceTime', '服务时间')" @keyup.enter="finishFieldLabelEdit('serviceTime', '服务时间')" />
                  <button v-else class="editable-label" type="button" @click="startFieldLabelEdit('serviceTime')">{{ getFieldLabel('serviceTime', '服务时间') }}</button>
                </template>
                <el-input v-model="form.serviceTime" maxlength="80" placeholder="例如：09:00 - 18:00，节假日专人值守" />
              </el-form-item>
            </el-col>
            <el-col :xs="24">
              <el-form-item>
                <template #label>
                  <el-input v-if="editingFieldLabel === 'address'" v-model="form.fieldLabels.address" class="label-edit-input" maxlength="30" @blur="finishFieldLabelEdit('address', '公司总部地址')" @keyup.enter="finishFieldLabelEdit('address', '公司总部地址')" />
                  <button v-else class="editable-label" type="button" @click="startFieldLabelEdit('address')">{{ getFieldLabel('address', '公司总部地址') }}</button>
                </template>
                <el-input v-model="form.address" maxlength="180" show-word-limit placeholder="请输入企业办公地址" />
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
          <el-alert
            class="config-tip"
            type="info"
            show-icon
            :closable="false"
            title="ICP备案号、公安备案号和备案跳转链接会展示在前台网站页脚最底部；官方二维码可在下方“二维码与荣誉证书”中上传。"
          />
          <el-row :gutter="18">
            <el-col :xs="24" :md="12">
              <el-form-item>
                <template #label>
                  <el-input v-if="editingFieldLabel === 'icpNumber'" v-model="form.fieldLabels.icpNumber" class="label-edit-input" maxlength="30" @blur="finishFieldLabelEdit('icpNumber', 'ICP备案号')" @keyup.enter="finishFieldLabelEdit('icpNumber', 'ICP备案号')" />
                  <button v-else class="editable-label" type="button" @click="startFieldLabelEdit('icpNumber')">{{ getFieldLabel('icpNumber', 'ICP备案号') }}</button>
                </template>
                <el-input v-model="form.icpNumber" maxlength="80" placeholder="例如：渝ICP备xxxxxxxx号" />
              </el-form-item>
            </el-col>
            <el-col :xs="24" :md="12">
              <el-form-item>
                <template #label>
                  <el-input v-if="editingFieldLabel === 'icpUrl'" v-model="form.fieldLabels.icpUrl" class="label-edit-input" maxlength="30" @blur="finishFieldLabelEdit('icpUrl', 'ICP备案链接')" @keyup.enter="finishFieldLabelEdit('icpUrl', 'ICP备案链接')" />
                  <button v-else class="editable-label" type="button" @click="startFieldLabelEdit('icpUrl')">{{ getFieldLabel('icpUrl', 'ICP备案链接') }}</button>
                </template>
                <el-input v-model="form.icpUrl" placeholder="https://beian.miit.gov.cn/" />
              </el-form-item>
            </el-col>
            <el-col :xs="24" :md="12">
              <el-form-item>
                <template #label>
                  <el-input v-if="editingFieldLabel === 'policeNumber'" v-model="form.fieldLabels.policeNumber" class="label-edit-input" maxlength="30" @blur="finishFieldLabelEdit('policeNumber', '公安备案号')" @keyup.enter="finishFieldLabelEdit('policeNumber', '公安备案号')" />
                  <button v-else class="editable-label" type="button" @click="startFieldLabelEdit('policeNumber')">{{ getFieldLabel('policeNumber', '公安备案号') }}</button>
                </template>
                <el-input v-model="form.policeNumber" maxlength="80" placeholder="例如：渝公网安备 xxxxxxxxxxxxxx号" />
              </el-form-item>
            </el-col>
            <el-col :xs="24" :md="12">
              <el-form-item>
                <template #label>
                  <el-input v-if="editingFieldLabel === 'policeUrl'" v-model="form.fieldLabels.policeUrl" class="label-edit-input" maxlength="30" @blur="finishFieldLabelEdit('policeUrl', '公安备案链接')" @keyup.enter="finishFieldLabelEdit('policeUrl', '公安备案链接')" />
                  <button v-else class="editable-label" type="button" @click="startFieldLabelEdit('policeUrl')">{{ getFieldLabel('policeUrl', '公安备案链接') }}</button>
                </template>
                <el-input v-model="form.policeUrl" placeholder="https://beian.mps.gov.cn" />
              </el-form-item>
            </el-col>
            <el-col :xs="24" :md="12">
              <el-form-item>
                <template #label>
                  <el-input v-if="editingFieldLabel === 'licenseNumber'" v-model="form.fieldLabels.licenseNumber" class="label-edit-input" maxlength="30" @blur="finishFieldLabelEdit('licenseNumber', '许可编号')" @keyup.enter="finishFieldLabelEdit('licenseNumber', '许可编号')" />
                  <button v-else class="editable-label" type="button" @click="startFieldLabelEdit('licenseNumber')">{{ getFieldLabel('licenseNumber', '许可编号') }}</button>
                </template>
                <el-input v-model="form.licenseNumber" maxlength="80" placeholder="请输入许可证或业务经营许可编号" />
              </el-form-item>
            </el-col>
            <el-col :xs="24" :md="12">
              <el-form-item>
                <template #label>
                  <el-input v-if="editingFieldLabel === 'complaintPhone'" v-model="form.fieldLabels.complaintPhone" class="label-edit-input" maxlength="30" @blur="finishFieldLabelEdit('complaintPhone', '投诉与服务监督')" @keyup.enter="finishFieldLabelEdit('complaintPhone', '投诉与服务监督')" />
                  <button v-else class="editable-label" type="button" @click="startFieldLabelEdit('complaintPhone')">{{ getFieldLabel('complaintPhone', '投诉与服务监督') }}</button>
                </template>
                <el-input v-model="form.complaintPhone" maxlength="40" placeholder="请输入投诉电话" />
              </el-form-item>
            </el-col>
            <el-col :xs="24" :md="12">
              <el-form-item>
                <template #label>
                  <el-input v-if="editingFieldLabel === 'technicalSupport'" v-model="form.fieldLabels.technicalSupport" class="label-edit-input" maxlength="30" @blur="finishFieldLabelEdit('technicalSupport', '法律与技术支持')" @keyup.enter="finishFieldLabelEdit('technicalSupport', '法律与技术支持')" />
                  <button v-else class="editable-label" type="button" @click="startFieldLabelEdit('technicalSupport')">{{ getFieldLabel('technicalSupport', '法律与技术支持') }}</button>
                </template>
                <el-input v-model="form.technicalSupport" maxlength="80" placeholder="例如：足动科技" />
              </el-form-item>
            </el-col>
            <el-col :xs="24" :md="12">
              <el-form-item>
                <template #label>
                  <el-input v-if="editingFieldLabel === 'reportEmail'" v-model="form.fieldLabels.reportEmail" class="label-edit-input" maxlength="30" @blur="finishFieldLabelEdit('reportEmail', '服务邮箱')" @keyup.enter="finishFieldLabelEdit('reportEmail', '服务邮箱')" />
                  <button v-else class="editable-label" type="button" @click="startFieldLabelEdit('reportEmail')">{{ getFieldLabel('reportEmail', '服务邮箱') }}</button>
                </template>
                <el-input v-model="form.reportEmail" maxlength="80" placeholder="例如：admin@example.com" />
              </el-form-item>
            </el-col>
            <el-col :xs="24" :md="12">
              <el-form-item>
                <template #label>
                  <el-input v-if="editingFieldLabel === 'minorReportEmail'" v-model="form.fieldLabels.minorReportEmail" class="label-edit-input" maxlength="30" @blur="finishFieldLabelEdit('minorReportEmail', '未成年人信息保护邮箱')" @keyup.enter="finishFieldLabelEdit('minorReportEmail', '未成年人信息保护邮箱')" />
                  <button v-else class="editable-label" type="button" @click="startFieldLabelEdit('minorReportEmail')">{{ getFieldLabel('minorReportEmail', '未成年人信息保护邮箱') }}</button>
                </template>
                <el-input v-model="form.minorReportEmail" maxlength="80" placeholder="例如：admin@example.com" />
              </el-form-item>
            </el-col>
            <el-col :xs="24" :md="12">
              <el-form-item prop="copyright">
                <template #label>
                  <el-input v-if="editingFieldLabel === 'copyright'" v-model="form.fieldLabels.copyright" class="label-edit-input" maxlength="30" @blur="finishFieldLabelEdit('copyright', '版权信息')" @keyup.enter="finishFieldLabelEdit('copyright', '版权信息')" />
                  <button v-else class="editable-label" type="button" @click="startFieldLabelEdit('copyright')">{{ getFieldLabel('copyright', '版权信息') }}</button>
                </template>
                <el-input v-model="form.copyright" maxlength="120" show-word-limit />
              </el-form-item>
            </el-col>
          </el-row>

          <div class="dynamic-header">
            <span>版权与免责声明</span>
            <el-button text type="primary" @click="addLegalNote">新增说明</el-button>
          </div>
          <div v-for="(note, index) in form.legalNotes" :key="`note-${index}`" class="line-editor">
            <el-input v-model="form.legalNotes[index]" maxlength="180" show-word-limit placeholder="请输入页脚版权或免责声明" />
            <el-button :icon="Delete" circle plain type="danger" @click="removeItem(form.legalNotes, index)" />
          </div>
        </el-card>

        <el-card class="config-card" shadow="never">
          <template #header>
            <div class="card-title">
              <el-icon><Medal /></el-icon>
              <span>页脚价值点</span>
            </div>
          </template>
          <div class="dynamic-header">
            <span>网站底部能力展示</span>
            <el-button text type="primary" @click="addFeature">新增价值点</el-button>
          </div>
          <div v-for="(item, index) in form.featureItems" :key="`feature-${index}`" class="feature-editor">
            <el-input v-model="item.title" placeholder="标题，例如：责任" maxlength="20" />
            <el-select v-model="item.icon" placeholder="图标风格">
              <el-option label="责任" value="shield" />
              <el-option label="线路" value="route" />
              <el-option label="服务" value="service" />
              <el-option label="体验" value="experience" />
              <el-option label="生态" value="leaf" />
              <el-option label="分享" value="share" />
            </el-select>
            <el-input v-model="item.description" placeholder="简短说明，建议 20-36 字" maxlength="80" show-word-limit />
            <el-button :icon="Delete" circle plain type="danger" @click="removeItem(form.featureItems, index)" />
          </div>
        </el-card>

        <el-card class="config-card" shadow="never">
          <template #header>
            <div class="card-title">
              <el-icon><Link /></el-icon>
              <span>导航与友情链接</span>
            </div>
          </template>
          <div class="dynamic-header">
            <span>顶部链接</span>
            <el-button text type="primary" @click="addLink('topLinks')">新增链接</el-button>
          </div>
          <div v-for="(link, index) in form.topLinks" :key="`top-${index}`" class="link-editor">
            <el-input v-model="link.label" placeholder="链接名称" maxlength="30" />
            <el-input v-model="link.url" placeholder="/about 或 https://example.com" />
            <el-button :icon="Delete" circle plain type="danger" @click="removeItem(form.topLinks, index)" />
          </div>

          <div class="dynamic-header friends-header">
            <span>合规与服务说明项</span>
            <div class="header-actions">
              <el-button text type="primary" @click="openLegalEditor">前往页面编辑内容</el-button>
              <el-button text type="primary" @click="addLink('complianceLinks')">新增说明项</el-button>
            </div>
          </div>
          <div v-for="(link, index) in form.complianceLinks" :key="`compliance-${index}`" class="link-editor">
            <el-input v-model="link.label" placeholder="例如：营业执照、服务规范、投诉反馈专区" maxlength="30" />
            <el-input v-model="link.url" placeholder="/about 或 https://example.com，可留空" />
            <el-button :icon="Delete" circle plain type="danger" @click="removeItem(form.complianceLinks, index)" />
          </div>

          <div class="dynamic-header friends-header">
            <span>友情链接</span>
            <el-button text type="primary" @click="addLink('friendlyLinks')">新增链接</el-button>
          </div>
          <div v-for="(link, index) in form.friendlyLinks" :key="`friend-${index}`" class="link-editor">
            <el-input v-model="link.label" placeholder="链接名称" maxlength="30" />
            <el-input v-model="link.url" placeholder="https://example.com" />
            <el-button :icon="Delete" circle plain type="danger" @click="removeItem(form.friendlyLinks, index)" />
          </div>
        </el-card>

        <el-card class="config-card" shadow="never">
          <template #header>
            <div class="card-title">
              <el-icon><Picture /></el-icon>
              <span>二维码与荣誉证书</span>
            </div>
          </template>
          <el-alert
            class="config-tip"
            type="info"
            show-icon
            :closable="false"
            title="上传后的二维码会显示在网站页脚右侧，可分别维护服务号、顾问微信、小程序或 App 下载码。"
          />
          <div class="dynamic-header">
            <span>官方二维码</span>
            <el-button text type="primary" @click="addQrCode">新增二维码</el-button>
          </div>
          <div v-for="(item, index) in form.qrCodes" :key="`qr-${index}`" class="media-editor">
            <div class="media-preview">
              <img v-if="item.imageUrl" :src="getImageUrl(item.imageUrl)" alt="二维码预览" />
              <span v-else>QR</span>
            </div>
            <div class="media-fields">
              <el-input v-model="item.label" placeholder="名称，例如：微信公众号" maxlength="30" />
              <el-input v-model="item.description" placeholder="说明，例如：扫码关注服务动态" maxlength="60" />
              <el-input v-model="item.imageUrl" placeholder="图片路径，也可以点击右侧上传" />
            </div>
            <el-upload :show-file-list="false" :http-request="options => uploadImage(options, item)" :before-upload="beforeImageUpload">
              <el-button :icon="UploadFilled" plain>上传</el-button>
            </el-upload>
            <el-button :icon="Delete" circle plain type="danger" @click="removeItem(form.qrCodes, index)" />
          </div>

          <div class="dynamic-header friends-header">
            <span>荣誉证书</span>
            <el-button text type="primary" @click="addCertificate">新增证书</el-button>
          </div>
          <div v-for="(item, index) in form.certificates" :key="`cert-${index}`" class="media-editor">
            <div class="media-preview cert">
              <img v-if="item.imageUrl" :src="getImageUrl(item.imageUrl)" alt="证书预览" />
              <span v-else>证书</span>
            </div>
            <div class="media-fields">
              <el-input v-model="item.title" placeholder="证书名称" maxlength="40" />
              <el-input v-model="item.description" placeholder="证书说明" maxlength="80" />
              <el-input v-model="item.imageUrl" placeholder="图片路径，也可以点击右侧上传" />
            </div>
            <el-upload :show-file-list="false" :http-request="options => uploadImage(options, item)" :before-upload="beforeImageUpload">
              <el-button :icon="UploadFilled" plain>上传</el-button>
            </el-upload>
            <el-button :icon="Delete" circle plain type="danger" @click="removeItem(form.certificates, index)" />
          </div>
        </el-card>
      </el-form>

    </div>
  </div>
</template>

<script setup>
import { nextTick, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Checked, Delete, Link, Medal, OfficeBuilding, Picture, UploadFilled } from '@element-plus/icons-vue'
import { getFooterConfig, saveFooterConfig } from '@/api/siteFooter'
import request from '@/utils/request'

const baseAPI = process.env.VUE_APP_BASE_API || '/api'
const loading = ref(false)
const saving = ref(false)
const formRef = ref(null)
const editingFieldLabel = ref('')

const defaultFieldLabels = () => ({
  enabled: '前台展示状态',
  companyName: '企业名称',
  brandName: '品牌名称',
  slogan: '页脚标语',
  consultationPhone: '旅行咨询电话',
  cruisePhone: '团队与邮轮咨询',
  serviceTime: '服务时间',
  address: '公司总部地址',
  icpNumber: 'ICP备案号',
  icpUrl: 'ICP备案链接',
  policeNumber: '公安备案号',
  policeUrl: '公安备案链接',
  licenseNumber: '许可编号',
  complaintPhone: '投诉与服务监督',
  technicalSupport: '法律与技术支持',
  reportEmail: '服务邮箱',
  minorReportEmail: '未成年人信息保护邮箱',
  copyright: '版权信息'
})

const defaultLegalPages = () => ([
  {
    type: 'business-license',
    title: '营业执照',
    navText: '企业主体与经营信息',
    kicker: '企业资质',
    heading: '重庆侠客行国际旅行社有限公司主体说明',
    summary: '展示企业主体、经营范围与资质核验方式，方便用户识别平台服务主体。',
    lead: '侠客行国旅依法开展旅行咨询、线路组织、订单服务与售后协助等相关业务。平台展示的企业信息用于帮助用户了解服务主体，具体登记信息以市场监督管理部门公示为准。',
    notice: '如需查看营业执照扫描件或进一步核验主体信息，可联系平台客服或通过官方公开渠道查询。',
    qualificationTitle: '营业执照资质图片',
    imageUrl: '',
    sections: [
      { title: '主体信息', text: '', items: ['企业名称：重庆侠客行国际旅行社有限公司', '品牌简称：侠客行国旅', '总部地址：重庆市渝中区解放碑商圈时代旅行中心 18F'] },
      { title: '核验方式', text: '用户可通过国家企业信用信息公示系统、地方市场监督管理部门公开渠道，核验企业登记状态、统一社会信用代码、经营范围和行政许可信息。', items: [] }
    ]
  },
  {
    type: 'travel-license',
    title: '旅行社业务经营许可',
    navText: '旅行社许可与业务边界',
    kicker: '经营许可',
    heading: '旅行社业务经营许可说明',
    summary: '说明旅行社业务经营许可、服务范围、订单确认与履约边界。',
    lead: '平台围绕旅行社业务许可范围提供线路咨询、行程组织、产品预订与出行服务。涉及第三方服务时，以订单确认信息、供应商规则和目的地实时政策为准。',
    notice: '线路页面、攻略和图片仅作服务说明，最终安排以合同、订单确认单和出行通知为准。',
    qualificationTitle: '旅行社业务经营许可资质图片',
    imageUrl: '',
    sections: [
      { title: '许可信息', text: '', items: ['许可编号：L-CQ-XXK-2026', '业务范围：境内旅游、入境旅游相关咨询与组织服务'] },
      { title: '订单确认', text: '用户提交订单后，平台将根据库存、成团情况、供应商确认、实名信息和支付状态进行确认。', items: [] }
    ]
  },
  {
    type: 'service-standard',
    title: '服务规范',
    navText: '预订、履约与售后标准',
    kicker: '服务标准',
    heading: '侠客行国旅服务规范',
    summary: '明确咨询、预订、出行、变更、退款和售后处理的基本流程。',
    lead: '我们希望每一次旅行服务都清楚、可追踪、可沟通。平台通过订单记录、客服跟进和出行通知，帮助用户理解服务进度与权责边界。',
    notice: '涉及退改、赔付或补偿事项，以订单规则、合同约定及实际责任认定为准。',
    qualificationTitle: '',
    imageUrl: '',
    sections: [
      { title: '咨询与预订', text: '', items: ['展示线路亮点、价格说明、费用包含与不包含项目', '重要限制条件、年龄要求、证件要求和取消规则应在下单前提示'] },
      { title: '售后与争议', text: '用户可通过在线服务与投诉反馈专区、客服电话或订单售后入口提交问题。', items: [] }
    ]
  },
  {
    type: 'community-guidelines',
    title: '社区公约',
    navText: '评价、攻略与互动规则',
    kicker: '社区秩序',
    heading: '侠客行社区公约',
    summary: '维护真实、友好、有帮助的旅行内容环境。',
    lead: '平台鼓励用户分享真实体验、实用攻略和建设性建议，也会保护旅行者、商家、导游和目的地社区的合法权益。',
    notice: '如你认为内容被误判，可通过意见反馈或客服渠道提交申诉材料。',
    qualificationTitle: '',
    imageUrl: '',
    sections: [
      { title: '鼓励发布', text: '', items: ['真实旅行经历、实用路线建议、避坑提醒和消费体验', '对服务改进有帮助的理性反馈'] },
      { title: '禁止内容', text: '', items: ['虚假评价、刷单引流、恶意诋毁或冒充他人', '泄露他人隐私、证件、联系方式、订单信息或未成年人个人信息'] }
    ]
  },
  {
    type: 'privacy-safety',
    title: '安全与隐私保护',
    navText: '数据、账号与未成年人保护',
    kicker: '隐私安全',
    heading: '安全与隐私保护说明',
    summary: '说明平台如何保护账号安全、订单信息、联系方式与未成年人信息。',
    lead: '平台仅在实现注册登录、订单履约、客户服务、风险控制和法律合规所必需的范围内处理用户信息，并采取合理措施保护数据安全。',
    notice: '隐私或未成年人信息相关问题可联系 safe@xkxtrip.com；账号和订单服务可联系 service@xkxtrip.com。',
    qualificationTitle: '',
    imageUrl: '',
    sections: [
      { title: '信息使用范围', text: '', items: ['账号注册、短信验证、身份识别和登录安全', '订单预订、出行通知、客服沟通、发票或合同处理'] },
      { title: '未成年人保护', text: '未成年人使用平台服务应取得监护人同意。涉及未成年人订单、证件、联系方式、照片或出行安全事项时，平台将以更审慎方式处理。', items: [] }
    ]
  },
  {
    type: 'support-feedback',
    title: '在线服务与投诉反馈专区',
    navText: '客服、投诉与监督渠道',
    kicker: '服务反馈',
    heading: '在线服务与投诉反馈专区',
    summary: '提供咨询、售后、投诉、监督和紧急问题处理渠道。',
    lead: '如果你在咨询、预订、支付、出行或售后过程中遇到问题，可以通过平台客服、电话、邮箱或订单记录提交反馈。',
    notice: '服务时间为 09:00 - 20:00，节假日专人在线；紧急出行问题将优先处理。',
    qualificationTitle: '投诉反馈说明图片',
    imageUrl: '',
    sections: [
      { title: '联系方式', text: '', items: ['服务邮箱：service@xkxtrip.com', '旅行咨询电话：400-800-5178', '投诉与服务监督：12345 / 023-6789-5178'] },
      { title: '处理流程', text: '', items: ['普通咨询：按服务时间尽快响应', '订单售后：结合订单号、支付记录、出行凭证和沟通记录核查'] }
    ]
  }
])

const defaultForm = () => ({
  enabled: true,
  companyName: '侠客行国际旅行社有限公司',
  brandName: '侠客行国旅',
  slogan: '扎根重庆，连接山城、三峡与西南山河的品质旅行服务',
  consultationPhone: '400-800-5178',
  cruisePhone: '023-6789-5178',
  serviceTime: '09:00 - 20:00，节假日专人在线',
  address: '重庆市渝中区解放碑商圈时代旅行中心 18F',
  copyright: '© 2021-2026 侠客行国旅 版权所有',
  icpNumber: '',
  icpUrl: 'https://beian.miit.gov.cn/',
  policeNumber: '',
  policeUrl: 'https://beian.mps.gov.cn',
  licenseNumber: 'L-CQ-XXK-2026',
  complaintPhone: '12345 / 023-6789-5178',
  technicalSupport: '侠客行数字旅行中心',
  reportEmail: 'service@xkxtrip.com',
  minorReportEmail: 'safe@xkxtrip.com',
  businessLicenseImageUrl: '',
  travelLicenseImageUrl: '',
  fieldLabels: defaultFieldLabels(),
  featureItems: [
    { title: '守信', description: '行程说明、费用明细与服务标准清楚呈现，重要节点可追踪。', icon: 'shield' },
    { title: '甄选', description: '围绕重庆、三峡与西南目的地，打磨小而美的品质线路。', icon: 'route' },
    { title: '陪伴', description: '顾问、导游与客服协同响应，从预订到返程持续跟进。', icon: 'service' },
    { title: '自在', description: '控制车程、住宿与游览节奏，让旅途保持轻松和松弛感。', icon: 'experience' },
    { title: '友好', description: '尊重当地风俗与自然环境，倡导低打扰、负责任的旅行。', icon: 'leaf' },
    { title: '共创', description: '收集真实评价与旅途故事，把反馈转化为下一次优化。', icon: 'share' }
  ],
  topLinks: [
    { label: '关于侠客行', url: '/about' },
    { label: '山城线路', url: '/tickets' },
    { label: '目的地灵感', url: '/scenic' },
    { label: '旅行攻略', url: '/guide' },
    { label: '服务承诺', url: '/about' },
    { label: '联系我们', url: '/about' }
  ],
  complianceLinks: [
    { label: '营业执照', url: '/legal/business-license' },
    { label: '旅行社业务经营许可', url: '/legal/travel-license' },
    { label: '服务规范', url: '/legal/service-standard' },
    { label: '社区公约', url: '/legal/community-guidelines' },
    { label: '安全与隐私保护', url: '/legal/privacy-safety' },
    { label: '在线服务与投诉反馈专区', url: '/legal/support-feedback' }
  ],
  friendlyLinks: [],
  qrCodes: [
    { label: '侠客行服务号', imageUrl: '', description: '线路上新与出行提醒' },
    { label: '旅行顾问', imageUrl: '', description: '定制咨询与售后协助' }
  ],
  certificates: [],
  legalPages: defaultLegalPages(),
  legalNotes: ['平台展示的图片、攻略与目的地资料仅用于旅行服务说明，出行前请以订单确认信息和当地实时政策为准。']
})

const form = reactive(defaultForm())

const rules = {
  companyName: [{ required: true, message: '请输入企业名称', trigger: 'blur' }],
  brandName: [{ required: true, message: '请输入品牌名称', trigger: 'blur' }],
  copyright: [{ required: true, message: '请输入版权信息', trigger: 'blur' }]
}

const cleanLinks = (links = []) => links.filter(item => item.label)
const assignForm = (data = {}) => {
  Object.assign(form, defaultForm(), data)
  form.fieldLabels = { ...defaultFieldLabels(), ...(data.fieldLabels || {}) }
  form.featureItems = data.featureItems?.length ? data.featureItems : defaultForm().featureItems
  form.topLinks = data.topLinks?.length ? data.topLinks : defaultForm().topLinks
  form.complianceLinks = data.complianceLinks?.length ? data.complianceLinks : defaultForm().complianceLinks
  form.qrCodes = data.qrCodes?.length ? data.qrCodes : defaultForm().qrCodes
  form.friendlyLinks = data.friendlyLinks || []
  form.certificates = data.certificates || []
  form.legalPages = data.legalPages?.length ? data.legalPages : defaultLegalPages()
  form.legalNotes = data.legalNotes?.length ? data.legalNotes : defaultForm().legalNotes
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

const addLink = (field) => {
  form[field].push({ label: '', url: '' })
}

const addFeature = () => {
  form.featureItems.push({ title: '', description: '', icon: 'service' })
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

const getFieldLabel = (key, fallback) => form.fieldLabels?.[key] || fallback

const startFieldLabelEdit = async (key) => {
  editingFieldLabel.value = key
  await nextTick()
  document.querySelector('.label-edit-input input')?.focus()
}

const finishFieldLabelEdit = (key, fallback) => {
  const value = form.fieldLabels?.[key]?.trim()
  form.fieldLabels[key] = value || fallback
  editingFieldLabel.value = ''
}

const normalizePayload = () => ({
  ...form,
  businessLicenseImageUrl: form.legalPages.find(item => item.type === 'business-license')?.imageUrl || form.businessLicenseImageUrl,
  travelLicenseImageUrl: form.legalPages.find(item => item.type === 'travel-license')?.imageUrl || form.travelLicenseImageUrl,
  featureItems: form.featureItems.filter(item => item.title),
  topLinks: cleanLinks(form.topLinks),
  complianceLinks: cleanLinks(form.complianceLinks),
  friendlyLinks: cleanLinks(form.friendlyLinks),
  qrCodes: form.qrCodes.filter(item => item.label),
  certificates: form.certificates.filter(item => item.title),
  legalPages: form.legalPages.map(page => ({
    ...page,
    sections: (page.sections || []).filter(section => section.title).map(section => ({
      ...section,
      items: (section.items || []).filter(Boolean)
    }))
  })),
  legalNotes: form.legalNotes.filter(Boolean)
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

const openLegalEditor = () => {
  window.open('/legal/business-license', '_blank')
}

const beforeImageUpload = (file) => {
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

const getImageUrl = (url) => {
  if (!url) return ''
  return url.startsWith('http') ? url : baseAPI + url
}

onMounted(fetchConfig)
</script>

<style lang="scss" scoped>
.footer-config-page {
  padding: 24px;
  background:
    linear-gradient(180deg, #eef3f8 0%, #f7f9fc 240px),
    #f7f9fc;
  min-height: 100%;
}

.page-hero {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 20px;
  padding: 24px 28px;
  margin-bottom: 22px;
  border-radius: 14px;
  background: linear-gradient(135deg, #17233d, #24547a);
  color: #fff;
  box-shadow: 0 14px 30px rgba(23, 35, 61, 0.16);

  p {
    margin: 0 0 8px;
    color: #9bd8ff;
    font-size: 13px;
    font-weight: 800;
    letter-spacing: 0;
  }

  h1 {
    margin: 0;
    font-size: 26px;
    font-weight: 900;
  }

  span {
    display: block;
    margin-top: 10px;
    color: rgba(255, 255, 255, 0.78);
  }
}

.config-layout {
  display: block;
  max-width: 1280px;
  margin: 0 auto;
}

.config-card {
  border: 0;
  border-radius: 12px;
  margin-bottom: 14px;
  box-shadow: 0 10px 24px rgba(31, 45, 61, 0.07);

  :deep(.el-card__header) {
    padding: 14px 18px;
    border-bottom: 1px solid #edf1f7;
  }

  :deep(.el-card__body) {
    padding: 18px;
  }

  :deep(.el-input__wrapper),
  :deep(.el-textarea__inner) {
    border-radius: 9px;
    min-height: 38px;
    box-shadow: 0 0 0 1px #dfe7f2 inset;
  }

  :deep(.el-form-item) {
    margin-bottom: 16px;
  }
}

.config-tip {
  margin-bottom: 14px;
  border-radius: 8px;
}

.card-title,
.dynamic-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  color: #17233d;
  font-size: 15px;
  font-weight: 900;
}

.card-title {
  justify-content: flex-start;
}

.editable-label {
  padding: 0;
  border: 0;
  background: transparent;
  color: #314057;
  font: inherit;
  font-weight: 800;
  cursor: text;
}

.editable-label:hover {
  color: #1f7ae0;
}

.label-edit-input {
  width: 210px;

  :deep(.el-input__wrapper) {
    min-height: 28px;
    border-radius: 7px;
  }
}

.dynamic-header {
  margin: 4px 0 10px;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.friends-header {
  margin-top: 18px;
}

.line-editor,
.link-editor,
.feature-editor,
.media-editor {
  display: grid;
  gap: 10px;
  align-items: center;
  margin-bottom: 10px;
}

.line-editor {
  grid-template-columns: minmax(0, 1fr) auto;
}

.link-editor {
  grid-template-columns: 190px minmax(0, 1fr) auto;
}

.feature-editor {
  grid-template-columns: 140px 150px minmax(0, 1fr) auto;
}

.media-editor {
  grid-template-columns: 76px minmax(0, 1fr) auto auto;
  padding: 12px;
  border-radius: 12px;
  background: #f8fbff;
  border: 1px solid #edf2fa;
}

.legal-license-editor {
  grid-template-columns: 86px minmax(0, 1fr) auto;
  margin-bottom: 0;
  background: #fbfcfe;
}

.media-fields {
  display: grid;
  gap: 10px;
}

.media-fields strong {
  color: #17233d;
  font-size: 13px;
}

.media-preview {
  width: 66px;
  height: 66px;
  border-radius: 10px;
  background: #ffffff;
  border: 1px dashed #b7c6d9;
  display: grid;
  place-items: center;
  color: #60718a;
  font-weight: 800;
  overflow: hidden;

  &.cert {
    width: 76px;
  }

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }
}

@media (max-width: 1180px) {
  .feature-editor,
  .media-editor {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .footer-config-page {
    padding: 14px;
  }

  .page-hero {
    flex-direction: column;
    align-items: flex-start;
    border-radius: 14px;
  }

  .link-editor,
  .feature-editor,
  .media-editor {
    grid-template-columns: 1fr;
  }
}
</style>
