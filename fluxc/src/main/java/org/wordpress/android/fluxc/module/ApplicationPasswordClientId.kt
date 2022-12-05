package org.wordpress.android.fluxc.module

import javax.inject.Qualifier
import kotlin.annotation.AnnotationRetention.RUNTIME

/**
 * Defines the name to use for naming the WordPress "application passwords" that the app
 * will create.
 */
@Qualifier
@MustBeDocumented
@Retention(RUNTIME)
annotation class ApplicationPasswordClientId
