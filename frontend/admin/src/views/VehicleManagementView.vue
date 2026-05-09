<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import ScooterMapPreview from '@/components/ScooterMapPreview.vue'
import {
  ALL_MAP_LAYERS,
  MAP_LAYER_NO_PARKING,
  MAP_LAYER_PARKING_POINT,
  MAP_LAYER_SCOOTER,
  MAP_LAYER_ZONE
} from '@/utils/adminMapVisuals'
import { fetchAdminMapLayers } from '@/utils/adminMapLayers'
import { formatBinaryStatus, formatDateTime, formatNumber } from '@/utils/format'

const TYPE_ALL = 'all'
const TYPE_ZONE = MAP_LAYER_ZONE
const TYPE_NO_PARKING = MAP_LAYER_NO_PARKING
const TYPE_PARKING_POINT = MAP_LAYER_PARKING_POINT
const TYPE_SCOOTER = MAP_LAYER_SCOOTER

const router = useRouter()

const loadingLayers = ref(false)
const layers = ref({
  zones: [],
  noParkingZones: [],
  scooters: [],
  parkingPoints: []
})

const selectedType = ref(TYPE_ALL)
const selectedIdByType = ref({
  [TYPE_ZONE]: '',
  [TYPE_NO_PARKING]: '',
  [TYPE_PARKING_POINT]: '',
  [TYPE_SCOOTER]: ''
})
const selectedDetail = ref(null)
let skipTypeResetOnce = false

const isAllType = computed(() => selectedType.value === TYPE_ALL)

const typeOptions = [
  { value: TYPE_ALL, label: '全部' },
  { value: TYPE_ZONE, label: '片区' },
  { value: TYPE_NO_PARKING, label: '禁停区' },
  { value: TYPE_PARKING_POINT, label: '停车点' },
  { value: TYPE_SCOOTER, label: '车辆' }
]

const typeNameMap = {
  [TYPE_ALL]: '全部对象',
  [TYPE_ZONE]: '片区',
  [TYPE_NO_PARKING]: '禁停区',
  [TYPE_PARKING_POINT]: '停车点',
  [TYPE_SCOOTER]: '车辆'
}

const allOptionLabelMap = {
  [TYPE_ZONE]: '全部片区',
  [TYPE_NO_PARKING]: '全部禁停区',
  [TYPE_PARKING_POINT]: '全部停车点',
  [TYPE_SCOOTER]: '全部车辆'
}

const visibleTypes = computed(() => {
  if (isAllType.value) {
    return ALL_MAP_LAYERS.slice()
  }
  return [selectedType.value]
})

const itemListByType = computed(() => {
  return {
    [TYPE_ZONE]: layers.value.zones,
    [TYPE_NO_PARKING]: layers.value.noParkingZones,
    [TYPE_PARKING_POINT]: layers.value.parkingPoints,
    [TYPE_SCOOTER]: layers.value.scooters
  }
})

const currentList = computed(() => {
  if (isAllType.value) {
    return []
  }
  return itemListByType.value[selectedType.value] || []
})

const currentSelectedId = computed(() => {
  if (isAllType.value) {
    return ''
  }
  return selectedIdByType.value[selectedType.value] || ''
})

const currentSelectedItem = computed(() => {
  if (isAllType.value || !currentSelectedId.value) {
    return null
  }

  return currentList.value.find((item) => String(item.id) === String(currentSelectedId.value)) || null
})

const zoneSummary = computed(() => {
  const list = layers.value.zones
  const assigned = list.filter((item) => item.dispatcherName).length

  return {
    total: list.length,
    assigned,
    unassigned: list.length - assigned
  }
})

const noParkingSummary = computed(() => {
  const list = layers.value.noParkingZones
  const enabled = list.filter((item) => Number(item.status) === 1).length

  return {
    total: list.length,
    enabled,
    disabled: list.length - enabled
  }
})

const parkingPointSummary = computed(() => {
  const list = layers.value.parkingPoints
  const enabled = list.filter((item) => Number(item.status) === 1).length

  return {
    total: list.length,
    enabled,
    disabled: list.length - enabled
  }
})

const scooterSummary = computed(() => {
  const list = layers.value.scooters
  const online = list.filter(
    (item) => item.faultStatus !== 1 && Number.isFinite(item.longitude) && Number.isFinite(item.latitude)
  ).length
  const fault = list.filter((item) => Number(item.faultStatus) === 1).length

  return {
    total: list.length,
    online,
    fault
  }
})

