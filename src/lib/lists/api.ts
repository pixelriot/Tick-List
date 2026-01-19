import { normalizeList, type ShoppingItem, type ShoppingList } from './types';

const API_URL_KEY = 'apiUrl';

function getApiUrl(): string {
	if (typeof localStorage === 'undefined') {
		throw new Error('API base URL is not defined.');
	}
	const savedApiUrl = localStorage.getItem(API_URL_KEY);
	if (savedApiUrl && savedApiUrl.trim()) {
		return savedApiUrl.trim();
	}
	throw new Error('API base URL is not defined.');
}

export function isValidApiUrl(url: string): boolean {
	if (!url.trim()) return false;
	try {
		const parsed = new URL(url);
		return parsed.protocol === 'http:' || parsed.protocol === 'https:';
	} catch {
		return false;
	}
}

export async function fetchSharedList(sharingId: string): Promise<ShoppingList | null> {
	try {
		const response = await fetch(`${getApiUrl()}/share?sharingId=${sharingId}`);
		if (!response.ok) {
			throw new Error(`HTTP ${response.status}: ${response.statusText}`);
		}
		const data = (await response.json()) as ShoppingList;
		return normalizeList(data);
	} catch (error) {
		console.error('Error fetching shared list:', error);
		return null;
	}
}

export async function shareList(list: ShoppingList): Promise<ShoppingList | null> {
	try {
		const response = await fetch(`${getApiUrl()}/share`, {
			method: 'POST',
			headers: {
				'Content-Type': 'application/json'
			},
			body: JSON.stringify(normalizeList(list))
		});

		if (!response.ok) {
			throw new Error(`HTTP ${response.status}: ${response.statusText}`);
		}

		const data = (await response.json()) as ShoppingList;
		return normalizeList(data);
	} catch (error) {
		console.error('Error sharing list:', error);
		return null;
	}
}

export async function updateSharedListName(sharingId: string, name: string): Promise<boolean> {
	try {
		const response = await fetch(`${getApiUrl()}/share`, {
			method: 'PATCH',
			headers: {
				'Content-Type': 'application/json'
			},
			body: JSON.stringify({
				sharingId,
				name: name.trim()
			})
		});
		if (!response.ok) {
			throw new Error(`HTTP ${response.status}: ${response.statusText}`);
		}
		return true;
	} catch (error) {
		console.error('Error updating shared list name:', error);
		return false;
	}
}

export async function updateItemOnServer(
	sharingId: string,
	item: ShoppingItem
): Promise<ShoppingList | null> {
	try {
		const response = await fetch(`${getApiUrl()}/item`, {
			method: 'POST',
			headers: {
				'Content-Type': 'application/json'
			},
			body: JSON.stringify({
				sharingId,
				updateItem: item
			})
		});

		if (!response.ok) {
			throw new Error(`HTTP ${response.status}: ${response.statusText}`);
		}

		const updatedList = (await response.json()) as ShoppingList;
		return normalizeList(updatedList);
	} catch (error) {
		console.error('Failed to update item on server:', error);
		return null;
	}
}

export async function deleteItemOnServer(
	sharingId: string,
	item: ShoppingItem
): Promise<ShoppingList | null> {
	try {
		const response = await fetch(`${getApiUrl()}/item`, {
			method: 'DELETE',
			headers: {
				'Content-Type': 'application/json'
			},
			body: JSON.stringify({
				sharingId,
				updateItem: item
			})
		});

		if (!response.ok) {
			throw new Error(`HTTP ${response.status}: ${response.statusText}`);
		}

		const updatedList = (await response.json()) as ShoppingList;
		return normalizeList(updatedList);
	} catch (error) {
		console.error('Failed to delete item on server:', error);
		return null;
	}
}
