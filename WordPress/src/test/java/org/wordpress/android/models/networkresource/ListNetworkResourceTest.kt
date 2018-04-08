package org.wordpress.android.models.networkresource

import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.equalTo
import org.junit.Assert.assertThat
import org.junit.Test

class ListNetworkResourceTest {
    @Test
    fun testInitState() {
        val initState: ListNetworkResource<String> = ListNetworkResource.Init()

        assertThat(initState.data, `is`(emptyList()))

        assertThat(initState.isFetchingFirstPage(), `is`(false))
        assertThat(initState.isLoadingMore(), `is`(false))
        assertThat(initState.shouldFetch(true), `is`(false))
        assertThat(initState.shouldFetch(false), `is`(false))
    }

    @Test
    fun testReadyState() {
        val testData = listOf("item1", "item2")
        val readyState: ListNetworkResource<String> = ListNetworkResource.Ready(testData)

        assertThat(readyState.data, `is`(equalTo(testData)))

        assertThat(readyState.isFetchingFirstPage(), `is`(false))
        assertThat(readyState.isLoadingMore(), `is`(false))
        assertThat(readyState.shouldFetch(true), `is`(false))
        assertThat(readyState.shouldFetch(false), `is`(true))
    }

    @Test
    fun testLoadingFirstPageState() {
        val testDataReady = listOf("item3", "item4")
        val readyState: ListNetworkResource<String> = ListNetworkResource.Ready(testDataReady)
        val loadingState: ListNetworkResource<String> = ListNetworkResource.Loading(readyState)

        assertThat(loadingState.data, `is`(equalTo(testDataReady)))

        assertThat(loadingState.isFetchingFirstPage(), `is`(true))
        assertThat(loadingState.isLoadingMore(), `is`(false))
        assertThat(loadingState.shouldFetch(true), `is`(false))
        assertThat(loadingState.shouldFetch(false), `is`(false))
    }

    @Test
    fun testLoadMoreState() {
        val testDataReady = listOf("item5", "item6")
        val readyState: ListNetworkResource<String> = ListNetworkResource.Ready(testDataReady)
        val loadingState: ListNetworkResource<String> = ListNetworkResource.Loading(readyState, true)

        assertThat(loadingState.data, `is`(equalTo(testDataReady)))

        assertThat(loadingState.isFetchingFirstPage(), `is`(false))
        assertThat(loadingState.isLoadingMore(), `is`(true))
        assertThat(loadingState.shouldFetch(true), `is`(false))
        assertThat(loadingState.shouldFetch(false), `is`(false))
    }

    @Test
    fun testSuccessState() {
        val testDataSuccess = listOf("item 7")

        val successState = ListNetworkResource.Success(testDataSuccess)
        assertThat(successState.data, `is`(equalTo(testDataSuccess)))
        assertThat(successState.canLoadMore, `is`(false))

        val successState2 = ListNetworkResource.Success(testDataSuccess, true)
        assertThat(successState2.data, `is`(equalTo(testDataSuccess)))
        assertThat(successState2.canLoadMore, `is`(true))
    }

    @Test
    fun testErrorState() {
        val testDataReady = listOf("item8", "item9")
        val readyState: ListNetworkResource<String> = ListNetworkResource.Ready(testDataReady)
        val loadingState: ListNetworkResource<String> = ListNetworkResource.Loading(readyState, true)

        val errorMessage = "Some error message"
        val errorState = loadingState.error(errorMessage)
        assertThat(errorState.errorMessage, `is`(equalTo(errorMessage)))
        assertThat(errorState.data, `is`(testDataReady))
    }

    @Test
    fun testGetTransformedListNetworkResource() {
        val testDataReady = listOf("item10", "item11", "not-item")
        val readyState: ListNetworkResource<String> = ListNetworkResource.Ready(testDataReady)
        val toUpperCase: (List<String>) -> List<String> = { list ->
            list.map { it.toUpperCase() }
        }
        val newReadyState = readyState.getTransformedListNetworkResource(toUpperCase)
        assertThat(newReadyState.data, `is`(equalTo(toUpperCase(testDataReady))))
        assertThat(newReadyState.data.size, `is`(3))
        assertThat(newReadyState is ListNetworkResource.Ready, `is`(true))

        val filterNotItem: (List<String>) -> List<String> = { list ->
            list.filter { it != "not-item".toUpperCase() }
        }
        val loadingState: ListNetworkResource<String> = newReadyState.loading(true)
        val newLoadingState = loadingState.getTransformedListNetworkResource(filterNotItem)
        assertThat(newLoadingState.data, `is`(equalTo(filterNotItem(loadingState.data))))
        assertThat(newLoadingState.data.size, `is`(2))
        assertThat(newLoadingState is ListNetworkResource.Loading, `is`(true))
    }
}
