package org.wordpress.android.rules

/**
 * Annotation used to denote you want to retry a UI test function.
 *
 * @property numberOfTimes the number of times you want to retry the function, with a default of 1.
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class Retry(val numberOfTimes: Int = 1)
