package com.pixelriot.ticklist

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pixelriot.ticklist.data.ListStore
import com.pixelriot.ticklist.data.ShoppingItem
import com.pixelriot.ticklist.data.ShoppingList
import com.pixelriot.ticklist.data.SupabaseGateway
import com.pixelriot.ticklist.data.createList
import com.pixelriot.ticklist.data.isShareCode
import com.pixelriot.ticklist.data.mergeIncomingItem
import com.pixelriot.ticklist.data.mergeRefresh
import com.pixelriot.ticklist.data.normalizeItem
import com.pixelriot.ticklist.data.normalizeList
import com.pixelriot.ticklist.data.sortedLists
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.decodeRecordOrNull
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.yield
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.util.UUID

enum class Screen { Lists, Detail, Settings }

enum class ThemeMode { Dark, Light, System }

enum class SubmitResult { Created, Joined, Invalid }

class TickListViewModel(app: Application) : AndroidViewModel(app) {
    private val store = ListStore(File(app.filesDir, "lists"))
    private val prefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val gateway = SupabaseGateway.create()
    private val mutex = Mutex()

    private val _lists = MutableStateFlow(sortedLists(store.loadAll()))
    val lists: StateFlow<List<ShoppingList>> = _lists.asStateFlow()

    private val _currentListId = MutableStateFlow(prefs.getString(KEY_CURRENT, null))
    val currentListId: StateFlow<String?> = _currentListId.asStateFlow()

    private val _screen = MutableStateFlow(Screen.Lists)
    val screen: StateFlow<Screen> = _screen.asStateFlow()

    private val _joining = MutableStateFlow(false)
    val joining: StateFlow<Boolean> = _joining.asStateFlow()

    private val _fetching = MutableStateFlow(false)
    val fetching: StateFlow<Boolean> = _fetching.asStateFlow()

    private val _activeRequests = MutableStateFlow(0)
    val activeRequests: StateFlow<Int> = _activeRequests.asStateFlow()

    private val _syncError = MutableStateFlow(false)
    val syncError: StateFlow<Boolean> = _syncError.asStateFlow()

    private val _sharing = MutableStateFlow<Set<String>>(emptySet())
    val sharing: StateFlow<Set<String>> = _sharing.asStateFlow()

    private val _shareCode = MutableStateFlow<String?>(null)
    val shareCode: StateFlow<String?> = _shareCode.asStateFlow()

    private val _editingItem = MutableStateFlow<ShoppingItem?>(null)
    val editingItem: StateFlow<ShoppingItem?> = _editingItem.asStateFlow()

    private val _themeMode = MutableStateFlow(
        prefs.getString(KEY_MODE, ThemeMode.Dark.name)?.let {
            runCatching { ThemeMode.valueOf(it) }.getOrDefault(ThemeMode.Dark)
        } ?: ThemeMode.Dark,
    )
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _themeId = MutableStateFlow(prefs.getString(KEY_THEME, "tokyo-night") ?: "tokyo-night")
    val themeId: StateFlow<String> = _themeId.asStateFlow()

    private val _debug = MutableStateFlow(prefs.getBoolean(KEY_DEBUG, false))
    val debug: StateFlow<Boolean> = _debug.asStateFlow()

    private val _debugHint = MutableStateFlow(false)
    val debugHint: StateFlow<Boolean> = _debugHint.asStateFlow()

    private val _toasts = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val toasts: SharedFlow<String> = _toasts.asSharedFlow()

    private val pendingItems = linkedMapOf<String, ShoppingItem>()
    private var debounceJob: Job? = null
    private var syncJob: Job? = null

    init {
        val savedId = _currentListId.value
        if (savedId != null && _lists.value.any { it.id == savedId }) {
            _screen.value = Screen.Detail
            startSync()
        } else if (savedId != null) {
            _currentListId.value = null
            prefs.edit().remove(KEY_CURRENT).apply()
        }
    }

    fun currentList(): ShoppingList? {
        val id = _currentListId.value ?: return null
        return _lists.value.find { it.id == id }
    }

    fun openList(id: String) {
        if (_lists.value.none { it.id == id }) return
        _currentListId.value = id
        prefs.edit().putString(KEY_CURRENT, id).apply()
        _screen.value = Screen.Detail
        startSync()
    }

    fun back() {
        when (_screen.value) {
            Screen.Detail -> {
                stopSync()
                _editingItem.value = null
                _screen.value = Screen.Lists
            }
            Screen.Settings -> _screen.value = Screen.Lists
            Screen.Lists -> Unit
        }
    }

    fun showSettings() {
        _screen.value = Screen.Settings
    }

