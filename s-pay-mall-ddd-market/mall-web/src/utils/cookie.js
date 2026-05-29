export function getCookie(name) {
  const value = `; ${document.cookie}`
  const parts = value.split(`; ${name}=`)
  if (parts.length === 2) {
    const raw = parts.pop().split(';').shift()
    try {
      return decodeURIComponent(raw)
    } catch {
      return raw
    }
  }
  return null
}

/** 合法 loginToken：指纹或微信 openid，不能是整段二维码 ticket */
export function isValidLoginToken(token) {
  if (!token || typeof token !== 'string') return false
  if (token.length > 64) return false
  if (token.includes('+') || token.includes('%')) return false
  return true
}

export function setCookie(name, value, days = 30) {
  const date = new Date()
  date.setTime(date.getTime() + days * 24 * 60 * 60 * 1000)
  document.cookie = `${name}=${encodeURIComponent(value)};expires=${date.toUTCString()};path=/`
}

export function removeCookie(name) {
  document.cookie = `${name}=;expires=Thu, 01 Jan 1970 00:00:00 GMT;path=/`
}
