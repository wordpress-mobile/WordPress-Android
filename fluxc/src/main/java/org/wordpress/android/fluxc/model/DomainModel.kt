package org.wordpress.android.fluxc.model

import org.wordpress.android.fluxc.network.rest.wpcom.site.Domain

/**
 * Lightweight version of the [Domain] to be stored in the database.
 */
data class DomainModel(
    val domain: String,
    val primaryDomain: Boolean,
    val wpcomDomain: Boolean
)

fun Domain.asDomainModel() = DomainModel(
    domain = domain.orEmpty(),
    primaryDomain = primaryDomain,
    wpcomDomain = wpcomDomain
)
