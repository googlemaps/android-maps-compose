package com.google.maps.android.compose

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.core.graphics.createBitmap
import com.google.android.gms.maps.model.Tile
import com.google.android.gms.maps.model.TileProvider
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.delay

/**
 * This activity demonstrates how to use Tile Overlays with Jetpack Compose.
 */
class TileOverlayActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Content()
        }
    }
}

@Composable
private fun Content() {
    GoogleMap(
        modifier = Modifier.fillMaxSize(),
    ) {
        UpdatedTileOverlay()
    }
}

/**
 * This composable demonstrates how to use a [TileOverlay] with a [TileProvider] that
 * updates its content periodically.
 */
@Composable
private fun UpdatedTileOverlay() {
    var tileProviderIndex by remember { mutableIntStateOf(0) }
    var renderedIndex by remember { mutableIntStateOf(0) }
    val state = rememberTileOverlayState()

    val size = with(LocalDensity.current) { 256.dp.toPx() }.toInt()
    val tileProvider = remember(tileProviderIndex) {
        TileProvider { _, _, _ ->
            Tile(size, size, renderTiles(renderedIndex, size))
        }
    }

    TileOverlay(tileProvider = tileProvider, state = state, fadeIn = false)

    LaunchedEffect(Unit) {
        // This LaunchedEffect demonstrates two ways to update a tile overlay.

        // 1. Invalidate the cache to redraw tiles with new data.
        // Here, we're calling `state.clearTileCache()` every second for 5 seconds.
        // This tells the map to request new tiles from the *existing* TileProvider,
        // which will then re-render them using the latest `renderedIndex`.
        repeat(5) {
            delay(1000)
            renderedIndex += 1
            state.clearTileCache()
        }

        // 2. Update the TileProvider instance itself.
        // After 5 seconds, we update `tileProviderIndex`. Because this is a key
        // to the `remember` block for our TileProvider, Compose will discard the
        // old provider and create a new one.
        tileProviderIndex += 1

        // Now, we continue invalidating the cache to demonstrate that the *new*
        // TileProvider is the one responding to the `clearTileCache` calls.
        while (true) {
            delay(1000)
            renderedIndex += 1
            state.clearTileCache()
        }
    }
}

/**
 * Helper function to dynamically generate a tile image.
 * The [TileProvider] interface requires that a [ByteArray] is returned for each tile.
 * This function creates a [Bitmap], draws the current [index] on it, and then compresses
 * it into a [ByteArray] to be returned by the provider.
 */
private fun renderTiles(index: Int, size: Int): ByteArray {
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        color = Color.Black.toArgb()
        textSize = 100f
    }
    val bitmap = createBitmap(size, size).also {
        Canvas(it).drawText(index.toString(), size / 2f, size / 2f, paint)
    }

    val format = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        Bitmap.CompressFormat.WEBP_LOSSLESS
    } else {
        Bitmap.CompressFormat.PNG
    }

    return ByteArrayOutputStream().use { stream ->
        bitmap.compress(format, 0, stream)
        stream.toByteArray()
    }
}
