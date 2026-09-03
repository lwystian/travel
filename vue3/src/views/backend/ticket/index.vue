<template>
  <div class="tour-management">
    <div class="page-header">
      <h1 class="page-title">行程管理</h1>
      <p class="page-subtitle">{{ isMiniappMode ? '小程序商品展示管理' : '本地行程商品管理' }}</p>
    </div>

    <div v-if="!isMiniappMode" class="action-bar">
      <div class="action-right">
        <el-button type="primary" @click="showAddDialog" class="add-btn">
          <el-icon><Plus /></el-icon> 添加行程
        </el-button>
      </div>
    </div>

    <!-- 搜索表单 -->
    <el-card shadow="never" class="search-card">
      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="行程名称" class="search-item">
          <el-input v-model="searchForm.title" placeholder="请输入行程名称" clearable style="width: 140px;"></el-input>
        </el-form-item>
        <el-form-item label="行程类型" class="search-item">
          <el-select v-model="searchForm.tourType" placeholder="请选择" clearable style="width: 130px;">
            <el-option v-for="item in tourTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="出发城市" class="search-item">
          <el-cascader
            v-model="searchForm.cityPath"
            :options="chinaRegionOptions"
            :props="regionCascaderProps"
            placeholder="请选择"
            clearable
            filterable
            popper-class="region-cascader-popper"
            style="width: 180px;"
            @change="handleSearchCityChange"
            @expand-change="handleSearchCityExpandChange"
          />
        </el-form-item>
        <el-form-item label="目的地" class="search-item">
          <el-cascader
            v-model="searchForm.destinationPath"
            :options="chinaRegionOptions"
            :props="regionCascaderProps"
            placeholder="请选择或搜索"
            clearable
            filterable
            popper-class="region-cascader-popper"
            style="width: 160px;"
            @change="handleSearchDestinationChange"
            @expand-change="handleSearchDestinationExpandChange"
          />
        </el-form-item>
        <el-form-item class="search-buttons">
          <el-button type="primary" @click="handleSearch">
            <el-icon><Search /></el-icon> 查询
          </el-button>
          <el-button @click="resetSearch">
            <el-icon><Refresh /></el-icon> 重置
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 行程列表表格 -->
    <el-card shadow="never" class="table-card">
      <section v-if="isMiniappMode && tourList.length" class="mini-sort-panel">
        <header>
          <div>
            <strong>前台展示顺序</strong>
            <span>拖动当前页商品调整官网行程列表顺序，松开后自动保存。</span>
          </div>
          <el-tag v-if="miniSortSaving" type="warning" effect="plain">正在保存</el-tag>
          <el-tag v-else type="success" effect="plain">已同步后端</el-tag>
        </header>
        <div class="mini-sort-list">
          <article
            v-for="(item, index) in tourList"
            :key="item.sourceId || item.id"
            :class="['mini-sort-item', { dragging: miniDraggedIndex === index }]"
            draggable="true"
            @dragstart="handleMiniDragStart($event, index)"
            @dragover.prevent
            @drop.prevent="handleMiniDrop(index)"
            @dragend="miniDraggedIndex = -1"
          >
            <el-icon class="drag-handle" title="拖动排序"><Rank /></el-icon>
            <span class="sort-position">{{ (currentPage - 1) * pageSize + index + 1 }}</span>
            <img :src="item.mainImage" alt="" @error="event => event.target.style.visibility = 'hidden'" />
            <strong :title="item.title">{{ item.title }}</strong>
            <el-tag :type="item.websiteVisible ? 'success' : 'info'" size="small">
              {{ item.websiteVisible ? '官网展示' : '官网隐藏' }}
            </el-tag>
          </article>
        </div>
      </section>

      <el-table :data="tourList" border style="width: 100%" v-loading="loading" header-align="center">
        <el-table-column :label="isMiniappMode ? '小程序ID' : 'ID'" :width="isMiniappMode ? 150 : 70" align="center" header-align="center">
          <template #default="scope">
            <span class="source-id" :title="String(scope.row.sourceId || scope.row.id)">
              {{ scope.row.sourceId || scope.row.id }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="code" label="行程编号" width="120" align="center" header-align="center">
          <template #default="scope">
            <el-tag v-if="scope.row.code" size="small" effect="plain">{{ scope.row.code }}</el-tag>
            <span v-else class="muted-text">待生成</span>
          </template>
        </el-table-column>
        <el-table-column label="行程封面" width="120" align="center" header-align="center">
          <template #default="scope">
            <el-image
              v-if="scope.row.mainImage"
              :src="scope.row.mainImage"
              fit="cover"
              style="width: 80px; height: 60px; border-radius: 4px;"
              :preview-src-list="[scope.row.mainImage]"
              :preview-teleported="true"
              :z-index="3000"
            />
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="行程名称" min-width="200">
          <template #default="scope">
            <div class="tour-title">
              <span class="title-text">{{ scope.row.title }}</span>
              <el-tag v-if="scope.row.tourType" size="small" type="primary">{{ getTourTypeLabel(scope.row.tourType) }}</el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="subtitle" label="副标题" min-width="120" show-overflow-tooltip align="center" header-align="center" />
        <el-table-column label="出发地/目的地" width="140" align="center" header-align="center">
          <template #default="scope">
            <span>{{ getCityLabel(scope.row.city) }}</span>
            <span v-if="scope.row.destination"> → {{ getDestinationLabel(scope.row.destination) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="行程天数" width="90" align="center" header-align="center">
          <template #default="scope">
            {{ scope.row.days }}天{{ scope.row.days > 1 ? (scope.row.days - 1) + '晚' : '' }}
          </template>
        </el-table-column>
        <el-table-column label="最低价" width="100" align="center" header-align="center">
          <template #default="scope">
            <span v-if="scope.row.pricingMode === 'inquiry'" class="muted-text">客服咨询</span>
            <span v-else class="price">¥{{ scope.row.minPrice }}</span>
          </template>
        </el-table-column>
        <el-table-column label="评分" width="80" align="center" header-align="center">
          <template #default="scope">
            <el-rate v-model="scope.row.starRating" disabled text-color="#ff9900" />
          </template>
        </el-table-column>
        <el-table-column label="标签" width="150" align="center" header-align="center">
          <template #default="scope">
            <el-tag v-for="tag in parseTags(scope.row.tags)" :key="tag" size="small" style="margin-right: 4px; margin-bottom: 2px;">{{ tag }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="isMiniappMode ? '小程序状态' : '状态'" width="100" align="center" header-align="center">
          <template #default="scope">
            <el-tag :type="(isMiniappMode ? scope.row.sourceStatus : scope.row.status) === 1 ? 'success' : 'info'">
              {{ (isMiniappMode ? scope.row.sourceStatus : scope.row.status) === 1 ? '上架' : '下架' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column v-if="isMiniappMode" label="官网排序" width="145" align="center" header-align="center">
          <template #default="scope">
            <el-input-number
              v-model="scope.row.sortOrder"
              :min="-9999"
              :max="9999"
              :step="1"
              size="small"
              controls-position="right"
              style="width: 105px;"
              :disabled="scope.row.displaySaving"
              @change="(value, oldValue) => handleMiniSortChange(scope.row, value, oldValue)"
            />
          </template>
        </el-table-column>
        <el-table-column v-if="isMiniappMode" label="官网展示" width="105" fixed="right" align="center" header-align="center">
          <template #default="scope">
            <el-switch
              v-model="scope.row.websiteVisible"
              :loading="scope.row.displaySaving"
              @change="value => handleMiniVisibilityChange(scope.row, value)"
            />
          </template>
        </el-table-column>
        <el-table-column v-else label="操作" width="100" fixed="right" align="center" header-align="center">
          <template #default="scope">
            <el-dropdown trigger="click" @command="(cmd) => handleAction(cmd, scope.row)">
              <el-button type="primary" size="small">
                操作<el-icon class="el-icon--right"><ArrowDown /></el-icon>
              </el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="edit">
                    <el-icon><Edit /></el-icon> 编辑
                  </el-dropdown-item>
                  <el-dropdown-item command="detail">
                    <el-icon><List /></el-icon> 预订详情
                  </el-dropdown-item>
                  <el-dropdown-item :command="scope.row.status === 1 ? 'off' : 'on'">
                    <el-icon><Switch /></el-icon> {{ scope.row.status === 1 ? '下架' : '上架' }}
                  </el-dropdown-item>
                  <el-dropdown-item command="delete" divided style="color: #F56C6C;">
                    <el-icon><Delete /></el-icon> 删除
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-container">
        <el-pagination
          background
          layout="total, sizes, prev, pager, next, jumper"
          :total="total"
          :page-size="pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :current-page="currentPage"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>

    <!-- 添加/编辑行程对话框 -->
    <el-dialog
      v-if="!isMiniappMode"
      :title="isEdit ? '编辑行程' : '添加行程'"
      v-model="dialogVisible"
      width="75%"
      class="tour-dialog"
      :close-on-click-modal="false"
      :close-on-press-escape="false"
      :before-close="handleTourDialogBeforeClose"
    >
      <el-form ref="tourFormRef" :model="tourForm" :rules="tourRules" label-width="100px">
        <el-row :gutter="20">
          <el-col :span="24" v-if="isEdit">
            <el-form-item label="行程编号">
              <el-input v-model="tourForm.code" disabled>
                <template #append>系统编号</template>
              </el-input>
              <div class="form-tip">编号由系统按业务规则自动生成，订单、支付和售后会引用该编号，不建议人工修改。</div>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="行程名称" prop="title">
              <el-input v-model="tourForm.title" placeholder="请输入行程名称" maxlength="100" show-word-limit />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="副标题" prop="subtitle">
              <el-input v-model="tourForm.subtitle" placeholder="请输入副标题" maxlength="50" show-word-limit />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="行程封面" prop="mainImage">
              <el-upload
                class="image-uploader"
                :show-file-list="false"
                :http-request="handleImageUpload"
                :before-upload="beforeImageUpload"
                accept="image/*"
              >
                <img v-if="tourForm.mainImage" :src="tourForm.mainImage" class="uploaded-image" />
                <el-icon v-else class="image-uploader-icon"><Plus /></el-icon>
              </el-upload>
              <div class="upload-tip">点击上传封面图片，建议尺寸 400x300</div>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="标签" prop="tag">
              <el-input v-model="tourForm.tag" placeholder="如：周边游、一日游" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="行程类型" prop="tourType">
              <el-select v-model="tourForm.tourType" placeholder="请选择行程类型" style="width: 100%;">
                <el-option v-for="item in tourTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="出发城市" prop="city">
              <el-cascader
                v-model="tourForm.cityPath"
                :options="chinaRegionOptions"
                :props="regionCascaderProps"
                placeholder="请选择或搜索出发城市"
                clearable
                filterable
                popper-class="region-cascader-popper"
                style="width: 100%;"
                @change="handleTourCityChange"
                @expand-change="handleTourCityExpandChange"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="目的地" prop="destination">
              <el-cascader
                v-model="tourForm.destinationPath"
                :options="chinaRegionOptions"
                :props="regionCascaderProps"
                placeholder="请选择或搜索目的地"
                clearable
                filterable
                popper-class="region-cascader-popper"
                style="width: 100%;"
                @change="handleTourDestinationChange"
                @expand-change="handleTourDestinationExpandChange"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="行程天数" prop="days">
              <el-input-number v-model="tourForm.days" :min="1" :max="30" style="width: 100%;" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="出发月份" prop="month">
              <el-date-picker
                v-model="tourForm.month"
                type="month"
                placeholder="请选择出发月份"
                format="YYYY-MM"
                value-format="M"
                style="width: 100%;"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="评分" prop="starRating">
              <el-rate v-model="tourForm.starRating" allow-half />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="出行班期" prop="recommendDate">
              <el-date-picker
                v-model="travelDateValues"
                type="dates"
                placeholder="请选择出行班期"
                format="YYYY-MM-DD"
                value-format="YYYY-MM-DD"
                style="width: 100%;"
                @change="syncTravelDatesToForm"
              />
              <div class="form-tip">选择一个或多个出行日期；保存时首个日期作为主班期，其余日期作为更多班期。</div>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="已报名人数" prop="enrolledCount">
              <el-input-number v-model="tourForm.enrolledCount" :min="0" style="width: 100%;" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="行程特色" prop="feature">
              <el-input v-model="tourForm.feature" type="textarea" :rows="2" placeholder="请输入行程特色" maxlength="200" show-word-limit />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="标签" prop="tags">
              <el-input v-model="tagsInput" placeholder="请输入标签，多个标签用空格分隔，如：纯玩 无购物 摄影" />
              <div class="form-tip">多个标签用空格分隔，输入即生效；历史逗号分隔数据仍会自动兼容</div>
              <div v-if="parsedTags.length > 0" class="tags-preview">
                <el-tag v-for="tag in parsedTags" :key="tag" size="small" style="margin-right: 4px; margin-top: 4px;">{{ tag }}</el-tag>
              </div>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="行程状态" prop="status">
              <el-switch
                v-model="tourForm.status"
                :active-value="1"
                :inactive-value="0"
                active-text="上架"
                inactive-text="下架"
              />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="handleCancelTourDialog">取消</el-button>
          <el-button type="primary" @click="submitForm" :loading="formLoading">确定</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 预订详情管理对话框 -->
    <TourDetailManager
      v-if="!isMiniappMode"
      v-model="detailDialogVisible"
      :tour-id="detailManagerTourId"
      :tour-title="detailManagerTourTitle"
    />
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search, Refresh, Edit, Delete, Switch, ArrowDown, List, Rank } from '@element-plus/icons-vue'
import request from '@/utils/request'
import TourDetailManager from './TourDetailManager.vue'
import { getPublicTourSourceConfig } from '@/api/tourSource'
import { tourTypeOptions, getTourTypeLabel } from '@/utils/tourTypes'
import { getSupportedImageMessage, isSupportedImageFile } from '@/utils/imageCompression'
import { chinaRegionOptions, regionCascaderProps, getRegionKeyword, getRegionLabel, findRegionPath, selectRegionOnExpand } from '@/utils/chinaRegion'

// 出发城市映射
const cityMap = {
  chongqing: '重庆', chengdu: '成都', guiyang: '贵阳', kunming: '昆明',
  yichang: '宜昌', wuhan: '武汉', changsha: '长沙', guangzhou: '广州',
  shenzhen: '深圳', shanghai: '上海', hangzhou: '杭州', nanjing: '南京',
  xian: '西安', beijing: '北京', sanya: '三亚', haikou: '海口'
}

const destinationOptions = [
  { code: 'beijing', name: '北京', cities: [{ code: 'beijing', name: '北京' }] },
  { code: 'tianjin', name: '天津', cities: [{ code: 'tianjin', name: '天津' }] },
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
    { code: 'changchun', name: '长春' }, { code: 'jilin', name: '吉林' }, { code: 'siping', name: '四平' }
  ]},
  { code: 'heilongjiang', name: '黑龙江', cities: [
    { code: 'harbin', name: '哈尔滨' }, { code: 'daqing', name: '大庆' },
    { code: 'mudanjiang', name: '牡丹江' }, { code: 'jixi', name: '鸡西' }
  ]},
  { code: 'shanghai', name: '上海', cities: [{ code: 'shanghai', name: '上海' }] },
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
    { code: 'yantai', name: '烟台' }, { code: 'weihai', name: '威海' }, { code: 'taian', name: '泰安' }
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
    { code: 'zhuhai', name: '珠海' }, { code: 'foshan', name: '佛山' }, { code: 'chaozhou', name: '潮州' }
  ]},
  { code: 'guangxi', name: '广西', cities: [
    { code: 'nanning', name: '南宁' }, { code: 'guilin', name: '桂林' },
    { code: 'liuzhou', name: '柳州' }, { code: 'beihai', name: '北海' }, { code: 'baise', name: '百色' }
  ]},
  { code: 'hainan', name: '海南', cities: [
    { code: 'haikou', name: '海口' }, { code: 'sanya', name: '三亚' },
    { code: 'wuzhishan', name: '五指山' }, { code: 'qionghai', name: '琼海' }
  ]},
  { code: 'chongqing', name: '重庆', cities: [{ code: 'chongqing', name: '重庆' }] },
  { code: 'sichuan', name: '四川', cities: [
    { code: 'chengdu', name: '成都' }, { code: 'jiuzhaigou', name: '九寨沟' },
    { code: 'leshan', name: '乐山' }, { code: 'garze', name: '甘孜' }, { code: 'aba', name: '阿坝' }
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
    { code: 'jiayuguan', name: '嘉峪关' }, { code: 'zhangye', name: '张掖' }, { code: 'dunhuang', name: '敦煌' }
  ]},
  { code: 'qinghai', name: '青海', cities: [
    { code: 'xining', name: '西宁' }, { code: 'hainan', name: '海南州' }, { code: 'haixi', name: '海西' }
  ]},
  { code: 'ningxia', name: '宁夏', cities: [
    { code: 'yinchuan', name: '银川' }, { code: 'shizuishan', name: '石嘴山' }, { code: 'guyuan', name: '固原' }
  ]},
  { code: 'xinjiang', name: '新疆', cities: [
    { code: 'wulumuqi', name: '乌鲁木齐' }, { code: 'turpan', name: '吐鲁番' },
    { code: 'kashgar', name: '喀什' }, { code: 'yili', name: '伊犁' }
  ]},
  { code: 'taiwan', name: '台湾', cities: [
    { code: 'taipei', name: '台北' }, { code: 'kaohsiung', name: '高雄' }, { code: 'taichung', name: '台中' }
  ]},
  { code: 'xianggang', name: '香港', cities: [{ code: 'xianggang', name: '香港' }] },
  { code: 'aomen', name: '澳门', cities: [{ code: 'aomen', name: '澳门' }] }
]

const allCityOptions = computed(() => {
  const options = []
  destinationOptions.forEach(province => {
    province.cities.forEach(city => {
      options.push({ code: city.code, name: `${province.name} / ${city.name}` })
    })
  })
  return options
})

const legacyLocationMap = {
  '\u95B2\u5DB6\u7C21': '重庆',
  '\u93B4\u6130\u5158': '成都',
  '\u7490\u7538\u69FC': '贵阳',
  '\u93C4\u55D8\u69D1': '昆明',
  '\u7039\u6EC4\u69CD': '宜昌',
  '\u59DD\uFE3D\u773D': '武汉',
  '\u95C0\u630E\u77D9': '长沙',
  '\u9A9E\u57AE\u7A9E': '广州',
  '\u5A23\u535E\u6E77': '深圳',
  '\u6D93\u5A43\u6363': '上海',
  '\u93C9\uE15E\u7A9E': '杭州',
  '\u9357\u693E\u542B': '南京',
  '\u7457\u57AE\u7568': '西安',
  '\u9356\u693E\u542B': '北京',
  '\u6D93\u5909\u7C39': '三亚',
  '\u5A34\u5CF0\u5F5B': '海口',
  '\u7457\u632E\u77D9\u7F07\u3085\u77DD': '西沙群岛',
  '\u6D93\u592F\u573A': '三峡'
}

const getDestinationLabel = (dest) => {
  if (!dest) return '-'
  const normalizedDest = String(dest).trim()
  const specialDestinationMap = {
    xisha: '西沙群岛',
    sanxia: '三峡',
    sanyan: '三峡'
  }
  if (specialDestinationMap[normalizedDest]) {
    return specialDestinationMap[normalizedDest]
  }
  if (legacyLocationMap[normalizedDest]) {
    return legacyLocationMap[normalizedDest]
  }
  if (normalizedDest.includes('/')) {
    const [provinceCode, cityCode] = normalizedDest.split('/')
    const province = destinationOptions.find(p => p.code === provinceCode)
    const city = province?.cities.find(c => c.code === cityCode)
    return city ? `${province.name} / ${city.name}` : normalizedDest
  }
  const option = allCityOptions.value.find(o => o.code === normalizedDest)
  if (option) return option.name
  const province = destinationOptions.find(p => p.code === normalizedDest)
  return province ? province.name : normalizedDest
}

// 分页参数
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const isMiniappMode = ref(false)

// 行程列表
const tourList = ref([])
const loading = ref(false)
const miniSortSaving = ref(false)
const miniDraggedIndex = ref(-1)
let tourFetchSequence = 0
const miniVisibilityOverrides = new Map()

// 搜索表单
const searchForm = reactive({
  title: '',
  tourType: '',
  city: '',
  cityPath: [],
  destination: '',
  destinationPath: []
})

// 对话框
const dialogVisible = ref(false)
const isEdit = ref(false)
const formLoading = ref(false)
const tourFormRef = ref(null)
const tourDialogSnapshot = ref('')

// 预订详情管理
const detailDialogVisible = ref(false)
const detailManagerTourId = ref(null)
const detailManagerTourTitle = ref('')

// 标签输入
const tagsInput = ref('')
const travelDateValues = ref([])
const parsedTags = computed(() => {
  if (!tagsInput.value) return []
  return tagsInput.value.split(/[,\s，、]+/).map(t => t.trim()).filter(t => t)
})

const normalizeMonthValue = (value) => {
  if (!value) return ''
  if (typeof value === 'number') return String(value)
  if (/^\d{4}-\d{1,2}$/.test(value)) return String(Number(value.split('-')[1]))
  return String(Number(value))
}

const parseMoreDates = (value) => {
  if (!value) return []
  const currentYear = new Date().getFullYear()
  const normalizeDate = (item) => {
    if (/^\d{4}-\d{2}-\d{2}$/.test(item)) return item
    if (/^\d{1,2}-\d{1,2}$/.test(item)) {
      const [month, day] = item.split('-')
      return `${currentYear}-${String(Number(month)).padStart(2, '0')}-${String(Number(day)).padStart(2, '0')}`
    }
    return item
  }
  if (Array.isArray(value)) return value.filter(Boolean)
  return String(value)
    .split(/[、，,\s]+/)
    .map(item => item.trim())
    .map(normalizeDate)
    .filter(Boolean)
}

const formatMoreDates = (dates) => {
  return (dates || [])
    .filter(Boolean)
    .map(item => {
      if (/^\d{4}-\d{2}-\d{2}$/.test(item)) return item.slice(5)
      return item
    })
    .sort()
    .join('、')
}

const sortTravelDates = (dates) => Array.from(new Set((dates || []).filter(Boolean))).sort()

const syncTravelDatesToForm = () => {
  const dates = sortTravelDates(travelDateValues.value)
  tourForm.recommendDate = dates[0] || ''
  moreDateValuesCompat.value = dates.slice(1)
}

const moreDateValuesCompat = ref([])

const formatMoreDatesForDisplay = (value) => {
  if (!value) return ''
  const items = Array.isArray(value) ? value : String(value).split(/[、，,\s]+/)
  return items
    .map(item => {
      const text = String(item || '').trim()
      if (/^\d{4}-\d{1,2}-\d{1,2}$/.test(text)) {
        const [, month, day] = text.split('-')
        return `${month.padStart(2, '0')}-${day.padStart(2, '0')}`
      }
      if (/^\d{1,2}-\d{1,2}$/.test(text)) {
        const [month, day] = text.split('-')
        return `${month.padStart(2, '0')}-${day.padStart(2, '0')}`
      }
      return text
    })
    .filter(Boolean)
    .join('、')
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
      // 如果不是JSON，按常见分隔符分隔
      return tags.split(/[,\s，、]+/).map(t => t.trim()).filter(t => t)
    }
  }
  return []
}

