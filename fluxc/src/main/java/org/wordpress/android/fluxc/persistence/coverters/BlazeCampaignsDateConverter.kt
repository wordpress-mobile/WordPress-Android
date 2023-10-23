package org.wordpress.android.fluxc.persistence.coverters

import androidx.room.TypeConverter
import org.wordpress.android.fluxc.network.rest.wpcom.blaze.BlazeCampaignsUtils
import java.util.Date

class BlazeCampaignsDateConverter {
    @TypeConverter
    fun stringToDate(value: String?) = value?.let { BlazeCampaignsUtils.stringToDate(it) }

    @TypeConverter
    fun dateToString(date: Date?) = date?.let { BlazeCampaignsUtils.dateToString(it) }
}
