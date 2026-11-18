create table if not exists public.profiles (
  id uuid primary key references auth.users(id) on delete cascade,
  display_name text,
  avatar_url text,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

alter table public.profiles enable row level security;
grant select, insert, update on public.profiles to authenticated;

create policy "profiles_select_own"
  on public.profiles for select
  to authenticated
  using ((select auth.uid()) = id);

create policy "profiles_insert_own"
  on public.profiles for insert
  to authenticated
  with check ((select auth.uid()) = id);

create policy "profiles_update_own"
  on public.profiles for update
  to authenticated
  using ((select auth.uid()) = id)
  with check ((select auth.uid()) = id);

create table if not exists public.paired_devices (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  device_name text not null,
  device_model text,
  device_type text not null default 'wear_os'
    check (device_type in ('wear_os')),
  status text not null default 'pending'
    check (status in ('pending', 'paired', 'revoked')),
  paired_at timestamptz,
  last_seen_at timestamptz,
  created_at timestamptz not null default now()
);

create index if not exists paired_devices_user_id_idx
  on public.paired_devices(user_id);

alter table public.paired_devices enable row level security;
grant select, insert, update, delete on public.paired_devices to authenticated;

create policy "paired_devices_select_own"
  on public.paired_devices for select
  to authenticated
  using ((select auth.uid()) = user_id);

create policy "paired_devices_insert_own"
  on public.paired_devices for insert
  to authenticated
  with check ((select auth.uid()) = user_id);

create policy "paired_devices_update_own"
  on public.paired_devices for update
  to authenticated
  using ((select auth.uid()) = user_id)
  with check ((select auth.uid()) = user_id);

create policy "paired_devices_delete_own"
  on public.paired_devices for delete
  to authenticated
  using ((select auth.uid()) = user_id);
