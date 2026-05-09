<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { isAmapConfigured, loadAmap } from '@/utils/amap'
import { parsePolygonPoints } from '@/utils/polygon'

const DEFAULT_CENTER = [103.99015677941316, 30.762680478785253]
const DEFAULT_ZOOM = 14

const props = defineProps({
  scooters: {
    type: Array,
    default: () => []
  },
  zones: {
    type: Array,
    default: () => []
  },
  noParkingZones: {
    type: Array,
    default: () => []
  },
  parkingPoints: {
    type: Array,
    default: () => []
  },
  selectedScooterId: {
    type: [String, Number],
    default: ''
  },
  selectedZoneId: {
    type: [String, Number],
    default: ''
  },
  selectedNoParkingZoneId: {
    type: [String, Number],
    default: ''
  },
  selectedParkingPointId: {
    type: [String, Number],
    default: ''
  },
  focusZoneId: {
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
let scooterMarkers = []
let zoneOverlays = []
let noParkingOverlays = []
let parkingPointMarkers = []
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

const hasMappableContent = computed(() => {
  const hasScooters = props.scooters.some((item) => Number.isFinite(item.longitude) && Number.isFinite(item.latitude))
  const hasZones = props.zones.some((item) => parsePolygonPoints(item.polygon).length >= 3)
  const hasNoParkingZones = props.noParkingZones.some((item) => parsePolygonPoints(item.polygon).length >= 3)
  const hasParkingPoints = props.parkingPoints.some(
    (item) => Number.isFinite(item.longitude) && Number.isFinite(item.latitude)
  )

  return hasScooters || hasZones || hasNoParkingZones || hasParkingPoints
})

const clearOverlayList = (overlayListRef) => {
  if (!map || !overlayListRef.length) {
    return []
  }

  map.remove(overlayListRef)
  return []
}

const getScooterStyle = (scooter, active) => {
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

const getZoneStyle = (active) => {
  if (active) {
    return {
      strokeColor: '#0b0e0d',
      strokeWeight: 3,
      strokeOpacity: 1,
      fillColor: '#4d9ed8',
      fillOpacity: 0.22
    }
  }

  return {
    strokeColor: '#0a5e9a',
    strokeWeight: 2,
    strokeOpacity: 0.9,
    fillColor: '#4d9ed8',
    fillOpacity: 0.12
  }
}

const getNoParkingStyle = (active) => {
  if (active) {
    return {
      strokeColor: '#0b0e0d',
      strokeWeight: 3,
      strokeOpacity: 1,
      strokeStyle: 'dashed',
      fillColor: '#cf7b6f',
      fillOpacity: 0.24
    }
  }

  return {
    strokeColor: '#8f3a2d',
    strokeWeight: 2,
    strokeOpacity: 0.9,
    strokeStyle: 'dashed',
    fillColor: '#cf7b6f',
    fillOpacity: 0.15
  }
}

const renderZoneOverlays = () => {
  if (!map || !AMap) {
    return
  }

  zoneOverlays = clearOverlayList(zoneOverlays)

  zoneOverlays = props.zones
    .map((item) => {
      const path = parsePolygonPoints(item.polygon).map((point) => [point.longitude, point.latitude])
      if (path.length < 3) {
        return null
      }

      const active = String(item.id) === String(props.selectedZoneId || '')
      const polygon = new AMap.Polygon({
        path,
        bubble: true,
        ...getZoneStyle(active)
      })

      polygon.on('click', () => {
        emit('select', {
          type: 'zone',
          item
        })
      })

      return polygon
    })
    .filter(Boolean)

  if (zoneOverlays.length) {
    map.add(zoneOverlays)
  }
}

const renderNoParkingOverlays = () => {
  if (!map || !AMap) {
    return
  }

  noParkingOverlays = clearOverlayList(noParkingOverlays)

  noParkingOverlays = props.noParkingZones
    .map((item) => {
      const path = parsePolygonPoints(item.polygon).map((point) => [point.longitude, point.latitude])
      if (path.length < 3) {
        return null
      }

      const active = String(item.id) === String(props.selectedNoParkingZoneId || '')
      const polygon = new AMap.Polygon({
        path,
        bubble: true,
        ...getNoParkingStyle(active)
      })

      polygon.on('click', () => {
        emit('select', {
          type: 'noParkingZone',
          item
        })
      })

      return polygon
    })
    .filter(Boolean)

  if (noParkingOverlays.length) {
    map.add(noParkingOverlays)
  }
}

const renderParkingPointMarkers = () => {
  if (!map || !AMap) {
    return
  }

  parkingPointMarkers = clearOverlayList(parkingPointMarkers)

  parkingPointMarkers = props.parkingPoints
    .filter((item) => Number.isFinite(item.longitude) && Number.isFinite(item.latitude))
    .map((item) => {
      const active = String(item.id) === String(props.selectedParkingPointId || '')
      const marker = new AMap.CircleMarker({
        center: [item.longitude, item.latitude],
        radius: active ? 8 : 6,
        strokeColor: '#2b2b2b',
        strokeWeight: active ? 3 : 2,
        strokeOpacity: 1,
        fillColor: '#f8d65c',
        fillOpacity: 1,
        zIndex: active ? 250 : 240,
        bubble: true
      })

      marker.on('click', () => {
        emit('select', {
          type: 'parkingPoint',
          item
        })
      })

      return marker
    })

  if (parkingPointMarkers.length) {
    map.add(parkingPointMarkers)
  }
}

const renderScooters = () => {
  if (!map || !AMap) {
    return
  }

  scooterMarkers = clearOverlayList(scooterMarkers)

  scooterMarkers = props.scooters
    .filter((item) => Number.isFinite(item.longitude) && Number.isFinite(item.latitude))
    .map((item) => {
      const active = String(item.id) === String(props.selectedScooterId || '')
      const marker = new AMap.CircleMarker({
        center: [item.longitude, item.latitude],
        zIndex: active ? 270 : 260,
        bubble: true,
        ...getScooterStyle(item, active)
      })

      marker.on('click', () => {
        emit('select', {
          type: 'scooter',
          item
        })
      })

      return marker
    })

  if (scooterMarkers.length) {
    map.add(scooterMarkers)
  }
}

const focusZone = () => {
  if (!map || !props.focusZoneId) {
    return false
  }

  const target = props.zones.find((item) => String(item.id) === String(props.focusZoneId))
  if (!target) {
    return false
  }

  const path = parsePolygonPoints(target.polygon).map((point) => [point.longitude, point.latitude])
  if (path.length < 3 || !AMap) {
    return false
  }

  const focusPolygon = new AMap.Polygon({ path })
  map.setFitView([focusPolygon], false, [56, 56, 56, 56])
  return true
}

const fitViewToAll = () => {
  if (!map) {
    return
  }

  const overlays = [...zoneOverlays, ...noParkingOverlays, ...scooterMarkers, ...parkingPointMarkers].filter(Boolean)

  if (!overlays.length) {
    map.setZoomAndCenter(DEFAULT_ZOOM, DEFAULT_CENTER)
    return
  }

  map.setFitView(overlays, false, [48, 48, 48, 48])
}

const renderMapData = ({ fitView = true } = {}) => {
  renderZoneOverlays()
  renderNoParkingOverlays()
  renderParkingPointMarkers()
  renderScooters()

  if (!fitView) {
    return
  }

  if (!focusZone()) {
    fitViewToAll()
  }
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

    renderMapData({ fitView: true })
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

  scooterMarkers = clearOverlayList(scooterMarkers)
  zoneOverlays = clearOverlayList(zoneOverlays)
  noParkingOverlays = clearOverlayList(noParkingOverlays)
  parkingPointMarkers = clearOverlayList(parkingPointMarkers)

  if (map && typeof map.destroy === 'function') {
    map.destroy()
  }

  map = null
  AMap = null
}

watch(
  () => [props.scooters, props.zones, props.noParkingZones, props.parkingPoints],
  () => {
    if (!map || !AMap) {
      return
    }

    renderMapData({ fitView: true })
  },
  { deep: true }
)

watch(
  () => [props.selectedScooterId, props.selectedZoneId, props.selectedNoParkingZoneId, props.selectedParkingPointId],
  () => {
    if (!map || !AMap) {
      return
    }

    renderMapData({ fitView: false })
  }
)

watch(
  () => props.focusZoneId,
  () => {
    if (!map || !AMap) {
      return
    }

    if (!focusZone()) {
      fitViewToAll()
    }
  }
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
      <p>正在初始化地图资源。</p>
    </div>

    <div v-show="isAmapConfigured() && !loadError" ref="mapContainer" class="map-stage" :style="mapHeightStyle"></div>

    <div v-if="!loading && isAmapConfigured() && !loadError && !hasMappableContent" class="map-empty">
      当前没有可展示的地图要素数据。
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