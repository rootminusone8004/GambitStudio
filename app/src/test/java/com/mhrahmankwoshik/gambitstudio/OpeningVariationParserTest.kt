package com.mhrahmankwoshik.gambitstudio

import com.mhrahmankwoshik.gambitstudio.data.OpeningDatabase
import com.mhrahmankwoshik.gambitstudio.data.OpeningVariationParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OpeningVariationParserTest {

    @Test
    fun testParseScotchGambitVariations() {
        val fileContent = """
            1. e4 e5 2. Nf3 Nc6 3. d4 exd4 4. Bc4
            Nf6 5. e5 Ne4 6. Qe2 Nc5 7. c3 d3
            Nf6 5. e5 Qe7 6. O-O
        """.trimIndent()

        val result = OpeningVariationParser.parseFileContent(fileContent)

        assertTrue("Should have no errors: ${result.errors}", result.errors.isEmpty())
        assertEquals("Should parse 3 variation paths", 3, result.paths.size)

        // Path 0: Base Scotch Gambit (7 half-moves)
        val path0 = result.paths[0]
        assertTrue(path0.isBaseLine)
        assertEquals(7, path0.movesUci.size)
        assertEquals(listOf("e2e4", "e7e5", "g1f3", "b8c6", "d2d4", "e5d4", "f1c4"), path0.movesUci)

        // Path 1: Base + Nf6 5. e5 Ne4 6. Qe2 Nc5 7. c3 d3 (7 + 7 = 14 half-moves)
        val path1 = result.paths[1]
        assertEquals(14, path1.movesUci.size)
        assertEquals(path0.movesUci, path1.movesUci.take(7))
        assertEquals(listOf("g8f6", "e4e5", "f6e4", "d1e2", "e4c5", "c2c3", "d4d3"), path1.movesUci.drop(7))

        // Path 2: Base + Nf6 5. e5 Qe7 6. O-O (7 + 4 = 11 half-moves)
        val path2 = result.paths[2]
        assertEquals(11, path2.movesUci.size)
        assertEquals(path0.movesUci, path2.movesUci.take(7))
        assertEquals(listOf("g8f6", "e4e5", "d8e7", "e1g1"), path2.movesUci.drop(7))
    }

    @Test
    fun testParseWithZeroZeroCastling() {
        val fileContent = """
            1. e4 e5 2. Nf3 Nc6 3. d4 exd4 4. Bc4
            Nf6 5. e5 Qe7 0-0
        """.trimIndent()

        val result = OpeningVariationParser.parseFileContent(fileContent)
        assertTrue("Should parse 0-0 castling cleanly: ${result.errors}", result.errors.isEmpty())
        assertEquals(2, result.paths.size)
        val path1 = result.paths[1]
        assertEquals("e1g1", path1.movesUci.last())
    }

    @Test
    fun testParseWithCommentsAndBlankLines() {
        val fileContent = """
            # Scotch Gambit Master File
            // Base Line:
            1. e4 e5 2. Nf3 Nc6 3. d4 exd4 4. Bc4

            # Line 1: Classical 4... Bc5
            Bc5 5. c3 dxc3 6. Bxf7+ Kxf7 7. Qd5+
        """.trimIndent()

        val result = OpeningVariationParser.parseFileContent(fileContent)
        assertTrue("Should parse with comments: ${result.errors}", result.errors.isEmpty())
        assertEquals(2, result.paths.size)
        val path1 = result.paths[1]
        // 7 base moves + 6 continuation moves = 13
        assertEquals(13, path1.movesUci.size)
    }

    @Test
    fun testIllegalMoveReportsErrorCleanly() {
        val fileContent = """
            1. e4 e5 2. Nf3 Nc6 3. d4 exd4 4. Bc4
            Nf6 5. e5 Ne4 6. Qe2 Nc5 7. c3 Qe3
        """.trimIndent()

        val result = OpeningVariationParser.parseFileContent(fileContent)
        // Qe3 is illegal for Black at move 7 in this line
        assertEquals(1, result.errors.size)
        assertTrue(result.errors[0].contains("Qe3"))
        // Valid moves up to 7. c3 were still captured
        assertEquals(2, result.paths.size)
        assertEquals(13, result.paths[1].movesUci.size)
    }

    @Test
    fun testTennisonGambitVariations() {
        val fileContent = """
            1. Nf3 d5 2. e4 dxe4 3. Ng5
            Nf6 4. Bc4 e6 5. Nc3
            e5 4. Nxe4 f5 5. Ng3
        """.trimIndent()

        val result = OpeningVariationParser.parseFileContent(fileContent)
        assertTrue("Should parse Tennison lines: ${result.errors}", result.errors.isEmpty())
        assertEquals(3, result.paths.size)
    }

    @Test
    fun testHierarchicalIndentedTreeParsing() {
        val fileContent = """
            1. e4 e5 2. Nf3 Nc6 3. d4 exd4 4. Bc4
               Be7 5. c3 dxc3 6. Qd5 Nh6 7. Bxh6
                  gxh6 8.Qf7#
                  0-0 8. Bxg7 Kxg7 9. Nxc3
               Bb4 5. c3 dxc3 6. bxc3
                  Bc5 7. Bxf7 Kxf7 8. Qd5+ Ke8 9. Qxc5
                  Ba5 7. Qb3 Qe7 8. 0-0
        """.trimIndent()

        val result = OpeningVariationParser.parseFileContent(fileContent)
        assertTrue("Should have no errors: ${result.errors}", result.errors.isEmpty())
        assertEquals("Should produce 5 paths (1 base + 4 branches)", 5, result.paths.size)

        // Path 0: Base line
        val path0 = result.paths[0]
        assertTrue(path0.isBaseLine)
        assertEquals(7, path0.movesUci.size)

        // Path 1: ends in checkmate 8. Qf7#
        val path1 = result.paths[1]
        assertEquals(15, path1.movesUci.size)
        assertEquals("d5f7", path1.movesUci.last())

        // Path 2: ends in 9. Nxc3
        val path2 = result.paths[2]
        assertEquals(17, path2.movesUci.size)
        assertEquals("b1c3", path2.movesUci.last())

        // Path 3: ends in 9. Qxc5
        val path3 = result.paths[3]
        assertEquals(17, path3.movesUci.size)
        assertEquals("d5c5", path3.movesUci.last())

        // Path 4: ends in 8. O-O
        val path4 = result.paths[4]
        assertEquals(15, path4.movesUci.size)
        assertEquals("e1g1", path4.movesUci.last())
    }
}
