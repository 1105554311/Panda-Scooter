<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import ZoneMapEditor from '@/components/ZoneMapEditor.vue'
import { getZoneDetail } from '@/api'
import { useUiStore } from '@/stores/ui'
import { formatDateTime } from '@/utils/format'
import { getEditorCache } from '@/utils/editorCache'
import { countPolygonPoints, formatPolygonPoints, getPolygonCenter } from '@/utils/polygon'

const route = useRoute()
const router = useRouter()
const uiStore = useUiStore()
const CACHE_SCOPE = 'zone'

const zoneId = computed(() => Number(route.params.id))
const loading = ref(false)
const detail = ref(null)

const centerText = computed(() => {
  const center = getPolygonCenter(detail.value?.polygon)
  if (!center) {
    return '--'
  }

  return `${center.longitude.toFixed(5)}, ${center.latitude.toFixed(5)}`
})

const fetchDetail = async () => {
  if (!Number.isFinite(zoneId.value)) {
    router.replace({ name: 'zones' })
    return
  }

  loading.value = true

  try {
    const cachedItem = getEditorCache(CACHE_SCOPE, zoneId.value)
    if (cachedItem) {
      detail.value = cachedItem
    }

    const response = await getZoneDetail({
      areaId: zoneId.value,
      id: zoneId.value
    })
    detail.value = response.data || detail.value
  } catch (error) {
    if (!detail.value) {
      uiStore.pushToast({
        message: '未获取到片区详情，请返回列表重试',
        tone: 'warning'
      })
      router.replace({ name: 'zones' })
    }
  } finally {
    loading.value = false
  }
}

onMounted(fetchDetail)
</script>

<template>
  <div class="section-grid">
    <section v-if="detail" class="page-surface detail-page">
      <div class="detail-header">
        <div>
          <h2 class="panel-title">片区详情</h2>
          <p class="panel-description">详情页已独立展示，地图和原始坐标不再挤在弹窗里。</p>
        </div>
        <div class="button-row">
          <button type="button" class="button-secondary" @click="router.push({ name: 'zones' })">返回列表</button>
          <button type="button" class="button-primary" @click="router.push({ name: 'zone-edit', params: { id: detail.id } })">
            编辑片区
          </button>
        </div>
      </div>

      <div class="detail-grid">
        <div class="detail-card">
          <span class="detail-label">片区名称</span>
          <strong>{{ detail.name }}</strong>
        </div>
        <div class="detail-card">
          <span class="detail-label">调度员</span>
          <strong>{{ detail.dispatcher?.name || '未分配' }}</strong>
          <p class="detail-subtext">{{ detail.dispatcher?.email || '暂无邮箱' }}</p>
        </div>
        <div class="detail-card">
          <span class="detail-label">顶点数</span>
          <strong>{{ countPolygonPoints(detail.polygon) }}</strong>
        </div>
        <div class="detail-card">
          <span class="detail-label">中心点</span>
          <strong>{{ centerText }}</strong>
        </div>
        <div class="detail-card">
          <span class="detail-label">车辆数</span>
          <strong>{{ detail.vehicleCount ?? '--' }}</strong>
        </div>
        <div class="detail-card">
          <span class="detail-label">创建时间</span>
          <strong>{{ formatDateTime(detail.createTime) }}</strong>
        </div>
      </div>

      <section class="page-surface map-panel">
        <ZoneMapEditor
          :model-value="detail.polygon ? formatPolygonPoints(detail.polygon) : ''"
          :readonly="true"
          :height="520"
        />
      </section>

      <div class="detail-card">
        <span class="detail-label">原始坐标</span>
        <pre class="detail-code">{{ detail.polygon || '--' }}</pre>
      </div>
    </section>

    <section v-else-if="loading" class="page-surface loading-panel">
      <div class="empty-state">加载片区详情中...</div>
    </section>
  </div>
</template>

<style scoped>
.detail-page,
.map-panel {
  padding: 24px;
}

.detail-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 24px;
}

.detail-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 20px;
}

.detail-card {
  border: 1px solid var(--line-soft);
  background: var(--bg-subtle);
  padding: 18px 20px;
}

.detail-label {
  display: block;
  color: var(--text-subtle);
  font-size: 13px;
  letter-spacing: 0.08em;
}

.detail-card strong {
  display: block;
  margin-top: 8px;
  font-size: 18px;
  font-weight: 400;
}

.detail-subtext {
  margin: 8px 0 0;
  color: var(--text-subtle);
  font-size: 13px;
}

.detail-code {
  margin: 10px 0 0;
  padding: 16px;
  border: 1px solid var(--line-soft);
  background: #ffffff;
  white-space: pre-wrap;
  word-break: break-all;
}

@media (max-width: 1080px) {
  .detail-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 720px) {
  .detail-page,
  .map-panel {
    padding: 20px;
  }

  .detail-header {
    flex-direction: column;
  }

  .detail-grid {
    grid-template-columns: 1fr;
  }
}
</style>
