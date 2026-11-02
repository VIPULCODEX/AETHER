package com.aether.android.ui.study

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aether.android.data.ApiKeyStore
import com.aether.android.data.ClaudeNotesClient
import com.aether.android.data.LlmStatus
import com.aether.android.data.OnDeviceLlm
import com.aether.android.data.ShareInbox
import com.aether.android.data.ReadableExtractor
import com.aether.android.data.WebPageReader
import com.aether.core.data.NotebookStorage
import com.aether.core.data.PersonalizationRepository
import com.aether.core.data.StudyRepository
import com.aether.core.engine.SubtitleParser
import com.aether.core.model.BehaviorEventType
import com.aether.core.engine.Digest
import com.aether.core.engine.DigestEngine
import com.aether.core.engine.ExamEngine
import com.aether.core.engine.Grade
import com.aether.core.engine.MindMapEngine
import com.aether.core.engine.NotesPipeline
import com.aether.core.engine.StructuredNote
import com.aether.core.engine.PracticeQuestion
import com.aether.core.engine.QuizQuestion
import com.aether.core.engine.SpacedRepetition
import com.aether.core.engine.StudyAnswer
import com.aether.core.engine.StudyEngine
import com.aether.core.model.Flashcard
import com.aether.core.model.Notebook
import com.aether.core.model.StudyMistake
import com.aether.core.model.StudySource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class StudyTab(val label: String) {
    SOURCES("Sources"), ASK("Ask"), DIGEST("Digest"), NOTES("Notes"), PRACTICE("Practice"), MAP("Map"), CARDS("Cards"), QUIZ("Quiz"), ERRORS("Errors")
}

data class ChatTurn(val question: String, val answer: StudyAnswer)

data class QuizState(val questions: List<QuizQuestion>, val index: Int = 0, val picked: Int? = null, val score: Int = 0) {
    val finished: Boolean get() = index >= questions.size
}

data class StudyUiState(
    val notebooks: List<Notebook> = emptyList(),
    val selected: Notebook? = null,
    val sources: List<StudySource> = emptyList(),
    val cards: List<Flashcard> = emptyList(),
    val tab: StudyTab = StudyTab.SOURCES,
    val chat: List<ChatTurn> = emptyList(),
    val digest: Digest? = null,
    val digestSourceId: String? = null,
    /** Point text -> the model's plain-language explanation (or null while it is being written). */
    val explanations: Map<String, String?> = emptyMap(),
    val practice: List<PracticeQuestion>? = null,
    val practiceMessage: String? = null,
    val showAnswer: Boolean = false,
    val quiz: QuizState? = null,
    val thinking: Boolean = false,
    val notice: String? = null,
    val readerOpen: Boolean = false,
    val readerBusy: Boolean = false,
    val readerUrl: String = "",
    val playing: StudySource? = null,
    val notes: List<StructuredNote> = emptyList(),
    val notesSourceId: String? = null,
    val notesBusy: String? = null,
    val notesDeep: Boolean = false,
    val practiceTopic: String = ""
) {
    val dueCards: List<Flashcard> get() = cards.filter { it.dueAt <= System.currentTimeMillis() }
}

private const val MAX_DEEP_CHUNKS = 40

