<template>
  <div class="profile-container">
    <div class="page-header-wrapper">
      <div class="page-header">
        <div class="header-content">
          <div class="eyebrow">Account Center</div>
          <h1 class="page-title">个人中心</h1>
          <p class="page-subtitle">管理展示资料、登录手机号与邮箱验证信息</p>
        </div>
        <div class="header-summary">
          <span>账户安全</span>
          <strong>{{ userForm.phone && userForm.email ? '已完善' : '待完善' }}</strong>
        </div>
      </div>
    </div>

    <!-- 现代化标签页区域 -->
    <div class="profile-section">
      <div class="section-container">
          <div class="profile-tabs">
            <el-tabs
            v-model="activeTab"
            class="modern-tabs"
          >
            <!-- 基本信息标签页 -->
            <el-tab-pane label="基本信息" name="basic">
              <template #label>
                <div class="tab-label">
                  <el-icon><User /></el-icon>
                  <span>基本信息</span>
                </div>
              </template>

              <div class="profile-content profile-redesign">
                <section class="account-overview">
                  <div class="account-person">
                    <div class="avatar-frame">
                      <el-avatar :size="92" :src="avatarUrl" class="profile-avatar">
                        <span>{{ displayName.charAt(0) || '用' }}</span>
                      </el-avatar>
                      <el-upload
                        action="#"
                        :auto-upload="true"
                        :show-file-list="false"
                        :http-request="customUploadAvatar"
                        :before-upload="beforeAvatarUpload"
                      >
                        <el-tooltip content="更换头像" placement="top">
                          <button class="avatar-action" type="button">
                            <el-icon><Camera /></el-icon>
                          </button>
                        </el-tooltip>
                      </el-upload>
                    </div>
                    <div class="person-copy">
                      <span class="status-pill">当前账户</span>
                      <h2>{{ displayName }}</h2>
                      <p>{{ userForm.phone ? `登录手机号 ${maskedPhone}` : '请绑定手机号以保护账户安全' }}</p>
                    </div>
                  </div>
                  <div class="overview-metrics">
                    <div class="metric-item">
                      <span>安全状态</span>
                      <strong>{{ securityLevelText }}</strong>
                    </div>
                    <div class="metric-item">
                      <span>邮箱</span>
                      <strong>{{ userForm.email ? '已验证' : '未绑定' }}</strong>
                    </div>
                    <div class="metric-item">
                      <span>手机</span>
                      <strong>{{ userForm.phone ? '已验证' : '待验证' }}</strong>
                    </div>
                  </div>
                </section>

                <div class="profile-grid">
                  <section class="profile-card profile-editor">
                    <div class="section-heading">
                      <div>
                        <span>Profile</span>
                        <h3>展示资料</h3>
                      </div>
                      <p>昵称会显示在评论、攻略和个人中心中，账号标识仅用于系统兼容与审计。</p>
                    </div>

                    <el-form ref="userFormRef" :model="userForm" :rules="rules" label-position="top" class="clean-form" hide-required-asterisk>
                      <div class="field-stack">
                        <el-form-item label="账号标识" prop="username" class="quiet-field">
                          <el-input v-model="userForm.username" disabled :prefix-icon="User" />
                        </el-form-item>
                        <el-form-item label="昵称" prop="nickname">
                          <el-input v-model="userForm.nickname" :prefix-icon="EditPen" placeholder="请输入昵称" maxlength="30" />
                        </el-form-item>
                        <el-form-item label="性别" prop="sex">
                          <el-segmented v-model="userForm.sex" :options="genderOptions" />
                        </el-form-item>
                      </div>
                      <div class="editor-actions">
                        <span>保存后会同步更新顶部欢迎语和个人展示名</span>
                        <el-button type="primary" @click="submitUserInfo" :loading="savingProfile">
                          <el-icon><Check /></el-icon>
                          保存资料
                        </el-button>
                      </div>
                    </el-form>
                  </section>

                  <section class="profile-card security-panel">
                    <div class="section-heading">
                      <div>
                        <span>Security</span>
                        <h3>账号绑定</h3>
                      </div>
                      <p>邮箱和手机号需要先完成极验验证，再通过验证码确认绑定或更换。</p>
                    </div>

                    <div class="binding-list">
                      <article class="binding-card">
                        <div class="binding-icon mail">
                          <el-icon><Message /></el-icon>
                        </div>
                        <div class="binding-body">
                          <div class="binding-title">
                            <h4>电子邮箱</h4>
                            <span :class="{ verified: userForm.email }">{{ userForm.email ? '已验证' : '未绑定' }}</span>
                          </div>
                          <p>{{ maskedEmail }}</p>
                        </div>
                        <el-button plain @click="openEmailDialog">
                          {{ userForm.email ? '更换' : '绑定' }}
                        </el-button>
                      </article>

                      <article class="binding-card">
                        <div class="binding-icon mobile">
                          <el-icon><Phone /></el-icon>
                        </div>
                        <div class="binding-body">
                          <div class="binding-title">
                            <h4>手机号码</h4>
                            <span :class="{ verified: userForm.phone }">{{ userForm.phone ? '已验证' : '待验证' }}</span>
                          </div>
                          <p>{{ maskedPhone }}</p>
                        </div>
                        <el-button plain @click="openPhoneDialog">
                          {{ userForm.phone ? '更换' : '绑定' }}
                        </el-button>
                      </article>
                    </div>
                  </section>
                </div>
              </div>
            </el-tab-pane>

            <!-- 修改密码标签页 -->
            <el-tab-pane label="修改密码" name="password">
              <template #label>
                <div class="tab-label">
                  <el-icon><Lock /></el-icon>
                  <span>修改密码</span>
                </div>
              </template>

              <div class="password-content">
                <div class="password-card">
                  <div class="password-header">
                    <div class="password-icon">
                      <el-icon><Lock /></el-icon>
                    </div>
                    <div>
                      <span>Security Check</span>
                      <h3 class="password-title">修改登录密码</h3>
                      <p class="password-desc">修改前需要完成极验，并验证当前绑定手机号。</p>
                    </div>
                  </div>

                  <el-form
                    ref="passwordFormRef"
                    :model="passwordForm"
                    :rules="passwordRules"
                    label-position="top"
                    class="password-form secure-form"
                    hide-required-asterisk
                  >
                    <div class="password-grid">
                      <div class="password-fields">
                        <el-form-item label="当前密码" prop="oldPassword">
                          <el-input v-model="passwordForm.oldPassword" show-password placeholder="请输入当前密码" :prefix-icon="Lock" />
                        </el-form-item>

                        <el-form-item label="新密码" prop="newPassword">
                          <el-input v-model="passwordForm.newPassword" show-password placeholder="请输入新密码" :prefix-icon="Key" />
                        </el-form-item>

                        <el-form-item label="确认新密码" prop="confirmPassword">
                          <el-input v-model="passwordForm.confirmPassword" show-password placeholder="请再次输入新密码" :prefix-icon="Key" />
                        </el-form-item>

                        <el-form-item label="当前手机验证码" prop="currentPhoneCode">
                          <div class="code-row password-code-row">
                            <el-input v-model="passwordForm.currentPhoneCode" :prefix-icon="Phone" placeholder="请输入6位验证码" maxlength="6" clearable />
                            <el-button :loading="passwordCodeSending" :disabled="passwordCountdown > 0" @click="sendPasswordCode">
                              {{ passwordCountdown > 0 ? `${passwordCountdown}s` : '发送验证码' }}
                            </el-button>
                          </div>
                        </el-form-item>
                      </div>

                      <aside class="password-verify">
                        <div class="verify-card">
                          <span>手机验证</span>
                          <strong>{{ maskedPhone }}</strong>
                          <p>验证码将发送到当前绑定手机号。若手机号不可用，请先在基本信息中完成换绑。</p>
                        </div>
                        <div class="captcha-shell password-captcha">
                          <GeetestBox
                            ref="passwordCaptchaRef"
                            success-text="验证已通过，可以发送手机验证码"
                            @verified="handlePasswordGeetestVerified"
                            @error="handlePasswordGeetestError"
                            @unavailable="handlePasswordGeetestUnavailable"
                          />
                        </div>
                      </aside>
                    </div>

                    <div class="form-actions password-actions">
                      <span>密码修改成功后需要重新登录。</span>
                      <el-button type="primary" @click="submitPassword" class="save-btn" size="large">
                        <el-icon><Check /></el-icon>
                        确认修改
                      </el-button>
                    </div>
                  </el-form>
                </div>
              </div>
            </el-tab-pane>

            <el-tab-pane label="常用出行人" name="travelers">
              <template #label>
                <div class="tab-label">
                  <el-icon><User /></el-icon>
                  <span>常用出行人</span>
                </div>
              </template>

              <div class="traveler-center">
                <section class="traveler-hero">
                  <div>
                    <span>Travelers</span>
                    <h3>常用出行人</h3>
                    <p>维护本人或同行人的实名资料，预订行程时可直接选择，避免重复填写。</p>
                  </div>
                  <el-button type="primary" size="large" @click="openTravelerDialog()">
                    <el-icon><Plus /></el-icon>
                    新增出行人
                  </el-button>
                </section>

                <div v-if="travelerLoading" class="loading-state">
                  <el-skeleton :rows="5" animated />
                </div>
                <el-empty v-else-if="frequentTravelers.length === 0" description="暂无常用出行人">
                  <el-button type="primary" @click="openTravelerDialog()">添加出行人</el-button>
                </el-empty>
                <div v-else class="traveler-profile-list">
                  <article
                    v-for="traveler in frequentTravelers"
                    :key="traveler.id"
                    class="traveler-profile-card"
                  >
                    <div class="traveler-card-identity">
                      <div class="traveler-avatar">{{ traveler.name?.charAt(0) || '旅' }}</div>
                      <div class="traveler-main">
                        <h4>{{ traveler.name }}</h4>
                        <div class="traveler-tags">
                          <span>{{ traveler.travelerType === 'CHILD' ? '儿童' : '成人' }}</span>
                          <span>{{ traveler.gender === 'FEMALE' ? '女' : '男' }}</span>
                        </div>
                      </div>
                    </div>

                    <div class="traveler-fields">
                      <div>
                        <span>手机号码</span>
                        <strong>{{ maskPhone(traveler.phone, '未填写') }}</strong>
                      </div>
                      <div>
                        <span>证件类型</span>
                        <strong>{{ getTravelerIdTypeLabel(traveler.idType) }}</strong>
                      </div>
                      <div class="wide">
                        <span>证件号码</span>
                        <strong>{{ maskTravelerIdNumber(traveler.idNumber) }}</strong>
                      </div>
                      <div>
                        <span>出生日期</span>
                        <strong>{{ formatTravelerDate(traveler.birthDate) || '-' }}</strong>
                      </div>
                    </div>

                    <div class="traveler-card-actions">
                      <el-button plain @click="openTravelerDialog(traveler)">
                        <el-icon><EditPen /></el-icon>
                        编辑
                      </el-button>
                      <el-button type="danger" plain @click="removeTraveler(traveler)">
                        <el-icon><Delete /></el-icon>
                        删除
                      </el-button>
                    </div>
                  </article>
                </div>
              </div>
            </el-tab-pane>

            <!-- 我的评论标签页 -->
            <el-tab-pane label="我的评论" name="mycomments">
              <template #label>
                <div class="tab-label">
                  <el-icon><ChatDotRound /></el-icon>
                  <span>我的评论</span>
                </div>
              </template>

              <div class="my-comments-content">
                <div v-if="myCommentsLoading" class="loading-state">
                  <el-skeleton :rows="5" animated />
                </div>
                <div v-else-if="myComments.length === 0" class="empty-state">
                  <div class="empty-icon">💬</div>
                  <h3 class="empty-title">您还没有发表任何评论</h3>
                  <p class="empty-desc">去景点页面发表您的第一条评论吧</p>
                </div>
                <div v-else class="comment-list">
                  <div v-for="comment in myComments" :key="comment.id" class="comment-card">
                    <div class="comment-header">
                      <div class="scenic-info">
                        <el-icon><Location /></el-icon>
                        <span>{{ comment.targetName }}</span>
                      </div>
                      <div class="comment-tags">
                        <el-tag :type="comment.commentType === 'hotel' ? 'success' : 'primary'" size="small" effect="plain">
                          {{ comment.commentType === 'hotel' ? '酒店评论' : '景区评论' }}
                        </el-tag>
                        <el-tag :type="getReviewStatusType(comment.reviewStatus)" size="small">
                          {{ getReviewStatusText(comment.reviewStatus) }}
                        </el-tag>
                      </div>
                    </div>
                    <div class="comment-body">
                      <div class="comment-rating">
                        <el-rate v-model="comment.rating" disabled size="small" />
                      </div>
                      <p class="comment-content">{{ comment.content }}</p>
                    </div>
                    <div class="comment-footer">
                      <span class="comment-time">{{ formatDate(comment.createTime) }}</span>
                      <el-button
                        type="danger"
                        size="small"
                        @click="deleteMyComment(comment)"
                        v-if="comment.reviewStatus !== 1"
                      >
                        删除
                      </el-button>
                    </div>
                  </div>
                </div>
                <div class="pagination-wrapper" v-if="commentTotal > 0">
                  <el-pagination
                    background
                    layout="total, prev, pager, next"
                    :total="commentTotal"
                    :page-size="commentPageSize"
                    :current-page="commentPage"
                    @current-change="handleCommentPageChange"
                  />
                </div>
              </div>
            </el-tab-pane>

            <el-tab-pane label="站内消息" name="notifications">
              <template #label>
                <div class="tab-label">
                  <el-icon><Bell /></el-icon>
                  <span>站内消息</span>
                </div>
              </template>

              <div class="notifications-content">
                <div class="notification-toolbar">
                  <el-radio-group v-model="notificationReadStatus" @change="handleNotificationFilterChange" size="large">
                    <el-radio-button label="">全部</el-radio-button>
                    <el-radio-button :label="0">未读</el-radio-button>
                    <el-radio-button :label="1">已读</el-radio-button>
                  </el-radio-group>
                  <el-button type="primary" plain @click="markAllNotificationsRead" :disabled="notificationUnreadCount === 0">
                    全部标为已读
                  </el-button>
                </div>

                <div v-if="notificationLoading" class="loading-state">
                  <el-skeleton :rows="5" animated />
                </div>
                <el-empty v-else-if="notifications.length === 0" description="暂无站内消息" />
                <div v-else class="notification-list">
                  <div
                    v-for="item in notifications"
                    :key="item.id"
                    class="notification-card"
                    :class="{ unread: item.readStatus === 0 }"
                    @click="openNotification(item)"
                  >
                    <div class="notification-main">
                      <div class="notification-title-row">
                        <h3>{{ item.title }}</h3>
                        <el-tag v-if="item.readStatus === 0" type="danger" size="small">未读</el-tag>
                        <el-tag v-else type="info" size="small">已读</el-tag>
                      </div>
                      <p>{{ item.content }}</p>
                      <div class="notification-meta">
                        <span>发送人：{{ item.senderName || '系统' }}</span>
                        <span>{{ formatDate(item.createTime) }}</span>
                      </div>
                    </div>
                    <div class="notification-actions">
                      <el-button v-if="item.readStatus === 0" text type="primary" @click.stop="markNotificationRead(item)">
                        标为已读
                      </el-button>
                      <el-button v-if="item.linkUrl" text type="primary" @click.stop="openNotification(item)">
                        查看详情
                      </el-button>
                    </div>
                  </div>
                </div>

                <div class="pagination-wrapper" v-if="notificationTotal > 0">
                  <el-pagination
                    background
                    layout="total, prev, pager, next"
                    :total="notificationTotal"
                    :page-size="notificationPageSize"
                    :current-page="notificationPage"
                    @current-change="handleNotificationPageChange"
                  />
                </div>
              </div>
            </el-tab-pane>
          </el-tabs>
        </div>
      </div>
    </div>

    <el-dialog v-model="emailDialogVisible" :title="userForm.email ? '更换邮箱' : '绑定邮箱'" width="520px" class="secure-dialog">
      <el-form :model="emailBindForm" :rules="emailBindRules" ref="emailBindFormRef" label-position="top" class="secure-form" hide-required-asterisk>
        <el-form-item label="电子邮箱" prop="email">
          <el-input v-model="emailBindForm.email" :prefix-icon="Message" placeholder="请输入需要绑定的邮箱" clearable />
        </el-form-item>
        <div class="captcha-shell">
          <GeetestBox
            ref="emailCaptchaRef"
            success-text="验证已通过，可以发送邮箱验证码"
            @verified="handleEmailGeetestVerified"
            @error="handleEmailGeetestError"
            @unavailable="handleEmailGeetestUnavailable"
          />
        </div>
        <el-form-item label="邮箱验证码" prop="code">
          <div class="code-row">
            <el-input v-model="emailBindForm.code" placeholder="请输入6位验证码" maxlength="6" clearable />
            <el-button :loading="emailCodeSending" :disabled="emailCountdown > 0" @click="sendEmailBindCode">
              {{ emailCountdown > 0 ? `${emailCountdown}s` : '发送验证码' }}
            </el-button>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="emailDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="emailBinding" @click="confirmEmailBind">确认绑定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="phoneDialogVisible" :title="userForm.phone ? '更换手机号' : '绑定手机号'" width="520px" class="secure-dialog">
      <el-form :model="phoneBindForm" :rules="phoneBindRules" ref="phoneBindFormRef" label-position="top" class="secure-form" hide-required-asterisk>
        <el-form-item label="手机号码" prop="phone">
          <el-input v-model="phoneBindForm.phone" :prefix-icon="Phone" placeholder="请输入新的手机号码" maxlength="11" clearable />
        </el-form-item>
        <div class="captcha-shell">
          <GeetestBox
            ref="phoneCaptchaRef"
            success-text="验证已通过，可以发送短信验证码"
            @verified="handlePhoneGeetestVerified"
            @error="handlePhoneGeetestError"
            @unavailable="handlePhoneGeetestUnavailable"
          />
        </div>
        <el-form-item label="短信验证码" prop="smsCode">
          <div class="code-row">
            <el-input v-model="phoneBindForm.smsCode" placeholder="请输入6位验证码" maxlength="6" clearable />
            <el-button :loading="phoneCodeSending" :disabled="phoneCountdown > 0" @click="sendPhoneChangeCode">
              {{ phoneCountdown > 0 ? `${phoneCountdown}s` : '发送验证码' }}
            </el-button>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="phoneDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="phoneBinding" @click="confirmPhoneChange">确认变更</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="travelerDialogVisible"
      :title="editingTraveler ? '编辑出行人' : '新增出行人'"
      width="640px"
      class="secure-dialog traveler-profile-dialog"
      :close-on-click-modal="false"
    >
      <el-form
        ref="travelerFormRef"
        :model="travelerForm"
        :rules="travelerRules"
        label-position="top"
        class="secure-form traveler-manage-form"
        hide-required-asterisk
      >
        <div class="traveler-form-grid">
          <el-form-item label="姓名" prop="name">
            <el-input v-model="travelerForm.name" placeholder="请输入出行人真实姓名" clearable />
          </el-form-item>
          <el-form-item label="手机号码" prop="phone">
            <el-input v-model="travelerForm.phone" placeholder="请输入 11 位手机号码" clearable maxlength="11" />
          </el-form-item>
          <el-form-item label="性别" prop="gender">
            <el-segmented v-model="travelerForm.gender" :options="travelerGenderOptions" />
          </el-form-item>
          <el-form-item label="出行人类型" prop="travelerType">
            <el-segmented v-model="travelerForm.travelerType" :options="travelerTypeOptions" />
          </el-form-item>
          <el-form-item label="证件类型" prop="idType">
            <el-select v-model="travelerForm.idType" placeholder="请选择证件类型">
              <el-option label="身份证" value="ID_CARD" />
              <el-option label="护照" value="PASSPORT" />
            </el-select>
          </el-form-item>
          <el-form-item label="出生日期" prop="birthDate">
            <el-date-picker
              v-model="travelerForm.birthDate"
              type="date"
              placeholder="请选择出生日期"
              format="YYYY-MM-DD"
              value-format="YYYY-MM-DD"
              :disabled-date="disableFutureDate"
              style="width: 100%"
            />
          </el-form-item>
          <el-form-item label="证件号码" prop="idNumber" class="wide">
            <el-input v-model="travelerForm.idNumber" :placeholder="travelerIdNumberPlaceholder" clearable :maxlength="travelerForm.idType === 'ID_CARD' ? 18 : 20" />
          </el-form-item>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="travelerDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="travelerSaving" @click="saveTravelerProfile">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted, watch } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { useUserStore } from "@/store/user";
