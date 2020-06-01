package org.wordpress.android.fluxc.post;

import android.content.Context;

import com.yarolegovich.wellsql.WellSql;
import com.yarolegovich.wellsql.core.Identifiable;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.SingleStoreWellSqlConfigForTests;
import org.wordpress.android.fluxc.model.LocalOrRemoteId;
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId;
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId;
import org.wordpress.android.fluxc.model.PostModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.model.post.PostStatus;
import org.wordpress.android.fluxc.model.revisions.Diff;
import org.wordpress.android.fluxc.model.revisions.DiffOperations;
import org.wordpress.android.fluxc.model.revisions.LocalDiffModel;
import org.wordpress.android.fluxc.model.revisions.LocalRevisionModel;
import org.wordpress.android.fluxc.model.revisions.RevisionModel;
import org.wordpress.android.fluxc.network.rest.wpcom.post.PostRestClient;
import org.wordpress.android.fluxc.network.xmlrpc.post.PostXMLRPCClient;
import org.wordpress.android.fluxc.persistence.PostSqlUtils;
import org.wordpress.android.fluxc.persistence.WellSqlConfig;
import org.wordpress.android.fluxc.store.PostStore;
import org.wordpress.android.util.DateTimeUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

@RunWith(RobolectricTestRunner.class)
public class PostStoreDbIntegrationTest {
    private PostSqlUtils mPostSqlUtils = new PostSqlUtils();
    private PostStore mPostStore = new PostStore(new Dispatcher(), Mockito.mock(PostRestClient.class),
            Mockito.mock(PostXMLRPCClient.class), mPostSqlUtils);

    @Before
    public void setUp() {
        Context appContext = RuntimeEnvironment.application.getApplicationContext();

        List<Class<? extends Identifiable>> modelsToTest = new ArrayList<>();
        modelsToTest.add(PostModel.class);
        modelsToTest.add(LocalDiffModel.class);
        modelsToTest.add(LocalRevisionModel.class);

        WellSqlConfig config = new SingleStoreWellSqlConfigForTests(appContext, modelsToTest, "");
        WellSql.init(config);
        config.reset();
    }

    @Test
    public void testInsertNullPost() {
        assertEquals(0, mPostSqlUtils.insertOrUpdatePostOverwritingLocalChanges(null));

        assertEquals(0, PostTestUtils.getPostsCount());
    }

    @Test
    public void testSimpleInsertionAndRetrieval() {
        PostModel postModel = new PostModel();
        postModel.setRemotePostId(42);
        PostModel result = mPostSqlUtils.insertPostForResult(postModel);

        assertEquals(1, PostTestUtils.getPostsCount());
        assertEquals(42, PostTestUtils.getPosts().get(0).getRemotePostId());
        assertEquals(postModel, result);
    }

    @Test
    public void testInsertWithLocalChanges() {
        PostModel postModel = PostTestUtils.generateSampleUploadedPost();
        postModel.setIsLocallyChanged(true);
        mPostSqlUtils.insertPostForResult(postModel);

        String newTitle = "A different title";
        postModel.setTitle(newTitle);

        assertEquals(0, mPostSqlUtils.insertOrUpdatePostKeepingLocalChanges(postModel));
        assertEquals("A test post", PostTestUtils.getPosts().get(0).getTitle());

        assertEquals(1, mPostSqlUtils.insertOrUpdatePostOverwritingLocalChanges(postModel));
        assertEquals(newTitle, PostTestUtils.getPosts().get(0).getTitle());
    }

    @Test
    public void testUpdateChangesConfirmedForHashcode() {
        // Arrange
        PostModel postModel = new PostModel();
        postModel.setChangesConfirmedContentHashcode(postModel.contentHashcode());

        // Act
        mPostSqlUtils.insertPostForResult(postModel);

        // Assert
        assertEquals(postModel.getChangesConfirmedContentHashcode(),
                PostTestUtils.getPosts().get(0).getChangesConfirmedContentHashcode());
    }

