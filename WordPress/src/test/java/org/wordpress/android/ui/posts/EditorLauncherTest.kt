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
            "siteSource",     // -> WordPress.SITE or EditorConstants.EXTRA_QUICKPRESS_BLOG_ID
            "isPage",         // -> EditorConstants.EXTRA_IS_PAGE
            "isPromo",        // -> EditorConstants.EXTRA_IS_PROMO

            // addPostExtras()
            "postLocalId",    // -> EditorConstants.EXTRA_POST_LOCAL_ID
            "postRemoteId",   // -> EditorConstants.EXTRA_POST_REMOTE_ID
            "loadAutoSaveRevision", // -> EditorConstants.EXTRA_LOAD_AUTO_SAVE_REVISION
            "isQuickPress",   // -> EditorConstants.EXTRA_IS_QUICKPRESS
            "isLandingEditor", // -> EditorConstants.EXTRA_IS_LANDING_EDITOR
            "isLandingEditorOpenedForNewSite", // -> EditorConstants
                                               //    .EXTRA_IS_LANDING_EDITOR_OPENED_FOR_NEW_SITE

            // addReblogExtras()
            "reblogPostTitle", // -> EditorConstants.EXTRA_REBLOG_POST_TITLE
            "reblogPostQuote", // -> EditorConstants.EXTRA_REBLOG_POST_QUOTE
            "reblogPostImage", // -> EditorConstants.EXTRA_REBLOG_POST_IMAGE
            "reblogPostCitation", // -> EditorConstants.EXTRA_REBLOG_POST_CITATION
            "reblogAction",   // -> Intent.setAction()

            // addPageExtras()
            "pageTitle",      // -> EditorConstants.EXTRA_PAGE_TITLE
            "pageContent",    // -> EditorConstants.EXTRA_PAGE_CONTENT
            "pageTemplate",   // -> EditorConstants.EXTRA_PAGE_TEMPLATE

            // addMiscExtras()
            "voiceContent",   // -> EditorConstants.EXTRA_VOICE_CONTENT
            "insertMedia",    // -> EditorConstants.EXTRA_INSERT_MEDIA
            "source",         // -> AnalyticsUtils.EXTRA_CREATION_SOURCE_DETAIL
            "promptId",       // -> EditorConstants.EXTRA_PROMPT_ID
            "entryPoint"      // -> EditorConstants.EXTRA_ENTRY_POINT
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
