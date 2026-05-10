<script setup>
import { computed } from 'vue'
import { countPolygonPoints, getPolygonBounds, parsePolygonPoints } from '@/utils/polygon'

const props = defineProps({
  polygon: {
    type: [String, Array, Object],
    default: ''
  }
})

const points = computed(() => parsePolygonPoints(props.polygon))
const pointCount = computed(() => countPolygonPoints(props.polygon))

const viewPoints = computed(() => {
  if (!points.value.length) {
    return ''
  }

  const bounds = getPolygonBounds(points.value)
  const width = Math.max(0.0001, bounds.maxLongitude - bounds.minLongitude)
  const height = Math.max(0.0001, bounds.maxLatitude - bounds.minLatitude)

  return points.value
    .map((point) => {
      const x = ((point.longitude - bounds.minLongitude) / width) * 240 + 20
      const y = 200 - ((point.latitude - bounds.minLatitude) / height) * 160
      return `${x},${y}`
    })
    .join(' ')
})
</script>

<template>
  <div class="polygon-card">
    <div class="polygon-meta">
      <strong>{{ pointCount }}</strong>
      <span>个坐标点</span>
    </div>

    <div v-if="points.length" class="polygon-stage">
      <svg viewBox="0 0 280 220" class="polygon-svg" role="img" aria-label="多边形边界预览">
        <rect x="0" y="0" width="280" height="220" class="polygon-grid" />
        <polygon :points="viewPoints" class="polygon-shape" />
      </svg>
    </div>

    <div v-else class="empty-state">当前没有可预览的边界数据。</div>
  </div>
</template>

<style scoped>
.polygon-card {
  display: grid;
  gap: 14px;
}

.polygon-meta {
  display: flex;
  align-items: baseline;
  gap: 8px;
}

.polygon-meta strong {
  font-size: 26px;
  font-weight: 400;
}

.polygon-meta span {
  color: #737373;
  font-size: 13px;
}

.polygon-stage {
  border: 1px solid #e5e5e2;
  background: #fafaf8;
  padding: 12px;
}

.polygon-svg {
  width: 100%;
  height: auto;
  display: block;
}

.polygon-grid {
  fill: #ffffff;
  stroke: #e5e5e2;
}

.polygon-shape {
  fill: rgba(11, 14, 13, 0.08);
  stroke: #0b0e0d;
  stroke-width: 2;
}
</style>
