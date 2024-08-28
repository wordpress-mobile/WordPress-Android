package org.wordpress.android.ui.reader.utils

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.R
import org.wordpress.android.models.ReaderTag
import org.wordpress.android.models.ReaderTagList
import org.wordpress.android.models.ReaderTagType
import org.wordpress.android.ui.compose.components.menu.dropdown.MenuElementData
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.ui.utils.UiString.UiStringText
import org.wordpress.android.util.config.ReaderTagsFeedFeatureConfig

class ReaderTopBarMenuHelperTest {
    private val readerTagsFeedFeatureConfig: ReaderTagsFeedFeatureConfig = mock()
    val helper = ReaderTopBarMenuHelper(
        readerTagsFeedFeatureConfig = readerTagsFeedFeatureConfig
    )

    @Test
    fun `GIVEN all tags are available and tags FF disabled WHEN createMenu THEN all items are created correctly`() {
        whenever(readerTagsFeedFeatureConfig.isEnabled()).thenReturn(false)

        val tags = ReaderTagList().apply {
            add(mockFollowingTag()) // item 0
            add(mockDiscoverTag()) // item 1
            add(mockSavedTag()) // item 2
            add(mockLikedTag()) // item 3
            add(mockA8CTag()) // item 4
            add(mockFollowedP2sTag()) // item 5
            add(createCustomListTag("custom-list-1")) // item 6
            add(createCustomListTag("custom-list-2")) // item 7
            add(createCustomListTag("custom-list-3")) // item 8
        }

        val menu = helper.createMenu(tags)

        // compare the menu items one by one to check their indices
        val discoverItem = menu.findSingleItem { it.id == "1" }!!
        assertThat(discoverItem.text).isEqualTo(UiStringRes(R.string.reader_dropdown_menu_discover))

        val subscriptionsItem = menu.findSingleItem { it.id == "0" }!!
        assertThat(subscriptionsItem.text).isEqualTo(UiStringRes(R.string.reader_dropdown_menu_subscriptions))

        val savedItem = menu.findSingleItem { it.id == "2" }!!
        assertThat(savedItem.text).isEqualTo(UiStringRes(R.string.reader_dropdown_menu_saved))

        val likedItem = menu.findSingleItem { it.id == "3" }!!
        assertThat(likedItem.text).isEqualTo(UiStringRes(R.string.reader_dropdown_menu_liked))

        val a8cItem = menu.findSingleItem { it.id == "4" }!!
        assertThat(a8cItem.text).isEqualTo(UiStringRes(R.string.reader_dropdown_menu_automattic))

        val followedP2sItem = menu.findSingleItem { it.id == "5" }!!
        assertThat(followedP2sItem.text).isEqualTo(UiStringText("Followed P2s"))

        assertThat(menu).contains(MenuElementData.Divider)

        val customListsItem = menu.findSubMenu()!!
        assertThat(customListsItem.text).isEqualTo(UiStringRes(R.string.reader_dropdown_menu_lists))

        val customList1Item = customListsItem.children.findSingleItem { it.id == "6" }!!
        assertThat(customList1Item.text).isEqualTo(UiStringText("custom-list-1"))

        val customList2Item = customListsItem.children.findSingleItem { it.id == "7" }!!
        assertThat(customList2Item.text).isEqualTo(UiStringText("custom-list-2"))

        val customList3Item = customListsItem.children.findSingleItem { it.id == "8" }!!
        assertThat(customList3Item.text).isEqualTo(UiStringText("custom-list-3"))
    }

