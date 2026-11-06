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
        when (intent.getStringExtra("skin")) {
            "miui" -> state.skin = Skin.MIUI
            "system" -> state.skin = Skin.SYSTEM
            "material" -> state.skin = Skin.MATERIAL
        }

        setContent {
            SandboxTheme(state.skin, state.glassEnabled) {
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
