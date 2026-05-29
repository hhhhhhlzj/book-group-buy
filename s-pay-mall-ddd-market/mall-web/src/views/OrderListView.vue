<template>
  <AppLayout>
    <div class="page-wrap">
      <p class="lead">用户：{{ obfuscateUserId(userId) }}</p>
      <div v-if="loading" class="loading-tip">加载中…</div>
      <div v-if="error" class="lead lead-error">{{ error }}</div>
      <div v-if="!loading && !orders.length" class="empty-state">暂无订单</div>

      <div v-for="order in orders" :key="order.orderId" class="order-item">
        <div class="order-header">
          <div @click="copyOrderId(order.orderId)" style="cursor: pointer">订单号：{{ order.orderId }}</div>
          <div class="order-status">{{ orderStatusText(order.status) }}</div>
        </div>
        <div>{{ order.productName || '商品名称' }}</div>
        <div class="goods-card-hint">{{ formatDateTime(order.orderTime) }} · ¥{{ order.payAmount || order.totalAmount }}</div>
        <div class="order-actions">
          <button
            v-if="order.status === 'PAY_WAIT'"
            type="button"
            class="btn-primary"
            @click="continuePay(order)"
          >
            继续支付
          </button>
          <button
            type="button"
            class="btn-secondary"
            :disabled="order.status === 'CLOSE'"
            @click="openRefund(order.orderId)"
          >
            {{ order.status === 'CLOSE' ? '已关闭' : '申请退单' }}
          </button>
        </div>
      </div>

      <button v-if="hasMore" type="button" class="btn-primary" style="margin-top: 12px" :disabled="loading" @click="loadMore">
        加载更多
      </button>
    </div>

    <div v-if="refundOrderId" class="modal-overlay" @click.self="refundOrderId = null">
      <div class="modal-card">
        <h3>确认退单？</h3>
        <p class="lead">订单号：{{ refundOrderId }}</p>
        <div class="modal-actions">
          <button type="button" class="btn-primary" @click="confirmRefund">确认</button>
          <button type="button" class="btn-secondary" @click="refundOrderId = null">取消</button>
        </div>
      </div>
    </div>

    <div v-if="paymentModal" class="modal-overlay" @click.self="closePayment">
      <div class="modal-card payment-modal">
        <h3>支付确认</h3>
        <p>商品金额：￥{{ paymentModal.price }}</p>
        <p class="goods-card-hint">沙箱异常时可只验证「已唤起支付单」，不必完成付款</p>
        <div class="modal-actions">
          <button type="button" class="btn-primary" @click="submitPayForm">确认支付</button>
          <button type="button" class="btn-secondary" @click="closePayment">取消</button>
        </div>
      </div>
    </div>
  </AppLayout>
</template>

<script setup>
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import AppLayout from '@/components/AppLayout.vue'
import { useAuth } from '@/composables/useAuth'
import { createPayOrder, queryUserOrderList, refundOrder } from '@/api/mall'
import { listAddresses } from '@/api/userCenter'
import { formatDateTime, obfuscateUserId, orderStatusText, formatPrice } from '@/utils/format'

const router = useRouter()
const { requireUserId } = useAuth()

const userId = ref('')
const orders = ref([])
const lastId = ref(null)
const hasMore = ref(true)
const loading = ref(false)
const error = ref('')
const refundOrderId = ref(null)
const paymentModal = ref(null)

function injectPayForm(html, price) {
  document.querySelectorAll('form[data-pay-form]').forEach((node) => node.remove())
  const container = document.createElement('div')
  container.innerHTML = html
  container.querySelectorAll('form').forEach((form) => {
    form.setAttribute('data-pay-form', '1')
    document.body.appendChild(form)
  })
  paymentModal.value = { price: formatPrice(price) }
}

function submitPayForm() {
  const form = document.querySelector('form[data-pay-form]')
  if (form) form.submit()
  paymentModal.value = null
}

function closePayment() {
  document.querySelectorAll('form[data-pay-form]').forEach((node) => node.remove())
  paymentModal.value = null
}

async function continuePay(order) {
  error.value = ''
  try {
    const addrData = await listAddresses(userId.value)
    const list = addrData.list || []
    if (!list.length) {
      error.value = '请先添加收货地址'
      router.push('/mall/profile/address')
      return
    }
    const addr = list.find((a) => a.isDefault) || list[0]
    const html = await createPayOrder({
      userId: userId.value,
      productId: order.productId,
      marketType: order.marketType ?? 0,
      addressId: addr.id,
    })
    injectPayForm(html, order.payAmount || order.totalAmount)
  } catch (e) {
    error.value = e.message || '唤起支付失败'
  }
}

async function loadMore() {
  if (loading.value || !hasMore.value) return
  loading.value = true
  error.value = ''
  try {
    const data = await queryUserOrderList(userId.value, lastId.value)
    orders.value = lastId.value ? orders.value.concat(data.orderList || []) : data.orderList || []
    hasMore.value = !!data.hasMore
    lastId.value = data.lastId
  } catch (e) {
    error.value = e.message || '加载失败'
  } finally {
    loading.value = false
  }
}

function openRefund(orderId) {
  refundOrderId.value = orderId
}

async function confirmRefund() {
  try {
    const result = await refundOrder(userId.value, refundOrderId.value)
    if (result?.success === false) throw new Error(result.message || '退单失败')
    refundOrderId.value = null
    lastId.value = null
    hasMore.value = true
    orders.value = []
    await loadMore()
  } catch (e) {
    error.value = e.message || '退单失败'
  }
}

function copyOrderId(orderId) {
  navigator.clipboard?.writeText(orderId)
}

onMounted(() => {
  userId.value = requireUserId()
  loadMore()
})

onBeforeUnmount(closePayment)
</script>

<style scoped>
.order-actions {
  display: flex;
  gap: 8px;
  margin-top: 10px;
  flex-wrap: wrap;
}
</style>
