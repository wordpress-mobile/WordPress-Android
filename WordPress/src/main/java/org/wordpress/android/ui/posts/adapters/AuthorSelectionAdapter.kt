package org.wordpress.android.ui.posts.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import androidx.annotation.CallSuper
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.graphics.ColorUtils
import com.google.android.material.elevation.ElevationOverlayProvider
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.ui.posts.AuthorFilterListItemUIState
import org.wordpress.android.ui.posts.AuthorFilterSelection
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.GravatarUtils
import org.wordpress.android.util.getColorFromAttribute
import org.wordpress.android.util.image.ImageManager
import org.wordpress.android.util.image.ImageType.NO_PLACEHOLDER
import javax.inject.Inject

class AuthorSelectionAdapter(context: Context) : BaseAdapter() {
    @Inject lateinit var imageManager: ImageManager
    @Inject lateinit var uiHelpers: UiHelpers

    private val items = mutableListOf<AuthorFilterListItemUIState>()

    init {
        (context.applicationContext as WordPress).component().inject(this)
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        var view: View? = convertView
        val holder: DropdownViewHolder

        if (view == null) {
            val inflater = LayoutInflater.from(parent.context)
            view = inflater.inflate(R.layout.author_selection_dropdown, parent, false)
            holder = DropdownViewHolder(view)
            view.tag = holder
        } else {
            holder = view.tag as DropdownViewHolder
        }

        holder.bind(items[position], imageManager, uiHelpers)

        return view!!
    }

    override fun getItemId(position: Int): Long = items[position].id

    fun getIndexOfSelection(selection: AuthorFilterSelection): Int? {
        for ((index, item) in items.withIndex()) {
            if (item.id == selection.id) {
                return index
            }
        }

        return null
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var view: View? = convertView
        val holder: NormalViewHolder

        if (view == null) {
            val inflater = LayoutInflater.from(parent.context)
            view = inflater.inflate(R.layout.author_selection, parent, false)
            holder = NormalViewHolder(view)
            view.tag = holder
        } else {
            holder = view.tag as NormalViewHolder
        }

        holder.bind(items[position], imageManager, uiHelpers)

        return view!!
    }

    override fun hasStableIds(): Boolean = true

    override fun getItem(position: Int): Any = items[position]

    override fun getCount(): Int = items.count()

    fun updateItems(newItems: List<AuthorFilterListItemUIState>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    private open class NormalViewHolder(protected val itemView: View) {
        protected val image: AppCompatImageView = itemView.findViewById(R.id.author_selection_image)

        @CallSuper
        open fun bind(
            state: AuthorFilterListItemUIState,
            imageManager: ImageManager,
            uiHelpers: UiHelpers
        ) {
            /**
             * We can't use error/placeholder drawables as it causes an issue described here
             * https://github.com/wordpress-mobile/WordPress-Android/issues/9745.
             * It seems getView method always returns convertView == null when used with Spinner. When we invoke
             * imageManager.load..(url..) in the view holder and the 'url' is empty or the requests fails
             * an error/placeholder drawable is used. However, this results in another layout/measure phase
             * -> getView(..) is called again. However, since the convertView == null we inflate a new view and
             * imageManager.load..(..) is invoked again - this goes on forever.
             * In order to prevent this issue we don't use placeholder/error drawables in this case.
             * The cost of this solution is that an empty circle is shown if we don't have the avatar in the cache
             * and the request fails.
             */
            when (state) {
                is AuthorFilterListItemUIState.Everyone -> {
                    imageManager.load(image, state.imageRes)
                }
                is AuthorFilterListItemUIState.Me -> {
                    val avatarSize = image.resources.getDimensionPixelSize(R.dimen.avatar_sz_small)
                    val url = GravatarUtils.fixGravatarUrl(state.avatarUrl, avatarSize)
                    imageManager.loadIntoCircle(image, NO_PLACEHOLDER, url)
                }
            }
        }
    }

    private class DropdownViewHolder(itemView: View) : NormalViewHolder(itemView) {
        private val text: AppCompatTextView = itemView.findViewById(R.id.author_selection_text)

        override fun bind(
            state: AuthorFilterListItemUIState,
            imageManager: ImageManager,
            uiHelpers: UiHelpers
        ) {
            super.bind(state, imageManager, uiHelpers)
            val context = itemView.context

            // Because it's a custom popup list we need to manage colors of list items manually
            val elevationOverlayProvider = ElevationOverlayProvider(context)
            val appbarElevation = context.resources.getDimension(R.dimen.appbar_elevation)
            val elevatedSurfaceColor = elevationOverlayProvider.compositeOverlayWithThemeSurfaceColorIfNeeded(
                    appbarElevation
            )

            val selectedColor = ColorUtils
                    .setAlphaComponent(
                            context.getColorFromAttribute(R.attr.colorOnSurface),
                            context.resources.getInteger(R.integer.custom_popup_selected_list_item_opacity_dec)
                    )

            text.text = uiHelpers.getTextOfUiString(context, state.text)
            if (state.isSelected) {
                itemView.setBackgroundColor(selectedColor)
            } else {
                itemView.setBackgroundColor(elevatedSurfaceColor)
            }
        }
    }
}
