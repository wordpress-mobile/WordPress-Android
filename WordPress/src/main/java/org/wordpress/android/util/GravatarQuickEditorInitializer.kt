package org.wordpress.android.util

import android.content.Context
import androidx.startup.AppInitializer
import androidx.startup.Initializer

/**
 * Initializes the Gravatar Quick Editor on demand.
 *
 * The library registers an androidx.startup initializer that builds an encrypted DataStore (AndroidKeyStore +
 * Tink self-test) on the main thread before Application.onCreate, which showed up in Play Console as background
 * ANRs on every process start. That initializer is removed in the manifest and run from here instead, only when
 * the editor is actually about to be used. [AppInitializer.initializeComponent] is idempotent and synchronized,
 * so this is safe to call repeatedly and from any thread.
 *
 * The initializer class is internal to the library, hence the lookup by name; startup-runtime's consumer R8
 * rules keep every [Initializer] and its no-arg constructor.
 */
object GravatarQuickEditorInitializer {
    const val LIBRARY_PACKAGE = "com.gravatar.quickeditor."
    private const val INITIALIZER_CLASS = LIBRARY_PACKAGE + "initializer.QuickEditorContainerInitializer"

    @Suppress("UNCHECKED_CAST")
    fun initialize(context: Context) {
        val initializer = Class.forName(INITIALIZER_CLASS) as Class<out Initializer<Any>>
        AppInitializer.getInstance(context).initializeComponent(initializer)
    }
}
