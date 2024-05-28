package org.wordpress.android.ui.reader.subfilter

import android.os.Bundle
import androidx.fragment.app.Fragment
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.mock
import org.wordpress.android.models.ReaderTag
import org.wordpress.android.ui.reader.ReaderTestUtils

// fragment implementing SubFilterViewModelProvider for testing purposes only
@Suppress("MemberVisibilityCanBePrivate")
private open class SubFilterViewModelProviderFakeFragment(
    val viewModelKeyMap: Map<String, SubFilterViewModel> = emptyMap(),
    val viewModelTagMap: Map<ReaderTag, SubFilterViewModel> = emptyMap(),
) : Fragment(), SubFilterViewModelProvider {
    override fun getSubFilterViewModelForKey(key: String): SubFilterViewModel {
        return viewModelKeyMap[key] ?: error("No SubFilterViewModel found for key: $key")
    }

    override fun getSubFilterViewModelForTag(tag: ReaderTag, savedInstanceState: Bundle?): SubFilterViewModel {
        return viewModelTagMap[tag] ?: error("No SubFilterViewModel found for tag: $tag")
    }
}

class SubFilterViewModelProviderTest {
    @Test
    fun `getSubFilterViewModelForTag should use given tag for retrieving the appropriate ViewModel`() {
        // Given
        val tag1 = ReaderTestUtils.createTag("tag1")
        val viewModel1: SubFilterViewModel = mock()

        val tag2 = ReaderTestUtils.createTag("tag2")
        val viewModel2: SubFilterViewModel = mock()

        val fragment = SubFilterViewModelProviderFakeFragment(
            viewModelTagMap = mapOf(tag1 to viewModel1, tag2 to viewModel2)
        )

        // When
        val result1 = SubFilterViewModelProvider.getSubFilterViewModelForTag(fragment, tag1)
        val result2 = SubFilterViewModelProvider.getSubFilterViewModelForTag(fragment, tag2)

        // Then
        assertThat(result1).isEqualTo(viewModel1)
        assertThat(result2).isEqualTo(viewModel2)
    }

    @Test
    fun `getSubFilterViewModelForKey should use given key for retrieving the appropriate ViewModel`() {
        // Given
        val key1 = "key1"
        val viewModel1: SubFilterViewModel = mock()

        val key2 = "key2"
        val viewModel2: SubFilterViewModel = mock()

        val fragment = SubFilterViewModelProviderFakeFragment(
            viewModelKeyMap = mapOf(key1 to viewModel1, key2 to viewModel2)
        )

        // When
        val result1 = SubFilterViewModelProvider.getSubFilterViewModelForKey(fragment, key1)
        val result2 = SubFilterViewModelProvider.getSubFilterViewModelForKey(fragment, key2)

        // Then
        assertThat(result1).isEqualTo(viewModel1)
        assertThat(result2).isEqualTo(viewModel2)
    }
}
