package org.wordpress.android.models

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.R

@RunWith(MockitoJUnitRunner::class)
class NoticonUtilsTest {
    private val noteUtils = NoticonUtils()

    @Test
    fun `transforms mention noticon`() {
        val gridicon = noteUtils.noticonToGridicon("\uf814")

        assertThat(gridicon).isEqualTo(R.drawable.ic_mention_white_24dp)
    }

    @Test
    fun `transforms comment noticon`() {
        val gridicon = noteUtils.noticonToGridicon("\uf300")

        assertThat(gridicon).isEqualTo(R.drawable.ic_comment_white_24dp)
    }

    @Test
    fun `transforms add noticon`() {
        val gridicon = noteUtils.noticonToGridicon("\uf801")

        assertThat(gridicon).isEqualTo(R.drawable.ic_add_white_24dp)
    }

    @Test
    fun `transforms info noticon`() {
        val gridicon = noteUtils.noticonToGridicon("\uf455")

        assertThat(gridicon).isEqualTo(R.drawable.ic_info_white_24dp)
    }

    @Test
    fun `transforms lock noticon`() {
        val gridicon = noteUtils.noticonToGridicon("\uf470")

        assertThat(gridicon).isEqualTo(R.drawable.ic_lock_white_24dp)
    }

    @Test
    fun `transforms stats alt noticon`() {
        val gridicon = noteUtils.noticonToGridicon("\uf806")

        assertThat(gridicon).isEqualTo(R.drawable.ic_stats_alt_white_24dp)
    }

    @Test
    fun `transforms reblog noticon`() {
        val gridicon = noteUtils.noticonToGridicon("\uf805")

        assertThat(gridicon).isEqualTo(R.drawable.ic_reblog_white_24dp)
    }

    @Test
    fun `transforms star noticon`() {
        val gridicon = noteUtils.noticonToGridicon("\uf408")

        assertThat(gridicon).isEqualTo(R.drawable.ic_star_white_24dp)
    }

    @Test
    fun `transforms trophy noticon`() {
        val gridicon = noteUtils.noticonToGridicon("\uf804")

        assertThat(gridicon).isEqualTo(R.drawable.ic_trophy_white_24dp)
    }

    @Test
    fun `transforms reply noticon`() {
        val gridicon = noteUtils.noticonToGridicon("\uf467")

        assertThat(gridicon).isEqualTo(R.drawable.ic_reply_white_24dp)
    }

    @Test
    fun `transforms notice noticon`() {
        val gridicon = noteUtils.noticonToGridicon("\uf414")

        assertThat(gridicon).isEqualTo(R.drawable.ic_notice_white_24dp)
    }

    @Test
    fun `transforms checkmark noticon`() {
        val gridicon = noteUtils.noticonToGridicon("\uf418")

        assertThat(gridicon).isEqualTo(R.drawable.ic_checkmark_white_24dp)
    }

    @Test
    fun `transforms cart noticon`() {
        val gridicon = noteUtils.noticonToGridicon("\uf447")

        assertThat(gridicon).isEqualTo(R.drawable.ic_cart_white_24dp)
    }

    @Test
    fun `defaults to info noticon`() {
        val gridicon = noteUtils.noticonToGridicon("\uabc1")

        assertThat(gridicon).isEqualTo(R.drawable.ic_info_white_24dp)
    }
}
