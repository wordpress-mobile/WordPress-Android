package org.wordpress.android.ui.reader.subfilter

import com.google.gson.JsonParser
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.core.StringContains
import org.json.JSONObject
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.datasets.ReaderBlogTableWrapper
import org.wordpress.android.models.ReaderBlog
import org.wordpress.android.models.ReaderTag
import org.wordpress.android.models.ReaderTagType.FOLLOWED
import org.wordpress.android.ui.reader.subfilter.SubfilterListItem.Site
import org.wordpress.android.ui.reader.subfilter.SubfilterListItem.SiteAll
import org.wordpress.android.ui.reader.subfilter.SubfilterListItem.Tag
import org.wordpress.android.ui.reader.utils.ReaderUtilsWrapper
import java.lang.IllegalArgumentException

@RunWith(MockitoJUnitRunner::class)
class SubfilterListItemMapperTest {
    @Mock lateinit var readerUtilsWrapper: ReaderUtilsWrapper
    @Mock lateinit var readerBlogTableWrapper: ReaderBlogTableWrapper

    @Rule @JvmField var thrown: ExpectedException = ExpectedException.none()

    private lateinit var listItemMapper: SubfilterListItemMapper
    private val jsonTester = JsonParser()

    val blog = ReaderBlog.fromJson(JSONObject(SITE_JSON_WITH_BLOGID))
    val feed = ReaderBlog.fromJson(JSONObject(SITE_JSON_WITH_FEEDID))

    private val tag = ReaderTag(
            "news",
            "",
            "",
            "",
            FOLLOWED
    )

    @Before
    fun setUp() {
        listItemMapper = SubfilterListItemMapper(readerUtilsWrapper, readerBlogTableWrapper)

        whenever(readerBlogTableWrapper.getBlogInfo(any())).thenReturn(blog)
        whenever(readerBlogTableWrapper.getFeedInfo(any())).thenReturn(feed)
        whenever(readerUtilsWrapper.getTagFromTagName(any(), any())).thenReturn(tag)
    }

    @Test
    fun `fromJson returns SiteAll on empty json`() {
        // Given
        val json = ""

        // When
        val item = listItemMapper.fromJson(
                json = json,
                onClickAction = ::onClickActionDummy,
                isSelected = false
        )

        // Then
        assertThat(item is SiteAll).isTrue()
    }

    @Test
    fun `fromJson returns exception on unknown type`() {
        // Given
        val json = WRONG_TYPE_JSON
        thrown.expect(IllegalArgumentException::class.java)
        thrown.expectMessage(StringContains("fromJson > Unexpected Subfilter type"))

        // When
        listItemMapper.fromJson(
                json = json,
                onClickAction = ::onClickActionDummy,
                isSelected = false
        )

        // Then
        // Should have got an exception
    }

    @Test
    fun `fromJson returns SiteAll when no valid blogId and feedId`() {
        // Given
        val json = SITE_JSON

        // When
        val item = listItemMapper.fromJson(
                json = json,
                onClickAction = ::onClickActionDummy,
                isSelected = false
        )

        // Then
        assertThat(item is SiteAll).isTrue()
    }

    @Test
    fun `fromJson returns blog when valid blogId`() {
        // Given
        val json = SITE_JSON_WITH_BLOGID

        // When
        val item = listItemMapper.fromJson(
                json = json,
                onClickAction = ::onClickActionDummy,
                isSelected = false
        )

        // Then
        assertThat(item is Site && item.blog == blog).isTrue()
    }

    @Test
    fun `fromJson returns feed when valid feedId`() {
        // Given
        val json = SITE_JSON_WITH_FEEDID

        // When
        val item = listItemMapper.fromJson(
                json = json,
                onClickAction = ::onClickActionDummy,
                isSelected = false
        )

        // Then
        assertThat(item is Site && item.blog == feed).isTrue()
    }

    @Test
    fun `fromJson returns tag if tagSlug not empty`() {
        // Given
        val json = TAG_JSON

        // When
        val item = listItemMapper.fromJson(
                json = json,
                onClickAction = ::onClickActionDummy,
                isSelected = false
        )

        // Then
        assertThat(item is Tag && item.tag == tag).isTrue()
    }

    @Test
    fun `fromJson returns SiteAll if tagSlug is empty`() {
        // Given
        val json = TAG_JSON_EMPTY_SLUG

        // When
        val item = listItemMapper.fromJson(
                json = json,
                onClickAction = ::onClickActionDummy,
                isSelected = false
        )

        // Then
        assertThat(item is SiteAll).isTrue()
    }

    @Test
    fun `toJson returns correct SiteAll JSON`() {
        // Given
        val item = SiteAll(onClickAction = ::onClickActionDummy)

        // When
        val json = listItemMapper.toJson(item)

        // Then
        val got = jsonTester.parse(json)
        val exp = jsonTester.parse(SITE_ALL_JSON)

        assertThat(got).isEqualTo(exp)
    }

    @Test
    fun `toJson returns correct Site JSON`() {
        // Given
        val blog = ReaderBlog.fromJson(JSONObject(SITE_JSON))
        val item = Site(
                onClickAction = ::onClickActionDummy,
                blog = blog
        )

        // When
        val json = listItemMapper.toJson(item)

        // Then
        val got = jsonTester.parse(json)
        val exp = jsonTester.parse(SITE_JSON)

        assertThat(got).isEqualTo(exp)
    }

    @Test
    fun `toJson returns correct Tag JSON`() {
        // Given
        val item = Tag(
                onClickAction = ::onClickActionDummy,
                tag = tag
        )

        // When
        val json = listItemMapper.toJson(item)

        // Then
        val got = jsonTester.parse(json)
        val exp = jsonTester.parse(TAG_JSON)

        assertThat(got).isEqualTo(exp)
    }

    private companion object Fixtures {
        private const val SITE_ALL_JSON = "{\"blogId\":0,\"feedId\":0,\"tagSlug\":\"\",\"tagType\":0,\"type\":1}"
        private const val SITE_JSON = "{\"blogId\":0,\"feedId\":0,\"tagSlug\":\"\",\"tagType\":0,\"type\":2}"
        private const val SITE_JSON_WITH_BLOGID =
                "{\"blogId\":1234,\"feedId\":0,\"tagSlug\":\"\",\"tagType\":0,\"type\":2}"
        private const val SITE_JSON_WITH_FEEDID =
                "{\"blogId\":0,\"feedId\":1234,\"tagSlug\":\"\",\"tagType\":0,\"type\":2}"
        private const val TAG_JSON = "{\"blogId\":0,\"feedId\":0,\"tagSlug\":\"news\",\"tagType\":1,\"type\":4}"
        private const val TAG_JSON_EMPTY_SLUG = "{\"blogId\":0,\"feedId\":0,\"tagSlug\":\"\",\"tagType\":1,\"type\":4}"
        private const val WRONG_TYPE_JSON = "{\"blogId\":0,\"feedId\":0,\"tagSlug\":\"news\",\"tagType\":1,\"type\":10}"

        private fun onClickActionDummy(filter: SubfilterListItem) {}
    }
}
