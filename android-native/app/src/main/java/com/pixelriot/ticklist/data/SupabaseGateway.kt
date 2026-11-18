package com.pixelriot.ticklist.data

import android.util.Log
import com.pixelriot.ticklist.BuildConfig
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.exception.PostgrestRestException
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.serializer.KotlinXSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.util.UUID

/**
 * Mirrors `src/lib/lists/supabase.ts`: same tables, soft deletes, and the
 * share-before-items order required by the row level security policies.
 */
class SupabaseGateway(val client: SupabaseClient?) {
    val isConfigured: Boolean = client != null

    suspend fun health(): Boolean {
        val supabase = client ?: return false
        return try {
            supabase.from("lists").select(columns = Columns.list("id")) {
                limit(1)
            }
            true
        } catch (error: Exception) {
            Log.e(TAG, "Supabase health check failed", error)
            false
        }
    }

    suspend fun shareList(list: ShoppingList): ShoppingList? {
        val supabase = client ?: return null
        if (!list.sharingId.isNullOrBlank()) return normalizeList(list)
        val normalized = normalizeList(list)
        val inserted = insertList(supabase, normalized)
        if (inserted == InsertOutcome.Failed) return null

        val shareCode = getShareCode(supabase, normalized.id) ?: createShareCode(supabase, normalized.id)
        if (shareCode == null) return null

        if (inserted == InsertOutcome.AlreadyExists) {
            try {
                supabase.from("lists").update(listNameBody(normalized.name, clearDeleted = true)) {
                    filter { eq("id", normalized.id) }
                }
            } catch (error: Exception) {
                Log.e(TAG, "Failed to update existing list after sharing", error)
            }
        }

        if (normalized.items.isNotEmpty()) {
            try {
                supabase.from("list_items").upsert(itemsBody(normalized.id, normalized.items))
            } catch (error: Exception) {
                Log.e(TAG, "Failed to create list items", error)
                return null
            }
        }
        return normalized.copy(sharingId = shareCode)
    }

    suspend fun resolveShareCode(shareCode: String): String? {
        val supabase = client ?: return null
        return try {
            supabase.from("list_shares").select(columns = Columns.list("list_id")) {
                filter {
                    eq("share_code", shareCode)
                    exact("deleted_at", null)
                }
                limit(1)
            }.decodeSingleOrNull<ShareListIdRow>()?.list_id
        } catch (error: Exception) {
            Log.e(TAG, "Failed to resolve share code", error)
            null
        }
    }

    suspend fun fetchSharedList(shareCode: String, knownListId: String? = null): ShoppingList? {
        if (client == null) return null
        if (knownListId != null) {
            return coroutineScope {
                val resolvedId = async { resolveShareCode(shareCode) }
                val list = async { fetchSharedListById(knownListId, shareCode) }
                if (resolvedId.await() == knownListId) list.await() else null
            }
        }
        val listId = resolveShareCode(shareCode) ?: return null
        return fetchSharedListById(listId, shareCode)
    }

    suspend fun updateSharedListName(listId: String, name: String): Boolean {
        val supabase = client ?: return false
        return try {
            supabase.from("lists").update(listNameBody(name.trim(), clearDeleted = false)) {
                filter {
                    eq("id", listId)
                    exact("deleted_at", null)
                }
            }
            true
        } catch (error: Exception) {
            Log.e(TAG, "Failed to update list name", error)
            false
        }
    }

    suspend fun upsertItems(listId: String, items: List<ShoppingItem>): List<ShoppingItem> {
        val supabase = client ?: return emptyList()
        if (items.isEmpty()) return emptyList()
        val rows = supabase.from("list_items").upsert(itemsBody(listId, items)) {
            select()
        }.decodeList<ItemRow>()
        return rows.map { it.toItem() }
    }

    suspend fun deleteItem(listId: String, item: ShoppingItem): ShoppingItem? {
        val deleted = item.copy(deletedAt = nowIso())
        val rows = upsertItems(listId, listOf(deleted))
        return rows.firstOrNull()
    }

    private suspend fun fetchSharedListById(listId: String, shareCode: String?): ShoppingList? {
        val supabase = client ?: return null
        val listRow = supabase.from("lists").select(
                columns = Columns.list("id", "name", "updated_at", "deleted_at"),
            ) {
                filter {
                    eq("id", listId)
                    exact("deleted_at", null)
                }
                limit(1)
            }.decodeSingleOrNull<ListRow>() ?: return null

            val itemRows = supabase.from("list_items").select(
                columns = Columns.list(
                    "id",
                    "list_id",
                    "name",
                    "checked",
                    "amount",
                    "comment",
                    "updated_at",
                    "deleted_at",
                ),
            ) {
                filter {
                    eq("list_id", listId)
                    exact("deleted_at", null)
                }
            }.decodeList<ItemRow>()

return normalizeList(
                ShoppingList(
                    id = listRow.id,
                    name = listRow.name,
                    items = itemRows.map { it.toItem() },
                    sharingId = shareCode,
                    updatedAt = listRow.updated_at,
                    deletedAt = listRow.deleted_at,
                ),
            )
    }