import { useRoute, useRouter } from "vue-router";
import request from "@/utils/request";
import GeetestBox from '@/components/auth/GeetestBox.vue'
import {User,Lock,Key,Check,Camera,Phone,Message,EditPen,ChatDotRound,Location,Bell,Plus,Delete} from '@element-plus/icons-vue'
import { formatDate } from '@/utils/dateUtils'
import { maskEmail, maskPhone } from '@/utils/mask'
import { getFrequentTravelers, saveFrequentTraveler, updateFrequentTraveler, deleteFrequentTraveler } from '@/api/frequentTraveler'

const baseAPI = process.env.VUE_APP_BASE_API || "/api";
const userStore = useUserStore();
const route = useRoute();
const router = useRouter();
const activeTab = ref("basic");

// 我的评论相关
const myComments = ref([])
const myCommentsLoading = ref(false)
const commentPage = ref(1)
const commentPageSize = ref(10)
const commentTotal = ref(0)

// 站内消息相关
const notifications = ref([])
const notificationLoading = ref(false)
const notificationPage = ref(1)
const notificationPageSize = ref(10)
const notificationTotal = ref(0)
const notificationUnreadCount = ref(0)
const notificationReadStatus = ref('')
let notificationRefreshTimer = null
const NOTIFICATION_REFRESH_INTERVAL = 15000

