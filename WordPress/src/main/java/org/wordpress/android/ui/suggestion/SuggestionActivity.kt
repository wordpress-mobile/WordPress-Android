package org.wordpress.android.ui.suggestion

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.database.DataSetObserver
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.lifecycle.Observer
import kotlinx.android.synthetic.main.suggest_users_activity.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode.MAIN
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.networking.ConnectionChangeReceiver.ConnectionChangeEvent
import org.wordpress.android.ui.LocaleAwareActivity
import org.wordpress.android.ui.suggestion.adapters.SuggestionAdapter
import org.wordpress.android.ui.suggestion.service.SuggestionEvents.SuggestionNameListUpdated
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.NetworkUtils
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
                finishWithId(suggestionUserId)
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

                // Insure the text always starts with appropriate prefix
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
    }

    private fun exitIfOnlyOneMatchingUser(): Boolean {
        val onlySuggestedUser = if (suggestionAdapter?.count == 1) {
            suggestionAdapter?.getItem(0)?.value
        } else {
            null
        }

        if (onlySuggestedUser != null) {
            finishWithId(onlySuggestedUser)
        } else {
            // If there is not exactly 1 suggestion, notify that entered text is not a valid suggestion
            val suggestionType = getString(viewModel.suggestionTypeStringRes)
            val message = getString(R.string.suggestion_invalid, autocompleteText.text, suggestionType)
            ToastUtils.showToast(this@SuggestionActivity, message)
        }

        return onlySuggestedUser != null
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

    private fun finishWithId(userId: String?) {
        setResult(Activity.RESULT_OK, Intent().apply {
            putExtra(SELECTED_USER_ID, userId)
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

        viewModel.suggestions.observe(this, Observer {
            suggestionAdapter?.suggestionList = it
            autocompleteText.forceFiltering(autocompleteText.text)
            updateEmptyView()
        })

        autocompleteText.setAdapter(suggestionAdapter)
    }

    private fun updateEmptyView() {
        val hasSuggestions = suggestionAdapter?.suggestionList?.isNotEmpty() == true
        val showingSuggestions = suggestionAdapter?.isEmpty == false

        empty_view.apply {
            text = when {
                hasSuggestions -> getString(
                        R.string.suggestion_no_matching,
                        getString(viewModel.suggestionTypeStringRes)
                )
                NetworkUtils.isNetworkAvailable(applicationContext) -> getString(R.string.loading)
                else -> getString(R.string.suggestion_no_connection)
            }

            visibility = when {
                showingSuggestions -> View.GONE
                else -> View.VISIBLE
            }
        }
    }

    override fun onResume() {
        super.onResume()
        EventBus.getDefault().register(this)
    }

    override fun onPause() {
        EventBus.getDefault().unregister(this)
        super.onPause()
    }

    override fun onDestroy() {
        viewModel.onDestroy()
        super.onDestroy()
    }

    @Subscribe(threadMode = MAIN)
    fun onEventMainThread(event: SuggestionNameListUpdated) {
        viewModel.onSuggestionsUpdated(event.mRemoteBlogId)
    }

    @Subscribe(threadMode = MAIN)
    fun onEventMainThread(event: ConnectionChangeEvent) {
        viewModel.onConnectionChanged(event)
    }

    companion object {
        const val SELECTED_USER_ID = "SELECTED_USER_ID"

        const val INTENT_KEY_SUGGESTION_TYPE = "INTENT_KEY_SUGGESTION_TYPE"
        const val INTENT_KEY_SITE_MODEL = "INTENT_KEY_SITE_MODEL"
    }
}
