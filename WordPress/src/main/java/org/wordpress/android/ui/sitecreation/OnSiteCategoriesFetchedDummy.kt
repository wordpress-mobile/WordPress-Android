package org.wordpress.android.ui.sitecreation

import java.util.Random

class OnSiteCategoriesFetchedDummy(
    val error: Boolean = false,
    val data: List<String> = listOf("dummy" + Random().nextInt())
)
