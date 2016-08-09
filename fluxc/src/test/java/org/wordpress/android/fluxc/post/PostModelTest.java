package org.wordpress.android.fluxc.post;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.wordpress.android.fluxc.model.PostModel;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class PostModelTest {
    @Test
    public void testEquals() {
        PostModel testPost = PostTestUtils.generateSampleUploadedPost();
        PostModel testPost2 = PostTestUtils.generateSampleUploadedPost();

        testPost2.setRemotePostId(testPost.getRemotePostId() + 1);
        assertFalse(testPost.equals(testPost2));
        testPost2.setRemotePostId(testPost.getRemotePostId());
        assertTrue(testPost.equals(testPost2));
    }

    @Test
    public void testTerms() {
        PostModel testPost = PostTestUtils.generateSampleLocalDraftPost();

        testPost.setCategoryIdList(null);
        assertTrue(testPost.getCategoryIdList().isEmpty());

        List<Long> categoryIds = new ArrayList<>();
        testPost.setCategoryIdList(categoryIds);
        assertTrue(testPost.getCategoryIdList().isEmpty());

        categoryIds.add((long) 5);
        categoryIds.add((long) 6);
        testPost.setCategoryIdList(categoryIds);

        assertEquals(2, testPost.getCategoryIdList().size());
        assertTrue(categoryIds.containsAll(testPost.getCategoryIdList()) &&
                testPost.getCategoryIdList().containsAll(categoryIds));
    }
}
