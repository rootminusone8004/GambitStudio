package com.mhrahmankwoshik.gambitstudio.data

import com.mhrahmankwoshik.gambitstudio.data.model.Move
import com.mhrahmankwoshik.gambitstudio.data.model.Opening
import com.mhrahmankwoshik.gambitstudio.game.ChessBoard
import com.mhrahmankwoshik.gambitstudio.game.MoveGenerator

data class VariationPath(
    val movesUci: List<String>,
    val movesSan: List<String>,
    val lineIndex: Int,
    val isBaseLine: Boolean = false
)

data class ParseResult(
    val paths: List<VariationPath>,
    val errors: List<String>,
    val totalMovesCount: Int
)

object OpeningVariationParser {

    private data class TreeNode(
        val indent: Int,
        val lineNumber: Int,
        val boardAfter: ChessBoard,
        val movesUciSoFar: List<String>,
        val movesSanSoFar: List<String>,
        var hasChildren: Boolean = false
    )

    fun countIndent(line: String): Int {
        var count = 0
        for (ch in line) {
            when (ch) {
                ' ' -> count += 1
                '\t' -> count += 4
                else -> break
            }
        }
        return count
    }

    /**
     * Parses the multi-line text file of opening variations.
     *
     * Supports:
     * - Flat lines: Line 1 base, subsequent lines continuations.
     * - Indented trees: Indentation defines nested branches without repeating moves.
     *   e.g.:
     *   1. e4 e5 2. Nf3 Nc6 3. d4 exd4 4. Bc4
     *      Be7 5. c3 dxc3 6. Qd5 Nh6 7. Bxh6
     *         gxh6 8.Qf7#
     *         0-0 8. Bxg7 Kxg7 9. Nxc3
     *      Bb4 5. c3 dxc3 6. bxc3
     *         Bc5 7. Bxf7 Kxf7 8. Qd5+ Ke8 9. Qxc5
     *         Ba5 7. Qb3 Qe7 8. 0-0
     */
    fun parseFileContent(
        content: String,
        fallbackOpening: Opening? = null
    ): ParseResult {
        val lines = content.lines()
        val paths = mutableListOf<VariationPath>()
        val errors = mutableListOf<String>()
        val stack = mutableListOf<TreeNode>()

        for ((index, rawLine) in lines.withIndex()) {
            val lineNumber = index + 1
            val trimmed = rawLine.trim()

            // Skip empty or comment lines
            if (trimmed.isEmpty() || trimmed.startsWith("#") || trimmed.startsWith("//")) {
                continue
            }

            val indent = countIndent(rawLine)
            val tokens = tokenizeMoveLine(trimmed)
            if (tokens.isEmpty()) continue

            if (stack.isEmpty()) {
                // First valid line is the root (base opening moves)
                val initialBoard = ChessBoard()
                val (uciMoves, sanMoves, finalBoard, lineErrors) = parseMoveTokens(
                    board = initialBoard,
                    tokens = tokens,
                    lineNumber = lineNumber
                )

                if (lineErrors.isNotEmpty()) {
                    errors.addAll(lineErrors)
                }

                if (uciMoves.isNotEmpty()) {
                    val root = TreeNode(
                        indent = indent,
                        lineNumber = lineNumber,
                        boardAfter = finalBoard,
                        movesUciSoFar = uciMoves,
                        movesSanSoFar = sanMoves,
                        hasChildren = false
                    )
                    stack.add(root)
                    // Root is always recorded as the base line path
                    paths.add(
                        VariationPath(
                            movesUci = uciMoves,
                            movesSan = sanMoves,
                            lineIndex = lineNumber,
                            isBaseLine = true
                        )
                    )
                }
            } else {
                // Pop completed siblings or deeper subtrees
                while (stack.size > 1 && stack.last().indent >= indent) {
                    val popped = stack.removeAt(stack.size - 1)
                    if (!popped.hasChildren && popped.movesUciSoFar.isNotEmpty()) {
                        paths.add(
                            VariationPath(
                                movesUci = popped.movesUciSoFar,
                                movesSan = popped.movesSanSoFar,
                                lineIndex = popped.lineNumber,
                                isBaseLine = false
                            )
                        )
                    }
                }

                // Determine parent node and starting position
                val isExplicitFromStart = isMoveFromStart(tokens)
                val (startBoard, baseUci, baseSan) = if (isExplicitFromStart) {
                    Triple(ChessBoard(), emptyList<String>(), emptyList<String>())
                } else if (indent > stack.last().indent) {
                    val parent = stack.last()
                    parent.hasChildren = true
                    Triple(parent.boardAfter.copy(), parent.movesUciSoFar, parent.movesSanSoFar)
                } else {
                    // Sibling continuation of root (e.g. flat files with indent 0)
                    val root = stack.first()
                    root.hasChildren = true
                    Triple(root.boardAfter.copy(), root.movesUciSoFar, root.movesSanSoFar)
                }

                val (uciMoves, sanMoves, finalBoard, lineErrors) = parseMoveTokens(
                    board = startBoard,
                    tokens = tokens,
                    lineNumber = lineNumber
                )

                if (lineErrors.isNotEmpty()) {
                    errors.addAll(lineErrors)
                }

                if (uciMoves.isNotEmpty()) {
                    val node = TreeNode(
                        indent = indent,
                        lineNumber = lineNumber,
                        boardAfter = finalBoard,
                        movesUciSoFar = baseUci + uciMoves,
                        movesSanSoFar = baseSan + sanMoves,
                        hasChildren = false
                    )
                    stack.add(node)
                }
            }
        }

        // Pop remaining leaf nodes from stack (leaves become variation paths)
        while (stack.size > 1) {
            val popped = stack.removeAt(stack.size - 1)
            if (!popped.hasChildren && popped.movesUciSoFar.isNotEmpty()) {
                paths.add(
                    VariationPath(
                        movesUci = popped.movesUciSoFar,
                        movesSan = popped.movesSanSoFar,
                        lineIndex = popped.lineNumber,
                        isBaseLine = false
                    )
                )
            }
        }

        // If no path was defined from the text, use fallback opening if available
        if (paths.isEmpty() && fallbackOpening != null) {
            paths.add(
                VariationPath(
                    movesUci = fallbackOpening.movesUci,
                    movesSan = listOf(fallbackOpening.movesSan),
                    lineIndex = 1,
                    isBaseLine = true
                )
            )
        }

        val totalMoves = paths.sumOf { it.movesUci.size }
        return ParseResult(paths = paths, errors = errors, totalMovesCount = totalMoves)
    }

