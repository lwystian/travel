<template>
  <div class="travel-page">
    <!-- 顶部 Banner -->
    <div ref="bannerRef" class="banner" :style="{ '--page-hero-height': `${bannerHeight}px` }">
      <img
        class="banner-img"
        src="https://images.unsplash.com/photo-1469854523086-cc02fe5d8800?w=1920&h=600&fit=crop"
        alt="banner"
      />
      <div class="banner-mask">
        <div class="banner-content">
          <h1 class="banner-title">探索旅行者分享的精彩旅游体验和实用建议</h1>
          <div class="search-bar">
            <input
              v-model="searchForm.title"
              type="text"
              class="search-input"
              placeholder="游记/旅行攻略/目的地"
              @keyup.enter="handleSearch"
            />
            <button class="search-btn" @click="handleSearch">搜索</button>
          </div>
        </div>
      </div>
    </div>

    <!-- 内容区域 -->
    <div class="content">
      <!-- Tab 切换 -->
      <div class="tab-bar">
        <div class="tabs">
          <div
            v-for="tab in tabs"
            :key="tab"
            class="tab"
            :class="{ active: currentTab === tab }"
            @click="handleTabChange(tab)"
          >
            {{ tab }}
          </div>
        </div>
        <div class="filter-btn" @click="openFilter">
          <span class="filter-icon">≡</span>
          <span>筛选</span>
        </div>
      </div>

      <!-- 攻略列表 -->
      <div class="article-list" v-loading="loading">
        <!-- 空状态 -->
        <div class="empty-state" v-if="tableData.length === 0 && !loading">
          <div class="empty-icon">📝</div>
          <h3 class="empty-title">暂无攻略内容</h3>
          <p class="empty-desc">成为第一个分享旅游攻略的人吧！</p>
          <el-button type="primary" @click="goEdit" class="empty-action">
            <el-icon><Edit /></el-icon>
            发布攻略
          </el-button>
        </div>

        <!-- 攻略列表 -->
        <div v-for="item in tableData" :key="item.id" class="article-item" @click="goDetail(item.id)">
          <div class="article-cover">
            <img :src="getImageUrl(item.coverImage)" :alt="item.title" />
            <span v-if="item.isEssence" class="essence-tag">精华游记</span>
          </div>
          <div class="article-info">
            <div class="article-header">
              <h3 class="article-title">{{ item.title }}</h3>
              <span v-if="item.isApp" class="app-tag">APP</span>
            </div>
            <p class="article-desc">{{ truncateDesc(item.content) }}</p>
            <div class="article-meta">
              <div class="meta-item">
                <span class="icon-location">⊙</span>
                <span class="meta-text">{{ getDestinationLabel(item.destination) }}</span>
              </div>
              <div class="meta-item">
                <img :src="getImageUrl(item.userAvatar)" class="avatar" :alt="item.userNickname" />
                <span class="meta-text author">{{ item.userNickname || '旅行者' + item.userId }}</span>
              </div>
              <div class="meta-item">
                <span class="icon-view">◉</span>
                <span class="meta-text">{{ formatNumber(item.views) }}阅读</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 分页 -->
      <div class="pagination-section" v-if="total > 0">
        <el-pagination
          background
          layout="prev, pager, next"
          :current-page="currentPage"
          :page-size="pageSize"
          :total="total"
          @current-change="handleCurrentChange"
          class="modern-pagination"
        />
      </div>
    </div>

    <!-- 发布攻略悬浮按钮 -->
    <div class="float-publish-btn" @click="goEdit">
      <el-icon><Edit /></el-icon>
      <span>发布攻略</span>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onBeforeUnmount, watch, nextTick } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import request from '@/utils/request'
import { Edit } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

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

