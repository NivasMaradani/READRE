package com.example.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CollectionsBookmark
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import com.example.ui.theme.CardBackground
import com.example.ui.theme.DividerColor
import com.example.ui.theme.PrimaryText
import com.example.ui.theme.SecondaryBackground
import com.example.ui.theme.SecondaryText

enum class AppTab(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
) {
    LIBRARY("Library", Icons.Filled.CollectionsBookmark, Icons.Outlined.AutoStories, "tab_library"),
    BOOKMARKS("Bookmarks", Icons.Filled.Bookmark, Icons.Outlined.BookmarkBorder, "tab_bookmarks"),
    CONTINUE_READING("Continue", Icons.Filled.History, Icons.Outlined.History, "tab_continue_reading")
}

@Composable
fun ReaderBottomBar(
    currentTab: AppTab,
    onTabSelected: (AppTab) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier,
        containerColor = SecondaryBackground,
        contentColor = PrimaryText
    ) {
        AppTab.values().forEach { tab ->
            val selected = currentTab == tab
            NavigationBarItem(
                selected = selected,
                onClick = { onTabSelected(tab) },
                icon = {
                    Icon(
                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                        contentDescription = tab.title
                    )
                },
                label = { Text(tab.title) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = PrimaryText,
                    selectedTextColor = PrimaryText,
                    indicatorColor = CardBackground,
                    unselectedIconColor = SecondaryText,
                    unselectedTextColor = SecondaryText
                ),
                modifier = Modifier.testTag(tab.testTag)
            )
        }
    }
}
