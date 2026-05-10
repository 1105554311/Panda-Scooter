<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { loadECharts } from '@/utils/echarts'
import { formatCurrency, formatNumber } from '@/utils/format'

const props = defineProps({
  series: {
    type: Array,
    default: () => []
  }
})

const chartContainer = ref(null)
const chartReady = ref(false)
const chartError = ref('')

let chart = null
let resizeObserver = null

const normalizedSeries = computed(() => {
  return props.series.map((item) => ({
    time: item.time || '--',
    orderCount: Number(item.orderCount || 0),
    revenue: Number(item.revenue || 0)
  }))
})

const latestItem = computed(() => normalizedSeries.value[normalizedSeries.value.length - 1] || null)

const buildOption = () => ({
  animationDuration: 240,
  grid: {
    top: 48,
    left: 56,
    right: 64,
    bottom: 40
  },
  tooltip: {
    trigger: 'axis',
    backgroundColor: '#ffffff',
    borderColor: '#d4d4d1',
    borderWidth: 1,
    textStyle: {
      color: '#0b0e0d'
    },
    formatter(params) {
      const pointList = Array.isArray(params) ? params : [params]
      const title = pointList[0]?.axisValueLabel || '--'
      const lines = pointList.map((item) => {
        if (item.seriesName === '订单量') {
          return `${item.marker}${item.seriesName}: ${formatNumber(item.value)}`
        }

        return `${item.marker}${item.seriesName}: ${formatCurrency(item.value)}`
      })

      return [title, ...lines].join('<br>')
    }
  },
  legend: {
    top: 0,
    right: 0,
    itemWidth: 12,
    itemHeight: 12,
    textStyle: {
      color: '#737373',
      fontSize: 12
    },
    data: ['订单量', '营收']
  },
  xAxis: {
    type: 'category',
    boundaryGap: false,
    data: normalizedSeries.value.map((item) => item.time),
    axisLine: {
      lineStyle: {
        color: '#d4d4d1'
      }
    },
    axisTick: {
      show: false
    },
    axisLabel: {
      color: '#737373',
      fontSize: 12
    }
  },
  yAxis: [
    {
      type: 'value',
      name: '订单量',
      nameTextStyle: {
        color: '#737373',
        fontSize: 12
      },
      splitLine: {
        lineStyle: {
          color: '#e5e5e2'
        }
      },
      axisLine: {
        show: false
      },
      axisLabel: {
        color: '#737373',
        fontSize: 12
      }
    },
    {
      type: 'value',
      name: '营收',
      nameTextStyle: {
        color: '#737373',
        fontSize: 12
      },
      splitLine: {
        show: false
      },
      axisLine: {
        show: false
      },
      axisLabel: {
        color: '#737373',
        fontSize: 12,
        formatter(value) {
          return `¥${Number(value || 0).toFixed(0)}`
        }
      }
    }
  ],
  series: [
    {
      name: '订单量',
      type: 'line',
      smooth: true,
      symbol: 'circle',
      symbolSize: 7,
      yAxisIndex: 0,
      itemStyle: {
        color: '#0b0e0d'
      },
      lineStyle: {
        color: '#0b0e0d',
        width: 3
      },
      data: normalizedSeries.value.map((item) => item.orderCount)
    },
    {
      name: '营收',
      type: 'line',
      smooth: true,
      symbol: 'circle',
      symbolSize: 7,
      yAxisIndex: 1,
      itemStyle: {
        color: '#737373'
      },
      lineStyle: {
        color: '#737373',
        width: 3
      },
      data: normalizedSeries.value.map((item) => item.revenue)
    }
  ]
})

const applyOption = () => {
  if (!chart) {
    return
  }

  chart.setOption(buildOption(), true)
}

const ensureChart = async () => {
  await nextTick()

  if (!chartContainer.value) {
    return
  }

  if (!chart) {
    const echarts = await loadECharts()
    await nextTick()

    if (!chartContainer.value) {
      return
    }

    chart = echarts.init(chartContainer.value)

    if (typeof ResizeObserver !== 'undefined' && !resizeObserver) {
      resizeObserver = new ResizeObserver(() => {
        if (chart) {
          chart.resize()
        }
      })
      resizeObserver.observe(chartContainer.value)
    }
  }

  applyOption()
  chartReady.value = true
}

const syncChart = async () => {
  chartError.value = ''

  try {
    await ensureChart()
  } catch (error) {
    chartError.value = error?.message || '图表加载失败'
  }
}

const disposeChart = () => {
  if (resizeObserver) {
    resizeObserver.disconnect()
    resizeObserver = null
  }

  if (chart) {
    chart.dispose()
    chart = null
  }
}

watch(
  () => props.series,
  () => {
    syncChart()
  },
  { deep: true, immediate: true }
)

onMounted(syncChart)
onBeforeUnmount(disposeChart)
</script>

<template>
  <div class="chart-card">
    <div class="chart-summary">
      <div class="chart-legend">
        <span class="legend-item">
          <span class="legend-dot order-dot"></span>
          订单量
        </span>
        <span class="legend-item">
          <span class="legend-dot revenue-dot"></span>
          营收
        </span>
      </div>
      <div v-if="latestItem" class="chart-latest">
        <span>最近订单 {{ formatNumber(latestItem.orderCount) }}</span>
        <span>最近营收 {{ formatCurrency(latestItem.revenue) }}</span>
      </div>
    </div>

    <div v-if="chartError" class="empty-state">{{ chartError }}</div>
    <div ref="chartContainer" class="chart-stage" :data-ready="chartReady"></div>
  </div>
</template>

<style scoped>
.chart-card {
  display: grid;
  gap: 18px;
}

.chart-summary {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  flex-wrap: wrap;
}

.chart-legend,
.chart-latest {
  display: flex;
  align-items: center;
  gap: 14px;
  flex-wrap: wrap;
  color: #737373;
  font-size: 13px;
}

.legend-item {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.legend-dot {
  width: 10px;
  height: 10px;
}

.order-dot {
  background: #0b0e0d;
}

.revenue-dot {
  background: #737373;
}

.chart-stage {
  min-height: 320px;
  border: 1px solid #e5e5e2;
  background: #fafaf8;
}
</style>
