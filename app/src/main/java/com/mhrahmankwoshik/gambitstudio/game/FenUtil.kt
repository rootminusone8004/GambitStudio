package com.mhrahmankwoshik.gambitstudio.game

import com.mhrahmankwoshik.gambitstudio.data.model.Piece
import com.mhrahmankwoshik.gambitstudio.data.model.PieceColor
import com.mhrahmankwoshik.gambitstudio.data.model.PieceType
import com.mhrahmankwoshik.gambitstudio.data.model.Square

object FenUtil {
    const val STARTING_FEN = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"

    data class FenState(
        val board: Array<Array<Piece?>>,
        val activeColor: PieceColor,
        val whiteKingSideCastle: Boolean,
        val whiteQueenSideCastle: Boolean,
        val blackKingSideCastle: Boolean,
        val blackQueenSideCastle: Boolean,
        val enPassantTarget: Square?,
        val halfMoveClock: Int,
        val fullMoveNumber: Int
    )

    fun parse(fen: String): FenState {
        val parts = fen.trim().split("\\s+".toRegex())
        require(parts.isNotEmpty()) { "Invalid FEN string" }

        val piecePlacement = parts[0]
        val activeColor = if (parts.size > 1 && parts[1].equals("b", ignoreCase = true)) PieceColor.BLACK else PieceColor.WHITE
        val castling = if (parts.size > 2) parts[2] else "-"
        val enPassantStr = if (parts.size > 3) parts[3] else "-"
        val halfMove = if (parts.size > 4) parts[4].toIntOrNull() ?: 0 else 0
        val fullMove = if (parts.size > 5) parts[5].toIntOrNull() ?: 1 else 1

        val grid = Array(8) { Array<Piece?>(8) { null } }
        val rows = piecePlacement.split("/")
        require(rows.size == 8) { "Invalid piece placement rows in FEN" }

        for (rankIndex in 0..7) {
            val fenRow = rows[rankIndex]
            val rank = 7 - rankIndex // row 0 in FEN is rank 8 (index 7)
            var file = 0
            for (char in fenRow) {
                if (char.isDigit()) {
                    file += char.digitToInt()
                } else {
                    val color = if (char.isUpperCase()) PieceColor.WHITE else PieceColor.BLACK
                    val type = PieceType.fromChar(char) ?: error("Unknown piece char: $char")
                    if (file in 0..7) {
                        grid[file][rank] = Piece(type, color)
                    }
                    file++
                }
            }
        }

        val enPassant = if (enPassantStr != "-") Square.fromNotation(enPassantStr) else null

        return FenState(
            board = grid,
            activeColor = activeColor,
            whiteKingSideCastle = castling.contains('K'),
            whiteQueenSideCastle = castling.contains('Q'),
            blackKingSideCastle = castling.contains('k'),
            blackQueenSideCastle = castling.contains('q'),
            enPassantTarget = enPassant,
            halfMoveClock = halfMove,
            fullMoveNumber = fullMove
        )
    }

    fun toFen(
        board: Array<Array<Piece?>>,
        activeColor: PieceColor,
        wK: Boolean,
        wQ: Boolean,
        bK: Boolean,
        bQ: Boolean,
        enPassant: Square?,
        halfMoveClock: Int,
        fullMoveNumber: Int
    ): String {
        val sb = StringBuilder()

        // 1. Piece placement
        for (rank in 7 downTo 0) {
            var emptyCount = 0
            for (file in 0..7) {
                val piece = board[file][rank]
                if (piece == null) {
                    emptyCount++
                } else {
                    if (emptyCount > 0) {
                        sb.append(emptyCount)
                        emptyCount = 0
                    }
                    sb.append(piece.fenChar)
                }
            }
            if (emptyCount > 0) {
                sb.append(emptyCount)
            }
            if (rank > 0) sb.append('/')
        }

        // 2. Active color
        sb.append(if (activeColor == PieceColor.WHITE) " w " else " b ")

        // 3. Castling
        val castleSb = StringBuilder()
        if (wK) castleSb.append('K')
        if (wQ) castleSb.append('Q')
        if (bK) castleSb.append('k')
        if (bQ) castleSb.append('q')
        if (castleSb.isEmpty()) castleSb.append('-')
        sb.append(castleSb.toString())

        // 4. En passant
        sb.append(' ')
        sb.append(enPassant?.notation ?: "-")

        // 5. Halfmove clock & Fullmove number
        sb.append(" $halfMoveClock $fullMoveNumber")

        return sb.toString()
    }
}
