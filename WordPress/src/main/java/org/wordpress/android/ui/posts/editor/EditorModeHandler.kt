package org.wordpress.android.ui.posts.editor

import android.text.TextUtils
import dagger.Reusable
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.editor.EditorFragmentAbstract
import org.wordpress.android.editor.gutenberg.GutenbergEditorFragment
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.posts.PostEditorAnalyticsSession
import org.wordpress.android.ui.prefs.AppPrefs
import org.wordpress.android.util.SiteUtils
import org.wordpress.android.util.analytics.AnalyticsUtils
import org.wordpress.android.util.analytics.AnalyticsUtils.BlockEditorEnabledSource
import javax.inject.Inject

/**
 * Handles editor mode switching (HTML/Visual/Gutenberg) for the post editor.
 *
 * This class extracts editor mode management logic from EditPostActivity to improve
 * testability and separation of concerns.
 */
@Reusable
class EditorModeHandler @Inject constructor(
    private val dispatcher: Dispatcher
) {
    /**
     * Listener for editor mode events that require Activity-level handling.
     */
    interface EditorModeListener {
        fun getEditorFragment(): EditorFragmentAbstract?
        fun getSiteModel(): SiteModel
        fun getPostEditorAnalyticsSession(): PostEditorAnalyticsSession?
        fun isNewPost(): Boolean
        fun invalidateOptionsMenu()
        fun getString(resId: Int): String
    }

    private var listener: EditorModeListener? = null
    private var htmlModeOn: Boolean = false

    /**
     * Initializes the handler with the required listener.
     */
    fun start(listener: EditorModeListener) {
        this.listener = listener
    }

    /**
     * Returns whether HTML mode is currently enabled.
     */
    fun isHtmlModeOn(): Boolean = htmlModeOn

    /**
     * Sets the HTML mode state. Used for state restoration.
     */
    fun setHtmlModeOn(isOn: Boolean) {
        htmlModeOn = isOn
    }

    /**
     * Toggles between HTML and Visual mode.
     */
    fun toggleHtmlMode() {
        htmlModeOn = !htmlModeOn
        trackEditorModeSwitch()
        listener?.invalidateOptionsMenu()
        showModeSwitchedNotice()
    }

    /**
     * Shows a notice indicating which mode the editor switched to.
     */
    private fun showModeSwitchedNotice() {
        val listener = this.listener ?: return
        val messageResId = if (htmlModeOn) {
            org.wordpress.android.R.string.menu_html_mode_switched_notice
        } else {
            org.wordpress.android.R.string.menu_visual_mode_switched_notice
        }
        val message = listener.getString(messageResId)
        listener.getEditorFragment()?.showNotice(message)
    }

    /**
     * Tracks the editor mode switch in analytics.
     */
    private fun trackEditorModeSwitch() {
        val editorFragment = listener?.getEditorFragment()
        val isGutenberg = editorFragment is GutenbergEditorFragment
        val editor = when {
            htmlModeOn -> PostEditorAnalyticsSession.Editor.HTML
            isGutenberg -> PostEditorAnalyticsSession.Editor.GUTENBERG
            else -> PostEditorAnalyticsSession.Editor.CLASSIC
        }
        listener?.getPostEditorAnalyticsSession()?.switchEditor(editor)
    }

    /**
     * Enables Gutenberg editor if needed and shows informative dialogs.
     * Called when opening a post that may need Gutenberg enabled.
     */
    fun setGutenbergEnabledIfNeeded() {
        val listener = this.listener ?: return
        val site = listener.getSiteModel()

        if (AppPrefs.isGutenbergInfoPopupDisplayed(site.url)) {
            return
        }

        val showPopup = AppPrefs.shouldShowGutenbergInfoPopupForTheNewPosts(site.url)
        val showRolloutPopupPhase2 = AppPrefs.shouldShowGutenbergInfoPopupPhase2ForNewPosts(site.url)

        if (TextUtils.isEmpty(site.mobileEditor) && !listener.isNewPost()) {
            SiteUtils.enableBlockEditor(dispatcher, site)
            AnalyticsUtils.trackWithSiteDetails(
                Stat.EDITOR_GUTENBERG_ENABLED,
                site,
                BlockEditorEnabledSource.ON_BLOCK_POST_OPENING.asPropertyMap()
            )
        }

        if (showPopup) {
            showGutenbergInformativeDialog(site)
        } else if (showRolloutPopupPhase2) {
            showGutenbergRolloutV2InformativeDialog(site)
        }
    }

    /**
     * Shows the Gutenberg informative dialog.
     * Note: Dialog display is currently disabled but preference is still set.
     */
    private fun showGutenbergInformativeDialog(site: SiteModel) {
        // We are no longer showing the dialog, but we are leaving all the surrounding logic because
        // this is going in shortly before release, and we're going to remove all this logic in the
        // very near future.
        AppPrefs.setGutenbergInfoPopupDisplayed(site.url, true)
    }

    /**
     * Shows the Gutenberg rollout V2 informative dialog.
     * Note: Dialog display is currently disabled but preference is still set.
     */
    private fun showGutenbergRolloutV2InformativeDialog(site: SiteModel) {
        // We are no longer showing the dialog, but we are leaving all the surrounding logic because
        // this is going in shortly before release, and we're going to remove all this logic in the
        // very near future.
        AppPrefs.setGutenbergInfoPopupDisplayed(site.url, true)
    }
}
