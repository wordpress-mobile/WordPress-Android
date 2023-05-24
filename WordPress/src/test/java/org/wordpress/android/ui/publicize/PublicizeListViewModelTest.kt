package org.wordpress.android.ui.publicize

import androidx.lifecycle.Observer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.ui.publicize.services.PublicizeUpdateServicesV2
import org.wordpress.android.util.EventBusWrapper

@ExperimentalCoroutinesApi
class PublicizeListViewModelTest : BaseUnitTest() {
    private val publicizeUpdateServicesV2: PublicizeUpdateServicesV2 = mock()
    private val eventBusWrapper: EventBusWrapper = mock()
    private val accountStore: AccountStore = mock()
    private val classToTest = PublicizeListViewModel(
        publicizeUpdateServicesV2 = publicizeUpdateServicesV2,
        eventBusWrapper = eventBusWrapper,
        accountStore = accountStore,
        bgDispatcher = testDispatcher(),
    )
    private val actionObserver: Observer<PublicizeListViewModel.ActionEvent> = mock()

    @Before
    fun setup() {
        classToTest.actionEvents.observeForever(actionObserver)
    }

    @Test
    fun `Should call update services when onSiteAvailable is called`() {
        val siteModel = SiteModel()
        classToTest.onSiteAvailable(siteModel)
        verify(publicizeUpdateServicesV2).updateServices(eq(siteModel.siteId), any(), any())
    }

    @Test
    fun `Should not trigger OpenServiceDetails if service null when onTwitterDeprecationNoticeItemClick is called`() {
        classToTest.onTwitterDeprecationNoticeItemClick()
        verify(actionObserver, never()).onChanged(any())
    }
}
