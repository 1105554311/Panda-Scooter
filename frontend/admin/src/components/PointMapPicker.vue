<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { isAmapConfigured, loadAmap } from '@/utils/amap'
import { parsePolygonPoints } from '@/utils/polygon'

const DEFAULT_CENTER = [103.99015677941316, 30.762680478785253]
const DEFAULT_ZOOM = 15

const props = defineProps({
  modelValue: {
    type: Object,
    default: () => ({})
  },
  zones: {
    type: Array,
    default: () => []
  },
  noParkingZones: {
    type: Array,
    default: () => []
  },
  scooters: {
    type: Array,
    default: () => []
  },
  parkingPoints: {
    type: Array,
    default: () => []
  },
  activeParkingPointId: {
    type: [String, Number],
    default: ''
  },
  readonly: {
    type: Boolean,
    default: false
  },
  height: {
    type: Number,
    default: 420
  }
})

const emit = defineEmits(['update:modelValue', 'change'])

const mapContainer = ref(null)
const loading = ref(true)
const loadError = ref('')
const statusMessage = ref('')

let AMap = null
let map = null
let marker = null
let zonePolygons = []
let noParkingPolygons = []
let scooterMarkers = []
let parkingPointMarkers = []
let labels = []
let mapResizeObserver = null
let skipModelWatcher = false

const normalizedPoint = computed(() => {
  const longitude = Number(props.modelValue?.longtitude ?? props.modelValue?.longitude)
  const latitude = Number(props.modelValue?.latitude)

  if (!Number.isFinite(longitude) || !Number.isFinite(latitude)) {
    return null
  }

  return {
    longtitude: longitude,
    latitude
  }
})

const mapHeightStyle = computed(() => ({
  height: `${props.height}px`
}))

const configHint = computed(() => {
  if (isAmapConfigured()) {
    return ''
  }

  return '请在 admin 环境变量中配置 VITE_AMAP_WEB_KEY。如控制台要求安全密钥，请同时配置 VITE_AMAP_SECURITY_JS_CODE。'
})

const hasPoint = computed(() => Boolean(normalizedPoint.value))
const positionText = computed(() => {
  if (!normalizedPoint.value) {
    return '--'
  }

  return `${normalizedPoint.value.longtitude.toFixed(6)}, ${normalizedPoint.value.latitude.toFixed(6)}`
})

const clearOverlayList = (overlayListRef) => {
  if (!map || !overlayListRef.length) {
    return []
  }

  map.remove(overlayListRef)
  return []
}

const getPolygonCenterFromPath = (path) => {
  if (!path.length) {
    return null
  }

  const total = path.reduce(
    (result, point) => ({
      longitude: result.longitude + Number(point[0]),
      latitude: result.latitude + Number(point[1])
    }),
    { longitude: 0, latitude: 0 }
  )

  return [total.longitude / path.length, total.latitude / path.length]
}

const createLabelMarker = (position, text) => {
  if (!AMap || !Array.isArray(position) || position.length !== 2 || !text) {
    return null
  }

  return new AMap.Marker({
    position,
    zIndex: 310,
    bubble: true,
    offset: new AMap.Pixel(0, -4),
    content: `<div class="map-name-label">${text}</div>`,
    anchor: 'bottom-center'
  })
}

const emitPoint = (point) => {
  skipModelWatcher = true
  emit('update:modelValue', point ? { ...point } : { latitude: '', longtitude: '' })
  emit('change', point ? { ...point } : null)
}

const setDefaultView = () => {
  if (!map) {
    return
  }

  map.setZoomAndCenter(DEFAULT_ZOOM, DEFAULT_CENTER)
}

const removeMarker = () => {
  if (marker && map) {
    map.remove(marker)
  }

  marker = null
}

const focusCurrentPoint = () => {
  if (!map || !normalizedPoint.value) {
    return
  }

  map.setZoomAndCenter(17, [normalizedPoint.value.longtitude, normalizedPoint.value.latitude])
}

const fitViewToAll = () => {
  if (!map) {
    return
  }

  const overlays = [...zonePolygons, ...noParkingPolygons, ...scooterMarkers, ...parkingPointMarkers, marker].filter(Boolean)
  if (!overlays.length) {
    setDefaultView()
    return
  }

  map.setFitView(overlays, false, [48, 48, 48, 48])
}

