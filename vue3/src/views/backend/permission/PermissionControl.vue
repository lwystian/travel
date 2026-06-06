<template>
  <div class="permission-page">
    <section class="page-head">
      <div>
        <h2>权限控制</h2>
        <p>按管理员账号配置后台功能边界，并统一设置管理员内容审核策略。</p>
      </div>
      <el-button type="primary" :icon="Refresh" :loading="loading" @click="loadData">刷新</el-button>
    </section>

    <section class="policy-panel">
      <div class="policy-copy">
        <h3>管理员内容审核策略</h3>
        <p>超级管理员始终免审。管理员默认免审并跳过敏感词拦截，也可以按企业审核流程开启人工审核。</p>
      </div>
      <div class="policy-controls">
        <div class="policy-item">
          <span>管理员评论和发布需要审核</span>
          <el-switch v-model="moderationConfig.adminContentReviewRequired" @change="saveModerationConfig" />
        </div>
      </div>
    </section>

    <section class="permission-layout">
      <aside class="admin-list">
        <div class="panel-title">
          <h3>管理员账号</h3>
          <span>{{ administrators.length }} 人</span>
        </div>
        <button
          v-for="admin in administrators"
          :key="admin.id"
          class="admin-item"
          :class="{ active: selectedAdmin?.id === admin.id, super: admin.roleCode === 'SUPER_ADMIN' }"
          type="button"
          @click="selectAdmin(admin)"
        >
          <el-avatar :size="38" :src="resolveImageUrl(admin.avatar, '')">{{ avatarText(admin) }}</el-avatar>
          <span class="admin-meta">
            <strong>{{ admin.nickname || admin.username }}</strong>
            <small>{{ admin.roleName || roleLabel(admin.roleCode) }}</small>
          </span>
        </button>
      </aside>

      <main class="permission-panel">
        <div class="panel-title">
          <div>
            <h3>{{ selectedAdmin ? `${selectedAdmin.nickname || selectedAdmin.username} 的后台权限` : '请选择管理员' }}</h3>
            <p v-if="selectedAdmin?.roleCode === 'SUPER_ADMIN'">超级管理员默认拥有全部权限，不需要单独配置。</p>
            <p v-else>保存后该管理员将按勾选权限访问后台菜单和管理接口。</p>
          </div>
          <el-button
            type="primary"
            :icon="Check"
            :disabled="!selectedAdmin || selectedAdmin.roleCode === 'SUPER_ADMIN'"
            :loading="saving"
            @click="savePermissions"
          >
            保存权限
          </el-button>
        </div>

        <div class="permission-groups" v-loading="loading">
          <div v-for="group in permissionGroups" :key="group.name" class="permission-group">
            <div class="group-head">
              <h4>{{ group.name }}</h4>
              <el-button
                link
                type="primary"
                :disabled="!canEdit"
                @click="toggleGroup(group)"
              >
                {{ isGroupChecked(group) ? '取消全选' : '全选' }}
              </el-button>
            </div>
            <el-checkbox-group v-model="checkedPermissions" :disabled="!canEdit" class="permission-grid">
              <el-checkbox
                v-for="item in group.items"
                :key="item.code"
                :label="item.code"
                class="permission-card"
              >
                <span>{{ item.name }}</span>
                <small>{{ item.description }}</small>
              </el-checkbox>
            </el-checkbox-group>
          </div>
        </div>
      </main>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Check, Refresh } from '@element-plus/icons-vue'
import request from '@/utils/request'
import { resolveImageUrl } from '@/utils/imageUrl'
import { useUserStore } from '@/store/user'

const loading = ref(false)
const saving = ref(false)
const userStore = useUserStore()
const catalog = ref([])
const administrators = ref([])
const selectedAdmin = ref(null)
const checkedPermissions = ref([])
const moderationConfig = reactive({
  adminContentReviewRequired: false
})

const permissionGroups = computed(() => {
  const groups = new Map()
  catalog.value.forEach(item => {
    const name = item.groupName || '其他权限'
    if (!groups.has(name)) groups.set(name, [])
    groups.get(name).push(item)
  })
  return Array.from(groups.entries()).map(([name, items]) => ({ name, items }))
})

const canEdit = computed(() => selectedAdmin.value && selectedAdmin.value.roleCode !== 'SUPER_ADMIN')

const loadData = async () => {
  loading.value = true
  try {
    const [permissionData, configData] = await Promise.all([
      request.get('/admin-governance/permissions'),
      request.get('/admin-governance/moderation-config')
    ])
    catalog.value = permissionData.catalog || []
    administrators.value = sortAdministrators(permissionData.administrators || [])
    moderationConfig.adminContentReviewRequired = Boolean(
      configData?.adminContentReviewRequired ||
      configData?.adminGuideReviewRequired ||
      configData?.adminCommentReviewRequired
    )
    const current = selectedAdmin.value
      ? administrators.value.find(item => item.id === selectedAdmin.value.id)
      : administrators.value.find(item => item.roleCode === 'ADMIN') || administrators.value[0]
    selectAdmin(current || null)
  } finally {
    loading.value = false
  }
}

const selectAdmin = (admin) => {
  selectedAdmin.value = admin
  checkedPermissions.value = [...(admin?.permissions || [])]
}

