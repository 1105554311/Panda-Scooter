<script setup>
import { storeToRefs } from 'pinia'
import { useUiStore } from '@/stores/ui'

const uiStore = useUiStore()
const { toasts } = storeToRefs(uiStore)

const toneClassMap = {
  info: 'toast-info',
  success: 'toast-success',
  warning: 'toast-warning',
  danger: 'toast-danger'
}
</script>

<template>
  <Teleport to="body">
    <div class="toast-stack">
      <TransitionGroup name="fade-slide">
        <div
          v-for="toast in toasts"
          :key="toast.id"
          class="toast-item"
          :class="toneClassMap[toast.tone] || 'toast-info'"
        >
          <span>{{ toast.message }}</span>
          <button type="button" class="toast-close" @click="uiStore.dismissToast(toast.id)">x</button>
        </div>
      </TransitionGroup>
    </div>
  </Teleport>
</template>

<style scoped>
.toast-stack {
  position: fixed;
  top: 16px;
  right: 16px;
  z-index: 1100;
  display: grid;
  gap: 10px;
  max-width: min(360px, calc(100vw - 32px));
}

.toast-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 12px 14px;
  border: 1px solid #e5e5e2;
  background: #ffffff;
}

.toast-info {
  color: #0b0e0d;
}

.toast-success {
  color: #1f7a53;
}

.toast-warning {
  color: #b7791f;
}

.toast-danger {
  color: #b42318;
}

.toast-close {
  border: none;
  background: transparent;
  color: currentColor;
  font-size: 12px;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

@media (max-width: 720px) {
  .toast-stack {
    left: 16px;
    right: 16px;
    max-width: none;
  }
}
</style>
