import { getGeetestPublicConfig } from '@/api/auth'

let loadingPromise = null
const GEETEST_SCRIPT_ID = 'geetest-v3-script'
const GEETEST_SCRIPT_URL = 'https://static.geetest.com/static/tools/gt.js'
const SCRIPT_LOAD_TIMEOUT = 10000

export const loadGeetestScript = () => {
  if (window.initGeetest) {
    return Promise.resolve()
  }
  if (loadingPromise) {
    return loadingPromise
  }
  loadingPromise = new Promise((resolve, reject) => {
    let settled = false
    let script = document.getElementById(GEETEST_SCRIPT_ID)

    const cleanup = () => {
      clearTimeout(timer)
      if (script) {
        script.onload = null
        script.onerror = null
      }
    }
    const fail = error => {
      if (settled) return
      settled = true
      cleanup()
      loadingPromise = null
      reject(error)
    }
    const done = () => {
      if (settled) return
      if (!window.initGeetest) {
        fail(new Error('极验三代脚本加载异常，请刷新页面后重试'))
        return
      }
      settled = true
      cleanup()
      resolve()
    }
    const timer = setTimeout(() => {
      fail(new Error('极验三代脚本加载超时，请检查网络后重试'))
    }, SCRIPT_LOAD_TIMEOUT)

    if (!script) {
      script = document.createElement('script')
      script.id = GEETEST_SCRIPT_ID
      script.src = GEETEST_SCRIPT_URL
      script.async = true
      document.head.appendChild(script)
    }
    script.async = true
    script.onload = done
    script.onerror = () => fail(new Error('极验三代脚本加载失败，请检查网络后重试'))
  })
  return loadingPromise
}

export const normalizeGeetestValidate = (captchaId, result = {}) => ({
  captchaId,
  geetestChallenge: result.geetest_challenge,
  geetestValidate: result.geetest_validate,
  geetestSeccode: result.geetest_seccode
})

export const getGeetestConfig = () => getGeetestPublicConfig()

export const isGeetestReady = config => {
  return Boolean(config?.enabled && config?.configured && config?.captchaId && config?.challenge)
}

export const translateGeetestError = error => {
  return error?.msg || error?.message || '滑动验证失败，请重新验证'
}
