import { defineStore } from 'pinia'
import request from '@/utils/request'
import { encryptPassword } from '@/utils/passwordCrypto'
// import { setToken, removeToken } from '@/utils/auth'

const TOKEN_EXPIRE_KEY = 'tokenExpire'
const TOKEN_EXPIRE_TIME = 2 * 60 * 60 * 1000 // 2小时

export const useUserStore = defineStore('user', {
  state: () => ({
    userInfo: JSON.parse(localStorage.getItem('userInfo')) || null,
    token: localStorage.getItem('userInfo') ? 'cookie' : '',
    role: localStorage.getItem('role') || '',
    menus: JSON.parse(localStorage.getItem('menus')) || [],
    tokenExpire: localStorage.getItem(TOKEN_EXPIRE_KEY) || null
  }),

  getters: {
    // 判断是否登录
    isLoggedIn: (state) => !!state.userInfo,
    // 判断是否是后台管理员（超级管理员也属于后台管理员）
    isAdmin: (state) => ['SUPER_ADMIN', 'ADMIN'].includes(state.userInfo?.roleCode),
    // 判断是否是超级管理员
    isSuperAdmin: (state) => state.userInfo?.roleCode === 'SUPER_ADMIN',
    // 判断是否是普通用户
    isUser: (state) => state.userInfo?.roleCode === 'USER',
    permissions: (state) => state.userInfo?.permissions || [],
    hasPermission: (state) => (permission) => {
      const permissions = state.userInfo?.permissions || []
      return permissions.includes(permission)
    },
    // 判断token是否过期
    isTokenExpired: (state) => {
      if (!state.tokenExpire) return !state.userInfo
      return Date.now() > Number(state.tokenExpire)
    }
  },

  actions: {
    // 更新用户信息
    updateUserInfo(data) {
      if (!data) return
      this.userInfo = data
      localStorage.setItem('userInfo', JSON.stringify(data))
    },
    
    setUserInfo(data) {
      if (!data) return
      
      this.userInfo = data.userInfo || data
      this.token = 'cookie'
      this.role = this.userInfo?.roleCode || data.roleCode
      
      // 存储到 LocalStorage
      localStorage.setItem('userInfo', JSON.stringify(this.userInfo))
      localStorage.setItem('role', this.role || '')
      
      // 存储token过期时间
      const expireTime = Date.now() + TOKEN_EXPIRE_TIME
      this.tokenExpire = expireTime
      localStorage.setItem(TOKEN_EXPIRE_KEY, expireTime.toString())
    },
    syncFromStorage() {
      this.role = localStorage.getItem('role') || ''
      this.tokenExpire = localStorage.getItem(TOKEN_EXPIRE_KEY) || null
      try {
        this.userInfo = JSON.parse(localStorage.getItem('userInfo')) || null
        this.menus = JSON.parse(localStorage.getItem('menus')) || []
        this.token = this.userInfo ? 'cookie' : ''
      } catch {
        this.userInfo = null
        this.menus = []
        this.token = ''
      }
    },
    clearUserInfo() {
      this.userInfo = null
      this.token = ''
      this.role = ''
      this.menus = []
      this.tokenExpire = null
      // 清除 LocalStorage
      localStorage.removeItem('userInfo')
      localStorage.removeItem('role')
      localStorage.removeItem('menus')
      localStorage.removeItem(TOKEN_EXPIRE_KEY)
    },
    setMenus(menus) {
      if (!menus) return
      this.menus = menus
      localStorage.setItem('menus', JSON.stringify(menus))
    },
    // 获取用户信息和菜单 - 从localStorage恢复
    async getUserInfo() {
      // 检查token是否过期
      if (this.isTokenExpired && this.userInfo) {
        this.clearUserInfo()
        throw new Error('Token expired')
      }
      
      const userInfo = JSON.parse(localStorage.getItem('userInfo'))
      const menus = JSON.parse(localStorage.getItem('menus'))
      
      if (userInfo && menus) {
        this.userInfo = userInfo
        this.menus = menus
        return { userInfo, menus }
      }
      
      // 如果没有缓存的数据，清除状态并抛出错误
      this.clearUserInfo()
      throw new Error('No cached user info')
    },
    // 登录
    async login(loginForm) {
      try {
        const encryptedPassword = await encryptPassword(loginForm.password)
        const res = await request.post('/auth/login/password', {
          phone: loginForm.phone,
          encryptedPassword,
          agreementAccepted: loginForm.agreementAccepted === true
        })
        this.setUserInfo(res)
        return res
      } catch (error) {
        this.clearUserInfo()
        throw error
      }
    },
    // 注册

    // 退出登录
    async logout() {
      this.clearUserInfo()
    },
    // 检查登录状态
    checkLoginStatus() {
      // 先检查token是否存在，再检查是否过期
      if (!this.userInfo) return false
      if (this.isTokenExpired) {
        this.clearUserInfo()
        return false
      }
      return true
    },
    // 刷新token过期时间（在用户活跃时调用）
    refreshTokenExpire() {
      if (this.userInfo) {
        const expireTime = Date.now() + TOKEN_EXPIRE_TIME
        this.tokenExpire = expireTime
        localStorage.setItem(TOKEN_EXPIRE_KEY, expireTime.toString())
      }
    }
  }
})
