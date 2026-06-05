import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'
import logger from '@/utils/logger'
import { prepareImageUploadFormData } from '@/utils/imageCompression'

let isHandlingTokenExpired = false

const clearAuthStorage = () => {
  localStorage.removeItem('userInfo')
  localStorage.removeItem('role')
  localStorage.removeItem('menus')
  localStorage.removeItem('tokenExpire')
}

const handleTokenExpired = () => {
  if (isHandlingTokenExpired) return
  isHandlingTokenExpired = true

  clearAuthStorage()
  const currentRoute = router.currentRoute.value
  const needsLogin = currentRoute.path.startsWith('/back') ||
    currentRoute.matched.some(record => record.meta.requiresAuth)

  if (needsLogin) {
    ElMessage.error('登录已过期，请重新登录')
  }

  setTimeout(() => {
    if (needsLogin && router.currentRoute.value.path !== '/login') {
      router.push({
        path: '/login',
        query: { redirect: router.currentRoute.value.fullPath }
      })
    }

    setTimeout(() => {
      isHandlingTokenExpired = false
    }, 1000)
  }, 100)
}

const service = axios.create({
  baseURL: process.env.VUE_APP_BASE_API || '/api',
  timeout: 15000,
  withCredentials: true,
  headers: {
    'Content-Type': 'application/json;charset=utf-8',
    'X-Requested-With': 'XMLHttpRequest'
  }
})

service.interceptors.request.use(
  async config => {
    if (config.method === 'get') {
      config.headers['Cache-Control'] = 'no-cache'
      config.headers.Pragma = 'no-cache'
    }

    if (config.method?.toLowerCase() === 'post' && config.data instanceof FormData) {
      config.data = await prepareImageUploadFormData(config.url, config.data)
    }

    return config
  },
  error => {
    logger.error('Request interceptor failed:', error)
    return Promise.reject(error)
  }
)

const syncAuthFromHeaders = (response) => {
  const tokenExpire = response.headers?.['x-token-expire']

  if (tokenExpire) {
    localStorage.setItem('tokenExpire', tokenExpire)
  }
}

const getBusinessErrorMessage = (code, fallback) => {
  switch (String(code)) {
    case '403':
      return fallback || '没有权限进行此操作'
    case '404':
      return fallback || '请求的资源不存在'
    case '500':
      return fallback || '服务器内部错误'
    default:
      return fallback || `请求失败(${code})`
  }
}

const getHttpErrorMessage = (error, config) => {
  if (config.errorMsg) return config.errorMsg

  if (error.response) {
    switch (error.response.status) {
      case 400:
        return '请求参数错误'
      case 403:
        return '拒绝访问'
      case 404:
        return '请求的资源不存在'
      case 408:
        return '请求超时'
      case 500:
        return '服务器内部错误'
      case 501:
        return '服务未实现'
      case 502:
        return '网关错误'
      case 503:
        return '服务不可用'
      case 504:
        return '网关超时'
      default:
        return error.response.data?.msg || `请求失败(${error.response.status})`
    }
  }

  if (error.code === 'ECONNABORTED') {
    return '请求超时，请检查网络连接'
  }

  if (error.message?.includes('Network Error')) {
    return '网络连接失败，请检查网络设置'
  }

  return '网络请求失败，请稍后重试'
}

service.interceptors.response.use(
  response => {
    syncAuthFromHeaders(response)

    if (response.config?.responseType === 'blob') {
      return response
    }

    const res = response.data
    const config = response.config || {}
    const showDefaultMsg = config.showDefaultMsg !== false

    if (res.code === '200') {
      try {
        if (config.successMsg) {
          ElMessage.success(config.successMsg)
        } else if (showDefaultMsg && config.method && config.method.toLowerCase() !== 'get') {
          if (res.msg && res.msg !== 'success' && res.msg !== '操作成功') {
            ElMessage.success(res.msg)
          }
        }

        if (typeof config.onSuccess === 'function') {
          config.onSuccess(res.data)
        }
      } catch (err) {
        logger.error('Success handler failed:', err)
      }

      return res.data
    }

    try {
      if (res.code === '401') {
        handleTokenExpired()
      } else if (config.errorMsg) {
        ElMessage.error(config.errorMsg)
      } else if (showDefaultMsg) {
        ElMessage.error(getBusinessErrorMessage(res.code, res.msg))
      }

      if (typeof config.onError === 'function') {
        config.onError(res)
      }
    } catch (err) {
      logger.error('Business error handler failed:', err)
      return Promise.reject(err)
    }

    return Promise.reject({
      code: res.code,
      message: res.msg || '请求失败',
      data: res.data,
      type: 'business'
    })
  },
  error => {
    const config = error.config || {}

    if (typeof config.onError === 'function') {
      try {
        config.onError(error)
      } catch (err) {
        logger.error('HTTP error callback failed:', err)
      }
    }

    if (error.response?.status === 401) {
      handleTokenExpired()
    } else if (config.showDefaultMsg !== false) {
      ElMessage({
        message: getHttpErrorMessage(error, config),
        type: 'error',
        duration: 5000,
        showClose: true
      })
    }

    return Promise.reject({
      code: error.response?.status,
      message: error.message,
      data: error.response?.data,
      type: 'http',
      originalError: error
    })
  }
)

const request = {
  get(url, params, config = {}) {
    if (params == null) {
      return service.get(url, config)
    }
    return service.get(url, { params, ...config })
  },

  post(url, data, config = {}) {
    return service.post(url, data, config)
  },

  put(url, data, config = {}) {
    return service.put(url, data, config)
  },

  delete(url, config = {}) {
    return service.delete(url, config)
  },

  upload(url, formData, config = {}) {
    return service.post(url, formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
        'X-Requested-With': 'XMLHttpRequest'
      },
      ...config
    })
  },

  download(url, params, config = {}) {
    return service.get(url, {
      params,
      responseType: 'blob',
      ...config
    })
  }
}

export default request
