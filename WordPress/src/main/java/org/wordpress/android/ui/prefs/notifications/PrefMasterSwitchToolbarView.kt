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
import org.wordpress.android.util.getColorFromAttribute
import org.wordpress.android.util.redirectContextClickToLongPressListener

/**
 * Custom view for master switch in toolbar for preferences.
 */
class PrefMasterSwitchToolbarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
    defStyleRes: Int = 0
) : LinearLayout(context, attrs, defStyle, defStyleRes),
        OnCheckedChangeListener,
        OnLongClickListener,
        OnClickListener {
    // We should refactor this part to use style attributes, since enum is not too theming friendly
    enum class PrefMasterSwitchToolbarViewStyle constructor(
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
            fun fromInt(value: Int): PrefMasterSwitchToolbarViewStyle? =
                    values().firstOrNull { it.value == value }
        }
    }

    private var hintOn: String? = null
    private var hintOff: String? = null
    private var masterSwitch: SwitchCompat
    private var masterSwitchToolbarListener: MasterSwitchToolbarListener? = null
    private var toolbarSwitch: Toolbar
    private var titleOff: String? = null
    private var titleOn: String? = null
    private var viewStyle: PrefMasterSwitchToolbarViewStyle? = null
    private var titleContentDescription: String? = null

    val isMasterChecked: Boolean
        get() = masterSwitch.isChecked

    /**
     * Interface definition for callbacks to be invoked on interaction with master switch toolbar.
     */
    interface MasterSwitchToolbarListener {
        /**
         * Called when the checked state of master switch is changed.
         *
         * @param buttonView The master switch whose state has changed.
         * @param isChecked The new checked state of master switch.
         */
        fun onMasterSwitchCheckedChanged(buttonView: CompoundButton?, isChecked: Boolean)
    }

    init {
        inflate(context, R.layout.preferences_master_switch_toolbar, this)

        toolbarSwitch = findViewById(R.id.toolbar_with_switch)
        toolbarSwitch.inflateMenu(R.menu.notifications_settings_secondary)

        val menuItem = toolbarSwitch.menu.findItem(R.id.master_switch)
        masterSwitch = menuItem.actionView as SwitchCompat
        setChecked(true)

        attrs?.let {
            val typedArray = context.obtainStyledAttributes(
                    it,
                    R.styleable.PrefMasterSwitchToolbarView, 0, 0
            )
            try {
                titleContentDescription = typedArray
                        .getString(R.styleable.PrefMasterSwitchToolbarView_masterContentDescription)

                val titleOn = typedArray.getString(R.styleable.PrefMasterSwitchToolbarView_masterTitleOn)
                val titleOff = typedArray.getString(R.styleable.PrefMasterSwitchToolbarView_masterTitleOff)
                val hintOn = typedArray.getString(R.styleable.PrefMasterSwitchToolbarView_masterHintOn)
                val hintOff = typedArray.getString(R.styleable.PrefMasterSwitchToolbarView_masterHintOff)

                val contentInsetStart = resources.getDimensionPixelSize(
                        typedArray.getResourceId(
                                R.styleable.PrefMasterSwitchToolbarView_masterContentInsetStart,
                                R.dimen.toolbar_content_offset
                        )
                )
                val masterOffsetEndResId = typedArray.getResourceId(
                        R.styleable.PrefMasterSwitchToolbarView_masterOffsetEnd,
                        -1
                )
                var masterOffsetEnd = -1
                if (masterOffsetEndResId != -1) {
                    masterOffsetEnd = resources.getDimensionPixelSize(
                            masterOffsetEndResId
                    )
                }
                val viewStyle = typedArray.getInt(
                        R.styleable.PrefMasterSwitchToolbarView_masterViewStyle,
                        -1
                )

                setTitleOn(titleOn)
                setTitleOff(titleOff)
                setHintOn(hintOn)
                setHintOff(hintOff)
                setContentOffset(contentInsetStart)
                setMasterOffsetEnd(masterOffsetEnd)

                if (viewStyle != -1) {
                    setViewStyle(viewStyle)
                }
            } finally {
                typedArray.recycle()
            }
        }

        masterSwitch.setOnCheckedChangeListener(this)
        toolbarSwitch.setOnLongClickListener(this)
        toolbarSwitch.setOnClickListener(this)

        ViewCompat.setLabelFor(toolbarSwitch, masterSwitch.id)
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
    fun setMasterOffsetEnd(offsetEnd: Int) {
        if (offsetEnd != -1) {
            masterSwitch.setPaddingRelative(
                    masterSwitch.left,
                    masterSwitch.top,
                    offsetEnd,
                    masterSwitch.bottom
            )
        }
    }

    private fun setupFocusabilityForTalkBack() {
        masterSwitch.isFocusable = false
        masterSwitch.isClickable = false
        toolbarSwitch.isFocusableInTouchMode = false
        toolbarSwitch.isFocusable = true
        toolbarSwitch.isClickable = true
    }

    /**
     * Loads initial state of the master switch and toolbar
     */
    fun loadInitialState(checkMaster: Boolean) {
        setChecked(checkMaster)
        setToolbarTitle(checkMaster)
        toolbarSwitch.visibility = View.VISIBLE
        updateToolbarSwitchForAccessibility()
    }

    @Suppress("MemberVisibilityCanBePrivate")
    fun setTitleOn(titleOn: String?) {
        this.titleOn = titleOn ?: resources.getString(R.string.master_switch_default_title_on)
    }

    @Suppress("MemberVisibilityCanBePrivate")
    fun setTitleOff(titleOff: String?) {
        this.titleOff = titleOff ?: resources.getString(R.string.master_switch_default_title_off)
    }

    @Suppress("MemberVisibilityCanBePrivate")
    fun setHintOn(hintOn: String?) {
        this.hintOn = hintOn
    }

    @Suppress("MemberVisibilityCanBePrivate")
    fun setHintOff(hintOff: String?) {
        this.hintOff = hintOff
    }

    @Suppress("MemberVisibilityCanBePrivate")
    fun setViewStyle(viewStyleInt: Int) {
        if (viewStyleInt == this.viewStyle?.value) {
            return
        }
        val nullableType = PrefMasterSwitchToolbarViewStyle.fromInt(viewStyleInt)
        nullableType?.let {
            updateViewStyle(nullableType)
        } ?: if (BuildConfig.DEBUG) {
            throw IllegalStateException("Unknown view style id: $viewStyleInt")
        } else {
            AppLog.e(
                    AppLog.T.SETTINGS,
                    "PrefMasterSwitchToolbarView.setViewStyle called from xml with an unknown viewStyle."
            )
        }
    }

    @Suppress("MemberVisibilityCanBePrivate")
    fun updateViewStyle(viewStyle: PrefMasterSwitchToolbarViewStyle) {
        if (viewStyle == this.viewStyle) {
            return
        }
        this.viewStyle = viewStyle
        loadResourcesForViewStyle(viewStyle)
    }

    private fun loadResourcesForViewStyle(viewStyle: PrefMasterSwitchToolbarViewStyle) {
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
        val toastText = if (masterSwitch.isChecked) {
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
        setChecked(!masterSwitch.isChecked)
    }

    fun setChecked(isChecked: Boolean) {
        masterSwitch.isChecked = isChecked
    }

    private fun setToolbarTitle(checkMaster: Boolean) {
        toolbarSwitch.title = if (checkMaster) {
            titleOn
        } else {
            titleOff
        }
    }

    /*
     * User toggled the master switch
     */
    override fun onCheckedChanged(buttonView: CompoundButton?, isChecked: Boolean) {
        setToolbarTitle(isChecked)
        masterSwitchToolbarListener?.onMasterSwitchCheckedChanged(buttonView, isChecked)
    }

    fun setMasterSwitchToolbarListener(masterSwitchToolbarListener: MasterSwitchToolbarListener) {
        this.masterSwitchToolbarListener = masterSwitchToolbarListener
    }
}
