package org.wordpress.android.fluxc.taxonomy;

import com.yarolegovich.wellsql.WellSql;

import org.wordpress.android.fluxc.model.TermModel;
import org.wordpress.android.fluxc.store.TaxonomyStore;

import java.util.List;

class TaxonomyTestUtils {
    private static TermModel generateSampleTerm() {
        TermModel example = new TermModel();
        example.setLocalSiteId(6);
        example.setRemoteTermId(5);
        example.setSlug("travel");
        example.setName("Travel");
        example.setDescription("Post about travelling");
        return example;
    }

    public static TermModel generateSampleCategory() {
        TermModel exampleCategory = generateSampleTerm();
        exampleCategory.setTaxonomy(TaxonomyStore.DEFAULT_TAXONOMY_CATEGORY);
        return exampleCategory;
    }

    public static TermModel generateSampleTag() {
        TermModel exampleTag = generateSampleTerm();
        exampleTag.setTaxonomy(TaxonomyStore.DEFAULT_TAXONOMY_TAG);
        return exampleTag;
    }

    public static TermModel generateSampleAuthor() {
        TermModel exampleAuthor = generateSampleTerm();
        exampleAuthor.setTaxonomy("author");
        return exampleAuthor;
    }

    static List<TermModel> getTerms() {
        return WellSql.select(TermModel.class).getAsModel();
    }

    static int getTermsCount() {
        return getTerms().size();
    }
}
