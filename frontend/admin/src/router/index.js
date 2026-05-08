import { createRouter, createWebHistory } from 'vue-router'
import AppShell from '@/components/AppShell.vue'
import DashboardView from '@/views/DashboardView.vue'
import DispatchersView from '@/views/DispatchersView.vue'
import LoginView from '@/views/LoginView.vue'
import NoParkingZoneEditorView from '@/views/NoParkingZoneEditorView.vue'
import NoParkingZonesView from '@/views/NoParkingZonesView.vue'
import PackagesView from '@/views/PackagesView.vue'
import ParkingPointEditorView from '@/views/ParkingPointEditorView.vue'
import ParkingPointsView from '@/views/ParkingPointsView.vue'
import PricingView from '@/views/PricingView.vue'
import VehicleManagementView from '@/views/VehicleManagementView.vue'
import ZoneDetailView from '@/views/ZoneDetailView.vue'
import ZoneEditorView from '@/views/ZoneEditorView.vue'
import ZonesView from '@/views/ZonesView.vue'
import { hasAdminToken } from '@/utils/auth'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: LoginView,
      meta: {
        guestOnly: true,
        title: '管理员登录'
      }
    },
    {
      path: '/',
      component: AppShell,
      meta: {
        requiresAuth: true
      },
      children: [
        {
          path: '',
          redirect: {
            name: 'dashboard'
          }
        },
        {
          path: 'dashboard',
          name: 'dashboard',
          component: DashboardView,
          meta: {
            title: '数据概览'
          }
        },
        {
          path: 'vehicles',
          name: 'vehicles',
          component: VehicleManagementView,
          meta: {
            title: '车辆管理'
          }
        },
        {
          path: 'pricing',
          name: 'pricing',
          component: PricingView,
          meta: {
            title: '定价管理'
          }
        },
        {
          path: 'packages',
          name: 'packages',
          component: PackagesView,
          meta: {
            title: '套餐管理'
          }
        },
        {
          path: 'zones',
          name: 'zones',
          component: ZonesView,
          meta: {
            title: '片区管理'
          }
        },
        {
          path: 'zones/new',
          name: 'zone-create',
          component: ZoneEditorView,
          meta: {
            title: '新增片区'
          }
        },
        {
          path: 'zones/:id',
          name: 'zone-detail',
          component: ZoneDetailView,
          meta: {
            title: '片区详情'
          }
        },
        {
          path: 'zones/:id/edit',
          name: 'zone-edit',
          component: ZoneEditorView,
          meta: {
            title: '编辑片区'
          }
        },
        {
          path: 'no-parking-zones',
          name: 'no-parking-zones',
          component: NoParkingZonesView,
          meta: {
            title: '禁停区管理'
          }
        },
        {
          path: 'no-parking-zones/new',
          name: 'no-parking-zone-create',
          component: NoParkingZoneEditorView,
          meta: {
            title: '新增禁停区'
          }
        },
        {
          path: 'no-parking-zones/:id/edit',
          name: 'no-parking-zone-edit',
          component: NoParkingZoneEditorView,
          meta: {
            title: '编辑禁停区'
          }
        },
        {
          path: 'parking-points',
          name: 'parking-points',
          component: ParkingPointsView,
          meta: {
            title: '停车点管理'
          }
        },
        {
          path: 'parking-points/new',
          name: 'parking-point-create',
          component: ParkingPointEditorView,
          meta: {
            title: '新增停车点'
          }
        },
        {
          path: 'parking-points/:id/edit',
          name: 'parking-point-edit',
          component: ParkingPointEditorView,
          meta: {
            title: '编辑停车点'
          }
        },
        {
          path: 'dispatchers',
          name: 'dispatchers',
          component: DispatchersView,
          meta: {
            title: '调度员管理'
          }
        }
      ]
    }
  ]
})

router.beforeEach((to) => {
  const authenticated = hasAdminToken()

  if (to.meta.requiresAuth && !authenticated) {
    return {
      name: 'login',
      query: {
        redirect: to.fullPath
      }
    }
  }

  if (to.meta.guestOnly && authenticated) {
    const redirect = typeof to.query.redirect === 'string' ? to.query.redirect : '/dashboard'
    return redirect
  }

  return true
})

export default router