const summaryByType = computed(() => {
  return {
    [TYPE_ZONE]: zoneSummary.value,
    [TYPE_NO_PARKING]: noParkingSummary.value,
    [TYPE_PARKING_POINT]: parkingPointSummary.value,
    [TYPE_SCOOTER]: scooterSummary.value
  }
})

const allSummary = computed(() => {
  const total =
    zoneSummary.value.total
    + noParkingSummary.value.total
    + parkingPointSummary.value.total
    + scooterSummary.value.total

  return {
    total,
    zoneTotal: zoneSummary.value.total,
    noParkingTotal: noParkingSummary.value.total,
    parkingPointTotal: parkingPointSummary.value.total,
    scooterTotal: scooterSummary.value.total,
    scooterFault: scooterSummary.value.fault
  }
})

const selectedIds = computed(() => {
  return {
    zoneId: selectedIdByType.value[TYPE_ZONE],
    noParkingZoneId: selectedIdByType.value[TYPE_NO_PARKING],
    parkingPointId: selectedIdByType.value[TYPE_PARKING_POINT],
    scooterId: selectedIdByType.value[TYPE_SCOOTER]
  }
})

const detailMode = computed(() => {
  if (selectedDetail.value) {
    return selectedDetail.value.type
  }

  if (currentSelectedItem.value) {
    return selectedType.value
  }

  return 'summary'
})

const detailData = computed(() => {
  if (selectedDetail.value) {
    return selectedDetail.value.item
  }

  return currentSelectedItem.value
})

const detailTitle = computed(() => {
  if (detailMode.value === 'summary') {
    return `${typeNameMap[selectedType.value]}汇总`
  }

  if (detailMode.value === TYPE_ZONE) {
    return '片区详情'
  }

  if (detailMode.value === TYPE_NO_PARKING) {
    return '禁停区详情'
  }

  if (detailMode.value === TYPE_PARKING_POINT) {
    return '停车点详情'
  }

  return '车辆详情'
})

const canEditCurrentDetail = computed(() => {
  if (!detailData.value) {
    return false
  }

  return [TYPE_ZONE, TYPE_NO_PARKING, TYPE_PARKING_POINT].includes(detailMode.value)
})

const editButtonText = computed(() => {
  if (detailMode.value === TYPE_ZONE) {
    return '编辑片区'
  }

  if (detailMode.value === TYPE_NO_PARKING) {
    return '编辑禁停区'
  }

  if (detailMode.value === TYPE_PARKING_POINT) {
    return '编辑停车点'
  }

  return '编辑'
})

const hasCoordinate = (item) => Number.isFinite(item?.longitude) && Number.isFinite(item?.latitude)

const formatCoordinate = (item) => {
  if (!hasCoordinate(item)) {
    return '--'
  }

  return `${item.longitude.toFixed(6)}, ${item.latitude.toFixed(6)}`
}

const getFaultText = (item) => {
  return Number(item?.faultStatus) === 1 ? '故障' : '正常'
}

const getRideText = (item) => {
  return Number(item?.rideStatus) === 1 ? '使用中' : '空闲'
}

const currentSelectOptions = computed(() => {
  if (isAllType.value) {
    return []
  }

  const list = currentList.value || []
  const allLabel = allOptionLabelMap[selectedType.value]

  return [
    { value: '', label: allLabel },
    ...list.map((item) => {
      if (selectedType.value === TYPE_ZONE) {
        return {
          value: String(item.id),
          label: item.name || `片区 #${item.id}`
        }
      }

      if (selectedType.value === TYPE_NO_PARKING) {
        return {
          value: String(item.id),
          label: item.name || `禁停区 #${item.id}`
        }
      }

      if (selectedType.value === TYPE_PARKING_POINT) {
        return {
          value: String(item.id),
          label: item.name || `停车点 #${item.id}`
        }
      }

      return {
        value: String(item.id),
        label: item.code || `车辆 #${item.id}`
      }
    })
  ]
})

const ensureSelectedIdsValid = () => {
  const next = { ...selectedIdByType.value }

  Object.entries(itemListByType.value).forEach(([type, list]) => {
    const selectedId = next[type]
    if (!selectedId) {
      return
    }

    const exists = list.some((item) => String(item.id) === String(selectedId))
    if (!exists) {
      next[type] = ''
    }
  })

  selectedIdByType.value = next
}

