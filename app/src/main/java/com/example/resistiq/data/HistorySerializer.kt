package com.example.resistiq.data

import com.example.resistiq.model.CalculationType
import com.example.resistiq.model.ResistorHistoryItem

/**
 * Line-based history encoding stored in a single Preferences DataStore string.
 *
 * One record per line, fields separated by '|'. Backslash, '|' and line breaks inside a field
 * are escaped, so any text round-trips exactly.
 */
object HistorySerializer {

    const val MAX_ITEMS = 50

    private const val SEPARATOR = '|'
    private const val FIELD_COUNT = 6

    private fun escape(s: String): String = buildString(s.length) {
        for (c in s) {
            when (c) {
                '\\' -> append("\\\\")
                SEPARATOR -> append("\\p")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                else -> append(c)
            }
        }
    }

    /** Splits on unescaped separators and unescapes each field in a single pass. */
    private fun splitFields(line: String): List<String> {
        val fields = mutableListOf<String>()
        val current = StringBuilder()
        var i = 0
        while (i < line.length) {
            val c = line[i]
            when {
                c == '\\' && i + 1 < line.length -> {
                    when (val next = line[i + 1]) {
                        'p' -> current.append(SEPARATOR)
                        'n' -> current.append('\n')
                        'r' -> current.append('\r')
                        else -> current.append(next)
                    }
                    i += 2
                    continue
                }
                c == SEPARATOR -> {
                    fields.add(current.toString())
                    current.clear()
                }
                else -> current.append(c)
            }
            i++
        }
        fields.add(current.toString())
        return fields
    }

    fun serialize(item: ResistorHistoryItem): String = listOf(
        item.id.toString(),
        item.type.name,
        item.title,
        item.details,
        item.timestamp.toString(),
        item.payloadJson
    ).joinToString(SEPARATOR.toString()) { escape(it) }

    fun deserialize(line: String): ResistorHistoryItem? {
        val parts = splitFields(line)
        if (parts.size != FIELD_COUNT) return null
        return try {
            ResistorHistoryItem(
                id = parts[0].toLong(),
                type = CalculationType.valueOf(parts[1]),
                title = parts[2],
                details = parts[3],
                timestamp = parts[4].toLong(),
                payloadJson = parts[5]
            )
        } catch (e: IllegalArgumentException) {
            null
        }
    }

    fun serializeList(list: List<ResistorHistoryItem>): String =
        list.joinToString("\n") { serialize(it) }

    fun deserializeList(data: String): List<ResistorHistoryItem> {
        if (data.isBlank()) return emptyList()
        return data.lines()
            .filter { it.isNotBlank() }
            .mapNotNull { deserialize(it) }
    }

    /** Newest first; drops the oldest records beyond [MAX_ITEMS]. */
    fun prepend(existing: List<ResistorHistoryItem>, item: ResistorHistoryItem): List<ResistorHistoryItem> =
        (listOf(item) + existing.filter { it.id != item.id }).take(MAX_ITEMS)
}
