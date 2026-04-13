package com.stx.meimo.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.stx.meimo.util.resolveImageUrl

val LocalHideImages = compositionLocalOf { false }

@Composable
fun MeimoImage(
    path: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    if (LocalHideImages.current) {
        Box(modifier = modifier.background(Color(0xFFBDBDBD)))
    } else {
        val url = resolveImageUrl(path)
        AsyncImage(
            model = url,
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = contentScale
        )
    }
}
