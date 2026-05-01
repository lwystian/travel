<template>
  <div class="guide-edit-container">
    <div class="page-header">
      <h1>{{ form.id ? '编辑攻略' : '发布新攻略' }}</h1>
      <p>分享您的旅行经历，帮助更多人探索世界的美好</p>
    </div>
    
    <div class="edit-form-container">
      <el-form 
        :model="form" 
        label-width="80px" 
        ref="formRef"
        class="edit-form"
      >
        <el-form-item label="标题" prop="title" required>
          <el-input 
            v-model="form.title" 
            placeholder="请输入攻略标题（建议30字以内）" 
            maxlength="50"
            show-word-limit
          />
        </el-form-item>

        <el-form-item label="目的地" prop="destination" required>
          <el-cascader
            v-model="form.destination"
            :options="destinationOptions"
            placeholder="请选择目的地"
            filterable
            style="width: 100%;"
            :props="{
              value: 'code',
              label: 'name',
              children: 'cities'
            }"
          />
          <div class="form-tips">
            选择攻略的目的地，帮助其他用户更好地查找
          </div>
        </el-form-item>
        
        <el-form-item label="封面" prop="coverImage" required>
          <div class="cover-uploader-container">
            <el-upload
              class="cover-uploader"
              action="#"
              :show-file-list="false"
              :http-request="customUploadCover"
              :before-upload="beforeCoverUpload"
            >
              <div v-if="form.coverImage" class="cover-preview-container">
                <img :src="getImageUrl(form.coverImage)" class="cover-preview" />
                <div class="cover-hover-mask">
                  <el-icon class="upload-icon"><EditPen /></el-icon>
                  <span>更换封面</span>
                </div>
              </div>
              <div v-else class="upload-placeholder">
                <el-icon class="upload-icon"><Plus /></el-icon>
                <span>上传封面图片</span>
                <div class="upload-tip">推荐尺寸: 900×600px, JPG/PNG格式</div>
              </div>
            </el-upload>
            <div class="form-tips" v-if="!form.coverImage">
              添加一张精美的封面图能吸引更多读者
            </div>
          </div>
        </el-form-item>
        
        <el-form-item label="内容" prop="content" required>
          <div class="editor-container">
            <WangEditor v-model="form.content" />
            <div class="form-tips">
              支持文字、图片、视频等多媒体内容，可插入旅游景点图片丰富您的攻略
            </div>
          </div>
        </el-form-item>
        
        <el-form-item>
          <div class="form-actions">
            <el-button @click="goBack">取消</el-button>
            <el-button type="primary" @click="submit" :loading="submitting">
              {{ form.id ? '保存修改' : '发布攻略' }}
            </el-button>
          </div>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import WangEditor from '@/components/WangEditor.vue'
import request from '@/utils/request'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store/user'