// 获取目的地标签
const getDestinationLabel = (dest) => {
  if (!dest) return '未知目的地'
  // 处理新的省市格式（如 "hubei/wuhan" 或 "sichuan/chengdu"）
  if (dest.includes('/')) {
    const [provinceCode, cityCode] = dest.split('/')
    const province = destinationOptions.find(p => p.code === provinceCode)
    const city = province?.cities.find(c => c.code === cityCode)
    return city ? `${province.name} · ${city.name}` : dest
  }
  // 兼容旧格式
  const province = destinationOptions.find(p => p.code === dest)
  if (province) {
    return province.cities.length === 1 ? province.name : province.name + ' · 选择城市'
  }
  return dest || '未知目的地'
}

const baseAPI = process.env.VUE_APP_BASE_API || '/api'
const router = useRouter()
const route = useRoute()
const bannerRef = ref(null)
const bannerHeight = ref(480)

const updateBannerHeight = () => {
  if (!bannerRef.value) return
  const rect = bannerRef.value.getBoundingClientRect()
  const pageTop = rect.top + window.scrollY
  bannerHeight.value = Math.max(320, Math.round(window.innerHeight - pageTop))
}

// 数据
const tableData = ref([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

// 搜索表单
const searchForm = reactive({
  title: ''
})

// Tab 切换
const tabs = ['热门游记', '推荐游记', '最新游记']
const currentTab = ref('热门游记')

// 获取图片完整URL
const getImageUrl = (url) => {
  if (!url) return ''
  return url.startsWith('http') ? url : baseAPI + url
}

// 格式化数字
const formatNumber = (num) => {
  if (!num) return '0'
  if (num >= 10000) {
    return (num / 10000).toFixed(1) + 'w'
  }
  return num.toString()
}

// 截取描述内容
const truncateDesc = (content) => {
  if (!content) return '暂无描述'
  // 去除HTML标签
  const text = content.replace(/<[^>]*>/g, '')
  if (text.length <= 120) return text
  return text.substring(0, 120) + '...'
}

// 获取攻略列表
const fetchGuides = async () => {
  loading.value = true
  try {
    let orderBy = 'createTime'
    let order = 'desc'
    
    // 根据Tab确定排序规则
    if (currentTab.value === '热门游记') {
      orderBy = 'views'
      order = 'desc'
    } else if (currentTab.value === '最新游记') {
      orderBy = 'createTime'
      order = 'desc'
    } else if (currentTab.value === '推荐游记') {
      orderBy = 'likes'
      order = 'desc'
    }
    
    await request.get('/guide/page', {
      title: searchForm.title || undefined,
      currentPage: currentPage.value,
      size: pageSize.value,
      orderBy: orderBy,
      order: order
    }, {
      showDefaultMsg: false,
      onSuccess: (res) => {
        tableData.value = res.records || []
        total.value = res.total || 0
      }
    })
  } catch (error) {
    console.error('获取攻略列表失败', error)
  } finally {
    loading.value = false
  }
}

// 搜索
const handleSearch = () => {
  currentPage.value = 1
  fetchGuides()
  nextTick(() => {
    document.querySelector('.content')?.scrollIntoView({ behavior: 'smooth' })
  })
}

// Tab切换
const handleTabChange = (tab) => {
  currentTab.value = tab
  currentPage.value = 1
  fetchGuides()
}

// 打开筛选（预留功能）
const openFilter = () => {
  ElMessage.info('筛选功能开发中...')
}

// 分页变化
const handleCurrentChange = (page) => {
  currentPage.value = page
  fetchGuides()
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

// 跳转详情
const goDetail = (id) => {
  router.push(`/guide/detail/${id}`)
}

// 跳转编辑
const goEdit = () => {
  router.push('/guide/edit')
}

// 清除搜索
// 重置搜索
// 处理URL参数
const handleUrlParams = () => {
  const searchParam = route.query.search
  if (searchParam) {
    searchForm.title = decodeURIComponent(String(searchParam))
  }
  fetchGuides()
}

// 监听路由参数变化
watch(() => route.query.search, (newSearch, oldSearch) => {
  if (newSearch !== oldSearch) {
    handleUrlParams()
  }
})

// 生命周期
onMounted(() => {
  handleUrlParams()
  nextTick(updateBannerHeight)
  setTimeout(updateBannerHeight, 100)
  setTimeout(updateBannerHeight, 400)
  window.addEventListener('resize', updateBannerHeight)
  window.addEventListener('orientationchange', updateBannerHeight)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', updateBannerHeight)
  window.removeEventListener('orientationchange', updateBannerHeight)
})
</script>

<style scoped>
.travel-page {
  width: 100%;
  min-height: 100vh;
  background: #F9F9F9;
  font-family: "Microsoft YaHei", "PingFang SC", Arial, sans-serif;
  color: #333;
  position: relative;
}

/* Banner */
.banner {
  position: relative;
  width: 100%;
  height: var(--page-hero-height, 480px);
  overflow: hidden;
}

.banner-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.banner-mask {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.35);
  display: flex;
  align-items: center;
  justify-content: center;
}

.banner-content {
  text-align: center;
  padding: 0 20px;
  max-width: 800px;
  width: 100%;
}

.banner-title {
  font-size: 36px;
  color: #fff;
  font-weight: 700;
  text-shadow: 0 2px 12px rgba(0, 0, 0, 0.5);
  margin: 0 0 30px;
  line-height: 1.3;
}

.search-bar {
  display: flex;
  width: 100%;
  max-width: 600px;
  margin: 0 auto;
  height: 56px;
  background: #fff;
  border-radius: 28px;
  overflow: hidden;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.2);
}

.search-input {
  flex: 1;
  border: none;
  outline: none;
  padding: 0 24px;
  font-size: 16px;
  color: #333;
}

.search-input::placeholder {
  color: #bbb;
  font-size: 14px;
}

.search-btn {
  width: 100px;
  border: none;
  background: #f90;
  color: #fff;
  font-size: 16px;
  font-weight: 500;
  cursor: pointer;
  transition: background 0.2s;
}

.search-btn:hover {
  background: #f80;
}

/* 内容区 */
.content {
  max-width: 1100px;
  margin: 0 auto;
  padding: 30px 20px;
}

/* Tab */
.tab-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  padding: 0 30px;
  border-bottom: 1px solid #eee;
  border-radius: 8px 8px 0 0;
}

