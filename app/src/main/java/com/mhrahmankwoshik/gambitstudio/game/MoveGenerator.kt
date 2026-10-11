package com.mhrahmankwoshik.gambitstudio.game

import com.mhrahmankwoshik.gambitstudio.data.model.GameStatus
import com.mhrahmankwoshik.gambitstudio.data.model.Move
import com.mhrahmankwoshik.gambitstudio.data.model.Piece
import com.mhrahmankwoshik.gambitstudio.data.model.PieceColor
import com.mhrahmankwoshik.gambitstudio.data.model.PieceType
import com.mhrahmankwoshik.gambitstudio.data.model.Square

object MoveGenerator {

    private val KNIGHT_OFFSETS = arrayOf(
        Pair(1, 2), Pair(2, 1), Pair(2, -1), Pair(1, -2),
        Pair(-1, -2), Pair(-2, -1), Pair(-2, 1), Pair(-1, 2)
    )

    private val BISHOP_DIRECTIONS = arrayOf(
        Pair(1, 1), Pair(1, -1), Pair(-1, 1), Pair(-1, -1)
    )

    private val ROOK_DIRECTIONS = arrayOf(
        Pair(1, 0), Pair(-1, 0), Pair(0, 1), Pair(0, -1)
    )

    private val KING_OFFSETS = arrayOf(
        Pair(1, 0), Pair(-1, 0), Pair(0, 1), Pair(0, -1),
        Pair(1, 1), Pair(1, -1), Pair(-1, 1), Pair(-1, -1)
    )

    fun isSquareAttacked(board: ChessBoard, square: Square, byColor: PieceColor): Boolean {
        val f = square.file
        val r = square.rank

        // 1. Attacked by pawns
        val pawnPusher = if (byColor == PieceColor.WHITE) -1 else 1
        val pawnRank = r + pawnPusher
        if (pawnRank in 0..7) {
            if (f - 1 in 0..7) {
                val p = board.getPiece(f - 1, pawnRank)
                if (p != null && p.color == byColor && p.type == PieceType.PAWN) return true
            }
            if (f + 1 in 0..7) {
                val p = board.getPiece(f + 1, pawnRank)
                if (p != null && p.color == byColor && p.type == PieceType.PAWN) return true
            }
        }

        // 2. Attacked by knights
        for (offset in KNIGHT_OFFSETS) {
            val nf = f + offset.first
            val nr = r + offset.second
            if (nf in 0..7 && nr in 0..7) {
                val p = board.getPiece(nf, nr)
                if (p != null && p.color == byColor && p.type == PieceType.KNIGHT) return true
            }
        }

        // 3. Attacked by bishops / queens (diagonals)
        for (dir in BISHOP_DIRECTIONS) {
            var step = 1
            while (true) {
                val df = f + dir.first * step
                val dr = r + dir.second * step
                if (df !in 0..7 || dr !in 0..7) break
                val p = board.getPiece(df, dr)
                if (p != null) {
                    if (p.color == byColor && (p.type == PieceType.BISHOP || p.type == PieceType.QUEEN)) {
                        return true
                    }
                    break
                }
                step++
            }
        }

        // 4. Attacked by rooks / queens (orthogonals)
        for (dir in ROOK_DIRECTIONS) {
            var step = 1
            while (true) {
                val df = f + dir.first * step
                val dr = r + dir.second * step
                if (df !in 0..7 || dr !in 0..7) break
                val p = board.getPiece(df, dr)
                if (p != null) {
                    if (p.color == byColor && (p.type == PieceType.ROOK || p.type == PieceType.QUEEN)) {
                        return true
                    }
                    break
                }
                step++
            }
        }

        // 5. Attacked by king
        for (offset in KING_OFFSETS) {
            val kf = f + offset.first
            val kr = r + offset.second
            if (kf in 0..7 && kr in 0..7) {
                val p = board.getPiece(kf, kr)
                if (p != null && p.color == byColor && p.type == PieceType.KING) return true
            }
        }

        return false
    }

    fun isKingInCheck(board: ChessBoard, color: PieceColor): Boolean {
        val kingSquare = board.findKing(color) ?: return false
        return isSquareAttacked(board, kingSquare, color.opposite())
    }

    fun generateLegalMoves(board: ChessBoard): List<Move> {
        val pseudoMoves = generatePseudoLegalMoves(board)
        val legalMoves = mutableListOf<Move>()
        val color = board.activeColor

        for (move in pseudoMoves) {
            val clone = board.copy()
            clone.applyMove(move)
            if (!isKingInCheck(clone, color)) {
                // Calculate SAN
                move.san = formatSan(board, move, clone)
                legalMoves.add(move)
            }
        }

        return legalMoves
    }

