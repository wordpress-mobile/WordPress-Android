package org.wordpress.android.ui.reader

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.WordPress
import org.wordpress.android.ui.reader.services.update.TagUpdateClientUtilsProvider

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class TagUpdateClientUtilsProviderTest : BaseUnitTest() {
    private lateinit var clientProvider: TagUpdateClientUtilsProvider

    @Before
    fun setUp() {
        clientProvider = TagUpdateClientUtilsProvider()
    }

    @Test
    fun `getRestClientForTagUpdate return the expected client version`() {
        assertThat(clientProvider.getRestClientForTagUpdate()).isEqualTo(WordPress.getRestClientUtilsV1_3())
    }

    @Test
    fun `getTagUpdateEndpointURL return the expected end point URL`() {
        assertThat(clientProvider.getTagUpdateEndpointURL()).isEqualTo("https://public-api.wordpress.com/rest/v1.3/")
    }
}
