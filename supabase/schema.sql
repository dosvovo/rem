-- 在 Supabase Dashboard 的 SQL Editor 中执行本脚本

create extension if not exists pgcrypto;

create table if not exists public.notes (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users (id) on delete cascade,
  title text,
  content text not null,
  pinned boolean not null default false,
  edited boolean not null default false,
  color text not null default 'default'
    check (color in ('default', 'red', 'orange', 'yellow', 'green', 'blue')),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create index if not exists notes_user_updated_idx
  on public.notes (user_id, pinned desc, updated_at desc);

-- updated_at 自动维护
create or replace function public.set_updated_at()
returns trigger as $$
begin
  new.updated_at = now();
  return new;
end;
$$ language plpgsql;

drop trigger if exists notes_set_updated_at on public.notes;
create trigger notes_set_updated_at
  before update on public.notes
  for each row execute function public.set_updated_at();

-- 行级安全：用户只能访问自己的笔记
alter table public.notes enable row level security;

drop policy if exists "用户可查看自己的笔记" on public.notes;
create policy "用户可查看自己的笔记"
  on public.notes for select
  using (auth.uid() = user_id);

drop policy if exists "用户可创建自己的笔记" on public.notes;
create policy "用户可创建自己的笔记"
  on public.notes for insert
  with check (auth.uid() = user_id);

drop policy if exists "用户可更新自己的笔记" on public.notes;
create policy "用户可更新自己的笔记"
  on public.notes for update
  using (auth.uid() = user_id)
  with check (auth.uid() = user_id);

drop policy if exists "用户可删除自己的笔记" on public.notes;
create policy "用户可删除自己的笔记"
  on public.notes for delete
  using (auth.uid() = user_id);
