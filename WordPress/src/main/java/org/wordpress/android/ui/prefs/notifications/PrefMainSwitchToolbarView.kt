package org.wordpress.android.ui.prefs.notifications

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.View.OnClickListener
import android.view.View.OnLongClickListener
import android.widget.CompoundButton
import android.widget.CompoundButton.OnCheckedChangeListener
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.appcompat.widget.SwitchCompat
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import org.wordpress.android.BuildConfig
import org.wordpress.android.R
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.extensions.getColorFromAttribute
import org.wordpress.android.util.extensions.redirectContextClickToLongPressListener

/**
 * Custom view for main switch in toolbar for preferences.
 */
class PrefMainSwitchToolbarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
    defStyleRes: Int = 0
) : LinearLayout(context, attrs, defStyle, defStyleRes),
        OnCheckedChangeListener,
        OnLongClickListener,
        OnClickListener {
    // We should refactor this part to use style attributes, since enum is not too theming friendly
    enum class PrefMainSwitchToolbarViewStyle constructor(
        val value: Int,
        @AttrRes val titleColorResId: Int,
        @AttrRes val backgroundColorResId: Int
    ) {
        HIGHLIGHTED(
                0,
                R.attr.colorOnPrimary,
                R.attr.colorPrimary
        ),
        NORMAL(
                1,
                R.attr.colorOnSurface,
                R.attr.colorSurface
        );

        companion object {
            fun fromInt(value: Int): PrefMainSwitchToolbarViewStyle? =
                    values().firstOrNull { it.value == value }
        }
    }

    private var hintOn: String? = null
    private var hintOff: String? = null
    private var mainSwitch: SwitchCompat
    private var mainSwitchToolbarListener: MainSwitchToolbarListener? = null
    private var toolbarSwitch: Toolbar
    private var titleOff: String? = null
    private var titleOn: String? = null
    private var viewStyle: PrefMainSwitchToolbarViewStyle? = null
    private var titleContentDescription: String? = null

    val isMainChecked: Boolean
        get() = mainSwitch.isChecked

    /**
     * Interface definition for callbacks to be invoked on interaction with main switch toolbar.
     */
    interface MainSwitchToolbarListener {
        /**
         * Called when the checked state of main switch is changed.
         *
         * @param buttonView The main switch whose state has changed.
         * @param isChecked The new checked state of main switch.
         */
        fun onMainSwitchCheckedChanged(buttonView: CompoundButton?, isChecked: Boolean)
    }

    init {
        inflate(context, R.layout.preferences_main_switch_toolbar, this)

        toolbarSwitch = findViewById(R.id.toolbar_with_switch)
        toolbarSwitch.inflateMenu(R.menu.notifications_settings_secondary)

        val menuItem = toolbarSwitch.menu.findItem(R.id.main_switch)
        mainSwitch = menuItem.actionView as SwitchCompat
        setChecked(true)

        attrs?.let {
            val typedArray = context.obtainStyledAttributes(
                    it,
                    R.styleable.PrefMainSwitchToolbarView, 0, 0
            )
            try {
                titleContentDescription = typedArray
                        .getString(R.styleable.PrefMainSwitchToolbarView_mainContentDescription)

                val titleOn = typedArray.getString(R.styleable.PrefMainSwitchToolbarView_mainTitleOn)
                val titleOff = typedArray.getString(R.styleable.PrefMainSwitchToolbarView_mainTitleOff)
                val hintOn = typedArray.getString(R.styleable.PrefMainSwitchToolbarView_mainHintOn)
                val hintOff = typedArray.getString(R.styleable.PrefMainSwitchToolbarView_mainHintOff)

                val contentInsetStart = resources.getDimensionPixelSize(
                        typedArray.getResourceId(
                                R.styleable.PrefMainSwitchToolbarView_mainContentInsetStart,
                                R.dimen.toolbar_content_offset
                        )
                )
                val offsetEndResId = typedArray.getResourceId(R.styleable.PrefMainSwitchToolbarView_mainOffsetEnd, -1)
                var offsetEnd = -1
                if (offsetEndResId != -1) {
                    offsetEnd = resources.getDimensionPixelSize(offsetEndResId)
                }
                val viewStyle = typedArray.getInt(R.styleable.PrefMainSwitchToolbarView_mainViewStyle, -1)

                setTitleOn(titleOn)
                setTitleOff(titleOff)
                setHintOn(hintOn)
                setHintOff(hintOff)
                setContentOffset(contentInsetStart)
                setOffsetEnd(offsetEnd)

                if (viewStyle != -1) {
                    setViewStyle(viewStyle)
                }
            } finally {
                typedArray.recycle()
            }
        }

        mainSwitch.setOnCheckedChangeListener(this)
        toolbarSwitch.setOnLongClickListener(this)
        toolbarSwitch.setOnClickListener(this)

        ViewCompat.setLabelFor(toolbarSwitch, mainSwitch.id)
        setupFocusabilityForTalkBack()
        toolbarSwitch.redirectContextClickToLongPressListener()
    }

    @Suppress("MemberVisibilityCanBePrivate")
    fun updateToolbarSwitchForAccessibility() {
        titleContentDescription?.let {
            for (i in 0 until toolbarSwitch.childCount) {
                if (toolbarSwitch.getChildAt(i) is TextView) {
                    toolbarSwitch.getChildAt(i).contentDescription = titleContentDescription
                }
            }
        }
    }

    @Suppress("MemberVisibilityCanBePrivate")
    fun setContentOffset(offset: Int) {
        toolbarSwitch.setContentInsetsAbsolute(offset, 0)
    }

    /**
     * Applies end padding to the switch menu
     */
    @Suppress("MemberVisibilityCanBePrivate")
    fun setOffsetEnd(offsetEnd: Int) {
        if (offsetEnd != -1) {
            mainSwitch.setPaddingRelative(
                    mainSwitch.left,
                    mainSwitch.top,
                    offsetEnd,
                    mainSwitch.bottom
            )
        }
    }

    private fun setupFocusabilityForTalkBack() {
        mainSwitch.isFocusable = false
        mainSwitch.isClickable = false
        toolbarSwitch.isFocusableInTouchMode = false
        toolbarSwitch.isFocusable = true
        toolbarSwitch.isClickable = true
    }

    /**
     * Loads initial state of the main switch and toolbar
     */
    fun loadInitialState(checkMain: Boolean) {
        setChecked(checkMain)
        setToolbarTitle(checkMain)
        toolbarSwitch.visibility = View.VISIBLE
        updateToolbarSwitchForAccessibility()
    }

    @Suppress("MemberVisibilityCanBePrivate")
    fun setTitleOn(titleOn: String?) {
        this.titleOn = titleOn ?: resources.getString(R.string.main_switch_default_title_on)
    }

    @Suppress("MemberVisibilityCanBePrivate")
    fun setTitleOff(titleOff: String?) {
        this.titleOff = titleOff ?: resources.getString(R.string.main_switch_default_title_off)
    }

    @Suppress("MemberVisibilityCanBePrivate")
    fun setHintOn(hintOn: String?) {
        this.hintOn = hintOn
    }

    @Suppress("MemberVisibilityCanBePrivate")
    fun setHintOff(hintOff: String?) {
        this.hintOff = hintOff
    }

    @Suppress("MemberVisibilityCanBePrivate", "UseCheckOrError")
    fun setViewStyle(viewStyleInt: Int) {
        if (viewStyleInt == this.viewStyle?.value) {
            return
        }
        val nullableType = PrefMainSwitchToolbarViewStyle.fromInt(viewStyleInt)
        nullableType?.let {
            updateViewStyle(nullableType)
        } ?: if (BuildConfig.DEBUG) {
            throw IllegalStateException("Unknown view style id: $viewStyleInt")
        } else {
            AppLog.e(
                    AppLog.T.SETTINGS,
                    "PrefMainSwitchToolbarView.setViewStyle called from xml with an unknown viewStyle."
            )
        }
    }

    @Suppress("MemberVisibilityCanBePrivate")
    fun updateViewStyle(viewStyle: PrefMainSwitchToolbarViewStyle) {
        if (viewStyle == this.viewStyle) {
            return
        }
        this.viewStyle = viewStyle
        loadResourcesForViewStyle(viewStyle)
    }

    private fun loadResourcesForViewStyle(viewStyle: PrefMainSwitchToolbarViewStyle) {
        val titleColor = context.getColorFromAttribute(viewStyle.titleColorResId)
        val backgroundColor = context.getColorFromAttribute(viewStyle.backgroundColorResId)

        toolbarSwitch.setTitleTextColor(titleColor)
        toolbarSwitch.setBackgroundColor(backgroundColor)
    }

    override fun setBackgroundColor(@ColorInt color: Int) {
        toolbarSwitch.setBackgroundColor(color)
    }

    /*
     * User long clicked the toolbar
     */
    override fun onLongClick(v: View?): Boolean {
        val toastText = if (mainSwitch.isChecked) {
            hintOn
        } else {
            hintOff
        }

        if (toastText?.isNotEmpty() == true) {
            Toast.makeText(
                    context,
                    toastText,
                    Toast.LENGTH_SHORT
            ).show()
        }
        return true
    }

    /*
    * User clicked the toolbar
    */
    override fun onClick(v: View?) {
        setChecked(!mainSwitch.isChecked)
    }

    fun setChecked(isChecked: Boolean) {
        mainSwitch.isChecked = isChecked
    }

    private fun setToolbarTitle(checkMain: Boolean) {
        toolbarSwitch.title = if (checkMain) {
            titleOn
        } else {
            titleOff
        }
    }

    /*
     * User toggled the main switch
     */
    override fun onCheckedChanged(buttonView: CompoundButton?, isChecked: Boolean) {
        setToolbarTitle(isChecked)
        mainSwitchToolbarListener?.onMainSwitchCheckedChanged(buttonView, isChecked)
    }

    fun setMainSwitchToolbarListener(mainSwitchToolbarListener: MainSwitchToolbarListener) {
        this.mainSwitchToolbarListener = mainSwitchToolbarListener
    }
}