    @Test
    public void testPushAndFetchCollision() throws InterruptedException {
        // Test uploading a post, fetching remote posts and updating the db from the fetch first

        PostModel postModel = PostTestUtils.generateSampleLocalDraftPost();
        mPostSqlUtils.insertPostForResult(postModel);

        // The post after uploading, updated with the remote post ID, about to be saved locally
        PostModel postFromUploadResponse = PostTestUtils.getPosts().get(0);
        postFromUploadResponse.setIsLocalDraft(false);
        postFromUploadResponse.setRemotePostId(42);

        // The same post, but fetched from the server from FETCH_POSTS (so no local ID until insertion)
        final PostModel postFromPostListFetch = postFromUploadResponse.clone();
        postFromPostListFetch.setId(0);

        mPostSqlUtils.insertOrUpdatePostOverwritingLocalChanges(postFromPostListFetch);
        mPostSqlUtils.insertOrUpdatePostOverwritingLocalChanges(postFromUploadResponse);

        assertEquals(1, PostTestUtils.getPosts().size());

        PostModel finalPost = PostTestUtils.getPosts().get(0);
        assertEquals(42, finalPost.getRemotePostId());
        assertEquals(postModel.getLocalSiteId(), finalPost.getLocalSiteId());
    }

    @Test
    public void testInsertWithoutLocalChanges() {
        PostModel postModel = PostTestUtils.generateSampleUploadedPost();
        mPostSqlUtils.insertPostForResult(postModel);

        String newTitle = "A different title";
        postModel.setTitle(newTitle);

        assertEquals(1, mPostSqlUtils.insertOrUpdatePostKeepingLocalChanges(postModel));
        assertEquals(newTitle, PostTestUtils.getPosts().get(0).getTitle());

        newTitle = "Another different title";
        postModel.setTitle(newTitle);

        assertEquals(1, mPostSqlUtils.insertOrUpdatePostOverwritingLocalChanges(postModel));
        assertEquals(newTitle, PostTestUtils.getPosts().get(0).getTitle());
    }

    @Test
    public void testGetPostsForSite() {
        PostModel uploadedPost1 = PostTestUtils.generateSampleUploadedPost();
        mPostSqlUtils.insertPostForResult(uploadedPost1);

        PostModel uploadedPost2 = PostTestUtils.generateSampleUploadedPost();
        uploadedPost2.setLocalSiteId(8);
        mPostSqlUtils.insertPostForResult(uploadedPost2);

        SiteModel site1 = new SiteModel();
        site1.setId(uploadedPost1.getLocalSiteId());

        SiteModel site2 = new SiteModel();
        site2.setId(uploadedPost2.getLocalSiteId());

        assertEquals(2, PostTestUtils.getPostsCount());

        assertEquals(1, mPostStore.getPostsCountForSite(site1));
        assertEquals(1, mPostStore.getPostsCountForSite(site2));
    }

    @Test
    public void testGetPostsWithFormatForSite() {
        PostModel textPost = PostTestUtils.generateSampleUploadedPost();
        PostModel imagePost = PostTestUtils.generateSampleUploadedPost("image");
        PostModel videoPost = PostTestUtils.generateSampleUploadedPost("video");
        mPostSqlUtils.insertPostForResult(textPost);
        mPostSqlUtils.insertPostForResult(imagePost);
        mPostSqlUtils.insertPostForResult(videoPost);

        SiteModel site = new SiteModel();
        site.setId(textPost.getLocalSiteId());

        ArrayList<String> postFormat = new ArrayList<>();
        postFormat.add("image");
        postFormat.add("video");
        List<PostModel> postList = mPostStore.getPostsForSiteWithFormat(site, postFormat);

        assertEquals(2, postList.size());
        assertTrue(postList.contains(imagePost));
        assertTrue(postList.contains(videoPost));
        assertFalse(postList.contains(textPost));
    }

    @Test
    public void testGetPublishedPosts() {
        SiteModel site = new SiteModel();
        site.setId(6);

        PostModel uploadedPost = PostTestUtils.generateSampleUploadedPost();
        mPostSqlUtils.insertPostForResult(uploadedPost);

        PostModel localDraft = PostTestUtils.generateSampleLocalDraftPost();
        mPostSqlUtils.insertPostForResult(localDraft);

        assertEquals(2, PostTestUtils.getPostsCount());
        assertEquals(2, mPostStore.getPostsCountForSite(site));

        assertEquals(1, mPostStore.getUploadedPostsCountForSite(site));
    }

    @Test
    public void testGetPostByLocalId() {
        PostModel post = PostTestUtils.generateSampleLocalDraftPost();
        mPostSqlUtils.insertPostForResult(post);

        assertEquals(post, mPostStore.getPostByLocalPostId(post.getId()));
    }

