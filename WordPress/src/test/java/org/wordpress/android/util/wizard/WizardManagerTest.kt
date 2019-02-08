package org.wordpress.android.util.wizard

import org.junit.Before
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.ui.sitecreation.SiteCreationStep
import org.wordpress.android.ui.sitecreation.SiteCreationStep.DOMAINS
import org.wordpress.android.ui.sitecreation.SiteCreationStep.SEGMENTS
import org.wordpress.android.ui.sitecreation.SiteCreationStep.SITE_PREVIEW

private val STEPS = listOf(SEGMENTS, DOMAINS, SITE_PREVIEW)

@RunWith(MockitoJUnitRunner::class)
class WizardManagerTest {
    private lateinit var manager: WizardManager<SiteCreationStep>

    @Before
    fun setUp() {
        manager = WizardManager(STEPS)
    }
}

