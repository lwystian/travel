<template>
  <div class="rich-markdown-editor">
    <div class="editor-mode-bar">
      <el-radio-group v-model="mode" size="small">
        <el-radio-button label="rich">富文本</el-radio-button>
        <el-radio-button label="markdown">Markdown</el-radio-button>
      </el-radio-group>
    </div>

    <wang-editor
      v-if="mode === 'rich'"
      v-model="innerValue"
      :height="height"
      :placeholder="placeholder"
      @change="emitRichValue"
    />

    <div v-else class="markdown-layout">
      <el-input
        v-model="markdownValue"
        type="textarea"
        :rows="markdownRows"
        :placeholder="markdownPlaceholder"
        @input="emitMarkdownValue"
      />
      <div class="markdown-preview content-display" v-html="markdownPreview"></div>
    </div>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import WangEditor from '@/components/WangEditor.vue'
import {
  decodeMarkdownContent,
  encodeMarkdownContent,
  isMarkdownContent,
  renderContent,
  renderMarkdown
} from '@/utils/contentRenderer'

const props = defineProps({
  modelValue: { type: String, default: '' },
  height: { type: String, default: '360px' },
  placeholder: { type: String, default: '请输入内容' },
  markdownRows: { type: Number, default: 14 }
})

const emit = defineEmits(['update:modelValue', 'change'])

const initialMarkdown = isMarkdownContent(props.modelValue)
const mode = ref(initialMarkdown ? 'markdown' : 'rich')
const innerValue = ref(initialMarkdown ? renderContent(props.modelValue) : (props.modelValue || ''))
const markdownValue = ref(initialMarkdown ? decodeMarkdownContent(props.modelValue) : '')

const markdownPlaceholder = computed(() => `${props.placeholder}\n\n支持标题、列表、表格、图片、链接、引用、代码块等常用 Markdown。`)
const markdownPreview = computed(() => renderContent(encodeMarkdownContent(markdownValue.value)))

watch(() => props.modelValue, (value) => {
  if (isMarkdownContent(value)) {
    const decoded = decodeMarkdownContent(value)
    mode.value = 'markdown'
    if (decoded !== markdownValue.value) markdownValue.value = decoded
    innerValue.value = renderContent(value)
    return
  }
  if (mode.value === 'rich' && value !== innerValue.value) {
    innerValue.value = value || ''
  }
})

watch(mode, (value) => {
  if (value === 'markdown') {
    if (!markdownValue.value) {
      markdownValue.value = htmlToPlainText(innerValue.value)
    }
    emitMarkdownValue()
  } else {
    innerValue.value = renderMarkdown(markdownValue.value)
    emitRichValue(innerValue.value)
  }
})

const htmlToPlainText = (html) => String(html || '')
  .replace(/<br\s*\/?>/gi, '\n')
  .replace(/<\/p>/gi, '\n')
  .replace(/<\/h[1-6]>/gi, '\n')
  .replace(/<li>/gi, '- ')
  .replace(/<\/li>/gi, '\n')
  .replace(/<[^>]+>/g, '')
  .replace(/&nbsp;/g, ' ')
  .replace(/&amp;/g, '&')
  .replace(/&lt;/g, '<')
  .replace(/&gt;/g, '>')
  .trim()

const emitRichValue = (value) => {
  innerValue.value = value || ''
  emit('update:modelValue', innerValue.value)
  emit('change', innerValue.value)
}

const emitMarkdownValue = () => {
  const value = encodeMarkdownContent(markdownValue.value)
  innerValue.value = markdownPreview.value
  emit('update:modelValue', value)
  emit('change', value)
}
</script>

<style scoped>
.rich-markdown-editor {
  width: 100%;
}

.editor-mode-bar {
  margin-bottom: 10px;
}

.markdown-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  gap: 12px;
}

.markdown-preview {
  min-height: 300px;
  padding: 16px 18px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  background: #fff;
  color: #303133;
  line-height: 1.8;
  overflow: auto;
}

@media (max-width: 900px) {
  .markdown-layout {
    grid-template-columns: 1fr;
  }
}
</style>
