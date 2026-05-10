export const ACTIVE_API_ENV = 'test'
export const DEFAULT_API_ENV = ACTIVE_API_ENV

export const API_ENVS = Object.freeze({
  mock: {
    key: 'mock',
    label: 'Mock 开发环境',
    baseURL: import.meta.env.VITE_ADMIN_MOCK_API_BASE_URL || 'https://m1.apifoxmock.com/m1/7776188-7522280-7128403'
  },
  test: {
    key: 'test',
    label: '联调测试环境',
    baseURL: import.meta.env.VITE_ADMIN_TEST_API_BASE_URL || (import.meta.env.DEV ? '' : 'http://8.137.181.89:8080')
  },
  prod: {
    key: 'prod',
    label: '生产环境',
    baseURL: import.meta.env.VITE_ADMIN_PROD_API_BASE_URL || ''
  },
  custom: {
    key: 'custom',
    label: '自定义环境',
    baseURL: ''
  }
})

export const AMAP_WEB_KEY = import.meta.env.VITE_AMAP_WEB_KEY || ''
export const AMAP_SECURITY_JS_CODE = import.meta.env.VITE_AMAP_SECURITY_JS_CODE || ''
export const AMAP_JSAPI_VERSION = import.meta.env.VITE_AMAP_JSAPI_VERSION || '2.0'
export const ECHARTS_CDN_URL = import.meta.env.VITE_ECHARTS_CDN_URL || 'https://cdn.jsdelivr.net/npm/echarts@5/dist/echarts.min.js'

const isValidApiEnv = (env) => Boolean(API_ENVS[env])

export const normalizeApiEnv = (env) => (isValidApiEnv(env) ? env : DEFAULT_API_ENV)

export const getCustomApiBaseURL = () => ''

export const getCurrentApiEnv = () => DEFAULT_API_ENV

export const getApiConfig = (env = getCurrentApiEnv()) => {
  const normalizedEnv = normalizeApiEnv(env)
  return API_ENVS[normalizedEnv]
}

export const getApiBaseURL = (env = getCurrentApiEnv()) => getApiConfig(env).baseURL

export const getApiEnvOptions = () => {
  return Object.values(API_ENVS).map((item) => {
    return {
      ...item,
      configured: Boolean(item.baseURL)
    }
  })
}

export const setApiEnv = (env) => {
  return getApiConfig(env)
}

export const setCustomApiBaseURL = (value) => {
  return String(value || '').trim()
}
