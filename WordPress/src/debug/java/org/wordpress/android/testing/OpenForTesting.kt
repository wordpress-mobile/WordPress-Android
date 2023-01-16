package org.wordpress.android.testing

/**
 * This annotation allows us to open some classes in debug build only for mocking purposes while they are final in
 * release builds.
 */
@Target(AnnotationTarget.ANNOTATION_CLASS)
annotation class OpenClassAnnotation

/**
 * Annotate a class with [OpenForTesting] if you want it to be extendable in debug builds.
 */
@OpenClassAnnotation
@Target(AnnotationTarget.CLASS)
annotation class OpenForTesting
