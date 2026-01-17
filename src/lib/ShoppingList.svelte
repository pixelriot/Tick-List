<script lang="ts">
	import { onMount } from 'svelte';
	import { flip } from 'svelte/animate';
	import { Menu, NotebookPen, Plus } from '@lucide/svelte';
	import { Input } from './components/ui/input';
	import { Button } from './components/ui/button';
	import Spinner from './components/ui/spinner/spinner.svelte';
	import { Skeleton } from '$lib/components/ui/skeleton/index.js';
	import * as Empty from '$lib/components/ui/empty/index.js';
	import {
		saveListToStorage,
		fetchSharedList,
		updateItemOnServer as updateItemOnServerApi,
		deleteItemOnServer as deleteItemOnServerApi,
		type ShoppingList,
		type ShoppingItem
	} from './ListsService';
	import ItemEditModal from './ItemEditModal.svelte';
	import ShoppingListItem from '$lib/ShoppingListItem.svelte';
	import { toast } from 'svelte-sonner';

	let {
		onBackToLists,
		currentList
	}: {
		onBackToLists: () => void;
		currentList: ShoppingList | null;
	} = $props();

	let newItemName = $state('');
	let newItemInput = $state<HTMLInputElement | null>(null);
	let activeRequestCount = $state(0);
	let isFetchingList = $state(false);

	// Modal state for editing items
	let editingItem = $state<ShoppingItem | null>(null);

	let isRefreshing = $derived(activeRequestCount > 0);
	let sorted = $derived(currentList ? sortedItems(currentList.items) : { active: [], checked: [] });

	function setCurrentList(nextList: ShoppingList | null) {
		currentList = nextList;
	}

	onMount(async () => {
		if (currentList?.sharingId) {
			isFetchingList = true;
			try {
				const latestList = await fetchSharedList(currentList.sharingId);
				if (latestList) {
					setCurrentList(latestList);
				}
			} catch (error) {
				console.error('Failed to refresh shared list on mount:', error);
			} finally {
				isFetchingList = false;
			}
		}
	});

	function addItem() {
		if (!currentList || !newItemName.trim()) return;
		// Check for duplicate items
		if (
			currentList.items.some((item) => item.name.toLowerCase() === newItemName.trim().toLowerCase())
		) {
			toast.error('An item with this name already exists in the list!');
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
			const updatedList = {
				...currentList,
				items: [...currentList.items, newItem]
			};
			setCurrentList(updatedList);
			saveListToStorage(updatedList);
			updateItemOnServer(newItem);
			newItemName = '';

			// Keep focus on the input after adding an item (avoid blur to prevent keyboard flicker)
			requestAnimationFrame(() => {
				newItemInput?.focus();
			});
		} catch (error) {
			console.error('Failed to add item:', error);
			toast.error('Failed to add item. Please try again.');
		}
	}

	function toggleItem(itemId: string) {
		let toggledItem: ShoppingItem | undefined;
		if (!currentList) return;
		const updatedItems = currentList.items.map((item) => {
			if (item.id === itemId) {
				toggledItem = { ...item, checked: !item.checked };
				return toggledItem;
			}
			return item;
		});
		if (!toggledItem) return;
		const updatedList = { ...currentList, items: updatedItems };
		setCurrentList(updatedList);
		saveListToStorage(updatedList);
		updateItemOnServer(toggledItem);
	}

	async function deleteItem(itemId: string) {
		let itemToDelete: ShoppingItem | undefined;
		if (!currentList) return;
		itemToDelete = currentList.items.find((item) => item.id === itemId);
		if (!itemToDelete) return;
		const updatedList = {
			...currentList,
			items: currentList.items.filter((item) => item.id !== itemId)
		};
		setCurrentList(updatedList);
		if (itemToDelete) {
			saveListToStorage(updatedList);
			// Call server API if list is shared
			await deleteItemOnServer(itemToDelete);
		}
	}

	async function updateItemOnServer(item: ShoppingItem) {
		if (!currentList?.sharingId) return;

		activeRequestCount++;

		try {
			const updatedList = await updateItemOnServerApi(currentList.sharingId, item);

			// Only update the list if this is the last active request
			activeRequestCount--;
			if (activeRequestCount === 0) {
				if (updatedList) {
					setCurrentList(updatedList);
				}
			}
		} catch (error) {
			console.error('Failed to update item on server:', error);
			activeRequestCount--;
		}
	}

	async function deleteItemOnServer(item: ShoppingItem) {
		if (!currentList?.sharingId) return;

		activeRequestCount++;

		try {
			const updatedList = await deleteItemOnServerApi(currentList.sharingId, item);

			// Only update the list if this is the last active request
			activeRequestCount--;
			if (activeRequestCount === 0) {
				if (updatedList) {
					setCurrentList(updatedList);
				}
			}
		} catch (error) {
			console.error('Failed to delete item on server:', error);
			activeRequestCount--;
			// Optionally show a user-friendly message or implement retry logic
		}
	}

	function openEditModal(item: ShoppingItem) {
		if (!item) return;
		console.log('Opening edit modal for item:', item.name);
		editingItem = { ...item }; // Create a copy to avoid direct mutation
	}

	function saveEditedItem(editedItem: ShoppingItem) {
		if (!editedItem || !currentList) return;
		// Check for duplicate names (excluding current item)
		if (
			currentList.items.some(
				(item) =>
					item.id !== editedItem.id && item.name.toLowerCase() === editedItem.name.toLowerCase()
			)
		) {
			alert('An item with this name already exists in the list!');
			return;
		}
		try {
			const updatedItems = currentList.items.map((item) =>
				item.id === editedItem.id ? editedItem : item
			);
			const updatedList = { ...currentList, items: updatedItems };
			setCurrentList(updatedList);
			saveListToStorage(updatedList);
			updateItemOnServer(editedItem);
			closeEditModal();
		} catch (error) {
			console.error('Failed to save edited item:', error);
			toast.error('Failed to save changes. Please try again.');
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

		{#if currentList}
			<h1 class="text-xl font-bold">{currentList.name}</h1>
		{:else}
			<h1 class="text-xl font-bold">No List Selected</h1>
		{/if}

		<div class="flex-1"></div>

		{#if isRefreshing}
			<Spinner />
		{/if}
	</header>

	{#if isFetchingList}
		<div class="flex-1 space-y-6 overflow-auto pb-4">
			{#each Array(5) as _}
				<Skeleton class="m-3 flex h-14 " />
			{/each}
		</div>
	{:else if currentList}
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
				<Empty.Root class="h-full">
					<Empty.Header>
						<Empty.Title>Your list is empty</Empty.Title>
						<Empty.Description>Add some items to get started!</Empty.Description>
					</Empty.Header>
				</Empty.Root>
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
				disabled={!currentList}
			/>
			<Button
				class="[--radius:9999rem]"
				variant="outline"
				size="icon"
				onclick={addItem}
				tabindex={-1}
				aria-label="Add item"
			>
				<Plus />
			</Button>
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
