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

        // Verify the state

        assertThat(initState.previous, `is`(nullValue()))
        assertThat(initState.data, `is`(emptyList()))

        // We are not loading anything
        assertThat(initState.isFetchingFirstPage(), `is`(false))
        assertThat(initState.isLoadingMore(), `is`(false))

        // We shouldn't be able to fetch anything
        assertThat(initState.shouldFetch(true), `is`(false))
        assertThat(initState.shouldFetch(false), `is`(false))

        // State transitions

        val readyState = initState.ready(ArrayList())
        assertThat(readyState.previous, `is`(equalTo(initState)))

        val successState = initState.success(ArrayList())
        assertThat(successState.previous, `is`(equalTo(initState)))

        val loadingState = initState.loading(false)
        assertThat(loadingState.previous, `is`(equalTo(initState)))

        val errorState = initState.error("error")
        assertThat(errorState.previous, `is`(equalTo(initState)))
    }

    @Test
    fun testReadyState() {
        val initState: ListNetworkResource<String> = ListNetworkResource.Init()
        val testData = listOf("item1", "item2")
        val readyState: ListNetworkResource<String> = ListNetworkResource.Ready(initState, testData)

        // Verify the state

        assertThat(readyState.data, `is`(equalTo(testData)))
        assertThat(readyState.previous, `is`(equalTo(initState)))

        // We are not loading anything
        assertThat(readyState.isFetchingFirstPage(), `is`(false))
        assertThat(readyState.isLoadingMore(), `is`(false))

        // We can refresh the first page but we can't load more
        assertThat(readyState.shouldFetch(true), `is`(false))
        assertThat(readyState.shouldFetch(false), `is`(true))

        // State transitions

        val successState = readyState.success(ArrayList())
        assertThat(successState.previous, `is`(equalTo(readyState)))
        assertThat(successState.previous?.data, `is`(equalTo(testData)))

        val loadingState = readyState.loading(false)
        assertThat(loadingState.previous, `is`(equalTo(readyState)))
        assertThat(loadingState.previous?.data, `is`(equalTo(testData)))

        val errorState = readyState.error("error")
        assertThat(errorState.previous, `is`(equalTo(readyState)))
        assertThat(errorState.previous?.data, `is`(equalTo(testData)))
    }

    @Test
    fun testLoadingFirstPageState() {
        val testDataReady = listOf("item3", "item4")
        val readyState: ListNetworkResource<String> = ListNetworkResource.Ready(ListNetworkResource.Init(),
                testDataReady)
        val loadingState: ListNetworkResource<String> = ListNetworkResource.Loading(readyState)

        // Verify the state

        assertThat(loadingState.data, `is`(equalTo(testDataReady)))
        assertThat(loadingState.previous, `is`(equalTo(readyState)))

        // We are not first page
        assertThat(loadingState.isFetchingFirstPage(), `is`(true))
        assertThat(loadingState.isLoadingMore(), `is`(false))

        // We are already fetching
        assertThat(loadingState.shouldFetch(true), `is`(false))
        assertThat(loadingState.shouldFetch(false), `is`(false))

        // State transitions

        val testDataSuccess = listOf("item 5")

        val successState = loadingState.success(testDataSuccess)
        assertThat(successState.previous, `is`(equalTo(loadingState)))
        assertThat(successState.previous?.data, `is`(equalTo(testDataReady)))
        assertThat(successState.data, `is`(equalTo(testDataSuccess)))

        assertThat(successState.previous?.isLoadingMore(), `is`(false))
        assertThat(successState.previous?.isFetchingFirstPage(), `is`(true))

        val errorState = loadingState.error("error")
        assertThat(errorState.previous, `is`(equalTo(loadingState)))
        assertThat(errorState.previous?.data, `is`(equalTo(testDataReady)))
        assertThat(errorState.data, `is`(equalTo(testDataReady)))

        assertThat(errorState.previous?.isLoadingMore(), `is`(false))
        assertThat(errorState.previous?.isFetchingFirstPage(), `is`(true))
    }
}
