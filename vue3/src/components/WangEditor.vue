<template>
  <div class="wang-editor">
    <div ref="toolbarRef" class="toolbar-container"></div>
    <div ref="editorRef" class="editor-container content-display"></div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, watch, nextTick } from 'vue'
import '@wangeditor/editor/dist/css/style.css'
import '@/styles/content-display.css'

const props = defineProps({
  modelValue: {
    type: String,
    default: ''
  },
  height: {
    type: String,
    default: '500px'
  },
  placeholder: {
    type: String,
    default: '请输入内容...'
  },
  uploadImgServer: {
    type: String,
    default: '/api/file/upload/img'
  }
})

const emit = defineEmits(['update:modelValue', 'change'])

// 编辑器实例
const toolbarRef = ref(null)
const editorRef = ref(null)
const editorInstance = ref(null)
const toolbarInstance = ref(null)

// 创建编辑器
const createEditor = async () => {
  if (!window.wangEditor) {
    console.error('wangEditor 未加载')
    return
  }

  try {
    // 创建编辑器
    const editor = window.wangEditor.createEditor({
      selector: editorRef.value,
      html: props.modelValue || '',
      config: {
        placeholder: props.placeholder,
        // 设置默认样式
        editorConfig: {
          defaultFontSize: '16px',
          defaultFontFamily: 'Arial, sans-serif',
          defaultLineHeight: '1.5',
          defaultTextAlign: 'left', // 设置默认文本左对齐
        },
        MENU_CONF: {
          uploadImage: {
            server: props.uploadImgServer,
            fieldName: 'file',
            customInsert(res, insertFn) {
              const url = res.data
              insertFn(url.startsWith('/') ? `/api${url}` : `/api/${url}`)
            },
            onSuccess(file, res) {
              console.log('图片上传成功', res)
            },
            onFailed(file, res) {
              console.error('图片上传失败', res)
            },
            onError(file, err, res) {
              console.error('图片上传出错', err, res)
            }
          }
        },
        onChange() {
          const html = editor.getHtml()
          emit('update:modelValue', html)
          emit('change', html)
        }
      }
    })

    // 创建工具栏
    const toolbar = window.wangEditor.createToolbar({
      selector: toolbarRef.value,
      editor,
      mode: 'default',
      config: {}
    })

    editorInstance.value = editor
    toolbarInstance.value = toolbar

  } catch (error) {
    console.error('创建编辑器失败:', error)
  }
}

// 初始化编辑器
onMounted(async () => {
  try {
    // 动态引入wangeditor
    const wangEditor = await import('@wangeditor/editor')
    window.wangEditor = wangEditor
    
    // 等待DOM更新后创建编辑器
    nextTick(() => {
      createEditor()
    })
  } catch (error) {
    console.error('加载wangEditor失败:', error)
  }
})

// 监听内容变化
watch(() => props.modelValue, (newVal) => {
  if (editorInstance.value && newVal !== editorInstance.value.getHtml()) {
    editorInstance.value.setHtml(newVal || '')
  }
})

// 销毁编辑器
onBeforeUnmount(() => {
  if (editorInstance.value) {
    editorInstance.value.destroy()
    editorInstance.value = null
  }
  if (toolbarInstance.value) {
    toolbarInstance.value.destroy()
    toolbarInstance.value = null
  }
})

// 暴露方法
defineExpose({
  getEditorInstance: () => editorInstance.value
})
</script>

<style lang="scss" scoped>
.wang-editor {
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  overflow: hidden;
  
  .toolbar-container {
    border-bottom: 1px solid #dcdfe6;
  }
  
  .editor-container {
    width: 100%;
    min-height: v-bind('props.height');
  }
}

// 让编辑器正文复用前台展示排版，减少编辑态和展示态的样式差异。
:deep(.w-e-text-container) {
  .w-e-text {
    color: #303133;
    font-size: 15px;
    line-height: 1.85;
    text-align: left;
  }
}
</style>