// 图片上传
const handleImageUpload = async (options) => {
  const { file, onSuccess, onError } = options
  const formData = new FormData()
  formData.append('file', file)

  try {
    const imageUrl = await request.upload('/file/upload/img', formData, {
      showDefaultMsg: false
    })
    if (imageUrl) {
      tourForm.mainImage = imageUrl
      ElMessage.success('图片上传成功')
      onSuccess()
    } else {
      onError(new Error('上传失败'))
    }
  } catch (error) {
    ElMessage.error('图片上传失败')
    onError(error)
  }
}

// 图片上传前校验
const beforeImageUpload = (file) => {
  if (!isSupportedImageFile(file)) {
    ElMessage.error(getSupportedImageMessage())
    return false
  }
  return true
}

const getTourFormSnapshot = () => JSON.stringify({
  ...tourForm,
  tagsInput: tagsInput.value,
  travelDateValues: travelDateValues.value
})

const markTourDialogPristine = () => {
  tourDialogSnapshot.value = getTourFormSnapshot()
}

const isTourDialogDirty = () => {
  return dialogVisible.value && tourDialogSnapshot.value && tourDialogSnapshot.value !== getTourFormSnapshot()
}

const confirmDiscardTourChanges = async () => {
  if (!isTourDialogDirty()) return true
  return ElMessageBox.confirm('当前修改尚未保存，是否放弃这些修改？', '未保存修改', {
    confirmButtonText: '放弃修改',
    cancelButtonText: '继续编辑',
    type: 'warning',
    distinguishCancelAndClose: true
  }).then(() => true).catch(() => false)
}

