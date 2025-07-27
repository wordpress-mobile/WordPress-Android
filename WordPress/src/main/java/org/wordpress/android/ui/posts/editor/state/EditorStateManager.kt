package org.wordpress.android.ui.posts.editor.state

import androidx.lifecycle.LifecycleOwner
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.posts.EditPostRepository
import org.wordpress.android.ui.posts.editor.PostLoadingState
import org.wordpress.android.util.AppLog
import javax.inject.Inject
import javax.inject.Singleton

/**
 * PHASE 3: Editor State Management
 * 
 * Manages the overall state of the editor, including:
 * - Editor type determination (Gutenberg vs Aztec)
 * - Editor switching logic
 * - Configuration change handling
 * - Editor loading states
 * - Editor mode transitions
 * 
 * Extracted from EditPostActivity to provide centralized editor state management.
 */
@Singleton
class EditorStateManager @Inject constructor() {

    // Editor types
    enum class EditorType {
        GUTENBERG,
        GUTENBERG_KIT,
        AZTEC
    }

    // Editor modes
    enum class EditorMode {
        VISUAL,
        HTML
    }

    // Configuration
    private var isInitialized = false
    private var listener: EditorStateListener? = null
    private var lifecycleOwner: LifecycleOwner? = null
    private var editPostRepository: EditPostRepository? = null
    private var siteModel: SiteModel? = null
    
    // Current state
    private var currentEditorType: EditorType = EditorType.AZTEC
    private var currentEditorMode: EditorMode = EditorMode.VISUAL
    private var isEditorLoading: Boolean = false
    private var pendingEditorSwitch: EditorType? = null
    
    /**
     * Interface for listening to editor state changes
     */
    interface EditorStateListener {
        fun onEditorTypeChanged(newType: EditorType, oldType: EditorType)
        fun onEditorModeChanged(newMode: EditorMode, oldMode: EditorMode)
        fun onEditorLoadingStateChanged(isLoading: Boolean)
        fun onEditorSwitchRequested(fromType: EditorType, toType: EditorType)
        fun onConfigurationChangeHandled()
        fun showEditorSwitchDialog(fromType: EditorType, toType: EditorType)
    }

    /**
     * Initialize the editor state manager
     */
    fun initialize(
        listener: EditorStateListener,
        lifecycleOwner: LifecycleOwner,
        editPostRepository: EditPostRepository,
        siteModel: SiteModel
    ) {
        this.listener = listener
        this.lifecycleOwner = lifecycleOwner
        this.editPostRepository = editPostRepository
        this.siteModel = siteModel
        this.isInitialized = true
        
        // Determine initial editor type
        currentEditorType = determineEditorType(siteModel)
        
        AppLog.d(AppLog.T.POSTS, "EditorStateManager initialized with type: $currentEditorType")
    }

    /**
     * Determine the appropriate editor type for a site
     */
    fun determineEditorType(site: SiteModel): EditorType {
        return when {
            // Check if Gutenberg Kit is enabled and available
            isGutenbergKitEnabled(site) -> EditorType.GUTENBERG_KIT
            
            // Check if regular Gutenberg is enabled
            isGutenbergEnabled(site) -> EditorType.GUTENBERG
            
            // Default to Aztec
            else -> EditorType.AZTEC
        }
    }

    /**
     * Request a switch to a different editor type
     */
    fun requestEditorSwitch(toType: EditorType) {
        if (!isInitialized) {
            AppLog.w(AppLog.T.POSTS, "EditorStateManager not initialized")
            return
        }

        if (currentEditorType == toType) {
            AppLog.d(AppLog.T.POSTS, "Already using $toType editor")
            return
        }

        AppLog.d(AppLog.T.POSTS, "Requesting editor switch from $currentEditorType to $toType")
        
        // Check if switch is allowed
        if (canSwitchEditor(currentEditorType, toType)) {
            pendingEditorSwitch = toType
            listener?.onEditorSwitchRequested(currentEditorType, toType)
        } else {
            AppLog.w(AppLog.T.POSTS, "Editor switch from $currentEditorType to $toType not allowed")
        }
    }

    /**
     * Confirm and execute the pending editor switch
     */
    fun confirmEditorSwitch() {
        pendingEditorSwitch?.let { newType ->
            val oldType = currentEditorType
            currentEditorType = newType
            pendingEditorSwitch = null
            
            AppLog.d(AppLog.T.POSTS, "Editor switch confirmed: $oldType -> $newType")
            listener?.onEditorTypeChanged(newType, oldType)
        }
    }

