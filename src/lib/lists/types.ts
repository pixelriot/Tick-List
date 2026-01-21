export type ShoppingItem = {
	id: string;
	name: string;
	checked: boolean;
	amount: number;
	comment?: string;
	updatedAt?: string;
	deletedAt?: string | null;
};

export type ShoppingList = {
	id: string;
	name: string;
	items: ShoppingItem[];
	sharingId?: string;
	updatedAt?: string;
	deletedAt?: string | null;
};

export function normalizeItem(item: ShoppingItem): ShoppingItem {
	const normalizedName = item.name?.trim() ?? '';
	const normalizedComment = item.comment?.trim();
	const amount = Number.isFinite(item.amount) && item.amount > 0 ? item.amount : 1;

	return {
		id: item.id,
		name: normalizedName,
		checked: Boolean(item.checked),
		amount,
		comment: normalizedComment || undefined,
		updatedAt: item.updatedAt,
		deletedAt: item.deletedAt ?? null
	};
}

export function normalizeList(list: ShoppingList): ShoppingList {
	const normalizedName = list.name?.trim() ?? '';
	const normalizedSharingId = list.sharingId?.trim();
	const items = Array.isArray(list.items) ? list.items.map(normalizeItem) : [];

	return {
		id: list.id,
		name: normalizedName,
		items,
		sharingId: normalizedSharingId || undefined,
		updatedAt: list.updatedAt,
		deletedAt: list.deletedAt ?? null
	};
}

export function createList(name: string): ShoppingList {
	return normalizeList({
		id: crypto.randomUUID(),
		name,
		items: []
	});
}
