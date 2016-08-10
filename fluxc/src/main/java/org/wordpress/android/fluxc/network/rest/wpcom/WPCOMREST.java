package org.wordpress.android.fluxc.network.rest.wpcom;

import org.wordpress.android.fluxc.annotations.Endpoint;

public class WPCOMREST {
    // Top-level endpoints
    @Endpoint("/me/")
    public static MeEndpoint me = new MeEndpoint();
    @Endpoint("/sites/")
    public static SitesEndpoint sites = new SitesEndpoint();
    @Endpoint("/users/")
    public static UsersEndpoint users = new UsersEndpoint();

    public static class MeEndpoint extends WPComEndpoint {
        private static final String ME_ENDPOINT = "/me/";

        @Endpoint("/me/settings/")
        public WPComEndpoint settings = new WPComEndpoint(ME_ENDPOINT + "settings/");
        @Endpoint("/me/sites/")
        public WPComEndpoint sites = new WPComEndpoint(ME_ENDPOINT + "sites/");

        private MeEndpoint() {
            super(ME_ENDPOINT);
        }
    }

    public static class SitesEndpoint extends WPComEndpoint {
        private static final String SITES_ENDPOINT = "/sites/";

        @Endpoint("/sites/new/")
        public WPComEndpoint new_ = new WPComEndpoint(SITES_ENDPOINT + "new/");

        private SitesEndpoint() {
            super("/sites/");
        }

        @Endpoint("/sites/$site/")
        public SiteEndpoint site(long siteId) {
            return new SiteEndpoint(getEndpoint(), siteId);
        }

        public static class SiteEndpoint extends WPComEndpoint {
            @Endpoint("/sites/$site/posts/")
            public final PostsEndpoint posts = new PostsEndpoint(getEndpoint());

            private SiteEndpoint(String previousEndpoint, long siteId) {
                super(previousEndpoint, siteId);
            }

            public static class PostsEndpoint extends WPComEndpoint {
                @Endpoint("/sites/$site/posts/new/")
                public WPComEndpoint new_ = new WPComEndpoint(getEndpoint() + "new/");

                private PostsEndpoint(String previousEndpoint) {
                    super(previousEndpoint + "posts/");
                }

                @Endpoint("/sites/$site/posts/$post_ID/")
                public PostEndpoint post(long postId) {
                    return new PostEndpoint(getEndpoint(), postId);
                }

                public static class PostEndpoint extends WPComEndpoint {
                    @Endpoint("/sites/$site/posts/$post_ID/delete/")
                    public final WPComEndpoint delete = new WPComEndpoint(getEndpoint() + "delete/");

                    private PostEndpoint(String previousEndpoint, long postId) {
                        super(previousEndpoint, postId);
                    }
                }
            }
        }
    }

    public static class UsersEndpoint extends WPComEndpoint {
        private static final String USERS_ENDPOINT = "/users/";

        @Endpoint("/users/new/")
        public WPComEndpoint new_ = new WPComEndpoint(USERS_ENDPOINT + "new/");

        private UsersEndpoint() {
            super(USERS_ENDPOINT);
        }
    }
}
