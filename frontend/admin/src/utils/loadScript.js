const pendingScripts = new Map()

const createScriptElement = (src, id) => {
  const script = document.createElement('script')
  script.src = src
  script.async = true
  script.defer = true

  if (id) {
    script.dataset.loaderId = id
  }

  return script
}

export const loadExternalScript = ({ src, id = '', checkLoaded } = {}) => {
  if (typeof window === 'undefined' || typeof document === 'undefined') {
    return Promise.reject(new Error('当前环境不支持加载外部脚本'))
  }

  if (!src) {
    return Promise.reject(new Error('外部脚本地址不能为空'))
  }

  if (typeof checkLoaded === 'function' && checkLoaded()) {
    return Promise.resolve()
  }

  if (pendingScripts.has(src)) {
    return pendingScripts.get(src)
  }

  const existingScript = id
    ? document.querySelector(`script[data-loader-id="${id}"]`)
    : document.querySelector(`script[src="${src}"]`)

  const promise = new Promise((resolve, reject) => {
    const handleLoad = () => {
      if (typeof checkLoaded === 'function' && !checkLoaded()) {
        reject(new Error(`脚本已加载，但全局对象不可用: ${src}`))
        return
      }

      resolve()
    }

    const handleError = () => {
      reject(new Error(`脚本加载失败: ${src}`))
    }

    if (existingScript) {
      existingScript.addEventListener('load', handleLoad, { once: true })
      existingScript.addEventListener('error', handleError, { once: true })

      window.setTimeout(() => {
        if (typeof checkLoaded === 'function' && checkLoaded()) {
          resolve()
        }
      }, 0)

      return
    }

    const script = createScriptElement(src, id)
    script.addEventListener('load', handleLoad, { once: true })
    script.addEventListener('error', handleError, { once: true })
    document.head.appendChild(script)
  }).finally(() => {
    pendingScripts.delete(src)
  })

  pendingScripts.set(src, promise)
  return promise
}
