package org.wordpress.android.ui.jetpack.restore.builders

import kotlinx.coroutines.InternalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.Mock
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.Constants
import org.wordpress.android.R
import org.wordpress.android.test
import org.wordpress.android.ui.jetpack.common.CheckboxSpannableLabel
import org.wordpress.android.ui.jetpack.common.JetpackBackupRestoreListItemState.FootnoteState
import org.wordpress.android.ui.jetpack.common.JetpackListItemState
import org.wordpress.android.ui.jetpack.common.JetpackListItemState.ActionButtonState
import org.wordpress.android.ui.utils.HtmlMessageUtils
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.ui.utils.UiString.UiStringText
import org.wordpress.android.util.text.PercentFormatter
import java.util.Date

private const val TEST_SITE_ID = 1L
private const val SERVER_CREDS_LINK = "${Constants.URL_JETPACK_SETTINGS}/$TEST_SITE_ID"
private const val SERVER_CREDS_MSG_WITH_CLICKABLE_LINK =
        "<a href=\"$SERVER_CREDS_LINK\">Enter your server credentials&lt</a> " +
                "to enable one click site restores from backups."

@InternalCoroutinesApi
class RestoreStateListItemBuilderTest : BaseUnitTest() {
    private lateinit var builder: RestoreStateListItemBuilder

    @Mock private lateinit var checkboxSpannableLabel: CheckboxSpannableLabel
    @Mock private lateinit var htmlMessageUtils: HtmlMessageUtils
    @Mock private lateinit var percentFormatter: PercentFormatter

    @Before
    fun setUp() {
        builder = RestoreStateListItemBuilder(checkboxSpannableLabel, htmlMessageUtils, percentFormatter)
        whenever(htmlMessageUtils.getHtmlMessageFromStringFormatResId(anyInt(), any()))
                .thenReturn(SERVER_CREDS_MSG_WITH_CLICKABLE_LINK)
    }

    @Test
    fun `given not awaiting creds, when items are built, then restore details action btn is enabled`() =
            test {
                val items = buildDetailsListStateItems(isAwaitingCredentials = false)

                val restoreSiteButton = items.filterIsInstance(ActionButtonState::class.java)
                        .firstOrNull { it.text == UiStringRes(R.string.restore_details_action_button) }
                assertThat(restoreSiteButton?.isEnabled).isTrue
            }

    @Test
    fun `given awaiting creds, when items are built, then restore details action btn is disabled`() =
            test {
                val items = buildDetailsListStateItems(isAwaitingCredentials = true)

                val restoreSiteButton = items.filterIsInstance(ActionButtonState::class.java)
                        .firstOrNull { it.text == UiStringRes(R.string.restore_details_action_button) }
                assertThat(restoreSiteButton?.isEnabled).isFalse
            }

    @Test
    fun `given not awaiting creds, when items are built, then server creds msg does not exist`() =
            test {
                val items = buildDetailsListStateItems(isAwaitingCredentials = false)

                assertThat(items.filterIsInstance(FootnoteState::class.java)
                        .firstOrNull { it.text == UiStringText(SERVER_CREDS_MSG_WITH_CLICKABLE_LINK) })
                        .isNull()
            }

    @Test
    fun `given awaiting creds, when items are built, then server creds msg exists`() =
            test {
                val items = buildDetailsListStateItems(isAwaitingCredentials = true)

                assertThat(items.filterIsInstance(FootnoteState::class.java).first().text)
                        .isEqualTo(UiStringText(SERVER_CREDS_MSG_WITH_CLICKABLE_LINK))
            }

    @Test
    fun `given server creds msg exists, when items are built, then primary colored plus button exists`() =
            test {
                val items = buildDetailsListStateItems(isAwaitingCredentials = true)

                val serverCredsMsg = items.filterIsInstance(FootnoteState::class.java)
                        .first { it.text == UiStringText(SERVER_CREDS_MSG_WITH_CLICKABLE_LINK) }
                with(serverCredsMsg) {
                    assertThat(iconRes).isEqualTo(R.drawable.ic_plus_white_24dp)
                    assertThat(iconColorResId).isEqualTo(R.color.colorPrimary)
                }
            }

    @Test
    fun `whenever buildProgressListStateItems is called should call PercentFormatter`() = test {
        val progress = 30
        whenever(percentFormatter.format(progress))
                .thenReturn("100")
        builder.buildProgressListStateItems(progress, Date(0))
        verify(percentFormatter).format(progress)
    }

    private fun buildDetailsListStateItems(
        isAwaitingCredentials: Boolean = false
    ): List<JetpackListItemState> {
        return builder.buildDetailsListStateItems(
                published = mock(),
                availableItems = emptyList(),
                siteId = TEST_SITE_ID,
                isAwaitingCredentials = isAwaitingCredentials,
                onCreateDownloadClick = mock(),
                onCheckboxItemClicked = mock(),
                onEnterServerCredsIconClicked = mock()
        )
    }
}
