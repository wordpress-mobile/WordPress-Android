package org.wordpress.android.ui.mediapicker

import android.content.Intent
import android.os.Bundle
import androidx.annotation.StringRes
import org.wordpress.android.R

data class MediaPickerSetup(
    val dataSource: DataSource,
    val canMultiselect: Boolean,
    val allowedTypes: Set<MediaType>,
    val cameraEnabled: Boolean,
    @StringRes val title: Int
) {
    enum class DataSource {
        DEVICE, WP_LIBRARY, STOCK_LIBRARY, GIF_LIBRARY
    }

    fun toBundle(bundle: Bundle) {
        bundle.putSerializable(KEY_DATA_SOURCE, dataSource)
        bundle.putStringArrayList(KEY_ALLOWED_TYPES, ArrayList(allowedTypes.map { it.name }))
        bundle.putBoolean(KEY_CAN_MULTISELECT, canMultiselect)
        bundle.putBoolean(KEY_CAMERA_ENABLED, cameraEnabled)
        bundle.putInt(KEY_TITLE, title)
    }

    fun toIntent(intent: Intent) {
        intent.putExtra(KEY_DATA_SOURCE, dataSource)
        intent.putStringArrayListExtra(KEY_ALLOWED_TYPES, ArrayList(allowedTypes.map { it.name }))
        intent.putExtra(KEY_CAN_MULTISELECT, canMultiselect)
        intent.putExtra(KEY_CAMERA_ENABLED, cameraEnabled)
        intent.putExtra(KEY_TITLE, title)
    }

    companion object {
        private const val KEY_DATA_SOURCE = "key_data_source"
        private const val KEY_CAN_MULTISELECT = "key_can_multiselect"
        private const val KEY_ALLOWED_TYPES = "key_allowed_types"
        private const val KEY_CAMERA_ENABLED = "key_camera_enabled"
        private const val KEY_TITLE = "key_title"

        fun fromBundle(bundle: Bundle): MediaPickerSetup {
            val dataSource = bundle.getSerializable(KEY_DATA_SOURCE) as DataSource
            val allowedTypes = (bundle.getStringArrayList(KEY_ALLOWED_TYPES) ?: listOf<String>()).map {
                MediaType.valueOf(
                        it
                )
            }.toSet()
            val multipleSelectionAllowed = bundle.getBoolean(KEY_CAN_MULTISELECT)
            val cameraAllowed = bundle.getBoolean(KEY_CAMERA_ENABLED)
            val title = bundle.getInt(KEY_TITLE)
            return MediaPickerSetup(
                    dataSource,
                    multipleSelectionAllowed,
                    allowedTypes,
                    cameraAllowed,
                    title
            )
        }

        fun fromIntent(intent: Intent): MediaPickerSetup {
            val dataSource = intent.getSerializableExtra(KEY_DATA_SOURCE) as DataSource
            val allowedTypes = (intent.getStringArrayListExtra(KEY_ALLOWED_TYPES) ?: listOf<String>()).map {
                MediaType.valueOf(
                        it
                )
            }.toSet()
            val multipleSelectionAllowed = intent.getBooleanExtra(KEY_CAN_MULTISELECT, false)
            val cameraAllowed = intent.getBooleanExtra(KEY_CAMERA_ENABLED, false)
            val title = intent.getIntExtra(KEY_TITLE, R.string.photo_picker_photo_or_video_title)
            return MediaPickerSetup(
                    dataSource,
                    multipleSelectionAllowed,
                    allowedTypes,
                    cameraAllowed,
                    title
            )
        }
    }
}