const resetDetailWhenAllSelected = () => {
  if (!currentSelectedId.value) {
    selectedDetail.value = null
  }
}

const syncCurrentTypeSelectionToDetail = () => {
  if (!currentSelectedItem.value) {
    resetDetailWhenAllSelected()
    return
  }

  selectedDetail.value = {
    type: selectedType.value,
    item: currentSelectedItem.value
  }
}

const handleMapSelect = (payload) => {
  if (!payload || !payload.type || !payload.item) {
    return
  }

  const type = payload.type
  const id = String(payload.item.id)

  skipTypeResetOnce = true
  selectedType.value = type
  selectedIdByType.value = {
    ...selectedIdByType.value,
    [type]: id
  }

  selectedDetail.value = {
    type,
    item: payload.item
  }
}

const handleTypeChange = () => {
  if (skipTypeResetOnce) {
    skipTypeResetOnce = false
    return
  }

  if (isAllType.value) {
    selectedDetail.value = null
    return
  }

  selectedIdByType.value = {
    ...selectedIdByType.value,
    [selectedType.value]: ''
  }
  selectedDetail.value = null
}

const handleObjectChange = () => {
  if (!currentSelectedId.value) {
    resetDetailWhenAllSelected()
    return
  }

  syncCurrentTypeSelectionToDetail()
}

const gotoEdit = () => {
  if (!canEditCurrentDetail.value || !detailData.value) {
    return
  }

  const id = detailData.value.id
  if (detailMode.value === TYPE_ZONE) {
    router.push({ name: 'zone-edit', params: { id } })
    return
  }

  if (detailMode.value === TYPE_NO_PARKING) {
    router.push({ name: 'no-parking-zone-edit', params: { id } })
    return
  }

  router.push({ name: 'parking-point-edit', params: { id } })
}

const fetchLayers = async () => {
  loadingLayers.value = true

  try {
    const value = await fetchAdminMapLayers({ force: true })
    layers.value = {
      zones: value.zones || [],
      noParkingZones: value.noParkingZones || [],
      parkingPoints: value.parkingPoints || [],
      scooters: value.scooters || []
    }
  } catch (error) {
    layers.value = {
      zones: [],
      noParkingZones: [],
      parkingPoints: [],
      scooters: []
    }
  } finally {
    loadingLayers.value = false
  }
}

watch(
  () => layers.value,
  () => {
    ensureSelectedIdsValid()

    if (selectedDetail.value) {
      const list = itemListByType.value[selectedDetail.value.type] || []
      const updated = list.find((item) => String(item.id) === String(selectedDetail.value.item.id))
      if (updated) {
        selectedDetail.value = {
          type: selectedDetail.value.type,
          item: updated
        }
      }
    }
  },
  { deep: true }
)

watch(
  () => selectedType.value,
  () => {
    handleTypeChange()
  }
)

watch(
  () => currentSelectedId.value,
  () => {
    handleObjectChange()
  }
)

onMounted(async () => {
  await fetchLayers()
})
</script>