    /**
     * Splits a line of moves into clean move tokens, removing move numbers and comments.
     */
    fun tokenizeMoveLine(line: String): List<String> {
        // Remove trailing inline comments
        val commentStripped = line.split('#')[0].split("//")[0].trim()

        // Normalize castling before anything else
        val castlingNormalized = commentStripped
            .replace(Regex("(?i)0-0-0"), "O-O-O")
            .replace(Regex("(?i)0-0"), "O-O")

        // Separate attached move numbers like "5.e5" or "4...Nf6" -> "5. e5", "4... Nf6"
        val spaced = castlingNormalized
            .replace(Regex("(\\d+\\.+)([A-Za-z0-9])"), "$1 $2")

        val rawTokens = spaced.split(Regex("\\s+")).filter { it.isNotBlank() }
        val moveTokens = mutableListOf<String>()

        for (token in rawTokens) {
            val clean = token.trim()
            // Ignore pure move numbers like "1.", "2.", "4...", "12."
            if (clean.matches(Regex("^\\d+\\.+$"))) continue
            // Ignore game result symbols
            if (clean in listOf("1-0", "0-1", "1/2-1/2", "*")) continue
            moveTokens.add(clean)
        }

        return moveTokens
    }

    private fun isMoveFromStart(tokens: List<String>): Boolean {
        if (tokens.isEmpty()) return false
        val first = tokens.first()
        return first.startsWith("1.") || first.startsWith("1...")
    }

    private data class LineParseOutcome(
        val uciMoves: List<String>,
        val sanMoves: List<String>,
        val finalBoard: ChessBoard,
        val errors: List<String>
    )

