package com.xiemingxin.nandu.ui.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.ImageBitmap

/**
 * 从 app/src/main/assets/ 读取图片。
 * path 示例：images/ui/icons/weather_rain.webp
 * 图片缺失时不会崩溃，会显示占位色块。
 */
@Composable
fun AssetImage(
    path: String,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    contentScale: ContentScale = ContentScale.Crop,
    fallbackPath: String? = null,
    placeholderText: String = "缺图"
) {
    val context = LocalContext.current
    val bitmap = remember(path, fallbackPath) {
        loadAssetBitmap(context.assets, path) ?: fallbackPath?.let { loadAssetBitmap(context.assets, it) }
    }
    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = contentScale
        )
    } else {
        MissingAssetBox(modifier = modifier, text = placeholderText)
    }
}

@Composable
fun AssetIcon(
    path: String,
    modifier: Modifier = Modifier.size(22.dp),
    contentDescription: String? = null
) {
    AssetImage(
        path = path,
        modifier = modifier,
        contentDescription = contentDescription,
        contentScale = ContentScale.Fit,
        placeholderText = "图"
    )
}

private fun loadAssetBitmap(assetManager: android.content.res.AssetManager, path: String): ImageBitmap? {
    return try {
        assetManager.open(path).use { stream ->
            BitmapFactory.decodeStream(stream)?.asImageBitmap()
        }
    } catch (_: Throwable) {
        null
    }
}

@Composable
private fun MissingAssetBox(modifier: Modifier, text: String) {
    Box(
        modifier = modifier.background(Color(0xFF2A2117)),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = Color(0xFF8B7355), fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}
