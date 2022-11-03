package org.wordpress.android.fluxc.comments

import com.android.volley.NetworkResponse
import com.android.volley.RequestQueue
import com.android.volley.Response
import org.assertj.core.api.Assertions.assertThat
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.CommentStatus.APPROVED
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.comments.CommentsMapper
import org.wordpress.android.fluxc.network.HTTPAuthManager
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.xmlrpc.XMLRPCRequest
import org.wordpress.android.fluxc.network.xmlrpc.XMLRPCRequestBuilder
import org.wordpress.android.fluxc.network.xmlrpc.comment.CommentsXMLRPCClient
import org.wordpress.android.fluxc.persistence.comments.CommentsDao.CommentEntity
import org.wordpress.android.fluxc.store.CommentStore.CommentError
import org.wordpress.android.fluxc.store.CommentStore.CommentErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.test
import org.wordpress.android.fluxc.utils.CommentErrorUtilsWrapper
import org.wordpress.android.fluxc.utils.DateTimeUtilsWrapper
import java.util.concurrent.CountDownLatch

@RunWith(RobolectricTestRunner::class)
class CommentsXMLRPCClientTest {
    private lateinit var dispatcher: Dispatcher
    private lateinit var requestQueue: RequestQueue
    private lateinit var userAgent: UserAgent
    private lateinit var httpAuthManager: HTTPAuthManager
    private lateinit var commentsMapper: CommentsMapper
    private lateinit var commentErrorUtilsWrapper: CommentErrorUtilsWrapper
    private lateinit var site: SiteModel

    private lateinit var xmlRpcClient: CommentsXMLRPCClient
    private var mockedResponse = ""
    private var countDownLatch: CountDownLatch? = null

    @Before
    fun setUp() {
        dispatcher = Mockito.mock(Dispatcher::class.java)
        requestQueue = Mockito.mock(RequestQueue::class.java)
        userAgent = Mockito.mock(UserAgent::class.java)
        httpAuthManager = Mockito.mock(HTTPAuthManager::class.java)
        commentErrorUtilsWrapper = Mockito.mock(CommentErrorUtilsWrapper::class.java)
        commentsMapper = CommentsMapper(DateTimeUtilsWrapper())
        site = Mockito.mock(SiteModel::class.java)

        doAnswer { invocation ->
            val request = invocation.arguments[0] as XMLRPCRequest
            try {
                val requestClass = Class.forName(
                        "org.wordpress.android.fluxc.network.xmlrpc.XMLRPCRequest"
                ) as Class<XMLRPCRequest>
                // Reflection code equivalent to:
                // Object o = request.parseNetworkResponse(data)
                val parseNetworkResponse = requestClass.getDeclaredMethod(
                        "parseNetworkResponse",
                        NetworkResponse::class.java
                )
                parseNetworkResponse.isAccessible = true
                val nr = NetworkResponse(mockedResponse.toByteArray())
                val o = parseNetworkResponse.invoke(request, nr) as Response<Any>
                // Reflection code equivalent to:
                // request.deliverResponse(o)
                val deliverResponse = requestClass.getDeclaredMethod("deliverResponse", Any::class.java)
                deliverResponse.isAccessible = true
                deliverResponse.invoke(request, o.result)
            } catch (e: Exception) {
                Assert.assertTrue("Unexpected exception: $e", false)
            }
            countDownLatch?.countDown()
            null
        }.whenever(requestQueue).add<Any>(any())

        xmlRpcClient = CommentsXMLRPCClient(
                dispatcher = dispatcher,
                requestQueue = requestQueue,
                userAgent = userAgent,
                httpAuthManager = httpAuthManager,
                commentErrorUtilsWrapper = commentErrorUtilsWrapper,
                xmlrpcRequestBuilder = XMLRPCRequestBuilder(),
                commentsMapper = commentsMapper
        )

        whenever(site.selfHostedSiteId).thenReturn(SITE_ID)
        whenever(site.username).thenReturn("username")
        whenever(site.password).thenReturn("password")
        whenever(site.xmlRpcUrl).thenReturn("https://self-hosted/xmlrpc.php")
    }

