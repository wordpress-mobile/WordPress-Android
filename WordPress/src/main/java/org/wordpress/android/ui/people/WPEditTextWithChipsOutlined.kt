package org.wordpress.android.ui.people

import android.animation.Animator
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Parcel
import android.os.Parcelable
import android.os.Parcelable.Creator
import android.text.Editable
import android.text.TextUtils.TruncateAt
import android.text.TextWatcher
import android.util.AttributeSet
import android.util.TypedValue
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.TextView.OnEditorActionListener
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.ColorUtils
import com.google.android.flexbox.FlexboxLayout
import com.google.android.material.chip.Chip
import com.google.android.material.shape.CornerFamily
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.shape.ShapeAppearanceModel
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textview.MaterialTextView
import org.wordpress.android.R
import org.wordpress.android.databinding.WpEditTextWithChipsOutlinedBinding
import org.wordpress.android.util.RtlUtils
import org.wordpress.android.util.extensions.getColorResIdFromAttribute
import java.util.LinkedHashMap

/**
 * As of our understanding, currently the TextInputLayout (that is a LinearLayout) works in OutlinedBox mode having
 * a single TextInputEditText as a direct child, inflating needed views into an internal not accessible FrameLayout;
 * so not easily possible to add additional views over the main TextInputLayout keeping the usual
 * behaviours of the TextInputEditText.
 *
 * This component was mainly created to address the move of PeopleInviteFragment into material design.
 * Specifically tries to cover the usage of chips in combination with an EditText while mimic an
 * OutlinedBox with TextInputLayout material design behaviour.
 *
 * The implementation is pretty basic and do not cover all possible use cases. We should leave this component usage
 * once we find a more out of the box solution with material design lib (keep an eye on future releases) that
 * covers our use cases.
 *
 * Supports hint/label animation (with RTL support).
 *
 * Based and inspired by https://stackoverflow.com/a/61748466
 *
 * Note: other possible implementation is using ImageSpan in the EditText but we found cursor/keyboard management
 * to be more complex and less clean and preferred to use the FlexBox approach for now.
 */

class WPEditTextWithChipsOutlined @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {
    private enum class HelperTextState {
        HINT_VISIBLE,
        LABEL_VISIBLE
    }

    private var helperTextState: HelperTextState = HelperTextState.HINT_VISIBLE
    private var flexBox: FlexboxLayout
    private var editor: TextInputEditText
    private var label: MaterialTextView
    private var hint: MaterialTextView

    private lateinit var outlineDrawable: MaterialShapeDrawable

    private var outlineColorDefault: Int = 0
    private var outlineColorFocused: Int = 0
    private var colorSurface: Int = 0

    private var outlineColorAlphaFocused: Float = 0f
    private var outlineColorAlphaDefault: Float = 0f
    private var hintLabelColorAlphaDefault: Float = 0f

    private var outlinePixelWidthDefault: Int = 0
    private var outlinePixelWidthFocused: Int = 0

    private var hintResourceId = 0
    private var chipifyEnabled = false
    private var maxChips = 0

    private var isRightToLeft = false

    private var itemsManager: ItemsManagerInterface? = null

    fun setItemsManager(itemsManager: ItemsManagerInterface?) {
        this.itemsManager = itemsManager
    }

    interface ItemsManagerInterface {
        fun onRemoveItem(item: String)
        fun onAddItem(item: String)
    }

    enum class ItemValidationState(@AttrRes val colorAttr: Int) {
        NEUTRAL(R.attr.colorOnSurface),
        VALIDATED(R.attr.colorPrimary),
        VALIDATED_WITH_ERRORS(R.attr.wpColorError);

        @ColorInt fun colorFromState(context: Context): Int {
            val color = context.getColorResIdFromAttribute(this.colorAttr)

            return ContextCompat.getColor(context, color)
        }

        companion object {
            fun stateFromColor(context: Context, color: Int): ItemValidationState {
                return values().first { value ->
                    ContextCompat.getColor(context, context.getColorResIdFromAttribute(value.colorAttr)) == color
                }
            }
        }
    }

