package org.wordpress.android.fluxc.network.rest.wpcom.post;

import org.wordpress.android.fluxc.Payload;
import org.wordpress.android.fluxc.network.Response;
import org.wordpress.android.fluxc.network.rest.wpcom.taxonomy.TermWPComRestResponse;

import java.util.List;
import java.util.Map;

public class PostWPComRestResponse extends Payload implements Response {
    public class PostsResponse {
        public List<PostWPComRestResponse> posts;
    }

    public class PostThumbnail {
        public long ID;
        public String URL;
        public String guid;
        public String mime_type;
        public int width;
        public int height;
    }

    public class Capabilities {
        public boolean publish_post;
        public boolean edit_post;
        public boolean delete_post;
    }

    public long ID;
    public long site_ID;
    public String date;
    public String modified;
    public String title;
    public String URL;
    public String short_URL;
    public String content;
    public String excerpt;
    public String slug;
    public String guid;
    public String status;
    public boolean sticky;
    public String password;

    public PostParent parent;

    public String type;
    public String featured_image;
    // TODO: This can probably use MediaModel instead, and we can drop PostThumbnail
    public PostThumbnail post_thumbnail;
    public String format;
    public GeoLocation geo;
    public Map<String, TermWPComRestResponse> tags;
    public Map<String, TermWPComRestResponse> categories;
    public Capabilities capabilities;
}
