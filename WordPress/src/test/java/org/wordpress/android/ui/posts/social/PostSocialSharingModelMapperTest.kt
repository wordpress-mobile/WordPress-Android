package org.wordpress.android.ui.posts.social

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.ui.compose.components.TrainOfIconsModel
import org.wordpress.android.usecase.social.ShareLimit
import org.wordpress.android.util.StringProvider

@ExperimentalCoroutinesApi
class PostSocialSharingModelMapperTest : BaseUnitTest() {
    private val stringProvider: StringProvider = mock()
    private val classToTest = PostSocialSharingModelMapper(
        stringProvider = stringProvider,
    )

    private val shareLimitEnabled = ShareLimit.Enabled(
        shareLimit = 10,
        publicizedCount = 10,
        sharedPostsCount = 10,
        sharesRemaining = 10,
    )
    private val description = "description"

    @Test
    fun `Should return model with blank description and isLowShares = false if share limit is disabled`() {
        mockTitleSharingToZeroAccounts("title")
        val actual = classToTest.map(emptyList(), ShareLimit.Disabled)
        assertThat(actual.title).isNotBlank
        assertThat(actual.description).isBlank
        assertThat(actual.isLowOnShares).isFalse
    }

    @Test
    fun `Should map title correctly when sharing to 0 accounts`() {
        val connections = listOf(
            postSocialConnection(isSharingEnabled = false),
            postSocialConnection(isSharingEnabled = false),
        )
        mockDescription()
        val expected = "Sharing to 0 accounts"
        mockTitleSharingToZeroAccounts(expected)
        val actual = classToTest.map(
            connections = connections,
            shareLimit = shareLimitEnabled,
        ).title
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `Should map title correctly when sharing to single account`() {
        val singleEnabledConnection = postSocialConnection(isSharingEnabled = true)
        val connections = listOf(
            singleEnabledConnection,
        )
        mockDescription()
        val expected = "Sharing to @account"
        whenever(
            stringProvider.getString(
                R.string.jetpack_social_social_shares_title_single_account,
                singleEnabledConnection.externalName
            )
        ).thenReturn(expected)
        val actual = classToTest.map(
            connections = connections,
            shareLimit = shareLimitEnabled,
        ).title
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `Should map title correctly when sharing to all accounts`() {
        val enabledConnection = postSocialConnection(isSharingEnabled = true)
        val connections = listOf(
            enabledConnection,
            enabledConnection,
            enabledConnection,
        )
        mockDescription()
        val expected = "Sharing to all account"
        whenever(
            stringProvider.getString(R.string.jetpack_social_social_shares_title_all_accounts, connections.size)
        ).thenReturn(expected)
        val actual = classToTest.map(
            connections = connections,
            shareLimit = shareLimitEnabled,
        ).title
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `Should map title correctly when sharing to some accounts (more than one but not all)`() {
        val connections = listOf(
            postSocialConnection(isSharingEnabled = true),
            postSocialConnection(isSharingEnabled = true),
            postSocialConnection(isSharingEnabled = false),
        )
        mockDescription()
        val expected = "Sharing to 2 out of 3 accounts"
        mockSharingToSomeAccounts(connections, expected)
        val actual = classToTest.map(
            connections = connections,
            shareLimit = shareLimitEnabled,
        ).title
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `Should map description correctly (multiple shares)`() {
        val connections = listOf(
            postSocialConnection(isSharingEnabled = false),
        )
        mockTitleSharingToZeroAccounts("")
        mockDescription()
        val actual = classToTest.map(
            connections = connections,
            shareLimit = shareLimitEnabled,
        ).description
        assertThat(actual).isEqualTo(description)
    }

    @Test
    fun `Should map description correctly (one share)`() {
        val descriptionOneShare = "One share remaining"
        val connections = listOf(
            postSocialConnection(isSharingEnabled = false),
        )
        mockTitleSharingToZeroAccounts("")
        whenever(stringProvider.getString(R.string.jetpack_social_social_shares_remaining_one))
            .thenReturn(descriptionOneShare)
        val shareLimit = shareLimitEnabled.copy(
            sharesRemaining = 1,
        )
        val actual = classToTest.map(
            connections = connections,
            shareLimit = shareLimit,
        ).description
        assertThat(actual).isEqualTo(descriptionOneShare)
    }

    @Test
    fun `Should map icon models correctly`() {
        val connections = listOf(
            postSocialConnection(isSharingEnabled = true),
            postSocialConnection(isSharingEnabled = false),
            postSocialConnection(isSharingEnabled = true),
        )
        mockDescription()
        mockSharingToSomeAccounts(connections, "")
        // data is null because we get it from static method PublicizeServiceIcon#fromServiceId
        val expected = listOf(
            TrainOfIconsModel(
                data = null,
                // isSharingEnabled = false
                alpha = 0.3F,
            ),
            TrainOfIconsModel(
                data = null,
                // isSharingEnabled = true
                alpha = 1F,
            ),
            TrainOfIconsModel(
                data = null,
                // isSharingEnabled = true
                alpha = 1F,
            )
        )
        val actual = classToTest.map(
            connections = connections,
            shareLimit = shareLimitEnabled,
        ).iconModels
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `Should map low on shares correctly when no more shares left`() {
        val connections = listOf(
            postSocialConnection(isSharingEnabled = false),
            postSocialConnection(isSharingEnabled = false),
        )
        mockTitleSharingToZeroAccounts("")
        val shareLimit = shareLimitEnabled.copy(
            sharesRemaining = 0,
        )
        mockDescription(shareLimit)
        val actual = classToTest.map(
            connections = connections,
            shareLimit = shareLimit,
        ).isLowOnShares
        val expected = true
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `Should map low on shares correctly when sharing to some accounts but not enough shares for all`() {
        val connections = listOf(
            postSocialConnection(isSharingEnabled = true),
            postSocialConnection(isSharingEnabled = true),
            postSocialConnection(isSharingEnabled = false),
            postSocialConnection(isSharingEnabled = false),
            postSocialConnection(isSharingEnabled = false),
        )
        mockSharingToSomeAccounts(connections, "")
        val shareLimit = shareLimitEnabled.copy(
            sharesRemaining = 4,
        )
        mockDescription(shareLimit)
        val actual = classToTest.map(
            connections = connections,
            shareLimit = shareLimit,
        ).isLowOnShares
        val expected = true
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `Should map low on shares correctly when remaining shares less than number of available accounts`() {
        val connections = listOf(
            postSocialConnection(isSharingEnabled = false),
            postSocialConnection(isSharingEnabled = false),
            postSocialConnection(isSharingEnabled = false),
        )
        mockTitleSharingToZeroAccounts("")
        val shareLimit = shareLimitEnabled.copy(
            sharesRemaining = 2,
        )
        mockDescription(shareLimit)
        val actual = classToTest.map(
            connections = connections,
            shareLimit = shareLimit,
        ).isLowOnShares
        val expected = true
        assertThat(actual).isEqualTo(expected)
    }

    private fun mockDescription(
        shareLimit: ShareLimit.Enabled = shareLimitEnabled,
    ) {
        whenever(
            stringProvider.getString(
                R.string.jetpack_social_social_shares_remaining,
                shareLimit.sharesRemaining
            )
        ).thenReturn(description)
    }

    private fun mockTitleSharingToZeroAccounts(expected: String) {
        whenever(stringProvider.getString(R.string.jetpack_social_social_shares_title_not_sharing))
            .thenReturn(expected)
    }

    private fun mockSharingToSomeAccounts(
        connections: List<PostSocialConnection>,
        expected: String
    ) {
        val sharingEnabledConnections = connections.filter { it.isSharingEnabled }
        whenever(
            stringProvider.getString(
                R.string.jetpack_social_social_shares_title_part_of_the_accounts,
                sharingEnabledConnections.size,
                connections.size
            )
        ).thenReturn(expected)
    }

    private fun postSocialConnection(isSharingEnabled: Boolean) = PostSocialConnection(
        connectionId = 123,
        service = "service",
        label = "label",
        externalId = "externalId",
        externalName = "externalName",
        iconResId = R.drawable.ic_social_tumblr,
        isSharingEnabled = isSharingEnabled,
    )
}
