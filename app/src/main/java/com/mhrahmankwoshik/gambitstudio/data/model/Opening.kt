package com.mhrahmankwoshik.gambitstudio.data.model

data class Opening(
    val id: String,
    val name: String,
    val eco: String = "",
    val targetColor: PieceColor, // The side that dictates or plays this opening/gambit
    val movesUci: List<String>,
    val movesSan: String,
    val description: String = "",
    val isUserFavorite: Boolean = true
)
