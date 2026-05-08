import { getApiBaseURL, getApiConfig } from './env'
import { clearAdminSession, getAdminToken, isAuthErrorMessage } from '@/utils/auth'
import { emitToast, emitUnauthorized } from '@/utils/events'

const isAbsoluteUrl = (url) => /^https?:\/\//.test(url)
const isSuccessCode = (code) => ['0', '200'].includes(String(code))

const mergeBaseAndPath = (baseURL, url) => {
  const normalizedBaseURL = String(baseURL || '').replace(/\/+$/, '')
  const normalizedPath = `${url.startsWith('/') ? '' : '/'}${url}`

  const parsedBaseURL = new URL(normalizedBaseURL)
  const baseSegments = parsedBaseURL.pathname.split('/').filter(Boolean)
  const pathSegments = normalizedPath.split('/').filter(Boolean)

  let overlap = 0

  for (let size = Math.min(baseSegments.length, pathSegments.length); size > 0; size -= 1) {
    const baseTail = baseSegments.slice(-size).join('/')
    const pathHead = pathSegments.slice(0, size).join('/')

    if (baseTail === pathHead) {
      overlap = size
      break
    }
  }

  const mergedPath = [...baseSegments, ...pathSegments.slice(overlap)].join('/')
  parsedBaseURL.pathname = mergedPath ? `/${mergedPath}` : '/'
  parsedBaseURL.search = ''
  parsedBaseURL.hash = ''

  return parsedBaseURL.toString().replace(/\/$/, '')
}

const serializeParams = (params = {}) => {
  const searchParams = new URLSearchParams()

  Object.entries(params).forEach(([key, value]) => {
    if (value === undefined || value === null || value === '') {
      return
    }

    searchParams.set(key, String(value))
  })

  return searchParams.toString()
}

const buildUrl = (url, params, baseURL) => {
  const normalizedUrl = isAbsoluteUrl(url)
    ? url
    : baseURL
      ? mergeBaseAndPath(baseURL, url)
      : url

  const queryString = serializeParams(params)

  if (!queryString) {
    return normalizedUrl
  }

  return `${normalizedUrl}${normalizedUrl.includes('?') ? '&' : '?'}${queryString}`
}

const resolveErrorMessage = (payload, fallback) => {
  if (payload && typeof payload.msg === 'string' && payload.msg.trim()) {
    return payload.msg.trim()
  }

  if (typeof payload === 'string' && payload.trim()) {
    return payload.trim()
  }

  if (typeof fallback === 'string' && fallback.trim()) {
    return fallback.trim()
  }

  return '请求失败'
}

const isUnauthorizedResponse = (message, statusCode) => {
  return statusCode === 401 || statusCode === 403 || isAuthErrorMessage(message)
}

const rejectWithMessage = (message, extra = {}) => {
  const unauthorized = isUnauthorizedResponse(message, extra.statusCode)

  if (unauthorized) {
    clearAdminSession()
    emitUnauthorized({ message })
  } else {
    emitToast({
      message,
      tone: extra.tone || 'danger'
    })
  }

  const error = new Error(message)
  Object.assign(error, { handled: true, unauthorized }, extra)
  throw error
}

const parseResponseBody = async (response) => {
  const contentType = response.headers.get('content-type') || ''

  if (contentType.includes('application/json')) {
    return response.json()
  }

  return response.text()
}

const request = async (options = {}) => {
  const {
    url,
    params,
    baseURL: customBaseURL,
    env,
    method = 'GET',
    headers = {},
    data,
    skipAuth = false,
    ...rest
  } = options

  const resolvedBaseURL = customBaseURL || getApiBaseURL(env)

  if (!isAbsoluteUrl(url) && !resolvedBaseURL && !String(url || '').startsWith('/')) {
    const apiConfig = getApiConfig(env)
    return rejectWithMessage(`${apiConfig.label} API 地址未配置`)
  }

  const token = skipAuth ? '' : getAdminToken()
  const requestHeaders = {
    ...(data !== undefined ? { 'Content-Type': 'application/json' } : {}),
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
    ...headers
  }

  const response = await fetch(buildUrl(url, params, resolvedBaseURL), {
    method,
    headers: requestHeaders,
    body: data !== undefined ? JSON.stringify(data) : undefined,
    ...rest
  }).catch((error) => {
    return rejectWithMessage(error?.message || '网络异常', { cause: error })
  })

  const payload = await parseResponseBody(response)

  if (!response.ok) {
    return rejectWithMessage(resolveErrorMessage(payload, `请求失败: ${response.status}`), {
      statusCode: response.status,
      data: payload
    })
  }

  if (payload && typeof payload === 'object' && typeof payload.code !== 'undefined' && !isSuccessCode(payload.code)) {
    return rejectWithMessage(resolveErrorMessage(payload), {
      statusCode: response.status,
      data: payload,
      code: payload.code
    })
  }

  return payload
}

export default request
