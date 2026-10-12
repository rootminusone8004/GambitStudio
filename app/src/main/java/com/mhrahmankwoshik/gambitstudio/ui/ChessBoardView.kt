package com.mhrahmankwoshik.gambitstudio.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.util.AttributeSet
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.mhrahmankwoshik.gambitstudio.R
import com.mhrahmankwoshik.gambitstudio.data.model.Move
import com.mhrahmankwoshik.gambitstudio.data.model.Piece
import com.mhrahmankwoshik.gambitstudio.data.model.PieceColor
import com.mhrahmankwoshik.gambitstudio.data.model.PieceType
import com.mhrahmankwoshik.gambitstudio.data.model.Square
import com.mhrahmankwoshik.gambitstudio.game.ChessBoard
import kotlin.math.min

class ChessBoardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // Board state
    var board: ChessBoard = ChessBoard()
        set(value) {
            field = value
            invalidate()
        }

    var isFlipped: Boolean = false
        set(value) {
            field = value
            invalidate()
        }

    var lastMove: Move? = null
        set(value) {
            field = value
            invalidate()
        }

    var isKingInCheck: Boolean = false
        set(value) {
            field = value
            invalidate()
        }

    var checkKingSquare: Square? = null
        set(value) {
            field = value
            invalidate()
        }

    var legalMoves: List<Move> = emptyList()
        set(value) {
            field = value
            invalidate()
        }

    var selectedSquare: Square? = null
        set(value) {
            field = value
            invalidate()
        }

    // Callbacks
    var onSquareSelectedListener: ((Square) -> Unit)? = null
    var onMoveAttemptListener: ((Square, Square) -> Unit)? = null

    // Touch & Drag state
    private var isDragging = false
    private var dragStartSquare: Square? = null
    private var dragCurrentX = 0f
    private var dragCurrentY = 0f

    // Dimensions
    private var squareSize = 0f
    private var boardRect = RectF()

    // Paints
    private val lightSquarePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.lichess_board_light)
    }
    private val darkSquarePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.lichess_board_dark)
    }
    private val lastMoveLightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.lichess_last_move_light)
    }
    private val lastMoveDarkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.lichess_last_move_dark)
    }
    private val selectedSquareLightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.lichess_selected_square_light)
    }
    private val selectedSquareDarkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.lichess_selected_square_dark)
    }
    private val checkSquarePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.lichess_check_square)
        alpha = 230
    }
    private val legalMoveDotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.lichess_legal_move_dot)
        style = Paint.Style.FILL
    }
    private val legalMoveCaptureRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.lichess_legal_move_dot)
        style = Paint.Style.STROKE
    }
    private val coordinateLightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.lichess_coordinate_light)
        textSize = 28f
        isFakeBoldText = true
    }
    private val coordinateDarkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.lichess_coordinate_dark)
        textSize = 28f
        isFakeBoldText = true
    }

    // Piece bitmaps cache
    private val pieceBitmaps = mutableMapOf<String, Bitmap>()
    private var cachedPieceSize = 0

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = MeasureSpec.getSize(heightMeasureSpec)
        val size = min(width, height)
        setMeasuredDimension(size, size)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val size = min(w, h).toFloat()
        squareSize = size / 8f
        boardRect.set(0f, 0f, size, size)
        legalMoveCaptureRingPaint.strokeWidth = squareSize * 0.08f
        coordinateLightPaint.textSize = squareSize * 0.22f
        coordinateDarkPaint.textSize = squareSize * 0.22f
        loadPieceBitmaps(squareSize.toInt())
    }

    private fun loadPieceBitmaps(targetSize: Int) {
        if (targetSize <= 0 || targetSize == cachedPieceSize) return
        cachedPieceSize = targetSize
        pieceBitmaps.clear()

        val pieceResMap = mapOf(
            "w_p" to R.drawable.ic_chess_wp,
            "w_n" to R.drawable.ic_chess_wn,
            "w_b" to R.drawable.ic_chess_wb,
            "w_r" to R.drawable.ic_chess_wr,
            "w_q" to R.drawable.ic_chess_wq,
            "w_k" to R.drawable.ic_chess_wk,
            "b_p" to R.drawable.ic_chess_bp,
            "b_n" to R.drawable.ic_chess_bn,
            "b_b" to R.drawable.ic_chess_bb,
            "b_r" to R.drawable.ic_chess_br,
            "b_q" to R.drawable.ic_chess_bq,
            "b_k" to R.drawable.ic_chess_bk
        )

        for ((key, resId) in pieceResMap) {
            val drawable = ContextCompat.getDrawable(context, resId) ?: continue
            val bmp = drawable.toBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888)
            pieceBitmaps[key] = bmp
        }
    }

    private fun getPieceBitmap(piece: Piece): Bitmap? {
        val colorKey = if (piece.color == PieceColor.WHITE) "w" else "b"
        val typeKey = piece.type.symbol.lowercaseChar().toString()
        return pieceBitmaps["${colorKey}_${typeKey}"]
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        drawBoardSquares(canvas)
        drawSquareHighlights(canvas)
        drawCoordinates(canvas)
        drawPieces(canvas)
        drawLegalMoveHints(canvas)
        drawDraggingPiece(canvas)
    }

    private fun drawBoardSquares(canvas: Canvas) {
        for (f in 0..7) {
            for (r in 0..7) {
                val left = getSquareLeft(f)
                val top = getSquareTop(r)
                val isLight = (f + r) % 2 != 0
                val paint = if (isLight) lightSquarePaint else darkSquarePaint
                canvas.drawRect(left, top, left + squareSize, top + squareSize, paint)
            }
        }
    }

    private fun drawSquareHighlights(canvas: Canvas) {
        // Last move highlight
        lastMove?.let { move ->
            for (sq in listOf(move.from, move.to)) {
                val left = getSquareLeft(sq.file)
                val top = getSquareTop(sq.rank)
                val isLight = (sq.file + sq.rank) % 2 != 0
                val paint = if (isLight) lastMoveLightPaint else lastMoveDarkPaint
                canvas.drawRect(left, top, left + squareSize, top + squareSize, paint)
            }
        }

        // King in check highlight
        if (isKingInCheck) {
            checkKingSquare?.let { sq ->
                val left = getSquareLeft(sq.file)
                val top = getSquareTop(sq.rank)
                canvas.drawRect(left, top, left + squareSize, top + squareSize, checkSquarePaint)
            }
        }

        // Selected square highlight
        selectedSquare?.let { sq ->
            val left = getSquareLeft(sq.file)
            val top = getSquareTop(sq.rank)
            val isLight = (sq.file + sq.rank) % 2 != 0
            val paint = if (isLight) selectedSquareLightPaint else selectedSquareDarkPaint
            canvas.drawRect(left, top, left + squareSize, top + squareSize, paint)
        }
    }

    private fun drawCoordinates(canvas: Canvas) {
        // File coordinates (a-h) on bottom edge
        for (f in 0..7) {
            val displayFile = if (isFlipped) 7 - f else f
            val bottomRank = if (isFlipped) 7 else 0
            val isLight = (displayFile + bottomRank) % 2 != 0
            val paint = if (isLight) coordinateLightPaint else coordinateDarkPaint
            val char = ('a' + displayFile).toString()

            val left = getSquareLeft(displayFile) + squareSize * 0.76f
            val top = getSquareTop(bottomRank) + squareSize * 0.92f
            canvas.drawText(char, left, top, paint)
        }

        // Rank coordinates (1-8) on left edge
        for (r in 0..7) {
            val displayRank = if (isFlipped) 7 - r else r
            val leftFile = if (isFlipped) 7 else 0
            val isLight = (leftFile + displayRank) % 2 != 0
            val paint = if (isLight) coordinateLightPaint else coordinateDarkPaint
            val char = (displayRank + 1).toString()

            val left = getSquareLeft(leftFile) + squareSize * 0.08f
            val top = getSquareTop(displayRank) + squareSize * 0.28f
            canvas.drawText(char, left, top, paint)
        }
    }

    private fun drawPieces(canvas: Canvas) {
        for (f in 0..7) {
            for (r in 0..7) {
                // If dragging this piece, skip drawing on square
                if (isDragging && dragStartSquare == Square(f, r)) continue

                val piece = board.getPiece(f, r) ?: continue
                val bmp = getPieceBitmap(piece) ?: continue
                val left = getSquareLeft(f)
                val top = getSquareTop(r)
                canvas.drawBitmap(bmp, left, top, null)
            }
        }
    }

    private fun drawLegalMoveHints(canvas: Canvas) {
        val destSquares = legalMoves.map { it.to }.toSet()
        for (sq in destSquares) {
            val left = getSquareLeft(sq.file)
            val top = getSquareTop(sq.rank)
            val cx = left + squareSize / 2f
            val cy = top + squareSize / 2f

            val targetPiece = board.getPiece(sq)
            if (targetPiece != null || (board.enPassantTarget != null && sq == board.enPassantTarget)) {
                // Capture indicator: ring around square
                val radius = squareSize * 0.44f
                canvas.drawCircle(cx, cy, radius, legalMoveCaptureRingPaint)
            } else {
                // Empty destination: dot in center
                val radius = squareSize * 0.16f
                canvas.drawCircle(cx, cy, radius, legalMoveDotPaint)
            }
        }
    }

    private fun drawDraggingPiece(canvas: Canvas) {
        if (!isDragging || dragStartSquare == null) return
        val piece = board.getPiece(dragStartSquare!!) ?: return
        val bmp = getPieceBitmap(piece) ?: return

        val left = dragCurrentX - squareSize / 2f
        val top = dragCurrentY - squareSize / 2f
        canvas.drawBitmap(bmp, left, top, null)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isEnabled) return false

        val x = event.x
        val y = event.y

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                val square = getSquareAtCoords(x, y) ?: return false
                val piece = board.getPiece(square)

                // If already selected and tapped a legal destination
                if (selectedSquare != null && legalMoves.any { it.to == square }) {
                    performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    onMoveAttemptListener?.invoke(selectedSquare!!, square)
                    return true
                }

                // If tapped own piece
                if (piece != null && piece.color == board.activeColor) {
                    performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    selectedSquare = square
                    dragStartSquare = square
                    isDragging = true
                    dragCurrentX = x
                    dragCurrentY = y
                    onSquareSelectedListener?.invoke(square)
                    invalidate()
                    return true
                }

                // Deselect if tapped elsewhere
                if (selectedSquare != null) {
                    selectedSquare = null
                    legalMoves = emptyList()
                    invalidate()
                }
            }

            MotionEvent.ACTION_MOVE -> {
                if (isDragging) {
                    dragCurrentX = x
                    dragCurrentY = y
                    invalidate()
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (isDragging && dragStartSquare != null) {
                    val dropSquare = getSquareAtCoords(x, y)
                    if (dropSquare != null && dropSquare != dragStartSquare) {
                        if (legalMoves.any { it.to == dropSquare }) {
                            performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            onMoveAttemptListener?.invoke(dragStartSquare!!, dropSquare)
                        }
                    }
                }
                isDragging = false
                dragStartSquare = null
                invalidate()
            }
        }
        return true
    }

    private fun getSquareLeft(file: Int): Float {
        val col = if (isFlipped) 7 - file else file
        return col * squareSize
    }

    private fun getSquareTop(rank: Int): Float {
        val row = if (isFlipped) rank else 7 - rank
        return row * squareSize
    }

    private fun getSquareAtCoords(x: Float, y: Float): Square? {
        if (squareSize <= 0f) return null
        val col = (x / squareSize).toInt()
        val row = (y / squareSize).toInt()
        if (col !in 0..7 || row !in 0..7) return null

        val file = if (isFlipped) 7 - col else col
        val rank = if (isFlipped) row else 7 - row
        return Square(file, rank)
    }
}
