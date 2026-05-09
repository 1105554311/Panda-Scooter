<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import MapLayerToggleBar from '@/components/MapLayerToggleBar.vue'
import ZoneMapEditor from '@/components/ZoneMapEditor.vue'
import { addZone, editZone, getDispatcherList, getZoneDetail } from '@/api'
import {
  ALL_MAP_LAYERS,
  MAP_LAYER_NO_PARKING,
  MAP_LAYER_PARKING_POINT,
  MAP_LAYER_SCOOTER,
  MAP_LAYER_ZONE
} from '@/utils/adminMapVisuals'
import { useUiStore } from '@/stores/ui'
import { getEditorCache, removeEditorCache } from '@/utils/editorCache'
import { fetchAdminMapLayers } from '@/utils/adminMapLayers'
import { validatePolygonPoints } from '@/utils/polygon'
import {
  formatLatLngCenterTextFromCanonicalPolygon,
  formatZonePolygonForApi,
  normalizeZonePolygonForEditor
} from '@/utils/noParkingPolygon'

const route = useRoute()
const router = useRouter()
const uiStore = useUiStore()

const CACHE_SCOPE = 'zone'
const zoneId = computed(() => Number(route.params.id))
const editing = computed(() => route.name === 'zone-edit')
const saving = ref(false)
const loading = ref(false)
const dispatchers = ref([])
const zones = ref([])
const noParkingZones = ref([])
const scooters = ref([])
const parkingPoints = ref([])
const visibleTypes = ref([MAP_LAYER_ZONE])
const form = ref({
  id: '',
  name: '',
  polygon: '',
  dispatcherId: ''
})

const otherZones = computed(() => {
  return zones.value.filter((item) => String(item.id) !== String(form.value.id || ''))
})

const pointCount = computed(() => {
  return validatePolygonPoints(form.value.polygon).points.length
})

const polygonCenterText = computed(() => {
  return formatLatLngCenterTextFromCanonicalPolygon(form.value.polygon)
})

const pageTitle = computed(() => (editing.value ? '编辑片区' : '新增片区'))

const applyRecord = (item) => {
  form.value = {
    id: item.id,
    name: item.name || '',
    polygon: item.polygon ? normalizeZonePolygonForEditor(item.polygon) : '',
    dispatcherId: item.dispatcher?.id ? String(item.dispatcher.id) : ''
  }
}

const fetchDispatchers = async () => {
  try {
    const response = await getDispatcherList({
      page: 1,
      pageSize: 200
    })
    dispatchers.value = response.data?.dispatcherList || []
  } catch (error) {
    dispatchers.value = []
  }
}

const fetchMapLayers = async () => {
  try {
    const layers = await fetchAdminMapLayers({
      force: true
    })
    zones.value = layers.zones || []
    noParkingZones.value = layers.noParkingZones || []
    scooters.value = layers.scooters || []
    parkingPoints.value = layers.parkingPoints || []
  } catch (error) {
    zones.value = []
    noParkingZones.value = []
    scooters.value = []
    parkingPoints.value = []
  }
}

const hydrateEditData = () => {
  if (!editing.value || !Number.isFinite(zoneId.value)) {
    return true
  }

  const cachedItem = getEditorCache(CACHE_SCOPE, zoneId.value)
  if (cachedItem) {
    applyRecord(cachedItem)
    return true
  }

  const listItem = zones.value.find((item) => Number(item.id) === zoneId.value)
  if (listItem) {
    applyRecord(listItem)
    return true
  }

  return false
}

const fetchDetail = async () => {
  if (!editing.value || !Number.isFinite(zoneId.value)) {
    return
  }

  try {
    const response = await getZoneDetail({
      areaId: zoneId.value,
      id: zoneId.value
    })
    const data = response.data || {}
    applyRecord(data)
  } catch (error) {
    if (!form.value.id) {
      uiStore.pushToast({
        message: '未获取到片区详情，请返回列表重试',
        tone: 'warning'
      })
      router.replace({ name: 'zones' })
    }
  }
}

const goBack = () => {
  router.push({ name: 'zones' })
}

