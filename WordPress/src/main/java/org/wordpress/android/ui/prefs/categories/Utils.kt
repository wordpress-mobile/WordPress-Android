package org.wordpress.android.ui.prefs.categories

sealed class CategoryDetailNavigation
object CreateCategory : CategoryDetailNavigation()
data class EditCategory(val categoryId: Long) : CategoryDetailNavigation()
