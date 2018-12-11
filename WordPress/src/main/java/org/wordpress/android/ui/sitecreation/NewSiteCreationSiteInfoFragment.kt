package org.wordpress.android.ui.sitecreation

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
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
import org.wordpress.android.ui.ActivityLauncher
import org.wordpress.android.ui.accounts.HelpActivity
import org.wordpress.android.ui.sitecreation.verticals.NewSiteCreationSiteInfoViewModel
import javax.inject.Inject

class NewSiteCreationSiteInfoFragment : NewSiteCreationBaseFormFragment<NewSiteCreationListener>() {
    @Inject internal lateinit var viewModelFactory: ViewModelProvider.Factory

    private lateinit var nonNullActivity: FragmentActivity
    private lateinit var viewModel: NewSiteCreationSiteInfoViewModel

    private lateinit var skipNextButton: AppCompatButton
    private lateinit var siteTitleEditText: TextInputEditText
    private lateinit var tagLineEditText: TextInputEditText

    private lateinit var skipClickedListener: OnSkipClickedListener
    private lateinit var siteInfoScreenListener: SiteInfoScreenListener

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        if (context !is OnSkipClickedListener) {
            throw IllegalStateException("Parent activity must implement OnSkipClickedListener.")
        }
        if (context !is SiteInfoScreenListener) {
            throw IllegalStateException("Parent activity must implement SiteInfoScreenListener.")
        }
        skipClickedListener = context
        siteInfoScreenListener = context
    }

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
        initSkipNextButton(rootView)
        siteTitleEditText = rootView.findViewById(R.id.site_info_site_title)
        tagLineEditText = rootView.findViewById(R.id.site_info_tag_line)
        initViewModel()
        initTextWatchers()
    }

    private fun initSkipNextButton(rootView: ViewGroup) {
        skipNextButton = rootView.findViewById(R.id.site_info_skip_or_next_button)
        skipNextButton.setOnClickListener { viewModel.onSkipNextClicked() }
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
        addTextWatcher(siteTitleEditText) { viewModel.updateSiteTitle(it) }
        addTextWatcher(tagLineEditText) { viewModel.updateTagLine(it) }
    }

    private fun initViewModel() {
        viewModel = ViewModelProviders.of(this, viewModelFactory)
                .get(NewSiteCreationSiteInfoViewModel::class.java)
        viewModel.uiState.observe(this, Observer {
            it?.let { state ->
                val updateEditTextIfDifferent = { editText: TextInputEditText, value: String ->
                    // This let's us avoid triggering the TextWatcher unnecessarily
                    if (editText.text.toString() != value) {
                        editText.setText(value)
                    }
                }
                updateEditTextIfDifferent(siteTitleEditText, state.siteTitle)
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
        viewModel.onHelpClicked.observe(
                this,
                Observer {
                    ActivityLauncher.viewHelpAndSupport(
                            activity!!,
                            HelpActivity.Origin.NEW_SITE_CREATION_SITE_INFO,
                            null,
                            null
                    )
                })
        viewModel.skipBtnClicked.observe(this, Observer {
            skipClickedListener.onSkipClicked()
        })
        viewModel.nextBtnClicked.observe(this, Observer { uiState ->
            uiState?.let {
                siteInfoScreenListener.onSiteInfoFinished(uiState.siteTitle, uiState.tagLine)
            }
        })
    }

    override fun onHelp() {
        viewModel.onHelpClicked()
    }

    companion object {
        const val TAG = "site_creation_site_info_fragment_tag"

        fun newInstance(): NewSiteCreationSiteInfoFragment {
            return NewSiteCreationSiteInfoFragment()
        }
    }
}
