package org.wordpress.android.fluxc.network

import javax.inject.Inject

sealed class AcceptHeaderStrategy(val header: String = ACCEPT_HEADER, open val value: String) {
    class JsonAcceptHeader @Inject constructor() : AcceptHeaderStrategy(value = APPLICATION_JSON_VALUE)

    companion object {
        private const val ACCEPT_HEADER = "Accept"
        private const val APPLICATION_JSON_VALUE = "application/json"
    }
}
