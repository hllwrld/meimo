package com.stx.meimo.ui.component

import android.text.Html
import android.text.method.LinkMovementMethod
import android.widget.TextView
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun HtmlContent(
    html: String,
    modifier: Modifier = Modifier,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    maxLines: Int = Int.MAX_VALUE
) {
    val spanned = remember(html) {
        Html.fromHtml(html, Html.FROM_HTML_MODE_COMPACT)
    }
    val argbColor = textColor.toArgb()
    val textSize = MaterialTheme.typography.bodyMedium.fontSize.value

    AndroidView(
        factory = { context ->
            TextView(context).apply {
                movementMethod = LinkMovementMethod.getInstance()
                setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, textSize)
            }
        },
        update = { tv ->
            tv.text = spanned
            tv.setTextColor(argbColor)
            tv.maxLines = maxLines
        },
        modifier = modifier
    )
}
