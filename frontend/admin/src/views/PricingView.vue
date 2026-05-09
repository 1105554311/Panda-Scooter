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

const calculatePreviewPrice = (rules) => {
  const basePrice = Number(rules.basePrice || 0)
  const pricePerMin = Number(rules.pricePerMin || 0)
  const billingInterval = Number(rules.billingInterval || 0)

  if (!Number.isFinite(basePrice) || basePrice < 0) {
    return formatCurrency(0)
  }

  if (!Number.isFinite(billingInterval) || billingInterval <= 0) {
    return formatCurrency(basePrice)
  }

  return formatCurrency(basePrice + (18 / billingInterval) * pricePerMin)
}

const ridePreviewPrice = computed(() => calculatePreviewPrice(form.value))
const currentRidePreviewPrice = computed(() => calculatePreviewPrice(currentRules.value))

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
  <div class="section-grid pricing-layout">
    <section class="page-surface pricing-panel">
      <div class="panel-header">
        <h2 class="panel-title">当前定价策略</h2>
      </div>

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
        <div class="current-pricing-item">
          <span>18 分钟预估费用</span>
          <strong>{{ currentRidePreviewPrice }}</strong>
        </div>
      </div>
    </section>

    <section class="page-surface pricing-panel">
      <div class="panel-header">
        <h2 class="panel-title">修改定价策略</h2>
      </div>

      <div class="form-grid">
        <label class="form-field span-3">
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

        <label class="form-field span-3">
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

        <label class="form-field span-3">
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

        <label class="form-field span-3">
          <span class="field-label">18 分钟预估费用</span>
          <input class="field-input" :value="ridePreviewPrice" readonly />
          <span class="field-help">自动计算，仅供预览</span>
        </label>
      </div>

      <div class="button-row pricing-actions">
        <button type="button" class="button-primary" :disabled="saving || loading" @click="submit">
          {{ saving ? '保存中...' : '保存策略' }}
        </button>
      </div>
    </section>
  </div>
</template>

<style scoped>
.pricing-layout {
  gap: 20px;
}

.pricing-panel {
  padding: 28px;
}

.current-pricing-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.current-pricing-item {
  border: 1px solid #ecece8;
  background: #fafaf8;
  padding: 14px;
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

@media (max-width: 900px) {
  .current-pricing-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 720px) {
  .pricing-panel {
    padding: 20px;
  }

  .current-pricing-grid {
    grid-template-columns: 1fr;
  }
}
</style>
