package org.wordpress.android

import com.nhaarman.mockitokotlin2.internal.createInstance
import org.mockito.Mockito

/**
 * This method allows you to match a nullable parameter in mocked methods
 */
inline fun <reified T : Any> anyNullable(): T? {
    return Mockito.any(T::class.java) ?: createInstance()
}
