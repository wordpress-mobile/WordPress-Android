package org.wordpress.android.models;

import android.test.InstrumentationTestCase;

import org.wordpress.android.WordPressDB;

import java.util.Date;

/**
 * Created by roundhill on 3/12/14.
 */
public class PostTest extends InstrumentationTestCase {
    private WordPressDB mDB;
    private Post post;

    @Override
    protected void setUp() throws Exception {
        mDB = new WordPressDB(getInstrumentation().getContext());

        post = new Post();
        post.setLocalTableBlogId(1233);
        post.setLocalTablePostId(12342134L);
        post.setIsPage(true);
        post.setCategories("Bananas, Apples, Fun");
        post.setCustomFields("{ \"field\":\"Here is a custom field\"}");
        post.setDateCreated(new Date().getTime());
        post.setDate_created_gmt(post.getDateCreated());
        post.setDescription("This is my description.");
        post.setLink("http://www.link.com");
        post.setAllowComments(true);
        post.setAllowPings(true);
        post.setPostExcerpt("Post Excerpt");
        post.setKeywords("keyword1, keyword2");
        post.setMoreText("More Text!");
        post.setPermaLink("http://www.www.com");
        post.setPostStatus("draft");
        post.setRemotePostId("123432");
        post.setTitle("My Title");
        post.setUserId("testid");
        post.setAuthorDisplayName("Test User");
        post.setAuthorId("wp_author_id");
        post.setPassword("123123123");
        post.setPostFormat("da post format");
        post.setSlug("this_is_the_slug");
        post.setLocalDraft(true);
        post.setUploaded(true);
        post.setLatitude(12.12d);
        post.setLongitude(21.21d);
        post.setPageParentId("parent_id");
        post.setPageParentTitle("parent title");
        post.setLocalChange(true);
        post.setMediaPaths("media paths media paths");
        post.setQuickPostType("qptype");

        super.setUp();
    }

    public void testInvalidPostIdLoad() {
        Post post = mDB.getPostForLocalTablePostId(-1);

        assertNull(post);
    }

    public void testPostSaveAndLoad() {
        Post post = new Post(1, false);
        post.setTitle("test-post");
        mDB.savePost(post);

        Post loadedPost = mDB.getPostForLocalTablePostId(post.getLocalTablePostId());

        assertNotNull(loadedPost);
        assertEquals(loadedPost.getTitle(), post.getTitle());
    }

    public void testPostCopy() {
        Post copiedPost = post.copy();
        assertNotNull(copiedPost);
        assertNotSame(post, copiedPost);
        assertEquals(post.getLocalTableBlogId(), copiedPost.getLocalTableBlogId());
        assertEquals(post.getLocalTablePostId(), copiedPost.getLocalTablePostId());
        assertEquals(post.isPage(), copiedPost.isPage());
        assertEquals(post.getPostStatus(), copiedPost.getPostStatus());
        assertEquals(post.getTitle(), copiedPost.getTitle());
        assertEquals(post.getDescription(), copiedPost.getDescription());
        assertEquals(post.getLink(), copiedPost.getLink());
        assertEquals(post.getDateCreated(), copiedPost.getDateCreated());
        assertEquals(post.getDate_created_gmt(), copiedPost.getDate_created_gmt());
        assertEquals(post.getRemotePostId(), copiedPost.getRemotePostId());
        assertEquals(post.getUserId(), copiedPost.getUserId());
        assertEquals(post.getLatitude(), copiedPost.getLatitude());
        assertEquals(post.getLongitude(), copiedPost.getLongitude());
        assertEquals(post.isLocalDraft(), copiedPost.isLocalDraft());
        assertEquals(post.isLocalChange(), copiedPost.isLocalChange());
        assertEquals(post.getMediaPaths(), copiedPost.getMediaPaths());
        assertEquals(post.isAllowComments(), copiedPost.isAllowComments());
        assertEquals(post.isAllowPings(), copiedPost.isAllowPings());
        assertEquals(post.getPostExcerpt(), copiedPost.getPostExcerpt());
        assertEquals(post.getKeywords(), copiedPost.getKeywords());
        assertEquals(post.getMoreText(), copiedPost.getMoreText());
        assertEquals(post.getPermaLink(), copiedPost.getPermaLink());
        assertEquals(post.getQuickPostType(), copiedPost.getQuickPostType());
        assertEquals(post.isUploaded(), copiedPost.isUploaded());
        assertEquals(post.getPassword(), copiedPost.getPassword());
        assertEquals(post.getPostFormat(), copiedPost.getPostFormat());
        assertEquals(post.getSlug(), copiedPost.getSlug());
        assertEquals(post.getAuthorDisplayName(), copiedPost.getAuthorDisplayName());
        assertEquals(post.getAuthorId(), copiedPost.getAuthorId());
        assertEquals(post.getContent(), copiedPost.getContent());
        assertEquals(post.getStatusEnum(), copiedPost.getStatusEnum());
    }

    public void testPostEnumStatus() {
        post.setStatusEnum(PostStatus.DRAFT);
        assertEquals(post.getStatusEnum(), PostStatus.DRAFT);
        post.setStatusEnum(PostStatus.PUBLISHED);
        assertEquals(post.getStatusEnum(), PostStatus.PUBLISHED);
        post.setStatusEnum(PostStatus.PENDING);
        assertEquals(post.getStatusEnum(), PostStatus.PENDING);
        post.setStatusEnum(PostStatus.PRIVATE);
        assertEquals(post.getStatusEnum(), PostStatus.PRIVATE);
        post.setStatusEnum(PostStatus.SCHEDULED);
        assertEquals(post.getStatusEnum(), PostStatus.SCHEDULED);
        post.setStatusEnum(PostStatus.UNKNOWN);
        assertEquals(post.getStatusEnum(), PostStatus.UNKNOWN);
    }

}
