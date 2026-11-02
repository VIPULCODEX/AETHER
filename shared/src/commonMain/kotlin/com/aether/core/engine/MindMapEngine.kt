package com.aether.core.engine

import com.aether.core.model.Goal
import com.aether.core.model.MindMapLayout
import com.aether.core.model.MindNode
import com.aether.core.model.MindNodeKind
import com.aether.core.model.PositionedNode
import com.aether.core.model.StudySource
import com.aether.core.model.Task
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

private const val MAX_BRANCHES = 5
private const val MAX_LEAVES_PER_BRANCH = 3
private const val RING_GAP = 170f
private const val NODE_SPACING = 54f

/**
 * Builds mind maps entirely on-device and lays them out radially. Two
 * derivations share one layout: study sources (topics -> key sentences, via
 * the same term statistics as the summarizer) and the Goal hierarchy
 * (Life Vision -> ... -> tasks), so a map is always a live view of the data
 * rather than a second copy to keep in sync.
 */
class MindMapEngine {

    fun fromSources(title: String, sources: List<StudySource>): List<MindNode> {
        val rootId = "root"
        val nodes = mutableListOf(MindNode(rootId, title, title, null, MindNodeKind.ROOT))
        val populated = sources.filter { splitSentences(it.content).isNotEmpty() }
        populated.forEach { source ->
            val parent = if (populated.size > 1) {
                MindNode("src-${source.id}", source.title, source.title, rootId, MindNodeKind.BRANCH).also { nodes += it }.id
            } else rootId
            nodes += topicNodes(source, parent)
        }
        return nodes
    }

    private fun topicNodes(source: StudySource, parentId: String): List<MindNode> {
        val sentences = splitSentences(source.content)
        val sentenceTerms = sentences.map { tokenize(it) }
        val docFreq = sentenceTerms.flatMap { it.distinct() }.groupingBy { it }.eachCount()
        val termFreq = sentenceTerms.flatten().groupingBy { it }.eachCount()
        // A topic must recur across sentences, otherwise it's just a rare word.
        val topics = docFreq.filter { it.value >= 2 && it.key.length >= 4 }.keys
            .sortedWith(compareByDescending<String> { docFreq.getValue(it) * 3 + termFreq.getValue(it) }.thenBy { it })
            .take(MAX_BRANCHES * 2)
        if (topics.isEmpty()) {
            return sentences.take(MAX_BRANCHES).mapIndexed { i, s ->
                MindNode("${source.id}-l$i", shorten(s), s, parentId, MindNodeKind.LEAF)
            }
        }
        val surface = surfaceForms(source.content)
        val assigned = topics.associateWith { mutableListOf<String>() }
        sentences.forEachIndexed { i, s ->
            val terms = sentenceTerms[i]
            val topic = topics.maxByOrNull { t -> terms.count { it == t } }?.takeIf { t -> t in terms }
            if (topic != null) assigned.getValue(topic) += s
        }
        // Keep the fullest branches; a topic whose sentences all went elsewhere would be an empty stub.
        val kept = topics.filter { assigned.getValue(it).isNotEmpty() }
            .sortedByDescending { assigned.getValue(it).size }.take(MAX_BRANCHES)
        return kept.flatMap { topic ->
            val branchId = "${source.id}-b-$topic"
            val form = surface[topic] ?: topic
            val label = if (form.any { it.isUpperCase() }) form else form.replaceFirstChar { it.uppercase() }
            val leaves = assigned.getValue(topic).take(MAX_LEAVES_PER_BRANCH)
            listOf(MindNode(branchId, label, label, parentId, MindNodeKind.BRANCH)) +
                leaves.mapIndexed { i, s -> MindNode("$branchId-l$i", shorten(s), s, branchId, MindNodeKind.LEAF) }
        }
    }

