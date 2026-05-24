<template>
  <div class="tickets-page">
    <!-- 筛选区域 - 移除了搜索框 -->
    <div class="filters-section" v-if="hasSearched">
      <div class="filters-container">
        <!-- 动态筛选条件行 - 只要有搜索结果就显示 -->
        <div class="filters-wrapper" v-if="hasSearched">
          <div class="filters-row">
            <!-- 行程类型筛选 - 动态生成 -->
            <div class="filter-item" v-if="availableFilters.tourTypes.length > 0">
              <span class="filter-label">行程类型：</span>
              <div class="filter-tags">
                <!-- 全部标签 -->
                <span
                  :class="['filter-tag', { active: !activeFilters.tourType }]"
                  @click="removeFilter('tourType')"
                >
                  全部
                  <span class="filter-count">({{ totalCount }})</span>
                </span>
                <span
                  v-for="item in availableFilters.tourTypes"
                  :key="item.value"
                  :class="['filter-tag', { active: activeFilters.tourType === item.value }]"
                  @click="toggleFilter('tourType', item.value)"
                >
                  {{ item.label }}
                  <span class="filter-count">({{ item.count }})</span>
                </span>
              </div>
            </div>

            <!-- 出发城市筛选 - 动态生成 -->
            <div class="filter-item" v-if="availableFilters.cities.length > 0">
              <span class="filter-label">出发城市：</span>
              <div class="filter-tags">
                <!-- 全部标签 -->
                <span
                  :class="['filter-tag', { active: !activeFilters.city }]"
                  @click="removeFilter('city')"
                >
                  全部
                  <span class="filter-count">({{ totalCount }})</span>
                </span>
                <span
                  v-for="item in availableFilters.cities"
                  :key="item.value"
                  :class="['filter-tag', { active: activeFilters.city === item.value }]"
                  @click="toggleFilter('city', item.value)"
                >
                  {{ item.label }}
                  <span class="filter-count">({{ item.count }})</span>
                </span>
              </div>
            </div>

            <!-- 目标地点筛选 - 动态生成 -->
            <div class="filter-item" v-if="availableFilters.destinations.length > 0">
              <span class="filter-label">目标地点：</span>
              <div class="filter-tags">
                <span
                  :class="['filter-tag', { active: !activeFilters.destination }]"
                  @click="removeFilter('destination')"
                >
                  全部
                  <span class="filter-count">({{ totalCount }})</span>
                </span>
                <span
                  v-for="item in availableFilters.destinations"
                  :key="item.value"
                  :class="['filter-tag', { active: activeFilters.destination === item.value }]"
                  @click="toggleFilter('destination', item.value)"
                >
                  {{ item.label }}
                  <span class="filter-count">({{ item.count }})</span>
                </span>
              </div>
            </div>

            <!-- 出行天数筛选 - 统一使用范围格式 -->
            <div class="filter-item" v-if="availableFilters.daysList.length > 0">
              <span class="filter-label">出行天数：</span>
              <div class="filter-tags">
                <span
                  :class="['filter-tag', { active: !activeFilters.days }]"
                  @click="removeFilter('days')"
                >
                  全部
                  <span class="filter-count">({{ totalCount }})</span>
                </span>
                <span
                  v-for="item in availableFilters.daysList"
                  :key="item.value"
                  :class="['filter-tag', { active: activeFilters.days === item.value }]"
                  @click="toggleFilter('days', item.value)"
                >
                  {{ item.label }}
                  <span class="filter-count">({{ item.count }})</span>
                </span>
              </div>
            </div>

            <!-- 出行时间筛选 - 动态生成 -->
            <div class="filter-item" v-if="availableFilters.months.length > 0">
              <span class="filter-label">出行时间：</span>
              <div class="filter-tags">
                <span
                  :class="['filter-tag', { active: !activeFilters.month }]"
                  @click="removeFilter('month')"
                >
                  全部
                  <span class="filter-count">({{ totalCount }})</span>
                </span>
                <span
                  v-for="item in availableFilters.months"
                  :key="item.value"
                  :class="['filter-tag', { active: activeFilters.month === item.value }]"
                  @click="toggleFilter('month', item.value)"
                >
                  {{ item.label }}
                  <span class="filter-count">({{ item.count }})</span>
                </span>
              </div>
            </div>

            <!-- 价格区间筛选 - 动态生成 -->
            <div class="filter-item" v-if="availableFilters.priceRanges.length > 0">
              <span class="filter-label">价格区间：</span>
              <div class="filter-tags">
                <span
                  :class="['filter-tag', { active: !activeFilters.priceRange }]"
                  @click="removeFilter('priceRange')"
                >
                  全部
                  <span class="filter-count">({{ totalCount }})</span>
                </span>
                <span
                  v-for="item in availableFilters.priceRanges"
                  :key="item.value"
                  :class="['filter-tag', { active: activeFilters.priceRange === item.value }]"
                  @click="toggleFilter('priceRange', item.value)"
                >
                  {{ item.label }}
                  <span class="filter-count">({{ item.count }})</span>
                </span>
              </div>
            </div>

            <!-- 产品主题筛选 - 动态生成 -->
            <div class="filter-item" v-if="availableFilters.themes.length > 0">
              <span class="filter-label">产品主题：</span>
              <div class="filter-tags">
                <span
                  :class="['filter-tag', { active: !activeFilters.theme }]"
                  @click="removeFilter('theme')"
                >
                  全部
                  <span class="filter-count">({{ totalCount }})</span>
                </span>
                <span
                  v-for="item in availableFilters.themes"
                  :key="item.value"
                  :class="['filter-tag', { active: activeFilters.theme === item.value }]"
                  @click="toggleFilter('theme', item.value)"
                >
                  {{ item.label }}
                  <span class="filter-count">({{ item.count }})</span>
                </span>
              </div>
            </div>
          </div>

          <!-- 已选筛选条件汇总 -->
          <div class="active-filters-bar" v-if="hasActiveFilters">
            <span class="active-label">已选条件：</span>
            <div class="active-tags">
              <span v-if="activeFilters.tourType" class="active-tag">
                {{ getTourTypeLabel(activeFilters.tourType) }}
                <el-icon @click="removeFilter('tourType')"><Close /></el-icon>
              </span>
              <span v-if="activeFilters.city" class="active-tag">
                {{ getCityLabel(activeFilters.city) }}
                <el-icon @click="removeFilter('city')"><Close /></el-icon>
              </span>
              <span v-if="activeFilters.destination" class="active-tag">
                {{ getDestinationLabel(activeFilters.destination) }}
                <el-icon @click="removeFilter('destination')"><Close /></el-icon>
              </span>
              <span v-if="activeFilters.days" class="active-tag">
                {{ getDaysLabel(activeFilters.days) }}
                <el-icon @click="removeFilter('days')"><Close /></el-icon>
              </span>
              <span v-if="activeFilters.month" class="active-tag">
                {{ getMonthLabel(activeFilters.month) }}
                <el-icon @click="removeFilter('month')"><Close /></el-icon>
              </span>
              <span v-if="activeFilters.priceRange" class="active-tag">
                {{ getPriceLabel(activeFilters.priceRange) }}
                <el-icon @click="removeFilter('priceRange')"><Close /></el-icon>
              </span>
              <span v-if="activeFilters.theme" class="active-tag">
                {{ getThemeLabel(activeFilters.theme) }}
                <el-icon @click="removeFilter('theme')"><Close /></el-icon>
              </span>
              <span class="clear-all" @click="clearAllFilters">清除全部</span>
            </div>
          </div>
        </div>

        <!-- 排序和结果统计 -->
        <div class="sort-bar" v-if="hasSearched">
          <div class="result-stats">
            <template v-if="searchKeyword">
              搜索“<strong class="search-keyword">{{ displaySearchKeyword }}</strong>” 共找到 <strong>{{ totalCount }}</strong> 个行程
            </template>
            <template v-else>
              共找到 <strong>{{ totalCount }}</strong> 个行程
            </template>
          </div>
          <div class="sort-options">
            <span class="sort-label">排序：</span>
            <div class="sort-tags">
              <span
                v-for="item in sortOptions"
                :key="item.value"
                :class="['sort-tag', { active: sortType === item.value }]"
                @click="sortType = item.value"
              >
                {{ item.label }}
              </span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 行程列表区域 -->
    <div class="tickets-list">
      <div
        v-for="item in ticketList"
        :key="item.id"
        class="ticket-card"
        @click="goToDetail(item.id)"
      >
        <!-- 左侧图片 -->
        <div class="card-image">
          <img :src="item.mainImage" :alt="item.title" />
          <span class="image-tag">{{ item.tag }}</span>
        </div>

        <!-- 中间信息 -->
        <div class="card-content">
          <div class="card-title">
            <div class="star-badge">
              <span class="num">{{ item.starRating || 4.8 }}</span>
              <span class="star">★</span>
            </div>
            <h3>{{ item.title }}</h3>
          </div>

          <!-- 出发城市 - 左对齐显示 -->
          <div class="card-row departure-row">
            <span class="row-label departure-label">出发城市：</span>
            <span class="row-departure">{{ item.subtitle }}</span>
          </div>

          <div class="card-row">
            <span class="row-label">推荐班期：</span>
            <span class="row-date">{{ item.recommendDate }}</span>
            <span class="row-label more-label">更多班期：</span>
            <span class="row-more">{{ item.moreDates }}</span>
          </div>

          <div class="card-row feature-row">
            <span class="row-label">班期特色：</span>
            <span class="row-feature">{{ item.feature }}</span>
          </div>

          <!-- 标签组 -->
          <div class="card-tags">
            <span class="card-tag" v-for="tag in item.tags" :key="tag">{{ tag }}</span>
          </div>
        </div>

        <!-- 右侧价格 - 底部对齐 -->
        <div class="card-price">
          <div class="price-wrapper">
            <span class="currency">¥</span>
            <span class="price">{{ formatPrice(item.minPrice) }}</span>
            <span class="unit">起/人</span>
          </div>
        </div>
      </div>

      <!-- 分页 -->
      <div class="pagination-container" v-if="totalCount > 0">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 30, 50]"
          :total="totalCount"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>

      <!-- 空状态 -->
      <div v-if="totalCount === 0 && hasSearched && !loading" class="empty-state">
        <el-empty :description="`没有找到“${displaySearchKeyword}”相关的行程`" />
        <el-button type="primary" plain @click="resetSearch">查看全部行程</el-button>
      </div>

      <!-- 初始状态 - 未搜索时显示热门推荐 -->
      <div v-if="!hasSearched && !loading" class="initial-state">
        <div class="hot-search">
          <span class="hot-title">热门推荐：</span>
          <span v-for="keyword in hotKeywords" :key="keyword.value" class="hot-tag" @click="quickSearch(keyword)">{{ keyword.label }}</span>
        </div>
        <div class="recommend-section">
          <h3>精选推荐</h3>
          <div class="recommend-list">
            <div class="recommend-card" v-for="item in recommendList" :key="item.id" @click="goToDetail(item.id)">
              <img :src="item.mainImage" :alt="item.title" />
              <div class="recommend-info">
                <h4>{{ item.title }}</h4>
                <p class="recommend-price">¥{{ formatPrice(item.minPrice) }}起</p>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 加载状态 -->
      <div v-if="loading" class="loading-state">
        <el-icon class="is-loading"><Loading /></el-icon>
        <span>加载中...</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch, nextTick } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Search, Loading, Close } from '@element-plus/icons-vue'
