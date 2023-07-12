package org.wordpress.android.ui.compose.styles

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import org.wordpress.android.R

@Composable
fun smallTitle() = MaterialTheme.typography.bodyLarge.copy(
    fontWeight = FontWeight.Normal,
    fontSize = 14.sp,
    color = colorResource(R.color.material_on_surface_emphasis_high_type)
)

@Composable
fun subTitle() = MaterialTheme.typography.titleMedium.copy(
    fontWeight = FontWeight.Medium,
    fontStyle = FontStyle.Normal,
    color = colorResource(R.color.material_on_surface_emphasis_high_type)
)

@Composable
fun footerCTA() = MaterialTheme.typography.titleMedium.copy(
    fontStyle = FontStyle.Normal,
    color = colorResource(R.color.primary_emphasis_medium_selector)
)