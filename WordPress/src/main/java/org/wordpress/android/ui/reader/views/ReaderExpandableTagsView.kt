package org.wordpress.android.ui.reader.views

import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewTreeObserver.OnPreDrawListener
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.models.ReaderTag
import org.wordpress.android.models.ReaderTagList
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.DisplayUtils
import javax.inject.Inject

class ReaderExpandableTagsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ChipGroup(context, attrs, defStyleAttr) {
    @Inject lateinit var uiHelpers: UiHelpers

    private val tagChips
        get() = (0 until childCount - 1).map { getChildAt(it) as Chip }

    private val overflowIndicatorChip
        get() = getChildAt(childCount - 1) as Chip

    private val lastVisibleTagChipIndex
        get() = tagChips.filter { it.visibility == View.VISIBLE }.lastIndex

    private val lastVisibleTagChip
        get() = getChildAt(lastVisibleTagChipIndex)

    private val hiddenTagChipsCount
        get() = tagChips.size - (lastVisibleTagChipIndex + 1)

    private val isOverflowIndicatorChipOutsideBounds
        get() = !isChipWithinBounds(overflowIndicatorChip)

    private val maxWidthForChip: Int
        get() {
            val width = DisplayUtils.getDisplayPixelWidth(context) -
                    resources.getDimensionPixelSize(R.dimen.reader_card_margin) * 2
            return (width * MAX_WIDTH_FACTOR).toInt()
        }

    init {
        (context.applicationContext as WordPress).component().inject(this)
    }

    fun loadTags(tags: ReaderTagList) {
        removeAllViews()
        addOverflowIndicatorChip()
        addTagChips(tags)
        expandLayout(false)
    }

    private fun addOverflowIndicatorChip() {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val chip = inflater.inflate(
                R.layout.reader_expandable_tags_view_chip,
                this,
                false
        ) as Chip
        chip.text = resources.getString(R.string.reader_expandable_tags_view_overflow_indicator_collapse_title)
        chip.isCheckable = true
        chip.setOnCheckedChangeListener { _, isChecked ->
            expandLayout(isChecked)
        }
        addView(chip)
    }

    private fun addTagChips(tags: List<ReaderTag>) {
        tags.forEachIndexed { index, tag ->
            val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            val chip = inflater.inflate(
                    R.layout.reader_expandable_tags_view_chip,
                    this,
                    false
            ) as Chip
            chip.tag = tag.tagSlug
            chip.text = tag.tagTitle
            chip.maxWidth = maxWidthForChip
            chip.setOnClickListener { // TODO - set click listener
            }
            addView(chip, index)
        }
    }

    private fun expandLayout(isChecked: Boolean) {
        isSingleLine = !isChecked
        showAllTagChips()
        preLayout {
            hideTagChipsOutsideBounds()
            updateLastVisibleTagChip()
            updateOverflowIndicatorChip()
        }
        requestLayout()
    }

    private fun showAllTagChips() { tagChips.forEach { uiHelpers.updateVisibility(it, true) } }

    private fun hideTagChipsOutsideBounds() {
        tagChips.forEach { uiHelpers.updateVisibility(it, isChipWithinBounds(it)) }
    }

    private fun isChipWithinBounds(chip: Chip): Boolean {
        val chipGroupLocationOnScreen = IntArray(2)
        getLocationOnScreen(chipGroupLocationOnScreen)

        val childChipRect = Rect(0, 0, chip.width, chip.height)
        getChildVisibleRect(chip, childChipRect, null)

        return if (isSingleLine) {
            if (layoutDirection == View.LAYOUT_DIRECTION_LTR) {
                childChipRect.right < (chipGroupLocationOnScreen[0] + width)
            } else {
                childChipRect.left > chipGroupLocationOnScreen[0]
            }
        } else {
            childChipRect.bottom <= (chipGroupLocationOnScreen[1] + height)
        }
    }

    private fun updateLastVisibleTagChip() {
        lastVisibleTagChip?.let {
            uiHelpers.updateVisibility(it, !isOverflowIndicatorChipOutsideBounds)
        }
    }

    private fun updateOverflowIndicatorChip() {
        val showOverflowIndicatorChip = hiddenTagChipsCount > 0 || !isSingleLine
        uiHelpers.updateVisibility(overflowIndicatorChip, showOverflowIndicatorChip)

        overflowIndicatorChip.text = if (isSingleLine) {
            String.format(
                resources.getString(R.string.reader_expandable_tags_view_overflow_indicator_expand_title),
                hiddenTagChipsCount
            )
        } else {
            resources.getString(R.string.reader_expandable_tags_view_overflow_indicator_collapse_title)
        }
    }

    private fun View.preLayout(what: () -> Unit) {
        viewTreeObserver.addOnPreDrawListener(object : OnPreDrawListener {
            override fun onPreDraw(): Boolean {
                viewTreeObserver.removeOnPreDrawListener(this)
                what.invoke()
                return true
            }
        })
    }

    companion object {
        private const val MAX_WIDTH_FACTOR = 0.75
    }
}
