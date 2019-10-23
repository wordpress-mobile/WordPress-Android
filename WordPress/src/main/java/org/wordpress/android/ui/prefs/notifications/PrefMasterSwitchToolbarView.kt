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
import androidx.appcompat.widget.SwitchCompat
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import org.wordpress.android.R
import org.wordpress.android.ui.prefs.AppPrefs
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
    private var hintOn: String? = null
    private var hintOff: String? = null
    private var prefKey: String? = null
    private var masterSwitch: SwitchCompat
    private var masterSwitchToolbarListener: MasterSwitchToolbarListener? = null
    private var toolbarSwitch: Toolbar
    private var titleOff: String? = null
    private var titleOn: String? = null

    val isMasterChecked: Boolean
        get() = masterSwitch.isChecked

    var shouldSaveMasterKeyOnToggle: Boolean = true

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
        masterSwitch.isChecked = true

        attrs?.let {
            val typedArray = context.obtainStyledAttributes(
                    it,
                    R.styleable.PrefMasterSwitchToolbarView, 0, 0
            )
            try {
                val titleOn = typedArray.getString(R.styleable.PrefMasterSwitchToolbarView_masterTitleOn)
                val titleOff = typedArray.getString(R.styleable.PrefMasterSwitchToolbarView_masterTitleOff)
                val hintOn = typedArray.getString(R.styleable.PrefMasterSwitchToolbarView_masterHintOn)
                val hintOff = typedArray.getString(R.styleable.PrefMasterSwitchToolbarView_masterHintOff)
                val prefKey = typedArray.getString(R.styleable.PrefMasterSwitchToolbarView_prefKey)
                val titleContentDescription = typedArray
                        .getString(R.styleable.PrefMasterSwitchToolbarView_masterContentDescription)
                val contentInsetStart = resources.getDimensionPixelSize(
                        typedArray.getResourceId(
                                R.styleable.PrefMasterSwitchToolbarView_masterContentInsetStart,
                                R.dimen.toolbar_content_offset
                        )
                )

                setTitleOn(titleOn)
                setTitleOff(titleOff)
                setHintOn(hintOn)
                setHintOff(hintOff)
                setPrefKey(prefKey)
                setToolbarTitleContentDescription(titleContentDescription)
                setContentOffset(contentInsetStart)
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

    fun setToolbarTitleContentDescription(titleContentDescription: String?) {
        titleContentDescription?.let {
            for (i in 0 until toolbarSwitch.childCount) {
                if (toolbarSwitch.getChildAt(i) is TextView) {
                    toolbarSwitch.getChildAt(i).contentDescription = titleContentDescription
                }
            }
        }
    }

    private fun setContentOffset(offset: Int) {
        toolbarSwitch.setContentInsetsAbsolute(offset, 0)
    }

    private fun setupFocusabilityForTalkBack() {
        masterSwitch.isFocusable = false
        masterSwitch.isClickable = false
        toolbarSwitch.isFocusableInTouchMode = false
        toolbarSwitch.isFocusable = true
        toolbarSwitch.isClickable = true
    }

    /**
     * Sets pref key to be used for persistence in shared preferences
     */
    fun setPrefKey(prefKey: String?) {
        prefKey?.let {
            this.prefKey = it

            val isMasterChecked = AppPrefs.getMasterSwitchKeyEnabled(it)
            loadInitialState(isMasterChecked)
        }
    }

    /**
     * Loads initial state of the master switch and toolbar
     */
    fun loadInitialState(checkMaster: Boolean) {
        masterSwitch.isChecked = checkMaster

        toolbarSwitch.title = if (checkMaster) {
            titleOn
        } else {
            titleOff
        }
        toolbarSwitch.visibility = View.VISIBLE
    }

    fun setTitleOn(titleOn: String?) {
        this.titleOn = titleOn ?: resources.getString(R.string.master_switch_default_title_on)
    }

    fun setTitleOff(titleOff: String?) {
        this.titleOff = titleOff ?: resources.getString(R.string.master_switch_default_title_off)
    }

    fun setHintOn(hintOn: String?) {
        this.hintOn = hintOn ?: resources.getString(R.string.master_switch_default_hint_on)
    }

    fun setHintOff(hintOff: String?) {
        this.hintOff = hintOff ?: resources.getString(R.string.master_switch_default_hint_off)
    }

    /*
     * User long clicked the toolbar
     */
    override fun onLongClick(v: View?): Boolean {
        Toast.makeText(
                context,
                if (masterSwitch.isChecked) {
                    hintOn
                } else {
                    hintOff
                },
                Toast.LENGTH_SHORT
        ).show()
        return true
    }

    /*
    * User clicked the toolbar
    */
    override fun onClick(v: View?) {
        masterSwitch.isChecked = !masterSwitch.isChecked
    }

    /*
     * User toggled the master switch
     */
    override fun onCheckedChanged(buttonView: CompoundButton?, isChecked: Boolean) {
        toolbarSwitch.title = if (isChecked) {
            titleOn
        } else {
            titleOff
        }

        if (shouldSaveMasterKeyOnToggle) {
            prefKey?.let {
                saveMasterKeyEnabled(it)
            }
        }
        masterSwitchToolbarListener?.onMasterSwitchCheckedChanged(buttonView, isChecked)
    }

    fun saveMasterKeyEnabled(masterKey: String) {
        AppPrefs.setMasterSwitchKeyEnabled(isMasterChecked, masterKey)
    }

    fun setMasterSwitchToolbarListener(masterSwitchToolbarListener: MasterSwitchToolbarListener) {
        this.masterSwitchToolbarListener = masterSwitchToolbarListener
    }
}
