// Supabase Edge Function: 发送到期的预设邮件
// 由 pg_cron 每 5 分钟调用一次（Authorization 为 service_role key）
// 收件人 = 预设创建者的注册邮箱；Resend 免费模式要求该邮箱与 Resend 注册邮箱一致
// 可选 Secret：NOTE_EMAIL_FROM（默认 Memo <onboarding@resend.dev>）

import { createClient } from 'jsr:@supabase/supabase-js@2'
import { Resend } from 'npm:resend@4'

const corsHeaders = {
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Headers': 'authorization, x-client-info, apikey, content-type',
}

const BJ_OFFSET_MS = 8 * 3600 * 1000

function nextSendAt(job, from = new Date()) {
  const [h, m] = job.send_time.split(':').map(Number)
  const bj = new Date(from.getTime() + BJ_OFFSET_MS)
  const y = bj.getUTCFullYear()
  const mo = bj.getUTCMonth()
  const d = bj.getUTCDate()
  const at = (yy, mm, dd, hh, mi) => new Date(Date.UTC(yy, mm, dd, hh - 8, mi))

  if (job.frequency === 'daily') {
    let t = at(y, mo, d, h, m)
    if (t <= from) t = at(y, mo, d + 1, h, m)
    return t.toISOString()
  }
  if (job.frequency === 'weekly') {
    const bjDay = bj.getUTCDay() === 0 ? 7 : bj.getUTCDay()
    const diff = (job.weekday - bjDay + 7) % 7
    let t = at(y, mo, d + diff, h, m)
    if (t <= from) t = new Date(t.getTime() + 7 * 86400000)
    return t.toISOString()
  }
  let t = at(y, mo, job.monthday, h, m)
  if (t <= from) t = at(y, mo + 1, job.monthday, h, m)
  return t.toISOString()
}

Deno.serve(async (req) => {
  if (req.method === 'OPTIONS') {
    return new Response('ok', { headers: corsHeaders })
  }
  try {
    const expected = `Bearer ${Deno.env.get('SUPABASE_SERVICE_ROLE_KEY')}`
    if ((req.headers.get('Authorization') ?? '') !== expected) {
      return new Response('unauthorized', { status: 401, headers: corsHeaders })
    }

    const admin = createClient(
      Deno.env.get('SUPABASE_URL')!,
      Deno.env.get('SUPABASE_SERVICE_ROLE_KEY')!,
    )
    const apiKey = Deno.env.get('RESEND_API_KEY')
    const from = Deno.env.get('NOTE_EMAIL_FROM') ?? 'Memo <onboarding@resend.dev>'
    if (!apiKey) {
      return new Response('email not configured', { status: 500, headers: corsHeaders })
    }

    const { data: dueJobs, error } = await admin
      .from('scheduled_emails')
      .select('*')
      .eq('enabled', true)
      .lte('next_send_at', new Date().toISOString())
    if (error) {
      console.log('查询到期任务失败:', error.message)
      return new Response(error.message, { status: 500, headers: corsHeaders })
    }
    if (!dueJobs?.length) {
      return new Response('no due jobs', { headers: corsHeaders })
    }

    const resend = new Resend(apiKey)
    let sent = 0
    for (const job of dueJobs) {
      const next = nextSendAt(job)
      const { data: claimed, error: claimErr } = await admin
        .from('scheduled_emails')
        .update({ next_send_at: next })
        .eq('id', job.id)
        .eq('next_send_at', job.next_send_at)
        .select()
      if (claimErr || !claimed?.length) continue

      const { data: owner } = await admin.auth.admin.getUserById(job.user_id)
      const to = owner?.user?.email
      if (!to) {
        console.log('任务', job.id, '找不到创建者邮箱，跳过')
        continue
      }
      const { error: sendErr } = await resend.emails.send({
        from,
        to: [to],
        subject: `[定时] ${job.title}`,
        text: `${job.content}\n\n--\n来自个人备忘录 · 定时预设`,
      })
      if (sendErr) {
        console.log('任务', job.id, '发送失败:', JSON.stringify(sendErr))
      } else {
        sent += 1
        console.log('任务', job.id, '已发送至', to)
      }
    }
    return new Response(`sent ${sent}`, { headers: corsHeaders })
  } catch (e) {
    console.log('异常:', String(e))
    return new Response(String(e), { status: 500, headers: corsHeaders })
  }
})
