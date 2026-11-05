package com.opensandbox.dhizuku.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
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

@Composable
internal fun SettingsScreen(state: AppState) {
    Column(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(stringResource(R.string.settings_appearance), style = MaterialTheme.typography.titleSmall)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = { state.skin = Skin.MATERIAL },
                enabled = state.skin != Skin.MATERIAL,
            ) { Text(stringResource(R.string.appearance_material)) }

            OutlinedButton(
                onClick = { state.skin = Skin.MIUI },
                enabled = state.skin != Skin.MIUI,
            ) { Text(stringResource(R.string.appearance_miui)) }
        }

        Divider()

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.settings_glass), style = MaterialTheme.typography.titleSmall)
                Text(
                    stringResource(R.string.settings_glass_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            SkinSwitch(
                checked = state.glassEnabled,
                onCheckedChange = { state.glassEnabled = it },
            )
        }

        Divider()

        val context = LocalContext.current

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = {
                val clipboard =
                    context.getSystemService(android.content.Context.CLIPBOARD_SERVICE)
                        as android.content.ClipboardManager
                clipboard.setPrimaryClip(
                    android.content.ClipData.newPlainText("diagnostics", state.diagnosticsReport()),
                )
            }) { Text(stringResource(R.string.button_copy_diagnostics)) }
        }

        Text(
            stringResource(R.string.settings_ax_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Text(
            stringResource(
                R.string.log_path,
                state.logFile()?.absolutePath ?: stringResource(R.string.log_none),
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Text(
            stringResource(R.string.settings_about),
            style = MaterialTheme.typography.titleSmall,
        )
        Text(
            stringResource(R.string.version_format, BuildConfig.VERSION_NAME),
            style = MaterialTheme.typography.bodySmall,
        )
        Text(
            stringResource(R.string.about_package, context.packageName),
            style = MaterialTheme.typography.bodySmall,
        )
        Text(
            stringResource(R.string.about_managers, state.installedManagers().joinToString(", ")),
            style = MaterialTheme.typography.bodySmall,
        )
        Text(
            stringResource(R.string.about_shell, state.shellLabel.ifEmpty { "未解析" }),
            style = MaterialTheme.typography.bodySmall,
        )
    }
}
