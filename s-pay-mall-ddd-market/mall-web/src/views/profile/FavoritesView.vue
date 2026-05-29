<template>
  <div>
    <h3>我的收藏</h3>
    <p class="lead">最多 5 条</p>
    <p v-if="error" class="lead lead-error">{{ error }}</p>
    <div v-if="!loading && !items.length" class="empty-state">暂无收藏</div>
    <div class="profile-books-grid">
      <ProfileBookCard v-for="book in items" :key="book.goodsId" :book="book" />
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useAuth } from '@/composables/useAuth'
import { CATALOG_BOOKS } from '@/data/books'
import { listFavorites } from '@/api/userCenter'
import ProfileBookCard from '@/components/ProfileBookCard.vue'

const { requireUserId } = useAuth()

const goodsIds = ref([])
const loading = ref(false)
const error = ref('')

const items = computed(() => {
  const set = new Set(goodsIds.value)
  return CATALOG_BOOKS.filter((b) => set.has(b.goodsId))
})

onMounted(async () => {
  loading.value = true
  try {
    const data = await listFavorites(requireUserId())
    goodsIds.value = data.goodsIds || []
  } catch (e) {
    error.value = e.message || '加载失败'
  } finally {
    loading.value = false
  }
})
</script>
