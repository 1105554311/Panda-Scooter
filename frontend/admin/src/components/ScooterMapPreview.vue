<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { isAmapConfigured, loadAmap } from '@/utils/amap'

const DEFAULT_CENTER = [103.99015677941316, 30.762680478785253]
const DEFAULT_ZOOM = 14

const props = defineProps({
  scooters: {
    type: Array,
    default: () => []
  },
  selectedScooterId: {
    type: [String, Number],
    default: ''
  },
  height: {
    type: Number,
    default: 500
  }
})

const emit = defineEmits(['select'])

const mapContainer = ref(null)
const loading = ref(true)
const loadError = ref('')

let AMap = null
let map = null
let markers = []
let mapResizeObserver = null

const mapHeightStyle = computed(() => ({
  height: `${props.height}px`
}))

const configHint = computed(() => {
  if (isAmapConfigured()) {
    return ''
  }

  return '请在 admin 环境变量中配置 VITE_AMAP_WEB_KEY。如控制台要求安全密钥，请同时配置 VITE_AMAP_SECURITY_JS_CODE。'
})

const hasMappableScooters = computed(() => {
  return props.scooters.some((item) => Number.isFinite(item.longitude) && Number.isFinite(item.latitude))
})

const clearMarkers = () => {
  if (!map || !markers.length) {
    markers = []
    return
  }

  map.remove(markers)
  markers = []
}

const getMarkerStyle = (scooter, active) => {
  if (active) {
    return {
      radius: 8,
      fillColor: '#0b0e0d',
      strokeColor: '#ffffff',
      strokeWeight: 2
    }
  }

  if (Number(scooter.faultStatus) === 1) {
    return {
      radius: 7,
      fillColor: '#8f3a2d',
      strokeColor: '#ffffff',
      strokeWeight: 2
    }
  }

  return {
    radius: 7,
    fillColor: '#2f6f46',
    strokeColor: '#ffffff',
    strokeWeight: 2
  }
}

const renderScooters = () => {
  if (!map || !AMap) {
    return
  }

  clearMarkers()

  markers = props.scooters
    .filter((item) => Number.isFinite(item.longitude) && Number.isFinite(item.latitude))
    .map((item) => {
      const active = String(item.id) === String(props.selectedScooterId || '')
      const marker = new AMap.CircleMarker({
        center: [item.longitude, item.latitude],
        zIndex: active ? 230 : 220,
        bubble: true,
        ...getMarkerStyle(item, active)
      })

      marker.on('click', () => {
        emit('select', item)
      })

      return marker
    })

  if (!markers.length) {
    map.setZoomAndCenter(DEFAULT_ZOOM, DEFAULT_CENTER)
    return
  }

  map.add(markers)
  map.setFitView(markers, false, [48, 48, 48, 48])
}

const setupMap = async () => {
  if (!isAmapConfigured()) {
    loading.value = false
    return
  }

  try {
    AMap = await loadAmap()
    await nextTick()

    if (!mapContainer.value) {
      throw new Error('地图容器不存在。')
    }

    map = new AMap.Map(mapContainer.value, {
      zoom: DEFAULT_ZOOM,
      center: DEFAULT_CENTER,
      resizeEnable: true,
      viewMode: '3D'
    })

    map.addControl(new AMap.ToolBar({ position: 'RB' }))
    map.addControl(new AMap.Scale())

    if (typeof ResizeObserver !== 'undefined') {
      mapResizeObserver = new ResizeObserver(() => {
        if (map && typeof map.resize === 'function') {
          map.resize()
        }
      })
      mapResizeObserver.observe(mapContainer.value)
    }

    renderScooters()
  } catch (error) {
    loadError.value = error?.message || '地图加载失败。'
  } finally {
    loading.value = false
  }
}

const destroyMap = () => {
  if (mapResizeObserver) {
    mapResizeObserver.disconnect()
    mapResizeObserver = null
  }

  clearMarkers()

  if (map && typeof map.destroy === 'function') {
    map.destroy()
  }

  map = null
  AMap = null
}

watch(
  () => [props.scooters, props.selectedScooterId],
  () => {
    if (!map || !AMap) {
      return
    }
    renderScooters()
  },
  { deep: true }
)

onMounted(setupMap)
onBeforeUnmount(destroyMap)
</script>

<template>
  <div class="scooter-map-preview">
    <div v-if="configHint" class="map-state map-state-warning">
      <strong>地图未配置</strong>
      <p>{{ configHint }}</p>
    </div>

    <div v-else-if="loadError" class="map-state map-state-warning">
      <strong>地图加载失败</strong>
      <p>{{ loadError }}</p>
    </div>

    <div v-else-if="loading" class="map-state">
      <strong>地图加载中</strong>
      <p>正在初始化车辆地图。</p>
    </div>

    <div v-show="isAmapConfigured() && !loadError" ref="mapContainer" class="map-stage" :style="mapHeightStyle"></div>

    <div v-if="!loading && isAmapConfigured() && !loadError && !hasMappableScooters" class="map-empty">
      当前筛选条件下没有可展示坐标的车辆。
    </div>
  </div>
</template>

<style scoped>
.scooter-map-preview {
  display: grid;
  gap: 10px;
}

.map-stage,
.map-state,
.map-empty {
  border: 1px solid #e5e5e2;
  background: #ffffff;
}

.map-stage {
  width: 100%;
  min-height: 320px;
  overflow: hidden;
}

.map-state {
  padding: 18px 20px;
}

.map-state strong {
  color: #0b0e0d;
  font-size: 14px;
}

.map-state p {
  margin: 8px 0 0;
  color: #737373;
  font-size: 13px;
  line-height: 1.6;
}

.map-state-warning {
  background: #fafaf8;
}

.map-empty {
  padding: 12px 14px;
  font-size: 13px;
  color: #737373;
}
</style>
