import { normalizePoint, parsePolygonPoints } from '@/utils/polygon'

const roundCoordinate = (value) => Number(Number(value).toFixed(6))

const toCanonicalPoints = (polygonOrPoints) => {
  if (Array.isArray(polygonOrPoints)) {
    return polygonOrPoints.map((point) => normalizePoint(point)).filter(Boolean)
  }

  return parsePolygonPoints(polygonOrPoints)
}

const toLatLngOrderedAmapPath = (polygon) => {
  return parsePolygonPoints(polygon).map((point) => [point.latitude, point.longitude])
}

const normalizeLatLngOrderedPolygonForEditor = (polygon) => {
  const points = parsePolygonPoints(polygon).map((point) => ({
    longitude: point.latitude,
    latitude: point.longitude
  }))

  return JSON.stringify(
    points.map((point) => [roundCoordinate(point.longitude), roundCoordinate(point.latitude)])
  )
}

const formatLatLngOrderedPolygonForApi = (polygonOrPoints) => {
  const points = toCanonicalPoints(polygonOrPoints)

  return JSON.stringify(
    points.map((point) => [roundCoordinate(point.latitude), roundCoordinate(point.longitude)])
  )
}

export const formatLatLngCenterTextFromRawPolygon = (polygon, digits = 5) => {
  const points = parsePolygonPoints(polygon)
  if (!points.length) {
    return '--'
  }

  const totals = points.reduce(
    (result, point) => ({
      latitude: result.latitude + point.longitude,
      longitude: result.longitude + point.latitude
    }),
    { latitude: 0, longitude: 0 }
  )

  const centerLatitude = totals.latitude / points.length
  const centerLongitude = totals.longitude / points.length
  return `${centerLongitude.toFixed(digits)}, ${centerLatitude.toFixed(digits)}`
}

export const formatLatLngCenterTextFromCanonicalPolygon = (polygon, digits = 5) => {
  const points = parsePolygonPoints(polygon)
  if (!points.length) {
    return '--'
  }

  const totals = points.reduce(
    (result, point) => ({
      latitude: result.latitude + point.latitude,
      longitude: result.longitude + point.longitude
    }),
    { latitude: 0, longitude: 0 }
  )

  const centerLatitude = totals.latitude / points.length
  const centerLongitude = totals.longitude / points.length
  return `${centerLongitude.toFixed(digits)}, ${centerLatitude.toFixed(digits)}`
}

export const toNoParkingAmapPath = toLatLngOrderedAmapPath
export const normalizeNoParkingPolygonForEditor = normalizeLatLngOrderedPolygonForEditor
export const formatNoParkingPolygonForApi = formatLatLngOrderedPolygonForApi

export const toZoneAmapPath = toLatLngOrderedAmapPath
export const normalizeZonePolygonForEditor = normalizeLatLngOrderedPolygonForEditor
export const formatZonePolygonForApi = formatLatLngOrderedPolygonForApi
