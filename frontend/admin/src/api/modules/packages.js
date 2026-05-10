import request from '../request'

export const getPackageList = (params) => {
  return request({
    url: '/admin/packages/getPackageList',
    method: 'GET',
    params
  })
}

export const addPackage = (data) => {
  return request({
    url: '/admin/packages/addPackage',
    method: 'POST',
    data
  })
}

export const editPackage = (data) => {
  return request({
    url: '/admin/packages/editPackage',
    method: 'POST',
    data
  })
}

export const deletePackage = (payload = {}) => {
  return request({
    url: '/admin/packages/deletePackage',
    method: 'DELETE',
    params: {
      packageId: payload.packageId ?? payload.id,
      title: payload.title
    }
  })
}
