import {
  getDispatcherList,
  getNoParkingZoneList,
  getParkingPointList,
  getScooterList,
  getZoneList
} from '@/api'

const CACHE_TTL_MS = 60 * 1000

let cachedLayers = null
let cachedAt = 0
let inflightPromise = null

const toNumberOrNull = (value) => {
  const numeric = Number(value)
  return Number.isFinite(numeric) ? numeric : null
}

const normalizeScooter = (item = {}) => {
  return {
    id: item.id,
    code: item.code || '',
    areaId: toNumberOrNull(item.areaId ?? item.area_id),
    faultStatus: Number(item.faultStatus ?? item.fault_status ?? 0),
    rideStatus: Number(item.rideStatus ?? item.ride_status ?? 0),
    battery: toNumberOrNull(item.battery),
    longitude: toNumberOrNull(item.longitude),
    latitude: toNumberOrNull(item.latitude),
    createTime: item.createTime || item.create_time || ''
  }
}

const normalizeParkingPoint = (item = {}) => {
  const longitude = toNumberOrNull(item.longtitude ?? item.longitude)
  const latitude = toNumberOrNull(item.latitude)

  return {
    id: item.id,
    name: item.name || `停车点 #${item.id}`,
    status: Number(item.status ?? 1),
    longitude,
    latitude
  }
}

const normalizeZone = (item = {}, dispatchersByArea = {}) => {
  const dispatcher = dispatchersByArea[Number(item.id)] || null
  const name = item.name || `片区 #${item.id}`

  return {
    id: item.id,
    name,
    polygon: item.polygon || '',
    dispatcherName: dispatcher?.name || '',
    dispatcherEmail: dispatcher?.email || '',
    label: dispatcher?.name ? `${name} / 调度员：${dispatcher.name}` : name
  }
}

const normalizeNoParkingZone = (item = {}) => {
  const name = item.name || `禁停区 #${item.id}`

  return {
    id: item.id,
    name,
    polygon: item.polygon || '',
    label: name
  }
}

const mergeAndDedupeScooters = (list = []) => {
  const map = list.reduce((result, item) => {
    if (!item?.id) {
      return result
    }

    result.set(String(item.id), item)
    return result
  }, new Map())

  return Array.from(map.values())
}

export const fetchAdminMapLayers = async ({ force = false } = {}) => {
  const now = Date.now()
  if (!force && cachedLayers && now - cachedAt < CACHE_TTL_MS) {
    return cachedLayers
  }

  if (inflightPromise) {
    return inflightPromise
  }

  inflightPromise = (async () => {
    let zones = []
    let noParkingZones = []
    let scooters = []
    let parkingPoints = []
    let dispatchers = []

    const tasks = [
      getZoneList({ page: 1, pageSize: 1000 }).then((res) => {
        zones = Array.isArray(res.data?.areaList) ? res.data.areaList : []
      }),
      getNoParkingZoneList({ page: 1, pageSize: 1000 }).then((res) => {
        noParkingZones = Array.isArray(res.data?.areaList) ? res.data.areaList : []
      }),
      getScooterList({}).then((res) => {
        scooters = Array.isArray(res.data?.areaList) ? res.data.areaList : []
      }),
      getParkingPointList({ page: 1, pageSize: 1000 }).then((res) => {
        parkingPoints = Array.isArray(res.data?.areaList) ? res.data.areaList : []
      }),
      getDispatcherList({ page: 1, pageSize: 1000 }).then((res) => {
        dispatchers = Array.isArray(res.data?.dispatcherList) ? res.data.dispatcherList : []
      })
    ]

    await Promise.allSettled(tasks)

    if (!scooters.length && zones.length) {
      const fallbackResults = await Promise.allSettled(
        zones.map((item) =>
          getScooterList({
            areaId: Number(item.id)
          }).then((res) => (Array.isArray(res.data?.areaList) ? res.data.areaList : []))
        )
      )

      scooters = fallbackResults
        .filter((item) => item.status === 'fulfilled')
        .flatMap((item) => item.value)
    }

    const dispatchersByArea = dispatchers.reduce((result, item) => {
      const areaId = Number(item.areaId)
      if (Number.isFinite(areaId) && areaId > 0) {
        result[areaId] = item
      }
      return result
    }, {})

    const normalizedLayers = {
      zones: zones.map((item) => normalizeZone(item, dispatchersByArea)),
      noParkingZones: noParkingZones.map((item) => normalizeNoParkingZone(item)),
      scooters: mergeAndDedupeScooters(scooters.map((item) => normalizeScooter(item))),
      parkingPoints: parkingPoints.map((item) => normalizeParkingPoint(item))
    }

    cachedLayers = normalizedLayers
    cachedAt = Date.now()
    return normalizedLayers
  })()
    .catch(() => {
      cachedLayers = {
        zones: [],
        noParkingZones: [],
        scooters: [],
        parkingPoints: []
      }
      cachedAt = Date.now()
      return cachedLayers
    })
    .finally(() => {
      inflightPromise = null
    })

  return inflightPromise
}
