package org.wordpress.android.designsystem

import org.wordpress.android.R

object DesignSystemDataSource {
    val startScreenButtonOptions = listOf(
        Pair(R.string.design_system_foundation, DesignSystemScreen.Foundation.name),
        Pair(R.string.design_system_components, DesignSystemScreen.Components.name),
    )
    val foundationScreenButtonOptions = listOf(
        R.string.design_system_foundation_colors,
        R.string.design_system_foundation_fonts,
        R.string.design_system_foundation_Lengths
    )
    val componentsScreenButtonOptions = listOf(
        R.string.design_system_components_dsbutton
    )
}
