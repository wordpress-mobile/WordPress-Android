package org.wordpress.android.ui.suggestion

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.database.DataSetObserver
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import androidx.activity.addCallback
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.databinding.SuggestUsersActivityBinding
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.networking.ConnectionChangeReceiver.ConnectionChangeEvent
import org.wordpress.android.ui.LocaleAwareActivity
import org.wordpress.android.ui.suggestion.FinishAttempt.NotExactlyOneAvailable
import org.wordpress.android.ui.suggestion.FinishAttempt.OnlyOneAvailable
import org.wordpress.android.ui.suggestion.adapters.SuggestionAdapter
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import org.wordpress.android.util.ToastUtils
import org.wordpress.android.util.extensions.getSerializableExtraCompat
import org.wordpress.android.util.extensions.onBackPressedCompat
import org.wordpress.android.widgets.SuggestionAutoCompleteText
import javax.inject.Inject

class SuggestionActivity : LocaleAwareActivity() {
    private var suggestionAdapter: SuggestionAdapter? = null
    private var siteId: Long? = null

    @Inject
    lateinit var viewModel: SuggestionViewModel
    private lateinit var binding: SuggestUsersActivityBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (application as WordPress).component().inject(this)
        with(SuggestUsersActivityBinding.inflate(layoutInflater)) {
            setContentView(root)
            binding = this
        }

        onBackPressedDispatcher.addCallback(this) {
            viewModel.trackExit(false)
            onBackPressedDispatcher.onBackPressedCompat(this)
        }

