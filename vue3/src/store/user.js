import { defineStore } from 'pinia'
import request from '@/utils/request'
import { login, register } from '@/api/user'
// import { setToken, removeToken } from '@/utils/auth'

const TOKEN_EXPIRE_KEY = 'tokenExpire'
const TOKEN_EXPIRE_TIME = 2 * 60 * 60 * 1000 // 2小时

export const useUserStore = defineStore('user', {
  state: () => ({
    userInfo: JSON.parse(localStorage.getItem('userInfo')) || null,
    token: localStorage.getItem('token') || '',
    role: localStorage.getItem('role') || '',
    menus: JSON.parse(localStorage.getItem('menus')) || [],
    tokenExpire: localStorage.getItem(TOKEN_EXPIRE_KEY) || null
  }),

  getters: {
    // 判断是否登录
    isLoggedIn: (state) => !!state.token,
    // 判断是否是管理员
    isAdmin: (state) => state.userInfo?.roleCode === 'ADMIN',
    // 判断是否是普通用户
    isUser: (state) => state.userInfo?.roleCode === 'USER',
    // 判断token是否过期
    isTokenExpired: (state) => {
      if (!state.tokenExpire) return !state.token
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
      this.token = data.token
      this.role = data.roleCode
      
      // 存储到 LocalStorage
      localStorage.setItem('userInfo', JSON.stringify(this.userInfo))
      localStorage.setItem('token', this.token || '')
      localStorage.setItem('role', this.role || '')
      
      // 存储token过期时间
      const expireTime = Date.now() + TOKEN_EXPIRE_TIME
      this.tokenExpire = expireTime
      localStorage.setItem(TOKEN_EXPIRE_KEY, expireTime.toString())
    },
    clearUserInfo() {
      this.userInfo = null
      this.token = ''
      this.role = ''
      this.menus = []
      this.tokenExpire = null
      // 清除 LocalStorage
      localStorage.removeItem('userInfo')
      localStorage.removeItem('token')
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
      if (this.isTokenExpired && this.token) {
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
        const res = await request.post('/user/login', loginForm)
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
      if (!this.token) return false
      if (this.isTokenExpired) {
        this.clearUserInfo()
        return false
      }
      return true
    },
    // 刷新token过期时间（在用户活跃时调用）
    refreshTokenExpire() {
      if (this.token) {
        const expireTime = Date.now() + TOKEN_EXPIRE_TIME
        this.tokenExpire = expireTime
        localStorage.setItem(TOKEN_EXPIRE_KEY, expireTime.toString())
      }
    }
  }
})