import { getMallApiBase, getMarketApiBase, SOURCE, CHANNEL } from './config'
import { requestJson, assertSuccess } from './http'

export async function fetchQrcodeTicket(sceneStr) {
  const base = getMallApiBase()
  const payload = await requestJson(
    `${base}/api/v1/login/weixin_qrcode_ticket_scene?sceneStr=${encodeURIComponent(sceneStr)}`,
  )
  return assertSuccess(payload, '获取二维码失败')
}

export async function checkLoginScene(ticket, sceneStr) {
  const base = getMallApiBase()
  const qs = `ticket=${encodeURIComponent(ticket)}&sceneStr=${encodeURIComponent(sceneStr)}`
  return requestJson(`${base}/api/v1/login/check_login_scene?${qs}`)
}

export async function exchangeLoginBind(bind) {
  const base = getMallApiBase()
  const payload = await requestJson(
    `${base}/api/v1/login/exchange_bind?bind=${encodeURIComponent(bind)}`,
  )
  return assertSuccess(payload, '登录链接已失效，请重新扫码或使用无痕登录')
}

export async function queryGoodsList(userId) {
  const base = getMarketApiBase()
  const payload = await requestJson(`${base}/index/query_goods_list`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ source: SOURCE, channel: CHANNEL, userId }),
  })
  return assertSuccess(payload, '加载商品列表失败')
}

export async function queryMarketConfig(userId, goodsId) {
  const base = getMarketApiBase()
  const payload = await requestJson(`${base}/index/query_group_buy_market_config`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ userId, source: SOURCE, channel: CHANNEL, goodsId }),
  })
  return assertSuccess(payload, '加载商品详情失败')
}

export async function createPayOrder(body) {
  const base = getMallApiBase()
  const payload = await requestJson(`${base}/api/v1/alipay/create_pay_order`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  })
  return assertSuccess(payload, '创建订单失败')
}

export async function queryUserOrderList(userId, lastId, pageSize = 10) {
  const base = getMallApiBase()
  const payload = await requestJson(`${base}/api/v1/alipay/query_user_order_list`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ userId, lastId, pageSize }),
  })
  return assertSuccess(payload, '加载订单失败')
}

export async function refundOrder(userId, orderId) {
  const base = getMallApiBase()
  const payload = await requestJson(`${base}/api/v1/alipay/refund_order`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ userId, orderId }),
  })
  return assertSuccess(payload, '退单失败')
}
