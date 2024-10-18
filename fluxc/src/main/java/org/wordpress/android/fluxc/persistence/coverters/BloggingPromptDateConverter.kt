package org.wordpress.android.fluxc.persistence.coverters

import androidx.room.TypeConverter
import org.wordpress.android.fluxc.network.rest.wpcom.bloggingprompts.BloggingPromptsUtils
import java.util.Date

/**
 * A Room type converter for Blogging Prompt dates in YYYY-MM-DD format
 */
class BloggingPromptDateConverter {
    @TypeConverter
    fun stringToDate(value: String?) = value?.let { BloggingPromptsUtils.stringToDate(it) }

    @TypeConverter
    fun dateToString(date: Date?) = date?.let { BloggingPromptsUtils.dateToString(it) }
}
