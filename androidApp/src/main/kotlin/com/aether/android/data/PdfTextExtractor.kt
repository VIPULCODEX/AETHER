package com.aether.android.data

import android.content.Context
import android.net.Uri
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val MAX_CHARS = 400_000

/** Pulls selectable text out of a PDF on-device. Scanned/image-only PDFs have none; those need OCR, which this doesn't do. */
object PdfTextExtractor {

    suspend fun extract(context: Context, uri: Uri): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            PDFBoxResourceLoader.init(context.applicationContext)
            val stream = context.contentResolver.openInputStream(uri) ?: error("Couldn't open the PDF")
            stream.use { input ->
                PDDocument.load(input).use { doc ->
                    // One form feed between pages so notes can cite page numbers; other consumers treat it as whitespace.
                    val stripper = PDFTextStripper()
                    val text = (1..doc.numberOfPages).joinToString("\u000c") { page ->
                        stripper.startPage = page
                        stripper.endPage = page
                        stripper.getText(doc).replace(Regex("[ \\t]+\\n"), "\n").trim()
                    }.trim('\u000c', ' ', '\n')
                    require(text.isNotBlank()) { "This PDF has no selectable text (it looks scanned)." }
                    text.take(MAX_CHARS)
                }
            }
        }
    }
}
