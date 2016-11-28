package org.wordpress.android.fluxc.comment;

import android.content.Context;

import com.yarolegovich.wellsql.WellSql;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.wordpress.android.fluxc.model.CommentModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.persistence.CommentSqlUtils;
import org.wordpress.android.fluxc.persistence.WellSqlConfig;

import static org.junit.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
public class CommentStoreUnitTest {
    @Before
    public void setUp() {
        Context appContext = RuntimeEnvironment.application.getApplicationContext();
        WellSqlConfig config = new WellSqlConfig(appContext);
        WellSql.init(config);
        config.reset();
    }

    @Test
    public void testGetCommentBySiteAndRemoteId() {
        SiteModel siteModel = new SiteModel();
        siteModel.setId(21);
        long remoteCommentId = 42;

        // Init Comment Model
        CommentModel commentModel = new CommentModel();
        commentModel.setContent("Best ponies come from the future.");
        commentModel.setLocalSiteId(siteModel.getId());
        commentModel.setRemoteCommentId(remoteCommentId);
        CommentSqlUtils.insertOrUpdateComment(commentModel);

        // Get comment by site and remote id
        CommentModel queriedComment = CommentSqlUtils.getCommentBySiteAndRemoteId(siteModel, remoteCommentId);
        assertEquals("Best ponies come from the future.", queriedComment.getContent());
    }

    @Test
    public void testMultiGetCommentBySiteAndRemoteId() {
        SiteModel siteModel = new SiteModel();
        siteModel.setId(21);

        // Init Comment Models
        CommentModel commentModel = new CommentModel();
        commentModel.setLocalSiteId(siteModel.getId());
        commentModel.setContent("Pony #1");
        commentModel.setRemoteCommentId(1);
        CommentSqlUtils.insertOrUpdateComment(commentModel);

        commentModel = new CommentModel();
        commentModel.setLocalSiteId(siteModel.getId());
        commentModel.setContent("Pony #2");
        commentModel.setRemoteCommentId(2);
        CommentSqlUtils.insertOrUpdateComment(commentModel);

        commentModel = new CommentModel();
        commentModel.setLocalSiteId(siteModel.getId());
        commentModel.setContent("Pony #3");
        commentModel.setRemoteCommentId(3);
        CommentSqlUtils.insertOrUpdateComment(commentModel);

        // Get comment by site and remote id
        CommentModel queriedComment = CommentSqlUtils.getCommentBySiteAndRemoteId(siteModel, 1);
        assertEquals("Pony #1", queriedComment.getContent());

        // Get comment by site and remote id
        queriedComment = CommentSqlUtils.getCommentBySiteAndRemoteId(siteModel, 3);
        assertEquals("Pony #3", queriedComment.getContent());

        // Get comment by site and remote id
        queriedComment = CommentSqlUtils.getCommentBySiteAndRemoteId(siteModel, 2);
        assertEquals("Pony #2", queriedComment.getContent());
    }


    @Test
    public void testFailToGetCommentBySiteAndRemoteId() {
        assertEquals(null, CommentSqlUtils.getCommentBySiteAndRemoteId(new SiteModel(), 42));
    }
}
