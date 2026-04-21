const AUTH_ERROR_MESSAGES = [
  '未登录',
  '登录已失效，请重新登录'
]

export const clearDispatcherSession = () => {
  uni.removeStorageSync('dispatcherToken')
  uni.removeStorageSync('dispatcherCurrentTask')
  uni.removeStorageSync('dispatcherUserInfo')
}

export const isAuthErrorMessage = (message = '') => {
  const normalizedMessage = String(message || '').trim()
  return AUTH_ERROR_MESSAGES.includes(normalizedMessage)
}

export const isUnauthorizedError = (error) => {
  return Boolean(error && error.unauthorized)
}
