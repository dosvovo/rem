import { defineStore } from 'pinia'
import { ref } from 'vue'
import { supabase, isSupabaseConfigured } from '../services/supabase'
import { clearNotesCache } from '../services/cache'

export const useAuthStore = defineStore('auth', () => {
  const user = ref(null)
  const initialized = ref(false)
  const configured = isSupabaseConfigured

  async function init() {
    if (!configured) {
      initialized.value = true
      return
    }
    const { data } = await supabase.auth.getSession()
    user.value = data.session?.user ?? null
    supabase.auth.onAuthStateChange((_event, session) => {
      user.value = session?.user ?? null
    })
    initialized.value = true
  }

  async function signUp(email, password) {
    const { error } = await supabase.auth.signUp({ email, password })
    return error
  }

  async function signIn(email, password) {
    const { error } = await supabase.auth.signInWithPassword({ email, password })
    if (!error) {
      const { data } = await supabase.auth.getUser()
      user.value = data.user
    }
    return error
  }

  async function signOut() {
    await supabase.auth.signOut()
    user.value = null
    await clearNotesCache()
  }

  return { user, initialized, configured, init, signUp, signIn, signOut }
})
