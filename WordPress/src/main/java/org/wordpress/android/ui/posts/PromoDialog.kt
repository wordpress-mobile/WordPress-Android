package org.wordpress.android.ui.posts

import android.app.Dialog
import android.os.Bundle
import android.support.annotation.DrawableRes
import android.support.v7.app.AppCompatDialog
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

class PromoDialog : BaseYesNoFragmentDialog() {
    private val STATE_KEY_LINK_LABEL = "state_key_link_label"
    private val STATE_KEY_DRAWABLE_RES_ID = "state_key_drawable"

    private lateinit var linkLabel: String
    @DrawableRes
    private var drawableResId: Int = -1

    interface PromoDialogClickInterface {
        fun onLinkClicked(instanceTag: String)
    }

    override fun setupDialog(dialog: Dialog, style: Int) {
        (dialog as AppCompatDialog).supportRequestWindowFeature(Window.FEATURE_NO_TITLE)
    }

    @JvmOverloads
    fun setArgs(
        tag: String,
        title: String,
        message: String,
        positiveButtonLabel: String,
        @DrawableRes drawableResId: Int,
        negativeButtonLabel: String = "",
        linkLabel: String = ""
    ) {
        setArgs(tag, title, message, positiveButtonLabel, negativeButtonLabel)

        this.linkLabel = linkLabel
        this.drawableResId = drawableResId
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState != null) {
            linkLabel = savedInstanceState.getString(STATE_KEY_LINK_LABEL)
            drawableResId = savedInstanceState.getInt(STATE_KEY_DRAWABLE_RES_ID)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(STATE_KEY_LINK_LABEL, linkLabel)
        outState.putInt(STATE_KEY_DRAWABLE_RES_ID, drawableResId)

        super.onSaveInstanceState(outState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.promo_dialog_advanced, container, false)
        initializeView(view)
        return view
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCanceledOnTouchOutside(false)
        dialog.setCancelable(false)
        return dialog
    }

    private fun initializeView(view: View) {
        val image = view.findViewById<ImageView>(R.id.promo_dialog_image)
        image.setImageResource(drawableResId)
        val imageContainer = view.findViewById<LinearLayout>(R.id.promo_dialog_image_container)
        imageContainer.visibility = if (DisplayUtils.isLandscape(activity)) View.GONE else View.VISIBLE

        val title = view.findViewById<WPTextView>(R.id.promo_dialog_title)
        title.text = mTitle

        val description = view.findViewById<WPTextView>(R.id.promo_dialog_description)
        description.text = mMessage

        val link = view.findViewById<WPTextView>(R.id.promo_dialog_link)
        if (linkLabel.isNotEmpty() && activity is PromoDialogClickInterface) {
            link.text = linkLabel
            link.setOnClickListener({ (activity as PromoDialogClickInterface).onLinkClicked(mTag) })
        } else {
            link.visibility = View.GONE
        }

        val buttonPositive = view.findViewById<Button>(R.id.promo_dialog_button_positive)
        buttonPositive.text = mPositiveButtonLabel
        buttonPositive.setOnClickListener({
            if (activity is BasicYesNoDialogClickInterface) {
                (activity as BasicYesNoDialogClickInterface).onPositiveClicked(mTag)
            } else {
                this.dismiss()
            }
        })

        val buttonNegative = view.findViewById<Button>(R.id.promo_dialog_button_negative)
        if (mNegativeButtonLabel.isEmpty()) {
            buttonNegative.visibility = View.GONE
        } else {
            buttonNegative.text = mNegativeButtonLabel
            buttonNegative.setOnClickListener({
                if (activity is BasicYesNoDialogClickInterface) {
                    (activity as BasicYesNoDialogClickInterface).onNegativeClicked(mTag)
                } else {
                    this.dismiss()
                }
            })
        }
    }
}
