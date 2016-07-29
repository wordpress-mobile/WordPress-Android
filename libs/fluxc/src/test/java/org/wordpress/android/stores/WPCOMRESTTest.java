package org.wordpress.android.stores;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.wordpress.android.stores.network.rest.wpcom.WPCOMREST;

import static org.junit.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
public class WPCOMRESTTest {
    private static final String WPCOM_REST_PREFIX = "https://public-api.wordpress.com/rest";
    private static final String WPCOM_PREFIX_V1 = WPCOM_REST_PREFIX + "/v1";
    private static final String WPCOM_PREFIX_V1_1 = WPCOM_REST_PREFIX + "/v1.1";
    private static final String WPCOM_PREFIX_V1_2 = WPCOM_REST_PREFIX + "/v1.2";
    private static final String WPCOM_PREFIX_V1_3 = WPCOM_REST_PREFIX + "/v1.3";

    @Test
    public void testWithParams() {
        assertEquals("/sites/546/posts/", WPCOMREST.POSTS.withSiteId(546));
        assertEquals("/sites/546/posts/6/delete", WPCOMREST.POST_DELETE.withSiteIdAndContentId(546, 6));

        assertEquals(WPCOM_PREFIX_V1 + "/sites/546/posts/", WPCOMREST.POSTS.getUrlV1WithSiteId(546));
        assertEquals(WPCOM_PREFIX_V1 + "/sites/546/posts/6/delete",
                WPCOMREST.POST_DELETE.getUrlV1WithSiteIdAndContentId(546, 6));
    }
}
