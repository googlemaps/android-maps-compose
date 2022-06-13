package com.google.maps.android.compose

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.clustering.ClusterItem
import com.google.maps.android.clustering.ClusterManager
import kotlin.random.Random

private val singapore = LatLng(1.35, 103.87)
private val singapore2 = LatLng(2.50, 103.87)
private val TAG = MapClusteringActivity::class.simpleName

class MapClusteringActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GoogleMapClustering()
        }
    }
}

@Composable
fun GoogleMapClustering() {
    val items = remember { mutableStateListOf<MyItem>() }
    LaunchedEffect(Unit) {
        for (i in 1..10) {
            val position = LatLng(
                singapore2.latitude + Random.nextFloat(),
                singapore2.longitude + Random.nextFloat(),
            )
            items.add(MyItem(position, "Marker", "Snippet"))
        }
    }
    GoogleMapClustering(items = items)
}

@OptIn(MapsComposeExperimentalApi::class)
@Composable
fun GoogleMapClustering(items: List<MyItem>) {
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(singapore, 10f)
    }
    GoogleMap(
        modifier = Modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState
    ) {
        val context = LocalContext.current
        var clusterManager by remember { mutableStateOf<ClusterManager<MyItem>?>(null) }
        MapEffect(items) { map ->
            if (clusterManager == null) {
                clusterManager = ClusterManager<MyItem>(context, map)
            }
            clusterManager?.addItems(items)
        }
        LaunchedEffect(key1 = cameraPositionState.isMoving) {
            if (!cameraPositionState.isMoving) {
                clusterManager?.cluster()
            }
        }
        MarkerInfoWindow(
            state = rememberMarkerState(position = singapore),
            onClick = {
                // This won't work :(
                Log.d(TAG, "I cannot be clicked :( $it")
                true
            }
        )
    }
}

data class MyItem(
    val itemPosition: LatLng,
    val itemTitle: String,
    val itemSnippet: String,
) : ClusterItem {
    override fun getPosition(): LatLng =
        itemPosition

    override fun getTitle(): String =
        itemTitle

    override fun getSnippet(): String =
        itemSnippet
}