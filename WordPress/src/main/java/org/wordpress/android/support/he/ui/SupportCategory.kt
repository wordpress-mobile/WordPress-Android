package org.wordpress.android.support.he.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import org.wordpress.android.R

enum class SupportCategory(val icon: ImageVector, val labelRes: Int) {
    APPLICATION(Icons.Default.PhoneAndroid, R.string.he_support_category_application),
    JETPACK_CONNECTION(Icons.Default.Settings, R.string.he_support_category_jetpack_connection),
    SITE_MANAGEMENT(Icons.Default.Language, R.string.he_support_category_site_management),
    BILLING(Icons.Default.CreditCard, R.string.he_support_category_billing),
    TECHNICAL_ISSUES(Icons.Default.Settings, R.string.he_support_category_technical_issues),
    OTHER(Icons.AutoMirrored.Filled.Help, R.string.he_support_category_other)
}