const bindMarkerEvents = () => {
  if (!marker || props.readonly) {
    return
  }

  marker.on('dragend', (event) => {
    const point = {
      longtitude: Number(event.lnglat?.getLng?.()),
      latitude: Number(event.lnglat?.getLat?.())
    }

    if (!Number.isFinite(point.longtitude) || !Number.isFinite(point.latitude)) {
      return
    }

    statusMessage.value = '停车点位置已更新。'
    emitPoint(point)
  })
}

const applyMarker = (point, { center = true, shouldEmit = false } = {}) => {
  removeMarker()

  if (!map || !AMap || !point) {
    return
  }

  marker = new AMap.Marker({
    position: [point.longtitude, point.latitude],
    draggable: !props.readonly,
    cursor: props.readonly ? 'default' : 'move'
  })

  map.add(marker)
  bindMarkerEvents()

  if (center) {
    map.setZoomAndCenter(17, [point.longtitude, point.latitude])
  }

  if (shouldEmit) {
    emitPoint(point)
  }

  if (!props.readonly) {
    statusMessage.value = '可拖动标记微调停车点位置。'
  }
}

const renderStaticZonePolygons = () => {
  if (!map || !AMap) {
    return
  }

  zonePolygons = clearOverlayList(zonePolygons)

  zonePolygons = props.zones
    .map((item) => {
      const path = parsePolygonPoints(item.polygon).map((point) => [point.longitude, point.latitude])
      if (path.length < 3) {
        return null
      }

      return new AMap.Polygon({
        path,
        strokeColor: '#0a5e9a',
        strokeWeight: 2,
        strokeOpacity: 0.85,
        fillColor: '#4d9ed8',
        fillOpacity: 0.12,
        bubble: true
      })
    })
    .filter(Boolean)

  if (zonePolygons.length) {
    map.add(zonePolygons)
  }
}

const renderStaticNoParkingPolygons = () => {
  if (!map || !AMap) {
    return
  }

  noParkingPolygons = clearOverlayList(noParkingPolygons)

  noParkingPolygons = props.noParkingZones
    .map((item) => {
      const path = parsePolygonPoints(item.polygon).map((point) => [point.longitude, point.latitude])
      if (path.length < 3) {
        return null
      }

      return new AMap.Polygon({
        path,
        strokeColor: '#8f3a2d',
        strokeWeight: 2,
        strokeOpacity: 0.9,
        strokeStyle: 'dashed',
        fillColor: '#cf7b6f',
        fillOpacity: 0.15,
        bubble: true
      })
    })
    .filter(Boolean)

  if (noParkingPolygons.length) {
    map.add(noParkingPolygons)
  }
}

const renderScooterMarkers = () => {
  if (!map || !AMap) {
    return
  }

  scooterMarkers = clearOverlayList(scooterMarkers)

  scooterMarkers = props.scooters
    .filter((item) => Number.isFinite(item.longitude) && Number.isFinite(item.latitude))
    .map((item) => {
      const isFault = Number(item.faultStatus) === 1
      return new AMap.CircleMarker({
        center: [item.longitude, item.latitude],
        radius: 5,
        strokeColor: '#ffffff',
        strokeWeight: 2,
        strokeOpacity: 1,
        fillColor: isFault ? '#8f3a2d' : '#2f6f46',
        fillOpacity: 1,
        zIndex: 250,
        bubble: true
      })
    })

  if (scooterMarkers.length) {
    map.add(scooterMarkers)
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
      const isActive = String(item.id) === String(props.activeParkingPointId || '')
      return new AMap.CircleMarker({
        center: [item.longitude, item.latitude],
        radius: isActive ? 7 : 5,
        strokeColor: '#2b2b2b',
        strokeWeight: isActive ? 3 : 2,
        strokeOpacity: 1,
        fillColor: '#f8d65c',
        fillOpacity: 1,
        zIndex: isActive ? 255 : 245,
        bubble: true
      })
    })

  if (parkingPointMarkers.length) {
    map.add(parkingPointMarkers)
  }
}

