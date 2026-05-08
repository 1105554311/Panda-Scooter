<script setup>
import { storeToRefs } from 'pinia'
import BaseModal from '@/components/BaseModal.vue'
import { useUiStore } from '@/stores/ui'

const uiStore = useUiStore()
const { confirm } = storeToRefs(uiStore)
</script>

<template>
  <BaseModal
    :model-value="confirm.open"
    :title="confirm.title"
    width="480px"
    @update:model-value="uiStore.resolveConfirm(false)"
  >
    <p class="confirm-message">{{ confirm.message }}</p>
    <template #footer>
      <div class="button-row">
        <button type="button" class="button-secondary" @click="uiStore.resolveConfirm(false)">
          {{ confirm.cancelText }}
        </button>
        <button
          type="button"
          :class="confirm.tone === 'danger' ? 'button-danger' : 'button-primary'"
          @click="uiStore.resolveConfirm(true)"
        >
          {{ confirm.confirmText }}
        </button>
      </div>
    </template>
  </BaseModal>
</template>

<style scoped>
.confirm-message {
  margin: 0;
  font-size: 15px;
  color: #4b5563;
}
</style>
