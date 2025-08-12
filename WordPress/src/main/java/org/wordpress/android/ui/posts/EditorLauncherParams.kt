package org.wordpress.android.ui.posts

import org.wordpress.android.ui.PagePostCreationSourcesDetail
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.posts.PostUtils.EntryPoint

/**
 * Type-safe parameters for launching editor activities.
 *
 * This data class replaces the Bundle-based approach with named parameters in Kotlin
 * and provides a builder pattern for Java compatibility.
 *
 * All fields are mapped to Intent extras in EditorLauncher.addEditorExtras().
 * See EditorLauncherTest for field-to-method mapping documentation.
 */
data class EditorLauncherParams(
    val site: SiteModel,
    val isPage: Boolean? = null,
    val isPromo: Boolean? = null,
    val postLocalId: Int? = null,
    val postRemoteId: Long? = null,
    val loadAutoSaveRevision: Boolean? = null,
    val isQuickPress: Boolean? = null,
    val isLandingEditor: Boolean? = null,
    val isLandingEditorOpenedForNewSite: Boolean? = null,
    val reblogPostTitle: String? = null,
    val reblogPostQuote: String? = null,
    val reblogPostImage: String? = null,
    val reblogPostCitation: String? = null,
    val reblogAction: String? = null,
    val pageTitle: String? = null,
    val pageContent: String? = null,
    val pageTemplate: String? = null,
    val voiceContent: String? = null,
    val insertMedia: ArrayList<*>? = null,
    val source: PagePostCreationSourcesDetail? = null,
    val promptId: Int? = null,
    val entryPoint: EntryPoint? = null
) {
    /**
     * Java-friendly builder pattern for EditorLauncherParams.
     */
    class Builder(private val site: SiteModel) {
        private var isPage: Boolean? = null
        private var isPromo: Boolean? = null
        private var postLocalId: Int? = null
        private var postRemoteId: Long? = null
        private var loadAutoSaveRevision: Boolean? = null
        private var isQuickPress: Boolean? = null
        private var isLandingEditor: Boolean? = null
        private var isLandingEditorOpenedForNewSite: Boolean? = null
        private var reblogPostTitle: String? = null
        private var reblogPostQuote: String? = null
        private var reblogPostImage: String? = null
        private var reblogPostCitation: String? = null
        private var reblogAction: String? = null
        private var pageTitle: String? = null
        private var pageContent: String? = null
        private var pageTemplate: String? = null
        private var voiceContent: String? = null
        private var insertMedia: ArrayList<*>? = null
        private var source: PagePostCreationSourcesDetail? = null
        private var promptId: Int? = null
        private var entryPoint: EntryPoint? = null

        fun isPage(isPage: Boolean?) = apply { this.isPage = isPage }
        fun isPromo(isPromo: Boolean?) = apply { this.isPromo = isPromo }
        fun postLocalId(postLocalId: Int?) = apply { this.postLocalId = postLocalId }
        fun postRemoteId(postRemoteId: Long?) = apply { this.postRemoteId = postRemoteId }
        fun loadAutoSaveRevision(loadAutoSaveRevision: Boolean?) = apply {
            this.loadAutoSaveRevision = loadAutoSaveRevision
        }

        fun isQuickPress(isQuickPress: Boolean?) = apply { this.isQuickPress = isQuickPress }
        fun isLandingEditor(isLandingEditor: Boolean?) = apply { this.isLandingEditor = isLandingEditor }
        fun isLandingEditorOpenedForNewSite(isLandingEditorOpenedForNewSite: Boolean?) = apply {
            this.isLandingEditorOpenedForNewSite = isLandingEditorOpenedForNewSite
        }

        fun reblogPostTitle(reblogPostTitle: String?) = apply { this.reblogPostTitle = reblogPostTitle }
        fun reblogPostQuote(reblogPostQuote: String?) = apply { this.reblogPostQuote = reblogPostQuote }
        fun reblogPostImage(reblogPostImage: String?) = apply { this.reblogPostImage = reblogPostImage }
        fun reblogPostCitation(reblogPostCitation: String?) = apply { this.reblogPostCitation = reblogPostCitation }
        fun reblogAction(reblogAction: String?) = apply { this.reblogAction = reblogAction }
        fun pageTitle(pageTitle: String?) = apply { this.pageTitle = pageTitle }
        fun pageContent(pageContent: String?) = apply { this.pageContent = pageContent }
        fun pageTemplate(pageTemplate: String?) = apply { this.pageTemplate = pageTemplate }
        fun voiceContent(voiceContent: String?) = apply { this.voiceContent = voiceContent }
        fun insertMedia(insertMedia: ArrayList<*>?) = apply { this.insertMedia = insertMedia }
        fun source(source: PagePostCreationSourcesDetail?) = apply { this.source = source }
        fun promptId(promptId: Int?) = apply { this.promptId = promptId }
        fun entryPoint(entryPoint: EntryPoint?) = apply { this.entryPoint = entryPoint }

        fun build(): EditorLauncherParams {
            return EditorLauncherParams(
                site = site,
                isPage = isPage,
                isPromo = isPromo,
                postLocalId = postLocalId,
                postRemoteId = postRemoteId,
                loadAutoSaveRevision = loadAutoSaveRevision,
                isQuickPress = isQuickPress,
                isLandingEditor = isLandingEditor,
                isLandingEditorOpenedForNewSite = isLandingEditorOpenedForNewSite,
                reblogPostTitle = reblogPostTitle,
                reblogPostQuote = reblogPostQuote,
                reblogPostImage = reblogPostImage,
                reblogPostCitation = reblogPostCitation,
                reblogAction = reblogAction,
                pageTitle = pageTitle,
                pageContent = pageContent,
                pageTemplate = pageTemplate,
                voiceContent = voiceContent,
                insertMedia = insertMedia,
                source = source,
                promptId = promptId,
                entryPoint = entryPoint
            )
        }
    }
}
