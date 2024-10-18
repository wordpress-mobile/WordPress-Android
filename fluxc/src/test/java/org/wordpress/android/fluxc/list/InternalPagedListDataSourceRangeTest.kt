package org.wordpress.android.fluxc.list

import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.list.datasource.InternalPagedListDataSource
import org.wordpress.android.fluxc.model.list.datasource.ListItemDataSourceInterface
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

private const val IS_LIST_FULLY_FETCHED = false
private val MOCKED_GET_ITEM_IDENTIFIERS_RESULT = listOf(1L, 3L, 5L)

internal data class InternalPagedListDataSourceRangeTestCase(
    val startPosition: Int,
    val endPosition: Int,
    val isValid: Boolean,
    val message: String? = null
)

@RunWith(Parameterized::class)
internal class InternalPagedListDataSourceRangeTest(
    private val testCase: InternalPagedListDataSourceRangeTestCase
) {
    companion object {
        @JvmStatic
        @Parameters
        fun testCases(): List<InternalPagedListDataSourceRangeTestCase> =
                listOf(
                        InternalPagedListDataSourceRangeTestCase(
                                startPosition = 0,
                                endPosition = 1,
                                isValid = true
                        ),
                        InternalPagedListDataSourceRangeTestCase(
                                startPosition = 0,
                                endPosition = 0,
                                isValid = false,
                                message = "End position can't be 0"
                        ),
                        InternalPagedListDataSourceRangeTestCase(
                                startPosition = -1,
                                endPosition = 0,
                                isValid = false,
                                message = "Start position can't be less than 0"
                        ),
                        InternalPagedListDataSourceRangeTestCase(
                                startPosition = 1,
                                endPosition = 0,
                                isValid = false,
                                message = "Start position can't be more than end position"
                        ),
                        InternalPagedListDataSourceRangeTestCase(
                                startPosition = 1,
                                endPosition = MOCKED_GET_ITEM_IDENTIFIERS_RESULT.size + 1,
                                isValid = false,
                                message = "End position can't be more than the total size"
                        )
                )
    }

    @Test
    fun `test range`() {
        val getItemsInRange = {
            createDataSource().getItemsInRange(testCase.startPosition, testCase.endPosition)
        }
        if (testCase.isValid) {
            val items = getItemsInRange()
            assertNotNull(items, testCase.message)
        } else {
            assertFailsWith(IllegalArgumentException::class, testCase.message) {
                getItemsInRange()
            }
        }
    }

    private fun createDataSource(): InternalPagedListDataSource<TestListDescriptor, Long, TestPagedListResultType> {
        val itemDataSource = mock<ListItemDataSourceInterface<TestListDescriptor, Long, TestPagedListResultType>>()
        whenever(itemDataSource.getItemIdentifiers(any(), any(), eq(IS_LIST_FULLY_FETCHED))).thenReturn(
                MOCKED_GET_ITEM_IDENTIFIERS_RESULT
        )
        return InternalPagedListDataSource(
                listDescriptor = mock(),
                remoteItemIds = mock(),
                isListFullyFetched = IS_LIST_FULLY_FETCHED,
                itemDataSource = itemDataSource
        )
    }
}
