package com.opensandbox.dhizuku.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
internal fun SettingsScreen(state: AppState) {
    val context = LocalContext.current
    var showLog by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        GlassCard(Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.settings_appearance), fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
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
        }

        GlassCard(Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.settings_font), fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
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
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(
                    if (state.harmonyReady) R.string.font_harmony_ready
                    else R.string.font_harmony_missing
                ),
                style = MaterialTheme.typography.bodySmall,
            )
            if (!state.harmonyReady) {
                Spacer(Modifier.height(6.dp))
                OutlinedButton(onClick = { state.downloadHarmonyFont() }, enabled = !state.fontBusy) {
                    Text(stringResource(R.string.font_download))
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                stringResource(
                    if (state.notoReady) R.string.font_noto_ready
                    else R.string.font_noto_missing
                ),
                style = MaterialTheme.typography.bodySmall,
            )
            if (!state.notoReady) {
                Spacer(Modifier.height(6.dp))
                OutlinedButton(
                    onClick = { state.downloadFont(DownloadableFont.NOTO) },
                    enabled = !state.fontBusy,
                ) { Text(stringResource(R.string.font_noto_download)) }
            }
        }

        GlassCard(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.settings_glass), fontWeight = FontWeight.SemiBold)
                    Text(stringResource(R.string.settings_glass_hint), style = MaterialTheme.typography.bodySmall)
                }
                SkinSwitch(checked = state.glassEnabled, onCheckedChange = { state.glassEnabled = it })
            }
            Spacer(Modifier.height(10.dp))
            Text("玻璃透明度  ${(state.glassAlpha * 100).toInt()}%", style = MaterialTheme.typography.bodySmall)
            Slider(
                value = state.glassAlpha,
                onValueChange = { state.applyGlassAlpha(it) },
                valueRange = 0.05f..0.6f,
            )
            Text("背景光晕  ${(state.glowStrength * 100).toInt()}%", style = MaterialTheme.typography.bodySmall)
            Slider(
                value = state.glowStrength,
                onValueChange = { state.applyGlowStrength(it) },
                valueRange = 0f..1.5f,
            )
        }

        GlassCard(Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.settings_about), fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            Text(stringResource(R.string.version_format, BuildConfig.VERSION_NAME), style = MaterialTheme.typography.bodySmall)
            Text(stringResource(R.string.about_package, context.packageName), style = MaterialTheme.typography.bodySmall)
            Text(stringResource(R.string.about_managers, state.installedManagers().joinToString(", ")), style = MaterialTheme.typography.bodySmall)
            Text(stringResource(R.string.about_shell, state.shellLabel.ifEmpty { "未解析" }), style = MaterialTheme.typography.bodySmall)
        }

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
