package com.mhrahmankwoshik.gambitstudio.game

import com.mhrahmankwoshik.gambitstudio.data.model.GameStatus
import com.mhrahmankwoshik.gambitstudio.data.model.Move
import com.mhrahmankwoshik.gambitstudio.data.model.MoveRecord
import com.mhrahmankwoshik.gambitstudio.data.model.PieceColor
import com.mhrahmankwoshik.gambitstudio.data.model.Square

class ChessGame(
    var playerColor: PieceColor = PieceColor.WHITE
) {
    var board: ChessBoard = ChessBoard()
        private set

    val moveHistory: MutableList<Move> = mutableListOf()
    private val boardHistory: MutableList<ChessBoard> = mutableListOf()

    var status: GameStatus = GameStatus.ACTIVE
        private set

    var lastMove: Move? = null
        private set

    init {
        resetGame(playerColor)
    }

    fun resetGame(newPlayerColor: PieceColor = playerColor, initialMovesUci: List<String> = emptyList()) {
        playerColor = newPlayerColor
        board = ChessBoard()
        moveHistory.clear()
        boardHistory.clear()
        boardHistory.add(board.copy())
        lastMove = null
        status = GameStatus.ACTIVE

        for (uci in initialMovesUci) {
            val legalMoves = getLegalMoves()
            val move = legalMoves.find { it.uci == uci || it.uci.startsWith(uci.take(4)) } ?: break
            boardHistory.add(board.copy())
            board.applyMove(move)
            moveHistory.add(move)
            lastMove = move
            status = MoveGenerator.getGameStatus(board)
        }
    }

    fun getMoveHistoryUci(): List<String> = moveHistory.map { it.uci }

    fun isPlayerTurn(): Boolean = status == GameStatus.ACTIVE && board.activeColor == playerColor
    fun isEngineTurn(): Boolean = status == GameStatus.ACTIVE && board.activeColor != playerColor

    fun getLegalMoves(): List<Move> = MoveGenerator.generateLegalMoves(board)

    fun getLegalMovesForSquare(square: Square): List<Move> {
        return getLegalMoves().filter { it.from == square }
    }

    fun makeMove(move: Move): Boolean {
        if (status != GameStatus.ACTIVE) return false

        val legalMoves = getLegalMoves()
        val matchingMove = legalMoves.find {
            it.from == move.from && it.to == move.to && (move.promotion == null || it.promotion == move.promotion)
        } ?: return false

        // Save snapshot before move
        boardHistory.add(board.copy())

        board.applyMove(matchingMove)
        moveHistory.add(matchingMove)
        lastMove = matchingMove

        // Update status
        status = MoveGenerator.getGameStatus(board)
        return true
    }

    fun makeEngineMove(uci: String): Move? {
        if (status != GameStatus.ACTIVE) return null

        val legalMoves = getLegalMoves()
        val matchingMove = legalMoves.find { it.uci == uci || it.uci.startsWith(uci.take(4)) } ?: return null

        boardHistory.add(board.copy())
        board.applyMove(matchingMove)
        moveHistory.add(matchingMove)
        lastMove = matchingMove

        status = MoveGenerator.getGameStatus(board)
        return matchingMove
    }

    fun undoMove(): Boolean {
        if (moveHistory.isEmpty() || boardHistory.size < 2) return false

        // If it's currently the player's turn, we undo both engine move and player move (2 steps)
        // If it's engine's turn (engine hadn't moved yet), we undo 1 step
        val stepsToRevert = if (isPlayerTurn() && moveHistory.size >= 2) 2 else 1

        for (i in 0 until stepsToRevert) {
            if (moveHistory.isNotEmpty() && boardHistory.size > 1) {
                moveHistory.removeAt(moveHistory.lastIndex)
                boardHistory.removeAt(boardHistory.lastIndex)
            }
        }

        board = boardHistory.last().copy()
        lastMove = moveHistory.lastOrNull()
        status = MoveGenerator.getGameStatus(board)
        return true
    }

    fun resign() {
        if (status == GameStatus.ACTIVE) {
            status = GameStatus.RESIGNED
        }
    }

    fun getMoveRecords(): List<MoveRecord> {
        val records = mutableListOf<MoveRecord>()
        var moveNum = 1
        var i = 0
        while (i < moveHistory.size) {
            val white = moveHistory[i].san
            val black = if (i + 1 < moveHistory.size) moveHistory[i + 1].san else null
            records.add(MoveRecord(moveNumber = moveNum, whiteMove = white, blackMove = black))
            moveNum++
            i += 2
        }
        return records
    }

    fun toPgn(): String {
        val sb = StringBuilder()
        sb.append("[Event \"Chessy Engine Match\"]\n")
        sb.append("[Site \"Chessy Mobile\"]\n")
        sb.append("[Date \"${java.text.SimpleDateFormat("yyyy.MM.dd", java.util.Locale.US).format(java.util.Date())}\"]\n")
        sb.append("[White \"${if (playerColor == PieceColor.WHITE) "Player" else "Stockfish"}\"]\n")
        sb.append("[Black \"${if (playerColor == PieceColor.BLACK) "Player" else "Stockfish"}\"]\n")
        val result = when (status) {
            GameStatus.CHECKMATE -> if (board.activeColor == PieceColor.WHITE) "0-1" else "1-0"
            GameStatus.RESIGNED -> if (playerColor == PieceColor.WHITE) "0-1" else "1-0"
            GameStatus.STALEMATE, GameStatus.DRAW_INSUFFICIENT_MATERIAL, GameStatus.DRAW_FIFTY_MOVES, GameStatus.DRAW_REPETITION -> "1/2-1/2"
            else -> "*"
        }
        sb.append("[Result \"$result\"]\n\n")

        val records = getMoveRecords()
        for (r in records) {
            sb.append("${r.moveNumber}. ${r.whiteMove} ")
            if (r.blackMove != null) {
                sb.append("${r.blackMove} ")
            }
        }
        sb.append(result)
        return sb.toString()
    }
}
