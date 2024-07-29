package org.wordpress.android.ui.reader.views

import android.content.Context
import android.content.res.Resources
import android.util.AttributeSet
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewTreeObserver.OnPreDrawListener
import androidx.annotation.ColorRes
import androidx.annotation.LayoutRes
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.ui.reader.discover.interests.TagUiState
import org.wordpress.android.ui.reader.models.ReaderReadingPreferences
import org.wordpress.android.ui.reader.tracker.ReaderTracker
import org.wordpress.android.ui.reader.utils.toTypeface
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.config.ReaderImprovementsFeatureConfig
import javax.inject.Inject
import android.R as AndroidR

class ReaderExpandableTagsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ChipGroup(context, attrs, defStyleAttr) {
    @Inject
    lateinit var uiHelpers: UiHelpers

    @Inject
    lateinit var readerTracker: ReaderTracker

    @Inject
    lateinit var readerImprovementsFeatureConfig: ReaderImprovementsFeatureConfig

    private var tagsUiState: List<TagUiState>? = null

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

    private val chipStyle
        get() = if (readerImprovementsFeatureConfig.isEnabled()) ChipStyle.New else ChipStyle.Legacy

    init {
        (context.applicationContext as WordPress).component().inject(this)
        layoutDirection = View.LAYOUT_DIRECTION_LOCALE
    }

    fun updateUi(
        tagsUiState: List<TagUiState>,
        readingPreferences: ReaderReadingPreferences? = null
    ) {
        if (this.tagsUiState != null && this.tagsUiState == tagsUiState) {
            return
        }
        this.tagsUiState = tagsUiState
        removeAllViews()
        addOverflowIndicatorChip(readingPreferences)
        addTagChips(tagsUiState, readingPreferences)
        expandLayout(false)
    }

    private fun addOverflowIndicatorChip(readingPreferences: ReaderReadingPreferences?) {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val chip = inflater.inflate(chipStyle.overflowChipLayoutRes, this, false) as Chip
        chip.setOnCheckedChangeListener { _, isChecked ->
            readerTracker.track(Stat.READER_CHIPS_MORE_TOGGLED)
            expandLayout(isChecked)
        }

        readingPreferences?.let {
            chip.setTextSize(TypedValue.COMPLEX_UNIT_PX, chip.textSize * it.fontSize.multiplier)
            chip.typeface = it.fontFamily.toTypeface()
        }

        addView(chip)
    }

    private fun addTagChips(tagsUiState: List<TagUiState>, readingPreferences: ReaderReadingPreferences?) {
        tagsUiState.forEachIndexed { index, tagUiState ->
            val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            val chip = inflater.inflate(chipStyle.chipLayoutRes, this, false) as Chip
            chip.tag = tagUiState.slug
            chip.text = tagUiState.title
            chip.maxWidth = tagUiState.maxWidth
            tagUiState.onClick?.let { onClick ->
                chip.setOnClickListener {
                    onClick.invoke(tagUiState.slug)
                }
            }

            readingPreferences?.let { prefs ->
                chip.setTextSize(TypedValue.COMPLEX_UNIT_PX, chip.textSize * prefs.fontSize.multiplier)
                chip.typeface = prefs.fontFamily.toTypeface()
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

    private fun showAllTagChips() {
        tagChips.forEach { uiHelpers.updateVisibility(it, true) }
    }

    private fun hideTagChipsOutsideBounds() {
        tagChips.forEach { uiHelpers.updateVisibility(it, isChipWithinBounds(it)) }
    }

    private fun isChipWithinBounds(chip: Chip) = if (isSingleLine) {
        if (layoutDirection == View.LAYOUT_DIRECTION_LTR) {
            chip.right <= right - (paddingEnd + chipSpacingHorizontal)
        } else {
            chip.left >= left + (paddingStart + chipSpacingHorizontal)
        }
    } else {
        chip.bottom <= bottom
    }

    private fun updateLastVisibleTagChip() {
        lastVisibleTagChip?.let {
            if (lastVisibleTagChipIndex > 0) {
                uiHelpers.updateVisibility(it, !isOverflowIndicatorChipOutsideBounds)
            }
        }
    }

    private fun updateOverflowIndicatorChip() {
        val showOverflowIndicatorChip = hiddenTagChipsCount > 0 || !isSingleLine
        uiHelpers.updateVisibility(overflowIndicatorChip, showOverflowIndicatorChip)
        overflowIndicatorChip.contentDescription =
            resources.getString(R.string.show_n_hidden_items_desc, hiddenTagChipsCount.toString())

        overflowIndicatorChip.text = if (isSingleLine) {
            chipStyle.overflowChipText(resources, hiddenTagChipsCount)
        } else {
            resources.getString(R.string.reader_expandable_tags_view_overflow_indicator_collapse_title)
        }

        chipStyle.overflowBackgroundColorRes(isSingleLine)?.let { chipBackgroundColorRes ->
            overflowIndicatorChip.setChipBackgroundColorResource(chipBackgroundColorRes)
        }
        chipStyle.overflowStrokeColorRes(isSingleLine)?.let { chipStrokeColorRes ->
            overflowIndicatorChip.setChipStrokeColorResource(chipStrokeColorRes)
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

    private sealed interface ChipStyle {
        @get:LayoutRes
        val chipLayoutRes: Int
        @get:LayoutRes
        val overflowChipLayoutRes: Int

        fun overflowChipText(resources: Resources, hiddenChipsCount: Int): String

        @ColorRes
        fun overflowBackgroundColorRes(isCollapsed: Boolean): Int? = null

        @ColorRes
        fun overflowStrokeColorRes(isCollapsed: Boolean): Int? = null

        object Legacy : ChipStyle {
            override val chipLayoutRes: Int
                get() = R.layout.reader_expandable_tags_view_chip
            override val overflowChipLayoutRes: Int
                get() = R.layout.reader_expandable_tags_view_overflow_chip

            override fun overflowChipText(resources: Resources, hiddenChipsCount: Int): String {
                return resources.getString(
                    R.string.reader_expandable_tags_view_overflow_indicator_expand_title,
                    hiddenChipsCount.toString()
                )
            }

            override fun overflowBackgroundColorRes(isCollapsed: Boolean): Int {
                return if (isCollapsed) {
                    R.color.on_surface_chip
                } else {
                    AndroidR.color.transparent
                }
            }
        }

        object New : ChipStyle {
            override val chipLayoutRes: Int
                get() = R.layout.reader_expandable_tags_view_chip_new
            override val overflowChipLayoutRes: Int
                get() = R.layout.reader_expandable_tags_view_overflow_chip_new

            override fun overflowChipText(resources: Resources, hiddenChipsCount: Int): String {
                return resources.getString(
                    R.string.reader_expandable_tags_view_overflow_indicator_expand_title_new,
                    hiddenChipsCount.toString()
                )
            }

            override fun overflowStrokeColorRes(isCollapsed: Boolean): Int {
                return if (isCollapsed) {
                    R.color.reader_chip_stroke_color
                } else {
                    AndroidR.color.transparent
                }
            }
        }
    }
}
