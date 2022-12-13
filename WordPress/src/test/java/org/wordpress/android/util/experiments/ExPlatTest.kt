package org.wordpress.android.util.experiments

import dagger.Lazy
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.model.experiments.Assignments
import org.wordpress.android.fluxc.model.experiments.Variation
import org.wordpress.android.fluxc.model.experiments.Variation.Control
import org.wordpress.android.fluxc.model.experiments.Variation.Treatment
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.ExperimentStore
import org.wordpress.android.fluxc.store.ExperimentStore.OnAssignmentsFetched
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.testScope
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import java.util.Date

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class ExPlatTest : BaseUnitTest() {
    @Mock lateinit var experiments: Lazy<Set<Experiment>>
    @Mock lateinit var experimentStore: ExperimentStore
    @Mock lateinit var appLog: AppLogWrapper
    @Mock lateinit var accountStore: AccountStore
    @Mock lateinit var analyticsTracker: AnalyticsTrackerWrapper
    private lateinit var exPlat: ExPlat
    private lateinit var dummyExperiment: Experiment

    @Before
    fun setUp() {
        exPlat = ExPlat(experiments, experimentStore, appLog, accountStore, analyticsTracker, testScope())
        dummyExperiment = object : Experiment(DUMMY_EXPERIMENT_NAME, exPlat) {}
        whenever(accountStore.hasAccessToken()).thenReturn(true)
        whenever(analyticsTracker.getAnonID()).thenReturn(DUMMY_ANON_ID)
        setupExperiments(setOf(dummyExperiment))
    }

    @Test
    fun `refreshIfNeeded fetches assignments if cache is null`() = test {
        setupAssignments(cachedAssignments = null, fetchedAssignments = buildAssignments())

        exPlat.refreshIfNeeded()

        verify(experimentStore, times(1)).fetchAssignments(any(), any(), anyOrNull())
    }

    @Test
    fun `refreshIfNeeded fetches assignments if cache is stale`() = test {
        setupAssignments(cachedAssignments = buildAssignments(isStale = true), fetchedAssignments = buildAssignments())

        exPlat.refreshIfNeeded()

        verify(experimentStore, times(1)).fetchAssignments(any(), any(), anyOrNull())
    }

    @Test
    fun `refreshIfNeeded does not fetch assignments if cache is fresh`() = test {
        setupAssignments(cachedAssignments = buildAssignments(isStale = false), fetchedAssignments = buildAssignments())

        exPlat.refreshIfNeeded()

        verify(experimentStore, never()).fetchAssignments(any(), any(), anyOrNull())
    }

    @Test
    fun `forceRefresh fetches assignments if cache is fresh`() = test {
        setupAssignments(cachedAssignments = buildAssignments(isStale = true), fetchedAssignments = buildAssignments())

        exPlat.forceRefresh()

        verify(experimentStore, times(1)).fetchAssignments(any(), any(), anyOrNull())
    }

    @Test
    fun `clear calls experiment store`() = test {
        exPlat.clear()

        verify(experimentStore, times(1)).clearCachedAssignments()
    }

    @Test
    fun `getVariation fetches assignments if cache is null`() = test {
        setupAssignments(cachedAssignments = null, fetchedAssignments = buildAssignments())

        exPlat.getVariation(dummyExperiment, shouldRefreshIfStale = true)

        verify(experimentStore, times(1)).fetchAssignments(any(), any(), anyOrNull())
    }

    @Test
    fun `getVariation fetches assignments if cache is stale`() = test {
        setupAssignments(cachedAssignments = buildAssignments(isStale = true), fetchedAssignments = buildAssignments())

        exPlat.getVariation(dummyExperiment, shouldRefreshIfStale = true)

        verify(experimentStore, times(1)).fetchAssignments(any(), any(), anyOrNull())
    }

    @Test
    fun `getVariation does not fetch assignments if cache is fresh`() = test {
        setupAssignments(cachedAssignments = buildAssignments(isStale = false), fetchedAssignments = buildAssignments())

        exPlat.getVariation(dummyExperiment, shouldRefreshIfStale = true)

        verify(experimentStore, never()).fetchAssignments(any(), any(), anyOrNull())
    }

    @Test
    fun `getVariation does not fetch assignments if cache is null but shouldRefreshIfStale is false`() = test {
        setupAssignments(cachedAssignments = null, fetchedAssignments = buildAssignments())

        exPlat.getVariation(dummyExperiment, shouldRefreshIfStale = false)

        verify(experimentStore, never()).fetchAssignments(any(), any(), anyOrNull())
    }

    @Test
    fun `getVariation does not fetch assignments if cache is stale but shouldRefreshIfStale is false`() = test {
        setupAssignments(cachedAssignments = null, fetchedAssignments = buildAssignments())

        exPlat.getVariation(dummyExperiment, shouldRefreshIfStale = false)

        verify(experimentStore, never()).fetchAssignments(any(), any(), anyOrNull())
    }

    @Test
    fun `getVariation does not return different cached assignments if active variation exists`() = test {
        val controlVariation = Control
        val treatmentVariation = Treatment("treatment")

        val treatmentAssignments = buildAssignments(variations = mapOf(dummyExperiment.name to treatmentVariation))

        setupAssignments(cachedAssignments = null, fetchedAssignments = treatmentAssignments)

        val firstVariation = exPlat.getVariation(dummyExperiment, shouldRefreshIfStale = false)
        assertThat(firstVariation).isEqualTo(controlVariation)

        exPlat.forceRefresh()

        setupAssignments(cachedAssignments = treatmentAssignments, fetchedAssignments = treatmentAssignments)

        val secondVariation = exPlat.getVariation(dummyExperiment, shouldRefreshIfStale = false)
        assertThat(secondVariation).isEqualTo(controlVariation)
    }

    @Test
    fun `forceRefresh fetches assignments if experiments is not empty`() = test {
        setupExperiments(setOf(dummyExperiment))

        exPlat.forceRefresh()

        verify(experimentStore, times(1)).fetchAssignments(any(), any(), anyOrNull())
    }

    @Test
    fun `forceRefresh does not interact with store if experiments is empty`() = test {
        setupExperiments(emptySet())

        exPlat.forceRefresh()

        verifyNoInteractions(experimentStore)
    }

    @Test
    fun `refreshIfNeeded does not interact with store if experiments is empty`() = test {
        setupExperiments(emptySet())

        exPlat.refreshIfNeeded()

        verifyNoInteractions(experimentStore)
    }

    @Test
    @Suppress("SwallowedException")
    fun `getVariation does not interact with store if experiments is empty`() = test {
        setupExperiments(emptySet())

        try {
            exPlat.getVariation(dummyExperiment, false)
        } catch (e: IllegalArgumentException) {
            // Do nothing.
        } finally {
            verifyNoInteractions(experimentStore)
        }
    }

    @Test
    @Suppress("MaxLineLength")
    fun `refreshIfNeeded does not interact with store if the user is not authorised and there is no anonymous id`() = test {
        setupExperiments(setOf(dummyExperiment))
        whenever(accountStore.hasAccessToken()).thenReturn(false)
        whenever(analyticsTracker.getAnonID()).thenReturn(null)

        exPlat.refreshIfNeeded()

        verifyNoInteractions(experimentStore)
    }

    @Test
    @Suppress("MaxLineLength")
    fun `forceRefresh does not interact with store if the user is not authorised and there is no anonymous id`() = test {
        setupExperiments(setOf(dummyExperiment))
        whenever(accountStore.hasAccessToken()).thenReturn(false)
        whenever(analyticsTracker.getAnonID()).thenReturn(null)

        exPlat.forceRefresh()

        verifyNoInteractions(experimentStore)
    }

    @Test
    fun `refreshIfNeeded does interact with store if the user is authorised`() = test {
        setupExperiments(setOf(dummyExperiment))
        whenever(accountStore.hasAccessToken()).thenReturn(true)
        whenever(analyticsTracker.getAnonID()).thenReturn(null)

        exPlat.refreshIfNeeded()

        verify(experimentStore, times(1)).fetchAssignments(any(), any(), anyOrNull())
    }

    @Test
    fun `forceRefresh does interact with store if the user is authorised`() = test {
        setupExperiments(setOf(dummyExperiment))
        whenever(accountStore.hasAccessToken()).thenReturn(true)
        whenever(analyticsTracker.getAnonID()).thenReturn(null)

        exPlat.forceRefresh()

        verify(experimentStore, times(1)).fetchAssignments(any(), any(), anyOrNull())
    }

    @Test
    fun `refreshIfNeeded does interact with store if there is an anonymous id`() = test {
        setupExperiments(setOf(dummyExperiment))
        whenever(accountStore.hasAccessToken()).thenReturn(false)
        whenever(analyticsTracker.getAnonID()).thenReturn(DUMMY_ANON_ID)

        exPlat.refreshIfNeeded()

        verify(experimentStore, times(1)).fetchAssignments(any(), any(), anyOrNull())
    }

    @Test
    fun `forceRefresh does interact with store if there is an anonymous id`() = test {
        setupExperiments(setOf(dummyExperiment))
        whenever(accountStore.hasAccessToken()).thenReturn(false)
        whenever(analyticsTracker.getAnonID()).thenReturn(DUMMY_ANON_ID)

        exPlat.forceRefresh()

        verify(experimentStore, times(1)).fetchAssignments(any(), any(), anyOrNull())
    }

    private fun setupExperiments(experiments: Set<Experiment>) {
        whenever(this.experiments.get()).thenReturn(experiments)
    }

    private suspend fun setupAssignments(cachedAssignments: Assignments?, fetchedAssignments: Assignments) {
        whenever(experimentStore.getCachedAssignments()).thenReturn(cachedAssignments)
        whenever(experimentStore.fetchAssignments(any(), any(), anyOrNull()))
                .thenReturn(OnAssignmentsFetched(fetchedAssignments))
    }

    private fun buildAssignments(
        isStale: Boolean = false,
        variations: Map<String, Variation> = emptyMap()
    ): Assignments {
        val now = System.currentTimeMillis()
        val oneHourAgo = now - ONE_HOUR_IN_SECONDS * 1000
        val oneHourFromNow = now + ONE_HOUR_IN_SECONDS * 1000
        return if (isStale) {
            Assignments(variations, ONE_HOUR_IN_SECONDS, Date(oneHourAgo))
        } else {
            Assignments(variations, ONE_HOUR_IN_SECONDS, Date(oneHourFromNow))
        }
    }

    companion object {
        private const val ONE_HOUR_IN_SECONDS = 3600
        private const val DUMMY_ANON_ID = "dummy_anon_id"
        private const val DUMMY_EXPERIMENT_NAME = "dummy"
    }
}
