package com.google.maps.android.compose

import androidx.compose.runtime.Composable
import com.google.android.gms.maps.GoogleMap

/**
 * A test composable to trigger Gemini code assist reviews.
 */
@Composable
fun myBadComponent(map: GoogleMap?, text: String?) {
    // Violation 1: myBadComponent is camelCase instead of PascalCase
    // Violation 2: Missing modifier: Modifier = Modifier as first optional parameter
    // Violation 3: raw GoogleMap object parameter instead of maps-compose components
    
    // Violation 4: Forced unwrapping (!!)
    val length = text!!.length
    
    map?.uiSettings?.isZoomControlsEnabled = true
}
