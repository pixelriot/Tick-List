<script lang="ts">
	import { Pen } from '@lucide/svelte';
	import { scale } from 'svelte/transition';
	import { backOut } from 'svelte/easing';
	import { Checkbox } from '$lib/components/ui/checkbox/index.js';
	import { Button } from './components/ui/button';
	import * as Item from '$lib/components/ui/item/index.js';
	import type { ShoppingItem } from './ListsService';

	let {
		item,
		onToggle,
		onEdit
	}: {
		item: ShoppingItem;
		onToggle: () => void;
		onEdit: () => void;
	} = $props();

	// Long-press (edit) settings
	const LONG_PRESS_DURATION = 500;
	const LONG_PRESS_MOVE_TOLERANCE = 10;

	let longPressTimeout: ReturnType<typeof setTimeout> | null = null;
	let longPressStart = { x: 0, y: 0 };

	function clearLongPress() {
		if (longPressTimeout) {
			clearTimeout(longPressTimeout);
			longPressTimeout = null;
		}
	}

	function handlePointerDown(event: PointerEvent) {
		const target = event.target as HTMLElement | null;
		if (target?.closest('button, input, a, label')) return;
		if (event.button !== 0) return;
		clearLongPress();
		longPressStart = { x: event.clientX, y: event.clientY };
		longPressTimeout = setTimeout(() => {
			handleEdit();
			clearLongPress();
		}, LONG_PRESS_DURATION);
	}

	function handlePointerMove(event: PointerEvent) {
		if (!longPressTimeout) return;
		const deltaX = event.clientX - longPressStart.x;
		const deltaY = event.clientY - longPressStart.y;
		if (Math.hypot(deltaX, deltaY) > LONG_PRESS_MOVE_TOLERANCE) {
			clearLongPress();
		}
	}

	function handlePointerUp() {
		clearLongPress();
	}

	function handlePointerCancel() {
		clearLongPress();
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
</script>

<Item.Root
	variant="outline"
	class="group relative m-2 overflow-hidden rounded-lg p-4"
	onpointerdown={handlePointerDown}
	onpointermove={handlePointerMove}
	onpointerup={handlePointerUp}
	onpointercancel={handlePointerCancel}
	style="touch-action: pan-y; user-select: none;"
	role="listitem"
	aria-label={item.name}
>
	<!-- Item content -->
	<div transition:scale={{ duration: 600, easing: backOut, start: 0.8 }}>
		<Item.Content class="transition-transform duration-200">
			<Item.Title>
				<Checkbox
					id="item-{item.id}"
					checked={item.checked}
					onclick={handleToggle}
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
	</Item.Actions>
</Item.Root>