// 全国省市数据
const destinationOptions = [
  {
    code: 'beijing', name: '北京', cities: [
      { code: 'beijing', name: '北京市' }
    ]
  },
  {
    code: 'tianjin', name: '天津', cities: [
      { code: 'tianjin', name: '天津市' }
    ]
  },
  {
    code: 'hebei', name: '河北', cities: [
      { code: 'shijiazhuang', name: '石家庄' },
      { code: 'tangshan', name: '唐山' },
      { code: 'qinhuangdao', name: '秦皇岛' },
      { code: 'baoding', name: '保定' },
      { code: 'zhangjiakou', name: '张家口' },
      { code: 'chengde', name: '承德' }
    ]
  },
  {
    code: 'shanxi', name: '山西', cities: [
      { code: 'taiyuan', name: '太原' },
      { code: 'datong', name: '大同' },
      { code: 'pingyao', name: '平遥' },
      { code: 'lvliang', name: '吕梁' }
    ]
  },
  {
    code: 'neimenggu', name: '内蒙古', cities: [
      { code: 'huhehaote', name: '呼和浩特' },
      { code: 'baotou', name: '包头' },
      { code: 'orodos', name: '鄂尔多斯' },
      { code: 'xilingol', name: '锡林郭勒' }
    ]
  },
  {
    code: 'liaoning', name: '辽宁', cities: [
      { code: 'shenyang', name: '沈阳' },
      { code: 'dalian', name: '大连' },
      { code: 'anshan', name: '鞍山' },
      { code: 'yingkou', name: '营口' }
    ]
  },
  {
    code: 'jilin', name: '吉林', cities: [
      { code: 'changchun', name: '长春' },
      { code: 'jilin', name: '吉林' },
      { code: 'siping', name: '四平' }
    ]
  },
  {
    code: 'heilongjiang', name: '黑龙江', cities: [
      { code: 'harbin', name: '哈尔滨' },
      { code: 'daqing', name: '大庆' },
      { code: 'mudanjiang', name: '牡丹江' },
      { code: 'jixi', name: '鸡西' }
    ]
  },
  {
    code: 'shanghai', name: '上海', cities: [
      { code: 'shanghai', name: '上海市' }
    ]
  },
  {
    code: 'jiangsu', name: '江苏', cities: [
      { code: 'nanjing', name: '南京' },
      { code: 'suzhou', name: '苏州' },
      { code: 'wuxi', name: '无锡' },
      { code: 'yangzhou', name: '扬州' },
      { code: 'xuzhou', name: '徐州' },
      { code: 'lianyungang', name: '连云港' }
    ]
  },
  {
    code: 'zhejiang', name: '浙江', cities: [
      { code: 'hangzhou', name: '杭州' },
      { code: 'ningbo', name: '宁波' },
      { code: 'wenzhou', name: '温州' },
      { code: 'shaoxing', name: '绍兴' },
      { code: 'jinhua', name: '金华' },
      { code: 'lishui', name: '丽水' }
    ]
  },
  {
    code: 'anhui', name: '安徽', cities: [
      { code: 'hefei', name: '合肥' },
      { code: 'huangshan', name: '黄山' },
      { code: 'bengbu', name: '蚌埠' },
      { code: 'fuyang', name: '阜阳' }
    ]
  },
  {
    code: 'fujian', name: '福建', cities: [
      { code: 'fuzhou', name: '福州' },
      { code: 'xiamen', name: '厦门' },
      { code: 'quanzhou', name: '泉州' },
      { code: 'nanping', name: '南平' }
    ]
  },
  {
    code: 'jiangxi', name: '江西', cities: [
      { code: 'nanchang', name: '南昌' },
      { code: 'jiujiang', name: '九江' },
      { code: 'jingdezhen', name: '景德镇' },
      { code: 'shangrao', name: '上饶' }
    ]
  },
  {
    code: 'shandong', name: '山东', cities: [
      { code: 'jinan', name: '济南' },
      { code: 'qingdao', name: '青岛' },
      { code: 'yantai', name: '烟台' },
      { code: 'weihai', name: '威海' },
      { code: 'taian', name: '泰安' }
    ]
  },
  {
    code: 'henan', name: '河南', cities: [
      { code: 'zhengzhou', name: '郑州' },
      { code: 'luoyang', name: '洛阳' },
      { code: 'kaifeng', name: '开封' },
      { code: 'anyang', name: '安阳' }
    ]
  },
  {
    code: 'hubei', name: '湖北', cities: [
      { code: 'wuhan', name: '武汉' },
      { code: 'yichang', name: '宜昌' },
      { code: 'shennongjia', name: '神农架' },
      { code: 'xiangyang', name: '襄阳' }
    ]
  },
  {
    code: 'hunan', name: '湖南', cities: [
      { code: 'changsha', name: '长沙' },
      { code: 'zhangjiajie', name: '张家界' },
      { code: 'xiangxi', name: '湘西' },
      { code: 'hengyang', name: '衡阳' }
    ]
  },
  {
    code: 'guangdong', name: '广东', cities: [
      { code: 'guangzhou', name: '广州' },
      { code: 'shenzhen', name: '深圳' },
      { code: 'zhuhai', name: '珠海' },
      { code: 'foshan', name: '佛山' },
      { code: 'chaozhou', name: '潮州' }
    ]
  },
  {
    code: 'guangxi', name: '广西', cities: [
      { code: 'nanning', name: '南宁' },
      { code: 'guilin', name: '桂林' },
      { code: 'liuzhou', name: '柳州' },
      { code: 'beihai', name: '北海' },
      { code: 'baise', name: '百色' }
    ]
  },
  {
    code: 'hainan', name: '海南', cities: [
      { code: 'haikou', name: '海口' },
      { code: 'sanya', name: '三亚' },
      { code: 'wuzhishan', name: '五指山' },
      { code: 'qionghai', name: '琼海' }
    ]
  },
  {
    code: 'chongqing', name: '重庆', cities: [
      { code: 'chongqing', name: '重庆市' }
    ]
  },
  {
    code: 'sichuan', name: '四川', cities: [
      { code: 'chengdu', name: '成都' },
      { code: 'jiuzhaigou', name: '九寨沟' },
      { code: 'leshan', name: '乐山' },
      { code: 'garze', name: '甘孜' },
      { code: 'aba', name: '阿坝' }
    ]
  },
  {
    code: 'guizhou', name: '贵州', cities: [
      { code: 'guiyang', name: '贵阳' },
      { code: 'zunyi', name: '遵义' },
      { code: 'liupanshui', name: '六盘水' },
      { code: 'qianxinan', name: '黔西南' }
    ]
  },
  {
    code: 'yunnan', name: '云南', cities: [
      { code: 'kunming', name: '昆明' },
      { code: 'dali', name: '大理' },
      { code: 'lijiang', name: '丽江' },
      { code: 'shangri-la', name: '香格里拉' },
      { code: 'xishuangbanna', name: '西双版纳' }
    ]
  },
  {
    code: 'xizang', name: '西藏', cities: [
      { code: 'lhasa', name: '拉萨' },
      { code: 'shigatse', name: '日喀则' },
      { code: 'nyingchi', name: '林芝' },
      { code: 'shannan', name: '山南' }
    ]
  },
  {
    code: 'shaanxi', name: '陕西', cities: [
      { code: 'xian', name: '西安' },
      { code: 'yanan', name: '延安' },
      { code: 'xianyang', name: '咸阳' },
      { code: 'hanzhong', name: '汉中' }
    ]
  },
  {
    code: 'gansu', name: '甘肃', cities: [
      { code: 'lanzhou', name: '兰州' },
      { code: 'tianshui', name: '天水' },
      { code: 'jiayuguan', name: '嘉峪关' },
      { code: 'zhangye', name: '张掖' },
      { code: 'dunhuang', name: '敦煌' }
    ]
  },
  {
    code: 'qinghai', name: '青海', cities: [
      { code: 'xining', name: '西宁' },
      { code: 'hainan', name: '海南州' },
      { code: 'haixi', name: '海西' }
    ]
  },
  {
    code: 'ningxia', name: '宁夏', cities: [
      { code: 'yinchuan', name: '银川' },
      { code: 'shizuishan', name: '石嘴山' },
      { code: 'guyuan', name: '固原' }
    ]
  },
  {
    code: 'xinjiang', name: '新疆', cities: [
      { code: 'wulumuqi', name: '乌鲁木齐' },
      { code: 'turpan', name: '吐鲁番' },
      { code: 'kashgar', name: '喀什' },
      { code: 'yili', name: '伊犁' }
    ]
  },
  {
    code: 'taiwan', name: '台湾', cities: [
      { code: 'taipei', name: '台北' },
      { code: 'kaohsiung', name: '高雄' },
      { code: 'taichung', name: '台中' }
    ]
  },
  {
    code: 'xianggang', name: '香港', cities: [
      { code: 'xianggang', name: '香港' }
    ]
  },
  {
    code: 'aomen', name: '澳门', cities: [
      { code: 'aomen', name: '澳门' }
    ]
  }
]

