<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import StatCard from '@/components/StatCard.vue'
import TrendChart from '@/components/TrendChart.vue'
import { getDataOverview, getLiveData, getZoneList } from '@/api'
import { createDateRange, formatCurrency, formatNumber, toISODate } from '@/utils/format'

const range = createDateRange(30)

const overviewFilters = ref({
  startDate: range.startDate,
  endDate: range.endDate,
  granularity: 'day',
  areaId: ''
})

const vehicleDate = ref(toISODate())

const areas = ref([])
const overview = ref({
  series: []
})
const vehicleOverview = ref({
  totalOnline: 0,
  totalFault: 0,
  regions: []
})
const loadingOverview = ref(false)
const loadingVehicleOverview = ref(false)
const loadingAreas = ref(false)

const overviewSummary = computed(() => {
  const series = Array.isArray(overview.value.series) ? overview.value.series : []

  return series.reduce(
    (result, item) => ({
      orders: result.orders + Number(item.orderCount || 0),
      revenue: result.revenue + Number(item.revenue || 0)
    }),
    { orders: 0, revenue: 0 }
  )
})

const vehicleCards = computed(() => {
  const regions = Array.isArray(vehicleOverview.value.regions) ? vehicleOverview.value.regions : []

  return regions.map((item) => ({
    key: item.id,
    name: item.name || `片区 #${item.id}`,
    online: Number(item.onlineScooters || 0),
    fault: Number(item.faultScooters || 0),
    areaId: item.id
  }))
})

const normalizedAreaId = (value) => {
  return value === '' ? undefined : Number(value)
}

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

const fetchOverview = async () => {
  loadingOverview.value = true

  try {
    const response = await getDataOverview({
      startDate: overviewFilters.value.startDate,
      endDate: overviewFilters.value.endDate,
      granularity: overviewFilters.value.granularity,
      areaId: normalizedAreaId(overviewFilters.value.areaId)
    })
    overview.value = response.data || { series: [] }
  } catch (error) {
    overview.value = { series: [] }
  } finally {
    loadingOverview.value = false
  }
}

const fetchVehicleOverview = async () => {
  loadingVehicleOverview.value = true

  try {
    const regionResults = await Promise.all(
      areas.value.map(async (item) => {
        try {
          const response = await getLiveData({
            date: vehicleDate.value,
            areaId: item.id
          })

          return {
            id: item.id,
            name: item.name,
            onlineScooters: response.data?.onlineScooters || 0,
            faultScooters: response.data?.faultScooters || 0
          }
        } catch (error) {
          return {
            id: item.id,
            name: item.name,
            onlineScooters: 0,
            faultScooters: 0
          }
        }
      })
    )

    vehicleOverview.value = {
      totalOnline: regionResults.reduce((sum, item) => sum + Number(item.onlineScooters || 0), 0),
      totalFault: regionResults.reduce((sum, item) => sum + Number(item.faultScooters || 0), 0),
      regions: regionResults
    }
  } catch (error) {
    vehicleOverview.value = {
      totalOnline: 0,
      totalFault: 0,
      regions: []
    }
  } finally {
    loadingVehicleOverview.value = false
  }
}

watch(
  () => [
    overviewFilters.value.startDate,
    overviewFilters.value.endDate,
    overviewFilters.value.granularity,
    overviewFilters.value.areaId
  ],
  () => {
    fetchOverview()
  }
)

watch(
  () => [vehicleDate.value],
  () => {
    fetchVehicleOverview()
  }
)

onMounted(async () => {
  await fetchAreas()
  await fetchOverview()
  await fetchVehicleOverview()
})
</script>

