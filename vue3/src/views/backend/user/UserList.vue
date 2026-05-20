<template>
  <div class="user-management">
    <!-- 页面标题 -->
    <div class="page-header">
      <h1 class="page-title">用户管理</h1>
      <p class="page-subtitle">User Management</p>
    </div>

    <!-- 操作按钮 -->
    <div class="action-bar">
      <div class="action-right">
        <el-button type="primary" @click="handleAdd" class="add-btn">
          <i class="el-icon-plus"></i> 新增用户
        </el-button>
      </div>
    </div>

    <!-- 搜索区域 -->
    <el-card class="search-card" shadow="never">
      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="用户名">
          <el-input v-model="searchForm.username" placeholder="请输入用户名" clearable>
            <template #prefix>
              <i class="el-icon-search"></i>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item label="昵称">
          <el-input v-model="searchForm.nickname" placeholder="请输入昵称" clearable>
            <template #prefix>
              <i class="el-icon-user"></i>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="searchForm.phone" placeholder="请输入手机号" clearable>
            <template #prefix>
              <i class="el-icon-phone"></i>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="searchForm.roleCode" placeholder="请选择角色" clearable>
            <el-option label="超级管理员" value="SUPER_ADMIN"></el-option>
            <el-option label="管理员" value="ADMIN"></el-option>
            <el-option label="普通用户" value="USER"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch" class="search-btn">
            <i class="el-icon-search"></i> 搜索
          </el-button>
          <el-button @click="resetSearch" class="reset-btn">
            <i class="el-icon-refresh"></i> 重置
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 表格区域 -->
    <el-card class="table-card" shadow="never">
      <el-table
        v-loading="loading"
        :data="tableData"
        border
        stripe
        style="width: 100%"
        class="user-table"
      >
        <el-table-column prop="id" label="ID" width="80" align="center"></el-table-column>
        <el-table-column prop="username" label="用户名" width="120">
          <template #default="scope">
            <div class="user-name">{{ scope.row.username }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="nickname" label="昵称" width="118" show-overflow-tooltip>
          <template #default="scope">
            <span class="nickname-text">{{ scope.row.nickname || '未设置' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="phone" label="手机号" width="130">
          <template #default="scope">
            <span>{{ scope.row.phone || '未绑定' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="email" label="邮箱" width="180">
          <template #default="scope">
            <span class="muted-text">{{ scope.row.email || '未绑定' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="sex" label="性别" width="80" align="center">
          <template #default="scope">
            {{ scope.row.sex === '男' ? '男' : scope.row.sex === '女' ? '女' : '未知' }}
          </template>
        </el-table-column>
        <el-table-column prop="roleCode" label="角色" width="132" align="center">
          <template #default="scope">
            <span class="role-badge" :class="getRoleClass(scope.row.roleCode)">
              <span class="role-badge__text">{{ getRoleLabel(scope.row.roleCode) }}</span>
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="scope">
            <el-switch
              v-model="scope.row.status"
              :active-value="1"
              :inactive-value="0"
              :disabled="!canManageUser(scope.row)"
              @change="handleStatusChange(scope.row)"
            ></el-switch>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180" align="center">
          <template #default="scope">
            <span class="date-text">{{ formatDate(scope.row.createTime) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" fixed="right" width="300" align="center">
          <template #default="scope">
            <el-button size="small" type="primary" plain :disabled="!canManageUser(scope.row)" @click="handleEdit(scope.row)" class="edit-btn">
              <i class="el-icon-edit"></i> 编辑
            </el-button>
            <el-button size="small" type="danger" plain :disabled="!canDeleteUser(scope.row)" @click="handleDelete(scope.row)" class="delete-btn">
              <i class="el-icon-delete"></i> 停用
            </el-button>
            <el-button size="small" type="warning" plain :disabled="!canManageUser(scope.row)" @click="handleResetPassword(scope.row)">
              <i class="el-icon-key"></i> 重置密码
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-container">
        <el-pagination
          background
          layout="total, sizes, prev, pager, next, jumper"
          :current-page="currentPage"
          :page-sizes="[10, 20, 50, 100]"
          :page-size="pageSize"
          :total="total"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        ></el-pagination>
      </div>
    </el-card>

    <!-- 用户表单对话框 -->
    <el-dialog
      :title="dialogTitle"
      v-model="dialogVisible"
      width="500px"
      @close="resetForm"
      class="user-dialog"
    >
      <el-form
        ref="userFormRef"
        :model="userForm"
        :rules="userFormRules"
        label-width="100px"
        class="user-form"
      >
        <el-form-item label="用户名" prop="username">
          <el-input v-model="userForm.username" placeholder="请输入用户名"></el-input>
        </el-form-item>
        <el-form-item label="密码" prop="password" v-if="dialogType === 'add'">
          <el-input v-model="userForm.password" type="password" placeholder="请输入密码"></el-input>
        </el-form-item>
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="userForm.nickname" placeholder="请输入昵称"></el-input>
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="userForm.email" placeholder="选填，作为备用联系方式"></el-input>
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="userForm.phone" placeholder="请输入手机号，用户可用于登录"></el-input>
        </el-form-item>
        <el-form-item label="性别" prop="sex">
          <el-radio-group v-model="userForm.sex">
            <el-radio label="男">男</el-radio>
            <el-radio label="女">女</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="角色" prop="roleCode">
          <el-select v-model="userForm.roleCode" placeholder="请选择角色">
            <el-option v-if="userStore.isSuperAdmin" label="管理员" value="ADMIN"></el-option>
            <el-option label="普通用户" value="USER"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-switch
            v-model="userForm.status"
            :active-value="1"
            :inactive-value="0"
          ></el-switch>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitForm" :loading="submitLoading">确定</el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 重置密码对话框 -->
    <el-dialog
      title="重置密码"
      v-model="resetPasswordDialogVisible"
      width="400px"
      class="reset-password-dialog"
    >
      <el-form
        ref="resetPasswordFormRef"
        :model="resetPasswordForm"
        :rules="resetPasswordFormRules"
        label-width="100px"
      >
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="resetPasswordForm.newPassword" type="password" placeholder="请输入新密码"></el-input>
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="resetPasswordForm.confirmPassword" type="password" placeholder="请确认新密码"></el-input>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="resetPasswordDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitResetPassword" :loading="resetPasswordLoading">确定</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'
import { formatDate } from '@/utils/dateUtils'
import { useUserStore } from '@/store/user'

const userStore = useUserStore()
const currentUserId = computed(() => userStore.userInfo?.id)

// 表格数据
const tableData = ref([])
const loading = ref(false)

// 分页
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

// 搜索表单
const searchForm = reactive({
  username: '',
  nickname: '',
  phone: '',
  roleCode: ''
})

// 用户表单
const userFormRef = ref(null)
const dialogVisible = ref(false)
const dialogType = ref('add') // 'add' 或 'edit'
const dialogTitle = ref('新增用户')
const submitLoading = ref(false)

const userForm = reactive({
  id: null,
  username: '',
  password: '',
  nickname: '',
  email: '',
  phone: '',
  sex: '男',
  roleCode: 'USER',
  status: 1
})

const userFormRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 50, message: '用户名长度在3到50个字符之间', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度在6到20个字符之间', trigger: 'blur' }
  ],
  email: [
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
  ],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号码', trigger: 'blur' }
  ],
  roleCode: [
    { required: true, message: '请选择角色', trigger: 'change' }
  ]
}

// 重置密码
const resetPasswordFormRef = ref(null)
const resetPasswordDialogVisible = ref(false)
const resetPasswordLoading = ref(false)
const resetPasswordUserId = ref(null)

const resetPasswordForm = reactive({
  newPassword: '',
  confirmPassword: ''
})

const resetPasswordFormRules = {
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度在6到20个字符之间', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== resetPasswordForm.newPassword) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

const getRoleLabel = (roleCode) => {
  const roleMap = {
    SUPER_ADMIN: '超级管理员',
    ADMIN: '管理员',
    USER: '普通用户'
  }
  return roleMap[roleCode] || '普通用户'
}

const getRoleClass = (roleCode) => {
  if (roleCode === 'SUPER_ADMIN') return 'role-super'
  if (roleCode === 'ADMIN') return 'role-admin'
  return 'role-user'
}

const canManageUser = (row) => {
  if (!row || row.id === currentUserId.value) return false
  if (row.protectedAccount) return false
  if (userStore.isSuperAdmin) return true
  return row.roleCode === 'USER'
}

const canDeleteUser = (row) => {
  return canManageUser(row) && !row.protectedAccount
}

// 生命周期钩子
onMounted(() => {
  fetchUsers()
})

// 获取用户列表
const fetchUsers = async () => {
  loading.value = true
  try {
    await request.get('/user/page', {
   
        username: searchForm.username,
        phone: searchForm.phone,
        nickname: searchForm.nickname,
        roleCode: searchForm.roleCode,
        currentPage: currentPage.value,
        size: pageSize.value
      },{
      onSuccess: (res) => {
        tableData.value = res.records||[]
        total.value = res.total||0
      }
      }
    )
  } catch (error) {
    console.error('获取用户列表失败:', error)
  } finally {
    loading.value = false
  }
}

// 搜索
const handleSearch = () => {
  currentPage.value = 1
  fetchUsers()
}

// 重置搜索
const resetSearch = () => {
  Object.keys(searchForm).forEach(key => {
    searchForm[key] = ''
  })
  currentPage.value = 1
  fetchUsers()
}

// 分页处理
const handleSizeChange = (size) => {
  pageSize.value = size
  fetchUsers()
}

const handleCurrentChange = (page) => {
  currentPage.value = page
  fetchUsers()
}

// 新增用户
const handleAdd = () => {
  dialogType.value = 'add'
  dialogTitle.value = '新增用户'
  dialogVisible.value = true
}

// 编辑用户
const handleEdit = (row) => {
  if (!canManageUser(row)) {
    ElMessage.warning('当前角色无权管理该账号')
    return
  }
  dialogType.value = 'edit'
  dialogTitle.value = '编辑用户'
  Object.keys(userForm).forEach(key => {
    if (key in row) {
      userForm[key] = row[key]
    }
  })
  dialogVisible.value = true
}

// 停用用户
const handleDelete = (row) => {
  if (!canDeleteUser(row)) {
    ElMessage.warning('该账号受保护，不能停用')
    return
  }
  ElMessageBox.confirm('确定要停用该用户吗？停用后该用户不能继续登录，历史业务数据会被保留。', '停用账号', {
    confirmButtonText: '确认停用',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await request.delete(`/user/delete/${row.id}`, {
        successMsg: '账号已停用'
      })
      fetchUsers()
    } catch (error) {
      console.error('停用用户失败:', error)
      fetchUsers()
    }
  }).catch(() => {})
}

// 状态变更
const handleStatusChange = async (row) => {
  if (!canManageUser(row)) {
    row.status = row.status === 1 ? 0 : 1
    ElMessage.warning('当前角色无权调整该账号状态')
    return
  }
  try {
    await request.put(`/user/status/${row.id}`, null, {
      params: { status: row.status },
      successMsg: `用户状态${row.status === 1 ? '启用' : '禁用'}成功`
    })
  } catch (error) {
    console.error('更新用户状态失败:', error)
    // 恢复原状态
    row.status = row.status === 1 ? 0 : 1
  }
}

// 重置密码
const handleResetPassword = (row) => {
  if (!canManageUser(row)) {
    ElMessage.warning('当前角色无权重置该账号密码')
    return
  }
  resetPasswordUserId.value = row.id
  resetPasswordForm.newPassword = ''
  resetPasswordForm.confirmPassword = ''
  resetPasswordDialogVisible.value = true
}

const submitResetPassword = () => {
  resetPasswordFormRef.value.validate(async (valid) => {
    if (valid) {
      resetPasswordLoading.value = true
      try {
        await request.put(`/user/resetPassword/${resetPasswordUserId.value}`, {
          newPassword: resetPasswordForm.newPassword
        }, {
          successMsg: '密码重置成功'
        })
        resetPasswordDialogVisible.value = false
      } catch (error) {
        console.error('重置密码失败:', error)
      } finally {
        resetPasswordLoading.value = false
      }
    }
  })
}

// 提交表单
const submitForm = () => {
  userFormRef.value.validate(async (valid) => {
    if (valid) {
      submitLoading.value = true
      try {
        if (dialogType.value === 'add') {
          await request.post('/user/add', userForm, {
            successMsg: '添加用户成功'
          })
        } else {
          await request.put(`/user/${userForm.id}`, userForm, {
            successMsg: '更新用户成功'
          })
        }
        dialogVisible.value = false
        fetchUsers()
      } catch (error) {
        console.error('提交表单失败:', error)
      } finally {
        submitLoading.value = false
      }
    }
  })
}

// 重置表单
const resetForm = () => {
  if (userFormRef.value) {
    userFormRef.value.resetFields()
  }
  Object.keys(userForm).forEach(key => {
    if (key === 'sex') {
      userForm[key] = '男'
    } else if (key === 'roleCode') {
      userForm[key] = 'USER'
    } else if (key === 'status') {
      userForm[key] = 1
    } else {
      userForm[key] = key === 'id' ? null : ''
    }
  })
}
</script>

<style lang="scss" scoped>
.user-management {
  padding: 20px;
  background-color: #f9fafc;
  min-height: calc(100vh - 120px);

  .page-header {
    margin-bottom: 24px;
    text-align: left;
    
    .page-title {
      font-size: 24px;
      color: #34495e;
      margin: 0 0 8px 0;
      font-weight: 500;
    }
    
    .page-subtitle {
      font-size: 14px;
      color: #7f8c8d;
      margin: 0;
      font-style: italic;
    }
  }

  .action-bar {
    margin-bottom: 20px;
    display: flex;
    justify-content: flex-end;
    
    .action-right {
      display: flex;
      justify-content: flex-end;
    }
    
    .add-btn {
      background-color: #2ecc71;
      border-color: #2ecc71;
      margin-right: 10px;
      
      &:hover, &:focus {
        background-color: #27ae60;
        border-color: #27ae60;
      }
    }
  }

  .search-card {
    margin-bottom: 20px;
    border-radius: 8px;
    background-color: #fff;
    box-shadow: none;
    
      .search-form {
        padding: 10px 0;
        display: flex;
        flex-wrap: nowrap;
        align-items: center;
        gap: 12px;
        overflow-x: auto;
      
        .el-form-item {
          margin-bottom: 0;
          margin-right: 0;
          flex: 0 0 auto;
        }

        :deep(.el-input) {
          width: 180px;
        }

        :deep(.el-select) {
          width: 150px;
        }
      
      .search-btn {
        background-color: #3498db;
        border-color: #3498db;
        
        &:hover, &:focus {
          background-color: #2980b9;
          border-color: #2980b9;
        }
      }
      
      .reset-btn {
        color: #7f8c8d;
        border-color: #bdc3c7;
        
        &:hover, &:focus {
          color: #34495e;
          border-color: #95a5a6;
          background-color: #FFFFFF;
        }
      }
    }
  }
  
  .table-card {
    border-radius: 8px;
    overflow: hidden;
    box-shadow: none;
    
    .user-table {
      border-radius: 4px;
      overflow: hidden;
      
      :deep(thead) {
        background-color: #ecf0f1;
        
        th {
          background-color: #ecf0f1;
          color: #34495e;
          font-weight: 500;
          padding: 12px 0;
        }
      }
      
      :deep(tbody tr) {
        transition: all 0.3s;
        
        &:hover {
          background-color: #f8f9fa;
        }
      }
      
      .user-name {
        font-weight: 500;
        color: #2980b9;
      }
      
      .date-text {
        color: #7f8c8d;
        font-size: 12px;
      }

      .muted-text {
        color: #95a5a6;
      }

      .nickname-text {
        display: inline-block;
        max-width: 104px;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
        vertical-align: middle;
      }

      .role-badge {
        display: inline-flex;
        align-items: center;
        justify-content: center;
        min-width: 96px;
        height: 26px;
        padding: 0 12px;
        border-radius: 999px;
        font-size: 12px;
        font-weight: 600;
        line-height: 1;
        white-space: nowrap;
        list-style: none;
        list-style-type: none;
        appearance: none;
        overflow: hidden;
        position: relative;
        vertical-align: middle;
        box-sizing: border-box;
        border: 0;
        outline: 0;
        box-shadow: none;
        background-clip: padding-box;
        text-indent: 0;
      }

      .role-badge::before,
      .role-badge::after {
        display: none !important;
        content: none !important;
      }

      .role-badge::marker {
        content: '';
        font-size: 0;
      }

      .role-badge__text {
        display: block;
        line-height: 1;
        list-style: none;
        max-width: none;
        overflow: visible;
        text-overflow: clip;
        white-space: nowrap;
      }

      .role-badge__text::before,
      .role-badge__text::after,
      .role-badge__text::marker {
        display: none !important;
        content: none !important;
        font-size: 0;
      }

      .role-super {
        color: #9f1239;
        background: #fff1f2;
      }

      .role-admin {
        color: #92400e;
        background: #fffbeb;
      }

      .role-user {
        color: #1d4ed8;
        background: #eff6ff;
      }
      
      .edit-btn {
        margin-right: 8px;
      }
    }
  }

  .pagination-container {
    margin-top: 20px;
    display: flex;
    justify-content: flex-end;
    padding: 0 20px;
  }

  .user-dialog, .reset-password-dialog {
    :deep(.el-dialog__header) {
      border-bottom: 1px solid #ecf0f1;
      padding: 20px;
      
      .el-dialog__title {
        font-size: 18px;
        color: #34495e;
        font-weight: 500;
      }
    }
    
    :deep(.el-dialog__body) {
      padding: 30px 20px;
    }
    
    :deep(.el-dialog__footer) {
      border-top: 1px solid #ecf0f1;
      padding: 15px 20px;
    }
  }
}
</style> 
