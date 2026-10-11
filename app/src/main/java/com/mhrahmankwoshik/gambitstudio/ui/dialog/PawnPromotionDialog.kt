package com.mhrahmankwoshik.gambitstudio.ui.dialog

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.Window
import com.mhrahmankwoshik.gambitstudio.R
import com.mhrahmankwoshik.gambitstudio.data.model.PieceColor
import com.mhrahmankwoshik.gambitstudio.data.model.PieceType
import com.mhrahmankwoshik.gambitstudio.databinding.DialogPawnPromotionBinding

class PawnPromotionDialog(
    context: Context,
    private val color: PieceColor,
    private val onSelected: (PieceType) -> Unit
) : Dialog(context) {

    private lateinit var binding: DialogPawnPromotionBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        binding = DialogPawnPromotionBinding.inflate(layoutInflater)
        setContentView(binding.root)
        window?.setBackgroundDrawableResource(android.R.color.transparent)
        setCancelable(false)

        val isWhite = color == PieceColor.WHITE
        binding.btnPromoteQueen.setImageResource(if (isWhite) R.drawable.ic_chess_wq else R.drawable.ic_chess_bq)
        binding.btnPromoteRook.setImageResource(if (isWhite) R.drawable.ic_chess_wr else R.drawable.ic_chess_br)
        binding.btnPromoteBishop.setImageResource(if (isWhite) R.drawable.ic_chess_wb else R.drawable.ic_chess_bb)
        binding.btnPromoteKnight.setImageResource(if (isWhite) R.drawable.ic_chess_wn else R.drawable.ic_chess_bn)

        binding.btnPromoteQueen.setOnClickListener {
            onSelected(PieceType.QUEEN)
            dismiss()
        }
        binding.btnPromoteRook.setOnClickListener {
            onSelected(PieceType.ROOK)
            dismiss()
        }
        binding.btnPromoteBishop.setOnClickListener {
            onSelected(PieceType.BISHOP)
            dismiss()
        }
        binding.btnPromoteKnight.setOnClickListener {
            onSelected(PieceType.KNIGHT)
            dismiss()
        }
    }
}
