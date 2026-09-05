import { openDB } from 'idb'

const DB_NAME = 'personal-memo'
const DB_VERSION = 1

function getDb() {
  return openDB(DB_NAME, DB_VERSION, {
    upgrade(db) {
      if (!db.objectStoreNames.contains('notes')) {
        db.createObjectStore('notes', { keyPath: 'id' })
      }
      if (!db.objectStoreNames.contains('meta')) {
        db.createObjectStore('meta')
      }
    },
  })
}

function toPlain(value) {
  return JSON.parse(JSON.stringify(value))
}

export async function saveNotesCache(notes) {
  const db = await getDb()
  const tx = db.transaction(['notes', 'meta'], 'readwrite')
  const store = tx.objectStore('notes')
  await store.clear()
  for (const note of toPlain(notes)) {
    await store.put(note)
  }
  await tx.objectStore('meta').put(Date.now(), 'cached_at')
  await tx.done
}

export async function loadNotesCache() {
  const db = await getDb()
  return db.getAll('notes')
}

export async function clearNotesCache() {
  const db = await getDb()
  const tx = db.transaction(['notes', 'meta'], 'readwrite')
  await tx.objectStore('notes').clear()
  await tx.objectStore('meta').clear()
  await tx.done
}
