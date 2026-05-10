const normalizePositiveId = (value) => {
  const num = Number(value)
  return Number.isFinite(num) && num > 0 ? num : null
}

export const normalizeDispatcher = (item = {}) => {
  const id = normalizePositiveId(item.id)
  if (!id) {
    return null
  }

  return {
    id,
    name: typeof item.name === 'string' ? item.name.trim() : '',
    email: typeof item.email === 'string' ? item.email.trim() : '',
    areaId: normalizePositiveId(item.areaId)
  }
}

export const normalizeDispatcherList = (list) => {
  if (!Array.isArray(list)) {
    return []
  }

  return list
    .map((item) => normalizeDispatcher(item))
    .filter(Boolean)
}

export const getZoneDispatchers = (zone = {}, options = {}) => {
  const fromList = normalizeDispatcherList(zone.dispatchers)
  if (fromList.length) {
    return fromList
  }

  const fromSingle = normalizeDispatcher(zone.dispatcher)
  if (fromSingle) {
    return [fromSingle]
  }

  const fallbackByArea = options.fallbackByArea || null
  const zoneId = normalizePositiveId(zone.id)

  if (!zoneId || !fallbackByArea || !fallbackByArea[zoneId]) {
    return []
  }

  const fallbackRaw = Array.isArray(fallbackByArea[zoneId])
    ? fallbackByArea[zoneId]
    : [fallbackByArea[zoneId]]
  return normalizeDispatcherList(fallbackRaw)
}

export const withZoneDispatchers = (zone = {}, options = {}) => {
  return {
    ...zone,
    dispatchers: getZoneDispatchers(zone, options)
  }
}

export const countZoneDispatchers = (zone = {}, options = {}) => {
  return getZoneDispatchers(zone, options).length
}

export const hasDispatcherInZone = (zone = {}, dispatcherId, options = {}) => {
  const targetId = normalizePositiveId(dispatcherId)
  if (!targetId) {
    return false
  }

  return getZoneDispatchers(zone, options).some((item) => item.id === targetId)
}

const getDisplayName = (dispatcher, index) => {
  const name = typeof dispatcher?.name === 'string' ? dispatcher.name.trim() : ''
  return name || `调度员${index + 1}`
}

export const formatZoneDispatcherDisplay = (zone = {}, options = {}) => {
  const dispatchers = getZoneDispatchers(zone, options)
  const count = dispatchers.length

  if (!count) {
    return '未分配'
  }

  const names = dispatchers.map((item, index) => getDisplayName(item, index))
  if (count <= 2) {
    return names.join('、')
  }

  return `${names[0]}${names[1]}等${count}人`
}
