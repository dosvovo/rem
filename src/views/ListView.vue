<script setup>
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { useNotesStore } from '../stores/notes'
import { formatNoteTime, noteTitle, notePreview } from '../utils/note'

const router = useRouter()
const auth = useAuthStore()
const notes = useNotesStore()

onMounted(() => {
  notes.fetchNotes()
})

function openEdit(id) {
  router.push({ name: 'edit', params: { id } })
}

async function logout() {
  await auth.signOut()
  notes.reset()
  router.replace({ name: 'login' })
}
</script>

<template>
  <div class="list-page">
    <header class="topbar">
      <h1>备忘录</h1>
      <button class="icon-btn" title="退出登录" @click="logout">退出</button>
    </header>

    <div class="search">
      <input v-model="notes.searchQuery" type="search" placeholder="搜索标题或正文" />
    </div>

    <p v-if="notes.fromCache" class="banner banner-warn">
      离线中，展示最近缓存（只读）
    </p>
    <p v-else-if="notes.syncError" class="banner banner-warn">
      同步失败：{{ notes.syncError }}
    </p>

    <main v-if="notes.loading && notes.filteredNotes.length === 0" class="center">
      <p>加载中...</p>
    </main>

    <main v-else-if="notes.filteredNotes.length === 0" class="center">
      <template v-if="notes.searchQuery">
        <p class="empty-title">没有匹配的笔记</p>
        <p class="empty-sub">换个关键词试试</p>
      </template>
      <template v-else>
        <p class="empty-title">还没有笔记</p>
        <p class="empty-sub">点击右下角 + 记下第一条</p>
      </template>
    </main>

    <main v-else class="cards">
      <template v-if="notes.pinnedNotes.length">
        <h2 class="group">置顶</h2>
        <article
          v-for="note in notes.pinnedNotes"
          :key="note.id"
          class="card"
          :class="`color-${note.color}`"
          @click="openEdit(note.id)"
        >
          <div class="card-head">
            <h3 class="card-title">{{ noteTitle(note) }}</h3>
            <span class="card-time">{{ formatNoteTime(note.updated_at) }}</span>
          </div>
          <p v-if="notePreview(note)" class="card-body">{{ notePreview(note) }}</p>
        </article>
      </template>

      <template v-if="notes.otherNotes.length">
        <h2 v-if="notes.pinnedNotes.length" class="group">其他</h2>
        <article
          v-for="note in notes.otherNotes"
          :key="note.id"
          class="card"
          :class="`color-${note.color}`"
          @click="openEdit(note.id)"
        >
          <div class="card-head">
            <h3 class="card-title">{{ noteTitle(note) }}</h3>
            <span class="card-time">{{ formatNoteTime(note.updated_at) }}</span>
          </div>
          <p v-if="notePreview(note)" class="card-body">{{ notePreview(note) }}</p>
        </article>
      </template>
    </main>

    <button class="fab" aria-label="新建笔记" @click="router.push({ name: 'new' })">+</button>
  </div>
</template>

<style scoped>
.list-page {
  min-height: 100%;
  padding-bottom: 96px;
}

.topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 18px 18px 10px;
}

h1 {
  font-size: 22px;
}

.icon-btn {
  font-size: 14px;
  color: var(--c-text-soft);
  padding: 6px 10px;
  border-radius: 8px;
}

.icon-btn:active {
  background: var(--c-border);
}

.search {
  padding: 4px 18px 10px;
}

.search input {
  width: 100%;
  padding: 11px 16px;
  border-radius: 12px;
  background: var(--c-card);
  border: 1px solid var(--c-border);
  font-size: 15px;
}

.banner {
  margin: 0 18px 10px;
  padding: 9px 14px;
  border-radius: 10px;
  font-size: 13px;
}

.banner-warn {
  background: #fef3c7;
  color: #92400e;
}

.center {
  padding: 80px 24px;
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

.group {
  font-size: 13px;
  color: var(--c-text-soft);
  font-weight: 600;
  margin: 14px 2px 8px;
}

.card {
  background: var(--c-card);
  border-radius: var(--radius);
  box-shadow: var(--shadow);
  padding: 14px 16px;
  margin-bottom: 10px;
  cursor: pointer;
  transition: transform 0.1s;
}

.card:active {
  transform: scale(0.985);
}

.card-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 10px;
}

.card-title {
  font-size: 15px;
  font-weight: 600;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.card-time {
  font-size: 12px;
  color: var(--c-text-soft);
  flex-shrink: 0;
}

.card-body {
  margin-top: 6px;
  font-size: 13px;
  line-height: 1.6;
  color: var(--c-text-soft);
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
  white-space: pre-line;
}

.color-red { background: #fee2e2; }
.color-orange { background: #ffedd5; }
.color-yellow { background: #fef9c3; }
.color-green { background: #dcfce7; }
.color-blue { background: #dbeafe; }

.fab {
  position: fixed;
  right: 20px;
  bottom: calc(20px + env(safe-area-inset-bottom));
  width: 58px;
  height: 58px;
  border-radius: 50%;
  background: var(--c-primary);
  color: #fff;
  font-size: 30px;
  line-height: 1;
  box-shadow: 0 4px 14px rgba(245, 158, 11, 0.45);
  display: grid;
  place-items: center;
  transition: transform 0.1s;
}

.fab:active {
  transform: scale(0.92);
}
</style>
