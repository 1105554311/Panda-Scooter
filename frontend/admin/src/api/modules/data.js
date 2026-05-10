import request from '../request'

export const getDataOverview = (params) => {
  return request({
    url: '/admin/data/overview',
    method: 'GET',
    params
  })
}

export const getLiveData = () => {
  return request({
    url: '/admin/data/liveData',
    method: 'GET'
  })
}
