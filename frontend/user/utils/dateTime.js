const BACKEND_DATE_TIME_PATTERN = /^(\d{4})-(\d{2})-(\d{2}) (\d{2}):(\d{2}):(\d{2})$/

const pad = (value) => String(value).padStart(2, '0')

export const parseDateTimeValue = (value) => {
  if (!value && value !== 0) {
    return null
  }

  if (value instanceof Date) {
    return Number.isNaN(value.getTime()) ? null : value
  }

  if (typeof value === 'string') {
    const trimmed = value.trim()
    const matched = trimmed.match(BACKEND_DATE_TIME_PATTERN)

    if (matched) {
      const [, year, month, day, hour, minute, second] = matched
      const date = new Date(
        Number(year),
        Number(month) - 1,
        Number(day),
        Number(hour),
        Number(minute),
        Number(second)
      )

      return Number.isNaN(date.getTime()) ? null : date
    }

    const isoDate = new Date(trimmed)
    return Number.isNaN(isoDate.getTime()) ? null : isoDate
  }

  const nextDate = new Date(value)
  return Number.isNaN(nextDate.getTime()) ? null : nextDate
}

export const formatBackendDateTime = (value = new Date()) => {
  const date = parseDateTimeValue(value)

  if (!date) {
    return ''
  }

  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
}

export const normalizeBackendDateTime = (value, fallback = new Date()) => {
  const formatted = formatBackendDateTime(value)
  return formatted || formatBackendDateTime(fallback)
}
