import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { supabase } from '../services/supabase'
import { saveNotesCache, loadNotesCache } from '../services/cache'

export const NOTE_COLORS = ['default', 'red', 'orange', 'yellow', 'green', 'blue']

export const useNotesStore = defineStore('notes', () => {
  const notes = ref([])
  const loading = ref(false)
  const syncError = ref('')
  const fromCache = ref(false)
  const searchQuery = ref('')

  const filteredNotes = computed(() => {
    let list = [...notes.value]
    const q = searchQuery.value.trim().toLowerCase()
    if (q) {
      list = list.filter(
        (n) =>
          (n.title ?? '').toLowerCase().includes(q) ||
          n.content.toLowerCase().includes(q)
      )
    }
    return list.sort((a, b) => {
      if (a.pinned !== b.pinned) return a.pinned ? -1 : 1
      return new Date(b.updated_at) - new Date(a.updated_at)
    })
  })

  const pinnedNotes = computed(() => filteredNotes.value.filter((n) => n.pinned))
  const otherNotes = computed(() => filteredNotes.value.filter((n) => !n.pinned))

  async function fetchNotes() {
    loading.value = true
    syncError.value = ''
    try {
      const { data, error } = await supabase
        .from('notes')
        .select('*')
        .order('updated_at', { ascending: false })
      if (error) throw error
      notes.value = data ?? []
      fromCache.value = false
      await saveNotesCache(notes.value)
    } catch (e) {
      const cached = await loadNotesCache()
      if (cached.length > 0) {
        notes.value = cached
        fromCache.value = true
      }
      syncError.value = e.message ?? '加载失败'
    } finally {
      loading.value = false
    }
  }

  async function createNote({ title, content, pinned, color }) {
    const { data: userData, error: userErr } = await supabase.auth.getUser()
    if (userErr || !userData?.user) throw new Error('登录状态已失效，请重新登录')
    const { data, error } = await supabase
      .from('notes')
      .insert({
        user_id: userData.user.id,
        title: title?.trim() || null,
        content,
        pinned: Boolean(pinned),
        color: color ?? 'default',
      })
      .select()
      .single()
    if (error) throw error
    notes.value.push(data)
    await saveNotesCache(notes.value)
    return data
  }

  async function updateNote(id, patch) {
    const { data, error } = await supabase
      .from('notes')
      .update(patch)
      .eq('id', id)
      .select()
      .single()
    if (error) throw error
    const index = notes.value.findIndex((n) => n.id === id)
    if (index !== -1) notes.value[index] = data
    await saveNotesCache(notes.value)
    return data
  }

  async function deleteNote(id) {
    const { error } = await supabase.from('notes').delete().eq('id', id)
    if (error) throw error
    notes.value = notes.value.filter((n) => n.id !== id)
    await saveNotesCache(notes.value)
  }

  function reset() {
    notes.value = []
    searchQuery.value = ''
    syncError.value = ''
    fromCache.value = false
  }

  return {
    notes,
    loading,
    syncError,
    fromCache,
    searchQuery,
    filteredNotes,
    pinnedNotes,
    otherNotes,
    fetchNotes,
    createNote,
    updateNote,
    deleteNote,
    reset,
  }
})
