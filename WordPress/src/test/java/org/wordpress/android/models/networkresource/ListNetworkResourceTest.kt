package org.wordpress.android.models.networkresource

import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.nullValue
import org.junit.Assert.assertThat
import org.junit.Test

class ListNetworkResourceTest {
    @Test
    fun testInitState() {
        val initState: ListNetworkResource<String> = ListNetworkResource.Init()

        assertThat(initState.previous, `is`(nullValue()))
        assertThat(initState.data, `is`(emptyList()))

        assertThat(initState.isFetchingFirstPage(), `is`(false))
        assertThat(initState.isLoadingMore(), `is`(false))
        assertThat(initState.shouldFetch(true), `is`(false))
        assertThat(initState.shouldFetch(false), `is`(false))
    }

    @Test
    fun testReadyState() {
        val initState: ListNetworkResource<String> = ListNetworkResource.Init()
        val testData = listOf("item1", "item2")
        val readyState: ListNetworkResource<String> = ListNetworkResource.Ready(initState, testData)

        assertThat(readyState.data, `is`(equalTo(testData)))
        assertThat(readyState.previous, `is`(equalTo(initState)))

        assertThat(readyState.isFetchingFirstPage(), `is`(false))
        assertThat(readyState.isLoadingMore(), `is`(false))
        assertThat(readyState.shouldFetch(true), `is`(false))
        assertThat(readyState.shouldFetch(false), `is`(true))
    }

    @Test
    fun testLoadingFirstPageState() {
        val testDataReady = listOf("item3", "item4")
        val readyState: ListNetworkResource<String> = ListNetworkResource.Ready(ListNetworkResource.Init(),
                testDataReady)
        val loadingState: ListNetworkResource<String> = ListNetworkResource.Loading(readyState)

        assertThat(loadingState.data, `is`(equalTo(testDataReady)))
        assertThat(loadingState.previous, `is`(equalTo(readyState)))
        assertThat(loadingState.previous?.data, `is`(equalTo(testDataReady)))

        assertThat(loadingState.isFetchingFirstPage(), `is`(true))
        assertThat(loadingState.isLoadingMore(), `is`(false))
        assertThat(loadingState.shouldFetch(true), `is`(false))
        assertThat(loadingState.shouldFetch(false), `is`(false))
    }

    @Test
    fun testLoadMoreState() {
        val testDataReady = listOf("item5", "item6")
        val readyState: ListNetworkResource<String> = ListNetworkResource.Ready(ListNetworkResource.Init(),
                testDataReady)
        val loadingState: ListNetworkResource<String> = ListNetworkResource.Loading(readyState, true)

        assertThat(loadingState.data, `is`(equalTo(testDataReady)))
        assertThat(loadingState.previous, `is`(equalTo(readyState)))

        assertThat(loadingState.isFetchingFirstPage(), `is`(false))
        assertThat(loadingState.isLoadingMore(), `is`(true))
        assertThat(loadingState.shouldFetch(true), `is`(false))
        assertThat(loadingState.shouldFetch(false), `is`(false))
    }

    @Test
    fun testSuccessState() {
        val testDataReady = listOf("item5", "item6")
        val readyState: ListNetworkResource<String> = ListNetworkResource.Ready(ListNetworkResource.Init(),
                testDataReady)
        val loadingState: ListNetworkResource<String> = ListNetworkResource.Loading(readyState)

        val testDataSuccess = listOf("item 7")

        val successState = loadingState.success(testDataSuccess)
        assertThat(successState.previous, `is`(equalTo(loadingState)))
        assertThat(successState.previous?.data, `is`(equalTo(testDataReady)))
        assertThat(successState.data, `is`(equalTo(testDataSuccess)))

        assertThat(successState.previous?.isLoadingMore(), `is`(false))
        assertThat(successState.previous?.isFetchingFirstPage(), `is`(true))
    }

    @Test
    fun testErrorState() {
        val testDataReady = listOf("item8", "item9")
        val readyState: ListNetworkResource<String> = ListNetworkResource.Ready(ListNetworkResource.Init(),
                testDataReady)
        val loadingState: ListNetworkResource<String> = ListNetworkResource.Loading(readyState, true)

        val errorMessage = "Some error message"
        val errorState = loadingState.error(errorMessage)
        assertThat(errorState.previous, `is`(equalTo(loadingState)))
        assertThat(errorState.previous?.data, `is`(equalTo(testDataReady)))
        assertThat(errorState.data, `is`(equalTo(errorState.previous?.data)))
        assertThat(errorState.errorMessage, `is`(equalTo(errorMessage)))

        assertThat(errorState.previous?.isLoadingMore(), `is`(true))
        assertThat(errorState.previous?.isFetchingFirstPage(), `is`(false))
    }

    @Test
    fun testGetTransformedListNetworkResource() {
        val testDataReady = listOf("item10", "item11", "not-item")
        val readyState: ListNetworkResource<String> = ListNetworkResource.Ready(ListNetworkResource.Init(),
                testDataReady)
        val toUpperCase: (List<String>) -> List<String> = { list ->
            list.map { it.toUpperCase() }
        }
        val newReadyState = readyState.getTransformedListNetworkResource(toUpperCase)
        assertThat(newReadyState.data, `is`(equalTo(toUpperCase(testDataReady))))
        assertThat(newReadyState.data.size, `is`(3))
        assertThat(newReadyState.previous, `is`(equalTo(readyState)))
        assertThat(newReadyState is ListNetworkResource.Ready, `is`(true))

        val filterNotItem: (List<String>) -> List<String> = { list ->
            list.filter { it != "not-item".toUpperCase() }
        }
        val loadingState: ListNetworkResource<String> = newReadyState.loading(true)
        val newLoadingState = loadingState.getTransformedListNetworkResource(filterNotItem)
        assertThat(newLoadingState.data, `is`(equalTo(filterNotItem(loadingState.data))))
        assertThat(newLoadingState.data.size, `is`(2))
        assertThat(newLoadingState.previous, `is`(equalTo(loadingState)))
        assertThat(newLoadingState is ListNetworkResource.Loading, `is`(true))
    }
}