.tabs {
  display: flex;
  gap: 50px;
}

.tab {
  position: relative;
  padding: 20px 0;
  font-size: 16px;
  color: #666;
  cursor: pointer;
  transition: color 0.2s;
}

.tab:hover {
  color: #f90;
}

.tab.active {
  color: #f90;
  font-weight: 600;
}

.tab.active::after {
  content: '';
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  height: 3px;
  background: #f90;
  border-radius: 2px;
}

.filter-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 14px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
  color: #666;
  cursor: pointer;
  transition: all 0.2s;
}

.filter-btn:hover {
  color: #f90;
  border-color: #f90;
}

.filter-icon {
  font-size: 16px;
  font-weight: bold;
}

/* 攻略列表 */
.article-list {
  background: #fff;
  padding: 0 30px;
  min-height: 400px;
  border-radius: 0 0 8px 8px;
}

.article-item {
  display: flex;
  gap: 24px;
  padding: 24px 0;
  border-bottom: 1px solid #f0f0f0;
  cursor: pointer;
  transition: background 0.2s;
}

.article-item:hover {
  background: #fafafa;
}

.article-item:last-child {
  border-bottom: none;
}

.article-cover {
  position: relative;
  width: 240px;
  height: 160px;
  flex-shrink: 0;
  border-radius: 8px;
  overflow: hidden;
}

.article-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.essence-tag {
  position: absolute;
  top: 8px;
  left: 8px;
  background: #fff;
  color: #e63;
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 4px;
  font-weight: 600;
}

.essence-tag::before {
  content: '★ ';
  color: #e63;
}

.article-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.article-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.article-title {
  font-size: 18px;
  font-weight: 600;
  color: #222;
  margin: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex: 1;
}

