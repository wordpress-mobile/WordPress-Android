package org.wordpress.android.fluxc.persistence

import com.google.gson.Gson
import com.yarolegovich.wellsql.WellSql
import org.assertj.core.api.Assertions.assertThat
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.scan.threat.BaseThreatModel
import org.wordpress.android.fluxc.model.scan.threat.ThreatMapper
import org.wordpress.android.fluxc.model.scan.threat.ThreatModel.CoreFileModificationThreatModel
import org.wordpress.android.fluxc.model.scan.threat.ThreatModel.DatabaseThreatModel
import org.wordpress.android.fluxc.model.scan.threat.ThreatModel.FileThreatModel
import org.wordpress.android.fluxc.model.scan.threat.ThreatModel.FileThreatModel.ThreatContext
import org.wordpress.android.fluxc.model.scan.threat.ThreatModel.GenericThreatModel
import org.wordpress.android.fluxc.model.scan.threat.ThreatModel.ThreatStatus
import org.wordpress.android.fluxc.model.scan.threat.ThreatModel.ThreatStatus.CURRENT
import org.wordpress.android.fluxc.model.scan.threat.ThreatModel.ThreatStatus.FIXED
import org.wordpress.android.fluxc.model.scan.threat.ThreatModel.VulnerableExtensionThreatModel
import org.wordpress.android.fluxc.model.scan.threat.ThreatModel.VulnerableExtensionThreatModel.Extension
import java.util.Date

@Config(manifest = Config.NONE)
@RunWith(RobolectricTestRunner::class)
class ThreatSqlUtilsTest {
    private val gson = Gson()
    private val threatMapper = ThreatMapper()
    private val threatSqlUtils = ThreatSqlUtils(gson, threatMapper)
    private lateinit var site: SiteModel

    private val threatStatus = CURRENT
    private val dummyBaseThreatModel = BaseThreatModel(
        id = 1L,
        signature = "test signature",
        description = "test description",
        status = threatStatus,
        firstDetected = Date(0)
    )

    @Before
    fun setUp() {
        val appContext = RuntimeEnvironment.application.applicationContext

        val config = WellSqlConfig(appContext)
        WellSql.init(config)
        config.reset()

        site = SiteModel().apply { id = 100 }
    }

    @Test
    fun `insert and retrieve for base threat properties work correctly`() {
        val threat = GenericThreatModel(baseThreatModel = dummyBaseThreatModel)

        threatSqlUtils.insertThreats(site, listOf(threat))
        val threats = threatSqlUtils.getThreats(site, listOf(threatStatus))

        assertEquals(1, threats.size)
        with(threats.first().baseThreatModel) {
            assertEquals(dummyBaseThreatModel.id, id)
            assertEquals(dummyBaseThreatModel.signature, signature)
            assertEquals(dummyBaseThreatModel.description, description)
            assertEquals(dummyBaseThreatModel.status, status)
            assertEquals(dummyBaseThreatModel.firstDetected, firstDetected)
        }
    }

    @Test
    fun `insert and retrieve for DatabaseThreatModel properties work correctly`() {
        val dummyThreat = DatabaseThreatModel(
            baseThreatModel = dummyBaseThreatModel,
            rows = listOf(DatabaseThreatModel.Row(id = 1, rowNumber = 1))
        )

        threatSqlUtils.insertThreats(site, listOf(dummyThreat))
        val threats = threatSqlUtils.getThreats(site, listOf(threatStatus))

        assertEquals(1, threats.size)
        assertThat(threats.first()).isInstanceOf(DatabaseThreatModel::class.java)
        with(threats.first() as DatabaseThreatModel) {
            assertEquals(dummyThreat.rows?.first()?.id, rows?.first()?.id)
            assertEquals(dummyThreat.rows?.first()?.rowNumber, rows?.first()?.rowNumber)
        }
    }

