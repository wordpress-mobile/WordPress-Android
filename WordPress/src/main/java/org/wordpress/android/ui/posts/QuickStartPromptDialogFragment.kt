package org.wordpress.android.ui.posts

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AppCompatDialogFragment
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.ui.main.SitePickerAdapter.SiteRecord
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.util.DisplayUtils
import org.wordpress.android.util.image.ImageManager
import org.wordpress.android.widgets.WPTextView
import javax.inject.Inject

class QuickStartPromptDialogFragment : AppCompatDialogFragment() {
    companion object {
        private const val STATE_KEY_DRAWABLE_RES_ID = "state_key_drawable"
        private const val STATE_KEY_TAG = "state_key_tag"
        private const val STATE_KEY_TITLE = "state_key_title"
        private const val STATE_KEY_MESSAGE = "state_key_message"
        private const val STATE_KEY_POSITIVE_BUTTON_LABEL = "state_key_positive_button_label"
        private const val STATE_KEY_NEGATIVE_BUTTON_LABEL = "state_key_negative_button_label"
        private const val UNDEFINED_RES_ID = -1
        private const val SITE_IMAGE_CORNER_RADIUS_IN_DP = 4
    }

    @DrawableRes private var drawableResId: Int = UNDEFINED_RES_ID
    private lateinit var fragmentTag: String
    private lateinit var message: String
    private lateinit var negativeButtonLabel: String
    private lateinit var positiveButtonLabel: String
    private lateinit var title: String
    private lateinit var siteRecord: SiteRecord

    @Inject lateinit var imageManager: ImageManager
    @Inject lateinit var selectedSiteRepository: SelectedSiteRepository

    override fun getTheme() = R.style.WordPress_FullscreenDialog_NoTitle

    interface QuickStartPromptClickInterface {
        fun onNegativeClicked(instanceTag: String)
        fun onPositiveClicked(instanceTag: String)
    }

    @JvmOverloads
    fun initialize(
        tag: String,
        title: String,
        message: String,
        positiveButtonLabel: String,
        @DrawableRes drawableResId: Int = UNDEFINED_RES_ID,
        negativeButtonLabel: String = ""
    ) {
        this.fragmentTag = tag
        this.title = title
        this.message = message
        this.positiveButtonLabel = positiveButtonLabel
        this.negativeButtonLabel = negativeButtonLabel
        this.drawableResId = drawableResId
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        initDagger()
    }

    private fun initDagger() {
        (requireActivity().application as WordPress).component().inject(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.siteRecord = SiteRecord(selectedSiteRepository.getSelectedSite())
        if (savedInstanceState != null) {
            fragmentTag = requireNotNull(savedInstanceState.getString(STATE_KEY_TAG))
            title = requireNotNull(savedInstanceState.getString(STATE_KEY_TITLE))
            message = requireNotNull(savedInstanceState.getString(STATE_KEY_MESSAGE))
            positiveButtonLabel = requireNotNull(savedInstanceState.getString(STATE_KEY_POSITIVE_BUTTON_LABEL))
            negativeButtonLabel = requireNotNull(savedInstanceState.getString(STATE_KEY_NEGATIVE_BUTTON_LABEL))
            drawableResId = savedInstanceState.getInt(STATE_KEY_DRAWABLE_RES_ID)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(STATE_KEY_TAG, fragmentTag)
        outState.putString(STATE_KEY_TITLE, title)
        outState.putString(STATE_KEY_MESSAGE, message)
        outState.putString(STATE_KEY_POSITIVE_BUTTON_LABEL, positiveButtonLabel)
        outState.putString(STATE_KEY_NEGATIVE_BUTTON_LABEL, negativeButtonLabel)
        outState.putInt(STATE_KEY_DRAWABLE_RES_ID, drawableResId)

        super.onSaveInstanceState(outState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.quick_start_prompt_dialog_fragment, container, false)
        initializeView(view)
        return view
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.setCanceledOnTouchOutside(false)
        dialog.setCancelable(false)
        return dialog
    }

    private fun initializeView(view: View) {
        updateSiteLayout(view)
        updateDialogTitle(view)
        updateDialogDescription(view)
        updatePositiveButton(view)
        updateNegativeButton(view)
    }

    private fun updateSiteLayout(view: View) {
        val siteLayout = view.findViewById<LinearLayout>(R.id.site_layout)
        val txtTitle = siteLayout.findViewById<TextView>(R.id.text_title)
        val txtDomain = view.findViewById<TextView>(R.id.text_domain)
        val imgBlavatar = view.findViewById<ImageView>(R.id.image_blavatar)

        txtTitle.text = siteRecord.blogNameOrHomeURL
        txtDomain.text = siteRecord.homeURL
        imageManager.loadImageWithCorners(
                imgBlavatar,
                siteRecord.blavatarType,
                siteRecord.blavatarUrl,
                DisplayUtils.dpToPx(requireContext(), SITE_IMAGE_CORNER_RADIUS_IN_DP)
        )
    }

    private fun updateDialogTitle(view: View) {
        val dialogTitle = view.findViewById<WPTextView>(R.id.quick_start_prompt_dialog_title)
        dialogTitle.text = title
    }

    private fun updateDialogDescription(view: View) {
        val description = view.findViewById<WPTextView>(R.id.quick_start_prompt_dialog_description)
        description.text = message
    }

    private fun updatePositiveButton(view: View) {
        val buttonPositive = view.findViewById<Button>(R.id.quick_start_prompt_dialog_button_positive)
        buttonPositive.text = positiveButtonLabel
        buttonPositive.setOnClickListener {
            if (activity is QuickStartPromptClickInterface) {
                (activity as QuickStartPromptClickInterface).onPositiveClicked(fragmentTag)
            }
            this.dismiss()
        }
    }

    private fun updateNegativeButton(view: View) {
        val buttonNegative = view.findViewById<Button>(R.id.quick_start_prompt_dialog_button_negative)
        if (negativeButtonLabel.isNotEmpty()) {
            buttonNegative.visibility = View.VISIBLE
            buttonNegative.text = negativeButtonLabel
            buttonNegative.setOnClickListener {
                if (activity is QuickStartPromptClickInterface) {
                    (activity as QuickStartPromptClickInterface).onNegativeClicked(fragmentTag)
                }
                this.dismiss()
            }
        }
    }
}
