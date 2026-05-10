import request from '../request'

const normalizeDispatcherListParams = (params = {}) => {
  const {
    pageSize,
    pagesize,
    areaId,
    ...rest
  } = params

  return {
    ...rest,
    pagesize: pagesize ?? pageSize,
    areaId
  }
}

export const getDispatcherList = (params) => {
  return request({
    url: '/admin/dispatchers/getDispatcherList',
    method: 'GET',
    params: normalizeDispatcherListParams(params)
  })
}

export const addDispatcher = (data) => {
  return request({
    url: '/admin/dispatchers/addDispatcher',
    method: 'POST',
    data
  })
}

export const editDispatcher = (data) => {
  return request({
    url: '/admin/dispatchers/editDispatcher',
    method: 'POST',
    data
  })
}

export const deleteDispatcher = (payload = {}) => {
  return request({
    url: '/admin/dispatchers/deleteDispatcher',
    method: 'DELETE',
    params: {
      dispatcherId: payload.dispatcherId ?? payload.id,
      name: payload.name,
      email: payload.email
    }
  })
}
