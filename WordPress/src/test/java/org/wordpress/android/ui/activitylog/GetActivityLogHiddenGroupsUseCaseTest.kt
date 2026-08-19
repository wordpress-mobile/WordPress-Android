package org.wordpress.android.ui.activitylog

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.jetpack.JetpackCapabilitiesUseCase
import org.wordpress.android.ui.jetpack.JetpackCapabilitiesUseCase.JetpackPurchasedProducts

private const val SITE_ID = 123L

@RunWith(MockitoJUnitRunner::class)
class GetActivityLogHiddenGroupsUseCaseTest {
    @Mock
    private lateinit var jetpackCapabilitiesUseCase: JetpackCapabilitiesUseCase

    private lateinit var useCase: GetActivityLogHiddenGroupsUseCase
    private lateinit var site: SiteModel

    @Before
    fun setUp() {
        useCase = GetActivityLogHiddenGroupsUseCase(jetpackCapabilitiesUseCase)
        site = SiteModel().apply { siteId = SITE_ID }
    }

    @Test
    fun `no groups hidden, when site has the backups-self-serve feature`() {
        site.planActiveFeatures = "subscriber-unlimited-imports,backups-self-serve,support"

        assertThat(useCase.getHiddenGroups(site)).isEmpty()
    }

    @Test
    fun `rewind and scan hidden, when site has no backup feature or purchases`() {
        site.planActiveFeatures = "subscriber-unlimited-imports,support"
        stubPurchasedProducts(scan = false, backup = false)

        assertThat(useCase.getHiddenGroups(site)).containsExactly("rewind", "scan")
    }

    @Test
    fun `rewind and scan hidden, when site has no plan features and no purchases`() {
        site.planActiveFeatures = null
        stubPurchasedProducts(scan = false, backup = false)

        assertThat(useCase.getHiddenGroups(site)).containsExactly("rewind", "scan")
    }

    @Test
    fun `only scan hidden, when site purchased a backup product`() {
        site.planActiveFeatures = null
        stubPurchasedProducts(scan = false, backup = true)

        assertThat(useCase.getHiddenGroups(site)).containsExactly("scan")
    }

    @Test
    fun `only rewind hidden, when site purchased a scan product`() {
        site.planActiveFeatures = null
        stubPurchasedProducts(scan = true, backup = false)

        assertThat(useCase.getHiddenGroups(site)).containsExactly("rewind")
    }

    @Test
    fun `no groups hidden, when site purchased backup and scan products`() {
        site.planActiveFeatures = null
        stubPurchasedProducts(scan = true, backup = true)

        assertThat(useCase.getHiddenGroups(site)).isEmpty()
    }

    private fun stubPurchasedProducts(scan: Boolean, backup: Boolean) {
        whenever(jetpackCapabilitiesUseCase.getCachedJetpackPurchasedProducts(SITE_ID))
            .thenReturn(JetpackPurchasedProducts(scan = scan, backup = backup))
    }
}
