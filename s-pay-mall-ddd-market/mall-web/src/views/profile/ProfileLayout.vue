<template>
  <AppLayout>
    <div class="page-wrap profile-layout">
      <aside class="profile-nav">
        <h2 class="profile-title">个人中心</h2>
        <router-link class="profile-link" active-class="active" to="/mall/profile/address">收货地址</router-link>
        <router-link class="profile-link" active-class="active" to="/mall/profile/favorites">我的收藏</router-link>
        <router-link class="profile-link" active-class="active" to="/mall/profile/history">浏览记录</router-link>
      </aside>
      <section class="profile-content">
        <router-view />
      </section>
    </div>
  </AppLayout>
</template>

<script setup>
import AppLayout from '@/components/AppLayout.vue'
</script>
