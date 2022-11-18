package org.wordpress.android.localcontentmigration

import org.wordpress.android.localcontentmigration.LocalContentEntity.EligibilityStatus
import org.wordpress.android.localcontentmigration.LocalContentEntityData.EligibilityStatusData
import javax.inject.Inject

class EligibilityHelper @Inject constructor(
    private val localMigrationContentResolver: LocalMigrationContentResolver,
) {
    fun validate() =
    localMigrationContentResolver.getResultForEntityType<EligibilityStatusData>(EligibilityStatus).validate()
}
