package org.wordpress.android.ui.newstats.repository

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.ui.newstats.datasource.StatsDataSource
import org.wordpress.android.ui.newstats.datasource.StatsErrorType
import org.wordpress.android.ui.newstats.datasource.StatsTagsData
import org.wordpress.android.ui.newstats.datasource.StatsTagsDataResult
import org.wordpress.android.ui.newstats.datasource.TagData
import org.wordpress.android.ui.newstats.datasource.TagGroupData

@ExperimentalCoroutinesApi
class StatsRepositoryTagsTest : BaseUnitTest() {
    @Mock
    private lateinit var statsDataSource: StatsDataSource

    @Mock
    private lateinit var appLogWrapper: AppLogWrapper

    private lateinit var repository: StatsRepository

    @Before
    fun setUp() {
        repository = StatsRepository(
            statsDataSource = statsDataSource,
            appLogWrapper = appLogWrapper,
            ioDispatcher = testDispatcher()
        )
    }

    @Test
    fun `given success, when fetchTags, then success result`() =
        test {
            whenever(
                statsDataSource.fetchStatsTags(
                    any(), any()
                )
            ).thenReturn(
                StatsTagsDataResult.Success(
                    createTestTagsData()
                )
            )

            val result = repository.fetchTags(
                TEST_SITE_ID
            )

            assertThat(result)
                .isInstanceOf(
                    TagsResult.Success::class.java
                )
            val success = result as TagsResult.Success
            assertThat(success.data.tagGroups)
                .hasSize(2)
        }

    @Test
    fun `given success, when fetchTags, then data is mapped correctly`() =
        test {
            whenever(
                statsDataSource.fetchStatsTags(
                    any(), any()
                )
            ).thenReturn(
                StatsTagsDataResult.Success(
                    createTestTagsData()
                )
            )

            val result = repository.fetchTags(
                TEST_SITE_ID
            )

            val success = result as TagsResult.Success
            val firstGroup = success.data.tagGroups[0]
            assertThat(firstGroup.views)
                .isEqualTo(TEST_CATEGORY_VIEWS)
            assertThat(firstGroup.tags).hasSize(1)
            assertThat(firstGroup.tags[0].name)
                .isEqualTo(TEST_CATEGORY_NAME)
            assertThat(firstGroup.tags[0].tagType)
                .isEqualTo("category")
        }

    @Test
    fun `given error, when fetchTags, then error result`() =
        test {
            whenever(
                statsDataSource.fetchStatsTags(
                    any(), any()
                )
            ).thenReturn(
                StatsTagsDataResult.Error(
                    StatsErrorType.NETWORK_ERROR
                )
            )

            val result = repository.fetchTags(
                TEST_SITE_ID
            )

            assertThat(result)
                .isInstanceOf(
                    TagsResult.Error::class.java
                )
        }

    @Test
    fun `given auth error, when fetchTags, then error message contains type`() =
        test {
            whenever(
                statsDataSource.fetchStatsTags(
                    any(), any()
                )
            ).thenReturn(
                StatsTagsDataResult.Error(
                    StatsErrorType.AUTH_ERROR
                )
            )

            val result = repository.fetchTags(
                TEST_SITE_ID
            )

            val error = result as TagsResult.Error
            assertThat(error.message)
                .isEqualTo("AUTH_ERROR")
        }

    @Test
    fun `given api error, when fetchTags, then error message contains type`() =
        test {
            whenever(
                statsDataSource.fetchStatsTags(
                    any(), any()
                )
            ).thenReturn(
                StatsTagsDataResult.Error(
                    StatsErrorType.API_ERROR
                )
            )

            val result = repository.fetchTags(
                TEST_SITE_ID
            )

            val error = result as TagsResult.Error
            assertThat(error.message)
                .isEqualTo("API_ERROR")
        }

    @Test
    fun `when fetchTags, then correct siteId is passed`() =
        test {
            whenever(
                statsDataSource.fetchStatsTags(
                    any(), any()
                )
            ).thenReturn(
                StatsTagsDataResult.Success(
                    createTestTagsData()
                )
            )

            repository.fetchTags(TEST_SITE_ID)

            verify(statsDataSource).fetchStatsTags(
                siteId = eq(TEST_SITE_ID),
                max = any()
            )
        }

    @Test
    fun `given empty tag groups, when fetchTags, then success with empty list`() =
        test {
            whenever(
                statsDataSource.fetchStatsTags(
                    any(), any()
                )
            ).thenReturn(
                StatsTagsDataResult.Success(
                    StatsTagsData(
                        tagGroups = emptyList()
                    )
                )
            )

            val result = repository.fetchTags(
                TEST_SITE_ID
            )

            val success = result as TagsResult.Success
            assertThat(success.data.tagGroups).isEmpty()
        }

    @Test
    fun `given multi-tag group, when fetchTags, then all tags preserved`() =
        test {
            val multiTagData = StatsTagsData(
                tagGroups = listOf(
                    TagGroupData(
                        tags = listOf(
                            TagData(
                                tagType = "tag",
                                name = "Alpha"
                            ),
                            TagData(
                                tagType = "category",
                                name = "Beta"
                            )
                        ),
                        views = 50
                    )
                )
            )
            whenever(
                statsDataSource.fetchStatsTags(
                    any(), any()
                )
            ).thenReturn(
                StatsTagsDataResult.Success(
                    multiTagData
                )
            )

            val result = repository.fetchTags(
                TEST_SITE_ID
            )

            val success = result as TagsResult.Success
            val group = success.data.tagGroups[0]
            assertThat(group.tags).hasSize(2)
            assertThat(group.tags[0].name)
                .isEqualTo("Alpha")
            assertThat(group.tags[1].name)
                .isEqualTo("Beta")
        }

    private fun createTestTagsData() = StatsTagsData(
        tagGroups = listOf(
            TagGroupData(
                tags = listOf(
                    TagData(
                        tagType = "category",
                        name = TEST_CATEGORY_NAME
                    )
                ),
                views = TEST_CATEGORY_VIEWS
            ),
            TagGroupData(
                tags = listOf(
                    TagData(
                        tagType = "tag",
                        name = TEST_TAG_NAME
                    )
                ),
                views = TEST_TAG_VIEWS
            )
        )
    )

    companion object {
        private const val TEST_SITE_ID = 123L
        private const val TEST_CATEGORY_NAME =
            "Uncategorized"
        private const val TEST_CATEGORY_VIEWS = 83L
        private const val TEST_TAG_NAME = "snaps"
        private const val TEST_TAG_VIEWS = 15L
    }
}
