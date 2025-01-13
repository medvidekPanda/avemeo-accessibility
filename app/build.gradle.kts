plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.avemeo.accessibility_service"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.avemeo.accessibility_service"
        minSdk = 28
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    signingConfigs {
        create("release") {
            storeFile = file("/Users/janptacek/git-projects/keys/avemeo-klic.jks")
            storePassword = "fP7@Nu{vkSCc:z*H8WUy/Q"
            keyAlias = "avemeo-key"
            keyPassword = "fP7@Nu{vkSCc:z*H8WUy/Q"
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        aidl = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
}