const submit = async () => {
  if (!form.value.name.trim()) {
    uiStore.pushToast({
      message: '请输入片区名称',
      tone: 'warning'
    })
    return
  }

  const validation = validatePolygonPoints(form.value.polygon)
  if (!validation.valid) {
    uiStore.pushToast({
      message: validation.errors[0] || '请先绘制有效边界',
      tone: 'warning'
    })
    return
  }

  saving.value = true

  try {
    const payload = {
      name: form.value.name.trim(),
      polygon: formatZonePolygonForApi(validation.points),
      dispatcherId: form.value.dispatcherId ? Number(form.value.dispatcherId) : undefined
    }

    if (editing.value) {
      await editZone({
        ...payload,
        id: Number(form.value.id || zoneId.value)
      })
      removeEditorCache(CACHE_SCOPE, zoneId.value)
    } else {
      await addZone(payload)
    }

    uiStore.pushToast({
      message: editing.value ? '片区已更新' : '片区已创建',
      tone: 'success'
    })

    router.push({ name: 'zones' })
  } catch (error) {
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  loading.value = true
  visibleTypes.value = ALL_MAP_LAYERS.filter((item) => item === MAP_LAYER_ZONE)
  await Promise.all([fetchMapLayers(), fetchDispatchers()])

  if (editing.value) {
    const hasCachedData = hydrateEditData()

    if (Number.isFinite(zoneId.value)) {
      await fetchDetail()
    } else if (!hasCachedData) {
      uiStore.pushToast({
        message: '未获取到片区数据，请从列表重新进入',
        tone: 'warning'
      })
      router.replace({ name: 'zones' })
      loading.value = false
      return
    }

    if (!form.value.id) {
      uiStore.pushToast({
        message: '未获取到片区数据，请从列表重新进入',
        tone: 'warning'
      })
      router.replace({ name: 'zones' })
    }
  }

  loading.value = false
})
</script>

<template>
  <div class="section-grid">
    <section class="page-surface editor-page">
      <div class="editor-header">
        <div>
          <h2 class="panel-title">{{ pageTitle }}</h2>
          <p class="panel-description">地图编辑页支持叠加显示片区、禁停区、车辆和停车点，便于精确绘制边界。</p>
        </div>
        <div class="button-row">
          <button type="button" class="button-secondary" @click="goBack">返回列表</button>
          <button type="button" class="button-primary" :disabled="saving || loading" @click="submit">
            {{ saving ? '保存中...' : editing ? '保存片区' : '创建片区' }}
          </button>
        </div>
      </div>

      <div class="editor-grid">
        <aside class="page-surface editor-panel">
          <div class="form-grid">
            <label class="form-field span-12">
              <span class="field-label">片区名称</span>
              <input v-model.trim="form.name" class="field-input" type="text" placeholder="例如：中央商务区" />
            </label>

            <label class="form-field span-12">
              <span class="field-label">调度员</span>
              <select v-model="form.dispatcherId" class="field-select">
                <option value="">未分配</option>
                <option v-for="item in dispatchers" :key="item.id" :value="String(item.id)">
                  {{ item.name }}{{ item.areaId ? ` / 当前片区 ${item.areaId}` : '' }}
                </option>
              </select>
            </label>
          </div>

          <div class="summary-grid">
            <div class="summary-item">
              <span>顶点数</span>
              <strong>{{ pointCount }}</strong>
            </div>
            <div class="summary-item">
              <span>中心点</span>
              <strong>{{ polygonCenterText }}</strong>
            </div>
          </div>
        </aside>

        <section class="page-surface map-panel">
          <ZoneMapEditor
            v-model="form.polygon"
            :zones="otherZones"
            :no-parking-zones="noParkingZones"
            :scooters="scooters"
            :parking-points="parkingPoints"
            :active-zone-id="form.id"
            :visible-types="visibleTypes"
            :readonly="false"
            :height="560"
          />

          <MapLayerToggleBar v-model="visibleTypes" :self-layer="MAP_LAYER_ZONE" />
        </section>
      </div>
    </section>
  </div>
</template>

<style scoped>
.editor-page,
.editor-panel,
.map-panel {
  padding: 24px;
}

.editor-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 24px;
}

.editor-grid {
  display: grid;
  grid-template-columns: 320px minmax(0, 1fr);
  gap: 20px;
  align-items: start;
}

.summary-grid {
  display: grid;
  gap: 12px;
  margin-top: 20px;
}

.summary-item {
  border: 1px solid var(--line-soft);
  background: var(--bg-subtle);
  padding: 16px 18px;
}

.summary-item span {
  display: block;
  color: var(--text-subtle);
  font-size: 13px;
  letter-spacing: 0.08em;
}

.summary-item strong {
  display: block;
  margin-top: 8px;
  font-size: 18px;
  font-weight: 400;
}

.map-panel {
  display: grid;
  gap: 12px;
}

@media (max-width: 1080px) {
  .editor-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 720px) {
  .editor-page,
  .editor-panel,
  .map-panel {
    padding: 20px;
  }

  .editor-header {
    flex-direction: column;
  }
}
</style>
