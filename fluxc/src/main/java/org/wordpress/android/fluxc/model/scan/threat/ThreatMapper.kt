package org.wordpress.android.fluxc.model.scan.threat

import org.wordpress.android.fluxc.model.scan.threat.ThreatModel.Extension
import org.wordpress.android.fluxc.model.scan.threat.ThreatModel.Fixable
import org.wordpress.android.fluxc.network.rest.wpcom.scan.threat.Threat
import javax.inject.Inject

class ThreatMapper @Inject constructor() {
    fun map(response: Threat): ThreatModel {
        return ThreatModel(
            id = response.id,
            signature = response.signature,
            description = response.description,
            status = response.status,
            fixable = Fixable(
                fixer = response.fixable?.fixer,
                target = response.fixable?.target
            ),
            extension = Extension(
                type = response.extension?.type,
                slug = response.extension?.slug,
                name = response.extension?.name,
                version = response.extension?.version,
                isPremium = response.extension?.isPremium ?: false
            ),
            firstDetected = response.firstDetected,
            fixedOn = response.fixedOn
        )
    }
}
