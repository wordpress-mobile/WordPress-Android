package org.wordpress.android.fluxc.persistence

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.wellsql.generated.ThreatModelTable
import com.yarolegovich.wellsql.WellSql
import com.yarolegovich.wellsql.core.Identifiable
import com.yarolegovich.wellsql.core.annotation.Column
import com.yarolegovich.wellsql.core.annotation.PrimaryKey
import com.yarolegovich.wellsql.core.annotation.Table
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.scan.threat.ThreatMapper
import org.wordpress.android.fluxc.model.scan.threat.ThreatModel
import org.wordpress.android.fluxc.model.scan.threat.ThreatModel.CoreFileModificationThreatModel
import org.wordpress.android.fluxc.model.scan.threat.ThreatModel.DatabaseThreatModel
import org.wordpress.android.fluxc.model.scan.threat.ThreatModel.FileThreatModel
import org.wordpress.android.fluxc.model.scan.threat.ThreatModel.ThreatStatus
import org.wordpress.android.fluxc.model.scan.threat.ThreatModel.VulnerableExtensionThreatModel
import org.wordpress.android.fluxc.network.rest.wpcom.scan.threat.Threat
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ThreatSqlUtils @Inject constructor(private val gson: Gson, private val threatMapper: ThreatMapper) {
    fun removeThreatsWithStatus(site: SiteModel, statuses: List<ThreatStatus>) {
        WellSql.delete(ThreatBuilder::class.java)
                .where()
                .equals(ThreatModelTable.LOCAL_SITE_ID, site.id)
                .isIn(ThreatModelTable.STATUS, statuses.map { it.value })
                .endWhere()
                .execute()
    }

    fun insertThreats(site: SiteModel, threatModels: List<ThreatModel>) {
        WellSql.insert(threatModels.map { it.toBuilder(site) }).execute()
    }

    fun getThreats(site: SiteModel, statuses: List<ThreatStatus>): List<ThreatModel> {
        return WellSql.select(ThreatBuilder::class.java)
            .where()
            .equals(ThreatModelTable.LOCAL_SITE_ID, site.id)
            .isIn(ThreatModelTable.STATUS, statuses.map { it.value })
            .endWhere()
            .asModel
            .map { it.build(gson, threatMapper) }
    }

    fun getThreatByThreatId(threatId: Long): ThreatModel? {
        return WellSql.select(ThreatBuilder::class.java)
            .where()
            .equals(ThreatModelTable.THREAT_ID, threatId)
            .endWhere()
            .asModel
            .firstOrNull()
            ?.build(gson, threatMapper)
    }

    private fun ThreatModel.toBuilder(site: SiteModel): ThreatBuilder {
        var fileName: String? = null
        var diff: String? = null
        var extension: String? = null
        var rows: String? = null
        var context: String? = null

        when (this) {
            is CoreFileModificationThreatModel -> {
                fileName = this.fileName
                diff = this.diff
            }
            is VulnerableExtensionThreatModel -> {
                with(this.extension) {
                    extension = gson.toJson(
                        Threat.Extension(
                            type = type.value,
                            slug = slug,
                            name = name,
                            version = version,
                            isPremium = isPremium
                        )
                    )
                }
            }
            is DatabaseThreatModel -> rows = gson.toJson(this.rows)
            is FileThreatModel -> {
                fileName = this.fileName
                context = gson.toJson(this.context)
            }
            else -> Unit // Do Nothing (ignore)
        }
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
            fixableTarget = baseThreatModel.fixable?.target,
            fileName = fileName,
            diff = diff,
            extension = extension,
            rows = rows,
            context = context
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
        @Column var fixableTarget: String? = null,
        @Column var fileName: String? = null,
        @Column var diff: String? = null,
        @Column var extension: String? = null,
        @Column var rows: String? = null,
        @Column var context: String? = null
    ) : Identifiable {
        constructor() : this(-1, 0, 0, 0, "", "", "", 0, 0)

        override fun setId(id: Int) {
            this.id = id
        }

        override fun getId() = id

        fun build(gson: Gson, threatMapper: ThreatMapper): ThreatModel {
            val threat = Threat(
                id = threatId,
                signature = signature,
                description = description,
                status = status,
                firstDetected = Date(firstDetected),
                fixable = fixableFixer?.let {
                    Threat.Fixable(
                        file = fixableFile,
                        fixer = it,
                        target = fixableTarget
                    )
                },
                fixedOn = fixedOn?.let { Date(it) },
                fileName = fileName,
                diff = diff,
                extension = extension?.let { gson.fromJson(extension, Threat.Extension::class.java) },
                rows = rows?.let {
                    gson.fromJson<List<DatabaseThreatModel.Row>>(
                        rows,
                        object : TypeToken<List<DatabaseThreatModel.Row?>?>() {}.type
                    )
                },
                context = context?.let { gson.fromJson(context, FileThreatModel.ThreatContext::class.java) }
            )

            return threatMapper.map(threat)
        }
    }
}
