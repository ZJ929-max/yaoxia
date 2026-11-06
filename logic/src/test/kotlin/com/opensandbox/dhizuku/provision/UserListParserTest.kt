package com.opensandbox.dhizuku.provision

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UserListParserTest {

    private val sample = """
        Users:
        	UserInfo{0:机主:4c13} running
        	UserInfo{10:OpenSandbox:410} running
        	UserInfo{11:Work:420}
    """.trimIndent()

    @Test
    fun `按名字找到用户 id`() {
        assertEquals(10, UserListParser.findId(sample, "OpenSandbox"))
        assertEquals(11, UserListParser.findId(sample, "Work"))
    }

    @Test
    fun `找不到返回 null`() {
        assertNull(UserListParser.findId(sample, "NotExist"))
    }

    @Test
    fun `列出全部 id`() {
        assertEquals(listOf(0, 10, 11), UserListParser.allIds(sample))
    }

    @Test
    fun `空输出不崩`() {
        assertEquals(emptyList<Int>(), UserListParser.allIds(""))
        assertNull(UserListParser.findId("", "x"))
    }
}
