package org.wordpress.android.ui.posts.editor.fragment

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LifecycleOwner
import org.wordpress.android.editor.AztecEditorFragment
import org.wordpress.android.editor.gutenberg.GutenbergEditorFragment
import org.wordpress.android.fluxc.model.PostImmutableModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.posts.EditPostRepository
import org.wordpress.android.ui.posts.editor.state.EditorStateManager
import org.wordpress.android.util.AppLog
import javax.inject.Inject
import javax.inject.Singleton

/**
 * PHASE 3: Editor Fragment Management
 * 
 * Manages the lifecycle and switching of editor fragments:
 * - Fragment creation and initialization
 * - Fragment switching between editor types
 * - Fragment state management
 * - Content transfer between fragments
 * - Fragment cleanup and disposal
 * 
 * Extracted from EditPostActivity to centralize fragment management logic.
 */
@Singleton
class EditorFragmentManager @Inject constructor() {

    // Configuration
    private var isInitialized = false
    private var listener: FragmentManagerListener? = null
    private var lifecycleOwner: LifecycleOwner? = null
    private var fragmentManager: FragmentManager? = null
    private var editPostRepository: EditPostRepository? = null
    private var siteModel: SiteModel? = null
    
    // Current state
    private var currentFragment: Fragment? = null
    private var currentEditorType: EditorStateManager.EditorType? = null
    private var isFragmentReady = false
    
    /**
     * Interface for fragment management events
     */
    interface FragmentManagerListener {
        fun onFragmentCreated(fragment: Fragment, editorType: EditorStateManager.EditorType)
        fun onFragmentReady(fragment: Fragment)
        fun onFragmentSwitching(fromFragment: Fragment?, toFragment: Fragment)
        fun onFragmentSwitched(newFragment: Fragment, editorType: EditorStateManager.EditorType)
        fun onFragmentError(error: String, editorType: EditorStateManager.EditorType)
        fun onContentTransferred(fromType: EditorStateManager.EditorType, toType: EditorStateManager.EditorType)
    }

    /**
     * Initialize the fragment manager
     */
    fun initialize(
        listener: FragmentManagerListener,
        lifecycleOwner: LifecycleOwner,
        fragmentManager: FragmentManager,
        editPostRepository: EditPostRepository,
        siteModel: SiteModel
    ) {
        this.listener = listener
        this.lifecycleOwner = lifecycleOwner
        this.fragmentManager = fragmentManager
        this.editPostRepository = editPostRepository
        this.siteModel = siteModel
        this.isInitialized = true
        
        AppLog.d(AppLog.T.POSTS, "EditorFragmentManager initialized")
    }

    /**
     * Create and return the appropriate editor fragment
     */
    fun createEditorFragment(editorType: EditorStateManager.EditorType): Fragment {
        if (!isInitialized) {
            throw IllegalStateException("EditorFragmentManager not initialized")
        }

        val fragment = when (editorType) {
            EditorStateManager.EditorType.GUTENBERG -> createGutenbergFragment()
            EditorStateManager.EditorType.GUTENBERG_KIT -> createGutenbergKitFragment()
            EditorStateManager.EditorType.AZTEC -> createAztecFragment()
        }

        currentFragment = fragment
        currentEditorType = editorType
        isFragmentReady = false

        AppLog.d(AppLog.T.POSTS, "Created ${editorType.name} editor fragment")
        listener?.onFragmentCreated(fragment, editorType)

        return fragment
    }