    private fun generatePseudoLegalMoves(board: ChessBoard): List<Move> {
        val moves = mutableListOf<Move>()
        val color = board.activeColor

        for (f in 0..7) {
            for (r in 0..7) {
                val piece = board.getPiece(f, r) ?: continue
                if (piece.color != color) continue
                val from = Square(f, r)

                when (piece.type) {
                    PieceType.PAWN -> generatePawnMoves(board, from, piece, moves)
                    PieceType.KNIGHT -> generateKnightMoves(board, from, moves)
                    PieceType.BISHOP -> generateSlidingMoves(board, from, BISHOP_DIRECTIONS, moves)
                    PieceType.ROOK -> generateSlidingMoves(board, from, ROOK_DIRECTIONS, moves)
                    PieceType.QUEEN -> {
                        generateSlidingMoves(board, from, BISHOP_DIRECTIONS, moves)
                        generateSlidingMoves(board, from, ROOK_DIRECTIONS, moves)
                    }
                    PieceType.KING -> generateKingMoves(board, from, piece, moves)
                }
            }
        }

        return moves
    }

    private fun generatePawnMoves(
        board: ChessBoard,
        from: Square,
        pawn: Piece,
        moves: MutableList<Move>
    ) {
        val isWhite = pawn.color == PieceColor.WHITE
        val forward = if (isWhite) 1 else -1
        val startRank = if (isWhite) 1 else 6
        val promoRank = if (isWhite) 7 else 0

        // Single forward move
        val oneStepRank = from.rank + forward
        if (oneStepRank in 0..7 && board.getPiece(from.file, oneStepRank) == null) {
            val to = Square(from.file, oneStepRank)
            if (oneStepRank == promoRank) {
                // Promotions
                for (promo in listOf(PieceType.QUEEN, PieceType.ROOK, PieceType.BISHOP, PieceType.KNIGHT)) {
                    moves.add(Move(from = from, to = to, promotion = promo))
                }
            } else {
                moves.add(Move(from = from, to = to))
                // Double forward move
                val twoStepRank = from.rank + 2 * forward
                if (from.rank == startRank && board.getPiece(from.file, twoStepRank) == null) {
                    moves.add(Move(from = from, to = Square(from.file, twoStepRank)))
                }
            }
        }

        // Diagonal captures
        for (df in listOf(-1, 1)) {
            val toFile = from.file + df
            val toRank = from.rank + forward
            if (toFile in 0..7 && toRank in 0..7) {
                val targetPiece = board.getPiece(toFile, toRank)
                val to = Square(toFile, toRank)

                // Regular capture
                if (targetPiece != null && targetPiece.color != pawn.color) {
                    if (toRank == promoRank) {
                        for (promo in listOf(PieceType.QUEEN, PieceType.ROOK, PieceType.BISHOP, PieceType.KNIGHT)) {
                            moves.add(Move(from = from, to = to, promotion = promo, capturedPiece = targetPiece))
                        }
                    } else {
                        moves.add(Move(from = from, to = to, capturedPiece = targetPiece))
                    }
                }

                // En passant capture
                if (board.enPassantTarget != null && to == board.enPassantTarget) {
                    val epCapturedRank = if (isWhite) 4 else 3
                    val epPiece = board.getPiece(toFile, epCapturedRank)
                    moves.add(Move(from = from, to = to, isEnPassant = true, capturedPiece = epPiece))
                }
            }
        }
    }

    private fun generateKnightMoves(board: ChessBoard, from: Square, moves: MutableList<Move>) {
        val color = board.activeColor
        for (offset in KNIGHT_OFFSETS) {
            val tf = from.file + offset.first
            val tr = from.rank + offset.second
            if (tf in 0..7 && tr in 0..7) {
                val target = board.getPiece(tf, tr)
                if (target == null || target.color != color) {
                    moves.add(Move(from = from, to = Square(tf, tr), capturedPiece = target))
                }
            }
        }
    }

    private fun generateSlidingMoves(
        board: ChessBoard,
        from: Square,
        directions: Array<Pair<Int, Int>>,
        moves: MutableList<Move>
    ) {
        val color = board.activeColor
        for (dir in directions) {
            var step = 1
            while (true) {
                val tf = from.file + dir.first * step
                val tr = from.rank + dir.second * step
                if (tf !in 0..7 || tr !in 0..7) break
                val target = board.getPiece(tf, tr)
                if (target == null) {
                    moves.add(Move(from = from, to = Square(tf, tr)))
                } else {
                    if (target.color != color) {
                        moves.add(Move(from = from, to = Square(tf, tr), capturedPiece = target))
                    }
                    break
                }
                step++
            }
        }
    }

