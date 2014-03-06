package org.wordpress.android.models;

import android.test.InstrumentationTestCase;

public class PostTest extends InstrumentationTestCase {
    private Post post;

    @Override
    protected void setUp() throws Exception {
        post = new Post(123, false);
        post.setId(9876L);
        post.setPostid("543");
        post.setPost_status("draft");
        post.setTitle("My Title");
        post.setDescription("This is my description.");
        post.setLink("http://www.link.com");
        post.setLatitude(12.12d);
        post.setLongitude(21.21d);
        post.setLocalDraft(true);
        post.setLocalChange(true);
        post.setMediaPaths("media paths media paths");
        post.setMt_allow_comments(true);
        post.setMt_allow_pings(true);
        post.setMt_excerpt("this is the excerpt.");
        post.setMt_keywords("keyword1, keyword2, keyword3");
        post.setMt_text_more("This is my more text\n\nmore and more.");
        post.setPermaLink("http://www.www.com");
        post.setQuickPostType("qptype");
        post.setUploaded(true);
        post.setWP_password("123123123");
        post.setWP_post_form("post_form");
        post.setWP_slug("this_is_the_slug");
        post.setWP_author_display_name("The Author");
        post.setWP_author_id("author_id");


        super.setUp();
    }

    public void testPostCopy() {
        Post copiedPost = post.copy();

        assertNotNull(copiedPost);
        assertNotSame(post, copiedPost);
        assertEquals(post.getId(), copiedPost.getId());
        assertEquals(post.getPostid(), copiedPost.getPostid());
        assertEquals(post.getPost_status(), copiedPost.getPost_status());
        assertEquals(post.getTitle(), copiedPost.getTitle());
        assertEquals(post.getDescription(), copiedPost.getDescription());
        assertEquals(post.getLink(), copiedPost.getLink());
        assertEquals(post.getLatitude(), copiedPost.getLatitude());
        assertEquals(post.getLongitude(), copiedPost.getLongitude());
        assertEquals(post.isLocalDraft(), copiedPost.isLocalDraft());
        assertEquals(post.isLocalChange(), copiedPost.isLocalChange());
        assertEquals(post.getMediaPaths(), copiedPost.getMediaPaths());
        assertEquals(post.isMt_allow_comments(), copiedPost.isMt_allow_comments());
        assertEquals(post.isMt_allow_pings(), copiedPost.isMt_allow_pings());
        assertEquals(post.getMt_excerpt(), copiedPost.getMt_excerpt());
        assertEquals(post.getMt_keywords(), copiedPost.getMt_keywords());
        assertEquals(post.getMt_text_more(), copiedPost.getMt_text_more());
        assertEquals(post.getPermaLink(), copiedPost.getPermaLink());
        assertEquals(post.getQuickPostType(), copiedPost.getQuickPostType());
        assertEquals(post.isUploaded(), copiedPost.isUploaded());
        assertEquals(post.getWP_password(), copiedPost.getWP_password());
        assertEquals(post.getWP_post_format(), copiedPost.getWP_post_format());
        assertEquals(post.getWP_slug(), copiedPost.getWP_slug());
        assertEquals(post.getWP_author_display_name(), copiedPost.getWP_author_display_name());
        assertEquals(post.getWP_author_id(), copiedPost.getWP_author_id());
        assertEquals(post.getContent(), copiedPost.getContent());
    }

}