// 获取目的地标签（用于显示）
const getDestinationLabel = (dest) => {
  if (!dest) return ''
  if (Array.isArray(dest)) {
    if (dest.length === 2) {
      const province = destinationOptions.find(p => p.code === dest[0])
      const city = province?.cities.find(c => c.code === dest[1])
      return city ? `${province.name} · ${city.name}` : dest[1]
    }
    return dest[dest.length - 1] || ''
  }
  const province = destinationOptions.find(p => p.code === dest)
  if (province) {
    return province.cities.length === 1 ? province.name : province.name + ' · 选择城市'
  }
  return dest
}

const baseAPI = process.env.VUE_APP_BASE_API || '/api'
const form = reactive({
  title: '',
  coverImage: '',
  content: '',
  destination: '',
  id: ''
})
const formRef = ref(null)
const route = useRoute()
const router = useRouter()
const submitting = ref(false)
const userStore = useUserStore()

// 获取图片完整URL
const getImageUrl = (url) => {
  if (!url) return ''
  return url.startsWith('http') ? url : baseAPI + url
}

onMounted(async () => {
  if (route.query.id) {
    try {
      await request.get(`/guide/${route.query.id}`, {}, {
        showDefaultMsg: false,
        onSuccess: (res) => {
          form.title = res.title
          form.coverImage = res.coverImage
          form.content = res.content
          // 处理目的地回显：将字符串转为数组格式
          if (res.destination) {
            form.destination = res.destination.includes('/') 
              ? res.destination.split('/') 
              : res.destination
          }
          form.id = res.id
        }
      })
    } catch (error) {
      ElMessage.error('获取攻略信息失败')
      console.error('获取攻略信息失败', error)
    }
  }
})

const beforeCoverUpload = (file) => {
  const isJPG = file.type === 'image/jpeg'
  const isPNG = file.type === 'image/png'
  const isLt2M = file.size / 1024 / 1024 < 2
  if (!isJPG && !isPNG) {
    ElMessage.error('封面只能是 JPG 或 PNG 格式!')
    return false
  }
  if (!isLt2M) {
    ElMessage.error('封面大小不能超过 2MB!')
    return false
  }
  return true
}

const customUploadCover = async (options) => {
  try {
    const { file } = options
    const formData = new FormData()
    formData.append('file', file)
    await request.post('/file/upload/img', formData, {
      headers: { token: localStorage.getItem('token') || '' },
      transformRequest: [(data) => data],
      successMsg: '封面上传成功',
      errorMsg: '封面上传失败',
      onSuccess: (data) => {
        form.coverImage = data
        options.onSuccess({ data })
      },
      onError: (error) => {
        options.onError(new Error(error.message || '上传失败'))
      }
    })
  } catch (error) {
    options.onError(error)
  }
}

