<template>
  <div class="symptom-input-wrapper">
    <div class="input-container">
      <el-input
        v-model="inputValue"
        :placeholder="placeholder"
        @input="handleInput"
        @keydown.down.prevent="moveSelection(1)"
        @keydown.up.prevent="moveSelection(-1)"
        @keydown.enter="handleEnter"
        @focus="handleFocus"
        @blur="handleBlur"
        class="symptom-input"
        :loading="isLoading"
      >
        <template #prefix>
          <Search class="input-icon" aria-hidden="true" />
        </template>
      </el-input>
    </div>

    <transition name="dropdown">
      <div v-if="showDropdown && suggestions.length" class="suggestions-dropdown" role="listbox">
        <div
          v-for="(suggestion, index) in suggestions"
          :key="index"
          class="suggestion-item"
          :class="{ 'is-selected': selectedIndex === index }"
          role="option"
          @mousedown.prevent="selectSuggestion(suggestion)"
          @mouseenter="selectedIndex = index"
        >
          <span class="suggestion-text" v-html="highlightMatch(suggestion)" />
          <ArrowRight class="suggestion-arrow" aria-hidden="true" />
        </div>
      </div>
    </transition>

    <transition name="fade">
      <div v-if="errorMessage" class="error-message" role="alert">
        <Warning class="error-icon" aria-hidden="true" />
        <span>{{ errorMessage }}</span>
      </div>
    </transition>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, onUnmounted } from 'vue'
import { Search, ArrowRight, Warning } from '@element-plus/icons-vue'
import { getSuggestions } from '@/services/medical/knowledgeGraph'

interface Props {
  modelValue?: string
  placeholder?: string
}

const props = withDefaults(defineProps<Props>(), {
  modelValue: '',
  placeholder: '请输入症状',
})

const emit = defineEmits<{
  'update:modelValue': [value: string]
  select: [value: string]
  submit: [value: string]
}>()

const inputValue = ref(props.modelValue)
const suggestions = ref<string[]>([])
const showDropdown = ref(false)
const isLoading = ref(false)
const selectedIndex = ref(-1)
const errorMessage = ref('')
const searchPrefix = ref('')
let debounceTimer: ReturnType<typeof setTimeout> | null = null

watch(
  () => props.modelValue,
  (newVal) => {
    inputValue.value = newVal
  }
)

const escapeHtml = (s: string) =>
  s.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')

const highlightMatch = (suggestion: string) => {
  const prefix = searchPrefix.value.trim()
  if (!prefix) return escapeHtml(suggestion)
  const lower = suggestion.toLowerCase()
  const idx = lower.indexOf(prefix.toLowerCase())
  if (idx < 0) return escapeHtml(suggestion)
  const before = escapeHtml(suggestion.slice(0, idx))
  const match = escapeHtml(suggestion.slice(idx, idx + prefix.length))
  const after = escapeHtml(suggestion.slice(idx + prefix.length))
  return `${before}<mark class="match-highlight">${match}</mark>${after}`
}

const moveSelection = (delta: number) => {
  if (!suggestions.value.length) return
  if (selectedIndex.value < 0) selectedIndex.value = 0
  else {
    selectedIndex.value = (selectedIndex.value + delta + suggestions.value.length) % suggestions.value.length
  }
}

const handleInput = () => {
  emit('update:modelValue', inputValue.value)
  errorMessage.value = ''
  searchPrefix.value = inputValue.value.trim()

  if (inputValue.value.trim().length >= 2) {
    if (debounceTimer) clearTimeout(debounceTimer)
    debounceTimer = setTimeout(() => {
      fetchSuggestions(inputValue.value.trim())
    }, 300)
  } else {
    suggestions.value = []
    showDropdown.value = false
  }
}

const fetchSuggestions = async (prefix: string) => {
  isLoading.value = true
  try {
    const result = await getSuggestions(prefix, 10)
    suggestions.value = result
    selectedIndex.value = -1
    showDropdown.value = result.length > 0
  } catch (error) {
    console.error('获取症状联想失败:', error)
    errorMessage.value = '获取症状建议失败，请稍后重试'
  } finally {
    isLoading.value = false
  }
}

const handleEnter = () => {
  if (selectedIndex.value >= 0 && suggestions.value.length) {
    const selected = suggestions.value[selectedIndex.value]
    if (selected) selectSuggestion(selected)
  } else if (inputValue.value.trim()) {
    emit('submit', inputValue.value.trim())
    showDropdown.value = false
  }
}

const handleFocus = () => {
  if (suggestions.value.length) showDropdown.value = true
}

const handleBlur = () => {
  setTimeout(() => {
    showDropdown.value = false
  }, 200)
}

const selectSuggestion = (suggestion: string) => {
  inputValue.value = suggestion
  emit('update:modelValue', suggestion)
  emit('select', suggestion)
  suggestions.value = []
  showDropdown.value = false
}

onUnmounted(() => {
  if (debounceTimer) clearTimeout(debounceTimer)
})
</script>

<style scoped>
.symptom-input-wrapper {
  position: relative;
  width: 100%;
}

.input-icon {
  color: var(--consult-text-muted);
}

.suggestions-dropdown {
  position: absolute;
  top: calc(100% + 4px);
  left: 0;
  right: 0;
  background: var(--consult-surface);
  border: 1px solid var(--consult-border);
  border-radius: var(--consult-radius);
  box-shadow: var(--consult-shadow-md);
  z-index: 100;
  overflow: hidden;
}

.suggestion-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 44px;
  padding: 10px 16px;
  cursor: pointer;
  transition: background-color var(--consult-transition);
}

.suggestion-item:hover,
.suggestion-item.is-selected {
  background: var(--consult-info-bg);
}

.suggestion-text {
  font-size: 14px;
  color: var(--consult-text-primary);
}

.suggestion-text :deep(.match-highlight) {
  background: var(--consult-info-bg);
  color: var(--consult-info);
  font-weight: 600;
  padding: 0 2px;
  border-radius: 2px;
}

.suggestion-arrow {
  font-size: 14px;
  color: var(--consult-text-muted);
}

.error-message {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 8px;
  padding: 8px 12px;
  background: var(--consult-danger-bg);
  color: var(--consult-danger);
  font-size: 12px;
  border-radius: var(--consult-radius);
}

.dropdown-enter-active,
.dropdown-leave-active {
  transition: all var(--consult-transition);
}

.dropdown-enter-from,
.dropdown-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}
</style>
