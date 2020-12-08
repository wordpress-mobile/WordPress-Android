package org.wordpress.android.fluxc.persistence

import com.wellsql.generated.ScanStateTable
import com.yarolegovich.wellsql.WellSql
import com.yarolegovich.wellsql.core.Identifiable
import com.yarolegovich.wellsql.core.annotation.Column
import com.yarolegovich.wellsql.core.annotation.PrimaryKey
import com.yarolegovich.wellsql.core.annotation.Table
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.scan.ScanStateModel
import org.wordpress.android.fluxc.model.scan.ScanStateModel.ScanProgressStatus
import org.wordpress.android.fluxc.model.scan.ScanStateModel.State
import org.wordpress.android.fluxc.model.scan.ScanStateModel.State.IDLE
import org.wordpress.android.fluxc.model.scan.ScanStateModel.State.PROVISIONING
import org.wordpress.android.fluxc.model.scan.ScanStateModel.State.SCANNING
import org.wordpress.android.fluxc.model.scan.ScanStateModel.State.UNAVAILABLE
import org.wordpress.android.fluxc.model.scan.ScanStateModel.State.UNKNOWN
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScanSqlUtils @Inject constructor() {
    fun replaceScanState(site: SiteModel, scanStateModel: ScanStateModel) {
        val scanStatusBuilder = scanStateModel.toBuilder(site)
        WellSql.delete(ScanStateBuilder::class.java)
            .where()
            .equals(ScanStateTable.LOCAL_SITE_ID, site.id)
            .endWhere()
            .execute()
        WellSql.insert(scanStatusBuilder).execute()
    }

    fun getScanStateForSite(site: SiteModel): ScanStateModel? {
        val scanStateBuilder = getScanStateBuilder(site)
        return scanStateBuilder?.build()
    }

    private fun getScanStateBuilder(site: SiteModel): ScanStateBuilder? {
        return WellSql.select(ScanStateBuilder::class.java)
            .where()
            .equals(ScanStateTable.LOCAL_SITE_ID, site.id)
            .endWhere()
            .asModel
            .firstOrNull()
    }

    private fun ScanStateModel.toBuilder(site: SiteModel): ScanStateBuilder {
        val startDate = when (state) {
            IDLE -> mostRecentStatus?.startDate?.time ?: 0
            SCANNING -> currentStatus?.startDate?.time ?: 0
            PROVISIONING, UNAVAILABLE, UNKNOWN -> 0
        }

        val progress = when (state) {
            IDLE -> mostRecentStatus?.progress ?: 0
            SCANNING -> currentStatus?.progress ?: 0
            PROVISIONING, UNAVAILABLE, UNKNOWN -> 0
        }

        val isInitial = when (state) {
            IDLE -> mostRecentStatus?.isInitial ?: false
            SCANNING -> currentStatus?.isInitial ?: false
            PROVISIONING, UNAVAILABLE, UNKNOWN -> false
        }

        return ScanStateBuilder(
            localSiteId = site.id,
            remoteSiteId = site.siteId,
            state = state.value,
            startDate = startDate,
            duration = mostRecentStatus?.duration ?: 0,
            progress = progress,
            reason = reason,
            error = mostRecentStatus?.error ?: false,
            initial = isInitial,
            hasCloud = hasCloud
        )
    }

    @Table(name = "ScanState")
    data class ScanStateBuilder(
        @PrimaryKey
        @Column private var id: Int = -1,
        @Column var localSiteId: Int,
        @Column var remoteSiteId: Long,
        @Column var state: String,
        @Column var startDate: Long,
        @Column var duration: Int = 0,
        @Column var progress: Int = 0,
        @Column var reason: String? = null,
        @Column var error: Boolean = false,
        @Column var initial: Boolean = false,
        @Column var hasCloud: Boolean = false
    ) : Identifiable {
        constructor() : this(-1, 0, 0, "", 0, 0, 0, "", false, false, false)

        override fun setId(id: Int) {
            this.id = id
        }

        override fun getId() = id

        fun build(): ScanStateModel {
            val stateForModel = State.fromValue(state) ?: UNKNOWN

            var currentStatus: ScanProgressStatus? = null
            var mostRecentStatus: ScanProgressStatus? = null

            when (stateForModel) {
                SCANNING -> {
                    currentStatus = ScanProgressStatus(
                        startDate = Date(startDate),
                        progress = progress,
                        isInitial = initial
                    )
                }
                IDLE -> {
                    mostRecentStatus = ScanProgressStatus(
                        startDate = Date(startDate),
                        duration = duration,
                        progress = progress,
                        error = error,
                        isInitial = initial
                    )
                }
                else -> { // Do nothing
                }
            }

            return ScanStateModel(
                state = stateForModel,
                hasCloud = hasCloud,
                mostRecentStatus = mostRecentStatus,
                currentStatus = currentStatus,
                reason = reason
            )
        }
    }
}
