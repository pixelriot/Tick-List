<script lang="ts">
	import { onDestroy, onMount } from 'svelte';
	import { flip } from 'svelte/animate';
	import { Menu, NotebookPen, Plus, WifiOff, RefreshCw } from '@lucide/svelte';
	import { Input } from './components/ui/input';
	import { Button } from './components/ui/button';
	import Spinner from './components/ui/spinner/spinner.svelte';
	import { Skeleton } from '$lib/components/ui/skeleton/index.js';
	import * as Empty from '$lib/components/ui/empty/index.js';
	import { toast } from 'svelte-sonner';
	import { isSupabaseConfigured, supabase } from '$lib/supabase/client';
	import type { RealtimeChannel } from '@supabase/supabase-js';
	import {
		deleteSharedItem,
		fetchSharedList,
		getSupabaseHealth,
		upsertSharedItem
	} from '$lib/lists/supabase';
	import { saveListToStorage } from '$lib/lists/storage';
	import { normalizeItem, type ShoppingList, type ShoppingItem } from '$lib/lists/types';
	import ItemEditModal from './ItemEditModal.svelte';
	import ShoppingListItem from '$lib/ShoppingListItem.svelte';

	let {
		onBackToLists,
		currentList
	}: {
		onBackToLists: () => void;
		currentList: ShoppingList | null;
	} = $props();

	let newItemName = $state('');
	let newItemInput = $state<HTMLInputElement | null>(null);

	let isFetchingList = $state(false);
	let activeRequestCount = $state(0);
	let pendingItemUpdateTimer = $state<ReturnType<typeof setTimeout> | null>(null);
	let pendingItemUpdates = $state<Record<string, ShoppingItem>>({});
	let activeSubscription = $state<RealtimeChannel | null>(null);
	let hasSyncError = $state(false);

	let editingItem = $state<ShoppingItem | null>(null);

	let isRefreshing = $derived(activeRequestCount > 0);
	let sortedList = $derived.by(() => {
		if (!currentList?.items?.length) return { active: [], checked: [] };
		const active = currentList.items
			.filter((i) => !i.checked)
			.sort((a, b) => a.name.localeCompare(b.name));
		const checked = currentList.items
			.filter((i) => i.checked)
			.sort((a, b) => a.name.localeCompare(b.name));
		return { active, checked };
	});

	onMount(async () => {
		if (!currentList?.sharingId) return;
		if (!isSupabaseConfigured || !supabase) {
			hasSyncError = true;
			return;
		}
		await refreshSharedList();
		await startRealtime();
	});

	onDestroy(() => {
		activeSubscription?.unsubscribe();
	});

	function applyLocalListUpdate(nextList: ShoppingList) {
		currentList = nextList;
		saveListToStorage(nextList);
	}

	function mergeIncomingItem(item: ShoppingItem) {
		if (!currentList) return;
		const existing = currentList.items.find((entry) => entry.id === item.id);

		if (existing?.updatedAt && item.updatedAt) {
			if (new Date(item.updatedAt).getTime() <= new Date(existing.updatedAt).getTime()) return;
		}

		if (item.deletedAt) {
			applyLocalListUpdate({
				...currentList,
				items: currentList.items.filter((e) => e.id !== item.id)
			});
		} else if (!existing) {
			applyLocalListUpdate({ ...currentList, items: [...currentList.items, item] });
		} else {
			applyLocalListUpdate({
				...currentList,
				items: currentList.items.map((e) => (e.id === item.id ? item : e))
			});
		}
	}

	function queueItemUpdate(item: ShoppingItem) {
		if (!currentList?.sharingId) return;
		pendingItemUpdates = { ...pendingItemUpdates, [item.id]: item };
		if (pendingItemUpdateTimer) clearTimeout(pendingItemUpdateTimer);
		pendingItemUpdateTimer = setTimeout(async () => {
			const updates = Object.values(pendingItemUpdates);
			if (!currentList?.sharingId || updates.length === 0) return;
			pendingItemUpdates = {};
			pendingItemUpdateTimer = null;

			activeRequestCount++;
			try {
				const updatedItem = await upsertSharedItem(currentList.id, updates[updates.length - 1]);
				if (updatedItem) mergeIncomingItem(updatedItem);
				hasSyncError = false;
			} catch (error) {
				console.error('Failed to update item on server:', error);
				hasSyncError = true;
			} finally {
				activeRequestCount = Math.max(activeRequestCount - 1, 0);
			}
		}, 300);
	}

	async function refreshSharedList() {
		if (!currentList?.sharingId) return;
		isFetchingList = true;
		try {
			const latestList = await fetchSharedList(currentList.sharingId);
			if (latestList) {
				currentList = latestList;
				saveListToStorage(latestList);
				hasSyncError = false;
			}
		} catch (error) {
			console.error('Failed to refresh shared list:', error);
			hasSyncError = true;
		} finally {
			isFetchingList = false;
		}
	}

	async function startRealtime() {
		if (!currentList?.sharingId || !supabase) return;
		const listId = currentList.id;
		activeSubscription?.unsubscribe();
		const channel = supabase
			.channel(`list-sync-${listId}`)
			.on(
				'postgres_changes',
				{ event: '*', schema: 'public', table: 'list_items', filter: `list_id=eq.${listId}` },
				(payload) => {
					const r = (payload.new ?? payload.old) as {
						id: string;
						name: string;
						checked: boolean;
						amount: number;
						comment: string | null;
						updated_at: string;
						deleted_at: string | null;
					};
					mergeIncomingItem({
						id: r.id,
						name: r.name,
						checked: r.checked,
						amount: r.amount,
						comment: r.comment ?? undefined,
						updatedAt: r.updated_at,
						deletedAt: r.deleted_at
					});
				}
			);

		channel.subscribe((status) => {
			hasSyncError = status === 'CHANNEL_ERROR';
		});

		activeSubscription = channel;
	}

	function addItem() {
		if (!currentList || !newItemName.trim()) return;
		const trimmedName = newItemName.trim();
		if (currentList.items.some((item) => item.name.toLowerCase() === trimmedName.toLowerCase())) {
			toast.error('An item with this name already exists in the list!');
			return;
		}
		const newItem = normalizeItem({
			id: crypto.randomUUID(),
			name: trimmedName,
			checked: false,
			amount: 1
		});
		applyLocalListUpdate({ ...currentList, items: [...currentList.items, newItem] });
		queueItemUpdate(newItem);
		newItemName = '';
		requestAnimationFrame(() => newItemInput?.focus());
	}

	function toggleItem(itemId: string) {
		if (!currentList) return;
		let toggledItem: ShoppingItem | undefined;
		const updatedItems = currentList.items.map((item) => {
			if (item.id === itemId) {
				toggledItem = normalizeItem({ ...item, checked: !item.checked });
				return toggledItem;
			}
			return item;
		});
		if (!toggledItem) return;
		applyLocalListUpdate({ ...currentList, items: updatedItems });
		queueItemUpdate(toggledItem);
	}

	async function deleteItem(itemId: string) {
		if (!currentList) return;
		const itemToDelete = currentList.items.find((item) => item.id === itemId);
		if (!itemToDelete) return;
		applyLocalListUpdate({
			...currentList,
			items: currentList.items.filter((item) => item.id !== itemId)
		});

		if (currentList?.sharingId) {
			activeRequestCount++;
			try {
				const deletedItem = await deleteSharedItem(currentList.id, itemToDelete);
				if (activeRequestCount === 1 && deletedItem) mergeIncomingItem(deletedItem);
				hasSyncError = false;
			} catch (error) {
				console.error('Failed to delete item on server:', error);
				toast.error('Failed to delete item on server. Please try again.');
				hasSyncError = true;
			} finally {
				activeRequestCount = Math.max(activeRequestCount - 1, 0);
			}
		}
	}

	function saveEditedItem(editedItem: ShoppingItem) {
		if (!editedItem || !currentList) return;
		if (
			currentList.items.some(
				(i) => i.id !== editedItem.id && i.name.toLowerCase() === editedItem.name.toLowerCase()
			)
		) {
			toast.error('An item with this name already exists in the list!');
			return;
		}
		const normalizedItem = normalizeItem(editedItem);
		applyLocalListUpdate({
			...currentList,
			items: currentList.items.map((i) => (i.id === normalizedItem.id ? normalizedItem : i))
		});
		queueItemUpdate(normalizedItem);
		editingItem = null;
	}

	async function refreshConnectivity() {
		const healthy = await getSupabaseHealth();
		if (healthy && currentList?.sharingId) {
			await refreshSharedList();
			await startRealtime();
		} else {
			hasSyncError = true;
		}
	}
