<script lang="ts">
	import { onMount } from 'svelte';
	import { version } from '$app/environment';
	import { Settings, X, Sun, Moon } from '@lucide/svelte';
	import * as Card from '$lib/components/ui/card/index.js';
	import * as ToggleGroup from '$lib/components/ui/toggle-group/index.js';
	import { Button } from '$lib/components/ui/button/index.js';
	import { Label } from '$lib/components/ui/label/index.js';
	import { Input } from '$lib/components/ui/input/index.js';
	import { Switch } from '$lib/components/ui/switch/index.js';
	import { resetMode, setMode } from 'mode-watcher';

	let { onBack }: { onBack: () => void } = $props();

	let debugMode = $state(false);
	let showRestartHint = $state(false);
	let apiUrl = $state('');

	onMount(() => {
		// Load debug setting from localStorage
		const savedDebug = localStorage.getItem('debug');
		debugMode = savedDebug === 'true';
		// Load API URL from localStorage
		const savedApiUrl = localStorage.getItem('apiUrl');
		apiUrl = savedApiUrl || '';
	});

	function toggleDebug() {
		debugMode = !debugMode;
		// Save to localStorage
		localStorage.setItem('debug', debugMode.toString());
		// Show restart hint
		showRestartHint = true;
	}

	function updateApiUrl() {
		localStorage.setItem('apiUrl', apiUrl);
	}
</script>

<main class="flex h-screen w-full flex-col">
	<header class="flex h-[60px] w-full flex-row items-center gap-2 px-2">
		<Settings size={20} />
		<h1 class="text-xl font-bold">Settings</h1>
		<div class="flex-grow"></div>
		<Button variant="ghost" size="icon" onclick={onBack}>
			<X />
		</Button>
	</header>

	<div class="container mx-auto max-w-2xl space-y-6 p-2">
		<!-- Version Information -->
		<Card.Root>
			<Card.Header>
				<Card.Title>About</Card.Title>
			</Card.Header>
			<Card.Content>
				<div class="flex items-center justify-between">
					<div class="space-y-0.5">
						<Label>Version</Label>
						<p class="text-muted-foreground text-sm">Current application version</p>
					</div>
					<div class="text-muted-foreground font-mono text-sm font-medium">
						v{version}
					</div>
				</div>
			</Card.Content>
		</Card.Root>

		<!-- Regular Settings -->
		<Card.Root>
			<Card.Header>
				<Card.Title>Modes</Card.Title>
			</Card.Header>
			<Card.Content class="align-center flex justify-center">
				<ToggleGroup.Root type="single">
					<ToggleGroup.Item value="light" onclick={() => setMode('light')}><Sun /></ToggleGroup.Item
					>
					<ToggleGroup.Item value="dark" onclick={() => setMode('dark')}><Moon /></ToggleGroup.Item>
					<ToggleGroup.Item value="system" onclick={() => resetMode()}>System</ToggleGroup.Item>
				</ToggleGroup.Root>
			</Card.Content>
		</Card.Root>

		<!-- Debug Setting -->
		<Card.Root>
			<Card.Header>
				<Card.Title>Developer Options</Card.Title>
			</Card.Header>
			<Card.Content>
				<div class="flex items-center justify-between">
					<div class="space-y-0.5">
						<Label for="debug-switch">Debug Mode</Label>
						<p class="text-muted-foreground text-sm">Enable debug logging and developer tools</p>
					</div>
					<Switch id="debug-switch" checked={debugMode} onCheckedChange={toggleDebug} />
				</div>

				{#if showRestartHint}
					<div class="mt-4 rounded-md border border-blue-200 bg-blue-50 p-3">
						<p class="text-sm font-medium text-blue-800">
							⚠️ Restart the app to apply debug mode changes
						</p>
					</div>
				{/if}
			</Card.Content>

			<Card.Content>
				<div class="space-y-2">
					<Label for="api-url">API URL</Label>
					<Input
						id="api-url"
						bind:value={apiUrl}
						onchange={updateApiUrl}
						placeholder="https://api.example.com/api"
					/>
					<p class="text-muted-foreground text-sm">Url for data sharing & synchronization</p>
				</div>
			</Card.Content>
		</Card.Root>
	</div>
</main>

<style>
	main {
		padding-top: env(safe-area-inset-top);
		padding-bottom: env(safe-area-inset-bottom);
		padding-left: env(safe-area-inset-left);
		padding-right: env(safe-area-inset-right);
	}
</style>
