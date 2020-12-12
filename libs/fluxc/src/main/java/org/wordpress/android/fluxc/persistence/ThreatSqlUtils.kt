package org.wordpress.android.fluxc.persistence

import com.wellsql.generated.ThreatModelTable
import com.yarolegovich.wellsql.WellSql
import com.yarolegovich.wellsql.core.Identifiable
import com.yarolegovich.wellsql.core.annotation.Column
import com.yarolegovich.wellsql.core.annotation.PrimaryKey
import com.yarolegovich.wellsql.core.annotation.Table
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.scan.threat.BaseThreatModel
import org.wordpress.android.fluxc.model.scan.threat.ThreatModel
import org.wordpress.android.fluxc.model.scan.threat.ThreatModel.Fixable
import org.wordpress.android.fluxc.model.scan.threat.ThreatModel.GenericThreatModel
import org.wordpress.android.fluxc.model.scan.threat.ThreatModel.ThreatStatus
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ThreatSqlUtils @Inject constructor() {
    fun replaceThreatsForSite(site: SiteModel, threatModels: List<ThreatModel>) {
        WellSql.delete(ThreatBuilder::class.java)
            .where()
            .equals(ThreatModelTable.LOCAL_SITE_ID, site.id)
            .endWhere()
            .execute()
        WellSql.insert(threatModels.map { it.toBuilder(site) }).execute()
    }

    fun getThreatsForSite(site: SiteModel): List<ThreatModel> {
        return WellSql.select(ThreatBuilder::class.java)
            .where()
            .equals(ThreatModelTable.LOCAL_SITE_ID, site.id)
            .endWhere()
            .asModel
            .map { it.build() }
    }

    private fun ThreatModel.toBuilder(site: SiteModel): ThreatBuilder {
        return ThreatBuilder(
            threatId = baseThreatModel.id,
            localSiteId = site.id,
            remoteSiteId = site.siteId,
            signature = baseThreatModel.signature,
            description = baseThreatModel.description,
            status = baseThreatModel.status.value,
            firstDetected = baseThreatModel.firstDetected.time,
            fixedOn = baseThreatModel.fixedOn?.time,
            fixableFile = baseThreatModel.fixable?.file,
            fixableFixer = baseThreatModel.fixable?.fixer?.value,
            fixableTarget = baseThreatModel.fixable?.target
        )
    }

    @Table(name = "ThreatModel")
    data class ThreatBuilder(
        @PrimaryKey
        @Column private var id: Int = -1,
        @Column var threatId: Long,
        @Column var localSiteId: Int,
        @Column var remoteSiteId: Long,
        @Column var signature: String,
        @Column var description: String,
        @Column var status: String,
        @Column var firstDetected: Long,
        @Column var fixedOn: Long? = null,
        @Column var fixableFile: String? = null,
        @Column var fixableFixer: String? = null,
        @Column var fixableTarget: String? = null
    ) : Identifiable {
        constructor() : this(-1, 0, 0, 0, "", "", "", 0, 0, "", "", "")

        override fun setId(id: Int) {
            this.id = id
        }

        override fun getId() = id

        fun build(): ThreatModel {
            val fixable: Fixable? = fixableFixer?.let {
                Fixable(
                    file = fixableFile,
                    fixer = Fixable.FixType.fromValue(it),
                    target = fixableTarget
                )
            }

            val baseThreatModel = BaseThreatModel(
                id = threatId,
                signature = signature,
                description = description,
                status = ThreatStatus.fromValue(status),
                firstDetected = Date(firstDetected),
                fixable = fixable,
                fixedOn = fixedOn?.let { Date(it) }
            )

            return GenericThreatModel(baseThreatModel)
        }
    }
}
