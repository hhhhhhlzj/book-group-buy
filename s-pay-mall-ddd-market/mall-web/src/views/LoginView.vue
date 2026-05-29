<template>
  <div class="login-page">
    <div class="login-card">
      <div class="login-title"><span class="seal">书</span>书香拼团</div>
      <p class="login-sub">技术好书共读社区</p>

      <div class="qr-box">
        <img :src="qrSrc" alt="微信二维码" @error="onQrError" />
      </div>
      <p class="lead">请使用<strong>微信</strong>扫描二维码登录</p>
      <p v-if="pollHint" class="lead poll-hint">{{ pollHint }}</p>
      <p v-if="envHint" class="lead env-hint">{{ envHint }}</p>
      <p v-if="error" class="lead lead-error">{{ error }}</p>

      <button type="button" class="btn-secondary" style="margin-top: 20px" @click="stealthLogin">
        无痕登录 · 浏览器指纹
      </button>
    </div>
  </div>
</template>

<script setup>
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import FingerprintJS from '@fingerprintjs/fingerprintjs'
import { useAuth } from '@/composables/useAuth'
import { fetchQrcodeTicket, checkLoginScene, exchangeLoginBind } from '@/api/mall'
import { imageUrl } from '@/api/config'
import { getCookie, isValidLoginToken, removeCookie } from '@/utils/cookie'

const route = useRoute()
const router = useRouter()
const { login } = useAuth()

const qrSrc = ref(imageUrl('/images/placeholder.png'))
const error = ref('')
const pollHint = ref('扫码后约 1～2 秒内自动跳转')
const envHint = import.meta.env.DEV
  ? '本地开发：公众号回调须打到本机 8070（需内网穿透）。否则请用下方「无痕登录」。'
  : ''
let intervalId = null
let sceneStr = ''
let visitorId = ''
let ticket = ''

function redirectAfterLogin() {
  const target = typeof route.query.redirect === 'string' ? route.query.redirect : '/mall/'
  router.replace(target)
}

function applyLogin(userId) {
  if (!isValidLoginToken(userId)) {
    error.value = '登录态异常，请重新扫码或使用无痕登录'
    return false
  }
  login(userId)
  redirectAfterLogin()
  return true
}

function stealthLogin() {
  if (!visitorId) return
  applyLogin(visitorId)
}

function onQrError() {
  qrSrc.value = imageUrl('/images/book-cover.png')
}

async function pollLogin() {
  if (!ticket) return
  try {
    const data = await checkLoginScene(ticket, sceneStr)
    if (data.code === '0000' && data.data) {
      if (!isValidLoginToken(data.data)) {
        pollHint.value = '等待微信确认登录…'
        return
      }
      clearInterval(intervalId)
      pollHint.value = '登录成功，正在进入商城…'
      applyLogin(data.data)
      return
    }
    if (data.code === '0003') {
      pollHint.value = '已扫码，等待确认…'
    }
  } catch (e) {
    console.warn('login poll failed', e)
  }
}

async function tryBindLogin() {
  const bind = typeof route.query.bind === 'string' ? route.query.bind : ''
  if (!bind) return false
  try {
    const openid = await exchangeLoginBind(bind)
    pollHint.value = '已通过扫码链接登录，正在进入商城…'
    return applyLogin(openid)
  } catch (e) {
    error.value = e.message || '登录链接无效'
    return false
  }
}

onMounted(async () => {
  const stale = getCookie('loginToken')
  if (stale && !isValidLoginToken(stale)) {
    removeCookie('loginToken')
  }

  if (await tryBindLogin()) {
    return
  }

  try {
    const fp = await FingerprintJS.load()
    const result = await fp.get()
    visitorId = result.visitorId
    sceneStr = visitorId.toUpperCase()

    ticket = await fetchQrcodeTicket(sceneStr)
    qrSrc.value = `https://mp.weixin.qq.com/cgi-bin/showqrcode?ticket=${encodeURIComponent(ticket)}`
    pollLogin()
    intervalId = setInterval(pollLogin, 1500)
  } catch (e) {
    error.value = '登录初始化失败，请确认 8070 商城服务已启动'
    console.error(e)
  }
})

onBeforeUnmount(() => {
  if (intervalId) clearInterval(intervalId)
})
</script>

<style scoped>
.seal {
  display: inline-block;
  width: 28px;
  height: 28px;
  line-height: 26px;
  text-align: center;
  color: #fff;
  background: var(--vermillion);
  border-radius: 4px;
  margin-right: 8px;
}
</style>
