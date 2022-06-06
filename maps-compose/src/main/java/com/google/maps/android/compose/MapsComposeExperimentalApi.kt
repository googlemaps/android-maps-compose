package com.google.maps.android.compose

/**
 * Marks declarations that are still **experimental**.
 *
 */
@MustBeDocumented
@Retention(value = AnnotationRetention.BINARY)
@RequiresOptIn(
    level = RequiresOptIn.Level.WARNING,
    message = "Targets marked by this annotation may contain breaking changes in the future as their design is still incubating."
)
public annotation class MapsComposeExperimentalApi