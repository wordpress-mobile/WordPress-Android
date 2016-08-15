package org.wordpress.android.fluxc.network.rest.wpcom;

import java.lang.String;
import org.wordpress.android.fluxc.annotations.Endpoint;
import org.wordpress.android.fluxc.annotations.endpoint.WPComEndpoint;

public class WPCOMRESTGen {
    @Endpoint("/sites/")
    public static SitesEndpoint sites = new SitesEndpoint("/");

    @Endpoint("/users/")
    public static UsersEndpoint users = new UsersEndpoint("/");

    @Endpoint("/$test/")
    public static TestEndpoint test(long testId) {
        return new TestEndpoint("/", testId);
    }

    public static class SitesEndpoint extends WPComEndpoint {
        private static final String SITES_ENDPOINT = "sites/";

        @Endpoint("/sites/new/")
        public WPComEndpoint new_ = new WPComEndpoint(getEndpoint() + "new/");

        private SitesEndpoint(String previousEndpoint) {
            super(previousEndpoint + SITES_ENDPOINT);
        }

        @Endpoint("/sites/$site/")
        public SiteEndpoint site(long siteId) {
            return new SiteEndpoint(getEndpoint(), siteId);
        }

        public static class SiteEndpoint extends WPComEndpoint {
            @Endpoint("/sites/$site/posts/")
            public PostsEndpoint posts = new PostsEndpoint(getEndpoint());

            @Endpoint("/sites/$site/post-formats/")
            public WPComEndpoint post_formats = new WPComEndpoint(getEndpoint() + "post-formats/");

            private SiteEndpoint(String previousEndpoint, long siteId) {
                super(previousEndpoint, siteId);
            }

            public static class PostsEndpoint extends WPComEndpoint {
                private static final String POSTS_ENDPOINT = "posts/";

                @Endpoint("/sites/$site/posts/new/")
                public WPComEndpoint new_ = new WPComEndpoint(getEndpoint() + "new/");

                private PostsEndpoint(String previousEndpoint) {
                    super(previousEndpoint + POSTS_ENDPOINT);
                }

                @Endpoint("/sites/$site/posts/$post_ID/")
                public PostEndpoint post(long postId) {
                    return new PostEndpoint(getEndpoint(), postId);
                }

                public static class PostEndpoint extends WPComEndpoint {
                    @Endpoint("/sites/$site/posts/$post_ID/delete/")
                    public WPComEndpoint delete = new WPComEndpoint(getEndpoint() + "delete/");

                    private PostEndpoint(String previousEndpoint, long postId) {
                        super(previousEndpoint, postId);
                    }
                }
            }
        }
    }

    public static class UsersEndpoint extends WPComEndpoint {
        private static final String USERS_ENDPOINT = "users/";

        @Endpoint("/users/new/")
        public WPComEndpoint new_ = new WPComEndpoint(getEndpoint() + "new/");

        private UsersEndpoint(String previousEndpoint) {
            super(previousEndpoint + USERS_ENDPOINT);
        }

        @Endpoint("/users/$user/")
        public WPComEndpoint user(long userId) {
            return new WPComEndpoint(getEndpoint(), userId);
        }
    }

    public static class TestEndpoint extends WPComEndpoint {
        @Endpoint("/$test/sub/")
        public WPComEndpoint sub = new WPComEndpoint(getEndpoint() + "sub/");

        private TestEndpoint(String previousEndpoint, long testId) {
            super(previousEndpoint, testId);
        }
    }
}