    @Test
    fun `fetchCommentsPage returns fetched page`() = test {
        mockedResponse = """
            <?xml version="1.0" encoding="UTF-8"?><methodResponse><params><param><value><array><data>
            <value><struct><member><name>date_created_gmt</name><value><dateTime.iso8601>20210727T23:56:21
            </dateTime.iso8601></value></member><member><name>user_id</name><value><string>1</string>
            </value></member><member><name>comment_id</name><value><string>44</string></value></member>
            <member><name>parent</name><value><string>41</string></value></member><member><name>status</name>
            <value><string>hold</string></value></member><member><name>content</name><value><string>
            this is a content example</string></value></member><member><name>link</name><value><string>
            http://test-debug/index.php/2021/04/01/happy-monday/#comment-44</string></value>
            </member><member><name>post_id</name><value><string>367</string></value></member>
            <member><name>post_title</name><value><string>happy-monday</string></value></member>
            <member><name>author</name><value><string>authorname</string></value></member><member>
            <name>author_url</name><value><string></string></value></member><member><name>author_email</name>
            <value><string>authorname@mydomain.com</string></value></member><member><name>author_ip
            </name><value><string>111.222.333.444</string></value></member><member><name>type
            </name><value><string></string></value></member></struct></value></data>
            </array></value></param></params></methodResponse>
        """

        val payload = xmlRpcClient.fetchCommentsPage(
                site = site,
                number = PAGE_LEN,
                offset = 0,
                status = APPROVED
        )

        assertThat(payload.isError).isFalse
        assertThat(payload.response).isNotEmpty
    }

    @Test
    fun `fetchCommentsPage returns error on API fail`() = test {
        mockedResponse = """<?xml version="1.0" encoding="UTF-8"?>
            <methodResponse><params><param><value>
            <string>error</string>
            </value></param></params></methodResponse>
        """

        whenever(commentErrorUtilsWrapper.networkToCommentError(any())).thenReturn(CommentError(GENERIC_ERROR, ""))

        val payload = xmlRpcClient.fetchCommentsPage(
                site = site,
                number = PAGE_LEN,
                offset = 0,
                status = APPROVED
        )

        assertThat(payload.isError).isTrue
    }

    @Test
    fun `pushComment returns pushed comment`() = test {
        mockedResponse = """
            <?xml version="1.0" encoding="UTF-8"?>
            <methodResponse>
              <params>
                <param>
                  <value>
                  <boolean>1</boolean>
                  </value>
                </param>
              </params>
            </methodResponse>
        """

        val comment = getDefaultComment()

        val payload = xmlRpcClient.pushComment(
                site = site,
                comment = comment
        )

        assertThat(payload.isError).isFalse
        assertThat(payload.response).isEqualTo(comment)
    }

    @Test
    fun `updateEditComment returns pushed comment`() = test {
        mockedResponse = """
            <?xml version="1.0" encoding="UTF-8"?>
            <methodResponse>
              <params>
                <param>
                  <value>
                  <boolean>1</boolean>
                  </value>
                </param>
              </params>
            </methodResponse>
        """

        val comment = getDefaultComment()

        val payload = xmlRpcClient.updateEditComment(
                site = site,
                comment = comment
        )

        assertThat(payload.isError).isFalse
        assertThat(payload.response).isEqualTo(comment)
    }

    @Test
    @Suppress("MaxLineLength")
    fun `fetchComment returns fetched comment`() = test {
        mockedResponse = """
            <?xml version="1.0" encoding="UTF-8"?>
            <methodResponse>
              <params>
                <param>
                  <value>
                  <struct>
              <member><name>date_created_gmt</name><value><dateTime.iso8601>20210727T20:33:41</dateTime.iso8601></value></member>
              <member><name>user_id</name><value><string>44</string></value></member>
              <member><name>comment_id</name><value><string>34</string></value></member>
              <member><name>parent</name><value><string>33</string></value></member>
              <member><name>status</name><value><string>approve</string></value></member>
              <member><name>content</name><value><string>test1000</string></value></member>
              <member><name>link</name><value><string>http://test-debug.org/index.php/2021/04/01/no-jp/#comment-34</string></value></member>
              <member><name>post_id</name><value><string>367</string></value></member>
              <member><name>post_title</name><value><string>no jp</string></value></member>
              <member><name>author</name><value><string>authorname</string></value></member>
              <member><name>author_url</name><value><string></string></value></member>
              <member><name>author_email</name><value><string>authorname@mydomain.com</string></value></member>
              <member><name>author_ip</name><value><string>111.111.111.111</string></value></member>
              <member><name>type</name><value><string></string></value></member>
            </struct>
                  </value>
                </param>
              </params>
            </methodResponse>
        """

        val comment = getDefaultComment()

        val payload = xmlRpcClient.fetchComment(
                site = site,
                remoteCommentId = comment.remoteCommentId
        )

        assertThat(payload.isError).isFalse
        assertThat(payload.response).isEqualTo(comment)
    }