    /**
     * Switch to a different editor fragment
     */
    fun switchToEditor(
        newEditorType: EditorStateManager.EditorType,
        preserveContent: Boolean = true
    ) {
        if (!isInitialized) {
            AppLog.w(AppLog.T.POSTS, "EditorFragmentManager not initialized")
            return
        }

        if (currentEditorType == newEditorType) {
            AppLog.d(AppLog.T.POSTS, "Already using ${newEditorType.name} editor")
            return
        }

        val oldFragment = currentFragment
        val oldType = currentEditorType

        AppLog.d(AppLog.T.POSTS, "Switching from ${oldType?.name} to ${newEditorType.name}")

        // Create new fragment
        val newFragment = createEditorFragment(newEditorType)

        // Transfer content if requested
        if (preserveContent && oldFragment != null && oldType != null) {
            transferContent(oldFragment, oldType, newFragment, newEditorType)
        }

        // Notify about switching
        listener?.onFragmentSwitching(oldFragment, newFragment)

        // Update current state
        currentFragment = newFragment
        currentEditorType = newEditorType
        isFragmentReady = false

        // Notify about completion
        listener?.onFragmentSwitched(newFragment, newEditorType)
    }

    /**
     * Mark the current fragment as ready
     */
    fun markFragmentReady() {
        if (currentFragment != null && !isFragmentReady) {
            isFragmentReady = true
            AppLog.d(AppLog.T.POSTS, "Fragment marked as ready: ${currentEditorType?.name}")
            listener?.onFragmentReady(currentFragment!!)
        }
    }

    /**
     * Get the current editor fragment
     */
    fun getCurrentFragment(): Fragment? = currentFragment

    /**
     * Get the current editor type
     */
    fun getCurrentEditorType(): EditorStateManager.EditorType? = currentEditorType

    /**
     * Check if current fragment is ready
     */
    fun isCurrentFragmentReady(): Boolean = isFragmentReady

    /**
     * Update fragment with post content
     */
    fun updateFragmentContent(post: PostImmutableModel) {
        currentFragment?.let { fragment ->
            when (fragment) {
                is GutenbergEditorFragment -> {
                    fragment.updateContent(post.content, post.title)
                }
                is AztecEditorFragment -> {
                    fragment.setTitle(post.title)
                    fragment.setContent(post.content)
                }
                else -> {
                    AppLog.w(AppLog.T.POSTS, "Unknown fragment type for content update")
                }
            }
            AppLog.d(AppLog.T.POSTS, "Updated fragment content")
        }
    }

    /**
     * Get content from current fragment
     */
    fun getContentFromFragment(): Pair<String, String>? {
        return currentFragment?.let { fragment ->
            when (fragment) {
                is GutenbergEditorFragment -> {
                    Pair(fragment.getTitle() ?: "", fragment.getContent() ?: "")
                }
                is AztecEditorFragment -> {
                    Pair(fragment.getTitle() ?: "", fragment.getContent() ?: "")
                }
                else -> {
                    AppLog.w(AppLog.T.POSTS, "Unknown fragment type for content retrieval")
                    null
                }
            }
        }
    }

    /**
     * Create Gutenberg editor fragment
     */
    private fun createGutenbergFragment(): GutenbergEditorFragment {
        return GutenbergEditorFragment.newInstance(
            // Pass necessary parameters for Gutenberg
        )
    }

    /**
     * Create Gutenberg Kit editor fragment
     */
    private fun createGutenbergKitFragment(): Fragment {
        // For now, return regular Gutenberg
        // In real implementation, this would create GutenbergKitEditorFragment
        return createGutenbergFragment()
    }

    /**
     * Create Aztec editor fragment
     */
    private fun createAztecFragment(): AztecEditorFragment {
        return AztecEditorFragment.newInstance(
            "", // title
            "", // content
            false // expanded toolbar
        )
    }

    /**
     * Transfer content between editor fragments
     */
    private fun transferContent(
        fromFragment: Fragment,
        fromType: EditorStateManager.EditorType,
        toFragment: Fragment,
        toType: EditorStateManager.EditorType
    ) {
        try {
            // Get content from source fragment
            val (title, content) = getContentFromFragmentDirect(fromFragment) ?: return

            // Convert content if needed
            val convertedContent = convertContent(content, fromType, toType)
            val convertedTitle = title // Title usually doesn't need conversion

            // Set content in target fragment
            setContentInFragmentDirect(toFragment, convertedTitle, convertedContent)

            AppLog.d(AppLog.T.POSTS, "Content transferred from ${fromType.name} to ${toType.name}")
            listener?.onContentTransferred(fromType, toType)

        } catch (e: Exception) {
            AppLog.e(AppLog.T.POSTS, "Error transferring content: ${e.message}")
            listener?.onFragmentError("Content transfer failed: ${e.message}", toType)
        }
    }

