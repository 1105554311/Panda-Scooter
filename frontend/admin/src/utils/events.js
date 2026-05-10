export const APP_EVENTS = Object.freeze({
  toast: 'panda-admin:toast',
  unauthorized: 'panda-admin:unauthorized'
})

const emitEvent = (name, detail) => {
  if (typeof window === 'undefined') {
    return
  }

  window.dispatchEvent(new CustomEvent(name, { detail }))
}

export const emitToast = (detail) => {
  emitEvent(APP_EVENTS.toast, detail)
}

export const emitUnauthorized = (detail) => {
  emitEvent(APP_EVENTS.unauthorized, detail)
}
