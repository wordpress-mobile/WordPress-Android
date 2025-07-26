package org.wordpress.android.ui.posts.editor.publishing

import android.text.TextUtils
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.fluxc.model.AccountModel
import org.wordpress.android.fluxc.model.PostImmutableModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.MediaStore
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.DateTimeUtils
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service for validating posts before publishing, scheduling, or saving.
 * Extracted from EditPostActivity to centralize validation logic.
 */
@Singleton
class PostValidationService @Inject constructor(
    private val mediaStore: MediaStore
) {
    
    private var accountStore: AccountStore? = null
    private var siteModel: SiteModel? = null
    
    fun initialize(accountStore: AccountStore, siteModel: SiteModel) {
        this.accountStore = accountStore
        this.siteModel = siteModel
    }
    
    /**
     * Validates a post before publishing
     */
    fun validateForPublishing(post: PostImmutableModel): ValidationResult {
        AppLog.d(AppLog.T.POSTS, "PostValidationService: Validating post for publishing")
        
        // Check email verification
        val emailValidation = validateEmailVerification()
        if (!emailValidation.isValid) {
            return emailValidation
        }
        
        // Check post content
        val contentValidation = validatePostContent(post)
        if (!contentValidation.isValid) {
            return contentValidation
        }
        
        // Check media uploads
        val mediaValidation = validateMediaUploads(post)
        if (!mediaValidation.isValid) {
            return mediaValidation
        }
        
        // Check site permissions
        val permissionValidation = validatePublishingPermissions(post)
        if (!permissionValidation.isValid) {
            return permissionValidation
        }
        
        AppLog.d(AppLog.T.POSTS, "Post validation passed for publishing")
        return ValidationResult.valid()
    }
    
    /**
     * Validates a post before scheduling
     */
    fun validateForScheduling(post: PostImmutableModel, scheduledDate: Date): ValidationResult {
        AppLog.d(AppLog.T.POSTS, "PostValidationService: Validating post for scheduling")
        
        // First run standard publishing validation
        val publishValidation = validateForPublishing(post)
        if (!publishValidation.isValid) {
            return publishValidation
        }
        
        // Validate scheduled date is in the future
        val dateValidation = validateScheduledDate(scheduledDate)
        if (!dateValidation.isValid) {
            return dateValidation
        }
        
        // Check if site supports scheduling
        val schedulingSupport = validateSchedulingSupport()
        if (!schedulingSupport.isValid) {
            return schedulingSupport
        }
        
        AppLog.d(AppLog.T.POSTS, "Post validation passed for scheduling")
        return ValidationResult.valid()
    }
    
    /**
     * Validates a post before saving as draft
     */
    fun validateForSaving(post: PostImmutableModel): ValidationResult {
        AppLog.d(AppLog.T.POSTS, "PostValidationService: Validating post for saving")
        
        // Basic content validation (less strict than publishing)
        if (post.title.isBlank() && post.content.isBlank()) {
            return ValidationResult.invalid(
                "Post must have either a title or content to be saved"
            )
        }
        
        // Check if user has permission to save drafts
        val site = siteModel
        if (site != null && !site.hasCapabilityEditPosts) {
            return ValidationResult.invalid(
                "You don't have permission to save posts on this site"
            )
        }
        
        AppLog.d(AppLog.T.POSTS, "Post validation passed for saving")
        return ValidationResult.valid()
    }
    
    /**
     * Validates account email verification status
     */
    private fun validateEmailVerification(): ValidationResult {
        val accountStore = this.accountStore ?: return ValidationResult.invalid("Account store not initialized")
        val account: AccountModel = accountStore.account
        
        if (!account.emailVerified) {
            val message = if (TextUtils.isEmpty(account.email)) {
                WordPress.getContext().getString(R.string.editor_confirm_email_prompt_message)
            } else {
                String.format(
                    WordPress.getContext().getString(R.string.editor_confirm_email_prompt_message_with_email),
                    account.email
                )
            }
            
            AppLog.w(AppLog.T.POSTS, "Email verification required: ${account.email}")
            return ValidationResult.invalid(message, requiresEmailVerification = true)
        }
        
        return ValidationResult.valid()
    }
    
    /**
     * Validates post content requirements
     */
    private fun validatePostContent(post: PostImmutableModel): ValidationResult {
        // Check minimum content requirements
        if (post.title.isBlank() && post.content.isBlank()) {
            return ValidationResult.invalid(
                "Post must have either a title or content before publishing"
            )
        }
        
        // Check for prohibited content (basic checks)
        if (containsProhibitedContent(post.content)) {
            return ValidationResult.invalid(
                "Post contains prohibited content that cannot be published"
            )
        }
        
        // Validate excerpts if present
        if (post.excerpt.length > MAX_EXCERPT_LENGTH) {
            return ValidationResult.invalid(
                "Post excerpt is too long (maximum $MAX_EXCERPT_LENGTH characters)"
            )
        }
        
        return ValidationResult.valid()
    }
    
    /**
     * Validates media upload status
     */
    private fun validateMediaUploads(post: PostImmutableModel): ValidationResult {
        val mediaList = mediaStore.getMediaForPost(post)
        
        // Check for failed uploads
        val failedUploads = mediaList.filter { media ->
            media.uploadState == "FAILED"
        }
        
        if (failedUploads.isNotEmpty()) {
            AppLog.w(AppLog.T.POSTS, "Found ${failedUploads.size} failed media uploads")
            return ValidationResult.invalid(
                "Some media uploads have failed. Please retry or remove failed media before publishing."
            )
        }
        
        // Check for uploads in progress
        val uploadsInProgress = mediaList.filter { media ->
            media.uploadState == "UPLOADING" || media.uploadState == "QUEUED"
        }
        
        if (uploadsInProgress.isNotEmpty()) {
            AppLog.w(AppLog.T.POSTS, "Found ${uploadsInProgress.size} media uploads in progress")
            return ValidationResult.invalid(
                "Media uploads are still in progress. Please wait for uploads to complete before publishing."
            )
        }
        
        return ValidationResult.valid()
    }
    
    /**
     * Validates publishing permissions for the site
     */
    private fun validatePublishingPermissions(post: PostImmutableModel): ValidationResult {
        val site = siteModel ?: return ValidationResult.invalid("Site not available")
        
        // Check basic publishing capability
        if (!site.hasCapabilityPublishPosts) {
            return ValidationResult.invalid(
                "You don't have permission to publish posts on this site"
            )
        }
        
        // Check if post is already published and user can edit published posts
        if (post.status == "publish" && !site.hasCapabilityEditPublishedPosts) {
            return ValidationResult.invalid(
                "You don't have permission to edit published posts on this site"
            )
        }
        
        // Check category/tag limitations if any
        // This could be expanded based on site-specific restrictions
        
        return ValidationResult.valid()
    }
    
    /**
     * Validates scheduled date
     */
    private fun validateScheduledDate(scheduledDate: Date): ValidationResult {
        val currentTime = Date()
        
        if (scheduledDate.before(currentTime)) {
            return ValidationResult.invalid(
                "Scheduled date must be in the future"
            )
        }
        
        // Check if date is too far in the future (optional business rule)
        val maxFutureDate = DateTimeUtils.addMonths(currentTime, 12) // 1 year maximum
        if (scheduledDate.after(maxFutureDate)) {
            return ValidationResult.invalid(
                "Scheduled date cannot be more than 1 year in the future"
            )
        }
        
        return ValidationResult.valid()
    }
    
    /**
     * Validates site supports scheduling
     */
    private fun validateSchedulingSupport(): ValidationResult {
        val site = siteModel ?: return ValidationResult.invalid("Site not available")
        
        // Most WordPress sites support scheduling, but we could add specific checks here
        if (!site.hasCapabilityPublishPosts) {
            return ValidationResult.invalid(
                "You don't have permission to schedule posts on this site"
            )
        }
        
        return ValidationResult.valid()
    }
    
    /**
     * Basic content filtering
     */
    private fun containsProhibitedContent(content: String): Boolean {
        // This is a placeholder for more sophisticated content filtering
        // Could include spam detection, inappropriate content checks, etc.
        
        // Example: Check for excessive links (basic spam detection)
        val linkPattern = Regex("<a\\s+[^>]*href", RegexOption.IGNORE_CASE)
        val linkCount = linkPattern.findAll(content).count()
        
        if (linkCount > MAX_LINKS_PER_POST) {
            AppLog.w(AppLog.T.POSTS, "Post contains too many links: $linkCount")
            return true
        }
        
        return false
    }
    
    fun cleanup() {
        accountStore = null
        siteModel = null
    }
    
    companion object {
        private const val MAX_EXCERPT_LENGTH = 300
        private const val MAX_LINKS_PER_POST = 20
    }
}

/**
 * Result of a validation operation
 */
data class ValidationResult(
    val isValid: Boolean,
    val errorMessage: String = "",
    val requiresEmailVerification: Boolean = false,
    val validationCode: ValidationCode = ValidationCode.SUCCESS
) {
    companion object {
        fun valid() = ValidationResult(isValid = true)
        
        fun invalid(
            message: String,
            requiresEmailVerification: Boolean = false,
            code: ValidationCode = ValidationCode.GENERAL_ERROR
        ) = ValidationResult(
            isValid = false,
            errorMessage = message,
            requiresEmailVerification = requiresEmailVerification,
            validationCode = code
        )
    }
}

/**
 * Specific validation error codes for handling different validation failures
 */
enum class ValidationCode {
    SUCCESS,
    EMAIL_VERIFICATION_REQUIRED,
    INSUFFICIENT_CONTENT,
    PROHIBITED_CONTENT,
    MEDIA_UPLOAD_FAILED,
    MEDIA_UPLOAD_IN_PROGRESS,
    INSUFFICIENT_PERMISSIONS,
    INVALID_SCHEDULED_DATE,
    SCHEDULING_NOT_SUPPORTED,
    GENERAL_ERROR
}