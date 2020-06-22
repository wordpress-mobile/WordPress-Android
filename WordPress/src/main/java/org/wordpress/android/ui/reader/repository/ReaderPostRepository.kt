package org.wordpress.android.ui.reader.repository

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode.MAIN
import org.json.JSONObject
import org.wordpress.android.datasets.ReaderPostTable
import org.wordpress.android.datasets.ReaderTagTable
import org.wordpress.android.models.ReaderPostList
import org.wordpress.android.models.ReaderTag
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.modules.IO_THREAD
import org.wordpress.android.ui.reader.ReaderConstants
import org.wordpress.android.ui.reader.ReaderEvents.SearchPostsEnded
import org.wordpress.android.ui.reader.ReaderEvents.SearchPostsStarted
import org.wordpress.android.ui.reader.ReaderEvents.UpdatePostsEnded
import org.wordpress.android.ui.reader.ReaderEvents.UpdatePostsStarted
import org.wordpress.android.ui.reader.actions.ReaderActions.UpdateResult.CHANGED
import org.wordpress.android.ui.reader.actions.ReaderActions.UpdateResult.FAILED
import org.wordpress.android.ui.reader.actions.ReaderActions.UpdateResult.HAS_NEW
import org.wordpress.android.ui.reader.actions.ReaderActions.UpdateResult.UNCHANGED
import org.wordpress.android.ui.reader.repository.ReaderDataRequest.PostsForTagRequest
import org.wordpress.android.ui.reader.services.post.ReaderPostServiceStarter
import org.wordpress.android.ui.reader.services.post.ReaderPostServiceStarter.UpdateAction
import org.wordpress.android.ui.reader.services.post.ReaderPostServiceStarter.UpdateAction.REQUEST_NEWER
import org.wordpress.android.util.EventBusWrapper
import org.wordpress.android.util.NetworkUtils
import org.wordpress.android.viewmodel.ContextProvider
import java.util.concurrent.TimeUnit.SECONDS
import javax.inject.Inject
import javax.inject.Named
import kotlin.DeprecationLevel.WARNING
import kotlin.coroutines.CoroutineContext

