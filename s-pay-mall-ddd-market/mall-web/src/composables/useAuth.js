import { ref, computed } from 'vue'
import { getCookie, setCookie, removeCookie } from '@/utils/cookie'

const token = ref(getCookie('loginToken'))

export function useAuth() {
  const isLoggedIn = computed(() => !!token.value)

  function syncFromCookie() {
    token.value = getCookie('loginToken')
  }

  function login(userId) {
    setCookie('loginToken', userId, 30)
    token.value = userId
  }

  function logout() {
    removeCookie('loginToken')
    token.value = null
  }

  function requireUserId() {
    syncFromCookie()
    return token.value
  }

  return { token, isLoggedIn, login, logout, syncFromCookie, requireUserId }
}
