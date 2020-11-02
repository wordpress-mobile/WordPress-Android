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
import androidx.lifecycle.Observer
import kotlinx.android.synthetic.main.suggest_users_activity.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.wordpress.android.R
import org.wordpress.android.R.string
import org.wordpress.android.WordPress
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.networking.ConnectionChangeReceiver.ConnectionChangeEvent
import org.wordpress.android.ui.LocaleAwareActivity
import org.wordpress.android.ui.suggestion.FinishAttempt.NotExactlyOneAvailable
import org.wordpress.android.ui.suggestion.FinishAttempt.OnlyOneAvailable
import org.wordpress.android.ui.suggestion.adapters.SuggestionAdapter
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import org.wordpress.android.util.ToastUtils
import org.wordpress.android.widgets.SuggestionAutoCompleteText
import javax.inject.Inject

class SuggestionActivity : LocaleAwareActivity() {
    private var suggestionAdapter: SuggestionAdapter? = null
    private var siteId: Long? = null
    @Inject lateinit var viewModel: SuggestionViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (application as WordPress).component().inject(this)
        setContentView(R.layout.suggest_users_activity)

        val siteModel = intent.getSerializableExtra(INTENT_KEY_SITE_MODEL) as? SiteModel
        val suggestionType = intent.getSerializableExtra(INTENT_KEY_SUGGESTION_TYPE) as? SuggestionType
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

        rootView.setOnClickListener {
            // The previous activity is visible "behind" this Activity if the list of Suggestions does not fill
            // the entire screen. If the user taps a part of the screen showing the still-visible previous
            // Activity, then finish this Activity and return the user to the previous Activity.
            finish()
        }

        autocompleteText.apply {
            initializeWithPrefix(viewModel.suggestionPrefix)
            setOnItemClickListener { _, _, position, _ ->
                val suggestionUserId = suggestionAdapter?.getItem(position)?.value
                finishWithValue(suggestionUserId)
            }

            setOnKeyListener { _, keyCode, event ->
                if (event.action == KeyEvent.ACTION_UP) {
                    if (keyCode == KeyEvent.KEYCODE_ENTER) {
                        return@setOnKeyListener exitIfOnlyOneMatchingUser()
                    }
                }
                false
            }

            setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    exitIfOnlyOneMatchingUser()
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
                            finish()
                        } else if (!s.startsWith(prefix)) {
                            // Re-insert prefix if it was deleted
                            val string = "$prefix$s"
                            autocompleteText.setText(string)
                            autocompleteText.setSelection(1)
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

        viewModel.suggestions.observe(this, Observer { suggestions ->
            if (suggestions.isEmpty()) {
                // Notify user that there are no suggestions and exit the suggestions activity because
                // there is nothing the user can do here if there aren't any suggestions
                val message = getString(string.suggestion_none, viewModel.suggestionTypeString)
                ToastUtils.showToast(this@SuggestionActivity, message)
                finish()
            }

            suggestionAdapter?.suggestionList = suggestions

            // Calling forceFiltering is needed to force the suggestions list to always
            // immediately refresh when there is new data
            autocompleteText.forceFiltering(autocompleteText.text)

            // Ensure that the suggestions list is displayed wth the new data. This is particularly needed when
            // suggestion list was empty before the new data was received, otherwise the no-longer-empty
            // suggestion list will not display when it is updated. Wrapping this in the isAttachedToWindow
            // check avoids a crash if the suggestions are loaded when the view is not attached.
            if (autocompleteText.isAttachedToWindow) {
                autocompleteText.showDropDown()
            }

            updateEmptyView()
        })
    }

    private fun exitIfOnlyOneMatchingUser(): Boolean {
        return when (val finishAttempt = viewModel.onAttemptToFinish(
                suggestionAdapter?.filteredSuggestions,
                autocompleteText.text.toString()
        )) {
            is OnlyOneAvailable -> {
                finishWithValue(finishAttempt.onlySelectedValue)
                true
            }
            is NotExactlyOneAvailable -> {
                ToastUtils.showToast(this@SuggestionActivity, finishAttempt.errorMessage)
                false
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

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.do_nothing, R.anim.do_nothing)
    }

    private fun finishWithValue(value: String?) {
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

        autocompleteText.setAdapter(suggestionAdapter)
    }

    private fun updateEmptyView() {
        empty_view.apply {
            val (newText, newVisibility) = viewModel.getEmptyViewState(suggestionAdapter?.filteredSuggestions)
            text = newText
            visibility = newVisibility
        }
    }

    override fun onResume() {
        super.onResume()
        EventBus.getDefault().register(this)
        if (autocompleteText.isAttachedToWindow) {
            autocompleteText.showDropDown()
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
