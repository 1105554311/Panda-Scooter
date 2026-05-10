<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import BaseModal from '@/components/BaseModal.vue'
import MapLayerToggleBar from '@/components/MapLayerToggleBar.vue'
import ZoneMapEditor from '@/components/ZoneMapEditor.vue'
import { addZone, editDispatcher, editZone, getDispatcherList, getZoneDetail } from '@/api'
import { ALL_MAP_LAYERS, MAP_LAYER_ZONE } from '@/utils/adminMapVisuals'
import { useUiStore } from '@/stores/ui'
import { getEditorCache, removeEditorCache } from '@/utils/editorCache'
import { fetchAdminMapLayers } from '@/utils/adminMapLayers'
import { validatePolygonPoints } from '@/utils/polygon'
import {
  formatLatLngCenterTextFromCanonicalPolygon,
  formatZonePolygonForApi,
  normalizeZonePolygonForEditor
} from '@/utils/noParkingPolygon'
import { getZoneDispatchers } from '@/utils/zoneDispatchers'

const route = useRoute()
const router = useRouter()
const uiStore = useUiStore()

const CACHE_SCOPE = 'zone'
const FULL_FETCH_PAGE_SIZE = 200
const MAX_FETCH_PAGES = 200
const zoneId = computed(() => Number(route.params.id))
const editing = computed(() => route.name === 'zone-edit')
const saving = ref(false)
const loading = ref(false)
const dispatcherSaving = ref(false)
const dispatcherModalOpen = ref(false)
const selectedDispatcherId = ref('')
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
  dispatchers: []
})

const normalizePositiveId = (value) => {
  const num = Number(value)
  return Number.isFinite(num) && num > 0 ? num : null
}

const getDispatcherAreaId = (item = {}) => {
  return normalizePositiveId(item.areaId ?? item.area_id ?? item.area?.id)
}

const normalizeDispatcherItem = (item = {}) => {
  const id = normalizePositiveId(item.id)
  if (!id) {
    return null
  }

  return {
    ...item,
    id,
    name: typeof item.name === 'string' ? item.name.trim() : '',
    email: typeof item.email === 'string' ? item.email.trim() : '',
    areaId: getDispatcherAreaId(item)
  }
}

const normalizedDispatchers = computed(() => {
  return dispatchers.value
    .map((item) => normalizeDispatcherItem(item))
    .filter(Boolean)
})

