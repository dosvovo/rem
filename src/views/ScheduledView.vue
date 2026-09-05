<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useScheduleStore } from '../stores/schedule'
import { WEEKDAY_LABELS, describeJob } from '../services/schedule'

const router = useRouter()
const schedule = useScheduleStore()

const showForm = ref(false)
const saving = ref(false)
const toast = ref('')
let toastTimer = null

const title = ref('')
const content = ref('')
const frequency = ref('daily')
const weekday = ref(1)
const monthday = ref(1)
const sendTime = ref('08:00')

function showToast(message) {
  toast.value = message
  clearTimeout(toastTimer)
  toastTimer = setTimeout(() => (toast.value = ''), 2600)
}

onMounted(() => {
  schedule.fetchJobs()
})

onMounted(() => clearTimeout(toastTimer))

const canSave = computed(
  () => title.value.trim() && content.value.trim() && sendTime.value
)

async function save() {
  if (!canSave.value || saving.value) return
  saving.value = true
  try {
    await schedule.createJob({
      title: title.value,
      content: content.value,
      frequency: frequency.value,
      weekday: weekday.value,
      monthday: monthday.value,
      sendTime: sendTime.value,
    })
    title.value = ''
    content.value = ''
    showForm.value = false
    showToast('已创建，到点自动发送')
  } catch (e) {
    showToast(e.message ?? '创建失败，请重试')
  } finally {
    saving.value = false
  }
}

async function toggle(job) {
  try {
    await schedule.toggleJob(job)
  } catch (e) {
    showToast(e.message ?? '操作失败')
  }
}

async function remove(job) {
  if (!window.confirm(`删除预设「${job.title}」？`)) return
  try {
    await schedule.deleteJob(job.id)
  } catch (e) {
    showToast(e.message ?? '删除失败')
  }
}

const weekdayOptions = WEEKDAY_LABELS.map((label, index) => ({
  value: index + 1,
  label,
}))

const monthdayOptions = Array.from({ length: 28 }, (_, i) => ({
  value: i + 1,
  label: `${i + 1} 号`,
}))
</script>

<template>
  <div class="schedule-page">
    <header class="topbar">
      <button class="back" @click="router.back()">‹ 返回</button>
      <h1>定时邮件</h1>
      <button class="add" @click="showForm = !showForm">
        {{ showForm ? '收起' : '新建' }}
      </button>
    </header>

    <p class="note">
      邮件发送到你的注册邮箱。若时刻已过，新预设会在 5 分钟内先发一次作为验证。
    </p>

    <section v-if="showForm" class="form-card">
      <input v-model="title" class="field" placeholder="邮件主题" />
      <textarea
        v-model="content"
        class="field area"
        placeholder="邮件正文（预设内容，到点原样发送）"
      ></textarea>

      <div class="row">
        <span class="label">频率</span>
        <div class="seg">
          <button
            v-for="(label, key) in { daily: '每天', weekly: '每周', monthly: '每月' }"
            :key="key"
            class="seg-item"
            :class="{ active: frequency === key }"
            @click="frequency = key"
          >
            {{ label }}
          </button>
        </div>
      </div>

      <div v-if="frequency === 'weekly'" class="row">
        <span class="label">星期</span>
        <select v-model="weekday" class="field select">
          <option v-for="opt in weekdayOptions" :key="opt.value" :value="opt.value">
            {{ opt.label }}
          </option>
        </select>
      </div>

      <div v-if="frequency === 'monthly'" class="row">
        <span class="label">日期</span>
        <select v-model="monthday" class="field select">
          <option v-for="opt in monthdayOptions" :key="opt.value" :value="opt.value">
            {{ opt.label }}
          </option>
        </select>
      </div>

      <div class="row">
        <span class="label">时刻</span>
        <input v-model="sendTime" type="time" class="field time" />
      </div>

      <button class="btn-primary" :disabled="!canSave || saving" @click="save">
        {{ saving ? '保存中...' : '保存预设' }}
      </button>
    </section>

    <main v-if="schedule.loading && schedule.jobs.length === 0" class="center">
      <p>加载中...</p>
    </main>

    <main v-else-if="schedule.jobs.length === 0" class="center">
      <p class="empty-title">还没有预设</p>
      <p class="empty-sub">点右上角「新建」创建一个定时邮件</p>
    </main>

    <main v-else class="cards">
      <article v-for="job in schedule.jobs" :key="job.id" class="job-card">
        <div class="job-head">
          <h3 class="job-title">{{ job.title }}</h3>
          <button
            class="switch"
            :class="{ on: job.enabled }"
            role="switch"
            :aria-label="job.enabled ? '停用' : '启用'"
            @click="toggle(job)"
          >
            <span class="knob"></span>
          </button>
        </div>
        <p class="job-content">{{ job.content }}</p>
        <div class="job-foot">
          <span class="job-freq">{{ describeJob(job) }}</span>
          <button class="del" @click="remove(job)">删除</button>
        </div>
      </article>
    </main>

    <transition name="fade">
      <div v-if="toast" class="toast">{{ toast }}</div>
    </transition>
  </div>
