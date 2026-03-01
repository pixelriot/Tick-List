<script lang="ts">
	import * as Drawer from '$lib/components/ui/drawer/index.js';
	import { Button } from '$lib/components/ui/button/index.js';
	import { Label } from './components/ui/label';
	import { Copy } from '@lucide/svelte';
	import { toast } from 'svelte-sonner';
	import { invoke, isTauri } from '@tauri-apps/api/core';

	let {
		sharingId = null,
		onClose = () => {},
		onCopy = () => {}
	}: { sharingId: string | null; onClose: () => void; onCopy: () => void } = $props();

	let open = $state(false);

	$effect(() => {
		open = !!sharingId;
	});

	$effect(() => {
		if (!open && sharingId !== null) {
			onClose();
		}
	});

	async function copyToClipboard() {
		if (!sharingId) return;
		try {
			await onCopy();
		} catch (err) {
			console.error('Copy failed:', err);
			toast.error('Failed to copy sharing ID');
		}
	}
</script>

<Drawer.Root bind:open>
	<Drawer.Content class="h-fit">
		<div class="space-y-4 p-4">
			<div class="grid gap-3">
				<Label>Sharing code</Label>
				<div class="flex items-center justify-center gap-3">
					<div class="font-mono break-all">{sharingId || ''}</div>
					<Button
						variant="ghost"
						size="icon"
						onclick={copyToClipboard}
						aria-label="Copy sharing code"
					>
						<Copy />
					</Button>
				</div>
			</div>

			<p class="text-muted-foreground text-sm">
				Share this code with your friends and family. Lists can be added by entering the sharing
				code in the "New list" field and pressing Add.
			</p>
		</div>
	</Drawer.Content>
</Drawer.Root>
