import { ref } from 'vue'
import request from '@/utils/request'

const enabled = ref(true)
const ready = ref(false)
let loaded = false
let loadingPromise = null

export const usePublicInteraction = () => {
  const loadPublicInteractionConfig = async () => {
    if (loaded) return enabled.value
    if (!loadingPromise) {
      loadingPromise = request.get('/admin-governance/public-interaction-config', null, {
        showDefaultMsg: false
      }).then(config => {
        enabled.value = config?.enabled !== false
        loaded = true
        ready.value = true
        return enabled.value
      }).catch(() => {
        enabled.value = true
        loaded = true
        ready.value = true
        return enabled.value
      }).finally(() => {
        loadingPromise = null
      })
    }
    return loadingPromise
  }

  return {
    publicInteractionEnabled: enabled,
    publicInteractionReady: ready,
    loadPublicInteractionConfig
  }
}
