
package com.mhrahmankwoshik.gambitstudio

import com.mhrahmankwoshik.gambitstudio.data.OpeningVariationParser
import com.mhrahmankwoshik.gambitstudio.game.ChessGame
import com.mhrahmankwoshik.gambitstudio.data.model.PieceColor
import org.junit.Test
import org.junit.Assert.*

class UndoAndBranchWorkflowTest {

    @Test
    fun testChessGameSinglePlyUndo() {
        val game = ChessGame(PieceColor.WHITE)
        // 1. e4
        game.makeEngineMove("e2e4")
        assertEquals(1, game.getMoveHistoryUci().size)
        assertEquals(PieceColor.BLACK, game.board.activeColor)

        // 1... e5
        game.makeEngineMove("e7e5")
        assertEquals(2, game.getMoveHistoryUci().size)
        assertEquals(PieceColor.WHITE, game.board.activeColor)

        // 2. Nf3
        game.makeEngineMove("g1f3")
        assertEquals(3, game.getMoveHistoryUci().size)
        assertEquals(PieceColor.BLACK, game.board.activeColor)

        // Undo single move
        val undone = game.undoSingleMove()
        assertTrue(undone)
        assertEquals(2, game.getMoveHistoryUci().size)
        assertEquals(listOf("e2e4", "e7e5"), game.getMoveHistoryUci())
        assertEquals(PieceColor.WHITE, game.board.activeColor)

        // Now play 2. f4 (King's Gambit instead of 2. Nf3)
        val move = game.makeEngineMove("f2f4")
        assertNotNull(move)
        assertEquals(3, game.getMoveHistoryUci().size)
        assertEquals(listOf("e2e4", "e7e5", "f2f4"), game.getMoveHistoryUci())
        assertEquals(PieceColor.BLACK, game.board.activeColor)
    }

    @Test
    fun testSaveThenUndoAndSaveAlternativeBranch() {
        // Step 1: User saves Line 1: 1. e4 e5 2. Nf3 Nc6 3. Bc4 Bc5
        val line1 = listOf("e4", "e5", "Nf3", "Nc6", "Bc4", "Bc5")
        var fileContent = OpeningVariationParser.mergeNewVariation("", line1)

        val parse1 = OpeningVariationParser.parseFileContent(fileContent)
        assertEquals(1, OpeningVariationParser.getLeafVariationPaths(parse1).size)

        // Step 2: User undid 1 move (Bc5) and played Nf6
        val line2 = listOf("e4", "e5", "Nf3", "Nc6", "Bc4", "Nf6")
        fileContent = OpeningVariationParser.mergeNewVariation(fileContent, line2)

        val parse2 = OpeningVariationParser.parseFileContent(fileContent)
        val leaves2 = OpeningVariationParser.getLeafVariationPaths(parse2)
        assertEquals(2, leaves2.size)

        // Step 3: User undid back to move 3, and played 3. d4 exd4 4. c3
        val line3 = listOf("e4", "e5", "Nf3", "Nc6", "d4", "exd4", "c3")
        fileContent = OpeningVariationParser.mergeNewVariation(fileContent, line3)

        val parse3 = OpeningVariationParser.parseFileContent(fileContent)
        val leaves3 = OpeningVariationParser.getLeafVariationPaths(parse3)
        assertEquals(3, leaves3.size)

        // All 3 branches are intact
        val leafUcis = leaves3.map { it.movesUci }
        assertTrue(leafUcis.any { it.contains("f8c5") }) // Line 1
        assertTrue(leafUcis.any { it.contains("g8f6") }) // Line 2
        assertTrue(leafUcis.any { it.contains("d2d4") }) // Line 3
    }
}
