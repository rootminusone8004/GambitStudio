package com.mhrahmankwoshik.gambitstudio.data

import android.content.Context
import android.net.Uri
import com.mhrahmankwoshik.gambitstudio.data.model.Opening
import java.io.File
import java.util.concurrent.ConcurrentHashMap

object OpeningFileManager {

    private val pathsCache = ConcurrentHashMap<String, List<List<String>>>()

    private fun getOpeningsDirectory(context: Context): File {
        val dir = File(context.filesDir, "openings")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun getOpeningFile(context: Context, openingId: String): File {
        return File(getOpeningsDirectory(context), "$openingId.txt")
    }

    /**
     * Retrieves the file content for an opening.
     * If the file does not exist yet, creates the default file with base line 1
     * (and initial variation examples for favorites like Scotch Gambit).
     */
    fun getOrInitializeOpeningContent(context: Context, opening: Opening): String {
        val file = getOpeningFile(context, opening.id)
        if (file.exists() && file.length() > 0) {
            return try {
                file.readText()
            } catch (e: Exception) {
                generateDefaultContent(opening)
            }
        }

        val defaultContent = generateDefaultContent(opening)
        try {
            file.writeText(defaultContent)
        } catch (e: Exception) {
            // In case of write failure, still return the generated string
        }
        return defaultContent
    }

    /**
     * Generates default content where Line 1 is the base moves.
     */
    fun generateDefaultContent(opening: Opening): String {
        return if (opening.id == "scotch_gambit") {
            """
1. e4 e5 2. Nf3 Nc6 3. d4 exd4 4. Bc4
   Be7 5. c3 dxc3 6. Qd5 Nh6 7. Bxh6
      gxh6 8.Qf7#
      0-0 8. Bxg7 Kxg7 9. Nxc3
   Bb4 5. c3 dxc3 6. bxc3
      Bc5 7. Bxf7 Kxf7 8. Qd5+ Ke8 9. Qxc5
      Ba5 7. Qb3 Qe7 8. 0-0
            """.trimIndent()
        } else {
            """
${opening.movesSan}
# Use indentation to branch continuation lines without repeating moves:
#    Continuation branch 1
#       Sub-branch 1a
#       Sub-branch 1b
#    Continuation branch 2
            """.trimIndent()
        }
    }

    /**
     * Saves content to file, parses it, updates cache, and returns ParseResult.
     */
    fun saveOpeningContent(context: Context, opening: Opening, content: String): ParseResult {
        val file = getOpeningFile(context, opening.id)
        file.writeText(content)

        val parseResult = OpeningVariationParser.parseFileContent(content, opening)
        val validUciPaths = parseResult.paths.map { it.movesUci }
        pathsCache[opening.id] = if (validUciPaths.isNotEmpty()) validUciPaths else listOf(opening.movesUci)

        return parseResult
    }

    /**
     * Loads the variation paths for an opening, using cache if available.
     * Guaranteed to return at least [opening.movesUci].
     */
    fun loadVariationPaths(context: Context, opening: Opening): List<List<String>> {
        pathsCache[opening.id]?.let { return it }

        val content = getOrInitializeOpeningContent(context, opening)
        val parseResult = OpeningVariationParser.parseFileContent(content, opening)
        val validUciPaths = parseResult.paths.map { it.movesUci }

        val finalPaths = if (validUciPaths.isNotEmpty()) validUciPaths else listOf(opening.movesUci)
        pathsCache[opening.id] = finalPaths
        return finalPaths
    }

    /**
     * Checks if the user has custom continuation lines defined for this opening.
     */
    fun hasCustomPaths(context: Context, opening: Opening): Boolean {
        val paths = loadVariationPaths(context, opening)
        return paths.size > 1 || (paths.isNotEmpty() && paths[0].size > opening.movesUci.size)
    }

    /**
     * Resets opening file to default.
     */
    fun resetToDefault(context: Context, opening: Opening): ParseResult {
        val defaultContent = generateDefaultContent(opening)
        return saveOpeningContent(context, opening, defaultContent)
    }

    /**
     * Deletes the variation file and clears from cache.
     */
    fun deleteOpeningFile(context: Context, openingId: String) {
        pathsCache.remove(openingId)
        val file = getOpeningFile(context, openingId)
        if (file.exists()) {
            file.delete()
        }
    }

    fun exportContentToUri(context: Context, uri: Uri, content: String) {
        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
            outputStream.write(content.toByteArray(Charsets.UTF_8))
        }
    }

    fun readContentFromUri(context: Context, uri: Uri): String {
        return context.contentResolver.openInputStream(uri)?.use { inputStream ->
            inputStream.bufferedReader(Charsets.UTF_8).readText()
        } ?: ""
    }
}
