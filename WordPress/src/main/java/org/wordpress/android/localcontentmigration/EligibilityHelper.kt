package org.wordpress.android.localcontentmigration

import org.wordpress.android.localcontentmigration.LocalContentEntity.EligibilityStatus
import org.wordpress.android.localcontentmigration.LocalContentEntityData.EligibilityStatusData
import javax.inject.Inject

class EligibilityHelper @Inject constructor(
    private val localMigrationContentResolver: LocalMigrationContentResolver,
) {
    @Suppress("ForbiddenComment")
    /** TODO: This should perform some additional pre-flight checks. We should:
     * * On WordPress:
     *    1. Check for local-only data
     */
    fun validate() =
    localMigrationContentResolver.getResultForEntityType<EligibilityStatusData>(EligibilityStatus).validate()
}
