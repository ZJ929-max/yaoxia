# 清单里声明的组件由 AGP 自动保留，这里只补三方库的反射/序列化需求

# Dhizuku / Shizuku 的 AIDL 与回调
-keep class com.rosan.dhizuku.** { *; }
-keep class rikka.shizuku.** { *; }

# Backdrop / Miuix 走 Compose，运行时按名称查找的类很少，但保留入口更稳
-keep class com.kyant.backdrop.** { *; }
-keep class top.yukonga.miuix.** { *; }

# 保留行号，崩溃栈可读
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Dhizuku 的 init() 引用了隐藏类，编译期不存在，运行时由系统提供
-dontwarn android.app.ActivityThread

# 四大组件按基类兜底保留：AGP 对清单组件的自动 keep 在
# 「清单里是相对类名 + R8 全量混淆」时可能漏掉，导致装机即 ClassNotFound
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
