<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import PaginationBar from '@/components/PaginationBar.vue'
import { deleteNoParkingZone, getNoParkingZoneList } from '@/api'
import { useUiStore } from '@/stores/ui'
import { formatBinaryStatus, formatDateTime } from '@/utils/format'
import { setEditorCache } from '@/utils/editorCache'
import { countPolygonPoints } from '@/utils/polygon'
import { formatLatLngCenterTextFromRawPolygon } from '@/utils/noParkingPolygon'

const router = useRouter()
const uiStore = useUiStore()
const CACHE_SCOPE = 'no-parking-zone'

const filters = ref({
  keyword: '',
  page: 1,
  pageSize: 10
})

const zones = ref([])
const total = ref(0)
const totalVehicleCount = ref(0)
const loading = ref(false)

const enabledCount = computed(() => zones.value.filter((item) => Number(item.status) === 1).length)
const disabledCount = computed(() => zones.value.filter((item) => Number(item.status) === 0).length)

const formatCenterText = (polygon) => {
  return formatLatLngCenterTextFromRawPolygon(polygon)
}

const fetchZones = async () => {
  loading.value = true

  try {
    const response = await getNoParkingZoneList({
      page: filters.value.page,
      pageSize: filters.value.pageSize,
      keyword: filters.value.keyword || undefined
    })

    const data = response.data || {}
    const list = Array.isArray(data.areaList) ? data.areaList : []

    zones.value = list
    total.value = Number(data.total ?? list.length)
    totalVehicleCount.value = Number(data.vehicleCount || 0)
  } catch (error) {
    zones.value = []
    total.value = 0
    totalVehicleCount.value = 0
  } finally {
    loading.value = false
  }
}

const openEdit = (item) => {
  setEditorCache(CACHE_SCOPE, item)
  router.push({ name: 'no-parking-zone-edit', params: { id: item.id } })
}

const removeItem = async (item) => {
  const confirmed = await uiStore.confirmAction({
    title: '删除禁停区',
    message: `确认删除禁停区“${item.name}”吗？`,
    confirmText: '删除',
    tone: 'danger'
  })

  if (!confirmed) {
    return
  }

  try {
    await deleteNoParkingZone({
      NoParkingAreaId: item.id,
      name: item.name
    })

    uiStore.pushToast({
      message: '禁停区已删除',
      tone: 'success'
    })

    await fetchZones()
  } catch (error) {
  }
}

const applyFilters = async () => {
  filters.value.page = 1
  await fetchZones()
}

watch(
  () => [filters.value.page, filters.value.pageSize],
  () => {
    fetchZones()
  }
)

watch(
  () => [filters.value.keyword],
  () => {
    if (filters.value.page !== 1) {
      filters.value.page = 1
      return
    }

    fetchZones()
  }
)
onMounted(fetchZones)
</script>

<template>
  <div class="section-grid">
    <section class="metric-grid">
      <article class="card-surface stat-mini">
        <span>当前页禁停区数</span>
        <strong>{{ zones.length }}</strong>
      </article>
      <article class="card-surface stat-mini">
        <span>启用禁停区</span>
        <strong>{{ enabledCount }}</strong>
      </article>
      <article class="card-surface stat-mini">
        <span>停用禁停区</span>
        <strong>{{ disabledCount }}</strong>
      </article>
      <article class="card-surface stat-mini">
        <span>覆盖车辆数</span>
        <strong>{{ totalVehicleCount }}</strong>
      </article>
    </section>

    <section class="page-surface page-panel">
      <div class="panel-header">
        <div>
          <h2 class="panel-title">禁停区列表</h2>
        </div>
        <div class="button-row">
          <button type="button" class="button-primary" @click="router.push({ name: 'no-parking-zone-create' })">
            新增禁停区
          </button>
        </div>
      </div>

      <div class="toolbar">
        <label class="form-field toolbar-grow">
          <span class="field-label">关键字</span>
          <input
            v-model.trim="filters.keyword"
            class="field-input"
            type="text"
            placeholder="搜索禁停区名称"
            @keydown.enter.prevent="applyFilters"
          />
        </label>
        </div>

      <div class="responsive-table">
        <table class="data-table">
          <thead>
            <tr>
              <th>ID</th>
              <th>名称</th>
              <th>状态</th>
              <th>顶点数</th>
              <th>中心点</th>
              <th>创建时间</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody v-if="zones.length">
            <tr v-for="item in zones" :key="item.id">
              <td>#{{ item.id }}</td>
              <td>{{ item.name || '--' }}</td>
              <td>
                <span class="status-pill" :class="formatBinaryStatus(item.status).tone">
                  {{ formatBinaryStatus(item.status).text }}
                </span>
              </td>
              <td>{{ countPolygonPoints(item.polygon) }}</td>
              <td>{{ formatCenterText(item.polygon) }}</td>
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

        <div v-if="!zones.length && !loading" class="empty-state">暂无禁停区数据。</div>
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
