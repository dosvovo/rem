-- 定时发送功能：数据库定时任务配置
-- 执行前：把下面 <SERVICE_ROLE_KEY> 替换为你的 service_role key
-- 获取位置：Supabase Dashboard -> Project Settings -> API Keys -> service_role
-- 注意：service_role key 是管理级密钥，只在本项目的 SQL 中使用，不要发给他人

create extension if not exists pg_cron;
create extension if not exists pg_net;

select cron.unschedule('send-scheduled-emails')
where exists (select 1 from cron.job where jobname = 'send-scheduled-emails');

select cron.schedule(
  'send-scheduled-emails',
  '*/5 * * * *',
  $$
  select net.http_post(
    url := 'https://lfnxuqcvuvheyhdbzclq.supabase.co/functions/v1/send-scheduled-email',
    headers := jsonb_build_object(
      'Content-Type', 'application/json',
      'Authorization', 'Bearer <SERVICE_ROLE_KEY>'
    ),
    body := jsonb_build_object('source', 'pg_cron')
  );
  $$
);
