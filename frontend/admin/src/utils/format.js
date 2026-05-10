const numberFormatter = new Intl.NumberFormat('zh-CN')
const currencyFormatter = new Intl.NumberFormat('zh-CN', {
  style: 'currency',
  currency: 'CNY',
  minimumFractionDigits: 2,
  maximumFractionDigits: 2
})

const dateTimeFormatter = new Intl.DateTimeFormat('zh-CN', {
  year: 'numeric',
  month: '2-digit',
  day: '2-digit',
  hour: '2-digit',
  minute: '2-digit'
})

const dateFormatter = new Intl.DateTimeFormat('zh-CN', {
  year: 'numeric',
  month: '2-digit',
  day: '2-digit'
})

export const formatNumber = (value, fallback = '--') => {
  const numericValue = Number(value)
  return Number.isFinite(numericValue) ? numberFormatter.format(numericValue) : fallback
}

export const formatCurrency = (value, fallback = '--') => {
  const numericValue = Number(value)
  return Number.isFinite(numericValue) ? currencyFormatter.format(numericValue) : fallback
}

export const formatDate = (value, fallback = '--') => {
  if (!value) {
    return fallback
  }

  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? fallback : dateFormatter.format(date)
}

export const formatDateTime = (value, fallback = '--') => {
  if (!value) {
    return fallback
  }

  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? fallback : dateTimeFormatter.format(date)
}

export const formatPackageType = (value) => {
  switch (Number(value)) {
    case 1:
      return '月卡'
    case 2:
      return '季卡'
    case 3:
      return '年卡'
    default:
      return '未分类'
  }
}

export const formatPackageStatus = (value) => {
  switch (Number(value)) {
    case 1:
      return {
        text: '启用',
        tone: 'success'
      }
    case 0:
      return {
        text: '停用',
        tone: 'warning'
      }
    default:
      return {
        text: '未知',
        tone: 'danger'
      }
  }
}

export const formatBinaryStatus = (value, enabledText = '启用', disabledText = '停用') => {
  switch (Number(value)) {
    case 1:
      return {
        text: enabledText,
        tone: 'success'
      }
    case 0:
      return {
        text: disabledText,
        tone: 'warning'
      }
    default:
      return {
        text: '未知',
        tone: 'danger'
      }
  }
}

export const createDateRange = (days = 30) => {
  const endDate = new Date()
  const startDate = new Date()
  startDate.setDate(endDate.getDate() - days)

  return {
    startDate: toISODate(startDate),
    endDate: toISODate(endDate)
  }
}

export const toISODate = (value = new Date()) => {
  const date = value instanceof Date ? value : new Date(value)
  if (Number.isNaN(date.getTime())) {
    return ''
  }

  const year = date.getFullYear()
  const month = `${date.getMonth() + 1}`.padStart(2, '0')
  const day = `${date.getDate()}`.padStart(2, '0')
  return `${year}-${month}-${day}`
}
