import java.util.Properties

plugins {
    // AGP 9 起 Kotlin 支持内置，不再需要 org.jetbrains.kotlin.android
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

val versionProperties = Properties().apply {
    rootProject.file("version.properties").inputStream().use { load(it) }
}

android {
    namespace = "com.opensandbox.dhizuku"
    compileSdk = 37
    buildToolsVersion = "36.0.0"

    defaultConfig {
        applicationId = "com.opensandbox.dhizuku"
        minSdk = 28
        targetSdk = 37
        versionCode = versionProperties.getProperty("versionCode").toInt()
        versionName = versionProperties.getProperty("versionName")
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    signingConfigs {
        create("release") {
            val keystore = rootProject.file("keystore.properties")
            if (keystore.exists()) {
                val props = Properties().apply { keystore.inputStream().use { load(it) } }
                storeFile = rootProject.file(props.getProperty("storeFile"))
                storePassword = props.getProperty("storePassword")
                keyAlias = props.getProperty("keyAlias")
                keyPassword = props.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            // 开 R8：压体积、去无用代码；清单里声明的组件 R8 会自动保留
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }
}

dependencies {
    implementation("io.github.iamr0s:Dhizuku-API:2.5.4")
    implementation("dev.rikka.shizuku:api:13.0.0")
    // 清单里声明的 rikka.shizuku.ShizukuProvider 在这个包里；只加 api 会导致启动即崩
    implementation("dev.rikka.shizuku:provider:13.0.0")

    // 界面：Material 3 + AppCompat
    implementation("com.google.android.material:material:1.12.0")

    // Compose（BOM 统一版本）
    implementation(platform("androidx.compose:compose-bom:2026.09.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")

    // 小米 HyperOS 风格组件（Apache-2.0）
    implementation("top.yukonga.miuix.kmp:miuix-ui:0.9.4")
    // 液态玻璃（Apache-2.0）
    implementation("io.github.kyant0:backdrop:2.0.1")

    // 小米风格组件 + 液态玻璃
    implementation("top.yukonga.miuix.kmp:miuix-ui:0.9.4")
    implementation("io.github.kyant0:backdrop:2.0.1")
    implementation("androidx.activity:activity-compose:1.9.3")
}
