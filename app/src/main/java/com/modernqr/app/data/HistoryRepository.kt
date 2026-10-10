package com.modernqr.app.data

import com.modernqr.app.domain.qr.QrPayloadParser
import com.modernqr.app.domain.qr.QrPayloadType
import kotlinx.coroutines.flow.Flow

class HistoryRepository(private val dao: HistoryDao) {
    fun observeHistory(query: String, filter: String): Flow<List<HistoryItem>> = dao.observeHistory(
        escapedQuery = HistoryPrivacyPolicy.escapeLikeQuery(query.trim()),
        entryType = filter.takeIf { it == "Scanned" || it == "Generated" },
        pinnedOnly = filter == "Pinned"
    )

    fun observeCount(): Flow<Int> = dao.observeCount()

    suspend fun add(content: String, format: String, type: String): HistorySaveResult {
        if (!HistoryPrivacyPolicy.canStore(content, format)) return HistorySaveResult.SENSITIVE_SKIPPED
        return if (dao.insertUnlessConsecutiveDuplicate(HistoryItem(content = content, format = format, type = type))) {
            HistorySaveResult.SAVED
        } else {
            HistorySaveResult.DUPLICATE_SKIPPED
        }
    }

    suspend fun removePreviouslyStoredSensitiveEntries() {
        val sensitiveIds = dao.getAllOnce()
            .filterNot { HistoryPrivacyPolicy.canStore(it.content, it.format) }
            .map(HistoryItem::id)
        if (sensitiveIds.isNotEmpty()) dao.deleteByIds(sensitiveIds)
    }

    suspend fun update(item: HistoryItem) = dao.update(item)
    suspend fun delete(item: HistoryItem) = dao.delete(item)
    suspend fun clear() = dao.clearHistory()
}

enum class HistorySaveResult {
    SAVED,
    SENSITIVE_SKIPPED,
    DUPLICATE_SKIPPED
}

object HistoryPrivacyPolicy {
    private val sensitiveTypes = setOf(
        QrPayloadType.WIFI,
        QrPayloadType.VCARD,
        QrPayloadType.EMAIL,
        QrPayloadType.TELEPHONE,
        QrPayloadType.SMS
    )

    fun canStore(content: String, format: String): Boolean =
        QrPayloadParser.parse(content, format).type !in sensitiveTypes

    fun escapeLikeQuery(query: String): String = buildString(query.length) {
        query.forEach { character ->
            if (character == '\\' || character == '%' || character == '_') append('\\')
            append(character)
        }
    }
}
