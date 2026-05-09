<script setup>
import { computed, onMounted, ref } from 'vue'
import { getPricingRules, updatePricingRules } from '@/api'
import { useUiStore } from '@/stores/ui'
import { formatCurrency } from '@/utils/format'

const uiStore = useUiStore()

const form = ref({
  pricePerMin: '',
  basePrice: '',
  billingInterval: ''
})
const currentRules = ref({
  pricePerMin: '',
  basePrice: '',
  billingInterval: ''
})
const loading = ref(false)
const saving = ref(false)

const ridePreviewPrice = computed(() => {
  const basePrice = Number(form.value.basePrice || 0)
  const pricePerMin = Number(form.value.pricePerMin || 0)
  const billingInterval = Number(form.value.billingInterval || 0)
  const duration = 18

  if (!Number.isFinite(billingInterval) || billingInterval <= 0) {
    return formatCurrency(basePrice)
  }

  return formatCurrency(basePrice + (duration / billingInterval) * pricePerMin)
})

const fetchRules = async () => {
  loading.value = true

  try {
    const response = await getPricingRules()
    const data = response.data || {}
    form.value = {
      pricePerMin: data.pricePerMin ?? '',
      basePrice: data.basePrice ?? '',
      billingInterval: data.billingInterval ?? ''
    }
    currentRules.value = {
      pricePerMin: data.pricePerMin ?? '',
      basePrice: data.basePrice ?? '',
      billingInterval: data.billingInterval ?? ''
    }
  } finally {
    loading.value = false
  }
}

const submit = async () => {
  const pricePerMin = Number(form.value.pricePerMin)
  const basePrice = Number(form.value.basePrice)
  const billingInterval = Number(form.value.billingInterval)

  if (!Number.isFinite(pricePerMin) || pricePerMin <= 0) {
    uiStore.pushToast({
      message: '请输入有效的计费单价',
      tone: 'warning'
    })
    return
  }

  if (!Number.isFinite(basePrice) || basePrice < 0) {
    uiStore.pushToast({
      message: '请输入有效的起步价',
      tone: 'warning'
    })
    return
  }

  if (!Number.isFinite(billingInterval) || billingInterval <= 0) {
    uiStore.pushToast({
      message: '请输入有效的计费间隔',
      tone: 'warning'
    })
    return
  }

  saving.value = true

  try {
    await updatePricingRules({
      pricePerMin,
      basePrice,
      billingInterval
    })

    currentRules.value = {
      pricePerMin,
      basePrice,
      billingInterval
    }

    uiStore.pushToast({
      message: '定价策略已保存',
      tone: 'success'
    })
  } finally {
    saving.value = false
  }
}

onMounted(fetchRules)
</script>

<template>
  <div class="pricing-grid">
    <section class="page-surface pricing-panel">
      <div class="panel-header">
        <div>
          <h2 class="panel-title">定价策略</h2>
        </div>
      </div>

      <section class="current-pricing-card">
        <h3>当前定价策略</h3>
        <div class="current-pricing-grid">
          <div class="current-pricing-item">
            <span>计费单价</span>
            <strong>{{ formatCurrency(currentRules.pricePerMin || 0) }}</strong>
          </div>
          <div class="current-pricing-item">
            <span>起步价</span>
            <strong>{{ formatCurrency(currentRules.basePrice || 0) }}</strong>
          </div>
          <div class="current-pricing-item">
            <span>计费间隔</span>
            <strong>{{ currentRules.billingInterval || '--' }} 分钟</strong>
          </div>
        </div>
      </section>

      <div class="form-grid">
        <label class="form-field span-4">
          <span class="field-label">计费单价</span>
          <input
            v-model="form.pricePerMin"
            class="field-input"
            type="number"
            min="0"
            step="0.01"
            placeholder="0.20"
          />
          <span class="field-help">单位：元 / 计费间隔</span>
        </label>

        <label class="form-field span-4">
          <span class="field-label">起步价</span>
          <input
            v-model="form.basePrice"
            class="field-input"
            type="number"
            min="0"
            step="0.01"
            placeholder="1.00"
          />
          <span class="field-help">首次计费金额</span>
        </label>

        <label class="form-field span-4">
          <span class="field-label">计费间隔</span>
          <input
            v-model="form.billingInterval"
            class="field-input"
            type="number"
            min="1"
            step="1"
            placeholder="30"
          />
          <span class="field-help">单位：分钟</span>
        </label>
      </div>

      <div class="button-row pricing-actions">
        <button type="button" class="button-primary" :disabled="saving || loading" @click="submit">
          {{ saving ? '保存中...' : '保存策略' }}
        </button>
      </div>
    </section>

    <section class="page-surface pricing-panel">
      <div class="panel-header">
        <div>
          <h2 class="panel-title">计费预览</h2>
        </div>
      </div>

      <div class="preview-stack">
        <div class="preview-card">
          <span>起步价</span>
          <strong>{{ formatCurrency(form.basePrice || 0) }}</strong>
        </div>
        <div class="preview-card">
          <span>计费单价</span>
          <strong>{{ formatCurrency(form.pricePerMin || 0) }}</strong>
        </div>
        <div class="preview-card preview-card-strong">
          <span>18 分钟预估费用</span>
          <strong>{{ ridePreviewPrice }}</strong>
        </div>
      </div>
    </section>
  </div>
</template>

<style scoped>
.pricing-grid {
  display: grid;
  grid-template-columns: 1.15fr 0.85fr;
  gap: 20px;
}

.pricing-panel {
  padding: 28px;
}

.current-pricing-card {
  margin-bottom: 20px;
  border: 1px solid #e5e5e2;
  background: #fafaf8;
  padding: 16px;
}

.current-pricing-card h3 {
  margin: 0 0 12px;
  font-size: 16px;
  font-weight: 500;
}

.current-pricing-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.current-pricing-item {
  border: 1px solid #ecece8;
  background: #ffffff;
  padding: 12px;
}

.current-pricing-item span {
  display: block;
  color: #737373;
  font-size: 12px;
  letter-spacing: 0.08em;
}

.current-pricing-item strong {
  display: block;
  margin-top: 8px;
  font-size: 20px;
  font-weight: 400;
}

.pricing-actions {
  margin-top: 20px;
}

.preview-stack {
  display: grid;
  gap: 12px;
}

.preview-card {
  padding: 20px;
  border: 1px solid #e5e5e2;
  background: #fafaf8;
}

.preview-card span {
  display: block;
  color: #737373;
  font-size: 13px;
  letter-spacing: 0.08em;
}

.preview-card strong {
  display: block;
  margin-top: 8px;
  font-size: 28px;
  font-weight: 400;
}

.preview-card-strong {
  background: #ffffff;
  border-color: #d4d4d1;
}

@media (max-width: 1080px) {
  .pricing-grid {
    grid-template-columns: 1fr;
  }

  .current-pricing-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 720px) {
  .pricing-panel {
    padding: 20px;
  }
}
</style>
