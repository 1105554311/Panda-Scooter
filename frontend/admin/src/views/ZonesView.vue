<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import PaginationBar from '@/components/PaginationBar.vue'
import { deleteZone, getDispatcherList, getZoneList } from '@/api'
import { useUiStore } from '@/stores/ui'
import { formatDateTime } from '@/utils/format'
import { setEditorCache } from '@/utils/editorCache'
import { countPolygonPoints } from '@/utils/polygon'
import { formatLatLngCenterTextFromRawPolygon } from '@/utils/noParkingPolygon'
import { formatZoneDispatcherDisplay, hasDispatcherInZone, withZoneDispatchers } from '@/utils/zoneDispatchers'

const router = useRouter()
const uiStore = useUiStore()

const UNASSIGNED_FILTER = '__UNASSIGNED__'
const FULL_FETCH_PAGE_SIZE = 200
const MAX_FETCH_PAGES = 200
const CACHE_SCOPE = 'zone'

const filters = ref({
  keyword: '',
  dispatcherId: '',
  page: 1,
  pageSize: 10
})

const zones = ref([])
const rawAllZones = ref([])
const filteredZones = ref([])
const total = ref(0)
const loading = ref(false)
const dispatchers = ref([])

const normalizePositiveId = (value) => {
  const num = Number(value)
  return Number.isFinite(num) && num > 0 ? num : null
}

const dispatcherMapByArea = computed(() => {
  return dispatchers.value.reduce((result, item) => {
    const areaId = normalizePositiveId(item.areaId)
    if (!areaId) {
      return result
    }

    if (!result[areaId]) {
      result[areaId] = []
    }

    result[areaId].push(item)
    return result
  }, {})
})

const normalizeZoneList = (list = []) => {
  return list.map((item) => withZoneDispatchers(item, { fallbackByArea: dispatcherMapByArea.value }))
}

const totalZoneCount = computed(() => rawAllZones.value.length)

const assignedZoneCount = computed(() => {
  return rawAllZones.value.filter((item) => item.dispatchers.length > 0).length
})

const unassignedZoneCount = computed(() => {
  return Math.max(totalZoneCount.value - assignedZoneCount.value, 0)
})

const formatCenterText = (polygon) => {
  return formatLatLngCenterTextFromRawPolygon(polygon)
}

const fetchAllItems = async (requester, listKey) => {
  let page = 1
  let apiTotal = null
  const merged = []

  while (page <= MAX_FETCH_PAGES) {
    const response = await requester({
      page,
      pageSize: FULL_FETCH_PAGE_SIZE
    })

    const data = response.data || {}
    const list = Array.isArray(data[listKey]) ? data[listKey] : []
    const totalFromApi = Number(data.total)

    if (Number.isFinite(totalFromApi) && totalFromApi >= 0) {
      apiTotal = totalFromApi
    }

    merged.push(...list)

    if (!list.length) {
      break
    }

    if (apiTotal !== null && merged.length >= apiTotal) {
      break
    }

    if (list.length < FULL_FETCH_PAGE_SIZE) {
      break
    }

    page += 1
  }

  return merged
}

const fetchDispatchers = async () => {
  try {
    dispatchers.value = await fetchAllItems(getDispatcherList, 'dispatcherList')
  } catch (error) {
    dispatchers.value = []
  }
}

const fetchZones = async () => {
  try {
    const list = await fetchAllItems(getZoneList, 'areaList')
    rawAllZones.value = normalizeZoneList(list)
  } catch (error) {
    rawAllZones.value = []
  }
}

const syncPagedZones = () => {
  const pageSize = Number(filters.value.pageSize) > 0 ? Number(filters.value.pageSize) : 10
  const maxPage = Math.max(1, Math.ceil(filteredZones.value.length / pageSize))
  const normalizedPage = Number(filters.value.page) > 0 ? Number(filters.value.page) : 1
  const currentPage = Math.min(normalizedPage, maxPage)

  if (currentPage !== normalizedPage) {
    filters.value.page = currentPage
    return
  }

  const start = (currentPage - 1) * pageSize
  zones.value = filteredZones.value.slice(start, start + pageSize)
  total.value = filteredZones.value.length
}

const applyLocalFilters = () => {
  const keyword = filters.value.keyword.trim().toLowerCase()
  const selectedDispatcherId = filters.value.dispatcherId

  let next = rawAllZones.value.slice()

  if (keyword) {
    next = next.filter((item) => {
      return String(item.name || '').toLowerCase().includes(keyword)
    })
  }

  if (selectedDispatcherId === UNASSIGNED_FILTER) {
    next = next.filter((item) => {
      return item.dispatchers.length === 0
    })
  } else if (selectedDispatcherId) {
    const selectedId = normalizePositiveId(selectedDispatcherId)
    next = selectedId ? next.filter((item) => hasDispatcherInZone(item, selectedId)) : []
  }

  filteredZones.value = next
  syncPagedZones()
}

