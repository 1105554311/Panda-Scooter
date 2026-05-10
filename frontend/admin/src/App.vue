<script setup>
import { onBeforeUnmount, onMounted } from 'vue'
import { RouterView, useRoute, useRouter } from 'vue-router'
import ConfirmDialog from '@/components/ConfirmDialog.vue'
import ToastStack from '@/components/ToastStack.vue'
import { useAuthStore } from '@/stores/auth'
import { useUiStore } from '@/stores/ui'
import { APP_EVENTS } from '@/utils/events'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()
const uiStore = useUiStore()

const handleToast = (event) => {
  uiStore.pushToast(event.detail)
}

const handleUnauthorized = (event) => {
  authStore.clearSession()

  const message = event?.detail?.message
  if (message) {
    uiStore.pushToast({
      message,
      tone: 'warning'
    })
  }

  if (route.name !== 'login') {
    router.replace({
      name: 'login',
      query: {
        redirect: route.fullPath
      }
    })
  }
}

onMounted(() => {
  authStore.hydrate()
  window.addEventListener(APP_EVENTS.toast, handleToast)
  window.addEventListener(APP_EVENTS.unauthorized, handleUnauthorized)
})

onBeforeUnmount(() => {
  window.removeEventListener(APP_EVENTS.toast, handleToast)
  window.removeEventListener(APP_EVENTS.unauthorized, handleUnauthorized)
})
</script>

<template>
  <RouterView />
  <ToastStack />
  <ConfirmDialog />
</template>
