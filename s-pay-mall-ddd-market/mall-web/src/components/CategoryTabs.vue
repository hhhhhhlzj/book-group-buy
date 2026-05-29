<template>
  <div class="category-tabs">
    <button
      v-for="cat in categories"
      :key="cat.id"
      type="button"
      class="category-tab"
      :class="{ active: modelValue === cat.id }"
      @click="$emit('update:modelValue', cat.id)"
    >
      {{ cat.name }}
    </button>
  </div>
</template>

<script setup>
import { CATEGORIES } from '@/data/categories'

defineProps({
  modelValue: { type: String, default: 'all' },
})

defineEmits(['update:modelValue'])

const categories = CATEGORIES
</script>
