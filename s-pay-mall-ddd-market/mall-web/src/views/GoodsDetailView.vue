<template>
  <AppLayout>
    <div class="page-wrap" v-if="page">
      <div class="detail-hero">
        <img :src="currentSlide" :alt="page.title" />
      </div>
      <div class="swiper-dots">
        <span
          v-for="(slide, index) in page.slides"
          :key="index"
          class="swiper-dot"
          :class="{ active: index === slideIndex }"
        />
      </div>

      <div class="detail-title-row">
        <h1 class="product-title">{{ page.title }}</h1>
        <button type="button" class="btn-secondary fav-btn" @click="toggleFavorite">
          {{ favorited ? '已收藏' : '收藏' }}
        </button>
      </div>
      <div class="book-meta">
        <div v-for="meta in page.meta" :key="meta.label" class="meta-item">
          <span class="label">{{ meta.label }}</span>
          <span class="value">{{ meta.value }}</span>
        </div>
      </div>

      <p v-if="promotionText" class="promotion-text">{{ promotionText }}</p>
      <p v-if="loadError" class="lead lead-error">{{ loadError }}</p>

      <section>
        <h3>本书特色</h3>
        <div class="highlight-grid">
          <div v-for="item in page.highlights" :key="item.title" class="highlight-item">
            <div class="h-title">{{ item.title }}</div>
            <div class="h-desc">{{ item.desc }}</div>
          </div>
        </div>
      </section>

      <section class="group-list">
        <h3>拼团队伍</h3>
        <div v-if="!teamList.length" class="empty-state">暂无进行中的队伍，赶紧开团吧</div>
        <div v-for="team in teamList" :key="team.teamId" class="group-item">
          <div>
            <div>{{ obfuscateUserId(team.userId) }}</div>
            <div class="goods-card-hint">
              组队仅剩 {{ team.targetCount - team.lockCount }} 人 · {{ team.validTimeCountdown }}
            </div>
          </div>
          <button type="button" class="group-btn" @click="payJoin(team.teamId)">参与拼团</button>
        </div>
      </section>
    </div>

    <div class="action-bar" v-if="market">
      <button type="button" class="secondary" @click="$router.push('/mall/orders')">我的订单</button>
      <button type="button" class="secondary" @click="payAlone">单独购买</button>
      <button type="button" @click="payGroup">开团购买</button>
    </div>

    <div v-if="addressModal" class="modal-overlay" @click.self="addressModal = null">
      <div class="modal-card">
        <h3>选择收货地址</h3>
        <p v-if="addressError" class="lead lead-error">{{ addressError }}</p>
        <div v-if="!addressList.length" class="empty-state">
          <p>暂无收货地址，请先添加后再下单</p>
          <button type="button" class="btn-primary" style="margin-top: 12px" @click="goAddAddress">
            去添加地址
          </button>
        </div>
        <div
          v-for="addr in addressList"
          :key="addr.id"
          class="address-pick-item"
          :class="{ selected: selectedAddressId === addr.id }"
          @click="selectedAddressId = addr.id"
        >
          <strong>{{ addr.receiverName }}</strong> {{ addr.receiverPhone }}
          <span v-if="addr.isDefault" class="tag-default">默认</span>
          <div class="goods-card-hint">
            {{ addr.province }}{{ addr.city }}{{ addr.district }}{{ addr.detailAddress }}
          </div>
        </div>
        <div class="modal-actions">
          <button type="button" class="btn-primary" :disabled="!selectedAddressId" @click="confirmAddressPay">
            确认下单
          </button>
          <button type="button" class="btn-secondary" @click="addressModal = null">取消</button>
        </div>
      </div>
    </div>

    <div v-if="paymentModal" class="modal-overlay" @click.self="paymentModal = false">
      <div class="modal-card payment-modal">
        <h3>支付确认</h3>
        <p>商品金额：￥{{ paymentModal.price }}</p>
        <p>买家账号：<span class="copyable" @click="copyText('kvhmoj3832@sandbox.com')">kvhmoj3832@sandbox.com</span></p>
        <p>登录密码：111111</p>
        <p>支付密码：111111</p>
        <div class="modal-actions">
          <button type="button" class="btn-primary" @click="submitPayForm">确认支付</button>
          <button type="button" class="btn-secondary" @click="closePayment">取消</button>
        </div>
      </div>
    </div>
  </AppLayout>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AppLayout from '@/components/AppLayout.vue'
import { getBookPage } from '@/data/books'
import { useAuth } from '@/composables/useAuth'
import { createPayOrder, queryMarketConfig } from '@/api/mall'
import {
  addFavorite,
  favoriteStatus,
  listAddresses,
  recordBrowse,
  removeFavorite,
} from '@/api/userCenter'
import { obfuscateUserId, formatPrice } from '@/utils/format'

