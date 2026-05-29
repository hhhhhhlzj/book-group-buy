<template>
  <router-link class="goods-card" :to="`/mall/goods/${book.goodsId}`">
    <div class="goods-card-cover">
      <img :src="book.cover" :alt="book.title" />
    </div>
    <div class="goods-card-body">
      <div class="goods-card-title">{{ book.title }}</div>
      <div class="goods-card-blurb">{{ book.blurb }}</div>
      <div class="goods-card-price">
        <template v-if="book.usingFallback">￥{{ book.payPrice }} 起（参考价）</template>
        <template v-else>
          拼团价 ￥{{ book.payPrice }}
          <span v-if="book.originalPrice > book.payPrice" class="goods-card-origin">￥{{ book.originalPrice }}</span>
        </template>
      </div>
      <div class="goods-card-hint">{{ book.teamHint }}</div>
    </div>
  </router-link>
</template>

<script setup>
defineProps({
  book: { type: Object, required: true },
})
</script>
