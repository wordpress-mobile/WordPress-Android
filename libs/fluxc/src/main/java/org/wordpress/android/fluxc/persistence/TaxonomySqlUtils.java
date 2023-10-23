package org.wordpress.android.fluxc.persistence;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.wellsql.generated.TermModelTable;
import com.yarolegovich.wellsql.WellSql;

import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.model.TermModel;

import java.util.Collections;
import java.util.List;

public class TaxonomySqlUtils {
    public static int insertOrUpdateTerm(@NonNull TermModel term) {
        List<TermModel> termResult = WellSql.select(TermModel.class)
                .where().beginGroup()
                .equals(TermModelTable.ID, term.getId())
                .or()
                .beginGroup()
                .equals(TermModelTable.REMOTE_TERM_ID, term.getRemoteTermId())
                .equals(TermModelTable.LOCAL_SITE_ID, term.getLocalSiteId())
                .equals(TermModelTable.TAXONOMY, term.getTaxonomy())
                .endGroup()
                .endGroup().endWhere().getAsModel();

        if (termResult.isEmpty()) {
            // insert
            WellSql.insert(term).asSingleTransaction(true).execute();
            return 1;
        } else {
            return WellSql.update(TermModel.class).whereId(termResult.get(0).getId())
                    .put(term, new UpdateAllExceptId<>(TermModel.class)).execute();
        }
    }

    @NonNull
    public static List<TermModel> getTermsForSite(
            @NonNull SiteModel site,
            @NonNull String taxonomyName) {
        return WellSql.select(TermModel.class)
                .where().beginGroup()
                .equals(TermModelTable.LOCAL_SITE_ID, site.getId())
                .equals(TermModelTable.TAXONOMY, taxonomyName)
                .endGroup().endWhere()
                .getAsModel();
    }

    @Nullable
    public static TermModel getTermByRemoteId(
            @NonNull SiteModel site,
            long remoteTermId,
            @NonNull String taxonomyName) {
        List<TermModel> termResult = WellSql.select(TermModel.class)
                .where().beginGroup()
                .equals(TermModelTable.LOCAL_SITE_ID, site.getId())
                .equals(TermModelTable.REMOTE_TERM_ID, remoteTermId)
                .equals(TermModelTable.TAXONOMY, taxonomyName)
                .endGroup().endWhere()
                .getAsModel();

        if (!termResult.isEmpty()) {
            return termResult.get(0);
        }
        return null;
    }

    @Nullable
    public static TermModel getTermByName(
            @NonNull SiteModel site,
            @NonNull String termName,
            @NonNull String taxonomyName) {
        List<TermModel> termResult = WellSql.select(TermModel.class)
                .where().beginGroup()
                .equals(TermModelTable.LOCAL_SITE_ID, site.getId())
                .equals(TermModelTable.NAME, termName)
                .equals(TermModelTable.TAXONOMY, taxonomyName)
                .endGroup().endWhere()
                .getAsModel();

        if (!termResult.isEmpty()) {
            return termResult.get(0);
        }
        return null;
    }

    @NonNull
    public static List<TermModel> getTermsFromRemoteIdList(
            @NonNull List<Long> remoteTermIds,
            @NonNull SiteModel site,
            @NonNull String taxonomyName) {
        if (remoteTermIds.isEmpty()) {
            return Collections.emptyList();
        }

        return WellSql.select(TermModel.class)
                .where().beginGroup()
                .equals(TermModelTable.TAXONOMY, taxonomyName)
                .equals(TermModelTable.LOCAL_SITE_ID, site.getId())
                .isIn(TermModelTable.REMOTE_TERM_ID, remoteTermIds)
                .endGroup().endWhere()
                .getAsModel();
    }

    @NonNull
    public static List<TermModel> getTermsFromRemoteNameList(
            @NonNull List<String> remoteTermNames,
            @NonNull SiteModel site,
            @NonNull String taxonomyName) {
        if (remoteTermNames.isEmpty()) {
            return Collections.emptyList();
        }

        return WellSql.select(TermModel.class)
                .where().beginGroup()
                .equals(TermModelTable.TAXONOMY, taxonomyName)
                .equals(TermModelTable.LOCAL_SITE_ID, site.getId())
                .isIn(TermModelTable.NAME, remoteTermNames)
                .endGroup().endWhere()
                .getAsModel();
    }

    public static int clearTaxonomyForSite(
            @NonNull SiteModel site,
            @NonNull String taxonomyName) {
        return WellSql.delete(TermModel.class)
                .where().beginGroup()
                .equals(TermModelTable.LOCAL_SITE_ID, site.getId())
                .equals(TermModelTable.TAXONOMY, taxonomyName)
                .endGroup().endWhere()
                .execute();
    }

    public static int removeTerm(@NonNull TermModel term) {
        return WellSql.delete(TermModel.class)
                .where().beginGroup()
                .equals(TermModelTable.TAXONOMY, term.getTaxonomy())
                .equals(TermModelTable.REMOTE_TERM_ID, term.getRemoteTermId())
                .equals(TermModelTable.LOCAL_SITE_ID, term.getLocalSiteId())
                .endGroup().endWhere()
                .execute();
    }

    public static int deleteAllTerms() {
        return WellSql.delete(TermModel.class).execute();
    }
}