const renderLabels = () => {
  if (!map || !AMap) {
    return
  }

  labels = clearOverlayList(labels)

  const zoneLabels = props.zones
    .map((item) => {
      const path = parsePolygonPoints(item.polygon).map((point) => [point.longitude, point.latitude])
      if (path.length < 3) {
        return null
      }

      return createLabelMarker(getPolygonCenterFromPath(path), item.label || item.name || `片区 #${item.id}`)
    })
    .filter(Boolean)

  const noParkingLabels = props.noParkingZones
    .map((item) => {
      const path = parsePolygonPoints(item.polygon).map((point) => [point.longitude, point.latitude])
      if (path.length < 3) {
        return null
      }

      return createLabelMarker(getPolygonCenterFromPath(path), item.label || item.name || `禁停区 #${item.id}`)
    })
    .filter(Boolean)

  const parkingPointLabels = props.parkingPoints
    .filter((item) => Number.isFinite(item.longitude) && Number.isFinite(item.latitude))
    .map((item) => {
      return createLabelMarker([item.longitude, item.latitude], item.name || `停车点 #${item.id}`)
    })
    .filter(Boolean)

  labels = [...zoneLabels, ...noParkingLabels, ...parkingPointLabels]

  if (labels.length) {
    map.add(labels)
  }
}

const renderBackgroundLayers = ({ fitView = false } = {}) => {
  renderStaticZonePolygons()
  renderStaticNoParkingPolygons()
  renderScooterMarkers()
  renderParkingPointMarkers()
  renderLabels()

  if (fitView) {
    fitViewToAll()
  }
}

const syncMarkerFromModel = ({ center = false } = {}) => {
  if (!map || !AMap) {
    return
  }

  if (!normalizedPoint.value) {
    removeMarker()
    if (!props.readonly) {
      statusMessage.value = '点击地图设置停车点，拖动标记可以继续微调。'
    }
    fitViewToAll()
    return
  }

  applyMarker(normalizedPoint.value, {
    center,
    shouldEmit: false
  })
}

const handleMapClick = (event) => {
  if (props.readonly) {
    return
  }

  const point = {
    longtitude: Number(event.lnglat?.getLng?.()),
    latitude: Number(event.lnglat?.getLat?.())
  }

  if (!Number.isFinite(point.longtitude) || !Number.isFinite(point.latitude)) {
    return
  }

  applyMarker(point, {
    center: false,
    shouldEmit: true
  })
  statusMessage.value = '停车点位置已更新。'
}

const clearPoint = () => {
  removeMarker()
  statusMessage.value = '停车点已清空。'
  emitPoint(null)
  fitViewToAll()
}

const setupMap = async () => {
  if (!isAmapConfigured()) {
    loading.value = false
    statusMessage.value = '未配置高德 Key，地图选点器暂不可用。'
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

    map.addControl(
      new AMap.ToolBar({
        position: 'RB'
      })
    )
    map.addControl(new AMap.Scale())
    map.on('click', handleMapClick)

    renderBackgroundLayers({ fitView: false })
    syncMarkerFromModel({ center: true })

    if (!hasPoint.value) {
      fitViewToAll()
    }

    if (typeof ResizeObserver !== 'undefined') {
      mapResizeObserver = new ResizeObserver(() => {
        if (map && typeof map.resize === 'function') {
          map.resize()
        }
      })
      mapResizeObserver.observe(mapContainer.value)
    }

    if (!props.readonly && !normalizedPoint.value) {
      statusMessage.value = '点击地图设置停车点，拖动标记可以继续微调。'
    }
  } catch (error) {
    loadError.value = error?.message || '地图加载失败。'
    statusMessage.value = '地图不可用，已切换到坐标预览。'
  } finally {
    loading.value = false
  }
}

const destroyMap = () => {
  if (mapResizeObserver) {
    mapResizeObserver.disconnect()
    mapResizeObserver = null
  }

  removeMarker()
  zonePolygons = clearOverlayList(zonePolygons)
  noParkingPolygons = clearOverlayList(noParkingPolygons)
  scooterMarkers = clearOverlayList(scooterMarkers)
  parkingPointMarkers = clearOverlayList(parkingPointMarkers)
  labels = clearOverlayList(labels)

  if (map && typeof map.destroy === 'function') {
    map.destroy()
  }

  map = null
  AMap = null
}

