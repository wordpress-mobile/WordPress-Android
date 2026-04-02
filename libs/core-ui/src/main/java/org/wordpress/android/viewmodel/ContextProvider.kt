package org.wordpress.android.viewmodel

import android.content.Context
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContextProvider
@Inject constructor(private var context: Context) {
    fun getContext(): Context = context
}