const closeTourDialog = () => {
  tourDialogSnapshot.value = ''
  dialogVisible.value = false
}

const handleCancelTourDialog = async () => {
  if (await confirmDiscardTourChanges()) {
    closeTourDialog()
  }
}

const handleTourDialogBeforeClose = async (done) => {
  if (await confirmDiscardTourChanges()) {
    tourDialogSnapshot.value = ''
    done()
  }
}

// 表单数据
const tourForm = reactive({
  id: null,
  code: '',
  title: '',
  subtitle: '',
  mainImage: '',
  tag: '',
  tourType: '',
  city: '',
  cityPath: [],
  destination: '',
  destinationPath: [],
  days: 1,
  month: String(new Date().getMonth() + 1),
  starRating: 5,
  recommendDate: '',
  moreDates: '',
  feature: '',
  tags: [],
  enrolledCount: 0,
  status: 1
})

// 表单验证规则
const tourRules = {
  title: [{ required: true, message: '请输入行程名称', trigger: 'blur' }],
  mainImage: [{ required: true, message: '请上传行程封面', trigger: 'change' }],
  tourType: [{ required: true, message: '请选择行程类型', trigger: 'change' }],
  city: [{ required: true, message: '请选择出发城市', trigger: 'change' }],
  destination: [{ required: true, message: '请选择或输入目的地', trigger: 'change' }],
  days: [{ required: true, message: '请输入行程天数', trigger: 'blur' }],
  month: [{ required: true, message: '请选择出发月份', trigger: 'change' }],
  recommendDate: [{ required: true, message: '请选择出行班期', trigger: 'change' }],
  feature: [{ required: true, message: '请输入行程特色', trigger: 'blur' }]
}

