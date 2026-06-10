import { ref } from 'vue'
import { ElMessageBox } from 'element-plus'

const normalize = (value) => JSON.stringify(value ?? null)

export function createUnsavedDialogGuard(getState, visibleRef) {
  const snapshot = ref('')

  const markPristine = () => {
    snapshot.value = normalize(getState())
  }

  const clearSnapshot = () => {
    snapshot.value = ''
  }

  const isDirty = () => {
    return Boolean(visibleRef?.value && snapshot.value && snapshot.value !== normalize(getState()))
  }

  const confirmDiscard = async () => {
    if (!isDirty()) return true
    return ElMessageBox.confirm('当前修改尚未保存，是否放弃这些修改？', '未保存修改', {
      confirmButtonText: '放弃修改',
      cancelButtonText: '继续编辑',
      type: 'warning',
      distinguishCancelAndClose: true
    }).then(() => true).catch(() => false)
  }

  const requestClose = async () => {
    if (await confirmDiscard()) {
      clearSnapshot()
      visibleRef.value = false
      return true
    }
    return false
  }

  const beforeClose = async (done) => {
    if (await confirmDiscard()) {
      clearSnapshot()
      done()
    }
  }

  const closeAfterSave = () => {
    markPristine()
    clearSnapshot()
    visibleRef.value = false
  }

  return {
    markPristine,
    clearSnapshot,
    isDirty,
    requestClose,
    beforeClose,
    closeAfterSave
  }
}
