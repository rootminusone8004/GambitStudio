package com.mhrahmankwoshik.gambitstudio.data.model

data class Piece(val type: PieceType, val color: PieceColor) {
    val fenChar: Char
        get() {
            val c = type.symbol
            return if (color == PieceColor.WHITE) c.uppercaseChar() else c
        }

    val isWhite: Boolean
        get() = color == PieceColor.WHITE

    val isBlack: Boolean
        get() = color == PieceColor.BLACK
}
