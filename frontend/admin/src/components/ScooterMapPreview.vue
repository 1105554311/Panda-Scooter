<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { isAmapConfigured, loadAmap } from '@/utils/amap'
import {
  ALL_MAP_LAYERS,
  MAP_ICON_PATHS,
  MAP_ICON_SIZE,
  MAP_LAYER_NO_PARKING,
  MAP_LAYER_PARKING_POINT,
  MAP_LAYER_SCOOTER,
  MAP_LAYER_ZONE,
  buildIconMarkerContent,
  isLayerVisible
} from '@/utils/adminMapVisuals'
import { toNoParkingAmapPath, toZoneAmapPath } from '@/utils/noParkingPolygon'

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
  visibleTypes: {
    type: Array,
    default: () => ALL_MAP_LAYERS.slice()
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
let noParkingMarkers = []
let mapResizeObserver = null

const mapHeightStyle = computed(() => ({
  height: `${props.height}px`
}))

const configHint = computed(() => {
  if (isAmapConfigured()) {
    return ''
  }

  return '请在 admin 环境变量中配置 VITE_AMAP_WEB_KEY。若控制台要求安全密钥，请同时配置 VITE_AMAP_SECURITY_JS_CODE。'
})

const hasMappableContent = computed(() => {
  const hasScooters =
    isLayerVisible(props.visibleTypes, MAP_LAYER_SCOOTER)
    && props.scooters.some((item) => Number.isFinite(item.longitude) && Number.isFinite(item.latitude))
  const hasZones =
    isLayerVisible(props.visibleTypes, MAP_LAYER_ZONE)
    && props.zones.some((item) => parsePolygonPoints(item.polygon).length >= 3)
  const hasNoParkingZones =
    isLayerVisible(props.visibleTypes, MAP_LAYER_NO_PARKING)
    && props.noParkingZones.some((item) => parsePolygonPoints(item.polygon).length >= 3)
  const hasParkingPoints =
    isLayerVisible(props.visibleTypes, MAP_LAYER_PARKING_POINT)
    && props.parkingPoints.some((item) => Number.isFinite(item.longitude) && Number.isFinite(item.latitude))

  return hasScooters || hasZones || hasNoParkingZones || hasParkingPoints
})

const clearOverlayList = (overlayListRef) => {
  if (!map || !overlayListRef.length) {
    return []
  }

  map.remove(overlayListRef)
  return []
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

const createIconMarker = ({ position, iconType, active = false, badgeColor = '', zIndex = 250 }) => {
  const size = MAP_ICON_SIZE[iconType]
  const width = active ? size.activeWidth : size.width
  const height = active ? size.activeHeight : size.height

  return new AMap.Marker({
    position,
    content: buildIconMarkerContent({
      src: MAP_ICON_PATHS[iconType],
      width,
      height,
      badgeColor
    }),
    offset: new AMap.Pixel(Math.round(-width / 2), Math.round(-height / 2)),
    zIndex,
    bubble: true
  })
}

const resolvePolygonCenter = (item, path) => {
  const center = item?.center
  if (center) {
    if (Array.isArray(center) && center.length >= 2) {
      const first = Number(center[0])
      const second = Number(center[1])
      if (Number.isFinite(first) && Number.isFinite(second)) {
        if (Math.abs(first) <= 90 && Math.abs(second) <= 180) {
          return [second, first]
        }
        return [first, second]
      }
    } else if (typeof center === 'string') {
      const values = center.split(',').map((segment) => Number(segment.trim()))
      if (values.length >= 2 && Number.isFinite(values[0]) && Number.isFinite(values[1])) {
        if (Math.abs(values[0]) <= 90 && Math.abs(values[1]) <= 180) {
          return [values[1], values[0]]
        }
        return [values[0], values[1]]
      }
    } else if (typeof center === 'object') {
      const latitude = Number(center.latitude)
      const longitude = Number(center.longitude)
      if (Number.isFinite(latitude) && Number.isFinite(longitude)) {
        return [longitude, latitude]
      }
    }
  }

  if (!Array.isArray(path) || !path.length) {
    return null
  }

  const total = path.reduce(
    (result, point) => {
      return {
        longitude: result.longitude + Number(point[0]),
        latitude: result.latitude + Number(point[1])
      }
    },
    { longitude: 0, latitude: 0 }
  )

  return [total.longitude / path.length, total.latitude / path.length]
}

const renderZoneOverlays = () => {
  if (!map || !AMap) {
    return
  }

  zoneOverlays = clearOverlayList(zoneOverlays)
  if (!isLayerVisible(props.visibleTypes, MAP_LAYER_ZONE)) {
    return
  }

  zoneOverlays = props.zones
    .map((item) => {
      const path = toZoneAmapPath(item.polygon)
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
          type: MAP_LAYER_ZONE,
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
  if (!isLayerVisible(props.visibleTypes, MAP_LAYER_NO_PARKING)) {
    return
  }

  noParkingOverlays = props.noParkingZones
    .map((item) => {
      const path = toNoParkingAmapPath(item.polygon)
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
          type: MAP_LAYER_NO_PARKING,
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

const renderNoParkingMarkers = () => {
  if (!map || !AMap) {
    return
  }

  noParkingMarkers = clearOverlayList(noParkingMarkers)
  if (!isLayerVisible(props.visibleTypes, MAP_LAYER_NO_PARKING)) {
    return
  }

  noParkingMarkers = props.noParkingZones
    .map((item) => {
      const path = toNoParkingAmapPath(item.polygon)
      if (path.length < 3) {
        return null
      }

      const active = String(item.id) === String(props.selectedNoParkingZoneId || '')
      const center = resolvePolygonCenter(item, path)
      if (!center) {
        return null
      }

      const marker = createIconMarker({
        position: center,
        iconType: 'noParkingZone',
        active,
        zIndex: active ? 280 : 270
      })

      marker.on('click', () => {
        emit('select', {
          type: MAP_LAYER_NO_PARKING,
          item
        })
      })

      return marker
    })
    .filter(Boolean)

  if (noParkingMarkers.length) {
    map.add(noParkingMarkers)
  }
}

const renderParkingPointMarkers = () => {
  if (!map || !AMap) {
    return
  }

  parkingPointMarkers = clearOverlayList(parkingPointMarkers)
  if (!isLayerVisible(props.visibleTypes, MAP_LAYER_PARKING_POINT)) {
    return
  }

  parkingPointMarkers = props.parkingPoints
    .filter((item) => Number.isFinite(item.longitude) && Number.isFinite(item.latitude))
    .map((item) => {
      const active = String(item.id) === String(props.selectedParkingPointId || '')
      const marker = createIconMarker({
        position: [item.longitude, item.latitude],
        iconType: 'parkingPoint',
        active,
        zIndex: active ? 290 : 250
      })

      marker.on('click', () => {
        emit('select', {
          type: MAP_LAYER_PARKING_POINT,
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
  if (!isLayerVisible(props.visibleTypes, MAP_LAYER_SCOOTER)) {
    return
  }

  scooterMarkers = props.scooters
    .filter((item) => Number.isFinite(item.longitude) && Number.isFinite(item.latitude))
    .map((item) => {
      const active = String(item.id) === String(props.selectedScooterId || '')
      const marker = createIconMarker({
        position: [item.longitude, item.latitude],
        iconType: 'scooter',
        active,
        badgeColor: Number(item.faultStatus) === 1 ? '#d50000' : '',
        zIndex: active ? 300 : 260
      })

      marker.on('click', () => {
        emit('select', {
          type: MAP_LAYER_SCOOTER,
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
  if (!map || !props.focusZoneId || !isLayerVisible(props.visibleTypes, MAP_LAYER_ZONE)) {
    return false
  }

  const target = props.zones.find((item) => String(item.id) === String(props.focusZoneId))
  if (!target) {
    return false
  }

  const path = toZoneAmapPath(target.polygon)
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

  const overlays = [
    ...zoneOverlays,
    ...noParkingOverlays,
    ...noParkingMarkers,
    ...scooterMarkers,
    ...parkingPointMarkers
  ].filter(Boolean)

  if (!overlays.length) {
    map.setZoomAndCenter(DEFAULT_ZOOM, DEFAULT_CENTER)
    return
  }

  map.setFitView(overlays, false, [48, 48, 48, 48])
}

const renderMapData = ({ fitView = true } = {}) => {
  renderZoneOverlays()
  renderNoParkingOverlays()
  renderNoParkingMarkers()
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
  noParkingMarkers = clearOverlayList(noParkingMarkers)

  if (map && typeof map.destroy === 'function') {
    map.destroy()
  }

  map = null
  AMap = null
}

watch(
  () => [props.scooters, props.zones, props.noParkingZones, props.parkingPoints, props.visibleTypes],
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
