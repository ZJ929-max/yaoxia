// SPDX-License-Identifier: MPL-2.0
// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.

package com.opensandbox.dhizuku.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.opensandbox.dhizuku.BuildConfig
import com.opensandbox.dhizuku.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsScreen(state: AppState, bottomInset: androidx.compose.ui.unit.Dp) {
    val context = LocalContext.current
    var showLog by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(
                start = Metrics.pageHorizontal,
                top = 4.dp,
                end = Metrics.pageHorizontal,
                bottom = bottomInset + 16.dp,
            ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // ---------------- 外观：皮肤 / 字体 / 液态玻璃 三合一 ----------------
        GlassCard(Modifier.fillMaxWidth()) {
            Text(
                stringResource(R.string.settings_appearance),
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.height(12.dp))

            // 皮肤
            Text(stringResource(R.string.settings_skin), style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(6.dp))
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                val options = listOf(
                    Skin.SYSTEM to R.string.appearance_system,
                    Skin.MATERIAL to R.string.appearance_material,
                    Skin.MIUI to R.string.appearance_miui,
                )
                options.forEachIndexed { index, (skin, label) ->
                    SegmentedButton(
                        selected = state.skin == skin,
                        onClick = { state.skin = skin },
                        shape = SegmentedButtonDefaults.itemShape(index, options.size),
                    ) { Text(stringResource(label), maxLines = 1) }
                }
            }

            Spacer(Modifier.height(12.dp))
            SkinDivider()
            Spacer(Modifier.height(12.dp))

            // 字体
            Text(stringResource(R.string.settings_font), style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(6.dp))
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                val fonts = listOf(
                    AppFont.SYSTEM to R.string.font_system,
                    AppFont.DEFAULT to R.string.font_default,
                    AppFont.HARMONY to R.string.font_harmony,
                )
                fonts.forEachIndexed { index, (choice, label) ->
                    SegmentedButton(
                        selected = state.font == choice,
                        onClick = { state.chooseFont(choice) },
                        shape = SegmentedButtonDefaults.itemShape(index, fonts.size),
                    ) { Text(stringResource(label), maxLines = 1) }
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                stringResource(
                    if (state.harmonyReady) R.string.font_harmony_ready
                    else R.string.font_harmony_missing
                ),
                style = MaterialTheme.typography.bodySmall,
            )
            if (!state.harmonyReady) {
                OutlinedButton(
                    onClick = { state.downloadHarmonyFont() },
                    enabled = !state.fontBusy,
                ) { Text(stringResource(R.string.font_download)) }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                stringResource(
                    if (state.notoReady) R.string.font_noto_ready
                    else R.string.font_noto_missing
                ),
                style = MaterialTheme.typography.bodySmall,
            )
            if (!state.notoReady) {
                OutlinedButton(
                    onClick = { state.downloadFont(DownloadableFont.NOTO) },
                    enabled = !state.fontBusy,
                ) { Text(stringResource(R.string.font_noto_download)) }
            }

            Spacer(Modifier.height(12.dp))
            SkinDivider()
            Spacer(Modifier.height(12.dp))

            // 液态玻璃
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.settings_glass), fontWeight = FontWeight.SemiBold)
                    Text(stringResource(R.string.settings_glass_hint), style = MaterialTheme.typography.bodySmall)
                }
                SkinSwitch(checked = state.glassEnabled, onCheckedChange = { state.glassEnabled = it })
            }
            Spacer(Modifier.height(10.dp))
            Text("玻璃透明度  ${(state.glassAlpha * 100).toInt()}%", style = MaterialTheme.typography.bodySmall)
            SkinSlider(
                value = state.glassAlpha,
                onValueChange = { state.applyGlassAlpha(it) },
                valueRange = 0.05f..0.6f,
            )
            Text("玻璃模糊  ${state.blurRadius.toInt()} dp", style = MaterialTheme.typography.bodySmall)
            SkinSlider(
                value = state.blurRadius,
                onValueChange = { state.applyBlur(it) },
                valueRange = 0f..40f,
            )
            Text("背景光晕  ${(state.glowStrength * 100).toInt()}%", style = MaterialTheme.typography.bodySmall)
            SkinSlider(
                value = state.glowStrength,
                onValueChange = { state.applyGlowStrength(it) },
                valueRange = 0f..1.5f,
            )
        }

        // ---------------- 关于 ----------------
        GlassCard(Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.settings_about), fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            Text(stringResource(R.string.version_format, BuildConfig.VERSION_NAME), style = MaterialTheme.typography.bodySmall)
            Text(stringResource(R.string.about_package, context.packageName), style = MaterialTheme.typography.bodySmall)
            Text(stringResource(R.string.about_managers, state.installedManagers().joinToString(", ")), style = MaterialTheme.typography.bodySmall)
            Text(stringResource(R.string.about_shell, state.shellLabel.ifEmpty { "未解析" }), style = MaterialTheme.typography.bodySmall)
        }

        // ---------------- 诊断 ----------------
        GlassCard(Modifier.fillMaxWidth()) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = {
                    val cm = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE)
                        as android.content.ClipboardManager
                    cm.setPrimaryClip(android.content.ClipData.newPlainText("diagnostics", state.diagnosticsReport()))
                    android.widget.Toast.makeText(context, R.string.toast_copied, android.widget.Toast.LENGTH_SHORT).show()
                }) { Text(stringResource(R.string.button_copy_diagnostics)) }

                OutlinedButton(onClick = { showLog = true }) {
                    Text(stringResource(R.string.settings_view_log))
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                stringResource(R.string.log_path, state.logFile()?.absolutePath ?: stringResource(R.string.log_none)),
                style = MaterialTheme.typography.bodySmall,
            )
            Text(stringResource(R.string.settings_ax_hint), style = MaterialTheme.typography.bodySmall)
        }
    }

    if (showLog) {
        val text = state.logFile()?.readText().orEmpty()
        AlertDialog(
            onDismissRequest = { showLog = false },
            title = { Text(stringResource(R.string.settings_log_title)) },
            text = { Text(text.ifEmpty { stringResource(R.string.settings_log_empty) }, style = MaterialTheme.typography.bodySmall) },
            confirmButton = { TextButton(onClick = { showLog = false }) { Text(stringResource(R.string.dialog_cancel)) } },
        )
    }
}