    @Test
    fun `GIVEN custom lists has 2 items or less WHEN createMenu THEN custom lists items are shown outside a submenu`() {
        val tags = ReaderTagList().apply {
            add(mockFollowingTag()) // item 0
            add(mockDiscoverTag()) // item 1
            add(mockSavedTag()) // item 2
            add(mockLikedTag()) // item 3
            add(mockA8CTag()) // item 4
            add(mockFollowedP2sTag()) // item 5
            add(createCustomListTag("custom-list-1")) // item 6
            add(createCustomListTag("custom-list-2")) // item 7
        }
        val menu = helper.createMenu(tags)

        val customListItem1 = menu.findSingleItem { it.id == "6" }!!
        assertThat(customListItem1.text).isEqualTo(UiStringText("custom-list-1"))

        val customListItem2 = menu.findSingleItem { it.id == "7" }!!
        assertThat(customListItem2.text).isEqualTo(UiStringText("custom-list-2"))
    }

    @Test
    fun `GIVEN all tags are available and tags FF enabled WHEN createMenu THEN all items are created correctly`() {
        whenever(readerTagsFeedFeatureConfig.isEnabled()).thenReturn(true)

        val tags = ReaderTagList().apply {
            add(mockFollowingTag()) // item 0
            add(mockDiscoverTag()) // item 1
            add(mockSavedTag()) // item 2
            add(mockLikedTag()) // item 3
            add(mockTagsTag()) // item 4
            add(mockA8CTag()) // item 5
            add(mockFollowedP2sTag()) // item 6
            add(createCustomListTag("custom-list-1")) // item 7
            add(createCustomListTag("custom-list-2")) // item 8
            add(createCustomListTag("custom-list-3")) // item 9
        }

        val menu = helper.createMenu(tags)

        // compare the menu items one by one to check their indices
        val discoverItem = menu.findSingleItem { it.id == "1" }!!
        assertThat(discoverItem.text).isEqualTo(UiStringRes(R.string.reader_dropdown_menu_discover))

        val subscriptionsItem = menu.findSingleItem { it.id == "0" }!!
        assertThat(subscriptionsItem.text).isEqualTo(UiStringRes(R.string.reader_dropdown_menu_subscriptions))

        val savedItem = menu.findSingleItem { it.id == "2" }!!
        assertThat(savedItem.text).isEqualTo(UiStringRes(R.string.reader_dropdown_menu_saved))

        val likedItem = menu.findSingleItem { it.id == "3" }!!
        assertThat(likedItem.text).isEqualTo(UiStringRes(R.string.reader_dropdown_menu_liked))

        val tagsItem = menu.findSingleItem { it.id == "4" }!!
        assertThat(tagsItem.text).isEqualTo(UiStringRes(R.string.reader_dropdown_menu_tags))

        val a8cItem = menu.findSingleItem { it.id == "5" }!!
        assertThat(a8cItem.text).isEqualTo(UiStringRes(R.string.reader_dropdown_menu_automattic))

        val followedP2sItem = menu.findSingleItem { it.id == "6" }!!
        assertThat(followedP2sItem.text).isEqualTo(UiStringText("Followed P2s"))

        assertThat(menu).contains(MenuElementData.Divider)

        val customListsItem = menu.findSubMenu()!!
        assertThat(customListsItem.text).isEqualTo(UiStringRes(R.string.reader_dropdown_menu_lists))

        val customList1Item = customListsItem.children.findSingleItem { it.id == "7" }!!
        assertThat(customList1Item.text).isEqualTo(UiStringText("custom-list-1"))

        val customList2Item = customListsItem.children.findSingleItem { it.id == "8" }!!
        assertThat(customList2Item.text).isEqualTo(UiStringText("custom-list-2"))

        val customList3Item = customListsItem.children.findSingleItem { it.id == "9" }!!
        assertThat(customList3Item.text).isEqualTo(UiStringText("custom-list-3"))
    }

    @Test
    fun `GIVEN discover not present WHEN createMenu THEN discover menu item not created`() {
        val tags = ReaderTagList().apply {
            add(mockFollowingTag()) // item 0
            add(mockSavedTag()) // item 1
            add(mockLikedTag()) // item 2
            add(mockA8CTag()) // item 3
            add(createCustomListTag("custom-list-1")) // item 4
            add(createCustomListTag("custom-list-2")) // item 5
            add(createCustomListTag("custom-list-3")) // item 6
        }

        val menu = helper.createMenu(tags)

        val discoverItem = menu
            .findSingleItem { it.text == UiStringRes(R.string.reader_dropdown_menu_discover) }
        assertThat(discoverItem).isNull()
    }

