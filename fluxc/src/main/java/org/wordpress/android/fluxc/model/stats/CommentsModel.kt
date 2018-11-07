package org.wordpress.android.fluxc.model.stats

data class CommentsModel(
    var date: String? = null,
    var blogID: Long = 0,
    val monthlyComments: Int,
    val totalComments: Int,
    val mostActiveDay: String,
    val mostActiveTime: String,
    var posts: List<Post>,
    var authors: List<Author>? = null
) {
    data class Post(val id: Int, val name: String, val comments: Int, val link: String)
    /*
    String name = currentAuthorJSON.getString("name");
                int comments = currentAuthorJSON.getInt("comments");
                String url = currentAuthorJSON.getString("link");
                String gravatar = currentAuthorJSON.getString("gravatar");
                JSONObject followData = currentAuthorJSON.optJSONObject("follow_data");
     */
    data class Author(val name: String, val comments: Int, val link: String, val gravatar: String )
}