watch(
  () => [props.modelValue?.latitude, props.modelValue?.longtitude, props.modelValue?.longitude],
  () => {
    if (skipModelWatcher) {
      skipModelWatcher = false
      return
    }

    if (!map || !AMap) {
      return
    }

    syncMarkerFromModel()
  }
)

watch(
  () => [props.zones, props.noParkingZones, props.scooters, props.parkingPoints],
  () => {
    if (!map || !AMap) {
      return
    }

    renderBackgroundLayers({ fitView: !hasPoint.value })
  },
  { deep: true }
)

watch(
  () => props.activeParkingPointId,
  () => {
    if (!map || !AMap) {
      return
    }

    renderParkingPointMarkers()
  }
)

onMounted(setupMap)
onBeforeUnmount(destroyMap)
</script>

<template>
  <div class="point-map-picker">
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
      <p>正在初始化地图选点器。</p>
    </div>

    <div
      v-show="isAmapConfigured() && !loadError"
      ref="mapContainer"
      class="map-stage"
      :style="mapHeightStyle"
    ></div>

    <div v-if="!isAmapConfigured() || loadError" class="fallback-preview">
      <div class="preview-grid">
        <div class="preview-item">
          <span>经度</span>
          <strong>{{ normalizedPoint ? normalizedPoint.longtitude.toFixed(6) : '--' }}</strong>
        </div>
        <div class="preview-item">
          <span>纬度</span>
          <strong>{{ normalizedPoint ? normalizedPoint.latitude.toFixed(6) : '--' }}</strong>
        </div>
      </div>
    </div>

    <div v-if="!readonly" class="map-toolbar">
      <button
        type="button"
        class="button-secondary"
        :disabled="loading || Boolean(loadError) || !isAmapConfigured()"
        @click="hasPoint ? focusCurrentPoint() : fitViewToAll()"
      >
        {{ hasPoint ? '定位当前点位' : '查看全部要素' }}
      </button>
      <button type="button" class="button-secondary" :disabled="loading || !hasPoint" @click="clearPoint">
        清空点位
      </button>
    </div>

    <div class="map-meta">
      <div class="meta-item">
        <span>坐标</span>
        <strong>{{ positionText }}</strong>
      </div>
    </div>

    <p v-if="statusMessage" class="map-hint">{{ statusMessage }}</p>
  </div>
</template>

<style scoped>
.point-map-picker {
  display: grid;
  gap: 14px;
}

.map-stage,
.map-state,
.fallback-preview,
.map-meta {
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
  display: block;
  margin: 0;
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

.fallback-preview {
  padding: 18px;
}

.preview-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.preview-item {
  padding: 16px 18px;
  border-right: 1px solid #e5e5e2;
}

.preview-item:last-child {
  border-right: none;
}

.preview-item span,
.meta-item span {
  display: block;
  color: #737373;
  font-size: 13px;
  letter-spacing: 0.08em;
}

.preview-item strong,
.meta-item strong {
  display: block;
  margin-top: 8px;
  font-size: 18px;
  font-weight: 400;
  color: #0b0e0d;
}

.map-toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.map-meta {
  display: grid;
}

.meta-item {
  padding: 16px 18px;
}

.map-hint {
  margin: 0;
  color: #737373;
  font-size: 13px;
}

:global(.map-name-label) {
  display: inline-block;
  max-width: 220px;
  padding: 2px 8px;
  border: 1px solid #d8d8d3;
  background: rgba(255, 255, 255, 0.95);
  color: #1b1b1b;
  font-size: 12px;
  line-height: 1.4;
  white-space: nowrap;
  text-overflow: ellipsis;
  overflow: hidden;
  border-radius: 2px;
}

@media (max-width: 720px) {
  .preview-grid {
    grid-template-columns: 1fr;
  }

  .preview-item {
    border-right: none;
    border-bottom: 1px solid #e5e5e2;
  }

  .preview-item:last-child {
    border-bottom: none;
  }
}
</style>
