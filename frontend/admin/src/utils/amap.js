import {
  AMAP_JSAPI_VERSION,
  AMAP_SECURITY_JS_CODE,
  AMAP_WEB_KEY
} from '@/api/env'
import { loadExternalScript } from '@/utils/loadScript'

const REQUIRED_PLUGINS = ['AMap.MouseTool', 'AMap.PolygonEditor', 'AMap.ToolBar', 'AMap.Scale']

let amapLoadPromise = null

const isAmapLoaderReady = () => {
  return typeof window !== 'undefined' && Boolean(window.AMapLoader)
}

export const isAmapConfigured = () => {
  return Boolean(String(AMAP_WEB_KEY || '').trim())
}

export const loadAmap = async () => {
  if (!isAmapConfigured()) {
    throw new Error('未配置高德地图 Web Key')
  }

  if (amapLoadPromise) {
    return amapLoadPromise
  }

  amapLoadPromise = (async () => {
    if (typeof window === 'undefined') {
      throw new Error('当前环境不支持加载地图')
    }

    const trimmedSecurityCode = String(AMAP_SECURITY_JS_CODE || '').trim()
    if (trimmedSecurityCode) {
      window._AMapSecurityConfig = {
        securityJsCode: trimmedSecurityCode
      }
    }

    await loadExternalScript({
      src: 'https://webapi.amap.com/loader.js',
      id: 'admin-amap-loader-script',
      checkLoaded: isAmapLoaderReady
    })

    const AMap = await window.AMapLoader.load({
      key: String(AMAP_WEB_KEY).trim(),
      version: AMAP_JSAPI_VERSION,
      plugins: REQUIRED_PLUGINS
    })

    if (!AMap) {
      throw new Error('高德地图对象初始化失败')
    }

    return AMap
  })().catch((error) => {
    amapLoadPromise = null
    throw error
  })

  return amapLoadPromise
}