    @Test
    fun `fetchComment returns error on API fail`() = test {
        mockedResponse = """<?xml version="1.0" encoding="UTF-8"?>
            <methodResponse><params><param><value>
            <string>error</string>
            </value></param></params></methodResponse>
        """

        val comment = getDefaultComment()
        whenever(commentErrorUtilsWrapper.networkToCommentError(any())).thenReturn(CommentError(GENERIC_ERROR, ""))

        val payload = xmlRpcClient.fetchComment(
                site = site,
                remoteCommentId = comment.remoteCommentId
        )

        assertThat(payload.isError).isTrue
    }

    @Test
    fun `deleteComment returns no comment`() = test {
        mockedResponse = """
            <?xml version="1.0" encoding="UTF-8"?>
            <methodResponse>
              <params>
                <param>
                  <value>
                  <boolean>1</boolean>
                  </value>
                </param>
              </params>
            </methodResponse>
        """

        val comment = getDefaultComment()

        val payload = xmlRpcClient.deleteComment(
                site = site,
                remoteCommentId = comment.remoteCommentId
        )

        assertThat(payload.isError).isFalse
        assertThat(payload.response).isNull()
    }

    @Test
    fun `createNewReply returns udpated reply`() = test {
        mockedResponse = """
            <?xml version="1.0" encoding="UTF-8"?>
            <methodResponse>
              <params>
                <param>
                  <value>
                  <int>56</int>
                  </value>
                </param>
              </params>
            </methodResponse>
        """

        val comment = getDefaultComment()
        val reply = comment.copy(content = "new reply content")

        val payload = xmlRpcClient.createNewReply(
                site = site,
                comment = comment,
                reply = reply
        )

        assertThat(payload.isError).isFalse
        assertThat(payload.response is CommentEntity).isTrue
        assertThat((payload.response as CommentEntity).remoteCommentId).isEqualTo(56)
    }

    @Test
    fun `createNewComment returns udpated comment`() = test {
        mockedResponse = """
            <?xml version="1.0" encoding="UTF-8"?>
            <methodResponse>
              <params>
                <param>
                  <value>
                  <int>56</int>
                  </value>
                </param>
              </params>
            </methodResponse>
        """

        val comment = getDefaultComment()

        val payload = xmlRpcClient.createNewComment(
                site = site,
                remotePostId = 100,
                comment = comment
        )

        assertThat(payload.isError).isFalse
        assertThat(payload.response is CommentEntity).isTrue
        assertThat((payload.response as CommentEntity).remoteCommentId).isEqualTo(56)
    }

    private fun getDefaultComment() = CommentEntity(
            id = 0,
            remoteCommentId = 34,
            remotePostId = 367,
            authorId = 44,
            localSiteId = 0,
            remoteSiteId = 200,
            authorUrl = "",
            authorName = "authorname",
            authorEmail = "authorname@mydomain.com",
            authorProfileImageUrl = null,
            postTitle = "no jp",
            status = "approved",
            datePublished = "2021-07-27T20:33:41+00:00",
            publishedTimestamp = 0,
            content = "test1000",
            url = "http://test-debug.org/index.php/2021/04/01/no-jp/#comment-34",
            hasParent = true,
            parentId = 33,
            iLike = false
    )

    companion object {
        private const val SITE_ID = 200L
        private const val PAGE_LEN = 30
    }
}
