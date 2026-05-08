import { defineStore } from 'pinia'

let toastSeed = 0

const DEFAULT_CONFIRM = {
  open: false,
  title: '',
  message: '',
  confirmText: '确认',
  cancelText: '取消',
  tone: 'primary',
  resolver: null
}

export const useUiStore = defineStore('ui', {
  state: () => ({
    toasts: [],
    confirm: {
      ...DEFAULT_CONFIRM
    }
  }),
  actions: {
    pushToast(payload = {}) {
      const toast = {
        id: ++toastSeed,
        message: payload.message || '操作已完成',
        tone: payload.tone || 'info',
        duration: payload.duration ?? 2400
      }

      this.toasts.push(toast)

      if (toast.duration > 0 && typeof window !== 'undefined') {
        window.setTimeout(() => {
          this.dismissToast(toast.id)
        }, toast.duration)
      }
    },
    dismissToast(id) {
      this.toasts = this.toasts.filter((toast) => toast.id !== id)
    },
    confirmAction(options = {}) {
      return new Promise((resolve) => {
        this.confirm = {
          open: true,
          title: options.title || '请确认操作',
          message: options.message || '',
          confirmText: options.confirmText || '确认',
          cancelText: options.cancelText || '取消',
          tone: options.tone || 'primary',
          resolver: resolve
        }
      })
    },
    resolveConfirm(result) {
      const resolver = this.confirm.resolver
      this.confirm = {
        ...DEFAULT_CONFIRM
      }
      if (typeof resolver === 'function') {
        resolver(result)
      }
    }
  }
})
