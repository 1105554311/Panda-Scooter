const EPSILON = 1e-10

const roundCoordinate = (value) => {
  return Number(Number(value).toFixed(6))
}

const stripClosingPoint = (points = []) => {
  if (points.length < 2) {
    return points
  }

  const firstPoint = points[0]
  const lastPoint = points[points.length - 1]

  if (
    Math.abs(firstPoint.longitude - lastPoint.longitude) < EPSILON &&
    Math.abs(firstPoint.latitude - lastPoint.latitude) < EPSILON
  ) {
    return points.slice(0, -1)
  }

  return points
}

export const normalizePoint = (point) => {
  if (!point) {
    return null
  }

  if (typeof point.getLng === 'function' && typeof point.getLat === 'function') {
    const longitude = Number(point.getLng())
    const latitude = Number(point.getLat())

    if (Number.isFinite(longitude) && Number.isFinite(latitude)) {
      return { longitude, latitude }
    }
  }

  if (Array.isArray(point) && point.length >= 2) {
    const longitude = Number(point[0])
    const latitude = Number(point[1])

    if (Number.isFinite(longitude) && Number.isFinite(latitude)) {
      return { longitude, latitude }
    }
  }

  const longitude = Number(point.longitude ?? point.lng ?? point.x)
  const latitude = Number(point.latitude ?? point.lat ?? point.y)

  if (!Number.isFinite(longitude) || !Number.isFinite(latitude)) {
    return null
  }

  return {
    longitude,
    latitude
  }
}

const parseDelimitedPolygon = (rawValue) => {
  return rawValue
    .split(/[;\n]/)
    .map((segment) => segment.trim())
    .filter(Boolean)
    .map((segment) => {
      const values = segment
        .replace(/^\[|\]$/g, '')
        .split(',')
        .map((item) => Number(item.trim()))

      return normalizePoint(values)
    })
    .filter(Boolean)
}

export const parsePolygonPoints = (polygon) => {
  if (!polygon) {
    return []
  }

  if (Array.isArray(polygon)) {
    return stripClosingPoint(polygon.map(normalizePoint).filter(Boolean))
  }

  if (typeof polygon === 'object') {
    return stripClosingPoint([normalizePoint(polygon)].filter(Boolean))
  }

  const rawValue = String(polygon).trim()
  if (!rawValue) {
    return []
  }

  try {
    const parsed = JSON.parse(rawValue)
    return Array.isArray(parsed)
      ? stripClosingPoint(parsed.map(normalizePoint).filter(Boolean))
      : []
  } catch (error) {
    return stripClosingPoint(parseDelimitedPolygon(rawValue))
  }
}

export const formatPolygonPoints = (polygon) => {
  return JSON.stringify(
    parsePolygonPoints(polygon).map((point) => [roundCoordinate(point.longitude), roundCoordinate(point.latitude)])
  )
}

export const countPolygonPoints = (polygon) => parsePolygonPoints(polygon).length

export const getPolygonBounds = (points = []) => {
  if (!points.length) {
    return null
  }

  return points.reduce(
    (result, point) => ({
      minLongitude: Math.min(result.minLongitude, point.longitude),
      maxLongitude: Math.max(result.maxLongitude, point.longitude),
      minLatitude: Math.min(result.minLatitude, point.latitude),
      maxLatitude: Math.max(result.maxLatitude, point.latitude)
    }),
    {
      minLongitude: points[0].longitude,
      maxLongitude: points[0].longitude,
      minLatitude: points[0].latitude,
      maxLatitude: points[0].latitude
    }
  )
}

export const getPolygonCenter = (polygon) => {
  const points = parsePolygonPoints(polygon)
  if (!points.length) {
    return null
  }

  const total = points.reduce(
    (result, point) => ({
      longitude: result.longitude + point.longitude,
      latitude: result.latitude + point.latitude
    }),
    { longitude: 0, latitude: 0 }
  )

  return {
    longitude: total.longitude / points.length,
    latitude: total.latitude / points.length
  }
}

