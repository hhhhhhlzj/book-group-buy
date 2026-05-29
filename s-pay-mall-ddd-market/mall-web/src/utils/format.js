export function obfuscateUserId(userId) {
  if (!userId) return ''
  if (userId.length <= 4) return userId
  return `${userId.slice(0, 2)}${'*'.repeat(userId.length - 4)}${userId.slice(-2)}`
}

export function formatPrice(value) {
  const num = Number(value)
  if (Number.isNaN(num)) return '--'
  return num.toFixed(0)
}

export function formatDateTime(timeStr) {
  if (!timeStr) return ''
  const date = new Date(timeStr)
  const pad = (n) => String(n).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`
}

const STATUS_MAP = {
  CREATE: '新创建',
  PAY_WAIT: '等待支付',
  PAY_SUCCESS: '支付成功',
  DEAL_DONE: '交易完成',
  CLOSE: '已关闭',
  WAIT_REFUND: '退款中',
}

export function orderStatusText(status) {
  return STATUS_MAP[status] || status
}
