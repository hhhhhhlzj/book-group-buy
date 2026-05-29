<template>
  <div>
    <h3>浏览记录</h3>
    <p class="lead">最近 5 条</p>
    <p v-if="error" class="lead lead-error">{{ error }}</p>
    <div v-if="!loading && !items.length" class="empty-state">暂无浏览记录</div>
    <div class="profile-books-grid">
      <ProfileBookCard v-for="book in items" :key="book.goodsId" :book="book" />
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useAuth } from '@/composables/useAuth'
import { CATALOG_BOOKS } from '@/data/books'
import { listBrowseHistory } from '@/api/userCenter'
import ProfileBookCard from '@/components/ProfileBookCard.vue'

const { requireUserId } = useAuth()

const goodsIds = ref([])
const loading = ref(false)
const error = ref('')

const items = computed(() => {
  const order = goodsIds.value
  return order
    .map((id) => CATALOG_BOOKS.find((b) => b.goodsId === id))
    .filter(Boolean)
})

onMounted(async () => {
  loading.value = true
  try {
    const data = await listBrowseHistory(requireUserId())
    goodsIds.value = data.goodsIds || []
  } catch (e) {
    error.value = e.message || '加载失败'
  } finally {
    loading.value = false
  }
})
</script>
