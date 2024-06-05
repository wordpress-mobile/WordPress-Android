package org.wordpress.android.ui.reader.utils

import androidx.collection.SparseArrayCompat
import androidx.collection.forEach
import androidx.collection.isNotEmpty
import androidx.collection.set
import org.wordpress.android.R
import org.wordpress.android.models.ReaderTag
import org.wordpress.android.models.ReaderTagList
import org.wordpress.android.models.ReaderTagType
import org.wordpress.android.ui.compose.components.menu.dropdown.MenuElementData
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.util.config.ReaderTagsFeedFeatureConfig
import org.wordpress.android.util.extensions.indexOrNull
import javax.inject.Inject

class ReaderTopBarMenuHelper @Inject constructor(
    private val readerTagsFeedFeatureConfig: ReaderTagsFeedFeatureConfig
) {
    fun createMenu(readerTagsList: ReaderTagList): List<MenuElementData> {
        return mutableListOf<MenuElementData>().apply {
            readerTagsList.indexOrNull { it.isDiscover }?.let { discoverIndex ->
                add(createDiscoverItem(getMenuItemIdFromReaderTagIndex(discoverIndex)))
            }
            readerTagsList.indexOrNull { it.isFollowedSites }?.let { followingIndex ->
                add(createSubscriptionsItem(getMenuItemIdFromReaderTagIndex(followingIndex)))
            }
            readerTagsList.indexOrNull { it.isBookmarked }?.let { savedIndex ->
                add(createSavedItem(getMenuItemIdFromReaderTagIndex(savedIndex)))
            }
            readerTagsList.indexOrNull { it.isPostsILike }?.let { likedIndex ->
                add(createLikedItem(getMenuItemIdFromReaderTagIndex(likedIndex)))
            }
            readerTagsList.indexOrNull { it.isA8C }?.let { a8cIndex ->
                add(createAutomatticItem(getMenuItemIdFromReaderTagIndex(a8cIndex)))
            }
            readerTagsList.indexOrNull { it.isP2 }?.let { followedP2sIndex ->
                add(createFollowedP2sItem(
                    id = getMenuItemIdFromReaderTagIndex(followedP2sIndex),
                    text = readerTagsList[followedP2sIndex].tagTitle,
                ))
            }
            if (readerTagsFeedFeatureConfig.isEnabled()) {
                readerTagsList.indexOrNull { it.isTags }?.let { tagsIndex ->
                    add(createTagsItem(getMenuItemIdFromReaderTagIndex(tagsIndex)))
                }
            }
            readerTagsList
                .foldIndexed(SparseArrayCompat<ReaderTag>()) { index, sparseArray, readerTag ->
                    if (readerTag.tagType == ReaderTagType.CUSTOM_LIST) {
                        sparseArray[index] = readerTag
                    }
                    sparseArray
                }
                .takeIf { it.isNotEmpty() }
                ?.let { customListsArray ->
                    add(MenuElementData.Divider)
                    createCustomListsItems(customListsArray)
                }
        }
    }

    private fun MutableList<MenuElementData>.createCustomListsItems(
        customListsArray: SparseArrayCompat<ReaderTag>
    ) {
        if (customListsArray.size() > 2) {
            // If custom lists has more than 2 items, we add a submenu called "Lists"
            add(createCustomListsItem(customListsArray))
        } else {
            // If the custom lists has 2 or less items, we add the items directly without submenu
            customListsArray.forEach { index, readerTag ->
                add(
                    MenuElementData.Item.Single(
                        id = getMenuItemIdFromReaderTagIndex(index),
                        text = UiString.UiStringText(readerTag.tagTitle),
                    )
                )
            }
        }
    }

    private fun createDiscoverItem(id: String): MenuElementData.Item.Single {
        return MenuElementData.Item.Single(
            id = id,
            text = UiString.UiStringRes(R.string.reader_dropdown_menu_discover),
            leadingIcon = R.drawable.ic_reader_discover_24dp,
        )
    }

    private fun createSubscriptionsItem(id: String): MenuElementData.Item.Single {
        return MenuElementData.Item.Single(
            id = id,
            text = UiString.UiStringRes(R.string.reader_dropdown_menu_subscriptions),
            leadingIcon = R.drawable.ic_reader_subscriptions_24dp,
        )
    }

    private fun createSavedItem(id: String): MenuElementData.Item.Single {
        return MenuElementData.Item.Single(
            id = id,
            text = UiString.UiStringRes(R.string.reader_dropdown_menu_saved),
            leadingIcon = R.drawable.ic_reader_saved_24dp,
        )
    }

    private fun createLikedItem(id: String): MenuElementData.Item.Single {
        return MenuElementData.Item.Single(
            id = id,
            text = UiString.UiStringRes(R.string.reader_dropdown_menu_liked),
            leadingIcon = R.drawable.ic_reader_liked_24dp,
        )
    }

    private fun createAutomatticItem(id: String): MenuElementData.Item.Single {
        return MenuElementData.Item.Single(
            id = id,
            text = UiString.UiStringRes(R.string.reader_dropdown_menu_automattic),
        )
    }

    private fun createFollowedP2sItem(id: String, text: String): MenuElementData.Item.Single {
        return MenuElementData.Item.Single(
            id = id,
            text = UiString.UiStringText(text),
        )
    }

    private fun createTagsItem(id: String): MenuElementData.Item.Single {
        return MenuElementData.Item.Single(
            id = id,
            text = UiString.UiStringRes(R.string.reader_dropdown_menu_tags),
            leadingIcon = R.drawable.ic_reader_tags_24dp,
        )
    }

    private fun createCustomListsItem(customLists: SparseArrayCompat<ReaderTag>): MenuElementData.Item.SubMenu {
        val customListsMenuItems = mutableListOf<MenuElementData.Item.Single>()
        customLists.forEach { index, readerTag ->
            customListsMenuItems.add(
                MenuElementData.Item.Single(
                    id = getMenuItemIdFromReaderTagIndex(index),
                    text = UiString.UiStringText(readerTag.tagTitle),
                )
            )
        }
        return MenuElementData.Item.SubMenu(
            // We don't need this ID since this menu item just opens the sub-menu. It doesn't
            // change the content that is currently being displayed.
            id = "custom-lists",
            text = UiString.UiStringRes(R.string.reader_dropdown_menu_lists),
            children = customListsMenuItems,
        )
    }

    private fun getMenuItemIdFromReaderTagIndex(readerTagIndex: Int): String = "$readerTagIndex"

    fun getReaderTagIndexFromMenuItem(menuItem: MenuElementData.Item.Single) = menuItem.id.toInt()
}
