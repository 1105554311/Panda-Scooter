<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import BaseModal from '@/components/BaseModal.vue'
import PaginationBar from '@/components/PaginationBar.vue'
import { addPackage, deletePackage, editPackage, getPackageList } from '@/api'
import { useUiStore } from '@/stores/ui'
import { formatCurrency, formatDateTime, formatPackageType } from '@/utils/format'

const uiStore = useUiStore()

const createDefaultFilters = () => ({
  keyword: '',
  type: '',
  page: 1,
  pageSize: 10
})

const createDefaultForm = () => ({
  id: '',
  title: '',
  description: '',
  price: '',
  type: 1
})

const filters = ref(createDefaultFilters())
const allPackages = ref([])
const loading = ref(false)
const saving = ref(false)
const modalOpen = ref(false)
const editing = ref(false)
const form = ref(createDefaultForm())

const totalCount = computed(() => allPackages.value.length)

const monthCount = computed(() => {
  return allPackages.value.filter((item) => Number(item.type) === 1).length
})

const quarterCount = computed(() => {
  return allPackages.value.filter((item) => Number(item.type) === 2).length
})

const yearCount = computed(() => {
  return allPackages.value.filter((item) => Number(item.type) === 3).length
})

const filteredPackages = computed(() => {
  if (!filters.value.type) {
    return allPackages.value
  }

  const type = Number(filters.value.type)
  return allPackages.value.filter((item) => Number(item.type) === type)
})

const pagedPackages = computed(() => {
  const page = Math.max(1, Number(filters.value.page) || 1)
  const pageSize = Math.max(1, Number(filters.value.pageSize) || 10)
  const start = (page - 1) * pageSize
  return filteredPackages.value.slice(start, start + pageSize)
})

const listTotal = computed(() => filteredPackages.value.length)

const fetchPackages = async () => {
  loading.value = true

  try {
    const keyword = filters.value.keyword || undefined
    const pageSize = 100
    let page = 1
    let total = 0
    const records = []

    do {
      const response = await getPackageList({
        page,
        pageSize,
        keyword
      })

      const data = response.data || {}
      const list = Array.isArray(data.list) ? data.list : []
      total = Number(data.total ?? 0)

      records.push(...list)
      page += 1

      if (!list.length) {
        break
      }
    } while (records.length < total)

    allPackages.value = records
  } catch (error) {
    allPackages.value = []
  } finally {
    loading.value = false
  }
}

const ensurePageInRange = () => {
  const maxPage = Math.max(1, Math.ceil(listTotal.value / (Number(filters.value.pageSize) || 10)))
  if (filters.value.page > maxPage) {
    filters.value.page = maxPage
  }
}

const resetForm = () => {
  form.value = createDefaultForm()
}

const openCreate = () => {
  editing.value = false
  resetForm()
  modalOpen.value = true
}

const openEdit = (item) => {
  editing.value = true
  form.value = {
    id: item.id,
    title: item.title || '',
    description: item.description || '',
    price: item.price ?? '',
    type: Number(item.type || 1)
  }
  modalOpen.value = true
}

const submit = async () => {
  if (!form.value.title.trim()) {
    uiStore.pushToast({
      message: '请输入套餐标题',
      tone: 'warning'
    })
    return
  }

  const price = Number(form.value.price)
  if (!Number.isFinite(price) || price <= 0) {
    uiStore.pushToast({
      message: '请输入有效的套餐价格',
      tone: 'warning'
    })
    return
  }

  saving.value = true

  try {
    const payload = {
      title: form.value.title.trim(),
      description: form.value.description.trim(),
      price,
      type: Number(form.value.type || 1)
    }

    if (editing.value) {
      await editPackage({
        ...payload,
        id: Number(form.value.id),
        editTime: new Date().toISOString()
      })
    } else {
      await addPackage({
        ...payload,
        createTime: new Date().toISOString()
      })
    }

    uiStore.pushToast({
      message: editing.value ? '套餐已更新' : '套餐已创建',
      tone: 'success'
    })

    modalOpen.value = false
    await fetchPackages()
    ensurePageInRange()
  } finally {
    saving.value = false
  }
}

const removeItem = async (item) => {
  const confirmed = await uiStore.confirmAction({
    title: '删除套餐',
    message: `确认删除套餐“${item.title}”吗？`,
    confirmText: '删除',
    tone: 'danger'
  })

  if (!confirmed) {
    return
  }

  try {
    await deletePackage({
      packageId: item.id,
      title: item.title
    })

    uiStore.pushToast({
      message: '套餐已删除',
      tone: 'success'
    })

    await fetchPackages()
    ensurePageInRange()
  } catch (error) {
  }
}

const getDailyAveragePrice = (item) => {
  const price = Number(item?.price)
  const type = Number(item?.type)

  if (!Number.isFinite(price)) {
    return '--'
  }

  if (type === 1) {
    return formatCurrency(price / 30)
  }

  if (type === 2) {
    return formatCurrency(price / 90)
  }

  if (type === 3) {
    return formatCurrency(price / 365)
  }

  return '--'
}

const applyFilters = async () => {
  filters.value.page = 1
  await fetchPackages()
}

