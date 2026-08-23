package com.jegly.pages.core.tts

data class Sentence(val text: String, val start: Int, val end: Int)

object SentenceChunker {

    private val SENTENCE_BOUNDARY = Regex("""(?<=[.!?])\s+(?=[A-Z"'])""")
    private const val MAX_CHARS = 300

    fun chunk(text: String): List<Sentence> {
        val cleaned = text.trim()
        if (cleaned.isEmpty()) return emptyList()

        val rawSentences = SENTENCE_BOUNDARY.split(cleaned)
        val sentences = mutableListOf<Sentence>()
        var cursor = 0

        for (raw in rawSentences) {
            val trimmed = raw.trim()
            if (trimmed.isEmpty()) {
                cursor = (cursor + raw.length + 1).coerceAtMost(cleaned.length)
                continue
            }
            val start = cleaned.indexOf(trimmed, cursor).coerceAtLeast(cursor)
            val end = (start + trimmed.length).coerceAtMost(cleaned.length)

            // Sub-split if sentence exceeds max chars (e.g. long lists)
            if (trimmed.length > MAX_CHARS) {
                val commaChunks = trimmed.split(Regex(",\\s+"))
                var subCursor = start
                val buf = StringBuilder()
                for (part in commaChunks) {
                    if (buf.isNotEmpty() && buf.length + part.length + 2 > MAX_CHARS) {
                        val s = buf.toString().trim()
                        sentences.add(Sentence(s, subCursor, subCursor + s.length))
                        subCursor += s.length
                        buf.clear()
                    }
                    if (buf.isNotEmpty()) buf.append(", ")
                    buf.append(part)
                }
                if (buf.isNotEmpty()) {
                    val s = buf.toString().trim()
                    sentences.add(Sentence(s, subCursor, subCursor + s.length))
                }
            } else {
                sentences.add(Sentence(trimmed, start, end))
            }
            cursor = end
        }
        return sentences.filter { it.text.isNotBlank() }
    }
}
