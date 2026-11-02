package com.aether.android.data

import org.jsoup.Jsoup
import org.jsoup.nodes.Element

data class ReadablePage(val title: String, val text: String)

/**
 * Reader-mode extraction: strips navigation, ads and chrome, finds the main
 * article body, and emits clean text with "## Heading" markers (which the
 * Study Hub's digest turns into an outline). Pure HTML-in / text-out so it
 * has no Android or network dependency.
 */
object ReadableExtractor {

    private const val NOISE = "script, style, noscript, nav, footer, aside, form, iframe, svg, button, header nav, " +
        "[role=navigation], [role=banner], [aria-hidden=true], .advert, .ads, .cookie, .sidebar, .comments, .share"

    /** Pulls the first http(s) link out of arbitrary shared text, or null. */
    fun findUrl(shared: String): String? =
        Regex("https?://[^\\s<>\"\']+").find(shared)?.value?.trimEnd('.', ',', ')', ']')

    fun extract(html: String, baseUrl: String = ""): ReadablePage {
        val doc = Jsoup.parse(html, baseUrl)
        val title = doc.select("meta[property=og:title]").attr("content").ifBlank { doc.title() }.trim()
        doc.select(NOISE).remove()

        val root = pickRoot(doc.body() ?: return ReadablePage(title, ""))
        val blocks = mutableListOf<String>()
        root.select("h1, h2, h3, h4, p, li, blockquote, pre").forEach { el ->
            val text = el.text().replace(Regex("\\[(?:\\d+|[a-z]|citation needed)]"), "").replace(Regex("\\s{2,}"), " ").trim()
            if (text.isEmpty()) return@forEach
            // Skip link-farm blocks (menus, "related articles") but keep headings regardless.
            val linkRatio = el.select("a").toList().sumOf { it.text().length }.toDouble() / text.length
            when {
                el.tagName().startsWith("h") && el.tagName().length == 2 -> if (text.length in 2..140) blocks += "## $text"
                el.tagName() == "pre" -> blocks += text.take(600)
                text.length >= 25 && linkRatio < 0.6 -> blocks += if (el.tagName() == "li") "• $text" else text
            }
        }
        return ReadablePage(title, blocks.joinToString("\n\n").trim())
    }

    /** `<article>`/`<main>` when substantial, else the element holding the most paragraph text. */
    private fun pickRoot(body: Element): Element {
        listOf("article", "main", "[role=main]").forEach { sel ->
            body.selectFirst(sel)?.takeIf { it.text().length > 400 }?.let { return it }
        }
        return body.select("div, section, td").maxByOrNull { el ->
            el.children().toList().filter { it.tagName() == "p" }.sumOf { it.text().length }
        }?.takeIf { it.select("p").isNotEmpty() } ?: body
    }
}
