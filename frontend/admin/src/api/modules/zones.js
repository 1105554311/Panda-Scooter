import request from '../request'

const normalizeZoneListParams = (params = {}) => {
  const {
    pageSize,
    pagesize,
    dispatcherId,
    ...rest
  } = params

  return {
    ...rest,
    pagesize: pagesize ?? pageSize,
    dispatcherId
  }
}

export const getZoneList = (params) => {
  return request({
    url: '/admin/zones/getZoneList',
    method: 'GET',
    params: normalizeZoneListParams(params)
  })
}

export const addZone = (data) => {
  return request({
    url: '/admin/zones/addZone',
    method: 'POST',
    data
  })
}

export const getZoneDetail = (params = {}) => {
  return request({
    url: '/admin/zones/getZoneDetail',
    method: 'GET',
    params: {
      areaId: params.areaId ?? params.id,
      id: params.id ?? params.areaId
    }
  })
}

export const editZone = (data) => {
  return request({
    url: '/admin/zones/editZone',
    method: 'POST',
    data
  })
}

export const deleteZone = (payload = {}) => {
  return request({
    url: '/admin/zones/deleteZone',
    method: 'DELETE',
    params: {
      areaId: payload.areaId ?? payload.id,
      name: payload.name
    }
  })
}