    @Test
    fun `GIVEN subscriptions not present WHEN createMenu THEN subscriptions menu item not created`() {
        val tags = ReaderTagList().apply {
            add(mockDiscoverTag()) // item 0
            add(mockSavedTag()) // item 1
            add(mockLikedTag()) // item 2
            add(mockA8CTag()) // item 3
            add(createCustomListTag("custom-list-1")) // item 4
            add(createCustomListTag("custom-list-2")) // item 5
            add(createCustomListTag("custom-list-3")) // item 6
        }

        val menu = helper.createMenu(tags)

        val subscriptionsItem = menu
            .findSingleItem { it.text == UiStringRes(R.string.reader_dropdown_menu_subscriptions) }
        assertThat(subscriptionsItem).isNull()
    }

    @Test
    fun `GIVEN saved not present WHEN createMenu THEN saved menu item not created`() {
        val tags = ReaderTagList().apply {
            add(mockDiscoverTag()) // item 0
            add(mockFollowingTag()) // item 1
            add(mockLikedTag()) // item 2
            add(mockA8CTag()) // item 3
            add(createCustomListTag("custom-list-1")) // item 4
            add(createCustomListTag("custom-list-2")) // item 5
            add(createCustomListTag("custom-list-3")) // item 6
        }

        val menu = helper.createMenu(tags)

        val savedItem = menu
            .findSingleItem { it.text == UiStringRes(R.string.reader_dropdown_menu_saved) }
        assertThat(savedItem).isNull()
    }

    @Test
    fun `GIVEN liked not present WHEN createMenu THEN liked menu item not created`() {
        val tags = ReaderTagList().apply {
            add(mockDiscoverTag()) // item 0
            add(mockFollowingTag()) // item 1
            add(mockSavedTag()) // item 2
            add(mockA8CTag()) // item 3
            add(createCustomListTag("custom-list-1")) // item 4
            add(createCustomListTag("custom-list-2")) // item 5
            add(createCustomListTag("custom-list-3")) // item 6
        }

        val menu = helper.createMenu(tags)

        val likedItem = menu
            .findSingleItem { it.text == UiStringRes(R.string.reader_dropdown_menu_liked) }
        assertThat(likedItem).isNull()
    }

    @Test
    fun `GIVEN a8c not present WHEN createMenu THEN a8c menu item not created`() {
        val tags = ReaderTagList().apply {
            add(mockDiscoverTag()) // item 0
            add(mockFollowingTag()) // item 1
            add(mockSavedTag()) // item 2
            add(mockLikedTag()) // item 3
            add(createCustomListTag("custom-list-1")) // item 4
            add(createCustomListTag("custom-list-2")) // item 5
            add(createCustomListTag("custom-list-3")) // item 6
        }

        val menu = helper.createMenu(tags)

        val a8cItem = menu
            .findSingleItem { it.text == UiStringRes(R.string.reader_dropdown_menu_automattic) }
        assertThat(a8cItem).isNull()
    }

    @Test
    fun `GIVEN custom lists not present WHEN createMenu THEN custom lists menu item not created`() {
        val tags = ReaderTagList().apply {
            add(mockDiscoverTag()) // item 0
            add(mockFollowingTag()) // item 1
            add(mockSavedTag()) // item 2
            add(mockLikedTag()) // item 3
            add(mockA8CTag()) // item 4
        }

        val menu = helper.createMenu(tags)

        val customListsItem = menu.findSubMenu()
        assertThat(menu).doesNotContain(MenuElementData.Divider)
        assertThat(customListsItem).isNull()
    }