<template>
  <div class="section-grid">
    <section class="metric-grid">
      <StatCard
        label="区间总订单"
        :value="formatNumber(overviewSummary.orders || 0)"
        :hint="`趋势时间 ${overviewFilters.startDate} 至 ${overviewFilters.endDate}`"
      />
      <StatCard
        label="区间总营收"
        :value="formatCurrency(overviewSummary.revenue || 0)"
        :hint="`趋势时间 ${overviewFilters.startDate} 至 ${overviewFilters.endDate}`"
      />
      <StatCard
        label="车辆概况"
        :value="`${formatNumber(vehicleOverview.totalOnline || 0)} / ${formatNumber(vehicleOverview.totalFault || 0)}`"
        :hint="`统计日期 ${vehicleDate || '--'}，正常 / 故障`"
      />
    </section>

    <section class="page-surface dashboard-panel">
      <div class="panel-header">
        <div>
          <h2 class="panel-title">订单与营收趋势</h2>
        </div>
      </div>

      <div class="toolbar dashboard-toolbar">
        <label class="form-field toolbar-grow">
          <span class="field-label">开始日期</span>
          <input v-model="overviewFilters.startDate" class="field-input" type="date" />
        </label>
        <label class="form-field toolbar-grow">
          <span class="field-label">结束日期</span>
          <input v-model="overviewFilters.endDate" class="field-input" type="date" />
        </label>
        <label class="form-field">
          <span class="field-label">粒度</span>
          <select v-model="overviewFilters.granularity" class="field-select">
            <option value="day">按天</option>
            <option value="week">按周</option>
            <option value="month">按月</option>
          </select>
        </label>
        <label class="form-field toolbar-grow">
          <span class="field-label">片区</span>
          <select v-model="overviewFilters.areaId" class="field-select" :disabled="loadingAreas">
            <option value="">全部片区</option>
            <option v-for="item in areas" :key="item.id" :value="String(item.id)">{{ item.name }}</option>
          </select>
        </label>
      </div>

      <div class="dashboard-summary-row">
        <div class="summary-tile">
          <span>区间总订单</span>
          <strong>{{ formatNumber(overviewSummary.orders) }}</strong>
        </div>
        <div class="summary-tile">
          <span>区间总营收</span>
          <strong>{{ formatCurrency(overviewSummary.revenue) }}</strong>
        </div>
      </div>

      <TrendChart :series="overview.series || []" />
    </section>

    <section class="page-surface dashboard-panel">
      <div class="panel-header">
        <div>
          <h2 class="panel-title">车辆概况</h2>
        </div>
      </div>

      <div class="toolbar">
        <label class="form-field toolbar-grow">
          <span class="field-label">统计日期</span>
          <input v-model="vehicleDate" class="field-input" type="date" />
        </label>
      </div>

      <div class="vehicle-grid">
        <article v-for="item in vehicleCards" :key="item.key" class="vehicle-card">
          <div class="vehicle-card-header">
            <div>
              <span class="vehicle-card-kicker">区域 #{{ item.areaId }}</span>
              <h3>{{ item.name }}</h3>
            </div>
            <div class="vehicle-card-meta">
              <span>单区域</span>
            </div>
          </div>

          <div class="vehicle-card-body">
            <div class="vehicle-stat">
              <span>正常车辆</span>
              <strong>{{ formatNumber(item.online) }}</strong>
            </div>
            <div class="vehicle-stat">
              <span>故障车辆</span>
              <strong>{{ formatNumber(item.fault) }}</strong>
            </div>
          </div>
        </article>
      </div>
    </section>
  </div>
</template>

<style scoped>
.dashboard-panel {
  padding: 28px;
}

.dashboard-toolbar {
  margin-bottom: 16px;
}

.dashboard-summary-row {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 18px;
}

.summary-tile {
  padding: 20px;
  border: 1px solid #e5e5e2;
  background: #fafaf8;
}

.summary-tile span,
.vehicle-stat span,
.toolbar-note strong {
  display: block;
  color: #737373;
  font-size: 13px;
  letter-spacing: 0.08em;
}

.summary-tile strong {
  display: block;
  margin-top: 8px;
  font-size: 30px;
  font-weight: 400;
}

.toolbar-note {
  min-width: 260px;
  padding: 18px;
  border: 1px solid #e5e5e2;
  background: #fafaf8;
}

.toolbar-note p {
  margin: 6px 0 0;
  color: #525252;
}

.vehicle-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.vehicle-card {
  border: 1px solid #e5e5e2;
  background: #ffffff;
  padding: 18px;
  display: grid;
  gap: 16px;
}

.vehicle-card.overall {
  background: #fafaf8;
  grid-column: 1 / -1;
}

.vehicle-card-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.vehicle-card-kicker {
  display: inline-block;
  margin-bottom: 6px;
  color: #737373;
  font-size: 12px;
  letter-spacing: 0.12em;
}

.vehicle-card-header h3 {
  margin: 0;
  font-size: 20px;
  font-weight: 400;
}

.vehicle-card-meta {
  color: #737373;
  font-size: 12px;
}

.vehicle-card-body {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.vehicle-stat {
  padding: 16px;
  border: 1px solid #ecece8;
  background: #fcfcfb;
}

.vehicle-stat strong {
  display: block;
  margin-top: 8px;
  font-size: 28px;
  font-weight: 400;
}

@media (max-width: 980px) {
  .dashboard-summary-row,
  .vehicle-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 720px) {
  .dashboard-panel {
    padding: 20px;
  }
}
</style>
