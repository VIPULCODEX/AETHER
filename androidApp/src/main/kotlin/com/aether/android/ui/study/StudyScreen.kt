package com.aether.android.ui.study

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.aether.android.ui.components.AetherBars
import com.aether.android.ui.components.AetherButton
import com.aether.android.ui.components.AetherCard
import com.aether.android.ui.components.AetherOutlinedButton
import com.aether.android.ui.components.AetherScaffold
import com.aether.android.ui.components.SectionHeader
import com.aether.android.ui.components.aetherTextFieldColors
import com.aether.android.ui.theme.AetherCoral
import com.aether.android.ui.theme.AetherEmerald
import com.aether.android.ui.theme.AetherOnAccent
import com.aether.android.ui.theme.AetherSky
import com.aether.android.ui.theme.AetherSurface2
import com.aether.android.ui.theme.AetherTextPrimary
import com.aether.android.ui.theme.AetherTextSecondary
import com.aether.core.engine.Grade
import com.aether.core.engine.NotesPipeline
import com.aether.core.engine.PracticeQuestion
import com.aether.core.engine.QuestionType
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.Icon
import android.os.StatFs
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import com.aether.android.data.LlmStatus
import com.aether.android.data.PdfTextExtractor
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import com.aether.android.ui.mindmap.MindMapView

@Composable
fun StudyScreen(viewModel: StudyViewModel) {
    val state by viewModel.uiState.collectAsState()
    val notebook = state.selected

    BackHandler(enabled = notebook != null) { if (state.playing != null) viewModel.stopPlaying() else viewModel.close() }

    AetherScaffold(title = notebook?.title ?: "Study") {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(top = AetherBars.TopContentPadding, bottom = AetherBars.BottomContentPadding),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item { ReaderCard(state, viewModel) }
            state.playing?.let { playing -> item(key = "player-${playing.id}") { VideoPlayerCard(playing, onClose = viewModel::stopPlaying) } }
            if (notebook == null) {
                notebookList(state, viewModel)
            } else {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                        StudyTab.values().forEach { tab ->
                            Pill(tab.label, selected = state.tab == tab) { viewModel.selectTab(tab) }
                        }
                    }
                }
                when (state.tab) {
                    StudyTab.SOURCES -> sourcesTab(state, viewModel)
                    StudyTab.ASK -> askTab(state, viewModel)
                    StudyTab.DIGEST -> digestTab(state, viewModel)
                    StudyTab.NOTES -> notesTab(state, vm)
                    StudyTab.PRACTICE -> practiceTab(state, viewModel)
                    StudyTab.MAP -> mapTab(state, viewModel)
                    StudyTab.CARDS -> cardsTab(state, viewModel)
                    StudyTab.QUIZ -> quizTab(state, viewModel)
                    StudyTab.ERRORS -> errorsTab(viewModel)
                }
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.notebookList(state: StudyUiState, vm: StudyViewModel) {
    item {
        var title by remember { mutableStateOf("") }
        AetherCard(modifier = Modifier.fillMaxWidth()) {
            Text("New notebook", style = MaterialTheme.typography.titleMedium, color = AetherTextPrimary)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = title, onValueChange = { title = it }, modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("e.g. Thermodynamics, Paper: Attention Is All You Need") },
                colors = aetherTextFieldColors()
            )
            Spacer(Modifier.height(10.dp))
            AetherButton("Create", modifier = Modifier.fillMaxWidth(), enabled = title.isNotBlank(), onClick = { vm.createNotebook(title); title = "" })
        }
    }
    if (state.notebooks.isEmpty()) {
        item {
            Text(
                "Add your notes, paper text or chapters as sources. Then ask questions with citations, get summaries, flashcards with spaced repetition, and quizzes. Everything runs on this device.",
                style = MaterialTheme.typography.bodyMedium, color = AetherTextSecondary
            )
        }
    }
    items(state.notebooks, key = { it.id }) { nb ->
        AetherCard(modifier = Modifier.fillMaxWidth(), onClick = { vm.open(nb.id) }) {
            Text(nb.title, style = MaterialTheme.typography.titleMedium, color = AetherTextPrimary)
        }
    }
    item { StorageCard(vm) }
}

