import { normalizeItem, normalizeList, type ShoppingItem, type ShoppingList } from './types';
import type { Database } from '$lib/supabase/types';
import { isSupabaseConfigured, supabase } from '$lib/supabase/client';

type ListRow = {
	id: string;
	name: string;
	updated_at: string;
	deleted_at: string | null;
};

type ItemRow = {
	id: string;
	list_id: string;
	name: string;
	checked: boolean;
	amount: number;
	comment: string | null;
	updated_at: string;
	deleted_at: string | null;
};

const SHARE_CODE_LENGTH = 8;
const SHARE_CODE_ALPHABET = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789';

type ListInsert = Database['public']['Tables']['lists']['Insert'];
type ListItemInsert = Database['public']['Tables']['list_items']['Insert'];
type ListShareInsert = Database['public']['Tables']['list_shares']['Insert'];

function generateShareCode(): string {
	const bytes = crypto.getRandomValues(new Uint8Array(SHARE_CODE_LENGTH));
	let code = '';
	for (const value of bytes) {
		code += SHARE_CODE_ALPHABET[value % SHARE_CODE_ALPHABET.length];
	}
	return code;
}

function mapListRow(row: ListRow, items: ShoppingItem[]): ShoppingList {
	return normalizeList({
		id: row.id,
		name: row.name,
		items,
		sharingId: undefined,
		updatedAt: row.updated_at,
		deletedAt: row.deleted_at
	});
}

function mapItemRow(row: ItemRow): ShoppingItem {
	return normalizeItem({
		id: row.id,
		name: row.name,
		checked: row.checked,
		amount: row.amount,
		comment: row.comment ?? undefined,
		updatedAt: row.updated_at,
		deletedAt: row.deleted_at
	});
}

export async function shareList(list: ShoppingList): Promise<ShoppingList | null> {
	if (!isSupabaseConfigured || !supabase) {
		console.error('Supabase is not configured.');
		return null;
	}
	if (list.sharingId) {
		return normalizeList(list);
	}

	const normalized = normalizeList(list);
	const listPayload: ListInsert = {
		id: normalized.id,
		name: normalized.name,
		deleted_at: null
	};

	const { error: listError } = await supabase.from('lists').upsert(listPayload);
	if (listError) {
		console.error('Failed to create list:', listError);
		return null;
	}

	if (normalized.items.length > 0) {
		const itemsPayload: ListItemInsert[] = normalized.items.map((item) => ({
			id: item.id,
			list_id: normalized.id,
			name: item.name,
			checked: item.checked,
			amount: item.amount,
			comment: item.comment ?? null,
			deleted_at: item.deletedAt ?? null
		}));
		const { error: itemsError } = await supabase.from('list_items').upsert(itemsPayload);
		if (itemsError) {
			console.error('Failed to create list items:', itemsError);
			return null;
		}
	}

	const shareCode = (await getShareCodeForList(normalized.id)) ?? (await createShareCode(normalized.id));
	if (!shareCode) {
		return null;
	}

	return {
		...normalized,
		sharingId: shareCode
	};
}

async function createShareCode(listId: string): Promise<string | null> {
	if (!supabase) return null;
	for (let attempt = 0; attempt < 5; attempt += 1) {
		const shareCode = generateShareCode();
		const payload: ListShareInsert = {
			id: crypto.randomUUID(),
			list_id: listId,
			share_code: shareCode,
			deleted_at: null
		};
		const { error } = await supabase.from('list_shares').insert(payload);

		if (!error) {
			return shareCode;
		}
		if (error.code !== '23505') {
			console.error('Failed to create share code:', error);
			return null;
		}
	}

	console.error('Failed to create share code: duplicate attempts exceeded');
	return null;
}

async function getShareCodeForList(listId: string): Promise<string | null> {
	if (!supabase) return null;
	const { data, error } = await supabase
		.from('list_shares')
		.select('share_code')
		.eq('list_id', listId)
		.is('deleted_at', null)
		.maybeSingle();

	if (error) {
		console.error('Failed to read share code:', error);
		return null;
	}

	return data?.share_code ?? null;
}

export async function resolveShareCode(shareCode: string): Promise<string | null> {
	if (!supabase) return null;
	const { data, error } = await supabase
		.from('list_shares')
		.select('list_id')
		.eq('share_code', shareCode)
		.is('deleted_at', null)
		.maybeSingle();

	if (error) {
		console.error('Failed to resolve share code:', error);
		return null;
	}

	return data?.list_id ?? null;
}

export async function fetchSharedListById(
	listId: string,
	shareCode?: string
): Promise<ShoppingList | null> {
	if (!supabase) return null;
	const { data: listRow, error: listError } = await supabase
		.from('lists')
		.select('id,name,updated_at,deleted_at')
		.eq('id', listId)
		.is('deleted_at', null)
		.maybeSingle();

	if (listError || !listRow) {
		if (listError) {
			console.error('Failed to fetch list:', listError);
		}
		return null;
	}

	const { data: itemRows, error: itemsError } = await supabase
		.from('list_items')
		.select('id,list_id,name,checked,amount,comment,updated_at,deleted_at')
		.eq('list_id', listId)
		.is('deleted_at', null);

	if (itemsError) {
		console.error('Failed to fetch list items:', itemsError);
		return null;
	}

	const items = (itemRows ?? []).map(mapItemRow);
	const list = mapListRow(listRow, items);
	return {
		...list,
		sharingId: shareCode
	};
}

export async function fetchSharedList(shareCode: string): Promise<ShoppingList | null> {
	if (!supabase) return null;
	const listId = await resolveShareCode(shareCode);
	if (!listId) return null;
	return fetchSharedListById(listId, shareCode);
}

export async function updateSharedListName(listId: string, name: string): Promise<boolean> {
	if (!supabase) return false;
	const { error } = await supabase
		.from('lists')
		.update({ name: name.trim() })
		.eq('id', listId)
		.is('deleted_at', null);

	if (error) {
		console.error('Failed to update list name:', error);
		return false;
	}
	return true;
}

export async function upsertSharedItem(
	listId: string,
	item: ShoppingItem
): Promise<ShoppingItem | null> {
	if (!supabase) return null;
	const normalized = normalizeItem(item);
	const payload: ListItemInsert = {
		id: normalized.id,
		list_id: listId,
		name: normalized.name,
		checked: normalized.checked,
		amount: normalized.amount,
		comment: normalized.comment ?? null,
		deleted_at: normalized.deletedAt ?? null
	};

	const { data, error } = await supabase
		.from('list_items')
		.upsert(payload)
		.select('id,list_id,name,checked,amount,comment,updated_at,deleted_at')
		.single();

	if (error) {
		console.error('Failed to upsert item:', error);
		return null;
	}

	return mapItemRow(data as ItemRow);
}

export async function deleteSharedItem(
	listId: string,
	item: ShoppingItem
): Promise<ShoppingItem | null> {
	return upsertSharedItem(listId, { ...item, deletedAt: new Date().toISOString() });
}

export async function getSupabaseHealth(): Promise<boolean> {
	if (!supabase) return false;
	const { error } = await supabase
		.from('lists')
		.select('id')
		.limit(1)
		.maybeSingle();

	if (error) {
		console.error('Supabase health check failed:', error);
		return false;
	}

	return true;
}