    init {
        attrs?.also {
            val stylesAttributes = context.theme.obtainStyledAttributes(
                    attrs,
                    R.styleable.WPEditTextWithChipsOutlined,
                    0,
                    0
            )

            try {
                hintResourceId = stylesAttributes.getResourceId(
                        R.styleable.WPEditTextWithChipsOutlined_wpHint,
                        0
                )
                chipifyEnabled = stylesAttributes.getBoolean(
                        R.styleable.WPEditTextWithChipsOutlined_wpChipifyEnabled,
                        false
                )

                maxChips = stylesAttributes.getInteger(
                        R.styleable.WPEditTextWithChipsOutlined_wpMaxNumberChips, 0
                )
            } finally {
                stylesAttributes.recycle()
            }
        }

        inflate(getContext(), R.layout.wp_edit_text_with_chips_outlined, this)

        flexBox = findViewById(R.id.flexbox)
        editor = findViewById(R.id.flexbox_editor)
        label = findViewById(R.id.flexbox_label)
        hint = findViewById(R.id.flexbox_hint)

        isRightToLeft = RtlUtils.isRtl(context)

        if (hasHint()) {
            label.setText(hintResourceId)
            hint.setText(hintResourceId)
        }

        isSaveEnabled = true

        loadOutlineDrawable()
        loadDimensions()
        loadColors()

        styleView(isEditorFocused(), hasItemsOrText(), false)

        setListeners()
    }

    fun getTextIfAvailableOrNull(): String? {
        throwExceptionIfChipifyNotEnabled()

        if (!hasText() || !canAddMoreChips()) {
            resetText()
            return null
        }

        return removeDelimiterFromItemIfPresent(editor.text.toString())
    }

    fun addOrUpdateChip(item: String, state: ItemValidationState) {
        throwExceptionIfChipifyNotEnabled()
        chipify(item, state)
    }

    fun removeChip(item: String) {
        throwExceptionIfChipifyNotEnabled()
        val chips = getChipsMap()

        chips[item]?.let {
            withBinding {
                flexbox.removeView(it)
                if (flexbox.childCount == 1) {
                    styleView(isEditorFocused(), hasItemsOrText(), true)
                }
            }
        }
    }

    fun getChipsStrings(): MutableSet<String> {
        throwExceptionIfChipifyNotEnabled()

        return getChipsMap().keys
    }

    fun hasChips(): Boolean {
        throwExceptionIfChipifyNotEnabled()
        return getChipsMap().isNotEmpty()
    }

    fun containsChip(item: String): Boolean {
        throwExceptionIfChipifyNotEnabled()
        return getChipsMap().containsKey(item)
    }

    private fun <T> withBinding(block: WpEditTextWithChipsOutlinedBinding.() -> T): T {
        return with(WpEditTextWithChipsOutlinedBinding.bind(this)) {
            block()
        }
    }

    private fun loadOutlineDrawable() {
        val shapeAppearanceModel = ShapeAppearanceModel.builder()
                .setAllCorners(
                        CornerFamily.ROUNDED,
                        resources.getDimension(R.dimen.edit_with_chips_outline_radius)
                )
                .build()

        outlineDrawable = MaterialShapeDrawable(shapeAppearanceModel)
    }

    private fun loadDimensions() {
        outlinePixelWidthDefault = resources.getDimensionPixelSize(R.dimen.edit_with_chips_outline_default_width)
        outlinePixelWidthFocused = resources.getDimensionPixelSize(R.dimen.edit_with_chips_outline_focused_width)
    }

    private fun loadColors() {
        outlineColorDefault = ContextCompat.getColor(context,
                TypedValue().let {
                    getContext().theme.resolveAttribute(R.attr.colorOnSurface, it, true)
                    it.resourceId
                })
        outlineColorFocused = ContextCompat.getColor(context,
                TypedValue().let {
                    getContext().theme.resolveAttribute(R.attr.colorPrimary, it, true)
                    it.resourceId
                })
        colorSurface = ContextCompat.getColor(context,
                TypedValue().let {
                    getContext().theme.resolveAttribute(R.attr.colorSurface, it, true)
                    it.resourceId
                })

        outlineColorAlphaDefault = ResourcesCompat.getFloat(
                resources,
                R.dimen.edit_with_chips_outline_default_alpha
        )
        outlineColorAlphaFocused = ResourcesCompat.getFloat(
                resources,
                R.dimen.edit_with_chips_outline_focused_alpha
        )

        hintLabelColorAlphaDefault = ResourcesCompat.getFloat(
                resources,
                R.dimen.edit_with_chips_hint_label_default_alpha
        )
    }

