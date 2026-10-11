package com.mhrahmankwoshik.gambitstudio.ui

import android.app.Dialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mhrahmankwoshik.gambitstudio.data.OpeningDatabase
import com.mhrahmankwoshik.gambitstudio.data.OpeningFileManager
import com.mhrahmankwoshik.gambitstudio.data.OpeningVariationParser
import com.mhrahmankwoshik.gambitstudio.data.model.Opening
import com.mhrahmankwoshik.gambitstudio.data.model.PieceColor
import com.mhrahmankwoshik.gambitstudio.databinding.ActivityMainBinding
import com.mhrahmankwoshik.gambitstudio.databinding.DialogCreateRepertoireBinding
import com.mhrahmankwoshik.gambitstudio.databinding.DialogTrainingFileViewBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: OpeningsAdapter

    private var pendingExportContent: String? = null
    private val exportFileLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain")
    ) { uri: Uri? ->
        uri?.let { destUri ->
            val content = pendingExportContent ?: return@let
            try {
                OpeningFileManager.exportContentToUri(this, destUri, content)
                Toast.makeText(this, "File exported successfully!", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(this, "Export failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private val importFileLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { importUri ->
            handleImportFile(importUri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupListeners()
    }

    override fun onResume() {
        super.onResume()
        refreshList()
    }

    private fun setupRecyclerView() {
        adapter = OpeningsAdapter(
            allOpenings = OpeningDatabase.getOpenings(this),
            onTrainOpening = { opening ->
                TrainingActivity.start(this, opening.id)
            },
            onViewOpening = { opening ->
                showViewFileDialog(opening)
            },
            onExportOpening = { opening ->
                exportOpeningFile(opening)
            },
            onDeleteOpening = { opening ->
                confirmDeleteOpening(opening)
            }
        )

        binding.rvOpenings.layoutManager = LinearLayoutManager(this)
        binding.rvOpenings.adapter = adapter
    }

    private fun setupListeners() {
        binding.btnCreateOpening.setOnClickListener {
            showCreateOpeningDialog()
        }

        binding.btnCreateFirstOpening.setOnClickListener {
            showCreateOpeningDialog()
        }

        binding.btnImportOpening.setOnClickListener {
            importFileLauncher.launch("*/*")
        }

        binding.btnAddSample.setOnClickListener {
            val sample = OpeningDatabase.saveOpening(
                this,
                Opening(
                    id = "scotch_gambit",
                    name = "Scotch Gambit",
                    eco = "C44",
                    targetColor = PieceColor.WHITE,
                    movesUci = listOf("e2e4", "e7e5", "g1f3", "b8c6", "d2d4", "e5d4", "f1c4"),
                    movesSan = "1. e4 e5 2. Nf3 Nc6 3. d4 exd4 4. Bc4",
                    description = "High-tempo gambit sacrificing d4 pawn for active king attack."
                ),
                fileContent = """
1. e4 e5 2. Nf3 Nc6 3. d4 exd4 4. Bc4
   Be7 5. c3 dxc3 6. Qd5 Nh6 7. Bxh6
      gxh6 8.Qf7#
      0-0 8. Bxg7 Kxg7 9. Nxc3
   Bb4 5. c3 dxc3 6. bxc3
      Bc5 7. Bxf7 Kxf7 8. Qd5+ Ke8 9. Qxc5
      Ba5 7. Qb3 Qe7 8. 0-0
                """.trimIndent()
            )
            refreshList()
            Toast.makeText(this, "Added sample: ${sample.name}", Toast.LENGTH_SHORT).show()
        }

        binding.chipGroupFilters.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isEmpty()) return@setOnCheckedStateChangeListener
            when (checkedIds.first()) {
                binding.chipFilterAll.id -> adapter.applyFilter(OpeningsAdapter.FilterMode.ALL)
                binding.chipFilterWhite.id -> adapter.applyFilter(OpeningsAdapter.FilterMode.WHITE)
                binding.chipFilterBlack.id -> adapter.applyFilter(OpeningsAdapter.FilterMode.BLACK)
            }
        }
    }

    private fun refreshList() {
        val openings = OpeningDatabase.getOpenings(this)
        adapter.updateData(openings)

        if (openings.isEmpty()) {
            binding.layoutEmptyState.visibility = View.VISIBLE
            binding.rvOpenings.visibility = View.GONE
            binding.tvOpeningsCount.text = "0 files"
        } else {
            binding.layoutEmptyState.visibility = View.GONE
            binding.rvOpenings.visibility = View.VISIBLE
            binding.tvOpeningsCount.text = "${openings.size} file(s)"
        }
    }

    private fun showCreateOpeningDialog() {
        val dialog = Dialog(this)
        val dialogBinding = DialogCreateRepertoireBinding.inflate(layoutInflater)
        dialog.setContentView(dialogBinding.root)

        dialogBinding.chipGroupSide.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.contains(dialogBinding.chipSideWhite.id)) {
                dialogBinding.tvSideExplanation.text =
                    "White opening. When practicing, the app auto-plays White's recorded moves. You play Black's moves (and both sides for new lines)."
            } else {
                dialogBinding.tvSideExplanation.text =
                    "Black opening. When practicing, the app auto-plays Black's recorded moves. You play White's moves (and both sides for new lines)."
            }
        }

        dialogBinding.btnStartTraining.setOnClickListener {
            val name = dialogBinding.etRepertoireName.text?.toString()?.trim() ?: ""
            if (name.isEmpty()) {
                dialogBinding.tilRepertoireName.error = "Please enter an opening name"
                return@setOnClickListener
            }

            val isWhite = dialogBinding.chipSideWhite.isChecked
            val targetColor = if (isWhite) PieceColor.WHITE else PieceColor.BLACK
            val id = name.lowercase().replace("'", "").replace(Regex("[^a-z0-9]+"), "_").trim('_')

            val newOpening = Opening(
                id = id,
                name = name,
                targetColor = targetColor,
                movesUci = emptyList(),
                movesSan = "",
                description = "Custom opening file"
            )

            val initialContent = OpeningFileManager.getOrInitializeOpeningContent(this, newOpening)
            OpeningDatabase.saveOpening(this, newOpening, initialContent)

            dialog.dismiss()
            TrainingActivity.start(this, newOpening.id)
        }

        dialogBinding.btnCancelCreate.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showViewFileDialog(opening: Opening) {
        val content = OpeningFileManager.getOrInitializeOpeningContent(this, opening)
        val parseResult = OpeningVariationParser.parseFileContent(content)
        val leafCount = OpeningVariationParser.getLeafVariationPaths(parseResult).size
        val file = OpeningFileManager.getOpeningFile(this, opening.id)

        val dialog = Dialog(this)
        val dialogBinding = DialogTrainingFileViewBinding.inflate(layoutInflater)
        dialog.setContentView(dialogBinding.root)

        dialogBinding.tvFileName.text = "${opening.id}.txt"
        dialogBinding.tvFileSummary.text = "${opening.name} • $leafCount variation line(s)"
        dialogBinding.tvRawFileContent.text = if (content.isBlank()) "(Empty file - tap Train to record moves)" else content
        dialogBinding.tvFilePath.text = "File: ${file.absolutePath}"

        dialogBinding.btnCopyFileContent.setOnClickListener {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("${opening.name} Opening File", content))
            Toast.makeText(this, "Copied to clipboard", Toast.LENGTH_SHORT).show()
        }

        dialogBinding.btnExportFile.setOnClickListener {
            dialog.dismiss()
            exportOpeningFile(opening)
        }

        dialogBinding.btnCloseView.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun exportOpeningFile(opening: Opening) {
        val content = OpeningFileManager.getOrInitializeOpeningContent(this, opening)
        pendingExportContent = content
        val defaultName = "${opening.id}.txt"
        exportFileLauncher.launch(defaultName)
    }

    private fun confirmDeleteOpening(opening: Opening) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Delete ${opening.name}?")
            .setMessage("Are you sure you want to delete this opening and its .txt file?")
            .setPositiveButton("Delete") { _, _ ->
                OpeningDatabase.deleteOpening(this, opening.id)
                refreshList()
                Toast.makeText(this, "Deleted ${opening.name}", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun handleImportFile(uri: Uri) {
        try {
            val content = OpeningFileManager.readContentFromUri(this, uri)
            if (content.isBlank()) {
                Toast.makeText(this, "Selected file is empty", Toast.LENGTH_SHORT).show()
                return
            }

            val rawFileName = uri.lastPathSegment?.substringAfterLast('/') ?: "imported_opening"
            val cleanName = rawFileName.removeSuffix(".txt").replace('_', ' ').replace('-', ' ')
                .split(" ")
                .joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }

            val id = cleanName.lowercase().replace("'", "").replace(Regex("[^a-z0-9]+"), "_").trim('_')

            val parseResult = OpeningVariationParser.parseFileContent(content)
            val baseLine = parseResult.paths.firstOrNull { it.isBaseLine } ?: parseResult.paths.firstOrNull()

            val baseUci = baseLine?.movesUci ?: emptyList()
            val baseSan = if (baseLine != null) OpeningVariationParser.formatSanMoves(baseLine.movesSan) else ""

            val newOpening = Opening(
                id = id,
                name = cleanName,
                targetColor = PieceColor.WHITE,
                movesUci = baseUci,
                movesSan = baseSan,
                description = "Imported opening file"
            )

            OpeningDatabase.saveOpening(this, newOpening, content)
            refreshList()

            Toast.makeText(this, "Imported $cleanName successfully!", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Import failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
