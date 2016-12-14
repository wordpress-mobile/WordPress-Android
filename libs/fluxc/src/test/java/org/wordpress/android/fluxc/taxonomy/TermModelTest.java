package org.wordpress.android.fluxc.taxonomy;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.wordpress.android.fluxc.model.TermModel;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class TermModelTest {
    @Test
    public void testEquals() {
        TermModel testCategory = TaxonomyTestUtils.generateSampleCategory();
        TermModel testCategory2 = TaxonomyTestUtils.generateSampleCategory();

        testCategory2.setRemoteTermId(testCategory.getRemoteTermId() + 1);
        assertFalse(testCategory.equals(testCategory2));
        testCategory2.setRemoteTermId(testCategory.getRemoteTermId());
        assertTrue(testCategory.equals(testCategory2));
    }
}
