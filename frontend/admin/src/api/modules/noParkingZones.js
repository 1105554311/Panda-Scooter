import request from '../request'

const normalizeNoParkingZoneListParams = (params = {}) => {
  const { pageSize, pagesize, ...rest } = params

  return {
    ...rest,
    pagesize: pagesize ?? pageSize
  }
}

export const getNoParkingZoneList = (params) => {
  return request({
    url: '/admin/noParkingZones/getZoneList',
    method: 'GET',
    params: normalizeNoParkingZoneListParams(params)
  })
}

export const addNoParkingZone = (data) => {
  return request({
    url: '/admin/noParkingZones/addZone',
    method: 'POST',
    data
  })
}

export const editNoParkingZone = (data) => {
  return request({
    url: '/admin/noParkingZones/editZone',
    method: 'POST',
    data
  })
}

export const deleteNoParkingZone = (payload = {}) => {
  return request({
    url: '/admin/noParkingZones/deleteZone',
    method: 'DELETE',
    params: {
      NoParkingAreaId: payload.NoParkingAreaId ?? payload.id,
      name: payload.name
    }
  })
}
