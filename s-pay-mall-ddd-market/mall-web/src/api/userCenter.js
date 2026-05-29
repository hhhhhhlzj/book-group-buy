import { getMallApiBase } from './config'
import { requestJson, assertSuccess } from './http'

function post(path, body) {
  const base = getMallApiBase()
  return requestJson(`${base}/api/v1/user/${path}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  })
}

export async function listAddresses(userId) {
  const payload = await post('address/list', { userId })
  return assertSuccess(payload, '加载地址失败')
}

export async function saveAddress(body) {
  const payload = await post('address/save', body)
  return assertSuccess(payload, '保存地址失败')
}

export async function deleteAddress(userId, addressId) {
  const payload = await post('address/delete', { userId, addressId })
  return assertSuccess(payload, '删除地址失败')
}

export async function setDefaultAddress(userId, addressId) {
  const payload = await post('address/set_default', { userId, addressId })
  return assertSuccess(payload, '设置默认地址失败')
}

export async function listFavorites(userId) {
  const payload = await post('favorite/list', { userId })
  return assertSuccess(payload, '加载收藏失败')
}

export async function addFavorite(userId, goodsId) {
  const payload = await post('favorite/add', { userId, goodsId })
  return assertSuccess(payload, '收藏失败')
}

export async function removeFavorite(userId, goodsId) {
  const payload = await post('favorite/remove', { userId, goodsId })
  return assertSuccess(payload, '取消收藏失败')
}

export async function favoriteStatus(userId, goodsId) {
  const payload = await post('favorite/status', { userId, goodsId })
  return assertSuccess(payload, '查询收藏状态失败')
}

export async function listBrowseHistory(userId) {
  const payload = await post('browse/list', { userId })
  return assertSuccess(payload, '加载浏览记录失败')
}

export async function recordBrowse(userId, goodsId) {
  const payload = await post('browse/record', { userId, goodsId })
  return assertSuccess(payload, '记录浏览失败')
}
