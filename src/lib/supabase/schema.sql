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
$$ language plpgsql;

drop trigger if exists touch_lists on lists;
create trigger touch_lists before update on lists
for each row execute function touch_updated_at();

drop trigger if exists touch_list_items on list_items;
create trigger touch_list_items before update on list_items
for each row execute function touch_updated_at();

drop trigger if exists touch_list_shares on list_shares;
create trigger touch_list_shares before update on list_shares
for each row execute function touch_updated_at();