// 常用出行人
const frequentTravelers = ref([])
const travelerLoading = ref(false)
const travelerSaving = ref(false)
const travelerDialogVisible = ref(false)
const editingTraveler = ref(null)
const travelerFormRef = ref(null)

// 表单引用
const userFormRef = ref(null);
const passwordFormRef = ref(null);
const emailBindFormRef = ref(null);
const phoneBindFormRef = ref(null);
const emailCaptchaRef = ref(null);
const phoneCaptchaRef = ref(null);
const passwordCaptchaRef = ref(null);
const savingProfile = ref(false);
const emailDialogVisible = ref(false);
const phoneDialogVisible = ref(false);
const emailCodeSending = ref(false);
const phoneCodeSending = ref(false);
const passwordCodeSending = ref(false);
const emailBinding = ref(false);
const phoneBinding = ref(false);
const emailCountdown = ref(0);
const phoneCountdown = ref(0);
const passwordCountdown = ref(0);
const emailGeetestValidate = ref(null);
const phoneGeetestValidate = ref(null);
const passwordGeetestValidate = ref(null);
const emailGeetestRequired = ref(true);
const phoneGeetestRequired = ref(true);
const passwordGeetestRequired = ref(true);
let emailTimer = null;
let phoneTimer = null;
let passwordTimer = null;

// 用户表单数据
const userForm = reactive({
  id: "",
  username: "",
  nickname: "",
  email: "",
  phone: "",
  sex: "",
  avatar: "",
});

const emailBindForm = reactive({
  email: "",
  code: "",
});

const phoneBindForm = reactive({
  phone: "",
  smsCode: "",
});

const travelerForm = reactive({
  name: '',
  gender: 'MALE',
  travelerType: 'ADULT',
  phone: '',
  idType: 'ID_CARD',
  idNumber: '',
  birthDate: ''
})

const genderOptions = ["男", "女"];
const travelerGenderOptions = [
  { label: '男', value: 'MALE' },
  { label: '女', value: 'FEMALE' }
]
const travelerTypeOptions = [
  { label: '成人', value: 'ADULT' },
  { label: '儿童', value: 'CHILD' }
]
const travelerIdNumberPlaceholder = computed(() => travelerForm.idType === 'ID_CARD' ? '请输入 18 位身份证号码' : '请输入 5-20 位护照号码')

const displayName = computed(() => userForm.nickname || userForm.username || "用户");
const securityLevelText = computed(() => {
  if (userForm.phone && userForm.email) return "已完善";
  if (userForm.phone || userForm.email) return "基础保护";
  return "待完善";
});
const maskedPhone = computed(() => {
  return maskPhone(userForm.phone, "未绑定手机号");
});
const maskedEmail = computed(() => {
  return maskEmail(userForm.email, "绑定后可用于接收验证码、安全提醒与重要通知");
});

// 头像地址
const avatarUrl = computed(() => {
  if (!userForm.avatar) return "";
  return userForm.avatar.startsWith("http") ? userForm.avatar : baseAPI + userForm.avatar;
});

// 密码表单数据
const passwordForm = reactive({
  oldPassword: "",
  newPassword: "",
  confirmPassword: "",
  currentPhoneCode: "",
});

// 表单校验规则
const rules = {
  nickname: [
    { required: true, message: "请输入昵称", trigger: "blur" },
    { min: 2, max: 30, message: "昵称长度在2到30个字符之间", trigger: "blur" },
  ],
  sex: [{ required: true, message: "请选择性别", trigger: "change" }],
};

const emailBindRules = {
  email: [
    { required: true, message: "请输入邮箱地址", trigger: "blur" },
    { type: "email", message: "请输入正确的邮箱地址", trigger: ["blur", "change"] },
  ],
  code: [
    { required: true, message: "请输入邮箱验证码", trigger: "blur" },
    { pattern: /^\d{6}$/, message: "验证码为6位数字", trigger: "blur" },
  ],
};

const phoneBindRules = {
  phone: [
    { required: true, message: "请输入手机号码", trigger: "blur" },
    { pattern: /^1[3-9]\d{9}$/, message: "请输入正确的手机号码", trigger: ["blur", "change"] },
  ],
  smsCode: [
    { required: true, message: "请输入短信验证码", trigger: "blur" },
    { pattern: /^\d{6}$/, message: "验证码为6位数字", trigger: "blur" },
  ],
};

// 密码表单校验规则
const passwordRules = {
  oldPassword: [
    { required: true, message: "请输入旧密码", trigger: "blur" },
    { min: 6, message: "密码长度不能小于6个字符", trigger: "blur" },
  ],
  newPassword: [
    { required: true, message: "请输入新密码", trigger: "blur" },
    { min: 6, message: "密码长度不能小于6个字符", trigger: "blur" },
  ],
  confirmPassword: [
    { required: true, message: "请再次输入新密码", trigger: "blur" },
    {
      validator: (rule, value, callback) => {
        if (value !== passwordForm.newPassword) {
          callback(new Error("两次输入的密码不一致"));
        } else {
          callback();
        }
      },
      trigger: ["blur", "change"],
    },
  ],
  currentPhoneCode: [
    { required: true, message: "请输入当前手机验证码", trigger: "blur" },
    { pattern: /^\d{6}$/, message: "验证码为6位数字", trigger: "blur" },
  ],
};

const validateCnIdCard = (value) => {
  if (!/^\d{17}[\dXx]$/.test(value)) return false
  const weights = [7, 9, 10, 5, 8, 4, 2, 1, 6, 3, 7, 9, 10, 5, 8, 4, 2]
  const checks = ['1', '0', 'X', '9', '8', '7', '6', '5', '4', '3', '2']
  const sum = value.slice(0, 17).split('').reduce((total, num, index) => total + Number(num) * weights[index], 0)
  return checks[sum % 11] === value.charAt(17).toUpperCase()
}

const validateTravelerIdNumber = (rule, value, callback) => {
  const idType = travelerForm.idType
  const normalized = String(value || '').trim()
  if (!normalized) {
    callback(new Error('请输入证件号码'))
    return
  }
  if (idType === 'ID_CARD' && !validateCnIdCard(normalized)) {
    callback(new Error('请输入有效的 18 位身份证号码'))
    return
  }
  if (idType === 'PASSPORT' && !/^[A-Za-z0-9]{5,20}$/.test(normalized)) {
    callback(new Error('护照号码需为 5-20 位字母或数字'))
    return
  }
  callback()
}

