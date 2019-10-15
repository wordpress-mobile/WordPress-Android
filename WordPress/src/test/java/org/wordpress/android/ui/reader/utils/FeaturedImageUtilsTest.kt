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
    fun `finds exactly the same featured image`() {
        val image = "https://wordpress.com/image.png"
        val postBody = buildHtml(image)
        val readerPost = ReaderPost()
        readerPost.featuredImage = image
        readerPost.text = postBody

        val result = featuredImageUtils.showFeaturedImage(readerPost.featuredImage, readerPost.text)

        assertThat(result).isFalse()
    }

    @Test
    fun `finds featured image with size params`() {
        val featuredImage = "https://i0.wp.com/www.subtraction.com/wp-content/uploads/2017/11/" +
                "2017-11-06-iphone-x.png?quality=80&strip=info&w=1600"
        val bodyImage = "https://i0.wp.com/www.subtraction.com/wp-content/uploads/2017/11/" +
                "2017-11-06-iphone-x.png?ssl=1"
        val postBody = buildHtml(bodyImage)
        val readerPost = ReaderPost()
        readerPost.featuredImage = featuredImage
        readerPost.text = postBody

        val result = featuredImageUtils.showFeaturedImage(readerPost.featuredImage, readerPost.text)

        assertThat(result).isFalse()
    }

    @Test
    fun `does not find featured image with the same name`() {
        val featuredImage = "https://wordpress.com/image.png"
        val bodyImage = "https://wordpress.com/image2.png"
        val postBody = buildHtml(bodyImage)
        val readerPost = ReaderPost()
        readerPost.featuredImage = featuredImage
        readerPost.text = postBody

        val result = featuredImageUtils.showFeaturedImage(readerPost.featuredImage, readerPost.text)

        assertThat(result).isTrue()
    }

    @Test
    fun `does not find featured image with the same name from different host`() {
        val featuredImage = "https://wordpress.com/image.png"
        val bodyImage = "https://wordpress2.com/image.png"
        val postBody = buildHtml(bodyImage)
        val readerPost = ReaderPost()
        readerPost.featuredImage = featuredImage
        readerPost.text = postBody

        val result = featuredImageUtils.showFeaturedImage(readerPost.featuredImage, readerPost.text)

        assertThat(result).isTrue()
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
