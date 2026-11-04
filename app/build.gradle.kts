import java.util.Properties

plugins {
    id("com.android.application")
}

// A release-kulcs sosem kerül a repóba. Helyben keystore.properties, CI-ban környezeti változók adják.
val signing = Properties().apply {
    val local = rootProject.file("keystore.properties")
    if (local.exists()) local.inputStream().use { load(it) }
}
fun secret(key: String, env: String): String? = signing.getProperty(key) ?: System.getenv(env)

android {
    namespace = "io.github.yxonyx.kapu"
    compileSdk = 37

    defaultConfig {
        applicationId = "io.github.yxonyx.kapu"
        minSdk = 26
        targetSdk = 36
        versionCode = 2
        versionName = "1.1.0"
    }

    signingConfigs {
        val store = secret("storeFile", "KAPU_STORE_FILE")
        if (store != null) {
            create("release") {
                storeFile = rootProject.file(store)
                storePassword = secret("storePassword", "KAPU_STORE_PASSWORD")
                keyAlias = secret("keyAlias", "KAPU_KEY_ALIAS")
                keyPassword = secret("keyPassword", "KAPU_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.findByName("release")
        }
    }

    // Reprodukálható build (IzzyOnDroid, F-Droid): a Play-metaadat-blokk kimarad.
    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.8.0")
    implementation("com.google.android.material:material:1.14.0")
    testImplementation("junit:junit:4.13.2")
    // Az android.jar org.json-ja a JVM-tesztekben csak csonk. Ez a valódi, a RuleStore tesztjeihez.
    testImplementation("org.json:json:20260814")
}
