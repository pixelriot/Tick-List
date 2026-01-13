<script lang="ts">
	import { onMount } from 'svelte';
	import { flip } from 'svelte/animate';
	import { Menu, NotebookPen } from '@lucide/svelte';
	import { Input } from './components/ui/input';
	import { Button } from './components/ui/button';
	import Spinner from './components/ui/spinner/spinner.svelte';
	import {
		currentList,
		saveListToStorage,
		updateItemOnServer as updateItemOnServerApi,
		deleteItemOnServer as deleteItemOnServerApi,
		type ShoppingItem,
		type ShoppingList
	} from './ListsService';
	import ItemEditModal from './ItemEditModal.svelte';
	import ShoppingListItem from '$lib/ShoppingListItem.svelte';

	let { onBackToLists }: { onBackToLists: () => void } = $props();

	let list = $state<ShoppingList | null>(null);
	let newItemName = $state('');
	let newItemInput = $state<HTMLInputElement | null>(null);
	let isRefreshing = $state(false);
	let activeRequestCount = $state(0);

	// Modal state for editing items
	let editingItem = $state<ShoppingItem | null>(null);

	const unsubscribe = currentList.subscribe((value) => {
		list = value;
	});

	onMount(() => {
		return () => unsubscribe();
	});

	function addItem() {
		if (!list || !newItemName.trim()) return;
		// Check for duplicate items
		if (list.items.some((item) => item.name.toLowerCase() === newItemName.trim().toLowerCase())) {
			alert('An item with this name already exists in the list!');
			return;
		}
		try {
			let newItem = {
				id: crypto.randomUUID(),
				name: newItemName.trim(),
				checked: false,
				amount: 1,
				comment: undefined
			};
			list.items.push(newItem);
			newItemName = '';
			saveListToStorage(list);
			updateItemOnServer(newItem);
			// Keep focus on the input after adding an item
			newItemInput?.blur();
			setTimeout(() => {
				newItemInput?.focus();
			}, 10);
		} catch (error) {
			console.error('Failed to add item:', error);
			alert('Failed to add item. Please try again.');
		}
	}

	function toggleItem(itemId: string) {
		const item = list?.items.find((item) => item.id === itemId);
		if (item) {
			item.checked = !item.checked;
			saveListToStorage(list!);
			updateItemOnServer(item);
		}
	}

	async function deleteItem(itemId: string) {
		if (!list) return;

		const itemToDelete = list.items.find((item) => item.id === itemId);
		if (!itemToDelete) return;

		try {
			// Update local state immediately for better UX
			list.items = list.items.filter((item) => item.id !== itemId);
			saveListToStorage(list);

			// Call server API if list is shared
			await deleteItemOnServer(itemToDelete);
		} catch (error) {
			console.error('Failed to delete item:', error);
			alert('Failed to delete item. Please try again.');
		}
	}

	async function updateItemOnServer(item: ShoppingItem) {
		if (!list?.sharingId) return;

		activeRequestCount++;
		isRefreshing = activeRequestCount > 0;

		try {
			const updatedList = await updateItemOnServerApi(list.sharingId, item);

			// Only update the list if this is the last active request
			activeRequestCount--;
			if (activeRequestCount === 0) {
				if (updatedList) {
					currentList.set(updatedList);
				}
				isRefreshing = false;
			}
		} catch (error) {
			console.error('Failed to update item on server:', error);
			activeRequestCount--;
			if (activeRequestCount === 0) {
				isRefreshing = false;
			}
		}
	}

	async function deleteItemOnServer(item: ShoppingItem) {
		if (!list?.sharingId) return;

		activeRequestCount++;
		isRefreshing = activeRequestCount > 0;

		try {
			const updatedList = await deleteItemOnServerApi(list.sharingId, item);

			// Only update the list if this is the last active request
			activeRequestCount--;
			if (activeRequestCount === 0) {
				if (updatedList) {
					currentList.set(updatedList);
				}
				isRefreshing = false;
			}
		} catch (error) {
			console.error('Failed to delete item on server:', error);
			activeRequestCount--;
			if (activeRequestCount === 0) {
				isRefreshing = false;
			}
			// Optionally show a user-friendly message or implement retry logic
		}
	}

	function openEditModal(item: ShoppingItem) {
		if (!item) return;
		console.log('Opening edit modal for item:', item.name);
		editingItem = { ...item }; // Create a copy to avoid direct mutation
	}

	function saveEditedItem(editedItem: ShoppingItem) {
		if (!editedItem || !list) return;
		// Check for duplicate names (excluding current item)
		if (
			list.items.some(
				(item) =>
					item.id !== editedItem.id && item.name.toLowerCase() === editedItem.name.toLowerCase()
			)
		) {
			alert('An item with this name already exists in the list!');
			return;
		}
		try {
			// Find and replace the item in the list
			const itemIndex = list.items.findIndex((item) => item.id === editedItem.id);
			if (itemIndex !== -1) {
				list.items[itemIndex] = editedItem;
				saveListToStorage(list);
				updateItemOnServer(editedItem);
			}
			closeEditModal();
		} catch (error) {
			console.error('Failed to save edited item:', error);
			alert('Failed to save changes. Please try again.');
		}
	}

	function closeEditModal() {
		editingItem = null;
	}

	// Sort: active items alphabetically, checked items at bottom
	function sortedItems(items: ShoppingItem[]) {
		if (!items || items.length === 0) {
			return { active: [], checked: [] };
		}
		const active = items
			.filter((item) => !item.checked)
			.sort((a, b) => a.name.localeCompare(b.name));
		const checked = items
			.filter((item) => item.checked)
			.sort((a, b) => a.name.localeCompare(b.name));
		return { active, checked };
	}
