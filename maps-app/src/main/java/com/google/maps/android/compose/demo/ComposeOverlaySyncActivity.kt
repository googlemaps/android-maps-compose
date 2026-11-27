/*
 * Copyright 2026 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.maps.android.compose.demo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.MarkerComposable
import com.google.maps.android.compose.demo.theme.MapsComposeSampleTheme
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberUpdatedMarkerState
import kotlinx.coroutines.flow.drop
import kotlin.math.roundToInt

/**
 * Demonstrates the synchronization gap and frame-pacing behavior when rendering interactive
 * Jetpack Compose UI overlays on top of a [GoogleMap], particularly on high-refresh-rate
 * (120 Hz) and large-screen / foldable devices.
 *
 * ## Why Compose Overlays Drift Behind the Map
 *
 * When a user pans or flings a [GoogleMap], two independent rendering pipelines execute on
 * separate threads:
 *
 * 1. **Maps SDK Render Thread (GL/Vulkan):** Processes gestures, advances the camera, and renders
 *    map tiles directly on the GPU at the display's full refresh rate (e.g., 120 Hz / 8.33 ms per
 *    frame).
 * 2. **Jetpack Compose UI Thread (Android Main Thread):** Has no direct access to the map's render
 *    loop. It relies on asynchronous `OnCameraMove` callbacks forwarded into
 *    [CameraPositionState.position], then projects coordinates via
 *    `cameraPositionState.projection?.toScreenLocation(...)` and submits a separate HWUI frame.
 *
 * Two effects compound to create visible separation ("drift") between a Compose overlay and its
 * geographic anchor on the map:
 *
 * - **Asynchronous pipeline latency:** By the time `OnCameraMove` reaches the UI thread and Compose
 *   lays out and draws the overlay, the map render thread has already advanced to a newer camera
 *   state. On screen, the map shows time `T` while the Compose overlay reflects `T - 1` or `T - 2`.
 * - **120 Hz vs. ~60 Hz callback cadence:** On 120 Hz displays, `OnCameraMove` callbacks typically
 *   arrive in Java/Kotlin at ~60 Hz (~16.67 ms intervals). On every other display refresh, the map
 *   moves while Compose receives no camera update, producing visible double-stepping.
 *
 * ## Limitations of Existing Workarounds
 *
 * - **Bitmap markers (`BitmapDescriptorFactory.fromBitmap` / [MarkerComposable]):** Rendered inside
 *   the map's GL/Vulkan engine (zero drift), but flattening animated or interactive Compose UI to
 *   bitmaps every frame incurs high CPU allocation and GPU texture upload overhead.
 * - **Advanced Markers:** Native to the Maps SDK, but cannot host arbitrary animated Compose UI and
 *   can still exhibit slight anchor float during active panning.
 *
 * ## Foldable / Large-Screen GPU Tail Scaling
 *
 * On large unfolded displays running at 120 Hz (8.33 ms frame budget), median GPU time stays low
 * (2-3 ms), but p95 GPU frame time during panning can spike above 8.33 ms and scale non-linearly
 * with visible display area. Under the `LATEST` renderer, the map renders into a `SurfaceTexture`
 * composited inside the app process (TextureView-style composition) rather than a dedicated
 * `SurfaceView` hardware layer, which can add full-screen composition overhead on large viewports.
 *
 * ## Modes in This Demo
 *
 * 1. [OverlaySyncMode.COMPOSE_OVERLAY]: Draws interactive Jetpack Compose price pills projected
 *    via `cameraPositionState.projection?.toScreenLocation(...)` directly above native map-rendered
 *    ground-truth circles at the exact same [LatLng] coordinates, with a live HUD comparing display
 *    refresh rate, Compose frame rate, and `OnCameraMove` callback rate.
 * 2. [OverlaySyncMode.BITMAP_MARKER]: Renders the same Compose pills through [MarkerComposable]
 *    so they move inside the map engine with zero drift, illustrating the static-bitmap tradeoff.
 * 3. [OverlaySyncMode.CLEAN_MAP]: Renders a full-screen empty [GoogleMap] with zero markers or
 *    overlays (plus a **Hide HUD** button) for clean `adb shell dumpsys gfxinfo` GPU profiling.
 */
class ComposeOverlaySyncActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MapsComposeSampleTheme {
                ComposeOverlaySyncScreen()
            }
        }
    }
}

/**
 * Display modes for [ComposeOverlaySyncActivity].
 */
internal enum class OverlaySyncMode(val label: String) {
    COMPOSE_OVERLAY("Compose Overlay"),
    BITMAP_MARKER("MarkerComposable"),
    CLEAN_MAP("Clean Map"),
}

/**
 * A sample coordinate paired with a price label used to compare native map rendering against
 * Compose screen-space overlays.
 */
internal data class OverlayDemoPin(
    val id: String,
    val label: String,
    val position: LatLng,
)

/**
 * Five pins distributed around Singapore so all five appear in the initial viewport and move
 * across the screen together during horizontal or vertical panning gestures.
 */
private val demoPins = listOf(
    OverlayDemoPin("pin-1", "$189", LatLng(1.3640, 103.8540)),
    OverlayDemoPin("pin-2", "$245", LatLng(1.3588, 103.8742)),
    OverlayDemoPin("pin-3", "$310", LatLng(1.3520, 103.8930)),
    OverlayDemoPin("pin-4", "$165", LatLng(1.3450, 103.8640)),
    OverlayDemoPin("pin-5", "$420", LatLng(1.3710, 103.8820)),
)

private val initialOverlaySyncCameraPosition = CameraPosition.fromLatLngZoom(
    LatLng(1.3588, 103.8742),
    12.5f,
)

private val GroundTruthGreen = Color(0xFF16A34A)
private val SelectedPillColor = Color(0xFF111827)
private val DefaultPillColor = Color(0xFFFFFFFF)
private val DefaultPillBorder = Color(0xFFD1D5DB)

/**
 * Tracks the rolling event frequency (in Hz) over a sliding nanosecond window.
 *
 * Used to compare the rate of Compose UI frames (`withFrameNanos`) against the rate of
 * `OnCameraMove` callbacks (`CameraPositionState.position` updates) while the user pans the map.
 */
internal class RollingRateMeter(
    private val windowNanos: Long = 500_000_000L,
) {
    private val timestamps = ArrayDeque<Long>()

    /**
     * Records a new sample at [timestampNanos] and returns the current rolling frequency in Hz.
     * Returns `0f` until at least two samples with a non-zero span exist in the window.
     */
    fun record(timestampNanos: Long): Float {
        timestamps.addLast(timestampNanos)
        val cutoff = timestampNanos - windowNanos
        while (timestamps.size > 1 && timestamps.first() < cutoff) {
            timestamps.removeFirst()
        }
        if (timestamps.size < 2) return 0f
        val spanNanos = timestamps.last() - timestamps.first()
        if (spanNanos <= 0L) return 0f
        return ((timestamps.size - 1) * 1_000_000_000.0 / spanNanos).toFloat()
    }

    /** Clears all recorded timestamps. */
    fun reset() {
        timestamps.clear()
    }
}