const dispatcherById = computed(() => {
  return normalizedDispatchers.value.reduce((result, item) => {
    result.set(item.id, item)
    return result
  }, new Map())
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

const currentZoneId = computed(() => normalizePositiveId(form.value.id) || normalizePositiveId(zoneId.value))

const currentDispatchers = computed(() => {
  const zoneIdValue = currentZoneId.value
  if (!zoneIdValue) {
    return []
  }

  const byId = new Map()
  const addCurrentDispatcher = (item, fallbackAreaId = null) => {
    const normalized = normalizeDispatcherItem(item)
    if (!normalized) {
      return
    }

    const canonical = dispatcherById.value.get(normalized.id)
    const areaId = canonical
      ? getDispatcherAreaId(canonical)
      : getDispatcherAreaId(normalized) || fallbackAreaId
    if (areaId !== zoneIdValue) {
      return
    }

    byId.set(normalized.id, {
      ...normalized,
      ...canonical,
      id: normalized.id,
      name: canonical?.name || normalized.name,
      email: canonical?.email || normalized.email,
      areaId: zoneIdValue
    })
  }

  normalizedDispatchers.value.forEach((item) => addCurrentDispatcher(item))

  getZoneDispatchers({
    id: zoneIdValue,
    dispatchers: form.value.dispatchers
  }).forEach((item) => addCurrentDispatcher(item, zoneIdValue))

  const zoneRecord = zones.value.find((item) => normalizePositiveId(item.id) === zoneIdValue)
  if (zoneRecord) {
    getZoneDispatchers(zoneRecord).forEach((item) => addCurrentDispatcher(item, zoneIdValue))
  }

  return Array.from(byId.values())
})

const currentDispatcherIds = computed(() => {
  return new Set(currentDispatchers.value.map((item) => normalizePositiveId(item.id)).filter(Boolean))
})

const assignedDispatcherIds = computed(() => {
  const result = new Set()

  normalizedDispatchers.value.forEach((item) => {
    if (item.areaId) {
      result.add(item.id)
    }
  })

  zones.value.forEach((zone) => {
    const areaId = normalizePositiveId(zone.id)
    if (!areaId) {
      return
    }

    getZoneDispatchers(zone).forEach((item) => {
      const dispatcherId = normalizePositiveId(item.id)
      if (dispatcherId) {
        result.add(dispatcherId)
      }
    })
  })

  currentDispatchers.value.forEach((item) => {
    result.add(item.id)
  })

  return result
})

const availableDispatchers = computed(() => {
  return normalizedDispatchers.value.filter((item) => {
    return item.id && !currentDispatcherIds.value.has(item.id) && !assignedDispatcherIds.value.has(item.id)
  })
})

const applyRecord = (item) => {
  const selectedDispatchers = getZoneDispatchers(item)

  form.value = {
    id: item.id,
    name: item.name || '',
    polygon: item.polygon ? normalizeZonePolygonForEditor(item.polygon) : '',
    dispatchers: selectedDispatchers
  }
}

const getDispatcherName = (item = {}, index = 0) => {
  return item.name || `调度员 ${index + 1}`
}

const fetchAllItems = async (requester, listKey) => {
  let page = 1
  let apiTotal = null
  const merged = []

  while (page <= MAX_FETCH_PAGES) {
    const response = await requester({
      page,
      pageSize: FULL_FETCH_PAGE_SIZE
    })

    const data = response.data || {}
    const list = Array.isArray(data[listKey]) ? data[listKey] : []
    const totalFromApi = Number(data.total)

    if (Number.isFinite(totalFromApi) && totalFromApi >= 0) {
      apiTotal = totalFromApi
    }

    merged.push(...list)

    if (!list.length) {
      break
    }

    if (apiTotal !== null && merged.length >= apiTotal) {
      break
    }

    if (list.length < FULL_FETCH_PAGE_SIZE) {
      break
    }

    page += 1
  }

  return merged
}

const fetchDispatchers = async () => {
  try {
    dispatchers.value = await fetchAllItems(getDispatcherList, 'dispatcherList')
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
      // id: zoneId.value
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

const refreshDispatcherState = async () => {
  await Promise.all([fetchDispatchers(), fetchDetail(), fetchMapLayers()])
}

const openDispatcherModal = async () => {
  if (!editing.value || !currentZoneId.value) {
    uiStore.pushToast({
      message: '请先保存片区后再添加调度员',
      tone: 'warning'
    })
    return
  }

  selectedDispatcherId.value = ''
  await Promise.all([fetchDispatchers(), fetchMapLayers()])
  dispatcherModalOpen.value = true
}

const addDispatcherToZone = async () => {
  const dispatcherId = normalizePositiveId(selectedDispatcherId.value)

  if (!dispatcherId || !currentZoneId.value) {
    uiStore.pushToast({
      message: '请选择要添加的调度员',
      tone: 'warning'
    })
    return
  }

  const selectedDispatcher = availableDispatchers.value.find((item) => item.id === dispatcherId)
  if (!selectedDispatcher) {
    uiStore.pushToast({
      message: '该调度员已被分配，请刷新后重试',
      tone: 'warning'
    })
    return
  }

  dispatcherSaving.value = true

  try {
    await editDispatcher({
      id: dispatcherId,
      areaId: currentZoneId.value
    })

    uiStore.pushToast({
      message: '调度员已添加到当前片区',
      tone: 'success'
    })

    dispatcherModalOpen.value = false
    await refreshDispatcherState()
  } catch (error) {
  } finally {
    dispatcherSaving.value = false
  }
}

const removeDispatcherFromZone = async (item) => {
  const dispatcherId = normalizePositiveId(item.id)

  if (!dispatcherId) {
    return
  }

  const confirmed = await uiStore.confirmAction({
    title: '删除片区调度员',
    message: `确认从当前片区删除调度员“${item.name || dispatcherId}”吗？`,
    confirmText: '删除',
    tone: 'danger'
  })

  if (!confirmed) {
    return
  }

  dispatcherSaving.value = true

  try {
    await editDispatcher({
      id: dispatcherId,
      areaId: null
    })

    uiStore.pushToast({
      message: '调度员已从当前片区删除',
      tone: 'success'
    })

    await refreshDispatcherState()
  } catch (error) {
  } finally {
    dispatcherSaving.value = false
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
      polygon: formatZonePolygonForApi(validation.points)
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

            <div class="form-field span-12">
              <div class="dispatcher-header">
                <span class="field-label">片区调度员</span>
                <button
                  type="button"
                  class="button-secondary button-compact"
                  :disabled="loading || dispatcherSaving"
                  @click="openDispatcherModal"
                >
                  添加
                </button>
              </div>

              <div v-if="currentDispatchers.length" class="dispatcher-list">
                <div v-for="(item, index) in currentDispatchers" :key="item.id" class="dispatcher-item">
                  <div class="dispatcher-info">
                    <strong>{{ getDispatcherName(item, index) }}</strong>
                    <span v-if="item.email">{{ item.email }}</span>
                  </div>
                  <button
                    type="button"
                    class="button-danger button-compact"
                    :disabled="dispatcherSaving"
                    @click="removeDispatcherFromZone(item)"
                  >
                    删除
                  </button>
                </div>
              </div>

              <div v-else class="empty-state dispatcher-empty">
                {{ editing ? '当前片区暂无调度员。' : '创建片区后可添加调度员。' }}
              </div>
            </div>
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

    <BaseModal v-model="dispatcherModalOpen" title="添加调度员" width="560px">
      <div v-if="availableDispatchers.length" class="add-dispatcher-list">
        <label
          v-for="item in availableDispatchers"
          :key="item.id"
          class="add-dispatcher-option"
          :class="{ selected: selectedDispatcherId === String(item.id) }"
        >
          <input v-model="selectedDispatcherId" type="radio" :value="String(item.id)" />
          <span>
            <strong>{{ item.name }}</strong>
            <small>{{ item.email || '未填写邮箱' }}</small>
          </span>
        </label>
      </div>
      <div v-else class="empty-state">暂无未分配调度员。</div>

      <template #footer>
        <div class="button-row">
          <button type="button" class="button-secondary" @click="dispatcherModalOpen = false">取消</button>
          <button
            type="button"
            class="button-primary"
            :disabled="dispatcherSaving || !selectedDispatcherId"
            @click="addDispatcherToZone"
          >
            {{ dispatcherSaving ? '添加中...' : '添加' }}
          </button>
        </div>
      </template>
    </BaseModal>
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

.dispatcher-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.button-compact {
  min-height: 34px;
  padding: 0 12px;
  font-size: 12px;
}

.dispatcher-list,
.add-dispatcher-list {
  display: grid;
  gap: 10px;
}

.dispatcher-item,
.add-dispatcher-option {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  min-height: 58px;
  border: 1px solid var(--line-soft);
  background: var(--bg-subtle);
  padding: 10px 12px;
}

.dispatcher-info,
.add-dispatcher-option span {
  min-width: 0;
  display: grid;
  gap: 4px;
}

.dispatcher-info strong,
.add-dispatcher-option strong {
  font-size: 14px;
  font-weight: 400;
}

.dispatcher-info span,
.add-dispatcher-option small {
  color: var(--text-subtle);
  font-size: 12px;
  overflow-wrap: anywhere;
}

.dispatcher-empty {
  padding: 18px 12px;
}

.add-dispatcher-option {
  justify-content: flex-start;
  cursor: pointer;
}

.add-dispatcher-option.selected {
  border-color: var(--text-main);
  background: #ffffff;
}

.add-dispatcher-option input {
  flex: 0 0 auto;
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