    @Test
    fun `insert and retrieve for CoreFileModificationThreatModel properties work correctly`() {
        val dummyThreat = CoreFileModificationThreatModel(
            baseThreatModel = dummyBaseThreatModel,
            fileName = "test filename",
            diff = "test diff"
        )

        threatSqlUtils.insertThreats(site, listOf(dummyThreat))
        val threats = threatSqlUtils.getThreats(site, listOf(threatStatus))

        assertEquals(1, threats.size)
        assertThat(threats.first()).isInstanceOf(CoreFileModificationThreatModel::class.java)
        with(threats.first() as CoreFileModificationThreatModel) {
            assertEquals(dummyThreat.fileName, fileName)
            assertEquals(dummyThreat.diff, diff)
        }
    }

    @Test
    fun `insert and retrieve for VulnerableExtensionThreatModel properties work correctly`() {
        val dummyThreat = VulnerableExtensionThreatModel(
            baseThreatModel = dummyBaseThreatModel,
            extension = Extension(
                type = Extension.ExtensionType.PLUGIN,
                slug = "test slug",
                name = "test name",
                version = "test version",
                isPremium = false
            )
        )

        threatSqlUtils.insertThreats(site, listOf(dummyThreat))
        val threats = threatSqlUtils.getThreats(site, listOf(threatStatus))

        assertEquals(1, threats.size)
        assertThat(threats.first()).isInstanceOf(VulnerableExtensionThreatModel::class.java)
        with((threats.first() as VulnerableExtensionThreatModel).extension) {
            assertEquals(dummyThreat.extension.type, type)
            assertEquals(dummyThreat.extension.slug, slug)
            assertEquals(dummyThreat.extension.name, name)
            assertEquals(dummyThreat.extension.version, version)
            assertEquals(dummyThreat.extension.isPremium, isPremium)
        }
    }

    @Test
    fun `insert and retrieve for FileThreatModel properties work correctly`() {
        val dummyThreat = FileThreatModel(
            baseThreatModel = dummyBaseThreatModel,
            fileName = "test fileName",
            context = ThreatContext(
                lines = listOf(ThreatContext.ContextLine(lineNumber = 1, contents = "test content"))
            )
        )

        threatSqlUtils.insertThreats(site, listOf(dummyThreat))
        val threats = threatSqlUtils.getThreats(site, listOf(threatStatus))

        assertEquals(1, threats.size)
        assertThat(threats.first()).isInstanceOf(FileThreatModel::class.java)
        with((threats.first() as FileThreatModel)) {
            assertEquals(dummyThreat.fileName, fileName)
            assertEquals(dummyThreat.context.lines.first().lineNumber, context.lines.first().lineNumber)
            assertEquals(dummyThreat.context.lines.first().contents, context.lines.first().contents)
        }
    }

    @Test
    fun `threat model gets retrieved for the given threat id`() {
        val threatId = dummyBaseThreatModel.id
        val dummyThreat = GenericThreatModel(baseThreatModel = dummyBaseThreatModel)
        threatSqlUtils.insertThreats(site, listOf(dummyThreat))

        val threat = threatSqlUtils.getThreatByThreatId(threatId)

        assertThat(threat).isInstanceOf(GenericThreatModel::class.java)
        assertEquals(threatId, threat?.baseThreatModel?.id)
    }

    @Test
    fun `removeThreatsWithStatus() removes only corresponding threats`() {
        val threat1 = GenericThreatModel(baseThreatModel = dummyBaseThreatModel.copy(status = CURRENT))
        val threat2 = GenericThreatModel(baseThreatModel = dummyBaseThreatModel.copy(status = FIXED))
        threatSqlUtils.insertThreats(site, listOf(threat1, threat2))

        threatSqlUtils.removeThreatsWithStatus(site, listOf(CURRENT))
        val threats = threatSqlUtils.getThreats(site, ThreatStatus.values().toList())

        assertEquals(1, threats.size)
    }

    @Test
    fun `getThreats returns only threats with corresponding status`() {
        val threat1 = GenericThreatModel(baseThreatModel = dummyBaseThreatModel.copy(status = CURRENT))
        val threat2 = GenericThreatModel(baseThreatModel = dummyBaseThreatModel.copy(status = FIXED))
        threatSqlUtils.insertThreats(site, listOf(threat1, threat2))

        val threats = threatSqlUtils.getThreats(site, listOf(CURRENT))

        assertEquals(threats[0].baseThreatModel.status, CURRENT)
        assertEquals(threats.size, 1)
    }
}
