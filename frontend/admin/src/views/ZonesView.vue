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

const router = useRouter()
const uiStore = useUiStore()

const filters = ref({
  keyword: '',
  dispatcherId: '',
  page: 1,
  pageSize: 10
})

const zones = ref([])
const total = ref(0)
const loading = ref(false)
const dispatchers = ref([])
const CACHE_SCOPE = 'zone'

const dispatcherMapByArea = computed(() => {
  return dispatchers.value.reduce((result, item) => {
    if (item.areaId) {
      result[item.areaId] = item
    }
    return result
  }, {})
})

const assignedZoneCount = computed(() => {
  return zones.value.filter((item) => dispatcherMapByArea.value[item.id]).length
})

const formatCenterText = (polygon) => {
  return formatLatLngCenterTextFromRawPolygon(polygon)
}

const fetchDispatchers = async () => {
  try {
    const response = await getDispatcherList({
      page: 1,
      pageSize: 100
    })

    dispatchers.value = response.data?.dispatcherList || []
  } catch (error) {
    dispatchers.value = []
  }
}

const fetchZones = async () => {
  loading.value = true

  try {
    const response = await getZoneList({
      page: filters.value.page,
      pageSize: filters.value.pageSize,
      keyword: filters.value.keyword || undefined,
      dispatcherId: filters.value.dispatcherId ? Number(filters.value.dispatcherId) : undefined
    })

    const data = response.data || {}
    const list = Array.isArray(data.areaList) ? data.areaList : []

    zones.value = list
    total.value = Number(data.total ?? list.length)
  } catch (error) {
    zones.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
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

    await Promise.all([fetchZones(), fetchDispatchers()])
  } catch (error) {
  }
}

const applyFilters = async () => {
  filters.value.page = 1
  await fetchZones()
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
    fetchZones()
  }
)

watch(
  () => [filters.value.keyword, filters.value.dispatcherId],
  () => {
    if (filters.value.page !== 1) {
      filters.value.page = 1
      return
    }

    fetchZones()
  }
)
onMounted(async () => {
  await Promise.all([fetchZones(), fetchDispatchers()])
})
</script>

<template>
  <div class="section-grid">
    <section class="metric-grid">
      <article class="card-surface stat-mini">
        <span>当前页片区数</span>
        <strong>{{ zones.length }}</strong>
      </article>
      <article class="card-surface stat-mini">
        <span>已分配调度员</span>
        <strong>{{ assignedZoneCount }}</strong>
      </article>
      <article class="card-surface stat-mini">
        <span>未分配调度员</span>
        <strong>{{ zones.length - assignedZoneCount }}</strong>
      </article>
      <article class="card-surface stat-mini">
        <span>总记录数</span>
        <strong>{{ total }}</strong>
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
              <td>{{ dispatcherMapByArea[item.id]?.name || '未分配' }}</td>
              <td>{{ formatDateTime(item.createTime) }}</td>
              <td>
                <div class="inline-actions">
                  <button
                    type="button"
                    class="button-secondary"
                    @click="
                      setEditorCache(CACHE_SCOPE, item);
                      router.push({ name: 'zone-detail', params: { id: item.id } });
                    "
                  >
                    详情
                  </button>
                  <button
                    type="button"
                    class="button-secondary"
                    @click="
                      setEditorCache(CACHE_SCOPE, item);
                      router.push({ name: 'zone-edit', params: { id: item.id } });
                    "
                  >
                    编辑
                  </button>
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