.app-tag {
  flex-shrink: 0;
  background: #ffeac2;
  color: #f90;
  font-size: 12px;
  font-weight: 700;
  padding: 4px 14px;
  border-radius: 20px;
  border: 1px solid #ffd989;
}

.article-desc {
  font-size: 13px;
  color: #888;
  line-height: 1.7;
  margin: 0 0 16px;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.article-meta {
  display: flex;
  align-items: center;
  gap: 28px;
  margin-top: auto;
  font-size: 13px;
  color: #999;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 6px;
}

.icon-location,
.icon-view {
  color: #f90;
  font-size: 14px;
}

.avatar {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  object-fit: cover;
}

.author {
  color: #f90;
}

.meta-text {
  color: #999;
}

/* 空状态样式 */
.empty-state {
  text-align: center;
  padding: 80px 20px;
  color: #64748b;
}

.empty-icon {
  font-size: 64px;
  margin-bottom: 24px;
  opacity: 0.6;
}

.empty-title {
  font-size: 24px;
  font-weight: 600;
  margin: 0 0 12px;
  color: #2d3748;
}

.empty-desc {
  font-size: 16px;
  margin: 0 0 32px;
  opacity: 0.8;
}

.empty-action {
  border-radius: 8px;
  padding: 12px 32px;
  font-weight: 600;
  background: #f90;
  border: none;
}

.empty-action:hover {
  background: #f80;
}

/* 分页样式 */
.pagination-section {
  background: #fff;
  padding: 20px 30px;
  display: flex;
  justify-content: center;
  border-radius: 0 0 8px 8px;
}

.modern-pagination {
  :deep(.el-pagination) {
    .el-pager li {
      border-radius: 4px;
      margin: 0 4px;
      transition: all 0.3s ease;

      &:hover {
        background: #f90;
        color: white;
      }

      &.is-active {
        background: #f90;
        color: white;
      }
    }

    .btn-prev, .btn-next {
      border-radius: 4px;
      transition: all 0.3s ease;

      &:hover {
        background: #f90;
        color: white;
      }
    }
  }
}

/* 发布攻略悬浮按钮 */
.float-publish-btn {
  position: fixed;
  bottom: 40px;
  right: 40px;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 24px;
  background: linear-gradient(135deg, #f90 0%, #f80 100%);
  color: #fff;
  border-radius: 40px;
  cursor: pointer;
  box-shadow: 0 4px 15px rgba(255, 153, 0, 0.3);
  transition: all 0.3s ease;
  z-index: 100;
  font-weight: 600;
  border: none;
}

.float-publish-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(255, 153, 0, 0.4);
}

.float-publish-btn .el-icon {
  font-size: 18px;
}

/* 响应式 */
@media (max-width: 768px) {
  .banner {
    height: var(--page-hero-height, 320px);
  }
  
  .banner-title {
    font-size: 22px;
    margin-bottom: 20px;
  }
  
  .search-bar {
    height: 48px;
    border-radius: 24px;
  }
  
  .search-input {
    padding: 0 18px;
    font-size: 14px;
  }
  
  .search-btn {
    width: 80px;
    font-size: 14px;
  }
  
  .tab-bar {
    padding: 0 16px;
  }
  
  .tabs {
    gap: 20px;
  }
  
  .tab {
    padding: 14px 0;
    font-size: 14px;
  }
  
  .article-list {
    padding: 0 16px;
  }
  
  .article-item {
    flex-direction: column;
    gap: 12px;
  }
  
  .article-cover {
    width: 100%;
    height: 180px;
  }
  
  .article-title {
    font-size: 16px;
    white-space: normal;
    -webkit-line-clamp: 2;
    display: -webkit-box;
    -webkit-box-orient: vertical;
  }
  
  .article-meta {
    flex-wrap: wrap;
    gap: 16px;
  }
  
  .float-publish-btn {
    bottom: 20px;
    right: 20px;
    padding: 10px 20px;
    font-size: 14px;
  }
}
</style>
