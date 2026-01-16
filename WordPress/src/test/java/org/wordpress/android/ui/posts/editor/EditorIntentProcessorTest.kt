package org.wordpress.android.ui.posts.editor

import android.content.Intent
import android.os.Bundle
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.ui.posts.EditorConstants
import org.wordpress.android.util.ReblogUtils

@RunWith(MockitoJUnitRunner::class)
class EditorIntentProcessorTest {
    @Mock
    private lateinit var siteStore: SiteStore

    @Mock
    private lateinit var reblogUtils: ReblogUtils

    @Mock
    private lateinit var siteModel: SiteModel

    private lateinit var processor: EditorIntentProcessor

    @Before
    fun setUp() {
        processor = EditorIntentProcessor(siteStore, reblogUtils)
    }

    // region processIntent - NewPost tests
    @Test
    fun `processIntent returns NewPost for empty extras without post id`() {
        // Arrange
        val intent = mock<Intent>()
        val extras = createMockBundle()

        // Act
        val result = processor.processIntent(intent, extras, siteModel, isRestarting = false)

        // Assert
        assertThat(result).isInstanceOf(EditorIntentProcessor.IntentProcessResult.NewPost::class.java)
    }

    @Test
    fun `processIntent returns NewPost with isPage true when EXTRA_IS_PAGE is true`() {
        // Arrange
        val intent = mock<Intent>()
        val extras = createMockBundle(isPage = true)

        // Act
        val result = processor.processIntent(intent, extras, siteModel, isRestarting = false)

        // Assert
        assertThat(result).isInstanceOf(EditorIntentProcessor.IntentProcessResult.NewPost::class.java)
        assertThat((result as EditorIntentProcessor.IntentProcessResult.NewPost).isPage).isTrue()
    }
    // endregion

    // region processIntent - ExistingPost tests
    @Test
    fun `processIntent returns ExistingPost when local post id is present`() {
        // Arrange
        val intent = mock<Intent>()
        val extras = createMockBundle(localPostId = 123)

        // Act
        val result = processor.processIntent(intent, extras, siteModel, isRestarting = false)

        // Assert
        assertThat(result).isInstanceOf(EditorIntentProcessor.IntentProcessResult.ExistingPost::class.java)
        assertThat((result as EditorIntentProcessor.IntentProcessResult.ExistingPost).localPostId).isEqualTo(123)
    }

    @Test
    fun `processIntent returns ExistingPost with loadAutoSaveRevision when flag is set`() {
        // Arrange
        val intent = mock<Intent>()
        val extras = createMockBundle(localPostId = 123, loadAutoSaveRevision = true)

        // Act
        val result = processor.processIntent(intent, extras, siteModel, isRestarting = false)

        // Assert
        assertThat(result).isInstanceOf(EditorIntentProcessor.IntentProcessResult.ExistingPost::class.java)
        val existingPostResult = result as EditorIntentProcessor.IntentProcessResult.ExistingPost
        assertThat(existingPostResult.localPostId).isEqualTo(123)
        assertThat(existingPostResult.loadAutoSaveRevision).isTrue()
    }
    // endregion

    // region processIntent - ShareAction tests
    @Test
    fun `processIntent returns ShareActionText for ACTION_SEND with text`() {
        // Arrange
        val intent = mock<Intent>()
        whenever(intent.action).thenReturn(Intent.ACTION_SEND)
        whenever(intent.getStringExtra(Intent.EXTRA_SUBJECT)).thenReturn("Test Title")
        whenever(intent.getStringExtra(Intent.EXTRA_TEXT)).thenReturn("Test content")
        val extras = createMockBundle()

        // Act
        val result = processor.processIntent(intent, extras, siteModel, isRestarting = false)

        // Assert
        assertThat(result).isInstanceOf(EditorIntentProcessor.IntentProcessResult.ShareActionText::class.java)
        val shareResult = result as EditorIntentProcessor.IntentProcessResult.ShareActionText
        assertThat(shareResult.title).isEqualTo("Test Title")
    }
    // endregion

    // region processIntent - Reblog tests
    @Test
    fun `processIntent returns Reblog for ACTION_REBLOG`() {
        // Arrange
        val intent = mock<Intent>()
        whenever(intent.action).thenReturn(EditorConstants.ACTION_REBLOG)
        whenever(intent.getStringExtra(EditorConstants.EXTRA_REBLOG_POST_TITLE)).thenReturn("Reblog Title")
        whenever(intent.getStringExtra(EditorConstants.EXTRA_REBLOG_POST_QUOTE)).thenReturn("Quote")
        whenever(reblogUtils.reblogContent(
            imageUrl = anyOrNull(),
            quote = any(),
            citationTitle = anyOrNull(),
            citationUrl = anyOrNull(),
            isGutenberg = any()
        )).thenReturn("Reblog content")
        val extras = createMockBundle()

        // Act
        val result = processor.processIntent(intent, extras, siteModel, isRestarting = false)

        // Assert
        assertThat(result).isInstanceOf(EditorIntentProcessor.IntentProcessResult.Reblog::class.java)
        val reblogResult = result as EditorIntentProcessor.IntentProcessResult.Reblog
        assertThat(reblogResult.title).isEqualTo("Reblog Title")
        assertThat(reblogResult.content).isEqualTo("Reblog content")
    }
    // endregion

