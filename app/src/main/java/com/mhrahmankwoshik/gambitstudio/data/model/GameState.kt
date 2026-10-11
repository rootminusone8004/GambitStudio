package com.mhrahmankwoshik.gambitstudio.data.model

enum class GameStatus {
    ACTIVE,
    CHECKMATE,
    STALEMATE,
    DRAW_INSUFFICIENT_MATERIAL,
    DRAW_FIFTY_MOVES,
    DRAW_REPETITION,
    RESIGNED
}

data class MoveRecord(
    val moveNumber: Int,
    val whiteMove: String?,
    val blackMove: String?
)
