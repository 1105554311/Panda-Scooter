<script setup>
import { computed } from 'vue'

const props = defineProps({
  page: {
    type: Number,
    default: 1
  },
  pageSize: {
    type: Number,
    default: 10
  },
  total: {
    type: Number,
    default: 0
  },
  loading: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['update:page', 'update:pageSize'])

const pageSizeOptions = [10, 20, 50]

const resolvedPageSize = computed(() => {
  return props.pageSize > 0 ? props.pageSize : 10
})

const totalPages = computed(() => {
  return Math.max(1, Math.ceil(props.total / resolvedPageSize.value))
})

const goPrev = () => {
  if (props.page <= 1 || props.loading) {
    return
  }

  emit('update:page', props.page - 1)
}

const goNext = () => {
  if (props.page >= totalPages.value || props.loading) {
    return
  }

  emit('update:page', props.page + 1)
}
</script>

<template>
  <div class="pagination-bar">
    <div class="pagination-info">
      <strong>{{ total }}</strong>
      <span>条记录</span>
      <span>第 {{ page }} / {{ totalPages }} 页</span>
    </div>

    <div class="pagination-controls">
      <label class="pagination-size">
        <span>每页</span>
        <select
          :value="resolvedPageSize"
          class="field-select"
          @change="emit('update:pageSize', Number($event.target.value))"
        >
          <option v-for="item in pageSizeOptions" :key="item" :value="item">{{ item }}</option>
        </select>
      </label>

      <div class="button-row">
        <button type="button" class="button-secondary" :disabled="page <= 1 || loading" @click="goPrev">
          上一页
        </button>
        <button
          type="button"
          class="button-secondary"
          :disabled="page >= totalPages || loading"
          @click="goNext"
        >
          下一页
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.pagination-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding-top: 20px;
  border-top: 1px solid #e5e5e2;
}

.pagination-info {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  color: #6b7280;
  font-size: 14px;
}

.pagination-controls {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.pagination-size {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: #6b7280;
  font-size: 14px;
}

.pagination-size :deep(.field-select) {
  width: 84px;
  min-height: 40px;
}

@media (max-width: 720px) {
  .pagination-bar {
    flex-direction: column;
    align-items: stretch;
  }

  .pagination-controls {
    justify-content: space-between;
  }
}
</style>
