// SPDX-License-Identifier: MPL-2.0
// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.

package com.opensandbox.dhizuku.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.opensandbox.dhizuku.core.FailureReporter

/**
 * 只做三件事：装崩溃兜底、创建状态、把 Compose 挂上去。
 * 业务逻辑全在 AppState 与各层，界面不再碰 DevicePolicyManager。
 */
class MainActivity : ComponentActivity() {
    private lateinit var state: AppState

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        FailureReporter.install(this)
        state = AppState(this)
        when (intent.getStringExtra("font")) {
            "harmony" -> state.chooseFont(AppFont.HARMONY)
            "default" -> state.chooseFont(AppFont.DEFAULT)
            "system" -> state.chooseFont(AppFont.SYSTEM)
        }
        when (intent.getStringExtra("action")) {
            "downloadfont", "downloadharmony" -> state.downloadHarmonyFont()
            "downloadnoto" -> state.downloadFont(DownloadableFont.NOTO)
            "selftest" -> state.runSelfTest()
        }
        if (intent.getStringExtra("search") == "1") state.searchExpanded = true
        when (intent.getStringExtra("skin")) {
            "miui" -> state.skin = Skin.MIUI
            "system" -> state.skin = Skin.SYSTEM
            "material" -> state.skin = Skin.MATERIAL
        }

        setContent {
            val family = when (state.font) {
                AppFont.SYSTEM -> androidx.compose.ui.text.font.FontFamily.Default
                AppFont.DEFAULT -> FontFiles.load(this, DownloadableFont.NOTO) ?: defaultFontFamily()
                AppFont.HARMONY -> FontFiles.load(this, DownloadableFont.HARMONY)
                    ?: androidx.compose.ui.text.font.FontFamily.Default
            }
            SandboxTheme(
                skin = state.skin,
                glassEnabled = state.glassEnabled,
                fontFamily = family,
                glassAlpha = state.glassAlpha,
                glowStrength = state.glowStrength,
                blurRadius = state.blurRadius,
            ) {
                SandboxApp(state, intent.getIntExtra("tab", 0))
            }
        }

        state.connect()
    }

    override fun onDestroy() {
        state.shutdown()
        super.onDestroy()
    }
}