const fetchAllData = async () => {
  loading.value = true

  try {
    await fetchDispatchers()
    await fetchZones()
    applyLocalFilters()
  } finally {
    loading.value = false
  }
}

const formatDispatcherText = (item) => {
  return formatZoneDispatcherDisplay(item, { fallbackByArea: dispatcherMapByArea.value })
}

const removeItem = async (item) => {
  const confirmed = await uiStore.confirmAction({
    title: '删除片区',
    message: `确认删除片区“${item.name}”吗？`,
    confirmText: '删除',
    tone: 'danger'
  })

  if (!confirmed) {
    return
  }

  try {
    await deleteZone({
      areaId: item.id,
      name: item.name
    })

    uiStore.pushToast({
      message: '片区已删除',
      tone: 'success'
    })

    await fetchAllData()
  } catch (error) {
  }
}

const applyFilters = () => {
  filters.value.page = 1
  applyLocalFilters()
}

const openZoneDetail = (item) => {
  setEditorCache(CACHE_SCOPE, item)
  router.push({
    name: 'zone-detail',
    params: { id: item.id },
    query: { id: item.id }
  })
}

const openZoneEdit = (item) => {
  setEditorCache(CACHE_SCOPE, item)
  router.push({
    name: 'zone-edit',
    params: { id: item.id },
    query: { id: item.id }
  })
}

watch(
  () => [filters.value.page, filters.value.pageSize],
  () => {
    syncPagedZones()
  }
)

watch(
  () => [filters.value.keyword, filters.value.dispatcherId],
  () => {
    if (filters.value.page !== 1) {
      filters.value.page = 1
      return
    }

    applyLocalFilters()
  }
)

onMounted(async () => {
  await fetchAllData()
})
</script>

<template>
  <div class="section-grid">
    <section class="metric-grid">
      <article class="card-surface stat-mini">
        <span>总片区数</span>
        <strong>{{ totalZoneCount }}</strong>
      </article>
      <article class="card-surface stat-mini">
        <span>已分配调度员的片区</span>
        <strong>{{ assignedZoneCount }}</strong>
      </article>
      <article class="card-surface stat-mini">
        <span>未分配调度员的片区数</span>
        <strong>{{ unassignedZoneCount }}</strong>
      </article>
    </section>

    <section class="page-surface page-panel">
      <div class="panel-header">
        <div>
          <h2 class="panel-title">片区列表</h2>
        </div>
        <div class="button-row">
          <button type="button" class="button-primary" @click="router.push({ name: 'zone-create' })">新增片区</button>
        </div>
      </div>

      <div class="toolbar">
        <label class="form-field toolbar-grow">
          <span class="field-label">关键字</span>
          <input
            v-model.trim="filters.keyword"
            class="field-input"
            type="text"
            placeholder="搜索片区名称"
            @keydown.enter.prevent="applyFilters"
          />
        </label>

        <label class="form-field toolbar-grow">
          <span class="field-label">调度员</span>
          <select v-model="filters.dispatcherId" class="field-select">
            <option value="">全部调度员</option>
            <option :value="UNASSIGNED_FILTER">未分配</option>
            <option v-for="item in dispatchers" :key="item.id" :value="String(item.id)">{{ item.name }}</option>
          </select>
        </label>
      </div>

      <div class="responsive-table">
        <table class="data-table">
          <thead>
            <tr>
              <th>ID</th>
              <th>片区名称</th>
              <th>顶点数</th>
              <th>中心点</th>
              <th>调度员</th>
              <th>创建时间</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody v-if="zones.length">
            <tr v-for="item in zones" :key="item.id">
              <td>#{{ item.id }}</td>
              <td><strong>{{ item.name }}</strong></td>
              <td>{{ countPolygonPoints(item.polygon) }}</td>
              <td>{{ formatCenterText(item.polygon) }}</td>
              <td>{{ formatDispatcherText(item) }}</td>
              <td>{{ formatDateTime(item.createTime) }}</td>
              <td>
                <div class="inline-actions">
                  <button type="button" class="button-secondary" @click="openZoneDetail(item)">详情</button>
                  <button type="button" class="button-secondary" @click="openZoneEdit(item)">编辑</button>
                  <button type="button" class="button-danger" @click="removeItem(item)">删除</button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>

        <div v-if="!zones.length && !loading" class="empty-state">暂无片区数据。</div>
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