        val siteModel = intent.getSerializableExtraCompat<SiteModel>(INTENT_KEY_SITE_MODEL)
        val suggestionType = intent.getSerializableExtraCompat<SuggestionType>(INTENT_KEY_SUGGESTION_TYPE)
        when {
            siteModel == null -> abortDueToMissingIntentExtra(INTENT_KEY_SITE_MODEL)
            suggestionType == null -> abortDueToMissingIntentExtra(INTENT_KEY_SUGGESTION_TYPE)
            else -> initializeActivity(siteModel, suggestionType)
        }
    }

    private fun abortDueToMissingIntentExtra(key: String) {
        val message = "${this.javaClass.simpleName} started without $key. Finishing Activity."
        AppLog.e(T.EDITOR, message)
        finish()
    }

    private fun initializeActivity(siteModel: SiteModel, suggestionType: SuggestionType) {
        siteId = siteModel.siteId
        viewModel.init(suggestionType, siteModel).let { supportsSuggestions ->
            if (!supportsSuggestions) {
                finish()
                return
            }
        }

        initializeSuggestionAdapter()

        // The previous activity is visible "behind" this Activity if the list of Suggestions does not fill
        // the entire screen. If the user taps a part of the screen showing the still-visible previous
        // Activity, then finish this Activity and return the user to the previous Activity.
        binding.rootView.setOnClickListener {
            viewModel.trackExit(false)
            finish()
        }

        binding.autocompleteText.apply {
            initializeWithPrefix(viewModel.suggestionPrefix)
            setOnItemClickListener { _, _, position, _ ->
                val suggestionUserId = suggestionAdapter?.getItem(position)?.value
                finishWithValue(suggestionUserId)
            }

            setOnKeyListener { _, keyCode, event ->
                if (event.action == KeyEvent.ACTION_UP) {
                    if (keyCode == KeyEvent.KEYCODE_ENTER) {
                        exitIfOnlyOneMatchingUser()
                        return@setOnKeyListener true
                    }
                }
                false
            }

            setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    exitIfOnlyOneMatchingUser()
                    true
                } else {
                    false
                }
            }

            setOnFocusChangeListener { _, _ ->
                // The purpose of this Activity is to allow the user to select a user, so we want
                // the dropdown to always be visible.
                post { showDropDown() }
            }

            viewModel.suggestionPrefix.let { prefix ->

                // Ensure the text always starts with appropriate prefix
                addTextChangedListener(object : TextWatcher {
                    var matchesPrefixBeforeChanged: Boolean? = null

                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                        matchesPrefixBeforeChanged = s?.let { it.length == 1 && it[0] == prefix }
                    }

                    override fun afterTextChanged(s: Editable?) {}
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                        if (s == null) {
                            return
                        } else if (s.isEmpty() && matchesPrefixBeforeChanged == true) {
                            // Tapping delete when only the prefix is shown exits the suggestions UI
                            viewModel.trackExit(false)
                            finish()
                        } else if (s.startsWith("$prefix ")) {
                            // Tapping the space key directly after the prefix exits the suggestions UI
                            finishWithValue("", false)
                        } else if (!s.startsWith(prefix)) {
                            // Re-insert prefix if it was deleted
                            val string = "$prefix$s"
                            binding.autocompleteText.setText(string)
                            binding.autocompleteText.setSelection(1)
                            showDropDown()
                        }
                        matchesPrefixBeforeChanged = null
                    }
                })

                if (text.isEmpty()) {
                    setText("$prefix")
                    setSelection(1)
                }
            }

            updateEmptyView()
            post { requestFocus() }
            showDropdownOnTouch()
        }

        viewModel.suggestionData.observe(this, { suggestionResult ->
            suggestionAdapter?.suggestionList = suggestionResult.suggestions

            // Calling forceFiltering is needed to force the suggestions list to always
            // immediately refresh when there is new data
            binding.autocompleteText.forceFiltering(binding.autocompleteText.text)

            // Ensure that the suggestions list is displayed wth the new data. This is particularly needed when
            // suggestion list was empty before the new data was received, otherwise the no-longer-empty
            // suggestion list will not display when it is updated. Wrapping this in the isAttachedToWindow
            // check avoids a crash if the suggestions are loaded when the view is not attached.
            if (binding.autocompleteText.isAttachedToWindow) {
                binding.autocompleteText.showDropDown()
            }

            updateEmptyView()
        })
    }

    private fun exitIfOnlyOneMatchingUser() {
        when (val finishAttempt = viewModel.onAttemptToFinish(
            suggestionAdapter?.filteredSuggestions,
            binding.autocompleteText.text.toString()
        )) {
            is OnlyOneAvailable -> {
                finishWithValue(finishAttempt.onlySelectedValue)
            }
            is NotExactlyOneAvailable -> {
                ToastUtils.showToast(this@SuggestionActivity, finishAttempt.errorMessage)
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun SuggestionAutoCompleteText.showDropdownOnTouch() {
        setOnTouchListener { _, _ ->
            // Prevent touching the view from dismissing the suggestion list if it's not empty
            if (!adapter.isEmpty) {
                showDropDown()
            }
            false
        }
    }

    // overridePendingTransition is deprecated in SDK 34 in favor of overrideActivityTransition, but the latter requires
    // SDK 34. overridePendingTransition still works on Android 14 so using it should be safe for now.
    @Suppress("DEPRECATION")
    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.do_nothing, R.anim.do_nothing)
    }

    private fun finishWithValue(value: String?, withSuggestion: Boolean = true) {
        viewModel.trackExit(withSuggestion)

        setResult(Activity.RESULT_OK, Intent().apply {
            putExtra(SELECTED_VALUE, value)
        })
        finish()
    }

    private fun initializeSuggestionAdapter() {
        suggestionAdapter = SuggestionAdapter(this, viewModel.suggestionPrefix).apply {
            setBackgroundColorAttr(R.attr.colorGutenbergBackground)

            registerDataSetObserver(object : DataSetObserver() {
                override fun onChanged() {
                    updateEmptyView()
                }

                override fun onInvalidated() {
                    updateEmptyView()
                }
            })
        }

        binding.autocompleteText.setAdapter(suggestionAdapter)
    }

    private fun updateEmptyView() {
        binding.emptyView.apply {
            val (newText, newVisibility) = viewModel.getEmptyViewState(suggestionAdapter?.filteredSuggestions)
            text = newText
            visibility = newVisibility
        }
    }

    override fun onResume() {
        super.onResume()
        EventBus.getDefault().register(this)
        if (binding.autocompleteText.isAttachedToWindow) {
            binding.autocompleteText.showDropDown()
        }
    }

    override fun onPause() {
        EventBus.getDefault().unregister(this)
        super.onPause()
    }

    @Subscribe
    fun onEventMainThread(event: ConnectionChangeEvent) {
        viewModel.onConnectionChanged(event)
        updateEmptyView()
    }

    companion object {
        const val SELECTED_VALUE = "SELECTED_VALUE"

        const val INTENT_KEY_SUGGESTION_TYPE = "INTENT_KEY_SUGGESTION_TYPE"
        const val INTENT_KEY_SITE_MODEL = "INTENT_KEY_SITE_MODEL"
    }
}
