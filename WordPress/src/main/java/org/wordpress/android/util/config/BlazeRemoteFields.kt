package org.wordpress.android.util.config

import org.wordpress.android.annotation.RemoteFieldDefaultGenerater
import javax.inject.Inject

const val BLAZE_NON_DISMISSABLE_HASH_REMOTE_FIELD = "blaze_non_dismissable_hash"
const val BLAZE_NON_DISMISSABLE_HASH_DEFAULT = "step-4"


@RemoteFieldDefaultGenerater(
    remoteField = BLAZE_NON_DISMISSABLE_HASH_REMOTE_FIELD,
    defaultValue = BLAZE_NON_DISMISSABLE_HASH_DEFAULT
)

class BlazeNonDismissableHashConfig @Inject constructor(appConfig: AppConfig) :
    RemoteConfigField<String>(
        appConfig,
        BLAZE_NON_DISMISSABLE_HASH_REMOTE_FIELD
    )


const val BLAZE_COMPLETED_STEP_HASH_REMOTE_FIELD = "blaze_completed_step_hash"
const val BLAZE_COMPLETED_STEP_HASH_DEFAULT = "step-5"

@RemoteFieldDefaultGenerater(
    remoteField = BLAZE_COMPLETED_STEP_HASH_REMOTE_FIELD,
    defaultValue = BLAZE_COMPLETED_STEP_HASH_DEFAULT
)
class BlazeCompletedStepHashConfig @Inject constructor(appConfig: AppConfig) :
    RemoteConfigField<String>(
        appConfig,
        BLAZE_COMPLETED_STEP_HASH_REMOTE_FIELD
    )
