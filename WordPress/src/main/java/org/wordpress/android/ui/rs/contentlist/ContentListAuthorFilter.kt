package org.wordpress.android.ui.rs.contentlist

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import org.wordpress.android.R
import org.wordpress.android.ui.posts.AuthorFilterSelection

/** The "mine or everyone's" filter in an rs content list's app bar (WordPress.com sites only). */
@Composable
fun ContentListAuthorFilterButton(
    authorFilter: AuthorFilterSelection,
    avatarUrl: String?,
    onSelectionChanged: (AuthorFilterSelection) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val contentDesc = stringResource(R.string.post_list_toggle_author_filter)

    Box {
        IconButton(onClick = { expanded = true }) {
            AuthorFilterIcon(
                selection = authorFilter,
                avatarUrl = avatarUrl,
                contentDescription = contentDesc
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            AuthorFilterSelection.entries.forEach { selection ->
                val label = when (selection) {
                    AuthorFilterSelection.ME -> stringResource(R.string.me)
                    AuthorFilterSelection.EVERYONE -> stringResource(R.string.everyone)
                }
                DropdownMenuItem(
                    text = {
                        Text(
                            text = label,
                            color = if (selection == authorFilter) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                Color.Unspecified
                            }
                        )
                    },
                    leadingIcon = {
                        AuthorFilterIcon(
                            selection = selection,
                            avatarUrl = avatarUrl,
                            contentDescription = null
                        )
                    },
                    onClick = {
                        expanded = false
                        onSelectionChanged(selection)
                    }
                )
            }
        }
    }
}

@Composable
private fun AuthorFilterIcon(
    selection: AuthorFilterSelection,
    avatarUrl: String?,
    contentDescription: String?
) {
    val personIcon = if (selection == AuthorFilterSelection.ME) {
        Icons.Filled.Person
    } else {
        Icons.Outlined.Person
    }
    if (selection == AuthorFilterSelection.ME && !avatarUrl.isNullOrBlank()) {
        AsyncImage(
            model = avatarUrl,
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            fallback = rememberVectorPainter(personIcon),
            error = rememberVectorPainter(personIcon),
            modifier = Modifier
                .size(AVATAR_SIZE)
                .clip(CircleShape)
        )
    } else {
        Icon(
            personIcon,
            contentDescription = contentDescription
        )
    }
}

private val AVATAR_SIZE = 24.dp