    private enum class InsertOutcome { Inserted, AlreadyExists, Failed }

    private suspend fun insertList(supabase: SupabaseClient, list: ShoppingList): InsertOutcome {
        return try {
            supabase.from("lists").insert(
                JsonArray(
                    listOf(
                        buildJsonObject {
                            put("id", list.id)
                            put("name", list.name)
                            put("deleted_at", JsonNull)
                        },
                    ),
                ),
            )
            InsertOutcome.Inserted
        } catch (error: Exception) {
            if (error.isUniqueViolation()) {
                InsertOutcome.AlreadyExists
            } else {
                Log.e(TAG, "Failed to create list", error)
                InsertOutcome.Failed
            }
        }
    }

    private suspend fun getShareCode(supabase: SupabaseClient, listId: String): String? {
        return try {
            supabase.from("list_shares").select(columns = Columns.list("share_code")) {
                filter {
                    eq("list_id", listId)
                    exact("deleted_at", null)
                }
                limit(1)
            }.decodeSingleOrNull<ShareCodeRow>()?.share_code
        } catch (error: Exception) {
            Log.e(TAG, "Failed to read share code", error)
            null
        }
    }

    private suspend fun createShareCode(supabase: SupabaseClient, listId: String): String? {
        repeat(5) {
            val shareCode = generateShareCode()
            try {
                supabase.from("list_shares").insert(
                    JsonArray(
                        listOf(
                            buildJsonObject {
                                put("id", UUID.randomUUID().toString())
                                put("list_id", listId)
                                put("share_code", shareCode)
                                put("deleted_at", JsonNull)
                            },
                        ),
                    ),
                )
                return shareCode
            } catch (error: Exception) {
                if (!error.isUniqueViolation()) {
                    Log.e(TAG, "Failed to create share code", error)
                    return null
                }
            }
        }
        Log.e(TAG, "Failed to create share code: duplicate attempts exceeded")
        return null
    }

    private fun listNameBody(name: String, clearDeleted: Boolean): JsonObject {
        return buildJsonObject {
            put("name", name)
            if (clearDeleted) put("deleted_at", JsonNull)
        }
    }

    private fun itemsBody(listId: String, items: List<ShoppingItem>): JsonArray {
        return JsonArray(
            items.map { item ->
                val normalized = normalizeItem(item)
                buildJsonObject {
                    put("id", normalized.id)
                    put("list_id", listId)
                    put("name", normalized.name)
                    put("checked", normalized.checked)
                    put("amount", normalized.amount)
                    if (normalized.comment == null) put("comment", JsonNull) else put("comment", normalized.comment)
                    if (normalized.deletedAt == null) {
                        put("deleted_at", JsonNull)
                    } else {
                        put("deleted_at", normalized.deletedAt)
                    }
                }
            },
        )
    }

    companion object {
        private const val TAG = "TickList"

        fun create(): SupabaseGateway {
            val url = BuildConfig.SUPABASE_URL.trim()
            val key = BuildConfig.SUPABASE_ANON_KEY.trim()
            if (url.isEmpty() || key.isEmpty()) return SupabaseGateway(null)
            val json = Json {
                ignoreUnknownKeys = true
                explicitNulls = true
                encodeDefaults = true
            }
            val supabase = createSupabaseClient(supabaseUrl = url, supabaseKey = key) {
                defaultSerializer = KotlinXSerializer(json)
                install(Postgrest)
                install(Realtime) {
                    requireValidSession = false
                    accessToken = { key }
                }
            }
            return SupabaseGateway(supabase)
        }
    }
}

private fun Throwable.isUniqueViolation(): Boolean {
    if (this is PostgrestRestException && code == "23505") return true
    return message?.contains("23505") == true
}

@Serializable
private data class ListRow(
    val id: String,
    val name: String,
    val updated_at: String? = null,
    val deleted_at: String? = null,
)

@Serializable
private data class ItemRow(
    val id: String,
    val list_id: String? = null,
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

@Serializable
private data class ShareCodeRow(val share_code: String)

@Serializable
private data class ShareListIdRow(val list_id: String)
