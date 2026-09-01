<template>
  <el-input
    v-if="editMode && multiline"
    :model-value="modelValue"
    type="textarea"
    :rows="rows"
    :maxlength="normalizedMaxlength"
    show-word-limit
    class="inline-edit-input inline-edit-input--textarea"
    :placeholder="placeholder"
    @update:model-value="emit('update:modelValue', $event)"
  />
  <el-input
    v-else-if="editMode"
    :model-value="modelValue"
    :maxlength="normalizedMaxlength"
    class="inline-edit-input"
    :placeholder="placeholder"
    @update:model-value="emit('update:modelValue', $event)"
  />
  <component :is="tag" v-else>
    {{ modelValue }}
  </component>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  modelValue: {
    type: [String, Number],
    default: ''
  },
  editMode: {
    type: Boolean,
    default: false
  },
  tag: {
    type: String,
    default: 'span'
  },
  multiline: {
    type: Boolean,
    default: false
  },
  rows: {
    type: Number,
    default: 2
  },
  maxlength: {
    type: [Number, String],
    default: 220
  },
  placeholder: {
    type: String,
    default: '请输入内容'
  }
})

const normalizedMaxlength = computed(() => {
  const value = Number(props.maxlength)
  return Number.isFinite(value) && value > 0 ? Math.floor(value) : 220
})

const emit = defineEmits(['update:modelValue'])
</script>

<style scoped>
.inline-edit-input {
  width: 100%;
}

.inline-edit-input :deep(.el-input__wrapper),
.inline-edit-input :deep(.el-textarea__inner) {
  border-radius: 8px;
}

.inline-edit-input--textarea {
  display: block;
}
</style>
