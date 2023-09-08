package org.wordpress.android.fluxc.taxonomy;

import androidx.annotation.NonNull;

import com.yarolegovich.wellsql.WellSql;

import org.wordpress.android.fluxc.model.TermModel;
import org.wordpress.android.fluxc.store.TaxonomyStore;

import java.util.List;

class TaxonomyTestUtils {
    @NonNull
    private static TermModel generateSampleTerm(@NonNull String taxonomy) {
        return new TermModel(
                0,
                6,
                5,
                taxonomy,
                "Travel",
                "travel",
                "Post about travelling",
                0,
                0
        );
    }

    @NonNull
    public static TermModel generateSampleCategory() {
        return generateSampleTerm(TaxonomyStore.DEFAULT_TAXONOMY_CATEGORY);
    }

    @NonNull
    public static TermModel generateSampleTag() {
        return generateSampleTerm(TaxonomyStore.DEFAULT_TAXONOMY_TAG);
    }

    @NonNull
    public static TermModel generateSampleAuthor() {
        return generateSampleTerm("author");
    }

    @NonNull
    static List<TermModel> getTerms() {
        return WellSql.select(TermModel.class).getAsModel();
    }

    static int getTermsCount() {
        return getTerms().size();
    }
}
