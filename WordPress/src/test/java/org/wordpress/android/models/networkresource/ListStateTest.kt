package org.wordpress.android.models.networkresource

import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.equalTo
import org.junit.Assert.assertThat
import org.junit.Test

class ListStateTest {
    @Test
    fun testInitState() {
        val initState: ListState<String> = ListState.Init()

        assertThat(initState.data, `is`(emptyList()))

        assertThat(initState.isFetchingFirstPage(), `is`(false))
        assertThat(initState.isLoadingMore(), `is`(false))
        assertThat(initState.shouldFetch(true), `is`(false))
        assertThat(initState.shouldFetch(false), `is`(false))
    }

    @Test
    fun testReadyState() {
        val testData = listOf("item1", "item2")
        val readyState: ListState<String> = ListState.Ready(testData)

        assertThat(readyState.data, `is`(equalTo(testData)))

        assertThat(readyState.isFetchingFirstPage(), `is`(false))
        assertThat(readyState.isLoadingMore(), `is`(false))
        assertThat(readyState.shouldFetch(true), `is`(false))
        assertThat(readyState.shouldFetch(false), `is`(true))
    }

    @Test
    fun testLoadingFirstPageState() {
        val testData = listOf("item3", "item4")
        val readyState: ListState<String> = ListState.Ready(testData)
        val loadingState: ListState<String> = ListState.Loading(readyState)

        assertThat(loadingState.data, `is`(equalTo(testData)))

        assertThat(loadingState.isFetchingFirstPage(), `is`(true))
        assertThat(loadingState.isLoadingMore(), `is`(false))
        assertThat(loadingState.shouldFetch(true), `is`(false))
        assertThat(loadingState.shouldFetch(false), `is`(false))
    }

    @Test
    fun testLoadMoreState() {
        val testData = listOf("item5", "item6")
        val readyState: ListState<String> = ListState.Ready(testData)
        val loadingState: ListState<String> = ListState.Loading(readyState, true)

        assertThat(loadingState.data, `is`(equalTo(testData)))

        assertThat(loadingState.isFetchingFirstPage(), `is`(false))
        assertThat(loadingState.isLoadingMore(), `is`(true))
        assertThat(loadingState.shouldFetch(true), `is`(false))
        assertThat(loadingState.shouldFetch(false), `is`(false))
    }

    @Test
    fun testSuccessStateWhereAllDataIsLoaded() {
        val testData = listOf("item7")

        val successState = ListState.Success(testData)
        assertThat(successState.data, `is`(equalTo(testData)))
        assertThat(successState.canLoadMore, `is`(false))
    }

    @Test
    fun testSuccessStatesWhereMoreDataCanBeLoaded() {
        val testData = listOf("item8")

        val successState2 = ListState.Success(testData, true)
        assertThat(successState2.data, `is`(equalTo(testData)))
        assertThat(successState2.canLoadMore, `is`(true))
    }

    @Test
    fun testErrorState() {
        val testDataReady = listOf("item9", "item10")
        val readyState: ListState<String> = ListState.Ready(testDataReady)
        val loadingState: ListState<String> = ListState.Loading(readyState, true)

        val errorMessage = "Some error message"
        val errorState = ListState.Error(loadingState, errorMessage)
        assertThat(errorState.errorMessage, `is`(equalTo(errorMessage)))
        assertThat(errorState.data, `is`(testDataReady))
    }

    @Test
    fun testGetTransformedByUpperCaseListState() {
        val testData = listOf("item11", "item12", "item13")
        val readyState: ListState<String> = ListState.Ready(testData)
        val toUpperCase: (List<String>) -> List<String> = { list ->
            list.map { it.toUpperCase() }
        }
        val transformedReadyState = readyState.transform(toUpperCase)
        assertThat(transformedReadyState.data, `is`(equalTo(toUpperCase(testData))))
        assertThat(transformedReadyState.data.size, `is`(3))
        assertThat(transformedReadyState is ListState.Ready, `is`(true))
    }

    @Test
    fun testGetTransformedByFilterListState() {
        val testData = listOf("item14", "item15", "not-item")
        val readyState: ListState<String> = ListState.Ready(testData)
        val loadingState: ListState<String> = ListState.Loading(readyState, true)
        val filterNotItem: (List<String>) -> List<String> = { list ->
            list.filter { it != "not-item" }
        }
        val transformedLoadingState = loadingState.transform(filterNotItem)
        assertThat(transformedLoadingState.data, `is`(equalTo(filterNotItem(testData))))
        assertThat(transformedLoadingState.data.size, `is`(2))
        assertThat(transformedLoadingState is ListState.Loading, `is`(true))
    }
}
