<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import BaseModal from '@/components/BaseModal.vue'
import PaginationBar from '@/components/PaginationBar.vue'
import {
  addDispatcher,
  deleteDispatcher,
  editDispatcher,
  getDispatcherList,
  getZoneList
} from '@/api'
import { useUiStore } from '@/stores/ui'
import { formatDateTime } from '@/utils/format'

const uiStore = useUiStore()

const createDefaultFilters = () => ({
  keyword: '',
  areaId: '',
  page: 1,
  pageSize: 10
})

const createDefaultForm = () => ({
  id: '',
  name: '',
  email: '',
  password: '',
  areaId: ''
})

const filters = ref(createDefaultFilters())
const dispatchers = ref([])
const areas = ref([])
const total = ref(0)
const loading = ref(false)
const saving = ref(false)
const modalOpen = ref(false)
const editing = ref(false)
const form = ref(createDefaultForm())

const areaMap = computed(() => {
  return areas.value.reduce((result, item) => {
    result[item.id] = item.name
    return result
  }, {})
})

const assignedCount = computed(() => {
  return dispatchers.value.filter((item) => item.areaId).length
})

const fetchAreas = async () => {
  try {
    const response = await getZoneList({
      page: 1,
      pageSize: 100
    })

    areas.value = response.data?.areaList || []
  } catch (error) {
    areas.value = []
  }
}

