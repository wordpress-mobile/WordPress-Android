package org.wordpress.android.fluxc.experiments

import android.content.SharedPreferences
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.inOrder
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.experiments.Assignments
import org.wordpress.android.fluxc.model.experiments.AssignmentsModel
import org.wordpress.android.fluxc.network.rest.wpcom.experiments.ExperimentRestClient
import org.wordpress.android.fluxc.store.ExperimentStore
import org.wordpress.android.fluxc.store.ExperimentStore.Companion.EXPERIMENT_ASSIGNMENTS_KEY
import org.wordpress.android.fluxc.store.ExperimentStore.FetchedAssignmentsPayload
import org.wordpress.android.fluxc.store.ExperimentStore.OnAssignmentsFetched
import org.wordpress.android.fluxc.store.ExperimentStore.Platform.CALYPSO
import org.wordpress.android.fluxc.test
import org.wordpress.android.fluxc.tools.initCoroutineEngine
import org.wordpress.android.fluxc.utils.PreferenceUtils.PreferenceUtilsWrapper

@RunWith(MockitoJUnitRunner::class)
class ExperimentStoreTest {
    @Mock private lateinit var experimentRestClient: ExperimentRestClient
    @Mock private lateinit var preferenceUtils: PreferenceUtilsWrapper
    @Mock private lateinit var sharedPreferences: SharedPreferences
    @Mock private lateinit var sharedPreferencesEditor: SharedPreferences.Editor

    private lateinit var experimentStore: ExperimentStore

    @Before
    fun setUp() {
        whenever(preferenceUtils.getFluxCPreferences()).thenReturn(sharedPreferences)
        whenever(sharedPreferences.edit()).thenReturn(sharedPreferencesEditor)
        whenever(sharedPreferencesEditor.putString(any(), any())).thenReturn(sharedPreferencesEditor)

        experimentStore = ExperimentStore(experimentRestClient, preferenceUtils, initCoroutineEngine())
    }

    @Test
    fun `fetch assignments emits correct event when successful`() = test {
        whenever(experimentRestClient.fetchAssignments(defaultPlatform, emptyList())).thenReturn(successfulPayload)

        val onAssignmentsFetched = experimentStore.fetchAssignments(defaultPlatform, emptyList())

        assertThat(onAssignmentsFetched).isEqualTo(OnAssignmentsFetched(successfulAssignments))
    }

    @Test
    fun `fetch assignments stores result locally when successful`() = test {
        whenever(experimentRestClient.fetchAssignments(defaultPlatform, emptyList())).thenReturn(successfulPayload)

        experimentStore.fetchAssignments(defaultPlatform, emptyList())

        verify(sharedPreferences).edit()
        inOrder(sharedPreferencesEditor).apply {
            verify(sharedPreferencesEditor).putString(EXPERIMENT_ASSIGNMENTS_KEY, successfulModelJson)
            verify(sharedPreferencesEditor).apply()
        }
    }

    @Test
    fun `get cached assignments returns last fetch result when existent`() {
        whenever(sharedPreferences.getString(EXPERIMENT_ASSIGNMENTS_KEY, null)).thenReturn(successfulModelJson)

        val cachedAssignments = experimentStore.getCachedAssignments()

        assertThat(cachedAssignments).isNotNull
        assertThat(cachedAssignments).isEqualTo(successfulAssignments)
    }

    @Test
    fun `get cached assignments returns null when no fetch results were stored`() {
        whenever(sharedPreferences.getString(EXPERIMENT_ASSIGNMENTS_KEY, null)).thenReturn(null)

        val cachedAssignments = experimentStore.getCachedAssignments()

        assertThat(cachedAssignments).isNull()
    }

    companion object {
        val defaultPlatform = CALYPSO

        private val successfulVariations = mapOf(
                "experiment_one" to null,
                "experiment_two" to "treatment",
                "experiment_three" to "other"
        )

        private val successfulModel = AssignmentsModel(successfulVariations, 3600, 1604964458273)

        const val successfulModelJson = "{\"variations\":{" +
                "\"experiment_one\":null," +
                "\"experiment_two\":\"treatment\"," +
                "\"experiment_three\":\"other\"}," +
                "\"ttl\":3600," +
                "\"fetchedAt\":1604964458273}"

        val successfulPayload = FetchedAssignmentsPayload(successfulModel)

        val successfulAssignments = Assignments.fromModel(successfulModel)
    }
}
