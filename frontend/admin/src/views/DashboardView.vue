<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import StatCard from '@/components/StatCard.vue'
import TrendChart from '@/components/TrendChart.vue'
import { getDataOverview, getLiveData, getZoneList } from '@/api'
import { createDateRange, formatCurrency, formatDate, formatNumber } from '@/utils/format'

const range = createDateRange(30)

const text = {
  totalOrders: '今日总订单',
  totalRevenue: '今日总营收',
  onlineFaultScooters: '\u5728\u7ebf/\u6545\u969c\u8f66\u8f86',
  updatedAt: '\u66f4\u65b0\u4e8e',
  trendTitle: '\u8ba2\u5355\u4e0e\u8425\u6536\u8d8b\u52bf',
  startDate: '\u5f00\u59cb\u65e5\u671f',
  endDate: '\u7ed3\u675f\u65e5\u671f',
  granularity: '\u7c92\u5ea6',
  byDay: '\u6309\u5929',
  byWeek: '\u6309\u5468',
  byMonth: '\u6309\u6708',
  area: '\u7247\u533a',
  allAreas: '\u5168\u90e8\u7247\u533a',
  rangeOrders: '\u533a\u95f4\u603b\u8ba2\u5355',
  rangeRevenue: '\u533a\u95f4\u603b\u8425\u6536'
}

const overviewFilters = ref({
  startDate: range.startDate,
  endDate: range.endDate,
  granularity: 'day',
  areaId: ''
})

const areas = ref([])
const overview = ref({
  series: []
})
const liveData = ref({
  updatedAt: '',
  todayOrders: 0,
  todayRevenue: 0,
  onlineScooters: 0,
  faultScooters: 0
})
const loadingOverview = ref(false)
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

const updatedAtHint = computed(() => {
  return `${text.updatedAt}${formatDate(liveData.value.updatedAt)}`
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

const fetchLiveData = async () => {
  try {
    const response = await getLiveData()
    liveData.value = {
      updatedAt: response.data?.updatedAt || '',
      todayOrders: Number(response.data?.todayOrders || 0),
      todayRevenue: Number(response.data?.todayRevenue || 0),
      onlineScooters: Number(response.data?.onlineScooters || 0),
      faultScooters: Number(response.data?.faultScooters || 0)
    }
  } catch (error) {
    liveData.value = {
      updatedAt: '',
      todayOrders: 0,
      todayRevenue: 0,
      onlineScooters: 0,
      faultScooters: 0
    }
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

onMounted(async () => {
  await fetchAreas()
  await fetchOverview()
  await fetchLiveData()
})
</script>

<template>
  <div class="section-grid">
    <section class="metric-grid">
      <StatCard :label="text.totalOrders" :value="formatNumber(liveData.todayOrders)" :hint="updatedAtHint" />
      <StatCard :label="text.totalRevenue" :value="formatCurrency(liveData.todayRevenue)" :hint="updatedAtHint" />
      <StatCard
        :label="text.onlineFaultScooters"
        :value="`${formatNumber(liveData.onlineScooters)} / ${formatNumber(liveData.faultScooters)}`"
        :hint="updatedAtHint"
      />
    </section>

    <section class="page-surface dashboard-panel">
      <div class="panel-header">
        <div>
          <h2 class="panel-title">{{ text.trendTitle }}</h2>
        </div>
      </div>

      <div class="toolbar dashboard-toolbar">
        <label class="form-field toolbar-grow">
          <span class="field-label">{{ text.startDate }}</span>
          <input v-model="overviewFilters.startDate" class="field-input" type="date" />
        </label>
        <label class="form-field toolbar-grow">
          <span class="field-label">{{ text.endDate }}</span>
          <input v-model="overviewFilters.endDate" class="field-input" type="date" />
        </label>
        <label class="form-field">
          <span class="field-label">{{ text.granularity }}</span>
          <select v-model="overviewFilters.granularity" class="field-select">
            <option value="day">{{ text.byDay }}</option>
            <option value="week">{{ text.byWeek }}</option>
            <option value="month">{{ text.byMonth }}</option>
          </select>
        </label>
        <label class="form-field toolbar-grow">
          <span class="field-label">{{ text.area }}</span>
          <select v-model="overviewFilters.areaId" class="field-select" :disabled="loadingAreas">
            <option value="">{{ text.allAreas }}</option>
            <option v-for="item in areas" :key="item.id" :value="String(item.id)">{{ item.name }}</option>
          </select>
        </label>
      </div>

      <div class="dashboard-summary-row">
        <div class="summary-tile">
          <span>{{ text.rangeOrders }}</span>
          <strong>{{ formatNumber(overviewSummary.orders) }}</strong>
        </div>
        <div class="summary-tile">
          <span>{{ text.rangeRevenue }}</span>
          <strong>{{ formatCurrency(overviewSummary.revenue) }}</strong>
        </div>
      </div>

      <TrendChart :series="overview.series || []" />
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

.summary-tile span {
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

@media (max-width: 980px) {
  .dashboard-summary-row {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 720px) {
  .dashboard-panel {
    padding: 20px;
  }
}
</style>