const fetchDispatchers = async () => {
  loading.value = true

  try {
    const response = await getDispatcherList({
      page: filters.value.page,
      pageSize: filters.value.pageSize,
      keyword: filters.value.keyword || undefined,
      areaId: filters.value.areaId ? Number(filters.value.areaId) : undefined
    })

    const data = response.data || {}
    const list = Array.isArray(data.dispatcherList) ? data.dispatcherList : []

    dispatchers.value = list
    total.value = Number(data.total ?? list.length)
  } catch (error) {
    dispatchers.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

const openCreate = () => {
  editing.value = false
  form.value = createDefaultForm()
  modalOpen.value = true
}

const openEdit = (item) => {
  editing.value = true
  form.value = {
    id: item.id,
    name: item.name || '',
    email: item.email || '',
    password: '',
    areaId: item.areaId ? String(item.areaId) : ''
  }
  modalOpen.value = true
}

const submit = async () => {
  if (!form.value.name.trim()) {
    uiStore.pushToast({
      message: '请输入调度员姓名',
      tone: 'warning'
    })
    return
  }

  if (!form.value.email.trim()) {
    uiStore.pushToast({
      message: '请输入调度员邮箱',
      tone: 'warning'
    })
    return
  }

  if (!form.value.password.trim()) {
    uiStore.pushToast({
      message: '请输入登录密码',
      tone: 'warning'
    })
    return
  }

  saving.value = true

  try {
    const payload = {
      name: form.value.name.trim(),
      email: form.value.email.trim(),
      password: form.value.password.trim(),
      areaId: form.value.areaId ? Number(form.value.areaId) : undefined
    }

    if (editing.value) {
      await editDispatcher({
        ...payload,
        id: Number(form.value.id)
      })
    } else {
      await addDispatcher(payload)
    }

    uiStore.pushToast({
      message: editing.value ? '调度员已更新' : '调度员已新增',
      tone: 'success'
    })

    modalOpen.value = false
    await fetchDispatchers()
  } catch (error) {
  } finally {
    saving.value = false
  }
}

const removeItem = async (item) => {
  const confirmed = await uiStore.confirmAction({
    title: '删除调度员',
    message: `确认删除调度员“${item.name}”吗？`,
    confirmText: '删除',
    tone: 'danger'
  })

  if (!confirmed) {
    return
  }

  try {
    await deleteDispatcher({
      dispatcherId: item.id,
      name: item.name,
      email: item.email
    })

    uiStore.pushToast({
      message: '调度员已删除',
      tone: 'success'
    })

    await fetchDispatchers()
  } catch (error) {
  }
}

const applyFilters = async () => {
  filters.value.page = 1
  await fetchDispatchers()
}

watch(
  () => [filters.value.page, filters.value.pageSize],
  () => {
    fetchDispatchers()
  }
)

watch(
  () => [filters.value.keyword, filters.value.areaId],
  () => {
    if (filters.value.page !== 1) {
      filters.value.page = 1
      return
    }

    fetchDispatchers()
  }
)
onMounted(async () => {
  await fetchAreas()
  await fetchDispatchers()
})
</script>

<template>
  <div class="section-grid">
    <section class="metric-grid">
      <article class="card-surface stat-mini">
        <span>当前页调度员数</span>
        <strong>{{ dispatchers.length }}</strong>
      </article>
      <article class="card-surface stat-mini">
        <span>已分配片区</span>
        <strong>{{ assignedCount }}</strong>
      </article>
      <article class="card-surface stat-mini">
        <span>未分配片区</span>
        <strong>{{ dispatchers.length - assignedCount }}</strong>
      </article>
      <article class="card-surface stat-mini">
        <span>总记录数</span>
        <strong>{{ total }}</strong>
      </article>
    </section>

    <section class="page-surface page-panel">
      <div class="panel-header">
        <div>
          <h2 class="panel-title">调度员列表</h2>
        </div>
        <div class="button-row">
          <button type="button" class="button-primary" @click="openCreate">新增调度员</button>
        </div>
      </div>

      <div class="toolbar">
        <label class="form-field toolbar-grow">
          <span class="field-label">关键字</span>
          <input
            v-model.trim="filters.keyword"
            class="field-input"
            type="text"
            placeholder="搜索姓名或邮箱"
            @keydown.enter.prevent="applyFilters"
          />
        </label>

        <label class="form-field toolbar-grow">
          <span class="field-label">片区</span>
          <select v-model="filters.areaId" class="field-select">
            <option value="">全部片区</option>
            <option v-for="item in areas" :key="item.id" :value="String(item.id)">{{ item.name }}</option>
          </select>
        </label>

        </div>

      <div class="responsive-table">
        <table class="data-table">
          <thead>
            <tr>
              <th>ID</th>
              <th>姓名</th>
              <th>邮箱</th>
              <th>所属片区</th>
              <th>创建时间</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody v-if="dispatchers.length">
            <tr v-for="item in dispatchers" :key="item.id">
              <td>#{{ item.id }}</td>
              <td>{{ item.name }}</td>
              <td>{{ item.email || '--' }}</td>
              <td>{{ areaMap[item.areaId] || (item.areaId ? `片区 ${item.areaId}` : '未分配') }}</td>
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

        <div v-if="!dispatchers.length && !loading" class="empty-state">暂无调度员数据。</div>
      </div>

      <PaginationBar
        :page="filters.page"
        :page-size="filters.pageSize"
        :total="total"
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
      :title="editing ? '编辑调度员' : '新增调度员'"
      width="760px"
    >
      <div class="form-grid">
        <label class="form-field span-6">
          <span class="field-label">姓名</span>
          <input v-model.trim="form.name" class="field-input" type="text" placeholder="请输入姓名" />
        </label>

        <label class="form-field span-6">
          <span class="field-label">邮箱</span>
          <input v-model.trim="form.email" class="field-input" type="email" placeholder="dispatcher@example.com" />
        </label>

        <label class="form-field span-6">
          <span class="field-label">登录密码</span>
          <input v-model="form.password" class="field-input" type="password" placeholder="请输入登录密码" />
        </label>

        <label class="form-field span-6">
          <span class="field-label">所属片区</span>
          <select v-model="form.areaId" class="field-select">
            <option value="">暂不分配</option>
            <option v-for="item in areas" :key="item.id" :value="String(item.id)">{{ item.name }}</option>
          </select>
        </label>
      </div>

      <template #footer>
        <div class="button-row">
          <button type="button" class="button-secondary" @click="modalOpen = false">取消</button>
          <button type="button" class="button-primary" :disabled="saving" @click="submit">
            {{ saving ? '保存中...' : editing ? '保存修改' : '创建调度员' }}
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

@media (max-width: 720px) {
  .page-panel {
    padding: 20px;
  }
}
</style>