<template>
  <div class="section-grid">
    <section class="page-surface dashboard-panel">
      <div class="panel-header">
        <div>
          <h2 class="panel-title">车辆管理</h2>
        </div>
      </div>

      <div class="vehicle-manage-grid">
        <section class="page-surface map-panel">
          <ScooterMapPreview
            :scooters="layers.scooters"
            :zones="layers.zones"
            :no-parking-zones="layers.noParkingZones"
            :parking-points="layers.parkingPoints"
            :focus-zone-id="selectedType === TYPE_ZONE ? (selectedIds.zoneId || '') : ''"
            :selected-zone-id="selectedIds.zoneId"
            :selected-no-parking-zone-id="selectedIds.noParkingZoneId"
            :selected-parking-point-id="selectedIds.parkingPointId"
            :selected-scooter-id="selectedIds.scooterId"
            :visible-types="visibleTypes"
            :height="620"
            @select="handleMapSelect"
          />
        </section>

        <aside class="page-surface vehicle-side-panel">
          <section class="side-card">
            <h3 class="side-title">选择对象</h3>
            <label class="form-field">
              <span class="field-label">类型</span>
              <select v-model="selectedType" class="field-select" :disabled="loadingLayers">
                <option v-for="item in typeOptions" :key="item.value" :value="item.value">{{ item.label }}</option>
              </select>
            </label>

            <label class="form-field">
              <span class="field-label">对象</span>
              <select
                :value="currentSelectedId"
                class="field-select"
                :disabled="loadingLayers || isAllType"
                @change="
                  selectedIdByType = {
                    ...selectedIdByType,
                    [selectedType]: $event.target.value
                  }
                "
              >
                <option v-if="isAllType" value="">全部模式下不可选对象</option>
                <option v-for="option in currentSelectOptions" v-else :key="option.value" :value="option.value">
                  {{ option.label }}
                </option>
              </select>
            </label>
          </section>

          <section class="side-card">
            <h3 class="side-title">{{ detailTitle }}</h3>

            <div v-if="detailMode === 'summary' && isAllType" class="summary-grid">
              <article class="mini-metric">
                <span>总对象数</span>
                <strong>{{ formatNumber(allSummary.total) }}</strong>
              </article>
              <article class="mini-metric">
                <span>片区</span>
                <strong>{{ formatNumber(allSummary.zoneTotal) }}</strong>
              </article>
              <article class="mini-metric">
                <span>禁停区</span>
                <strong>{{ formatNumber(allSummary.noParkingTotal) }}</strong>
              </article>
              <article class="mini-metric">
                <span>停车点</span>
                <strong>{{ formatNumber(allSummary.parkingPointTotal) }}</strong>
              </article>
              <article class="mini-metric">
                <span>车辆</span>
                <strong>{{ formatNumber(allSummary.scooterTotal) }}</strong>
              </article>
              <article class="mini-metric">
                <span>故障车辆</span>
                <strong>{{ formatNumber(allSummary.scooterFault) }}</strong>
              </article>
            </div>

            <div v-else-if="detailMode === 'summary'" class="summary-grid">
              <article class="mini-metric">
                <span>总数</span>
                <strong>{{ formatNumber(summaryByType[selectedType].total) }}</strong>
              </article>
              <article v-if="selectedType === TYPE_ZONE" class="mini-metric">
                <span>已分配调度员</span>
                <strong>{{ formatNumber(summaryByType[selectedType].assigned) }}</strong>
              </article>
              <article v-if="selectedType === TYPE_ZONE" class="mini-metric">
                <span>未分配调度员</span>
                <strong>{{ formatNumber(summaryByType[selectedType].unassigned) }}</strong>
              </article>
              <article v-if="selectedType === TYPE_NO_PARKING || selectedType === TYPE_PARKING_POINT" class="mini-metric">
                <span>启用</span>
                <strong>{{ formatNumber(summaryByType[selectedType].enabled) }}</strong>
              </article>
              <article v-if="selectedType === TYPE_NO_PARKING || selectedType === TYPE_PARKING_POINT" class="mini-metric">
                <span>停用</span>
                <strong>{{ formatNumber(summaryByType[selectedType].disabled) }}</strong>
              </article>
              <article v-if="selectedType === TYPE_SCOOTER" class="mini-metric">
                <span>在线车辆</span>
                <strong>{{ formatNumber(summaryByType[selectedType].online) }}</strong>
              </article>
              <article v-if="selectedType === TYPE_SCOOTER" class="mini-metric">
                <span>故障车辆</span>
                <strong>{{ formatNumber(summaryByType[selectedType].fault) }}</strong>
              </article>
            </div>

            <div v-else-if="detailMode === TYPE_ZONE && detailData" class="detail-grid">
              <div class="detail-item">
                <span>片区名称</span>
                <strong>{{ detailData.name || '--' }}</strong>
              </div>
              <div class="detail-item">
                <span>片区 ID</span>
                <strong>#{{ detailData.id }}</strong>
              </div>
              <div class="detail-item">
                <span>调度员</span>
                <strong>{{ detailData.dispatcherName || '未分配' }}</strong>
              </div>
              <div class="detail-item">
                <span>调度员邮箱</span>
                <strong>{{ detailData.dispatcherEmail || '--' }}</strong>
              </div>
              <div class="detail-item">
                <span>创建时间</span>
                <strong>{{ formatDateTime(detailData.createTime) }}</strong>
              </div>
            </div>

            <div v-else-if="detailMode === TYPE_NO_PARKING && detailData" class="detail-grid">
              <div class="detail-item">
                <span>禁停区名称</span>
                <strong>{{ detailData.name || '--' }}</strong>
              </div>
              <div class="detail-item">
                <span>禁停区 ID</span>
                <strong>#{{ detailData.id }}</strong>
              </div>
              <div class="detail-item">
                <span>状态</span>
                <strong>{{ formatBinaryStatus(detailData.status).text }}</strong>
              </div>
              <div class="detail-item">
                <span>创建时间</span>
                <strong>{{ formatDateTime(detailData.createTime) }}</strong>
              </div>
            </div>

            <div v-else-if="detailMode === TYPE_PARKING_POINT && detailData" class="detail-grid">
              <div class="detail-item">
                <span>停车点名称</span>
                <strong>{{ detailData.name || '--' }}</strong>
              </div>
              <div class="detail-item">
                <span>停车点 ID</span>
                <strong>#{{ detailData.id }}</strong>
              </div>
              <div class="detail-item">
                <span>状态</span>
                <strong>{{ formatBinaryStatus(detailData.status).text }}</strong>
              </div>
              <div class="detail-item">
                <span>坐标</span>
                <strong>{{ formatCoordinate(detailData) }}</strong>
              </div>
              <div class="detail-item">
                <span>创建时间</span>
                <strong>{{ formatDateTime(detailData.createTime) }}</strong>
              </div>
            </div>

            <div v-else-if="detailMode === TYPE_SCOOTER && detailData" class="detail-grid">
              <div class="detail-item">
                <span>车辆编码</span>
                <strong>{{ detailData.code || '--' }}</strong>
              </div>
              <div class="detail-item">
                <span>车辆 ID</span>
                <strong>#{{ detailData.id }}</strong>
              </div>
              <div class="detail-item">
                <span>骑行状态</span>
                <strong>{{ getRideText(detailData) }}</strong>
              </div>
              <div class="detail-item">
                <span>故障状态</span>
                <strong>{{ getFaultText(detailData) }}</strong>
              </div>
              <div class="detail-item">
                <span>电量</span>
                <strong>{{ detailData.battery === null ? '--' : `${detailData.battery}%` }}</strong>
              </div>
              <div class="detail-item">
                <span>坐标</span>
                <strong>{{ formatCoordinate(detailData) }}</strong>
              </div>
              <div class="detail-item">
                <span>创建时间</span>
                <strong>{{ formatDateTime(detailData.createTime) }}</strong>
              </div>
            </div>

            <div v-else class="empty-state">请选择具体对象或点击地图查看详情。</div>

            <div v-if="canEditCurrentDetail" class="detail-actions">
              <button type="button" class="button-primary" @click="gotoEdit">{{ editButtonText }}</button>
            </div>
          </section>
        </aside>
      </div>
    </section>
  </div>
