package org.wordpress.android.ui.stats.refresh.utils

import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.stats.PublicizeModel.Service
import org.wordpress.android.viewmodel.ResourceProvider

class ServiceMapperTest : BaseUnitTest() {
    @Mock lateinit var resourceProvider: ResourceProvider
    private lateinit var serviceMapper: ServiceMapper
    @Before
    fun setUp() {
        serviceMapper = ServiceMapper(resourceProvider)
    }

    @Test
    fun `maps facebook item correctly`() {
        val service = Service("facebook", 15000)
        val pixelSize = 10
        whenever(resourceProvider.getDimensionPixelSize(R.dimen.avatar_sz_small)).thenReturn(pixelSize)

        val result = serviceMapper.map(listOf(service))

        assertThat(result).hasSize(1)
        result[0].apply {
            val iconUrl = "https://secure.gravatar.com/blavatar/2343ec78a04c6ea9d80806345d31fd78?s=$pixelSize"
            assertThat(this.iconUrl).isEqualTo(iconUrl)
            assertThat(this.textResource).isEqualTo(R.string.stats_insights_facebook)
            assertThat(this.text).isNull()
            assertThat(this.icon).isNull()
            assertThat(this.showDivider).isFalse()
            assertThat(this.value).isEqualTo("15k")
        }
    }

    @Test
    fun `maps twitter item correctly`() {
        val service = Service("twitter", 15000)
        val pixelSize = 10
        whenever(resourceProvider.getDimensionPixelSize(R.dimen.avatar_sz_small)).thenReturn(pixelSize)

        val result = serviceMapper.map(listOf(service))

        assertThat(result).hasSize(1)
        result[0].apply {
            val iconUrl = "https://secure.gravatar.com/blavatar/7905d1c4e12c54933a44d19fcd5f9356?s=$pixelSize"
            assertThat(this.iconUrl).isEqualTo(iconUrl)
            assertThat(this.textResource).isEqualTo(R.string.stats_insights_twitter)
            assertThat(this.text).isNull()
            assertThat(this.icon).isNull()
            assertThat(this.showDivider).isFalse()
            assertThat(this.value).isEqualTo("15k")
        }
    }

    @Test
    fun `maps tumblr item correctly`() {
        val service = Service("tumblr", 15000)
        val pixelSize = 10
        whenever(resourceProvider.getDimensionPixelSize(R.dimen.avatar_sz_small)).thenReturn(pixelSize)

        val result = serviceMapper.map(listOf(service))

        assertThat(result).hasSize(1)
        result[0].apply {
            val iconUrl = "https://secure.gravatar.com/blavatar/84314f01e87cb656ba5f382d22d85134?s=$pixelSize"
            assertThat(this.iconUrl).isEqualTo(iconUrl)
            assertThat(this.textResource).isEqualTo(R.string.stats_insights_tumblr)
            assertThat(this.text).isNull()
            assertThat(this.icon).isNull()
            assertThat(this.showDivider).isFalse()
            assertThat(this.value).isEqualTo("15k")
        }
    }

    @Test
    fun `maps path item correctly`() {
        val service = Service("path", 15000)
        val pixelSize = 10
        whenever(resourceProvider.getDimensionPixelSize(R.dimen.avatar_sz_small)).thenReturn(pixelSize)

        val result = serviceMapper.map(listOf(service))

        assertThat(result).hasSize(1)
        result[0].apply {
            val iconUrl = "https://secure.gravatar.com/blavatar/3a03c8ce5bf1271fb3760bb6e79b02c1?s=$pixelSize"
            assertThat(this.iconUrl).isEqualTo(iconUrl)
            assertThat(this.textResource).isEqualTo(R.string.stats_insights_path)
            assertThat(this.text).isNull()
            assertThat(this.icon).isNull()
            assertThat(this.showDivider).isFalse()
            assertThat(this.value).isEqualTo("15k")
        }
    }

    @Test
    fun `maps google plus item correctly`() {
        val service = Service("google_plus", 15000)
        val pixelSize = 10
        whenever(resourceProvider.getDimensionPixelSize(R.dimen.avatar_sz_small)).thenReturn(pixelSize)

        val result = serviceMapper.map(listOf(service))

        assertThat(result).hasSize(1)
        result[0].apply {
            val iconUrl = "https://secure.gravatar.com/blavatar/4a4788c1dfc396b1f86355b274cc26b3?s=$pixelSize"
            assertThat(this.iconUrl).isEqualTo(iconUrl)
            assertThat(this.textResource).isEqualTo(R.string.stats_insights_google_plus)
            assertThat(this.text).isNull()
            assertThat(this.icon).isNull()
            assertThat(this.showDivider).isFalse()
            assertThat(this.value).isEqualTo("15k")
        }
    }

    @Test
    fun `maps linkedin item correctly`() {
        val service = Service("linkedin", 15000)
        val pixelSize = 10
        whenever(resourceProvider.getDimensionPixelSize(R.dimen.avatar_sz_small)).thenReturn(pixelSize)

        val result = serviceMapper.map(listOf(service))

        assertThat(result).hasSize(1)
        result[0].apply {
            val iconUrl = "https://secure.gravatar.com/blavatar/f54db463750940e0e7f7630fe327845e?s=$pixelSize"
            assertThat(this.iconUrl).isEqualTo(iconUrl)
            assertThat(this.textResource).isEqualTo(R.string.stats_insights_linkedin)
            assertThat(this.text).isNull()
            assertThat(this.icon).isNull()
            assertThat(this.showDivider).isFalse()
            assertThat(this.value).isEqualTo("15k")
        }
    }

    @Test
    fun `maps unknown item correctly`() {
        val serviceName = "unknown"
        val service = Service(serviceName, 15000)
        val pixelSize = 10
        whenever(resourceProvider.getDimensionPixelSize(R.dimen.avatar_sz_small)).thenReturn(pixelSize)

        val result = serviceMapper.map(listOf(service))

        assertThat(result).hasSize(1)
        result[0].apply {
            assertThat(this.iconUrl).isNull()
            assertThat(this.textResource).isNull()
            assertThat(this.text).isEqualTo(serviceName)
            assertThat(this.icon).isNull()
            assertThat(this.showDivider).isFalse()
            assertThat(this.value).isEqualTo("15k")
        }
    }

    @Test
    fun `shows divider on all items but last`() {
        val service1 = Service("facebook", 15000)
        val service2 = Service("twitter", 500)
        val service3 = Service("tumblr", 50)
        val pixelSize = 10
        whenever(resourceProvider.getDimensionPixelSize(R.dimen.avatar_sz_small)).thenReturn(pixelSize)

        val result = serviceMapper.map(listOf(service1, service2, service3))

        assertThat(result).hasSize(3)
        assertThat(result[0].showDivider).isTrue()
        assertThat(result[1].showDivider).isTrue()
        assertThat(result[2].showDivider).isFalse()
    }
}
