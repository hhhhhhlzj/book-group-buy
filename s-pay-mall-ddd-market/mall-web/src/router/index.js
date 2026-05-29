import { createRouter, createWebHistory } from 'vue-router'
import { getCookie, isValidLoginToken, removeCookie } from '@/utils/cookie'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', redirect: '/mall/' },
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/LoginView.vue'),
      meta: { guest: true },
    },
    {
      path: '/mall/',
      name: 'mall-home',
      component: () => import('@/views/MallHomeView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/mall/goods/:goodsId',
      name: 'goods-detail',
      component: () => import('@/views/GoodsDetailView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/mall/orders',
      name: 'orders',
      component: () => import('@/views/OrderListView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/mall/profile',
      component: () => import('@/views/profile/ProfileLayout.vue'),
      meta: { requiresAuth: true },
      children: [
        { path: '', redirect: '/mall/profile/address' },
        {
          path: 'address',
          name: 'profile-address',
          component: () => import('@/views/profile/AddressView.vue'),
        },
        {
          path: 'favorites',
          name: 'profile-favorites',
          component: () => import('@/views/profile/FavoritesView.vue'),
        },
        {
          path: 'history',
          name: 'profile-history',
          component: () => import('@/views/profile/HistoryView.vue'),
        },
      ],
    },
  ],
})

router.beforeEach((to) => {
  let loginToken = getCookie('loginToken')
  if (loginToken && !isValidLoginToken(loginToken)) {
    removeCookie('loginToken')
    loginToken = null
  }
  if (to.meta.requiresAuth && !loginToken) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
  if (to.name === 'login' && loginToken) {
    const redirect = typeof to.query.redirect === 'string' ? to.query.redirect : '/mall/'
    return redirect
  }
  return true
})

export default router