// 获取行程列表
const fetchTours = async () => {
  const fetchSequence = ++tourFetchSequence
  loading.value = true
  try {
    const res = await request.get('/tour/page', {
      keyword: searchForm.title,
      tourType: searchForm.tourType,
      city: searchForm.city,
      destination: searchForm.destination,
      includeInactive: true,
      currentPage: currentPage.value,
      pageSize: pageSize.value
    }, { showDefaultMsg: false })
    if (fetchSequence !== tourFetchSequence) return

    tourList.value = (res.records || []).map(item => {
      const sourceId = String(item.sourceId || '')
      const serverVisible = normalizeWebsiteVisible(item.websiteVisible)
      const overrideVisible = miniVisibilityOverrides.get(sourceId)
      if (overrideVisible === serverVisible) miniVisibilityOverrides.delete(sourceId)
      return {
        ...item,
        moreDates: formatMoreDatesForDisplay(item.moreDates),
        tags: parseTags(item.tags),
        websiteVisible: overrideVisible ?? serverVisible,
        displaySaving: false
      }
    })
    total.value = res.total || 0
  } catch (error) {
    if (fetchSequence !== tourFetchSequence) return
    console.error('获取行程列表失败:', error)
    ElMessage.error('获取行程列表失败，请检查小程序接口状态')
  } finally {
    if (fetchSequence === tourFetchSequence) loading.value = false
  }
}

