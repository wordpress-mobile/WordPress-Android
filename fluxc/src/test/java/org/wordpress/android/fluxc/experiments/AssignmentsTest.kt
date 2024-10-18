package org.wordpress.android.fluxc.experiments

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.fluxc.model.experiments.Assignments
import org.wordpress.android.fluxc.model.experiments.AssignmentsModel
import org.wordpress.android.fluxc.model.experiments.Variation.Control
import org.wordpress.android.fluxc.model.experiments.Variation.Treatment
import java.lang.System.currentTimeMillis
import java.util.Date

@RunWith(MockitoJUnitRunner::class)
class AssignmentsTest {
    @Test
    fun `is stale if expiry time is before or equal current time`() {
        val now = currentTimeMillis()
        val oneHourAgo = now - ONE_HOUR_IN_SECONDS * 1000
        val assignments = Assignments(emptyMap(), ONE_HOUR_IN_SECONDS, Date(oneHourAgo))
        assertThat(assignments.isStale(Date(now))).isTrue
    }

    @Test
    fun `is not stale if expiry time is after current time`() {
        val now = currentTimeMillis()
        val oneHourFromNow = now + ONE_HOUR_IN_SECONDS * 1000
        val assignments = Assignments(emptyMap(), ONE_HOUR_IN_SECONDS, Date(oneHourFromNow))
        assertThat(assignments.isStale(Date(now))).isFalse
    }

    @Test
    fun `variation is control if experiment is not found`() {
        val model = AssignmentsModel(mapOf("test_experiment" to "treatment"))
        val assignments = Assignments.fromModel(model)
        val variation = assignments.getVariationForExperiment("other_experiment")
        assertThat(variation).isInstanceOf(Control::class.java)
    }

    @Test
    fun `null variation is mapped to control type`() {
        val model = AssignmentsModel(mapOf("test_experiment" to null))
        val assignments = Assignments.fromModel(model)
        val variation = assignments.getVariationForExperiment("test_experiment")
        assertThat(variation).isInstanceOf(Control::class.java)
    }

    @Test
    fun `treatment variation is mapped to treatment type with correct name`() {
        val model = AssignmentsModel(mapOf("test_experiment" to "treatment_name"))
        val assignments = Assignments.fromModel(model)
        val variation = assignments.getVariationForExperiment("test_experiment")
        assertThat(variation).isEqualTo(Treatment("treatment_name"))
    }

    companion object {
        private const val ONE_HOUR_IN_SECONDS = 3600
    }
}
