package org.wordpress.android.fluxc.post;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.wordpress.android.fluxc.model.PostModel;
import org.wordpress.android.fluxc.model.post.PostStatus;
import org.wordpress.android.fluxc.network.BaseRequest;

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
    public void testClone() {
        PostModel testPost = PostTestUtils.generateSampleLocalDraftPost();

        // Fill a few more sample fields
        testPost.setDateCreated("1955-11-05T06:15:00-0800");
        testPost.setStatus(PostStatus.SCHEDULED.toString());
        List<Long> categoryList = new ArrayList<>();
        categoryList.add(45L);
        testPost.setCategoryIdList(categoryList);

        testPost.error = new BaseRequest.BaseNetworkError(BaseRequest.GenericErrorType.PARSE_ERROR);

        PostModel clonedPost = (PostModel) testPost.clone();

        assertFalse(testPost == clonedPost);
        assertTrue(testPost.equals(clonedPost));

        // The inherited error should also be cloned
        assertFalse(testPost.error == clonedPost.error);
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