import { getTourPage, getTourFilters, getHotTourKeywords, getTicketFeaturedTours } from '@/api/tour'
void Search

const router = useRouter()
const route = useRoute()

// =============================================
// 常量定义
// =============================================
// 出发城市映射
const cityMap = {
  chongqing: '重庆', chengdu: '成都', kunming: '昆明', guiyang: '贵阳',
  sanya: '三亚', beijing: '北京', shanghai: '上海', changsha: '长沙',
  yichang: '宜昌', hangzhou: '杭州', haikou: '海口', wanzhou: '万州',
  fuling: '涪陵', enshi: '恩施', guangzhou: '广州', shenzhen: '深圳'
}

// 解析标签（处理字符串或数组格式）
const parseTags = (tags) => {
  if (!tags) return []
  if (Array.isArray(tags)) return tags
  if (typeof tags === 'string') {
    // 尝试解析JSON数组
    try {
      return JSON.parse(tags)
    } catch {
      // 如果不是JSON，按逗号分隔
      return tags.split(',').map(t => t.trim()).filter(t => t)
    }
  }
  return []
}

// 全国省市数据
const destinationOptions = [
  { code: 'beijing', name: '北京', cities: [{ code: 'beijing', name: '北京市' }] },
  { code: 'tianjin', name: '天津', cities: [{ code: 'tianjin', name: '天津市' }] },
  { code: 'hebei', name: '河北', cities: [
    { code: 'shijiazhuang', name: '石家庄' }, { code: 'tangshan', name: '唐山' },
    { code: 'qinhuangdao', name: '秦皇岛' }, { code: 'baoding', name: '保定' },
    { code: 'zhangjiakou', name: '张家口' }, { code: 'chengde', name: '承德' }
  ]},
  { code: 'shanxi', name: '山西', cities: [
    { code: 'taiyuan', name: '太原' }, { code: 'datong', name: '大同' },
    { code: 'pingyao', name: '平遥' }, { code: 'lvliang', name: '吕梁' }
  ]},
  { code: 'neimenggu', name: '内蒙古', cities: [
    { code: 'huhehaote', name: '呼和浩特' }, { code: 'baotou', name: '包头' },
    { code: 'orodos', name: '鄂尔多斯' }, { code: 'xilingol', name: '锡林郭勒' }
  ]},
  { code: 'liaoning', name: '辽宁', cities: [
    { code: 'shenyang', name: '沈阳' }, { code: 'dalian', name: '大连' },
    { code: 'anshan', name: '鞍山' }, { code: 'yingkou', name: '营口' }
  ]},
  { code: 'jilin', name: '吉林', cities: [
    { code: 'changchun', name: '长春' }, { code: 'jilin', name: '吉林' },
    { code: 'siping', name: '四平' }
  ]},
  { code: 'heilongjiang', name: '黑龙江', cities: [
    { code: 'harbin', name: '哈尔滨' }, { code: 'daqing', name: '大庆' },
    { code: 'mudanjiang', name: '牡丹江' }, { code: 'jixi', name: '鸡西' }
  ]},
  { code: 'shanghai', name: '上海', cities: [{ code: 'shanghai', name: '上海市' }] },
  { code: 'jiangsu', name: '江苏', cities: [
    { code: 'nanjing', name: '南京' }, { code: 'suzhou', name: '苏州' },
    { code: 'wuxi', name: '无锡' }, { code: 'yangzhou', name: '扬州' },
    { code: 'xuzhou', name: '徐州' }, { code: 'lianyungang', name: '连云港' }
  ]},
  { code: 'zhejiang', name: '浙江', cities: [
    { code: 'hangzhou', name: '杭州' }, { code: 'ningbo', name: '宁波' },
    { code: 'wenzhou', name: '温州' }, { code: 'shaoxing', name: '绍兴' },
    { code: 'jinhua', name: '金华' }, { code: 'lishui', name: '丽水' }
  ]},
  { code: 'anhui', name: '安徽', cities: [
    { code: 'hefei', name: '合肥' }, { code: 'huangshan', name: '黄山' },
    { code: 'bengbu', name: '蚌埠' }, { code: 'fuyang', name: '阜阳' }
  ]},
  { code: 'fujian', name: '福建', cities: [
    { code: 'fuzhou', name: '福州' }, { code: 'xiamen', name: '厦门' },
    { code: 'quanzhou', name: '泉州' }, { code: 'nanping', name: '南平' }
  ]},
  { code: 'jiangxi', name: '江西', cities: [
    { code: 'nanchang', name: '南昌' }, { code: 'jiujiang', name: '九江' },
    { code: 'jingdezhen', name: '景德镇' }, { code: 'shangrao', name: '上饶' }
  ]},
  { code: 'shandong', name: '山东', cities: [
    { code: 'jinan', name: '济南' }, { code: 'qingdao', name: '青岛' },
    { code: 'yantai', name: '烟台' }, { code: 'weihai', name: '威海' },
    { code: 'taian', name: '泰安' }
  ]},
  { code: 'henan', name: '河南', cities: [
    { code: 'zhengzhou', name: '郑州' }, { code: 'luoyang', name: '洛阳' },
    { code: 'kaifeng', name: '开封' }, { code: 'anyang', name: '安阳' }
  ]},
  { code: 'hubei', name: '湖北', cities: [
    { code: 'wuhan', name: '武汉' }, { code: 'yichang', name: '宜昌' },
    { code: 'shennongjia', name: '神农架' }, { code: 'xiangyang', name: '襄阳' }
  ]},
  { code: 'hunan', name: '湖南', cities: [
    { code: 'changsha', name: '长沙' }, { code: 'zhangjiajie', name: '张家界' },
    { code: 'xiangxi', name: '湘西' }, { code: 'hengyang', name: '衡阳' }
  ]},
  { code: 'guangdong', name: '广东', cities: [
    { code: 'guangzhou', name: '广州' }, { code: 'shenzhen', name: '深圳' },
    { code: 'zhuhai', name: '珠海' }, { code: 'foshan', name: '佛山' },
    { code: 'chaozhou', name: '潮州' }
  ]},
  { code: 'guangxi', name: '广西', cities: [
    { code: 'nanning', name: '南宁' }, { code: 'guilin', name: '桂林' },
    { code: 'liuzhou', name: '柳州' }, { code: 'beihai', name: '北海' },
    { code: 'baise', name: '百色' }
  ]},
  { code: 'hainan', name: '海南', cities: [
    { code: 'haikou', name: '海口' }, { code: 'sanya', name: '三亚' },
    { code: 'wuzhishan', name: '五指山' }, { code: 'qionghai', name: '琼海' }
  ]},
  { code: 'chongqing', name: '重庆', cities: [{ code: 'chongqing', name: '重庆市' }] },
  { code: 'sichuan', name: '四川', cities: [
    { code: 'chengdu', name: '成都' }, { code: 'jiuzhaigou', name: '九寨沟' },
    { code: 'leshan', name: '乐山' }, { code: 'garze', name: '甘孜' },
    { code: 'aba', name: '阿坝' }
  ]},
  { code: 'guizhou', name: '贵州', cities: [
    { code: 'guiyang', name: '贵阳' }, { code: 'zunyi', name: '遵义' },
    { code: 'liupanshui', name: '六盘水' }, { code: 'qianxinan', name: '黔西南' }
  ]},
  { code: 'yunnan', name: '云南', cities: [
    { code: 'kunming', name: '昆明' }, { code: 'dali', name: '大理' },
    { code: 'lijiang', name: '丽江' }, { code: 'shangri-la', name: '香格里拉' },
    { code: 'xishuangbanna', name: '西双版纳' }
  ]},
  { code: 'xizang', name: '西藏', cities: [
    { code: 'lhasa', name: '拉萨' }, { code: 'shigatse', name: '日喀则' },
    { code: 'nyingchi', name: '林芝' }, { code: 'shannan', name: '山南' }
  ]},
  { code: 'shaanxi', name: '陕西', cities: [
    { code: 'xian', name: '西安' }, { code: 'yanan', name: '延安' },
    { code: 'xianyang', name: '咸阳' }, { code: 'hanzhong', name: '汉中' }
  ]},
  { code: 'gansu', name: '甘肃', cities: [
    { code: 'lanzhou', name: '兰州' }, { code: 'tianshui', name: '天水' },
    { code: 'jiayuguan', name: '嘉峪关' }, { code: 'zhangye', name: '张掖' },
    { code: 'dunhuang', name: '敦煌' }
  ]},
  { code: 'qinghai', name: '青海', cities: [
    { code: 'xining', name: '西宁' }, { code: 'hainan', name: '海南州' },
    { code: 'haixi', name: '海西' }
  ]},
  { code: 'ningxia', name: '宁夏', cities: [
    { code: 'yinchuan', name: '银川' }, { code: 'shizuishan', name: '石嘴山' },
    { code: 'guyuan', name: '固原' }
  ]},
  { code: 'xinjiang', name: '新疆', cities: [
    { code: 'wulumuqi', name: '乌鲁木齐' }, { code: 'turpan', name: '吐鲁番' },
    { code: 'kashgar', name: '喀什' }, { code: 'yili', name: '伊犁' }
  ]},
  { code: 'taiwan', name: '台湾', cities: [
    { code: 'taipei', name: '台北' }, { code: 'kaohsiung', name: '高雄' },
    { code: 'taichung', name: '台中' }
  ]},
  { code: 'xianggang', name: '香港', cities: [{ code: 'xianggang', name: '香港' }] },
  { code: 'aomen', name: '澳门', cities: [{ code: 'aomen', name: '澳门' }] }
]

