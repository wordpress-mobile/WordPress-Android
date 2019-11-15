package org.wordpress.android.ui.reader.utils

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.models.ReaderTag
import org.wordpress.android.models.ReaderTagList
import org.wordpress.android.models.ReaderTagType.BOOKMARKED
import org.wordpress.android.models.ReaderTagType.DEFAULT
import org.wordpress.android.models.ReaderTagType.FOLLOWED

@RunWith(MockitoJUnitRunner::class)
class ReaderUtilsTest {
    private fun getShuffledTagList(): ReaderTagList {
        val tagList = ReaderTagList()
        tagList.addAll(listOf(
                ReaderTag("", "", "", ReaderTag.LIKED_PATH, DEFAULT),
                ReaderTag("", "", "", "https://genericendpoint2.com", FOLLOWED),
                ReaderTag("", "", "", ReaderTag.DISCOVER_PATH, DEFAULT),
                ReaderTag("", "", "", "https://genericendpoint4.com", DEFAULT),
                ReaderTag("", "", "", "", BOOKMARKED),
                ReaderTag("", "", "", ReaderTag.FOLLOWING_PATH, DEFAULT),
                ReaderTag("", "", "", "https://genericendpoint7.com", DEFAULT)
        ))

        return tagList
    }

    private fun getExpectedTagList(): ReaderTagList {
        val tagList = ReaderTagList()
        tagList.addAll(listOf(
                ReaderTag("", "", "", ReaderTag.FOLLOWING_PATH, DEFAULT),
                ReaderTag("", "", "", ReaderTag.DISCOVER_PATH, DEFAULT),
                ReaderTag("", "", "", ReaderTag.LIKED_PATH, DEFAULT),
                ReaderTag("", "", "", "", BOOKMARKED),
                ReaderTag("", "", "", "https://genericendpoint2.com", FOLLOWED),
                ReaderTag("", "", "", "https://genericendpoint4.com", DEFAULT),
                ReaderTag("", "", "", "https://genericendpoint7.com", DEFAULT)
        ))

        return tagList
    }

    @Test
    fun `getOrderedTagsList return the desired ordered list`() {
        val shuffledList = getShuffledTagList()
        val orederList = ReaderUtils.getOrderedTagsList(shuffledList, ReaderUtils.getDefaultTagInfo())
        assertThat(orederList).isEqualTo(getExpectedTagList())
    }
}
