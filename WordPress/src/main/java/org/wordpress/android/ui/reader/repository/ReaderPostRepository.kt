package org.wordpress.android.ui.reader.repository

import android.util.Log
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode.MAIN
import org.json.JSONObject
import org.wordpress.android.models.ReaderPostList
import org.wordpress.android.models.ReaderTag
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.modules.IO_THREAD
import org.wordpress.android.ui.reader.ReaderEvents.SearchPostsEnded
import org.wordpress.android.ui.reader.ReaderEvents.SearchPostsStarted
import org.wordpress.android.ui.reader.ReaderEvents.UpdatePostsEnded
import org.wordpress.android.ui.reader.ReaderEvents.UpdatePostsStarted
import org.wordpress.android.ui.reader.repository.ReaderDataRequest.DiscoverRequest
import org.wordpress.android.ui.reader.repository.ReaderDataRequest.PostsForBlogRequest
import org.wordpress.android.ui.reader.repository.ReaderDataRequest.PostsForFeedRequest
import org.wordpress.android.ui.reader.repository.ReaderDataRequest.PostsForTagRequest
import org.wordpress.android.ui.reader.services.post.ReaderPostServiceStarter.UpdateAction
import org.wordpress.android.util.EventBusWrapper
import org.wordpress.android.viewmodel.ContextProvider
import java.util.concurrent.TimeUnit.SECONDS
import javax.inject.Inject
import javax.inject.Named

