<script lang="ts">
	import { onMount } from 'svelte';
	import { goto } from '$app/navigation';
	import Spinner from '$lib/components/ui/spinner/spinner.svelte';

	onMount(async () => {
		if (localStorage.getItem('debug') === 'true') {
			await initEruda();
		}
		goto('/main');
	});

	async function initEruda() {
		const eruda = await import('eruda');
		eruda.default.init();
		eruda.default.position({ x: 10, y: 300 });
	}
</script>

<div class="flex h-screen w-full items-center justify-center">
	<Spinner class="size-8" />
</div>

<style>
</style>
