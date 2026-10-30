plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "io.github.lxzrvi.rekey"
    compileSdk = 37
    compileSdkMinor = 0

    defaultConfig {
        applicationId = "io.github.lxzrvi.rekey"
        minSdk = 31
        targetSdk = 37
        versionCode = 3
        versionName = "0.3.0"
        vectorDrawables.useSupportLibrary = true
    }

    buildTypes {
        release {
            // Fast GitHub-only release path: package directly, without R8/resource shrinking.
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            // Production signing is configured only when a secure keystore is available.
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.bundles.compose)
    implementation(libs.haze)
    implementation(libs.haze.blur)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
