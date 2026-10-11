package com.mhrahmankwoshik.gambitstudio.ui

import android.app.Dialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mhrahmankwoshik.gambitstudio.R
import com.mhrahmankwoshik.gambitstudio.data.OpeningDatabase
import com.mhrahmankwoshik.gambitstudio.data.OpeningFileManager
import com.mhrahmankwoshik.gambitstudio.data.OpeningVariationParser
import com.mhrahmankwoshik.gambitstudio.data.model.Move
import com.mhrahmankwoshik.gambitstudio.data.model.Opening
import com.mhrahmankwoshik.gambitstudio.data.model.PieceColor
import com.mhrahmankwoshik.gambitstudio.data.model.PieceType
import com.mhrahmankwoshik.gambitstudio.data.model.Square
import com.mhrahmankwoshik.gambitstudio.databinding.ActivityTrainingBinding
import com.mhrahmankwoshik.gambitstudio.databinding.DialogCreateRepertoireBinding
import com.mhrahmankwoshik.gambitstudio.databinding.DialogTrainingFileViewBinding
import com.mhrahmankwoshik.gambitstudio.game.ChessGame
import com.mhrahmankwoshik.gambitstudio.game.MoveGenerator
import com.mhrahmankwoshik.gambitstudio.ui.dialog.PawnPromotionDialog
import com.mhrahmankwoshik.gambitstudio.util.SoundPlayer
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class TrainingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTrainingBinding
    private val moveHistoryAdapter = MoveHistoryAdapter()

    private var currentOpening: Opening? = null
    private var knownUciPaths = mutableListOf<List<String>>()

    private val chessGame = ChessGame(playerColor = PieceColor.WHITE)
    private var lastMoveWasAutoPlay = false
    private var autoPlayJob: Job? = null
    private var pendingPromotion: Pair<Square, Square>? = null

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

    companion object {
        const val EXTRA_OPENING_ID = "EXTRA_OPENING_ID"

        fun start(context: Context, openingId: String? = null) {
            val intent = Intent(context, TrainingActivity::class.java).apply {
                if (openingId != null) {
                    putExtra(EXTRA_OPENING_ID, openingId)
                }
            }
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTrainingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupBoardListeners()
        setupButtons()

        val openingId = intent.getStringExtra(EXTRA_OPENING_ID)
        val initialOpening = if (openingId != null) {
            OpeningDatabase.getOpeningById(openingId, this)
        } else null

        if (initialOpening != null) {
            loadOpening(initialOpening)
        } else {
            showCreateOpeningDialog()
        }
    }

    private fun setupRecyclerView() {
        binding.rvMoveHistory.apply {
            adapter = moveHistoryAdapter
            layoutManager = LinearLayoutManager(this@TrainingActivity, LinearLayoutManager.HORIZONTAL, false)
        }
    }

    private fun setupBoardListeners() {
        binding.chessBoardView.onSquareSelectedListener = { square ->
            val legalMoves = MoveGenerator.generateLegalMoves(chessGame.board)
                .filter { it.from == square }
            binding.chessBoardView.legalMoves = legalMoves
        }

        binding.chessBoardView.onMoveAttemptListener = { from, to ->
            binding.chessBoardView.selectedSquare = null
            binding.chessBoardView.legalMoves = emptyList()
            handleMoveAttempt(from, to)
        }
    }

    private fun setupButtons() {
        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnViewFile.setOnClickListener {
            showViewFileDialog()
        }

        binding.btnSwitchRepertoire.setOnClickListener {
            showSwitchOpeningDialog()
        }

        binding.btnUndo.setOnClickListener {
            undoMove()
        }

        binding.btnFlipBoard.setOnClickListener {
            binding.chessBoardView.isFlipped = !binding.chessBoardView.isFlipped
            binding.chessBoardView.invalidate()
        }

        binding.btnResetLine.setOnClickListener {
            resetCurrentLine()
        }

        binding.btnSaveLine.setOnClickListener {
            saveCurrentLine()
        }
    }

    private fun loadOpening(opening: Opening) {
        autoPlayJob?.cancel()
        currentOpening = opening

        // Update toolbar titles
        binding.tvTrainingTitle.text = opening.name
        val colorName = if (opening.targetColor == PieceColor.WHITE) "White" else "Black"
        val paths = OpeningFileManager.loadVariationPaths(this, opening)
        knownUciPaths = paths.toMutableList()
        val lineCount = if (paths.size == 1 && paths[0].isEmpty()) 0 else paths.size
        binding.tvTrainingSubtitle.text = "$colorName • $lineCount variation line(s)"

        // Default board orientation to opening side (targetColor at bottom)
        binding.chessBoardView.isFlipped = (opening.targetColor == PieceColor.BLACK)

        resetCurrentLine()
    }

    private fun resetCurrentLine() {
        autoPlayJob?.cancel()
        chessGame.resetGame(newPlayerColor = PieceColor.WHITE)
        lastMoveWasAutoPlay = false
        pendingPromotion = null

        updateBoardView()
        updateStatusAndCheckAutoplay()
    }

    private fun handleMoveAttempt(from: Square, to: Square) {
        val piece = chessGame.board.getPiece(from) ?: return
        if (piece.color != chessGame.board.activeColor) return

        val isPawn = piece.type == PieceType.PAWN
        val isPromoRank = (piece.color == PieceColor.WHITE && to.rank == 7) ||
                (piece.color == PieceColor.BLACK && to.rank == 0)

        if (isPawn && isPromoRank) {
            val hasPromo = chessGame.getLegalMoves().any { it.from == from && it.to == to && it.promotion != null }
            if (hasPromo) {
                pendingPromotion = Pair(from, to)
                PawnPromotionDialog(this, piece.color) { promoType ->
                    confirmPromotion(promoType)
                }.show()
                return
            }
        }

        val move = Move(from = from, to = to)
        if (chessGame.makeMove(move)) {
            lastMoveWasAutoPlay = false
            onMoveExecuted(move)
        }
    }

    private fun confirmPromotion(promoType: PieceType) {
        val pending = pendingPromotion ?: return
        pendingPromotion = null
        val move = Move(from = pending.first, to = pending.second, promotion = promoType)
        if (chessGame.makeMove(move)) {
            lastMoveWasAutoPlay = false
            onMoveExecuted(move)
        }
    }

    private fun onMoveExecuted(move: Move) {
        if (move.capturedPiece != null) {
            SoundPlayer.playCapture()
        } else {
            SoundPlayer.playMove()
        }

        updateBoardView()
        updateStatusAndCheckAutoplay()
    }

    private fun updateBoardView() {
        binding.chessBoardView.board = chessGame.board.copy()
        binding.chessBoardView.lastMove = chessGame.lastMove
        val inCheck = MoveGenerator.isKingInCheck(chessGame.board, chessGame.board.activeColor)
        binding.chessBoardView.isKingInCheck = inCheck
        binding.chessBoardView.checkKingSquare = if (inCheck) chessGame.board.findKing(chessGame.board.activeColor) else null
        binding.chessBoardView.invalidate()

        moveHistoryAdapter.submitList(chessGame.getMoveRecords())
        binding.rvMoveHistory.scrollToPosition(maxOf(0, chessGame.getMoveRecords().size - 1))
    }

    private fun updateStatusAndCheckAutoplay() {
        val opening = currentOpening ?: return
        val activeColor = chessGame.board.activeColor
        val targetColor = opening.targetColor
        val historyUci = chessGame.getMoveHistoryUci()
        val currentPly = historyUci.size
        val moveNumber = (currentPly / 2) + 1

        binding.tvMoveCount.text = "Move $moveNumber (${currentPly} ply)"

        // Check matching variations in file
        val matchingPaths = knownUciPaths.filter { path ->
            path.size > currentPly && path.take(currentPly) == historyUci
        }

        // Available candidate moves at this position
        val candidateMovesUci = matchingPaths.map { it[currentPly] }.distinct()

        // Turn indicator
        val activeColorName = if (activeColor == PieceColor.WHITE) "White" else "Black"
        val isTargetColorTurn = (activeColor == targetColor)

        if (isTargetColorTurn) {
            binding.tvTurnIndicator.text = "$activeColorName to move (${opening.name} side)"
        } else {
            binding.tvTurnIndicator.text = "$activeColorName to move (Opponent side)"
        }

        // Branch chips setup
        updateBranchChips(candidateMovesUci)

        if (matchingPaths.isNotEmpty()) {
            binding.tvStatusBadge.text = "IN FILE"
            binding.tvStatusBadge.backgroundTintList = ContextCompat.getColorStateList(this, R.color.status_green)

            if (isTargetColorTurn) {
                binding.tvStatusDescription.text = "Recorded reply found. Auto-playing response…"

                autoPlayJob?.cancel()
                autoPlayJob = lifecycleScope.launch {
                    delay(300)
                    val nextUci = candidateMovesUci.first()
                    val executed = chessGame.makeEngineMove(nextUci)
                    if (executed != null) {
                        lastMoveWasAutoPlay = true
                        onMoveExecuted(executed)
                    }
                }
            } else {
                binding.tvStatusDescription.text = "Playing moves from recorded file (${matchingPaths.size} line(s))."
            }
        } else {
            binding.tvStatusBadge.text = "NEW LINE"
            binding.tvStatusBadge.backgroundTintList = ContextCompat.getColorStateList(this, R.color.primaryOrangeDark)
            binding.tvStatusDescription.text = "New variation: play both sides to record, then tap Save Line."
        }
    }

    private fun updateBranchChips(candidateMovesUci: List<String>) {
        binding.chipGroupBranches.removeAllViews()
        if (candidateMovesUci.size > 1) {
            binding.scrollBranchChips.visibility = View.VISIBLE
            for (uci in candidateMovesUci) {
                val legalMove = chessGame.getLegalMoves().find { it.uci == uci }
                val label = legalMove?.san ?: uci
                val chip = Chip(this).apply {
                    text = label
                    isClickable = true
                    setOnClickListener {
                        autoPlayJob?.cancel()
                        val executed = chessGame.makeEngineMove(uci)
                        if (executed != null) {
                            lastMoveWasAutoPlay = true
                            onMoveExecuted(executed)
                        }
                    }
                }
                binding.chipGroupBranches.addView(chip)
            }
        } else {
            binding.scrollBranchChips.visibility = View.GONE
        }
    }

    private fun undoMove() {
        autoPlayJob?.cancel()
        if (chessGame.getMoveRecords().isEmpty()) return

        val wasAuto = lastMoveWasAutoPlay
        chessGame.undoMove()

        // If the last move was an auto-play response from file, undo user move before it too
        if (wasAuto && chessGame.getMoveRecords().isNotEmpty()) {
            chessGame.undoMove()
        }
        lastMoveWasAutoPlay = false
        SoundPlayer.playMove()

        updateBoardView()
        updateStatusAndCheckAutoplay()
    }

    private fun saveCurrentLine() {
        val opening = currentOpening ?: run {
            Toast.makeText(this, "Please select or create an opening first.", Toast.LENGTH_SHORT).show()
            return
        }

        val moveRecords = chessGame.getMoveRecords()
        if (moveRecords.isEmpty()) {
            Toast.makeText(this, "No moves played yet. Play a line on the board to save.", Toast.LENGTH_SHORT).show()
            return
        }

        // Build list of SAN moves in the played sequence
        val playedSanMoves = mutableListOf<String>()
        for (record in moveRecords) {
            record.whiteMove?.let { playedSanMoves.add(it) }
            record.blackMove?.let { playedSanMoves.add(it) }
        }

        if (playedSanMoves.isEmpty()) {
            Toast.makeText(this, "No moves played.", Toast.LENGTH_SHORT).show()
            return
        }

        val existingContent = OpeningFileManager.getOrInitializeOpeningContent(this, opening)
        val updatedContent = OpeningVariationParser.mergeNewVariation(existingContent, playedSanMoves)

        val parseResult = OpeningFileManager.saveOpeningContent(this, opening, updatedContent)
        val leafPaths = OpeningVariationParser.getLeafVariationPaths(parseResult)

        // Update opening base line if empty
        val baseLine = parseResult.paths.firstOrNull { it.isBaseLine } ?: parseResult.paths.firstOrNull()
        val updatedOpening = if (baseLine != null && opening.movesUci.isEmpty()) {
            opening.copy(
                movesUci = baseLine.movesUci,
                movesSan = OpeningVariationParser.formatSanMoves(baseLine.movesSan)
            )
        } else {
            opening
        }

        OpeningDatabase.saveOpening(this, updatedOpening, updatedContent)
        currentOpening = updatedOpening

        val newPaths = OpeningFileManager.loadVariationPaths(this, updatedOpening)
        knownUciPaths = newPaths.toMutableList()

        val linesCount = leafPaths.size
        val colorName = if (updatedOpening.targetColor == PieceColor.WHITE) "White" else "Black"
        binding.tvTrainingSubtitle.text = "$colorName • $linesCount variation line(s)"

        Toast.makeText(
            this,
            "Saved! Line added to ${updatedOpening.name} ($linesCount variation lines total)",
            Toast.LENGTH_LONG
        ).show()

        updateStatusAndCheckAutoplay()
    }

    private fun showViewFileDialog() {
        val opening = currentOpening ?: return
        val content = OpeningFileManager.getOrInitializeOpeningContent(this, opening)
        val parseResult = OpeningVariationParser.parseFileContent(content)
        val leafCount = OpeningVariationParser.getLeafVariationPaths(parseResult).size
        val file = OpeningFileManager.getOpeningFile(this, opening.id)

        val dialog = Dialog(this)
        val dialogBinding = DialogTrainingFileViewBinding.inflate(layoutInflater)
        dialog.setContentView(dialogBinding.root)

        dialogBinding.tvFileName.text = "${opening.id}.txt"
        dialogBinding.tvFileSummary.text = "${opening.name} • $leafCount variation line(s)"
        dialogBinding.tvRawFileContent.text = if (content.isBlank()) "(Empty file - play and save moves to add lines)" else content
        dialogBinding.tvFilePath.text = "File: ${file.absolutePath}"

        dialogBinding.btnCopyFileContent.setOnClickListener {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("${opening.name} Opening File", content))
            Toast.makeText(this, "Copied file content to clipboard", Toast.LENGTH_SHORT).show()
        }

        dialogBinding.btnExportFile.setOnClickListener {
            pendingExportContent = content
            val exportFileName = "${opening.id}.txt"
            exportFileLauncher.launch(exportFileName)
            dialog.dismiss()
        }

        dialogBinding.btnCloseView.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
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

            loadOpening(newOpening)
            dialog.dismiss()
        }

        dialogBinding.btnCancelCreate.setOnClickListener {
            dialog.dismiss()
            if (currentOpening == null) {
                finish()
            }
        }

        dialog.setCancelable(currentOpening != null)
        dialog.show()
    }

    private fun showSwitchOpeningDialog() {
        val openings = OpeningDatabase.getOpenings(this)
        val options = mutableListOf<String>()
        options.add("➕ Create New Opening File…")
        for (op in openings) {
            val colorStr = if (op.targetColor == PieceColor.WHITE) "White" else "Black"
            options.add("${op.name} ($colorStr)")
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("Switch Opening File")
            .setItems(options.toTypedArray()) { _, which ->
                if (which == 0) {
                    showCreateOpeningDialog()
                } else {
                    val chosen = openings[which - 1]
                    loadOpening(chosen)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
