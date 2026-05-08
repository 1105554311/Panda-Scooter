import { ECHARTS_CDN_URL } from '@/api/env'
import { loadExternalScript } from '@/utils/loadScript'

const isEChartsReady = () => {
  return typeof window !== 'undefined' && Boolean(window.echarts)
}

export const loadECharts = async () => {
  if (isEChartsReady()) {
    return window.echarts
  }

  await loadExternalScript({
    src: ECHARTS_CDN_URL,
    id: 'admin-echarts-script',
    checkLoaded: isEChartsReady
  })

  if (!isEChartsReady()) {
    throw new Error('ECharts 加载失败')
  }

  return window.echarts
}
