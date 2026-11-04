import java.util.Properties

plugins { id("com.android.application") }

android {
    namespace = "com.opensandbox.dhizuku"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.opensandbox.dhizuku"
        minSdk = 28
        targetSdk = 34
        versionCode = 2
        versionName = "0.2.0"
    }

    buildFeatures { aidl = true }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
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

dependencies { implementation("io.github.iamr0s:Dhizuku-API:2.4") }
