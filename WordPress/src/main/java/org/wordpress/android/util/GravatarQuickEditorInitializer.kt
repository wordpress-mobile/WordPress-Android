package org.wordpress.android.util

import android.content.Context
import androidx.startup.AppInitializer
import androidx.startup.Initializer
import org.wordpress.android.util.AppLog.T

/**
 * Initializes the Gravatar Quick Editor on demand.
 *
 * The library registers an androidx.startup initializer that builds an encrypted DataStore (AndroidKeyStore +
 * Tink self-test) on the main thread before Application.onCreate, which showed up in Play Console as background
 * ANRs on every process start. That initializer is removed in the manifest and run from here instead, only when
 * the editor is actually about to be used. [AppInitializer.initializeComponent] is idempotent and synchronized,
 * so this is safe to call repeatedly and from any thread.
 *
 * The initializer class is internal to the library, hence the lookup by name. WordPress/proguard.cfg keeps the
 * class and its no-arg constructor so the lookup survives R8.
 *
 * TODO: remove this (and the manifest override) once gravatar-android builds its container lazily or exposes a
 *  public initialize(context) entry point.
 */
object GravatarQuickEditorInitializer {
    private const val INITIALIZER_CLASS = "com.gravatar.quickeditor.initializer.QuickEditorContainerInitializer"

    /**
     * Runs the library initializer, returning false (after logging) if it could not be run. Callers must not
     * open the editor in that case, since it would throw on its uninitialized container.
     */
    @Suppress("UNCHECKED_CAST", "TooGenericExceptionCaught")
    fun initialize(context: Context): Boolean = try {
        val initializer = Class.forName(INITIALIZER_CLASS) as Class<out Initializer<Any>>
        AppInitializer.getInstance(context).initializeComponent(initializer)
        true
    } catch (e: Exception) {
        AppLog.e(T.MAIN, "Failed to initialize the Gravatar quick editor", e)
        false
    } catch (e: LinkageError) {
        // e.g. NoClassDefFoundError if a library bump moves the class; an Error, so not covered above
        AppLog.e(T.MAIN, "Failed to initialize the Gravatar quick editor", e)
        false
    }
}
