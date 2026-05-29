<template>
  <header class="top-bar">
    <div class="brand"><span class="seal">书</span>书香拼团</div>
    <nav class="top-bar-actions">
      <router-link class="nav-link" :class="{ active: isActive('/mall/') }" to="/mall/">商城</router-link>
      <router-link class="nav-link" :class="{ active: isActive('/mall/orders') }" to="/mall/orders">我的订单</router-link>
      <router-link class="nav-link" :class="{ active: isActive('/mall/profile') }" to="/mall/profile">个人中心</router-link>
      <a class="nav-link" href="#" title="打开智能客服" @click.prevent="openCs">智能客服</a>
      <a class="nav-link" href="#" @click.prevent="logout">退出</a>
    </nav>
  </header>
  <main>
    <slot />
  </main>
</template>

<script setup>
import { useRoute, useRouter } from 'vue-router'
import { useAuth } from '@/composables/useAuth'
import { openCustomerService } from '@/utils/customerService'

const route = useRoute()
const router = useRouter()
const { logout: clearAuth } = useAuth()

function openCs() {
  const goodsId = route.name === 'goods-detail' ? route.params.goodsId : undefined
  openCustomerService(goodsId, router)
}

function isActive(prefix) {
  if (prefix === '/mall/') {
    return route.path === '/mall/' || route.path.startsWith('/mall/goods')
  }
  if (prefix === '/mall/profile') {
    return route.path.startsWith('/mall/profile')
  }
  return route.path === prefix || route.path.startsWith(`${prefix}/`)
}

function logout() {
  clearAuth()
  router.push({ name: 'login' })
}
</script>
