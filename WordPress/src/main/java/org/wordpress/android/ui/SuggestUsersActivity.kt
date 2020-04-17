package org.wordpress.android.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.inputmethod.EditorInfo
import androidx.annotation.VisibleForTesting
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

class SuggestUsersActivity : LocaleAwareActivity() {
    private var suggestionServiceConnectionManager: SuggestionServiceConnectionManager? = null
    private var suggestionAdapter: SuggestionAdapter? = null
    private var siteId: Long? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.suggest_users_activity)

        (intent.getSerializableExtra(WordPress.SITE) as? SiteModel)?.let {
            siteId = it.siteId
            initializeSuggestionAdapter(it)
        }

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

            setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    val filteredSuggestions = suggestionAdapter?.filteredSuggestions
                    val onlySuggestion = getOnlyElement(filteredSuggestions)
                    if (onlySuggestion != null) {
                        finishWithId(onlySuggestion.userLogin)
                    } else {
                        // If there is not exactly 1 suggestion, notify that entered text is not a valid user
                        val message = getString(R.string.suggestion_invalid_user, text)
                        ToastUtils.showToast(this@SuggestUsersActivity, message)
                    }

                    true
                } else {
                    false
                }
            }

            setOnFocusChangeListener { _ , _ ->
                // The purpose of this Activity is to allow the user to select a user, so we want
                // the dropdown to always be visible.
                post { showDropDown() }
            }

            // Insure the text always starts with an "@"
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun afterTextChanged(s: Editable?) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    if (s?.startsWith("@") == false) {
                        autocompleteText.setText(resources.getString(R.string.at_username, s))
                        autocompleteText.setSelection(1)
                        showDropDown()
                    }
                }
            })
            if (text.isEmpty()) {
                setText("@")
                setSelection(1)
            }

            post { requestFocus() }
        }
    }

    private fun finishWithId(userId: String?) {
        setResult(Activity.RESULT_OK, Intent().apply {
            putExtra(SELECTED_USER_ID, userId)
        })
        finish()
    }

    private fun initializeSuggestionAdapter(site: SiteModel) {
        if (!SiteUtils.isAccessedViaWPComRest(site)) {
            AppLog.d(AppLog.T.EDITOR, "Cannot setup user suggestions for non-WPCom site")
        } else {
            val connectionManager = SuggestionServiceConnectionManager(this, site.siteId)
            val adapter = SuggestionUtils.setupSuggestions(site, this, connectionManager)
            autocompleteText.setAdapter(adapter)

            suggestionServiceConnectionManager = connectionManager
            suggestionAdapter = adapter
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
        }
    }

    companion object {
        const val SELECTED_USER_ID = "SELECTED_USER_ID"

        /**
         * @return If [list] has one element, returns that element. Otherwise returns null.
         */
        @VisibleForTesting
        internal fun <T> getOnlyElement(list: List<T>?): T? =
                if (list?.size == 1) {
                    // Using firstOrNull() instead of first() because all lists are mutable and it is possible for
                    // the list's single element to be removed from another thread between the size check and the
                    // retrieval of the first element. Admittedly, that is unlikely, but we're being cautious to
                    // avoid any risk of first() throwing a NoSuchElementException.
                    list.firstOrNull()
                } else {
                    null
                }
    }
}
