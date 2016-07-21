package org.wordpress.android.stores;

import android.content.Context;

import com.yarolegovich.wellsql.WellSql;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.wordpress.android.stores.model.PostModel;
import org.wordpress.android.stores.network.rest.wpcom.post.PostRestClient;
import org.wordpress.android.stores.network.xmlrpc.post.PostXMLRPCClient;
import org.wordpress.android.stores.persistence.WellSqlConfig;
import org.wordpress.android.stores.store.PostStore;

import static org.junit.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
public class PostStoreUnitTest {
    private PostStore mPostStore = new PostStore(new Dispatcher(), Mockito.mock(PostRestClient.class),
            Mockito.mock(PostXMLRPCClient.class));

    @Before
    public void setUp() {
        Context appContext = RuntimeEnvironment.application.getApplicationContext();

        WellSqlConfig config = new SingleStoreWellSqlConfigForTests(appContext, PostModel.class);
        WellSql.init(config);
        config.reset();
    }

    @Test
    public void testSimpleInsertionAndRetrieval() {
        PostModel postModel = new PostModel();
        postModel.setPostId(42);
        WellSql.insert(postModel).execute();

        assertEquals(1, mPostStore.getPostsCount());

        assertEquals(42, mPostStore.getPosts().get(0).getPostId());
    }
}
