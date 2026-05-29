export async function requestJson(url, options = {}) {
  const response = await fetch(url, options)
  const payload = await response.json()
  return payload
}

export function assertSuccess(payload, fallback = '请求失败') {
  if (!payload || payload.code !== '0000') {
    throw new Error(payload?.info || fallback)
  }
  return payload.data
}
