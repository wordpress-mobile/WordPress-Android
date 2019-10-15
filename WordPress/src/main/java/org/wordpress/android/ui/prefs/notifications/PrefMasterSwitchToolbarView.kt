package org.wordpress.android.ui.prefs.notifications

import android.content.Context
import android.content.SharedPreferences
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
import androidx.preference.PreferenceManager
import org.wordpress.android.R
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
    private var mHintOn: String? = null
    private var mHintOff: String? = null
    private var mPrefKey: String? = null
    private var mMasterSwitch: SwitchCompat
    private var mMasterSwitchToolbarListener: MasterSwitchToolbarListener? = null
    private var mSharedPreferences: SharedPreferences
    private var mToolbarSwitch: Toolbar
    private var mTitleOff: String? = null
    private var mTitleOn: String? = null

    val isMasterChecked: Boolean
        get() = mMasterSwitch.isChecked

    /**
     * Interface definition for callbacks to be invoked on interaction with master switch toolbar.
     */
    interface MasterSwitchToolbarListener {
        /**
         * Called when the checked state of master switch is changed.
         *
         * @param buttonView The master switch whose state has changed.
         * @param isChecked  The new checked state of master switch.
         */
        fun onMasterSwitchCheckedChanged(buttonView: CompoundButton?, isChecked: Boolean)
    }

    init {
        inflate(context, R.layout.preferences_master_switch_toolbar, this)

        mToolbarSwitch = findViewById(R.id.toolbar_with_switch)
        mToolbarSwitch.inflateMenu(R.menu.notifications_settings_secondary)

        val menuItem = mToolbarSwitch.menu.findItem(R.id.master_switch)
        mMasterSwitch = menuItem.actionView as SwitchCompat
        mMasterSwitch.isChecked = true

        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext())

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

        mMasterSwitch.setOnCheckedChangeListener(this)
        mToolbarSwitch.setOnLongClickListener(this)
        mToolbarSwitch.setOnClickListener(this)

        ViewCompat.setLabelFor(mToolbarSwitch, mMasterSwitch.id)
        setupFocusabilityForTalkBack()
        mToolbarSwitch.redirectContextClickToLongPressListener()
    }

    private fun setToolbarTitleContentDescription(titleContentDescription: String?) {
        titleContentDescription?.let {
            for (i in 0 until mToolbarSwitch.childCount) {
                if (mToolbarSwitch.getChildAt(i) is TextView) {
                    mToolbarSwitch.getChildAt(i).contentDescription = titleContentDescription
                }
            }
        }
    }

    private fun setContentOffset(offset: Int) {
        mToolbarSwitch.setContentInsetsAbsolute(offset, 0)
    }

    private fun setupFocusabilityForTalkBack() {
        mMasterSwitch.isFocusable = false
        mMasterSwitch.isClickable = false
        mToolbarSwitch.isFocusableInTouchMode = false
        mToolbarSwitch.isFocusable = true
        mToolbarSwitch.isClickable = true
    }

    /**
     * Sets pref key to be used for persistence in shared preferences
     * Fetches last saved value and sets initial state of the master switch and toolbar
     */
    fun setPrefKey(prefKey: String?) {
        prefKey?.let {
            mPrefKey = it

            val isMasterChecked = mSharedPreferences.getBoolean(it, true)
            mMasterSwitch.isChecked = isMasterChecked

            mToolbarSwitch.title = if (isMasterChecked) {
                mTitleOn
            } else {
                mTitleOff
            }
            mToolbarSwitch.visibility = View.VISIBLE
        }
    }

    fun setTitleOn(titleOn: String?) {
        mTitleOn = titleOn ?: resources.getString(R.string.master_switch_default_title_on)
    }

    fun setTitleOff(titleOff: String?) {
        mTitleOff = titleOff ?: resources.getString(R.string.master_switch_default_title_off)
    }

    fun setHintOn(hintOn: String?) {
        mHintOn = hintOn ?: resources.getString(R.string.master_switch_default_hint_on)
    }

    fun setHintOff(hintOff: String?) {
        mHintOff = hintOff ?: resources.getString(R.string.master_switch_default_hint_off)
    }

    /*
     * User long clicked the toolbar
     */
    override fun onLongClick(v: View?): Boolean {
        Toast.makeText(
                context,
                if (mMasterSwitch.isChecked) {
                    mHintOn
                } else {
                    mHintOff
                },
                Toast.LENGTH_SHORT
        ).show()
        return true
    }

    /*
    * User clicked the toolbar
    */
    override fun onClick(v: View?) {
        mMasterSwitch.isChecked = !mMasterSwitch.isChecked
    }

    /*
     * User toggled the master switch
     */
    override fun onCheckedChanged(buttonView: CompoundButton?, isChecked: Boolean) {
        mToolbarSwitch.title = if (isChecked) {
            mTitleOn
        } else {
            mTitleOff
        }
        mPrefKey?.let {
            mSharedPreferences.edit().putBoolean(it, isChecked)
                    .apply()
        }
        mMasterSwitchToolbarListener?.onMasterSwitchCheckedChanged(buttonView, isChecked)
    }

    fun setMasterSwitchToolbarListener(masterSwitchToolbarListener: MasterSwitchToolbarListener) {
        mMasterSwitchToolbarListener = masterSwitchToolbarListener
    }
}
