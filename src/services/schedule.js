const BJ_OFFSET_MS = 8 * 3600 * 1000

export const WEEKDAY_LABELS = ['周一', '周二', '周三', '周四', '周五', '周六', '周日']

export const FREQUENCY_LABELS = {
  daily: '每天',
  weekly: '每周',
  monthly: '每月',
}

function atBjTime(y, mo, d, h, m) {
  return new Date(Date.UTC(y, mo, d, h - 8, m))
}

function bjParts(from) {
  const bj = new Date(from.getTime() + BJ_OFFSET_MS)
  return {
    y: bj.getUTCFullYear(),
    mo: bj.getUTCMonth(),
    d: bj.getUTCDate(),
    day: bj.getUTCDay() === 0 ? 7 : bj.getUTCDay(),
  }
}

function parseTime(sendTime) {
  const [h, m] = sendTime.split(':').map(Number)
  return { h, m }
}

// 新建预设时使用：取最近的目标时点，已过去也保留，便于立即触发一次验证链路
export function initialNextSendAt({ frequency, weekday, monthday, sendTime }) {
  const now = new Date()
  const { h, m } = parseTime(sendTime)
  const { y, mo, d, day } = bjParts(now)
  if (frequency === 'daily') {
    return atBjTime(y, mo, d, h, m).toISOString()
  }
  if (frequency === 'weekly') {
    const diff = (weekday - day + 7) % 7
    return atBjTime(y, mo, d + diff, h, m).toISOString()
  }
  return atBjTime(y, mo, monthday, h, m).toISOString()
}

// 启用/发送后推进使用：严格取未来时点
export function futureNextSendAt(job, from = new Date()) {
  const { h, m } = parseTime(job.send_time)
  const { y, mo, d, day } = bjParts(from)
  if (job.frequency === 'daily') {
    let t = atBjTime(y, mo, d, h, m)
    if (t <= from) t = atBjTime(y, mo, d + 1, h, m)
    return t.toISOString()
  }
  if (job.frequency === 'weekly') {
    const diff = (job.weekday - day + 7) % 7
    let t = atBjTime(y, mo, d + diff, h, m)
    if (t <= from) t = new Date(t.getTime() + 7 * 86400000)
    return t.toISOString()
  }
  let t = atBjTime(y, mo, job.monthday, h, m)
  if (t <= from) t = atBjTime(y, mo + 1, job.monthday, h, m)
  return t.toISOString()
}

export function describeJob(job) {
  const time = job.send_time
  if (job.frequency === 'daily') return `每天 ${time}`
  if (job.frequency === 'weekly') {
    return `每周${WEEKDAY_LABELS[job.weekday - 1]} ${time}`
  }
  return `每月 ${job.monthday} 号 ${time}`
}