</script>

<main class="flex h-dvh w-full flex-col overflow-hidden">
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
		{:else if currentList?.sharingId && hasSyncError}
			<Button variant="outline" size="icon" onclick={refreshConnectivity}>
				<RefreshCw />
			</Button>
		{/if}
	</header>

	{#if isFetchingList}
		<div class="flex-1 space-y-6 overflow-auto pb-4">
			{#each Array(10) as _}
				<Skeleton class="m-3 flex h-14 " />
			{/each}
		</div>
	{:else if currentList}
		<div class="flex-1 space-y-6 overflow-auto pb-4">
			<!-- active items  -->
			{#if sortedList.active.length > 0}
				<ul class="space-y-3" role="list" aria-label="Active items">
					{#each sortedList.active as item (item.id)}
						<li animate:flip={{ duration: 300 }}>
							<ShoppingListItem
								{item}
								onToggle={() => toggleItem(item.id)}
								onEdit={() => (editingItem = { ...item })}
							/>
						</li>
					{/each}
				</ul>
			{/if}

			<!-- completed items -->
			{#if sortedList.checked.length > 0}
				<div class="flex items-center">
					<div class="flex-1 border-t"></div>
					<span class="mx-4 text-sm font-medium" style="color: var(--muted-foreground);"
						>COMPLETED</span
					>
					<div class="flex-1 border-t"></div>
				</div>

				<ul class="space-y-3" role="list" aria-label="Completed items">
					{#each sortedList.checked as item (item.id)}
						<li animate:flip={{ duration: 300 }}>
							<ShoppingListItem
								{item}
								onToggle={() => toggleItem(item.id)}
								onEdit={() => (editingItem = { ...item })}
							/>
						</li>
					{/each}
				</ul>
			{/if}

			<!-- empty -->
			{#if sortedList.active.length === 0 && sortedList.checked.length === 0}
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
				disabled={!newItemName.trim()}
				aria-label="Add item"
			>
				<Plus />
			</Button>
		</footer>

		<ItemEditModal
			item={editingItem}
			onSave={saveEditedItem}
			onClose={() => (editingItem = null)}
			onDelete={(item) => {
				deleteItem(item.id);
				editingItem = null;
			}}
		/>
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
