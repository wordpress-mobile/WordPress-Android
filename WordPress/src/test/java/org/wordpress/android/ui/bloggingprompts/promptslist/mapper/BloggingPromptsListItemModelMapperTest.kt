package org.wordpress.android.ui.bloggingprompts.promptslist.mapper

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.ui.bloggingprompts.promptslist.BloggingPromptsListFixtures
import org.wordpress.android.util.LocaleManagerWrapper
import java.util.Locale

@ExperimentalCoroutinesApi
class BloggingPromptsListItemModelMapperTest : BaseUnitTest() {
    @Mock lateinit var localeManagerWrapper: LocaleManagerWrapper
    lateinit var mapper: BloggingPromptsListItemModelMapper

    @Before
    fun setUp() {
        whenever(localeManagerWrapper.getLocale()).thenReturn(Locale.US)
        mapper = BloggingPromptsListItemModelMapper(localeManagerWrapper)
    }

    @Test
    fun `when toUiModel is called, then maps to UI model correctly`() {
        val result = mapper.toUiModel(BloggingPromptsListFixtures.DOMAIN_MODEL)

        assertThat(result).isEqualTo(BloggingPromptsListFixtures.UI_MODEL)
    }
}