@Composable
internal fun ComposeOverlaySyncScreen(
    modifier: Modifier = Modifier,
) {
    val cameraPositionState = rememberCameraPositionState {
        position = initialOverlaySyncCameraPosition
    }
    var mode by remember { mutableStateOf(OverlaySyncMode.COMPOSE_OVERLAY) }
    var selectedPinId by remember { mutableStateOf<String?>(null) }
    var hudExpanded by remember { mutableStateOf(true) }
    var mapSize by remember { mutableStateOf(IntSize.Zero) }

    // Live telemetry meters updated only while panning so the activity remains completely idle
    // when stationary (avoiding unnecessary recomposition and keeping UI tests deterministic).
    val cameraMeter = remember { RollingRateMeter() }
    val frameMeter = remember { RollingRateMeter() }
    var cameraCallbackHz by remember { mutableFloatStateOf(0f) }
    var composeFrameHz by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(cameraPositionState) {
        snapshotFlow { cameraPositionState.position }
            .drop(1)
            .collect {
                cameraCallbackHz = cameraMeter.record(System.nanoTime())
            }
    }

    LaunchedEffect(cameraPositionState.isMoving) {
        if (cameraPositionState.isMoving) {
            frameMeter.reset()
            while (true) {
                withFrameNanos { frameTimeNanos ->
                    composeFrameHz = frameMeter.record(frameTimeNanos)
                }
            }
        } else {
            cameraMeter.reset()
            frameMeter.reset()
        }
    }

    val view = LocalView.current
    val configuration = LocalConfiguration.current
    val displayRefreshHz = view.display?.refreshRate ?: 60f

    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { mapSize = it },
    ) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            uiSettings = remember {
                MapUiSettings(
                    zoomControlsEnabled = false,
                    myLocationButtonEnabled = false,
                )
            },
        ) {
            when (mode) {
                OverlaySyncMode.COMPOSE_OVERLAY -> {
                    // Render ground-truth anchor circles directly inside the Maps SDK GL/Vulkan
                    // engine so any drift of the Compose pills above them is immediately visible.
                    demoPins.forEach { pin ->
                        Circle(
                            center = pin.position,
                            radius = 120.0,
                            fillColor = GroundTruthGreen.copy(alpha = 0.85f),
                            strokeColor = Color.White,
                            strokeWidth = 4f,
                            zIndex = 1f,
                        )
                    }
                }

                OverlaySyncMode.BITMAP_MARKER -> {
                    demoPins.forEach { pin ->
                        val isSelected = selectedPinId == pin.id
                        val markerState = rememberUpdatedMarkerState(position = pin.position)
                        MarkerComposable(
                            keys = arrayOf<Any>(pin.id, isSelected),
                            state = markerState,
                            anchor = Offset(0.5f, 0.5f),
                            onClick = {
                                selectedPinId = if (isSelected) null else pin.id
                                true
                            },
                        ) {
                            PricePill(
                                label = pin.label,
                                selected = isSelected,
                                showAnchorDot = false,
                            )
                        }
                    }
                }

                OverlaySyncMode.CLEAN_MAP -> {
                    // Intentionally empty: zero markers or overlays for clean GPU baseline profiling.
                }
            }
        }

        if (mode == OverlaySyncMode.COMPOSE_OVERLAY) {
            ProjectedComposeOverlays(
                cameraPositionState = cameraPositionState,
                pins = demoPins,
                selectedPinId = selectedPinId,
                onPinClick = { pinId ->
                    selectedPinId = if (selectedPinId == pinId) null else pinId
                },
            )
        }

        // Top HUD with mode selector and live 120 Hz vs. camera callback rate readout.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .systemBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            if (hudExpanded) {
                TelemetryHudCard(
                    mode = mode,
                    onModeSelected = { mode = it },
                    displayRefreshHz = displayRefreshHz,
                    composeFrameHz = composeFrameHz,
                    cameraCallbackHz = cameraCallbackHz,
                    isMoving = cameraPositionState.isMoving,
                    mapSize = mapSize,
                    densityDpi = configuration.densityDpi,
                    onHideHud = { hudExpanded = false },
                )
            } else {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                    shadowElevation = 4.dp,
                    modifier = Modifier.align(Alignment.TopEnd),
                ) {
                    TextButton(onClick = { hudExpanded = true }) {
                        Text(text = "Show HUD")
                    }
                }
            }
        }
    }
}

/**
 * Projects each [OverlayDemoPin] from geographic coordinates into screen pixels using
 * [CameraPositionState.projection] whenever [CameraPositionState.position] changes.
 *
 * Because `CameraPositionState.position` is updated asynchronously via `OnCameraMove` on the
 * Android UI thread, these Compose pills visibly lag and double-step behind the green native
 * map circles whenever the user pans on a high-refresh-rate display.
 */
