<script setup>
import { computed, ref } from 'vue'
import { RouterLink, RouterView, useRoute, useRouter } from 'vue-router'
import { adminLogout } from '@/api'
import { useAuthStore } from '@/stores/auth'
import { useUiStore } from '@/stores/ui'
import { formatDate } from '@/utils/format'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const uiStore = useUiStore()

const navItems = [
  { label: '数据概览', to: { name: 'dashboard' } },
  { label: '车辆管理', to: { name: 'vehicles' } },
  { label: '定价管理', to: { name: 'pricing' } },
  { label: '套餐管理', to: { name: 'packages' } },
  { label: '片区管理', to: { name: 'zones' } },
  { label: '禁停区管理', to: { name: 'no-parking-zones' } },
  { label: '停车点管理', to: { name: 'parking-points' } },
  { label: '调度员管理', to: { name: 'dispatchers' } }
]

const sidebarOpen = ref(false)

const pageTitle = computed(() => route.meta.title || '后台管理系统')
const todayLabel = computed(() => formatDate(new Date()))

const handleLogout = async () => {
  const confirmed = await uiStore.confirmAction({
    title: '退出登录',
    message: '确认退出当前管理员账号吗？',
    confirmText: '退出',
    tone: 'danger'
  })

  if (!confirmed) {
    return
  }

  try {
    await adminLogout()
  } catch (error) {
  } finally {
    authStore.clearSession()
    router.replace({ name: 'login' })
  }
}
</script>

<template>
  <div class="shell" :data-sidebar-open="sidebarOpen">
    <header class="shell-topbar">
      <div class="brand">
        <div class="brand-mark">PS</div>
        <div class="brand-text">
          <p class="brand-title">Panda Scooter</p>
          <p class="brand-subtitle">{{ pageTitle }}</p>
        </div>
      </div>

      <div class="topbar-right">
        <div class="account-card">
          <span class="account-label">当前账号</span>
          <strong class="account-name">{{ authStore.displayName }}</strong>
        </div>
        <button type="button" class="button-secondary topbar-logout" @click="handleLogout">退出</button>
        <button type="button" class="sidebar-toggle" @click="sidebarOpen = !sidebarOpen">菜单</button>
      </div>
    </header>

    <div class="shell-body">
      <aside class="shell-sidebar">
        <div class="sidebar-section">
          <div class="sidebar-date">
            <span class="sidebar-date-label">今日</span>
            <strong class="sidebar-date-value">{{ todayLabel }}</strong>
          </div>
        </div>

        <nav class="sidebar-section nav-section">
          <RouterLink
            v-for="item in navItems"
            :key="item.label"
            :to="item.to"
            class="nav-link"
            active-class="nav-link-active"
            @click="sidebarOpen = false"
          >
            <span class="nav-label">{{ item.label }}</span>
            <span class="nav-arrow">›</span>
          </RouterLink>
        </nav>
      </aside>

      <main class="shell-content">
        <RouterView />
      </main>
    </div>
  </div>
</template>

<style scoped>
.shell {
  min-height: 100vh;
  background: var(--bg-page);
}

.shell-topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 16px 24px;
  background: #ffffff;
  border-bottom: 1px solid var(--line-soft);
}

.brand {
  display: flex;
  align-items: center;
  gap: 16px;
}

.brand-mark {
  width: 48px;
  height: 48px;
  display: grid;
  place-items: center;
  background: #0b0e0d;
  color: #ffffff;
  font-size: 18px;
  letter-spacing: 0.12em;
}

.brand-text,
.brand-title,
.brand-subtitle {
  margin: 0;
}

.brand-title {
  font-size: 18px;
  color: #0b0e0d;
  letter-spacing: 0.12em;
}

.brand-subtitle {
  margin-top: 6px;
  font-size: 13px;
  color: #737373;
  letter-spacing: 0.08em;
}

.topbar-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.account-card {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  justify-content: center;
  padding: 8px 0;
}

.account-label {
  font-size: 12px;
  color: #737373;
  letter-spacing: 0.08em;
}

.account-name {
  margin-top: 4px;
  font-size: 16px;
  font-weight: 400;
  color: #0b0e0d;
}

.topbar-logout {
  min-width: 88px;
}

.sidebar-toggle {
  display: none;
  min-height: 44px;
  padding: 0 16px;
  border: 1px solid var(--line-strong);
  background: transparent;
  color: #0b0e0d;
  border-radius: 0;
  letter-spacing: 0.12em;
}

.shell-body {
  display: grid;
  grid-template-columns: 280px minmax(0, 1fr);
  gap: 24px;
  padding: 24px;
}

.shell-sidebar {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.sidebar-section {
  background: #ffffff;
  border: 1px solid var(--line-soft);
}

.sidebar-date {
  padding: 24px;
}

.sidebar-date-label {
  display: block;
  font-size: 12px;
  color: #737373;
  letter-spacing: 0.12em;
}

.sidebar-date-value {
  display: block;
  margin-top: 10px;
  font-size: 24px;
  font-weight: 400;
  color: #0b0e0d;
  letter-spacing: 0.08em;
}

.nav-section {
  display: block;
}

.nav-link {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 18px 20px;
  border-bottom: 1px solid var(--line-soft);
  color: #0b0e0d;
  background: #ffffff;
}

.nav-link:last-child {
  border-bottom: none;
}

.nav-link-active {
  background: #0b0e0d;
  color: #ffffff;
}

.nav-label {
  font-size: 15px;
  letter-spacing: 0.08em;
}

.nav-arrow {
  font-size: 20px;
  line-height: 1;
}

.shell-content {
  min-width: 0;
}

@media (max-width: 1080px) {
  .shell-topbar {
    padding: 16px;
  }

  .shell-body {
    grid-template-columns: 1fr;
    padding: 16px;
  }

  .shell-sidebar {
    position: fixed;
    top: 81px;
    left: 16px;
    right: 16px;
    z-index: 100;
    transform: translateY(-10px);
    opacity: 0;
    pointer-events: none;
    transition: opacity 0.2s ease, transform 0.2s ease;
  }

  .shell[data-sidebar-open='true'] .shell-sidebar {
    transform: translateY(0);
    opacity: 1;
    pointer-events: auto;
  }

  .sidebar-toggle {
    display: inline-flex;
    align-items: center;
    justify-content: center;
  }
}

@media (max-width: 720px) {
  .shell-topbar {
    align-items: flex-start;
    flex-direction: column;
  }

  .topbar-right {
    width: 100%;
    flex-wrap: wrap;
    justify-content: space-between;
  }

  .account-card {
    align-items: flex-start;
  }
}
</style>
