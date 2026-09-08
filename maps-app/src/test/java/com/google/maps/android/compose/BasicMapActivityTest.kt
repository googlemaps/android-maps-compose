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

package com.google.maps.android.compose

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class BasicMapActivityTest {

    @Test
    fun lightSchemeTogglesToDark() {
        val result = toggledMapColorScheme(
            current = ComposeMapColorScheme.LIGHT,
            systemInDarkTheme = false,
        )

        assertThat(result).isEqualTo(ComposeMapColorScheme.DARK)
    }

    @Test
    fun darkSchemeTogglesToLight() {
        val result = toggledMapColorScheme(
            current = ComposeMapColorScheme.DARK,
            systemInDarkTheme = true,
        )

        assertThat(result).isEqualTo(ComposeMapColorScheme.LIGHT)
    }

    @Test
    fun followSystemTogglesAwayFromEffectiveScheme() {
        assertThat(
            toggledMapColorScheme(
                current = ComposeMapColorScheme.FOLLOW_SYSTEM,
                systemInDarkTheme = false,
            )
        ).isEqualTo(ComposeMapColorScheme.DARK)
        assertThat(
            toggledMapColorScheme(
                current = ComposeMapColorScheme.FOLLOW_SYSTEM,
                systemInDarkTheme = true,
            )
        ).isEqualTo(ComposeMapColorScheme.LIGHT)
    }
}
