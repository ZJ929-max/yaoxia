import java.util.Properties

val versionProperties = Properties().apply {
    rootProject.file("version.properties").inputStream().use { load(it) }
}

plugins { id("com.android.application") }

android {
    namespace = "com.opensandbox.dhizuku"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.opensandbox.dhizuku"
        minSdk = 28
        targetSdk = 34
        versionCode = versionProperties.getProperty("versionCode").toInt()
        versionName = versionProperties.getProperty("versionName")
    }

    buildFeatures {
        aidl = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    signingConfigs {
        create("release") {
            val ks = rootProject.file("keystore.properties")
            if (ks.exists()) {
                val p = Properties().apply { ks.inputStream().use { this.load(it) } }
                storeFile = rootProject.file(p.getProperty("storeFile"))
                storePassword = p.getProperty("storePassword")
                keyAlias = p.getProperty("keyAlias")
                keyPassword = p.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            // 保持类名不被混淆：Dhizuku 会用类名反射加载 RemoteDpmService
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
        }
    }
}

dependencies {
    // dhi / she 的协议客户端
    implementation("io.github.iamr0s:Dhizuku-API:2.5.4")
    // shi 的官方 SDK
    implementation("dev.rikka.shizuku:api:13.0.0")
}