    @Test
    fun `GIVEN single menu item built with helper WHEN getReaderTagIndexFromMenuItem then return correct index`() {
        val tags = ReaderTagList().apply {
            add(mockFollowingTag()) // item 0
            add(mockDiscoverTag()) // item 1
            add(mockSavedTag()) // item 2
            add(mockLikedTag()) // item 3
            add(mockA8CTag()) // item 4
            add(createCustomListTag("custom-list-1")) // item 5
            add(createCustomListTag("custom-list-2")) // item 6
            add(createCustomListTag("custom-list-3")) // item 7
        }

        val menu = helper.createMenu(tags)

        val discoverItem = menu
            .findSingleItem { it.text == UiStringRes(R.string.reader_dropdown_menu_discover) }!!
        assertThat(helper.getReaderTagIndexFromMenuItem(discoverItem)).isEqualTo(1)

        val subscriptionsItem = menu
            .findSingleItem { it.text == UiStringRes(R.string.reader_dropdown_menu_subscriptions) }!!
        assertThat(helper.getReaderTagIndexFromMenuItem(subscriptionsItem)).isEqualTo(0)

        val savedItem = menu
            .findSingleItem { it.text == UiStringRes(R.string.reader_dropdown_menu_saved) }!!
        assertThat(helper.getReaderTagIndexFromMenuItem(savedItem)).isEqualTo(2)

        val likedItem = menu
            .findSingleItem { it.text == UiStringRes(R.string.reader_dropdown_menu_liked) }!!
        assertThat(helper.getReaderTagIndexFromMenuItem(likedItem)).isEqualTo(3)

        val a8cItem = menu
            .findSingleItem { it.text == UiStringRes(R.string.reader_dropdown_menu_automattic) }!!
        assertThat(helper.getReaderTagIndexFromMenuItem(a8cItem)).isEqualTo(4)

        val customList1Item = menu.findSubMenu()!!
            .children
            .findSingleItem { it.text == UiStringText("custom-list-1") }!!
        assertThat(helper.getReaderTagIndexFromMenuItem(customList1Item)).isEqualTo(5)

        val customList2Item = menu.findSubMenu()!!
            .children
            .findSingleItem { it.text == UiStringText("custom-list-2") }!!
        assertThat(helper.getReaderTagIndexFromMenuItem(customList2Item)).isEqualTo(6)

        val customList3Item = menu.findSubMenu()!!
            .children
            .findSingleItem { it.text == UiStringText("custom-list-3") }!!
        assertThat(helper.getReaderTagIndexFromMenuItem(customList3Item)).isEqualTo(7)
    }

    private fun List<MenuElementData>.findSingleItem(
        predicate: (MenuElementData.Item.Single) -> Boolean
    ): MenuElementData.Item.Single? {
        return find { it is MenuElementData.Item.Single && predicate(it) } as? MenuElementData.Item.Single
    }

    private fun List<MenuElementData>.findSubMenu(): MenuElementData.Item.SubMenu? {
        return find { it is MenuElementData.Item.SubMenu } as? MenuElementData.Item.SubMenu
    }

    private fun mockDiscoverTag(): ReaderTag {
        return mock {
            on { isDiscover } doReturn true
        }
    }

    private fun mockFollowingTag(): ReaderTag {
        return mock {
            on { isFollowedSites } doReturn true
        }
    }

    private fun mockSavedTag(): ReaderTag {
        return mock {
            on { isBookmarked } doReturn true
        }
    }

    private fun mockLikedTag(): ReaderTag {
        return mock {
            on { isPostsILike } doReturn true
        }
    }

    private fun mockA8CTag(): ReaderTag {
        return mock {
            on { isA8C } doReturn true
        }
    }

    private fun mockFollowedP2sTag(): ReaderTag {
        return mock {
            on { isP2 } doReturn true
            on { tagTitle } doReturn "Followed P2s"
        }
    }

    private fun mockTagsTag(): ReaderTag {
        return mock {
            on { isTags } doReturn true
        }
    }

    private fun createCustomListTag(title: String): ReaderTag {
        return ReaderTag(
            title,
            title,
            title,
            "http://endpoint.com",
            ReaderTagType.CUSTOM_LIST
        )
    }
}
