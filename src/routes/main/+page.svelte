<script lang="ts">
	import { onMount } from 'svelte';
	import ListSelection from '$lib/ListSelection.svelte';
	import ShoppingList from '$lib/ShoppingList.svelte';
	import Settings from '$lib/Settings.svelte';
	import { loadListFromStorage, type ShoppingList as ShoppingListType } from '$lib/ListsService';

	const CURRENT_LIST_ID_KEY = 'currentListId';

	let activeComponent = $state('lists');
	let currentList = $state<ShoppingListType | null>(null);

	onMount(() => {
		if (typeof localStorage === 'undefined') return;
		const savedListId = localStorage.getItem(CURRENT_LIST_ID_KEY);
		if (savedListId) {
			handleListSelected(savedListId);
		} else {
			console.log('No saved list ID found in localStorage.');
		}
	});

	function handleListSelected(listId: string) {
		const list = loadListFromStorage(listId);
		if (list) {
			localStorage.setItem(CURRENT_LIST_ID_KEY, list.id);
			currentList = list;
			activeComponent = 'shopping';
		}
	}

	function handleShowSettings() {
		activeComponent = 'settings';
	}

	function handleBackToLists() {
		activeComponent = 'lists';
	}
</script>

{#if activeComponent === 'lists'}
	<ListSelection onListSelected={handleListSelected} onShowSettings={handleShowSettings} />
{:else if activeComponent === 'shopping'}
	<ShoppingList onBackToLists={handleBackToLists} {currentList} />
{:else if activeComponent === 'settings'}
	<Settings onBack={handleBackToLists} />
{/if}
