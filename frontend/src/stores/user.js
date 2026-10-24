import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { login as loginApi, register as registerApi, getMe, logout as logoutApi } from '@/api/auth'

const TOKEN_KEY = 'archive_token'
const PROFILE_KEY = 'archive_profile'

function readProfile() {
  try {
    return JSON.parse(localStorage.getItem(PROFILE_KEY) || 'null')
  } catch {
    return null
  }
}

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem(TOKEN_KEY) || '')
  const profile = ref(readProfile())
  const loaded = ref(false)

  const permissions = computed(() => new Set(profile.value?.permissions || []))
  const isAdmin = computed(() => (profile.value?.roleCodes || []).includes('ADMIN'))
  const displayName = computed(() => profile.value?.realName || profile.value?.username || '用户')

  function setToken(value) {
    token.value = value || ''
    if (value) localStorage.setItem(TOKEN_KEY, value)
    else localStorage.removeItem(TOKEN_KEY)
  }

  function setProfile(value) {
    profile.value = value
    loaded.value = true
    if (value) localStorage.setItem(PROFILE_KEY, JSON.stringify(value))
    else localStorage.removeItem(PROFILE_KEY)
  }

  function hasPerm(code) {
    if (!code) return true
    if (isAdmin.value) return true
    return permissions.value.has(code)
  }

  function hasAny(codes = []) {
    return codes.some((code) => hasPerm(code))
  }

  async function login(credentials) {
    const data = await loginApi(credentials)
    setToken(data.token)
    setProfile(data.user)
    return data
  }

  async function register(payload) {
    const data = await registerApi(payload)
    setToken(data.token)
    setProfile(data.user)
    return data
  }

  async function ensureProfile(force = false) {
    if (!token.value) return null
    if (loaded.value && !force) return profile.value
    const data = await getMe()
    setProfile(data)
    return data
  }

  async function logout() {
    try {
      await logoutApi()
    } catch {
      // ignore
    }
    clear()
  }

  function clear() {
    setToken('')
    setProfile(null)
    loaded.value = false
  }

  return {
    token,
    profile,
    loaded,
    permissions,
    isAdmin,
    displayName,
    hasPerm,
    hasAny,
    login,
    register,
    ensureProfile,
    logout,
    clear,
    setProfile
  }
})
