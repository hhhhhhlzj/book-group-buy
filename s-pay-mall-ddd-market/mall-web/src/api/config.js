const host = typeof window !== 'undefined' ? window.location.hostname || '127.0.0.1' : '127.0.0.1'

/** dev 走 Vite 代理（空字符串 = 相对路径）；prod 可 env 覆盖 */
export function getMallApiBase() {
  const env = import.meta.env.VITE_MALL_API
  if (env !== undefined && env !== '') return env.replace(/\/$/, '')
  if (import.meta.env.DEV) return ''
  return `http://${host}:8070`
}

export function getMarketApiBase() {
  const env = import.meta.env.VITE_MARKET_API
  if (env !== undefined && env !== '') return env.replace(/\/$/, '')
  if (import.meta.env.DEV) return '/gbm'
  return `http://${host}:8091/api/v1/gbm`
}

export const SOURCE = 's01'
export const CHANNEL = 'c01'
export const DEFAULT_GOODS_ID = '9890001'

export function imageUrl(path) {
  if (!path) path = '/images/book-cover.png'
  if (path.startsWith('http')) return path
  const normalized = path.replace(/^\.\//, '/')
  const assetPath = normalized.startsWith('/') ? normalized : `/${normalized}`
  if (import.meta.env.DEV) return assetPath
  return `${getMallApiBase()}${assetPath}`
}