    @Test
    public void testGetPostByRemoteId() {
        PostModel post = PostTestUtils.generateSampleUploadedPost();
        mPostSqlUtils.insertPostForResult(post);

        SiteModel site = new SiteModel();
        site.setId(6);

        assertEquals(post, mPostStore.getPostByRemotePostId(post.getRemotePostId(), site));
    }

    @Test
    public void testDeleteUploadedPosts() {
        SiteModel site = new SiteModel();
        site.setId(6);

        PostModel uploadedPost1 = PostTestUtils.generateSampleUploadedPost();
        mPostSqlUtils.insertPostForResult(uploadedPost1);

        PostModel uploadedPost2 = PostTestUtils.generateSampleUploadedPost();
        uploadedPost2.setRemotePostId(9);
        mPostSqlUtils.insertPostForResult(uploadedPost2);

        PostModel localDraft = PostTestUtils.generateSampleLocalDraftPost();
        mPostSqlUtils.insertPostForResult(localDraft);

        PostModel locallyChangedPost = PostTestUtils.generateSampleLocallyChangedPost();
        mPostSqlUtils.insertPostForResult(locallyChangedPost);

        assertEquals(4, mPostStore.getPostsCountForSite(site));

        mPostSqlUtils.deleteUploadedPostsForSite(site, false);

        assertEquals(2, mPostStore.getPostsCountForSite(site));
    }

    @Test
    public void testDeletePost() {
        SiteModel site = new SiteModel();
        site.setId(6);

        PostModel uploadedPost1 = PostTestUtils.generateSampleUploadedPost();
        mPostSqlUtils.insertPostForResult(uploadedPost1);

        PostModel uploadedPost2 = PostTestUtils.generateSampleUploadedPost();
        uploadedPost2.setRemotePostId(9);
        mPostSqlUtils.insertPostForResult(uploadedPost2);

        PostModel localDraft = PostTestUtils.generateSampleLocalDraftPost();
        mPostSqlUtils.insertPostForResult(localDraft);

        PostModel locallyChangedPost = PostTestUtils.generateSampleLocallyChangedPost();
        mPostSqlUtils.insertPostForResult(locallyChangedPost);

        assertEquals(4, mPostStore.getPostsCountForSite(site));

        mPostSqlUtils.deletePost(uploadedPost1);

        assertEquals(null, mPostStore.getPostByLocalPostId(uploadedPost1.getId()));
        assertEquals(3, mPostStore.getPostsCountForSite(site));

        mPostSqlUtils.deletePost(uploadedPost2);
        mPostSqlUtils.deletePost(localDraft);

        assertNotEquals(null, mPostStore.getPostByLocalPostId(locallyChangedPost.getId()));
        assertEquals(1, mPostStore.getPostsCountForSite(site));

        mPostSqlUtils.deletePost(locallyChangedPost);

        assertEquals(null, mPostStore.getPostByLocalPostId(locallyChangedPost.getId()));
        assertEquals(0, mPostStore.getPostsCountForSite(site));
        assertEquals(0, PostTestUtils.getPostsCount());
    }

    @Test
    public void testPostAndPageSeparation() {
        SiteModel site = new SiteModel();
        site.setId(6);

        PostModel post = new PostModel();
        post.setLocalSiteId(6);
        post.setRemotePostId(42);
        mPostSqlUtils.insertPostForResult(post);

        PostModel page = new PostModel();
        page.setIsPage(true);
        page.setLocalSiteId(6);
        page.setRemotePostId(43);
        mPostSqlUtils.insertPostForResult(page);

        assertEquals(2, PostTestUtils.getPostsCount());

        assertEquals(1, mPostStore.getPostsCountForSite(site));
        assertEquals(1, mPostStore.getPagesCountForSite(site));

        assertFalse(PostTestUtils.getPosts().get(0).isPage());
        assertTrue(PostTestUtils.getPosts().get(1).isPage());

        assertEquals(1, mPostStore.getUploadedPostsCountForSite(site));
        assertEquals(1, mPostStore.getUploadedPagesCountForSite(site));
    }

