package org.wordpress.android.ui.media

import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.annotation.StringRes
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.setFragmentResult
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import org.wordpress.android.R
import org.wordpress.android.databinding.MediaFieldEditBottomSheetBinding

/**
 * Edits one text field of [MediaSettingsActivity]. The new value is returned through the fragment result API
 * under [REQUEST_KEY]; the activity still decides when to save it.
 */
class MediaFieldEditBottomSheet : BottomSheetDialogFragment() {
    enum class Field(
        @StringRes val titleRes: Int,
        @StringRes val explanationRes: Int?,
        val isMultiLine: Boolean
    ) {
        TITLE(R.string.media_edit_title_text, null, false),
        CAPTION(R.string.media_edit_caption_text, R.string.media_settings_caption_sheet_explanation, true),
        ALT_TEXT(R.string.media_edit_alttext_text, R.string.media_settings_alt_text_sheet_explanation, true),
        DESCRIPTION(R.string.media_edit_description_text, R.string.media_settings_description_sheet_explanation, true)
    }

    override fun getTheme() = R.style.MediaSettings_EditSheetTheme

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        MediaFieldEditBottomSheetBinding.inflate(inflater, container, false).root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val field = Field.valueOf(requireNotNull(requireArguments().getString(ARG_FIELD)))
        with(MediaFieldEditBottomSheetBinding.bind(view)) {
            sheetTitle.setText(field.titleRes)
            sheetExplanation.isVisible = field.explanationRes != null
            field.explanationRes?.let { sheetExplanation.setText(it) }

            // none of these fields may contain newlines, so long values wrap instead of using a multi-line input
            fieldEdit.inputType = InputType.TYPE_CLASS_TEXT or
                    InputType.TYPE_TEXT_FLAG_CAP_SENTENCES or
                    InputType.TYPE_TEXT_FLAG_AUTO_CORRECT
            fieldEdit.imeOptions = EditorInfo.IME_ACTION_DONE
            if (field.isMultiLine) {
                fieldEdit.setHorizontallyScrolling(false)
                fieldEdit.maxLines = Int.MAX_VALUE
                fieldEdit.minLines = MULTI_LINE_MIN_LINES
            }
            if (field == Field.ALT_TEXT) {
                fieldLayout.helperText = getString(R.string.media_settings_alt_text_sheet_helper)
                fieldLayout.isCounterEnabled = true
            }

            if (savedInstanceState == null) {
                fieldEdit.setText(requireArguments().getString(ARG_VALUE))
                fieldEdit.setSelection(fieldEdit.length())
            }
            fieldEdit.requestFocus()

            buttonCancel.setOnClickListener { dismiss() }
            buttonSave.setOnClickListener {
                val result = bundleOf(KEY_FIELD to field.name, KEY_VALUE to fieldEdit.text.toString())
                setFragmentResult(REQUEST_KEY, result)
                dismiss()
            }
        }

        (dialog as? BottomSheetDialog)?.behavior?.apply {
            state = BottomSheetBehavior.STATE_EXPANDED
            skipCollapsed = true
        }
    }

    companion object {
        const val TAG = "MediaFieldEditBottomSheet"
        const val REQUEST_KEY = "media_field_edit"
        const val KEY_FIELD = "field"
        const val KEY_VALUE = "value"
        private const val ARG_FIELD = "arg_field"
        private const val ARG_VALUE = "arg_value"
        private const val MULTI_LINE_MIN_LINES = 3

        @JvmStatic
        fun newInstance(field: Field, value: String) = MediaFieldEditBottomSheet().apply {
            arguments = bundleOf(ARG_FIELD to field.name, ARG_VALUE to value)
        }
    }
}
