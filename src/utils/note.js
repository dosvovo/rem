export function formatNoteTime(iso) {
  const date = new Date(iso)
  const now = new Date()
  const pad = (n) => String(n).padStart(2, '0')
  const sameDay =
    date.getFullYear() === now.getFullYear() &&
    date.getMonth() === now.getMonth() &&
    date.getDate() === now.getDate()
  if (sameDay) return `${pad(date.getHours())}:${pad(date.getMinutes())}`
  if (date.getFullYear() === now.getFullYear()) {
    return `${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
  }
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
}

export function formatFullTime(iso) {
  const date = new Date(iso)
  const now = new Date()
  const pad = (n) => String(n).padStart(2, '0')
  const datePart =
    date.getFullYear() === now.getFullYear()
      ? `${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
      : `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
  return `${datePart} ${pad(date.getHours())}:${pad(date.getMinutes())}`
}

export function noteTitle(note) {
  if (note.title && note.title.trim()) return note.title.trim()
  const firstLine = note.content.split('\n').find((l) => l.trim())
  return firstLine?.trim() || '（无标题）'
}

export function notePreview(note, max = 120) {
  if (note.title && note.title.trim()) return note.content
  return note.content.split('\n').slice(1).join('\n').trim() || note.content
}

export function isOnline() {
  return navigator.onLine
}
