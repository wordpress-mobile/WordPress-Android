package org.wordpress.android.ui.sitecreation.siteinfo

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.content.res.ColorStateList
import android.os.Bundle
import android.support.annotation.LayoutRes
import android.support.design.widget.TextInputEditText
import android.support.v4.app.FragmentActivity
import android.support.v4.content.ContextCompat
import android.support.v4.view.ViewCompat
import android.support.v7.widget.AppCompatButton
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.ViewGroup
import android.widget.TextView
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.ui.accounts.HelpActivity
import org.wordpress.android.ui.sitecreation.NewSiteCreationBaseFormFragment
import org.wordpress.android.ui.sitecreation.misc.OnHelpClickedListener
import org.wordpress.android.ui.sitecreation.misc.OnSkipClickedListener
import org.wordpress.android.ui.sitecreation.verticals.NewSiteCreationSiteInfoViewModel
import javax.inject.Inject

class NewSiteCreationSiteInfoFragment : NewSiteCreationBaseFormFragment() {
    @Inject internal lateinit var viewModelFactory: ViewModelProvider.Factory

    private lateinit var nonNullActivity: FragmentActivity
    private lateinit var viewModel: NewSiteCreationSiteInfoViewModel

    private lateinit var skipNextButton: AppCompatButton
    private lateinit var siteTitleEditText: TextInputEditText
    private lateinit var tagLineEditText: TextInputEditText
    private lateinit var headerContainer: ViewGroup

    private lateinit var skipClickedListener: OnSkipClickedListener
    private lateinit var helpClickedListener: OnHelpClickedListener
    private lateinit var siteInfoScreenListener: SiteInfoScreenListener

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        if (context !is OnSkipClickedListener) {
            throw IllegalStateException("Parent activity must implement OnSkipClickedListener.")
        }
        if (context !is OnHelpClickedListener) {
            throw IllegalStateException("Parent activity must implement OnHelpClickedListener.")
        }
        if (context !is SiteInfoScreenListener) {
            throw IllegalStateException("Parent activity must implement SiteInfoScreenListener.")
        }
        skipClickedListener = context
        helpClickedListener = context
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
        initSkipNextButton(rootView)
        siteTitleEditText = rootView.findViewById(R.id.site_info_site_title)
        initTaglineEditText(rootView)
        headerContainer = rootView.findViewById(R.id.header_container)
        initHeaderTitleAndSubtitleText(rootView)
        initViewModel()
        initTextWatchers()
    }

    private fun initSkipNextButton(rootView: ViewGroup) {
        skipNextButton = rootView.findViewById(R.id.btn_skip)
        skipNextButton.setOnClickListener { viewModel.onSkipNextClicked() }
    }

    private fun initTaglineEditText(rootView: ViewGroup) {
        tagLineEditText = rootView.findViewById(R.id.site_info_tag_line)
        // Attributes must be assigned in this order or else behavior will change.
        tagLineEditText.inputType = InputType.TYPE_CLASS_TEXT
        tagLineEditText.setSingleLine(true)
        tagLineEditText.maxLines = Integer.MAX_VALUE
        tagLineEditText.setHorizontallyScrolling(false)
    }

    private fun initHeaderTitleAndSubtitleText(rootView: ViewGroup) {
        rootView.findViewById<TextView>(R.id.title).setText(R.string.new_site_creation_site_info_header_title)
        rootView.findViewById<TextView>(R.id.subtitle).setText(R.string.new_site_creation_site_info_header_subtitle)
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
                        ViewCompat.setBackgroundTintList(
                                this,
                                ColorStateList.valueOf(
                                        ContextCompat.getColor(
                                                nonNullActivity,
                                                buttonState.backgroundColor
                                        )
                                )
                        )
                    }
                }
            }
        })
        viewModel.onHelpClicked.observe(this, Observer {
            helpClickedListener.onHelpClicked(HelpActivity.Origin.NEW_SITE_CREATION_SITE_INFO)
        })
        viewModel.skipBtnClicked.observe(this, Observer {
            skipClickedListener.onSkipClicked()
        })
        viewModel.nextBtnClicked.observe(this, Observer { uiState ->
            uiState?.let {
                siteInfoScreenListener.onSiteInfoFinished(uiState.siteTitle, uiState.tagLine)
            }
        })
        viewModel.start()
    }

    override fun onHelp() {
        viewModel.onHelpClicked()
    }

    override fun getScreenTitle(): String {
        return arguments?.getString(EXTRA_SCREEN_TITLE)
                ?: throw IllegalStateException("Required argument screen title is missing.")
    }

    companion object {
        const val TAG = "site_creation_site_info_fragment_tag"

        fun newInstance(screenTitle: String): NewSiteCreationSiteInfoFragment {
            val fragment = NewSiteCreationSiteInfoFragment()
            val bundle = Bundle()
            bundle.putString(EXTRA_SCREEN_TITLE, screenTitle)
            fragment.arguments = bundle
            return fragment
        }
    }
}