const normalizeWebsiteVisible = value => {
  if (value === false || value === 0 || value === '0') return false
  if (typeof value === 'string' && value.toLowerCase() === 'false') return false
  return true
}

const getCityLabel = (city) => {
  if (!city) return '-'
  const normalizedCity = String(city).trim()
  return cityMap[normalizedCity] || legacyLocationMap[normalizedCity] || normalizedCity
}

const saveMiniDisplay = async (row, successMsg) => {
  row.displaySaving = true
  try {
    await request.put('/tour/source-display', {
      sourceId: row.sourceId,
      visible: row.websiteVisible,
      sortOrder: Number(row.sortOrder || 0)
    }, {
      showDefaultMsg: false,
      successMsg
    })
    miniVisibilityOverrides.set(String(row.sourceId || ''), Boolean(row.websiteVisible))
    tourFetchSequence += 1
    loading.value = false
    return true
  } catch (error) {
    console.error('保存小程序商品官网展示配置失败:', error)
    ElMessage.error('官网展示配置保存失败，请稍后重试')
    return false
  } finally {
    row.displaySaving = false
  }
}

const handleMiniVisibilityChange = async (row, value) => {
  const saved = await saveMiniDisplay(row, value ? '已在官网展示' : '已从官网隐藏')
  if (!saved) row.websiteVisible = !value
}