// 获取所有城市选项
const allCityOptions = computed(() => {
  const options = []
  destinationOptions.forEach(province => {
    province.cities.forEach(city => {
      options.push({ code: city.code, name: `${province.name} · ${city.name}` })
    })
  })
  return options
})

// 特殊目的地代码映射
const specialDestinationMap = {
  'xisha': '西沙群岛',
  'sanxia': '三峡',
  'sanyan': '三峡',
  'sanya': '三亚',
  'chongqing': '重庆',
  'chengdu': '成都',
  'kunming': '昆明',
  'guiyang': '贵阳',
  'lijiang': '丽江',
  'dali': '大理',
  'xiamen': '厦门',
  'hangzhou': '杭州',
  'suzhou': '苏州',
  'nanjing': '南京',
  'guilin': '桂林',
  'beihai': '北海'
}

// 获取目的地标签
const getDestinationLabel = (dest) => {
  if (!dest) return ''
  // 先检查特殊目的地映射
  if (specialDestinationMap[dest]) {
    return specialDestinationMap[dest]
  }
  if (dest.includes('/')) {
    const [provinceCode, cityCode] = dest.split('/')
    const province = destinationOptions.find(p => p.code === provinceCode)
    const city = province?.cities.find(c => c.code === cityCode)
    return city ? `${province.name} · ${city.name}` : dest
  }
  const option = allCityOptions.value.find(o => o.code === dest)
  if (option) return option.name
  const province = destinationOptions.find(p => p.code === dest)
  return province ? province.name : dest
}

