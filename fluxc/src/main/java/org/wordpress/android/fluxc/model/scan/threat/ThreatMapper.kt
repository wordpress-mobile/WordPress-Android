package org.wordpress.android.fluxc.model.scan.threat

import org.wordpress.android.fluxc.model.scan.threat.ThreatModel.CoreFileModificationThreatModel
import org.wordpress.android.fluxc.model.scan.threat.ThreatModel.DatabaseThreatModel
import org.wordpress.android.fluxc.model.scan.threat.ThreatModel.FileThreatModel
import org.wordpress.android.fluxc.model.scan.threat.ThreatModel.Fixable
import org.wordpress.android.fluxc.model.scan.threat.ThreatModel.Fixable.FixType
import org.wordpress.android.fluxc.model.scan.threat.ThreatModel.GenericThreatModel
import org.wordpress.android.fluxc.model.scan.threat.ThreatModel.ThreatStatus
import org.wordpress.android.fluxc.model.scan.threat.ThreatModel.VulnerableExtensionThreatModel
import org.wordpress.android.fluxc.model.scan.threat.ThreatModel.VulnerableExtensionThreatModel.Extension
import org.wordpress.android.fluxc.model.scan.threat.ThreatModel.VulnerableExtensionThreatModel.Extension.ExtensionType
import org.wordpress.android.fluxc.network.rest.wpcom.scan.threat.Threat
import java.util.Date
import javax.inject.Inject

class ThreatMapper @Inject constructor() {
    fun map(response: Threat): ThreatModel {
        val baseThreatModel = getBaseThreatModelFromResponse(response)

        return when {
            response.fileName != null && response.diff != null -> {
                CoreFileModificationThreatModel(
                    baseThreatModel = baseThreatModel,
                    fileName = response.fileName,
                    diff = response.diff
                )
            }
            response.context != null -> {
                FileThreatModel(
                    baseThreatModel = baseThreatModel,
                    fileName = response.fileName,
                    context = requireNotNull(response.context)
                )
            }
            response.rows != null || response.signature?.contains(DATABASE_SIGNATURE) == true -> {
                DatabaseThreatModel(
                    baseThreatModel = baseThreatModel,
                    rows = response.rows
                )
            }
            response.extension != null && ExtensionType.fromValue(response.extension.type) != ExtensionType.UNKNOWN -> {
                VulnerableExtensionThreatModel(
                    baseThreatModel = baseThreatModel,
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
                    baseThreatModel = baseThreatModel
                )
            }
        }
    }

    private fun getBaseThreatModelFromResponse(response: Threat): BaseThreatModel {
        val id = response.id ?: 0L
        val signature = response.signature ?: ""
        val firstDetected = response.firstDetected ?: Date(0)

        val status = ThreatStatus.fromValue(response.status)
        val description = response.description ?: ""

        val fixable = response.fixable?.let {
            val fixType = FixType.fromValue(it.fixer)
            Fixable(file = it.file, fixer = fixType, target = it.target)
        }

        return BaseThreatModel(
            id = id,
            signature = signature,
            description = description,
            status = status,
            firstDetected = firstDetected,
            fixable = fixable,
            fixedOn = response.fixedOn
        )
    }

    companion object {
        private const val DATABASE_SIGNATURE = "Suspicious.Links"
    }
}
