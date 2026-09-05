import { defineStore } from 'pinia'
import { ref } from 'vue'
import { supabase } from '../services/supabase'
import { initialNextSendAt, futureNextSendAt } from '../services/schedule'

export const useScheduleStore = defineStore('schedule', () => {
  const jobs = ref([])
  const loading = ref(false)

  async function fetchJobs() {
    loading.value = true
    try {
      const { data, error } = await supabase
        .from('scheduled_emails')
        .select('*')
        .order('created_at', { ascending: true })
      if (error) throw error
      jobs.value = data ?? []
    } finally {
      loading.value = false
    }
  }

  async function createJob({ title, content, frequency, weekday, monthday, sendTime }) {
    const { data: userData } = await supabase.auth.getUser()
    if (!userData?.user) throw new Error('登录状态已失效，请重新登录')
    const { data, error } = await supabase
      .from('scheduled_emails')
      .insert({
        user_id: userData.user.id,
        title: title.trim(),
        content: content.trim(),
        frequency,
        weekday: frequency === 'weekly' ? weekday : null,
        monthday: frequency === 'monthly' ? monthday : null,
        send_time: sendTime,
        enabled: true,
        next_send_at: initialNextSendAt({ frequency, weekday, monthday, sendTime }),
      })
      .select()
      .single()
    if (error) throw error
    jobs.value.push(data)
    return data
  }

  async function toggleJob(job) {
    const enabled = !job.enabled
    const patch = { enabled }
    if (enabled) {
      patch.next_send_at = futureNextSendAt(job)
    }
    const { data, error } = await supabase
      .from('scheduled_emails')
      .update(patch)
      .eq('id', job.id)
      .select()
      .single()
    if (error) throw error
    const index = jobs.value.findIndex((j) => j.id === job.id)
    if (index !== -1) jobs.value[index] = data
    return data
  }

  async function deleteJob(id) {
    const { error } = await supabase.from('scheduled_emails').delete().eq('id', id)
    if (error) throw error
    jobs.value = jobs.value.filter((j) => j.id !== id)
  }

  return { jobs, loading, fetchJobs, createJob, toggleJob, deleteJob }
})
