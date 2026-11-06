package com.opensandbox.dhizuku.ui

import android.content.Context
import androidx.compose.material3.Typography
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import com.opensandbox.dhizuku.core.Outcome
import top.yukonga.miuix.kmp.theme.TextStyles
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

enum class AppFont(val key: String) {
    SYSTEM("system"),
    DEFAULT("default"),
    HARMONY("harmony");

    companion object {
        fun of(key: String?): AppFont = entries.firstOrNull { it.key == key } ?: SYSTEM
    }
}

/** 当前生效的字体族；null 表示跟随平台默认。 */
val LocalFontFamily = staticCompositionLocalOf<FontFamily?> { null }

object FontPrefs {
    private const val PREF = "open_sandbox_ui"
    private const val KEY = "font_choice"

    fun load(context: Context): AppFont =
        AppFont.of(context.getSharedPreferences(PREF, Context.MODE_PRIVATE).getString(KEY, null))

    fun save(context: Context, font: AppFont) {
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().putString(KEY, font.key).apply()
    }
}

/** 可下载字体：不打包进 APK，按需下载到 filesDir。 */
enum class DownloadableFont(val fileName: String, val label: String, val mirrors: List<String>) {
    HARMONY(
        "HarmonyOS_SansSC_Regular.ttf",
        "鸿蒙字体",
        listOf(
            "https://cdn.jsdelivr.net/gh/zhiyuan1i/fonts-harmonyos-sans-cn@main/source/usr/share/fonts/truetype/harmonyos-sans/HarmonyOS_SansSC_Regular.ttf",
            "https://raw.githubusercontent.com/zhiyuan1i/fonts-harmonyos-sans-cn/main/source/usr/share/fonts/truetype/harmonyos-sans/HarmonyOS_SansSC_Regular.ttf",
            "https://ghproxy.net/https://raw.githubusercontent.com/zhiyuan1i/fonts-harmonyos-sans-cn/main/source/usr/share/fonts/truetype/harmonyos-sans/HarmonyOS_SansSC_Regular.ttf",
        ),
    ),
    NOTO(
        "NotoSansSC[wght].ttf",
        "思源黑体",
        listOf(
            "https://cdn.jsdelivr.net/gh/google/fonts@main/ofl/notosanssc/NotoSansSC%5Bwght%5D.ttf",
            "https://raw.githubusercontent.com/google/fonts/main/ofl/notosanssc/NotoSansSC%5Bwght%5D.ttf",
        ),
    ),
}

object FontFiles {
    fun file(context: Context, font: DownloadableFont): File = File(context.filesDir, font.fileName)

    fun isReady(context: Context, font: DownloadableFont): Boolean =
        file(context, font).let { it.exists() && it.length() > 1_000_000 }

    fun load(context: Context, font: DownloadableFont): FontFamily? {
        if (!isReady(context, font)) return null
        return runCatching { FontFamily(Font(file(context, font))) }.getOrNull()
    }

    fun download(context: Context, font: DownloadableFont): Outcome<Unit> {
        val target = file(context, font)
        val temp = File(context.cacheDir, font.fileName.replace("[", "_").replace("]", "_") + ".part")
        var lastError = "未知错误"
        for (url in font.mirrors) {
            try {
                temp.delete()
                val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 20_000
                    readTimeout = 180_000
                    instanceFollowRedirects = true
                }
                connection.inputStream.use { input -> temp.outputStream().use { input.copyTo(it) } }
                connection.disconnect()
                if (temp.length() < 1_000_000) {
                    lastError = "下载内容异常：${temp.length()} 字节"
                    continue
                }
                if (target.exists()) target.delete()
                if (temp.renameTo(target)) {
                    android.util.Log.i("YaoxiaFont", "downloaded ${font.name} from $url, ${target.length()} bytes")
                    return Outcome.ok(Unit)
                }
                lastError = "无法写入目标文件"
            } catch (t: Throwable) {
                lastError = "${t.javaClass.simpleName}: ${t.message}"
            }
        }
        android.util.Log.w("YaoxiaFont", "download ${font.name} failed: $lastError")
        return Outcome.fail(lastError)
    }
}

/** 「默认字体」的内置底座：Roboto（Apache-2.0）。中文由系统回退。 */
fun defaultFontFamily(): FontFamily = FontFamily(
    Font(com.opensandbox.dhizuku.R.font.roboto_regular, androidx.compose.ui.text.font.FontWeight.Normal),
)

/** 把字体族套到 Material Typography 的全部样式上。 */
fun Typography.withFamily(family: FontFamily?): Typography {
    if (family == null) return this
    fun TextStyle.f() = copy(fontFamily = family)
    return Typography(
        displayLarge = displayLarge.f(),
        displayMedium = displayMedium.f(),
        displaySmall = displaySmall.f(),
        headlineLarge = headlineLarge.f(),
        headlineMedium = headlineMedium.f(),
        headlineSmall = headlineSmall.f(),
        titleLarge = titleLarge.f(),
        titleMedium = titleMedium.f(),
        titleSmall = titleSmall.f(),
        bodyLarge = bodyLarge.f(),
        bodyMedium = bodyMedium.f(),
        bodySmall = bodySmall.f(),
        labelLarge = labelLarge.f(),
        labelMedium = labelMedium.f(),
        labelSmall = labelSmall.f(),
    )
}

/** 把字体族套到 Miuix 的全部文字样式上。 */
fun TextStyles.withFamily(family: FontFamily?): TextStyles {
    if (family == null) return this
    fun TextStyle.f() = copy(fontFamily = family)
    return TextStyles(
        main = main.f(),
        paragraph = paragraph.f(),
        body1 = body1.f(),
        body2 = body2.f(),
        button = button.f(),
        footnote1 = footnote1.f(),
        footnote2 = footnote2.f(),
        headline1 = headline1.f(),
        headline2 = headline2.f(),
        subtitle = subtitle.f(),
        title1 = title1.f(),
        title2 = title2.f(),
        title3 = title3.f(),
        title4 = title4.f(),
    )
}

/** 视觉参数持久化：让用户能调，而不是写死在代码里。 */
object UiPrefs {
    private const val PREF = "open_sandbox_ui"
    private const val KEY_ALPHA = "glass_alpha"
    private const val KEY_GLOW = "glow_strength"

    private fun prefs(context: Context) = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    fun glassAlpha(context: Context): Float = prefs(context).getFloat(KEY_ALPHA, 0.22f)
    fun setGlassAlpha(context: Context, value: Float) =
        prefs(context).edit().putFloat(KEY_ALPHA, value.coerceIn(0.05f, 0.6f)).apply()

    fun glow(context: Context): Float = prefs(context).getFloat(KEY_GLOW, 1f)
    fun setGlow(context: Context, value: Float) =
        prefs(context).edit().putFloat(KEY_GLOW, value.coerceIn(0f, 1.5f)).apply()
}
