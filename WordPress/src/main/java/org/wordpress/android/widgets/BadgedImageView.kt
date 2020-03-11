package org.wordpress.android.widgets

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Bitmap.Config.ARGB_8888
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Paint.ANTI_ALIAS_FLAG
import android.graphics.Paint.Style
import android.graphics.PorterDuff.Mode.CLEAR
import android.graphics.PorterDuffXfermode
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import androidx.annotation.DrawableRes
import androidx.appcompat.widget.AppCompatImageView
import org.wordpress.android.R
import org.wordpress.android.util.DisplayUtils

/**
 * A ImageView that can draw a badge at the corner of its view.
 * The main difference between this implementation and others commonly found online, is that this one uses
 * Porter/Duff Compositing to create a transparent space between the badge background and the view.
 */
class BadgedImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr) {
    companion object {
        const val DEFAULT_BADGE_BACKGROUND_SIZE = 16f
        const val DEFAULT_BADGE_BACKGROUND_BORDER_WIDTH = 0f
        const val DEFAULT_BADGE_ICON_SIZE = 16f
        const val DEFAULT_BADGE_HORIZONTAL_OFFSET = 0f
        const val DEFAULT_BADGE_VERTICAL_OFFSET = 0f
    }

    var badgeBackground: Drawable? = null
        set(value) {
            field = value
            invalidate()
        }
    var badgeBackgroundSize: Float = 0f
        set(value) {
            field = value
            invalidate()
        }
    var badgeBackgroundBorderWidth: Float = 0f
        set(value) {
            field = value
            invalidate()
        }
    var badgeIcon: Drawable? = null
        set(value) {
            field = value
            invalidate()
        }
    var badgeIconSize: Float = 0f
        set(value) {
            field = value
            invalidate()
        }
    var badgeHorizontalOffset: Float = 0f
        set(value) {
            field = value
            invalidate()
        }
    var badgeVerticalOffset: Float = 0f
        set(value) {
            field = value
            invalidate()
        }

    init {
        val styledAttributes = context.obtainStyledAttributes(attrs, R.styleable.BadgedImageView)

        badgeBackground = styledAttributes.getDrawable(
                R.styleable.BadgedImageView_badgeBackground
        )

        badgeBackgroundSize = styledAttributes.getDimension(
                R.styleable.BadgedImageView_badgeBackgroundSize,
                DisplayUtils.dpToPx(context, DEFAULT_BADGE_BACKGROUND_SIZE.toInt()).toFloat()
        )

        badgeBackgroundBorderWidth = styledAttributes.getDimension(
                R.styleable.BadgedImageView_badgeBackgroundBorderWidth,
                DisplayUtils.dpToPx(context, DEFAULT_BADGE_BACKGROUND_BORDER_WIDTH.toInt()).toFloat()
        )

        badgeIcon = styledAttributes.getDrawable(
                R.styleable.BadgedImageView_badgeIcon
        )

        badgeIconSize = styledAttributes.getDimension(
                R.styleable.BadgedImageView_badgeIconSize,
                DisplayUtils.dpToPx(context, DEFAULT_BADGE_ICON_SIZE.toInt()).toFloat()
        )

        badgeHorizontalOffset = styledAttributes.getDimension(
                R.styleable.BadgedImageView_badgeHorizontalOffset,
                DisplayUtils.dpToPx(context, DEFAULT_BADGE_HORIZONTAL_OFFSET.toInt()).toFloat()
        )

        badgeVerticalOffset = styledAttributes.getDimension(
                R.styleable.BadgedImageView_badgeVerticalOffset,
                DisplayUtils.dpToPx(context, DEFAULT_BADGE_VERTICAL_OFFSET.toInt()).toFloat()
        )

        styledAttributes.recycle()
    }

    private val paint = Paint(ANTI_ALIAS_FLAG)
    private val eraserPaint = Paint(ANTI_ALIAS_FLAG).apply {
        color = Color.TRANSPARENT
        style = Style.FILL_AND_STROKE
        strokeWidth = badgeBackgroundBorderWidth
        xfermode = PorterDuffXfermode(CLEAR)
    }

    private var tempCanvasBitmap: Bitmap? = null
    private var tempCanvas: Canvas? = null
    private var invalidated = true

    fun setBadgeBackground(@DrawableRes badgeBackgroundResId: Int) {
        badgeBackground = context.getDrawable(badgeBackgroundResId)
    }

    fun setBadgeIcon(@DrawableRes badgeIconResId: Int) {
        badgeIcon = context.getDrawable(badgeIconResId)
    }

    override fun invalidate() {
        invalidated = true
        super.invalidate()
    }

    override fun onSizeChanged(width: Int, height: Int, oldWidht: Int, oldHeight: Int) {
        super.onSizeChanged(width, height, oldWidht, oldHeight)
        val sizeChanged = width != oldWidht || height != oldHeight
        val isValid = width > 0 && height > 0

        if (isValid && (tempCanvas == null || sizeChanged)) {
            tempCanvasBitmap = Bitmap.createBitmap(
                    width + badgeBackgroundSize.toInt() / 2,
                    height + badgeBackgroundSize.toInt() / 2,
                    ARGB_8888
            )
            tempCanvas = tempCanvasBitmap?.let { Canvas(it) }
            invalidated = true
        }
    }

    override fun onDraw(canvas: Canvas) {
        if (invalidated) {
            tempCanvas?.let {
                clearCanvas(it)
                super.onDraw(it)
                drawBadge(it)
            }

            invalidated = false
        }
        if (!invalidated) {
            tempCanvasBitmap?.let { canvas.drawBitmap(it, 0f, 0f, paint) }
        }
    }

    private fun clearCanvas(canvas: Canvas) {
        canvas.drawColor(Color.TRANSPARENT, CLEAR)
    }

    private fun drawBadge(canvas: Canvas) {
        val x = pivotX + width / 2f - badgeBackgroundSize / 2f + badgeHorizontalOffset
        val y = pivotY + height / 2f - badgeBackgroundSize / 2f + badgeVerticalOffset

        drawBadgeSpace(canvas, x, y)
        drawBadgeBackground(canvas, x, y)
        drawBadgeIcon(canvas, x, y)
    }

    private fun drawBadgeSpace(canvas: Canvas, x: Float, y: Float) {
        canvas.drawCircle(x, y, badgeBackgroundSize / 2f + badgeBackgroundBorderWidth, eraserPaint)
    }

    private fun drawBadgeBackground(canvas: Canvas, x: Float, y: Float) {
        if (badgeBackground != null) {
            badgeBackground?.setBounds(0, 0, badgeBackgroundSize.toInt(), badgeBackgroundSize.toInt())
            canvas.save()
            canvas.translate(x - badgeBackgroundSize / 2f, y - badgeBackgroundSize / 2f)
            badgeBackground?.draw(canvas)
            canvas.restore()
        }
    }

    private fun drawBadgeIcon(canvas: Canvas, x: Float, y: Float) {
        if (badgeIcon != null) {
            badgeIcon?.setBounds(0, 0, badgeIconSize.toInt(), badgeIconSize.toInt())
            canvas.save()
            canvas.translate(x - badgeIconSize / 2f, y - badgeIconSize / 2f)
            badgeIcon?.draw(canvas)
            canvas.restore()
        }
    }
}