    @Test
    public void testPostOrder() {
        SiteModel site = new SiteModel();
        site.setId(6);

        PostModel post = new PostModel();
        post.setLocalSiteId(6);
        post.setRemotePostId(42);
        post.setDateCreated(DateTimeUtils.iso8601UTCFromDate(new Date()));
        mPostSqlUtils.insertPostForResult(post);

        PostModel localDraft = new PostModel();
        localDraft.setLocalSiteId(6);
        localDraft.setIsLocalDraft(true);
        localDraft.setDateCreated("2016-01-01T07:00:00+00:00");
        mPostSqlUtils.insertPostForResult(localDraft);

        PostModel scheduledPost = new PostModel();
        scheduledPost.setLocalSiteId(6);
        scheduledPost.setRemotePostId(23);
        scheduledPost.setDateCreated("2056-01-01T07:00:00+00:00");
        mPostSqlUtils.insertPostForResult(scheduledPost);

        List<PostModel> posts = mPostSqlUtils.getPostsForSite(site, false);

        // Expect order draft > scheduled > published
        assertTrue(posts.get(0).isLocalDraft());
        assertEquals(23, posts.get(1).getRemotePostId());
        assertEquals(42, posts.get(2).getRemotePostId());
    }

    @Test
    public void testRemoveAllPosts() {
        PostModel uploadedPost1 = PostTestUtils.generateSampleUploadedPost();
        mPostSqlUtils.insertPostForResult(uploadedPost1);

        PostModel uploadedPost2 = PostTestUtils.generateSampleUploadedPost();
        uploadedPost2.setLocalSiteId(8);
        mPostSqlUtils.insertPostForResult(uploadedPost2);

        assertEquals(2, PostTestUtils.getPostsCount());

        mPostSqlUtils.deleteAllPosts();

        assertEquals(0, PostTestUtils.getPostsCount());
    }

    @Test
    public void testNumLocalChanges() {
        // first make sure there aren't any local changes
        assertEquals(mPostStore.getNumLocalChanges(), 0);

        // then add a post with local changes and ensure we get the correct count
        PostModel testPost = PostTestUtils.generateSampleLocalDraftPost();
        testPost.setIsLocallyChanged(true);
        mPostSqlUtils.insertOrUpdatePost(testPost, true);
        assertEquals(mPostStore.getNumLocalChanges(), 1);

        // delete the post and again check the count
        mPostSqlUtils.deletePost(testPost);
        assertEquals(mPostStore.getNumLocalChanges(), 0);
    }

    @Test
    public void testSavingAndRetrievalOfLocalRevision() {
        RevisionModel testRevisionModel = PostTestUtils.generateSamplePostRevision();
        SiteModel site = new SiteModel();
        site.setSiteId(77);

        PostModel postModel = PostTestUtils.generateSampleLocalDraftPost();
        mPostStore.setLocalRevision(testRevisionModel, site, postModel);

        RevisionModel retrievedRevision = mPostStore.getLocalRevision(site, postModel);

        assertTrue(testRevisionModel.equals(retrievedRevision));
    }

    @Test
    public void testUpdatingLocalRevision() {
        RevisionModel testRevisionModel = PostTestUtils.generateSamplePostRevision();
        SiteModel site = new SiteModel();
        site.setSiteId(77);

        PostModel postModel = PostTestUtils.generateSampleLocalDraftPost();
        mPostStore.setLocalRevision(testRevisionModel, site, postModel);

        testRevisionModel.setPostContent("new content");
        testRevisionModel.getContentDiffs().add(new Diff(DiffOperations.ADD, "new line"));
        mPostStore.setLocalRevision(testRevisionModel, site, postModel);

        RevisionModel retrievedRevision = mPostStore.getLocalRevision(site, postModel);

        assertTrue(testRevisionModel.equals(retrievedRevision));
    }

    @Test
    public void testDeleteLocalRevision() {
        RevisionModel testRevisionModel = PostTestUtils.generateSamplePostRevision();
        SiteModel site = new SiteModel();
        site.setSiteId(77);

        PostModel postModel = PostTestUtils.generateSampleLocalDraftPost();

        mPostStore.setLocalRevision(testRevisionModel, site, postModel);
        assertNotNull(mPostStore.getLocalRevision(site, postModel));

        mPostStore.deleteLocalRevision(testRevisionModel, site, postModel);
        assertNull(mPostStore.getLocalRevision(site, postModel));
    }

