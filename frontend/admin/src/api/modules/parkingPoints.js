import request from '../request'

const normalizeParkingPointListParams = (params = {}) => {
  const { pageSize, pagesize, ...rest } = params

  return {
    ...rest,
    pagesize: pagesize ?? pageSize
  }
}

const normalizeParkingPointPayload = (payload = {}) => {
  const {
    longitude,
    longtitude,
    createTime,
    create_time,
    ...rest
  } = payload

  return {
    ...rest,
    longtitude: longtitude ?? longitude,
    create_time: create_time ?? createTime
  }
}

export const getParkingPointList = (params) => {
  return request({
    url: '/admin/ParkingPoint/getPointList',
    method: 'GET',
    params: normalizeParkingPointListParams(params)
  })
}

export const addParkingPoint = (data) => {
  return request({
    url: '/admin/ParkingPoint/addPoint',
    method: 'POST',
    data: normalizeParkingPointPayload(data)
  })
}

export const editParkingPoint = (data) => {
  return request({
    url: '/admin/ParkingPoint/editPoint',
    method: 'POST',
    data: normalizeParkingPointPayload(data)
  })
}

export const deleteParkingPoint = (payload = {}) => {
  return request({
    url: '/admin/ParkingPoint/deletePoint',
    method: 'DELETE',
    params: {
      ParkingPointId: payload.ParkingPointId ?? payload.id,
      name: payload.name
    }
  })
}
