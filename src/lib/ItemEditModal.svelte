<script lang="ts">
	import * as Drawer from '$lib/components/ui/drawer/index.js';
	import { Button } from '$lib/components/ui/button/index.js';
	import { Input } from '$lib/components/ui/input/index.js';
	import { Label } from './components/ui/label';
	import { toast } from 'svelte-sonner';
	import type { ShoppingItem } from '$lib/lists/types';

	let {
		item = null,
		onSave,
		onClose,
		onDelete
	}: {
		item: ShoppingItem | null;
		onSave: (editedItem: ShoppingItem) => void;
		onClose: () => void;
		onDelete: (itemToDelete: ShoppingItem) => void;
	} = $props();

	let editName = $state('');
	let editAmount = $state(1);
	let editComment = $state('');
	let open = $state(false);
	let isDeleting = $state(false);

	// Update form when item changes
	$effect(() => {
		if (item) {
			editName = item.name;
			editAmount = item.amount;
			editComment = item.comment || '';
			open = true;
		} else {
			open = false;
		}
	});

	// Handle when drawer is closed externally (e.g., clicking outside)
	$effect(() => {
		if (!open && item !== null) {
			if (!isDeleting) {
				handleSave();
			}
			isDeleting = false;
			onClose();
		}
	});

	function handleSave() {
		if (!item) return;
		if (!editName.trim()) {
			toast.error('Item name is required.');
			return;
		}
		if (editAmount <= 0) {
			editAmount = 1;
		}
		// Create and return the edited item copy
		const editedItem: ShoppingItem = {
			...item,
			name: editName.trim(),
			amount: editAmount,
			comment: editComment.trim() || undefined
		};
		onSave(editedItem);
	}

	function handleClose() {
		open = false;
		onClose();
	}

	function handleDelete() {
		if (!item) return;
		if (!confirm(`Delete "${item.name}"?`)) {
			return;
		}
		isDeleting = true;
		onDelete(item);
		handleClose();
	}
</script>

<Drawer.Root bind:open>
	<Drawer.Content class="h-fit">
		<div class="space-y-4 p-4">
			<div class="grid gap-3">
				<Label for="item">Name</Label>
				<Input id="item" type="text" placeholder="Enter item name" bind:value={editName} />
			</div>
			<div class="grid gap-3">
				<Label for="amount">Amount</Label>
				<Input id="amount" type="number" placeholder="Enter item amount" bind:value={editAmount} />
			</div>
			<div class="grid gap-3">
				<Label for="comment">Comment</Label>
				<Input
					id="comment"
					type="text"
					placeholder="Enter comment (optional)"
					bind:value={editComment}
				/>
			</div>
		</div>
		<Drawer.Footer>
			<div class="flex w-full flex-col gap-2">
				<Button variant="ghost" class="text-red-600" onclick={handleDelete} disabled={!item}
					>Delete Item</Button
				>
			</div>
		</Drawer.Footer>
	</Drawer.Content>
</Drawer.Root>