</template>

<style scoped>
.dashboard-panel {
  padding: 28px;
}

.vehicle-manage-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 360px;
  gap: 16px;
}

.map-panel,
.vehicle-side-panel {
  padding: 18px;
}

.vehicle-side-panel {
  display: grid;
  gap: 12px;
}

.side-card {
  border: 1px solid #ecece8;
  background: #fcfcfb;
  padding: 14px;
}

.side-title {
  margin: 0 0 10px;
  font-size: 16px;
  font-weight: 500;
}

.summary-grid {
  display: grid;
  gap: 10px;
}

.mini-metric {
  border: 1px solid #e5e5e2;
  background: #ffffff;
  padding: 12px;
}

.mini-metric span {
  color: #737373;
  font-size: 12px;
  letter-spacing: 0.08em;
}

.mini-metric strong {
  display: block;
  margin-top: 8px;
  font-size: 24px;
  font-weight: 400;
}

.detail-grid {
  display: grid;
  gap: 10px;
}

.detail-item {
  border: 1px solid #ecece8;
  background: #ffffff;
  padding: 12px 14px;
}

.detail-item span {
  display: block;
  color: #737373;
  font-size: 12px;
  letter-spacing: 0.08em;
}

.detail-item strong {
  display: block;
  margin-top: 6px;
  font-size: 15px;
  font-weight: 400;
}

.detail-actions {
  margin-top: 12px;
  display: flex;
  justify-content: flex-end;
}

@media (max-width: 980px) {
  .vehicle-manage-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 720px) {
  .dashboard-panel,
  .map-panel,
  .vehicle-side-panel {
    padding: 20px;
  }
}
</style>