// todo: annmarie
// todo: check for network connectivity
// todo: add call to service
// todo: add calls to db
// todo: move all those classes/interfaces (at tht bottom) elsewhere
class ReaderPostRepository @Inject constructor(
    private val contextProvider: ContextProvider,
    private val eventBusWrapper: EventBusWrapper,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
    @Named(IO_THREAD) private val ioDispatcher: CoroutineDispatcher
) {
    private val mutableDiscoverFeed = ReactiveMutableLiveData<ReaderDataWrapper<*>>(
            onActive = { onActive() }, onInactive = { onInactive() })
    val discoverFeed: ReactiveMutableLiveData<ReaderDataWrapper<*>> = mutableDiscoverFeed


    private suspend fun getMockDiscoverFeed() {
        return withContext(ioDispatcher) {
            mutableDiscoverFeed.postValue(
                ReaderDataWrapper.Success(data = ReaderPostList.fromJson(JSONObject(discoverJson))))
        }
    }

    private suspend fun getMockError() {
        return withContext(ioDispatcher) {
            mutableDiscoverFeed.postValue(
                    ReaderDataWrapper.Error.NetworkError(Exception("There was a network error")))
        }
    }

    private suspend fun getMockInProgress() {
        return withContext(ioDispatcher) {
            mutableDiscoverFeed.postValue( ReaderDataWrapper.InProgress)
        }
    }

    // LiveData callbacks
    // We can use the same active & inactive listeners across all live data
    // Not sure if this is going to be used
    private fun onActive() {
            Log.i(javaClass.simpleName, "***=> OnActive reached")
        }

    private fun onInactive() {
        Log.i(javaClass.simpleName, "***=> OnInactive reached")
    }

    suspend fun requestDiscoverFeed(request: DiscoverRequest) {
        Log.i(javaClass.simpleName, request.toString())
        withContext(bgDispatcher) {
            delay(SECONDS.toMillis(5))
            getMockInProgress()
            delay(SECONDS.toMillis(5))
            getMockDiscoverFeed()
        }
    }

    suspend fun requestPostsForTag(request: PostsForTagRequest) {
        Log.i(javaClass.simpleName, request.toString())
        withContext(bgDispatcher) {
            delay(SECONDS.toMillis(5))
            getMockError()
        }
    }
    suspend fun requestPostsForBlog(request: PostsForBlogRequest) {
        Log.i(javaClass.simpleName, request.toString())
        withContext(bgDispatcher) {
            delay(SECONDS.toMillis(5))
            getMockError()
        }
    }
    suspend fun requestPostsForFeed(request: PostsForFeedRequest) {
        Log.i(javaClass.simpleName, request.toString())
        withContext(bgDispatcher) {
            delay(SECONDS.toMillis(5))
            getMockError()
        }
    }

    fun start() {
        eventBusWrapper.register(this)
    }

    fun stop() {
        eventBusWrapper.unregister(this)
    }

    // Event Bus events emitted from ReaderPostLogic (
    @Subscribe(threadMode = MAIN)
    fun onEventMainThread(event: UpdatePostsStarted) {
        Log.i(javaClass.simpleName, "***=> Received UpdatePostsStarted for ${event.readerTag?.tagNameForLog}")
    }

    @Subscribe(threadMode = MAIN)
    fun onEventMainThread(event: UpdatePostsEnded) {
        Log.i(javaClass.simpleName, "***=> Received UpdatePostsEnded for ${event.readerTag?.tagNameForLog}")
        val stuff = event.result
    }

    @Subscribe(threadMode = MAIN)
    fun onEventMainThread(event: SearchPostsStarted) {
        Log.i(javaClass.simpleName, "***=> Received SearchPostsStarted")
    }

    @Subscribe(threadMode = MAIN)
    fun onEventMainThread(event: SearchPostsEnded) {
        Log.i(javaClass.simpleName, "***=> Received SearchPostsEnded")
    }


    companion object {
        const val discoverJson = "{\n" +
                "  \"found\": 3978,\n" +
                "  \"posts\": [\n" +
                "    {\n" +
                "      \"ID\": 42190,\n" +
                "      \"site_ID\": 53424024,\n" +
                "      \"author\": {\n" +
                "        \"ID\": 118930156,\n" +
                "        \"login\": \"carolynannewells\",\n" +
                "        \"email\": false,\n" +
                "        \"name\": \"Carolyn Wells\",\n" +
                "        \"first_name\": \"Carolyn\",\n" +
                "        \"last_name\": \"Wells\",\n" +
                "        \"nice_name\": \"carolynannewells\",\n" +
                "        \"URL\": \"http:\\/\\/onceuponatime66.wordpress.com\",\n" +
                "        \"avatar_URL\": \"https:\\/\\/0.gravatar.com\\/avatar\\/" +
                "c5e37740ae83e815e3c4e9ef94367509?s=96&d=identicon&r=G\",\n" +
                "        \"profile_URL\": \"https:\\/\\/en.gravatar.com\\/carolynannewells\",\n" +
                "        \"site_ID\": 126167003,\n" +
                "        \"has_avatar\": true\n" +
                "      },\n" +
                "      \"date\": \"2020-06-02T10:00:08-04:00\",\n" +
                "      \"modified\": \"2020-06-01T19:46:26-04:00\",\n" +
                "      \"title\": \"Tabitha Farrar on Eating Disorder Recovery\",\n" +
                "      \"URL\": \"https:\\/\\/discover.wordpress.com\\/2020\\/06\\/02\\/" +
                "tabitha-farrar-on-eating-disorder-recovery\\/\",\n" +
                "      \"short_URL\": \"https:\\/\\/wp.me\\/p3Ca1O-aYu\",\n" +
                "      \"content\": \"<p>This is content ... Tabitha Farrar, one of the founders " +
                "of World Eating Disorders Action Day, shares her experience of blogging about " +
                "recovery &#8212; and supporting others in their own journeys.<\\/p>\\n\",\n" +
                "      \"excerpt\": \"<p>Tabitha Farrar, one of the founders of World Eating " +
                "Disorders Action Day, shares her experience of blogging about recovery &#8212; " +
                "and supporting others in their own journeys.<\\/p>\\n\",\n" +
                "      \"slug\": \"tabitha-farrar-on-eating-disorder-recovery\",\n" +
                "      \"guid\": \"https:\\/\\/discover.wordpress.com\\/?p=42190\",\n" +
                "      \"status\": \"publish\",\n" +
                "      \"sticky\": false,\n" +
                "      \"password\": \"\",\n" +
                "      \"parent\": false,\n" +
                "      \"type\": \"post\",\n" +
                "      \"discussion\": {\n" +
                "        \"comments_open\": true,\n" +
                "        \"comment_status\": \"open\",\n" +
                "        \"pings_open\": false,\n" +
                "        \"ping_status\": \"closed\",\n" +
                "        \"comment_count\": 0\n" +
                "      },\n" +
                "      \"likes_enabled\": true,\n" +
                "      \"sharing_enabled\": true,\n" +
                "      \"like_count\": 30,\n" +
                "      \"i_like\": false,\n" +
                "      \"is_reblogged\": false,\n" +
                "      \"is_following\": false,\n" +
                "      \"global_ID\": \"e12043ca2dc1d8d291e8a2837c6253d6\",\n" +
                "      \"featured_image\": \"https:\\/\\/discover.files.wordpress.com\\/2020\\/06\\/" +
                "screen-shot-2020-06-01-at-9.29.38-am.png\",\n" +
                "      \"post_thumbnail\": {\n" +
                "        \"ID\": 42195,\n" +
                "        \"URL\": \"https:\\/\\/discover.files.wordpress.com\\/2020\\/06\\/" +
                "screen-shot-2020-06-01-at-9.29.38-am.png\",\n" +
                "        \"guid\": \"http:\\/\\/discover.files.wordpress.com\\/2020\\/06\\/" +
                "screen-shot-2020-06-01-at-9.29.38-am.png\",\n" +
                "        \"mime_type\": \"image\\/png\",\n" +
                "        \"width\": 1934,\n" +
                "        \"height\": 1726\n" +
                "      },\n" +
                "      \"format\": \"standard\",\n" +
                "      \"geo\": false,\n" +
                "      \"menu_order\": 0,\n" +
                "      \"page_template\": \"\",\n" +
                "      \"publicize_URLs\": [],\n" +
                "      \"terms\": {\n" +
                "        \"category\": {\n" +
                "          \"Education\": {\n" +
                "            \"ID\": 1342,\n" +
                "            \"name\": \"Education\",\n" +
                "            \"slug\": \"education\",\n" +
                "            \"description\": \"Resources across disciplines and perspectives on " +
                "teaching, learning, and the educational system from educators, teachers, and parents.\",\n" +
                "            \"post_count\": 162,\n" +
                "            \"parent\": 0,\n" +
                "            \"meta\": {\n" +
                "              \"links\": {\n" +
                "                \"self\": \"https:\\/\\/public-api.wordpress.com\\/rest\\/v1.1\\/" +
                "sites\\/53424024\\/categories\\/slug:education\",\n" +
                "                \"help\": \"https:\\/\\/public-api.wordpress.com\\/rest\\/v1.2\\/" +
                "sites\\/53424024\\/categories\\/slug:education\\/help\",\n" +
                "                \"site\": \"https:\\/\\/public-api.wordpress.com\\/rest\\/v1.2\\/" +
                "sites\\/53424024\"\n" +
                "              }\n" +
                "            }\n" +
                "          },\n" +
                "          \"Writing\": {\n" +
                "            \"ID\": 349,\n" +
                "            \"name\": \"Writing\",\n" +
                "            \"slug\": \"writing\",\n" +
                "            \"description\": \"Writing, advice, and commentary on the act and " +
                "process of writing, blogging, and publishing.\",\n" +
                "            \"post_count\": 527,\n" +
                "            \"parent\": 0,\n" +
                "            \"meta\": {\n" +
                "              \"links\": {\n" +
                "                \"self\": \"https:\\/\\/public-api.wordpress.com\\/rest\\/v1.1\\/" +
                "sites\\/53424024\\/categories\\/slug:writing\",\n" +
                "                \"help\": \"https:\\/\\/public-api.wordpress.com\\/rest\\/v1.2\\/" +
                "sites\\/53424024\\/categories\\/slug:writing\\/help\",\n" +
                "                \"site\": \"https:\\/\\/public-api.wordpress.com\\/rest\\/v1.2\\/" +
                "sites\\/53424024\"\n" +
                "              }\n" +
                "            }\n" +
                "          }\n" +
                "        },\n" +
                "        \"post_tag\": {\n" +
                "          \"#ShareYourStory\": {\n" +
                "            \"ID\": 83876228,\n" +
                "            \"name\": \"#ShareYourStory\",\n" +
                "            \"slug\": \"shareyourstory\",\n" +
                "            \"description\": \"\",\n" +
                "            \"post_count\": 1,\n" +
                "            \"meta\": {\n" +
                "              \"links\": {\n" +
                "                \"self\": \"https:\\/\\/public-api.wordpress.com\\/rest\\/v1.1\\/" +
                "sites\\/53424024\\/tags\\/slug:shareyourstory\",\n" +
                "                \"help\": \"https:\\/\\/public-api.wordpress.com\\/rest\\/v1.2\\/" +
                "sites\\/53424024\\/tags\\/slug:shareyourstory\\/help\",\n" +
                "                \"site\": \"https:\\/\\/public-api.wordpress.com\\/rest\\/v1.2\\/" +
                "sites\\/53424024\"\n" +
                "              }\n" +
                "            }\n" +
                "          },\n" +
                "          \"World Eating Disorders Action Day\": {\n" +
                "            \"ID\": 501041846,\n" +
                "            \"name\": \"World Eating Disorders Action Day\",\n" +
                "            \"slug\": \"world-eating-disorders-action-day\",\n" +
                "            \"description\": \"\",\n" +
                "            \"post_count\": 1,\n" +
                "            \"meta\": {\n" +
                "              \"links\": {\n" +
                "                \"self\": \"https:\\/\\/public-api.wordpress.com\\/rest\\/v1.1\\/" +
                "sites\\/53424024\\/tags\\/slug:world-eating-disorders-action-day\",\n" +
                "                \"help\": \"https:\\/\\/public-api.wordpress.com\\/rest\\/v1.2\\/" +
                "sites\\/53424024\\/tags\\/slug:world-eating-disorders-action-day\\/help\",\n" +
                "                \"site\": \"https:\\/\\/public-api.wordpress.com\\/rest\\/v1.2\\/" +
                "sites\\/53424024\"\n" +
                "              }\n" +
                "            }\n" +
                "          }\n" +
                "        },\n" +
                "        \"post_format\": {},\n" +
                "        \"mentions\": {}\n" +
                "      },\n" +
                "      \"tags\": {\n" +
                "        \"World Eating Disorders Action Day\": {\n" +
                "          \"ID\": 501041846,\n" +
                "          \"name\": \"World Eating Disorders Action Day\",\n" +
                "          \"slug\": \"world-eating-disorders-action-day\",\n" +
                "          \"description\": \"\",\n" +
                "          \"post_count\": 1,\n" +
                "          \"meta\": {\n" +
                "            \"links\": {\n" +
                "              \"self\": \"https:\\/\\/public-api.wordpress.com\\/rest\\/v1.1\\/" +
                "sites\\/53424024\\/tags\\/slug:world-eating-disorders-action-day\",\n" +
                "              \"help\": \"https:\\/\\/public-api.wordpress.com\\/rest\\/v1.2\\/" +
                "sites\\/53424024\\/tags\\/slug:world-eating-disorders-action-day\\/help\",\n" +
                "              \"site\": \"https:\\/\\/public-api.wordpress.com\\/rest\\/v1.2\\/" +
                "sites\\/53424024\"\n" +
                "            }\n" +
                "          },\n" +
                "          \"display_name\": \"world-eating-disorders-action-day\"\n" +
                "        }\n" +
                "      },\n" +
                "      \"categories\": {\n" +
                "        \"Writing\": {\n" +
                "          \"ID\": 349,\n" +
                "          \"name\": \"Writing\",\n" +
                "          \"slug\": \"writing\",\n" +
                "          \"description\": \"Writing, advice, and commentary on the act and " +
                "process of writing, blogging, and publishing.\",\n" +
                "          \"post_count\": 527,\n" +
                "          \"parent\": 0,\n" +
                "          \"meta\": {\n" +
                "            \"links\": {\n" +
                "              \"self\": \"https:\\/\\/public-api.wordpress.com\\/rest\\/v1.1\\/" +
                "sites\\/53424024\\/categories\\/slug:writing\",\n" +
                "              \"help\": \"https:\\/\\/public-api.wordpress.com\\/rest\\/v1.2\\/" +
                "sites\\/53424024\\/categories\\/slug:writing\\/help\",\n" +
                "              \"site\": \"https:\\/\\/public-api.wordpress.com\\/rest\\/v1.2\\/" +
                "sites\\/53424024\"\n" +
                "            }\n" +
                "          }\n" +
                "        }\n" +
                "      },\n" +
                "      \"attachments\": {\n" +
                "        \"42195\": {\n" +
                "          \"ID\": 42195,\n" +
                "          \"URL\": \"https:\\/\\/discover.files.wordpress.com\\/2020\\/06\\/" +
                "screen-shot-2020-06-01-at-9.29.38-am.png\",\n" +
                "          \"guid\": \"http:\\/\\/discover.files.wordpress.com\\/2020\\/06\\/" +
                "screen-shot-2020-06-01-at-9.29.38-am.png\",\n" +
                "          \"date\": \"2020-06-01T12:40:25-04:00\",\n" +
                "          \"post_ID\": 42190,\n" +
                "          \"author_ID\": 118930156,\n" +
                "          \"file\": \"screen-shot-2020-06-01-at-9.29.38-am.png\",\n" +
                "          \"mime_type\": \"image\\/png\",\n" +
                "          \"extension\": \"png\",\n" +
                "          \"title\": \"Screen Shot 2020-06-01 at 9.29.38 AM\",\n" +
                "          \"caption\": \"Neural Rewiring for Eating Disorder Recovery book cover," +
                " tabithafarrar.com\",\n" +
                "          \"description\": \"\",\n" +
                "          \"alt\": \"\",\n" +
                "          \"thumbnails\": {\n" +
                "            \"thumbnail\": \"https:\\/\\/discover.files.wordpress.com\\/2020\\/06\\/" +
                "screen-shot-2020-06-01-at-9.29.38-am.png?w=150\",\n" +
                "            \"medium\": \"https:\\/\\/discover.files.wordpress.com\\/2020\\/06\\/" +
                "screen-shot-2020-06-01-at-9.29.38-am.png?w=315\",\n" +
                "            \"large\": \"https:\\/\\/discover.files.wordpress.com\\/2020\\/06\\/" +
                "screen-shot-2020-06-01-at-9.29.38-am.png?w=1147\",\n" +
                "            \"newspack-article-block-landscape-large\": \"https:\\/\\/discover.files." +
                "wordpress.com\\/2020\\/06\\/screen-shot-2020-06-01-at-9.29.38-am.png?w=1200&h=900&crop=1\",\n" +
                "            \"newspack-article-block-portrait-large\": \"https:\\/\\/discover.files." +
                "wordpress.com\\/2020\\/06\\/screen-shot-2020-06-01-at-9.29.38-am.png?w=900&h=1200&crop=1\",\n" +
                "            \"newspack-article-block-square-large\": \"https:\\/\\/discover.files." +
                "wordpress.com\\/2020\\/06\\/screen-shot-2020-06-01-at-9.29.38-am.png?w=1200&h=1200&crop=1\",\n" +
                "            \"newspack-article-block-landscape-medium\": \"https:\\/\\/discover.files." +
                "wordpress.com\\/2020\\/06\\/screen-shot-2020-06-01-at-9.29.38-am.png?w=800&h=600&crop=1\",\n" +
                "            \"newspack-article-block-portrait-medium\": \"https:\\/\\/discover.files." +
                "wordpress.com\\/2020\\/06\\/screen-shot-2020-06-01-at-9.29.38-am.png?w=600&h=800&crop=1\",\n" +
                "            \"newspack-article-block-square-medium\": \"https:\\/\\/discover.files." +
                "wordpress.com\\/2020\\/06\\/screen-shot-2020-06-01-at-9.29.38-am.png?w=800&h=800&crop=1\",\n" +
                "            \"newspack-article-block-landscape-small\": \"https:\\/\\/discover.files." +
                "wordpress.com\\/2020\\/06\\/screen-shot-2020-06-01-at-9.29.38-am.png?w=400&h=300&crop=1\",\n" +
                "            \"newspack-article-block-portrait-small\": \"https:\\/\\/discover.files." +
                "wordpress.com\\/2020\\/06\\/screen-shot-2020-06-01-at-9.29.38-am.png?w=300&h=400&crop=1\",\n" +
                "            \"newspack-article-block-square-small\": \"https:\\/\\/discover.files." +
                "wordpress.com\\/2020\\/06\\/screen-shot-2020-06-01-at-9.29.38-am.png?w=400&h=400&crop=1\",\n" +
                "            \"newspack-article-block-landscape-tiny\": \"https:\\/\\/discover.files." +
                "wordpress.com\\/2020\\/06\\/screen-shot-2020-06-01-at-9.29.38-am.png?w=200&h=150&crop=1\",\n" +
                "            \"newspack-article-block-portrait-tiny\": \"https:\\/\\/discover.files." +
                "wordpress.com\\/2020\\/06\\/screen-shot-2020-06-01-at-9.29.38-am.png?w=150&h=200&crop=1\",\n" +
                "            \"newspack-article-block-square-tiny\": \"https:\\/\\/discover.files." +
                "wordpress.com\\/2020\\/06\\/screen-shot-2020-06-01-at-9.29.38-am.png?w=200&h=200&crop=1\",\n" +
                "            \"newspack-article-block-uncropped\": \"https:\\/\\/discover.files." +
                "wordpress.com\\/2020\\/06\\/screen-shot-2020-06-01-at-9.29.38-am.png?w=1200\"\n" +
                "          },\n" +
                "          \"height\": 1726,\n" +
                "          \"width\": 1934,\n" +
                "          \"exif\": {\n" +
                "            \"aperture\": \"0\",\n" +
                "            \"credit\": \"\",\n" +
                "            \"camera\": \"\",\n" +
                "            \"caption\": \"\",\n" +
                "            \"created_timestamp\": \"0\",\n" +
                "            \"copyright\": \"\",\n" +
                "            \"focal_length\": \"0\",\n" +
                "            \"iso\": \"0\",\n" +
                "            \"shutter_speed\": \"0\",\n" +
                "            \"title\": \"\",\n" +
                "            \"orientation\": \"0\",\n" +
                "            \"keywords\": []\n" +
                "          },\n" +
                "          \"meta\": {\n" +
                "            \"links\": {\n" +
                "              \"self\": \"https:\\/\\/public-api.wordpress.com\\/rest\\/v1.2\\/" +
                "sites\\/53424024\\/media\\/42195\",\n" +
                "              \"help\": \"https:\\/\\/public-api.wordpress.com\\/rest\\/v1.2\\/" +
                "sites\\/53424024\\/media\\/42195\\/help\",\n" +
                "              \"site\": \"https:\\/\\/public-api.wordpress.com\\/rest\\/v1.2\\/" +
                "sites\\/53424024\",\n" +
                "              \"parent\": \"https:\\/\\/public-api.wordpress.com\\/rest\\/v1.1\\/" +
                "sites\\/53424024\\/posts\\/42190\"\n" +
                "            }\n" +
                "          }\n" +
                "        }\n" +
                "      },\n" +
                "      \"attachment_count\": 1,\n" +
                "      \"metadata\": [\n" +
                "        {\n" +
                "          \"id\": \"143898\",\n" +
                "          \"key\": \"geo_public\",\n" +
                "          \"value\": \"0\"\n" +
                "        },\n" +
                "        {\n" +
                "          \"id\": \"143903\",\n" +
                "          \"key\": \"_thumbnail_id\",\n" +
                "          \"value\": \"42195\"\n" +
                "        },\n" +
                "        {\n" +
                "          \"id\": \"143991\",\n" +
                "          \"key\": \"_wpas_done_22794201\",\n" +
                "          \"value\": \"1\"\n" +
                "        },\n" +
                "        {\n" +
                "          \"id\": \"143931\",\n" +
                "          \"key\": \"_wpas_mess\",\n" +
                "          \"value\": \"On World Eating Disorders Action Day we are sharing the " +
                "story of Tabitha Farrar and her recovery blog. #ShareYourStory\"\n" +
                "        }\n" +
                "      ],\n" +
                "      \"meta\": {\n" +
                "        \"links\": {\n" +
                "          \"self\": \"https:\\/\\/public-api.wordpress.com\\/rest\\/v1.1\\/read\\/" +
                "sites\\/53424024\\/posts\\/42190\",\n" +
                "          \"help\": \"https:\\/\\/public-api.wordpress.com\\/rest\\/v1.2\\/" +
                "sites\\/53424024\\/posts\\/42190\\/help\",\n" +
                "          \"site\": \"https:\\/\\/public-api.wordpress.com\\/rest\\/v1.2\\/" +
                "sites\\/53424024\",\n" +
                "          \"replies\": \"https:\\/\\/public-api.wordpress.com\\/rest\\/v1.1\\/" +
                "sites\\/53424024\\/posts\\/42190\\/replies\\/\",\n" +
                "          \"likes\": \"https:\\/\\/public-api.wordpress.com\\/rest\\/v1.1\\/" +
                "sites\\/53424024\\/posts\\/42190\\/likes\\/\"\n" +
                "        },\n" +
                "        \"data\": {\n" +
                "          \"site\": {\n" +
                "            \"ID\": 53424024,\n" +
                "            \"name\": \"Discover\",\n" +
                "            \"description\": \"A daily selection of the best content published on" +
                " WordPress, collected for you by humans who love to read.\",\n" +
                "            \"URL\": \"https:\\/\\/discover.wordpress.com\",\n" +
                "            \"jetpack\": false,\n" +
                "            \"subscribers_count\": 47606255,\n" +
                "            \"locale\": false,\n" +
                "            \"icon\": {\n" +
                "              \"img\": \"https:\\/\\/secure.gravatar.com\\/blavatar\\/" +
                "c9e4e04719c81ca4936a63ea2dce6ace\",\n" +
                "              \"ico\": \"https:\\/\\/secure.gravatar.com\\/blavatar\\/" +
                "c9e4e04719c81ca4936a63ea2dce6ace\"\n" +
                "            },\n" +
                "            \"logo\": {\n" +
                "              \"id\": 0,\n" +
                "              \"sizes\": [],\n" +
                "              \"url\": \"\"\n" +
                "            },\n" +
                "            \"visible\": null,\n" +
                "            \"is_private\": false,\n" +
                "            \"is_coming_soon\": false,\n" +
                "            \"is_following\": false,\n" +
                "            \"meta\": {\n" +
                "              \"links\": {\n" +
                "                \"self\": \"https:\\/\\/public-api.wordpress.com\\/rest\\/v1.2\\/" +
                "sites\\/53424024\",\n" +
                "                \"help\": \"https:\\/\\/public-api.wordpress.com\\/rest\\/v1.2\\/" +
                "sites\\/53424024\\/help\",\n" +
                "                \"posts\": \"https:\\/\\/public-api.wordpress.com\\/rest\\/v1.2\\/" +
                "sites\\/53424024\\/posts\\/\",\n" +
                "                \"comments\": \"https:\\/\\/public-api.wordpress.com\\/rest\\/v1.1\\/" +
                "sites\\/53424024\\/comments\\/\",\n" +
                "                \"xmlrpc\": \"https:\\/\\/discover.wordpress.com\\/xmlrpc.php\"\n" +
                "              }\n" +
                "            },\n" +
                "            \"launch_status\": false,\n" +
                "            \"site_migration\": null,\n" +
                "            \"is_fse_active\": false,\n" +
                "            \"is_fse_eligible\": false,\n" +
                "            \"is_core_site_editor_enabled\": false\n" +
                "          },\n" +
                "          \"likes\": {\n" +
                "            \"found\": 1,\n" +
                "            \"i_like\": false,\n" +
                "            \"site_ID\": 53424024,\n" +
                "            \"post_ID\": 42190,\n" +
                "            \"likes\": [\n" +
                "              {\n" +
                "                \"ID\": 187315272,\n" +
                "                \"login\": \"erprashantdeep90\",\n" +
                "                \"email\": false,\n" +
                "                \"name\": \"The Incredible Mishra\",\n" +
                "                \"first_name\": \"The Incredible\",\n" +
                "                \"last_name\": \"Mishra\",\n" +
                "                \"nice_name\": \"erprashantdeep90\",\n" +
                "                \"URL\": \"http:\\/\\/theincrediblemishrawritupsart.wordpress.com\",\n" +
                "                \"avatar_URL\": \"https:\\/\\/2.gravatar.com\\/avatar\\/" +
                "2c587712f1bc9bd1eb425e9fa13c76e2?s=96&d=identicon&r=G\",\n" +
                "                \"profile_URL\": \"https:\\/\\/en.gravatar.com\\/erprashantdeep90\",\n" +
                "                \"ip_address\": false,\n" +
                "                \"site_ID\": 178308566,\n" +
                "                \"site_visible\": true,\n" +
                "                \"default_avatar\": true\n" +
                "              }\n" +
                "            ]\n" +
                "          }\n" +
                "        }\n" +
                "      },\n" +
                "      \"capabilities\": {\n" +
                "        \"publish_post\": false,\n" +
                "        \"delete_post\": false,\n" +
                "        \"edit_post\": false\n" +
                "      },\n" +
                "      \"other_URLs\": {},\n" +
                "      \"feed_ID\": 41325786,\n" +
                "      \"feed_URL\": \"http:\\/\\/discover.wordpress.com\",\n" +
                "      \"pseudo_ID\": \"e12043ca2dc1d8d291e8a2837c6253d6\",\n" +
                "      \"is_external\": false,\n" +
                "      \"site_name\": \"Discover\",\n" +
                "      \"site_URL\": \"https:\\/\\/discover.wordpress.com\",\n" +
                "      \"site_is_private\": false,\n" +
                "      \"featured_media\": {\n" +
                "        \"uri\": \"https:\\/\\/discover.files.wordpress.com\\/2020\\/06\\/" +
                "gettyimages-521952628.jpg\",\n" +
                "        \"width\": 6260,\n" +
                "        \"height\": 2832,\n" +
                "        \"type\": \"image\"\n" +
                "      },\n" +
                "      \"use_excerpt\": false,\n" +
                "      \"is_following_conversation\": false\n" +
                "    }\n" +
                "  ],\n" +
                "  \"meta\": {\n" +
                "    \"links\": {\n" +
                "      \"counts\": \"https:\\/\\/public-api.wordpress.com\\/rest\\/v1.2\\/" +
                "sites\\/53424024\\/post-counts\\/post\"\n" +
                "    },\n" +
                "    \"next_page\": \"value=2020-06-02T10%3A00%3A08-04%3A00&id=42190\",\n" +
                "    \"wpcom\": true\n" +
                "  }\n" +
                "}"
    }
    // Old Stub methods end
}


