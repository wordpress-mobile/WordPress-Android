package org.wordpress.android.ui

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
import kotlinx.android.synthetic.main.suggest_users_activity.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode.MAIN
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.datasets.SuggestionTable
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.suggestion.adapters.SuggestionAdapter
import org.wordpress.android.ui.suggestion.service.SuggestionEvents.SuggestionNameListUpdated
import org.wordpress.android.ui.suggestion.util.SuggestionServiceConnectionManager
import org.wordpress.android.ui.suggestion.util.SuggestionUtils
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.SiteUtils
import org.wordpress.android.util.ToastUtils
import org.wordpress.android.widgets.SuggestionAutoCompleteText

class SuggestUsersActivity : LocaleAwareActivity() {
    private var suggestionServiceConnectionManager: SuggestionServiceConnectionManager? = null
    private var suggestionAdapter: SuggestionAdapter? = null
    private var siteId: Long? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.suggest_users_activity)

        val siteModel = intent.getSerializableExtra(WordPress.SITE) as? SiteModel
        if (siteModel == null) {
            val message = "${this.javaClass.simpleName} started without ${WordPress.SITE}. Finishing Activity."
            AppLog.e(AppLog.T.EDITOR, message)
            finish()
        } else {
            initializeActivity(siteModel)
        }
    }

    private fun initializeActivity(siteModel: SiteModel) {
        siteId = siteModel.siteId

        initializeSuggestionAdapter(siteModel)

        rootView.setOnClickListener {
            // The previous activity is visible "behind" this Activity if the list of Suggestions does not fill
            // the entire screen. If the user taps a part of the screen showing the still-visible previous
            // Activity, then finish this Activity and return the user to the previous Activity.
            finish()
        }

        autocompleteText.apply {
            setOnItemClickListener { _, _, position, _ ->
                val suggestionUserId = suggestionAdapter?.getItem(position)?.userLogin
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

            // Insure the text always starts with an "@"
            addTextChangedListener(object : TextWatcher {
                var singleAtChar: Boolean? = null

                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                    singleAtChar = s?.let { "@".contentEquals(it) }
                }
                override fun afterTextChanged(s: Editable?) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    if (s?.isEmpty() == true && singleAtChar == true) {
                        // Tapping delete when only @ is shown in the input should exit the @-mention UI
                        finish()
                    } else if (s?.startsWith("@") == false) {
                        // Re-insert initial @ if it was deleted
                        autocompleteText.setText(resources.getString(R.string.at_username, s))
                        autocompleteText.setSelection(1)
                        showDropDown()
                    }
                    singleAtChar = null
                }
            })

            if (text.isEmpty()) {
                setText("@")
                setSelection(1)
            }

            updateEmptyView()
            post { requestFocus() }
            showDropdownOnTouch()
        }
    }

    private fun exitIfOnlyOneMatchingUser(): Boolean {
        val onlySuggestedUser = if (suggestionAdapter?.count == 1) {
            suggestionAdapter?.getItem(0)?.userLogin
        } else {
            null
        }

        if (onlySuggestedUser != null) {
            finishWithId(onlySuggestedUser)
        } else {
            // If there is not exactly 1 suggestion, notify that entered text is not a valid user
            val message = getString(R.string.suggestion_invalid_user, autocompleteText.text)
            ToastUtils.showToast(this@SuggestUsersActivity, message)
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

    private fun initializeSuggestionAdapter(site: SiteModel) {
        if (SiteUtils.isAccessedViaWPComRest(site)) {
            val connectionManager = SuggestionServiceConnectionManager(this, site.siteId)
            val adapter = SuggestionUtils.setupSuggestions(site, this, connectionManager, true)?.apply {
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

            autocompleteText.setAdapter(adapter)

            suggestionServiceConnectionManager = connectionManager
            suggestionAdapter = adapter
        }
    }

    private fun updateEmptyView() {
        val hasSuggestions = suggestionAdapter?.suggestionList?.isNotEmpty() == true
        val showingSuggestions = suggestionAdapter?.isEmpty == false

        val viewText = if (hasSuggestions) {
            R.string.suggestion_no_matching_users
        } else {
            R.string.loading
        }
        empty_view.text = getString(viewText)

        empty_view.visibility = if (showingSuggestions) {
            View.GONE
        } else {
            View.VISIBLE
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
        suggestionServiceConnectionManager?.unbindFromService()
        super.onDestroy()
    }

    @Subscribe(threadMode = MAIN)
    fun onEventMainThread(event: SuggestionNameListUpdated) {
        // check if the updated suggestions are for the current blog and update the suggestions
        if (siteId != 0L && siteId == event.mRemoteBlogId) {
            val suggestions = SuggestionTable.getSuggestionsForSite(event.mRemoteBlogId)
            suggestionAdapter?.setSuggestionList(suggestions)

            // Calling forceFiltering is the only way I was able to force the suggestions list to immediately refresh
            // with the new data
            autocompleteText.forceFiltering(autocompleteText.text)
        }
    }

    companion object {
        const val SELECTED_USER_ID = "SELECTED_USER_ID"
    }
}