// todo: annmarie
// todo: move all those classes/interfaces (at the bottom) elsewhere
// todo: remove debug log lines
class ReaderPostRepository @Inject constructor(
    private val contextProvider: ContextProvider,
    private val eventBusWrapper: EventBusWrapper,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
    @Named(IO_THREAD) private val ioDispatcher: CoroutineDispatcher
) : CoroutineScope {
    // Holds posts for ReaderTag
    private val internalPostsForTag: HashMap<ReaderTag, ReaderData> = hashMapOf()

    // externally exposed livedata
    private val _postsForTag = ReactiveMutableLiveData<ReaderData>(
            onActive = { onActivePostsForTag() }, onInactive = { onInactivePostsForTag() })
    val postsForTag: ReactiveMutableLiveData<ReaderData> = _postsForTag

    private var readerTag: ReaderTag? = null
    private var isStarted = false

    fun start(startForTag: ReaderTag) {
        if (isStarted) {
            return
        }

        isStarted = true
        eventBusWrapper.register(this)
        readerTag = startForTag
    }

    fun stop() {
        eventBusWrapper.unregister(this)
        parentJob.cancelChildren()
    }

    // Notifications on observers
    private fun onActivePostsForTag() {
        loadPostsForTag()
    }

    private fun onInactivePostsForTag() {
        // TODO: Not sure if we are going to use this callback method for anything
    }

    // Local storage calls a.k.a DB calls
    private fun getPostsForTagFromLocalStorage(readerTag: ReaderTag): ReaderPostList {
        return ReaderPostTable.getPostsWithTag(
                readerTag,
                MAX_ROWS,
                EXCLUDE_TEXT_COLUMN
        )
    }

    private fun getNumPostsForTagFromLocalStorage(readerTag: ReaderTag): Int {
        return ReaderPostTable.getNumPostsWithTag(readerTag)
    }

    // The initial request is always REQUEST_NEWER, so make that the default
    private fun requestPostsForTagFromRemoteStorage(
        readerTag: ReaderTag,
        updateAction: UpdateAction = REQUEST_NEWER
    ) {
        // todo: do we need to tell the caller that network is not available?
        if (!NetworkUtils.isNetworkAvailable(contextProvider.getContext())) return

        ReaderPostServiceStarter.startServiceForTag(
                contextProvider.getContext(),
                readerTag,
                updateAction
        )
    }

    private fun shouldAutoUpdateTag(readerTag: ReaderTag?): Boolean {
        return if (readerTag == null) {
            true
        } else {
            ReaderTagTable.shouldAutoUpdateTag(readerTag)
        }
    }

    // Load posts
    private fun loadPostsForTag() {
        val tag = readerTag ?: return

        launch {
            val existsInternalPostsForTag = internalPostsForTag.contains(tag)
            val refresh = shouldAutoUpdateTag(tag)

            if (existsInternalPostsForTag) {
                _postsForTag.postValue(internalPostsForTag[tag]?.copy(readerTag = tag))
            } else {
                val result = runCatching { getPostsForTag(tag) }
                result.onSuccess {
                    internalPostsForTag[tag] = it
                    _postsForTag.postValue(internalPostsForTag[tag]?.copy(readerTag = tag))
                }
                        .onFailure {
                            _postsForTag.postValue(
                                    internalPostsForTag[tag]?.copy(
                                            readerTag = tag,
                                            status = ReaderDataStatus.Error.RetrievalError(
                                                    Exception(it)
                                            )
                                    )
                            )
                        }
            }
            if (refresh) {
                requestPostsForTagFromRemoteStorage(tag)
            }
        }
    }

    // Reload posts
    private fun reloadPostsForTag() {
        val tag = readerTag ?: return

        launch {
            val result = runCatching { getPostsForTag(tag) }
            result.onSuccess {
                internalPostsForTag[tag] = it
                _postsForTag.postValue(internalPostsForTag[tag]?.copy(readerTag = tag))
            }
                    .onFailure {
                        _postsForTag.postValue(
                                internalPostsForTag[tag]?.copy(
                                        readerTag = tag,
                                        status = ReaderDataStatus.Error.RetrievalError(
                                                Exception(it)
                                        )
                                )
                        )
                    }
        }
    }

    // Need to get the count & the data at the same time together
    // once you call await, you suspend the parent (the "withContext" until the value arrives)
    // works like a future promise but with coroutines
    private suspend fun getPostsForTag(readerTag: ReaderTag): ReaderData =
            withContext(ioDispatcher) {
                logCoroutine("getPostsForTag", coroutineContext)

                val postsForTagFromLocalDeferred = async {
                    logCoroutine("postsForTagFromLocalDeferred", coroutineContext)
                    getPostsForTagFromLocalStorage(readerTag)
                }

                val totalPostsForTagFromLocalDeferred = async {
                    logCoroutine("totalPostsForTagFromLocalDeferred", coroutineContext)
                    getNumPostsForTagFromLocalStorage(readerTag)
                }

                val readerPostList = postsForTagFromLocalDeferred.await()
                val totalEntriesForTag = totalPostsForTagFromLocalDeferred.await()

                ReaderData(
                        readerTag = null, data = readerPostList, totalEntries = totalEntriesForTag,
                        state = ReaderDataState.Completed, status = ReaderDataStatus.Success
                )
            }

    // public functions
    fun requestPostsForTag(request: PostsForTagRequest) {
        requestPostsForTagFromRemoteStorage(request.readerTag, request.action)
        readerTag = request.readerTag
    }

    // coroutine related - there may be some injectables that I am not aware of - ask jirka
    private val parentJob = SupervisorJob() // a job that can cancel all its children at once

    private val coroutineExceptionHandler =
            CoroutineExceptionHandler { context, throwable ->
                throwable.printStackTrace()
            }

    override val coroutineContext: CoroutineContext
        get() = ioDispatcher + parentJob + coroutineExceptionHandler

    // Event Bus events emitted from ReaderPostLogic (
    @Subscribe(threadMode = MAIN)
    fun onEventMainThread(event: UpdatePostsStarted) {
        val eventTag = event.readerTag ?: return
        if (isCurrentTag(eventTag)) {
            // ignore events not related to this instance of ReaderPostRepository
            return
        }
        Log.i(
             javaClass.simpleName,
                "***=> Received UpdatePostsStarted for ${event.readerTag?.tagNameForLog}"
        )
    }

    @Subscribe(threadMode = MAIN)
    fun onEventMainThread(event: UpdatePostsEnded) {
        if (event.readerTag != null && !isCurrentTag(event.readerTag)) {
            // ignore events not related to this instance of ReaderPostRepository
            return
        }

        Log.i(javaClass.simpleName, "***=> Received UpdatePostsEnded for Action ${event.action}")
        Log.i(javaClass.simpleName,
                "***=> Received UpdatePostsEnded for Tag ${event.readerTag?.tagNameForLog}"
        )

        event.result?.let {
            when (it) {
                HAS_NEW -> {
                    Log.i(javaClass.simpleName, "***=> is new")
                    reloadPostsForTag()
                }
                CHANGED -> {
                    Log.i(javaClass.simpleName, "***=> is changed")
                    reloadPostsForTag()
                }
                UNCHANGED -> {
                    Log.i(javaClass.simpleName, "***=> is unchanged")
                }
                FAILED -> {
                    Log.i(javaClass.simpleName, "***=> is failed")
                    // todo: what are we doing about failures?
                }
            }
        }
    }

    private fun isCurrentTag(tag: ReaderTag): Boolean {
        return ReaderTag.isSameTag(tag, readerTag)
    }

    @Subscribe(threadMode = MAIN)
    fun onEventMainThread(event: SearchPostsStarted) {
        Log.i(javaClass.simpleName, "***=> TODO")
    }

    @Subscribe(threadMode = MAIN)
    fun onEventMainThread(event: SearchPostsEnded) {
        Log.i(javaClass.simpleName, "***=> TODO")
    }

    // Keep discover fragment working while testing
    suspend fun getDiscoveryFeed(): LiveData<ReaderPostList> =
            withContext(bgDispatcher) {
                delay(SECONDS.toMillis(5))
                getMockDiscoverFeed()
            }

    private suspend fun getMockDiscoverFeed(): LiveData<ReaderPostList> {
        return withContext(ioDispatcher) {
            mutableDiscoverFeed.postValue(ReaderPostList.fromJson(JSONObject(discoverJson)))
            mutableDiscoverFeed
        }
    }

    private val mutableDiscoverFeed = MutableLiveData<ReaderPostList>()
    val discoveryFeed: MutableLiveData<ReaderPostList> = mutableDiscoverFeed

    companion object {
        private const val EXCLUDE_TEXT_COLUMN = true
        private const val MAX_ROWS = ReaderConstants.READER_MAX_POSTS_TO_DISPLAY

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

/***
 * Used for data requests requested from the Reader repositories
 */
sealed class ReaderDataRequest(val action: UpdateAction) {
    class DiscoverRequest(val interests: List<String>, action: UpdateAction) : ReaderDataRequest(
            action
    )
    class PostsForTagRequest(val readerTag: ReaderTag, action: UpdateAction) : ReaderDataRequest(
            action
    )
    class PostsForBlogRequest(val blogId: Long, action: UpdateAction) : ReaderDataRequest(action)
    class PostsForFeedRequest(val feedId: Long, action: UpdateAction) : ReaderDataRequest(action)

    override fun toString(): String {
        return "${this.javaClass.simpleName}($action)"
    }
}

private fun newReaderData() =
        ReaderData(null, ReaderPostList(), null, ReaderDataState.Started, ReaderDataStatus.Success)

// Contains the readerPostList, the total number of entries available at time of request which is
// up for debate as a separate call - the reader adapter uses this value to determine UI features,
// so quite possibly it should stay together
// This data wrapper is by NO means FINAL
// We need to decide how to return state/status & what that looks like
//  Should it be part of the wrapper
//  Should there be another observer
data class ReaderData(
    val readerTag: ReaderTag?, val data: ReaderPostList, val totalEntries: Int?,
    val state: ReaderDataState, val status: ReaderDataStatus
)

sealed class ReaderDataState {
    object Started : ReaderDataState()
    object Completed : ReaderDataState()

    override fun toString(): String {
        return "${this.javaClass.simpleName})"
    }
}

// need a way to get errors back to the caller, so they can make UI decisions for
// communication back to the user. Super important.
// Should the requester listen to a separate endpoint for requestErrors?
sealed class ReaderDataStatus {
    object Success : ReaderDataStatus()
    sealed class Error(val exception: Exception) : ReaderDataStatus() {
        class NetworkError(exception: Exception) : Error(exception)
        class NotFoundError(exception: Exception) : Error(exception)
        class MissingParamsError(exception: Exception) : Error(exception)
        class RetrievalError(exception: Exception) : Error(exception)
    }

    override fun toString(): String {
        return "${this.javaClass.simpleName})"
    }
}

/** ReactiveMutableLiveData is a handy extension of MutableLiveData.
 * The main purpose is to monitor onActive and onInactive
 *  situations because they are unreachable using straight up LiveData
 */
class ReactiveMutableLiveData<T>(private val onReactiveListener: OnReactiveListener) :
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
            return object : OnReactiveListener {
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

/** logs coroutine info for debugging */

@Deprecated(level = WARNING, message = "For testing only and will be removed")
fun logCoroutine(methodName: String, coroutineContext: CoroutineContext) {
    Log.d(
            "testCoroutine", "Thread for $methodName ${Thread.currentThread().name}" +
            " and the context is $coroutineContext"
    )
}