    @Test
    public void testDeleteLocalRevisionOfAPostOrPage() {
        RevisionModel testRevisionModel = PostTestUtils.generateSamplePostRevision();
        SiteModel site = new SiteModel();
        site.setSiteId(77);

        PostModel postModel = PostTestUtils.generateSampleLocalDraftPost();
        postModel.setRemoteSiteId(77);

        mPostStore.setLocalRevision(testRevisionModel, site, postModel);
        assertNotNull(mPostStore.getLocalRevision(site, postModel));

        mPostStore.deleteLocalRevisionOfAPostOrPage(postModel);
        assertNull(mPostStore.getLocalRevision(site, postModel));
    }

    @Test
    public void testGetLocalDraftPostsMethodOnlyReturnsLocalDrafts() {
        // Arrange
        final String baseTitle = "Alexandrine Thiel";
        for (int i = 0; i < 3; i++) {
            final String compoundTitle = baseTitle.concat(":").concat(UUID.randomUUID().toString());
            final PostModel post = PostTestUtils.generateSampleLocalDraftPost(compoundTitle);
            mPostSqlUtils.insertPostForResult(post);
        }

        final PostModel localDraftPage = PostTestUtils.generateSampleLocalDraftPost();
        localDraftPage.setIsPage(true);
        mPostSqlUtils.insertPostForResult(localDraftPage);

        final PostModel uploadedPost = PostTestUtils.generateSampleUploadedPost();
        mPostSqlUtils.insertPostForResult(uploadedPost);

        final SiteModel site = new SiteModel();
        site.setId(PostTestUtils.DEFAULT_LOCAL_SITE_ID);

        // Act
        final List<PostModel> localDraftPosts = mPostStore.getLocalDraftPosts(site);

        // Assert
        assertEquals(3, localDraftPosts.size());
        for (PostModel localDraftPost : localDraftPosts) {
            assertTrue(localDraftPost.isLocalDraft());
            assertTrue(localDraftPost.getTitle().startsWith(baseTitle));

            assertNotEquals(uploadedPost.getId(), localDraftPost.getId());
            assertNotEquals(localDraftPage.getId(), localDraftPost.getId());
        }
    }

    @Test
    public void testGetPostsWithLocalChangesReturnsLocalDraftsAndChangedPostsOnly() {
        // Arrange
        final SiteModel site = new SiteModel();
        site.setId(PostTestUtils.DEFAULT_LOCAL_SITE_ID);

        // Objects that should be included
        final ArrayList<Integer> expectedPostIds = new ArrayList<>();
        final String baseTitle = "Rerum";
        for (int i = 0; i < 3; i++) {
            final String compoundTitle = baseTitle.concat(":").concat(UUID.randomUUID().toString());
            final PostModel post = PostTestUtils.generateSampleLocalDraftPost(compoundTitle);
            mPostSqlUtils.insertPostForResult(post);
            expectedPostIds.add(post.getId());
        }
        for (int i = 0; i < 5; i++) {
            final String compoundTitle = baseTitle.concat(":").concat(UUID.randomUUID().toString());
            final PostModel post = PostTestUtils.generateSampleLocallyChangedPost(compoundTitle);
            mPostSqlUtils.insertPostForResult(post);
            expectedPostIds.add(post.getId());
        }

        final PostModel modifiedUploadPost = PostTestUtils.generateSampleUploadedPost();
        modifiedUploadPost.setTitle(baseTitle.concat("-c_up_post"));
        modifiedUploadPost.setIsLocallyChanged(true);
        mPostSqlUtils.insertPostForResult(modifiedUploadPost);
        expectedPostIds.add(modifiedUploadPost.getId());

        final PostModel modifiedPublishedPost = PostTestUtils.generateSampleUploadedPost();
        modifiedPublishedPost.setTitle(baseTitle.concat("-mpp"));
        modifiedPublishedPost.setIsLocallyChanged(true);
        modifiedPublishedPost.setStatus(PostStatus.PUBLISHED.toString());
        mPostSqlUtils.insertPostForResult(modifiedPublishedPost);
        expectedPostIds.add(modifiedPublishedPost.getId());

        // Objects that should not be included
        final PostModel localDraftPage = PostTestUtils.generateSampleLocalDraftPost();
        localDraftPage.setIsPage(true);
        mPostSqlUtils.insertPostForResult(localDraftPage);

        final PostModel unchangedUploadedPost = PostTestUtils.generateSampleUploadedPost();
        mPostSqlUtils.insertPostForResult(unchangedUploadedPost);

        final PostModel unchangedPublishedPost = PostTestUtils.generateSampleUploadedPost();
        modifiedPublishedPost.setStatus(PostStatus.PUBLISHED.toString());
        mPostSqlUtils.insertPostForResult(unchangedPublishedPost);

        final List<Integer> unexpectedPostIds = Arrays.asList(
                localDraftPage.getId(),
                unchangedUploadedPost.getId(),
                unchangedPublishedPost.getId());

        // Act
        final List<PostModel> locallyChangedPosts = mPostStore.getPostsWithLocalChanges(site);

        // Assert
        assertEquals(expectedPostIds.size(), locallyChangedPosts.size());
        for (PostModel locallyChangedPost : locallyChangedPosts) {
            assertThat(locallyChangedPost.getId()).isNotIn(unexpectedPostIds);
            assertThat(locallyChangedPost.getId()).isIn(expectedPostIds);

            assertTrue(locallyChangedPost.isLocalDraft() || locallyChangedPost.isLocallyChanged());
            assertThat(locallyChangedPost.getTitle()).startsWith(baseTitle);
        }
    }


