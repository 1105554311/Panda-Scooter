<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import ScooterMapPreview from '@/components/ScooterMapPreview.vue'
import { getScooterList, getZoneList } from '@/api'
import { formatDateTime, formatNumber } from '@/utils/format'

const areas = ref([])
const loadingAreas = ref(false)
const loadingScooters = ref(false)
const scooterAreaId = ref('')
const scooters = ref([])
const selectedScooter = ref(null)

const normalizedAreaId = (value) => {
  return value === '' ? undefined : Number(value)
}

const normalizeScooterRecord = (item) => {
  const longitude = Number(item.longitude)
  const latitude = Number(item.latitude)
  const faultStatus = Number(item.faultStatus ?? item.fault_status ?? 0)
  const rideStatus = Number(item.rideStatus ?? item.ride_status ?? 0)
  const battery = Number(item.battery)

  return {
    id: item.id,
    code: item.code || '--',
    rideStatus,
    faultStatus,
    battery: Number.isFinite(battery) ? battery : null,
    longitude: Number.isFinite(longitude) ? longitude : null,
    latitude: Number.isFinite(latitude) ? latitude : null,
    createTime: item.createTime
  }
}

const scooterSummary = computed(() => {
  const list = scooters.value
  const online = list.filter(
    (item) => item.faultStatus !== 1 && Number.isFinite(item.longitude) && Number.isFinite(item.latitude)
  ).length
  const fault = list.filter((item) => item.faultStatus === 1).length

  return { online, fault }
})

const selectedAreaName = computed(() => {
  if (!scooterAreaId.value) {
    return '全部片区'
  }

  const area = areas.value.find((item) => String(item.id) === String(scooterAreaId.value))
  return area?.name || '--'
})

const selectedScooterDetail = computed(() => {
  if (!selectedScooter.value) {
    return null
  }

  return {
    ...selectedScooter.value,
    faultText: selectedScooter.value.faultStatus === 1 ? '故障' : '正常',
    rideText: selectedScooter.value.rideStatus === 1 ? '使用中' : '空闲'
  }
})

const fetchAreas = async () => {
  loadingAreas.value = true

  try {
    const response = await getZoneList({
      page: 1,
      pageSize: 100
    })
    areas.value = response.data?.areaList || []
  } catch (error) {
    areas.value = []
  } finally {
    loadingAreas.value = false
  }
}

const fetchScooters = async () => {
  loadingScooters.value = true

  try {
    const selectedArea = normalizedAreaId(scooterAreaId.value)
    const response = await getScooterList({
      areaId: selectedArea
    })

    let list = response.data?.areaList
    if (!Array.isArray(list) && Number.isFinite(selectedArea)) {
      const fallbackResponse = await getScooterList({ areaId: selectedArea })
      list = fallbackResponse.data?.areaList
    }

    scooters.value = Array.isArray(list) ? list.map(normalizeScooterRecord) : []
    selectedScooter.value = null
  } catch (error) {
    scooters.value = []
    selectedScooter.value = null
  } finally {
    loadingScooters.value = false
  }
}

watch(
  () => scooterAreaId.value,
  () => {
    fetchScooters()
  }
)

onMounted(async () => {
  await fetchAreas()
  await fetchScooters()
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

      <div class="toolbar vehicle-manage-toolbar">
        <label class="form-field toolbar-grow">
          <span class="field-label">片区</span>
          <select v-model="scooterAreaId" class="field-select" :disabled="loadingAreas || loadingScooters">
            <option value="">全部片区</option>
            <option v-for="item in areas" :key="item.id" :value="String(item.id)">{{ item.name }}</option>
          </select>
        </label>
        <article class="card-surface mini-metric">
          <span>在线车辆数</span>
          <strong>{{ formatNumber(scooterSummary.online) }}</strong>
        </article>
        <article class="card-surface mini-metric">
          <span>故障车辆数</span>
          <strong>{{ formatNumber(scooterSummary.fault) }}</strong>
        </article>
      </div>

      <div class="vehicle-manage-grid">
        <section class="page-surface map-panel">
          <ScooterMapPreview
            :scooters="scooters"
            :selected-scooter-id="selectedScooter?.id"
            :height="520"
            @select="selectedScooter = $event"
          />
        </section>

        <aside class="page-surface vehicle-detail-panel">
          <h3>车辆详情</h3>
          <p class="detail-kicker">当前片区：{{ selectedAreaName }}</p>

          <div v-if="selectedScooterDetail" class="detail-grid">
            <div class="detail-item">
              <span>车辆编码</span>
              <strong>{{ selectedScooterDetail.code }}</strong>
            </div>
            <div class="detail-item">
              <span>车辆 ID</span>
              <strong>#{{ selectedScooterDetail.id }}</strong>
            </div>
            <div class="detail-item">
              <span>骑行状态</span>
              <strong>{{ selectedScooterDetail.rideText }}</strong>
            </div>
            <div class="detail-item">
              <span>故障状态</span>
              <strong>{{ selectedScooterDetail.faultText }}</strong>
            </div>
            <div class="detail-item">
              <span>电量</span>
              <strong>{{ selectedScooterDetail.battery === null ? '--' : `${selectedScooterDetail.battery}%` }}</strong>
            </div>
            <div class="detail-item">
              <span>经纬度</span>
              <strong>
                {{ Number.isFinite(selectedScooterDetail.longitude) && Number.isFinite(selectedScooterDetail.latitude)
                  ? `${selectedScooterDetail.longitude.toFixed(6)}, ${selectedScooterDetail.latitude.toFixed(6)}`
                  : '--' }}
              </strong>
            </div>
            <div class="detail-item">
              <span>创建时间</span>
              <strong>{{ formatDateTime(selectedScooterDetail.createTime) }}</strong>
            </div>
          </div>
          <div v-else class="empty-state">请在地图上点击任一车辆查看详情。</div>
        </aside>
      </div>
    </section>
  </div>
</template>

<style scoped>
.dashboard-panel {
  padding: 28px;
}

.vehicle-manage-toolbar {
  align-items: stretch;
}

.mini-metric {
  min-width: 180px;
  padding: 14px 16px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  border: 1px solid #e5e5e2;
}

.mini-metric span {
  color: #737373;
  font-size: 12px;
  letter-spacing: 0.08em;
}

.mini-metric strong {
  margin-top: 8px;
  font-size: 26px;
  font-weight: 400;
}

.vehicle-manage-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 300px;
  gap: 16px;
}

.map-panel,
.vehicle-detail-panel {
  padding: 18px;
}

.vehicle-detail-panel h3 {
  margin: 0;
  font-size: 20px;
  font-weight: 400;
}

.detail-kicker {
  margin: 8px 0 12px;
  color: #737373;
  font-size: 13px;
}

.detail-grid {
  display: grid;
  gap: 10px;
}

.detail-item {
  border: 1px solid #ecece8;
  background: #fcfcfb;
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

@media (max-width: 980px) {
  .vehicle-manage-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 720px) {
  .dashboard-panel,
  .map-panel,
  .vehicle-detail-panel {
    padding: 20px;
  }
}
</style>