watch(
  () => [filters.value.page, filters.value.pageSize, listTotal.value],
  () => {
    ensurePageInRange()
  }
)

watch(
  () => filters.value.type,
  () => {
    filters.value.page = 1
  }
)

watch(
  () => filters.value.keyword,
  () => {
    if (filters.value.page !== 1) {
      filters.value.page = 1
      return
    }

    fetchPackages()
  }
)

onMounted(fetchPackages)
</script>

<template>
  <div class="section-grid">
    <section class="metric-grid">
      <article class="card-surface stat-mini">
        <span>总套餐数</span>
        <strong>{{ totalCount }}</strong>
      </article>
      <article class="card-surface stat-mini">
        <span>月卡数量</span>
        <strong>{{ monthCount }}</strong>
      </article>
      <article class="card-surface stat-mini">
        <span>季卡数量</span>
        <strong>{{ quarterCount }}</strong>
      </article>
      <article class="card-surface stat-mini">
        <span>年卡数量</span>
        <strong>{{ yearCount }}</strong>
      </article>
    </section>

    <section class="page-surface page-panel">
      <div class="panel-header">
        <div>
          <h2 class="panel-title">套餐列表</h2>
        </div>
        <div class="button-row">
          <button type="button" class="button-primary" @click="openCreate">新增套餐</button>
        </div>
      </div>

      <div class="toolbar">
        <label class="form-field toolbar-grow">
          <span class="field-label">关键词</span>
          <input
            v-model.trim="filters.keyword"
            class="field-input"
            type="text"
            placeholder="搜索套餐标题或描述"
            @keydown.enter.prevent="applyFilters"
          />
        </label>

        <label class="form-field">
          <span class="field-label">套餐类型</span>
          <select v-model="filters.type" class="field-select">
            <option value="">全部</option>
            <option value="1">月卡</option>
            <option value="2">季卡</option>
            <option value="3">年卡</option>
          </select>
        </label>
      </div>

      <div class="responsive-table">
        <table class="data-table">
          <thead>
            <tr>
              <th>ID</th>
              <th>套餐</th>
              <th>类型</th>
              <th>价格</th>
              <th>日均价</th>
              <th>创建时间</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody v-if="pagedPackages.length">
            <tr v-for="item in pagedPackages" :key="item.id">
              <td>#{{ item.id }}</td>
              <td>
                <strong>{{ item.title }}</strong>
                <p class="row-subtext">{{ item.description || '暂无描述' }}</p>
              </td>
              <td>{{ formatPackageType(item.type) }}</td>
              <td>{{ formatCurrency(item.price) }}</td>
              <td>{{ getDailyAveragePrice(item) }}</td>
              <td>{{ formatDateTime(item.createTime) }}</td>
              <td>
                <div class="inline-actions">
                  <button type="button" class="button-secondary" @click="openEdit(item)">编辑</button>
                  <button type="button" class="button-danger" @click="removeItem(item)">删除</button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>

        <div v-if="!pagedPackages.length && !loading" class="empty-state">暂无套餐数据。</div>
      </div>

      <PaginationBar
        :page="filters.page"
        :page-size="filters.pageSize"
        :total="listTotal"
        :loading="loading"
        @update:page="filters.page = $event"
        @update:page-size="
          filters.pageSize = $event;
          filters.page = 1;
        "
      />
    </section>

    <BaseModal
      v-model="modalOpen"
      :title="editing ? '编辑套餐' : '新增套餐'"
      width="760px"
    >
      <div class="form-grid">
        <label class="form-field span-6">
          <span class="field-label">套餐标题</span>
          <input v-model.trim="form.title" class="field-input" type="text" placeholder="例如：月卡套餐" />
        </label>

        <label class="form-field span-3">
          <span class="field-label">套餐类型</span>
          <select v-model="form.type" class="field-select">
            <option :value="1">月卡</option>
            <option :value="2">季卡</option>
            <option :value="3">年卡</option>
          </select>
        </label>

        <label class="form-field span-3">
          <span class="field-label">价格</span>
          <input v-model="form.price" class="field-input" type="number" min="0" step="0.01" />
        </label>

        <label class="form-field span-12">
          <span class="field-label">套餐描述</span>
          <textarea
            v-model="form.description"
            class="field-textarea"
            placeholder="描述套餐权益、有效期或赠送分钟数"
          ></textarea>
        </label>
      </div>

      <template #footer>
        <div class="button-row">
          <button type="button" class="button-secondary" @click="modalOpen = false">取消</button>
          <button type="button" class="button-primary" :disabled="saving" @click="submit">
            {{ saving ? '保存中...' : editing ? '保存修改' : '创建套餐' }}
          </button>
        </div>
      </template>
    </BaseModal>
  </div>
</template>

<style scoped>
.page-panel {
  padding: 28px;
}

.stat-mini {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 20px;
}

.stat-mini span {
  color: #737373;
  font-size: 13px;
  letter-spacing: 0.08em;
}

.stat-mini strong {
  font-size: 32px;
  font-weight: 400;
}

.row-subtext {
  margin: 6px 0 0;
  color: #737373;
  font-size: 12px;
}

@media (max-width: 720px) {
  .page-panel {
    padding: 20px;
  }
}
</style>
