drop policy if exists "paired_devices_insert_own" on public.paired_devices;
drop policy if exists "paired_devices_update_own" on public.paired_devices;

create policy "paired_devices_insert_pending_own"
  on public.paired_devices for insert
  to authenticated
  with check (
    (select auth.uid()) = user_id
    and status = 'pending'
    and paired_at is null
    and last_seen_at is null
  );

revoke update on public.paired_devices from authenticated;
grant select, insert, delete on public.paired_devices to authenticated;

alter table public.paired_devices
  add constraint paired_devices_name_length_check
  check (char_length(device_name) between 1 and 64);

alter table public.paired_devices
  add constraint paired_devices_model_length_check
  check (device_model is null or char_length(device_model) <= 128);
