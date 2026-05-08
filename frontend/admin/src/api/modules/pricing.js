import request from '../request'

export const getPricingRules = () => {
  return request({
    url: '/admin/pricingRule/getRules',
    method: 'GET'
  })
}

export const updatePricingRules = (data) => {
  return request({
    url: '/admin/pricingRule/editRules',
    method: 'POST',
    data
  })
}
