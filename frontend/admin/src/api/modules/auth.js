import request from '../request'

export const adminLogin = (data) => {
  return request({
    url: '/admin/log/login',
    method: 'POST',
    data,
    skipAuth: true
  })
}

export const adminLogout = () => {
  return request({
    url: '/admin/log/logout',
    method: 'POST'
  })
}