@OptIn(ExperimentalCoroutinesApi::class)
class StudyViewModel(
    private val repository: StudyRepository,
    private val engine: StudyEngine,
    val mindMapEngine: MindMapEngine,
    private val llm: OnDeviceLlm,
    private val digestEngine: DigestEngine,
    private val examEngine: ExamEngine,
    private val personalizationRepository: PersonalizationRepository,
    private val apiKeyStore: ApiKeyStore,
    private val claude: ClaudeNotesClient
) : ViewModel() {

    val mistakes: StateFlow<List<StudyMistake>> = repository.observeMistakes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Keeps a wrong answer in the error notebook, tagged by topic, so it resurfaces on a spaced schedule. */
    fun recordMistake(topic: String, question: String, correct: String, yours: String) {
        val nb = selectedId.value ?: return
        viewModelScope.launch {
            repository.recordMistake(nb, topic.ifBlank { uiState.value.selected?.title ?: "General" }, question, correct, yours)
            recordStudyActivity()
        }
    }

    fun reviewMistake(m: StudyMistake, gotItRight: Boolean) {
        viewModelScope.launch { repository.reviewMistake(m, gotItRight) }
    }

    fun deleteMistake(id: String) {
        viewModelScope.launch { repository.deleteMistake(id) }
    }

    val hasClaudeKey: Boolean get() = apiKeyStore.getClaudeKey() != null

    val llmStatus: StateFlow<LlmStatus> = llm.status
    val modelBytes: Long get() = llm.modelBytes

    val storage: StateFlow<List<NotebookStorage>> = repository.observeStorage()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val selectedId = MutableStateFlow<String?>(null)
    private val local = MutableStateFlow(Local())

    private data class Local(
        val tab: StudyTab = StudyTab.SOURCES,
        val chat: List<ChatTurn> = emptyList(),
        val digest: Digest? = null,
        val digestSourceId: String? = null,
        val explanations: Map<String, String?> = emptyMap(),
        val practice: List<PracticeQuestion>? = null,
        val practiceMessage: String? = null,
        val showAnswer: Boolean = false,
        val quiz: QuizState? = null,
        val thinking: Boolean = false,
        val notice: String? = null,
        val readerOpen: Boolean = false,
        val readerBusy: Boolean = false,
        val readerUrl: String = "",
        val playingId: String? = null,
        val practiceTopic: String = "",
        val notes: List<StructuredNote> = emptyList(),
        val notesSourceId: String? = null,
        val notesBusy: String? = null,
        val notesDeep: Boolean = false
    )

    private val data = selectedId.flatMapLatest { id ->
        if (id == null) {
            combine(repository.observeNotebooks(), flowOf(emptyList<StudySource>()), flowOf(emptyList<Flashcard>())) { a, b, c -> Triple(a, b, c) }
        } else {
            combine(repository.observeNotebooks(), repository.observeSources(id), repository.observeCards(id)) { a, b, c -> Triple(a, b, c) }
        }
    }

    val uiState: StateFlow<StudyUiState> = combine(data, selectedId, local) { (notebooks, sources, cards), id, l ->
        StudyUiState(
            notebooks = notebooks, selected = notebooks.find { it.id == id }, sources = sources, cards = cards,
            tab = l.tab, chat = l.chat, digest = l.digest, digestSourceId = l.digestSourceId, explanations = l.explanations,
            practice = l.practice, practiceMessage = l.practiceMessage, showAnswer = l.showAnswer, quiz = l.quiz,
            thinking = l.thinking, notice = l.notice, readerOpen = l.readerOpen, readerBusy = l.readerBusy, readerUrl = l.readerUrl,
            playing = sources.find { it.id == l.playingId },
            notes = l.notes, notesSourceId = l.notesSourceId, notesBusy = l.notesBusy, notesDeep = l.notesDeep, practiceTopic = l.practiceTopic
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StudyUiState())

    init {
        viewModelScope.launch { llm.loadIfPresent() }
        // A link shared from the browser opens the reader pre-filled.
        viewModelScope.launch {
            ShareInbox.pending.collect { shared ->
                if (shared == null) return@collect
                ShareInbox.pending.value = null
                val url = ReadableExtractor.findUrl(shared)
                if (url == null) {
                    local.value = local.value.copy(readerOpen = true, notice = "That share didn't contain a web link.")
                    return@collect
                }
                local.value = local.value.copy(readerOpen = true, readerUrl = url)
            }
        }
    }

    // ---- model -------------------------------------------------------------
    fun importModel(uri: android.net.Uri, name: String) {
        viewModelScope.launch { llm.importModel(uri, name) }
    }

    fun downloadModel() {
        viewModelScope.launch { llm.downloadDefaultModel() }
    }

    fun removeModel() = llm.removeModel()

    fun report(message: String?) {
        local.value = local.value.copy(notice = message)
    }

    // ---- notebooks ---------------------------------------------------------
    fun createNotebook(title: String) {
        if (title.isBlank()) return
        viewModelScope.launch { open(repository.createNotebook(title)) }
    }

    fun open(id: String) {
        local.value = Local(readerOpen = local.value.readerOpen, readerUrl = local.value.readerUrl)
        selectedId.value = id
    }

    fun close() {
        selectedId.value = null
        local.value = Local()
    }

    fun deleteNotebook(id: String) {
        viewModelScope.launch {
            repository.deleteNotebook(id)
            if (selectedId.value == id) close()
        }
    }

    fun selectTab(tab: StudyTab) {
        local.value = local.value.copy(tab = tab)
    }

    fun saveExamNotes(notes: String) {
        val id = selectedId.value ?: return
        viewModelScope.launch { repository.setExamNotes(id, notes) }
    }

    // ---- structured notes ---------------------------------------------------
    /**
     * Source -> page-aware chunks -> structured notes -> verified against the source pages -> merged. On-device rules
     * by default; with [deep] each chunk goes to the user's own Claude key. A cloud failure on one chunk falls back
     * to the rules for that chunk, so a flaky network never loses the whole run.
     */
    fun buildNotes(sourceId: String, deep: Boolean) {
        val src = uiState.value.sources.find { it.id == sourceId } ?: return
        val key = if (deep) apiKeyStore.getClaudeKey() else null
        if (deep && key == null) { report("Add your Claude API key in Settings first."); return }
        viewModelScope.launch {
            local.value = local.value.copy(notesSourceId = sourceId, notesBusy = "Reading pages...", notesDeep = deep, notes = emptyList(), notice = null)
            val (notes, cloudErrors) = withContext(Dispatchers.Default) {
                val chunks = NotesPipeline.chunk(NotesPipeline.pagesFromContent(src.content)).let { if (deep) it.take(MAX_DEEP_CHUNKS) else it }
                var errors = 0
                var lastError: String? = null
                val extracted = chunks.mapIndexed { i, c ->
                    local.value = local.value.copy(notesBusy = "Section ${i + 1} of ${chunks.size}: ${c.heading.take(30)}")
                    if (key == null) NotesPipeline.extractRules(c)
                    else withContext(Dispatchers.IO) { runCatching { claude.extract(key, c) } }
                        .getOrElse { errors++; lastError = it.message; NotesPipeline.extractRules(c) }
                }
                NotesPipeline.merge(NotesPipeline.verifyAll(extracted, chunks)) to (errors to lastError)
            }
            val (errors, lastError) = cloudErrors
            local.value = local.value.copy(
                notes = notes, notesBusy = null,
                notice = when {
                    notes.isEmpty() -> "No sections found. Notes need a source with some structure (headings)."
                    errors > 0 -> "$errors section(s) used on-device extraction because the cloud call failed: $lastError"
                    else -> null
                }
            )
        }
    }

    // ---- sources & reader --------------------------------------------------
    fun addSource(title: String, content: String, url: String? = null) {
        val id = selectedId.value ?: return
        if (content.isBlank()) return
        viewModelScope.launch {
            repository.addSource(id, title.ifBlank { content.lines().first().take(40) }, content, url)
            local.value = local.value.copy(digest = null)
        }
    }

    /** A video becomes a source through its subtitles: the transcript is what the study tools read. */
    fun addVideoSource(videoUri: String, title: String, subtitleText: String) {
        val id = selectedId.value ?: return
        val cues = SubtitleParser.parse(subtitleText)
        if (cues.isEmpty()) {
            local.value = local.value.copy(notice = "That subtitle file had no readable lines. Use an .srt or .vtt file.")
            return
        }
        viewModelScope.launch {
            repository.addSource(
                id, title, SubtitleParser.toStudyText(cues), mediaUri = videoUri, transcript = SubtitleParser.encode(cues)
            )
            local.value = local.value.copy(digest = null, notice = null)
        }
    }

    fun play(sourceId: String) {
        local.value = local.value.copy(playingId = sourceId)
    }

    fun stopPlaying() {
        local.value = local.value.copy(playingId = null)
    }

    fun deleteSource(id: String) {
        viewModelScope.launch {
            repository.deleteSource(id)
            local.value = local.value.copy(digest = null)
        }
    }

    fun openReader() {
        local.value = local.value.copy(readerOpen = true)
    }

    fun closeReader() {
        local.value = local.value.copy(readerOpen = false, readerUrl = "", notice = null)
    }

    fun setReaderUrl(url: String) {
        local.value = local.value.copy(readerUrl = url)
    }

    /** Reads the page, saves it as a source (in the open notebook, or a "Reading list" one), and shows its digest. */
    fun readPage(rawUrl: String) {
        val url = ReadableExtractor.findUrl(rawUrl) ?: rawUrl.trim().let { if (it.contains('.') && !it.contains(' ')) "https://$it" else it }
        if (local.value.readerBusy) return
        local.value = local.value.copy(readerBusy = true, notice = null)
        viewModelScope.launch {
            WebPageReader.read(url)
                .onSuccess { page ->
                    val notebookId = selectedId.value
                        ?: repository.observeNotebooks().first().find { it.title == READING_LIST }?.id
                        ?: repository.createNotebook(READING_LIST)
                    repository.addSource(notebookId, page.title, page.text, url)
                    selectedId.value = notebookId
                    val source = repository.observeSources(notebookId).first().last()
                    local.value = Local(tab = StudyTab.DIGEST, digestSourceId = source.id)
                    buildDigest(source.id)
                }
                .onFailure {
                    local.value = local.value.copy(readerBusy = false, notice = it.message ?: "Couldn't read that page")
                }
        }
    }

    // ---- ask / digest / explain -------------------------------------------
    fun ask(question: String) {
        if (question.isBlank() || local.value.thinking) return
        val chunks = engine.chunk(uiState.value.sources)
        local.value = local.value.copy(thinking = true)
        viewModelScope.launch {
            val answer = engine.answerGrounded(question, chunks, llm)
            local.value = local.value.copy(chat = local.value.chat + ChatTurn(question.trim(), answer), thinking = false)
        }
    }

    fun buildDigest(sourceId: String) {
        local.value = local.value.copy(thinking = true, digestSourceId = sourceId, digest = null, explanations = emptyMap())
        viewModelScope.launch {
            val source = uiState.value.sources.find { it.id == sourceId }
                ?: selectedId.value?.let { repository.observeSources(it).first().find { s -> s.id == sourceId } }
            val digest = source?.let { digestEngine.digestGrounded(it.content, llm) }
            local.value = local.value.copy(digest = digest, thinking = false, readerOpen = false, readerBusy = false, readerUrl = "")
        }
    }

    /** "Explain simply": the model re-teaches one point using the notebook's exam notes. Needs a loaded model. */
    fun explain(point: com.aether.core.engine.DigestPoint) {
        if (point.text in local.value.explanations) return
        local.value = local.value.copy(explanations = local.value.explanations + (point.text to null))
        viewModelScope.launch {
            val result = examEngine.explain("${point.text} ${point.context}", uiState.value.selected?.examNotes, llm)
                ?: "Load an on-device model in the Sources tab to get explanations."
            local.value = local.value.copy(explanations = local.value.explanations + (point.text to result))
        }
    }

    // ---- practice ----------------------------------------------------------
    fun generatePractice(topic: String) {
        if (local.value.thinking) return
        val chunks = engine.chunk(uiState.value.sources)
        local.value = local.value.copy(thinking = true, practice = null, practiceMessage = null, practiceTopic = topic)
        viewModelScope.launch {
            val questions = examEngine.practice(chunks, topic, uiState.value.selected?.examNotes, llm)
            local.value = local.value.copy(
                thinking = false,
                practice = questions.takeIf { it.isNotEmpty() },
                practiceMessage = if (questions.isEmpty()) {
                    if (llm.isReady) "The model's output wasn't usable this time. Try again, or narrow the topic." else "Practice questions are written by the on-device model. Load one in the Sources tab."
                } else null
            )
        }
    }

    // ---- cards & quiz ------------------------------------------------------
    fun generateCards() {
        val id = selectedId.value ?: return
        val state = uiState.value
        val existing = state.cards.map { it.front }.toSet()
        val fresh = state.sources.flatMap { engine.generateCards(it) }.filter { it.front !in existing }
        if (fresh.isEmpty()) return
        viewModelScope.launch { repository.addCards(id, fresh) }
    }

    fun revealAnswer() {
        local.value = local.value.copy(showAnswer = true)
    }

    fun grade(card: Flashcard, grade: Grade) {
        viewModelScope.launch {
            repository.applySchedule(card.id, SpacedRepetition.review(card, grade, System.currentTimeMillis()))
            recordStudyActivity()
            local.value = local.value.copy(showAnswer = false)
        }
    }

    fun startQuiz() {
        val questions = engine.buildQuiz(uiState.value.cards, count = 5, seed = System.currentTimeMillis().toInt())
        local.value = local.value.copy(quiz = QuizState(questions))
    }

    fun pickOption(index: Int) {
        val quiz = local.value.quiz ?: return
        if (quiz.picked != null || quiz.finished) return
        viewModelScope.launch { recordStudyActivity() }
        val q = quiz.questions[quiz.index]
        val correct = q.correctIndex == index
        if (!correct) recordMistake("", q.prompt, q.options[q.correctIndex], q.options.getOrElse(index) { "" })
        local.value = local.value.copy(quiz = quiz.copy(picked = index, score = quiz.score + if (correct) 1 else 0))
    }

    fun nextQuestion() {
        val quiz = local.value.quiz ?: return
        local.value = local.value.copy(quiz = quiz.copy(index = quiz.index + 1, picked = null))
    }

    /** Remembers only *that* and *when* you studied (hour of day), if learning is on, so AETHER can find your study hours. */
    private suspend fun recordStudyActivity() {
        if (!personalizationRepository.observePrefs().first().learningEnabled) return
        personalizationRepository.recordEvent(BehaviorEventType.STUDY_REVIEW, "Study", java.time.LocalTime.now().hour)
    }

    private companion object {
        const val READING_LIST = "Reading list"
    }
}