private fun androidx.compose.foundation.lazy.LazyListScope.sourcesTab(state: StudyUiState, vm: StudyViewModel) {
    item {
        var title by remember { mutableStateOf("") }
        var text by remember { mutableStateOf("") }
        val context = LocalContext.current
        val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            if (uri != null) {
                val body = runCatching {
                    context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                }.getOrNull()
                if (!body.isNullOrBlank()) vm.addSource(uri.lastPathSegment?.substringAfterLast('/') ?: "Imported file", body)
            }
        }
        val scope = rememberCoroutineScope()
        val pdfPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            if (uri != null) scope.launch {
                vm.report(null)
                PdfTextExtractor.extract(context, uri)
                    .onSuccess { vm.addSource(uri.lastPathSegment?.substringAfterLast('/')?.removeSuffix(".pdf") ?: "PDF", it) }
                    .onFailure { vm.report(it.message ?: "Couldn't read that PDF") }
            }
        }
        var pendingVideo by remember { mutableStateOf<Pair<Uri, String>?>(null) }
        val videoPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            if (uri != null) {
                runCatching { context.contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION) }
                pendingVideo = uri to (uri.lastPathSegment?.substringAfterLast('/')?.substringBeforeLast('.') ?: "Video")
            }
        }
        val subtitlePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            val video = pendingVideo
            if (uri != null && video != null) {
                val text = runCatching { context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } }.getOrNull()
                vm.addVideoSource(video.first.toString(), video.second, text.orEmpty())
                pendingVideo = null
            }
        }
        AetherCard(modifier = Modifier.fillMaxWidth()) {
            Text("Add a source", style = MaterialTheme.typography.titleMedium, color = AetherTextPrimary)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = title, onValueChange = { title = it }, modifier = Modifier.fillMaxWidth(), placeholder = { Text("Title (optional)") }, colors = aetherTextFieldColors())
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = text, onValueChange = { text = it }, modifier = Modifier.fillMaxWidth(), minLines = 4, placeholder = { Text("Paste notes, an abstract, a chapter...") }, colors = aetherTextFieldColors())
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AetherButton("Add", enabled = text.isNotBlank(), onClick = { vm.addSource(title, text); title = ""; text = "" })
                AetherOutlinedButton("Text file", onClick = { picker.launch(arrayOf("text/*")) })
                AetherOutlinedButton("PDF", onClick = { pdfPicker.launch(arrayOf("application/pdf")) })
            }
            Spacer(Modifier.height(8.dp))
            val video = pendingVideo
            if (video == null) {
                AetherOutlinedButton("Video + subtitles", onClick = { videoPicker.launch(arrayOf("video/*")) })
            } else {
                Text("Video chosen: ${video.second}. Now pick its subtitle file (.srt or .vtt) so AETHER can read what is said.", style = MaterialTheme.typography.bodyMedium, color = AetherTextPrimary)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    AetherButton("Pick subtitles", onClick = { subtitlePicker.launch(arrayOf("*/*")) })
                    AetherOutlinedButton("Cancel", onClick = { pendingVideo = null })
                }
            }
            state.notice?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, style = MaterialTheme.typography.bodyMedium, color = AetherCoral)
            }
        }
    }
    item { ExamNotesCard(state, vm) }
    item { ModelCard(vm) }
    items(state.sources, key = { it.id }) { source ->
        AetherCard(modifier = Modifier.fillMaxWidth()) {
            Text(source.title, style = MaterialTheme.typography.titleMedium, color = AetherTextPrimary)
            Text(
                "${source.content.split(Regex("\\s+")).size} words" + (source.url?.let { " · ${it.removePrefix("https://").removePrefix("http://").take(40)}" } ?: ""),
                style = MaterialTheme.typography.labelSmall, color = AetherTextSecondary
            )
            Spacer(Modifier.height(4.dp))
            Text(source.content.take(160), style = MaterialTheme.typography.bodyMedium, color = AetherTextSecondary)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                if (source.mediaUri != null) {
                    Text("Play video", style = MaterialTheme.typography.labelLarge, color = AetherSky, modifier = Modifier.clickable { vm.play(source.id) })
                }
                Text("Remove", style = MaterialTheme.typography.labelLarge, color = AetherCoral, modifier = Modifier.clickable { vm.deleteSource(source.id) })
            }
        }
    }
    item {
        state.selected?.let { nb ->
            AetherOutlinedButton("Delete notebook", onClick = { vm.deleteNotebook(nb.id) })
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.askTab(state: StudyUiState, vm: StudyViewModel) {
    item {
        var question by remember { mutableStateOf("") }
        val llmStatus by vm.llmStatus.collectAsState()
        Column {
            Text(
                if (llmStatus is LlmStatus.Ready || (llmStatus is LlmStatus.NotLoaded && vm.modelBytes > 0)) "On-device model on: answers are phrased by the model, grounded in your sources." else "Model off: answers are extracted straight from your sources. Load a model in Sources for fluent answers.",
                style = MaterialTheme.typography.labelSmall, color = AetherTextSecondary
            )
            Spacer(Modifier.height(8.dp))
            if (state.sources.isEmpty()) {
                Text("Add a source first. Answers come only from your sources, with citations.", style = MaterialTheme.typography.bodyMedium, color = AetherTextSecondary)
                Spacer(Modifier.height(10.dp))
            }
            OutlinedTextField(value = question, onValueChange = { question = it }, modifier = Modifier.fillMaxWidth(), placeholder = { Text("Ask about your sources") }, colors = aetherTextFieldColors())
            Spacer(Modifier.height(8.dp))
            AetherButton(if (state.thinking) "Thinking..." else "Ask", modifier = Modifier.fillMaxWidth(), enabled = question.isNotBlank() && state.sources.isNotEmpty() && !state.thinking, onClick = { vm.ask(question); question = "" })
        }
    }
    items(state.chat.asReversed()) { turn ->
        AetherCard(modifier = Modifier.fillMaxWidth()) {
            Text(turn.question, style = MaterialTheme.typography.labelLarge, color = AetherSky)
            Spacer(Modifier.height(6.dp))
            Text(turn.answer.text, style = MaterialTheme.typography.bodyMedium, color = AetherTextPrimary)
            turn.answer.citations.forEachIndexed { i, c ->
                Spacer(Modifier.height(8.dp))
                Text("[${i + 1}] ${c.sourceTitle}: \"${c.snippet.take(110)}${if (c.snippet.length > 110) "..." else ""}\"", style = MaterialTheme.typography.labelSmall, color = AetherTextSecondary)
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.mapTab(state: StudyUiState, vm: StudyViewModel) {
    item {
        val title = state.selected?.title.orEmpty()
        val nodes = remember(state.sources, title) { vm.mindMapEngine.fromSources(title, state.sources) }
        if (nodes.size <= 1) {
            Text("Add a source first. The map is built from its key topics and sentences.", style = MaterialTheme.typography.bodyMedium, color = AetherTextSecondary)
        } else {
            MindMapView(nodes = nodes, engine = vm.mindMapEngine, modifier = Modifier.fillMaxWidth().height(480.dp))
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.cardsTab(state: StudyUiState, vm: StudyViewModel) {
    val due = state.dueCards
    item {
        Column {
            Text("${state.cards.size} cards · ${due.size} due", style = MaterialTheme.typography.titleMedium, color = AetherTextPrimary)
            Spacer(Modifier.height(8.dp))
            AetherOutlinedButton("Generate cards from sources", onClick = vm::generateCards)
        }
    }
    val card = due.firstOrNull()
    if (card == null) {
        item {
            Text(
                if (state.cards.isEmpty()) "No cards yet. Generate them from your sources." else "All caught up. Come back when more cards are due.",
                style = MaterialTheme.typography.bodyMedium, color = AetherTextSecondary
            )
        }
    } else {
        item {
            AetherCard(modifier = Modifier.fillMaxWidth()) {
                Text(card.front, style = MaterialTheme.typography.titleMedium, color = AetherTextPrimary)
                if (state.showAnswer) {
                    Spacer(Modifier.height(12.dp))
                    Text(card.back, style = MaterialTheme.typography.bodyMedium, color = AetherEmerald)
                }
            }
        }
        item {
            if (!state.showAnswer) {
                AetherButton("Show answer", modifier = Modifier.fillMaxWidth(), onClick = vm::revealAnswer)
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Grade.values().forEach { g ->
                        AetherButton(g.label, modifier = Modifier.weight(1f), accentColor = if (g == Grade.AGAIN) AetherCoral else AetherSky, onClick = { vm.grade(card, g) })
                    }
                }
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.quizTab(state: StudyUiState, vm: StudyViewModel) {
    val quiz = state.quiz
    if (quiz == null || quiz.questions.isEmpty()) {
        item {
            Column {
                Text(
                    if (quiz != null) "Not enough cloze cards for a quiz yet. Add more source text and generate cards." else "Multiple-choice questions built from your cards.",
                    style = MaterialTheme.typography.bodyMedium, color = AetherTextSecondary
                )
                Spacer(Modifier.height(10.dp))
                AetherButton("Start quiz", modifier = Modifier.fillMaxWidth(), enabled = state.cards.isNotEmpty(), onClick = vm::startQuiz)
            }
        }
    } else if (quiz.finished) {
        item {
            AetherCard(modifier = Modifier.fillMaxWidth()) {
                Text("Score: ${quiz.score} / ${quiz.questions.size}", style = MaterialTheme.typography.titleMedium, color = AetherTextPrimary)
                Spacer(Modifier.height(10.dp))
                AetherButton("New quiz", onClick = vm::startQuiz)
            }
        }
    } else {
        val q = quiz.questions[quiz.index]
        item {
            Column {
                SectionHeader("Question ${quiz.index + 1} of ${quiz.questions.size}")
                Spacer(Modifier.height(6.dp))
                Text(q.prompt, style = MaterialTheme.typography.titleMedium, color = AetherTextPrimary)
            }
        }
        items(q.options.size) { i ->
            val picked = quiz.picked
            val color = when {
                picked == null -> AetherSurface2
                i == q.correctIndex -> AetherEmerald
                i == picked -> AetherCoral
                else -> AetherSurface2
            }
            Text(
                q.options[i], style = MaterialTheme.typography.bodyMedium,
                color = if (color == AetherSurface2) AetherTextPrimary else AetherOnAccent,
                modifier = Modifier.fillMaxWidth().background(color, RoundedCornerShape(16.dp)).clickable { vm.pickOption(i) }.padding(16.dp)
            )
        }
        if (quiz.picked != null) {
            item { AetherButton(if (quiz.index + 1 < quiz.questions.size) "Next" else "Finish", modifier = Modifier.fillMaxWidth(), onClick = vm::nextQuestion) }
        }
    }
}

@Composable
private fun Pill(label: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        label, style = MaterialTheme.typography.labelLarge, maxLines = 1, softWrap = false,
        color = if (selected) AetherOnAccent else AetherTextPrimary,
        modifier = Modifier
            .background(if (selected) AetherSky else AetherSurface2, RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    )
}

@Composable
private fun ModelCard(vm: StudyViewModel) {
    val status by vm.llmStatus.collectAsState()
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) vm.importModel(uri, uri.lastPathSegment?.substringAfterLast('/') ?: "On-device model")
    }
    val hasModel = status is LlmStatus.Ready || vm.modelBytes > 0
    AetherCard(modifier = Modifier.fillMaxWidth()) {
        Text("On-device AI", style = MaterialTheme.typography.titleMedium, color = AetherTextPrimary)
        Spacer(Modifier.height(4.dp))
        Text(
            when (val s = status) {
                LlmStatus.NotLoaded -> if (hasModel) "Installed on this device. It loads when needed and is released from memory in the background."
                else "Download a small AI model once (about 547 MB, Wi-Fi recommended). After that, answers, summaries, explanations and practice questions are written by the model, fully offline. Without it, AETHER still answers straight from your sources."
                is LlmStatus.Downloading -> "Downloading the model: ${s.percent}%. You can leave this screen; if the connection drops, tap Download again to resume."
                LlmStatus.Loading -> "Getting the model ready..."
                is LlmStatus.Ready -> "Ready: ${s.modelName}"
                is LlmStatus.Failed -> "Problem: ${s.message}"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = if (status is LlmStatus.Failed) AetherCoral else AetherTextSecondary
        )
        (status as? LlmStatus.Downloading)?.let {
            Spacer(Modifier.height(10.dp))
            androidx.compose.material3.LinearProgressIndicator(
                progress = { it.percent / 100f },
                modifier = Modifier.fillMaxWidth().height(6.dp),
                color = AetherSky,
                trackColor = AetherSurface2
            )
        }
        Spacer(Modifier.height(10.dp))
        val busy = status is LlmStatus.Downloading || status is LlmStatus.Loading
        if (!hasModel) {
            AetherButton("Download AI model (547 MB)", modifier = Modifier.fillMaxWidth(), enabled = !busy, onClick = vm::downloadModel)
            Spacer(Modifier.height(8.dp))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            AetherOutlinedButton("Use my own file", onClick = { picker.launch(arrayOf("*/*")) })
            if (hasModel) AetherOutlinedButton("Remove", onClick = vm::removeModel)
        }
        Spacer(Modifier.height(6.dp))
        Text(
            "Default: Qwen2.5-0.5B (Apache-2.0). It is small, so expect simpler writing than a big cloud model. Advanced: pick any MediaPipe .task file (Gemma 3 1B writes better but needs a Hugging Face login to download).",
            style = MaterialTheme.typography.labelSmall, color = AetherTextSecondary
        )
    }
}

@Composable
private fun ReaderCard(state: StudyUiState, vm: StudyViewModel) {
    if (!state.readerOpen) {
        Row(
            Modifier
                .fillMaxWidth()
                .background(AetherSurface2, RoundedCornerShape(16.dp))
                .clickable { vm.openReader() }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Language, contentDescription = "Read a web page", tint = AetherSky)
            Spacer(Modifier.width(12.dp))
            Text("Read a web page", style = MaterialTheme.typography.labelLarge, color = AetherTextPrimary)
        }
        return
    }
    AetherCard(modifier = Modifier.fillMaxWidth()) {
        Text("Read a web page", style = MaterialTheme.typography.titleMedium, color = AetherTextPrimary)
        Text(
            "Paste a link, or share one to AETHER from your browser. The page is fetched once, cleaned to readable text, saved to your notebook, and turned into an interactive digest. This is the only step that uses the network.",
            style = MaterialTheme.typography.bodyMedium, color = AetherTextSecondary
        )
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = state.readerUrl, onValueChange = vm::setReaderUrl, modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("https://...") }, singleLine = true, colors = aetherTextFieldColors()
        )
        state.notice?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, style = MaterialTheme.typography.bodyMedium, color = AetherCoral)
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            AetherButton(if (state.readerBusy) "Reading..." else "Read & summarize", enabled = state.readerUrl.isNotBlank() && !state.readerBusy, onClick = { vm.readPage(state.readerUrl) })
            AetherOutlinedButton("Close", onClick = vm::closeReader)
        }
    }
}

@Composable
private fun ExamNotesCard(state: StudyUiState, vm: StudyViewModel) {
    val notebook = state.selected ?: return
    var notes by remember(notebook.id, notebook.examNotes) { mutableStateOf(notebook.examNotes.orEmpty()) }
    AetherCard(modifier = Modifier.fillMaxWidth()) {
        Text("How is this exam asked?", style = MaterialTheme.typography.titleMedium, color = AetherTextPrimary)
        Text(
            "Describe the pattern in your own words. Explanations and practice questions are written to match it.",
            style = MaterialTheme.typography.bodyMedium, color = AetherTextSecondary
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = notes, onValueChange = { notes = it }, modifier = Modifier.fillMaxWidth(), minLines = 3,
            placeholder = { Text("e.g. GATE CS: mostly numericals on cache mapping, 2-mark traps on pipeline hazards, MSQ on replacement policies") },
            colors = aetherTextFieldColors()
        )
        Spacer(Modifier.height(8.dp))
        AetherButton("Save", enabled = notes.trim() != notebook.examNotes.orEmpty(), onClick = { vm.saveExamNotes(notes) })
    }
}

private fun LazyListScope.digestTab(state: StudyUiState, vm: StudyViewModel) {
    if (state.sources.isEmpty()) {
        item { Text("Add a source (or read a web page) and its digest appears here.", style = MaterialTheme.typography.bodyMedium, color = AetherTextSecondary) }
        return
    }
    item {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
            state.sources.forEach { src -> Pill(src.title.take(26), selected = state.digestSourceId == src.id) { vm.buildDigest(src.id) } }
        }
    }
    val digest = state.digest
    if (digest == null) {
        item {
            Text(
                if (state.thinking) "Reading..." else "Pick a source above to build its digest.",
                style = MaterialTheme.typography.bodyMedium, color = AetherTextSecondary
            )
        }
        return
    }
    item {
        AetherCard(modifier = Modifier.fillMaxWidth()) {
            Text(
                "TL;DR · ${digest.readMinutes} min read" + if (digest.byModel) " · written by the on-device model" else "",
                style = MaterialTheme.typography.labelSmall, color = AetherTextSecondary
            )
            Spacer(Modifier.height(6.dp))
            Text(digest.tldr, style = MaterialTheme.typography.bodyMedium, color = AetherTextPrimary)
        }
    }
    if (digest.outline.size > 1) {
        item {
            Column {
                Text("Outline", style = MaterialTheme.typography.labelSmall, color = AetherTextSecondary)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                    digest.outline.take(12).forEach { h ->
                        Text(h, style = MaterialTheme.typography.labelSmall, color = AetherTextPrimary, modifier = Modifier.background(AetherSurface2, RoundedCornerShape(50)).padding(horizontal = 10.dp, vertical = 6.dp))
                    }
                }
            }
        }
    }
    item { SectionHeader("Key points", "Tap a point to expand it.") }
    items(digest.points.size) { i ->
        val point = digest.points[i]
        var expanded by remember(point.text) { mutableStateOf(false) }
        AetherCard(modifier = Modifier.fillMaxWidth(), onClick = { expanded = !expanded }) {
            Text("${i + 1}. ${point.text}", style = MaterialTheme.typography.bodyMedium, color = AetherTextPrimary)
            if (expanded) {
                if (point.context.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(point.context, style = MaterialTheme.typography.bodyMedium, color = AetherTextSecondary)
                }
                Spacer(Modifier.height(10.dp))
                val explanation = state.explanations[point.text]
                when {
                    point.text !in state.explanations -> AetherOutlinedButton("Explain simply", onClick = { vm.explain(point) })
                    explanation == null -> Text("Thinking...", style = MaterialTheme.typography.labelSmall, color = AetherTextSecondary)
                    else -> {
                        Text(explanation, style = MaterialTheme.typography.bodyMedium, color = AetherEmerald)
                        Text("AI-written. Check against your source.", style = MaterialTheme.typography.labelSmall, color = AetherTextSecondary)
                    }
                }
            }
        }
    }
    if (digest.terms.isNotEmpty()) {
        item { SectionHeader("Key terms", "Tap a term for its definition.") }
        items(digest.terms.size) { i ->
            val term = digest.terms[i]
            var open by remember(term.term) { mutableStateOf(false) }
            AetherCard(modifier = Modifier.fillMaxWidth(), onClick = { open = !open }) {
                Text(term.term, style = MaterialTheme.typography.titleMedium, color = AetherSky)
                if (open) {
                    Spacer(Modifier.height(6.dp))
                    Text(term.definition, style = MaterialTheme.typography.bodyMedium, color = AetherTextPrimary)
                }
            }
        }
    }
    item {
        AetherButton("Make flashcards from my sources", modifier = Modifier.fillMaxWidth(), onClick = { vm.generateCards(); vm.selectTab(StudyTab.CARDS) })
    }
}

private fun LazyListScope.practiceTab(state: StudyUiState, vm: StudyViewModel) {
    item {
        var topic by remember { mutableStateOf("") }
        Column {
            Text(
                state.selected?.examNotes?.takeIf { it.isNotBlank() }?.let { "Pattern: $it" }
                    ?: "Tip: describe the exam pattern in Sources and questions will match it.",
                style = MaterialTheme.typography.labelSmall, color = AetherTextSecondary
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = topic, onValueChange = { topic = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, placeholder = { Text("Topic (optional), e.g. cache mapping") }, colors = aetherTextFieldColors())
            Spacer(Modifier.height(8.dp))
            AetherButton(if (state.thinking) "Writing questions..." else "Generate practice questions", modifier = Modifier.fillMaxWidth(), enabled = state.sources.isNotEmpty() && !state.thinking, onClick = { vm.generatePractice(topic) })
        }
    }
    state.practiceMessage?.let { item { Text(it, style = MaterialTheme.typography.bodyMedium, color = AetherTextSecondary) } }
    state.practice?.let { questions ->
        items(questions.size) { i -> PracticeCard(i + 1, questions[i]) { yours -> vm.recordMistake(state.practiceTopic, questions[i].prompt, questions[i].answerText(), yours) } }
    }
}

@Composable
private fun PracticeCard(number: Int, q: PracticeQuestion, onWrong: (String) -> Unit) {
    var selected by remember(q) { mutableStateOf(emptySet<Int>()) }
    var typed by remember(q) { mutableStateOf("") }
    var checked by remember(q) { mutableStateOf(false) }
    val correct = q.isCorrect(selected, typed.toDoubleOrNull())
    AetherCard(modifier = Modifier.fillMaxWidth()) {
        Text("Q$number · ${q.type.label}" + if (q.type == QuestionType.MSQ) " (select all that apply)" else "", style = MaterialTheme.typography.labelSmall, color = AetherTextSecondary)
        Text("Written by a small on-device model: verify numerical answers before trusting them.", style = MaterialTheme.typography.labelSmall, color = AetherCoral)
        Spacer(Modifier.height(6.dp))
        Text(q.prompt, style = MaterialTheme.typography.titleMedium, color = AetherTextPrimary)
        Spacer(Modifier.height(10.dp))
        if (q.type == QuestionType.NAT) {
            OutlinedTextField(
                value = typed, onValueChange = { typed = it }, enabled = !checked, singleLine = true,
                placeholder = { Text("Your answer") }, colors = aetherTextFieldColors(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )
        } else {
            q.options.forEachIndexed { idx, option ->
                val bg = when {
                    checked && idx in q.correct -> AetherEmerald
                    checked && idx in selected -> AetherCoral
                    idx in selected -> AetherSky
                    else -> AetherSurface2
                }
                Text(
                    "${'A' + idx}) $option", style = MaterialTheme.typography.bodyMedium,
                    color = if (bg == AetherSurface2) AetherTextPrimary else AetherOnAccent,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp)
                        .background(bg, RoundedCornerShape(14.dp))
                        .clickable(enabled = !checked) {
                            selected = if (q.type == QuestionType.MCQ) setOf(idx) else if (idx in selected) selected - idx else selected + idx
                        }
                        .padding(14.dp)
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        if (!checked) {
            AetherButton("Check", enabled = selected.isNotEmpty() || typed.isNotBlank(), onClick = {
                checked = true
                if (!correct) onWrong(if (q.type == QuestionType.NAT) typed else selected.sorted().joinToString(", ") { "${'A' + it}" })
            })
        } else {
            Text(if (correct) "Correct" else "Not quite", style = MaterialTheme.typography.titleMedium, color = if (correct) AetherEmerald else AetherCoral)
            if (q.type == QuestionType.NAT) Text("Answer: ${q.numeric}", style = MaterialTheme.typography.bodyMedium, color = AetherTextPrimary)
            if (q.explanation.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(q.explanation, style = MaterialTheme.typography.bodyMedium, color = AetherTextSecondary)
            }
        }
    }
}

@Composable
private fun StorageCard(vm: StudyViewModel) {
    val storage by vm.storage.collectAsState()
    val context = LocalContext.current
    val free = remember(storage) { runCatching { StatFs(context.filesDir.path).availableBytes }.getOrDefault(0L) }
    val modelBytes = vm.modelBytes
    val total = storage.sumOf { it.bytes } + modelBytes
    AetherCard(modifier = Modifier.fillMaxWidth()) {
        Text("Storage", style = MaterialTheme.typography.titleMedium, color = AetherTextPrimary)
        Text(
            "Study Hub uses about ${formatBytes(total)} on this device. ${formatBytes(free)} free.",
            style = MaterialTheme.typography.bodyMedium, color = AetherTextSecondary
        )
        storage.forEach { nb ->
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(nb.title, style = MaterialTheme.typography.bodyMedium, color = AetherTextPrimary)
                    Text("${nb.sourceCount} sources · ${formatBytes(nb.bytes)} · ${nb.cardCount} cards", style = MaterialTheme.typography.labelSmall, color = AetherTextSecondary)
                }
                Text("Delete", style = MaterialTheme.typography.labelLarge, color = AetherCoral, modifier = Modifier.clickable { vm.deleteNotebook(nb.notebookId) }.padding(8.dp))
            }
        }
        if (modelBytes > 0) {
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("On-device model", style = MaterialTheme.typography.bodyMedium, color = AetherTextPrimary)
                    Text(formatBytes(modelBytes), style = MaterialTheme.typography.labelSmall, color = AetherTextSecondary)
                }
                Text("Delete", style = MaterialTheme.typography.labelLarge, color = AetherCoral, modifier = Modifier.clickable { vm.removeModel() }.padding(8.dp))
            }
        }
    }
}

private fun formatBytes(bytes: Long): String = when {
    bytes >= 1_000_000_000 -> "%.1f GB".format(bytes / 1_000_000_000.0)
    bytes >= 1_000_000 -> "%.1f MB".format(bytes / 1_000_000.0)
    bytes >= 1_000 -> "%.0f KB".format(bytes / 1_000.0)
    else -> "$bytes B"
}


private fun LazyListScope.notesTab(state: StudyUiState, vm: StudyViewModel) {
    if (state.sources.isEmpty()) {
        item { Text("Add a PDF or text source and structured notes appear here.", style = MaterialTheme.typography.bodyMedium, color = AetherTextSecondary) }
        return
    }
    item {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Pick a source. On-device notes are instant and free; Deep notes use your own Claude key.", style = MaterialTheme.typography.bodyMedium, color = AetherTextSecondary)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                state.sources.forEach { src ->
                    Pill(src.title.take(26), selected = state.notesSourceId == src.id) { vm.buildNotes(src.id, deep = false) }
                }
            }
            if (vm.hasClaudeKey) {
                Text("Deep notes (cloud): sends this source's text, one section at a time, to Anthropic with your key.", style = MaterialTheme.typography.labelSmall, color = AetherTextSecondary)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                    state.sources.forEach { src -> AetherOutlinedButton("Deep: ${src.title.take(20)}", onClick = { vm.buildNotes(src.id, deep = true) }) }
                }
            }
        }
    }
    state.notesBusy?.let { busy -> item { Text(busy, style = MaterialTheme.typography.bodyMedium, color = AetherSky) } }
    state.notice?.let { n -> item { Text(n, style = MaterialTheme.typography.labelSmall, color = AetherCoral) } }
    if (state.notes.isEmpty()) return
    item {
        val sheet = NotesPipeline.revisionSheet(state.notes)
        val context = LocalContext.current
        AetherCard(modifier = Modifier.fillMaxWidth()) {
            Text("One-page revision sheet", style = MaterialTheme.typography.titleMedium, color = AetherEmerald)
            Text(
                (if (state.notesDeep) "Cloud-extracted" else "On-device extracted") + " · checked against the source pages · ${sheet.length}/2800 characters",
                style = MaterialTheme.typography.labelSmall, color = AetherTextSecondary
            )
            Spacer(Modifier.height(8.dp))
            Text(sheet, style = MaterialTheme.typography.bodyMedium, color = AetherTextPrimary)
            Spacer(Modifier.height(10.dp))
            AetherOutlinedButton("Copy sheet", onClick = {
                val cm = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                cm.setPrimaryClip(android.content.ClipData.newPlainText("AETHER revision sheet", sheet))
                vm.report("Revision sheet copied.")
            })
        }
    }
    item { SectionHeader("Detailed notes", "⚠ marks anything the source pages didn't confirm. Check those against the page.") }
    items(state.notes.size) { i ->
        val n = state.notes[i]
        AetherCard(modifier = Modifier.fillMaxWidth()) {
            Text("${n.topic}  ·  ${n.pageLabel}", style = MaterialTheme.typography.titleMedium, color = if (n.lowConfidence) AetherCoral else AetherSky)
            if (n.summary.isNotBlank()) { Spacer(Modifier.height(4.dp)); Text(n.summary, style = MaterialTheme.typography.bodyMedium, color = AetherTextPrimary) }
            n.definitions.forEach { Spacer(Modifier.height(4.dp)); Text("Definition: $it", style = MaterialTheme.typography.bodyMedium, color = AetherTextPrimary) }
            n.formulas.forEach { Spacer(Modifier.height(4.dp)); Text("Formula: $it", style = MaterialTheme.typography.bodyMedium, color = AetherEmerald) }
            if (n.flow.isNotBlank()) { Spacer(Modifier.height(4.dp)); Text("Flow: ${n.flow}", style = MaterialTheme.typography.bodyMedium, color = AetherTextPrimary) }
            n.traps.forEach { Spacer(Modifier.height(4.dp)); Text("Trap: $it", style = MaterialTheme.typography.bodyMedium, color = AetherCoral) }
            if (n.example.isNotBlank()) { Spacer(Modifier.height(4.dp)); Text("Example: ${n.example}", style = MaterialTheme.typography.bodyMedium, color = AetherTextSecondary) }
            n.flags.forEach { Spacer(Modifier.height(2.dp)); Text("⚠ $it", style = MaterialTheme.typography.labelSmall, color = AetherCoral) }
        }
    }
}


private fun LazyListScope.errorsTab(vm: StudyViewModel) {
    item {
        val all by vm.mistakes.collectAsState()
        val open = all.filter { !it.resolved }
        val now = System.currentTimeMillis()
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                "Every wrong answer lands here with its topic and comes back after 1, 3, 7 and 21 days until you get it right 4 times in a row.",
                style = MaterialTheme.typography.bodyMedium, color = AetherTextSecondary
            )
            if (open.isEmpty()) Text("Nothing here yet. Wrong answers in Practice and Quiz are saved automatically.", style = MaterialTheme.typography.bodyMedium, color = AetherTextSecondary)
            open.sortedBy { it.dueAt }.forEach { m ->
                var shown by remember(m.id) { mutableStateOf(false) }
                val due = m.dueAt <= now
                AetherCard(modifier = Modifier.fillMaxWidth()) {
                    Text(m.topic + if (due) " · due now" else " · in ${((m.dueAt - now) / 86_400_000L) + 1} day(s)", style = MaterialTheme.typography.labelSmall, color = if (due) AetherCoral else AetherTextSecondary)
                    Spacer(Modifier.height(4.dp))
                    Text(m.question, style = MaterialTheme.typography.titleMedium, color = AetherTextPrimary)
                    if (shown) {
                        Spacer(Modifier.height(6.dp))
                        Text("Answer: ${m.correctAnswer}", style = MaterialTheme.typography.bodyMedium, color = AetherEmerald)
                        if (m.yourAnswer.isNotBlank()) Text("You said: ${m.yourAnswer}", style = MaterialTheme.typography.bodyMedium, color = AetherCoral)
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AetherButton("Got it", onClick = { vm.reviewMistake(m, true) })
                            AetherOutlinedButton("Still wrong", onClick = { vm.reviewMistake(m, false) })
                        }
                    } else {
                        Spacer(Modifier.height(8.dp))
                        AetherOutlinedButton("Show answer", onClick = { shown = true })
                    }
                }
            }
        }
    }
}
