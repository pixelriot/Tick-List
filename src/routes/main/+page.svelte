<script lang="ts">
	import ListSelection from '$lib/ListSelection.svelte';
	import ShoppingList from '$lib/ShoppingList.svelte';
	import Settings from '$lib/Settings.svelte';

	let activeComponent = $state('lists');

	function handleListSelected() {
		activeComponent = 'shopping';
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
	<ShoppingList onBackToLists={handleBackToLists} />
{:else if activeComponent === 'settings'}
	<Settings onBack={handleBackToLists} />
{/if}
