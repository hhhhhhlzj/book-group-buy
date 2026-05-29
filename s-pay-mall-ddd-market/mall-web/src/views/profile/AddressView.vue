<template>
  <div>
    <div class="profile-header">
      <h3>收货地址</h3>
      <button type="button" class="btn-primary" @click="openForm()">新增地址</button>
    </div>
    <p v-if="error" class="lead lead-error">{{ error }}</p>
    <div v-if="!loading && !addresses.length" class="empty-state">暂无地址，请先添加</div>
    <div v-for="addr in addresses" :key="addr.id" class="address-card">
      <div class="address-main">
        <span v-if="addr.isDefault" class="tag-default">默认</span>
        <strong>{{ addr.receiverName }}</strong> {{ addr.receiverPhone }}
        <div class="goods-card-hint">
          {{ addr.province }} {{ addr.city }} {{ addr.district }} {{ addr.detailAddress }}
        </div>
      </div>
      <div class="address-actions">
        <button v-if="!addr.isDefault" type="button" class="btn-secondary" @click="makeDefault(addr.id)">设默认</button>
        <button type="button" class="btn-secondary" @click="openForm(addr)">编辑</button>
        <button type="button" class="btn-secondary" @click="remove(addr.id)">删除</button>
      </div>
    </div>

    <div v-if="showForm" class="modal-overlay" @click.self="showForm = false">
      <div class="modal-card">
        <h3>{{ form.id ? '编辑地址' : '新增地址' }}</h3>
        <label class="form-row"><span>收货人</span><input v-model="form.receiverName" /></label>
        <label class="form-row"><span>手机</span><input v-model="form.receiverPhone" /></label>
        <label class="form-row"><span>省</span><input v-model="form.province" /></label>
        <label class="form-row"><span>市</span><input v-model="form.city" /></label>
        <label class="form-row"><span>区</span><input v-model="form.district" /></label>
        <label class="form-row"><span>详细地址</span><input v-model="form.detailAddress" /></label>
        <label class="form-row checkbox-row">
          <input type="checkbox" v-model="form.isDefault" /> 设为默认地址
        </label>
        <div class="modal-actions">
          <button type="button" class="btn-primary" @click="submitForm">保存</button>
          <button type="button" class="btn-secondary" @click="showForm = false">取消</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useAuth } from '@/composables/useAuth'
import { deleteAddress, listAddresses, saveAddress, setDefaultAddress } from '@/api/userCenter'

const { requireUserId } = useAuth()

const addresses = ref([])
const loading = ref(false)
const error = ref('')
const showForm = ref(false)
const form = ref(emptyForm())

function emptyForm() {
  return {
    id: null,
    receiverName: '',
    receiverPhone: '',
    province: '',
    city: '',
    district: '',
    detailAddress: '',
    isDefault: false,
  }
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    const data = await listAddresses(requireUserId())
    addresses.value = data.list || []
  } catch (e) {
    error.value = e.message || '加载失败'
  } finally {
    loading.value = false
  }
}

function openForm(addr) {
  form.value = addr
    ? {
        id: addr.id,
        receiverName: addr.receiverName,
        receiverPhone: addr.receiverPhone,
        province: addr.province,
        city: addr.city,
        district: addr.district,
        detailAddress: addr.detailAddress,
        isDefault: !!addr.isDefault,
      }
    : emptyForm()
  showForm.value = true
}

async function submitForm() {
  const userId = requireUserId()
  try {
    await saveAddress({ userId, ...form.value })
    showForm.value = false
    await load()
  } catch (e) {
    error.value = e.message || '保存失败'
  }
}

async function remove(addressId) {
  try {
    await deleteAddress(requireUserId(), addressId)
    await load()
  } catch (e) {
    error.value = e.message || '删除失败'
  }
}

async function makeDefault(addressId) {
  try {
    await setDefaultAddress(requireUserId(), addressId)
    await load()
  } catch (e) {
    error.value = e.message || '设置失败'
  }
}

onMounted(load)
</script>
