import request from '@/utils/request'

const DEFAULT_PLUGINS = ['AMap.Geocoder', 'AMap.PlaceSearch', 'AMap.ToolBar', 'AMap.Scale']

let configPromise = null
let scriptPromise = null

const normalizePlugins = (plugins = []) => {
  const list = Array.isArray(plugins) ? plugins : [plugins]
  return [...new Set(list.filter(Boolean))]
}

const fetchAmapConfig = async () => {
  if (!configPromise) {
    configPromise = request.get('/scenic/amap/config', null, {
      showDefaultMsg: false
    }).catch(error => {
      configPromise = null
      throw error
    })
  }
  return configPromise
}

const loadPlugins = (plugins = []) => new Promise((resolve, reject) => {
  const amap = window.AMap
  if (!amap) {
    reject(new Error('AMap is not available'))
    return
  }

  const pluginList = normalizePlugins(plugins)
  if (pluginList.length === 0) {
    resolve(amap)
    return
  }

  amap.plugin(pluginList, () => resolve(amap))
})

const appendAmapScript = (webJsKey) => {
  if (window.AMap) {
    return Promise.resolve(window.AMap)
  }

  if (scriptPromise) {
    return scriptPromise
  }

  const existingScript = document.querySelector('script[data-amap-loader="true"]')
  if (existingScript) {
    scriptPromise = new Promise((resolve, reject) => {
      existingScript.addEventListener('load', () => resolve(window.AMap), { once: true })
      existingScript.addEventListener('error', reject, { once: true })
    })
    return scriptPromise
  }

  scriptPromise = new Promise((resolve, reject) => {
    const script = document.createElement('script')
    script.type = 'text/javascript'
    script.async = true
    script.dataset.amapLoader = 'true'
    script.src = `https://webapi.amap.com/maps?v=2.0&key=${encodeURIComponent(webJsKey)}&plugin=${encodeURIComponent(DEFAULT_PLUGINS.join(','))}`
    script.onload = () => {
      if (window.AMap) {
        resolve(window.AMap)
      } else {
        reject(new Error('AMap script loaded without AMap object'))
      }
    }
    script.onerror = reject
    document.head.appendChild(script)
  }).catch(error => {
    scriptPromise = null
    throw error
  })

  return scriptPromise
}

export const loadAmap = async (plugins = []) => {
  const config = await fetchAmapConfig()
  if (!config?.enabled || !config.webJsKey) {
    throw new Error('高德地图配置未完成')
  }

  if (config.securityCode) {
    window._AMapSecurityConfig = {
      securityJsCode: config.securityCode
    }
  }

  await appendAmapScript(config.webJsKey)
  return loadPlugins(plugins)
}
