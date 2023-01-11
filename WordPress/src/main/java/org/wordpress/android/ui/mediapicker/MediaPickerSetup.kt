package org.wordpress.android.ui.mediapicker

import android.content.Intent
import android.os.Bundle
import androidx.annotation.StringRes
import org.wordpress.android.R

data class MediaPickerSetup(
    val primaryDataSource: DataSource,
    val availableDataSources: Set<DataSource>,
    val canMultiselect: Boolean,
    val requiresStoragePermissions: Boolean,
    val allowedTypes: Set<MediaType>,
    val cameraSetup: CameraSetup,
    val systemPickerEnabled: Boolean,
    val editingEnabled: Boolean,
    val queueResults: Boolean,
    val defaultSearchView: Boolean,
    @StringRes val title: Int
) {
    enum class DataSource {
        DEVICE, WP_LIBRARY, STOCK_LIBRARY, GIF_LIBRARY
    }

    enum class CameraSetup {
        STORIES, ENABLED, HIDDEN
    }

    fun toBundle(bundle: Bundle) {
        bundle.putInt(KEY_PRIMARY_DATA_SOURCE, primaryDataSource.ordinal)
        bundle.putIntegerArrayList(KEY_AVAILABLE_DATA_SOURCES, ArrayList(availableDataSources.map { it.ordinal }))
        bundle.putIntegerArrayList(KEY_ALLOWED_TYPES, ArrayList(allowedTypes.map { it.ordinal }))
        bundle.putBoolean(KEY_CAN_MULTISELECT, canMultiselect)
        bundle.putBoolean(KEY_REQUIRES_STORAGE_PERMISSIONS, requiresStoragePermissions)
        bundle.putInt(KEY_CAMERA_SETUP, cameraSetup.ordinal)
        bundle.putBoolean(KEY_SYSTEM_PICKER_ENABLED, systemPickerEnabled)
        bundle.putBoolean(KEY_EDITING_ENABLED, editingEnabled)
        bundle.putBoolean(KEY_QUEUE_RESULTS, queueResults)
        bundle.putBoolean(KEY_DEFAULT_SEARCH_VIEW, defaultSearchView)
        bundle.putInt(KEY_TITLE, title)
    }

    fun toIntent(intent: Intent) {
        intent.putExtra(KEY_PRIMARY_DATA_SOURCE, primaryDataSource.ordinal)
        intent.putIntegerArrayListExtra(KEY_AVAILABLE_DATA_SOURCES, ArrayList(availableDataSources.map { it.ordinal }))
        intent.putIntegerArrayListExtra(KEY_ALLOWED_TYPES, ArrayList(allowedTypes.map { it.ordinal }))
        intent.putExtra(KEY_CAN_MULTISELECT, canMultiselect)
        intent.putExtra(KEY_REQUIRES_STORAGE_PERMISSIONS, requiresStoragePermissions)
        intent.putExtra(KEY_CAMERA_SETUP, cameraSetup.ordinal)
        intent.putExtra(KEY_SYSTEM_PICKER_ENABLED, systemPickerEnabled)
        intent.putExtra(KEY_EDITING_ENABLED, editingEnabled)
        intent.putExtra(KEY_QUEUE_RESULTS, queueResults)
        intent.putExtra(KEY_DEFAULT_SEARCH_VIEW, defaultSearchView)
        intent.putExtra(KEY_TITLE, title)
    }

    companion object {
        private const val KEY_PRIMARY_DATA_SOURCE = "key_primary_data_source"
        private const val KEY_AVAILABLE_DATA_SOURCES = "key_available_data_sources"
        private const val KEY_CAN_MULTISELECT = "key_can_multiselect"
        private const val KEY_REQUIRES_STORAGE_PERMISSIONS = "key_requires_storage_permissions"
        private const val KEY_ALLOWED_TYPES = "key_allowed_types"
        private const val KEY_CAMERA_SETUP = "key_camera_setup"
        private const val KEY_SYSTEM_PICKER_ENABLED = "key_system_picker_enabled"
        private const val KEY_EDITING_ENABLED = "key_editing_enabled"
        private const val KEY_QUEUE_RESULTS = "key_queue_results"
        private const val KEY_DEFAULT_SEARCH_VIEW = "key_default_search_view"
        private const val KEY_TITLE = "key_title"

        fun fromBundle(bundle: Bundle): MediaPickerSetup {
            val dataSource = DataSource.values()[bundle.getInt(KEY_PRIMARY_DATA_SOURCE)]
            val availableDataSources = (bundle.getIntegerArrayList(KEY_AVAILABLE_DATA_SOURCES) ?: listOf<Int>()).map {
                DataSource.values()[it]
            }.toSet()
            val allowedTypes = (bundle.getIntegerArrayList(KEY_ALLOWED_TYPES) ?: listOf<Int>()).map {
                MediaType.values()[it]
            }.toSet()
            val multipleSelectionAllowed = bundle.getBoolean(KEY_CAN_MULTISELECT)
            val cameraSetup = CameraSetup.values()[bundle.getInt(KEY_CAMERA_SETUP)]
            val requiresStoragePermissions = bundle.getBoolean(KEY_REQUIRES_STORAGE_PERMISSIONS)
            val systemPickerEnabled = bundle.getBoolean(KEY_SYSTEM_PICKER_ENABLED)
            val editingEnabled = bundle.getBoolean(KEY_EDITING_ENABLED)
            val queueResults = bundle.getBoolean(KEY_QUEUE_RESULTS)
            val defaultSearchView = bundle.getBoolean(KEY_DEFAULT_SEARCH_VIEW)
            val title = bundle.getInt(KEY_TITLE)
            return MediaPickerSetup(
                dataSource,
                availableDataSources,
                multipleSelectionAllowed,
                requiresStoragePermissions,
                allowedTypes,
                cameraSetup,
                systemPickerEnabled,
                editingEnabled,
                queueResults,
                defaultSearchView,
                title
            )
        }

        fun fromIntent(intent: Intent): MediaPickerSetup {
            val dataSource = DataSource.values()[intent.getIntExtra(KEY_PRIMARY_DATA_SOURCE, -1)]
            val availableDataSources = (intent.getIntegerArrayListExtra(KEY_AVAILABLE_DATA_SOURCES)
                ?: listOf<Int>()).map {
                DataSource.values()[it]
            }.toSet()
            val allowedTypes = (intent.getIntegerArrayListExtra(KEY_ALLOWED_TYPES) ?: listOf<Int>()).map {
                MediaType.values()[it]
            }.toSet()
            val multipleSelectionAllowed = intent.getBooleanExtra(KEY_CAN_MULTISELECT, false)
            val cameraSetup = CameraSetup.values()[intent.getIntExtra(KEY_CAMERA_SETUP, -1)]
            val requiresStoragePermissions = intent.getBooleanExtra(KEY_REQUIRES_STORAGE_PERMISSIONS, false)
            val systemPickerEnabled = intent.getBooleanExtra(KEY_SYSTEM_PICKER_ENABLED, false)
            val editingEnabled = intent.getBooleanExtra(KEY_SYSTEM_PICKER_ENABLED, false)
            val queueResults = intent.getBooleanExtra(KEY_QUEUE_RESULTS, false)
            val defaultSearchView = intent.getBooleanExtra(KEY_DEFAULT_SEARCH_VIEW, false)
            val title = intent.getIntExtra(KEY_TITLE, R.string.photo_picker_photo_or_video_title)
            return MediaPickerSetup(
                dataSource,
                availableDataSources,
                multipleSelectionAllowed,
                requiresStoragePermissions,
                allowedTypes,
                cameraSetup,
                systemPickerEnabled,
                editingEnabled,
                queueResults,
                defaultSearchView,
                title
            )
        }
    }
}
