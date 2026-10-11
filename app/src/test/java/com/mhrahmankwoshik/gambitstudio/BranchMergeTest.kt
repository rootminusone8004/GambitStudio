
package com.mhrahmankwoshik.gambitstudio

import com.mhrahmankwoshik.gambitstudio.data.OpeningVariationParser
import org.junit.Test
import org.junit.Assert.*

class BranchMergeTest {
    @Test
    fun testUndoAndBranch() {
        // Line 1 saved: 1. e4 e5 2. Nf3 Nc6 3. Bc4 Bc5
        val line1 = listOf("e4", "e5", "Nf3", "Nc6", "Bc4", "Bc5")
        val content1 = OpeningVariationParser.mergeNewVariation("", line1)
        println("Content 1:")
        println(content1)

        // Now user undid 3... Bc5 and played 3... Nf6
        val line2 = listOf("e4", "e5", "Nf3", "Nc6", "Bc4", "Nf6")
        val content2 = OpeningVariationParser.mergeNewVariation(content1, line2)
        println("\nContent 2 after branching:")
        println(content2)

        val parseResult = OpeningVariationParser.parseFileContent(content2)
        println("\nPaths parsed:")
        for ((idx, p) in parseResult.paths.withIndex()) {
            println("Path : SAN=" + p.movesSan + " UCI=" + p.movesUci)
        }
        assertEquals(0, parseResult.errors.size)
        // We expect at least the 2 branches to be parsed
        assertTrue(parseResult.paths.size >= 2)
    }
}
