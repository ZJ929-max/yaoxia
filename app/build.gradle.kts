import java.util.Properties

plugins {
    // AGP 9 起 Kotlin 支持内置，不再需要 org.jetbrains.kotlin.android
    id("com.android.application")
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
            // 保持类名不被混淆（如需反射加载）
            isMinifyEnabled = false
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
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
}
