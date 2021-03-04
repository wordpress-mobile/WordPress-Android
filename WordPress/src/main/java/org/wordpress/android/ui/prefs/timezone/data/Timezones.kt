package org.wordpress.android.ui.prefs.timezone.data

import com.google.gson.annotations.SerializedName

data class Timezones(
    @SerializedName("found")
    val found: Int,
    @SerializedName("manual_utc_offsets")
    val manualUtcOffsets: List<Timezone>,
    @SerializedName("timezones")
    val timezones: List<Timezone>,
    @SerializedName("timezones_by_continent")
    val timezonesByContinent: TimezonesByContinent
)

data class Timezone(
    @SerializedName("label")
    val label: String,
    @SerializedName("value")
    val value: String
)

data class TimezonesByContinent(
    @SerializedName("Africa")
    val africa: List<Timezone>,
    @SerializedName("America")
    val america: List<Timezone>,
    @SerializedName("Antarctica")
    val antarctica: List<Timezone>,
    @SerializedName("Arctic")
    val arctic: List<Timezone>,
    @SerializedName("Asia")
    val asia: List<Timezone>,
    @SerializedName("Atlantic")
    val atlantic: List<Timezone>,
    @SerializedName("Australia")
    val australia: List<Timezone>,
    @SerializedName("Europe")
    val europe: List<Timezone>,
    @SerializedName("Indian")
    val indian: List<Timezone>,
    @SerializedName("Pacific")
    val pacific: List<Timezone>
)
