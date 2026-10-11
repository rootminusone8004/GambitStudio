package com.mhrahmankwoshik.gambitstudio.data

import android.content.Context
import com.mhrahmankwoshik.gambitstudio.data.model.Opening
import com.mhrahmankwoshik.gambitstudio.data.model.PieceColor
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object OpeningDatabase {

    private var customOpeningsCache: MutableList<Opening>? = null

    val allOpenings: List<Opening>
        get() = customOpeningsCache ?: emptyList()

    fun getOpenings(context: Context? = null): List<Opening> {
        if (customOpeningsCache != null) {
            return customOpeningsCache!!
        }
        if (context != null) {
            val loaded = loadCustomOpeningsFromDisk(context)
            customOpeningsCache = loaded.toMutableList()
            return customOpeningsCache!!
        }
        return emptyList()
    }

    fun getOpeningById(id: String, context: Context? = null): Opening? {
        return getOpenings(context).find { it.id == id }
    }

    fun saveOpening(
        context: Context,
        opening: Opening,
        fileContent: String? = null
    ): Opening {
        val openings = getOpenings(context).toMutableList()
        val index = openings.indexOfFirst { it.id == opening.id }
        if (index >= 0) {
            openings[index] = opening
        } else {
            openings.add(opening)
        }
        customOpeningsCache = openings
        saveCustomOpeningsToDisk(context, openings)

        if (fileContent != null) {
            OpeningFileManager.saveOpeningContent(context, opening, fileContent)
        } else {
            OpeningFileManager.getOrInitializeOpeningContent(context, opening)
        }

        return opening
    }

    fun deleteOpening(context: Context, openingId: String): Boolean {
        val openings = getOpenings(context).toMutableList()
        val removed = openings.removeAll { it.id == openingId }
        if (removed) {
            customOpeningsCache = openings
            saveCustomOpeningsToDisk(context, openings)
            OpeningFileManager.deleteOpeningFile(context, openingId)
        }
        return removed
    }

    fun setOpeningsForTesting(openings: List<Opening>) {
        customOpeningsCache = openings.toMutableList()
    }

    fun clearOpeningsForTesting() {
        customOpeningsCache = mutableListOf()
    }

    private fun getCustomOpeningsFile(context: Context): File {
        return File(context.filesDir, "gambit_studio_openings.json")
    }

    private fun loadCustomOpeningsFromDisk(context: Context): List<Opening> {
        val file = getCustomOpeningsFile(context)
        if (!file.exists() || file.length() == 0L) {
            return emptyList()
        }
        return try {
            val jsonStr = file.readText()
            val jsonArray = JSONArray(jsonStr)
            val list = mutableListOf<Opening>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val uciArray = obj.getJSONArray("movesUci")
                val movesUci = mutableListOf<String>()
                for (j in 0 until uciArray.length()) {
                    movesUci.add(uciArray.getString(j))
                }
                list.add(
                    Opening(
                        id = obj.getString("id"),
                        name = obj.getString("name"),
                        eco = obj.optString("eco", ""),
                        targetColor = PieceColor.valueOf(obj.getString("targetColor")),
                        movesUci = movesUci,
                        movesSan = obj.getString("movesSan"),
                        description = obj.optString("description", ""),
                        isUserFavorite = obj.optBoolean("isUserFavorite", true)
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun saveCustomOpeningsToDisk(context: Context, openings: List<Opening>) {
        try {
            val jsonArray = JSONArray()
            for (op in openings) {
                val obj = JSONObject()
                obj.put("id", op.id)
                obj.put("name", op.name)
                obj.put("eco", op.eco)
                obj.put("targetColor", op.targetColor.name)
                val uciArray = JSONArray()
                op.movesUci.forEach { uciArray.put(it) }
                obj.put("movesUci", uciArray)
                obj.put("movesSan", op.movesSan)
                obj.put("description", op.description)
                obj.put("isUserFavorite", op.isUserFavorite)
                jsonArray.put(obj)
            }
            val file = getCustomOpeningsFile(context)
            file.writeText(jsonArray.toString(2))
        } catch (e: Exception) {
            // Ignored
        }
    }
}