    private fun parseMoveTokens(
        board: ChessBoard,
        tokens: List<String>,
        lineNumber: Int
    ): LineParseOutcome {
        val uciMoves = mutableListOf<String>()
        val sanMoves = mutableListOf<String>()
        val errors = mutableListOf<String>()
        val currentBoard = board.copy()

        for (token in tokens) {
            val matchedMove = findLegalMove(currentBoard, token)
            if (matchedMove != null) {
                currentBoard.applyMove(matchedMove)
                uciMoves.add(matchedMove.uci)
                sanMoves.add(matchedMove.san)
            } else {
                val legalMoves = MoveGenerator.generateLegalMoves(currentBoard)
                val sampleLegal = legalMoves.take(5).joinToString(", ") { it.san }
                val errorMsg = "Line $lineNumber: Move '$token' is not a legal move. (Suggestions: $sampleLegal…)"
                errors.add(errorMsg)
                // Stop parsing this line on the first illegal move
                break
            }
        }

        return LineParseOutcome(uciMoves, sanMoves, currentBoard, errors)
    }

    /**
     * Resolves a SAN, UCI, or loose notation token into a legal Move on the current board.
     */
    fun findLegalMove(board: ChessBoard, token: String): Move? {
        val legalMoves = MoveGenerator.generateLegalMoves(board)
        if (legalMoves.isEmpty()) return null

        val clean = token
            .replace("+", "")
            .replace("#", "")
            .replace("!", "")
            .replace("?", "")
            .trim()

        // 1. Exact SAN match (stripped of +, #)
        val sanMatch = legalMoves.find { it.san.replace("+", "").replace("#", "") == clean }
        if (sanMatch != null) return sanMatch

        // 2. Case-insensitive SAN match
        val sanIgnoreCase = legalMoves.find {
            it.san.replace("+", "").replace("#", "").equals(clean, ignoreCase = true)
        }
        if (sanIgnoreCase != null) return sanIgnoreCase

        // 3. Castling notation (O-O, 0-0, O-O-O, 0-0-0)
        val isKingSideCastle = clean.equals("O-O", ignoreCase = true) || clean == "0-0"
        if (isKingSideCastle) {
            val castle = legalMoves.find { it.isCastling && it.to.file == 6 }
            if (castle != null) return castle
        }

        val isQueenSideCastle = clean.equals("O-O-O", ignoreCase = true) || clean == "0-0-0"
        if (isQueenSideCastle) {
            val castle = legalMoves.find { it.isCastling && it.to.file == 2 }
            if (castle != null) return castle
        }

        // 4. Exact UCI match (e.g. "e2e4", "g1f3", "e1g1")
        val uciMatch = legalMoves.find { it.uci == clean.lowercase() }
        if (uciMatch != null) return uciMatch

        // 5. Piece move where capture 'x' was omitted (e.g. "Qf7" instead of "Qxf7", "Bf7" instead of "Bxf7")
        if (clean.length in 3..4 && clean[0].isUpperCase()) {
            val pieceChar = clean[0]
            val destSquare = clean.takeLast(2)
            val candidateMoves = legalMoves.filter {
                val p = board.getPiece(it.from)
                p != null && p.type.symbol.equals(pieceChar, ignoreCase = true) && it.to.notation == destSquare
            }
            if (candidateMoves.size == 1) {
                return candidateMoves.first()
            } else if (candidateMoves.size > 1 && clean.length == 4) {
                val disambig = clean[1]
                val match = candidateMoves.find {
                    ('a' + it.from.file == disambig) || (it.from.rank + 1 == disambig.digitToIntOrNull())
                }
                if (match != null) return match
            }
        }

        // 6. Pawn capture notation variants like "exd" or "ed" or "ed4"
        if (clean.length in 2..4 && clean[0] in 'a'..'h') {
            val fromFile = clean[0] - 'a'
            val pawnCaptures = legalMoves.filter {
                it.capturedPiece != null && it.from.file == fromFile && board.getPiece(it.from)?.type?.symbol?.lowercaseChar() == 'p'
            }

            if (clean.contains('x')) {
                // "exd4" or "exd"
                val targetStr = clean.substringAfter('x')
                if (targetStr.length == 2) {
                    val match = pawnCaptures.find { it.to.notation == targetStr }
                    if (match != null) return match
                } else if (targetStr.length == 1) {
                    val toFile = targetStr[0] - 'a'
                    val match = pawnCaptures.find { it.to.file == toFile }
                    if (match != null) return match
                }
            } else if (clean.length == 3 && clean[1] in 'a'..'h') {
                // "ed4" -> fromFile=e, toFile=d, rank=4
                val match = pawnCaptures.find { it.to.notation == clean.substring(1) }
                if (match != null) return match
            }
        }

        return null
    }

