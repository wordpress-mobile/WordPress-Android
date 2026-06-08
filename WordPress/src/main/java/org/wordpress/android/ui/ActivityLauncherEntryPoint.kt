package org.wordpress.android.ui

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.wordpress.android.ui.prefs.experimentalfeatures.ExperimentalFeatures

/**
 * Hilt entry point for static [ActivityLauncher] helpers that need access to singleton
 * dependencies. Used to gate activity routing decisions (e.g. the modern pages list flag)
 * without threading dependencies through every caller.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface ActivityLauncherEntryPoint {
    fun experimentalFeatures(): ExperimentalFeatures
}
