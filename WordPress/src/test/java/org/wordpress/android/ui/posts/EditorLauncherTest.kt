package org.wordpress.android.ui.posts

import org.junit.Test
import org.junit.Assert.assertEquals

class EditorLauncherTest {
    @Test
    fun `all EditorLauncherParams fields should be handled in addEditorExtras`() {
        // This test ensures that every field in EditorLauncherParams is handled by one of the
        // add*Extras methods in EditorLauncher. When adding new fields to EditorLauncherParams,
        // you must update both this test and the corresponding add*Extras method.

        val handledFields = setOf(
            // addBasicExtras()
            "site",           // -> WordPress.SITE
            "isPage",         // -> EditPostActivityConstants.EXTRA_IS_PAGE
            "isPromo",        // -> EditPostActivityConstants.EXTRA_IS_PROMO

            // addPostExtras()
            "postLocalId",    // -> EditPostActivityConstants.EXTRA_POST_LOCAL_ID
            "postRemoteId",   // -> EditPostActivityConstants.EXTRA_POST_REMOTE_ID
            "loadAutoSaveRevision", // -> EditPostActivityConstants.EXTRA_LOAD_AUTO_SAVE_REVISION
            "isQuickPress",   // -> EditPostActivityConstants.EXTRA_IS_QUICKPRESS
            "isLandingEditor", // -> EditPostActivityConstants.EXTRA_IS_LANDING_EDITOR
            "isLandingEditorOpenedForNewSite", // -> EditPostActivityConstants
                                               //    .EXTRA_IS_LANDING_EDITOR_OPENED_FOR_NEW_SITE

            // addReblogExtras()
            "reblogPostTitle", // -> EditPostActivityConstants.EXTRA_REBLOG_POST_TITLE
            "reblogPostQuote", // -> EditPostActivityConstants.EXTRA_REBLOG_POST_QUOTE
            "reblogPostImage", // -> EditPostActivityConstants.EXTRA_REBLOG_POST_IMAGE
            "reblogPostCitation", // -> EditPostActivityConstants.EXTRA_REBLOG_POST_CITATION
            "reblogAction",   // -> Intent.setAction()

            // addPageExtras()
            "pageTitle",      // -> EditPostActivityConstants.EXTRA_PAGE_TITLE
            "pageContent",    // -> EditPostActivityConstants.EXTRA_PAGE_CONTENT
            "pageTemplate",   // -> EditPostActivityConstants.EXTRA_PAGE_TEMPLATE

            // addMiscExtras()
            "voiceContent",   // -> EditPostActivityConstants.EXTRA_VOICE_CONTENT
            "insertMedia",    // -> EditPostActivityConstants.EXTRA_INSERT_MEDIA
            "source",         // -> AnalyticsUtils.EXTRA_CREATION_SOURCE_DETAIL
            "promptId",       // -> EditPostActivityConstants.EXTRA_PROMPT_ID
            "entryPoint"      // -> EditPostActivityConstants.EXTRA_ENTRY_POINT
        )

        val actualFields = EditorLauncherParams::class.java.declaredFields
            .filter { !it.isSynthetic && !it.name.contains("$") } // Filter out Kotlin synthetic fields
            .map { it.name }
            .toSet()

        assertEquals(
            "All EditorLauncherParams fields must be handled in addEditorExtras(). " +
                    "Missing: ${actualFields - handledFields}, " +
                    "Extra: ${handledFields - actualFields}",
            handledFields,
            actualFields
        )
    }
}
