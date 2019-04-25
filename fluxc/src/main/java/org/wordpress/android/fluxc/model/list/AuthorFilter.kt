package org.wordpress.android.fluxc.model.list

sealed class AuthorFilter {
    object Everyone : AuthorFilter()
    data class SpecificAuthor(val authorId: Long) : AuthorFilter()
}