const handleMiniSortChange = async (row, value, oldValue) => {
  const saved = await saveMiniDisplay(row, '官网排序已更新')
  if (!saved) {
    row.sortOrder = oldValue ?? 0
    return
  }
  await fetchTours()
}

const handleMiniDragStart = (event, index) => {
  if (miniSortSaving.value) {
    event.preventDefault()
    return
  }
  miniDraggedIndex.value = index
  if (event.dataTransfer) {
    event.dataTransfer.effectAllowed = 'move'
    event.dataTransfer.setData('text/plain', String(index))
  }
}

const handleMiniDrop = async targetIndex => {
  const sourceIndex = miniDraggedIndex.value
  miniDraggedIndex.value = -1
  if (sourceIndex < 0 || sourceIndex === targetIndex || miniSortSaving.value) return

  const previous = [...tourList.value]
  const reordered = [...tourList.value]
  const [moved] = reordered.splice(sourceIndex, 1)
  reordered.splice(targetIndex, 0, moved)
  tourList.value = reordered
  miniSortSaving.value = true
  try {
    const startOrder = (currentPage.value - 1) * pageSize.value + 1
    await request.put('/tour/source-display/reorder', {
      sourceIds: reordered.map(item => item.sourceId),
      startOrder
    }, {
      showDefaultMsg: false,
      successMsg: '前台展示顺序已保存'
    })
    reordered.forEach((item, index) => {
      item.sortOrder = startOrder + index
    })
    await fetchTours()
  } catch (error) {
    tourList.value = previous
    console.error('保存小程序商品拖拽顺序失败:', error)
    ElMessage.error('拖拽顺序保存失败，请稍后重试')
  } finally {
    miniSortSaving.value = false
  }
}

const handleSearchCityChange = (value) => {
  searchForm.city = getRegionKeyword(value)
}

const handleSearchCityExpandChange = (value) => {
  selectRegionOnExpand(value, nextValue => {
    searchForm.cityPath = nextValue
    handleSearchCityChange(nextValue)
  })
}

const handleSearchDestinationChange = (value) => {
  searchForm.destination = getRegionKeyword(value)
}

const handleSearchDestinationExpandChange = (value) => {
  selectRegionOnExpand(value, nextValue => {
    searchForm.destinationPath = nextValue
    handleSearchDestinationChange(nextValue)
  })
}

const handleTourCityChange = (value) => {
  tourForm.city = getRegionLabel(value, ' / ')
}

const handleTourCityExpandChange = (value) => {
  selectRegionOnExpand(value, nextValue => {
    tourForm.cityPath = nextValue
    handleTourCityChange(nextValue)
  })
}

const handleTourDestinationChange = (value) => {
  tourForm.destination = getRegionLabel(value, ' / ')
}

