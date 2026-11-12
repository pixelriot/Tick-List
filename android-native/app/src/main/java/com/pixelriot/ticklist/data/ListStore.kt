package com.pixelriot.ticklist.data

import kotlinx.serialization.json.Json
import java.io.File

/**
 * One JSON file per list, same shape the web app keeps in localStorage
 * (`shoppingList_<id>`). Reads are synchronous and local so the overview can
 * paint cached shared lists before the network returns.
 */
class ListStore(private val directory: File) {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = false
        prettyPrint = true
    }

    fun loadAll(): List<ShoppingList> {
        if (!directory.exists()) return emptyList()
        val files = directory.listFiles { file -> file.isFile && file.extension == "json" } ?: return emptyList()
        return files.mapNotNull { file ->
            runCatching {
                normalizeList(json.decodeFromString<ShoppingList>(file.readText()))
            }.getOrNull()
        }
    }

    fun save(list: ShoppingList) {
        directory.mkdirs()
        val normalized = normalizeList(list)
        val target = File(directory, "${normalized.id}.json")
        val temporary = File(directory, "${normalized.id}.json.tmp")
        temporary.writeText(json.encodeToString(ShoppingList.serializer(), normalized))
        if (!temporary.renameTo(target)) {
            target.writeText(temporary.readText())
            temporary.delete()
        }
    }

    fun delete(id: String) {
        File(directory, "$id.json").delete()
        File(directory, "$id.json.tmp").delete()
    }
}
