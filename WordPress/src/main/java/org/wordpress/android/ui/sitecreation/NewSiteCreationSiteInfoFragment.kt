package org.wordpress.android.ui.sitecreation

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.annotation.LayoutRes
import android.support.design.widget.TextInputEditText
import android.support.v4.app.FragmentActivity
import android.support.v4.content.ContextCompat
import android.support.v7.widget.AppCompatButton
import android.text.Editable
import android.text.TextWatcher
import android.view.ViewGroup
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.ui.sitecreation.verticals.NewSiteCreationSiteInfoViewModel
import javax.inject.Inject

class NewSiteCreationSiteInfoFragment : NewSiteCreationBaseFormFragment<NewSiteCreationListener>() {
    @Inject internal lateinit var viewModelFactory: ViewModelProvider.Factory

    private lateinit var nonNullActivity: FragmentActivity
    private lateinit var viewModel: NewSiteCreationSiteInfoViewModel

    private lateinit var skipNextButton: AppCompatButton
    private lateinit var businessNameEditText: TextInputEditText
    private lateinit var tagLineEditText: TextInputEditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        nonNullActivity = requireNotNull(activity)
        (nonNullActivity.application as WordPress).component().inject(this)
    }

    @LayoutRes
    override fun getContentLayout(): Int {
        return R.layout.new_site_creation_site_info_screen
    }

    override fun setupContent(rootView: ViewGroup) {
        // TODO: Get the title from the main VM
        skipNextButton = rootView.findViewById(R.id.site_info_skip_or_next_button)
        businessNameEditText = rootView.findViewById(R.id.site_info_business_name)
        tagLineEditText = rootView.findViewById(R.id.site_info_tag_line)
        initViewModel()
        initTextWatchers()
    }

    private fun initTextWatchers() {
        val addTextWatcher = { editText: TextInputEditText, onTextChanged: (String) -> Unit ->
            editText.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {}
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    onTextChanged(s?.toString() ?: "")
                }
            })
        }
        addTextWatcher(businessNameEditText) { viewModel.updateBusinessName(it) }
        addTextWatcher(tagLineEditText) { viewModel.updateTagLine(it) }
    }

    private fun initViewModel() {
        viewModel = ViewModelProviders.of(nonNullActivity, viewModelFactory)
                .get(NewSiteCreationSiteInfoViewModel::class.java)
        viewModel.uiState.observe(this, Observer {
            it?.let { state ->
                val updateEditTextIfDifferent = { editText: TextInputEditText, value: String ->
                    // This let's us avoid triggering the TextWatcher unnecessarily
                    if (editText.text.toString() != value) {
                        editText.setText(value)
                    }
                }
                updateEditTextIfDifferent(businessNameEditText, state.businessName)
                updateEditTextIfDifferent(tagLineEditText, state.tagLine)
                state.skipButtonState.let { buttonState ->
                    skipNextButton.apply {
                        setText(buttonState.text)
                        setTextColor(ContextCompat.getColor(nonNullActivity, buttonState.textColor))
                        setBackgroundColor(ContextCompat.getColor(nonNullActivity, buttonState.backgroundColor))
                    }
                }
            }
        })
    }

    override fun onHelp() {
        if (mSiteCreationListener != null) {
            mSiteCreationListener.helpCategoryScreen()
        }
    }

    override fun getScreenTitle(): String {
        val arguments = arguments
        if (arguments == null || !arguments.containsKey(EXTRA_SCREEN_TITLE)) {
            throw IllegalStateException("Required argument screen title is missing.")
        }
        return arguments.getString(EXTRA_SCREEN_TITLE)
    }

    companion object {
        const val TAG = "site_creation_site_info_fragment_tag"

        fun newInstance(screenTitle: String): NewSiteCreationSiteInfoFragment {
            val fragment = NewSiteCreationSiteInfoFragment()
            val bundle = Bundle()
            bundle.putString(NewSiteCreationBaseFormFragment.EXTRA_SCREEN_TITLE, screenTitle)
            fragment.arguments = bundle
            return fragment
        }
    }
}
