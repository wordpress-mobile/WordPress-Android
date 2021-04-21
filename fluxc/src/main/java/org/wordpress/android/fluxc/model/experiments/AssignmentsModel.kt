package org.wordpress.android.fluxc.model.experiments

import org.wordpress.android.fluxc.model.experiments.Variation.Control
import java.lang.System.currentTimeMillis
import java.util.Date

data class AssignmentsModel(
    val variations: Map<String, String?> = emptyMap(),
    val ttl: Int = 0,
    val fetchedAt: Long = currentTimeMillis()
)

data class Assignments(
    val variations: Map<String, Variation> = emptyMap(),
    val ttl: Int = 0,
    val fetchedAt: Date = Date()
) {
    val expiresAt = Date(fetchedAt.time + (ttl * 1000))

    fun isStale(now: Date = Date()) = !now.before(expiresAt)

    fun getVariationForExperiment(experiment: String) = variations[experiment] ?: Control

    companion object {
        fun fromModel(model: AssignmentsModel) = Assignments(
                model.variations.mapValues { Variation.fromName(it.value) },
                model.ttl,
                Date(model.fetchedAt)
        )
    }
}

sealed class Variation(open val name: String) {
    object Control : Variation(CONTROL)
    data class Treatment(override val name: String) : Variation(name)
    companion object {
        private const val CONTROL = "control"
        fun fromName(name: String?) = when (name) {
            CONTROL, null -> Control
            else -> Treatment(name)
        }
    }
}
