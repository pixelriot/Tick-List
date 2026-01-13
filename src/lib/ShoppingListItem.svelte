<script lang="ts">
	import { Trash2, Pen } from '@lucide/svelte';
	import { scale } from 'svelte/transition';
	import { backOut } from 'svelte/easing';
	import { Checkbox } from '$lib/components/ui/checkbox/index.js';
	import { Button } from './components/ui/button';
	import * as Item from '$lib/components/ui/item/index.js';
	import type { ShoppingItem } from './ListsService';

	let {
		item,
		onToggle,
		onDelete,
		onEdit
	}: {
		item: ShoppingItem;
		onToggle: () => void;
		onDelete: () => void;
		onEdit: () => void;
	} = $props();

	// Constants
	const SWIPE_DELETE_THRESHOLD = 120;
	const SWIPE_EDIT_THRESHOLD = 120;
	const SWIPE_VISIBILITY_THRESHOLD = 30;

	// Swipe state
	let swipeState = $state({
		x: 0,
		startX: 0,
		dragging: false,
		deleteThresholdPassed: false,
		editThresholdPassed: false
	});

	// Reset swipe state to initial position
	function resetSwipeState() {
		swipeState = {
			...swipeState,
			x: 0,
			dragging: false,
			deleteThresholdPassed: false,
			editThresholdPassed: false
		};
	}

	function handleTouchStart(event: TouchEvent) {
		if (!event.touches || event.touches.length === 0) return;

		const touch = event.touches[0];
		swipeState = {
			x: 0,
			startX: touch.clientX,
			dragging: true,
			deleteThresholdPassed: false,
			editThresholdPassed: false
		};
	}

	function handleTouchMove(event: TouchEvent) {
		if (!swipeState.dragging || !event.touches || event.touches.length === 0) return;

		const touch = event.touches[0];
		const deltaX = touch.clientX - swipeState.startX;

		// Allow both left swipe (delete) and right swipe (edit)
		if (deltaX < 0) {
			// Left swipe for delete
			swipeState.x = Math.max(deltaX, -SWIPE_DELETE_THRESHOLD);
			swipeState.deleteThresholdPassed = Math.abs(deltaX) > SWIPE_DELETE_THRESHOLD;
			swipeState.editThresholdPassed = false;
		} else if (deltaX > 0) {
			// Right swipe for edit
			swipeState.x = Math.min(deltaX, SWIPE_EDIT_THRESHOLD);
			swipeState.editThresholdPassed = Math.abs(deltaX) > SWIPE_EDIT_THRESHOLD;
			swipeState.deleteThresholdPassed = false;
		}
	}

	function handleTouchEnd() {
		if (swipeState.deleteThresholdPassed) {
			try {
				onDelete();
			} catch (error) {
				console.error('Failed to delete item:', error);
			}
			resetSwipeState();
		} else if (swipeState.editThresholdPassed) {
			try {
				onEdit();
			} catch (error) {
				console.error('Failed to edit item:', error);
			}
			resetSwipeState();
		} else {
			resetSwipeState();
		}
	}

	// Safe button handlers
	function handleToggle() {
		try {
			onToggle();
		} catch (error) {
			console.error('Failed to toggle item:', error);
		}
	}

	function handleEdit() {
		try {
			onEdit();
		} catch (error) {
			console.error('Failed to edit item:', error);
		}
	}

	function handleDelete() {
		try {
			onDelete();
		} catch (error) {
			console.error('Failed to delete item:', error);
		}
	}
</script>

<Item.Root
	variant="outline"
	class="group relative m-2 overflow-hidden rounded-lg p-4"
	ontouchstart={handleTouchStart}
	ontouchmove={handleTouchMove}
	ontouchend={handleTouchEnd}
	style="touch-action: pan-y; user-select: none;"
	role="listitem"
	aria-label={item.name}
>
	<!-- Item content -->
	<div transition:scale={{ duration: 600, easing: backOut, start: 0.8 }}>
		<Item.Content
			class="transition-transform duration-200"
			style="transform: translateX({swipeState?.x || 0}px);"
		>
			<Item.Title>
				<Checkbox
					id="item-{item.id}"
					checked={item.checked}
					onclick={handleToggle}
					onkeydown={(e) => {
						if (e.key === 'Delete' || e.key === 'Backspace') {
							e.preventDefault();
							handleDelete();
						}
					}}
					aria-label={`Mark ${item.name} as ${item.checked ? 'incomplete' : 'complete'}`}
				/>

				{item.name}
				{#if item.amount > 0}
					<span class="text-sm" style="color: var(--muted-foreground);">({item.amount})</span>
				{/if}
			</Item.Title>

			{#if item.comment}
				<Item.Description>
					{item.comment}
				</Item.Description>
			{/if}
		</Item.Content>
	</div>

	<!-- Desktop buttons -->
	<Item.Actions
		class="hidden md:block md:opacity-0 md:transition-opacity md:group-hover:opacity-100"
	>
		<Button
			variant="ghost"
			size="icon"
			onclick={handleEdit}
			aria-label="Edit {item.name}"
			title="Edit item"
		>
			<Pen size={16} />
		</Button>
		<Button
			variant="ghost"
			size="icon"
			onclick={handleDelete}
			aria-label="Delete {item.name}"
			title="Delete item"
		>
			<Trash2 size={16} />
		</Button>
	</Item.Actions>

	<!-- Left swipe indicator for delete -->
	<div
		class="absolute top-0 right-0 bottom-0 flex items-center justify-center transition-all duration-200"
		style="width: {Math.abs(swipeState?.x || 0) > SWIPE_VISIBILITY_THRESHOLD && swipeState.x < 0
			? Math.min(SWIPE_DELETE_THRESHOLD, Math.abs(swipeState?.x || 0))
			: 0}px; background: var(--destructive);"
		aria-hidden="true"
	>
		{#if Math.abs(swipeState?.x || 0) > SWIPE_VISIBILITY_THRESHOLD && swipeState.x < 0}
			<Trash2 size={24} class="text-white" />
		{/if}
	</div>

	<!-- Right swipe indicator for edit -->
	<div
		class="absolute top-0 bottom-0 left-0 flex items-center justify-center transition-all duration-200"
		style="width: {Math.abs(swipeState?.x || 0) > SWIPE_VISIBILITY_THRESHOLD && swipeState.x > 0
			? Math.min(SWIPE_EDIT_THRESHOLD, Math.abs(swipeState?.x || 0))
			: 0}px; background: var(--primary);"
		aria-hidden="true"
	>
		{#if Math.abs(swipeState?.x || 0) > SWIPE_VISIBILITY_THRESHOLD && swipeState.x > 0}
			<Pen size={24} class="text-white" />
		{/if}
	</div>
</Item.Root>
