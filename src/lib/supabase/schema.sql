create table if not exists lists (
	id uuid primary key,
	name text not null,
	created_at timestamptz not null default now(),
	updated_at timestamptz not null default now(),
	deleted_at timestamptz
);

create table if not exists list_items (
	id uuid primary key,
	list_id uuid not null references lists(id) on delete cascade,
	name text not null,
	checked boolean not null default false,
	amount integer not null default 1,
	comment text,
	created_at timestamptz not null default now(),
	updated_at timestamptz not null default now(),
	deleted_at timestamptz
);

create table if not exists list_shares (
	id uuid primary key,
	list_id uuid not null references lists(id) on delete cascade,
	share_code text not null unique,
	created_at timestamptz not null default now(),
	updated_at timestamptz not null default now(),
	deleted_at timestamptz
);

alter table lists enable row level security;
alter table list_items enable row level security;
alter table list_shares enable row level security;

drop policy if exists lists_select_shared on lists;
create policy lists_select_shared
	on lists for select
	using (
		exists (
			select 1
			from list_shares
			where list_shares.list_id = lists.id
				and list_shares.deleted_at is null
		)
	);

drop policy if exists lists_insert_open on lists;
create policy lists_insert_open
	on lists for insert
	with check (
		id is not null
		and length(btrim(name)) > 0
		and deleted_at is null
	);

drop policy if exists lists_update_shared on lists;
create policy lists_update_shared
	on lists for update
	using (
		exists (
			select 1
			from list_shares
			where list_shares.list_id = lists.id
				and list_shares.deleted_at is null
		)
	)
	with check (
		exists (
			select 1
			from list_shares
			where list_shares.list_id = lists.id
				and list_shares.deleted_at is null
		)
	);

drop policy if exists list_items_select_shared on list_items;
create policy list_items_select_shared
	on list_items for select
	using (
		exists (
			select 1
			from list_shares
			where list_shares.list_id = list_items.list_id
				and list_shares.deleted_at is null
		)
	);

drop policy if exists list_items_insert_shared on list_items;
create policy list_items_insert_shared
	on list_items for insert
	with check (
		exists (
			select 1
			from list_shares
			where list_shares.list_id = list_items.list_id
				and list_shares.deleted_at is null
		)
	);

drop policy if exists list_items_update_shared on list_items;
create policy list_items_update_shared
	on list_items for update
	using (
		exists (
			select 1
			from list_shares
			where list_shares.list_id = list_items.list_id
				and list_shares.deleted_at is null
		)
	)
	with check (
		exists (
			select 1
			from list_shares
			where list_shares.list_id = list_items.list_id
				and list_shares.deleted_at is null
		)
	);

drop policy if exists list_shares_select_open on list_shares;
create policy list_shares_select_open
	on list_shares for select
	using (true);

drop policy if exists list_shares_insert_open on list_shares;
create policy list_shares_insert_open
	on list_shares for insert
	with check (
		id is not null
		and deleted_at is null
		and share_code ~ '^TL[0-9]{6}$'
		and exists (
			select 1
			from lists
			where lists.id = list_shares.list_id
				and lists.deleted_at is null
		)
	);

drop policy if exists list_shares_update_shared on list_shares;
create policy list_shares_update_shared
	on list_shares for update
	using (
		exists (
			select 1
			from list_shares as ls
			where ls.list_id = list_shares.list_id
				and ls.deleted_at is null
		)
	)
	with check (
		exists (
			select 1
			from list_shares as ls
			where ls.list_id = list_shares.list_id
				and ls.deleted_at is null
		)
	);

create index if not exists list_items_list_updated_idx
	on list_items (list_id, updated_at);

create index if not exists lists_updated_idx
	on lists (updated_at);

create index if not exists list_shares_code_idx
	on list_shares (share_code);

create or replace function touch_updated_at()
returns trigger as $$
begin
	new.updated_at = now();
	return new;
end;
$$ language plpgsql
set search_path = pg_catalog;

drop trigger if exists touch_lists on lists;
create trigger touch_lists before update on lists
for each row execute function touch_updated_at();

drop trigger if exists touch_list_items on list_items;
create trigger touch_list_items before update on list_items
for each row execute function touch_updated_at();

drop trigger if exists touch_list_shares on list_shares;
create trigger touch_list_shares before update on list_shares
for each row execute function touch_updated_at();