const handleTourDestinationExpandChange = (value) => {
  selectRegionOnExpand(value, nextValue => {
    tourForm.destinationPath = nextValue
    handleTourDestinationChange(nextValue)
  })
}

// 搜索
const handleSearch = () => {
  currentPage.value = 1
  fetchTours()
}

// 重置搜索
const resetSearch = () => {
  searchForm.title = ''
  searchForm.tourType = ''
  searchForm.city = ''
  searchForm.cityPath = []
  searchForm.destination = ''
  searchForm.destinationPath = []
  currentPage.value = 1
  fetchTours()
}

// 分页
const handleSizeChange = (size) => {
  pageSize.value = size
  currentPage.value = 1
  fetchTours()
}

const handleCurrentChange = (page) => {
  currentPage.value = page
  fetchTours()
}

// 显示添加对话框
const showAddDialog = () => {
  isEdit.value = false
  resetForm()
  dialogVisible.value = true
  nextTick(markTourDialogPristine)
}

// 编辑
const handleEdit = (row) => {
  isEdit.value = true
  resetForm()
  Object.keys(tourForm).forEach(key => {
    if (row[key] !== undefined) {
      tourForm[key] = row[key]
    }
  })
  tourForm.cityPath = findRegionPath(row.city || '')
  tourForm.destinationPath = findRegionPath(row.destination || '')
  // 解析并设置标签输入
  tagsInput.value = parseTags(row.tags).join(' ')
  tourForm.month = normalizeMonthValue(row.month)
  travelDateValues.value = sortTravelDates([
    row.recommendDate,
    ...parseMoreDates(row.moreDates)
  ])
  dialogVisible.value = true
  nextTick(markTourDialogPristine)
}

// 下拉菜单操作处理
const handleAction = (command, row) => {
  switch (command) {
    case 'edit':
      handleEdit(row)
      break
    case 'detail':
      handleManageDetail(row)
      break
    case 'on':
    case 'off':
      handleToggleStatus(row)
      break
    case 'delete':
      handleDelete(row)
      break
  }
}

// 切换状态
const handleToggleStatus = async (row) => {
  const newStatus = row.status === 1 ? 0 : 1
  if (newStatus === 1) {
    const missing = getTourMissingFields(row)
    if (missing.length) {
      ElMessage.warning(`行程信息未完整，不能上架：请补充${missing.join('、')}`)
      return
    }
  }
  const statusText = newStatus === 1 ? '上架' : '下架'
  try {
    await request.put(`/tour/${row.id}`, { status: newStatus }, {
      successMsg: `行程${statusText}成功`,
      onSuccess: () => { row.status = newStatus }
    })
  } catch (error) {
    console.error('操作失败:', error)
  }
}

// 删除
const getTourMissingFields = (tour) => {
  const missing = []
  if (!tour.title) missing.push('行程标题')
  if (!tour.mainImage) missing.push('行程封面')
  if (!tour.tourType) missing.push('行程类型')
  if (!tour.city) missing.push('出发城市')
  if (!tour.destination) missing.push('目的地')
  if (!tour.days || tour.days <= 0) missing.push('行程天数')
  if (!tour.month) missing.push('出发月份')
  if (!tour.recommendDate) missing.push('出行班期')
  if (!tour.feature) missing.push('行程特色')
  return missing
}

