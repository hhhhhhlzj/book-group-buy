import { getCustomerServiceBase, SOURCE, CHANNEL, DEFAULT_GOODS_ID } from '@/api/config'
import { getCookie, isValidLoginToken } from '@/utils/cookie'

/**
 * 打开拼团智能客服（新标签），携带 userId、goodsId、source、channel。
 * @param {string} [goodsId] 当前商品 ID
 * @param {import('vue-router').Router} [router] 未登录时跳转登录
 * @returns {boolean} 是否已打开客服页
 */
export function openCustomerService(goodsId, router) {
  const userId = getCookie('loginToken')
  if (!userId || !isValidLoginToken(userId)) {
    if (router) {
      const redirect =
        router.currentRoute.value?.fullPath && router.currentRoute.value.name !== 'login'
          ? router.currentRoute.value.fullPath
          : undefined
      router.push({ name: 'login', query: redirect ? { redirect } : {} })
    }
    return false
  }
  const gid = goodsId || DEFAULT_GOODS_ID
  const base = getCustomerServiceBase().replace(/\/$/, '')
  const q = new URLSearchParams({
    userId,
    goodsId: gid,
    source: SOURCE,
    channel: CHANNEL,
  })
  window.open(`${base}/group-buy-chat.html?${q.toString()}`, '_blank')
  return true
}