sealed class ReaderDataRequest(val action: UpdateAction) {
    class DiscoverRequest(val interests: List<String>, action: UpdateAction) : ReaderDataRequest(action)
    class PostsForTagRequest(val tag: ReaderTag,  action: UpdateAction) : ReaderDataRequest(action)
    class PostsForBlogRequest(val blogId: Long,  action: UpdateAction) : ReaderDataRequest(action)
    class PostsForFeedRequest(val feedId: Long,  action: UpdateAction) : ReaderDataRequest(action)

    override fun toString(): String {
        return "${this.javaClass.simpleName}($action)"
    }
}

sealed class ReaderDataWrapper<out T : Any> {
    data class Success<out T : Any>(val data: T) : ReaderDataWrapper<T>()
    sealed class Error(val exception: Exception) : ReaderDataWrapper<Nothing>() {
        class NetworkError(exception: Exception) : Error(exception)
        class NotFoundError(exception: Exception) : Error(exception)
        // todo: we could identify errors and create classes as needed
    }
    object InProgress : ReaderDataWrapper<Nothing>()

    override fun toString(): String {
        return "${this.javaClass.simpleName})"
    }
}

/** ReactiveMutableLiveData is a handy extension of MutableLiveData.
 * The main purpose is to monitor onActive and onInactive
 *  situations because they are unreachable using straight up LiveData
 */