    private fun setListeners() {
        editor.setOnFocusChangeListener { _, hasFocus ->
            val canAnimate = hint.width > 0 && label.width > 0 && hint.height > 0 && label.height > 0
            if (canAnimate) {
                styleView(hasFocus, hasItemsOrText(), true)
            }

            if (chipifyEnabled && !hasFocus && hasText()) {
                addItem(editor.text.toString())
            }
        }

        if (chipifyEnabled) {
            editor.addTextChangedListener(object : TextWatcher {
                private var mShouldIgnoreChanges = false

                override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

                override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                    if (mShouldIgnoreChanges) { // used to avoid double call after calling setText from this method
                        return
                    }
                    mShouldIgnoreChanges = true

                    if (s.isNotBlank() && endsWithDelimiter(s.toString())) {
                        addItem(s.toString())
                    }

                    mShouldIgnoreChanges = false
                }

                override fun afterTextChanged(s: Editable) {}
            })

            editor.setOnEditorActionListener(OnEditorActionListener { _, actionId, event ->
                if (actionId == EditorInfo.IME_ACTION_DONE ||
                        event != null && event.keyCode == KeyEvent.KEYCODE_ENTER) {
                    val editable = editor.text

                    editable?.let {
                        val item = it.toString()

                        if (item.isNotBlank()) {
                            addItem(item)
                        }
                    }
                    return@OnEditorActionListener true
                } else {
                    return@OnEditorActionListener false
                }
            })

            // handle key preses from hardware keyboard
            editor.setOnKeyListener { _, _, keyEvent ->
                keyEvent.keyCode == KeyEvent.KEYCODE_DEL &&
                        keyEvent.action == KeyEvent.ACTION_DOWN && removeLastEnteredItem()
            }
        }
    }

    private fun canAddMoreChips(): Boolean {
        return withBinding {
            flexbox.childCount < maxChips + 1 || maxChips == 0
        }
    }

    private fun addItem(item: String) {
        if (!canAddMoreChips() && item.isNotBlank()) {
            resetText()
        } else {
            val cleanedItem = removeDelimiterFromItemIfPresent(item)

            if (cleanedItem.isNullOrBlank()) return

            resetText()

            itemsManager?.onAddItem(cleanedItem) ?: chipify(cleanedItem, ItemValidationState.NEUTRAL)
        }
    }

    private data class ChipReplaceData(val index: Int, val chip: Chip, val isAlreadyInGroup: Boolean)

    private fun chipify(item: String, state: ItemValidationState) {
        if (!chipifyEnabled) return

        val chipsMap = getChipsMap()

        val (index, chip, isAlreadyInGroup) = if (chipsMap.containsKey(item)) {
            ChipReplaceData(flexBox.indexOfChild(chipsMap[item]), chipsMap[item]!!, true)
        } else {
            ChipReplaceData(flexBox.childCount - 1, Chip(context), false)
        }

        chip.layoutDirection = View.LAYOUT_DIRECTION_LOCALE
        chip.setEnsureMinTouchTargetSize(true)
        chip.ensureAccessibleTouchTarget(resources.getDimensionPixelSize(R.dimen.people_chips_min_target))
        chip.text = item
        chip.setTextColor(state.colorFromState(context))
        chip.isCloseIconVisible = true
        chip.isClickable = false
        chip.isCheckable = false
        chip.ellipsize = TruncateAt.MIDDLE
        chip.closeIconContentDescription = resources.getString(
                R.string.invite_user_delete_desc,
                item
        )

        if (!isAlreadyInGroup) {
            chip.setOnCloseIconClickListener { chipView ->
                val itemName = chip.text.toString()
                withBinding {
                    flexbox.removeView(chipView)
                    itemsManager?.onRemoveItem(itemName)

                    if (flexbox.childCount == 1) {
                        styleView(isEditorFocused(), hasItemsOrText(), true)
                    }
                }
            }

            flexBox.addView(chip as View, index)
        }

        resetText()
    }

    private fun resetText() {
        editor.apply {
            text?.clear() ?: setText("")
        }
    }

    private fun removeDelimiterFromItemIfPresent(item: String?): String? {
        if (item.isNullOrBlank()) {
            return null
        }
        val trimmedItem = item.trim()
        for (itemDelimiter in ITEM_DELIMITERS) {
            if (trimmedItem.endsWith(itemDelimiter)) {
                return trimmedItem.substring(0, trimmedItem.length - itemDelimiter.length)
            }
        }
        return trimmedItem
    }

    private fun throwExceptionIfChipifyNotEnabled() {
        require(chipifyEnabled) { "Please set chipifyEnabled to true in order to use chips feature" }
    }

    // This should be fast enough for our use case, so we get fresh data always
    private fun getChipsMap(): MutableMap<String, Chip> {
        val chips: MutableMap<String, Chip> = LinkedHashMap()
        withBinding {
            for (i in 0 until flexbox.childCount) {
                val v: View = flexbox.getChildAt(i)
                if (v is Chip) {
                    chips[v.text.toString()] = v
                }
            }
        }

        return chips
    }

    private fun removeLastEnteredItem(): Boolean {
        if (hasText()) {
            return false
        }

        return withBinding {
            // try and remove the last entered username
            if (flexbox.childCount > 1) {
                val chipToRemove = flexBox.getChildAt(flexbox.childCount - 2) as Chip
                val itemName = chipToRemove.text.toString()
                flexBox.removeViewAt(flexbox.childCount - 2)
                itemsManager?.onRemoveItem(itemName)
                true
            } else {
                false
            }
        }
    }

    private fun endsWithDelimiter(string: String): Boolean {
        if (string.isBlank()) {
            return false
        }
        for (usernameDelimiter in ITEM_DELIMITERS) {
            if (string.endsWith(usernameDelimiter)) {
                return true
            }
        }
        return false
    }

    private fun styleView(hasFocus: Boolean, hasText: Boolean, animateOnChange: Boolean) {
        flexBox.background = outlineDrawable
        setLabelColor(hint, outlineColorDefault, hintLabelColorAlphaDefault)
        if (hasFocus) {
            setOutlineStroke(
                    outlinePixelWidthFocused,
                    outlineColorFocused,
                    outlineColorAlphaFocused
            )
        } else {
            setOutlineStroke(
                    outlinePixelWidthDefault,
                    outlineColorDefault,
                    outlineColorAlphaDefault
            )
        }

        if (!hasHint()) {
            hint.visibility = View.INVISIBLE
            label.visibility = View.INVISIBLE
            return
        }

        val previousLabelState = helperTextState

        if (hasFocus || hasText) {
            helperTextState = HelperTextState.LABEL_VISIBLE

            if (animateOnChange && previousLabelState != helperTextState) {
                animateViewTo(HelperTextState.LABEL_VISIBLE)
            } else {
                if (hasFocus) {
                    setLabelColor(label, outlineColorFocused, outlineColorAlphaFocused)
                    hint.visibility = View.INVISIBLE
                    label.visibility = View.VISIBLE
                } else {
                    setLabelColor(label, outlineColorDefault, hintLabelColorAlphaDefault)
                    hint.visibility = View.INVISIBLE
                    label.visibility = View.VISIBLE
                }
            }
        } else {
            helperTextState = HelperTextState.HINT_VISIBLE

            if (animateOnChange && previousLabelState != helperTextState) {
                animateViewTo(HelperTextState.HINT_VISIBLE)
            } else {
                setLabelColor(label, colorSurface, outlineColorAlphaFocused)

                hint.visibility = View.VISIBLE
                label.visibility = View.INVISIBLE
            }
        }
    }

    private fun animateViewTo(targetState: HelperTextState) {
        if (isRightToLeft) {
            hint.pivotX = hint.width.toFloat()
            hint.pivotY = 0f
        } else {
            hint.pivotX = 0f
            hint.pivotY = 0f
        }

        when (targetState) {
            HelperTextState.HINT_VISIBLE -> {
                hint.animate().cancel()

                if (hint.translationY == 0f) {
                    hint.translationY = label.y - hint.y
                    hint.scaleX = label.width.toFloat() / hint.width
                    hint.scaleX = label.height.toFloat() / hint.height
                }

                hint.animate()
                        .translationY(0f)
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(LABEL_ANIMATION_DURATION)
                        .setListener(object : Animator.AnimatorListener {
                            override fun onAnimationStart(animation: Animator?) {
                                label.visibility = View.INVISIBLE
                                hint.visibility = View.VISIBLE
                            }

                            override fun onAnimationEnd(animation: Animator?) {
                            }

                            override fun onAnimationCancel(animation: Animator?) {
                            }

                            override fun onAnimationRepeat(animation: Animator?) {
                            }
                        }).start()
            }
            HelperTextState.LABEL_VISIBLE -> {
                hint.animate().cancel()

                if (hint.translationY != 0f) {
                    hint.translationY = 0f
                    hint.scaleX = 1f
                    hint.scaleX = 1f
                }

                hint.animate()
                        .translationY(label.y - hint.y)
                        .scaleX(label.width.toFloat() / hint.width)
                        .scaleY(label.height.toFloat() / hint.height)
                        .setDuration(LABEL_ANIMATION_DURATION)
                        .setListener(object : Animator.AnimatorListener {
                            override fun onAnimationStart(animation: Animator?) {
                                setLabelColor(label, colorSurface, outlineColorAlphaFocused)
                                label.visibility = View.VISIBLE
                                hint.visibility = View.VISIBLE
                            }

                            override fun onAnimationEnd(animation: Animator?) {
                                hint.visibility = View.INVISIBLE
                                setLabelColor(label, outlineColorFocused, outlineColorAlphaFocused)
                            }

                            override fun onAnimationCancel(animation: Animator?) {
                            }

                            override fun onAnimationRepeat(animation: Animator?) {
                            }
                        }).start()
            }
        }
    }

    private fun setOutlineStroke(width: Int, color: Int, alpha: Float) {
        outlineDrawable.apply {
            this.setStroke(width.toFloat(), color)
            this.alpha = (alpha * 255).coerceAtMost(255f).toInt()
            this.fillColor = ColorStateList.valueOf(Color.TRANSPARENT)
        }
    }

    private fun setLabelColor(textView: MaterialTextView, color: Int, alpha: Float) {
        val colorWithAlpha = ColorUtils.setAlphaComponent(color, (alpha * 255).coerceAtMost(255f).toInt())
        textView.setTextColor(colorWithAlpha)
    }

    private fun hasItemsOrText() = hasItems() || hasText()
    private fun hasItems() = withBinding { flexbox.childCount > 1 }
    private fun hasText() = (editor.text?.length ?: 0) > 0
    private fun isEditorFocused() = editor.isFocused
    private fun hasHint() = hintResourceId != 0

    override fun onSaveInstanceState(): Parcelable {
        val savedState = SavedState(super.onSaveInstanceState())
        savedState.isFocused = isEditorFocused()
        savedState.labelState = helperTextState
        savedState.hasText = hasItemsOrText()
        savedState.chipsData.clear()
        savedState.chipsData.addAll(
                getChipsMap().map { (_, v) ->
                    ChipData(v.text.toString(), v.currentTextColor)
                }
        )
        return savedState
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        if (state !is SavedState) {
            super.onRestoreInstanceState(state)
            return
        }
        val savedState: SavedState = state
        super.onRestoreInstanceState(savedState.superState)

        helperTextState = savedState.labelState

        val chipsData = savedState.chipsData

        for (chip: ChipData in chipsData) {
            chipify(chip.text, ItemValidationState.stateFromColor(context, chip.color))
        }

        styleView(savedState.isFocused, savedState.hasText, false)
    }

    private data class ChipData(val text: String, val color: Int) : Parcelable {
        constructor(parcel: Parcel) : this(
                parcel.readString()!!,
                parcel.readInt()
        )

        override fun writeToParcel(parcel: Parcel, flags: Int) {
            parcel.writeString(text)
            parcel.writeInt(color)
        }

        override fun describeContents(): Int {
            return 0
        }

        companion object CREATOR : Creator<ChipData> {
            override fun createFromParcel(parcel: Parcel): ChipData {
                return ChipData(parcel)
            }

            override fun newArray(size: Int): Array<ChipData?> {
                return arrayOfNulls(size)
            }
        }
    }

    private class SavedState : BaseSavedState {
        var labelState = HelperTextState.HINT_VISIBLE
        var isFocused = false
        var hasText = false
        var chipsData = mutableListOf<ChipData>()

        constructor(superState: Parcelable?) : super(superState) {}
        @Suppress("unused")
        private constructor(`in`: Parcel) : super(`in`) {
            this.labelState = HelperTextState.values()[`in`.readInt()]
            this.isFocused = `in`.readInt() == 1
            this.hasText = `in`.readInt() == 1
            val arrayLen = `in`.readInt()
            if (arrayLen > 0) {
                val chipsDataArray = arrayOfNulls<ChipData>(arrayLen)
                `in`.readTypedArray(chipsDataArray, ChipData.CREATOR)
                this.chipsData = chipsDataArray.filterNotNull().toMutableList()
            }
        }

        override fun writeToParcel(out: Parcel, flags: Int) {
            super.writeToParcel(out, flags)
            out.writeInt(labelState.ordinal)
            out.writeInt(if (this.isFocused) 1 else 0)
            out.writeInt(if (this.hasText) 1 else 0)
            if (chipsData.size == 0) {
                out.writeInt(0)
            } else {
                out.writeInt(chipsData.size)
                out.writeTypedArray(chipsData.toTypedArray(), flags)
            }
        }

        companion object {
            @JvmField
            val CREATOR: Parcelable.Creator<SavedState> = object : Parcelable.Creator<SavedState> {
                override fun createFromParcel(`in`: Parcel): SavedState {
                    return SavedState(`in`)
                }

                override fun newArray(size: Int): Array<SavedState?> {
                    return arrayOfNulls(size)
                }
            }
        }
    }

    companion object {
        // this duration should be the material design default for the animation
        private const val LABEL_ANIMATION_DURATION = 167L
        private val ITEM_DELIMITERS = arrayOf(" ", ",")
    }
}
