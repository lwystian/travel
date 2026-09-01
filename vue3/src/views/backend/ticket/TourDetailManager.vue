<template>
  <el-dialog
    :title="`预订详情管理 - ${tourTitle}`"
    v-model="dialogVisible"
    width="900px"
    :close-on-click-modal="false"
    :close-on-press-escape="false"
    :before-close="handleMainDialogBeforeClose"
    class="tour-detail-dialog"
  >
    <el-tabs v-model="activeTab" type="border-card">
      <!-- 图片管理 -->
      <el-tab-pane label="图片管理" name="images">
        <div class="image-section">
          <div class="image-list">
            <div v-for="(img, index) in images" :key="index" class="image-item">
              <span class="image-label">{{ index + 1 }}</span>
              <el-upload
                class="image-uploader"
                :show-file-list="false"
                :http-request="(opt) => handleImageUpload(opt, index)"
                :before-upload="beforeImageUpload"
              >
                <img v-if="img" :src="img" class="uploaded-image" />
                <div v-else class="upload-placeholder">
                  <el-icon><Plus /></el-icon>
                  <span>上传图片</span>
                </div>
              </el-upload>
              <el-button v-if="img" type="danger" size="small" @click="removeImage(index)">删除</el-button>
            </div>
          </div>
          <div class="image-tip">建议尺寸：800x600，支持多种常见图片格式</div>
          <div class="section-actions">
            <el-button type="primary" @click="saveImages">保存图片</el-button>
          </div>
        </div>
      </el-tab-pane>

      <!-- 视频管理 -->
      <el-tab-pane label="视频管理" name="video">
        <div class="video-section">
          <el-form label-width="100px">
            <el-form-item label="启用视频">
              <el-switch v-model="videoEnabled" @change="handleVideoEnabledChange" />
            </el-form-item>
            <el-form-item v-if="videoEnabled" label="视频上传">
              <el-upload
                class="video-uploader"
                :show-file-list="false"
                :http-request="handleVideoUpload"
                :before-upload="beforeVideoUpload"
                accept="video/*"
              >
                <div v-if="videoUrl" class="video-preview">
                  <video :src="videoUrl" controls class="preview-video" />
                  <el-button type="danger" size="small" @click.stop="handleRemoveVideo">删除视频</el-button>
                </div>
                <div v-else class="upload-placeholder">
                  <el-icon><Upload /></el-icon>
                  <span>点击上传视频</span>
                </div>
              </el-upload>
            </el-form-item>
            <el-form-item v-if="videoUrl" label="视频封面">
              <el-upload
                class="image-uploader"
                :show-file-list="false"
                :http-request="handlePosterUpload"
                :before-upload="beforeImageUpload"
              >
                <img v-if="videoPoster" :src="videoPoster" class="poster-image" />
                <div v-else class="upload-placeholder small">
                  <el-icon><Plus /></el-icon>
                  <span>上传封面</span>
                </div>
              </el-upload>
            </el-form-item>
          </el-form>
          <div class="video-tip">支持 MP4、AVI 格式，建议大小不超过 100MB</div>
        </div>
      </el-tab-pane>

      <!-- 行程套餐 -->
      <el-tab-pane label="行程套餐" name="tripPackages">
        <div class="package-section">
          <div class="section-header">
            <div class="section-title-block">
              <span class="section-title">行程套餐（成人/儿童售价）</span>
              <span class="legacy-note">旧模式兼容字段：新产品价格请在套餐价格项中按出发日期配置。</span>
            </div>
            <el-button type="primary" size="small" @click="showAddTripPackage">
              <el-icon><Plus /></el-icon> 添加套餐
            </el-button>
          </div>
          <el-table :data="tripPackages" border style="width: 100%" size="small">
            <el-table-column prop="id" label="ID" width="60" />
            <el-table-column prop="name" label="套餐名称" min-width="120" />
            <el-table-column label="成人售价" width="110">
              <template #default="scope">
                <span class="price">¥{{ scope.row.adultPrice }}</span>
              </template>
            </el-table-column>
            <el-table-column label="成人划线价" width="120">
              <template #default="scope">
                <span v-if="hasPromotion(scope.row.originalAdultPrice, scope.row.adultPrice)" class="origin-price">¥{{ scope.row.originalAdultPrice }}</span>
                <span v-else>-</span>
              </template>
            </el-table-column>
            <el-table-column label="折扣" width="90">
              <template #default="scope">
                <el-tag v-if="getDiscountLabel(scope.row.originalAdultPrice, scope.row.adultPrice)" type="danger" effect="light" size="small">
                  {{ getDiscountLabel(scope.row.originalAdultPrice, scope.row.adultPrice) }}
                </el-tag>
                <span v-else>-</span>
              </template>
            </el-table-column>
            <el-table-column label="儿童售价" width="110">
              <template #default="scope">
                <span>{{ Number(scope.row.childPrice || 0) > 0 ? '¥' + scope.row.childPrice : '-' }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="description" label="描述" min-width="150" show-overflow-tooltip />
            <el-table-column label="状态" width="70">
              <template #default="scope">
                <el-tag :type="scope.row.status === 1 ? 'success' : 'info'" size="small">
                  {{ scope.row.status === 1 ? '启用' : '禁用' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="140">
              <template #default="scope">
                <el-button type="primary" size="small" @click="editTripPackage(scope.row)">编辑</el-button>
                <el-button type="danger" size="small" @click="handleDeleteTripPackage(scope.row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-tab-pane>

      <!-- 附加费用 -->
      <el-tab-pane label="附加费用" name="batchPackages">
        <div class="package-section">
          <div class="section-header">
            <div class="section-title-block">
              <span class="section-title">附加费用</span>
              <span class="legacy-note">旧模式兼容单价保留；新产品建议在附加费用价格项中绑定行程套餐和出发日期。</span>
            </div>
            <el-button type="primary" size="small" @click="showAddBatchPackage">
              <el-icon><Plus /></el-icon> 添加费用
            </el-button>
          </div>
          <el-table :data="batchPackages" border style="width: 100%" size="small">
            <el-table-column prop="id" label="ID" width="60" />
            <el-table-column prop="name" label="费用名称" min-width="150" />
            <el-table-column label="单价/份" width="110">
              <template #default="scope">
                <span class="price">+¥{{ scope.row.extraFeePerPerson }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="description" label="描述" min-width="150" show-overflow-tooltip />
            <el-table-column label="状态" width="70">
              <template #default="scope">
                <el-tag :type="scope.row.status === 1 ? 'success' : 'info'" size="small">
                  {{ scope.row.status === 1 ? '启用' : '禁用' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="140">
              <template #default="scope">
                <el-button type="primary" size="small" @click="editBatchPackage(scope.row)">编辑</el-button>
                <el-button type="danger" size="small" @click="handleDeleteBatchPackage(scope.row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-tab-pane>

      <!-- 出发班期 -->
      <el-tab-pane label="出发班期" name="batches">
        <div class="batch-section">
          <div class="section-header">
            <div class="section-title-block">
              <span class="section-title">出发日期管理</span>
              <span class="legacy-note">班期内的可选套餐/附加费用为旧模式入口；配置价格项后，以价格项绑定日期为主。</span>
            </div>
            <div class="header-actions">
              <el-button type="primary" size="small" @click="showAddBatch">
                <el-icon><Plus /></el-icon> 添加班期
              </el-button>
              <el-button type="success" size="small" @click="showBatchAdd">
                <el-icon><Plus /></el-icon> 批量添加
              </el-button>
            </div>
          </div>
          <table class="batch-table">
            <colgroup>
              <col style="width: 7%;">
              <col style="width: 16%;">
              <col style="width: 13%;">
              <col style="width: 13%;">
              <col style="width: 17%;">
              <col style="width: 10%;">
              <col style="width: 24%;">
            </colgroup>
            <thead>
              <tr>
                <th>ID</th>
                <th>出发日期</th>
                <th>成人附加费</th>
                <th>儿童附加费</th>
                <th>余位/锁定/容量</th>
                <th>状态</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="row in batches" :key="row.id">
                <td>{{ row.id }}</td>
                <td><span :class="{ expired: isExpired(row.departureDate) }">{{ row.departureDate }}</span></td>
                <td>{{ row.adultDateExtraFee > 0 ? '+¥' + row.adultDateExtraFee : '-' }}</td>
                <td>{{ row.childDateExtraFee > 0 ? '+¥' + row.childDateExtraFee : '-' }}</td>
                <td>
                  <span :class="{ warning: getAvailableSeats(row) <= 5 }">
                    {{ getAvailableSeats(row) }}/{{ row.occupied || 0 }}/{{ row.maxCapacity || 0 }}
                  </span>
                </td>
                <td><el-tag :type="getStatusType(row.status)" size="small">{{ row.status }}</el-tag></td>
                <td class="batch-action-cell">
                  <div class="table-actions">
                    <el-button type="primary" size="small" @click="editBatch(row)">编辑</el-button>
                    <el-button type="warning" size="small" @click="updateRemaining(row)">余位</el-button>
                    <el-button type="danger" size="small" @click="deleteBatch(row)">删除</el-button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </el-tab-pane>

      <el-tab-pane label="退订政策" name="refundPolicy">
        <div class="detail-content-section">
          <el-form label-width="100px">
            <el-form-item label="退订政策">
              <rich-markdown-editor
                v-model="refundPolicyContent"
                height="360px"
                placeholder="填写前台行程预订页展示的退订政策"
              />
            </el-form-item>
          </el-form>
          <div class="detail-content-actions">
            <el-button type="primary" @click="saveRefundPolicy" :loading="refundPolicyLoading">保存政策</el-button>
          </div>
        </div>
      </el-tab-pane>

      <!-- 出团通知 -->
      <el-tab-pane label="出团通知" name="notice">
        <div class="notice-section">
          <el-form label-width="100px">
            <el-form-item label="出团通知">
              <el-input
                v-model="notice"
                type="textarea"
                :rows="4"
                placeholder="请输入出团通知内容，如：周边游提前1天，国内游提前3天..."
                maxlength="500"
                show-word-limit
              />
            </el-form-item>
          </el-form>
          <div class="notice-actions">
            <el-button type="primary" @click="saveNotice" :loading="noticeLoading">保存通知</el-button>
          </div>
        </div>
      </el-tab-pane>

      <!-- 酒店预订 -->
      <el-tab-pane label="行程详细" name="detailContent">
        <div class="detail-content-section">
          <el-form label-width="100px">
            <el-form-item label="行程详细">
              <rich-markdown-editor
                v-model="detailContent"
                height="360px"
                placeholder="填写前台行程预订页展示的行程详细内容"
              />
            </el-form-item>
          </el-form>
          <div class="detail-content-actions">
            <el-button type="primary" @click="saveDetailContent" :loading="detailContentLoading">保存详情</el-button>
          </div>
        </div>
      </el-tab-pane>

      <el-tab-pane label="酒店预订" name="hotelBooking">
        <div class="hotel-booking-section">
          <div class="section-header">
            <span class="section-title">配套酒店选择</span>
            <div class="header-actions">
              <el-button type="primary" size="small" @click="showAddHotel">
                <el-icon><Plus /></el-icon> 添加酒店
              </el-button>
            </div>
          </div>

          <el-table :data="tourHotels" border style="width: 100%" size="small" class="hotel-table">
            <el-table-column label="酒店信息" min-width="200">
              <template #default="scope">
                <div class="hotel-info-cell">
                  <img v-if="scope.row.imageUrl" :src="scope.row.imageUrl" class="hotel-thumb" />
                  <div v-else class="hotel-thumb-placeholder">🏨</div>
                  <div class="hotel-details">
                    <div class="hotel-name">{{ scope.row.name }}</div>
                    <div class="hotel-meta">
                      <el-tag size="small" type="info">{{ scope.row.type }}</el-tag>
                      <span v-if="scope.row.starLevel" class="hotel-rating">
                        <span class="star">★</span> {{ scope.row.starLevel }}
                      </span>
                    </div>
                  </div>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="价格/晚" width="120">
              <template #default="scope">
                <span class="price-text">¥{{ scope.row.pricePerNight }}</span>
              </template>
            </el-table-column>
            <el-table-column label="预订天数" width="150">
              <template #default="scope">
                <el-input-number
                  v-model="scope.row.days"
                  :min="1"
                  :max="30"
                  size="small"
                  @change="handleHotelDaysChange(scope.row)"
                />
              </template>
            </el-table-column>
            <el-table-column label="总价" width="120">
              <template #default="scope">
                <span class="total-price">¥{{ (scope.row.pricePerNight * scope.row.days).toFixed(2) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="80">
              <template #default="scope">
                <el-switch
                  v-model="scope.row.enabled"
                  :active-value="1"
                  :inactive-value="0"
                  @change="handleHotelEnabledChange(scope.row)"
                />
              </template>
            </el-table-column>
            <el-table-column label="操作" width="120">
              <template #default="scope">
                <div class="table-actions">
                  <el-button type="primary" size="small" @click="editHotel(scope.row)">编辑</el-button>
                  <el-button type="danger" size="small" @click="deleteHotel(scope.row)">删除</el-button>
                </div>
              </template>
            </el-table-column>
          </el-table>

          <div class="hotel-tip">
            <el-icon><InfoFilled /></el-icon>
            <span>酒店将作为行程可选配套服务，用户在预订时可选择是否包含酒店住宿。支持自定义每晚价格和预订天数。</span>
          </div>
        </div>
      </el-tab-pane>
    </el-tabs>

    <div class="dialog-footer">
      <el-button @click="handleMainDialogCancel">关闭</el-button>
    </div>

    <!-- 添加/编辑行程套餐对话框 -->
    <el-dialog
      :title="isTripPackageEdit ? '编辑行程套餐' : '添加行程套餐'"
      v-model="tripPackageDialogVisible"
      width="880px"
      append-to-body
      :close-on-click-modal="false"
      :close-on-press-escape="false"
      :before-close="(done) => handleSubDialogBeforeClose('tripPackage', done)"
    >
      <el-form ref="tripPackageFormRef" :model="tripPackageForm" label-width="90px">
        <el-form-item label="套餐名称" prop="name">
          <el-input v-model="tripPackageForm.name" placeholder="如：标准套餐、VIP套餐" />
        </el-form-item>
        <el-form-item label="成人售价" prop="adultPrice">
          <el-input-number v-model="tripPackageForm.adultPrice" :precision="2" :min="0" :step="10" style="width: 100%;" />
          <div class="form-tip">旧模式兼容字段。未配置套餐价格项时作为前台和下单价格；已配置价格项后可不填，系统优先使用下方价格项。</div>
        </el-form-item>
        <el-form-item label="成人划线价">
          <el-input-number v-model="tripPackageForm.originalAdultPrice" :precision="2" :min="0" :step="10" style="width: 100%;" />
          <div class="form-tip">旧模式兼容划线价。价格项已配置划线价时，前台优先展示价格项划线价。</div>
        </el-form-item>
        <el-form-item label="儿童售价（可选）" prop="childPrice">
          <el-input-number v-model="tripPackageForm.childPrice" :precision="2" :min="0" :step="10" style="width: 100%;" />
          <div class="form-tip">不接待儿童或不单独设置儿童价时可留空；已配置价格项后以前台对应价格项为准。</div>
        </el-form-item>
        <el-form-item label="儿童划线价">
          <el-input-number v-model="tripPackageForm.originalChildPrice" :precision="2" :min="0" :step="10" style="width: 100%;" />
          <div class="form-tip">旧模式兼容划线价。价格项已配置划线价时，前台优先展示价格项划线价。</div>
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="tripPackageForm.description" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-switch v-model="tripPackageForm.status" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <div class="price-item-editor">
        <div class="price-item-head">
          <div>
            <h4>套餐价格项</h4>
            <p>新模式主入口：一个套餐可添加多个价格项，每个价格项可批量绑定多个出发日期。</p>
          </div>
          <el-button type="primary" size="small" @click="addPackagePriceItem">添加价格项</el-button>
        </div>
        <div v-if="packagePriceItems.length === 0" class="price-item-empty">暂无价格项，前台将使用上方售价；成人售价必须填写，儿童售价可选。</div>
        <div v-for="(item, index) in packagePriceItems" :key="item.localKey" class="price-item-card">
          <div class="price-item-card-head">
            <strong>价格项 {{ index + 1 }}</strong>
            <el-button type="danger" size="small" link @click="removePackagePriceItem(index)">删除</el-button>
          </div>
          <div class="price-item-grid package-price-grid">
            <label class="price-field">
              <span>成人售价</span>
              <el-input-number v-model="item.adultPrice" :precision="2" :min="0" :step="10" />
            </label>
            <label class="price-field">
              <span>成人划线价</span>
              <el-input-number v-model="item.originalAdultPrice" :precision="2" :min="0" :step="10" />
            </label>
            <label class="price-field">
              <span>儿童售价（可选）</span>
              <el-input-number v-model="item.childPrice" :precision="2" :min="0" :step="10" />
            </label>
            <label class="price-field">
              <span>儿童划线价</span>
              <el-input-number v-model="item.originalChildPrice" :precision="2" :min="0" :step="10" />
            </label>
            <div class="price-field price-field--switch">
              <span>状态</span>
              <el-switch v-model="item.status" :active-value="1" :inactive-value="0" active-text="启用" inactive-text="停用" />
            </div>
          </div>
          <el-select
            v-model="item.batchIds"
            multiple
            filterable
            collapse-tags
            collapse-tags-tooltip
            placeholder="选择适用出发日期"
            class="price-item-batches"
          >
            <el-option
              v-for="batch in batches"
              :key="batch.id"
              :label="formatBatchOptionLabel(batch)"
              :value="batch.id"
            />
          </el-select>
        </div>
      </div>
      <template #footer>
        <el-button @click="closeSubDialogWithConfirm('tripPackage')">取消</el-button>
        <el-button type="primary" @click="submitTripPackage" :loading="tripPackageLoading">确定</el-button>
      </template>
    </el-dialog>

    <!-- 添加/编辑附加费用对话框 -->
    <el-dialog
      :title="isBatchPackageEdit ? '编辑附加费用' : '添加附加费用'"
      v-model="batchPackageDialogVisible"
      width="820px"
      append-to-body
      :close-on-click-modal="false"
      :close-on-press-escape="false"
      :before-close="(done) => handleSubDialogBeforeClose('batchPackage', done)"
    >
      <el-form ref="batchPackageFormRef" :model="batchPackageForm" label-width="90px">
        <el-form-item label="费用名称" prop="name">
          <el-input v-model="batchPackageForm.name" placeholder="如：接送机、保险、单房差" />
        </el-form-item>
        <el-form-item label="售价/份" prop="extraFeePerPerson">
          <el-input-number v-model="batchPackageForm.extraFeePerPerson" :precision="2" :min="0" :step="10" style="width: 100%;" />
          <div class="form-tip">旧模式兼容单价。未配置附加费用价格项时使用；已配置价格项后，前台优先按行程套餐、价格项和出发日期展示。</div>
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="batchPackageForm.description" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-switch v-model="batchPackageForm.status" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <div class="price-item-editor">
        <div class="price-item-head">
          <div>
            <h4>附加费用价格项</h4>
            <p>新模式主入口：同一附加费用可以按行程套餐和出发日期分别设置单价。</p>
          </div>
          <el-button type="primary" size="small" @click="addAddonPriceItem">添加价格项</el-button>
        </div>
        <div v-if="addonPriceItems.length === 0" class="price-item-empty">暂无价格项，前台将使用上方售价/份。</div>
        <div v-for="(item, index) in addonPriceItems" :key="item.localKey" class="price-item-card">
          <div class="price-item-card-head">
            <strong>价格项 {{ index + 1 }}</strong>
            <el-button type="danger" size="small" link @click="removeAddonPriceItem(index)">删除</el-button>
          </div>
          <div class="price-item-grid addon-price-grid">
            <label class="price-field">
              <span>售价/份</span>
              <el-input-number v-model="item.price" :precision="2" :min="0" :step="10" />
            </label>
            <label class="price-field">
              <span>适用套餐</span>
              <el-select
                v-model="item.packageIds"
                multiple
                filterable
                collapse-tags
                collapse-tags-tooltip
                clearable
                placeholder="不选则适用全部可用套餐"
              >
                <el-option
                  v-for="pkg in tripPackages"
                  :key="pkg.id"
                  :label="pkg.name"
                  :value="pkg.id"
                  :disabled="!isAddonPackageEligible(item, pkg.id)"
                />
              </el-select>
            </label>
            <div class="price-field price-field--switch">
              <span>状态</span>
              <el-switch v-model="item.status" :active-value="1" :inactive-value="0" active-text="启用" inactive-text="停用" />
            </div>
          </div>
          <div class="form-tip">可多选套餐，套餐选项会按全部已选日期的可用范围取交集；留空表示这些日期下的全部可用套餐。</div>
          <el-select
            v-model="item.batchIds"
            multiple
            filterable
            collapse-tags
            collapse-tags-tooltip
            placeholder="选择适用出发日期"
            class="price-item-batches"
            @change="handleAddonPriceItemBatchesChange(item)"
          >
            <el-option
              v-for="batch in batches"
              :key="batch.id"
              :label="formatBatchOptionLabel(batch)"
              :value="batch.id"
            />
          </el-select>
        </div>
      </div>
      <template #footer>
        <el-button @click="closeSubDialogWithConfirm('batchPackage')">取消</el-button>
        <el-button type="primary" @click="submitBatchPackage" :loading="batchPackageLoading">确定</el-button>
      </template>
    </el-dialog>

    <!-- 添加/编辑班期对话框 -->
    <el-dialog
      :title="isBatchEdit ? '编辑班期' : '添加班期'"
      v-model="batchDialogVisible"
      width="450px"
      append-to-body
      :close-on-click-modal="false"
      :close-on-press-escape="false"
      :before-close="(done) => handleSubDialogBeforeClose('batch', done)"
    >
      <el-form ref="batchFormRef" :model="batchForm" label-width="100px">
        <el-form-item label="出发日期" prop="departureDate">
          <el-date-picker
            v-model="batchForm.departureDate"
            type="date"
            format="YYYY-MM-DD"
            value-format="YYYY-MM-DD"
            :disabled-date="disabledDate"
            style="width: 100%;"
          />
        </el-form-item>
        <el-form-item label="成人附加费">
          <el-input-number v-model="batchForm.adultDateExtraFee" :precision="2" :min="0" :step="10" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="儿童附加费">
          <el-input-number v-model="batchForm.childDateExtraFee" :precision="2" :min="0" :step="10" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="batchForm.status" style="width: 100%;">
            <el-option label="可报名" value="可报名" />
            <el-option label="已满员" value="已满员" />
            <el-option label="已结束" value="已结束" />
          </el-select>
        </el-form-item>
        <el-form-item label="余位">
          <el-input-number v-model="batchForm.remaining" :min="batchForm.occupied || 0" :max="batchForm.maxCapacity || 999" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="最大容量">
          <el-input-number v-model="batchForm.maxCapacity" :min="Math.max(1, batchForm.remaining || 0)" :max="999" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="可选套餐">
          <el-select
            v-model="batchForm.packageIds"
            multiple
            collapse-tags
            collapse-tags-tooltip
            placeholder="不选则默认全部行程套餐可选"
            style="width: 100%;"
          >
            <el-option v-for="pkg in tripPackages" :key="pkg.id" :label="formatPackageOptionLabel(pkg)" :value="pkg.id" />
          </el-select>
          <div class="form-tip">旧模式入口。仅用于兼容未配置套餐价格项的老产品；新产品请到“行程套餐 - 套餐价格项”中绑定出发日期。</div>
        </el-form-item>
        <el-form-item label="附加费用">
          <el-select
            v-model="batchForm.addonIds"
            multiple
            collapse-tags
            collapse-tags-tooltip
            placeholder="不选则默认全部附加费用可选"
            style="width: 100%;"
          >
            <el-option v-for="pkg in batchPackages" :key="pkg.id" :label="formatAddonOptionLabel(pkg)" :value="pkg.id" />
          </el-select>
          <div class="form-tip">旧模式入口。仅用于兼容未配置附加费用价格项的老产品；新产品请到“附加费用价格项”中绑定出发日期。</div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="closeSubDialogWithConfirm('batch')">取消</el-button>
        <el-button type="primary" @click="submitBatch" :loading="batchLoading">确定</el-button>
      </template>
    </el-dialog>

    <!-- 批量添加班期对话框 -->
    <el-dialog
      title="批量添加班期"
      v-model="batchAddDialogVisible"
      width="500px"
      append-to-body
      :close-on-click-modal="false"
      :close-on-press-escape="false"
      :before-close="(done) => handleSubDialogBeforeClose('batchAdd', done)"
    >
      <el-form :model="batchAddForm" label-width="100px">
        <el-form-item label="出发日期">
          <el-date-picker
            v-model="batchAddForm.dates"
            type="dates"
            placeholder="可一次选择多个出发日期"
            format="YYYY-MM-DD"
            value-format="YYYY-MM-DD"
            :disabled-date="disabledDate"
            style="width: 100%;"
          />
        </el-form-item>
        <el-form-item label="成人附加费">
          <el-input-number v-model="batchAddForm.adultDateExtraFee" :precision="2" :min="0" :step="10" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="儿童附加费">
          <el-input-number v-model="batchAddForm.childDateExtraFee" :precision="2" :min="0" :step="10" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="余位">
          <el-input-number v-model="batchAddForm.remaining" :min="0" :max="batchAddForm.maxCapacity || 999" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="最大容量">
          <el-input-number v-model="batchAddForm.maxCapacity" :min="Math.max(1, batchAddForm.remaining || 0)" :max="999" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="batchAddForm.status" style="width: 100%;">
            <el-option label="可报名" value="可报名" />
            <el-option label="已满员" value="已满员" />
          </el-select>
        </el-form-item>
        <el-form-item label="可选套餐">
          <el-select
            v-model="batchAddForm.packageIds"
            multiple
            collapse-tags
            collapse-tags-tooltip
            placeholder="不选则默认全部行程套餐可选"
            style="width: 100%;"
          >
            <el-option v-for="pkg in tripPackages" :key="pkg.id" :label="formatPackageOptionLabel(pkg)" :value="pkg.id" />
          </el-select>
          <div class="form-tip">旧模式入口。批量新增老产品班期时可用；新产品优先在套餐价格项中批量选择出发日期。</div>
        </el-form-item>
        <el-form-item label="附加费用">
          <el-select
            v-model="batchAddForm.addonIds"
            multiple
            collapse-tags
            collapse-tags-tooltip
            placeholder="不选则默认全部附加费用可选"
            style="width: 100%;"
          >
            <el-option v-for="pkg in batchPackages" :key="pkg.id" :label="formatAddonOptionLabel(pkg)" :value="pkg.id" />
          </el-select>
          <div class="form-tip">旧模式入口。批量新增老产品班期时可用；新产品优先在附加费用价格项中批量选择出发日期。</div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="closeSubDialogWithConfirm('batchAdd')">取消</el-button>
        <el-button type="primary" @click="submitBatchAdd" :loading="batchAddLoading">确定</el-button>
      </template>
    </el-dialog>

    <!-- 修改余位对话框 -->
    <el-dialog
      title="修改余位"
      v-model="remainingDialogVisible"
      width="350px"
      append-to-body
      :close-on-click-modal="false"
      :close-on-press-escape="false"
      :before-close="(done) => handleSubDialogBeforeClose('remaining', done)"
    >
      <el-form label-width="80px">
        <el-form-item label="班期">
          <span>{{ currentBatch?.departureDate }}</span>
        </el-form-item>
        <el-form-item label="当前余位">
          <span>{{ currentBatch?.remaining || 0 }}</span>
        </el-form-item>
        <el-form-item label="锁定名额">
          <span>{{ currentBatch?.occupied || 0 }}</span>
        </el-form-item>
        <el-form-item label="最大容量">
          <span>{{ currentBatch?.maxCapacity || 0 }}</span>
        </el-form-item>
        <el-form-item label="新余位">
          <el-input-number v-model="newRemaining" :min="currentBatch?.occupied || 0" :max="currentBatch?.maxCapacity || 999" style="width: 100%;" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="closeSubDialogWithConfirm('remaining')">取消</el-button>
        <el-button type="primary" @click="submitRemaining">确定</el-button>
      </template>
    </el-dialog>

    <!-- 添加/编辑酒店对话框 -->
    <el-dialog
      :title="isHotelEdit ? '编辑酒店' : '添加酒店'"
      v-model="hotelDialogVisible"
      width="550px"
      append-to-body
      :close-on-click-modal="false"
      :close-on-press-escape="false"
      :before-close="(done) => handleSubDialogBeforeClose('hotel', done)"
    >
      <el-form ref="hotelFormRef" :model="hotelForm" label-width="100px">
        <el-form-item label="选择酒店" prop="accommodationId">
          <el-select
            v-model="hotelForm.accommodationId"
            placeholder="请选择住宿"
            filterable
            style="width: 100%;"
            @change="handleAccommodationSelect"
          >
            <el-option
              v-for="acc in accommodationList"
              :key="acc.id"
              :label="acc.name"
              :value="acc.id"
            >
              <div class="accommodation-option">
                <span>{{ acc.name }}</span>
                <span class="accommodation-type">{{ acc.type }}</span>
              </div>
            </el-option>
          </el-select>
        </el-form-item>

        <el-form-item label="酒店名称">
          <el-input v-model="hotelForm.name" placeholder="自动填充或手动输入" />
        </el-form-item>

        <el-form-item label="酒店类型">
          <el-input v-model="hotelForm.type" placeholder="如：豪华酒店、经济型" />
        </el-form-item>

        <el-form-item label="每晚价格">
          <el-input-number v-model="hotelForm.pricePerNight" :precision="2" :min="0" :step="50" style="width: 100%;" />
        </el-form-item>

        <el-form-item label="预订天数">
          <el-input-number v-model="hotelForm.days" :min="1" :max="30" style="width: 100%;" />
        </el-form-item>

        <el-form-item label="状态">
          <el-switch v-model="hotelForm.enabled" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="closeSubDialogWithConfirm('hotel')">取消</el-button>
        <el-button type="primary" @click="submitHotel" :loading="hotelLoading">确定</el-button>
      </template>
    </el-dialog>
  </el-dialog>
</template>

<script setup>
import { ref, watch, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Upload, InfoFilled } from '@element-plus/icons-vue'
import request from '@/utils/request'
import RichMarkdownEditor from '@/components/RichMarkdownEditor.vue'
import { getSupportedImageMessage, isSupportedImageFile } from '@/utils/imageCompression'
import {
  getTourPackages,
  addTourPackage,
  updateTourPackage,
  deleteTourPackage,
  getTourPackagePriceItems,
  saveTourPackagePriceItem,
  deleteTourPackagePriceItem,
  getBatchPackages,
  addBatchPackage,
  updateBatchPackage,
  deleteBatchPackage,
  getBatchPackagePriceItems,
  saveBatchPackagePriceItem,
  deleteBatchPackagePriceItem,
  getTourBatches,
  addTourBatch,
  addTourBatchesBatch,
  updateTourBatch,
  deleteTourBatch
} from '@/api/tourDetail'
import { getTourDetailFull } from '@/api/tour'
import { updateTourImages, updateTourVideo } from '@/api/tour'

const props = defineProps({
  modelValue: Boolean,
  tourId: [Number, String],
  tourTitle: String
})

const emit = defineEmits(['update:modelValue'])

const dialogVisible = ref(false)
const activeTab = ref('images')
const loading = ref(false)
const mainSnapshot = ref('')
const subDialogSnapshots = ref({})

// 图片管理
const images = ref(['', '', '', '', ''])
const videoEnabled = ref(false)
const videoUrl = ref('')
const videoPoster = ref('')

// 行程套餐
const tripPackages = ref([])
const tripPackageDialogVisible = ref(false)
const isTripPackageEdit = ref(false)
const tripPackageLoading = ref(false)
const tripPackageFormRef = ref(null)
const tripPackageForm = ref({
  id: null, name: '', adultPrice: null, originalAdultPrice: null, childPrice: null, originalChildPrice: null, description: '', status: 1
})
const packagePriceItems = ref([])
const deletedPackagePriceItemIds = ref([])
const packageBatchAvailability = ref({})
const hasPackagePriceItemMode = ref(false)

// 批次套餐
const batchPackages = ref([])
const batchPackageDialogVisible = ref(false)
const isBatchPackageEdit = ref(false)
const batchPackageLoading = ref(false)
const batchPackageFormRef = ref(null)
const batchPackageForm = ref({
  id: null, name: '', extraFeePerPerson: 0, description: '', status: 1
})
const addonPriceItems = ref([])
const deletedAddonPriceItemIds = ref([])

// 班期
const batches = ref([])
const batchDialogVisible = ref(false)
const isBatchEdit = ref(false)
const batchLoading = ref(false)
const batchFormRef = ref(null)
const batchForm = ref({
  id: null, departureDate: '', adultDateExtraFee: 0, childDateExtraFee: 0,
  status: '可报名', remaining: 30, maxCapacity: 50, packageIds: [], addonIds: []
})

// 批量添加班期
const batchAddDialogVisible = ref(false)
const batchAddLoading = ref(false)
const batchAddForm = ref({
  dates: [], adultDateExtraFee: 0, childDateExtraFee: 0,
  remaining: 30, maxCapacity: 50, status: '可报名', packageIds: [], addonIds: []
})

// 余位修改
const remainingDialogVisible = ref(false)
const currentBatch = ref(null)
const newRemaining = ref(0)

// 出团通知
const notice = ref('')
const noticeLoading = ref(false)
const detailContent = ref('')
const detailContentLoading = ref(false)
const refundPolicyContent = ref('')
const refundPolicyLoading = ref(false)

// 产品信息（用于默认值）
const productInfo = ref({ days: 1 })

// 酒店预订
const tourHotels = ref([])
const hotelDialogVisible = ref(false)
const isHotelEdit = ref(false)
const hotelLoading = ref(false)
const hotelFormRef = ref(null)
const accommodationList = ref([])
const hotelForm = ref({
  id: null,
  accommodationId: null,
  name: '',
  type: '',
  pricePerNight: 0,
  days: 1,
  enabled: 1,
  imageUrl: '',
  starLevel: null
})

watch(() => props.modelValue, (val) => {
  dialogVisible.value = val
  if (val && props.tourId) {
    loadAllData()
  }
})

watch(dialogVisible, (val) => {
  emit('update:modelValue', val)
})

const getMainSnapshot = () => JSON.stringify({
  images: images.value,
  videoEnabled: videoEnabled.value,
  videoUrl: videoUrl.value,
  videoPoster: videoPoster.value,
  notice: notice.value,
  detailContent: detailContent.value,
  refundPolicyContent: refundPolicyContent.value
})

const markMainPristine = () => {
  mainSnapshot.value = getMainSnapshot()
}

const isMainDirty = () => {
  return dialogVisible.value && mainSnapshot.value && mainSnapshot.value !== getMainSnapshot()
}

const getSubDialogState = (type) => {
  switch (type) {
    case 'tripPackage': return { form: tripPackageForm.value, priceItems: packagePriceItems.value, deletedIds: deletedPackagePriceItemIds.value }
    case 'batchPackage': return { form: batchPackageForm.value, priceItems: addonPriceItems.value, deletedIds: deletedAddonPriceItemIds.value }
    case 'batch': return batchForm.value
    case 'batchAdd': return batchAddForm.value
    case 'remaining': return {
      batchId: currentBatch.value?.id || null,
      newRemaining: newRemaining.value
    }
    case 'hotel': return hotelForm.value
    default: return null
  }
}

const getSubDialogVisibleRef = (type) => {
  switch (type) {
    case 'tripPackage': return tripPackageDialogVisible
    case 'batchPackage': return batchPackageDialogVisible
    case 'batch': return batchDialogVisible
    case 'batchAdd': return batchAddDialogVisible
    case 'remaining': return remainingDialogVisible
    case 'hotel': return hotelDialogVisible
    default: return null
  }
}

const markSubDialogPristine = (type) => {
  subDialogSnapshots.value = {
    ...subDialogSnapshots.value,
    [type]: JSON.stringify(getSubDialogState(type) || {})
  }
}

const isSubDialogDirty = (type) => {
  const visibleRef = getSubDialogVisibleRef(type)
  const snapshot = subDialogSnapshots.value[type]
  if (!visibleRef?.value || !snapshot) return false
  return snapshot !== JSON.stringify(getSubDialogState(type) || {})
}

const confirmDiscardChanges = async () => {
  return ElMessageBox.confirm('当前修改尚未保存，是否放弃这些修改？', '未保存修改', {
    confirmButtonText: '放弃修改',
    cancelButtonText: '继续编辑',
    type: 'warning',
    distinguishCancelAndClose: true
  }).then(() => true).catch(() => false)
}

const handleMainDialogCancel = async () => {
  if (!isMainDirty() || await confirmDiscardChanges()) {
    mainSnapshot.value = ''
    dialogVisible.value = false
  }
}

const handleMainDialogBeforeClose = async (done) => {
  if (!isMainDirty() || await confirmDiscardChanges()) {
    mainSnapshot.value = ''
    done()
  }
}

const closeSubDialogWithConfirm = async (type) => {
  if (!isSubDialogDirty(type) || await confirmDiscardChanges()) {
    const visibleRef = getSubDialogVisibleRef(type)
    if (visibleRef) visibleRef.value = false
    subDialogSnapshots.value = { ...subDialogSnapshots.value, [type]: '' }
  }
}

const handleSubDialogBeforeClose = async (type, done) => {
  if (!isSubDialogDirty(type) || await confirmDiscardChanges()) {
    subDialogSnapshots.value = { ...subDialogSnapshots.value, [type]: '' }
    done()
  }
}

const loadAllData = async () => {
  loading.value = true
  try {
    await fetchTourDetail()
    await Promise.all([
      fetchTripPackages(),
      fetchBatchPackages(),
      fetchBatches(),
      fetchAccommodationList()
    ])
    await fetchPackageAvailability()
    await fetchTourHotels()
    await nextTick()
    markMainPristine()
  } finally {
    loading.value = false
  }
}

const fetchTourDetail = async () => {
  try {
    const res = await getTourDetailFull(props.tourId)
    if (res) {
      // 图片
      images.value = normalizeImageSlots(res.images?.main)
      while (images.value.length < 5) images.value.push('')
      // 视频（读取后端的启用状态）
      videoEnabled.value = res.video?.enabled === 1
      videoUrl.value = res.video?.url || ''
      videoPoster.value = res.video?.poster || ''
      // 初始化完成后允许自动保存
      isVideoInitialized = true
      // 出团通知
      notice.value = res.tour?.notice || ''
      detailContent.value = res.tour?.detailContent || ''
      refundPolicyContent.value = res.refundPolicy?.content || ''
      // 产品信息
      if (res.tour) {
        productInfo.value = {
          days: res.tour.days || 1
        }
      }
    }
  } catch (error) {
    console.error('获取行程详情失败:', error)
  }
}

const fetchTripPackages = async () => {
  try {
    const res = await getTourPackages(props.tourId)
    tripPackages.value = res || []
  } catch (error) {
    console.error('获取行程套餐失败:', error)
  }
}

const fetchBatchPackages = async () => {
  try {
    const res = await getBatchPackages(props.tourId)
    batchPackages.value = res || []
  } catch (error) {
    console.error('获取批次套餐失败:', error)
  }
}

const fetchBatches = async () => {
  try {
    const res = await getTourBatches(props.tourId)
    batches.value = res || []
  } catch (error) {
    console.error('获取班期失败:', error)
  }
}

const fetchPackageAvailability = async () => {
  const packages = tripPackages.value || []
  if (!packages.length) {
    packageBatchAvailability.value = {}
    hasPackagePriceItemMode.value = false
    return
  }
  try {
    const rows = await Promise.all(packages.map(async pkg => ({
      packageId: Number(pkg.id),
      items: await getTourPackagePriceItems(pkg.id)
    })))
    const availability = {}
    let hasActiveItems = false
    rows.forEach(({ packageId, items }) => {
      const batchIds = new Set()
      for (const item of items || []) {
        if (Number(item.status ?? 1) !== 1) continue
        hasActiveItems = true
        normalizeIdArray(item.batchIds).forEach(id => batchIds.add(id))
      }
      availability[packageId] = Array.from(batchIds)
    })
    packageBatchAvailability.value = availability
    hasPackagePriceItemMode.value = hasActiveItems
  } catch (error) {
    console.error('获取套餐适用日期失败:', error)
    packageBatchAvailability.value = {}
    hasPackagePriceItemMode.value = false
  }
}

const makeLocalKey = () => `${Date.now()}-${Math.random().toString(16).slice(2)}`

const normalizePriceItemBatchIds = (value) => normalizeIdArray(value)

const normalizePackagePriceItem = (item = {}) => ({
  localKey: makeLocalKey(),
  id: item.id || null,
  name: item.name || '',
  adultPrice: item.adultPrice ?? tripPackageForm.value.adultPrice ?? 0,
  originalAdultPrice: item.originalAdultPrice ?? null,
  childPrice: Number(item.childPrice ?? tripPackageForm.value.childPrice ?? 0) > 0
    ? (item.childPrice ?? tripPackageForm.value.childPrice)
    : null,
  originalChildPrice: item.originalChildPrice ?? null,
  batchIds: normalizePriceItemBatchIds(item.batchIds),
  status: item.status ?? 1,
  sortOrder: item.sortOrder ?? 0
})

const normalizeAddonPriceItem = (item = {}) => ({
  localKey: makeLocalKey(),
  id: item.id || null,
  name: item.name || '',
  price: item.price ?? batchPackageForm.value.extraFeePerPerson ?? 0,
  packageIds: (() => {
    const packageIds = normalizeIdArray(item.packageIds)
    return packageIds.length ? packageIds : normalizeIdArray(item.packageId ? [item.packageId] : [])
  })(),
  batchIds: normalizePriceItemBatchIds(item.batchIds),
  status: item.status ?? 1,
  sortOrder: item.sortOrder ?? 0
})

const addPackagePriceItem = () => {
  packagePriceItems.value.push(normalizePackagePriceItem({ name: `价格项${packagePriceItems.value.length + 1}` }))
}

const addAddonPriceItem = () => {
  addonPriceItems.value.push(normalizeAddonPriceItem({ name: `价格项${addonPriceItems.value.length + 1}` }))
}

const removePackagePriceItem = (index) => {
  const [removed] = packagePriceItems.value.splice(index, 1)
  if (removed?.id) {
    deletedPackagePriceItemIds.value.push(removed.id)
  }
}

const removeAddonPriceItem = (index) => {
  const [removed] = addonPriceItems.value.splice(index, 1)
  if (removed?.id) {
    deletedAddonPriceItemIds.value.push(removed.id)
  }
}

const formatBatchOptionLabel = (batch) => {
  if (!batch) return ''
  const available = getAvailableSeats(batch)
  return `${batch.departureDate}（${batch.status || '可报名'}，余${available}）`
}

const isPackageAvailableForBatch = (packageId, batchId) => {
  if (!packageId || !batchId) return false
  if (hasPackagePriceItemMode.value) {
    return normalizeIdArray(packageBatchAvailability.value[Number(packageId)]).includes(Number(batchId))
  }
  const batch = batches.value.find(row => Number(row.id) === Number(batchId))
  if (!batch) return false
  const packageIds = normalizeIdArray(batch.packageIds)
  return !packageIds.length || packageIds.includes(Number(packageId))
}

const isAddonPackageEligible = (item, packageId) => {
  const batchIds = normalizeIdArray(item?.batchIds)
  return !batchIds.length || batchIds.every(batchId => isPackageAvailableForBatch(packageId, batchId))
}

const handleAddonPriceItemBatchesChange = item => {
  const packageIds = normalizeIdArray(item?.packageIds)
  const retainedIds = packageIds.filter(packageId => isAddonPackageEligible(item, packageId))
  if (retainedIds.length === packageIds.length) return
  const removedNames = packageIds
    .filter(packageId => !retainedIds.includes(packageId))
    .map(packageId => tripPackages.value.find(pkg => Number(pkg.id) === packageId)?.name || `套餐${packageId}`)
  item.packageIds = retainedIds
  ElMessage.warning(`已移除不适用于全部所选日期的套餐：${removedNames.join('、')}`)
}

const validatePriceItemBatchConflicts = (items, label, getScope = () => 'default') => {
  const used = new Map()
  for (const item of items) {
    if (item.status !== 1) continue
    const rawScopes = getScope(item)
    const scopes = Array.isArray(rawScopes)
      ? (rawScopes.length ? rawScopes : ['all'])
      : [rawScopes || 'default']
    for (const batchId of normalizeIdArray(item.batchIds)) {
      for (const scope of scopes) {
        const conflictKey = `${scope}:${batchId}`
        if (used.has(conflictKey)) {
          const batch = batches.value.find(row => Number(row.id) === Number(batchId))
          ElMessage.warning(`${label}的 ${batch?.departureDate || batchId} 已被多个启用价格项绑定`)
          return false
        }
        used.set(conflictKey, item)
      }
    }
  }
  return true
}

const isBlankValue = (value) => value === null || value === undefined || value === ''

const validateRequiredText = (value, message) => {
  if (typeof value === 'string' ? value.trim() : value) return true
  ElMessage.warning(message)
  return false
}

const validateRequiredPositiveAmount = (value, message) => {
  const number = Number(value)
  if (Number.isFinite(number) && number > 0) return true
  ElMessage.warning(message)
  return false
}

const validateOptionalOriginalPrice = (originalPrice, salePrice, message) => {
  if (isBlankValue(originalPrice)) return true
  const original = Number(originalPrice)
  const sale = Number(salePrice)
  if (Number.isFinite(original) && Number.isFinite(sale) && original > sale) return true
  ElMessage.warning(message)
  return false
}

const validateTripPackageForm = () => {
  if (!validateRequiredText(tripPackageForm.value.name, '请输入套餐名称')) return false
  const hasPriceItems = packagePriceItems.value.length > 0
  if (!hasPriceItems && !validateRequiredPositiveAmount(tripPackageForm.value.adultPrice, '未配置套餐价格项时，请输入大于0的成人售价')) return false
  if (!isBlankValue(tripPackageForm.value.adultPrice) && Number(tripPackageForm.value.adultPrice) < 0) {
    ElMessage.warning('成人售价不能小于0')
    return false
  }
  if (!isBlankValue(tripPackageForm.value.childPrice) && Number(tripPackageForm.value.childPrice) < 0) {
    ElMessage.warning('儿童售价不能小于0')
    return false
  }
  if (!isBlankValue(tripPackageForm.value.originalAdultPrice) && !validateRequiredPositiveAmount(tripPackageForm.value.adultPrice, '填写成人划线价时，成人售价必须大于0')) return false
  if (!isBlankValue(tripPackageForm.value.originalChildPrice) && !validateRequiredPositiveAmount(tripPackageForm.value.childPrice, '填写儿童划线价时，儿童售价必须大于0')) return false
  if (!validateOptionalOriginalPrice(tripPackageForm.value.originalAdultPrice, tripPackageForm.value.adultPrice, '成人划线价必须高于成人售价')) return false
  if (!validateOptionalOriginalPrice(tripPackageForm.value.originalChildPrice, tripPackageForm.value.childPrice, '儿童划线价必须高于儿童售价')) return false
  return true
}

const validateBatchPackageForm = () => {
  if (!validateRequiredText(batchPackageForm.value.name, '请输入附加费用名称')) return false
  if (!validateRequiredPositiveAmount(batchPackageForm.value.extraFeePerPerson, '请输入大于0的附加费用售价')) return false
  return true
}

const validatePackagePriceItems = () => {
  for (let index = 0; index < packagePriceItems.value.length; index++) {
    const item = packagePriceItems.value[index]
    const label = `套餐价格项 ${index + 1}`
    if (!validateRequiredPositiveAmount(item.adultPrice, `${label} 的成人售价必须大于0`)) return false
    if (!isBlankValue(item.childPrice) && Number(item.childPrice) < 0) {
      ElMessage.warning(`${label} 的儿童售价不能小于0`)
      return false
    }
    if (!isBlankValue(item.originalChildPrice) && !validateRequiredPositiveAmount(item.childPrice, `${label} 填写儿童划线价时，儿童售价必须大于0`)) return false
    if (!validateOptionalOriginalPrice(item.originalAdultPrice, item.adultPrice, `${label} 的成人划线价必须高于成人售价`)) return false
    if (!validateOptionalOriginalPrice(item.originalChildPrice, item.childPrice, `${label} 的儿童划线价必须高于儿童售价`)) return false
  }
  return true
}

const validateAddonPriceItems = () => {
  for (let index = 0; index < addonPriceItems.value.length; index++) {
    const item = addonPriceItems.value[index]
    if (!validateRequiredPositiveAmount(item.price, `附加费用价格项 ${index + 1} 的售价/份必须大于0`)) return false
    if (Number(item.status ?? 1) !== 1) continue
    for (const packageId of normalizeIdArray(item.packageIds)) {
      if (!isAddonPackageEligible(item, packageId)) {
        const pkg = tripPackages.value.find(row => Number(row.id) === packageId)
        ElMessage.warning(`附加费用价格项 ${index + 1} 中，${pkg?.name || `套餐${packageId}`} 不适用于全部所选日期`)
        return false
      }
    }
  }
  return true
}

const savePackagePriceItems = async (packageId) => {
  if (!packageId) return
  if (!validatePackagePriceItems()) return false
  if (!validatePriceItemBatchConflicts(packagePriceItems.value, '套餐')) return false
  for (const itemId of deletedPackagePriceItemIds.value) {
    await deleteTourPackagePriceItem(packageId, itemId)
  }
  for (let index = 0; index < packagePriceItems.value.length; index++) {
    const item = packagePriceItems.value[index]
    await saveTourPackagePriceItem(packageId, {
      id: item.id,
      name: item.name || `价格项${index + 1}`,
      adultPrice: item.adultPrice,
      childPrice: Number(item.childPrice || 0) > 0 ? item.childPrice : null,
      originalAdultPrice: isBlankValue(item.originalAdultPrice) ? null : item.originalAdultPrice,
      originalChildPrice: isBlankValue(item.originalChildPrice) ? null : item.originalChildPrice,
      batchIds: serializeIdArray(item.batchIds),
      status: item.status ?? 1,
      sortOrder: item.sortOrder ?? index
    })
  }
  deletedPackagePriceItemIds.value = []
  return true
}

const saveAddonPriceItems = async (addonId) => {
  if (!addonId) return
  if (!validateAddonPriceItems()) return false
  if (!validatePriceItemBatchConflicts(
    addonPriceItems.value,
    '附加费用',
    item => normalizeIdArray(item.packageIds).map(packageId => `package-${packageId}`)
  )) return false
  for (const itemId of deletedAddonPriceItemIds.value) {
    await deleteBatchPackagePriceItem(addonId, itemId)
  }
  for (let index = 0; index < addonPriceItems.value.length; index++) {
    const item = addonPriceItems.value[index]
    await saveBatchPackagePriceItem(addonId, {
      id: item.id,
      name: item.name || `价格项${index + 1}`,
      price: item.price,
      packageId: null,
      packageIds: serializeIdArray(item.packageIds),
      originalPrice: null,
      batchIds: serializeIdArray(item.batchIds),
      status: item.status ?? 1,
      sortOrder: item.sortOrder ?? index
    })
  }
  deletedAddonPriceItemIds.value = []
  return true
}

// 图片上传
const handleImageUpload = async (options, index) => {
  const { file, onSuccess, onError } = options
  const formData = new FormData()
  formData.append('file', file)
  try {
    const imageUrl = await request.upload('/file/upload/img', formData, { showDefaultMsg: false })
    if (imageUrl) {
      images.value[index] = imageUrl
      ElMessage.success('图片上传成功')
      onSuccess()
    } else {
      ElMessage.error('上传失败')
      onError(new Error('上传失败'))
    }
  } catch (error) {
    ElMessage.error('图片上传失败: ' + (error.message || '未知错误'))
    onError(error)
  }
}

const removeImage = (index) => {
  images.value[index] = ''
}

const normalizeImageSlots = (value) => {
  const normalized = Array.isArray(value)
    ? [...new Set(value.map(item => String(item || '').trim()).filter(Boolean))].slice(0, 5)
    : []
  while (normalized.length < 5) normalized.push('')
  return normalized
}

const beforeImageUpload = (file) => {
  if (!isSupportedImageFile(file)) {
    ElMessage.error(getSupportedImageMessage())
    return false
  }
  return true
}

// 视频上传
const handleVideoUpload = async (options) => {
  const { file, onSuccess, onError } = options
  const formData = new FormData()
  formData.append('file', file)
  try {
    const url = await request.upload('/file/upload/video', formData, { showDefaultMsg: false })
    if (url) {
      videoUrl.value = url
      saveVideo(true, '视频已保存')
      onSuccess()
    } else {
      ElMessage.error('上传失败')
      onError(new Error('上传失败'))
    }
  } catch (error) {
    ElMessage.error('视频上传失败')
    onError(error)
  }
}

const beforeVideoUpload = (file) => {
  const isVideo = file.type.startsWith('video/')
  const isLt100M = file.size / 1024 / 1024 < 100
  if (!isVideo) ElMessage.error('只能上传视频!')
  if (!isLt100M) ElMessage.error('视频大小不能超过100MB!')
  return isVideo && isLt100M
}

const handlePosterUpload = async (options) => {
  const { file, onSuccess, onError } = options
  const formData = new FormData()
  formData.append('file', file)
  try {
    const poster = await request.upload('/file/upload/img', formData, { showDefaultMsg: false })
    if (poster) {
      videoPoster.value = poster
      saveVideo(true, '封面已保存')
      onSuccess()
    } else {
      ElMessage.error('上传失败')
      onError(new Error('上传失败'))
    }
  } catch (error) {
    ElMessage.error('封面上传失败')
    onError(error)
  }
}

// 行程套餐管理
const showAddTripPackage = () => {
  isTripPackageEdit.value = false
  tripPackageForm.value = { id: null, name: '', adultPrice: null, originalAdultPrice: null, childPrice: null, originalChildPrice: null, description: '', status: 1 }
  packagePriceItems.value = []
  deletedPackagePriceItemIds.value = []
  tripPackageDialogVisible.value = true
  nextTick(() => markSubDialogPristine('tripPackage'))
}

const editTripPackage = async (row) => {
  isTripPackageEdit.value = true
  tripPackageForm.value = { ...row }
  packagePriceItems.value = []
  deletedPackagePriceItemIds.value = []
  tripPackageDialogVisible.value = true
  try {
    const items = await getTourPackagePriceItems(row.id)
    packagePriceItems.value = (items || []).map(normalizePackagePriceItem)
  } catch (error) {
    console.error('获取套餐价格项失败:', error)
  }
  nextTick(() => markSubDialogPristine('tripPackage'))
}

const submitTripPackage = async () => {
  if (!validateTripPackageForm()) return
  tripPackageLoading.value = true
  try {
    const data = {
      ...tripPackageForm.value,
      adultPrice: isBlankValue(tripPackageForm.value.adultPrice) ? 0 : tripPackageForm.value.adultPrice,
      childPrice: Number(tripPackageForm.value.childPrice || 0) > 0 ? tripPackageForm.value.childPrice : null,
      originalAdultPrice: isBlankValue(tripPackageForm.value.originalAdultPrice) ? null : tripPackageForm.value.originalAdultPrice,
      originalChildPrice: isBlankValue(tripPackageForm.value.originalChildPrice) ? null : tripPackageForm.value.originalChildPrice,
      tourId: props.tourId
    }
    if (isTripPackageEdit.value) {
      await updateTourPackage(tripPackageForm.value.id, data)
    } else {
      const created = await addTourPackage(data)
      tripPackageForm.value.id = created?.id || null
    }
    if (tripPackageForm.value.id) {
      const saved = await savePackagePriceItems(tripPackageForm.value.id)
      if (!saved) return
    }
    ElMessage.success('保存成功')
    markSubDialogPristine('tripPackage')
    tripPackageDialogVisible.value = false
    await fetchTripPackages()
    await fetchPackageAvailability()
  } catch (error) {
    console.error('操作失败:', error)
  } finally {
    tripPackageLoading.value = false
  }
}

// 行程套餐删除处理函数
const handleDeleteTripPackage = (row) => {
  ElMessageBox.confirm(`删除套餐"${row.name}"?`, '提示', { type: 'warning' })
    .then(async () => {
      await deleteTourPackage(row.id)
      ElMessage.success('删除成功')
      await fetchTripPackages()
      await fetchPackageAvailability()
    }).catch(() => {})
}

// 批次套餐管理
const showAddBatchPackage = () => {
  isBatchPackageEdit.value = false
  batchPackageForm.value = { id: null, name: '', extraFeePerPerson: 0, description: '', status: 1 }
  addonPriceItems.value = []
  deletedAddonPriceItemIds.value = []
  batchPackageDialogVisible.value = true
  nextTick(() => markSubDialogPristine('batchPackage'))
}

const editBatchPackage = async (row) => {
  isBatchPackageEdit.value = true
  batchPackageForm.value = { ...row }
  addonPriceItems.value = []
  deletedAddonPriceItemIds.value = []
  batchPackageDialogVisible.value = true
  try {
    const items = await getBatchPackagePriceItems(row.id)
    addonPriceItems.value = (items || []).map(normalizeAddonPriceItem)
  } catch (error) {
    console.error('获取附加费用价格项失败:', error)
  }
  nextTick(() => markSubDialogPristine('batchPackage'))
}

const submitBatchPackage = async () => {
  if (!validateBatchPackageForm()) return
  batchPackageLoading.value = true
  try {
    const data = { ...batchPackageForm.value, tourId: props.tourId }
    if (isBatchPackageEdit.value) {
      await updateBatchPackage(batchPackageForm.value.id, data)
    } else {
      const created = await addBatchPackage(data)
      batchPackageForm.value.id = created?.id || null
    }
    if (batchPackageForm.value.id) {
      const saved = await saveAddonPriceItems(batchPackageForm.value.id)
      if (!saved) return
    }
    ElMessage.success('保存成功')
    markSubDialogPristine('batchPackage')
    batchPackageDialogVisible.value = false
    fetchBatchPackages()
  } catch (error) {
    console.error('操作失败:', error)
  } finally {
    batchPackageLoading.value = false
  }
}

// 批次套餐删除直接使用 API
const handleDeleteBatchPackage = (row) => {
  ElMessageBox.confirm(`删除套餐"${row.name}"?`, '提示', { type: 'warning' })
    .then(async () => {
      await deleteBatchPackage(row.id)
      ElMessage.success('删除成功')
      fetchBatchPackages()
    }).catch(() => {})
}

// 班期管理
const isExpired = (dateStr) => {
  if (!dateStr) return false
  return new Date(dateStr) < new Date(new Date().toDateString())
}

const toPositiveNumber = (value) => {
  const number = Number(value)
  return Number.isFinite(number) && number > 0 ? number : null
}

const hasPromotion = (originalPrice, salePrice) => {
  const original = toPositiveNumber(originalPrice)
  const sale = toPositiveNumber(salePrice)
  return Boolean(original && sale && original > sale)
}

const getDiscountLabel = (originalPrice, salePrice) => {
  if (!hasPromotion(originalPrice, salePrice)) return ''
  const discount = Number(salePrice) * 10 / Number(originalPrice)
  return `${Number(discount.toFixed(1)).toString()}折`
}

const formatAmount = (value) => {
  const number = Number(value || 0)
  return Number.isInteger(number) ? String(number) : number.toFixed(2)
}

const formatPackageOptionLabel = (pkg) => {
  if (!pkg) return ''
  const parts = [`成人¥${formatAmount(pkg.adultPrice)}`]
  if (Number(pkg.childPrice || 0) > 0) {
    parts.push(`儿童¥${formatAmount(pkg.childPrice)}`)
  }
  return `${pkg.name || `套餐 ${pkg.id}`}（${parts.join(' / ')}）`
}

const formatAddonOptionLabel = (pkg) => {
  if (!pkg) return ''
  return `${pkg.name || `附加费用 ${pkg.id}`}（¥${formatAmount(pkg.extraFeePerPerson)}/份）`
}

const getAvailableSeats = (batch) => {
  return Math.max(0, (batch?.remaining || 0) - (batch?.occupied || 0))
}

const normalizeBatchCapacity = (batch) => {
  const occupied = Number(batch.occupied || 0)
  const remaining = Number(batch.remaining || 0)
  const maxCapacity = Number(batch.maxCapacity || 0)
  return {
    ...batch,
    occupied,
    remaining: Math.max(occupied, remaining),
    maxCapacity: Math.max(1, remaining, maxCapacity),
    packageIds: normalizeIdArray(batch.packageIds),
    addonIds: normalizeIdArray(batch.addonIds)
  }
}

const normalizeIdArray = (value) => {
  if (!value) return []
  if (Array.isArray(value)) return value.map(Number).filter(Boolean)
  if (typeof value === 'string') {
    try {
      const parsed = JSON.parse(value)
      if (Array.isArray(parsed)) return parsed.map(Number).filter(Boolean)
    } catch (error) {
      void error
    }
    return value.split(/[,\s，、]+/).map(Number).filter(Boolean)
  }
  return []
}

const serializeIdArray = (value) => {
  const ids = normalizeIdArray(value)
  return ids.length ? JSON.stringify(ids) : ''
}

const validateBatchCapacity = (batch) => {
  const occupied = Number(batch.occupied || 0)
  const remaining = Number(batch.remaining || 0)
  const maxCapacity = Number(batch.maxCapacity || 0)
  if (remaining < occupied) {
    ElMessage.warning(`余位不能小于已锁定名额（当前锁定 ${occupied}）`)
    return false
  }
  if (remaining > maxCapacity) {
    ElMessage.warning('余位不能大于最大容量')
    return false
  }
  return true
}

const getStatusType = (status) => {
  switch (status) {
    case '可报名': return 'success'
    case '已满员': return 'warning'
    case '已结束': return 'info'
    default: return 'info'
  }
}

const disabledDate = (date) => {
  return date < new Date(new Date().toDateString())
}

const showAddBatch = () => {
  isBatchEdit.value = false
  batchForm.value = { id: null, departureDate: '', adultDateExtraFee: 0, childDateExtraFee: 0, status: '可报名', remaining: 30, maxCapacity: 50, packageIds: [], addonIds: [] }
  batchDialogVisible.value = true
  nextTick(() => markSubDialogPristine('batch'))
}

const editBatch = (row) => {
  isBatchEdit.value = true
  batchForm.value = normalizeBatchCapacity(row)
  batchDialogVisible.value = true
  nextTick(() => markSubDialogPristine('batch'))
}

const submitBatch = async () => {
  if (!batchForm.value.departureDate) {
    ElMessage.warning('请选择出发日期')
    return
  }
  const data = normalizeBatchCapacity({
    ...batchForm.value,
    tourId: props.tourId
  })
  data.packageIds = serializeIdArray(data.packageIds)
  data.addonIds = serializeIdArray(data.addonIds)
  if (!validateBatchCapacity(data)) return
  batchLoading.value = true
  try {
    if (isBatchEdit.value) {
      await updateTourBatch(batchForm.value.id, data)
    } else {
      await addTourBatch(data)
    }
    ElMessage.success('保存成功')
    markSubDialogPristine('batch')
    batchDialogVisible.value = false
    fetchBatches()
  } catch (error) {
    console.error('操作失败:', error)
  } finally {
    batchLoading.value = false
  }
}

const deleteBatch = (row) => {
  ElMessageBox.confirm(`删除 ${row.departureDate} 的班期?`, '提示', { type: 'warning' })
    .then(async () => {
      await deleteTourBatch(row.id)
      ElMessage.success('删除成功')
      fetchBatches()
    }).catch(() => {})
}

const showBatchAdd = () => {
  batchAddForm.value = { dates: [], adultDateExtraFee: 0, childDateExtraFee: 0, remaining: 30, maxCapacity: 50, status: '可报名', packageIds: [], addonIds: [] }
  batchAddDialogVisible.value = true
  nextTick(() => markSubDialogPristine('batchAdd'))
}

const submitBatchAdd = async () => {
  const dates = [...new Set(batchAddForm.value.dates || [])].sort()
  if (!dates.length) {
    ElMessage.warning('请选择出发日期')
    return
  }
  const normalizedForm = normalizeBatchCapacity(batchAddForm.value)
  if (!validateBatchCapacity(normalizedForm)) return
  batchAddLoading.value = true
  try {
    const batchList = dates.map(date => ({
      tourId: props.tourId, departureDate: date,
      adultDateExtraFee: normalizedForm.adultDateExtraFee,
      childDateExtraFee: normalizedForm.childDateExtraFee,
      status: normalizedForm.status,
      remaining: normalizedForm.remaining, maxCapacity: normalizedForm.maxCapacity,
      packageIds: serializeIdArray(normalizedForm.packageIds),
      addonIds: serializeIdArray(normalizedForm.addonIds)
    }))
    await addTourBatchesBatch(batchList)
    ElMessage.success(`成功添加 ${batchList.length} 个班期`)
    markSubDialogPristine('batchAdd')
    batchAddDialogVisible.value = false
    fetchBatches()
  } catch (error) {
    console.error('批量添加失败:', error)
  } finally {
    batchAddLoading.value = false
  }
}

const updateRemaining = (row) => {
  currentBatch.value = row
  newRemaining.value = Math.max(row.remaining || 0, row.occupied || 0)
  remainingDialogVisible.value = true
  nextTick(() => markSubDialogPristine('remaining'))
}

const submitRemaining = async () => {
  if (!currentBatch.value) return
  const data = normalizeBatchCapacity({ ...currentBatch.value, remaining: newRemaining.value, tourId: props.tourId })
  if (!validateBatchCapacity(data)) return
  try {
    await updateTourBatch(currentBatch.value.id, data)
    ElMessage.success('余位更新成功')
    markSubDialogPristine('remaining')
    closeSubDialogWithConfirm('remaining')
    fetchBatches()
  } catch (error) {
    console.error('更新余位失败:', error)
  }
}

// 出团通知
const saveNotice = async () => {
  noticeLoading.value = true
  try {
    await request.put(`/tour/${props.tourId}`, { notice: notice.value }, { successMsg: '保存成功' })
    markMainPristine()
  } catch (error) {
    console.error('保存失败:', error)
  } finally {
    noticeLoading.value = false
  }
}

// 保存图片
const saveDetailContent = async () => {
  detailContentLoading.value = true
  try {
    await request.put(`/tour/${props.tourId}`, { detailContent: detailContent.value }, { successMsg: '保存成功' })
    markMainPristine()
  } catch (error) {
    console.error('保存行程详细失败:', error)
  } finally {
    detailContentLoading.value = false
  }
}

const saveRefundPolicy = async () => {
  refundPolicyLoading.value = true
  try {
    await request.put(`/tour/${props.tourId}`, { refundPolicyContent: refundPolicyContent.value }, { successMsg: '保存成功' })
    markMainPristine()
  } catch (error) {
    console.error('保存退订政策失败:', error)
  } finally {
    refundPolicyLoading.value = false
  }
}

const saveImages = async () => {
  try {
    const imagesToSave = [...new Set(images.value.map(img => String(img || '').trim()).filter(Boolean))].slice(0, 5)
    await updateTourImages(props.tourId, imagesToSave)
    ElMessage.success('图片保存成功')
    markMainPristine()
  } catch (error) {
    console.error('保存图片失败:', error)
  }
}

// 保存视频
const saveVideo = async (showMsg = false, msg = '视频已保存') => {
  try {
    await updateTourVideo(props.tourId, {
      videoUrl: videoUrl.value,
      videoPoster: videoPoster.value,
      videoEnabled: videoEnabled.value ? 1 : 0
    })
    markMainPristine()
    if (showMsg) {
      ElMessage.success(msg)
    }
  } catch (error) {
    console.error('保存视频失败:', error)
  }
}

// 标记是否初始化完成
let isVideoInitialized = false

// tourId 变化时重置初始化标志
watch(() => props.tourId, () => {
  isVideoInitialized = false
})

// 启用视频开关变化时自动保存
const handleVideoEnabledChange = () => {
  if (!isVideoInitialized) return
  saveVideo(true, videoEnabled.value ? '视频已启用' : '视频已关闭')
}

// 删除视频后自动保存
const handleRemoveVideo = () => {
  if (!isVideoInitialized) return
  videoUrl.value = ''
  videoPoster.value = ''
  saveVideo(false)
}

// ==================== 酒店预订管理 ====================

// 获取可选住宿列表
const fetchAccommodationList = async () => {
  try {
    const res = await request.get('/accommodation/page', { currentPage: 1, size: 1000 })
    accommodationList.value = res?.records || []
  } catch (error) {
    console.error('获取住宿列表失败:', error)
  }
}

// 获取行程关联的酒店
const fetchTourHotels = async () => {
  try {
    const res = await request.get(`/tour-hotel/${props.tourId}`, {}, { showDefaultMsg: false })
    tourHotels.value = res || []
  } catch (error) {
    console.error('获取行程酒店失败:', error)
    tourHotels.value = []
  }
}

// 显示添加酒店对话框
const showAddHotel = () => {
  isHotelEdit.value = false
  hotelForm.value = {
    id: null,
    accommodationId: null,
    name: '',
    type: '',
    pricePerNight: 0,
    days: productInfo.value.days || 1,
    enabled: 1,
    imageUrl: '',
    starLevel: null
  }
  hotelDialogVisible.value = true
  nextTick(() => markSubDialogPristine('hotel'))
}

// 编辑酒店
const editHotel = (row) => {
  isHotelEdit.value = true
  hotelForm.value = { ...row }
  hotelDialogVisible.value = true
  nextTick(() => markSubDialogPristine('hotel'))
}

// 选择住宿时自动填充信息
const handleAccommodationSelect = (accommodationId) => {
  const accommodation = accommodationList.value.find(acc => acc.id === accommodationId)
  if (accommodation) {
    hotelForm.value.name = accommodation.name
    hotelForm.value.type = accommodation.type
    hotelForm.value.imageUrl = accommodation.imageUrl
    hotelForm.value.starLevel = accommodation.starLevel
    // 从价格区间提取价格
    const priceMatch = accommodation.priceRange?.match(/(\d+)/)
    if (priceMatch) {
      hotelForm.value.pricePerNight = parseInt(priceMatch[1])
    }
  }
}

// 提交酒店
const submitHotel = async () => {
  if (!hotelForm.value.name) {
    ElMessage.warning('请输入酒店名称')
    return
  }
  hotelLoading.value = true
  try {
    const data = {
      ...hotelForm.value,
      tourId: props.tourId
    }
    if (isHotelEdit.value) {
      await request.put(`/tour-hotel/${hotelForm.value.id}`, data)
    } else {
      await request.post('/tour-hotel', data)
    }
    ElMessage.success('保存成功')
    markSubDialogPristine('hotel')
    hotelDialogVisible.value = false
    fetchTourHotels()
  } catch (error) {
    console.error('保存酒店失败:', error)
  } finally {
    hotelLoading.value = false
  }
}

// 删除酒店
const deleteHotel = (row) => {
  ElMessageBox.confirm(`删除酒店"${row.name}"?`, '提示', { type: 'warning' })
    .then(async () => {
      await request.delete(`/tour-hotel/${row.id}`)
      ElMessage.success('删除成功')
      fetchTourHotels()
    }).catch(() => {})
}

// 酒店天数变化
const handleHotelDaysChange = async (row) => {
  try {
    await request.put(`/tour-hotel/${row.id}`, {
      ...row,
      tourId: props.tourId
    })
  } catch (error) {
    console.error('更新酒店天数失败:', error)
    fetchTourHotels()
  }
}

// 酒店启用状态变化
const handleHotelEnabledChange = async (row) => {
  try {
    await request.put(`/tour-hotel/${row.id}`, {
      ...row,
      tourId: props.tourId
    })
  } catch (error) {
    console.error('更新酒店状态失败:', error)
    fetchTourHotels()
  }
}
</script>

<style scoped>
.image-section, .video-section, .package-section, .batch-section, .notice-section {
  padding: 10px 0;
}

.batch-table {
  width: 100%;
  table-layout: fixed;
  border-collapse: collapse;
  border: 1px solid #ebeef5;
  font-size: 13px;
}

.batch-table th,
.batch-table td {
  height: 36px;
  padding: 5px 6px;
  text-align: center;
  vertical-align: middle;
  border-right: 1px solid #ebeef5;
  border-bottom: 1px solid #ebeef5;
  box-sizing: border-box;
}

.batch-table th {
  color: #909399;
  font-weight: 600;
  background: #f8fafc;
}

.batch-table td {
  color: #606266;
  background: #fff;
  word-break: break-word;
}

.batch-table .batch-action-cell {
  padding-left: 4px;
  padding-right: 4px;
}

.table-actions {
  display: flex;
  justify-content: center;
  gap: 4px;
  flex-wrap: nowrap;
  min-width: 0;
}

.batch-action-cell .table-actions :deep(.el-button) {
  min-width: 0;
  padding-left: 7px;
  padding-right: 7px;
}

.table-actions :deep(.el-button + .el-button) {
  margin-left: 0;
}

.capacity-tip {
  margin: -4px 0 8px 100px;
  color: #909399;
  font-size: 12px;
  line-height: 1.5;
}

.image-list {
  display: flex;
  gap: 15px;
  flex-wrap: wrap;
}

.image-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.image-label {
  font-weight: 600;
  color: #409eff;
}

.image-uploader {
  :deep(.el-upload) {
    border: 1px dashed #d9d9d9;
    border-radius: 6px;
    cursor: pointer;
    overflow: hidden;
  }
}

.uploaded-image, .poster-image {
  width: 140px;
  height: 100px;
  object-fit: cover;
  display: block;
}

.upload-placeholder {
  width: 140px;
  height: 100px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #8c939d;
  font-size: 12px;
}

.upload-placeholder.small {
  width: 140px;
  height: 80px;
}

.image-tip, .video-tip {
  margin-top: 10px;
  font-size: 12px;
  color: #909399;
}

.video-uploader {
  :deep(.el-upload) {
    border: 1px dashed #d9d9d9;
    border-radius: 6px;
    cursor: pointer;
  }
}

.video-preview {
  position: relative;
  display: inline-block;
}

.preview-video {
  width: 300px;
  height: 170px;
  object-fit: cover;
}

.video-preview .el-button {
  position: absolute;
  bottom: 8px;
  right: 8px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  margin-bottom: 15px;
}

.section-title-block {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}

.legacy-note {
  color: #909399;
  font-size: 12px;
  font-weight: 400;
  line-height: 1.45;
}

.header-actions {
  display: flex;
  gap: 10px;
}

.price {
  color: #f56c6c;
  font-weight: 600;
}

.origin-price {
  color: #909399;
  text-decoration: line-through;
}

.form-tip {
  margin-top: 6px;
  color: #909399;
  font-size: 12px;
  line-height: 1.5;
}

.price-item-editor {
  margin-top: 14px;
  padding: 14px;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  background: #fafafa;
}

.price-item-head,
.price-item-card-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.price-item-head {
  margin-bottom: 12px;
}

.price-item-head h4 {
  margin: 0;
  color: #1f2937;
  font-size: 14px;
}

.price-item-head p {
  margin: 5px 0 0;
  color: #6b7280;
  font-size: 12px;
  line-height: 1.5;
}

.price-item-empty {
  padding: 14px;
  border: 1px dashed #d1d5db;
  border-radius: 6px;
  color: #8c939d;
  font-size: 12px;
  text-align: center;
  background: #fff;
}

.price-item-card {
  padding: 12px;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  background: #fff;
}

.price-item-card + .price-item-card {
  margin-top: 10px;
}

.price-item-card-head {
  align-items: center;
  margin-bottom: 10px;
}

.price-item-card-head strong {
  color: #374151;
  font-size: 13px;
}

.price-item-grid {
  display: grid;
  gap: 10px;
  align-items: center;
}

.package-price-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.addon-price-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.price-field {
  display: grid;
  grid-template-columns: 86px minmax(0, 1fr);
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.price-field span {
  color: #606266;
  font-size: 12px;
  font-weight: 600;
  white-space: nowrap;
}

.price-field--switch {
  grid-template-columns: 86px auto;
  grid-column: 1 / -1;
  justify-content: start;
}

.addon-price-grid .price-field--switch {
  grid-column: auto;
}

.price-item-grid :deep(.el-input-number) {
  width: 100%;
}

.price-item-batches {
  width: 100%;
  margin-top: 10px;
}

@media (max-width: 900px) {
  .package-price-grid,
  .addon-price-grid {
    grid-template-columns: 1fr;
  }
}

.warning {
  color: #e6a23c;
  font-weight: 600;
}

.expired {
  color: #c0c4cc;
}

.notice-actions {
  margin-top: 15px;
}

.section-actions {
  margin-top: 15px;
}

:deep(.tour-detail-dialog .el-dialog__body) {
  padding: 15px 20px;
}

:deep(.el-tab-pane) {
  padding: 10px 0;
}

.dialog-footer {
  margin-top: 20px;
  text-align: right;
}

/* 酒店预订样式 */
.hotel-booking-section {
  padding: 10px 0;
}

.hotel-table {
  margin-bottom: 15px;
}

.hotel-info-cell {
  display: flex;
  align-items: center;
  gap: 12px;
}

.hotel-thumb {
  width: 60px;
  height: 45px;
  object-fit: cover;
  border-radius: 4px;
  border: 1px solid #eee;
}

.hotel-thumb-placeholder {
  width: 60px;
  height: 45px;
  background: #f5f5f5;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
}

.hotel-details {
  flex: 1;
  min-width: 0;
}

.hotel-name {
  font-size: 14px;
  font-weight: 600;
  color: #333;
  margin-bottom: 4px;
}

.hotel-meta {
  display: flex;
  align-items: center;
  gap: 8px;
}

.hotel-rating {
  font-size: 12px;
  color: #f60;
}

.hotel-rating .star {
  color: #ffb800;
}

.price-text {
  color: #f56c6c;
  font-weight: 600;
}

.total-price {
  color: #f60;
  font-weight: 700;
  font-size: 14px;
}

.hotel-tip {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px;
  background: #f0f9ff;
  border-radius: 4px;
  font-size: 12px;
  color: #666;
}

.hotel-tip .el-icon {
  color: #409eff;
}

/* 住宿选择框样式 */
.accommodation-option {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.accommodation-type {
  font-size: 12px;
  color: #999;
}

.table-actions {
  display: flex;
  justify-content: center;
  gap: 4px;
  flex-wrap: nowrap;
  min-width: 0;
}

.table-actions :deep(.el-button + .el-button) {
  margin-left: 0;
}
</style>
