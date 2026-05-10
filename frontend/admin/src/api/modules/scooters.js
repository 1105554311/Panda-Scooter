import request from '../request'

export const getScooterList = (params = {}) => {
  return request({
    url: '/admin/scooter/getScooterList',
    method: 'GET',
    params: {
      areaId: params.areaId
    }
  })
}