    /**
     * Formats a list of SAN tokens into standard move notation e.g. "1. e4 e5 2. Nf3 Nc6"
     */
    fun formatSanMoves(moves: List<String>): String {
        val sb = StringBuilder()
        for (i in moves.indices) {
            if (i % 2 == 0) {
                val moveNum = (i / 2) + 1
                sb.append("$moveNum. ")
            }
            sb.append(moves[i]).append(" ")
        }
        return sb.toString().trim()
    }

    class MoveTreeNode(
        val san: String,
        val halfMoveIndex: Int,
        val children: MutableList<MoveTreeNode> = mutableListOf(),
        var isTerminal: Boolean = false
    )

    fun buildMoveTree(paths: List<List<String>>): MoveTreeNode {
        val root = MoveTreeNode("", -1)
        for (path in paths) {
            if (path.isEmpty()) continue
            var curr = root
            for ((idx, san) in path.withIndex()) {
                val cleanSan = san.trim()
                var found = curr.children.find { it.san == cleanSan }
                if (found == null) {
                    found = MoveTreeNode(cleanSan, idx)
                    curr.children.add(found)
                }
                curr = found
            }
            curr.isTerminal = true
        }
        return root
    }

    private fun formatMovesSegment(segment: List<Pair<String, Int>>): String {
        val sb = StringBuilder()
        for ((san, idx) in segment) {
            if (idx % 2 == 0) {
                val moveNum = (idx / 2) + 1
                sb.append("$moveNum. ")
            }
            sb.append(san).append(" ")
        }
        return sb.toString().trim()
    }

    private fun serializeBranch(startNode: MoveTreeNode, indentSpaces: Int): List<String> {
        var curr = startNode
        val segment = mutableListOf(Pair(curr.san, curr.halfMoveIndex))
        while (curr.children.size == 1 && !(curr.isTerminal && curr.children.isNotEmpty())) {
            curr = curr.children[0]
            segment.add(Pair(curr.san, curr.halfMoveIndex))
        }

        val indentStr = " ".repeat(indentSpaces)
        val line = indentStr + formatMovesSegment(segment)
        val lines = mutableListOf(line)
        if (curr.children.isNotEmpty()) {
            for (child in curr.children) {
                lines.addAll(serializeBranch(child, indentSpaces + 3))
            }
        }
        return lines
    }

    /**
     * Serializes multiple move paths (lists of SAN moves) into an indented variation tree string.
     */
    fun serializeMoveTree(paths: List<List<String>>): String {
        val root = buildMoveTree(paths)
        val lines = mutableListOf<String>()
        for (child in root.children) {
            lines.addAll(serializeBranch(child, 0))
        }
        return lines.joinToString("\n")
    }

    /**
     * Extracts full leaf paths from a ParseResult (ignoring intermediate base roots when branches exist).
     */
    fun getLeafVariationPaths(parseResult: ParseResult): List<VariationPath> {
        val nonBase = parseResult.paths.filter { !it.isBaseLine }
        return if (nonBase.isNotEmpty()) nonBase else parseResult.paths
    }

    /**
     * Merges a newly played move line (list of SAN strings) into existing file content,
     * maintaining clean indentation and branching without repeating prefix moves.
     */
    fun mergeNewVariation(existingContent: String, newLineSan: List<String>): String {
        val cleanLine = newLineSan.map { it.trim() }.filter { it.isNotBlank() }
        if (cleanLine.isEmpty()) return existingContent

        val existingResult = parseFileContent(existingContent)
        val existingLeafPaths = getLeafVariationPaths(existingResult).map { it.movesSan }

        val alreadyPresent = existingLeafPaths.any { path ->
            path == cleanLine || (path.size >= cleanLine.size && path.take(cleanLine.size) == cleanLine)
        }

        val updatedPaths = if (alreadyPresent) {
            existingLeafPaths
        } else {
            val filtered = existingLeafPaths.filterNot { existingPath ->
                cleanLine.size > existingPath.size && cleanLine.take(existingPath.size) == existingPath
            }
            filtered + listOf(cleanLine)
        }

        return serializeMoveTree(updatedPaths)
    }
}