    /**
     * Cancel the pending editor switch
     */
    fun cancelEditorSwitch() {
        if (pendingEditorSwitch != null) {
            AppLog.d(AppLog.T.POSTS, "Editor switch cancelled")
            pendingEditorSwitch = null
        }
    }

    /**
     * Switch editor mode (Visual/HTML)
     */
    fun switchEditorMode(newMode: EditorMode) {
        if (!isInitialized) {
            AppLog.w(AppLog.T.POSTS, "EditorStateManager not initialized")
            return
        }

        if (currentEditorMode != newMode) {
            val oldMode = currentEditorMode
            currentEditorMode = newMode
            
            AppLog.d(AppLog.T.POSTS, "Editor mode switched: $oldMode -> $newMode")
            listener?.onEditorModeChanged(newMode, oldMode)
        }
    }

    /**
     * Update editor loading state
     */
    fun setEditorLoading(isLoading: Boolean) {
        if (this.isEditorLoading != isLoading) {
            this.isEditorLoading = isLoading
            AppLog.d(AppLog.T.POSTS, "Editor loading state changed: $isLoading")
            listener?.onEditorLoadingStateChanged(isLoading)
        }
    }

    /**
     * Handle configuration changes
     */
    fun handleConfigurationChange() {
        if (!isInitialized) {
            AppLog.w(AppLog.T.POSTS, "EditorStateManager not initialized")
            return
        }

        AppLog.d(AppLog.T.POSTS, "Handling configuration change")
        
        // Preserve current state and notify listener
        listener?.onConfigurationChangeHandled()
    }

    /**
     * Check if editor switch is allowed
     */
    private fun canSwitchEditor(fromType: EditorType, toType: EditorType): Boolean {
        // Don't allow switch if content would be lost
        val hasContent = editPostRepository?.hasPost() == true && 
                        !editPostRepository?.content.isNullOrEmpty()
        
        if (hasContent) {
            // Show dialog for user confirmation
            listener?.showEditorSwitchDialog(fromType, toType)
            return false  // Will be confirmed later through dialog
        }
        
        return true
    }

    /**
     * Check if Gutenberg is enabled for this site
     */
    private fun isGutenbergEnabled(site: SiteModel): Boolean {
        // Simplified logic - in real implementation this would check:
        // - Site settings
        // - Feature flags
        // - User preferences
        return site.isWPCom || site.isWPComAtomic
    }

    /**
     * Check if Gutenberg Kit is enabled for this site
     */
    private fun isGutenbergKitEnabled(site: SiteModel): Boolean {
        // Simplified logic - would check specific feature flags
        return false  // Disabled for now
    }

    // Getters
    fun getCurrentEditorType(): EditorType = currentEditorType
    fun getCurrentEditorMode(): EditorMode = currentEditorMode
    fun isEditorLoading(): Boolean = isEditorLoading
    fun hasPendingSwitch(): Boolean = pendingEditorSwitch != null
    fun getPendingSwitch(): EditorType? = pendingEditorSwitch

    /**
     * Get editor type display name
     */
    fun getEditorDisplayName(type: EditorType): String {
        return when (type) {
            EditorType.GUTENBERG -> "Gutenberg"
            EditorType.GUTENBERG_KIT -> "Gutenberg Kit"
            EditorType.AZTEC -> "Aztec"
        }
    }

    /**
     * Check if editor supports specific features
     */
    fun supportsFeature(feature: EditorFeature): Boolean {
        return when (feature) {
            EditorFeature.BLOCKS -> currentEditorType != EditorType.AZTEC
            EditorFeature.HTML_MODE -> true
            EditorFeature.MEDIA_UPLOAD -> true
            EditorFeature.REAL_TIME_COLLABORATION -> currentEditorType == EditorType.GUTENBERG_KIT
        }
    }

    enum class EditorFeature {
        BLOCKS,
        HTML_MODE,
        MEDIA_UPLOAD,
        REAL_TIME_COLLABORATION
    }

    /**
     * Cleanup resources
     */
    fun cleanup() {
        listener = null
        lifecycleOwner = null
        editPostRepository = null
        siteModel = null
        isInitialized = false
        pendingEditorSwitch = null
        
        AppLog.d(AppLog.T.POSTS, "EditorStateManager cleaned up")
    }
}