const route = useRoute()
const router = useRouter()
const { requireUserId } = useAuth()

const goodsId = computed(() => route.params.goodsId)
const page = computed(() => getBookPage(goodsId.value))
const slideIndex = ref(0)
const market = ref(null)
const teamList = ref([])
const loadError = ref('')
const paymentModal = ref(null)
const favorited = ref(false)
const addressModal = ref(null)
const addressList = ref([])
const selectedAddressId = ref(null)
const addressError = ref('')
let pendingPay = null
let payFormHtml = ''
let slideTimer = null

const currentSlide = computed(() => page.value.slides[slideIndex.value]?.src || '/images/book-cover.png')

const promotionText = computed(() => {
  if (!market.value) return ''
  const { goods, teamStatistic } = market.value
  return `直降 ¥${formatPrice(goods.deductionPrice)}，${teamStatistic.allTeamUserCount}人再抢，参与马上抢到`
})

function startSlideShow() {
  slideTimer = setInterval(() => {
    slideIndex.value = (slideIndex.value + 1) % page.value.slides.length
  }, 3000)
}

async function loadDetail() {
  const userId = requireUserId()
  try {
    const data = await queryMarketConfig(userId, goodsId.value)
    market.value = data
    teamList.value = data.teamList || []
    document.title = page.value.docTitle
  } catch (e) {
    loadError.value = e.message || '加载失败'
  }
}

function injectPayForm(html, price) {
  document.querySelectorAll('form[data-pay-form]').forEach((node) => node.remove())
  const container = document.createElement('div')
  container.innerHTML = html
  container.querySelectorAll('form').forEach((form) => {
    form.setAttribute('data-pay-form', '1')
    document.body.appendChild(form)
  })
  payFormHtml = html
  paymentModal.value = { price: formatPrice(price) }
}

async function requestPay(body, price) {
  try {
    const html = await createPayOrder(body)
    injectPayForm(html, price)
  } catch (e) {
    loadError.value = e.message || '下单失败'
  }
}

async function openAddressPay(body, price) {
  pendingPay = { body, price }
  addressError.value = ''
  selectedAddressId.value = null
  try {
    const data = await listAddresses(requireUserId())
    addressList.value = data.list || []
    if (!addressList.value.length) {
      loadError.value = '请先添加收货地址'
      router.push('/mall/profile/address')
      return
    }
    const def = addressList.value.find((a) => a.isDefault)
    selectedAddressId.value = def?.id || addressList.value[0]?.id || null
    addressModal.value = true
  } catch (e) {
    loadError.value = e.message || '加载地址失败'
  }
}

function goAddAddress() {
  addressModal.value = null
  router.push('/mall/profile/address')
}

function confirmAddressPay() {
  if (!pendingPay || !selectedAddressId.value) return
  const { body, price } = pendingPay
  addressModal.value = null
  requestPay({ ...body, addressId: selectedAddressId.value }, price)
  pendingPay = null
}

function payGroup() {
  if (!market.value) return
  const userId = requireUserId()
  openAddressPay(
    {
      userId,
      productId: market.value.goods.goodsId,
      teamId: null,
      activityId: market.value.activityId,
      marketType: 1,
    },
    market.value.goods.payPrice,
  )
}

function payJoin(teamId) {
  if (!market.value) return
  const userId = requireUserId()
  openAddressPay(
    {
      userId,
      productId: market.value.goods.goodsId,
      teamId,
      activityId: market.value.activityId,
      marketType: 1,
    },
    market.value.goods.payPrice,
  )
}

function payAlone() {
  if (!market.value) return
  const userId = requireUserId()
  openAddressPay(
    {
      userId,
      productId: market.value.goods.goodsId,
      marketType: 0,
    },
    market.value.goods.originalPrice,
  )
}

async function loadFavoriteStatus() {
  try {
    const data = await favoriteStatus(requireUserId(), goodsId.value)
    favorited.value = !!data.favorited
  } catch {
    favorited.value = false
  }
}

async function toggleFavorite() {
  const userId = requireUserId()
  try {
    if (favorited.value) {
      await removeFavorite(userId, goodsId.value)
      favorited.value = false
    } else {
      await addFavorite(userId, goodsId.value)
      favorited.value = true
    }
  } catch (e) {
    loadError.value = e.message || '收藏操作失败'
  }
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

function copyText(text) {
  navigator.clipboard?.writeText(text)
}

onMounted(async () => {
  loadDetail()
  startSlideShow()
  try {
    await recordBrowse(requireUserId(), goodsId.value)
  } catch {
    /* 浏览记录失败不阻断详情 */
  }
  loadFavoriteStatus()
})

onBeforeUnmount(() => {
  if (slideTimer) clearInterval(slideTimer)
  closePayment()
})
</script>
