export const ADMIN_TOKEN_KEY = 'adminToken'
export const ADMIN_USER_KEY = 'adminUserInfo'

const AUTH_ERROR_MESSAGES = ['未登录', '登录已失效，请重新登录']

const canUseStorage = () => typeof window !== 'undefined' && typeof window.localStorage !== 'undefined'

export const getAdminToken = () => {
  if (!canUseStorage()) {
    return ''
  }

  return window.localStorage.getItem(ADMIN_TOKEN_KEY) || ''
}

export const hasAdminToken = () => Boolean(getAdminToken())

export const getAdminUserInfo = () => {
  if (!canUseStorage()) {
    return null
  }

  const rawValue = window.localStorage.getItem(ADMIN_USER_KEY)

  if (!rawValue) {
    return null
  }

  try {
    return JSON.parse(rawValue)
  } catch (error) {
    return null
  }
}

export const setAdminSession = (payload = {}) => {
  if (!canUseStorage()) {
    return
  }

  const data = payload.data || payload
  const token = data.token || payload.token || ''
  const userInfo = {
    id: data.id || '',
    username: data.username || data.name || '管理员',
    email: data.email || ''
  }

  if (token) {
    window.localStorage.setItem(ADMIN_TOKEN_KEY, token)
  }

  window.localStorage.setItem(ADMIN_USER_KEY, JSON.stringify(userInfo))
}

export const clearAdminSession = () => {
  if (!canUseStorage()) {
    return
  }

  window.localStorage.removeItem(ADMIN_TOKEN_KEY)
  window.localStorage.removeItem(ADMIN_USER_KEY)
}

export const isAuthErrorMessage = (message = '') => {
  return AUTH_ERROR_MESSAGES.includes(String(message || '').trim())
}
