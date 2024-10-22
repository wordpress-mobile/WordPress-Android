package org.wordpress.android.fluxc.taxonomy;

import org.junit.Test;
import org.wordpress.android.fluxc.model.TermModel;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TermModelTest {
    @Test
    public void testEquals() {
        TermModel testCategory1 = TaxonomyTestUtils.generateSampleCategory();
        TermModel testCategory2 = TaxonomyTestUtils.generateSampleCategory();

        testCategory2.setRemoteTermId(testCategory1.getRemoteTermId() + 1);
        assertFalse(testCategory1.equals(testCategory2));
        testCategory2.setRemoteTermId(testCategory1.getRemoteTermId());
        assertTrue(testCategory1.equals(testCategory2));
    }
}
