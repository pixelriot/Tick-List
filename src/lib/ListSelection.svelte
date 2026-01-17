<script lang="ts">
	import { onMount } from 'svelte';
	import { scale } from 'svelte/transition';
	import { backOut } from 'svelte/easing';
	import { Pen, Trash2, X, Check, Share2, Settings, List } from '@lucide/svelte';
	import { Button } from '$lib/components/ui/button/index.js';
	import { Input } from '$lib/components/ui/input/index.js';
	import { Badge } from '$lib/components/ui/badge/index.js';
	import { Skeleton } from '$lib/components/ui/skeleton/index.js';
	import * as Item from '$lib/components/ui/item/index.js';
	import * as Empty from '$lib/components/ui/empty/index.js';
	import { toast } from 'svelte-sonner';
	import { invoke } from '@tauri-apps/api/core';
	import {
		loadListsFromStorage,
		saveListToStorage,
		deleteListFromStorage,
		fetchSharedList,
		shareList,
		updateSharedListName,
		type ShoppingList
	} from './ListsService';

	let {
		onListSelected,
		onShowSettings
	}: { onListSelected: (listId: string) => void; onShowSettings: () => void } = $props();

	let lists = $state<ShoppingList[]>([]);

	let isFetchingList = $state(false);

	let newListName = $state('');

	let editingListId = $state<string | null>(null);
	let editingListName = $state('');

	onMount(() => {
		lists = loadListsFromStorage();
	});

	async function selectList(id: string) {
		const selectedList = lists.find((list) => list.id === id) || null;
		console.log('Selected list:', $state.snapshot(selectedList));
		if (selectedList) {
			onListSelected(selectedList.id);
		}
	}

	function addList() {
		const trimmedName = newListName.trim();
		if (!trimmedName) return;

		// Check if trimmedName is exactly 8 numbers
		if (/^\d{8}$/.test(trimmedName)) {
			fetchList(trimmedName);
			newListName = '';
		} else {
			try {
				console.log('Adding new list:', trimmedName);

				let newList = {
					id: crypto.randomUUID(),
					name: trimmedName,
					items: []
				} as ShoppingList;
				saveListToStorage(newList);
				lists.push(newList);

				newListName = '';
			} catch (error) {
				console.error('Failed to add list:', error);
				toast.error('Failed to add list. Please try again.');
			}
		}
	}

	function startEditList(id: string, name: string) {
		editingListId = id;
		editingListName = name;
	}

	async function saveEditList() {
		if (!editingListId || !editingListName.trim()) return;
		const trimmedName = editingListName.trim();
		try {
			const editedList: ShoppingList | undefined = lists.find((list) => list.id === editingListId);
			if (editedList) {
				editedList.name = trimmedName;
				saveListToStorage(editedList);

				if (editedList.sharingId) {
					// Update the list on the server if it is shared
					const success = await updateSharedListName(editedList.sharingId, trimmedName);
					if (!success) {
						throw new Error('Failed to update shared list name');
					}
				}
			}
			editingListId = null;
			editingListName = '';
		} catch (error) {
			console.error('Failed to edit list:', error);
			toast.error('Failed to edit list. Please try again.');
		}
	}

	function cancelEdit() {
		editingListId = null;
		editingListName = '';
	}

	function deleteList(id: string) {
		const listToDelete = lists.find((list) => list.id === id);
		if (!listToDelete) return;
		const itemCount = listToDelete.items.length;
		const confirmMessage =
			itemCount > 0
				? `Delete "${listToDelete.name}" and its ${itemCount} item${itemCount !== 1 ? 's' : ''}?`
				: `Delete "${listToDelete.name}"?`;
		if (!confirm(confirmMessage)) return;

		try {
			lists = lists.filter((list) => list.id !== id);
			deleteListFromStorage(id);
		} catch (error) {
			console.error('Failed to delete list:', error);
			toast.error('Failed to delete list. Please try again.');
		}
	}

	async function fetchList(listId: string) {
		console.log('Fetching list:', listId);
		isFetchingList = true;
		try {
			const fetchedList = await fetchSharedList(listId);
			if (fetchedList) {
				//check if list already exists
				const existingList = lists.find((list) => list.id === fetchedList.id);
				if (existingList) {
					console.log('updating existing list');
					Object.assign(existingList, fetchedList);
				} else {
					console.log('new list fetched');
					lists.push(fetchedList);
				}
			}
		} catch (error) {
			console.error('Failed to fetch list:', error);
			toast.error('Failed to fetch list. Please try again.');
		} finally {
			isFetchingList = false;
		}
	}

	async function onShareListClicked(list: ShoppingList) {
		console.log('Share list:', $state.snapshot(list));
		const updatedList = await shareList(list);
		if (updatedList) {
			// Update the list in the local array
			const index = lists.findIndex((l) => l.id === list.id);
			if (index !== -1) {
				lists[index] = updatedList;
			}
		}
	}

	async function copySharingId(sharingId: string) {
		try {
			await invoke('plugin:clipboard-manager|write_text', { text: sharingId });
			toast.success('Sharing ID copied to clipboard');
		} catch (error) {
			console.error('Failed to copy to clipboard:', error);
			// Fallback to web API if Tauri fails
			try {
				await navigator.clipboard.writeText(sharingId);
				toast.success('Sharing ID copied to clipboard');
			} catch (fallbackError) {
				console.error('Fallback also failed:', fallbackError);
				toast.error('Failed to copy sharing ID');
			}
		}
	}