    fun submitNewList(raw: String): Pair<SubmitResult, String?> {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) {
            return SubmitResult.Invalid to "Enter a list name or sharing ID."
        }
        if (isShareCode(trimmed)) {
            join(trimmed.uppercase())
            return SubmitResult.Joined to null
        }
        if (_lists.value.any { it.name.equals(trimmed, ignoreCase = true) }) {
            return SubmitResult.Invalid to "A list with this name already exists."
        }
        return try {
            val created = createList(trimmed)
            viewModelScope.launch { mutex.withLock { persist(created) } }
            SubmitResult.Created to null
        } catch (error: Exception) {
            Log.e(TAG, "Failed to add list", error)
            toast("Failed to add list. Please try again.")
            SubmitResult.Invalid to null
        }
    }

    suspend fun rename(id: String, raw: String): RenameOutcome {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return RenameOutcome.Ignored
        val existing = _lists.value.find { it.id == id } ?: return RenameOutcome.Ignored
        if (_lists.value.any { it.id != id && it.name.equals(trimmed, ignoreCase = true) }) {
            return RenameOutcome.Invalid("A list with this name already exists.")
        }
        val updated = normalizeList(existing.copy(name = trimmed))
        try {
            mutex.withLock { persist(updated) }
            if (updated.sharingId != null) {
                val ok = gateway.updateSharedListName(updated.id, trimmed)
                if (!ok) {
                    toast("Failed to edit list. Please try again.")
                    return RenameOutcome.RemoteFailed
                }
            }
            return RenameOutcome.Saved
        } catch (error: Exception) {
            Log.e(TAG, "Failed to edit list", error)
            toast("Failed to edit list. Please try again.")
            return RenameOutcome.RemoteFailed
        }
    }

    fun deleteList(id: String) {
        viewModelScope.launch {
            mutex.withLock {
                store.delete(id)
                _lists.value = _lists.value.filter { it.id != id }
                if (_currentListId.value == id) {
                    _currentListId.value = null
                    prefs.edit().remove(KEY_CURRENT).apply()
                }
            }
        }
    }

    fun share(list: ShoppingList) {
        if (list.sharingId != null) {
            _shareCode.value = list.sharingId
            return
        }
        if (_sharing.value.contains(list.id)) return
        _sharing.value = _sharing.value + list.id
        viewModelScope.launch {
            try {
                val updated = gateway.shareList(list)
                if (updated?.sharingId != null) {
                    mutex.withLock { persist(updated) }
                    _shareCode.value = updated.sharingId
                } else {
                    toast("Failed to share list. Please try again.")
                }
            } catch (error: Exception) {
                Log.e(TAG, "Failed to share list", error)
                toast("Failed to share list. Please try again.")
            } finally {
                _sharing.value = _sharing.value - list.id
            }
        }
    }

    fun closeShare() {
        _shareCode.value = null
    }

    fun addItem(rawName: String): Boolean {
        val list = currentList() ?: return false
        val trimmed = rawName.trim()
        if (trimmed.isEmpty()) return false
        if (list.items.any { it.name.equals(trimmed, ignoreCase = true) }) {
            toast("An item with this name already exists in the list!")
            return false
        }
        val item = normalizeItem(
            ShoppingItem(
                id = UUID.randomUUID().toString(),
                name = trimmed,
                checked = false,
                amount = 1,
            ),
        )
        viewModelScope.launch {
            mutex.withLock {
                val latest = currentList() ?: return@withLock
                persist(latest.copy(items = latest.items + item))
            }
            queueItemUpdate(item)
        }
        return true
    }

    fun toggleItem(itemId: String) {
        val list = currentList() ?: return
        val toggled = list.items.find { it.id == itemId } ?: return
        val updated = normalizeItem(toggled.copy(checked = !toggled.checked))
        viewModelScope.launch {
            mutex.withLock {
                val latest = currentList() ?: return@withLock
                persist(latest.copy(items = latest.items.map { if (it.id == itemId) updated else it }))
            }
            queueItemUpdate(updated)
        }
    }

    fun openEditor(item: ShoppingItem) {
        _editingItem.value = item
    }

    fun closeEditor() {
        _editingItem.value = null
    }

    fun saveEditedItem(edited: ShoppingItem): Boolean {
        val list = currentList() ?: return false
        val name = edited.name.trim()
        if (name.isEmpty()) {
            toast("Item name is required.")
            return false
        }
        val amount = if (edited.amount > 0) edited.amount else 1
        val normalized = normalizeItem(edited.copy(name = name, amount = amount))
        if (list.items.any { it.id != normalized.id && it.name.equals(normalized.name, ignoreCase = true) }) {
            toast("An item with this name already exists in the list!")
            return false
        }
        viewModelScope.launch {
            mutex.withLock {
                val latest = currentList() ?: return@withLock
                persist(
                    latest.copy(items = latest.items.map { if (it.id == normalized.id) normalized else it }),
                )
            }
            queueItemUpdate(normalized)
        }
        return true
    }

    fun deleteItem(itemId: String) {
        val list = currentList() ?: return
        val item = list.items.find { it.id == itemId } ?: return
        viewModelScope.launch {
            mutex.withLock {
                pendingItems.remove(itemId)
                val latest = currentList() ?: return@withLock
                persist(latest.copy(items = latest.items.filter { it.id != itemId }))
            }
            if (list.sharingId == null) return@launch
            _activeRequests.value += 1
            try {
                val deleted = gateway.deleteItem(list.id, item)
                if (_activeRequests.value == 1 && deleted != null) {
                    mutex.withLock {
                        val latest = currentList() ?: return@withLock
                        if (latest.id == list.id) {
                            persist(latest.copy(items = mergeIncomingItem(latest.items, deleted)))
                        }
                    }
                }
                _syncError.value = false
            } catch (error: Exception) {
                Log.e(TAG, "Failed to delete item on server", error)
                toast("Failed to delete item on server. Please try again.")
                _syncError.value = true
            } finally {
                _activeRequests.value = (_activeRequests.value - 1).coerceAtLeast(0)
            }
        }
    }

    fun retrySync() {
        viewModelScope.launch {
            val healthy = gateway.health()
            val list = currentList()
            if (healthy && list?.sharingId != null) {
                flushPending()
                refreshSharedList()
                restartSubscription()
            } else {
                _syncError.value = true
            }
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
        prefs.edit().putString(KEY_MODE, mode.name).apply()
    }

    fun setTheme(id: String) {
        _themeId.value = id
        prefs.edit().putString(KEY_THEME, id).apply()
    }

    fun setDebug(enabled: Boolean) {
        _debug.value = enabled
        _debugHint.value = true
        prefs.edit().putBoolean(KEY_DEBUG, enabled).apply()
    }

    private fun join(code: String) {
        _joining.value = true
        viewModelScope.launch {
            try {
                val fetched = gateway.fetchSharedList(code)
                if (fetched != null) {
                    mutex.withLock { persist(fetched) }
                }
            } catch (error: Exception) {
                Log.e(TAG, "Failed to fetch list", error)
                toast("Failed to fetch list. Please try again.")
            } finally {
                _joining.value = false
            }
        }
    }

    private fun startSync() {
        stopSync()
        val list = currentList() ?: return
        if (list.sharingId == null) {
            _syncError.value = false
            return
        }
        if (!gateway.isConfigured) {
            _syncError.value = true
            return
        }
        val listId = list.id
        syncJob = viewModelScope.launch {
            launch { refreshSharedList() }
            launch { subscribe(listId) }
        }
    }

    private fun stopSync() {
        debounceJob?.cancel()
        val job = syncJob
        syncJob = null
        job?.cancel()
        viewModelScope.launch { flushPending() }
    }

    private fun restartSubscription() {
        val list = currentList() ?: return
        if (list.sharingId == null || !gateway.isConfigured) return
        syncJob?.cancel()
        val listId = list.id
        syncJob = viewModelScope.launch { subscribe(listId) }
    }

    private suspend fun refreshSharedList() {
        val list = currentList() ?: return
        val code = list.sharingId ?: return
        _fetching.value = true
        val before = list.items
        try {
            val latest = gateway.fetchSharedList(code, list.id) ?: return
            mutex.withLock {
                val current = currentList() ?: return@withLock
                if (current.id != list.id) return@withLock
                val mergedItems = mergeRefresh(before, current.items, latest.items)
                persist(latest.copy(items = mergedItems, sharingId = current.sharingId ?: latest.sharingId))
            }
            _syncError.value = false
        } catch (error: Exception) {
            Log.e(TAG, "Failed to refresh shared list", error)
            _syncError.value = true
        } finally {
            _fetching.value = false
        }
    }

    private suspend fun subscribe(listId: String) {
        val supabase = gateway.client ?: return
        val channel = supabase.channel("list-sync-$listId-${System.nanoTime()}")
        val changes = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = "list_items"
            filter { eq("list_id", listId) }
        }
        try {
            coroutineScope {
                launch {
                    changes.collect { action -> applyRealtime(listId, action) }
                }
                launch {
                    channel.systemFlow().collect { payload ->
                        _syncError.value = payload.status == "error"
                        if (_debug.value) Log.d(TAG, "realtime system ${payload.status}: ${payload.message}")
                    }
                }
                launch {
                    var sawConnection = false
                    supabase.realtime.status.collect { status ->
                        if (status == Realtime.Status.CONNECTED) {
                            sawConnection = true
                        } else if (sawConnection && status == Realtime.Status.DISCONNECTED) {
                            _syncError.value = true
                        }
                    }
                }
                yield()
                channel.subscribe(blockUntilSubscribed = false)
                awaitCancellation()
            }
        } finally {
            runCatching { channel.unsubscribe() }
        }
    }

    private suspend fun applyRealtime(listId: String, action: PostgresAction) {
        when (action) {
            is PostgresAction.Insert -> {
                val row = action.decodeRecordOrNull<RealtimeItemRow>() ?: return
                mergeRemote(listId, row.toItem())
            }
            is PostgresAction.Update -> {
                val row = action.decodeRecordOrNull<RealtimeItemRow>() ?: return
                mergeRemote(listId, row.toItem())
            }
            is PostgresAction.Select -> {
                val row = action.decodeRecordOrNull<RealtimeItemRow>() ?: return
                mergeRemote(listId, row.toItem())
            }
            is PostgresAction.Delete -> {
                val id = action.oldRecord["id"]?.jsonPrimitive?.contentOrNull ?: return
                mutex.withLock {
                    val latest = currentList() ?: return@withLock
                    if (latest.id != listId) return@withLock
                    persist(latest.copy(items = latest.items.filter { it.id != id }))
                }
            }
        }
    }

    private suspend fun mergeRemote(listId: String, item: ShoppingItem) {
        mutex.withLock {
            val latest = currentList() ?: return@withLock
            if (latest.id != listId) return@withLock
            persist(latest.copy(items = mergeIncomingItem(latest.items, item)))
        }
    }

    private fun queueItemUpdate(item: ShoppingItem) {
        val list = currentList() ?: return
        if (list.sharingId == null) return
        pendingItems[item.id] = item
        debounceJob?.cancel()
        debounceJob = viewModelScope.launch {
            delay(300)
            flushPending()
        }
    }

    private suspend fun flushPending() {
        val list = currentList() ?: return
        val sharingId = list.sharingId ?: return
        if (sharingId.isEmpty()) return
        val updates = mutex.withLock {
            if (pendingItems.isEmpty()) return
            val snapshot = pendingItems.values.toList()
            pendingItems.clear()
            snapshot
        }
        _activeRequests.value += 1
        try {
            val updated = gateway.upsertItems(list.id, updates)
            mutex.withLock {
                val stillCurrent = currentList() ?: return@withLock
                if (stillCurrent.id != list.id) return@withLock
                var items = stillCurrent.items
                updated.filter { pendingItems[it.id] == null }.forEach { incoming ->
                    items = mergeIncomingItem(items, incoming)
                }
                persist(stillCurrent.copy(items = items))
            }
            _syncError.value = false
        } catch (error: Exception) {
            Log.e(TAG, "Failed to update items on server", error)
            mutex.withLock {
                updates.forEach { item ->
                    if (pendingItems[item.id] == null) pendingItems[item.id] = item
                }
            }
            _syncError.value = true
        } finally {
            _activeRequests.value = (_activeRequests.value - 1).coerceAtLeast(0)
        }
    }

    private fun persist(list: ShoppingList) {
        val normalized = normalizeList(list)
        store.save(normalized)
        val without = _lists.value.filter { it.id != normalized.id }
        _lists.value = sortedLists(without + normalized)
    }

    private fun toast(message: String) {
        _toasts.tryEmit(message)
    }

    override fun onCleared() {
        syncJob?.cancel()
        super.onCleared()
    }

    companion object {
        private const val TAG = "TickList"
        private const val PREFS = "ticklist"
        private const val KEY_CURRENT = "currentListId"
        private const val KEY_MODE = "themeMode"
        private const val KEY_THEME = "themeId"
        private const val KEY_DEBUG = "debug"
    }
}

sealed interface RenameOutcome {
    data object Ignored : RenameOutcome
    data object Saved : RenameOutcome
    data object RemoteFailed : RenameOutcome
    data class Invalid(val message: String) : RenameOutcome
}

@Serializable
private data class RealtimeItemRow(
    val id: String,
    val name: String = "",
    val checked: Boolean = false,
    val amount: Int = 1,
    val comment: String? = null,
    val updated_at: String? = null,
    val deleted_at: String? = null,
) {
    fun toItem(): ShoppingItem = normalizeItem(
        ShoppingItem(
            id = id,
            name = name,
            checked = checked,
            amount = amount,
            comment = comment,
            updatedAt = updated_at,
            deletedAt = deleted_at,
        ),
    )
}
