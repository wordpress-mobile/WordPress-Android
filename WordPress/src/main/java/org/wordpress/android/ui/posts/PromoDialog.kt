package org.wordpress.android.ui.posts

import android.app.Dialog
import android.os.Bundle
import android.support.annotation.DrawableRes
import android.support.v7.app.AppCompatDialog
import android.support.v7.app.AppCompatDialogFragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout

import org.wordpress.android.R
import org.wordpress.android.util.DisplayUtils
import org.wordpress.android.widgets.WPTextView

class PromoDialog : AppCompatDialogFragment() {
    companion object {
        private const val STATE_KEY_LINK_LABEL = "state_key_link_label"
        private const val STATE_KEY_DRAWABLE_RES_ID = "state_key_drawable"
        private const val STATE_KEY_TAG = "state_key_tag"
        private const val STATE_KEY_TITLE = "state_key_title"
        private const val STATE_KEY_MESSAGE = "state_key_message"
        private const val STATE_KEY_POSITIVE_BUTTON_LABEL = "state_key_positive_button_label"
        private const val STATE_KEY_NEGATIVE_BUTTON_LABEL = "state_key_negative_button_label"
        private const val STATE_KEY_NEUTRAL_BUTTON_LABEL = "state_key_neutral_button_label"
        private const val UNDEFINED_RES_ID = -1
    }

    @DrawableRes
    private var drawableResId: Int = UNDEFINED_RES_ID
    private lateinit var fragmentTag: String
    private lateinit var linkLabel: String
    private lateinit var message: String
    private lateinit var negativeButtonLabel: String
    private lateinit var neutralButtonLabel: String
    private lateinit var positiveButtonLabel: String
    private lateinit var title: String

    interface PromoDialogClickInterface {
        fun onLinkClicked(instanceTag: String)
        fun onNegativeClicked(instanceTag: String)
        fun onNeutralClicked(instanceTag: String)
        fun onPositiveClicked(instanceTag: String)
    }

    override fun setupDialog(dialog: Dialog, style: Int) {
        (dialog as AppCompatDialog).supportRequestWindowFeature(Window.FEATURE_NO_TITLE)
    }

    @JvmOverloads
    fun initialize(
        tag: String,
        title: String,
        message: String,
        positiveButtonLabel: String,
        @DrawableRes drawableResId: Int = UNDEFINED_RES_ID,
        negativeButtonLabel: String = "",
        linkLabel: String = "",
        neutralButtonLabel: String = ""
    ) {
        this.fragmentTag = tag
        this.title = title
        this.message = message
        this.positiveButtonLabel = positiveButtonLabel
        this.negativeButtonLabel = negativeButtonLabel
        this.neutralButtonLabel = neutralButtonLabel
        this.linkLabel = linkLabel
        this.drawableResId = drawableResId
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState != null) {
            fragmentTag = savedInstanceState.getString(STATE_KEY_TAG)
            title = savedInstanceState.getString(STATE_KEY_TITLE)
            message = savedInstanceState.getString(STATE_KEY_MESSAGE)
            positiveButtonLabel = savedInstanceState.getString(STATE_KEY_POSITIVE_BUTTON_LABEL)
            negativeButtonLabel = savedInstanceState.getString(STATE_KEY_NEGATIVE_BUTTON_LABEL)
            neutralButtonLabel = savedInstanceState.getString(STATE_KEY_NEUTRAL_BUTTON_LABEL)
            linkLabel = savedInstanceState.getString(STATE_KEY_LINK_LABEL)
            drawableResId = savedInstanceState.getInt(STATE_KEY_DRAWABLE_RES_ID)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(STATE_KEY_TAG, fragmentTag)
        outState.putString(STATE_KEY_TITLE, title)
        outState.putString(STATE_KEY_MESSAGE, message)
        outState.putString(STATE_KEY_POSITIVE_BUTTON_LABEL, positiveButtonLabel)
        outState.putString(STATE_KEY_NEGATIVE_BUTTON_LABEL, negativeButtonLabel)
        outState.putString(STATE_KEY_NEUTRAL_BUTTON_LABEL, neutralButtonLabel)
        outState.putString(STATE_KEY_LINK_LABEL, linkLabel)
        outState.putInt(STATE_KEY_DRAWABLE_RES_ID, drawableResId)

        super.onSaveInstanceState(outState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.promo_dialog, container, false)
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
        val imageContainer = view.findViewById<LinearLayout>(R.id.promo_dialog_image_container)
        if (drawableResId == UNDEFINED_RES_ID) {
            imageContainer.visibility = View.GONE
        } else {
            val image = view.findViewById<ImageView>(R.id.promo_dialog_image)
            image.setImageResource(drawableResId)
            imageContainer.visibility = if (DisplayUtils.isLandscape(activity)) View.GONE else View.VISIBLE
        }

        val dialogTitle = view.findViewById<WPTextView>(R.id.promo_dialog_title)
        dialogTitle.text = title

        val description = view.findViewById<WPTextView>(R.id.promo_dialog_description)
        description.text = message

        val link = view.findViewById<WPTextView>(R.id.promo_dialog_link)
        if (linkLabel.isNotEmpty() && activity is PromoDialogClickInterface) {
            link.text = linkLabel
            link.setOnClickListener({ (activity as PromoDialogClickInterface).onLinkClicked(fragmentTag) })
        } else {
            link.visibility = View.GONE
        }

        val buttonPositive = view.findViewById<Button>(R.id.promo_dialog_button_positive)
        buttonPositive.text = positiveButtonLabel
        buttonPositive.setOnClickListener({
            if (activity is PromoDialogClickInterface) {
                (activity as PromoDialogClickInterface).onPositiveClicked(fragmentTag)
            }
            this.dismiss()
        })

        val buttonNegative = view.findViewById<Button>(R.id.promo_dialog_button_negative)
        if (!negativeButtonLabel.isEmpty()) {
            buttonNegative.visibility = View.VISIBLE
            buttonNegative.text = negativeButtonLabel
            buttonNegative.setOnClickListener({
                if (activity is PromoDialogClickInterface) {
                    (activity as PromoDialogClickInterface).onNegativeClicked(fragmentTag)
                }
                this.dismiss()
            })
        }

        val buttonNeutral = view.findViewById<Button>(R.id.promo_dialog_button_neutral)
        if (!neutralButtonLabel.isEmpty()) {
            buttonNeutral.visibility = View.VISIBLE
            buttonNeutral.text = neutralButtonLabel
            buttonNeutral.setOnClickListener({
                if (activity is PromoDialogClickInterface) {
                    (activity as PromoDialogClickInterface).onNeutralClicked(fragmentTag)
                }
                this.dismiss()
            })
        }
    }
}