</script>

<main class="flex h-screen w-full flex-col">
	<header class="flex h-[60px] w-full flex-row items-center gap-2 px-2">
		<svg
			width="32px"
			height="32px"
			viewBox="0 0 24 24"
			fill="none"
			xmlns="http://www.w3.org/2000/svg"
			style="color: var(--sidebar-foreground);"
		>
			<path
				fill-rule="evenodd"
				clip-rule="evenodd"
				d="M1 3C1 2.44772 1.44772 2 2 2C3.62481 2 5.06733 3.03971 5.58114 4.58114L5.72076 5L18.03 5C18.6859 4.99998 19.2437 4.99996 19.6951 5.04029C20.165 5.08226 20.6347 5.17512 21.064 5.43584C21.6667 5.80183 22.1211 6.36838 22.3477 7.03605C22.5091 7.51168 22.4978 7.99036 22.4369 8.45816C22.3783 8.90755 22.2573 9.45209 22.115 10.0924L21.8088 11.4704C21.664 12.1218 21.5435 12.6641 21.4106 13.1043C21.2716 13.5649 21.1006 13.9803 20.8231 14.36C20.4058 14.931 19.8446 15.3812 19.1967 15.6646C18.7658 15.8532 18.3232 15.93 17.8434 15.9658C17.3849 16 16.8295 16 16.1621 16H10.8379C10.1705 16 9.61512 16 9.15656 15.9658C8.67678 15.93 8.23421 15.8532 7.80328 15.6646C7.15536 15.3812 6.59418 14.931 6.17692 14.36C5.89941 13.9803 5.72844 13.5649 5.58939 13.1043C5.45649 12.6641 5.33602 12.1219 5.19125 11.4704L4.035 6.26729L3.68377 5.21359C3.44219 4.48885 2.76395 4 2 4C1.44772 4 1 3.55228 1 3ZM6.24662 7L7.13569 11.0008C7.29042 11.6971 7.39528 12.166 7.50404 12.5263C7.60908 12.8742 7.69899 13.0531 7.79172 13.18C8.00035 13.4655 8.28094 13.6906 8.6049 13.8323C8.74888 13.8953 8.94301 13.9443 9.30546 13.9713C9.68076 13.9994 10.1612 14 10.8745 14H16.1255C16.8388 14 17.3192 13.9994 17.6945 13.9713C18.057 13.9443 18.2511 13.8953 18.3951 13.8323C18.7191 13.6906 18.9997 13.4655 19.2083 13.18C19.301 13.0531 19.3909 12.8742 19.496 12.5263C19.6047 12.166 19.7096 11.6971 19.8643 11.0008L20.153 9.70159C20.3075 9.00651 20.408 8.54985 20.4536 8.19974C20.4982 7.858 20.4722 7.73312 20.4537 7.67868C20.3782 7.45613 20.2267 7.26728 20.0259 7.14528C19.9767 7.11544 19.8605 7.06302 19.5172 7.03235C19.1655 7.00094 18.6979 7 17.9859 7H6.24662Z"
				fill="currentColor"
			/>
			<path
				d="M11 19C11 20.1046 10.1046 21 9 21C7.89543 21 7 20.1046 7 19C7 17.8954 7.89543 17 9 17C10.1046 17 11 17.8954 11 19Z"
				fill="currentColor"
			/>
			<path
				d="M18 21C19.1046 21 20 20.1046 20 19C20 17.8954 19.1046 17 18 17C16.8954 17 16 17.8954 16 19C16 20.1046 16.8954 21 18 21Z"
				fill="currentColor"
			/>
		</svg>
		<h1 class="text-xl font-bold">Lists</h1>
		<div class="flex-grow"></div>
		<Button variant="ghost" size="icon" onclick={onShowSettings}>
			<Settings />
		</Button>
	</header>

	<form class="flex w-full items-center space-x-2 p-2">
		<Input
			type="text"
			placeholder="Enter a new list name or a sharing ID"
			bind:value={newListName}
			onkeydown={(e) => e.key === 'Enter' && addList()}
			aria-label="New list name or list id"
			maxlength={100}
		/>
		<Button onclick={addList}>Add</Button>
	</form>

	{#if lists.length === 0 && !isFetchingList}
		<Empty.Root>
			<Empty.Header>
				<Empty.Media variant="icon">
					<List />
				</Empty.Media>
				<Empty.Title>No lists available</Empty.Title>
				<Empty.Description>
					Add a new list or enter a sharing ID to fetch a shared list
				</Empty.Description>
			</Empty.Header>
		</Empty.Root>
	{:else}
		<ul class="flex-1 space-y-3 overflow-y-auto p-2">
			{#each lists as list (list.id)}
				<li transition:scale={{ duration: 200, start: 0.4, easing: backOut }}>
					<Item.Root variant="outline">
						{#if editingListId === list.id}
							<Item.Content>
								<form class="flex w-full items-center space-x-2">
									<Input
										type="text"
										placeholder="Edit list name"
										bind:value={editingListName}
										onkeydown={(e) => {
											if (e.key === 'Enter') saveEditList();
											else if (e.key === 'Escape') cancelEdit();
										}}
										aria-label="Edit list name"
										maxlength={100}
										autofocus
									/>
									<Button
										variant="outline"
										size="icon"
										onclick={saveEditList}
										aria-label="Save changes"
									>
										<Check />
									</Button>
									<Button
										variant="outline"
										size="icon"
										onclick={cancelEdit}
										aria-label="Cancel editing"
									>
										<X />
									</Button>
								</form>
							</Item.Content>
						{:else}
							<Item.Content onclick={() => selectList(list.id)}>
								<Item.Title>{list.name}</Item.Title>
								<Item.Description
									>{#if list.items}
										{list.items.filter((i: { checked: any }) => !i.checked).length} item{list.items.filter(
											(i: { checked: any }) => !i.checked
										).length === 1
											? ''
											: 's'} remaining
									{/if}
									{#if list.sharingId}
										<Badge
											variant="outline"
											onclick={(e) => {
												e.stopPropagation();
												copySharingId(list.sharingId!);
											}}
											class="cursor-pointer">Shared</Badge
										>
									{/if}</Item.Description
								>
							</Item.Content>
							<Item.Actions>
								<Button
									variant="ghost"
									size="icon"
									onclick={() => onShareListClicked(list)}
									aria-label={`Share ${list.name}`}
								>
									<Share2 size={24} />
								</Button>
								<Button
									variant="ghost"
									size="icon"
									onclick={() => startEditList(list.id, list.name)}
									aria-label={`Edit ${list.name}`}
								>
									<Pen size={24} />
								</Button>
								<Button
									variant="ghost"
									size="icon"
									onclick={() => deleteList(list.id)}
									aria-label={`Delete ${list.name}`}
								>
									<Trash2 size={24} />
								</Button>
							</Item.Actions>
						{/if}
					</Item.Root>
				</li>
			{/each}

			{#if isFetchingList}
				<Skeleton class=" flex h-20 " />
			{/if}
		</ul>
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
