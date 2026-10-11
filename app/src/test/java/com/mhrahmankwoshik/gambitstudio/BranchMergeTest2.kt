
package com.mhrahmankwoshik.gambitstudio

import com.mhrahmankwoshik.gambitstudio.data.OpeningVariationParser
import org.junit.Test
import org.junit.Assert.*

class BranchMergeTest2 {
    @Test
    fun testUndoAndBranchLonger() {
        // Line 1: 1. e4 e5 2. Nf3 Nc6 3. Bc4 Bc5
        val line1 = listOf("e4", "e5", "Nf3", "Nc6", "Bc4", "Bc5")
        val content1 = OpeningVariationParser.mergeNewVariation("", line1)

        // Line 2: 1. e4 e5 2. Nf3 Nc6 3. Bc4 Nf6 4. d3 d6
        val line2 = listOf("e4", "e5", "Nf3", "Nc6", "Bc4", "Nf6", "d3", "d6")
        val content2 = OpeningVariationParser.mergeNewVariation(content1, line2)
        println("Content 2:")
        println(content2)

        val parseResult = OpeningVariationParser.parseFileContent(content2)
        assertEquals(0, parseResult.errors.size)
        assertEquals(3, parseResult.paths.size) // base + 2 branches
        assertEquals(listOf("e2e4", "e7e5", "g1f3", "b8c6", "f1c4", "f8c5"), parseResult.paths[1].movesUci)
        assertEquals(listOf("e2e4", "e7e5", "g1f3", "b8c6", "f1c4", "g8f6", "d2d3", "d7d6"), parseResult.paths[2].movesUci)
    }

    @Test
    fun testUndoMultipleMovesAndBranchEarlier() {
        // Line 1: 1. e4 e5 2. Nf3 Nc6 3. Bc4 Bc5
        val line1 = listOf("e4", "e5", "Nf3", "Nc6", "Bc4", "Bc5")
        val content1 = OpeningVariationParser.mergeNewVariation("", line1)

        // Line 2: branch at move 2! 1. e4 e5 2. f4 exf4
        val line2 = listOf("e4", "e5", "f4", "exf4")
        val content2 = OpeningVariationParser.mergeNewVariation(content1, line2)
        println("Content 2 (earlier branch):")
        println(content2)

        val parseResult = OpeningVariationParser.parseFileContent(content2)
        assertEquals(0, parseResult.errors.size)
        // Check both lines are present
        val leafPaths = OpeningVariationParser.getLeafVariationPaths(parseResult)
        assertEquals(2, leafPaths.size)
    }
}