const handleDelete = (row) => {
  ElMessageBox.confirm(`确定要删除行程"${row.title}"吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await request.delete(`/tour/${row.id}`, {
        successMsg: '删除成功',
        onSuccess: () => fetchTours()
      })
    } catch (error) {
      console.error('删除失败:', error)
    }
  }).catch(() => {})
}

// 重置表单
const resetForm = () => {
  if (tourFormRef.value) tourFormRef.value.resetFields()
  tagsInput.value = ''
  Object.assign(tourForm, {
    id: null, code: '', title: '', subtitle: '', mainImage: '', tag: '',
    tourType: '', city: '', cityPath: [], destination: '', destinationPath: [], days: 1,
    month: String(new Date().getMonth() + 1), starRating: 5,
    recommendDate: '', moreDates: '', feature: '', tags: '',
    enrolledCount: 0, status: 1
  })
  travelDateValues.value = []
  moreDateValuesCompat.value = []
}

// 管理预订详情
const handleManageDetail = (row) => {
  detailManagerTourId.value = row.id
  detailManagerTourTitle.value = row.title
  detailDialogVisible.value = true
}

// 预览图片
const previewImage = () => {
  if (tourForm.mainImage) {
    window.open(tourForm.mainImage, '_blank')
  }
}
void previewImage

// 提交表单
const submitForm = async () => {
  tourForm.city = getRegionLabel(tourForm.cityPath, ' / ') || tourForm.city
  tourForm.destination = getRegionLabel(tourForm.destinationPath, ' / ') || tourForm.destination
  syncTravelDatesToForm()

  tourFormRef.value.validate(async (valid) => {
    if (!valid) {
      ElMessage.warning('请先补充必填项，再保存行程')
      return
    }
    const missing = getTourMissingFields(tourForm)
    if (missing.length) {
      ElMessage.warning(`请补充${missing.join('、')}后再保存`)
      return
    }
    formLoading.value = true
    try {
      // 将标签转换为空格分隔的字符串
      const submitData = {
        ...tourForm,
        month: Number(normalizeMonthValue(tourForm.month)),
        moreDates: formatMoreDates(moreDateValuesCompat.value),
        tags: parsedTags.value.join(' ')
      }
      delete submitData.code
      delete submitData.cityPath
      delete submitData.destinationPath

      if (isEdit.value) {
        await request.put(`/tour/${tourForm.id}`, submitData, {
          successMsg: '更新成功',
          onSuccess: () => {
            markTourDialogPristine()
            closeTourDialog()
            fetchTours()
          }
        })
      } else {
        await request.post('/tour', submitData, {
          successMsg: '添加成功',
          onSuccess: () => {
            markTourDialogPristine()
            closeTourDialog()
            fetchTours()
          }
        })
      }
    } catch (error) {
      console.error('操作失败:', error)
    } finally {
      formLoading.value = false
    }
  })
}

onMounted(async () => {
  try {
    const config = await getPublicTourSourceConfig()
    isMiniappMode.value = config?.sourceMode === 'MINIAPP'
  } catch (error) {
    console.error('获取行程商品来源失败:', error)
    ElMessage.warning('暂时无法识别行程商品来源，请刷新后重试')
  }
  await fetchTours()
})
</script>

<style lang="scss" scoped>
.tour-management {
  padding: 20px;
  background-color: #f5f7fa;
  min-height: calc(100vh - 120px);

  .page-header {
    margin-bottom: 20px;
    text-align: left;

    .page-title {
      font-size: 24px;
      color: #303133;
      margin: 0 0 4px 0;
      font-weight: 600;
    }

    .page-subtitle {
      font-size: 14px;
      color: #909399;
      margin: 0;
    }
  }

  .action-bar {
    margin-bottom: 16px;
    display: flex;
    justify-content: flex-end;

    .add-btn {
      background-color: #409eff;
      border-color: #409eff;
    }
  }

  .search-card {
    margin-bottom: 16px;
    border-radius: 8px;

    .search-form {
      display: flex;
      flex-wrap: nowrap;
      align-items: center;

      :deep(.el-form-item) {
        margin-bottom: 0;
        margin-right: 12px;
      }

      :deep(.el-form-item__label) {
        padding-right: 4px;
      }

      .search-item {
        flex-shrink: 0;
      }

      .search-buttons {
        flex-shrink: 0;
        margin-left: auto;
      }
    }
  }

  .table-card {
    border-radius: 8px;

    .mini-sort-panel {
      margin-bottom: 16px;
      padding: 16px;
      border: 1px solid #dbeafe;
      border-radius: 8px;
      background: #f8fbff;

      > header {
        display: flex;
        align-items: center;
        justify-content: space-between;
        gap: 16px;
        margin-bottom: 12px;

        > div {
          display: grid;
          gap: 4px;
        }

        strong {
          color: #1e3a8a;
        }

        span {
          color: #64748b;
          font-size: 13px;
        }
      }
    }

    .mini-sort-list {
      display: grid;
      gap: 8px;
    }

    .mini-sort-item {
      display: grid;
      grid-template-columns: 28px 34px 48px minmax(0, 1fr) auto;
      align-items: center;
      gap: 10px;
      min-height: 54px;
      padding: 7px 10px;
      border: 1px solid #e2e8f0;
      border-radius: 6px;
      background: #fff;
      cursor: grab;
      transition: border-color 0.16s ease, box-shadow 0.16s ease, opacity 0.16s ease;

      &:hover {
        border-color: #93c5fd;
        box-shadow: 0 5px 14px rgba(37, 99, 235, 0.08);
      }

      &.dragging {
        opacity: 0.45;
      }

      .drag-handle {
        color: #2563eb;
        font-size: 20px;
      }

      .sort-position {
        color: #64748b;
        font-variant-numeric: tabular-nums;
        text-align: center;
      }

      img {
        width: 48px;
        height: 38px;
        border-radius: 4px;
        object-fit: cover;
      }

      > strong {
        overflow: hidden;
        color: #1f2937;
        text-overflow: ellipsis;
        white-space: nowrap;
      }
    }

    .tour-title {
      display: flex;
      align-items: center;
      gap: 8px;

      .title-text {
        flex: 1;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }
    }

    .price {
      color: #f56c6c;
      font-weight: 600;
    }

    .source-id {
      display: block;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    .muted-text {
      color: #909399;
    }

    .action-buttons {
      display: flex;
      flex-wrap: wrap;
      gap: 4px;
      align-items: center;

      .el-button {
        flex-shrink: 0;
        padding: 4px 8px;
      }
    }

    .el-dropdown {
      display: inline-block;
    }
  }

  .pagination-container {
    margin-top: 16px;
    display: flex;
    justify-content: flex-end;
  }

  .tour-dialog {
    .image-uploader {
      :deep(.el-upload) {
        border: 1px dashed #d9d9d9;
        border-radius: 6px;
        cursor: pointer;
        position: relative;
        overflow: hidden;
        transition: border-color 0.2s;
        &:hover {
          border-color: #409eff;
        }
      }
      .uploaded-image {
        width: 148px;
        height: 100px;
        display: block;
        object-fit: cover;
      }
      .image-uploader-icon {
        font-size: 28px;
        color: #8c939d;
        width: 148px;
        height: 100px;
        line-height: 100px;
        text-align: center;
      }
    }
    .upload-tip {
      font-size: 12px;
      color: #909399;
      margin-top: 4px;
    }

    .form-tip {
      font-size: 12px;
      color: #909399;
      margin-top: 4px;
    }

    .tags-preview {
      margin-top: 8px;
    }

    :deep(.el-rate) {
      line-height: 32px;
    }
  }
}

:global(.el-image-viewer__wrapper) {
  z-index: 3000 !important;
}
</style>