</template>

<style scoped>
.schedule-page {
  min-height: 100%;
  padding-bottom: 40px;
}

.topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px;
}

.back {
  font-size: 15px;
  color: var(--c-primary-dark);
  padding: 6px 8px;
}

h1 {
  font-size: 16px;
  font-weight: 600;
}

.add {
  font-size: 15px;
  font-weight: 600;
  color: var(--c-primary-dark);
  padding: 6px 10px;
}

.note {
  margin: 0 18px 12px;
  font-size: 12px;
  color: var(--c-text-soft);
  line-height: 1.6;
}

.form-card {
  margin: 0 18px 16px;
  background: var(--c-card);
  border-radius: var(--radius);
  box-shadow: var(--shadow);
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.field {
  width: 100%;
  padding: 11px 14px;
  border-radius: 10px;
  border: 1px solid var(--c-border);
  background: var(--c-bg);
}

.field.area {
  min-height: 96px;
  resize: vertical;
  line-height: 1.6;
}

.field.select,
.field.time {
  width: auto;
  flex: 1;
}

.row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.label {
  font-size: 14px;
  color: var(--c-text-soft);
  width: 36px;
  flex-shrink: 0;
}

.seg {
  display: flex;
  flex: 1;
  border: 1px solid var(--c-border);
  border-radius: 10px;
  overflow: hidden;
}

.seg-item {
  flex: 1;
  padding: 9px 0;
  font-size: 14px;
  color: var(--c-text-soft);
}

.seg-item.active {
  background: var(--c-primary);
  color: #fff;
  font-weight: 600;
}

.center {
  padding: 70px 24px;
  text-align: center;
}

.empty-title {
  font-size: 16px;
  color: var(--c-text-soft);
}

.empty-sub {
  margin-top: 6px;
  font-size: 14px;
  color: #a8a29e;
}

.cards {
  padding: 0 18px;
}

.job-card {
  background: var(--c-card);
  border-radius: var(--radius);
  box-shadow: var(--shadow);
  padding: 14px 16px;
  margin-bottom: 10px;
}

.job-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.job-title {
  font-size: 15px;
  font-weight: 600;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.job-content {
  margin-top: 6px;
  font-size: 13px;
  line-height: 1.6;
  color: var(--c-text-soft);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  white-space: pre-line;
}

.job-foot {
  margin-top: 10px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.job-freq {
  font-size: 13px;
  color: var(--c-primary-dark);
  font-weight: 600;
}

.del {
  font-size: 13px;
  color: var(--c-danger);
  padding: 4px 8px;
  border-radius: 8px;
}

.del:active {
  background: var(--c-border);
}

.switch {
  width: 46px;
  height: 26px;
  border-radius: 13px;
  background: #d6d3d1;
  padding: 2px;
  transition: background 0.15s;
  flex-shrink: 0;
}

.switch.on {
  background: var(--c-primary);
}

.knob {
  display: block;
  width: 22px;
  height: 22px;
  border-radius: 50%;
  background: #fff;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.25);
  transition: transform 0.15s;
}

.switch.on .knob {
  transform: translateX(20px);
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