    private fun generateKingMoves(
        board: ChessBoard,
        from: Square,
        king: Piece,
        moves: MutableList<Move>
    ) {
        val color = king.color
        val opponentColor = color.opposite()

        // Regular 1-square moves
        for (offset in KING_OFFSETS) {
            val tf = from.file + offset.first
            val tr = from.rank + offset.second
            if (tf in 0..7 && tr in 0..7) {
                val target = board.getPiece(tf, tr)
                if (target == null || target.color != color) {
                    moves.add(Move(from = from, to = Square(tf, tr), capturedPiece = target))
                }
            }
        }

        // Castling moves
        if (isSquareAttacked(board, from, opponentColor)) {
            return // Cannot castle out of check
        }

        val rank = if (color == PieceColor.WHITE) 0 else 7
        val canKingSide = if (color == PieceColor.WHITE) board.whiteKingSideCastle else board.blackKingSideCastle
        val canQueenSide = if (color == PieceColor.WHITE) board.whiteQueenSideCastle else board.blackQueenSideCastle

        if (from == Square(4, rank)) {
            // Kingside (e1->g1 or e8->g8)
            if (canKingSide &&
                board.getPiece(5, rank) == null &&
                board.getPiece(6, rank) == null &&
                board.getPiece(7, rank)?.type == PieceType.ROOK &&
                !isSquareAttacked(board, Square(5, rank), opponentColor) &&
                !isSquareAttacked(board, Square(6, rank), opponentColor)
            ) {
                moves.add(Move(from = from, to = Square(6, rank), isCastling = true))
            }

            // Queenside (e1->c1 or e8->c8)
            if (canQueenSide &&
                board.getPiece(3, rank) == null &&
                board.getPiece(2, rank) == null &&
                board.getPiece(1, rank) == null &&
                board.getPiece(0, rank)?.type == PieceType.ROOK &&
                !isSquareAttacked(board, Square(3, rank), opponentColor) &&
                !isSquareAttacked(board, Square(2, rank), opponentColor)
            ) {
                moves.add(Move(from = from, to = Square(2, rank), isCastling = true))
            }
        }
    }

    fun getGameStatus(board: ChessBoard): GameStatus {
        val legalMoves = generateLegalMoves(board)
        if (legalMoves.isEmpty()) {
            return if (isKingInCheck(board, board.activeColor)) {
                GameStatus.CHECKMATE
            } else {
                GameStatus.STALEMATE
            }
        }

        if (board.halfMoveClock >= 100) {
            return GameStatus.DRAW_FIFTY_MOVES
        }

        if (isInsufficientMaterial(board)) {
            return GameStatus.DRAW_INSUFFICIENT_MATERIAL
        }

        return GameStatus.ACTIVE
    }

    fun isInsufficientMaterial(board: ChessBoard): Boolean {
        val pieces = mutableListOf<Piece>()
        for (f in 0..7) {
            for (r in 0..7) {
                board.getPiece(f, r)?.let { pieces.add(it) }
            }
        }

        // K vs K
        if (pieces.size == 2) return true

        // K+B vs K or K+N vs K
        if (pieces.size == 3) {
            val nonKings = pieces.filter { it.type != PieceType.KING }
            if (nonKings.size == 1) {
                val t = nonKings[0].type
                return t == PieceType.BISHOP || t == PieceType.KNIGHT
            }
        }

        return false
    }

    private fun formatSan(before: ChessBoard, move: Move, after: ChessBoard): String {
        val piece = before.getPiece(move.from) ?: return move.uci

        if (move.isCastling) {
            val res = if (move.to.file == 6) "O-O" else "O-O-O"
            return if (isKingInCheck(after, after.activeColor)) {
                if (generateLegalMoves(after).isEmpty()) "$res#" else "$res+"
            } else res
        }

        val sb = StringBuilder()
        if (piece.type != PieceType.PAWN) {
            sb.append(piece.type.symbol.uppercaseChar())
            // Disambiguation if multiple pieces of same type can reach move.to
            val candidates = generatePseudoLegalMoves(before).filter {
                it.to == move.to &&
                        it.from != move.from &&
                        before.getPiece(it.from)?.type == piece.type
            }
            if (candidates.isNotEmpty()) {
                val sameFile = candidates.any { it.from.file == move.from.file }
                val sameRank = candidates.any { it.from.rank == move.from.rank }
                if (!sameFile) {
                    sb.append(('a' + move.from.file))
                } else if (!sameRank) {
                    sb.append(move.from.rank + 1)
                } else {
                    sb.append(move.from.notation)
                }
            }
        } else {
            if (move.capturedPiece != null || move.isEnPassant) {
                sb.append(('a' + move.from.file))
            }
        }

        if (move.capturedPiece != null || move.isEnPassant) {
            sb.append('x')
        }

        sb.append(move.to.notation)

        if (move.promotion != null) {
            sb.append('=').append(move.promotion.symbol.uppercaseChar())
        }

        if (isKingInCheck(after, after.activeColor)) {
            val hasMoves = generateLegalMoves(after).isNotEmpty()
            sb.append(if (hasMoves) "+" else "#")
        }

        return sb.toString()
    }
}
