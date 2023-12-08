package org.wordpress.android.editor.savedinstance

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class TestParcelable(val data: String) : Parcelable
