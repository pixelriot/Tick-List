package com.pixelriot.ticklist.data

import kotlinx.serialization.Serializable
import java.security.SecureRandom
import java.text.Collator
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

@Serializable
data class ShoppingItem(
    val id: String,
    val name: String,
    val checked: Boolean = false,
    val amount: Int = 1,
    val comment: String? = null,
    val updatedAt: String? = null,
    val deletedAt: String? = null,
)

@Serializable
data class ShoppingList(
    val id: String,
    val name: String,
    val items: List<ShoppingItem> = emptyList(),
    val sharingId: String? = null,
    val updatedAt: String? = null,
    val deletedAt: String? = null,
)

fun normalizeItem(item: ShoppingItem): ShoppingItem {
    val name = item.name.trim()
    val comment = item.comment?.trim()?.takeIf { it.isNotEmpty() }
    val amount = if (item.amount > 0) item.amount else 1
    return item.copy(
        name = name,
        checked = item.checked,
        amount = amount,
        comment = comment,
        deletedAt = item.deletedAt,
    )
}

fun normalizeList(list: ShoppingList): ShoppingList {
    val sharingId = list.sharingId?.trim()?.takeIf { it.isNotEmpty() }
    val items = list.items.map(::normalizeItem)
    return list.copy(
        name = list.name.trim(),
        items = items,
        sharingId = sharingId,
        deletedAt = list.deletedAt,
    )
}

fun createList(name: String, id: String = UUID.randomUUID().toString()): ShoppingList {
    return normalizeList(
        ShoppingList(
            id = id,
            name = name,
            items = emptyList(),
        ),
    )
}

private val shareCodePattern = Regex("^TL\\d{6}$", RegexOption.IGNORE_CASE)

fun isShareCode(raw: String): Boolean = shareCodePattern.matches(raw.trim())

fun generateShareCode(nextDigit: (bound: Int) -> Int = { bound ->
    SecureRandom().nextInt(bound)
}): String {
    val digits = buildString(6) {
        repeat(6) { append(nextDigit(10)) }
    }
    return "TL$digits"
}

fun remainingLabel(count: Int): String {
    return if (count == 1) "1 item remaining" else "$count items remaining"
}

fun uncheckedCount(list: ShoppingList): Int = list.items.count { !it.checked }

private val nameCollator: Collator = Collator.getInstance()

fun sortedLists(lists: List<ShoppingList>): List<ShoppingList> {
    return lists.sortedWith(compareBy(nameCollator) { it.name })
}

fun partitionItems(items: List<ShoppingItem>): Pair<List<ShoppingItem>, List<ShoppingItem>> {
    val active = items.filter { !it.checked }.sortedWith(compareBy(nameCollator) { it.name })
    val checked = items.filter { it.checked }.sortedWith(compareBy(nameCollator) { it.name })
    return active to checked
}

fun parseTimestamp(value: String?): Long? {
    if (value.isNullOrBlank()) return null
    return try {
        Instant.parse(value).toEpochMilli()
    } catch (_: Exception) {
        try {
            OffsetDateTime.parse(value).toInstant().toEpochMilli()
        } catch (_: Exception) {
            try {
                LocalDateTime.parse(value).toInstant(ZoneOffset.UTC).toEpochMilli()
            } catch (_: Exception) {
                null
            }
        }
    }
}

/**
 * Last-write-wins on `updated_at`. A missing timestamp does not win over a present one
 * only when both sides parse: the web client skips the incoming row when its timestamp
 * is less than or equal to the stored one, and applies it otherwise.
 */
fun mergeIncomingItem(items: List<ShoppingItem>, incoming: ShoppingItem): List<ShoppingItem> {
    val normalized = normalizeItem(incoming)
    val existing = items.find { it.id == normalized.id }
    val existingMs = parseTimestamp(existing?.updatedAt)
    val incomingMs = parseTimestamp(normalized.updatedAt)
    if (existingMs != null && incomingMs != null && incomingMs <= existingMs) {
        return items
    }
    if (!normalized.deletedAt.isNullOrBlank()) {
        return items.filter { it.id != normalized.id }
    }
    if (existing == null) {
        return items + normalized
    }
    return items.map { if (it.id == normalized.id) normalized else it }
}

/**
 * Refresh merge used while a cached shared list stays on screen.
 * Items edited or added during the fetch stay. Items removed during the fetch
 * are not restored from the server snapshot.
 */
fun mergeRefresh(
    before: List<ShoppingItem>,
    current: List<ShoppingItem>,
    server: List<ShoppingItem>,
): List<ShoppingItem> {
    val snapshot = before.associateBy { it.id }
    val currentIds = current.map { it.id }.toSet()
    val changedLocally = current.filter { item -> snapshot[item.id] != item }
    val changedIds = changedLocally.map { it.id }.toSet()
    val serverItems = server.filter { item ->
        item.id !in changedIds && !(snapshot.containsKey(item.id) && item.id !in currentIds)
    }
    return serverItems + changedLocally
}

fun nowIso(): String = Instant.now().toString()
