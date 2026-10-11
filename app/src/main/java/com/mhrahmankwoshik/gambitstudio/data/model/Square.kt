package com.mhrahmankwoshik.gambitstudio.data.model

data class Square(val file: Int, val rank: Int) {
    init {
        require(file in 0..7 && rank in 0..7) { "Square out of bounds: file=$file, rank=$rank" }
    }

    val notation: String
        get() = "${('a' + file)}${rank + 1}"

    val isLight: Boolean
        get() = (file + rank) % 2 != 0

    companion object {
        fun fromNotation(notation: String): Square? {
            if (notation.length != 2) return null
            val f = notation[0].lowercaseChar() - 'a'
            val r = notation[1] - '1'
            if (f !in 0..7 || r !in 0..7) return null
            return Square(f, r)
        }
    }
}
