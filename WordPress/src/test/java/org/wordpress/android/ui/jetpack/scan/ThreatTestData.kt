package org.wordpress.android.ui.jetpack.scan

import org.wordpress.android.fluxc.model.scan.threat.BaseThreatModel
import org.wordpress.android.fluxc.model.scan.threat.ThreatModel.CoreFileModificationThreatModel
import org.wordpress.android.fluxc.model.scan.threat.ThreatModel.DatabaseThreatModel
import org.wordpress.android.fluxc.model.scan.threat.ThreatModel.DatabaseThreatModel.Row
import org.wordpress.android.fluxc.model.scan.threat.ThreatModel.FileThreatModel
import org.wordpress.android.fluxc.model.scan.threat.ThreatModel.FileThreatModel.ThreatContext
import org.wordpress.android.fluxc.model.scan.threat.ThreatModel.FileThreatModel.ThreatContext.ContextLine
import org.wordpress.android.fluxc.model.scan.threat.ThreatModel.Fixable
import org.wordpress.android.fluxc.model.scan.threat.ThreatModel.Fixable.FixType
import org.wordpress.android.fluxc.model.scan.threat.ThreatModel.GenericThreatModel
import org.wordpress.android.fluxc.model.scan.threat.ThreatModel.ThreatStatus
import org.wordpress.android.fluxc.model.scan.threat.ThreatModel.VulnerableExtensionThreatModel
import org.wordpress.android.fluxc.model.scan.threat.ThreatModel.VulnerableExtensionThreatModel.Extension
import org.wordpress.android.fluxc.model.scan.threat.ThreatModel.VulnerableExtensionThreatModel.Extension.ExtensionType
import java.util.Date

const val TEST_FILE_PATH = "/var/www/html/jp-scan-daily/wp-admin/index.php"
const val TEST_FILE_NAME = "index.php"
const val TEST_VULNERABLE_THREAT_SLUG = "test slug"
const val TEST_VULNERABLE_THREAT_VERSION = "test version"
const val TEST_SIGNATURE = "test signature"
const val TEST_DESCRIPTION = "test description"
const val TEST_CONTEXT_LINE_CONTENTS = "test contents"

object ThreatTestData {
    val extension = Extension(
        type = ExtensionType.PLUGIN,
        slug = TEST_VULNERABLE_THREAT_SLUG,
        name = "",
        version = TEST_VULNERABLE_THREAT_VERSION,
        isPremium = false
    )
    val rows = listOf(Row(id = 1, rowNumber = 1))
    val contextLine = ContextLine(
        lineNumber = 1,
        contents = TEST_CONTEXT_LINE_CONTENTS,
        highlights = listOf(Pair(2, 5))
    )

    val baseThreatModel = BaseThreatModel(
        id = 1L,
        signature = TEST_SIGNATURE,
        description = TEST_DESCRIPTION,
        status = ThreatStatus.CURRENT,
        firstDetected = Date(0),
        fixedOn = Date(123)
    )
    val genericThreatModel = GenericThreatModel(
        baseThreatModel = baseThreatModel
    )
    val coreFileModificationThreatModel = CoreFileModificationThreatModel(
        baseThreatModel = baseThreatModel,
        fileName = TEST_FILE_PATH,
        diff = ""
    )
    val databaseThreatModel = DatabaseThreatModel(
        baseThreatModel = baseThreatModel,
        rows = rows
    )
    val fileThreatModel = FileThreatModel(
        baseThreatModel = baseThreatModel,
        fileName = TEST_FILE_PATH,
        context = ThreatContext(lines = listOf(contextLine))
    )
    val vulnerableExtensionThreatModel = VulnerableExtensionThreatModel(
        baseThreatModel = baseThreatModel,
        extension = extension
    )
    val fixableThreatInCurrentStatus = GenericThreatModel(
        baseThreatModel.copy(
            fixable = Fixable(file = null, fixer = FixType.EDIT, target = null),
            status = ThreatStatus.CURRENT
        )
    )
    val notFixableThreatInCurrentStatus = GenericThreatModel(
        baseThreatModel.copy(
            fixable = null,
            status = ThreatStatus.CURRENT
        )
    )
    val fixableThreatInFixedStatus = GenericThreatModel(
        baseThreatModel.copy(
            fixable = Fixable(file = null, fixer = FixType.EDIT, target = null),
            status = ThreatStatus.FIXED
        )
    )
    val fixableThreatInIgnoredStatus = GenericThreatModel(
        baseThreatModel.copy(
            fixable = Fixable(file = null, fixer = FixType.EDIT, target = null),
            status = ThreatStatus.IGNORED
        )
    )
}
