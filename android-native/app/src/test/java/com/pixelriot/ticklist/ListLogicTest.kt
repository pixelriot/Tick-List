package com.pixelriot.ticklist

import com.pixelriot.ticklist.data.ListStore
import com.pixelriot.ticklist.data.ShoppingItem
import com.pixelriot.ticklist.data.createList
import com.pixelriot.ticklist.data.generateShareCode
import com.pixelriot.ticklist.data.isShareCode
import com.pixelriot.ticklist.data.mergeIncomingItem
import com.pixelriot.ticklist.data.mergeRefresh
import com.pixelriot.ticklist.data.normalizeItem
import com.pixelriot.ticklist.data.remainingLabel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files

class ListLogicTest {
    @Test
    fun normalizeTrimsAndFloorsAmount() {
        val item = normalizeItem(
            ShoppingItem(id = "1", name = "  milk  ", checked = true, amount = 0, comment = "  "),
        )
        assertEquals("milk", item.name)
        assertEquals(1, item.amount)
        assertNull(item.comment)
        assertTrue(item.checked)
    }

    @Test
    fun shareCodeMatchesServerPolicy() {
        assertEquals("TL333333", generateShareCode { 3 })
        assertTrue(isShareCode("tl123456"))
        assertTrue(isShareCode("TL000000"))
        assertFalse(isShareCode("TL12345"))
        assertFalse(isShareCode("XX123456"))
        assertFalse(isShareCode("groceries"))
    }

    @Test
    fun lastWriteWinsAndSoftDelete() {
        val older = item("a", "2024-01-01T00:00:00Z", "old")
        val newer = item("a", "2024-06-01T00:00:00Z", "new")
        assertEquals("new", mergeIncomingItem(listOf(older), newer).single().name)
        assertEquals("new", mergeIncomingItem(listOf(newer), older).single().name)

        val removed = newer.copy(deletedAt = "2024-07-01T00:00:00Z", updatedAt = "2024-07-01T00:00:00Z")
        assertTrue(mergeIncomingItem(listOf(newer), removed).isEmpty())

        val staleDelete = older.copy(deletedAt = "2024-01-02T00:00:00Z", updatedAt = "2024-01-02T00:00:00Z")
        assertEquals(1, mergeIncomingItem(listOf(newer), staleDelete).size)
    }

    @Test
    fun refreshKeepsLocalEditsAndLocalDeletes() {
        val beforeA = item("a", "2024-01-01T00:00:00Z", "milk")
        val beforeB = item("b", "2024-01-01T00:00:00Z", "bread")
        val edited = beforeA.copy(name = "oat milk")
        val serverA = beforeA.copy(name = "server milk", updatedAt = "2024-08-01T00:00:00Z")
        val serverB = beforeB.copy(name = "server bread")
        val serverC = item("c", "2024-08-01T00:00:00Z", "eggs")
        val merged = mergeRefresh(
            before = listOf(beforeA, beforeB),
            current = listOf(edited),
            server = listOf(serverA, serverB, serverC),
        )
        assertEquals(listOf("eggs", "oat milk"), merged.map { it.name })
    }

    @Test
    fun remainingLabelMatchesWebCopy() {
        assertEquals("1 item remaining", remainingLabel(1))
        assertEquals("0 items remaining", remainingLabel(0))
        assertEquals("2 items remaining", remainingLabel(2))
    }

    @Test
    fun storeRoundTrip() {
        val dir = Files.createTempDirectory("ticklist").toFile()
        val store = ListStore(dir)
        val list = createList("Groceries", id = "list-1").copy(sharingId = "TL123456")
        store.save(list)
        val loaded = store.loadAll().single()
        assertEquals("Groceries", loaded.name)
        assertEquals("TL123456", loaded.sharingId)
        store.delete(list.id)
        assertTrue(store.loadAll().isEmpty())
    }

    private fun item(id: String, updatedAt: String, name: String) = ShoppingItem(
        id = id,
        name = name,
        checked = false,
        amount = 1,
        updatedAt = updatedAt,
    )
}
