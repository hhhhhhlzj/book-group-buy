<template>
  <AppLayout>
    <div class="page-wrap">
      <p class="lead" :class="{ 'lead-error': loadError }">{{ statusText }}</p>
      <CategoryTabs v-model="activeCategory" />
      <div v-if="loading" class="loading-tip">加载中…</div>
      <div v-else class="catalog-grid">
        <GoodsCard v-for="book in filteredBooks" :key="book.goodsId" :book="book" />
      </div>
    </div>
  </AppLayout>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import AppLayout from '@/components/AppLayout.vue'
import CategoryTabs from '@/components/CategoryTabs.vue'
import GoodsCard from '@/components/GoodsCard.vue'
import { CATALOG_BOOKS, normalizeCover } from '@/data/books'
import { useAuth } from '@/composables/useAuth'
import { queryGoodsList } from '@/api/mall'
import { formatPrice } from '@/utils/format'

const { requireUserId } = useAuth()

const activeCategory = ref('all')
const loading = ref(true)
const loadError = ref(false)
const books = ref([])

const statusText = computed(() => {
  if (loadError.value) return '营销列表暂不可用，已展示参考价。请确认 8091 服务已启动。'
  return '以下为渠道书目，拼团价与在团人数来自营销服务（已登录试算）。'
})

const filteredBooks = computed(() => {
  if (activeCategory.value === 'all') return books.value
  return books.value.filter((b) => b.categoryId === activeCategory.value)
})

function buildFallbackBooks() {
  return CATALOG_BOOKS.map((item) => ({
    ...item,
    cover: normalizeCover(item.cover),
    payPrice: formatPrice(item.listPrice),
    originalPrice: item.listPrice,
    teamHint: '进入详情查看组队',
    usingFallback: true,
  }))
}

function mergeBooks(marketList) {
  const marketMap = {}
  ;(marketList || []).forEach((item) => {
    if (item?.goodsId) marketMap[item.goodsId] = item
  })

  return CATALOG_BOOKS.map((item) => {
    const market = marketMap[item.goodsId]
    const payPrice = market?.payPrice ?? item.listPrice
    const originalPrice = market?.originalPrice ?? item.listPrice
    const teamCount = market?.allTeamUserCount
    return {
      ...item,
      cover: normalizeCover(item.cover),
      payPrice: formatPrice(payPrice),
      originalPrice: Number(originalPrice),
      teamHint: teamCount != null ? `${teamCount} 人在团` : '进入详情查看组队',
      usingFallback: !market,
    }
  })
}

onMounted(async () => {
  const userId = requireUserId()
  try {
    const data = await queryGoodsList(userId)
    books.value = mergeBooks(data.goodsList)
    loadError.value = false
  } catch (e) {
    console.warn(e)
    books.value = buildFallbackBooks()
    loadError.value = true
  } finally {
    loading.value = false
  }
})
</script>
