package org.wordpress.android.fluxc.model.scan.threat

import org.wordpress.android.fluxc.model.scan.threat.ThreatModel.Extension
import org.wordpress.android.fluxc.model.scan.threat.ThreatModel.Fixable
import org.wordpress.android.fluxc.network.rest.wpcom.scan.threat.Threat
import java.util.Date
import javax.inject.Inject

class ThreatMapper @Inject constructor() {
    fun map(response: Threat): ThreatModel {
        return ThreatModel(
            id = response.id ?: 0L,
            signature = response.signature ?: "",
            description = response.description ?: "",
            status = response.status,
            fixable = response.fixable?.let {
                Fixable(
                    file = response.fixable.file,
                    fixer = response.fixable.fixer,
                    target = response.fixable.target
                )
            },
            extension = Extension(
                type = response.extension?.type,
                slug = response.extension?.slug,
                name = response.extension?.name,
                version = response.extension?.version,
                isPremium = response.extension?.isPremium ?: false
            ),
            firstDetected = response.firstDetected ?: Date(0),
            fixedOn = response.fixedOn,
            context = response.context,
            fileName = response.fileName,
            rows = response.rows,
            diff = response.diff
        )
    }
}
