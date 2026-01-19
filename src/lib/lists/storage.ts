import { normalizeList, type ShoppingList } from './types';

const STORAGE_PREFIX = 'shoppingList_';

export function loadListsFromStorage(): ShoppingList[] {
	if (typeof localStorage === 'undefined') return [];
	const lists: ShoppingList[] = [];
	for (let i = 0; i < localStorage.length; i++) {
		const key = localStorage.key(i);
		if (key?.startsWith(STORAGE_PREFIX)) {
			const listData = localStorage.getItem(key);
			if (!listData) continue;
			try {
				const parsed = JSON.parse(listData) as ShoppingList;
				lists.push(normalizeList(parsed));
			} catch (error) {
				console.error(`Failed to parse list data for key ${key}:`, error);
			}
		}
	}
	return lists.sort((a, b) => a.name.localeCompare(b.name));
}

export function loadListFromStorage(listId: string): ShoppingList | null {
	if (typeof localStorage === 'undefined') return null;
	const listData = localStorage.getItem(`${STORAGE_PREFIX}${listId}`);
	if (!listData) return null;
	try {
		return normalizeList(JSON.parse(listData) as ShoppingList);
	} catch (error) {
		console.error(`Failed to parse list data for id ${listId}:`, error);
		return null;
	}
}

export function saveListToStorage(list: ShoppingList) {
	if (typeof localStorage === 'undefined') return;
	localStorage.setItem(`${STORAGE_PREFIX}${list.id}`, JSON.stringify(normalizeList(list)));
}

export function deleteListFromStorage(listId: string) {
	if (typeof localStorage === 'undefined') return;
	localStorage.removeItem(`${STORAGE_PREFIX}${listId}`);
}
