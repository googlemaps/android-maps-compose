package com.google.maps.android.compose.navigation

/**
 * Represents UI events that can be triggered within the UI from a view model.
 */
sealed interface UiEvent {
    data class ShowSnackbar(val message: String) : UiEvent
    data object RequestLocationPermission : UiEvent
}