package org.wordpress.android.ui.posts.editor

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.util.WPPermissionUtils

@RunWith(MockitoJUnitRunner::class)
class EditorCameraHelperTest {
    private var cameraLaunched = false
    private val launchCamera: () -> Unit = { cameraLaunched = true }

    @Test
    fun `handlePermissionResult launches camera when requestCode matches and permissions granted`() {
        // Arrange
        cameraLaunched = false
        val requestCode = WPPermissionUtils.AZTEC_EDITOR_CAMERA_PERMISSION_REQUEST_CODE

        // Act
        val handled = EditorCameraHelper.handlePermissionResult(
            requestCode = requestCode,
            allGranted = true,
            launchCamera = launchCamera
        )

        // Assert
        assertThat(handled).isTrue()
        assertThat(cameraLaunched).isTrue()
    }

    @Test
    fun `handlePermissionResult does not launch camera when requestCode matches but permissions denied`() {
        // Arrange
        cameraLaunched = false
        val requestCode = WPPermissionUtils.AZTEC_EDITOR_CAMERA_PERMISSION_REQUEST_CODE

        // Act
        val handled = EditorCameraHelper.handlePermissionResult(
            requestCode = requestCode,
            allGranted = false,
            launchCamera = launchCamera
        )

        // Assert
        assertThat(handled).isTrue()
        assertThat(cameraLaunched).isFalse()
    }

    @Test
    fun `handlePermissionResult returns false when requestCode does not match`() {
        // Arrange
        cameraLaunched = false
        val differentRequestCode = 999

        // Act
        val handled = EditorCameraHelper.handlePermissionResult(
            requestCode = differentRequestCode,
            allGranted = true,
            launchCamera = launchCamera
        )

        // Assert
        assertThat(handled).isFalse()
        assertThat(cameraLaunched).isFalse()
    }

    @Test
    fun `handlePermissionResult does not launch camera when requestCode does not match even if granted`() {
        // Arrange
        cameraLaunched = false
        val differentRequestCode = WPPermissionUtils.EDITOR_MEDIA_PERMISSION_REQUEST_CODE

        // Act
        val handled = EditorCameraHelper.handlePermissionResult(
            requestCode = differentRequestCode,
            allGranted = true,
            launchCamera = launchCamera
        )

        // Assert
        assertThat(handled).isFalse()
        assertThat(cameraLaunched).isFalse()
    }

    @Test
    fun `handlePermissionResult returns true for camera request code regardless of grant status`() {
        // Arrange
        val requestCode = WPPermissionUtils.AZTEC_EDITOR_CAMERA_PERMISSION_REQUEST_CODE

        // Act & Assert - granted
        val handledWhenGranted = EditorCameraHelper.handlePermissionResult(
            requestCode = requestCode,
            allGranted = true,
            launchCamera = {}
        )
        assertThat(handledWhenGranted).isTrue()

        // Act & Assert - denied
        val handledWhenDenied = EditorCameraHelper.handlePermissionResult(
            requestCode = requestCode,
            allGranted = false,
            launchCamera = {}
        )
        assertThat(handledWhenDenied).isTrue()
    }
}