const daysMap = {
  // 周边游精确天数
  '1': '1天', '2': '2天', '3': '3天', '4': '4天', '5': '5天',
  // 普通行程范围
  '1-3': '1-3天', '4-6': '4-6天', '7-9': '7-9天', '10+': '10天以上'
}

const monthMap = {
  '1': '1月', '2': '2月', '3': '3月', '4': '4月', '5': '5月',
  '6': '6月', '7': '7月', '8': '8月', '9': '9月', '10': '10月',
  '11': '11月', '12': '12月'
}

const priceMap = {
  '0-500': '¥500以下', '500-1000': '¥500-1000',
  '1000-2000': '¥1000-2000', '2000+': '¥2000以上'
}

// 排序选项
const sortOptions = ref([
  { value: 'default', label: '默认排序' },
  { value: 'price_asc', label: '价格升序' },
  { value: 'price_desc', label: '价格降序' },
  { value: 'popular', label: '人气最高' }
])

// =============================================
// 响应式数据
// =============================================
const searchKeyword = ref('')
const searchDisplayKeyword = ref('')
const hasSearched = ref(false)
const loading = ref(false)
const initialLoading = ref(false)
const sortType = ref('default')

// 分页
const currentPage = ref(1)
const pageSize = ref(10)

// 当前活跃的筛选条件
const activeFilters = ref({
  tourType: '',
  city: '',
  destination: '',
  days: '',
  month: '',
  priceRange: '',
  theme: ''
})

