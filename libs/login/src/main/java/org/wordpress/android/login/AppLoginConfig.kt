package org.wordpress.android.login

/**
 * Configuration interface for app-specific login behavior.
 * Implemented by the app module to provide app-specific settings to the login library.
 */
interface AppLoginConfig {
    /**
     * Returns true if this is the Jetpack app, false for WordPress app.
     * Used to determine site filtering behavior during login.
     */
    val isJetpackApp: Boolean
}
