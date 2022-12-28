package org.wordpress.android.ui.people

import android.content.Context
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.Mock
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.model.RoleModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.models.InvitePeopleUtils
import org.wordpress.android.models.wrappers.RoleUtilsWrapper
import org.wordpress.android.models.wrappers.SimpleDateFormatWrapper
import org.wordpress.android.ui.people.InviteLinksApiCallsProvider.InviteLinksItem
import org.wordpress.android.util.DateTimeUtilsWrapper
import org.wordpress.android.viewmodel.ContextProvider
import java.text.DateFormat
import java.util.Date

@ExperimentalCoroutinesApi
class InvitePeopleUtilsTest : BaseUnitTest() {
    @Mock
    lateinit var siteStore: SiteStore
    @Mock
    lateinit var siteModel: SiteModel
    @Mock
    lateinit var contextProvider: ContextProvider
    @Mock
    lateinit var dateTimeUtilsWrapper: DateTimeUtilsWrapper
    @Mock
    lateinit var roleUtilsWrapper: RoleUtilsWrapper
    @Mock
    lateinit var context: Context
    @Mock
    lateinit var simpleDateFormatWrapper: SimpleDateFormatWrapper
    @Mock
    lateinit var dateFormat: DateFormat

    private lateinit var invitePeopleUtils: InvitePeopleUtils

    private val roles = listOf(
        RoleModel().apply {
            name = "administrator"
            displayName = "Administrator"
        },
        RoleModel().apply {
            name = "contributor"
            displayName = "Contributor"
        }
    )

    private val linkItems = mutableListOf(
        InviteLinksItem(
            role = "administrator",
            expiry = 0,
            link = "https://wordpress.com/linkdata"
        ),
        InviteLinksItem(
            role = "follower",
            expiry = 0,
            link = "https://wordpress.com/linkdata"
        )
    )

    @Before
    fun setUp() {
        val date: Date = mock()
        whenever(contextProvider.getContext()).thenReturn(context)
        whenever(roleUtilsWrapper.getInviteRoles(siteStore, siteModel, context)).thenReturn(roles)
        whenever(dateTimeUtilsWrapper.dateFromTimestamp(anyLong())).thenReturn(date)
        whenever(dateFormat.format(eq(date))).thenReturn("")
        whenever(simpleDateFormatWrapper.getDateInstance()).thenReturn(dateFormat)

        invitePeopleUtils = InvitePeopleUtils(
            siteStore,
            contextProvider,
            dateTimeUtilsWrapper,
            roleUtilsWrapper,
            simpleDateFormatWrapper
        )
    }

    @Test
    fun `link data found by available display name`() {
        val item = invitePeopleUtils.getInviteLinkDataFromRoleDisplayName(linkItems, siteModel, "Administrator")

        requireNotNull(item).let {
            assertThat(it.role).isEqualTo("administrator")
        }
    }

    @Test
    fun `link data not found by missing display name`() {
        val item = invitePeopleUtils.getInviteLinkDataFromRoleDisplayName(linkItems, siteModel, "Follower")

        assertThat(item).isNull()
    }

    @Test
    fun `display name found for available role`() {
        val displayName = invitePeopleUtils.getDisplayNameForRole(siteModel, "administrator")

        assertThat(displayName).isEqualTo("Administrator")
    }

    @Test
    fun `display name not found for missing role`() {
        val displayName = invitePeopleUtils.getDisplayNameForRole(siteModel, "follower")

        assertThat(displayName).isEmpty()
    }

    @Test
    fun `ui items list created as expected`() {
        val uiItemsList = invitePeopleUtils.getMappedLinksUiItems(linkItems, siteModel)

        assertThat(uiItemsList.count()).isEqualTo(1)
        assertThat(uiItemsList.get(0).roleName).isEqualTo("administrator")
    }

    @Test
    fun `role display names created as expected`() {
        val displayNamesList = invitePeopleUtils.getInviteLinksRoleDisplayNames(linkItems, siteModel)

        assertThat(displayNamesList.count()).isEqualTo(1)
        assertThat(displayNamesList.get(0)).isEqualTo("Administrator")
    }
}