// 基于当前搜索结果可用的筛选选项
const availableFilters = ref({
  tourTypes: [],
  cities: [],
  destinations: [],
  daysList: [],
  months: [],
  priceRanges: [],
  themes: []
})

const ticketList = ref([])
const recommendList = ref([])
const hotKeywords = ref([])

// 总数量（用于全部标签显示）
const totalCount = ref(0)

// 数据加载完成标志
const isDataLoaded = ref(false)

// =============================================
// 辅助函数：获取标签显示名称
// =============================================
const getCityLabel = (value) => cityMap[value] || value
const getDaysLabel = (value) => daysMap[value] || value
const getMonthLabel = (value) => monthMap[value] || value + '月'
const getPriceLabel = (value) => priceMap[value] || value
const getThemeLabel = (value) => value || ''
const getTourTypeLabel = (value) => value || ''
const displaySearchKeyword = computed(() => searchDisplayKeyword.value || searchKeyword.value)

const decorateFilterItems = (items = [], labelGetter) => {
  return items.map(item => ({
    ...item,
    label: labelGetter(item.value)
  }))
}

const applyFilterLabels = data => {
  availableFilters.value = {
    tourTypes: decorateFilterItems(data?.tourTypes, getTourTypeLabel),
    cities: decorateFilterItems(data?.cities, getCityLabel),
    destinations: decorateFilterItems(data?.destinations, getDestinationLabel),
    daysList: decorateFilterItems(data?.daysList, getDaysLabel),
    months: decorateFilterItems(data?.months, getMonthLabel),
    priceRanges: decorateFilterItems(data?.priceRanges, getPriceLabel),
    themes: decorateFilterItems(data?.themes, getThemeLabel)
  }
}

const normalizeTours = records => (records || []).map(item => ({
  ...item,
  tags: parseTags(item.tags)
}))

