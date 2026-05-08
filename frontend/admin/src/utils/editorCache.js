const memoryCache = new Map()

const getStorageKey = (scope) => `admin-editor-cache:${scope}`

const readScopeCache = (scope) => {
  const key = getStorageKey(scope)

  if (typeof window === 'undefined' || !window.sessionStorage) {
    return memoryCache.get(key) || {}
  }

  try {
    const rawValue = window.sessionStorage.getItem(key)
    return rawValue ? JSON.parse(rawValue) : {}
  } catch (error) {
    return {}
  }
}

const writeScopeCache = (scope, value) => {
  const key = getStorageKey(scope)

  if (typeof window === 'undefined' || !window.sessionStorage) {
    memoryCache.set(key, value)
    return
  }

  try {
    window.sessionStorage.setItem(key, JSON.stringify(value))
  } catch (error) {
  }
}

export const setEditorCache = (scope, item) => {
  if (!scope || !item || typeof item.id === 'undefined' || item.id === null) {
    return
  }

  const currentCache = readScopeCache(scope)
  currentCache[String(item.id)] = item
  writeScopeCache(scope, currentCache)
}

export const getEditorCache = (scope, id) => {
  if (!scope || typeof id === 'undefined' || id === null) {
    return null
  }

  const currentCache = readScopeCache(scope)
  return currentCache[String(id)] || null
}

export const removeEditorCache = (scope, id) => {
  if (!scope || typeof id === 'undefined' || id === null) {
    return
  }

  const currentCache = readScopeCache(scope)
  delete currentCache[String(id)]
  writeScopeCache(scope, currentCache)
}