</script>

<main class="flex h-screen w-full flex-col">
	<!-- Shopping List Header -->
	<header class="flex h-[60px] w-full flex-row items-center gap-2 px-2">
		<Button
			class="text-xl"
			size="icon"
			variant="ghost"
			onclick={onBackToLists}
			aria-label="Back to lists"
		>
			<Menu size={24} />
		</Button>

		{#if list}
			<h1 class="text-xl font-bold">{list.name}</h1>
		{:else}
			<h1 class="text-xl font-bold">No List Selected</h1>
		{/if}

		<div class="flex-1"></div>

		{#if isRefreshing}
			<Spinner />
		{/if}
	</header>

	{#if list}
		{@const sorted = sortedItems(list.items)}
		<div class="flex-1 space-y-6 overflow-auto pb-4">
			<!-- active items  -->
			{#if sorted.active.length > 0}
				<ul class="space-y-3" role="list" aria-label="Active items">
					{#each sorted.active as item (item.id)}
						<li animate:flip={{ duration: 300 }}>
							<ShoppingListItem
								{item}
								onToggle={() => toggleItem(item.id)}
								onDelete={() => deleteItem(item.id)}
								onEdit={() => openEditModal(item)}
							/>
						</li>
					{/each}
				</ul>
			{/if}

			<!-- completed items -->
			{#if sorted.checked.length > 0}
				<div class="flex items-center">
					<div class="flex-1 border-t"></div>
					<span class="mx-4 text-sm font-medium" style="color: var(--muted-foreground);"
						>COMPLETED</span
					>
					<div class="flex-1 border-t"></div>
				</div>

				<ul class="space-y-3" role="list" aria-label="Completed items">
					{#each sorted.checked as item (item.id)}
						<li animate:flip={{ duration: 300 }}>
							<ShoppingListItem
								{item}
								onToggle={() => toggleItem(item.id)}
								onDelete={() => deleteItem(item.id)}
								onEdit={() => openEditModal(item)}
							/>
						</li>
					{/each}
				</ul>
			{/if}

			<!-- empty -->
			{#if sorted.active.length === 0 && sorted.checked.length === 0}
				<div
					class="flex h-32 flex-col items-center justify-center text-center"
					style="color: var(--muted-foreground);"
				>
					<p class="text-lg">Your list is empty</p>
					<p class="mt-1 text-sm">Add some items to get started!</p>
				</div>
			{/if}
		</div>

		<footer class="bg-background flex shrink-0 items-center space-x-2 border-t p-4">
			<Input
				type="text"
				placeholder="Add new item..."
				bind:value={newItemName}
				bind:ref={newItemInput}
				onkeydown={(e) => e.key === 'Enter' && addItem()}
				aria-label="New item name"
				maxlength={100}
				disabled={!list}
			/>
			<Button onclick={addItem} aria-label="Add item">Add</Button>
		</footer>

		<ItemEditModal item={editingItem} onSave={saveEditedItem} onClose={closeEditModal} />
	{:else}
		<div class="m-auto flex h-full w-full flex-col items-center justify-center p-8 text-center">
			<NotebookPen size={48} />
			<p class="mt-2">Select a list or create a new one.</p>
		</div>
	{/if}
</main>

<style>
	main {
		padding-top: env(safe-area-inset-top);
		padding-bottom: env(safe-area-inset-bottom);
		padding-left: env(safe-area-inset-left);
		padding-right: env(safe-area-inset-right);
	}
</style>
