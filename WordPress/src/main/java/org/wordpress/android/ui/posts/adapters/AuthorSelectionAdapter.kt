package org.wordpress.android.ui.posts.adapters

import android.content.Context
import android.database.DataSetObserver
import android.support.annotation.ColorRes
import android.support.annotation.DrawableRes
import android.support.v4.content.ContextCompat
import android.support.v7.widget.AppCompatImageView
import android.support.v7.widget.AppCompatTextView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SpinnerAdapter
import androidx.annotation.StringRes
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.util.DisplayUtils
import org.wordpress.android.util.GravatarUtils
import org.wordpress.android.util.image.ImageManager
import org.wordpress.android.util.image.ImageType.AVATAR_WITH_BACKGROUND
import java.lang.ref.WeakReference
import javax.inject.Inject

class AuthorSelectionAdapter(context: Context) : SpinnerAdapter {
    @Inject lateinit var imageManager: ImageManager

    private var observers: MutableList<WeakReference<DataSetObserver>> = mutableListOf()

    var avatarUrl: String? = null
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    var selectedPosition: Int = 0

    init {
        (context.applicationContext as WordPress).component().inject(this)
    }

    override fun getCount(): Int = 2

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        var view: View? = convertView
        val holder: DropdownViewHolder

        if (view == null) {
            val inflater = LayoutInflater.from(parent.context)
            view = inflater.inflate(R.layout.post_list_author_selection_dropdown, parent, false)
            holder = DropdownViewHolder(view)
            view.tag = holder
        } else {
            holder = view.tag as DropdownViewHolder
        }

        @StringRes val textRes: Int = getItem(position)
        val isSelected = position == selectedPosition

        when (position) {
            0 -> {
                val avatarUrl = avatarUrl

                if (avatarUrl != null) {
                    holder.bind(avatarUrl, textRes, isSelected, imageManager)
                } else {
                    holder.bind(R.drawable.ic_user_grey_darken_20_24dp, textRes, isSelected, imageManager)
                }
            }

            else -> holder.bind(R.drawable.ic_multiple_users_grey_darken_20_24dp, textRes, isSelected, imageManager)
        }

        return view!!
    }

    @StringRes override fun getItem(position: Int): Int = when (position) {
        0 -> ME
        else -> EVERYONE
    }

    override fun getItemId(position: Int): Long {
        return when (position) {
            0 -> ME
            else -> EVERYONE
        }.toLong()
    }

    override fun getItemViewType(position: Int): Int = 0

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var view: View? = convertView
        val holder: NormalViewHolder

        if (view == null) {
            val inflater = LayoutInflater.from(parent.context)
            view = inflater.inflate(R.layout.post_list_author_selection, parent, false)
            holder = NormalViewHolder(view)
            view.tag = holder
        } else {
            holder = view.tag as NormalViewHolder
        }

        when (position) {
            0 -> {
                val avatarUrl = avatarUrl

                if (avatarUrl != null) {
                    holder.bind(avatarUrl, imageManager)
                } else {
                    holder.bind(R.drawable.ic_user_grey_darken_20_24dp, imageManager)
                }
            }

            else -> holder.bind(R.drawable.ic_multiple_users_grey_darken_20_24dp, imageManager)
        }

        return view!!
    }

    override fun getViewTypeCount(): Int = 1
    override fun hasStableIds(): Boolean = true
    override fun isEmpty(): Boolean = false

    fun notifyDataSetChanged() {
        for (observer in observers) {
            observer.get()?.onChanged()
        }
    }

    override fun registerDataSetObserver(observer: DataSetObserver) {
        for (alreadyAdded in observers) {
            if (observer == alreadyAdded.get()) {
                // No need to add since it is already added
                return
            }
        }

        observers.add(WeakReference(observer))
    }

    override fun unregisterDataSetObserver(observer: DataSetObserver) {
        for ((index, currentRef) in observers.withIndex()) {
            if (observer == currentRef.get()) {
                observers.removeAt(index)
                return
            }
        }
    }

    companion object {
        private const val ME = R.string.post_list_author_me
        private const val EVERYONE = R.string.post_list_author_everyone
    }

    class DropdownViewHolder(private val itemView: View) {
        private val image: AppCompatImageView = itemView.findViewById(R.id.post_list_author_selection_image)
        private val text: AppCompatTextView = itemView.findViewById(R.id.post_list_author_selection_text)

        private fun bind(@StringRes textRes: Int, isSelected: Boolean) {
            text.setText(textRes)

            @ColorRes val backgroundColorRes: Int = when (isSelected) {
                true -> R.color.grey_lighten_30_translucent_50
                false -> R.color.transparent
            }
            itemView.setBackgroundColor(ContextCompat.getColor(itemView.context, backgroundColorRes))
        }

        fun bind(avatarUrl: String, @StringRes textRes: Int, isSelected: Boolean, imageManager: ImageManager) {
            bind(textRes, isSelected)

            val avatarSize = image.resources.getDimensionPixelSize(R.dimen.avatar_sz_small)
            val url = GravatarUtils.fixGravatarUrl(avatarUrl, avatarSize)
            imageManager.loadIntoCircle(image, AVATAR_WITH_BACKGROUND, url)

            val padding = DisplayUtils.dpToPx(image.context, 8)
            image.setPaddingRelative(padding, padding, padding, padding)
        }

        fun bind(@DrawableRes imageRes: Int, @StringRes textRes: Int, isSelected: Boolean, imageManager: ImageManager) {
            bind(textRes, isSelected)

            imageManager.load(image, imageRes)

            val padding = DisplayUtils.dpToPx(image.context, 12)
            image.setPaddingRelative(padding, padding, padding, padding)
        }
    }

    class NormalViewHolder(itemView: View) {
        private val image: AppCompatImageView = itemView.findViewById(R.id.post_list_author_selection_image)

        fun bind(avatarUrl: String, imageManager: ImageManager) {
            val avatarSize = image.resources.getDimensionPixelSize(R.dimen.avatar_sz_small)
            val url = GravatarUtils.fixGravatarUrl(avatarUrl, avatarSize)
            imageManager.loadIntoCircle(image, AVATAR_WITH_BACKGROUND, url)

            val padding = DisplayUtils.dpToPx(image.context, 8)
            image.setPaddingRelative(padding, padding, padding, padding)
        }

        fun bind(@DrawableRes imageRes: Int, imageManager: ImageManager) {
            imageManager.load(image, imageRes)

            val padding = DisplayUtils.dpToPx(image.context, 12)
            image.setPaddingRelative(padding, padding, padding, padding)
        }
    }
}
