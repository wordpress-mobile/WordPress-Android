package org.wordpress.android.util.experiments

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.TEST_SCOPE
import org.wordpress.android.fluxc.model.experiments.Assignments
import org.wordpress.android.fluxc.model.experiments.Variation
import org.wordpress.android.fluxc.store.ExperimentStore
import org.wordpress.android.fluxc.store.ExperimentStore.OnAssignmentsFetched
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.test
import java.util.Date

@RunWith(MockitoJUnitRunner::class)
class ExPlatTest : BaseUnitTest() {
    @Mock lateinit var experimentStore: ExperimentStore
    @Mock lateinit var appLog: AppLogWrapper
    private lateinit var exPlat: ExPlat
    private lateinit var dummyExperiment: Experiment

    @Before
    fun setUp() {
        exPlat = ExPlat(experimentStore, appLog, TEST_SCOPE)
        dummyExperiment = object : Experiment("dummy", exPlat) {}
    }

    @Test
    fun `refresh fetches assignments if cache is null`() = test {
        setupAssignments(cachedAssignments = null, fetchedAssignments = buildAssignments())

        exPlat.refresh()

        verify(experimentStore, times(1)).fetchAssignments(any())
    }

    @Test
    fun `refresh fetches assignments if cache is stale`() = test {
        setupAssignments(cachedAssignments = buildAssignments(isStale = true), fetchedAssignments = buildAssignments())

        exPlat.refresh()

        verify(experimentStore, times(1)).fetchAssignments(any())
    }

    @Test
    fun `refresh does not fetch assignments if cache is fresh`() = test {
        setupAssignments(cachedAssignments = buildAssignments(isStale = false), fetchedAssignments = buildAssignments())

        exPlat.refresh()

        verify(experimentStore, never()).fetchAssignments(any())
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

        verify(experimentStore, times(1)).fetchAssignments(any())
    }

    @Test
    fun `getVariation fetches assignments if cache is stale`() = test {
        setupAssignments(cachedAssignments = buildAssignments(isStale = true), fetchedAssignments = buildAssignments())

        exPlat.getVariation(dummyExperiment,  shouldRefreshIfStale = true)

        verify(experimentStore, times(1)).fetchAssignments(any())
    }

    @Test
    fun `getVariation does not fetch assignments if cache is fresh`() = test {
        setupAssignments(cachedAssignments = buildAssignments(isStale = false), fetchedAssignments = buildAssignments())

        exPlat.getVariation(dummyExperiment,  shouldRefreshIfStale = true)

        verify(experimentStore, never()).fetchAssignments(any())
    }

    @Test
    fun `getVariation does not fetch assignments if cache is null but shouldRefreshIfStale is false`() = test {
        setupAssignments(cachedAssignments = null, fetchedAssignments = buildAssignments())

        exPlat.getVariation(dummyExperiment,  shouldRefreshIfStale = false)

        verify(experimentStore, never()).fetchAssignments(any())
    }

    @Test
    fun `getVariation does not fetch assignments if cache is stale but shouldRefreshIfStale is false`() = test {
        setupAssignments(cachedAssignments = null, fetchedAssignments = buildAssignments())

        exPlat.getVariation(dummyExperiment,  shouldRefreshIfStale = false)

        verify(experimentStore, never()).fetchAssignments(any())
    }

    private suspend fun setupAssignments(cachedAssignments: Assignments?, fetchedAssignments: Assignments) {
        whenever(experimentStore.getCachedAssignments()).thenReturn(cachedAssignments)
        whenever(experimentStore.fetchAssignments(any())).thenReturn(OnAssignmentsFetched(fetchedAssignments))
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
    }
}
