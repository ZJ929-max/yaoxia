package com.opensandbox.dhizuku.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.opensandbox.dhizuku.BuildConfig
import com.opensandbox.dhizuku.R

/** 主题：只换配色，布局不变——所以"外观"和"玻璃"是两个独立开关。 */
@Composable
fun SandboxTheme(skin: Skin, content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val base = if (dark) darkColorScheme() else lightColorScheme()

    val colors = when (skin) {
        Skin.MATERIAL -> base
        Skin.MIUI -> if (dark) {
            base.copy(
                primary = Color(0xFF9EC0FF),
                primaryContainer = Color(0xFF00306B),
                onPrimaryContainer = Color(0xFFDCE8FF)
            )
        } else {
            base.copy(
                primary = Color(0xFF3482FF),
                primaryContainer = Color(0xFFDCE8FF),
                onPrimaryContainer = Color(0xFF00174A)
            )
        }
    }

    MaterialTheme(colorScheme = colors, content = content)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SandboxApp(state: AppState) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val titles = listOf(
        stringResource(R.string.tab_status),
        stringResource(R.string.tab_apps),
        stringResource(R.string.tab_profile),
        stringResource(R.string.tab_settings)
    )

    Scaffold(
        topBar = { TopAppBar(title = { Text(titles[tab]) }) },
        bottomBar = {
            NavigationBar {
                titles.forEachIndexed { index, title ->
                    NavigationBarItem(
                        selected = tab == index,
                        onClick = { tab = index },
                        icon = {},
                        label = { Text(title) }
                    )
                }
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            when (tab) {
                0 -> StatusScreen(state)
                1 -> AppsScreen(state)
                2 -> ProfileScreen(state)
                else -> SettingsScreen(state)
            }
        }
    }
}

// ---------------------------------------------------------------- 状态页

@Composable
private fun StatusScreen(state: AppState) {
    LazyColumn(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(state.status, style = MaterialTheme.typography.bodyMedium)
                    if (state.session.isNotEmpty()) {
                        Text(
                            state.session,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(stringResource(R.string.section_channels), fontWeight = FontWeight.SemiBold)
                    state.channels.forEach { channel ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(channel.name, style = MaterialTheme.typography.bodySmall)
                            Text(
                                stringResource(
                                    if (channel.available) R.string.channel_available
                                    else R.string.channel_unavailable
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = if (channel.available) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = state::connect, enabled = !state.busy) {
                    Text(stringResource(R.string.button_connect))
                }
                OutlinedButton(onClick = state::requestScopes) {
                    Text(stringResource(R.string.button_scopes))
                }
            }
        }
    }
}

// ---------------------------------------------------------------- 应用页

@Composable
private fun AppsScreen(state: AppState) {
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                stringResource(R.string.apps_count, state.apps.size),
                style = MaterialTheme.typography.bodySmall
            )
            TextButton(onClick = state::refreshApps) {
                Text(stringResource(R.string.button_refresh))
            }
        }

        if (state.apps.isEmpty()) {
            Text(
                stringResource(R.string.apps_empty),
                Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodyMedium
            )
            return@Column
        }

        LazyColumn(Modifier.fillMaxSize()) {
            items(state.apps, key = { it.packageName }) { app ->
                Column(Modifier.fillMaxWidth()) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(app.label, fontWeight = FontWeight.Medium)
                            Text(
                                app.packageName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = app.frozen,
                            onCheckedChange = { state.toggleFrozen(app) }
                        )
                    }
                    Divider()
                }
            }
        }
    }
}

// ---------------------------------------------------------------- 隔离空间页

@Composable
private fun ProfileScreen(state: AppState) {
    Column(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(stringResource(R.string.profile_title), style = MaterialTheme.typography.titleMedium)
        Text(
            stringResource(R.string.profile_explain),
            style = MaterialTheme.typography.bodyMedium
        )
        Button(onClick = state::provisionManagedProfile) {
            Text(stringResource(R.string.button_provision_system))
        }
        OutlinedButton(onClick = state::createProfile) {
            Text(stringResource(R.string.button_create_profile))
        }
        Card(
            Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Text(
                state.status,
                Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

// ---------------------------------------------------------------- 设置页

@Composable
private fun SettingsScreen(state: AppState) {
    Column(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(stringResource(R.string.settings_appearance), style = MaterialTheme.typography.titleSmall)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = { state.skin = Skin.MATERIAL },
                enabled = state.skin != Skin.MATERIAL
            ) { Text(stringResource(R.string.appearance_material)) }

            OutlinedButton(
                onClick = { state.skin = Skin.MIUI },
                enabled = state.skin != Skin.MIUI
            ) { Text(stringResource(R.string.appearance_miui)) }
        }

        Divider()

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.settings_glass), style = MaterialTheme.typography.titleSmall)
                Text(
                    stringResource(R.string.settings_glass_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = state.glassEnabled,
                onCheckedChange = { state.glassEnabled = it }
            )
        }

        Divider()

        Text(
            stringResource(R.string.version_format, BuildConfig.VERSION_NAME),
            style = MaterialTheme.typography.bodySmall
        )
    }
}
