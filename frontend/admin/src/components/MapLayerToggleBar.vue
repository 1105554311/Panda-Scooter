<script setup>
import { computed } from 'vue'
import {
  ALL_MAP_LAYERS,
  MAP_LAYER_NO_PARKING,
  MAP_LAYER_PARKING_POINT,
  MAP_LAYER_SCOOTER,
  MAP_LAYER_ZONE
} from '@/utils/adminMapVisuals'

const props = defineProps({
  modelValue: {
    type: Array,
    default: () => []
  },
  selfLayer: {
    type: String,
    default: ''
  }
})

const emit = defineEmits(['update:modelValue'])

const selectedSet = computed(() => {
  return new Set(Array.isArray(props.modelValue) ? props.modelValue : [])
})

const optionList = computed(() => {
  const buildLabel = (type, defaultLabel, selfLabel) => {
    return props.selfLayer === type ? selfLabel : defaultLabel
  }

  return [
    {
      type: MAP_LAYER_ZONE,
      label: buildLabel(MAP_LAYER_ZONE, '显示片区', '显示（其他）片区')
    },
    {
      type: MAP_LAYER_NO_PARKING,
      label: buildLabel(MAP_LAYER_NO_PARKING, '显示禁停区', '显示（其他）禁停区')
    },
    {
      type: MAP_LAYER_PARKING_POINT,
      label: buildLabel(MAP_LAYER_PARKING_POINT, '显示停车点', '显示（其他）停车点')
    },
    {
      type: MAP_LAYER_SCOOTER,
      label: '显示车辆'
    }
  ]
})

const isChecked = (type) => selectedSet.value.has(type)

const toggleLayer = (type) => {
  const set = new Set(selectedSet.value)
  if (set.has(type)) {
    set.delete(type)
  } else {
    set.add(type)
  }

  const next = ALL_MAP_LAYERS.filter((item) => set.has(item))
  emit('update:modelValue', next)
}
</script>

<template>
  <div class="layer-toggle-bar">
    <button
      v-for="item in optionList"
      :key="item.type"
      type="button"
      class="toggle-button"
      :class="{ 'toggle-button-active': isChecked(item.type) }"
      @click="toggleLayer(item.type)"
    >
      {{ item.label }}
    </button>
  </div>
</template>

<style scoped>
.layer-toggle-bar {
  margin-top: 10px;
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.toggle-button {
  border: 1px solid #d4d4d1;
  background: #ffffff;
  color: #525252;
  font-size: 13px;
  letter-spacing: 0.04em;
  padding: 10px 14px;
  cursor: pointer;
}

.toggle-button-active {
  border-color: #0b0e0d;
  background: #0b0e0d;
  color: #ffffff;
}
</style>
