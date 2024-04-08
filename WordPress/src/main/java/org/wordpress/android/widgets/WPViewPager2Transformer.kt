package org.wordpress.android.widgets

import android.view.View
import androidx.viewpager2.widget.ViewPager2
import org.wordpress.android.widgets.WPViewPager2Transformer.TransformType.Flow
import org.wordpress.android.widgets.WPViewPager2Transformer.TransformType.Depth
import org.wordpress.android.widgets.WPViewPager2Transformer.TransformType.Zoom
import org.wordpress.android.widgets.WPViewPager2Transformer.TransformType.SlideOver
import kotlin.math.abs
import kotlin.math.max

/**
 * #### Transformer for ViewPager2
 *  This is a clone of [WPViewPagerTransformer], with ViewPager2 compatibility. The purpose of this class is to ease the
 *  migration from ViewPager to ViewPager2 by providing a drop-in replacement for wherever the ViewPager-based
 *  transformer is used.
 */
class WPViewPager2Transformer(private val mTransformType: TransformType) : ViewPager2.PageTransformer {
    sealed class TransformType {
        data object Flow: TransformType() { const val ROTATION_FACTOR = -30f }
        data object Depth: TransformType()
        data object Zoom: TransformType()
        data object SlideOver: TransformType()
    }

    override fun transformPage(page: View, position: Float) {
        val alpha: Float
        val scale: Float
        val translationX: Float
        when (mTransformType) {
            Flow -> {
                page.rotationY = position * Flow.ROTATION_FACTOR
                return
            }

            SlideOver -> if (position < 0 && position > -1) {
                // this is the page to the left
                scale = (abs((abs(position.toDouble()) - 1)) * (1.0f - SCALE_FACTOR_SLIDE) + SCALE_FACTOR_SLIDE)
                    .toFloat()
                alpha = max(MIN_ALPHA_SLIDE.toDouble(), (1 - abs(position.toDouble()))).toFloat()
                val pageWidth = page.width
                val translateValue = position * -pageWidth
                translationX = if (translateValue > -pageWidth) {
                    translateValue
                } else {
                    0f
                }
            } else {
                alpha = 1f
                scale = 1f
                translationX = 0f
            }

            Depth -> if (position > 0 && position < 1) {
                // moving to the right
                alpha = 1 - position
                scale = (MIN_SCALE_DEPTH + (1 - MIN_SCALE_DEPTH) * (1 - abs( position.toDouble()))).toFloat()
                translationX = page.width * -position
            } else {
                // use default for all other cases
                alpha = 1f
                scale = 1f
                translationX = 0f
            }

            Zoom -> if (position >= -1 && position <= 1) {
                scale = max(MIN_SCALE_ZOOM.toDouble(), (1 - abs(position.toDouble()))).toFloat()
                alpha = (MIN_ALPHA_ZOOM + (scale - MIN_SCALE_ZOOM) / (1 - MIN_SCALE_ZOOM) * (1 - MIN_ALPHA_ZOOM))
                val vMargin = (page.height * (1 - scale) / 2)
                val hMargin = (page.width * (1 - scale) / 2)
                translationX = if (position < 0) {
                    hMargin - vMargin / 2
                } else {
                    -hMargin + vMargin / 2
                }
            } else {
                alpha = 1f
                scale = 1f
                translationX = 0f
            }
        }
        page.setAlpha(alpha)
        page.translationX = translationX
        page.scaleX = scale
        page.scaleY = scale
    }

    companion object {
        private const val MIN_SCALE_DEPTH = 0.75f
        private const val MIN_SCALE_ZOOM = 0.85f
        private const val MIN_ALPHA_ZOOM = 0.5f
        private const val SCALE_FACTOR_SLIDE = 0.85f
        private const val MIN_ALPHA_SLIDE = 0.35f
    }
}
