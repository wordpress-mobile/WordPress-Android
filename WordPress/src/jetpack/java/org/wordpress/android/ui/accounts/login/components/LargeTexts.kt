package org.wordpress.android.ui.accounts.login.components

import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight

val texts = listOf(
        "Update a plugin",
        "Build a site",
        "Write a blog",
        "Watch your stats",
        "Check notifications",
        "Respond to comments",
        "Restore a backup",
        "Search for plugins",
        "Share on Facebook",
        "Fix a security issue",
        "Post a photo",
        "Add an author",
        "Update a plugin",
        "Build a site",
        "Write a blog",
        "Watch your stats",
        "Check notifications",
        "Respond to comments",
        "Restore a backup",
        "Search for plugins",
        "Share on Facebook",
        "Fix a security issue",
        "Post a photo",
        "Add an author",
)

@Composable
fun LargeText(text: String) {
    Text(
            text = text,
            style = MaterialTheme.typography.h3,
            fontWeight = FontWeight.SemiBold,
            color = Color(color = 0xFF64CA43)
    )
}

@Composable
fun LargeTexts(
    items: List<String> = texts,
    modifier: Modifier = Modifier,
) {
    AutoScrollingLazyColumn(
            items = items,
            modifier = modifier,
    ) {
        LargeText(it)
    }
}