const submit = async () => {
  if (!form.title.trim()) {
    return ElMessage.warning('请输入攻略标题')
  }
  
  if (!form.destination || (Array.isArray(form.destination) && form.destination.length === 0)) {
    return ElMessage.warning('请选择攻略目的地')
  }
  
  if (!form.coverImage) {
    return ElMessage.warning('请上传攻略封面图片')
  }
  
  if (!form.content || form.content === '<p><br></p>') {
    return ElMessage.warning('请填写攻略内容')
  }
  
  submitting.value = true
  form.userId = userStore.userInfo.id
  
  // 处理目的地：如果是数组则转为字符串
  const submitData = { ...form }
  if (Array.isArray(submitData.destination)) {
    submitData.destination = submitData.destination.join('/')
  }
  
  try {
    if (form.id) {
      await request.put('/guide/update', submitData, {
        successMsg: '修改成功',
        onSuccess: () => {
          router.push({ name: 'MyGuideList' })
        }
      })
    } else {
      await request.post('/guide/add', submitData, {
        successMsg: '发布成功',
        onSuccess: () => {
          form.title = ''
          form.coverImage = ''
          form.content = ''
          form.destination = ''
          router.push({ name: 'MyGuideList' })
        }
      })
    }
  } catch (error) {
    console.error('提交失败', error)
  } finally {
    submitting.value = false
  }
}

const goBack = () => {
  router.back()
}
</script>

<style lang="scss" scoped>
.guide-edit-container {
  max-width: 1000px;
  margin: 0 auto;
  padding: 20px;
}

.page-header {
  text-align: center;
  margin-bottom: 30px;
  
  h1 {
    font-size: 28px;
    font-weight: 600;
    color: #333;
    margin: 0 0 10px;
  }
  
  p {
    font-size: 16px;
    color: #666;
    max-width: 600px;
    margin: 0 auto;
  }
}

.edit-form-container {
  background: #fff;
  border-radius: 8px;
  padding: 30px;
}

.edit-form {
  .el-form-item {
    margin-bottom: 25px;
  }
}

.cover-uploader-container {
  width: 100%;
}

.cover-uploader {
  :deep(.el-upload) {
    cursor: pointer;
    position: relative;
    overflow: hidden;
    transition: all 0.3s;
    width: 100%;
    max-width: 360px;
  }
  
  .cover-preview-container {
    position: relative;
    width: 360px;
    height: 200px;
    border-radius: 8px;
    overflow: hidden;
    transition: all 0.3s;
    
    &:hover {
      .cover-hover-mask {
        opacity: 1;
      }
    }
    
    .cover-preview {
      width: 100%;
      height: 100%;
      object-fit: cover;
    }
    
    .cover-hover-mask {
      position: absolute;
      top: 0;
      left: 0;
      right: 0;
      bottom: 0;
      background: rgba(0, 0, 0, 0.5);
      display: flex;
      flex-direction: column;
      justify-content: center;
      align-items: center;
      opacity: 0;
      transition: opacity 0.3s;
      color: #fff;
      
      .upload-icon {
        font-size: 24px;
        margin-bottom: 8px;
      }
    }
  }
  
  .upload-placeholder {
    width: 360px;
    height: 200px;
    border: 1px dashed #d9d9d9;
    border-radius: 8px;
    display: flex;
    flex-direction: column;
    justify-content: center;
    align-items: center;
    color: #8c939d;
    transition: all 0.3s;
    
    &:hover {
      border-color: #409EFF;
      color: #409EFF;
    }
    
    .upload-icon {
      font-size: 28px;
      margin-bottom: 8px;
    }
    
    .upload-tip {
      font-size: 12px;
      margin-top: 8px;
      color: #999;
    }
  }
}

.form-tips {
  font-size: 13px;
  color: #909399;
  margin-top: 8px;
  line-height: 1.4;
}

.editor-container {
  :deep(.w-e-text-container) {
    min-height: 400px;
    max-height: 600px;
  }
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
  
  .el-button {
    min-width: 100px;
    margin-left: 16px;
  }
}

@media (max-width: 768px) {
  .guide-edit-container {
    padding: 15px;
  }
  
  .edit-form-container {
    padding: 20px 15px;
  }
  
  .cover-uploader {
    .cover-preview-container,
    .upload-placeholder {
      width: 100%;
      max-width: 100%;
    }
  }
  
  .form-actions {
    flex-direction: column-reverse;
    
    .el-button {
      width: 100%;
      margin: 8px 0;
    }
  }
}
</style> 