class ReactiveMutableLiveData<T>(private val onReactiveListener : OnReactiveListener):
        MutableLiveData<T>() {
    // Allow a way to hook up the external listeners
    constructor(onActive: () -> Unit = {}, onInactive: () -> Unit = {}) : this(
            setReactiveListener(onActive, onInactive)
    )
    override fun onActive() {
        Log.i(javaClass.simpleName, "***=> OnActive")
       onReactiveListener.onActive()
    }

    override fun onInactive() {
        Log.i(javaClass.simpleName, "***=> OnActive")
        onReactiveListener.onInactive()
    }

    companion object {
        /**
         * Creates a OnReactiveListener that can be passed into the constructor
         */
        fun setReactiveListener(onActive: () -> Unit, onInactive: () -> Unit): OnReactiveListener {
            return object: OnReactiveListener {
                override fun onActive() {
                    onActive.invoke()
                }

                override fun onInactive() {
                    onInactive.invoke()
                }
            }
        }
    }
}

interface OnReactiveListener {
    fun onActive()
    fun onInactive()
}

// Bad Ideas
// This may be a bad idea, but we have two options for getting requests to the repo
// (1) Send a request through a single "invoke" method
// (2) Use the discoverFeedRequest as a stream and process each as it comes it
// Note: Track requests so we don't send duplicates, but each fragment/vm pair should
// only request what is needed for their tab.
// private val discoverFeedRequestObserver = Observer<DiscoverRequest> { request ->
//    request?.let {
//        Log.i(javaClass.simpleName, "***=> a request has been made for the discover feed")
//    }
// }
// Bad Ideas for evebt bus listening based upon onAcitve or onInactive observers
///**
// * Check if the eventBus has been registered for this subscriber and if not, start it
// * This fun is probably not be necc.
// */
//private fun setup() {
//    // Make sure that EventBus is registered if not already
//    if (EventBus.getDefault().isRegistered(this)) {
//        return
//    }
//    start()
//}
//
///**
// * Iterate through the data and if no one is observering, shut down the bus - I wonder
// * if that is an operation that we want to do over and over again. Good question to pose
// * this fun is probably not necc.
// */
//private fun tearDown() {
//    if (mutableDiscoverFeed.hasActiveObservers() || mutableDiscoverFeed.hasObservers()) {
//        return
//    }
//    stop()
//}
// Old Stub methods start
//    private val mutableDiscoverFeed = ReactiveMutableLiveData<ReaderPostList>(
//            onActive = { onActive() }, onInactive = { onInactive() })
//    val discoverFeed: ReactiveMutableLiveData<ReaderPostList> = mutableDiscoverFeed
//
//    suspend fun getDiscoveryFeed(): LiveData<ReaderPostList> =
//            withContext(bgDispatcher) {
//                delay(SECONDS.toMillis(5))
//                getMockDiscoverFeed()
//            }
//
//    private suspend fun getMockDiscoverFeed(): LiveData<ReaderPostList> {
//        return withContext(ioDispatcher) {
//            mutableDiscoverFeed.postValue( ReaderPostList.fromJson(JSONObject(discoverJson))))
//        }
//    }


