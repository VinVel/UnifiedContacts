plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "net.velcore.unifiedcontacts"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "net.velcore.unifiedcontacts"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    releaseImplementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation("com.github.vestrel00:contacts-android:0.5.0")
    implementation("com.bettermile:address-formatter-kotlin:0.4.7")
}