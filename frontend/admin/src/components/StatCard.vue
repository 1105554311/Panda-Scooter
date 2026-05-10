<script setup>
defineProps({
  label: {
    type: String,
    default: ''
  },
  value: {
    type: String,
    default: '--'
  },
  hint: {
    type: String,
    default: ''
  }
})
</script>

<template>
  <article class="card-surface stat-card">
    <span class="stat-label">{{ label }}</span>
    <strong class="stat-value">{{ value }}</strong>
    <span v-if="hint" class="stat-hint">{{ hint }}</span>
  </article>
</template>

<style scoped>
.stat-card {
  display: flex;
  flex-direction: column;
  gap: 10px;
  min-height: 120px;
  padding: 20px;
}

.stat-label {
  color: #737373;
  font-size: 13px;
  font-weight: 400;
  letter-spacing: 0.1em;
}

.stat-value {
  font-size: clamp(28px, 4vw, 34px);
  line-height: 1.1;
  font-weight: 400;
  letter-spacing: 0.04em;
  color: #0b0e0d;
}

.stat-hint {
  color: #737373;
  font-size: 12px;
}
</style>