    /**
     * Tests that getPostsByLocalOrRemotePostIds works correctly in various situations.
     * <p>
     * Normally it's not a good idea to combine multiple tests like this, however due to Java's verbosity the tests
     * are combined to avoid having too much boilerplate code.
     */
    @Test
    public void testGetPostsByLocalOrRemoteIdsForOnlyLocalIds() {
        int localSiteId = 123;
        SiteModel site = new SiteModel();
        site.setId(localSiteId);
        int numberOfLocalPosts = 12;
        int numberOfRemotePosts = 129;

        // Setup local and remote ids
        List<LocalId> localIds = new ArrayList<>(numberOfLocalPosts);
        for (int i = 1; i <= numberOfLocalPosts; i++) {
            localIds.add(new LocalId(i));
        }
        ArrayList<RemoteId> remoteIds = new ArrayList<>(numberOfRemotePosts);
        for (int i = 1; i <= numberOfRemotePosts; i++) {
            remoteIds.add(new RemoteId(i));
        }
        List<LocalOrRemoteId> localAndRemoteIds = new ArrayList<>(localIds.size() + remoteIds.size());
        localAndRemoteIds.addAll(localIds);
        localAndRemoteIds.addAll(remoteIds);

        // Insert the posts for the local and remote ids
        generateAndInsertPosts(localSiteId, localIds, remoteIds);

        // Assert that querying localIds will only return local posts
        List<PostModel> retrievedLocalPosts = mPostStore.getPostsByLocalOrRemotePostIds(localIds, site);
        assertEquals(localIds.size(), retrievedLocalPosts.size());
        for (PostModel localPost : retrievedLocalPosts) {
            assertTrue(localPost.isLocalDraft());
        }

        // Assert that querying remoteIds only return remote posts
        List<PostModel> retrievedRemotePosts = mPostStore.getPostsByLocalOrRemotePostIds(remoteIds, site);
        assertEquals(remoteIds.size(), retrievedRemotePosts.size());
        for (PostModel remotePost : retrievedRemotePosts) {
            assertFalse(remotePost.isLocalDraft());
        }

        // Assert that querying both local and remote ids we retrieve all the posts
        List<PostModel> retrievedLocalAndRemotePosts =
                mPostStore.getPostsByLocalOrRemotePostIds(localAndRemoteIds, site);
        assertEquals(localIds.size() + remoteIds.size(), retrievedLocalAndRemotePosts.size());
    }

    private void generateAndInsertPosts(int localSiteId, List<LocalId> localIds, List<RemoteId> remoteIds) {
        for (int i = 1; i <= localIds.size(); i++) {
            PostModel post = PostTestUtils.generateSampleLocalDraftPost();
            post.setLocalSiteId(localSiteId);
            mPostSqlUtils.insertOrUpdatePost(post, false);
        }

        for (RemoteId remoteId : remoteIds) {
            PostModel post = PostTestUtils.generateSampleUploadedPost();
            post.setLocalSiteId(localSiteId);
            post.setRemotePostId(remoteId.getValue());
            mPostSqlUtils.insertOrUpdatePost(post, false);
        }
    }
}
