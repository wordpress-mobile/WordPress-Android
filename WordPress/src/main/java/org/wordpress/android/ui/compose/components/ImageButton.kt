package org.wordpress.android.ui.compose.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester.Companion.createRefs
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import org.wordpress.android.R
import org.wordpress.android.ui.utils.UiString

@Preview
@Composable
fun PreviewDrawButton() {
    ImageButton(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(
                Color.Gray,
                shape = RoundedCornerShape(6.dp)
            ),
        drawableLeft = Drawable(R.drawable.ic_story_icon_24dp),
        drawableRight = Drawable(R.drawable.ic_story_icon_24dp),
        drawableTop = Drawable(R.drawable.ic_story_icon_24dp),
        drawableBottom = Drawable(R.drawable.ic_story_icon_24dp),
        button = Button(text = UiString.UiStringText("Button Text")),
        onClick = {}
    )
}


@Composable
fun ImageButton(
    modifier: Modifier = Modifier,
    drawableLeft: Drawable? = null,
    drawableRight: Drawable? = null,
    drawableTop: Drawable? = null,
    drawableBottom: Drawable? = null,
    button: Button,
    onClick: () -> Unit
) {
    ConstraintLayout(modifier = modifier) {
        val (buttonTextRef) = createRefs()
        Box(modifier = Modifier
            .constrainAs(buttonTextRef) {
                top.linkTo(parent.top, drawableTop?.iconSize ?: 0.dp)
                bottom.linkTo(parent.bottom, drawableBottom?.iconSize ?: 0.dp)
                start.linkTo(parent.start, drawableLeft?.iconSize ?: 0.dp)
                end.linkTo(parent.end, drawableRight?.iconSize ?: 0.dp)
                width = Dimension.wrapContent
            }
            .clickable { onClick.invoke() }
        ) {
            val buttonTextValue: String = when (button.text) {
                is UiString.UiStringText -> button.text.toString()
                is UiString.UiStringRes -> stringResource(id = button.text.stringRes)
                is UiString.UiStringResWithParams -> stringResource(id = button.text.stringRes)
                else -> ""
            }

            Text(
                text = buttonTextValue,
                fontSize = button.fontSize,
                fontWeight = button.fontWeight,
                color = button.color,
            )
        }

        drawableLeft?.let { drawable ->
            val (imageLeft) = createRefs()
            Image(
                modifier = Modifier.constrainAs(imageLeft) {
                    top.linkTo(parent.top)
                    bottom.linkTo(parent.bottom)
                    start.linkTo(parent.start)
                },
                painter = painterResource(id = drawable.resId),
                contentDescription = null
            )
        }

        drawableRight?.let { drawable ->
            val (imageRight) = createRefs()
            Image(
                modifier = Modifier.constrainAs(imageRight) {
                    top.linkTo(parent.top)
                    bottom.linkTo(parent.bottom)
                    start.linkTo(buttonTextRef.end, margin = drawable.padding)
                }.size(drawable.iconSize),
                painter = painterResource(id = drawable.resId),
                contentDescription = null
            )
        }

        drawableTop?.let { drawable ->
            val (imageTop) = createRefs()
            Image(
                modifier = Modifier.constrainAs(imageTop) {
                    top.linkTo(parent.top)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                },
                painter = painterResource(id = drawable.resId),
                contentDescription = null
            )
        }

        drawableBottom?.let { drawable ->
            val (imageBottom) = createRefs()
            Image(
                modifier = Modifier.constrainAs(imageBottom) {
                    bottom.linkTo(parent.bottom)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                },
                painter = painterResource(id = drawable.resId),
                contentDescription = null
            )
        }
    }
}

/**
 * @param resId The drawable resource id to be displayed.
 * @param iconSize The icon size for the drawable, and if no value is provided the default size is 24 dp.
 * @param padding The padding between the drawable and the button text.
 */
data class Drawable(val resId: Int, val iconSize: Dp = 24.dp, val padding: Dp = 12.dp)

/**
 * @param text The text to be displayed.
 * @param modifier [Modifier] to apply to this layout.
 * @param color [Color] to apply to the text. If [Color.Unspecified], and [style] has no color set,
 * this will be [LocalContentColor].
 * @param fontSize The size of glyphs to use when painting the text. See [TextStyle.fontSize].
 * @param fontStyle The typeface variant to use when drawing the letters (e.g., italic).
 * See [TextStyle.fontStyle].
 * @param fontWeight The typeface thickness to use when painting the text (e.g., [FontWeight.Bold]).
 * @param fontFamily The font family to be used when rendering the text. See [TextStyle.fontFamily].
 * @param textAlign The alignment of the text within the lines of the paragraph.
 * See [TextStyle.textAlign].
 * @param style Style configuration for the text such as color, font, line height etc.
 */
data class Button(
    val text: UiString,
    val modifier: Modifier = Modifier,
    val color: Color = Color.Unspecified,
    val fontSize: TextUnit = TextUnit.Unspecified,
    val fontStyle: FontStyle? = null,
    val fontWeight: FontWeight? = null,
    val fontFamily: FontFamily? = null,
    val textAlign: TextAlign? = null,
    val style: TextStyle? = null,
)

