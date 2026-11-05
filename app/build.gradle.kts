import java.util.Properties

plugins {
    id("com.android.application")
}

// The release key never enters the repo: keystore.properties locally, environment variables in CI.
val signing = Properties().apply {
    val local = rootProject.file("keystore.properties")
    if (local.exists()) local.inputStream().use { load(it) }
}
fun secret(key: String, env: String): String? = signing.getProperty(key) ?: System.getenv(env)

android {
    namespace = "io.github.yxonyx.feedcut"
    compileSdk = 37

    defaultConfig {
        applicationId = "io.github.yxonyx.feedcut"
        minSdk = 26
        targetSdk = 36
        versionCode = 2
        versionName = "1.1.0"
    }

    signingConfigs {
        val store = secret("storeFile", "FEEDCUT_STORE_FILE")
        if (store != null) {
            create("release") {
                storeFile = rootProject.file(store)
                storePassword = secret("storePassword", "FEEDCUT_STORE_PASSWORD")
                keyAlias = secret("keyAlias", "FEEDCUT_KEY_ALIAS")
                keyPassword = secret("keyPassword", "FEEDCUT_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            // R8 drops the unused parts of AppCompat and Material: about a quarter of the size.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
            signingConfig = signingConfigs.findByName("release")
        }
    }

    // Reproducible builds (IzzyOnDroid, F-Droid): leave out the Play dependency metadata block.
    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }

    buildFeatures {
        buildConfig = true
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
    // android.jar's org.json is only a stub in JVM tests. The real one, for the RuleStore tests.
    testImplementation("org.json:json:20260814")
}
