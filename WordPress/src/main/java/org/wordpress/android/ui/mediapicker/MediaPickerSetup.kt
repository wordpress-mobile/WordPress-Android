package org.wordpress.android.ui.mediapicker

import android.content.Intent
import android.os.Bundle
import androidx.annotation.StringRes
import org.wordpress.android.R

data class MediaPickerSetup(
    val dataSource: DataSource,
    val canMultiselect: Boolean,
    val requiresStoragePermissions: Boolean,
    val allowedTypes: Set<MediaType>,
    val cameraEnabled: Boolean,
    val systemPickerEnabled: Boolean,
    val editingEnabled: Boolean,
    val queueResults: Boolean,
    val defaultSearchView: Boolean,
    @StringRes val title: Int
) {
    enum class DataSource {
        DEVICE, WP_LIBRARY, STOCK_LIBRARY, GIF_LIBRARY
    }

    fun toBundle(bundle: Bundle) {
        bundle.putSerializable(KEY_DATA_SOURCE, dataSource)
        bundle.putStringArrayList(KEY_ALLOWED_TYPES, ArrayList(allowedTypes.map { it.name }))
        bundle.putBoolean(KEY_CAN_MULTISELECT, canMultiselect)
        bundle.putBoolean(KEY_REQUIRES_STORAGE_PERMISSIONS, requiresStoragePermissions)
        bundle.putBoolean(KEY_CAMERA_ENABLED, cameraEnabled)
        bundle.putBoolean(KEY_SYSTEM_PICKER_ENABLED, systemPickerEnabled)
        bundle.putBoolean(KEY_EDITING_ENABLED, editingEnabled)
        bundle.putBoolean(KEY_QUEUE_RESULTS, queueResults)
        bundle.putBoolean(KEY_DEFAULT_SEARCH_VIEW, defaultSearchView)
        bundle.putInt(KEY_TITLE, title)
    }

    fun toIntent(intent: Intent) {
        intent.putExtra(KEY_DATA_SOURCE, dataSource)
        intent.putStringArrayListExtra(KEY_ALLOWED_TYPES, ArrayList(allowedTypes.map { it.name }))
        intent.putExtra(KEY_CAN_MULTISELECT, canMultiselect)
        intent.putExtra(KEY_REQUIRES_STORAGE_PERMISSIONS, requiresStoragePermissions)
        intent.putExtra(KEY_CAMERA_ENABLED, cameraEnabled)
        intent.putExtra(KEY_SYSTEM_PICKER_ENABLED, systemPickerEnabled)
        intent.putExtra(KEY_EDITING_ENABLED, editingEnabled)
        intent.putExtra(KEY_QUEUE_RESULTS, queueResults)
        intent.putExtra(KEY_DEFAULT_SEARCH_VIEW, defaultSearchView)
        intent.putExtra(KEY_TITLE, title)
    }

    companion object {
        private const val KEY_DATA_SOURCE = "key_data_source"
        private const val KEY_CAN_MULTISELECT = "key_can_multiselect"
        private const val KEY_REQUIRES_STORAGE_PERMISSIONS = "key_requires_storage_permissions"
        private const val KEY_ALLOWED_TYPES = "key_allowed_types"
        private const val KEY_CAMERA_ENABLED = "key_camera_enabled"
        private const val KEY_SYSTEM_PICKER_ENABLED = "key_system_picker_enabled"
        private const val KEY_EDITING_ENABLED = "key_editing_enabled"
        private const val KEY_QUEUE_RESULTS = "key_queue_results"
        private const val KEY_DEFAULT_SEARCH_VIEW = "key_default_search_view"
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
            val requiresStoragePermissions = bundle.getBoolean(KEY_REQUIRES_STORAGE_PERMISSIONS)
            val systemPickerEnabled = bundle.getBoolean(KEY_SYSTEM_PICKER_ENABLED)
            val editingEnabled = bundle.getBoolean(KEY_EDITING_ENABLED)
            val queueResults = bundle.getBoolean(KEY_QUEUE_RESULTS)
            val defaultSearchView = bundle.getBoolean(KEY_DEFAULT_SEARCH_VIEW)
            val title = bundle.getInt(KEY_TITLE)
            return MediaPickerSetup(
                    dataSource,
                    multipleSelectionAllowed,
                    requiresStoragePermissions,
                    allowedTypes,
                    cameraAllowed,
                    systemPickerEnabled,
                    editingEnabled,
                    queueResults,
                    defaultSearchView,
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
            val requiresStoragePermissions = intent.getBooleanExtra(KEY_REQUIRES_STORAGE_PERMISSIONS, false)
            val systemPickerEnabled = intent.getBooleanExtra(KEY_SYSTEM_PICKER_ENABLED, false)
            val editingEnabled = intent.getBooleanExtra(KEY_SYSTEM_PICKER_ENABLED, false)
            val queueResults = intent.getBooleanExtra(KEY_QUEUE_RESULTS, false)
            val defaultSearchView = intent.getBooleanExtra(KEY_DEFAULT_SEARCH_VIEW, false)
            val title = intent.getIntExtra(KEY_TITLE, R.string.photo_picker_photo_or_video_title)
            return MediaPickerSetup(
                    dataSource,
                    multipleSelectionAllowed,
                    requiresStoragePermissions,
                    allowedTypes,
                    cameraAllowed,
                    systemPickerEnabled,
                    editingEnabled,
                    queueResults,
                    defaultSearchView,
                    title
            )
        }
    }
}
