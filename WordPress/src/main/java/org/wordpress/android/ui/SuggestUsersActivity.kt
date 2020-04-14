package org.wordpress.android.ui

import android.app.Activity
import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import kotlinx.android.synthetic.main.suggestion_activity.*
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

class SuggestUsersActivity : LocaleAwareActivity() {
    private var suggestionServiceConnectionManager: SuggestionServiceConnectionManager? = null
    private var suggestionAdapter: SuggestionAdapter? = null
    private var siteId: Long? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.suggestion_activity)

        (intent.getSerializableExtra(WordPress.SITE) as? SiteModel)?.let {
            siteId = it.siteId
            initializeSuggestionAdapter(it)
        }

        autocompleteText.apply {
            setOnItemClickListener { _, _, position, _ ->
                val suggestionUserId = suggestionAdapter?.getItem(position)?.userLogin
                finishWithId(suggestionUserId)
            }
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun afterTextChanged(s: Editable?) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    // Insure the text always starts with an "@"
                    if (s?.startsWith("@") == false) {
                        autocompleteText.setText(resources.getString(R.string.at_username, s))
                        autocompleteText.setSelection(1)
                    }
                }
            })
            setOnFocusChangeListener { _ , hasFocus ->
                if (hasFocus && adapter != null) {
                    forceFiltering(text)
                }
            }

            // Override the enoughToFilter check to always return true so that we always show any available results.
            // Must do this before setting the initial "@" text in order to display all available results
            // immediately when the view loads
            setEnoughToFilterCheck { true }

            setText("@")
            setSelection(1)

            post {
                // Requesting focus after the UI loads insures that all available results display
                // immediately when the view loads
                requestFocus()
            }
        }

        removeTopWindowInset()
    }

    /*
     * Having a translucent status bar with this activity's theme prevents windowSoftInputMode of adjustResize
     * from properly adjusting the window size when the  keyboard is presented. Setting fitsSystemWindows
     * to be true on the root view in this layout fixes that problem, but has the side effect of increasing the
     * top inset on that view in order to prevent the view's content from appearing underneath the status bar.
     * We don't need to worry about that since this rootView is attached to the bottom of the screen, so we
     * can safely remove that top inset.
     *
     * See
     * https://stackoverflow.com/questions/21092888/windowsoftinputmode-adjustresize-not-working-with-translucent-action-navbar
     * for additional context.
     */
    private fun removeTopWindowInset() {
        rootView.setOnApplyWindowInsetsListener { v, insets ->
            val newFrame = Rect(
                    insets.stableInsetLeft,
                    0,
                    insets.systemWindowInsetRight,
                    insets.systemWindowInsetBottom
            )
            val newInsets = insets.replaceSystemWindowInsets(newFrame)
            v.onApplyWindowInsets(newInsets)
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
    }
}