    // region processIntent - PageFromLayout tests
    @Test
    fun `processIntent returns PageFromLayout when isPage with title and template`() {
        // Arrange
        val intent = mock<Intent>()
        whenever(siteStore.getBlockLayoutContent(eq(siteModel), any())).thenReturn("Layout content")
        val extras = createMockBundle(
            isPage = true,
            pageTitle = "Page Title",
            pageTemplate = "template-slug"
        )

        // Act
        val result = processor.processIntent(intent, extras, siteModel, isRestarting = false)

        // Assert
        assertThat(result).isInstanceOf(EditorIntentProcessor.IntentProcessResult.PageFromLayout::class.java)
        val pageResult = result as EditorIntentProcessor.IntentProcessResult.PageFromLayout
        assertThat(pageResult.title).isEqualTo("Page Title")
        assertThat(pageResult.content).isEqualTo("Layout content")
    }
    // endregion

    // region processIntent - InvalidIntent tests
    @Test
    fun `processIntent returns InvalidIntent when extras is null`() {
        // Arrange
        val intent = mock<Intent>()

        // Act
        val result = processor.processIntent(intent, null, siteModel, isRestarting = false)

        // Assert
        assertThat(result).isEqualTo(EditorIntentProcessor.IntentProcessResult.InvalidIntent)
    }
    // endregion

    // region getSiteForQuickPress tests
    @Test
    fun `getSiteForQuickPress returns null when extras is null`() {
        // Arrange
        val intent = mock<Intent>()

        // Act
        val result = processor.getSiteForQuickPress(intent, null)

        // Assert
        assertThat(result).isNull()
    }

    @Test
    fun `getSiteForQuickPress returns null when EXTRA_POST_LOCAL_ID is present`() {
        // Arrange
        val intent = mock<Intent>()
        val extras = createMockBundle(localPostId = 123)

        // Act
        val result = processor.getSiteForQuickPress(intent, extras)

        // Assert
        assertThat(result).isNull()
    }

    @Test
    fun `getSiteForQuickPress returns site when quickpress blog id is present`() {
        // Arrange
        val intent = mock<Intent>()
        val expectedSite = mock<SiteModel>()
        whenever(intent.action).thenReturn(Intent.ACTION_SEND)
        whenever(intent.getIntExtra(eq(EditorConstants.EXTRA_QUICKPRESS_BLOG_ID), eq(-1))).thenReturn(456)
        whenever(siteStore.getSiteByLocalId(456)).thenReturn(expectedSite)
        val extras = createMockBundle(quickPressBlogId = 456)

        // Act
        val result = processor.getSiteForQuickPress(intent, extras)

        // Assert
        assertThat(result).isEqualTo(expectedSite)
    }

    @Test
    fun `getSiteForQuickPress returns site when is quickpress flag is set`() {
        // Arrange
        val intent = mock<Intent>()
        val expectedSite = mock<SiteModel>()
        whenever(intent.getIntExtra(eq(EditorConstants.EXTRA_QUICKPRESS_BLOG_ID), eq(-1))).thenReturn(456)
        whenever(siteStore.getSiteByLocalId(456)).thenReturn(expectedSite)
        val extras = createMockBundle(isQuickPress = true, quickPressBlogId = 456)

        // Act
        val result = processor.getSiteForQuickPress(intent, extras)

        // Assert
        assertThat(result).isEqualTo(expectedSite)
    }
    // endregion

    // region extractMediaUrisFromShareIntent tests
    @Test
    fun `extractMediaUrisFromShareIntent returns empty list when no EXTRA_STREAM`() {
        // Arrange
        val intent = mock<Intent>()
        whenever(intent.hasExtra(Intent.EXTRA_STREAM)).thenReturn(false)

        // Act
        val result = processor.extractMediaUrisFromShareIntent(intent)

        // Assert
        assertThat(result).isEmpty()
    }
    // endregion

    // region Helper methods
    /**
     * Creates a mocked Bundle with the specified values.
     * This is necessary because Android's Bundle class doesn't work in unit tests.
     */
    private fun createMockBundle(
        localPostId: Int? = null,
        isPage: Boolean = false,
        isQuickPress: Boolean = false,
        quickPressBlogId: Int? = null,
        loadAutoSaveRevision: Boolean = false,
        pageTitle: String? = null,
        pageTemplate: String? = null
    ): Bundle {
        val bundle = mock<Bundle>()

        // Configure containsKey
        whenever(bundle.containsKey(EditorConstants.EXTRA_POST_LOCAL_ID)).thenReturn(localPostId != null)
        whenever(bundle.containsKey(EditorConstants.EXTRA_IS_QUICKPRESS)).thenReturn(isQuickPress)
        whenever(bundle.containsKey(EditorConstants.EXTRA_QUICKPRESS_BLOG_ID)).thenReturn(quickPressBlogId != null)

        // Configure getInt
        whenever(bundle.getInt(EditorConstants.EXTRA_POST_LOCAL_ID)).thenReturn(localPostId ?: 0)

        // Configure getBoolean
        whenever(bundle.getBoolean(EditorConstants.EXTRA_IS_PAGE)).thenReturn(isPage)
        whenever(bundle.getBoolean(EditorConstants.EXTRA_LOAD_AUTO_SAVE_REVISION)).thenReturn(loadAutoSaveRevision)

        // Configure getString
        whenever(bundle.getString(EditorConstants.EXTRA_PAGE_TITLE)).thenReturn(pageTitle)
        whenever(bundle.getString(EditorConstants.EXTRA_PAGE_TEMPLATE)).thenReturn(pageTemplate)

        return bundle
    }
    // endregion
}
