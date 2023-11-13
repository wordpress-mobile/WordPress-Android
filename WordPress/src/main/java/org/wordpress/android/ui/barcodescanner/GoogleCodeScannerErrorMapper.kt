package org.wordpress.android.ui.barcodescanner

import com.google.mlkit.common.MlKitException
import com.google.mlkit.common.MlKitException.ABORTED
import com.google.mlkit.common.MlKitException.ALREADY_EXISTS
import com.google.mlkit.common.MlKitException.CANCELLED
import com.google.mlkit.common.MlKitException.CODE_SCANNER_APP_NAME_UNAVAILABLE
import com.google.mlkit.common.MlKitException.CODE_SCANNER_CAMERA_PERMISSION_NOT_GRANTED
import com.google.mlkit.common.MlKitException.CODE_SCANNER_CANCELLED
import com.google.mlkit.common.MlKitException.CODE_SCANNER_GOOGLE_PLAY_SERVICES_VERSION_TOO_OLD
import com.google.mlkit.common.MlKitException.CODE_SCANNER_PIPELINE_INFERENCE_ERROR
import com.google.mlkit.common.MlKitException.CODE_SCANNER_PIPELINE_INITIALIZATION_ERROR
import com.google.mlkit.common.MlKitException.CODE_SCANNER_TASK_IN_PROGRESS
import com.google.mlkit.common.MlKitException.CODE_SCANNER_UNAVAILABLE
import com.google.mlkit.common.MlKitException.DATA_LOSS
import com.google.mlkit.common.MlKitException.DEADLINE_EXCEEDED
import com.google.mlkit.common.MlKitException.FAILED_PRECONDITION
import com.google.mlkit.common.MlKitException.INTERNAL
import com.google.mlkit.common.MlKitException.INVALID_ARGUMENT
import com.google.mlkit.common.MlKitException.MODEL_HASH_MISMATCH
import com.google.mlkit.common.MlKitException.MODEL_INCOMPATIBLE_WITH_TFLITE
import com.google.mlkit.common.MlKitException.NETWORK_ISSUE
import com.google.mlkit.common.MlKitException.NOT_ENOUGH_SPACE
import com.google.mlkit.common.MlKitException.NOT_FOUND
import com.google.mlkit.common.MlKitException.OUT_OF_RANGE
import com.google.mlkit.common.MlKitException.PERMISSION_DENIED
import com.google.mlkit.common.MlKitException.RESOURCE_EXHAUSTED
import com.google.mlkit.common.MlKitException.UNAUTHENTICATED
import com.google.mlkit.common.MlKitException.UNAVAILABLE
import com.google.mlkit.common.MlKitException.UNIMPLEMENTED
import com.google.mlkit.common.MlKitException.UNKNOWN
import javax.inject.Inject

class GoogleCodeScannerErrorMapper @Inject constructor() {
    @Suppress("ComplexMethod")
    fun mapGoogleMLKitScanningErrors(
        exception: Throwable?
    ): CodeScanningErrorType {
        return when ((exception as? MlKitException)?.errorCode) {
            ABORTED -> CodeScanningErrorType.Aborted
            ALREADY_EXISTS -> CodeScanningErrorType.AlreadyExists
            CANCELLED -> CodeScanningErrorType.Cancelled
            CODE_SCANNER_APP_NAME_UNAVAILABLE -> CodeScanningErrorType.CodeScannerAppNameUnavailable
            CODE_SCANNER_CAMERA_PERMISSION_NOT_GRANTED ->
                CodeScanningErrorType.CodeScannerCameraPermissionNotGranted
            CODE_SCANNER_CANCELLED -> CodeScanningErrorType.CodeScannerCancelled
            CODE_SCANNER_GOOGLE_PLAY_SERVICES_VERSION_TOO_OLD ->
                CodeScanningErrorType.CodeScannerGooglePlayServicesVersionTooOld
            CODE_SCANNER_PIPELINE_INFERENCE_ERROR -> CodeScanningErrorType.CodeScannerPipelineInferenceError
            CODE_SCANNER_PIPELINE_INITIALIZATION_ERROR ->
                CodeScanningErrorType.CodeScannerPipelineInitializationError
            CODE_SCANNER_TASK_IN_PROGRESS -> CodeScanningErrorType.CodeScannerTaskInProgress
            CODE_SCANNER_UNAVAILABLE -> CodeScanningErrorType.CodeScannerUnavailable
            DATA_LOSS -> CodeScanningErrorType.DataLoss
            DEADLINE_EXCEEDED -> CodeScanningErrorType.DeadlineExceeded
            FAILED_PRECONDITION -> CodeScanningErrorType.FailedPrecondition
            INTERNAL -> CodeScanningErrorType.Internal
            INVALID_ARGUMENT -> CodeScanningErrorType.InvalidArgument
            MODEL_HASH_MISMATCH -> CodeScanningErrorType.ModelHashMismatch
            MODEL_INCOMPATIBLE_WITH_TFLITE -> CodeScanningErrorType.ModelIncompatibleWithTFLite
            NETWORK_ISSUE -> CodeScanningErrorType.NetworkIssue
            NOT_ENOUGH_SPACE -> CodeScanningErrorType.NotEnoughSpace
            NOT_FOUND -> CodeScanningErrorType.NotFound
            OUT_OF_RANGE -> CodeScanningErrorType.OutOfRange
            PERMISSION_DENIED -> CodeScanningErrorType.PermissionDenied
            RESOURCE_EXHAUSTED -> CodeScanningErrorType.ResourceExhausted
            UNAUTHENTICATED -> CodeScanningErrorType.UnAuthenticated
            UNAVAILABLE -> CodeScanningErrorType.UnAvailable
            UNIMPLEMENTED -> CodeScanningErrorType.UnImplemented
            UNKNOWN -> CodeScanningErrorType.Unknown
            else -> CodeScanningErrorType.Other(exception)
        }
    }
}