const buildQueryParams = () => ({
  keyword: searchKeyword.value.trim(),
  tourType: activeFilters.value.tourType,
  city: activeFilters.value.city,
  destination: activeFilters.value.destination,
  days: activeFilters.value.days,
  month: activeFilters.value.month,
  priceRange: activeFilters.value.priceRange,
  theme: activeFilters.value.theme,
  sortType: sortType.value,
  currentPage: currentPage.value,
  pageSize: pageSize.value
})

const loadFilters = async () => {
  const filters = await getTourFilters({ keyword: searchKeyword.value.trim() })
  applyFilterLabels(filters || {})
}

const fetchTickets = async ({ scroll = false } = {}) => {
  loading.value = true
  try {
    const res = await getTourPage(buildQueryParams())
    ticketList.value = normalizeTours(res?.records)
    totalCount.value = Number(res?.total || 0)
    hasSearched.value = true
    isDataLoaded.value = true
    if (scroll) {
      setTimeout(() => {
        window.scrollTo({ top: 220, behavior: 'smooth' })
      }, 120)
    }
  } catch (error) {
    console.error('获取行程列表失败:', error)
    ElMessage.error('获取行程数据失败，请稍后重试')
    ticketList.value = []
    totalCount.value = 0
  } finally {
    loading.value = false
  }
}

const performSearch = async ({ resetPage = true, scroll = true } = {}) => {
  if (resetPage) {
    currentPage.value = 1
  }
  await Promise.all([
    loadFilters(),
    fetchTickets({ scroll })
  ])
}

// 是否有活跃的筛选条件
const hasActiveFilters = computed(() => {
  return activeFilters.value.tourType ||
         activeFilters.value.city ||
         activeFilters.value.destination ||
         activeFilters.value.days ||
         activeFilters.value.month ||
         activeFilters.value.priceRange ||
         activeFilters.value.theme
})

// =============================================
// 方法
// =============================================

// 格式化价格
const formatPrice = (price) => {
  if (!price) return '0'
  return price.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ',')
}

// 跳转到详情页
const goToDetail = (id) => {
  if (id) {
    router.push(`/ticket/booking/${id}`)
  }
}

// 执行搜索
const executeSearch = () => {
  performSearch()
}

// 快速搜索（热门词）
const quickSearch = (keyword) => {
  searchKeyword.value = keyword.value
  searchDisplayKeyword.value = keyword.label || keyword.value
  executeSearch()
}

// 切换筛选条件
const toggleFilter = (type, value) => {
  if (activeFilters.value[type] === value) {
    // 如果点击的是已选中的，则取消
    activeFilters.value[type] = ''
  } else {
    // 否则选中新的
    activeFilters.value[type] = value
  }
  performSearch()
}

// 移除某个筛选条件
const removeFilter = (type) => {
  activeFilters.value[type] = ''
  performSearch()
}

// 清除所有筛选条件
const clearAllFilters = () => {
  activeFilters.value = { tourType: '', city: '', destination: '', days: '', month: '', priceRange: '', theme: '' }
  performSearch()
}

// 重置搜索（回到初始状态）
const resetSearch = () => {
  searchKeyword.value = ''
  searchDisplayKeyword.value = ''
  activeFilters.value = { tourType: '', city: '', destination: '', days: '', month: '', priceRange: '', theme: '' }
  hasSearched.value = false
  sortType.value = 'default'
  currentPage.value = 1
  ticketList.value = []
  totalCount.value = 0
}

// 分页处理
const handleSizeChange = (size) => {
  pageSize.value = size
  currentPage.value = 1
  fetchTickets()
}

