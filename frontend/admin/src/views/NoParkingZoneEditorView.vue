<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import ZoneMapEditor from '@/components/ZoneMapEditor.vue'
import { addNoParkingZone, editNoParkingZone, getNoParkingZoneList } from '@/api'
import { useUiStore } from '@/stores/ui'
import { getEditorCache, removeEditorCache } from '@/utils/editorCache'
import { formatPolygonPoints, getPolygonCenter, validatePolygonPoints } from '@/utils/polygon'

const route = useRoute()
const router = useRouter()
const uiStore = useUiStore()

const CACHE_SCOPE = 'no-parking-zone'
const zoneId = computed(() => Number(route.params.id))
const editing = computed(() => route.name === 'no-parking-zone-edit')
const saving = ref(false)
const loading = ref(false)
const zones = ref([])
const form = ref({
  id: '',
  name: '',
  polygon: '',
  status: 1,
  createTime: ''
})

const otherZones = computed(() => {
  return zones.value.filter((item) => String(item.id) !== String(form.value.id || ''))
})

const pointCount = computed(() => {
  return validatePolygonPoints(form.value.polygon).points.length
})

const polygonCenterText = computed(() => {
  const center = getPolygonCenter(form.value.polygon)
  if (!center) {
    return '--'
  }

  return `${center.longitude.toFixed(5)}, ${center.latitude.toFixed(5)}`
})

const pageTitle = computed(() => (editing.value ? '编辑禁停区' : '新增禁停区'))

const applyRecord = (item) => {
  form.value = {
    id: item.id,
    name: item.name || '',
    polygon: item.polygon ? formatPolygonPoints(item.polygon) : '',
    status: Number(item.status ?? 1),
    createTime: item.createTime || ''
  }
}

const fetchZones = async () => {
  try {
    const response = await getNoParkingZoneList({
      page: 1,
      pageSize: 500
    })
    zones.value = response.data?.areaList || []
  } catch (error) {
    zones.value = []
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

const goBack = () => {
  router.push({ name: 'no-parking-zones' })
}

const submit = async () => {
  if (!form.value.name.trim()) {
    uiStore.pushToast({
      message: '请输入禁停区名称',
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
      polygon: formatPolygonPoints(validation.points),
      status: Number(form.value.status ?? 1)
    }

    if (editing.value) {
      await editNoParkingZone({
        ...payload,
        id: Number(form.value.id || zoneId.value),
        createTime: form.value.createTime || undefined
      })
      removeEditorCache(CACHE_SCOPE, zoneId.value)
    } else {
      await addNoParkingZone(payload)
    }

    uiStore.pushToast({
      message: editing.value ? '禁停区已更新' : '禁停区已创建',
      tone: 'success'
    })

    router.push({ name: 'no-parking-zones' })
  } catch (error) {
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  loading.value = true
  await fetchZones()

  if (!hydrateEditData()) {
    uiStore.pushToast({
      message: '未获取到禁停区数据，请从列表重新进入',
      tone: 'warning'
    })
    router.replace({ name: 'no-parking-zones' })
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
          <p class="panel-description">地图编辑已拆为独立页面，边界绘制和保存操作保持同屏可见。</p>
        </div>
        <div class="button-row">
          <button type="button" class="button-secondary" @click="goBack">返回列表</button>
          <button type="button" class="button-primary" :disabled="saving || loading" @click="submit">
            {{ saving ? '保存中...' : editing ? '保存禁停区' : '创建禁停区' }}
          </button>
        </div>
      </div>

      <div class="editor-grid">
        <aside class="page-surface editor-panel">
          <div class="form-grid">
            <label class="form-field span-12">
              <span class="field-label">名称</span>
              <input v-model.trim="form.name" class="field-input" type="text" placeholder="例如：步行街禁停区" />
            </label>

            <label class="form-field span-12">
              <span class="field-label">状态</span>
              <select v-model="form.status" class="field-select">
                <option :value="1">启用</option>
                <option :value="0">停用</option>
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
            :active-zone-id="form.id"
            :readonly="false"
            :height="560"
          />
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
