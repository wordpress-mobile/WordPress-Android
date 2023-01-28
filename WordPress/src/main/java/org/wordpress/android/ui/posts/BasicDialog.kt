package org.wordpress.android.ui.posts

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.android.support.AndroidSupportInjection
import org.wordpress.android.ui.posts.BasicDialogViewModel.BasicDialogModel
import org.wordpress.android.util.extensions.getParcelableCompat
import javax.inject.Inject

/**
 * Basic dialog fragment with support for 1,2 or 3 buttons.
 */
class BasicDialog : AppCompatDialogFragment() {
    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    private lateinit var viewModel: BasicDialogViewModel
    private lateinit var model: BasicDialogModel
    private var dismissedByPositiveButton: Boolean = false
    private var dismissedByNegativeButton: Boolean = false
    private var dismissedByCancelButton: Boolean = false

    fun initialize(model: BasicDialogModel) {
        this.model = model
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.isCancelable = true
        val theme = 0
        setStyle(STYLE_NORMAL, theme)

        if (savedInstanceState != null) {
            model = requireNotNull(savedInstanceState.getParcelableCompat(STATE_KEY_MODEL))
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putParcelable(STATE_KEY_MODEL, model)
        super.onSaveInstanceState(outState)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        viewModel = ViewModelProvider(requireActivity(), viewModelFactory)
            .get(BasicDialogViewModel::class.java)
        val builder = MaterialAlertDialogBuilder(requireActivity())
        builder.setMessage(model.message)
            .setPositiveButton(model.positiveButtonLabel) { _, _ ->
                dismissedByPositiveButton = true
                viewModel.onPositiveClicked(model.tag)
            }.setCancelable(true)

        model.title?.let {
            builder.setTitle(it)
        }

        model.negativeButtonLabel?.let { negativeLabel ->
            builder.setNegativeButton(negativeLabel) { _, _ ->
                dismissedByNegativeButton = true
                viewModel.onNegativeButtonClicked(model.tag)
            }
        }
        builder.setOnCancelListener {
            dismissedByCancelButton = true
        }

        model.cancelButtonLabel?.let { cancelLabel ->
            builder.setNeutralButton(cancelLabel) { _, _ ->
                // no-op - dialog is automatically dismissed
            }
        }
        return builder.create()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        AndroidSupportInjection.inject(this)
    }

    override fun onDismiss(dialog: DialogInterface) {
        if (!dismissedByPositiveButton && !dismissedByNegativeButton && !dismissedByCancelButton) {
            viewModel.onDismissByOutsideTouch(model.tag)
        }
        super.onDismiss(dialog)
    }

    companion object {
        private const val STATE_KEY_MODEL = "state_key_model"
    }
}
