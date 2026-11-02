package com.aether.android.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

private const val MAX_BYTES = 2_000_000
private const val MAX_REDIRECTS = 5

/**
 * Fetches one page the user explicitly asked for and reduces it to readable
 * text. This is the only networked feature in Study Hub, and it runs only
 * when the user taps it. No login, cookies or paywall circumvention: if the
 * page isn't publicly readable, the reader reports that instead.
 */
object WebPageReader {

    suspend fun read(url: String): Result<ReadablePage> = withContext(Dispatchers.IO) {
        runCatching {
            var current = url.trim()
            repeat(MAX_REDIRECTS + 1) {
                require(current.startsWith("http://") || current.startsWith("https://")) { "Only web links (http/https) are supported." }
                // Android blocks cleartext HTTP for modern apps, and nearly every site serves HTTPS anyway.
                if (current.startsWith("http://")) current = "https://" + current.removePrefix("http://")
                val conn = (URL(current).openConnection() as HttpURLConnection).apply {
                    instanceFollowRedirects = false
                    connectTimeout = 10_000
                    readTimeout = 15_000
                    setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android) AETHER-Reader")
                    setRequestProperty("Accept", "text/html,text/plain;q=0.9")
                }
                try {
                    val code = conn.responseCode
                    if (code in 300..399) {
                        current = URL(URL(current), conn.getHeaderField("Location") ?: error("Bad redirect")).toString()
                        return@repeat
                    }
                    check(code in 200..299) { "The site returned HTTP $code." }
                    val type = conn.contentType.orEmpty().lowercase()
                    check("pdf" !in type) { "That link is a PDF. Download it and use the PDF import instead." }
                    check("html" in type || "text" in type || type.isEmpty()) { "That link isn't a readable web page." }
                    val charset = Regex("charset=([\\w-]+)").find(type)?.groupValues?.get(1) ?: "UTF-8"
                    val bytes = conn.inputStream.use { it.readNBytesCapped(MAX_BYTES) }
                    val html = String(bytes, charset(charset))
                    val page = if ("html" in type || type.isEmpty()) ReadableExtractor.extract(html, current) else ReadablePage(current, html)
                    require(page.text.length >= 200) { "Couldn't find readable text on that page (it may need a login or load content with JavaScript)." }
                    return@withContext Result.success(page.copy(title = page.title.ifBlank { current }))
                } finally {
                    conn.disconnect()
                }
            }
            error("Too many redirects.")
        }
    }

    private fun java.io.InputStream.readNBytesCapped(max: Int): ByteArray {
        val out = java.io.ByteArrayOutputStream()
        val buf = ByteArray(8192)
        while (out.size() < max) {
            val n = read(buf)
            if (n < 0) break
            out.write(buf, 0, n)
        }
        return out.toByteArray()
    }
}
