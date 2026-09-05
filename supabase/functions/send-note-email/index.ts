// Supabase Edge Function: 保存笔记时发送邮件副本
// 部署方式（二选一）：
//   1. Dashboard -> Edge Functions -> send-note-email，粘贴本文件全部代码
//   2. CLI: supabase functions deploy send-note-email
// 需要的项目 Secrets（Dashboard -> Edge Functions -> Secrets）：
//   RESEND_API_KEY  Resend 平台的 re_ 开头 API Key（https://resend.com，免费 100 封/天）
//   NOTE_EMAIL_TO   收件邮箱（免费模式下必须与 Resend 注册邮箱一致）
// 注意：必须保留 CORS 处理，否则浏览器跨域预检失败，请求无法到达函数

import { createClient } from 'jsr:@supabase/supabase-js@2'
import { Resend } from 'npm:resend@4'

const corsHeaders = {
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Headers':
    'authorization, x-client-info, apikey, content-type',
}

Deno.serve(async (req) => {
  if (req.method === 'OPTIONS') {
    return new Response('ok', { headers: corsHeaders })
  }

  try {
    const authHeader = req.headers.get('Authorization') ?? ''
    const supabase = createClient(
      Deno.env.get('SUPABASE_URL')!,
      Deno.env.get('SUPABASE_ANON_KEY')!,
    )
    const token = authHeader.replace('Bearer ', '')
    const { data: userData } = await supabase.auth.getUser(token)
    if (!userData?.user) {
      return new Response('unauthorized', { status: 401, headers: corsHeaders })
    }

    const apiKey = Deno.env.get('RESEND_API_KEY')
    const to = Deno.env.get('NOTE_EMAIL_TO')
    if (!apiKey || !to) {
      return new Response('email not configured', { status: 500, headers: corsHeaders })
    }

    const { title, content, created_at } = await req.json()
    const resend = new Resend(apiKey)
    const firstLine = (content ?? '').split('\n')[0]?.slice(0, 30) || '新笔记'
    const subject = title?.trim() || firstLine

    const { error } = await resend.emails.send({
      from: 'Memo <onboarding@resend.dev>',
      to: [to],
      subject: `[备忘录] ${subject}`,
      text: `${content}\n\n--\n记于 ${created_at}`,
    })
    if (error) {
      return new Response(error.message, { status: 500, headers: corsHeaders })
    }
    return new Response('ok', { headers: corsHeaders })
  } catch (e) {
    return new Response(String(e), { status: 500, headers: corsHeaders })
  }
})
