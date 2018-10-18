package org.wordpress.android.ui.stats.refresh

import android.content.Context
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.TextPaint
import android.text.style.ClickableSpan
import android.view.View
import org.apache.commons.text.StringEscapeUtils
import org.wordpress.android.R
import org.wordpress.android.R.string
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.stats.InsightsLatestPostModel
import org.wordpress.android.fluxc.store.InsightsStore
import org.wordpress.android.ui.stats.StatsConstants
import org.wordpress.android.ui.stats.StatsUtils
import org.wordpress.android.ui.stats.refresh.BlockListItem.Columns
import org.wordpress.android.ui.stats.refresh.BlockListItem.Link
import org.wordpress.android.ui.stats.refresh.BlockListItem.Text
import org.wordpress.android.ui.stats.refresh.BlockListItem.Title
import org.wordpress.android.viewmodel.ResourceProvider
import java.util.Locale
import javax.inject.Inject

class LatestPostSummaryViewModel
@Inject constructor(
    private val insightsStore: InsightsStore,
    private val resourceProvider: ResourceProvider
) {
    suspend fun latestPostSummary(site: SiteModel, forced: Boolean = false): InsightsItem {
        val response = insightsStore.fetchLatestPostInsights(site, forced)
        val model = response.model
        val error = response.error

        return when {
            error != null -> Failed(R.string.stats_insights_latest_post_summary, error.message ?: error.type.name)
            else -> latestPostSummaryItem(model)
        }
    }

    private fun latestPostSummaryItem(model: InsightsLatestPostModel?): ListInsightItem {
        val items = mutableListOf<BlockListItem>()
        items.add(Title(string.stats_insights_latest_post_summary))
        if (model != null) {
            val message = buildMessage(model)
            items.add(Text(message))
            if (model.postViewsCount > 0 || model.postCommentCount > 0 || model.postLikeCount > 0) {
                val headers = listOf(R.string.stats_views, R.string.stats_likes, R.string.stats_comments)
                val values = listOf(
                        model.postViewsCount.toFormattedString(),
                        model.postLikeCount.toFormattedString(),
                        model.postCommentCount.toFormattedString()
                )
                items.add(Columns(headers = headers, values = values))
                items.add(Link(text = R.string.stats_insights_view_more) {})
            } else {
                items.add(Link(R.drawable.ic_share_blue_medium_24dp, R.string.stats_insights_share_post) {})
            }
        } else {
            val emptyMessage = resourceProvider.getString(R.string.stats_insights_latest_post_empty)
            items.add(Text(SpannableStringBuilder(emptyMessage)))
            items.add(Link(R.drawable.ic_create_blue_medium_24dp, R.string.stats_insights_create_post) {})
        }
        return ListInsightItem(items)
    }

    private fun buildMessage(model: InsightsLatestPostModel): Spannable {
        val sinceLabel = StatsUtils.getSinceLabel(resourceProvider, model.postDate)
                .toLowerCase(Locale.getDefault())
        val postTitle = StringEscapeUtils.unescapeHtml4(model.postTitle)
        val message = if (model.postViewsCount == 0) {
            val message = resourceProvider.getString(
                    string.stats_insights_latest_post_with_no_engagement,
                    sinceLabel,
                    postTitle
            )
            SpannableString(message).withClickableSpan(postTitle) {
                StatsUtils.openPostInReaderOrInAppWebview(
                        it,
                        model.siteId,
                        model.postId.toString(),
                        StatsConstants.ITEM_TYPE_POST,
                        model.postURL
                )
            }
        } else {
            resourceProvider.getString(
                    string.stats_insights_latest_post_message,
                    sinceLabel,
                    postTitle
            )
        }
        return SpannableStringBuilder(message)
    }

    private fun SpannableString.withClickableSpan(clickablePart: String, onClickListener: (Context) -> Unit): SpannableString {
        val clickableSpan = object : ClickableSpan() {
            override fun onClick(widget: View?) {
                widget?.context?.let { onClickListener.invoke(it) }
            }
            override fun updateDrawState(ds: TextPaint?) {
                ds?.color = resourceProvider.getColor(R.color.blue_wordpress)
                ds?.isUnderlineText = false
            }
        }
        val clickablePartStart = indexOf(clickablePart)
        setSpan(clickableSpan,
                clickablePartStart,
                clickablePartStart + clickablePart.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        return this
    }
}