const travelerRules = {
  name: [
    { required: true, message: '请输入出行人姓名', trigger: 'blur' },
    { min: 2, max: 20, message: '姓名长度为 2-20 个字符', trigger: 'blur' }
  ],
  phone: [
    { required: true, message: '请输入手机号码', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的 11 位手机号码', trigger: ['blur', 'change'] }
  ],
  gender: [{ required: true, message: '请选择性别', trigger: 'change' }],
  travelerType: [{ required: true, message: '请选择出行人类型', trigger: 'change' }],
  idType: [{ required: true, message: '请选择证件类型', trigger: 'change' }],
  idNumber: [
    { required: true, message: '请输入证件号码', trigger: 'blur' },
    { validator: validateTravelerIdNumber, trigger: ['blur', 'change'] }
  ],
  birthDate: [{ required: true, message: '请选择出生日期', trigger: 'change' }]
};

// 获取用户信息
const getUserInfo = async () => {
  try {
    // 如果用户已登录，从 store 中获取用户信息
    if (userStore.isLoggedIn) {
      // 从后端重新获取最新的用户信息
      const response = await request.get("/user/current", null, {
        showDefaultMsg: false,
      });

      // 确保返回数据存在
      if (response) {
        // 更新store中的用户信息
        userStore.updateUserInfo(response);

        // 直接更新表单数据
        userForm.id = response.id || "";
        userForm.username = response.username || "";
        userForm.nickname = response.nickname || "";
        userForm.email = response.email || "";
        userForm.phone = response.phone || "";
        userForm.sex = response.sex || "男";
        userForm.avatar = response.avatar || "";

        console.log("用户信息加载成功:", userForm);
      }
    }
  } catch (error) {
    console.error("获取用户信息失败", error);
    ElMessage.error("获取用户信息失败");
  }
};

// 上传头像前的校验
const beforeAvatarUpload = (file) => {
  const isJPG = file.type === "image/jpeg";
  const isPNG = file.type === "image/png";
  const isLt2M = file.size / 1024 / 1024 < 2;

  if (!isJPG && !isPNG) {
    ElMessage.error("头像只能是 JPG 或 PNG 格式!");
    return false;
  }
  if (!isLt2M) {
    ElMessage.error("头像大小不能超过 2MB!");
    return false;
  }
  return true;
};

// 自定义头像上传方法
const customUploadAvatar = async (options) => {
  try {
    const { file } = options;

    // 创建 FormData 对象
    const formData = new FormData();
    formData.append("file", file);

    // 设置自定义上传选项
    const uploadOptions = {
      headers: {
        token: localStorage.getItem("token") || "",
      },
      // 不进行JSON处理
      transformRequest: [(data) => data],
      // 自定义成功消息
      successMsg: "头像上传成功",
      // 自定义错误消息
      errorMsg: "头像上传失败",
      // 成功回调
      onSuccess: async (data) => {
        // 更新用户头像
        userForm.avatar = data;

        // 保存到后端
        await updateUserAvatar(data);

        // 通知上传成功
        options.onSuccess({ data });
      },
      // 错误回调
      onError: (error) => {
        console.error("头像上传错误:", error);
        options.onError(new Error(error.message || "上传失败"));
      },
    };

    // 发送上传请求
    await request.post("/file/upload/img", formData, uploadOptions);
  } catch (error) {
    options.onError(error);
    console.error("头像上传过程发生错误:", error);
  }
};

// 更新用户头像信息
const updateUserAvatar = async (avatarPath) => {
  try {
    await request.put(
      "/user/profile",
      { avatar: avatarPath },
      {
        showDefaultMsg: false,
        onSuccess: () => {
          // 更新本地用户信息
          const updatedUserInfo = { ...userStore.userInfo, avatar: avatarPath };
          userStore.updateUserInfo(updatedUserInfo);
        },
      }
    );
  } catch (error) {
    console.error("头像信息保存失败", error);
    ElMessage.error(error.message || "头像信息保存失败");
    throw error;
  }
};

// 提交用户信息更新
const submitUserInfo = async () => {
  if (!userFormRef.value) return;

  try {
    // 表单验证
    await userFormRef.value.validate();
    savingProfile.value = true;

    await request.put(
      "/user/profile",
      {
        nickname: userForm.nickname,
        sex: userForm.sex,
      },
      {
        showDefaultMsg: false,
        successMsg: "个人信息更新成功!",
        onSuccess: (data) => {
          // 更新本地用户信息
          const updatedUserInfo = {
            ...userStore.userInfo,
            ...(data || {}),
            nickname: userForm.nickname,
            sex: userForm.sex,
          };
          userStore.updateUserInfo(updatedUserInfo);
        },
      }
    );

  } catch (error) {
    console.error("保存个人信息失败", error);
    ElMessage.error(error.message || "保存个人信息失败");
  } finally {
    savingProfile.value = false;
  }
};

const startCountdown = (target) => {
  const countdown = target === "email" ? emailCountdown : target === "phone" ? phoneCountdown : passwordCountdown;
  if (target === "email" && emailTimer) clearInterval(emailTimer);
  if (target === "phone" && phoneTimer) clearInterval(phoneTimer);
  if (target === "password" && passwordTimer) clearInterval(passwordTimer);
  countdown.value = 60;
  const timer = setInterval(() => {
    countdown.value -= 1;
    if (countdown.value <= 0) {
      clearInterval(timer);
    }
  }, 1000);
  if (target === "email") emailTimer = timer;
  if (target === "phone") phoneTimer = timer;
  if (target === "password") passwordTimer = timer;
};

const openEmailDialog = () => {
  emailBindForm.email = userForm.email || "";
  emailBindForm.code = "";
  emailGeetestValidate.value = null;
  emailGeetestRequired.value = true;
  emailDialogVisible.value = true;
};

const openPhoneDialog = () => {
  phoneBindForm.phone = "";
  phoneBindForm.smsCode = "";
  phoneGeetestValidate.value = null;
  phoneGeetestRequired.value = true;
  phoneDialogVisible.value = true;
};

const handleEmailGeetestVerified = (validate) => {
  emailGeetestValidate.value = validate;
  emailGeetestRequired.value = Boolean(validate) || emailGeetestRequired.value;
};

const handleEmailGeetestError = () => {
  emailGeetestValidate.value = null;
};

const handleEmailGeetestUnavailable = () => {
  emailGeetestValidate.value = null;
  emailGeetestRequired.value = false;
};

const handlePhoneGeetestVerified = (validate) => {
  phoneGeetestValidate.value = validate;
  phoneGeetestRequired.value = Boolean(validate) || phoneGeetestRequired.value;
};

const handlePhoneGeetestError = () => {
  phoneGeetestValidate.value = null;
};

const handlePhoneGeetestUnavailable = () => {
  phoneGeetestValidate.value = null;
  phoneGeetestRequired.value = false;
};

const handlePasswordGeetestVerified = (validate) => {
  passwordGeetestValidate.value = validate;
  passwordGeetestRequired.value = Boolean(validate) || passwordGeetestRequired.value;
};

const handlePasswordGeetestError = (message) => {
  passwordGeetestValidate.value = null;
  ElMessage.error(message || "极验验证失败");
};

const handlePasswordGeetestUnavailable = () => {
  passwordGeetestValidate.value = null;
  passwordGeetestRequired.value = false;
};

const sendEmailBindCode = async () => {
  const validEmail = await emailBindFormRef.value.validateField("email").catch(() => false);
  if (validEmail === false) return;
  if (emailGeetestRequired.value && !emailGeetestValidate.value) {
    ElMessage.warning("请先完成极验安全验证");
    return;
  }
  emailCodeSending.value = true;
  try {
    await request.post("/user/email/bind/code", {
      email: emailBindForm.email,
      geetest: emailGeetestValidate.value
    }, { showDefaultMsg: false });
    ElMessage.success("邮箱验证码已发送");
    startCountdown("email");
    emailGeetestValidate.value = null;
    emailCaptchaRef.value?.resetCaptcha();
  } catch (error) {
    ElMessage.error(error.message || "邮箱验证码发送失败");
    emailGeetestValidate.value = null;
    emailCaptchaRef.value?.resetCaptcha();
  } finally {
    emailCodeSending.value = false;
  }
};

const confirmEmailBind = async () => {
  const valid = await emailBindFormRef.value.validate().catch(() => false);
  if (!valid) return;
  emailBinding.value = true;
  try {
    await request.post("/user/email/bind/confirm", { ...emailBindForm }, { showDefaultMsg: false });
    ElMessage.success("邮箱绑定成功");
    emailDialogVisible.value = false;
    await getUserInfo();
  } catch (error) {
    ElMessage.error(error.message || "邮箱绑定失败");
  } finally {
    emailBinding.value = false;
  }
};

const sendPhoneChangeCode = async () => {
  const validPhone = await phoneBindFormRef.value.validateField("phone").catch(() => false);
  if (validPhone === false) return;
  if (phoneGeetestRequired.value && !phoneGeetestValidate.value) {
    ElMessage.warning("请先完成极验安全验证");
    return;
  }
  phoneCodeSending.value = true;
  try {
    await request.post("/user/phone/change/code", {
      phone: phoneBindForm.phone,
      geetest: phoneGeetestValidate.value
    }, { showDefaultMsg: false });
    ElMessage.success("短信验证码已发送");
    startCountdown("phone");
    phoneGeetestValidate.value = null;
    phoneCaptchaRef.value?.resetCaptcha();
  } catch (error) {
    ElMessage.error(error.message || "短信验证码发送失败");
    phoneGeetestValidate.value = null;
    phoneCaptchaRef.value?.resetCaptcha();
  } finally {
    phoneCodeSending.value = false;
  }
};

const confirmPhoneChange = async () => {
  const valid = await phoneBindFormRef.value.validate().catch(() => false);
  if (!valid) return;
  phoneBinding.value = true;
  try {
    await request.post("/user/phone/change/confirm", { ...phoneBindForm }, { showDefaultMsg: false });
    ElMessage.success("手机号变更成功，请使用新手机号登录");
    phoneDialogVisible.value = false;
    await getUserInfo();
  } catch (error) {
    ElMessage.error(error.message || "手机号变更失败");
  } finally {
    phoneBinding.value = false;
  }
};

const sendPasswordCode = async () => {
  if (!userForm.phone) {
    ElMessage.warning("请先绑定手机号后再修改密码");
    return;
  }
  if (passwordGeetestRequired.value && !passwordGeetestValidate.value) {
    ElMessage.warning("请先完成极验安全验证");
    return;
  }
  passwordCodeSending.value = true;
  try {
    await request.post("/user/phone/current/code", {
      geetest: passwordGeetestValidate.value
    }, { showDefaultMsg: false });
    ElMessage.success("手机验证码已发送");
    startCountdown("password");
    passwordGeetestValidate.value = null;
    passwordCaptchaRef.value?.resetCaptcha();
  } catch (error) {
    ElMessage.error(error.message || "验证码发送失败");
    passwordGeetestValidate.value = null;
    passwordCaptchaRef.value?.resetCaptcha();
  } finally {
    passwordCodeSending.value = false;
  }
};

// 提交密码修改
const submitPassword = async () => {
  if (!passwordFormRef.value) return;
  if (!userForm.phone) {
    ElMessage.warning("请先绑定手机号后再修改密码");
    return;
  }

  try {
    // 表单验证
    await passwordFormRef.value.validate();

    await request.put(
      `/user/password/${userForm.id}`,
      {
        oldPassword: passwordForm.oldPassword,
        newPassword: passwordForm.newPassword,
        currentPhoneCode: passwordForm.currentPhoneCode,
      },
      {
        showDefaultMsg: false,

        onSuccess: () => {
          // 清空表单
          passwordForm.oldPassword = "";
          passwordForm.newPassword = "";
          passwordForm.confirmPassword = "";
          passwordForm.currentPhoneCode = "";

          // 提示用户重新登录
          ElMessageBox.confirm("密码已修改，需要重新登录", "提示", {
            confirmButtonText: "重新登录",
            cancelButtonText: "取消",
            type: "warning",
          }).then(() => {
            // 清除用户信息并跳转到登录页
            userStore.clearUserInfo();
            window.location.href = "/login";
          });
        },
      }
    );
  } catch (error) {
    console.error("密码修改失败", error);
    ElMessage.error(error.message || "密码修改失败");
  }
};

// 获取我的评论
const fetchMyComments = async () => {
  myCommentsLoading.value = true
  try {
    const [scenicRes, hotelRes] = await Promise.all([
      request.get('/comment/my', { currentPage: 1, size: 1000 }, { showDefaultMsg: false }),
      request.get('/accommodation/review/my', { currentPage: 1, size: 1000 }, { showDefaultMsg: false })
    ])

    const scenicComments = (scenicRes?.records || []).map(item => ({
      ...item,
      rating: Number(item.rating || 0),
      commentType: 'scenic',
      targetName: item.scenicName || '景区评论'
    }))
    const hotelComments = (hotelRes?.records || []).map(item => ({
      ...item,
      rating: Number(item.rating || 0),
      commentType: 'hotel',
      targetName: item.accommodationName || '酒店评论'
    }))
    const allComments = [...scenicComments, ...hotelComments].sort((a, b) => {
      return new Date(b.createTime || 0).getTime() - new Date(a.createTime || 0).getTime()
    })
    commentTotal.value = allComments.length
    const start = (commentPage.value - 1) * commentPageSize.value
    myComments.value = allComments.slice(start, start + commentPageSize.value)
  } catch (error) {
    console.error('获取我的评论失败', error)
  } finally {
    myCommentsLoading.value = false
  }
}

const fetchNotificationUnreadCount = async () => {
  try {
    notificationUnreadCount.value = await request.get('/notification/unread-count', {}, { showDefaultMsg: false })
  } catch (error) {
    notificationUnreadCount.value = 0
  }
}

const fetchNotifications = async () => {
  notificationLoading.value = true
  try {
    const params = {
      currentPage: notificationPage.value,
      size: notificationPageSize.value
    }
    if (notificationReadStatus.value !== '') {
      params.readStatus = notificationReadStatus.value
    }
    const res = await request.get('/notification/page', params, { showDefaultMsg: false })
    notifications.value = res?.records || []
    notificationTotal.value = res?.total || 0
    fetchNotificationUnreadCount()
  } catch (error) {
    console.error('获取站内消息失败', error)
  } finally {
    notificationLoading.value = false
  }
}

const startNotificationRefresh = () => {
  stopNotificationRefresh()
  notificationRefreshTimer = window.setInterval(() => {
    if (activeTab.value === 'notifications' && !document.hidden) {
      fetchNotifications()
    }
  }, NOTIFICATION_REFRESH_INTERVAL)
}

const stopNotificationRefresh = () => {
  if (notificationRefreshTimer) {
    window.clearInterval(notificationRefreshTimer)
    notificationRefreshTimer = null
  }
}

const handleNotificationVisibilityChange = () => {
  if (!document.hidden && activeTab.value === 'notifications') {
    fetchNotifications()
  }
}

const handleNotificationFilterChange = () => {
  notificationPage.value = 1
  fetchNotifications()
}

const handleNotificationPageChange = (page) => {
  notificationPage.value = page
  fetchNotifications()
}

const markNotificationRead = async (item) => {
  await request.put(`/notification/${item.id}/read`, {}, { showDefaultMsg: false })
  item.readStatus = 1
  fetchNotificationUnreadCount()
}

const markAllNotificationsRead = async () => {
  await request.put('/notification/read-all', {}, { showDefaultMsg: false })
  await fetchNotifications()
}

const openNotification = async (item) => {
  if (item.readStatus === 0) {
    await markNotificationRead(item)
  }
  if (item.linkUrl) {
    router.push(item.linkUrl)
  }
}

const formatTravelerDate = (dateStr) => {
  if (!dateStr) return ''
  return String(dateStr).split('T')[0]
}

const getTravelerIdTypeLabel = (type) => {
  const labels = {
    ID_CARD: '身份证',
    PASSPORT: '护照'
  }
  return labels[type] || '证件'
}

const maskTravelerIdNumber = (idNumber) => {
  if (!idNumber) return '未填写'
  const value = String(idNumber)
  if (value.length <= 8) return `${value.slice(0, 2)}****`
  return `${value.slice(0, 4)}****${value.slice(-4)}`
}

const disableFutureDate = (date) => date.getTime() > Date.now()

const fetchFrequentTravelers = async () => {
  travelerLoading.value = true
  try {
    const data = await getFrequentTravelers()
    frequentTravelers.value = Array.isArray(data) ? data : []
  } catch (error) {
    console.error('获取常用出行人失败', error)
    ElMessage.error(error.message || '获取常用出行人失败')
  } finally {
    travelerLoading.value = false
  }
}

const resetTravelerForm = () => {
  Object.assign(travelerForm, {
    name: '',
    gender: 'MALE',
    travelerType: 'ADULT',
    phone: '',
    idType: 'ID_CARD',
    idNumber: '',
    birthDate: ''
  })
  travelerFormRef.value?.clearValidate?.()
}

const openTravelerDialog = (traveler = null) => {
  editingTraveler.value = traveler
  if (traveler) {
    Object.assign(travelerForm, {
      name: traveler.name || '',
      gender: traveler.gender || 'MALE',
      travelerType: traveler.travelerType || 'ADULT',
      phone: traveler.phone || '',
      idType: traveler.idType || 'ID_CARD',
      idNumber: traveler.idNumber || '',
      birthDate: formatTravelerDate(traveler.birthDate)
    })
  } else {
    resetTravelerForm()
  }
  travelerDialogVisible.value = true
}

const saveTravelerProfile = async () => {
  try {
    await travelerFormRef.value?.validate()
  } catch {
    return
  }

  travelerSaving.value = true
  try {
    const payload = { ...travelerForm }
    if (editingTraveler.value) {
      await updateFrequentTraveler(editingTraveler.value.id, payload)
      ElMessage.success('出行人信息已更新')
    } else {
      await saveFrequentTraveler(payload)
      ElMessage.success('出行人已添加')
    }
    travelerDialogVisible.value = false
    await fetchFrequentTravelers()
  } catch (error) {
    console.error('保存常用出行人失败', error)
    ElMessage.error(error.message || '保存常用出行人失败')
  } finally {
    travelerSaving.value = false
  }
}

const removeTraveler = async (traveler) => {
  try {
    await ElMessageBox.confirm(`确定要删除出行人“${traveler.name}”吗？`, '删除确认', {
      confirmButtonText: '确认删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await deleteFrequentTraveler(traveler.id)
    ElMessage.success('出行人已删除')
    await fetchFrequentTravelers()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除常用出行人失败', error)
      ElMessage.error(error.message || '删除常用出行人失败')
    }
  }
}

// 监听标签页切换
watch(activeTab, (newTab) => {
  if (newTab === 'mycomments') {
    fetchMyComments()
  } else if (newTab === 'travelers') {
    fetchFrequentTravelers()
  } else if (newTab === 'notifications') {
    fetchNotifications()
    startNotificationRefresh()
  } else {
    stopNotificationRefresh()
  }
  if (route.query.tab !== newTab) {
    router.replace({ query: { ...route.query, tab: newTab } })
  }
})

watch(() => route.query.tab, (tab) => {
  const validTabs = ['basic', 'password', 'travelers', 'mycomments', 'notifications']
  if (validTabs.includes(tab) && activeTab.value !== tab) {
    activeTab.value = tab
  }
}, { immediate: true })

watch(() => travelerForm.idType, () => {
  travelerFormRef.value?.validateField?.('idNumber').catch?.(() => {})
})

// 评论分页变化
const handleCommentPageChange = (page) => {
  commentPage.value = page
  fetchMyComments()
}

// 获取审核状态文本
const getReviewStatusText = (status) => {
  switch (status) {
    case 0: return '待审核'
    case 1: return '已通过'
    case 2: return '已拒绝'
    default: return '未知'
  }
}

// 获取审核状态标签类型
const getReviewStatusType = (status) => {
  switch (status) {
    case 0: return 'warning'
    case 1: return 'success'
    case 2: return 'danger'
    default: return 'info'
  }
}

// 删除我的评论
const deleteMyComment = async (comment) => {
  try {
    const url = comment.commentType === 'hotel'
      ? `/accommodation/review/${comment.id}`
      : `/comment/delete/${comment.id}`
    await request.delete(url, {
      successMsg: '删除成功',
      onSuccess: () => fetchMyComments()
    })
  } catch (error) {
    console.error('删除评论失败', error)
  }
}

// 组件挂载时获取用户信息
onMounted(() => {
  getUserInfo();
  if (activeTab.value === 'notifications') {
    fetchNotifications()
    startNotificationRefresh()
  } else if (activeTab.value === 'travelers') {
    fetchFrequentTravelers()
  }
  document.addEventListener('visibilitychange', handleNotificationVisibilityChange)
})

onUnmounted(() => {
  stopNotificationRefresh()
  document.removeEventListener('visibilitychange', handleNotificationVisibilityChange)
  if (emailTimer) clearInterval(emailTimer);
  if (phoneTimer) clearInterval(phoneTimer);
  if (passwordTimer) clearInterval(passwordTimer);
})
</script>

<style lang="scss" scoped>
.profile-container {
  min-height: 100vh;
  background:
    radial-gradient(circle at 12% 0%, rgba(14, 165, 233, 0.08), transparent 28%),
    linear-gradient(180deg, #f8fafc 0%, #ffffff 42%);
  font-family: "思源黑体", "Source Han Sans", "Noto Sans CJK SC", sans-serif;
  color: #111827;

  // 通用容器样式
  .section-container {
    width: min(var(--frontend-container-safe-width), var(--frontend-container-page));
    margin: 0 auto;
    padding: 40px 0;
  }

  // 页面头部容器
  .page-header-wrapper {
    width: min(var(--frontend-container-safe-width), var(--frontend-container-page));
    margin: 0 auto;
    padding: 34px 0 0;
  }

  // 页面头部
  .page-header {
    display: grid;
    grid-template-columns: 1fr auto 1fr;
    justify-content: space-between;
    align-items: center;
    gap: 24px;
    padding: 30px;
    border: 1px solid rgba(203, 213, 225, 0.85);
    border-radius: 8px;
    background: rgba(255, 255, 255, 0.88);
    box-shadow: 0 18px 45px rgba(15, 23, 42, 0.08);
    backdrop-filter: blur(12px);
  }

  .header-content {
    grid-column: 2;
    text-align: center;
  }

  .eyebrow {
    margin-bottom: 8px;
    color: #0f766e;
    font-size: 13px;
    font-weight: 700;
    letter-spacing: 0;
  }

  .page-title {
    font-size: 34px;
    font-weight: 700;
    margin: 0 0 10px;
    color: #0f172a;
  }

  .page-subtitle {
    font-size: 16px;
    color: #64748b;
    margin: 0;
  }

  .header-summary {
    grid-column: 3;
    justify-self: end;
    min-width: 150px;
    border: 1px solid #d1fae5;
    border-radius: 999px;
    padding: 10px 16px;
    background: #ecfdf5;
    text-align: center;

    span,
    strong {
      display: block;
    }

    span {
      color: #047857;
      font-size: 12px;
      font-weight: 700;
    }

    strong {
      margin-top: 2px;
      color: #065f46;
      font-size: 16px;
    }
  }

  // 个人资料区域
  .profile-section {
    background: transparent;
    margin: 0;
    padding-top: 10px;
  }

  .profile-tabs {
    background: white;
    border-radius: 8px;
    box-shadow: 0 16px 48px rgba(15, 23, 42, 0.08);
    overflow: hidden;
    border: 1px solid #e5e7eb;
  }

  // 现代化标签页样式
  .modern-tabs {
    :deep(.el-tabs__header) {
      margin: 0;
      background: #fff;
      border-bottom: 1px solid #e2e8f0;
    }

    :deep(.el-tabs__nav-wrap) {
      padding: 0 24px;
    }

    :deep(.el-tabs__item) {
      padding: 20px 0;
      font-size: 16px;
      font-weight: 600;
      color: #64748b;
      border: none;
      margin-right: 40px;

      &.is-active {
        color: #0f766e;
      }

      &:hover {
        color: #0f766e;
      }
    }

    :deep(.el-tabs__active-bar) {
      background: #0f766e;
      height: 3px;
    }

    :deep(.el-tabs__content) {
      padding: 40px 24px;
    }

    .tab-label {
      display: flex;
      align-items: center;
      gap: 8px;
    }
  }

  // 个人资料内容
  .profile-content {
    .profile-layout {
      display: grid;
      grid-template-columns: 1fr 2fr;
      gap: 40px;
      align-items: start;
    }

    .account-panel {
      display: grid;
      grid-template-columns: 320px minmax(0, 1fr);
      gap: 24px;
      align-items: start;
    }

    .identity-card,
    .details-card {
      border: 1px solid #e5e7eb;
      border-radius: 8px;
      background: #fff;
      box-shadow: 0 14px 36px rgba(15, 23, 42, 0.07);
    }

    .identity-card {
      padding: 28px;
      text-align: center;
      position: sticky;
      top: 24px;

      .avatar-wrapper {
        position: relative;
        width: 112px;
        margin: 0 auto 18px;
      }

      .user-avatar {
        background: linear-gradient(135deg, #0f766e 0%, #2563eb 100%);
        color: #fff;
        font-size: 42px;
        font-weight: 700;
        box-shadow: 0 14px 30px rgba(37, 99, 235, 0.24);
      }

      .upload-btn {
        position: absolute;
        right: -4px;
        bottom: -4px;
        border: 3px solid #fff;
        box-shadow: 0 8px 20px rgba(15, 118, 110, 0.25);
      }

      h3 {
        margin: 0;
        color: #111827;
        font-size: 22px;
      }

      p {
        margin: 8px 0 0;
        color: #64748b;
      }
    }

    .identity-stats {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 12px;
      margin-top: 24px;

      div {
        border: 1px solid #e5e7eb;
        border-radius: 8px;
        padding: 14px 10px;
        background: #f8fafc;
      }

      strong,
      span {
        display: block;
      }

      strong {
        color: #0f766e;
        font-size: 16px;
      }

      span {
        margin-top: 4px;
        color: #64748b;
        font-size: 13px;
      }
    }

    .details-card {
      padding: 30px;
    }

    .card-heading {
      display: flex;
      justify-content: space-between;
      gap: 16px;
      margin-bottom: 24px;

      h3 {
        margin: 0;
        color: #111827;
        font-size: 22px;
      }

      p {
        margin: 8px 0 0;
        color: #64748b;
        line-height: 1.6;
      }
    }

    .field-tip {
      margin-top: 6px;
      color: #94a3b8;
      font-size: 12px;
      line-height: 1.5;
    }

    :deep(.el-input__wrapper) {
      min-height: 42px;
      border-radius: 8px;
      box-shadow: 0 0 0 1px #e5e7eb inset;
      transition: box-shadow 0.2s ease, background 0.2s ease;

      &:hover {
        box-shadow: 0 0 0 1px #99f6e4 inset;
      }

      &.is-focus {
        box-shadow: 0 0 0 1px #0f766e inset, 0 0 0 4px rgba(15, 118, 110, 0.12);
      }
    }

    :deep(.el-segmented) {
      --el-segmented-item-selected-color: #fff;
      --el-segmented-item-selected-bg-color: #0f766e;
      border-radius: 8px;
      padding: 4px;
      background: #f1f5f9;
    }

    .profile-form {
      padding-bottom: 22px;
      border-bottom: 1px solid #eef2f7;
      margin-bottom: 22px;
    }

    .form-grid {
      display: grid;
      grid-template-columns: repeat(2, minmax(0, 1fr));
      gap: 18px;
    }

    .form-actions.compact {
      display: flex;
      justify-content: flex-end;
      margin-top: 4px;
    }

    .security-list {
      display: flex;
      flex-direction: column;
      gap: 14px;
    }

    .security-row {
      display: grid;
      grid-template-columns: 44px minmax(0, 1fr) auto auto;
      gap: 14px;
      align-items: center;
      border: 1px solid #e5e7eb;
      border-radius: 8px;
      padding: 18px;
      background: linear-gradient(180deg, #ffffff 0%, #fbfdff 100%);
      transition: border-color 0.2s ease, box-shadow 0.2s ease, transform 0.2s ease;

      &:hover {
        border-color: #bae6fd;
        box-shadow: 0 12px 28px rgba(15, 23, 42, 0.08);
        transform: translateY(-1px);
      }
    }

    .security-icon {
      width: 44px;
      height: 44px;
      border-radius: 8px;
      display: grid;
      place-items: center;
      font-size: 20px;

      &.email {
        background: #ecfeff;
        color: #0891b2;
      }

      &.phone {
        background: #f0fdf4;
        color: #16a34a;
      }
    }

    .security-main {
      min-width: 0;

      h4 {
        margin: 0 0 6px;
        color: #111827;
        font-size: 15px;
      }

      p {
        margin: 0;
        color: #64748b;
        word-break: break-all;
      }
    }
  }

  // 头像区域
  .profile-redesign {
    display: flex;
    flex-direction: column;
    gap: 22px;

    .account-overview {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 24px;
      padding: 28px;
      border: 1px solid #e5e7eb;
      border-radius: 8px;
      background: linear-gradient(180deg, #ffffff 0%, #f8fafc 100%);
      box-shadow: 0 18px 42px rgba(15, 23, 42, 0.07);
    }

    .account-person {
      display: flex;
      align-items: center;
      gap: 20px;
      min-width: 0;
    }

    .avatar-frame {
      position: relative;
      width: 104px;
      height: 104px;
      display: grid;
      place-items: center;
      flex: 0 0 auto;
      border-radius: 8px;
      background: #eef6f4;
      border: 1px solid #dbeafe;
    }

    .profile-avatar {
      background: #0f766e;
      color: #fff;
      font-size: 34px;
      font-weight: 700;
      box-shadow: 0 12px 28px rgba(15, 118, 110, 0.22);
    }

    .avatar-action {
      position: absolute;
      right: 4px;
      bottom: 4px;
      width: 34px;
      height: 34px;
      display: grid;
      place-items: center;
      border: 1px solid #ccfbf1;
      border-radius: 8px;
      background: #ffffff;
      color: #0f766e;
      cursor: pointer;
      box-shadow: 0 8px 20px rgba(15, 23, 42, 0.12);
      transition: transform 0.18s ease, border-color 0.18s ease, color 0.18s ease;

      &:hover {
        color: #0d9488;
        border-color: #99f6e4;
        transform: translateY(-1px);
      }
    }

    .person-copy {
      min-width: 0;

      h2 {
        margin: 8px 0 6px;
        color: #0f172a;
        font-size: 28px;
        line-height: 1.2;
      }

      p {
        margin: 0;
        color: #64748b;
        font-size: 14px;
      }
    }

    .status-pill {
      display: inline-flex;
      align-items: center;
      height: 24px;
      padding: 0 10px;
      border-radius: 999px;
      background: #ecfdf5;
      color: #047857;
      font-size: 12px;
      font-weight: 700;
    }

    .overview-metrics {
      display: grid;
      grid-template-columns: repeat(3, 118px);
      gap: 12px;
    }

    .metric-item {
      min-height: 76px;
      padding: 14px;
      border: 1px solid #e5e7eb;
      border-radius: 8px;
      background: #ffffff;

      span,
      strong {
        display: block;
      }

      span {
        color: #94a3b8;
        font-size: 12px;
      }

      strong {
        margin-top: 8px;
        color: #0f172a;
        font-size: 16px;
      }
    }

    .profile-grid {
      display: grid;
      grid-template-columns: minmax(0, 1.05fr) minmax(360px, 0.95fr);
      gap: 22px;
      align-items: start;
    }

    .profile-card {
      border: 1px solid #e5e7eb;
      border-radius: 8px;
      background: #fff;
      box-shadow: 0 14px 34px rgba(15, 23, 42, 0.06);
    }

    .profile-editor,
    .security-panel {
      padding: 26px;
    }

    .section-heading {
      display: flex;
      justify-content: space-between;
      gap: 20px;
      margin-bottom: 22px;
      padding-bottom: 18px;
      border-bottom: 1px solid #eef2f7;

      span {
        display: block;
        margin-bottom: 6px;
        color: #0f766e;
        font-size: 12px;
        font-weight: 800;
      }

      h3 {
        margin: 0;
        color: #0f172a;
        font-size: 22px;
      }

      p {
        max-width: 380px;
        margin: 0;
        color: #64748b;
        line-height: 1.7;
        font-size: 13px;
      }
    }

    .clean-form {
      :deep(.el-form-item) {
        margin-bottom: 18px;
      }

      :deep(.el-form-item__label) {
        margin-bottom: 8px;
        color: #475569;
        font-size: 13px;
        font-weight: 700;
        line-height: 1.2;
      }

      :deep(.el-input__wrapper) {
        min-height: 44px;
        border-radius: 8px;
        background: #fff;
        box-shadow: 0 0 0 1px #dfe7ef inset;

        &:hover {
          box-shadow: 0 0 0 1px #99f6e4 inset;
        }

        &.is-focus {
          box-shadow: 0 0 0 1px #0f766e inset, 0 0 0 4px rgba(15, 118, 110, 0.1);
        }
      }

      :deep(.is-disabled .el-input__wrapper) {
        background: #f8fafc;
        box-shadow: 0 0 0 1px #e5e7eb inset;
      }

      :deep(.el-segmented) {
        --el-segmented-item-selected-bg-color: #0d9488;
        --el-segmented-item-selected-color: #fff;
        width: 100%;
        min-height: 44px;
        max-width: 100%;
        border-radius: 8px;
        background: #ecfdf5;
        padding: 4px;
      }

      :deep(.el-segmented__item) {
        min-height: 36px;
        border-radius: 7px;
        font-weight: 800;
      }
    }

    .field-stack {
      display: grid;
      grid-template-columns: repeat(2, minmax(0, 1fr));
      gap: 2px 18px;

      .quiet-field {
        grid-column: 1 / -1;
      }
    }

    .editor-actions {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 16px;
      margin-top: 6px;
      padding-top: 18px;
      border-top: 1px solid #eef2f7;

      span {
        color: #94a3b8;
        font-size: 13px;
      }

      .el-button {
        min-width: 118px;
        border-radius: 8px;
        font-weight: 700;
      }
    }

    .binding-list {
      display: flex;
      flex-direction: column;
      gap: 14px;
    }

    .binding-card {
      display: grid;
      grid-template-columns: 48px minmax(0, 1fr) auto;
      align-items: center;
      gap: 14px;
      padding: 16px;
      border: 1px solid #e5e7eb;
      border-radius: 8px;
      background: #fbfdff;
      transition: border-color 0.18s ease, box-shadow 0.18s ease, transform 0.18s ease;

      &:hover {
        border-color: #bae6fd;
        box-shadow: 0 12px 28px rgba(15, 23, 42, 0.07);
        transform: translateY(-1px);
      }

      .el-button {
        min-width: 74px;
        border-radius: 8px;
        font-weight: 700;
      }
    }

    .binding-icon {
      width: 48px;
      height: 48px;
      display: grid;
      place-items: center;
      border-radius: 8px;
      font-size: 21px;

      &.mail {
        background: #ecfeff;
        color: #0891b2;
      }

      &.mobile {
        background: #ecfdf5;
        color: #059669;
      }
    }

    .binding-title {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 10px;
      margin-bottom: 6px;

      h4 {
        margin: 0;
        color: #0f172a;
        font-size: 15px;
      }

      span {
        flex: 0 0 auto;
        padding: 3px 8px;
        border-radius: 999px;
        background: #f1f5f9;
        color: #64748b;
        font-size: 12px;
        font-weight: 700;

        &.verified {
          background: #ecfdf5;
          color: #047857;
        }
      }
    }

    .binding-body {
      min-width: 0;

      p {
        margin: 0;
        color: #64748b;
        font-size: 13px;
        line-height: 1.6;
        word-break: break-all;
      }
    }
  }

  .avatar-section {
    .avatar-card {
      background: white;
      border-radius: 16px;
      padding: 30px;
      text-align: center;
      box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
      border: 1px solid #e2e8f0;
    }

    .avatar-wrapper {
      position: relative;
      display: inline-block;
      margin-bottom: 20px;

      .user-avatar {
        border: 4px solid #fff;
        box-shadow: 0 8px 24px rgba(0, 0, 0, 0.15);
        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
        color: white;
        font-size: 48px;
        font-weight: 700;
      }

      .avatar-overlay {
        position: absolute;
        bottom: -5px;
        right: -5px;

        .upload-btn {
          background: linear-gradient(45deg, #667eea, #764ba2);
          border: 3px solid white;
          box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);

          &:hover {
            transform: scale(1.1);
          }
        }
      }
    }

    .avatar-info {
      .user-name {
        font-size: 20px;
        font-weight: 700;
        color: #2d3748;
        margin: 0 0 8px;
      }

      .user-desc {
        font-size: 14px;
        color: #64748b;
        margin: 0;
      }
    }
  }

  // 表单区域
  .form-section {
    .form-card {
      background: white;
      border-radius: 16px;
      box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
      border: 1px solid #e2e8f0;
      overflow: hidden;
    }

    .form-header {
      padding: 24px 24px 0;
      border-bottom: 1px solid #f1f5f9;
      margin-bottom: 24px;

      .form-title {
        font-size: 20px;
        font-weight: 700;
        color: #2d3748;
        margin: 0;
        display: flex;
        align-items: center;
        gap: 8px;

        .el-icon {
          color: #667eea;
        }
      }
    }

    .modern-form {
      padding: 0 24px 24px;

      .form-row {
        display: grid;
        grid-template-columns: 1fr 1fr;
        gap: 20px;
        margin-bottom: 20px;
      }

      .form-item {
        &.full-width {
          grid-column: 1 / -1;
        }

        :deep(.el-form-item__label) {
          font-weight: 600;
          color: #2d3748;
          margin-bottom: 8px;
          font-size: 14px;
        }

        .form-input {
          :deep(.el-input__wrapper) {
            border-radius: 8px;
            border: 2px solid #e2e8f0;
            transition: all 0.3s ease;
            box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);

            &:hover {
              border-color: #667eea;
            }

            &.is-focus {
              border-color: #667eea;
              box-shadow: 0 4px 12px rgba(102, 126, 234, 0.15);
            }
          }
        }

        .gender-group {
          display: flex;
          gap: 20px;

          .gender-radio {
            :deep(.el-radio__label) {
              display: flex;
              align-items: center;
              gap: 4px;
              font-weight: 500;
            }
          }
        }
      }

      .form-actions {
        display: flex;
        justify-content: center;
        margin-top: 32px;

        .save-btn {
          background: linear-gradient(45deg, #667eea, #764ba2);
          border: none;
          border-radius: 12px;
          font-weight: 600;
          padding: 12px 32px;
          box-shadow: 0 4px 15px rgba(102, 126, 234, 0.4);
          transition: all 0.3s ease;

          &:hover {
            transform: translateY(-2px);
            box-shadow: 0 6px 20px rgba(102, 126, 234, 0.6);
          }
        }
      }
    }
  }

  // 密码修改内容
  .password-content {
    max-width: 980px;
    margin: 0 auto;

    .password-card {
      background: linear-gradient(180deg, #ffffff 0%, #f8fafc 100%);
      border-radius: 8px;
      box-shadow: 0 18px 46px rgba(15, 23, 42, 0.08);
      border: 1px solid #e2e8f0;
      overflow: hidden;
    }

    .password-header {
      display: flex;
      align-items: center;
      gap: 16px;
      padding: 28px 30px;
      background:
        linear-gradient(135deg, rgba(14, 165, 233, 0.11), rgba(16, 185, 129, 0.08)),
        #ffffff;
      border-bottom: 1px solid #e2e8f0;

      .password-icon {
        width: 56px;
        height: 56px;
        display: grid;
        place-items: center;
        border-radius: 8px;
        background: #0f766e;
        color: #ffffff;
        font-size: 26px;
        box-shadow: 0 14px 30px rgba(15, 118, 110, 0.24);
      }

      span {
        display: block;
        margin-bottom: 6px;
        color: #0f766e;
        font-size: 12px;
        font-weight: 800;
      }

      .password-title {
        font-size: 26px;
        font-weight: 700;
        color: #0f172a;
        margin: 0 0 8px;
      }

      .password-desc {
        font-size: 14px;
        color: #64748b;
        margin: 0;
      }
    }

    .password-form {
      padding: 30px;

      .form-actions {
        display: flex;
        align-items: center;
        justify-content: space-between;
        gap: 16px;
        margin-top: 24px;
        padding-top: 22px;
        border-top: 1px solid #e2e8f0;

        span {
          color: #64748b;
          font-size: 13px;
        }

        .save-btn {
          min-width: 150px;
          height: 46px;
          background: #0f766e;
          border: none;
          border-radius: 12px;
          font-weight: 700;
          box-shadow: 0 12px 24px rgba(15, 118, 110, 0.2);
          transition: all 0.3s ease;

          &:hover {
            transform: translateY(-2px);
            box-shadow: 0 16px 30px rgba(15, 118, 110, 0.28);
          }
        }
      }
    }

    .password-grid {
      display: grid;
      grid-template-columns: minmax(0, 1fr) 340px;
      gap: 24px;
      align-items: start;
    }

    .password-verify {
      display: flex;
      flex-direction: column;
      gap: 14px;
    }

    .verify-card {
      padding: 18px;
      border: 1px solid #d1fae5;
      border-radius: 8px;
      background: #ecfdf5;

      span,
      strong {
        display: block;
      }

      span {
        color: #047857;
        font-size: 12px;
        font-weight: 800;
      }

      strong {
        margin-top: 8px;
        color: #064e3b;
        font-size: 20px;
      }

      p {
        margin: 10px 0 0;
        color: #047857;
        font-size: 13px;
        line-height: 1.7;
      }
    }
  }

  // 我的评论样式
  .my-comments-content {
    .loading-state {
      padding: 20px;
    }

    .empty-state {
      text-align: center;
      padding: 60px 20px;

      .empty-icon {
        font-size: 48px;
        margin-bottom: 16px;
      }

      .empty-title {
        font-size: 18px;
        font-weight: 600;
        color: #2d3748;
        margin: 0 0 8px;
      }

      .empty-desc {
        font-size: 14px;
        color: #64748b;
        margin: 0;
      }
    }

    .comment-list {
      display: flex;
      flex-direction: column;
      gap: 16px;
    }

    .comment-card {
      background: #f8fafc;
      border-radius: 12px;
      padding: 16px;
      border: 1px solid #e2e8f0;

      .comment-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 12px;

        .scenic-info {
          display: flex;
          align-items: center;
          gap: 6px;
          font-size: 14px;
          color: #667eea;
          font-weight: 600;

          .el-icon {
            font-size: 16px;
          }
        }

        .comment-tags {
          display: flex;
          align-items: center;
          gap: 8px;
          flex-shrink: 0;
        }
      }

      .comment-body {
        .comment-rating {
          margin-bottom: 8px;
        }

        .comment-content {
          margin: 0;
          font-size: 14px;
          color: #2d3748;
          line-height: 1.6;
        }
      }

      .comment-footer {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-top: 12px;

        .comment-time {
          font-size: 12px;
          color: #64748b;
        }
      }
    }

    .pagination-wrapper {
      display: flex;
      justify-content: center;
      margin-top: 24px;
    }
  }

  .notifications-content {
    padding: 24px;

    .notification-toolbar {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 16px;
      margin-bottom: 18px;
    }

    .notification-list {
      display: flex;
      flex-direction: column;
      gap: 12px;
      max-height: 640px;
      overflow-y: auto;
      padding-right: 6px;
    }

    .notification-card {
      display: flex;
      justify-content: space-between;
      gap: 20px;
      padding: 16px 18px;
      border: 1px solid #e2e8f0;
      border-radius: 12px;
      background: #fff;
      cursor: pointer;
      transition: border-color 0.2s ease, box-shadow 0.2s ease;

      &:hover {
        border-color: #93c5fd;
        box-shadow: 0 8px 24px rgba(15, 23, 42, 0.08);
      }

      &.unread {
        background: #fff7f7;
        border-color: #fecaca;
      }
    }

    .notification-main {
      min-width: 0;
      flex: 1;
    }

    .notification-title-row,
    .notification-meta,
    .notification-actions {
      display: flex;
      align-items: center;
      gap: 10px;
    }

    .notification-title-row {
      justify-content: space-between;

      h3 {
        margin: 0;
        font-size: 16px;
        color: #1f2937;
      }
    }

    .notification-card p {
      margin: 8px 0;
      color: #4b5563;
      line-height: 1.6;
    }

    .notification-meta {
      color: #94a3b8;
      font-size: 13px;
    }

    .notification-actions {
      flex-shrink: 0;
      align-self: center;
    }
  }

  .traveler-center {
    display: flex;
    flex-direction: column;
    gap: 18px;
  }

  .traveler-hero {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 20px;
    padding: 26px;
    border: 1px solid #e5e7eb;
    border-radius: 8px;
    background: linear-gradient(180deg, #ffffff 0%, #f8fafc 100%);
    box-shadow: 0 18px 42px rgba(15, 23, 42, 0.07);
    text-align: left;

    span {
      display: block;
      margin-bottom: 8px;
      color: #0f766e;
      font-size: 12px;
      font-weight: 900;
    }

    h3 {
      margin: 0 0 8px;
      color: #0f172a;
      font-size: 28px;
      font-weight: 900;
    }

    p {
      max-width: 620px;
      margin: 0;
      color: #64748b;
      line-height: 1.7;
    }

    .el-button {
      height: 46px;
      border-radius: 8px;
      font-weight: 800;
    }
  }

  .traveler-profile-list {
    display: flex;
    flex-direction: column;
    gap: 12px;
  }

  .traveler-profile-card {
    display: grid;
    grid-template-columns: 230px minmax(0, 1fr) 170px;
    align-items: center;
    gap: 18px;
    min-width: 0;
    padding: 16px 18px;
    border: 1px solid #e5e7eb;
    border-radius: 8px;
    background: linear-gradient(180deg, #ffffff 0%, #fbfdff 100%);
    box-shadow: 0 10px 28px rgba(15, 23, 42, 0.05);
    transition: border-color 0.18s ease, box-shadow 0.18s ease, transform 0.18s ease;

    &:hover {
      border-color: #bae6fd;
      box-shadow: 0 16px 36px rgba(15, 23, 42, 0.08);
      transform: translateY(-1px);
    }

  }

  .traveler-card-identity {
    display: flex;
    align-items: center;
    gap: 14px;
    min-width: 0;
  }

  .traveler-avatar {
    width: 52px;
    height: 52px;
    display: grid;
    place-items: center;
    flex: 0 0 auto;
    border-radius: 8px;
    background: #0f766e;
    color: #fff;
    font-size: 20px;
    font-weight: 900;
  }

  .traveler-main {
    min-width: 0;

    h4 {
      margin: 0;
      color: #0f172a;
      font-size: 18px;
      font-weight: 900;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }
  }

  .traveler-tags {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: 6px;
    margin-top: 8px;

    span {
      display: inline-flex;
      align-items: center;
      gap: 4px;
      height: 24px;
      padding: 0 9px;
      border-radius: 999px;
      background: #f1f5f9;
      color: #475569;
      font-size: 12px;
      font-weight: 800;
    }

  }

  .traveler-fields {
    display: grid;
    grid-template-columns: 0.95fr 0.8fr 1.45fr 0.85fr;
    gap: 10px 18px;
    align-items: center;

    div {
      min-width: 0;
      padding: 0 0 0 14px;
      border-left: 1px solid #e2e8f0;
    }

    .wide {
      grid-column: auto;
    }

    span,
    strong {
      display: block;
    }

    span {
      color: #94a3b8;
      font-size: 12px;
      font-weight: 800;
    }

    strong {
      margin-top: 4px;
      color: #334155;
      font-size: 14px;
      line-height: 1.4;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
      word-break: break-all;
    }
  }

  .traveler-card-actions {
    display: flex;
    flex-wrap: nowrap;
    align-items: center;
    justify-content: flex-end;
    gap: 8px;

    .el-button {
      min-width: 72px;
      border-radius: 8px;
      font-weight: 800;
      margin-left: 0;
    }
  }

  .traveler-profile-dialog {
    .traveler-form-grid {
      display: grid;
      grid-template-columns: repeat(2, minmax(0, 1fr));
      gap: 16px;
      align-items: start;

      .wide {
        grid-column: span 2;
      }
    }

    :deep(.el-form-item) {
      margin-bottom: 0;
    }

    :deep(.el-form-item__label) {
      display: flex;
      align-items: center;
      min-height: 20px;
      margin: 0 0 8px;
      padding: 0;
      color: #334155;
      font-size: 13px;
      font-weight: 800;
      line-height: 20px;
      text-align: left;
    }

    :deep(.el-input__wrapper),
    :deep(.el-select__wrapper) {
      min-height: 46px;
      border-radius: 8px;
      background: #ffffff;
      box-shadow: 0 0 0 1px #dfe7ef inset;
    }

    :deep(.el-input__wrapper.is-focus),
    :deep(.el-select__wrapper.is-focused) {
      box-shadow: 0 0 0 1px #0f766e inset, 0 0 0 4px rgba(15, 118, 110, 0.1);
    }

    :deep(.el-select__wrapper) {
      min-height: 46px;
      border-radius: 8px;
    }

    :deep(.el-select),
    :deep(.el-date-editor.el-input),
    :deep(.el-date-editor .el-input__wrapper) {
      width: 100%;
    }

    :deep(.el-segmented) {
      width: 100%;
      min-height: 46px;
      border-radius: 8px;
      background: #f1f5f9;
      padding: 4px;
    }

    :deep(.el-segmented__item) {
      min-height: 38px;
      border-radius: 7px;
      font-weight: 800;
    }
  }

  :deep(.secure-dialog) {
    border-radius: 8px;
    overflow: hidden;

    .el-dialog__header {
      padding: 22px 24px 12px;
      border-bottom: 1px solid #eef2f7;
    }

    .el-dialog__title {
      color: #0f172a;
      font-weight: 700;
    }

    .el-dialog__body {
      padding: 24px 26px 10px;
    }

    .el-dialog__footer {
      padding: 16px 24px 22px;
    }
  }

  .secure-form {
    :deep(.el-form-item) {
      margin-bottom: 18px;
    }

    :deep(.el-form-item__label) {
      margin-bottom: 8px;
      color: #334155;
      font-size: 14px;
      font-weight: 700;
      line-height: 1.2;
    }

    :deep(.el-input__wrapper) {
      min-height: 46px;
      border-radius: 12px;
      background: #ffffff;
      box-shadow: 0 0 0 1px #e2e8f0 inset;
      transition: box-shadow 0.18s ease;

      &:hover {
        box-shadow: 0 0 0 1px #7dd3fc inset;
      }

      &.is-focus {
        box-shadow: 0 0 0 1px #0ea5e9 inset, 0 0 0 3px rgba(14, 165, 233, 0.12);
      }
    }
  }

  .traveler-manage-form {
    .traveler-form-grid {
      display: grid;
      grid-template-columns: repeat(2, minmax(0, 1fr));
      gap: 18px 16px;
      align-items: start;

      .wide {
        grid-column: 1 / -1;
      }
    }

    :deep(.el-form-item) {
      display: block;
      margin-bottom: 0;
    }

    :deep(.el-form-item__label) {
      display: block;
      width: 100%;
      height: auto;
      margin: 0 0 8px;
      padding: 0;
      color: #334155;
      font-size: 13px;
      font-weight: 800;
      line-height: 20px;
      text-align: left;
    }

    :deep(.el-form-item__content) {
      display: block;
      width: 100%;
      line-height: normal;
    }

    :deep(.el-input),
    :deep(.el-select),
    :deep(.el-date-editor.el-input) {
      width: 100%;
    }

    :deep(.el-input__wrapper),
    :deep(.el-select__wrapper) {
      width: 100%;
      min-height: 46px;
      border-radius: 8px;
      background: #fff;
      box-shadow: 0 0 0 1px #dfe7ef inset;
      transition: box-shadow 0.18s ease, background 0.18s ease;
    }

    :deep(.el-input__wrapper:hover),
    :deep(.el-select__wrapper:hover) {
      box-shadow: 0 0 0 1px #99f6e4 inset;
    }

    :deep(.el-input__wrapper.is-focus),
    :deep(.el-select__wrapper.is-focused) {
      box-shadow: 0 0 0 1px #0f766e inset, 0 0 0 4px rgba(15, 118, 110, 0.1);
    }

    :deep(.el-segmented) {
      --el-segmented-item-selected-bg-color: #0d9488;
      --el-segmented-item-selected-color: #fff;
      width: 100%;
      height: 46px;
      min-height: 46px;
      padding: 4px;
      border-radius: 8px;
      background: #ecfdf5;
    }

    :deep(.el-segmented__group) {
      width: 100%;
      height: 100%;
    }

    :deep(.el-segmented__item) {
      height: 38px;
      min-height: 38px;
      border-radius: 7px;
      font-size: 14px;
      font-weight: 800;
    }

    :deep(.el-segmented__item-label) {
      height: 38px;
      line-height: 38px;
    }
  }

  .code-row {
    display: grid;
    grid-template-columns: minmax(0, 1fr) 118px;
    gap: 10px;
    width: 100%;

    .el-button {
      height: 46px;
      border-radius: 12px;
      font-weight: 700;
    }
  }

  .captcha-shell {
    margin-bottom: 18px;
    border: 1px solid #e5e7eb;
    border-radius: 8px;
    padding: 12px;
    background: #f8fafc;

    :deep(.geetest-box) {
      margin-bottom: 0;
    }
  }

  .password-captcha {
    margin-bottom: 0;
  }
}

@media (max-width: 900px) {
  .profile-container {
    .page-header {
      display: flex;
      align-items: flex-start;
      flex-direction: column;
    }

    .header-summary {
      width: 100%;
      text-align: center;
    }

    .password-content {
      .password-header,
      .password-form .form-actions {
        align-items: flex-start;
        flex-direction: column;
      }

      .password-grid {
        grid-template-columns: 1fr;
      }
    }

    .profile-content {
      &.profile-redesign {
        .account-overview,
        .account-person,
        .editor-actions,
        .section-heading {
          flex-direction: column;
          align-items: flex-start;
        }

        .overview-metrics,
        .profile-grid,
        .field-stack {
          grid-template-columns: 1fr;
          width: 100%;
        }

        .binding-card {
          grid-template-columns: 48px minmax(0, 1fr);

          .el-button {
            grid-column: 2;
            justify-self: start;
          }
        }
      }

      .account-panel,
      .form-grid {
        grid-template-columns: 1fr;
      }

      .identity-card {
        position: static;
      }

      .security-row {
        grid-template-columns: 44px minmax(0, 1fr);

        .el-tag,
        .el-button {
          grid-column: 2;
          justify-self: start;
        }
      }
    }

    .traveler-hero {
      flex-direction: column;
      align-items: flex-start;
    }

    .traveler-profile-card {
      grid-template-columns: 1fr;
      align-items: start;
    }

    .traveler-fields {
      grid-template-columns: repeat(2, minmax(0, 1fr));

      div {
        padding-left: 0;
        border-left: 0;
      }

      .wide {
        grid-column: span 2;
      }
    }

    .traveler-card-actions {
      justify-content: flex-start;
    }

    .traveler-profile-dialog {
      .traveler-form-grid {
        grid-template-columns: 1fr;

        .wide {
          grid-column: auto;
        }
      }
    }

    .traveler-manage-form {
      .traveler-form-grid {
        grid-template-columns: 1fr;

        .wide {
          grid-column: auto;
        }
      }
    }
  }
}

@media (max-width: 640px) {
  .profile-container {
    .traveler-fields {
      grid-template-columns: 1fr;

      div {
        padding-left: 0;
        border-left: 0;
      }

      .wide {
        grid-column: auto;
      }
    }
  }
}
</style>
