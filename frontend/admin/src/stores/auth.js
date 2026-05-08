import { defineStore } from 'pinia'
import {
  clearAdminSession,
  getAdminToken,
  getAdminUserInfo,
  setAdminSession
} from '@/utils/auth'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: '',
    user: null
  }),
  getters: {
    isAuthenticated: (state) => Boolean(state.token),
    displayName: (state) => state.user?.username || state.user?.email || '管理员'
  },
  actions: {
    hydrate() {
      this.token = getAdminToken()
      this.user = getAdminUserInfo()
    },
    applySession(payload) {
      setAdminSession(payload)
      this.hydrate()
    },
    clearSession() {
      clearAdminSession()
      this.token = ''
      this.user = null
    }
  }
})
