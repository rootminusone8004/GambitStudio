package com.mhrahmankwoshik.gambitstudio.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.mhrahmankwoshik.gambitstudio.data.model.MoveRecord
import com.mhrahmankwoshik.gambitstudio.databinding.ItemMoveHistoryBinding

class MoveHistoryAdapter : ListAdapter<MoveRecord, MoveHistoryAdapter.ViewHolder>(DiffCallback) {

    class ViewHolder(private val binding: ItemMoveHistoryBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(record: MoveRecord) {
            binding.tvMoveNumber.text = "${record.moveNumber}."
            binding.tvWhiteMove.text = record.whiteMove ?: ""
            binding.tvBlackMove.text = record.blackMove ?: ""
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemMoveHistoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    companion object DiffCallback : DiffUtil.ItemCallback<MoveRecord>() {
        override fun areItemsTheSame(oldItem: MoveRecord, newItem: MoveRecord): Boolean =
            oldItem.moveNumber == newItem.moveNumber

        override fun areContentsTheSame(oldItem: MoveRecord, newItem: MoveRecord): Boolean =
            oldItem == newItem
    }
}