    /**
     * Get content directly from a fragment
     */
    private fun getContentFromFragmentDirect(fragment: Fragment): Pair<String, String>? {
        return when (fragment) {
            is GutenbergEditorFragment -> {
                Pair(fragment.getTitle() ?: "", fragment.getContent() ?: "")
            }
            is AztecEditorFragment -> {
                Pair(fragment.getTitle() ?: "", fragment.getContent() ?: "")
            }
            else -> null
        }
    }

    /**
     * Set content directly in a fragment
     */
    private fun setContentInFragmentDirect(fragment: Fragment, title: String, content: String) {
        when (fragment) {
            is GutenbergEditorFragment -> {
                fragment.updateContent(content, title)
            }
            is AztecEditorFragment -> {
                fragment.setTitle(title)
                fragment.setContent(content)
            }
        }
    }

    /**
     * Convert content between editor formats
     */
    private fun convertContent(
        content: String,
        fromType: EditorStateManager.EditorType,
        toType: EditorStateManager.EditorType
    ): String {
        return when {
            fromType == EditorStateManager.EditorType.AZTEC && 
            toType in listOf(EditorStateManager.EditorType.GUTENBERG, EditorStateManager.EditorType.GUTENBERG_KIT) -> {
                // Convert HTML to Gutenberg blocks
                convertHtmlToBlocks(content)
            }
            fromType in listOf(EditorStateManager.EditorType.GUTENBERG, EditorStateManager.EditorType.GUTENBERG_KIT) &&
            toType == EditorStateManager.EditorType.AZTEC -> {
                // Convert Gutenberg blocks to HTML
                convertBlocksToHtml(content)
            }
            else -> {
                // No conversion needed or same type
                content
            }
        }
    }

    /**
     * Convert HTML content to Gutenberg blocks
     */
    private fun convertHtmlToBlocks(htmlContent: String): String {
        // Simplified conversion - in real implementation this would use
        // proper HTML to blocks conversion logic
        return if (htmlContent.isBlank()) {
            ""
        } else {
            "<!-- wp:paragraph -->\n<p>$htmlContent</p>\n<!-- /wp:paragraph -->"
        }
    }

    /**
     * Convert Gutenberg blocks to HTML
     */
    private fun convertBlocksToHtml(blocksContent: String): String {
        // Simplified conversion - in real implementation this would use
        // proper blocks to HTML conversion logic
        return blocksContent
            .replace(Regex("<!-- wp:.*? -->"), "")
            .replace(Regex("<!-- /wp:.*? -->"), "")
            .trim()
    }

    /**
     * Handle fragment lifecycle events
     */
    fun onFragmentResumed() {
        AppLog.d(AppLog.T.POSTS, "Fragment resumed: ${currentEditorType?.name}")
    }

    fun onFragmentPaused() {
        AppLog.d(AppLog.T.POSTS, "Fragment paused: ${currentEditorType?.name}")
    }

    fun onFragmentStopped() {
        AppLog.d(AppLog.T.POSTS, "Fragment stopped: ${currentEditorType?.name}")
    }

    /**
     * Cleanup resources
     */
    fun cleanup() {
        currentFragment = null
        currentEditorType = null
        isFragmentReady = false
        
        listener = null
        lifecycleOwner = null
        fragmentManager = null
        editPostRepository = null
        siteModel = null
        isInitialized = false
        
        AppLog.d(AppLog.T.POSTS, "EditorFragmentManager cleaned up")
    }
}