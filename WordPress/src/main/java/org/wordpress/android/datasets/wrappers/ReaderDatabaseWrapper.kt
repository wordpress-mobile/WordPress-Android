package org.wordpress.android.datasets.wrappers

import dagger.Reusable
import org.wordpress.android.datasets.ReaderDatabase
import javax.inject.Inject

@Reusable
class ReaderDatabaseWrapper @Inject constructor() {
    fun reset(retainBookmarkedPosts: Boolean) = ReaderDatabase.reset(retainBookmarkedPosts)
}
