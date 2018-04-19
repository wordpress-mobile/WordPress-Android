package org.wordpress.android.fluxc.post;

import org.junit.Test;
import org.wordpress.android.fluxc.model.PostModel;
import org.wordpress.android.fluxc.model.post.PostLocation;
import org.wordpress.android.fluxc.model.post.PostStatus;
import org.wordpress.android.fluxc.network.BaseRequest;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.wordpress.android.fluxc.post.PostTestUtils.EXAMPLE_LATITUDE;
import static org.wordpress.android.fluxc.post.PostTestUtils.EXAMPLE_LONGITUDE;

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

        PostModel clonedPost = testPost.clone();

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
        assertTrue(categoryIds.containsAll(testPost.getCategoryIdList())
                   && testPost.getCategoryIdList().containsAll(categoryIds));
    }

    @Test
    public void testLocation() {
        PostModel testPost = PostTestUtils.generateSampleLocalDraftPost();

        // Expect no location if none was set
        assertFalse(testPost.hasLocation());
        assertFalse(testPost.getLocation().isValid());
        assertFalse(testPost.shouldDeleteLatitude());
        assertFalse(testPost.shouldDeleteLongitude());

        // Verify state when location is set
        testPost.setLocation(new PostLocation(EXAMPLE_LATITUDE, EXAMPLE_LONGITUDE));

        assertTrue(testPost.hasLocation());
        assertEquals(EXAMPLE_LATITUDE, testPost.getLatitude(), 0);
        assertEquals(EXAMPLE_LONGITUDE, testPost.getLongitude(), 0);
        assertEquals(new PostLocation(EXAMPLE_LATITUDE, EXAMPLE_LONGITUDE), testPost.getLocation());
        assertFalse(testPost.shouldDeleteLatitude());
        assertFalse(testPost.shouldDeleteLongitude());

        // (0, 0) is a valid location
        testPost.setLocation(0, 0);

        assertTrue(testPost.hasLocation());
        assertEquals(0, testPost.getLatitude(), 0);
        assertEquals(0, testPost.getLongitude(), 0);
        assertEquals(new PostLocation(0, 0), testPost.getLocation());
        assertFalse(testPost.shouldDeleteLatitude());
        assertFalse(testPost.shouldDeleteLongitude());

        // Clearing the location should remove the location, and flag it for deletion on the server
        testPost.clearLocation();

        assertFalse(testPost.hasLocation());
        assertFalse(testPost.getLocation().isValid());
        assertTrue(testPost.shouldDeleteLatitude());
        assertTrue(testPost.shouldDeleteLongitude());
    }

    @Test
    public void testFilterEmptyTagsOnGetTagNameList() {
        PostModel testPost = PostTestUtils.generateSampleLocalDraftPost();

        testPost.setTagNames("pony,             ,ponies");
        List<String> tags = testPost.getTagNameList();
        assertTrue(tags.contains("pony"));
        assertTrue(tags.contains("ponies"));
        assertEquals(2, tags.size());
    }

    @Test
    public void testStripTagsOnGetTagNameList() {
        PostModel testPost = PostTestUtils.generateSampleLocalDraftPost();

        testPost.setTagNames("    pony   , ponies    , #popopopopopony");
        List<String> tags = testPost.getTagNameList();

        assertTrue(tags.contains("pony"));
        assertTrue(tags.contains("ponies"));
        assertTrue(tags.contains("#popopopopopony"));
        assertEquals(3, tags.size());
    }
}
