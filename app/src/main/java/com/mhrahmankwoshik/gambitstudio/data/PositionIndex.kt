package com.mhrahmankwoshik.gambitstudio.data

import com.mhrahmankwoshik.gambitstudio.game.ChessBoard
import com.mhrahmankwoshik.gambitstudio.game.MoveGenerator

/**
 * Remembers every board position that occurs in an opening file, so that a position
 * reached through a different move order (a transposition) is still recognised and
 * its recorded continuations can be reused.
 *
 * Built from the file's UCI variation paths: the paths are merged into a move tree and
 * every tree node is indexed by the position it leads to.
 */
class PositionIndex private constructor(
    private val root: Node,
    private val nodesByKey: Map<String, List<Node>>
) {

    class Node(
        val uci: String,
        val san: String,
        val children: MutableList<Node> = mutableListOf(),
        var isTerminal: Boolean = false
    )

    /** True if this exact position occurs anywhere in the recorded lines. */
    fun isKnown(board: ChessBoard): Boolean = nodesByKey.containsKey(positionKey(board))

    /** Recorded next moves (UCI) from this position, regardless of move order. */
    fun candidateMoves(board: ChessBoard): List<String> =
        nodesByKey[positionKey(board)].orEmpty()
            .flatMap { node -> node.children.map { it.uci } }
            .distinct()

    /**
     * Every recorded continuation (SAN, starting at this position) down to the end of each line.
     */
    fun continuationsFrom(board: ChessBoard): List<List<String>> =
        nodesByKey[positionKey(board)].orEmpty()
            .flatMap { collectTails(it) }
            .filter { it.isNotEmpty() }
            .distinct()

    /**
     * For a played line (UCI from the start position), finds every point where the line has left
     * the recorded move order but landed on a recorded position, and returns full SAN lines that
     * graft the recorded continuations onto the played move order. Includes the played line itself.
     *
     * Returns just the played line when no transposition occurs.
     */
    fun expandWithTranspositions(playedUci: List<String>): TranspositionExpansion {
        val board = ChessBoard()
        val playedSan = mutableListOf<String>()
        val lines = LinkedHashSet<List<String>>()
        var treeNode: Node? = root
        var transpositionPly = -1

        for (uci in playedUci) {
            val move = MoveGenerator.generateLegalMoves(board).find { it.uci == uci } ?: break
            board.applyMove(move)
            playedSan.add(move.san)
            treeNode = treeNode?.children?.find { it.uci == uci }

            if (treeNode == null) {
                val tails = continuationsFrom(board)
                if (tails.isNotEmpty()) {
                    if (transpositionPly < 0) transpositionPly = playedSan.size
                    for (tail in tails) lines.add(playedSan.toList() + tail)
                }
            }
        }

        lines.add(playedSan.toList())
        return TranspositionExpansion(
            lines = lines.toList(),
            transpositionPly = transpositionPly.takeIf { it >= 0 },
            graftedCount = lines.size - 1
        )
    }

    data class TranspositionExpansion(
        /** SAN lines from the start position to write into the file. */
        val lines: List<List<String>>,
        /** Ply count at which the played line first joined a known position, or null. */
        val transpositionPly: Int?,
        /** Number of lines added from recorded continuations (excluding the played line). */
        val graftedCount: Int
    )

    private fun collectTails(node: Node): List<List<String>> {
        if (node.children.isEmpty()) return listOf(emptyList())
        val tails = mutableListOf<List<String>>()
        if (node.isTerminal) tails.add(emptyList())
        for (child in node.children) {
            for (tail in collectTails(child)) tails.add(listOf(child.san) + tail)
        }
        return tails
    }

    companion object {
        val EMPTY: PositionIndex = fromUciPaths(emptyList())

        /**
         * Key identifying a position independent of move order and move counters:
         * piece placement, side to move, castling rights, and the en-passant square only
         * when an en-passant capture is actually legal.
         */
        fun positionKey(board: ChessBoard): String {
            val fields = board.toFen().split(' ')
            val ep = if (board.enPassantTarget != null &&
                MoveGenerator.generateLegalMoves(board).any { it.isEnPassant }
            ) fields[3] else "-"
            return "${fields[0]} ${fields[1]} ${fields[2]} $ep"
        }

        fun fromUciPaths(paths: List<List<String>>): PositionIndex {
            val root = Node(uci = "", san = "")
            for (path in paths) {
                if (path.isEmpty()) continue
                var curr = root
                for (uci in path) {
                    curr = curr.children.find { it.uci == uci }
                        ?: Node(uci = uci, san = "").also { curr.children.add(it) }
                }
                curr.isTerminal = true
            }

            val index = HashMap<String, MutableList<Node>>()
            val resolvedRoot = Node(uci = "", san = "")
            resolve(root, resolvedRoot, ChessBoard(), index)
            return PositionIndex(resolvedRoot, index)
        }

        /** Replays the raw UCI tree, filling in SAN and indexing each node by position. */
        private fun resolve(
            raw: Node,
            target: Node,
            board: ChessBoard,
            index: HashMap<String, MutableList<Node>>
        ) {
            target.isTerminal = raw.isTerminal
            index.getOrPut(positionKey(board)) { mutableListOf() }.add(target)
            if (raw.children.isEmpty()) return

            val legalMoves = MoveGenerator.generateLegalMoves(board)
            for (rawChild in raw.children) {
                val move = legalMoves.find { it.uci == rawChild.uci }
                    ?: legalMoves.find { it.uci.startsWith(rawChild.uci.take(4)) }
                    ?: continue
                val childBoard = board.copy()
                childBoard.applyMove(move)
                val child = Node(uci = move.uci, san = move.san)
                target.children.add(child)
                resolve(rawChild, child, childBoard, index)
            }
        }
    }
}
