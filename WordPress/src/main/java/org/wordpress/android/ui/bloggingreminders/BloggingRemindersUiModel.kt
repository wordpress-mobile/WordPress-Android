package org.wordpress.android.ui.bloggingreminders

import java.time.DayOfWeek

data class BloggingRemindersUiModel(val siteId: Int, val enabledDays: Set<DayOfWeek> = setOf())
