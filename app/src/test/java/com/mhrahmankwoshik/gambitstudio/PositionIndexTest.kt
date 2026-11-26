package com.mhrahmankwoshik.gambitstudio

import com.mhrahmankwoshik.gambitstudio.data.OpeningVariationParser
import com.mhrahmankwoshik.gambitstudio.data.PositionIndex
import com.mhrahmankwoshik.gambitstudio.game.ChessGame
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PositionIndexTest {

    private val fileContent = """
        1. e4 e5 2. Nf3 Nc6 3. Bc4
           Bc5 4. c3 Nf6 5. d4
           Nf6 4. Ng5 d5
    """.trimIndent()

    private fun indexFor(content: String): PositionIndex {
        val paths = OpeningVariationParser.parseFileContent(content).paths.map { it.movesUci }
        return PositionIndex.fromUciPaths(paths)
    }

    private fun play(vararg san: String): ChessGame {
        val game = ChessGame()
        val parsed = OpeningVariationParser.parseFileContent(san.joinToString(" "))
        assertTrue(parsed.errors.isEmpty())
        parsed.paths.first().movesUci.forEach { game.makeEngineMove(it) }
        return game
    }

    @Test
    fun recognisesPositionReachedByDifferentMoveOrder() {
        val index = indexFor(fileContent)
        val game = play("Nf3", "Nc6", "e4", "e5", "Bc4")

        assertTrue(index.isKnown(game.board))
        assertEquals(setOf("f8c5", "g8f6"), index.candidateMoves(game.board).toSet())
        assertEquals(
            setOf(listOf("Bc5", "c3", "Nf6", "d4"), listOf("Nf6", "Ng5", "d5")),
            index.continuationsFrom(game.board).toSet()
        )
    }

    @Test
    fun ignoresEnPassantSquareWhenNoCaptureIsPossible() {
        val index = indexFor("1. Nf3 d5 2. d4")
        val game = play("d4", "d5", "Nf3") // same position, no double push last
        assertTrue(index.isKnown(game.board))
    }

    @Test
    fun unknownPositionHasNoCandidates() {
        val index = indexFor(fileContent)
        val game = play("d4", "d5")
        assertFalse(index.isKnown(game.board))
        assertTrue(index.candidateMoves(game.board).isEmpty())
    }

    @Test
    fun expandGraftsRecordedContinuationsOntoNewMoveOrder() {
        val index = indexFor(fileContent)
        val game = play("Nf3", "Nc6", "e4", "e5", "Bc4")

        val expansion = index.expandWithTranspositions(game.getMoveHistoryUci())
        assertEquals(4, expansion.transpositionPly) // 1.Nf3 Nc6 2.e4 e5 == 1.e4 e5 2.Nf3 Nc6
        assertEquals(2, expansion.graftedCount)

        val merged = OpeningVariationParser.mergeNewVariations(fileContent, expansion.lines)
        val leaves = OpeningVariationParser
            .getLeafVariationPaths(OpeningVariationParser.parseFileContent(merged))
            .map { it.movesSan }
            .toSet()

        assertTrue(listOf("Nf3", "Nc6", "e4", "e5", "Bc4", "Bc5", "c3", "Nf6", "d4") in leaves)
        assertTrue(listOf("Nf3", "Nc6", "e4", "e5", "Bc4", "Nf6", "Ng5", "d5") in leaves)
        // Original lines are preserved
        assertTrue(listOf("e4", "e5", "Nf3", "Nc6", "Bc4", "Bc5", "c3", "Nf6", "d4") in leaves)
        assertTrue(listOf("e4", "e5", "Nf3", "Nc6", "Bc4", "Nf6", "Ng5", "d5") in leaves)
    }

    @Test
    fun expandWithoutTranspositionReturnsOnlyPlayedLine() {
        val index = indexFor(fileContent)
        val game = play("e4", "e5", "Nf3", "Nc6", "Bc4", "Be7")

        val expansion = index.expandWithTranspositions(game.getMoveHistoryUci())
        assertNull(expansion.transpositionPly)
        assertEquals(0, expansion.graftedCount)
        assertEquals(listOf(listOf("e4", "e5", "Nf3", "Nc6", "Bc4", "Be7")), expansion.lines)
    }

    @Test
    fun fileKeepsLinesWithDifferentFirstMoves() {
        val merged = OpeningVariationParser.mergeNewVariations(
            "1. e4 e5 2. Nf3", listOf(listOf("Nf3", "e5", "e4"))
        )
        val result = OpeningVariationParser.parseFileContent(merged)
        assertTrue(result.errors.isEmpty())
        val leaves = OpeningVariationParser.getLeafVariationPaths(result).map { it.movesSan }.toSet()
        assertEquals(setOf(listOf("e4", "e5", "Nf3"), listOf("Nf3", "e5", "e4")), leaves)

        // Re-merging must not drop the first line
        val again = OpeningVariationParser.mergeNewVariation(merged, listOf("Nf3", "e5", "e4", "Nc6"))
        val leavesAgain = OpeningVariationParser
            .getLeafVariationPaths(OpeningVariationParser.parseFileContent(again)).map { it.movesSan }.toSet()
        assertTrue(listOf("e4", "e5", "Nf3") in leavesAgain)
    }
}
