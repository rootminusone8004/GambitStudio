package com.mhrahmankwoshik.gambitstudio.game

import com.mhrahmankwoshik.gambitstudio.data.model.Move
import com.mhrahmankwoshik.gambitstudio.data.model.Piece
import com.mhrahmankwoshik.gambitstudio.data.model.PieceColor
import com.mhrahmankwoshik.gambitstudio.data.model.PieceType
import com.mhrahmankwoshik.gambitstudio.data.model.Square

class ChessBoard {
    val grid: Array<Array<Piece?>> = Array(8) { Array(8) { null } }
    var activeColor: PieceColor = PieceColor.WHITE
    var whiteKingSideCastle: Boolean = true
    var whiteQueenSideCastle: Boolean = true
    var blackKingSideCastle: Boolean = true
    var blackQueenSideCastle: Boolean = true
    var enPassantTarget: Square? = null
    var halfMoveClock: Int = 0
    var fullMoveNumber: Int = 1

    init {
        loadFen(FenUtil.STARTING_FEN)
    }

    fun getPiece(square: Square): Piece? = grid[square.file][square.rank]
    fun getPiece(file: Int, rank: Int): Piece? = if (file in 0..7 && rank in 0..7) grid[file][rank] else null

    fun setPiece(square: Square, piece: Piece?) {
        grid[square.file][square.rank] = piece
    }

    fun loadFen(fen: String) {
        val state = FenUtil.parse(fen)
        for (f in 0..7) {
            for (r in 0..7) {
                grid[f][r] = state.board[f][r]
            }
        }
        activeColor = state.activeColor
        whiteKingSideCastle = state.whiteKingSideCastle
        whiteQueenSideCastle = state.whiteQueenSideCastle
        blackKingSideCastle = state.blackKingSideCastle
        blackQueenSideCastle = state.blackQueenSideCastle
        enPassantTarget = state.enPassantTarget
        halfMoveClock = state.halfMoveClock
        fullMoveNumber = state.fullMoveNumber
    }

    fun toFen(): String {
        return FenUtil.toFen(
            board = grid,
            activeColor = activeColor,
            wK = whiteKingSideCastle,
            wQ = whiteQueenSideCastle,
            bK = blackKingSideCastle,
            bQ = blackQueenSideCastle,
            enPassant = enPassantTarget,
            halfMoveClock = halfMoveClock,
            fullMoveNumber = fullMoveNumber
        )
    }

    fun copy(): ChessBoard {
        val copy = ChessBoard()
        for (f in 0..7) {
            for (r in 0..7) {
                copy.grid[f][r] = this.grid[f][r]
            }
        }
        copy.activeColor = this.activeColor
        copy.whiteKingSideCastle = this.whiteKingSideCastle
        copy.whiteQueenSideCastle = this.whiteQueenSideCastle
        copy.blackKingSideCastle = this.blackKingSideCastle
        copy.blackQueenSideCastle = this.blackQueenSideCastle
        copy.enPassantTarget = this.enPassantTarget
        copy.halfMoveClock = this.halfMoveClock
        copy.fullMoveNumber = this.fullMoveNumber
        return copy
    }

    fun findKing(color: PieceColor): Square? {
        for (f in 0..7) {
            for (r in 0..7) {
                val piece = grid[f][r]
                if (piece != null && piece.type == PieceType.KING && piece.color == color) {
                    return Square(f, r)
                }
            }
        }
        return null
    }

    fun applyMove(move: Move) {
        val piece = getPiece(move.from) ?: return
        var isCapture = getPiece(move.to) != null
        val isPawn = piece.type == PieceType.PAWN

        // En passant capture
        if (isPawn && move.to == enPassantTarget) {
            val captureRank = if (piece.color == PieceColor.WHITE) 4 else 3
            grid[move.to.file][captureRank] = null
            isCapture = true
        }

        // Move piece
        val destinationPiece = if (move.promotion != null) {
            Piece(move.promotion, piece.color)
        } else {
            piece
        }
        setPiece(move.to, destinationPiece)
        setPiece(move.from, null)

        // Castling move: move the rook as well
        if (piece.type == PieceType.KING) {
            if (move.from.file == 4 && move.to.file == 6) {
                // Kingside castle
                val rook = getPiece(7, move.from.rank)
                setPiece(Square(5, move.from.rank), rook)
                setPiece(Square(7, move.from.rank), null)
            } else if (move.from.file == 4 && move.to.file == 2) {
                // Queenside castle
                val rook = getPiece(0, move.from.rank)
                setPiece(Square(3, move.from.rank), rook)
                setPiece(Square(0, move.from.rank), null)
            }
        }

        // Update castling rights
        if (piece.type == PieceType.KING) {
            if (piece.color == PieceColor.WHITE) {
                whiteKingSideCastle = false
                whiteQueenSideCastle = false
            } else {
                blackKingSideCastle = false
                blackQueenSideCastle = false
            }
        }
        if (piece.type == PieceType.ROOK) {
            if (piece.color == PieceColor.WHITE) {
                if (move.from == Square(0, 0)) whiteQueenSideCastle = false
                if (move.from == Square(7, 0)) whiteKingSideCastle = false
            } else {
                if (move.from == Square(0, 7)) blackQueenSideCastle = false
                if (move.from == Square(7, 7)) blackKingSideCastle = false
            }
        }
        // If a rook was captured on its starting square
        if (move.to == Square(0, 0)) whiteQueenSideCastle = false
        if (move.to == Square(7, 0)) whiteKingSideCastle = false
        if (move.to == Square(0, 7)) blackQueenSideCastle = false
        if (move.to == Square(7, 7)) blackKingSideCastle = false

        // Update en passant target square
        if (isPawn && kotlin.math.abs(move.to.rank - move.from.rank) == 2) {
            val epRank = (move.from.rank + move.to.rank) / 2
            enPassantTarget = Square(move.from.file, epRank)
        } else {
            enPassantTarget = null
        }

        // Halfmove clock (reset on pawn move or capture)
        if (isPawn || isCapture) {
            halfMoveClock = 0
        } else {
            halfMoveClock++
        }

        // Fullmove number
        if (activeColor == PieceColor.BLACK) {
            fullMoveNumber++
        }

        // Switch active player
        activeColor = activeColor.opposite()
    }
}