export const getPolygonArea = (polygon) => {
  const points = parsePolygonPoints(polygon)

  if (points.length < 3) {
    return 0
  }

  let area = 0

  points.forEach((point, index) => {
    const nextPoint = points[(index + 1) % points.length]
    area += point.longitude * nextPoint.latitude - nextPoint.longitude * point.latitude
  })

  return Math.abs(area) / 2
}

const getOrientation = (pointA, pointB, pointC) => {
  const value =
    (pointB.latitude - pointA.latitude) * (pointC.longitude - pointB.longitude) -
    (pointB.longitude - pointA.longitude) * (pointC.latitude - pointB.latitude)

  if (Math.abs(value) < EPSILON) {
    return 0
  }

  return value > 0 ? 1 : 2
}

const isPointOnSegment = (pointA, pointB, pointC) => {
  return (
    pointB.longitude <= Math.max(pointA.longitude, pointC.longitude) + EPSILON &&
    pointB.longitude + EPSILON >= Math.min(pointA.longitude, pointC.longitude) &&
    pointB.latitude <= Math.max(pointA.latitude, pointC.latitude) + EPSILON &&
    pointB.latitude + EPSILON >= Math.min(pointA.latitude, pointC.latitude)
  )
}

const doSegmentsIntersect = (segmentAStart, segmentAEnd, segmentBStart, segmentBEnd) => {
  const orientation1 = getOrientation(segmentAStart, segmentAEnd, segmentBStart)
  const orientation2 = getOrientation(segmentAStart, segmentAEnd, segmentBEnd)
  const orientation3 = getOrientation(segmentBStart, segmentBEnd, segmentAStart)
  const orientation4 = getOrientation(segmentBStart, segmentBEnd, segmentAEnd)

  if (orientation1 !== orientation2 && orientation3 !== orientation4) {
    return true
  }

  if (orientation1 === 0 && isPointOnSegment(segmentAStart, segmentBStart, segmentAEnd)) {
    return true
  }
  if (orientation2 === 0 && isPointOnSegment(segmentAStart, segmentBEnd, segmentAEnd)) {
    return true
  }
  if (orientation3 === 0 && isPointOnSegment(segmentBStart, segmentAStart, segmentBEnd)) {
    return true
  }
  if (orientation4 === 0 && isPointOnSegment(segmentBStart, segmentAEnd, segmentBEnd)) {
    return true
  }

  return false
}

export const hasSelfIntersection = (polygon) => {
  const points = parsePolygonPoints(polygon)
  const lastSegmentIndex = points.length - 1

  if (points.length < 4) {
    return false
  }

  for (let index = 0; index < points.length; index += 1) {
    const startPoint = points[index]
    const endPoint = points[(index + 1) % points.length]

    for (let compareIndex = index + 1; compareIndex < points.length; compareIndex += 1) {
      const compareStart = points[compareIndex]
      const compareEnd = points[(compareIndex + 1) % points.length]

      const isAdjacent =
        Math.abs(index - compareIndex) <= 1 ||
        (index === 0 && compareIndex === lastSegmentIndex) ||
        (compareIndex === 0 && index === lastSegmentIndex)

      if (isAdjacent) {
        continue
      }

      if (doSegmentsIntersect(startPoint, endPoint, compareStart, compareEnd)) {
        return true
      }
    }
  }

  return false
}

export const validatePolygonPoints = (polygon) => {
  const points = parsePolygonPoints(polygon)
  const errors = []

  if (points.length < 3) {
    errors.push('请至少绘制 3 个顶点。')
  }

  if (points.length >= 3 && getPolygonArea(points) <= EPSILON) {
    errors.push('片区面积无效，请重新绘制。')
  }

  if (points.length >= 4 && hasSelfIntersection(points)) {
    errors.push('片区边界存在自交，请调整顶点。')
  }

  return {
    points,
    valid: errors.length === 0,
    errors
  }
}
