import { writable } from 'svelte/store';

// Types
export type ShoppingItem = {
    id: string;
    name: string;
    checked: boolean;
    amount: number;
    comment?: string
};

export type ShoppingList = {
    id: string;
    name: string;
    items: ShoppingItem[];
    sharingId?: string;
};

function getApiUrl(): string {
    if (typeof localStorage !== 'undefined') {
        const savedApiUrl = localStorage.getItem('apiUrl');
        if (savedApiUrl && savedApiUrl.trim()) {
            return savedApiUrl.trim();
        }
    }
    throw new Error('API base URL is not defined.');
}

export const currentList = writable<ShoppingList | null>(null);

export const currentListId = writable<string>(
    typeof localStorage !== 'undefined' ? localStorage.getItem('currentListId') || '' : ''
);

currentListId.subscribe((value) => {
    if (typeof localStorage !== 'undefined') {
        localStorage.setItem('currentListId', value);
    }
});

export function loadListsFromStorage(): ShoppingList[] {
    if (typeof localStorage === 'undefined') return [];
    const lists: ShoppingList[] = [];
    // Scan localStorage for all shopping lists
    for (let i = 0; i < localStorage.length; i++) {
        const key = localStorage.key(i);
        if (key?.startsWith('shoppingList_')) {
            const listData = localStorage.getItem(key);
            if (listData) {
                try {
                    lists.push(JSON.parse(listData));
                } catch (error) {
                    console.error(`Failed to parse list data for key ${key}:`, error);
                }
            }
        }
    }
    // Sort by name for consistent ordering
    return lists.sort((a, b) => a.name.localeCompare(b.name));
}

export function saveListToStorage(list: ShoppingList) {
    if (typeof localStorage === 'undefined') return;
    localStorage.setItem(`shoppingList_${list.id}`, JSON.stringify(list));
}

export function deleteListFromStorage(listId: string) {
    if (typeof localStorage === 'undefined') return;
    localStorage.removeItem(`shoppingList_${listId}`);
}

export async function fetchSharedList(sharingId: string): Promise<ShoppingList | null> {
    try {
        const response = await fetch(
            `${getApiUrl()}/share?sharingId=${sharingId}`
        );
        if (response.ok) {
            const data = await response.json();
            saveListToStorage(data);
            return data;
        } else {
            throw new Error(`HTTP ${response.status}: ${response.statusText}`);
        }
    } catch (error) {
        console.error('Error fetching shared list:', error);
        return null;
    }
}

export async function shareList(list: ShoppingList): Promise<ShoppingList | null> {
    try {
        if (!list.sharingId) {
            list.sharingId = Math.random().toString().slice(2, 10);
        }

        const response = await fetch(`${getApiUrl()}/share`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(list)
        });

        if (response.ok) {
            const data = await response.json();
            saveListToStorage(data);
            return data;
        } else {
            throw new Error(`HTTP ${response.status}: ${response.statusText}`);
        }
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
                name
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

export async function updateItemOnServer(sharingId: string, item: ShoppingItem): Promise<ShoppingList | null> {
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

        const updatedList = await response.json();
        saveListToStorage(updatedList);
        return updatedList;
    } catch (error) {
        console.error('Failed to update item on server:', error);
        return null;
    }
}

export async function deleteItemOnServer(sharingId: string, item: ShoppingItem): Promise<ShoppingList | null> {
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

        const updatedList = await response.json();
        saveListToStorage(updatedList);
        return updatedList;
    } catch (error) {
        console.error('Failed to delete item on server:', error);
        return null;
    }
}