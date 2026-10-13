package com.mhrahmankwoshik.gambitstudio.ui.dialog

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.ViewGroup
import android.view.Window
import com.mhrahmankwoshik.gambitstudio.R
import com.mhrahmankwoshik.gambitstudio.data.model.PieceColor
import com.mhrahmankwoshik.gambitstudio.data.model.PieceType
import com.mhrahmankwoshik.gambitstudio.databinding.DialogPawnPromotionBinding

class PawnPromotionDialog(
    context: Context,
    private val color: PieceColor,
    private val onCancel: (() -> Unit)? = null,
    private val onSelected: (PieceType) -> Unit
) : Dialog(context) {

    private lateinit var binding: DialogPawnPromotionBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        binding = DialogPawnPromotionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        applyWindowDimensions()

        setCancelable(true)
        setCanceledOnTouchOutside(true)
        setOnCancelListener { onCancel?.invoke() }

        val isWhite = color == PieceColor.WHITE
        binding.btnPromoteQueen.setImageResource(if (isWhite) R.drawable.ic_chess_wq else R.drawable.ic_chess_bq)
        binding.btnPromoteRook.setImageResource(if (isWhite) R.drawable.ic_chess_wr else R.drawable.ic_chess_br)
        binding.btnPromoteBishop.setImageResource(if (isWhite) R.drawable.ic_chess_wb else R.drawable.ic_chess_bb)
        binding.btnPromoteKnight.setImageResource(if (isWhite) R.drawable.ic_chess_wn else R.drawable.ic_chess_bn)

        val selectQueen = {
            onSelected(PieceType.QUEEN)
            dismiss()
        }
        val selectRook = {
            onSelected(PieceType.ROOK)
            dismiss()
        }
        val selectBishop = {
            onSelected(PieceType.BISHOP)
            dismiss()
        }
        val selectKnight = {
            onSelected(PieceType.KNIGHT)
            dismiss()
        }

        binding.containerPromoteQueen.setOnClickListener { selectQueen() }
        binding.btnPromoteQueen.setOnClickListener { selectQueen() }

        binding.containerPromoteKnight.setOnClickListener { selectKnight() }
        binding.btnPromoteKnight.setOnClickListener { selectKnight() }

        binding.containerPromoteRook.setOnClickListener { selectRook() }
        binding.btnPromoteRook.setOnClickListener { selectRook() }

        binding.containerPromoteBishop.setOnClickListener { selectBishop() }
        binding.btnPromoteBishop.setOnClickListener { selectBishop() }

        binding.btnCancelPromotion.setOnClickListener {
            onCancel?.invoke()
            dismiss()
        }
    }

    override fun onStart() {
        super.onStart()
        applyWindowDimensions()
    }

    private fun applyWindowDimensions() {
        window?.apply {
            setBackgroundDrawableResource(android.R.color.transparent)
            val dm = context.resources.displayMetrics
            val maxAllowedWidth = (360 * dm.density).toInt()
            val targetWidth = (dm.widthPixels * 0.88).toInt().coerceAtMost(maxAllowedWidth)
            setLayout(targetWidth, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
    }
}
