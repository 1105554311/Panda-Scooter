<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { isAmapConfigured, loadAmap } from '@/utils/amap'
import PolygonPreview from '@/components/PolygonPreview.vue'
import {
  countPolygonPoints,
  formatPolygonPoints,
  getPolygonCenter,
  normalizePoint,
  parsePolygonPoints,
  validatePolygonPoints
} from '@/utils/polygon'

const DEFAULT_CENTER = [103.99015677941316, 30.762680478785253]
const DEFAULT_ZOOM = 15

const props = defineProps({
  modelValue: {
    type: [String, Array, Object],
    default: ''
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
  activeZoneId: {
    type: [String, Number],
    default: ''
  },
  activeNoParkingZoneId: {
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
const validationErrors = ref([])
const drawing = ref(false)
const editorEnabled = ref(false)
const draftPoints = ref([])

let AMap = null
let map = null
let polygonEditor = null
let editablePolygon = null
let zonePolygons = []
let noParkingPolygons = []
let scooterMarkers = []
let parkingPointMarkers = []
let draftPolygon = null
let draftPolyline = null
let draftMarkers = []
let mapResizeObserver = null
let skipModelWatcher = false
let polygonBackupValue = ''

const pointCount = computed(() => countPolygonPoints(props.modelValue))
const hasPolygon = computed(() => pointCount.value > 0)
const draftPointCount = computed(() => draftPoints.value.length)
const polygonCenter = computed(() => getPolygonCenter(props.modelValue))
const centerText = computed(() => {
  if (!polygonCenter.value) {
    return '--'
  }

  return `${polygonCenter.value.longitude.toFixed(5)}, ${polygonCenter.value.latitude.toFixed(5)}`
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

const getPolygonPathPoints = (polygonInstance) => {
  if (!polygonInstance || typeof polygonInstance.getPath !== 'function') {
    return []
  }

  return polygonInstance.getPath().map((point) => normalizePoint(point)).filter(Boolean)
}

const toAmapPath = (polygon) => {
  return parsePolygonPoints(polygon).map((point) => [point.longitude, point.latitude])
}

const getCurrentPolygonValue = () => {
  if (editablePolygon) {
    return formatPolygonPoints(getPolygonPathPoints(editablePolygon))
  }

  return formatPolygonPoints(props.modelValue)
}

const clearOverlayList = (overlayListRef) => {
  if (!map || !overlayListRef.length) {
    return []
  }

  map.remove(overlayListRef)
  return []
}


const setDefaultView = () => {
  if (!map) {
    return
  }

  map.setZoomAndCenter(DEFAULT_ZOOM, DEFAULT_CENTER)
}

const focusEditablePolygon = () => {
  if (!map || !editablePolygon) {
    setDefaultView()
    return
  }

  map.setFitView([editablePolygon], false, [56, 56, 56, 56])
}

const focusDraftGeometry = () => {
  if (!map) {
    return
  }

  const overlays = [draftPolygon, draftPolyline, ...draftMarkers].filter(Boolean)

  if (!overlays.length) {
    setDefaultView()
    return
  }

  map.setFitView(overlays, false, [56, 56, 56, 56])
}

const fitViewToAll = () => {
  if (!map) {
    return
  }

  const overlays = [
    ...zonePolygons,
    ...noParkingPolygons,
    ...scooterMarkers,
    ...parkingPointMarkers,
    editablePolygon,
    draftPolygon,
    draftPolyline,
    ...draftMarkers
  ].filter(Boolean)

  if (!overlays.length) {
    setDefaultView()
    return
  }

  map.setFitView(overlays, false, [56, 56, 56, 56])
}

const getScooterStyle = (scooter) => {
  if (Number(scooter.faultStatus) === 1) {
    return {
      radius: 6,
      fillColor: '#8f3a2d'
    }
  }

  return {
    radius: 6,
    fillColor: '#2f6f46'
  }
}

const renderStaticZonePolygons = () => {
  if (!map || !AMap) {
    return
  }

  zonePolygons = clearOverlayList(zonePolygons)

  zonePolygons = props.zones
    .filter((item) => String(item.id) !== String(props.activeZoneId || ''))
    .map((item) => {
      const path = toAmapPath(item.polygon)
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
    .filter((item) => String(item.id) !== String(props.activeNoParkingZoneId || ''))
    .map((item) => {
      const path = toAmapPath(item.polygon)
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
      const style = getScooterStyle(item)
      return new AMap.CircleMarker({
        center: [item.longitude, item.latitude],
        radius: style.radius,
        strokeColor: '#ffffff',
        strokeWeight: 2,
        strokeOpacity: 1,
        fillColor: style.fillColor,
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
      return new AMap.CircleMarker({
        center: [item.longitude, item.latitude],
        radius: 5,
        strokeColor: '#2b2b2b',
        strokeWeight: 2,
        strokeOpacity: 1,
        fillColor: '#f8d65c',
        fillOpacity: 1,
        zIndex: 245,
        bubble: true
      })
    })

  if (parkingPointMarkers.length) {
    map.add(parkingPointMarkers)
  }
}


const setEditorAdsorbPolygons = () => {
  if (!polygonEditor || typeof polygonEditor.setAdsorbPolygons !== 'function') {
    return
  }

  polygonEditor.setAdsorbPolygons([...zonePolygons, ...noParkingPolygons])
}

const validateAndEmit = (points, shouldEmit = true) => {
  const validation = validatePolygonPoints(points)
  validationErrors.value = validation.valid ? [] : validation.errors

  if (!validation.valid) {
    statusMessage.value = validation.errors[0] || '当前边界无效。'
  } else if (!props.readonly) {
    statusMessage.value = editorEnabled.value
      ? '已进入顶点编辑，可直接拖动、加点或删点。'
      : '边界已生成，可继续编辑顶点，或重新绘制。'
  }

  if (!shouldEmit) {
    return validation
  }

  skipModelWatcher = true
  emit('update:modelValue', formatPolygonPoints(points))
  emit('change', {
    points,
    valid: validation.valid,
    errors: validation.errors
  })

  return validation
}

const clearDraftGeometry = () => {
  if (!map) {
    draftPolygon = null
    draftPolyline = null
    draftMarkers = []
    return
  }

  if (draftPolygon) {
    map.remove(draftPolygon)
  }

  if (draftPolyline) {
    map.remove(draftPolyline)
  }

  if (draftMarkers.length) {
    map.remove(draftMarkers)
  }

  draftPolygon = null
  draftPolyline = null
  draftMarkers = []
}

const renderDraftGeometry = () => {
  if (!map || !AMap) {
    return
  }

  clearDraftGeometry()

  if (!draftPoints.value.length) {
    return
  }

  const path = draftPoints.value.map((point) => [point.longitude, point.latitude])

  draftMarkers = draftPoints.value.map((point, index) => {
    return new AMap.CircleMarker({
      center: [point.longitude, point.latitude],
      radius: index === 0 ? 6 : 5,
      strokeColor: '#0b0e0d',
      strokeWeight: 2,
      strokeOpacity: 1,
      fillColor: '#ffffff',
      fillOpacity: 1,
      zIndex: 260,
      bubble: true
    })
  })

  map.add(draftMarkers)

  if (path.length >= 2) {
    draftPolyline = new AMap.Polyline({
      path,
      strokeColor: '#0b0e0d',
      strokeWeight: 3,
      strokeOpacity: 1,
      lineJoin: 'round',
      lineCap: 'round',
      strokeStyle: 'dashed',
      bubble: true
    })
    map.add(draftPolyline)
  }

  if (path.length >= 3) {
    draftPolygon = new AMap.Polygon({
      path,
      strokeColor: '#0b0e0d',
      strokeWeight: 3,
      strokeOpacity: 1,
      fillColor: '#0b0e0d',
      fillOpacity: 0.12,
      bubble: true
    })
    map.add(draftPolygon)
  }
}

const closeEditor = () => {
  if (polygonEditor && typeof polygonEditor.close === 'function') {
    polygonEditor.close()
  }

  polygonEditor = null
  editorEnabled.value = false
}

const destroyEditablePolygon = () => {
  closeEditor()

  if (editablePolygon && map) {
    map.remove(editablePolygon)
  }

  editablePolygon = null
}

const bindEditorEvents = () => {
  if (!polygonEditor) {
    return
  }

  const sync = () => {
    const points = getPolygonPathPoints(editablePolygon)
    validateAndEmit(points)
  }

  ;['addnode', 'adjust', 'removenode', 'end'].forEach((eventName) => {
    polygonEditor.on(eventName, sync)
  })
}

const openEditor = () => {
  if (props.readonly || !map || !AMap || !editablePolygon) {
    return
  }

  closeEditor()
  polygonEditor = new AMap.PolygonEditor(map, editablePolygon)
  bindEditorEvents()
  setEditorAdsorbPolygons()
  polygonEditor.open()
  editorEnabled.value = true
  statusMessage.value = '已进入顶点编辑，可直接拖动、加点或删点。'
}

const toggleEditor = () => {
  if (!editablePolygon) {
    return
  }

  if (editorEnabled.value) {
    closeEditor()
    statusMessage.value = '已暂停顶点编辑，可重新开启或直接重新绘制。'
    return
  }

  openEditor()
}

const applyEditablePolygon = (
  polygonInstance,
  { fitView = true, shouldEmit = true, autoOpenEditor = !props.readonly } = {}
) => {
  destroyEditablePolygon()
  editablePolygon = polygonInstance

  if (!editablePolygon || !map) {
    validationErrors.value = []
    if (shouldEmit) {
      skipModelWatcher = true
      emit('update:modelValue', '')
    }
    return
  }

  editablePolygon.setOptions({
    strokeColor: '#0b0e0d',
    strokeWeight: 3,
    strokeOpacity: 1,
    fillColor: '#0b0e0d',
    fillOpacity: 0.12
  })

  map.add(editablePolygon)
  const points = getPolygonPathPoints(editablePolygon)
  validateAndEmit(points, shouldEmit)

  if (autoOpenEditor) {
    openEditor()
  }

  if (fitView) {
    focusEditablePolygon()
  }
}

const restorePolygonFromValue = (polygonValue, options = {}) => {
  const points = parsePolygonPoints(polygonValue)

  if (!points.length || !AMap) {
    destroyEditablePolygon()
    return
  }

  const polygonInstance = new AMap.Polygon({
    path: points.map((point) => [point.longitude, point.latitude]),
    bubble: true
  })

  applyEditablePolygon(polygonInstance, {
    fitView: options.fitView ?? true,
    shouldEmit: options.shouldEmit ?? false,
    autoOpenEditor: options.autoOpenEditor ?? !props.readonly
  })
}

const syncPolygonFromModel = ({ fitView = false } = {}) => {
  if (!map || !AMap) {
    return
  }

  const points = parsePolygonPoints(props.modelValue)
  if (!points.length) {
    destroyEditablePolygon()
    validationErrors.value = []
    if (!props.readonly) {
      statusMessage.value = '先点击开始绘制，再在地图上逐点落位。'
    }
    if (fitView) {
      fitViewToAll()
    }
    return
  }

  restorePolygonFromValue(props.modelValue, {
    fitView,
    shouldEmit: false,
    autoOpenEditor: !props.readonly
  })
}

const renderBackgroundLayers = ({ fitView = false } = {}) => {
  renderStaticZonePolygons()
  renderStaticNoParkingPolygons()
  renderScooterMarkers()
  renderParkingPointMarkers()
  setEditorAdsorbPolygons()

  if (fitView) {
    fitViewToAll()
  }
}

const stopDrawing = () => {
  drawing.value = false
}

const updateDraftStatus = () => {
  if (!draftPointCount.value) {
    statusMessage.value = '请在地图上单击添加第一个顶点。'
    return
  }

  if (draftPointCount.value < 3) {
    statusMessage.value = `已添加 ${draftPointCount.value} 个顶点，至少需要 3 个顶点才能完成绘制。`
    return
  }

  statusMessage.value = `已添加 ${draftPointCount.value} 个顶点，可继续补点，或直接点“完成绘制”。`
}

const clearPolygon = () => {
  stopDrawing()
  polygonBackupValue = ''
  draftPoints.value = []
  clearDraftGeometry()
  destroyEditablePolygon()
  validationErrors.value = []
  statusMessage.value = '边界已清空。'
  skipModelWatcher = true
  emit('update:modelValue', '')
  emit('change', {
    points: [],
    valid: false,
    errors: ['请先绘制有效边界。']
  })
  fitViewToAll()
}

const cancelDrawing = () => {
  stopDrawing()
  draftPoints.value = []
  clearDraftGeometry()
  validationErrors.value = []

  if (polygonBackupValue) {
    restorePolygonFromValue(polygonBackupValue, {
      fitView: true,
      shouldEmit: false,
      autoOpenEditor: !props.readonly
    })
    statusMessage.value = '已恢复原边界，可继续拖动顶点微调。'
  } else {
    statusMessage.value = '已取消绘制。'
    fitViewToAll()
  }

  polygonBackupValue = ''
}

const undoLastDraftPoint = () => {
  if (!draftPointCount.value) {
    return
  }

  draftPoints.value = draftPoints.value.slice(0, -1)
  renderDraftGeometry()

  if (draftPointCount.value) {
    focusDraftGeometry()
  } else {
    fitViewToAll()
  }

  updateDraftStatus()
}

const completeDrawing = () => {
  const validation = validatePolygonPoints(draftPoints.value)

  if (!validation.valid) {
    validationErrors.value = validation.errors
    statusMessage.value = validation.errors[0] || '当前边界无效。'
    return
  }

  const polygonInstance = new AMap.Polygon({
    path: validation.points.map((point) => [point.longitude, point.latitude]),
    bubble: true
  })

  stopDrawing()
  draftPoints.value = []
  clearDraftGeometry()
  polygonBackupValue = ''
  applyEditablePolygon(polygonInstance, {
    fitView: true,
    shouldEmit: true,
    autoOpenEditor: !props.readonly
  })
}

const handleDrawingClick = (event) => {
  if (!drawing.value) {
    return
  }

  const point = normalizePoint(event.lnglat)
  if (!point) {
    return
  }

  draftPoints.value = [...draftPoints.value, point]
  renderDraftGeometry()
  focusDraftGeometry()
  validationErrors.value = []
  updateDraftStatus()
}

const startDrawing = () => {
  if (props.readonly || !map) {
    return
  }

  polygonBackupValue = getCurrentPolygonValue()
  stopDrawing()
  destroyEditablePolygon()
  draftPoints.value = []
  clearDraftGeometry()
  validationErrors.value = []
  drawing.value = true
  updateDraftStatus()
}

const setupMap = async () => {
  if (!isAmapConfigured()) {
    loading.value = false
    statusMessage.value = '未配置高德 Key，地图编辑器暂不可用。'
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
    map.on('click', handleDrawingClick)

    renderBackgroundLayers({ fitView: false })
    syncPolygonFromModel({ fitView: true })

    if (!hasPolygon.value) {
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

    if (!props.readonly && !countPolygonPoints(props.modelValue)) {
      statusMessage.value = '先点击开始绘制，再在地图上逐点落位。'
    }
  } catch (error) {
    loadError.value = error?.message || '地图加载失败。'
    statusMessage.value = '地图不可用，已切换到静态预览。'
  } finally {
    loading.value = false
  }
}

const destroyMap = () => {
  if (mapResizeObserver) {
    mapResizeObserver.disconnect()
    mapResizeObserver = null
  }

  stopDrawing()
  draftPoints.value = []
  clearDraftGeometry()
  destroyEditablePolygon()
  zonePolygons = clearOverlayList(zonePolygons)
  noParkingPolygons = clearOverlayList(noParkingPolygons)
  scooterMarkers = clearOverlayList(scooterMarkers)
  parkingPointMarkers = clearOverlayList(parkingPointMarkers)

  if (map && typeof map.destroy === 'function') {
    map.off('click', handleDrawingClick)
    map.destroy()
  }

  map = null
  AMap = null
}

watch(
  () => props.modelValue,
  () => {
    if (skipModelWatcher) {
      skipModelWatcher = false
      return
    }

    if (!map || !AMap || drawing.value) {
      return
    }

    syncPolygonFromModel()
  }
)

watch(
  () => [props.zones, props.noParkingZones, props.scooters, props.parkingPoints],
  () => {
    if (!map || !AMap) {
      return
    }

    renderBackgroundLayers({ fitView: props.readonly && !hasPolygon.value })
  },
  { deep: true }
)

watch(
  () => [props.activeZoneId, props.activeNoParkingZoneId],
  () => {
    if (!map || !AMap) {
      return
    }

    renderBackgroundLayers({ fitView: false })
  }
)

onMounted(setupMap)
onBeforeUnmount(destroyMap)
</script>

<template>
  <div class="zone-map-editor">
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
      <p>正在初始化地图编辑器。</p>
    </div>

    <div
      v-show="isAmapConfigured() && !loadError"
      ref="mapContainer"
      class="map-stage"
      :style="mapHeightStyle"
    ></div>

    <div v-if="!isAmapConfigured() || loadError" class="fallback-preview">
      <PolygonPreview :polygon="modelValue" />
    </div>

    <div v-if="!readonly" class="map-toolbar">
      <button
        v-if="!drawing"
        type="button"
        class="button-primary"
        :disabled="loading || Boolean(loadError) || !isAmapConfigured()"
        @click="startDrawing"
      >
        {{ hasPolygon ? '重新绘制' : '开始绘制' }}
      </button>

      <button
        v-if="hasPolygon && !drawing"
        type="button"
        class="button-secondary"
        :disabled="loading || Boolean(loadError) || !isAmapConfigured()"
        @click="toggleEditor"
      >
        {{ editorEnabled ? '暂停顶点编辑' : '继续编辑顶点' }}
      </button>

      <button
        v-if="drawing"
        type="button"
        class="button-secondary"
        :disabled="loading || draftPointCount === 0"
        @click="undoLastDraftPoint"
      >
        撤销上一步
      </button>

      <button
        v-if="drawing"
        type="button"
        class="button-primary"
        :disabled="loading || draftPointCount < 3"
        @click="completeDrawing"
      >
        完成绘制
      </button>

      <button v-if="drawing" type="button" class="button-secondary" :disabled="loading" @click="cancelDrawing">
        取消绘制
      </button>

      <button
        type="button"
        class="button-secondary"
        :disabled="loading || Boolean(loadError) || !isAmapConfigured()"
        @click="drawing ? focusDraftGeometry() : hasPolygon ? focusEditablePolygon() : fitViewToAll()"
      >
        {{ drawing ? '回到绘制区域' : hasPolygon ? '回到当前边界' : '查看全部要素' }}
      </button>

      <button type="button" class="button-secondary" :disabled="loading" @click="clearPolygon">
        清空边界
      </button>
    </div>

    <div class="map-meta">
      <div class="meta-item">
        <span>{{ drawing ? '绘制顶点数' : '顶点数' }}</span>
        <strong>{{ drawing ? draftPointCount : pointCount }}</strong>
      </div>
      <div class="meta-item">
        <span>中心点</span>
        <strong>{{ centerText }}</strong>
      </div>
    </div>

    <p v-if="statusMessage" class="map-hint">{{ statusMessage }}</p>

    <div v-if="validationErrors.length" class="map-errors">
      <p v-for="message in validationErrors" :key="message">{{ message }}</p>
    </div>
  </div>
</template>

<style scoped>
.zone-map-editor {
  display: grid;
  gap: 14px;
}

.map-stage,
.map-state,
.fallback-preview,
.map-meta,
.map-errors {
  border: 1px solid #e5e5e2;
  background: #ffffff;
}

.map-stage {
  width: 100%;
  min-height: 320px;
  overflow: hidden;
}

.map-state,
.map-errors {
  padding: 18px 20px;
}

.map-state strong,
.map-errors p {
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

.map-toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.map-meta {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.meta-item {
  padding: 16px 18px;
  border-right: 1px solid #e5e5e2;
}

.meta-item:last-child {
  border-right: none;
}

.meta-item span {
  display: block;
  color: #737373;
  font-size: 13px;
  letter-spacing: 0.08em;
}

.meta-item strong {
  display: block;
  margin-top: 8px;
  font-size: 18px;
  font-weight: 400;
  color: #0b0e0d;
}

.map-hint {
  margin: 0;
  color: #737373;
  font-size: 13px;
}

.map-errors {
  background: #fafaf8;
}

.map-errors p + p {
  margin-top: 8px;
}


@media (max-width: 720px) {
  .map-meta {
    grid-template-columns: 1fr;
  }

  .meta-item {
    border-right: none;
    border-bottom: 1px solid #e5e5e2;
  }

  .meta-item:last-child {
    border-bottom: none;
  }
}
</style>
