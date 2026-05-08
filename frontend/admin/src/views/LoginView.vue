<script setup>
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { adminLogin } from '@/api'
import { useAuthStore } from '@/stores/auth'
import { useUiStore } from '@/stores/ui'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()
const uiStore = useUiStore()

const form = ref({
  email: '',
  password: ''
})

const loading = ref(false)

const redirectTarget = computed(() => {
  const redirect = typeof route.query.redirect === 'string' ? route.query.redirect.trim() : ''
  return redirect.startsWith('/') ? redirect : '/dashboard'
})

const submit = async () => {
  if (!form.value.email.trim()) {
    uiStore.pushToast({
      message: '请输入管理员邮箱',
      tone: 'warning'
    })
    return
  }

  if (!form.value.password.trim()) {
    uiStore.pushToast({
      message: '请输入登录密码',
      tone: 'warning'
    })
    return
  }

  loading.value = true

  try {
    const response = await adminLogin({
      email: form.value.email.trim(),
      password: form.value.password
    })

    const loginData = response.data || {}

    if (!loginData.token) {
      uiStore.pushToast({
        message: '登录成功，但未获取到 token',
        tone: 'danger'
      })
      return
    }

    authStore.applySession(loginData)

    if (!authStore.isAuthenticated) {
      uiStore.pushToast({
        message: '登录状态写入失败，请重试',
        tone: 'danger'
      })
      return
    }

    uiStore.pushToast({
      message: '登录成功',
      tone: 'success'
    })

    await router.replace(redirectTarget.value)

    if (router.currentRoute.value.name === 'login' && typeof window !== 'undefined') {
      window.location.replace(redirectTarget.value)
    }
  } catch (error) {
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-page">
    <div class="login-wrap">
      <section class="hero">
        <div class="logo-mark">PS</div>
        <h1 class="title">后台管理系统</h1>
        <p class="subtitle">登录后进入管理端</p>
      </section>

      <section class="card">
        <div class="login-card-header">
          <p class="login-eyebrow">管理员登录</p>
        </div>

        <form class="login-form" @submit.prevent="submit">
          <label class="form-field">
            <span class="field-label">邮箱</span>
            <input
              v-model.trim="form.email"
              class="field-input"
              type="email"
              placeholder="admin@example.com"
              autocomplete="username"
            />
          </label>

          <label class="form-field">
            <span class="field-label">密码</span>
            <input
              v-model="form.password"
              class="field-input"
              type="password"
              placeholder="请输入密码"
              autocomplete="current-password"
            />
          </label>

          <button type="submit" class="button-primary login-submit" :disabled="loading">
            {{ loading ? '登录中...' : '登录' }}
          </button>
        </form>
      </section>
    </div>
  </div>
</template>

<style scoped>
.login-page {
  min-height: 100vh;
  background: linear-gradient(180deg, #fafaf8 0%, #f0efe8 100%);
  padding: 48px 16px 64px;
}

.login-wrap {
  width: min(100%, 520px);
  margin: 0 auto;
}

.hero {
  padding: 32px 16px 48px;
  text-align: center;
}

.logo-mark {
  width: 72px;
  height: 72px;
  display: grid;
  place-items: center;
  margin: 0 auto 20px;
  background: #0b0e0d;
  color: #ffffff;
  font-size: 22px;
  letter-spacing: 0.14em;
}

.title {
  margin: 0;
  font-size: 32px;
  color: #0b0e0d;
  font-weight: 400;
  letter-spacing: 0.16em;
}

.subtitle {
  margin: 14px 0 0;
  font-size: 14px;
  color: #737373;
}

.card {
  background: #ffffff;
  border: 1px solid #e5e5e2;
  padding: 40px 32px;
}

.login-card-header {
  margin-bottom: 28px;
  padding-bottom: 20px;
  border-bottom: 1px solid #e5e5e2;
}

.login-eyebrow {
  margin: 0;
  color: #737373;
  font-size: 12px;
  letter-spacing: 0.14em;
  text-transform: uppercase;
}

.login-form {
  display: grid;
  gap: 18px;
}

.login-submit {
  width: 100%;
  min-height: 48px;
  margin-top: 6px;
}

@media (max-width: 720px) {
  .login-page {
    padding: 40px 16px 56px;
  }

  .hero {
    padding-bottom: 36px;
  }

  .title {
    font-size: 28px;
  }

  .card {
    padding: 24px 20px;
  }
}
</style>
