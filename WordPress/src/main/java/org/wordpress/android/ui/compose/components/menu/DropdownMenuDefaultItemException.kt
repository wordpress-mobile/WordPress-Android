package org.wordpress.android.ui.compose.components.menu

class DropdownMenuDefaultItemException(
    override val message: String = "DropdownMenu must have one default item."
) : RuntimeException()
