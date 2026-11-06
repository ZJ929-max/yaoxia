// SPDX-License-Identifier: MPL-2.0
// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.

package com.opensandbox.dhizuku.provision

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ShellCommandsTest {

    @Test
    fun `建工作资料命令格式`() {
        assertEquals(
            "pm create-user --profileOf 0 --managed Work",
            ShellCommands.createManagedProfile("Work"),
        )
    }

    @Test
    fun `装包与移出都带 user`() {
        assertTrue(ShellCommands.installExisting(10, "com.a").contains("--user 10"))
        assertTrue(ShellCommands.uninstallForUser(10, "com.a").contains("--user 10"))
    }

    @Test
    fun `删除用户先停再删`() {
        assertEquals("am stop-user 10", ShellCommands.stopUser(10))
        assertEquals("pm remove-user 10", ShellCommands.removeUser(10))
    }

    @Test
    fun `启动命令带 user 和组件`() {
        val cmd = ShellCommands.launch(10, "com.a/.Main")
        assertTrue(cmd.startsWith("am start"))
        assertTrue(cmd.contains("--user 10"))
        assertTrue(cmd.contains("com.a/.Main"))
    }
}
