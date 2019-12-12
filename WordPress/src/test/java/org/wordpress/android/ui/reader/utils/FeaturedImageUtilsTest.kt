package org.wordpress.android.ui.reader.utils

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.models.ReaderPost

@RunWith(MockitoJUnitRunner::class)
class FeaturedImageUtilsTest {
    private val featuredImageUtils = FeaturedImageUtils()
    @Test
    fun `does not show the same featured image twice`() {
        val image = "https://wordpress.com/image.png"
        val readerPost = initReaderPost(image, image)

        val result = featuredImageUtils.showFeaturedImage(readerPost.featuredImage, readerPost.text)

        assertThat(result).isFalse()
    }

    @Test
    fun `does not show featured image twice with only different with size params`() {
        val featuredImage = "https://i0.wp.com/www.subtraction.com/wp-content/uploads/2017/11/" +
                "2017-11-06-iphone-x.png?quality=80&strip=info&w=1600"
        val bodyImage = "https://i0.wp.com/www.subtraction.com/wp-content/uploads/2017/11/" +
                "2017-11-06-iphone-x-725x209.png?ssl=1"
        val readerPost = initReaderPost(bodyImage, featuredImage)

        val result = featuredImageUtils.showFeaturedImage(readerPost.featuredImage, readerPost.text)

        assertThat(result).isFalse()
    }

    @Test
    fun `shows featured image with a different name with dashes`() {
        val featuredImage = "https://wordpress.com/image.png"
        val bodyImage = "https://wordpress.com/image-dog-10.png"
        val readerPost = initReaderPost(bodyImage, featuredImage)

        val result = featuredImageUtils.showFeaturedImage(readerPost.featuredImage, readerPost.text)

        assertThat(result).isTrue()
    }

    @Test
    fun `shows featured image with different name`() {
        val featuredImage = "https://wordpress.com/image.png"
        val bodyImage = "https://wordpress.com/image2.png"
        val readerPost = initReaderPost(bodyImage, featuredImage)

        val result = featuredImageUtils.showFeaturedImage(readerPost.featuredImage, readerPost.text)

        assertThat(result).isTrue()
    }

    @Test
    fun `shows featured image with the same name from different host`() {
        val featuredImage = "https://wordpress.com/image.png"
        val bodyImage = "https://wordpress2.com/image.png"
        val readerPost = initReaderPost(bodyImage, featuredImage)

        val result = featuredImageUtils.showFeaturedImage(readerPost.featuredImage, readerPost.text)

        assertThat(result).isTrue()
    }

    private fun initReaderPost(
        bodyImage: String,
        featuredImage: String
    ): ReaderPost {
        val postBody = buildHtml(bodyImage)
        val readerPost = ReaderPost()
        readerPost.featuredImage = featuredImage
        readerPost.text = postBody
        return readerPost
    }

    private fun buildHtml(img: String) = "<html>\n" +
            "<head>\n" +
            "<title>Title of the document</title>\n" +
            "</head>\n" +
            "\n" +
            "<body>\n" +
            "<img alt=\"description\" src=\"$img\">" +
            "</body>\n" +
            "\n" +
            "</html>"
}