const savePermissions = async () => {
  if (!selectedAdmin.value || selectedAdmin.value.roleCode === 'SUPER_ADMIN') return
  saving.value = true
  try {
    await request.put(`/admin-governance/permissions/${selectedAdmin.value.id}`, {
      permissions: checkedPermissions.value
    })
    ElMessage.success('权限配置已保存')
    await loadData()
    await userStore.refreshCurrentUser()
  } finally {
    saving.value = false
  }
}

const saveModerationConfig = async () => {
  await request.put('/admin-governance/moderation-config', {
    adminContentReviewRequired: moderationConfig.adminContentReviewRequired
  }, { showDefaultMsg: false })
  ElMessage.success('审核策略已保存')
}

const toggleGroup = (group) => {
  const codes = group.items.map(item => item.code)
  const existing = new Set(checkedPermissions.value)
  if (isGroupChecked(group)) {
    codes.forEach(code => existing.delete(code))
  } else {
    codes.forEach(code => existing.add(code))
  }
  existing.add('dashboard:view')
  checkedPermissions.value = Array.from(existing)
}

const isGroupChecked = (group) => group.items.every(item => checkedPermissions.value.includes(item.code))
const avatarText = (admin) => (admin?.nickname || admin?.username || '管').slice(0, 1)
const roleLabel = (roleCode) => roleCode === 'SUPER_ADMIN' ? '超级管理员' : '管理员'

const sortAdministrators = (items) => [...items].sort((left, right) => {
  const leftRank = left?.roleCode === 'SUPER_ADMIN' ? 0 : 1
  const rightRank = right?.roleCode === 'SUPER_ADMIN' ? 0 : 1
  if (leftRank !== rightRank) return leftRank - rightRank
  return Number(right?.id || 0) - Number(left?.id || 0)
})

onMounted(loadData)
</script>

<style lang="scss" scoped>
.permission-page {
  min-height: 100%;
  padding: 24px;
  background: #f5f7fb;
}

.page-head,
.policy-panel,
.permission-layout {
  width: min(1480px, 100%);
  margin: 0 auto 18px;
}

.page-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 24px 28px;
  border: 1px solid #e8edf5;
  border-radius: 8px;
  background: linear-gradient(135deg, #ffffff 0%, #f5f9ff 100%);
  text-align: left;

  > div {
    flex: 1;
    min-width: 0;
    text-align: left;
  }

  h2 {
    margin: 0;
    color: #111827;
    font-size: 26px;
  }

  p {
    margin: 8px 0 0;
    color: #64748b;
  }
}

.policy-panel {
  display: flex;
  gap: 22px;
  align-items: flex-start;
  justify-content: flex-start;
  padding: 22px 24px;
  border: 1px solid #e7edf5;
  border-radius: 8px;
  background: #ffffff;
}

.policy-copy {
  flex: 1;
  text-align: left;

  h3 {
    margin: 0 0 8px;
    color: #172033;
    font-size: 18px;
  }

  p {
    margin: 0;
    color: #64748b;
    line-height: 1.7;
  }
}

.policy-controls {
  display: grid;
  width: min(430px, 100%);
  gap: 12px;
}

.policy-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  padding: 12px 14px;
  border-radius: 8px;
  background: #f8fafc;
  color: #334155;
  font-weight: 700;
}

.permission-layout {
  display: grid;
  grid-template-columns: 300px 1fr;
  gap: 18px;
}

.admin-list,
.permission-panel {
  border: 1px solid #e7edf5;
  border-radius: 8px;
  background: #ffffff;
}

.admin-list {
  padding: 16px;
}

.panel-title {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 14px;
  text-align: left;

  h3 {
    margin: 0;
    color: #172033;
    font-size: 18px;
  }

  p,
  span {
    margin: 6px 0 0;
    color: #64748b;
    font-size: 13px;
  }
}

.admin-item {
  display: flex;
  width: 100%;
  align-items: center;
  gap: 12px;
  margin-bottom: 10px;
  padding: 12px;
  border: 1px solid #edf2f7;
  border-radius: 8px;
  background: #ffffff;
  text-align: left;
  cursor: pointer;

  &.active {
    border-color: rgba(37, 99, 235, 0.45);
    background: #f3f8ff;
  }

  &.super {
    border-color: rgba(180, 130, 22, 0.3);
    background: #fffaf0;
  }
}

.admin-meta {
  display: grid;
  min-width: 0;

  strong {
    overflow: hidden;
    color: #172033;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  small {
    margin-top: 4px;
    color: #64748b;
  }
}

.permission-panel {
  padding: 20px;
}

.permission-group {
  padding: 16px 0;
  border-top: 1px solid #eef2f7;

  &:first-child {
    border-top: 0;
  }
}

.group-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;

  h4 {
    margin: 0;
    color: #1f2937;
    font-size: 16px;
  }
}

.permission-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(230px, 1fr));
  gap: 10px;
}

:deep(.permission-card) {
  height: auto;
  min-height: 76px;
  align-items: flex-start;
  margin: 0;
  padding: 12px;
  border: 1px solid #edf2f7;
  border-radius: 8px;
  background: #fbfcfe;

  .el-checkbox__label {
    display: grid;
    gap: 5px;
    color: #1f2937;
    font-weight: 760;
    line-height: 1.35;
    white-space: normal;
  }

  small {
    color: #64748b;
    font-size: 12px;
    font-weight: 500;
  }
}

@media (max-width: 980px) {
  .policy-panel,
  .permission-layout {
    grid-template-columns: 1fr;
    flex-direction: column;
  }

  .policy-controls {
    min-width: 0;
  }
}
</style>