    /** stem -> its most common original spelling (case kept, so acronyms like NADPH survive). */
    private fun surfaceForms(text: String): Map<String, String> =
        Regex("[\\p{L}][\\p{L}\\-]+").findAll(text).map { it.value }
            .mapNotNull { w -> tokenize(w).firstOrNull()?.let { it to w } }
            .groupBy({ it.first }, { it.second })
            .mapValues { (_, words) -> words.groupingBy { it }.eachCount().maxByOrNull { it.value }!!.key }

    private fun shorten(sentence: String): String =
        if (sentence.length <= 56) sentence.trimEnd('.') else sentence.take(53).trimEnd(' ', ',', ';') + "…"

    fun fromGoals(goals: List<Goal>, tasks: List<Task>, rootLabel: String = "My life"): List<MindNode> {
        val goalIds = goals.map { it.id }.toSet()
        val rootId = "root"
        val nodes = mutableListOf(MindNode(rootId, rootLabel, rootLabel, null, MindNodeKind.ROOT))
        goals.forEach { g ->
            val parent = g.parentGoalId?.takeIf { it in goalIds } ?: rootId
            val pct = (g.progress * 100).toInt()
            nodes += MindNode("g-${g.id}", g.title, "${g.title} · ${g.domain} · $pct% complete", parent.let { if (it == rootId) it else "g-$it" }, MindNodeKind.BRANCH)
        }
        tasks.filter { it.goalId in goalIds }.forEach { t ->
            nodes += MindNode("t-${t.id}", shorten(t.title), t.title + if (t.isDone) " · done" else "", "g-${t.goalId}", if (t.isDone) MindNodeKind.DONE else MindNodeKind.LEAF)
        }
        return nodes
    }

    /**
     * Radial tree layout: each subtree gets an angular slice proportional to
     * how many leaves it has, and rings widen as needed so crowded maps don't
     * overlap. Nodes in [collapsed] hide their descendants.
     */
    fun layout(nodes: List<MindNode>, collapsed: Set<String> = emptySet()): MindMapLayout {
        val root = nodes.firstOrNull { it.parentId == null } ?: return MindMapLayout(emptyList(), emptyList(), 0f)
        val children = nodes.filter { it.parentId != null }.groupBy { it.parentId!! }
        fun visible(n: MindNode): List<MindNode> = if (n.id in collapsed) emptyList() else children[n.id].orEmpty()

        val leafCount = HashMap<String, Int>()
        fun count(n: MindNode, seen: MutableSet<String>): Int {
            if (!seen.add(n.id)) return 0
            val kids = visible(n)
            val c = if (kids.isEmpty()) 1 else kids.sumOf { count(it, seen) }
            leafCount[n.id] = max(1, c)
            return leafCount.getValue(n.id)
        }
        val totalLeaves = count(root, mutableSetOf())

        var maxDepth = 1
        fun depthOf(n: MindNode, d: Int, seen: MutableSet<String>) {
            if (!seen.add(n.id)) return
            maxDepth = max(maxDepth, d)
            visible(n).forEach { depthOf(it, d + 1, seen) }
        }
        depthOf(root, 0, mutableSetOf())
        val ring = max(RING_GAP, totalLeaves * NODE_SPACING / (2f * PI.toFloat() * maxDepth))

        val placed = mutableListOf<PositionedNode>()
        val edges = mutableListOf<Pair<String, String>>()
        val seen = mutableSetOf<String>()
        fun place(n: MindNode, a0: Float, a1: Float, depth: Int) {
            if (!seen.add(n.id)) return
            val angle = (a0 + a1) / 2f
            val r = depth * ring
            placed += PositionedNode(
                n, (r * cos(angle)), (r * sin(angle)), depth,
                hasChildren = children[n.id].orEmpty().isNotEmpty(), collapsed = n.id in collapsed
            )
            val kids = visible(n)
            var start = a0
            kids.forEach { kid ->
                val span = (a1 - a0) * leafCount.getValue(kid.id) / leafCount.getValue(n.id).toFloat()
                edges += n.id to kid.id
                place(kid, start, start + span, depth + 1)
                start += span
            }
        }
        place(root, 0f, 2f * PI.toFloat(), 0)
        return MindMapLayout(placed, edges, maxDepth * ring)
    }
}
