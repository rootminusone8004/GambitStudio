package com.mhrahmankwoshik.gambitstudio.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.mhrahmankwoshik.gambitstudio.R
import com.mhrahmankwoshik.gambitstudio.data.OpeningFileManager
import com.mhrahmankwoshik.gambitstudio.data.OpeningVariationParser
import com.mhrahmankwoshik.gambitstudio.data.model.Opening
import com.mhrahmankwoshik.gambitstudio.data.model.PieceColor
import com.mhrahmankwoshik.gambitstudio.databinding.ItemRepertoireBinding

class OpeningsAdapter(
    private var allOpenings: List<Opening>,
    private val onTrainOpening: (Opening) -> Unit,
    private val onViewOpening: (Opening) -> Unit,
    private val onExportOpening: (Opening) -> Unit,
    private val onDeleteOpening: (Opening) -> Unit
) : RecyclerView.Adapter<OpeningsAdapter.OpeningViewHolder>() {

    enum class FilterMode { ALL, WHITE, BLACK }

    private var currentFilter = FilterMode.ALL
    private var displayedOpenings: List<Opening> = allOpenings

    fun updateData(newOpenings: List<Opening>) {
        allOpenings = newOpenings
        applyFilter(currentFilter)
    }

    fun applyFilter(filter: FilterMode) {
        currentFilter = filter
        displayedOpenings = when (filter) {
            FilterMode.ALL -> allOpenings
            FilterMode.WHITE -> allOpenings.filter { it.targetColor == PieceColor.WHITE }
            FilterMode.BLACK -> allOpenings.filter { it.targetColor == PieceColor.BLACK }
        }
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OpeningViewHolder {
        val binding = ItemRepertoireBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return OpeningViewHolder(binding)
    }

    override fun onBindViewHolder(holder: OpeningViewHolder, position: Int) {
        holder.bind(displayedOpenings[position])
    }

    override fun getItemCount(): Int = displayedOpenings.size

    inner class OpeningViewHolder(
        private val binding: ItemRepertoireBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(opening: Opening) {
            val context = itemView.context
            binding.tvOpeningName.text = opening.name

            val isWhite = opening.targetColor == PieceColor.WHITE
            val sideLabel = if (isWhite) "White" else "Black"
            binding.tvSideBadge.text = sideLabel

            if (isWhite) {
                binding.tvSideBadge.setTextColor(ContextCompat.getColor(context, R.color.textPrimary))
                binding.tvSideBadge.backgroundTintList = ContextCompat.getColorStateList(context, R.color.customSurfaceElevated)
            } else {
                binding.tvSideBadge.setTextColor(ContextCompat.getColor(context, R.color.primaryOrangeLight))
                binding.tvSideBadge.backgroundTintList = ContextCompat.getColorStateList(context, R.color.customSurfaceElevated)
            }

            // Load variation count from file
            val fileContent = OpeningFileManager.getOrInitializeOpeningContent(context, opening)
            val parseResult = OpeningVariationParser.parseFileContent(fileContent)
            val variationCount = OpeningVariationParser.getLeafVariationPaths(parseResult).size
            val countText = if (variationCount == 1 && parseResult.paths.firstOrNull()?.movesSan?.isEmpty() == true) {
                "0 variation lines"
            } else {
                "$variationCount variation line(s)"
            }

            binding.tvOpeningDetails.text = "$sideLabel • $countText • ${opening.id}.txt"

            // Preview moves
            val previewMoves = if (opening.movesSan.isNotBlank()) {
                opening.movesSan
            } else {
                val firstPath = parseResult.paths.firstOrNull()?.movesSan
                if (!firstPath.isNullOrEmpty()) {
                    OpeningVariationParser.formatSanMoves(firstPath)
                } else {
                    "(Empty line - tap Train to record moves)"
                }
            }
            binding.tvOpeningMoves.text = previewMoves

            binding.root.setOnClickListener {
                onTrainOpening(opening)
            }

            binding.btnTrain.setOnClickListener {
                onTrainOpening(opening)
            }

            binding.btnViewFile.setOnClickListener {
                onViewOpening(opening)
            }

            binding.btnExport.setOnClickListener {
                onExportOpening(opening)
            }

            binding.btnDelete.setOnClickListener {
                onDeleteOpening(opening)
            }
        }
    }
}
