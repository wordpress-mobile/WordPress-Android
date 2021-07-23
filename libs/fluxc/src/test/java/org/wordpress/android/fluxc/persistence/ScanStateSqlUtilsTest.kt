package org.wordpress.android.fluxc.persistence

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.yarolegovich.wellsql.WellSql
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.scan.ScanStateModel
import org.wordpress.android.fluxc.model.scan.ScanStateModel.Reason
import org.wordpress.android.fluxc.model.scan.ScanStateModel.ScanProgressStatus
import java.util.Date

@Config(manifest = Config.NONE)
@RunWith(RobolectricTestRunner::class)
class ScanStateSqlUtilsTest {
    private val scanSqlUtils = ScanSqlUtils()
    private lateinit var site: SiteModel

    @Before
    fun setUp() {
        val appContext = ApplicationProvider.getApplicationContext<Application>()

        val config = WellSqlConfig(appContext)
        WellSql.init(config)
        config.reset()

        site = SiteModel().apply { id = 100 }
    }

    @Test
    fun `given idle state scan state model, when model is saved, then save succeeds`() {
        val scanStateModel = getScanStateModel(ScanStateModel.State.IDLE)

        scanSqlUtils.replaceScanState(site, scanStateModel)
        val scanStateFromDb = scanSqlUtils.getScanStateForSite(site)

        assertEquals(scanStateModel, scanStateFromDb)
    }

    @Test
    fun `given scanning state scan state model, when model is saved, then save succeeds`() {
        val scanStateModel = getScanStateModel(ScanStateModel.State.SCANNING)

        scanSqlUtils.replaceScanState(site, scanStateModel)
        val scanStateFromDb = scanSqlUtils.getScanStateForSite(site)

        assertEquals(scanStateModel, scanStateFromDb)
    }

    @Test
    fun `given provisioning state scan state model, when model is saved, then save succeeds`() {
        val scanStateModel = getScanStateModel(ScanStateModel.State.PROVISIONING)

        scanSqlUtils.replaceScanState(site, scanStateModel)
        val scanStateFromDb = scanSqlUtils.getScanStateForSite(site)

        assertEquals(scanStateModel, scanStateFromDb)
    }

    private fun getScanStateModel(state: ScanStateModel.State): ScanStateModel {
        var scanStateModel = ScanStateModel(
                state = state,
                reason = Reason.UNKNOWN,
                hasCloud = true,
                hasValidCredentials = true
        )

        if (state == ScanStateModel.State.IDLE) {
            val mostRecentStatus = ScanProgressStatus(
                    startDate = Date(),
                    duration = 40,
                    progress = 30,
                    error = false,
                    isInitial = true
            )
            scanStateModel = scanStateModel.copy(mostRecentStatus = mostRecentStatus)
        } else if (state == ScanStateModel.State.SCANNING) {
            val currentStatus = ScanProgressStatus(
                    startDate = Date(),
                    progress = 30,
                    isInitial = true
            )
            scanStateModel = scanStateModel.copy(currentStatus = currentStatus)
        }

        return scanStateModel
    }
}
