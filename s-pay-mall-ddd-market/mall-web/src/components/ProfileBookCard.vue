<template>
  <router-link class="goods-card profile-book-card" :to="`/mall/goods/${book.goodsId}`">
    <div class="goods-card-cover">
      <img :src="normalizeCover(book.cover)" :alt="book.title" />
    </div>
    <div class="goods-card-body">
      <div class="goods-card-title">{{ book.title }}</div>
      <div class="goods-card-blurb">{{ book.blurb }}</div>
    </div>
  </router-link>
</template>

<script setup>
import { normalizeCover } from '@/data/books'

defineProps({
  book: { type: Object, required: true },
})
</script>