const handleCurrentChange = (page) => {
  currentPage.value = page
  fetchTickets()
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

// 从URL参数初始化搜索
const initFromUrl = () => {
  const searchParam = route.query.search
  const tourTypeParam = route.query.tourType
  const cityParam = route.query.city
  const daysParam = route.query.days
  const destinationParam = route.query.destination

  let hasParams = false

  // 处理搜索关键词
  if (searchParam) {
    searchKeyword.value = decodeURIComponent(searchParam)
    searchDisplayKeyword.value = searchKeyword.value
    hasParams = true
  }

  // 处理行程类型筛选
  if (tourTypeParam) {
    activeFilters.value.tourType = tourTypeParam
    hasParams = true
  }

  // 处理出发城市筛选
  if (cityParam) {
    activeFilters.value.city = cityParam
    hasParams = true
  }

  // 处理天数筛选
  if (daysParam) {
    activeFilters.value.days = daysParam
    hasParams = true
  }

  // 处理目的地筛选
  if (destinationParam) {
    activeFilters.value.destination = destinationParam
    hasParams = true
  }

  if (hasParams) {
    performSearch({ scroll: false })
  }
}

const loadInitialData = async () => {
  initialLoading.value = true
  try {
    const [keywords, featured] = await Promise.all([
      getHotTourKeywords(),
      getTicketFeaturedTours(),
      loadFilters()
    ])
    hotKeywords.value = Array.isArray(keywords)
      ? keywords.map(item => typeof item === 'string' ? { value: item, label: getDestinationLabel(item) || item } : item)
      : []
    recommendList.value = normalizeTours(featured)
  } catch (error) {
    console.error('获取行程预订初始数据失败:', error)
  } finally {
    initialLoading.value = false
    isDataLoaded.value = true
    nextTick(() => {
      initFromUrl()
    })
  }
}

watch(sortType, () => {
  if (hasSearched.value) {
    performSearch()
  }
})

// 监听路由变化，当搜索参数改变时重新搜索
watch(() => route.query, (query) => {
  const newSearch = query.search
  const newTourType = query.tourType
  const newCity = query.city
  const newDays = query.days
  const newDestination = query.destination

  // 如果有URL参数需要搜索，先清除所有现有筛选条件
  const hasParams = newSearch || newTourType || newCity || newDays || newDestination

  if (hasParams) {
    // 重置所有筛选条件（复用 resetSearch 函数）
    resetSearch()

    // 应用新的搜索关键词
    if (newSearch) {
      searchKeyword.value = decodeURIComponent(newSearch)
      searchDisplayKeyword.value = searchKeyword.value
    }

    // 应用新的筛选条件
    if (newTourType) {
      activeFilters.value.tourType = newTourType
    }
    if (newCity) {
      activeFilters.value.city = newCity
    }
    if (newDays) {
      activeFilters.value.days = newDays
    }
    if (newDestination) {
      activeFilters.value.destination = newDestination
    }

    hasSearched.value = true
    performSearch({ scroll: false })
  }
}, { deep: true })

// =============================================
// 生命周期
// =============================================
onMounted(() => {
  loadInitialData()
})
</script>

<style scoped>
.tickets-page {
  font-family: 'Microsoft YaHei', 'PingFang SC', sans-serif;
  background: #FFFFFF;
  min-height: 100vh;
}

/* =============================================
   筛选区域样式
============================================= */
.filters-section {
  background: #fff;
  border-bottom: 1px solid #e8e8e8;
  position: sticky;
  top: 0;
  z-index: 99;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.filters-container {
  max-width: 1320px;
  margin: 0 auto;
  padding: 20px;
}

/* 筛选条件外层橙色细框 */
.filters-wrapper {
  border: 1px solid #ffa500;
  border-radius: 12px;
  margin-bottom: 20px;
  padding: 16px 20px;
}

.filters-row {
  display: flex;
  flex-direction: column;
  gap: 0;
}

/* 每个筛选条件行 - 虚线分隔 */
.filter-item {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  flex-wrap: wrap;
  padding: 12px 0;
  border-bottom: 1px dashed #e0e0e0;
}

.filter-item:last-child {
  border-bottom: none;
}

.filter-label {
  font-size: 14px;
  color: #666;
  width: 80px;
  flex-shrink: 0;
  line-height: 32px;
  font-weight: 500;
}

.filter-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  flex: 1;
}

.filter-tag {
  display: inline-block;
  padding: 6px 16px;
  font-size: 13px;
  color: #666;
  background: #FFFFFF;
  border-radius: 24px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.filter-tag:hover {
  background: #ffede0;
  color: #f60;
}

.filter-tag.active {
  background: #FF9A00;
  color: #fff;
}

.filter-count {
  font-size: 11px;
  margin-left: 4px;
  opacity: 0.7;
}

/* 已选条件栏 */
.active-filters-bar {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
  padding: 12px 0 0 0;
  margin-top: 8px;
  border-top: 1px solid #f0f0f0;
}

.active-label {
  font-size: 13px;
  color: #666;
}

.active-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.active-tag {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 12px;
  font-size: 12px;
  color: #f60;
  background: #fff5f0;
  border-radius: 16px;
  border: 1px solid #ffe0c0;
}

.active-tag .el-icon {
  cursor: pointer;
  font-size: 14px;
}

.active-tag .el-icon:hover {
  color: #e55a00;
}

.clear-all {
  font-size: 12px;
  color: #999;
  cursor: pointer;
  padding: 4px 8px;
}

.clear-all:hover {
  color: #f60;
}

/* 排序栏 */
.sort-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 16px;
}

.result-stats {
  font-size: 14px;
  color: #666;
}

.result-stats strong {
  color: #f60;
  font-size: 18px;
}

.search-keyword {
  color: #e94560;
  font-size: 16px;
}

.sort-options {
  display: flex;
  align-items: center;
  gap: 12px;
}

.sort-label {
  font-size: 14px;
  color: #666;
}

.sort-tags {
  display: flex;
  gap: 8px;
}

.sort-tag {
  padding: 6px 16px;
  font-size: 13px;
  color: #666;
  background: #FFFFFF;
  border-radius: 24px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.sort-tag:hover {
  background: #ffede0;
  color: #f60;
}

.sort-tag.active {
  background: #FF9A00;
  color: #fff;
}

/* =============================================
   行程列表样式
============================================= */
.tickets-list {
  max-width: 1320px;
  margin: 0 auto;
  padding: 20px;
}

.ticket-card {
  display: flex;
  background: #fff;
  border-radius: 12px;
  overflow: hidden;
  margin-bottom: 16px;
  cursor: pointer;
  transition: all 0.2s ease;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.ticket-card:hover {
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
  transform: translateY(-2px);
}

.card-image {
  position: relative;
  width: 300px;
  height: 200px;
  flex-shrink: 0;
  overflow: hidden;
  background: #f0f0f0;
}

.card-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s ease;
}

.ticket-card:hover .card-image img {
  transform: scale(1.05);
}

.image-tag {
  position: absolute;
  top: 0;
  left: 0;
  background: linear-gradient(135deg, #FF9A00, #ff8c42);
  color: #fff;
  font-size: 12px;
  padding: 4px 12px;
  border-bottom-right-radius: 12px;
  font-weight: 500;
}

.card-content {
  flex: 1;
  padding: 16px 20px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  min-width: 0;
}

.card-title {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  margin-bottom: 4px;
}

.star-badge {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  background: linear-gradient(135deg, #ff9a3c, #FF9A00);
  color: #fff;
  font-size: 12px;
  padding: 2px 10px;
  border-radius: 16px;
  flex-shrink: 0;
}

.star-badge .star {
  font-size: 11px;
}

.card-title h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #222;
  line-height: 1.4;
}

.card-row {
  display: flex;
  align-items: flex-start;
  font-size: 13px;
  color: #666;
  flex-wrap: wrap;
  gap: 4px;
  line-height: 1.5;
}

/* 出发城市行 - 左对齐 */
.departure-row {
  margin-bottom: 2px;
}

.departure-label {
  color: #999;
  flex-shrink: 0;
}

.row-departure {
  color: #333;
  font-weight: 500;
}

.row-label {
  color: #999;
  flex-shrink: 0;
}

.more-label {
  margin-left: 24px;
}

.row-date {
  color: #333;
  font-weight: 500;
}

.row-more {
  color: #666;
}

/* 班期特色行 - 左对齐且可换行 */
.feature-row {
  align-items: flex-start;
}

.feature-row .row-feature {
  color: #f60;
  flex: 0 1 auto;
  min-width: 0;
  word-break: break-word;
  white-space: normal;
  line-height: 1.5;
}

.card-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 4px;
}

.card-tag {
  display: inline-block;
  padding: 2px 10px;
  font-size: 11px;
  color: #f60;
  background: #fff5f0;
  border-radius: 12px;
  border: 1px solid #ffe0c0;
}

/* 右侧价格区域 - 底部对齐 */
.card-price {
  width: 150px;
  height: 200px;
  flex-shrink: 0;
  display: flex;
  align-items: flex-end;
  justify-content: center;
  padding-bottom: 20px;
  background: #fefaf5;
  border-left: 1px solid #f0f0f0;
}

.price-wrapper {
  text-align: center;
}

.card-price .currency {
  font-size: 16px;
  font-weight: 500;
  color: #f60;
}

.card-price .price {
  font-size: 28px;
  font-weight: 700;
  line-height: 1;
  margin: 0 2px;
  color: #f60;
}

.card-price .unit {
  font-size: 12px;
  color: #999;
}

/* 初始状态样式 */
.initial-state {
  padding: 20px 0;
}

.hot-search {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
  padding: 16px 20px;
  background: #fff;
  border-radius: 12px;
  margin-bottom: 24px;
}

.hot-title {
  font-size: 14px;
  color: #666;
  font-weight: 500;
}

.hot-tag {
  padding: 6px 16px;
  font-size: 13px;
  color: #f60;
  background: #fff5f0;
  border-radius: 24px;
  cursor: pointer;
  transition: all 0.2s;
}

.hot-tag:hover {
  background: #f60;
  color: #fff;
}

.recommend-section h3 {
  font-size: 18px;
  font-weight: 600;
  color: #333;
  margin-bottom: 16px;
}

.recommend-list {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
}

.recommend-card {
  background: #fff;
  border-radius: 12px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.2s;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.recommend-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
}

.recommend-card img {
  width: 100%;
  height: 160px;
  object-fit: cover;
}

.recommend-info {
  padding: 12px;
}

.recommend-info h4 {
  font-size: 14px;
  font-weight: 600;
  color: #333;
  margin: 0 0 8px 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.recommend-price {
  font-size: 16px;
  font-weight: 600;
  color: #f60;
  margin: 0;
}

/* 分页样式 */
.pagination-container {
  display: flex;
  justify-content: center;
  margin-top: 32px;
  padding: 20px 0;
}

/* 空状态 */
.empty-state {
  text-align: center;
  padding: 80px 0;
  background: #fff;
  border-radius: 12px;
}

.loading-state {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 12px;
  padding: 80px 0;
  color: #999;
}

/* 响应式 */
@media (max-width: 1024px) {
  .recommend-list {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .ticket-card {
    flex-direction: column;
  }
  
  .card-image {
    width: 100%;
    height: 200px;
  }
  
  .card-price {
    width: 100%;
    padding: 16px 20px;
    justify-content: flex-start;
    border-left: none;
    border-top: 1px solid #f0f0f0;
  }
  
  .price-wrapper {
    display: flex;
    align-items: baseline;
    gap: 4px;
  }
  
  .more-label {
    margin-left: 8px;
  }
  
  .sort-bar {
    flex-direction: column;
    align-items: flex-start;
  }
  
  .recommend-list {
    grid-template-columns: 1fr;
  }
  
  .filter-label {
    width: 70px;
  }
  
  .filters-wrapper {
    padding: 12px 16px;
  }
  
  /* 班期特色行 - 左对齐，描述跟在标签后面 */
  .feature-row {
    align-items: flex-start;
  }

  .feature-row .row-feature {
    color: #f60;
    flex: 0 1 auto;
    min-width: 0;
    word-break: break-word;
    white-space: normal;
    line-height: 1.5;
  }
}
</style>
