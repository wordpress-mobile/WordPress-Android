package org.wordpress.android.ui.accounts.applicationpassword

import java.util.Date

data class ApplicationPasswordWithViewContext(
    val uuid: String,
    val name: String,
    val lastUsed: Date?
)
