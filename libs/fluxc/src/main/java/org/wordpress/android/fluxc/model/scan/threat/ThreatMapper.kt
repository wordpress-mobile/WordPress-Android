package org.wordpress.android.fluxc.model.scan.threat

import org.wordpress.android.fluxc.model.scan.threat.ThreatModel.CoreFileModificationThreatModel
import org.wordpress.android.fluxc.model.scan.threat.ThreatModel.DatabaseThreatModel
import org.wordpress.android.fluxc.model.scan.threat.ThreatModel.Extension
import org.wordpress.android.fluxc.model.scan.threat.ThreatModel.Extension.ExtensionType
import org.wordpress.android.fluxc.model.scan.threat.ThreatModel.FileThreatModel
import org.wordpress.android.fluxc.model.scan.threat.ThreatModel.Fixable
import org.wordpress.android.fluxc.model.scan.threat.ThreatModel.Fixable.FixType
import org.wordpress.android.fluxc.model.scan.threat.ThreatModel.GenericThreatModel
import org.wordpress.android.fluxc.model.scan.threat.ThreatModel.ThreatStatus
import org.wordpress.android.fluxc.model.scan.threat.ThreatModel.VulnerableExtensionThreatModel
import org.wordpress.android.fluxc.network.rest.wpcom.scan.threat.Threat
import java.util.Date
import javax.inject.Inject

class ThreatMapper @Inject constructor() {
    fun map(response: Threat): ThreatModel {
        return when {
            response.fileName != null && response.diff != null -> {
                CoreFileModificationThreatModel(
                    id = response.id ?: 0L,
                    signature = response.signature ?: "",
                    description = response.description ?: "",
                    status = ThreatStatus.fromValue(response.status),
                    firstDetected = response.firstDetected ?: Date(0),
                    fixable = response.fixable?.let {
                        Fixable(file = it.file, fixer = FixType.fromValue(it.fixer), target = it.target)
                    },
                    fixedOn = response.fixedOn,
                    fileName = response.fileName,
                    diff = response.diff
                )
            }
            response.context != null -> {
                FileThreatModel(
                    id = response.id ?: 0L,
                    signature = response.signature ?: "",
                    description = response.description ?: "",
                    status = ThreatStatus.fromValue(response.status),
                    firstDetected = response.firstDetected ?: Date(0),
                    fixable = response.fixable?.let {
                        Fixable(file = it.file, fixer = FixType.fromValue(it.fixer), target = it.target)
                    },
                    fixedOn = response.fixedOn,
                    fileName = response.fileName,
                    context = requireNotNull(response.context)
                )
            }
            response.rows != null || response.signature?.contains(DATABASE_SIGNATURE) == true -> {
                DatabaseThreatModel(
                    id = response.id ?: 0L,
                    signature = response.signature ?: "",
                    description = response.description ?: "",
                    status = ThreatStatus.fromValue(response.status),
                    firstDetected = response.firstDetected ?: Date(0),
                    fixable = response.fixable?.let {
                        Fixable(file = it.file, fixer = FixType.fromValue(it.fixer), target = it.target)
                    },
                    fixedOn = response.fixedOn,
                    rows = response.rows
                )
            }
            response.extension != null -> {
                VulnerableExtensionThreatModel(
                    id = response.id ?: 0L,
                    signature = response.signature ?: "",
                    description = response.description ?: "",
                    status = ThreatStatus.fromValue(response.status),
                    firstDetected = response.firstDetected ?: Date(0),
                    fixable = response.fixable?.let {
                        Fixable(file = it.file, fixer = FixType.fromValue(it.fixer), target = it.target)
                    },
                    fixedOn = response.fixedOn,
                    extension = Extension(
                        type = ExtensionType.fromValue(response.extension.type),
                        slug = response.extension.slug,
                        name = response.extension.name,
                        version = response.extension.version,
                        isPremium = response.extension.isPremium ?: false
                    )
                )
            }
            else -> {
                GenericThreatModel(
                    id = response.id ?: 0L,
                    signature = response.signature ?: "",
                    description = response.description ?: "",
                    status = ThreatStatus.fromValue(response.status),
                    firstDetected = response.firstDetected ?: Date(0),
                    fixable = response.fixable?.let {
                        Fixable(file = it.file, fixer = FixType.fromValue(it.fixer), target = it.target)
                    },
                    fixedOn = response.fixedOn
                )
            }
        }
    }

    companion object {
        private const val DATABASE_SIGNATURE = "Suspicious.Links"
    }
}
