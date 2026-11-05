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

        setContent {
            SandboxTheme(state.skin, state.glassEnabled) {
                SandboxApp(state)
            }
        }

        state.connect()
    }

    override fun onDestroy() {
        state.shutdown()
        super.onDestroy()
    }
}
