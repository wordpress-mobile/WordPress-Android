package org.wordpress.android.models

fun ReaderTagList.containsFollowingTag(): Boolean =
    find { it.isFollowedSites || it.isDefaultInMemoryTag } != null
