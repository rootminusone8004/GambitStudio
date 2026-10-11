package com.mhrahmankwoshik.gambitstudio.data.model

data class Move(
    val from: Square,
    val to: Square,
    val promotion: PieceType? = null,
    val isCastling: Boolean = false,
    val isEnPassant: Boolean = false,
    val capturedPiece: Piece? = null,
    var san: String = ""
) {
    val uci: String
        get() = "${from.notation}${to.notation}${promotion?.symbol?.lowercaseChar() ?: ""}"

    companion object {
        fun fromUci(uci: String, captured: Piece? = null): Move? {
            val clean = uci.trim()
            if (clean.length < 4) return null
            val from = Square.fromNotation(clean.substring(0, 2)) ?: return null
            val to = Square.fromNotation(clean.substring(2, 4)) ?: return null
            val promo = if (clean.length >= 5) PieceType.fromChar(clean[4]) else null
            return Move(from = from, to = to, promotion = promo, capturedPiece = captured)
        }
    }
}
