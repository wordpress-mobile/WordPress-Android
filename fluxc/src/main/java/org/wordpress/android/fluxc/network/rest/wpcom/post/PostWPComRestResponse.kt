package org.wordpress.android.fluxc.network.rest.wpcom.post

import com.google.gson.Gson
import com.google.gson.JsonIOException
import com.google.gson.JsonSyntaxException
import com.google.gson.TypeAdapter
import com.google.gson.TypeAdapterFactory
import com.google.gson.annotations.JsonAdapter
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken.BEGIN_ARRAY
import com.google.gson.stream.JsonToken.BEGIN_OBJECT
import com.google.gson.stream.JsonWriter
import org.wordpress.android.fluxc.network.rest.wpcom.post.PostWPComRestResponse.PostMeta.PostData.PostAutoSave
import org.wordpress.android.fluxc.network.rest.wpcom.taxonomy.TermWPComRestResponse
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T.POSTS
import java.io.IOException

data class PostWPComRestResponse(
    @SerializedName("ID") val remotePostId: Long = 0,
    @SerializedName("site_ID") val remoteSiteId: Long = 0,
    @SerializedName("date") val date: String? = null,
    @SerializedName("modified") val modified: String? = null,
    @SerializedName("title") val title: String? = null,
    @SerializedName("URL") val url: String? = null,
    @SerializedName("short_URL") val shortUrl: String? = null,
    @SerializedName("content") val content: String? = null,
    @SerializedName("excerpt") val excerpt: String? = null,
    @SerializedName("slug") val slug: String? = null,
    @SerializedName("guid") val guid: String? = null,
    @SerializedName("status") val status: String? = null,
    @SerializedName("sticky") val sticky: Boolean = false,
    @SerializedName("password") val password: String? = null,
    @SerializedName("parent") val parent: PostParent? = null,
    @SerializedName("type") val type: String,
    @SerializedName("featured_image") val featuredImage: String? = null,
    @SerializedName("post_thumbnail") val postThumbnail: PostThumbnail? = null,
    @SerializedName("format") val format: String? = null,
    @SerializedName("geo") val geo: GeoLocation? = null,
    @SerializedName("tags") val tags: Map<String, TermWPComRestResponse>? = null,
    @SerializedName("categories") val categories: Map<String, TermWPComRestResponse>? = null,
    @SerializedName("capabilities") val capabilities: Capabilities? = null,
    @SerializedName("meta") val meta: PostMeta? = null,
    @SerializedName("metadata") @JsonAdapter(MetaDataAdapterFactory::class) val metadata: List<PostMetaData>? = null,
    @SerializedName("author") val author: Author? = null
) {
    data class PostsResponse(
        @SerializedName("posts") val posts: List<PostWPComRestResponse>,
        @SerializedName("found") val found: Int
    )

    data class PostThumbnail(
        @SerializedName("ID") val id: Long = 0,
        @SerializedName("URL") val url: String? = null,
        @SerializedName("guid") val guid: String? = null,
        @SerializedName("mime_type") val mimeType: String? = null,
        @SerializedName("width") val width: Int = 0,
        @SerializedName("height") val height: Int = 0
    )

    data class Capabilities(
        @SerializedName("publish_post") val publishPost: Boolean = false,
        @SerializedName("edit_post") val editPost: Boolean = false,
        @SerializedName("delete_post") val deletePost: Boolean = false
    )

    data class PostMeta(@SerializedName("data") val data: PostData? = null) {
        data class PostData(@SerializedName("autosave") val autoSave: PostAutoSave? = null) {
            data class PostAutoSave(
                @SerializedName("ID") var revisionId: Long = 0,
                @SerializedName("modified") var modified: String? = null,
                @SerializedName("preview_URL") var previewUrl: String? = null,
                @SerializedName("title") var title: String? = null,
                @SerializedName("content") var content: String? = null,
                @SerializedName("excerpt") var excerpt: String? = null
            )
        }
    }

    data class PostMetaData(
        @SerializedName("id") var id: Long = 0,
        @SerializedName("key") var key: String? = null,
        @SerializedName("value") var value: Any? = null
    )

    fun getPostAutoSave(): PostAutoSave? {
        return meta?.data?.autoSave
    }

    data class Author(
        @SerializedName("ID") val id: Long = 0,
        @SerializedName("name") val name: String?
    )

    @Suppress("UNCHECKED_CAST")
    class MetaDataAdapterFactory : TypeAdapterFactory {
        override fun <T> create(gson: Gson, type: TypeToken<T>): TypeAdapter<T> {
            return MetaDataAdapter(gson) as TypeAdapter<T>
        }
    }

    class MetaDataAdapter(private val gson: Gson) : TypeAdapter<List<PostMetaData>>() {
        @Throws(IOException::class)
        override
        fun read(jsonReader: JsonReader): List<PostMetaData> {
            val metaDataList = arrayListOf<PostMetaData>()

            // Noticed several metadata formats in the json response like
            // {"metadata”:[{“id”:”5”,”key”:”geo_latitude”,”value”:”28.6139391”}]}
            // {"metadata”:[false]}, {"metadata":false},
            // {"metadata":[{"id":"15","key":"switch_like_status","value":[0]}]}
            // Returning only a list of PostMetaData type or empty list for other formats not needed currently.

            when (jsonReader.peek()) {
                BEGIN_ARRAY -> {
                    jsonReader.beginArray()
                    while (jsonReader.hasNext()) {
                        if (BEGIN_OBJECT == jsonReader.peek()) {
                            val type = object : TypeToken<PostMetaData>() {}.type
                            try {
                                metaDataList.add(gson.fromJson(jsonReader, type))
                            } catch (ex: Exception) {
                                when (ex) {
                                    is JsonSyntaxException,
                                    is JsonIOException -> {
                                        AppLog.w(POSTS, "Error in post metadata json conversion: " + ex.message)
                                        jsonReader.skipValue()
                                    }
                                }
                            }
                        } else {
                            jsonReader.skipValue()
                        }
                    }
                    jsonReader.endArray()
                }
                else -> {
                    jsonReader.skipValue()
                }
            }

            return metaDataList
        }

        override fun write(out: JsonWriter?, value: List<PostMetaData>) { // Do Nothing
        }
    }
}
