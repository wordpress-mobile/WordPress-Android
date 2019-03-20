package org.wordpress.android.fluxc.model.list

sealed class AuthorFilter {
    object Everyone : AuthorFilter()
    data class SpecificAuthor(val authorId: Long) : AuthorFilter()

    fun getValue(): Long = when (this) {
        Everyone -> -1
        is SpecificAuthor -> this.authorId
    }
}
