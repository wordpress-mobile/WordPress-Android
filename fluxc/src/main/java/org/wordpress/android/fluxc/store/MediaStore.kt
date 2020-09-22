package org.wordpress.android.fluxc.store

import android.text.TextUtils
import com.wellsql.generated.MediaModelTable
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode.ASYNC
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.Payload
import org.wordpress.android.fluxc.action.MediaAction
import org.wordpress.android.fluxc.action.MediaAction.CANCELED_MEDIA_UPLOAD
import org.wordpress.android.fluxc.action.MediaAction.CANCEL_MEDIA_UPLOAD
import org.wordpress.android.fluxc.action.MediaAction.DELETED_MEDIA
import org.wordpress.android.fluxc.action.MediaAction.DELETE_MEDIA
import org.wordpress.android.fluxc.action.MediaAction.FETCHED_MEDIA
import org.wordpress.android.fluxc.action.MediaAction.FETCHED_MEDIA_LIST
import org.wordpress.android.fluxc.action.MediaAction.FETCH_MEDIA
import org.wordpress.android.fluxc.action.MediaAction.FETCH_MEDIA_LIST
import org.wordpress.android.fluxc.action.MediaAction.PUSHED_MEDIA
import org.wordpress.android.fluxc.action.MediaAction.PUSH_MEDIA
import org.wordpress.android.fluxc.action.MediaAction.REMOVE_ALL_MEDIA
import org.wordpress.android.fluxc.action.MediaAction.REMOVE_MEDIA
import org.wordpress.android.fluxc.action.MediaAction.UPDATE_MEDIA
import org.wordpress.android.fluxc.action.MediaAction.UPLOADED_MEDIA
import org.wordpress.android.fluxc.action.MediaAction.UPLOADED_STOCK_MEDIA
import org.wordpress.android.fluxc.action.MediaAction.UPLOAD_MEDIA
import org.wordpress.android.fluxc.action.MediaAction.UPLOAD_STOCK_MEDIA
import org.wordpress.android.fluxc.annotations.action.Action
import org.wordpress.android.fluxc.model.MediaModel
import org.wordpress.android.fluxc.model.MediaModel.MediaUploadState
import org.wordpress.android.fluxc.model.MediaModel.MediaUploadState.DELETING
import org.wordpress.android.fluxc.model.MediaModel.MediaUploadState.FAILED
import org.wordpress.android.fluxc.model.MediaModel.MediaUploadState.QUEUED
import org.wordpress.android.fluxc.model.MediaModel.MediaUploadState.UPLOADED
import org.wordpress.android.fluxc.model.MediaModel.MediaUploadState.UPLOADING
import org.wordpress.android.fluxc.model.PostImmutableModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.StockMediaModel
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest.WPComGsonNetworkError
import org.wordpress.android.fluxc.network.rest.wpcom.media.MediaRestClient
import org.wordpress.android.fluxc.network.xmlrpc.media.MediaXMLRPCClient
import org.wordpress.android.fluxc.persistence.MediaSqlUtils
import org.wordpress.android.fluxc.store.MediaStore.MediaErrorType.BAD_REQUEST
import org.wordpress.android.fluxc.store.MediaStore.MediaErrorType.CONNECTION_ERROR
import org.wordpress.android.fluxc.store.MediaStore.MediaErrorType.DB_QUERY_FAILURE
import org.wordpress.android.fluxc.store.MediaStore.MediaErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.store.MediaStore.MediaErrorType.MALFORMED_MEDIA_ARG
import org.wordpress.android.fluxc.store.MediaStore.MediaErrorType.NULL_MEDIA_ARG
import org.wordpress.android.fluxc.store.MediaStore.MediaErrorType.TIMEOUT
import org.wordpress.android.fluxc.utils.MediaUtils
import org.wordpress.android.fluxc.utils.MimeType.Type
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T.MEDIA
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.ArrayList
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaStore @Inject constructor(
    dispatcher: Dispatcher?,
    private val mMediaRestClient: MediaRestClient,
    private val mMediaXmlrpcClient: MediaXMLRPCClient
) : Store(dispatcher) {
    //
    // Payloads
    //
    /**
     * Actions: FETCH(ED)_MEDIA, PUSH(ED)_MEDIA, UPLOADED_MEDIA, DELETE(D)_MEDIA, UPDATE_MEDIA, and REMOVE_MEDIA
     */
    open class MediaPayload @JvmOverloads constructor(
        @JvmField var site: SiteModel?,
        @JvmField var media: MediaModel?,
        error: MediaError? = null
    ) : Payload<MediaError?>() {
        init {
            this.error = error
        }
    }

    /**
     * Action: UPLOAD_MEDIA
     */
    class UploadMediaPayload : MediaPayload {
        val stripLocation: Boolean

        constructor(site: SiteModel?, media: MediaModel?, stripLocation: Boolean) : super(site, media, null) {
            this.stripLocation = stripLocation
        }

        constructor(site: SiteModel?, media: MediaModel?, error: MediaError?, stripLocation: Boolean) : super(
                site,
                media,
                error
        ) {
            this.stripLocation = stripLocation
        }
    }

    /**
     * Actions: FETCH_MEDIA_LIST
     */
    data class FetchMediaListPayload(
        @JvmField val site: SiteModel,
        @JvmField val number: Int = DEFAULT_NUM_MEDIA_PER_FETCH,
        @JvmField val loadMore: Boolean = false,
        @JvmField val mimeType: Type? = null
    ) : Payload<BaseNetworkError?>() {
        constructor(site: SiteModel) : this(site, DEFAULT_NUM_MEDIA_PER_FETCH, false, null)
        constructor(site: SiteModel, number: Int, loadMore: Boolean) : this(site, number, loadMore, null)
    }

    /**
     * Actions: FETCHED_MEDIA_LIST
     */
    data class FetchMediaListResponsePayload(
        @JvmField val site: SiteModel,
        @JvmField val mediaList: List<MediaModel>,
        @JvmField val loadedMore: Boolean,
        @JvmField val canLoadMore: Boolean,
        @JvmField val mimeType: Type?
    ) : Payload<MediaError?>() {
        constructor(site: SiteModel, error: MediaError?, mimeType: Type?) : this(
                site,
                listOf(),
                false,
                false,
                mimeType
        ) {
            this.error = error
        }
    }

    /**
     * Actions: UPLOADED_MEDIA, CANCELED_MEDIA_UPLOAD
     */
    data class ProgressPayload(
        @JvmField val media: MediaModel?,
        @JvmField val progress: Float,
        @JvmField val completed: Boolean,
        @JvmField val canceled: Boolean
    ) : Payload<MediaError?>() {
        constructor(media: MediaModel?, progress: Float, completed: Boolean, error: MediaError?) : this(
                media,
                progress,
                completed,
                false
        ) {
            this.error = error
        }
    }

    /**
     * Actions: CANCEL_MEDIA_UPLOAD
     */
    data class CancelMediaPayload @JvmOverloads constructor(
        @JvmField val site: SiteModel,
        @JvmField val media: MediaModel?,
        @JvmField val delete: Boolean = true
    ) : Payload<BaseNetworkError?>()

    /**
     * Actions: UPLOAD_STOCK_MEDIA
     */
    data class UploadStockMediaPayload(
        @JvmField val site: SiteModel,
        @JvmField val stockMediaList: List<StockMediaModel>
    ) : Payload<BaseNetworkError?>()

    /**
     * Actions: UPLOADED_STOCK_MEDIA
     */
    data class UploadedStockMediaPayload(
        @JvmField val site: SiteModel,
        @JvmField val mediaList: List<MediaModel>
    ) : Payload<UploadStockMediaError?>() {
        constructor(site: SiteModel, error: UploadStockMediaError) : this(site, listOf()) {
            this.error = error
        }
    }

    //
    // OnChanged events
    //
    data class MediaError(@JvmField val type: MediaErrorType, @JvmField val message: String?) : OnChangedError {
        constructor(type: MediaErrorType) : this(type, null)

        // NOTE: It seems the backend is sending a final " Back" string in the message
        // Note that the real string depends on current locale; this is not optimal and we thought to
        // try to filter it out in the client app but at the end it can be not reliable so we are
        // keeping it. We can try to get it filtered on the backend side.
        val apiUserMessageIfAvailable: String?
            get() {
                if (TextUtils.isEmpty(message)) {
                    return ""
                }
                return if (type == BAD_REQUEST) {
                    val splitMsg = message?.split("\\|".toRegex(), 2)?.toTypedArray()
                    if (null != splitMsg && splitMsg.size > 1) {
                        val userMessage = splitMsg[1]
                        if (TextUtils.isEmpty(userMessage)) {
                            message
                        } else userMessage

                        // NOTE: It seems the backend is sending a final " Back" string in the message
                        // Note that the real string depends on current locale; this is not optimal and we thought to
                        // try to filter it out in the client app but at the end it can be not reliable so we are
                        // keeping it. We can try to get it filtered on the backend side.
                    } else {
                        message
                    }
                } else {
                    message
                }
            }

        companion object {
            @JvmStatic fun fromIOException(e: IOException): MediaError {
                var message = e.localizedMessage
                val type = when (e) {
                    is SocketTimeoutException -> {
                        TIMEOUT
                    }
                    is ConnectException, is UnknownHostException -> {
                        CONNECTION_ERROR
                    }
                    else -> {
                        GENERIC_ERROR
                    }
                }
                var errorMessage = e.message
                if (errorMessage.isNullOrEmpty()) {
                    return MediaError(type, message)
                }
                errorMessage = errorMessage.toLowerCase(Locale.US)
                if (errorMessage.contains("broken pipe") || errorMessage.contains("epipe")) {
                    // do not use the real error message.
                    message = ""
                }
                return MediaError(type, message)
            }
        }
    }

    data class UploadStockMediaError(@JvmField val type: UploadStockMediaErrorType, @JvmField val message: String) :
            OnChangedError

    data class OnMediaChanged @JvmOverloads constructor(
        @JvmField val cause: MediaAction?,
        @JvmField val mediaList: List<MediaModel> = listOf()
    ) : OnChanged<MediaError?>() {
        constructor(cause: MediaAction?, error: MediaError?) : this(cause, listOf(), error)
        constructor(
            cause: MediaAction?,
            mediaList: List<MediaModel> = listOf(),
            error: MediaError? = null
        ) : this(cause, mediaList) {
            this.error = error
        }
    }

    data class OnMediaListFetched(
        @JvmField val site: SiteModel,
        @JvmField val canLoadMore: Boolean,
        @JvmField val mimeType: Type?
    ) : OnChanged<MediaError?>() {
        constructor(site: SiteModel, error: MediaError?, mimeType: Type?) : this(site, false, mimeType) {
            this.error = error
        }
    }

    data class OnMediaUploaded(
        @JvmField val media: MediaModel?,
        @JvmField val progress: Float,
        @JvmField val completed: Boolean,
        @JvmField val canceled: Boolean
    ) :
            OnChanged<MediaError?>()

    data class OnStockMediaUploaded(@JvmField val site: SiteModel, @JvmField val mediaList: List<MediaModel>) :
            OnChanged<UploadStockMediaError?>() {
        constructor(site: SiteModel, error: UploadStockMediaError) : this(site, listOf()) {
            this.error = error
        }
    }

    //
    // Errors
    //
    enum class MediaErrorType {
        // local errors, occur before sending network requests
        FS_READ_PERMISSION_DENIED,
        NULL_MEDIA_ARG,
        MALFORMED_MEDIA_ARG,
        DB_QUERY_FAILURE,
        EXCEEDS_FILESIZE_LIMIT,
        EXCEEDS_MEMORY_LIMIT,
        EXCEEDS_SITE_SPACE_QUOTA_LIMIT, // network errors, occur in response to network requests
        AUTHORIZATION_REQUIRED,
        CONNECTION_ERROR,
        NOT_AUTHENTICATED,
        NOT_FOUND,
        PARSE_ERROR,
        REQUEST_TOO_LARGE,
        SERVER_ERROR, // this is also returned when PHP max_execution_time or memory_limit is reached
        TIMEOUT,
        BAD_REQUEST,
        XMLRPC_OPERATION_NOT_ALLOWED,
        XMLRPC_UPLOAD_ERROR, // logic constraints errors
        INVALID_ID, // unknown/unspecified
        GENERIC_ERROR;

        companion object {
            @JvmStatic fun fromBaseNetworkError(baseError: BaseNetworkError): MediaErrorType {
                return when (baseError.type) {
                    GenericErrorType.NOT_FOUND -> NOT_FOUND
                    GenericErrorType.NOT_AUTHENTICATED -> NOT_AUTHENTICATED
                    GenericErrorType.AUTHORIZATION_REQUIRED -> AUTHORIZATION_REQUIRED
                    GenericErrorType.PARSE_ERROR -> PARSE_ERROR
                    GenericErrorType.SERVER_ERROR -> SERVER_ERROR
                    GenericErrorType.TIMEOUT -> TIMEOUT
                    else -> GENERIC_ERROR
                }
            }

            @JvmStatic fun fromHttpStatusCode(code: Int): MediaErrorType {
                return when (code) {
                    400 -> BAD_REQUEST
                    404 -> NOT_FOUND
                    403 -> NOT_AUTHENTICATED
                    413 -> REQUEST_TOO_LARGE
                    500 -> SERVER_ERROR
                    else -> GENERIC_ERROR
                }
            }

            @JvmStatic fun fromString(string: String?): MediaErrorType {
                if (string != null) {
                    for (v in values()) {
                        if (string.equals(v.name, ignoreCase = true)) {
                            return v
                        }
                    }
                }
                return GENERIC_ERROR
            }
        }
    }

    enum class UploadStockMediaErrorType {
        INVALID_INPUT, UNKNOWN, GENERIC_ERROR;

        companion object {
            @JvmStatic fun fromNetworkError(wpError: WPComGsonNetworkError): UploadStockMediaErrorType {
                // invalid upload request
                if (wpError.apiError.equals("invalid_input", ignoreCase = true)) {
                    return INVALID_INPUT
                }
                // can happen if invalid pexels image url is passed
                return if (wpError.type == GenericErrorType.UNKNOWN) {
                    UNKNOWN
                } else GENERIC_ERROR
            }
        }
    }

    // Ensures that the UploadStore is initialized whenever the MediaStore is,
    // to ensure actions are shadowed and repeated by the UploadStore
    @Inject lateinit var uploadStore: UploadStore
    @Subscribe(threadMode = ASYNC) override fun onAction(action: Action<*>) {
        val actionType = action.type as? MediaAction ?: return
        when (actionType) {
            PUSH_MEDIA -> performPushMedia(action.payload as MediaPayload)
            UPLOAD_MEDIA -> performUploadMedia(action.payload as UploadMediaPayload)
            FETCH_MEDIA_LIST -> performFetchMediaList(action.payload as FetchMediaListPayload)
            FETCH_MEDIA -> performFetchMedia(action.payload as MediaPayload)
            DELETE_MEDIA -> performDeleteMedia(action.payload as MediaPayload)
            CANCEL_MEDIA_UPLOAD -> performCancelUpload(action.payload as CancelMediaPayload)
            PUSHED_MEDIA -> handleMediaPushed(action.payload as MediaPayload)
            UPLOADED_MEDIA -> handleMediaUploaded(action.payload as ProgressPayload)
            FETCHED_MEDIA_LIST -> handleMediaListFetched(action.payload as FetchMediaListResponsePayload)
            FETCHED_MEDIA -> handleMediaFetched(action.payload as MediaPayload)
            DELETED_MEDIA -> handleMediaDeleted(action.payload as MediaPayload)
            CANCELED_MEDIA_UPLOAD -> handleMediaCanceled(action.payload as ProgressPayload)
            UPDATE_MEDIA -> updateMedia(action.payload as MediaModel, true)
            REMOVE_MEDIA -> removeMedia(action.payload as MediaModel)
            REMOVE_ALL_MEDIA -> removeAllMedia()
            UPLOAD_STOCK_MEDIA -> performUploadStockMedia(action.payload as UploadStockMediaPayload)
            UPLOADED_STOCK_MEDIA -> handleStockMediaUploaded(action.payload as UploadedStockMediaPayload)
        }
    }

    override fun onRegister() {
        AppLog.d(MEDIA, "MediaStore onRegister")
    }

    //
    // Getters
    //
    fun instantiateMediaModel(): MediaModel? {
        var media: MediaModel? = MediaModel()
        media = MediaSqlUtils.insertMediaForResult(media)
        if (media.id == -1) {
            media = null
        }
        return media
    }

    fun getAllSiteMedia(siteModel: SiteModel?): List<MediaModel> {
        return MediaSqlUtils.getAllSiteMedia(siteModel)
    }

    companion object {
        const val DEFAULT_NUM_MEDIA_PER_FETCH = 50
        @JvmField val NOT_DELETED_STATES = listOf(
                DELETING.toString(),
                FAILED.toString(),
                QUEUED.toString(),
                UPLOADED.toString(),
                UPLOADING.toString()
        )
    }

    fun getSiteMediaCount(siteModel: SiteModel?): Int {
        return getAllSiteMedia(siteModel).size
    }

    fun hasSiteMediaWithId(siteModel: SiteModel?, mediaId: Long): Boolean {
        return getSiteMediaWithId(siteModel, mediaId) != null
    }

    fun getSiteMediaWithId(siteModel: SiteModel?, mediaId: Long): MediaModel? {
        val media = MediaSqlUtils.getSiteMediaWithId(siteModel, mediaId)
        return if (media.size > 0) media[0] else null
    }

    fun getMediaWithLocalId(localMediaId: Int): MediaModel {
        return MediaSqlUtils.getMediaWithLocalId(localMediaId)
    }

    fun getSiteMediaWithIds(siteModel: SiteModel?, mediaIds: List<Long?>?): List<MediaModel> {
        return MediaSqlUtils.getSiteMediaWithIds(siteModel, mediaIds)
    }

    fun getSiteImages(siteModel: SiteModel?): List<MediaModel> {
        return MediaSqlUtils.getSiteImages(siteModel)
    }

    fun getSiteVideos(siteModel: SiteModel?): List<MediaModel> {
        return MediaSqlUtils.getSiteVideos(siteModel)
    }

    fun getSiteAudio(siteModel: SiteModel?): List<MediaModel> {
        return MediaSqlUtils.getSiteAudio(siteModel)
    }

    fun getSiteDocuments(siteModel: SiteModel?): List<MediaModel> {
        return MediaSqlUtils.getSiteDocuments(siteModel)
    }

    fun getSiteImageCount(siteModel: SiteModel?): Int {
        return getSiteImages(siteModel).size
    }

    fun getSiteImagesExcludingIds(siteModel: SiteModel?, filter: List<Long?>?): List<MediaModel> {
        return MediaSqlUtils.getSiteImagesExcluding(siteModel, filter)
    }

    fun getUnattachedSiteMedia(siteModel: SiteModel?): List<MediaModel> {
        return MediaSqlUtils.matchSiteMedia(siteModel, MediaModelTable.POST_ID, 0)
    }

    fun getUnattachedSiteMediaCount(siteModel: SiteModel?): Int {
        return getUnattachedSiteMedia(siteModel).size
    }

    fun getLocalSiteMedia(siteModel: SiteModel?): List<MediaModel> {
        val expectedState = UPLOADED
        return MediaSqlUtils.getSiteMediaExcluding(siteModel, MediaModelTable.UPLOAD_STATE, expectedState)
    }

    fun getSiteMediaWithState(siteModel: SiteModel?, expectedState: MediaUploadState?): List<MediaModel> {
        return MediaSqlUtils.matchSiteMedia(siteModel, MediaModelTable.UPLOAD_STATE, expectedState)
    }

    fun getUrlForSiteVideoWithVideoPressGuid(siteModel: SiteModel?, videoPressGuid: String?): String? {
        val media = MediaSqlUtils.matchSiteMedia(siteModel, MediaModelTable.VIDEO_PRESS_GUID, videoPressGuid)
        return if (media.size > 0) media[0].url else null
    }

    fun getThumbnailUrlForSiteMediaWithId(siteModel: SiteModel?, mediaId: Long): String? {
        val media = MediaSqlUtils.getSiteMediaWithId(siteModel, mediaId)
        return if (media.size > 0) media[0].thumbnailUrl else null
    }

    fun searchSiteMedia(siteModel: SiteModel?, searchTerm: String?): List<MediaModel> {
        return MediaSqlUtils.searchSiteMedia(siteModel, searchTerm)
    }

    fun searchSiteImages(siteModel: SiteModel?, searchTerm: String?): List<MediaModel> {
        return MediaSqlUtils.searchSiteImages(siteModel, searchTerm)
    }

    fun searchSiteVideos(siteModel: SiteModel?, searchTerm: String?): List<MediaModel> {
        return MediaSqlUtils.searchSiteVideos(siteModel, searchTerm)
    }

    fun searchSiteAudio(siteModel: SiteModel?, searchTerm: String?): List<MediaModel> {
        return MediaSqlUtils.searchSiteAudio(siteModel, searchTerm)
    }

    fun searchSiteDocuments(siteModel: SiteModel?, searchTerm: String?): List<MediaModel> {
        return MediaSqlUtils.searchSiteDocuments(siteModel, searchTerm)
    }

    fun getMediaForPostWithPath(postModel: PostImmutableModel, filePath: String?): MediaModel? {
        val media = MediaSqlUtils.matchPostMedia(postModel.id, MediaModelTable.FILE_PATH, filePath)
        return if (media.size > 0) media[0] else null
    }

    fun getMediaForPost(postModel: PostImmutableModel): List<MediaModel> {
        return MediaSqlUtils.matchPostMedia(postModel.id)
    }

    fun getMediaForPostWithState(postModel: PostImmutableModel, expectedState: MediaUploadState?): List<MediaModel> {
        return MediaSqlUtils.matchPostMedia(
                postModel.id, MediaModelTable.UPLOAD_STATE,
                expectedState
        )
    }

    fun getNextSiteMediaToDelete(siteModel: SiteModel?): MediaModel? {
        val media = MediaSqlUtils.matchSiteMedia(
                siteModel,
                MediaModelTable.UPLOAD_STATE, DELETING.toString()
        )
        return if (media.size > 0) media[0] else null
    }

    fun hasSiteMediaToDelete(siteModel: SiteModel?): Boolean {
        return getNextSiteMediaToDelete(siteModel) != null
    }

    private fun removeAllMedia() {
        MediaSqlUtils.deleteAllMedia()
        val event = OnMediaChanged(REMOVE_ALL_MEDIA)
        emitChange(event)
    }

    //
    // Action implementations
    //
    fun updateMedia(media: MediaModel?, emit: Boolean) {
        val event = when {
            media == null -> {
                OnMediaChanged(UPDATE_MEDIA, MediaError(NULL_MEDIA_ARG))
            }
            MediaSqlUtils.insertOrUpdateMedia(media) > 0 -> {
                OnMediaChanged(UPDATE_MEDIA, listOf(media))
            }
            else -> {
                OnMediaChanged(UPDATE_MEDIA, MediaError(DB_QUERY_FAILURE))
            }
        }
        if (emit) {
            emitChange(event)
        }
    }

    private fun removeMedia(media: MediaModel?) {
        val event = when {
            media == null -> {
                OnMediaChanged(REMOVE_MEDIA, MediaError(NULL_MEDIA_ARG))
            }
            MediaSqlUtils.deleteMedia(media) > 0 -> {
                OnMediaChanged(REMOVE_MEDIA, listOf(media))
            }
            else -> {
                OnMediaChanged(REMOVE_MEDIA, MediaError(DB_QUERY_FAILURE))
            }
        }
        emitChange(event)
    }

    //
    // Helper methods that choose the appropriate network client to perform an action
    //
    private fun performPushMedia(payload: MediaPayload) {
        if (payload.media == null) {
            // null or empty media list -or- list contains a null value
            notifyMediaError(NULL_MEDIA_ARG, PUSH_MEDIA, null)
            return
        } else if (payload.media!!.mediaId <= 0) {
            // need media ID to push changes
            notifyMediaError(MALFORMED_MEDIA_ARG, PUSH_MEDIA, payload.media)
            return
        }
        if (payload.site!!.isUsingWpComRestApi) {
            mMediaRestClient.pushMedia(payload.site, payload.media)
        } else {
            mMediaXmlrpcClient.pushMedia(payload.site, payload.media)
        }
    }

    private fun notifyMediaUploadError(errorType: MediaErrorType, errorMessage: String, media: MediaModel) {
        val onMediaUploaded = OnMediaUploaded(media, progress = 1F, completed = false, canceled = false)
        onMediaUploaded.error = MediaError(errorType, errorMessage)
        emitChange(onMediaUploaded)
    }

    private fun performUploadMedia(payload: UploadMediaPayload) {
        val errorMessage = MediaUtils.getMediaValidationError(payload.media!!)
        if (errorMessage != null) {
            AppLog.e(MEDIA, "Media doesn't have required data: $errorMessage")
            payload.media!!.setUploadState(FAILED)
            MediaSqlUtils.insertOrUpdateMedia(payload.media)
            notifyMediaUploadError(MALFORMED_MEDIA_ARG, errorMessage, payload.media!!)
            return
        }
        payload.media!!.setUploadState(UPLOADING)
        MediaSqlUtils.insertOrUpdateMedia(payload.media)
        if (payload.stripLocation) {
            MediaUtils.stripLocation(payload.media!!.filePath)
        }
        if (payload.site!!.isUsingWpComRestApi) {
            mMediaRestClient.uploadMedia(payload.site, payload.media)
        } else {
            mMediaXmlrpcClient.uploadMedia(payload.site, payload.media)
        }
    }

    private fun performFetchMediaList(payload: FetchMediaListPayload) {
        var offset = 0
        if (payload.loadMore) {
            val list: MutableList<String> = ArrayList()
            list.add(UPLOADED.toString())
            offset = if (payload.mimeType != null) {
                MediaSqlUtils.getMediaWithStatesAndMimeType(
                        payload.site,
                        list,
                        payload.mimeType.value
                )
                        .size
            } else {
                MediaSqlUtils.getMediaWithStates(payload.site, list).size
            }
        }
        if (payload.site.isUsingWpComRestApi) {
            mMediaRestClient.fetchMediaList(payload.site, payload.number, offset, payload.mimeType)
        } else {
            mMediaXmlrpcClient.fetchMediaList(payload.site, payload.number, offset, payload.mimeType)
        }
    }

    private fun performFetchMedia(payload: MediaPayload) {
        if (payload.site == null || payload.media == null) {
            // null or empty media list -or- list contains a null value
            notifyMediaError(NULL_MEDIA_ARG, FETCH_MEDIA, payload.media)
            return
        }
        if (payload.site!!.isUsingWpComRestApi) {
            mMediaRestClient.fetchMedia(payload.site, payload.media)
        } else {
            mMediaXmlrpcClient.fetchMedia(payload.site, payload.media)
        }
    }

    private fun performDeleteMedia(payload: MediaPayload) {
        if (payload.media == null) {
            notifyMediaError(NULL_MEDIA_ARG, DELETE_MEDIA, null)
            return
        }
        if (payload.site!!.isUsingWpComRestApi) {
            mMediaRestClient.deleteMedia(payload.site, payload.media)
        } else {
            mMediaXmlrpcClient.deleteMedia(payload.site, payload.media)
        }
    }

    private fun performCancelUpload(payload: CancelMediaPayload) {
        if (payload.media == null) {
            return
        }
        val media = payload.media
        if (payload.delete) {
            MediaSqlUtils.deleteMedia(media)
        } else {
            media.setUploadState(FAILED)
            MediaSqlUtils.insertOrUpdateMedia(media)
        }
        if (payload.site.isUsingWpComRestApi) {
            mMediaRestClient.cancelUpload(media)
        } else {
            mMediaXmlrpcClient.cancelUpload(media)
        }
    }

    private fun handleMediaPushed(payload: MediaPayload) {
        val media = payload.media?.apply {
            updateMedia(this, false)
        }
        val onMediaChanged = if (media != null) {
            OnMediaChanged(PUSH_MEDIA, listOf(media), payload.error)
        } else {
            OnMediaChanged(PUSH_MEDIA, payload.error)
        }
        emitChange(onMediaChanged)
    }

    private fun handleMediaUploaded(payload: ProgressPayload) {
        if (payload.isError || payload.canceled || payload.completed) {
            updateMedia(payload.media, false)
        }
        val onMediaUploaded = OnMediaUploaded(payload.media, payload.progress, payload.completed, payload.canceled)
        onMediaUploaded.error = payload.error
        emitChange(onMediaUploaded)
    }

    private fun handleMediaCanceled(payload: ProgressPayload) {
        val onMediaUploaded = OnMediaUploaded(payload.media, payload.progress, payload.completed, payload.canceled)
        onMediaUploaded.error = payload.error
        emitChange(onMediaUploaded)
    }

    private fun updateFetchedMediaList(payload: FetchMediaListResponsePayload) {
        // if we loaded another page, simply add the fetched media and be done
        if (payload.loadedMore) {
            for (media in payload.mediaList) {
                updateMedia(media, false)
            }
            return
        }

        // build separate lists of existing and new media
        val existingMediaList: MutableList<MediaModel> = ArrayList()
        val newMediaList: MutableList<MediaModel> = ArrayList()
        for (fetchedMedia in payload.mediaList) {
            val media = getSiteMediaWithId(payload.site, fetchedMedia.mediaId)
            if (media != null) {
                // retain the local ID, then update this media item
                fetchedMedia.id = media.id
                existingMediaList.add(fetchedMedia)
                updateMedia(fetchedMedia, false)
            } else {
                newMediaList.add(fetchedMedia)
            }
        }

        // remove media that is NOT in the existing list
        var mimeTypeValue = ""
        if (payload.mimeType != null) {
            mimeTypeValue = payload.mimeType.value
        }
        MediaSqlUtils.deleteUploadedSiteMediaNotInList(
                payload.site, existingMediaList, mimeTypeValue
        )

        // add new media
        for (media in newMediaList) {
            updateMedia(media, false)
        }
    }

    private fun handleMediaListFetched(payload: FetchMediaListResponsePayload) {
        val onMediaListFetched: OnMediaListFetched
        onMediaListFetched = if (payload.isError) {
            OnMediaListFetched(payload.site, payload.error, payload.mimeType)
        } else {
            updateFetchedMediaList(payload)
            OnMediaListFetched(payload.site, payload.canLoadMore, payload.mimeType)
        }
        emitChange(onMediaListFetched)
    }

    private fun handleMediaFetched(payload: MediaPayload) {
        val onMediaChanged = payload.media?.let { media ->
            MediaSqlUtils.insertOrUpdateMedia(media)
            OnMediaChanged(FETCH_MEDIA, listOf(media), payload.error)
        } ?: OnMediaChanged(FETCH_MEDIA, payload.error)
        emitChange(onMediaChanged)
    }

    private fun handleMediaDeleted(payload: MediaPayload) {
        val onMediaChanged = payload.media?.let { media ->
            MediaSqlUtils.deleteMedia(payload.media)
            OnMediaChanged(DELETE_MEDIA, listOf(media), payload.error)
        } ?: OnMediaChanged(DELETE_MEDIA, payload.error)
        emitChange(onMediaChanged)
    }

    private fun notifyMediaError(
        errorType: MediaErrorType,
        errorMessage: String?,
        cause: MediaAction,
        media: List<MediaModel>
    ) {
        val mediaChange = OnMediaChanged(cause, media)
        mediaChange.error = MediaError(errorType, errorMessage)
        emitChange(mediaChange)
    }

    private fun notifyMediaError(errorType: MediaErrorType, cause: MediaAction, media: MediaModel?) {
        notifyMediaError(errorType, null, cause, media)
    }

    private fun notifyMediaError(
        errorType: MediaErrorType,
        errorMessage: String?,
        cause: MediaAction,
        media: MediaModel?
    ) {
        val mediaList: List<MediaModel> = media?.let { listOf(media) } ?: listOf()
        notifyMediaError(errorType, errorMessage, cause, mediaList)
    }

    private fun performUploadStockMedia(payload: UploadStockMediaPayload) {
        mMediaRestClient.uploadStockMedia(payload.site, payload.stockMediaList)
    }

    private fun handleStockMediaUploaded(payload: UploadedStockMediaPayload) {
        val onStockMediaUploaded: OnStockMediaUploaded
        onStockMediaUploaded = if (payload.isError) {
            OnStockMediaUploaded(payload.site, payload.error!!)
        } else {
            // add uploaded media to the store
            for (media in payload.mediaList) {
                updateMedia(media, false)
            }
            OnStockMediaUploaded(payload.site, payload.mediaList)
        }
        emitChange(onStockMediaUploaded)
    }
}
