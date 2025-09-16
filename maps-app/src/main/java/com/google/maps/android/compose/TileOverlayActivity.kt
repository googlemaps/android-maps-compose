package com.google.maps.android.compose

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
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
        repeat(5) {
            delay(1000)
            renderedIndex += 1
            state.clearTileCache()
        }

        // update the tile provider
        tileProviderIndex += 1

        while (true) {
            delay(1000)
            renderedIndex += 1
            state.clearTileCache()
        }
    }
}

private fun renderTiles(index: Int, size: Int): ByteArray {
    val bitmap = createBitmap(size, size)

    val canvas = Canvas(bitmap)
    canvas.drawText(index.toString(), (size / 2).toFloat(), (size / 2).toFloat(), Paint().apply {
        color = Color.Black.toArgb()
        textSize = 100f
    })
    val outputStream = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.WEBP_LOSSLESS, 0, outputStream)
    return outputStream.toByteArray()
}