@Composable
private fun ProjectedComposeOverlays(
    cameraPositionState: CameraPositionState,
    pins: List<OverlayDemoPin>,
    selectedPinId: String?,
    onPinClick: (String) -> Unit,
) {
    // Reading cameraPositionState.position subscribes this composable to OnCameraMove updates.
    @Suppress("UNUSED_VARIABLE")
    val currentCameraPosition = cameraPositionState.position
    val projection = cameraPositionState.projection ?: return

    val density = LocalDensity.current
    val halfPillWidthPx = with(density) { 34.dp.roundToPx() }
    val pillHeightPx = with(density) { 32.dp.roundToPx() }

    Box(modifier = Modifier.fillMaxSize()) {
        pins.forEach { pin ->
            val screenPoint = projection.toScreenLocation(pin.position)
            val isSelected = selectedPinId == pin.id
            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            x = screenPoint.x - halfPillWidthPx,
                            y = screenPoint.y - pillHeightPx,
                        )
                    }
                    .clickable { onPinClick(pin.id) },
            ) {
                PricePill(
                    label = pin.label,
                    selected = isSelected,
                    showAnchorDot = true,
                )
            }
        }
    }
}

@Composable
private fun PricePill(
    label: String,
    selected: Boolean,
    showAnchorDot: Boolean,
    modifier: Modifier = Modifier,
) {
    val containerColor by animateColorAsState(
        targetValue = if (selected) SelectedPillColor else DefaultPillColor,
        label = "pillContainerColor",
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) DefaultPillColor else SelectedPillColor,
        label = "pillContentColor",
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = containerColor,
            contentColor = contentColor,
            shadowElevation = 4.dp,
            border = BorderStroke(1.dp, if (selected) SelectedPillColor else DefaultPillBorder),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
            )
        }
        if (showAnchorDot) {
            // Small red anchor dot directly under the pill aligns with the green map circle when
            // stationary and visually highlights any pixel offset during panning.
            Surface(
                shape = CircleShape,
                color = Color(0xFFDC2626),
                border = BorderStroke(1.5.dp, Color.White),
                modifier = Modifier.size(8.dp),
                content = {},
            )
        }
    }
}

@Composable
private fun TelemetryHudCard(
    mode: OverlaySyncMode,
    onModeSelected: (OverlaySyncMode) -> Unit,
    displayRefreshHz: Float,
    composeFrameHz: Float,
    cameraCallbackHz: Float,
    isMoving: Boolean,
    mapSize: IntSize,
    densityDpi: Int,
    onHideHud: () -> Unit,
) {
    val budgetMs = if (displayRefreshHz > 0f) 1000f / displayRefreshHz else 16.67f

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
        shadowElevation = 4.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Compose Overlay Sync",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                TextButton(onClick = onHideHud) {
                    Text(text = "Hide HUD")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                OverlaySyncMode.entries.forEach { option ->
                    FilterChip(
                        selected = mode == option,
                        onClick = { onModeSelected(option) },
                        label = {
                            Text(
                                text = option.label,
                                style = MaterialTheme.typography.labelSmall,
                            )
                        },
                    )
                }
            }

            val rateSummary = if (isMoving) {
                "Display: ${displayRefreshHz.roundToInt()} Hz (${"%.1f".format(budgetMs)} ms)  |  " +
                    "Compose: ${composeFrameHz.roundToInt()} Hz  |  " +
                    "OnCameraMove: ${cameraCallbackHz.roundToInt()} Hz"
            } else {
                "Display: ${displayRefreshHz.roundToInt()} Hz (${"%.1f".format(budgetMs)} ms)  |  " +
                    "Pan map to measure callback Hz"
            }

            Text(
                text = rateSummary,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
            )

            Text(
                text = "Viewport: ${mapSize.width}x${mapSize.height} px @ $densityDpi dpi  " +
                    "(Green = Map GL/Vulkan, Red/Pill = Compose UI)",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
