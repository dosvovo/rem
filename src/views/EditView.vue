<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter, onBeforeRouteLeave } from 'vue-router'
import { useNotesStore, NOTE_COLORS } from '../stores/notes'
import { isOnline } from '../utils/note'

const route = useRoute()
const router = useRouter()
const notes = useNotesStore()

const isEdit = computed(() => route.name === 'edit')
const existing = computed(() =>
  isEdit.value ? notes.notes.find((n) => n.id === route.params.id) : null
)

const title = ref('')
const content = ref('')
const pinned = ref(false)
const color = ref('default')
const loaded = ref(false)
const saving = ref(false)
const toast = ref('')
let toastTimer = null

function showToast(message) {
  toast.value = message
  clearTimeout(toastTimer)
  toastTimer = setTimeout(() => (toast.value = ''), 2400)
}

onMounted(() => {
  if (isEdit.value) {
    if (!existing.value) {
      router.replace({ name: 'list' })
      return
    }
    title.value = existing.value.title ?? ''
    content.value = existing.value.content
    pinned.value = existing.value.pinned
    color.value = existing.value.color
  }
  loaded.value = true
})

onUnmounted(() => clearTimeout(toastTimer))

const dirty = computed(() => {
  if (!loaded.value || saving.value) return false
  if (isEdit.value && existing.value) {
    return (
      title.value !== (existing.value.title ?? '') ||
      content.value !== existing.value.content ||
      pinned.value !== existing.value.pinned ||
      color.value !== existing.value.color
    )
  }
  return Boolean(title.value.trim() || content.value.trim())
})

onBeforeRouteLeave(() => {
  if (dirty.value && !window.confirm('有未保存的更改，确定离开？')) {
    return false
  }
})

function goBack() {
  router.back()
}

function validate() {
  if (!isOnline()) {
    showToast('当前离线，恢复网络后保存')
    return false
  }
  if (!title.value.trim() && !content.value.trim()) {
    showToast('先写点内容再保存')
    return false
  }
  return true
}

async function save() {
  if (!validate() || saving.value) return
  saving.value = true
  try {
    if (isEdit.value) {
      await notes.updateNote(existing.value.id, {
        title: title.value.trim() || null,
        content: content.value,
        pinned: pinned.value,
        color: color.value,
      })
    } else {
      await notes.createNote({
        title: title.value,
        content: content.value.trim(),
        pinned: pinned.value,
        color: color.value,
      })
    }
    await router.replace({ name: 'list' })
  } catch (e) {
    showToast(e.message ?? '保存失败，请重试')
  } finally {
    saving.value = false
  }
}

async function remove() {
  if (!window.confirm('确定删除这条笔记？')) return
  saving.value = true
  try {
    await notes.deleteNote(existing.value.id)
    await router.replace({ name: 'list' })
  } catch (e) {
    showToast(e.message ?? '删除失败，请重试')
    saving.value = false
  }
}
</script>

<template>
  <div class="edit-page">
    <header class="topbar">
      <button class="back" @click="goBack">‹ 返回</button>
      <h1>{{ isEdit ? '编辑笔记' : '新建笔记' }}</h1>
      <button class="save" :disabled="saving" @click="save">
        {{ saving ? '...' : '保存' }}
      </button>
    </header>

    <input v-model="title" class="title-input" placeholder="标题（可选）" />

    <textarea
      v-model="content"
      class="content-input"
      placeholder="写点什么..."
      autofocus
    ></textarea>

    <section class="tools">
      <div class="tool-row">
        <span class="tool-label">颜色</span>
        <div class="colors">
          <button
            v-for="c in NOTE_COLORS"
            :key="c"
            class="color-dot"
            :class="[`dot-${c}`, { active: color === c }]"
            :aria-label="`颜色 ${c}`"
            @click="color = c"
          ></button>
        </div>
      </div>
      <div class="tool-row">
        <span class="tool-label">置顶</span>
        <button class="switch" :class="{ on: pinned }" role="switch" @click="pinned = !pinned">
          <span class="knob"></span>
        </button>
      </div>
    </section>

    <button v-if="isEdit" class="delete" @click="remove">删除这条笔记</button>

    <transition name="fade">
      <div v-if="toast" class="toast">{{ toast }}</div>
    </transition>
  </div>
</template>

<style scoped>
.edit-page {
  min-height: 100%;
  display: flex;
  flex-direction: column;
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

.save {
  font-size: 15px;
  font-weight: 600;
  color: var(--c-primary-dark);
  padding: 6px 10px;
}

.save:disabled {
  opacity: 0.5;
}

.title-input {
  padding: 6px 20px;
  font-size: 19px;
  font-weight: 600;
}

.title-input::placeholder {
  color: #d6d3d1;
  font-weight: 400;
}

.content-input {
  flex: 1;
  min-height: 45vh;
  padding: 14px 20px 20px;
  font-size: 15px;
  line-height: 1.7;
  resize: none;
}

.content-input::placeholder {
  color: #d6d3d1;
}

.tools {
  padding: 16px 20px calc(24px + env(safe-area-inset-bottom));
  border-top: 1px solid var(--c-border);
  background: var(--c-card);
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.tool-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.tool-label {
  font-size: 14px;
  color: var(--c-text-soft);
}

.colors {
  display: flex;
  gap: 14px;
}

.color-dot {
  width: 26px;
  height: 26px;
  border-radius: 50%;
  border: 2px solid transparent;
  transition: transform 0.12s, border-color 0.12s;
}

.color-dot.active {
  transform: scale(1.15);
  border-color: var(--c-text);
}

.dot-default { background: #ffffff; box-shadow: inset 0 0 0 1px var(--c-border); }
.dot-red { background: #f87171; }
.dot-orange { background: #fb923c; }
.dot-yellow { background: #facc15; }
.dot-green { background: #4ade80; }
.dot-blue { background: #60a5fa; }

.switch {
  width: 46px;
  height: 26px;
  border-radius: 13px;
  background: #d6d3d1;
  padding: 2px;
  transition: background 0.15s;
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

.delete {
  margin: 0 20px calc(28px + env(safe-area-inset-bottom));
  padding: 12px;
  border-radius: var(--radius);
  color: var(--c-danger);
  border: 1px solid #fecaca;
  font-size: 15px;
  background: var(--c-card);
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
