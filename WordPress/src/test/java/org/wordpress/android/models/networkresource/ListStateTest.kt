package org.wordpress.android.models.networkresource

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class ListStateTest {
    @Test
    fun testInitState() {
        val initState: ListState<String> = ListState.Init()

        assertThat(initState.data).isEqualTo(emptyList<ListState<String>>())

        assertThat(initState.isFetchingFirstPage()).isEqualTo(false)
        assertThat(initState.isLoadingMore()).isEqualTo(false)
        assertThat(initState.shouldFetch(true)).isEqualTo(false)
        assertThat(initState.shouldFetch(false)).isEqualTo(false)
    }

    @Test
    fun testReadyState() {
        val testData = listOf("item1", "item2")
        val readyState: ListState<String> = ListState.Ready(testData)

        assertThat(readyState.data).isEqualTo(testData)

        assertThat(readyState.isFetchingFirstPage()).isEqualTo(false)
        assertThat(readyState.isLoadingMore()).isEqualTo(false)
        assertThat(readyState.shouldFetch(true)).isEqualTo(false)
        assertThat(readyState.shouldFetch(false)).isEqualTo(true)
    }

    @Test
    fun testLoadingFirstPageState() {
        val testData = listOf("item3", "item4")
        val readyState: ListState<String> = ListState.Ready(testData)
        val loadingState: ListState<String> = ListState.Loading(readyState)

        assertThat(loadingState.data).isEqualTo(testData)

        assertThat(loadingState.isFetchingFirstPage()).isEqualTo(true)
        assertThat(loadingState.isLoadingMore()).isEqualTo(false)
        assertThat(loadingState.shouldFetch(true)).isEqualTo(false)
        assertThat(loadingState.shouldFetch(false)).isEqualTo(false)
    }

    @Test
    fun testLoadMoreState() {
        val testData = listOf("item5", "item6")
        val readyState: ListState<String> = ListState.Ready(testData)
        val loadingState: ListState<String> = ListState.Loading(readyState, true)

        assertThat(loadingState.data).isEqualTo(testData)

        assertThat(loadingState.isFetchingFirstPage()).isEqualTo(false)
        assertThat(loadingState.isLoadingMore()).isEqualTo(true)
        assertThat(loadingState.shouldFetch(true)).isEqualTo(false)
        assertThat(loadingState.shouldFetch(false)).isEqualTo(false)
    }

    @Test
    fun testSuccessStateWhereAllDataIsLoaded() {
        val testData = listOf("item7")

        val successState = ListState.Success(testData)
        assertThat(successState.data).isEqualTo(testData)
        assertThat(successState.canLoadMore).isEqualTo(false)
    }

    @Test
    fun testSuccessStatesWhereMoreDataCanBeLoaded() {
        val testData = listOf("item8")

        val successState2 = ListState.Success(testData, true)
        assertThat(successState2.data).isEqualTo(testData)
        assertThat(successState2.canLoadMore).isEqualTo(true)
    }

    @Test
    fun testErrorState() {
        val testDataReady = listOf("item9", "item10")
        val readyState: ListState<String> = ListState.Ready(testDataReady)
        val loadingState: ListState<String> = ListState.Loading(readyState, true)

        val errorMessage = "Some error message"
        val errorState = ListState.Error(loadingState, errorMessage)
        assertThat(errorState.errorMessage).isEqualTo(errorMessage)
        assertThat(errorState.data).isEqualTo(testDataReady)
    }

    @Test
    fun testGetTransformedByUpperCaseListState() {
        val testData = listOf("item11", "item12", "item13")
        val readyState: ListState<String> = ListState.Ready(testData)
        val toUpperCase: (List<String>) -> List<String> = { list ->
            list.map { it.toUpperCase() }
        }
        val transformedReadyState = readyState.transform(toUpperCase)
        assertThat(transformedReadyState.data).isEqualTo(toUpperCase(testData))
        assertThat(transformedReadyState.data.size).isEqualTo(3)
        assertThat(transformedReadyState is ListState.Ready).isEqualTo(true)
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
        assertThat(transformedLoadingState.data).isEqualTo(filterNotItem(testData))
        assertThat(transformedLoadingState.data.size).isEqualTo(2)
        assertThat(transformedLoadingState is ListState.Loading).isEqualTo(true)
    }
}
